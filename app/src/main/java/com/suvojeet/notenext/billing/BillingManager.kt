package com.suvojeet.notenext.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import com.android.billingclient.api.*
import com.suvojeet.notenext.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BillingManager"
private const val PREFS = "billing_prefs"
private const val KEY_ACCOUNT_ID = "obfuscated_account_id"

/**
 * Donation product IDs — inhe Play Console mein exactly aise hi create karna hai:
 *   donate_small   → ₹49  / $0.99
 *   donate_medium  → ₹149 / $2.99
 *   donate_large   → ₹499 / $9.99
 *
 * Type: One-time product (Consumable) — taaki user baar baar donate kar sake.
 */
object DonationSkus {
    const val SMALL  = "donate_small"
    const val MEDIUM = "donate_medium"
    const val LARGE  = "donate_large"

    val ALL = listOf(SMALL, MEDIUM, LARGE)
}

/** Connection/catalog failures, each carrying the message the UI should show. */
enum class BillingError(@StringRes val messageRes: Int) {
    NETWORK(R.string.billing_error_network),
    PLAY_UNAVAILABLE(R.string.billing_error_play_unavailable),
    PRODUCTS_UNAVAILABLE(R.string.billing_error_products_unavailable),
    UNKNOWN(R.string.billing_error_unknown),
}

sealed class BillingState {
    data object Loading : BillingState()
    data object Ready   : BillingState()
    data class  Error(val error: BillingError) : BillingState()
}

sealed class PurchaseState {
    data object Idle : PurchaseState()

    /** Billing flow launched — Play's sheet is up, or we're waiting on its result. */
    data object InProgress : PurchaseState()

    /**
     * Purchase created but not paid yet (UPI, cash, carrier billing). Play may take
     * minutes or days to confirm; the user must NOT be asked to pay again.
     */
    data object AwaitingPayment : PurchaseState()

    data object Success : PurchaseState()
    data class  Failed(@StringRes val messageRes: Int) : PurchaseState()
}

/**
 * Price of a donation product. PBL 9 lets a one-time product carry several offers,
 * so fall back to the offer list when the legacy single-offer accessor is empty.
 */
fun ProductDetails.donationOffer(): ProductDetails.OneTimePurchaseOfferDetails? =
    oneTimePurchaseOfferDetails ?: oneTimePurchaseOfferDetailsList?.firstOrNull()

