package com.monospace.app.core.billing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ProFeature {
    NOTION_INTEGRATION,
    REMINDERS_SYNC,
    CUSTOM_WALLPAPERS,
    WIDGETS
}

@Singleton
class ProFeatureGate @Inject constructor(
    private val billingManager: BillingManager
) {
    // TODO: remove before release
    private val debugUnlockAll = true

    val isPro: Flow<Boolean> = if (debugUnlockAll)
        kotlinx.coroutines.flow.flowOf(true)
    else
        billingManager.billingState.map { it == BillingState.SUBSCRIBED }

    fun isFeatureEnabled(feature: ProFeature): Flow<Boolean> = isPro
}
