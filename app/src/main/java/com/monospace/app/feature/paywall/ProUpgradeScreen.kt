package com.monospace.app.feature.paywall

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monospace.app.core.billing.BillingManager
import com.monospace.app.ui.theme.FocusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProUpgradeScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ProUpgradeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedId by remember { mutableStateOf(BillingManager.PRO_ANNUAL_PRODUCT_ID) }

    val proFeatures = listOf(
        "Unlimited task lists & folders",
        "Notion integration",
        "Google Tasks sync",
        "Custom wallpapers",
        "Home screen widgets",
        "Priority support"
    )

    Scaffold(
        containerColor = FocusTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = FocusTheme.colors.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FocusTheme.colors.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                "Monospace Pro",
                style = FocusTheme.typography.title.copy(
                    color = FocusTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
            )
            Text(
                "Unlock everything. Work without limits.",
                style = FocusTheme.typography.body.copy(
                    color = FocusTheme.colors.secondary,
                    fontSize = 15.sp
                ),
                textAlign = TextAlign.Center
            )

            // Already Pro
            if (state.isPro) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(FocusTheme.colors.surface)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(FocusTheme.colors.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, null, tint = FocusTheme.colors.background, modifier = Modifier.size(28.dp))
                        }
                        Text(
                            "You're a Pro member!",
                            style = FocusTheme.typography.headline.copy(
                                color = FocusTheme.colors.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            } else {
                // Features list
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(FocusTheme.colors.surface)
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        proFeatures.forEach { feature ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(FocusTheme.colors.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        null,
                                        tint = FocusTheme.colors.background,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    feature,
                                    style = FocusTheme.typography.body.copy(
                                        color = FocusTheme.colors.primary,
                                        fontSize = 15.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Pricing options
                if (state.isLoading) {
                    CircularProgressIndicator(color = FocusTheme.colors.primary, modifier = Modifier.size(32.dp))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Annual plan
                        PricingOption(
                            productId = BillingManager.PRO_ANNUAL_PRODUCT_ID,
                            label = "Annual",
                            price = state.annualProduct?.subscriptionOfferDetails
                                ?.firstOrNull()?.pricingPhases?.pricingPhaseList
                                ?.firstOrNull()?.formattedPrice ?: "$49.99 / year",
                            badge = "Save 50%",
                            isSelected = selectedId == BillingManager.PRO_ANNUAL_PRODUCT_ID,
                            onSelect = { selectedId = BillingManager.PRO_ANNUAL_PRODUCT_ID }
                        )
                        PricingOption(
                            productId = BillingManager.PRO_PRODUCT_ID,
                            label = "Monthly",
                            price = state.monthlyProduct?.subscriptionOfferDetails
                                ?.firstOrNull()?.pricingPhases?.pricingPhaseList
                                ?.firstOrNull()?.formattedPrice ?: "$8.99 / month",
                            badge = null,
                            isSelected = selectedId == BillingManager.PRO_PRODUCT_ID,
                            onSelect = { selectedId = BillingManager.PRO_PRODUCT_ID }
                        )
                    }

                    // CTA button
                    val selectedProduct = if (selectedId == BillingManager.PRO_ANNUAL_PRODUCT_ID)
                        state.annualProduct else state.monthlyProduct
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(FocusTheme.colors.primary)
                            .clickable {
                                selectedProduct?.let {
                                    viewModel.purchase(context as Activity, it)
                                }
                            }
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Start 7-Day Free Trial",
                            style = FocusTheme.typography.label.copy(
                                color = FocusTheme.colors.background,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }

                    Text(
                        "Cancel anytime. No charges during trial.",
                        style = FocusTheme.typography.caption.copy(
                            color = FocusTheme.colors.secondary,
                            fontSize = 12.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PricingOption(
    productId: String,
    label: String,
    price: String,
    badge: String?,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) FocusTheme.colors.primary else FocusTheme.colors.divider,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onSelect() }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        label,
                        style = FocusTheme.typography.body.copy(
                            color = FocusTheme.colors.primary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                    if (badge != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(FocusTheme.colors.primary)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                badge,
                                style = FocusTheme.typography.caption.copy(
                                    color = FocusTheme.colors.background,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
            Text(
                price,
                style = FocusTheme.typography.body.copy(
                    color = if (isSelected) FocusTheme.colors.primary else FocusTheme.colors.secondary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            )
        }
    }
}
