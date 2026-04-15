package com.monospace.app.feature.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.monospace.app.core.billing.BillingManager
import com.monospace.app.core.billing.BillingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProUpgradeUiState(
    val isPro: Boolean = false,
    val isLoading: Boolean = true,
    val monthlyProduct: ProductDetails? = null,
    val annualProduct: ProductDetails? = null,
    val selectedProductId: String = BillingManager.PRO_ANNUAL_PRODUCT_ID
)

@HiltViewModel
class ProUpgradeViewModel @Inject constructor(
    private val billingManager: BillingManager
) : ViewModel() {

    val uiState: StateFlow<ProUpgradeUiState> = combine(
        billingManager.billingState,
        billingManager.productDetails
    ) { state, products ->
        ProUpgradeUiState(
            isPro = state == BillingState.SUBSCRIBED,
            isLoading = state == BillingState.LOADING,
            monthlyProduct = products.firstOrNull { it.productId == BillingManager.PRO_PRODUCT_ID },
            annualProduct = products.firstOrNull { it.productId == BillingManager.PRO_ANNUAL_PRODUCT_ID }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProUpgradeUiState())

    fun purchase(activity: Activity, product: ProductDetails) {
        billingManager.launchBillingFlow(activity, product)
    }
}