fun ProductDetails.donationPrice(): String = donationOffer()?.formattedPrice.orEmpty()

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PurchasesUpdatedListener {

    private val _billingState = MutableStateFlow<BillingState>(BillingState.Loading)
    val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Tokens currently being consumed. onPurchasesUpdated and the startup reconcile
     * can surface the same purchase at once — without this they'd both call
     * consumeAsync and the loser would report a spurious failure.
     */
    private val consuming = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    /** Stable anonymous per-install id — helps Play's fraud detection. Prefetched off the main thread. */
    @Volatile private var obfuscatedAccountId: String? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        // PBL 9: the library re-establishes a dropped connection on the next API
        // call, so we no longer hand-roll exponential backoff.
        .enableAutoServiceReconnection()
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    init {
        scope.launch(Dispatchers.IO) { obfuscatedAccountId = loadOrCreateAccountId() }
        connect()
    }

    private fun loadOrCreateAccountId(): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ACCOUNT_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_ACCOUNT_ID, it).apply()
        }
    }

    private fun connect() {
        if (billingClient.isReady) {
            onConnected()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    onConnected()
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.responseCode} ${result.debugMessage}")
                    _billingState.value = BillingState.Error(result.responseCode.toBillingError())
                }
            }

            override fun onBillingServiceDisconnected() {
                // enableAutoServiceReconnection() handles the retry. Don't surface an
                // error here — a transient drop would otherwise blank out a working screen.
                Log.d(TAG, "Billing service disconnected; auto-reconnect will retry")
            }
        })
    }

    private fun onConnected() {
        queryProducts()
        reconcilePurchases()
    }

    /**
     * Called when the donate screen opens or resumes. Picks up purchases that
     * completed while the app was away (deferred UPI payments in particular).
     */
    fun refresh() {
        if (billingClient.isReady) {
            if (_products.value.isEmpty()) queryProducts()
            reconcilePurchases()
        } else {
            _billingState.value = BillingState.Loading
            connect()
        }
    }

    /** User tapped "Try again" on the error state. */
    fun retry() {
        _billingState.value = BillingState.Loading
        connect()
    }

    private fun queryProducts() {
        val productList = DonationSkus.ALL.map { sku ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(sku)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        // PBL 9 signature: the callback delivers a QueryProductDetailsResult rather
        // than a bare List<ProductDetails>.
        billingClient.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "queryProductDetails failed: ${result.responseCode} ${result.debugMessage}")
                _billingState.value = BillingState.Error(result.responseCode.toBillingError())
                return@queryProductDetailsAsync
            }

            queryResult.unfetchedProductList.forEach {
                Log.w(TAG, "Product not fetched: ${it.productId} (status ${it.statusCode})")
            }

            // Sort by price ascending (small → medium → large)
            val details = queryResult.productDetailsList.sortedBy {
                it.donationOffer()?.priceAmountMicros ?: 0L
            }
            _products.value = details
            _billingState.value = if (details.isEmpty()) {
                // Products missing from Play Console, or not yet live on this track.
                BillingState.Error(BillingError.PRODUCTS_UNAVAILABLE)
            } else {
                BillingState.Ready
            }
        }
    }

    /**
     * Consumes anything Play still has on record for this user.
     *
     * Without this, a donation that completed while the app was closed (or whose
     * consume call never landed) stays owned forever: the user can't donate again
     * and Play auto-refunds the payment after three days.
     */
    private fun reconcilePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "queryPurchases failed: ${result.responseCode} ${result.debugMessage}")
                return@queryPurchasesAsync
            }
            purchases.forEach { handlePurchase(it, userInitiated = false) }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        _purchaseState.value = PurchaseState.InProgress

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .apply {
                // Only needed when the product exposes offers rather than a single
                // legacy price; naming the offer is required in that case.
                if (productDetails.oneTimePurchaseOfferDetails == null) {
                    productDetails.donationOffer()?.offerToken?.let { setOfferToken(it) }
                }
            }
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .apply { obfuscatedAccountId?.let { setObfuscatedAccountId(it) } }
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "launchBillingFlow failed: ${result.responseCode} ${result.debugMessage}")
            onPurchaseFailure(result)
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it, userInitiated = true) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = PurchaseState.Idle
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // A previous donation is still unconsumed. Clear it so the next tap works.
                _purchaseState.value = PurchaseState.Failed(R.string.purchase_error_already_owned)
                reconcilePurchases()
            }
            else -> onPurchaseFailure(result)
        }
    }

    private fun handlePurchase(purchase: Purchase, userInitiated: Boolean) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> consume(purchase, userInitiated)
            Purchase.PurchaseState.PENDING -> _purchaseState.value = PurchaseState.AwaitingPayment
            else -> {
                // UNSPECIFIED_STATE — nothing is owed and nothing to consume.
                Log.d(TAG, "Ignoring purchase in unspecified state: ${purchase.products}")
            }
        }
    }

    /**
     * Donation = consumable purchase.
     * Consume karna ZAROORI hai taaki user aage bhi donate kar sake.
     *
     * @param userInitiated true when this came from a tap. Recovered purchases still
     *   show the thank-you (the user did pay) but stay silent on failure, so a
     *   background reconcile never throws an unexplained error at the user.
     */
    private fun consume(purchase: Purchase, userInitiated: Boolean) {
        if (!consuming.add(purchase.purchaseToken)) return

        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.consumeAsync(consumeParams) { consumeResult, _ ->
            consuming.remove(purchase.purchaseToken)
            when (consumeResult.responseCode) {
                BillingClient.BillingResponseCode.OK,
                    // Already consumed elsewhere (e.g. another device) — same outcome.
                BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
                    _purchaseState.value = PurchaseState.Success
                }
                else -> {
                    Log.w(TAG, "consume failed: ${consumeResult.responseCode} ${consumeResult.debugMessage}")
                    if (userInitiated) {
                        _purchaseState.value = PurchaseState.Failed(R.string.purchase_error_generic)
                    }
                }
            }
        }
    }

    private fun onPurchaseFailure(result: BillingResult) {
        _purchaseState.value = PurchaseState.Failed(result.toPurchaseMessage())
    }

    fun resetPurchaseState() {
        _purchaseState.value = PurchaseState.Idle
    }
}

private fun Int.toBillingError(): BillingError = when (this) {
    BillingClient.BillingResponseCode.NETWORK_ERROR,
    BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
    BillingClient.BillingResponseCode.SERVICE_TIMEOUT,
    BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> BillingError.NETWORK

    // PBL 9 reports a blocked Play Store (OEM kids mode, etc.) as BILLING_UNAVAILABLE
    // where PBL 7 returned a generic ERROR.
    BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
    BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> BillingError.PLAY_UNAVAILABLE

    BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> BillingError.PRODUCTS_UNAVAILABLE

    else -> BillingError.UNKNOWN
}

/** PBL 9 adds a sub-response code that explains *why* a purchase was declined. */
@StringRes
private fun BillingResult.toPurchaseMessage(): Int {
    when (onPurchasesUpdatedSubResponseCode) {
        BillingClient.OnPurchasesUpdatedSubResponseCode.PAYMENT_DECLINED_DUE_TO_INSUFFICIENT_FUNDS ->
            return R.string.purchase_error_insufficient_funds
        BillingClient.OnPurchasesUpdatedSubResponseCode.USER_INELIGIBLE ->
            return R.string.purchase_error_ineligible
    }
    return when (responseCode) {
        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> R.string.purchase_error_already_owned
        else -> responseCode.toBillingError().let {
            if (it == BillingError.UNKNOWN) R.string.purchase_error_generic else it.messageRes
        }
    }
}
