package com.monospace.app.feature.blocking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.monospace.app.core.service.AppBlockingState
import com.monospace.app.ui.theme.FocusTheme
import kotlinx.coroutines.delay

private const val OUTDOOR_ACCURACY_THRESHOLD_M = 20f
private const val GPS_TIMEOUT_MS = 60_000L
private const val UNLOCK_DURATION_MS = 15 * 60 * 1_000L

private sealed interface TouchGrassState {
    data object RequestingPermission : TouchGrassState
    data object Searching : TouchGrassState
    data object Success : TouchGrassState
    data object PermissionDenied : TouchGrassState
    data object Timeout : TouchGrassState
}

@Composable
fun TouchGrassScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<TouchGrassState>(TouchGrassState.RequestingPermission) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        state = if (granted) TouchGrassState.Searching else TouchGrassState.PermissionDenied
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            state = TouchGrassState.Searching
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    if (state == TouchGrassState.Searching) {
        GpsWatcher(
            onOutdoorDetected = {
                AppBlockingState.grantTemporaryUnlock(UNLOCK_DURATION_MS)
                state = TouchGrassState.Success
            },
            onTimeout = { state = TouchGrassState.Timeout }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FocusTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            when (state) {
                is TouchGrassState.Searching -> SearchingContent()
                is TouchGrassState.Success -> SuccessContent(onDismiss)
                is TouchGrassState.PermissionDenied, is TouchGrassState.Timeout -> ErrorContent(
                    message = if (state is TouchGrassState.Timeout)
                        "Không tìm được GPS ngoài trời.\nHãy thử lại ở khu vực thoáng."
                    else
                        "Cần quyền vị trí để phát hiện bạn đang ở ngoài trời.",
                    onDismiss = onDismiss
                )
                else -> {}
            }
        }
    }
}

@Composable
private fun SearchingContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulse_scale"
    )

    Text(
        "Touch Grass",
        style = FocusTheme.typography.title.copy(
            color = FocusTheme.colors.primary,
            fontSize = 26.sp
        )
    )

    Spacer(Modifier.height(8.dp))

    Text(
        "Bước ra ngoài để được nghỉ 15 phút",
        style = FocusTheme.typography.body.copy(
            color = FocusTheme.colors.secondary,
            textAlign = TextAlign.Center
        )
    )

    Spacer(Modifier.height(48.dp))

    Box(
        modifier = Modifier
            .size(140.dp)
            .scale(pulseScale)
            .background(FocusTheme.colors.primary.copy(alpha = 0.1f), CircleShape)
            .border(2.dp, FocusTheme.colors.primary.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = FocusTheme.colors.primary,
            modifier = Modifier.size(48.dp)
        )
    }

    Spacer(Modifier.height(24.dp))

    Text(
        "Đang tìm GPS ngoài trời...",
        style = FocusTheme.typography.caption.copy(color = FocusTheme.colors.secondary)
    )
}

@Composable
private fun SuccessContent(onDismiss: () -> Unit) {
    Text(
        "Bạn đã ra ngoài!",
        style = FocusTheme.typography.title.copy(
            color = FocusTheme.colors.success,
            fontSize = 26.sp
        )
    )

    Spacer(Modifier.height(12.dp))

    Text(
        "15 phút nghỉ ngơi",
        style = FocusTheme.typography.body.copy(color = FocusTheme.colors.secondary),
        textAlign = TextAlign.Center
    )

    Spacer(Modifier.height(32.dp))

    Box(
        modifier = Modifier
            .size(100.dp)
            .background(FocusTheme.colors.success.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = FocusTheme.colors.success,
            modifier = Modifier.size(48.dp)
        )
    }

    Spacer(Modifier.height(32.dp))

    TextButton(onClick = onDismiss) {
        Text(
            "Đóng",
            style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary)
        )
    }
}

@Composable
private fun ErrorContent(message: String, onDismiss: () -> Unit) {
    Text(
        message,
        style = FocusTheme.typography.body.copy(
            color = FocusTheme.colors.secondary,
            textAlign = TextAlign.Center
        )
    )

    Spacer(Modifier.height(24.dp))

    TextButton(onClick = onDismiss) {
        Text(
            "Quay lại",
            style = FocusTheme.typography.body.copy(color = FocusTheme.colors.primary)
        )
    }
}

@Composable
private fun GpsWatcher(
    onOutdoorDetected: () -> Unit,
    onTimeout: () -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var detected = false

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (!detected && location.hasAccuracy() && location.accuracy <= OUTDOOR_ACCURACY_THRESHOLD_M) {
                    detected = true
                    onOutdoorDetected()
                }
            }
            @Deprecated("Deprecated in API 29")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        runCatching {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1_000L, 0f, listener
            )
        }

        onDispose {
            locationManager.removeUpdates(listener)
        }
    }

    LaunchedEffect(Unit) {
        delay(GPS_TIMEOUT_MS)
        onTimeout()
    }
}
