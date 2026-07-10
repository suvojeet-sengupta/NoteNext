package com.suvojeet.notenext.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

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

sealed class BillingState {
    data object Loading  : BillingState()
    data object Ready    : BillingState()
    data object Error    : BillingState()
}

sealed class PurchaseState {
    data object Idle      : PurchaseState()
    data object Pending   : PurchaseState()
    data object Success   : PurchaseState()
    data class  Failed(val message: String) : PurchaseState()
}

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

    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val reconnectScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    init {
        connectToPlayBilling()
    }

    private fun connectToPlayBilling() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    reconnectAttempts = 0
                    _billingState.value = BillingState.Ready
                    queryProducts()
                } else {
                    _billingState.value = BillingState.Error
                }
            }

            override fun onBillingServiceDisconnected() {
                _billingState.value = BillingState.Error
                if (reconnectAttempts < maxReconnectAttempts) {
                    val delayMs = (Math.pow(2.0, reconnectAttempts.toDouble()) * 1000L).toLong().coerceAtMost(30_000L)
                    reconnectAttempts++
                    reconnectScope.launch {
                        delay(delayMs)
                        connectToPlayBilling()
                    }
                }
            }
        })
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

        billingClient.queryProductDetailsAsync(params) { result, productDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                // Sort by price ascending (small → medium → large)
                _products.value = productDetailsList.sortedBy {
                    it.oneTimePurchaseOfferDetails?.priceAmountMicros ?: 0L
                }
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        _purchaseState.value = PurchaseState.Pending

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseState.value = PurchaseState.Failed("Could not launch billing flow: ${result.debugMessage}")
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = PurchaseState.Idle
            }
            else -> {
                _purchaseState.value = PurchaseState.Failed(result.debugMessage)
            }
        }
    }

    /**
     * Donation = consumable purchase.
     * Consume karna ZAROORI hai taaki user aage bhi donate kar sake.
     */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.consumeAsync(consumeParams) { consumeResult, _ ->
                if (consumeResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _purchaseState.value = PurchaseState.Success
                } else {
                    _purchaseState.value = PurchaseState.Failed(consumeResult.debugMessage)
                }
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            _purchaseState.value = PurchaseState.Pending
        }
    }

    fun resetPurchaseState() {
        _purchaseState.value = PurchaseState.Idle
    }
}
