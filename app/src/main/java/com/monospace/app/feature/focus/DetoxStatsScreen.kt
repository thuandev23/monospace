package com.monospace.app.feature.focus

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monospace.app.core.domain.model.DetoxBadge
import com.monospace.app.core.domain.model.DetoxStats
import com.monospace.app.ui.theme.FocusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetoxStatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: FocusViewModel = hiltViewModel()
) {
    val stats by viewModel.detoxStats.collectAsState()

    Scaffold(
        containerColor = FocusTheme.colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Detox Stats",
                        style = FocusTheme.typography.title.copy(
                            color = FocusTheme.colors.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = FocusTheme.colors.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FocusTheme.colors.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            StreakHero(stats = stats)
            StatsRow(stats = stats)
            HorizontalDivider(color = FocusTheme.colors.divider, thickness = 0.5.dp)
            BadgesGrid(badges = stats.badges)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StreakHero(stats: DetoxStats) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "${stats.currentStreak}",
            style = FocusTheme.typography.title.copy(
                color = FocusTheme.colors.primary,
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            "ngày streak",
            style = FocusTheme.typography.headline.copy(
                color = FocusTheme.colors.secondary,
                fontSize = 16.sp
            )
        )
        if (stats.currentStreak > 0) {
            Text(
                streakEmoji(stats.currentStreak),
                fontSize = 28.sp
            )
        }
    }
}

@Composable
private fun StatsRow(stats: DetoxStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FocusTheme.colors.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(value = "${stats.longestStreak}", label = "Dài nhất")
        StatDivider()
        StatItem(value = "${stats.totalSessions}", label = "Tổng sessions")
        StatDivider()
        StatItem(
            value = "${stats.badges.count { it.unlocked }}/${stats.badges.size}",
            label = "Badges"
        )
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = FocusTheme.typography.title.copy(
                color = FocusTheme.colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = FocusTheme.typography.caption.copy(
                color = FocusTheme.colors.secondary,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .size(width = 0.5.dp, height = 36.dp)
            .background(FocusTheme.colors.divider)
    )
}

@Composable
private fun BadgesGrid(badges: List<DetoxBadge>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Badges",
            style = FocusTheme.typography.headline.copy(
                color = FocusTheme.colors.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        )

        badges.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { badge ->
                    BadgeCard(badge = badge, modifier = Modifier.weight(1f))
                }
                // fill remaining slots if last row has < 3 items
                repeat(3 - row.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun BadgeCard(badge: DetoxBadge, modifier: Modifier = Modifier) {
    val unlocked = badge.unlocked
    val alpha = if (unlocked) 1f else 0.35f

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (unlocked) FocusTheme.colors.primary.copy(alpha = 0.1f)
                else FocusTheme.colors.surface
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            badge.emoji,
            fontSize = 28.sp,
            modifier = if (!unlocked) Modifier else Modifier
        )
        Text(
            badge.name,
            style = FocusTheme.typography.caption.copy(
                color = FocusTheme.colors.primary.copy(alpha = alpha),
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            ),
            textAlign = TextAlign.Center
        )
        Text(
            badge.description,
            style = FocusTheme.typography.caption.copy(
                color = FocusTheme.colors.secondary.copy(alpha = alpha),
                fontSize = 10.sp
            ),
            textAlign = TextAlign.Center
        )
        if (unlocked) {
            Text(
                "✓ Đã đạt",
                style = FocusTheme.typography.caption.copy(
                    color = FocusTheme.colors.success,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

private fun streakEmoji(streak: Int) = when {
    streak >= 30 -> "🏆"
    streak >= 7  -> "⚡"
    streak >= 3  -> "🔥"
    else         -> "🌱"
}
