package com.suvojeet.notenext.ui.donate

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.android.billingclient.api.ProductDetails
import com.suvojeet.notenext.billing.BillingManager
import com.suvojeet.notenext.billing.BillingState
import com.suvojeet.notenext.billing.PurchaseState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DonationViewModel @Inject constructor(
    private val billingManager: BillingManager
) : ViewModel() {

    val billingState: StateFlow<BillingState> = billingManager.billingState
    val purchaseState: StateFlow<PurchaseState> = billingManager.purchaseState
    val products: StateFlow<List<ProductDetails>> = billingManager.products

    fun donate(activity: Activity, product: ProductDetails) {
        billingManager.launchPurchaseFlow(activity, product)
    }

    fun resetPurchaseState() {
        billingManager.resetPurchaseState()
    }
}
