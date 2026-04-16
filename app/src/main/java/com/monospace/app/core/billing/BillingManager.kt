package com.monospace.app.core.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class BillingState {
    LOADING, NOT_SUBSCRIBED, SUBSCRIBED, ERROR
}

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"
        const val PRO_PRODUCT_ID = "monospace_pro_monthly"
        const val PRO_ANNUAL_PRODUCT_ID = "monospace_pro_annual"
    }

    private val _billingState = MutableStateFlow(BillingState.LOADING)
    val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    private val _productDetails = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetails: StateFlow<List<ProductDetails>> = _productDetails.asStateFlow()

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    init {
        connect()
    }

    private fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing setup successful")
                    queryPurchases()
                    queryProducts()
                } else {
                    Log.e(TAG, "Billing setup failed: ${result.debugMessage} (Code: ${result.responseCode})")
                    _billingState.value = BillingState.ERROR
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
                _billingState.value = BillingState.ERROR
            }
        })
    }

    private fun queryPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasActiveSub = purchases.any { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        purchase.products.any { it in listOf(PRO_PRODUCT_ID, PRO_ANNUAL_PRODUCT_ID) }
                }
                _billingState.value = if (hasActiveSub) BillingState.SUBSCRIBED else BillingState.NOT_SUBSCRIBED
            } else {
                Log.e(TAG, "Query purchases failed: ${result.debugMessage}")
                _billingState.value = BillingState.NOT_SUBSCRIBED
            }
        }
    }

    private fun queryProducts() {
        val productList = listOf(PRO_PRODUCT_ID, PRO_ANNUAL_PRODUCT_ID).map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        
        billingClient.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Products queried: ${details.size}")
                _productDetails.value = details
            } else {
                Log.e(TAG, "Query products failed: ${result.debugMessage} (Code: ${result.responseCode})")
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            Log.e(TAG, "No offer token found for product: ${productDetails.productId}")
            return
        }

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()
        billingClient.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }
                }
                _billingState.value = BillingState.SUBSCRIBED
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(TAG, "User canceled the purchase")
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${result.debugMessage} (Code: ${result.responseCode})")
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        billingClient.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
        ) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.e(TAG, "Acknowledge purchase failed: ${result.debugMessage}")
            }
        }
    }
}
