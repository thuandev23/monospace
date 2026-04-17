package com.monospace.app.feature.blocking

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monospace.app.core.service.AppBlockingState
import com.monospace.app.ui.theme.FocusTheme

private const val REQUIRED_REPS = 5
// Dùng magnitude |a| = sqrt(x²+y²+z²) — hoạt động bất kể điện thoại đặt hướng nào
private const val MAG_UP = 12.5f      // ngực đẩy lên (magnitude > gravity)
private const val MAG_DOWN = 7.5f     // ngực xuống thấp (magnitude < gravity)
private const val UNLOCK_DURATION_MS = 3 * 60 * 1_000L

@Composable
fun PushUpScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var repCount by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var noSensor by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor == null) {
            noSensor = true
            return@DisposableEffect onDispose {}
        }

        var phase = "NEUTRAL"

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (done) return
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val mag = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                when {
                    phase != "DOWN" && mag < MAG_DOWN -> phase = "DOWN"
                    phase == "DOWN" && mag > MAG_UP -> {
                        phase = "NEUTRAL"
                        repCount++
                        if (repCount >= REQUIRED_REPS) {
                            done = true
                            AppBlockingState.grantTemporaryUnlock(UNLOCK_DURATION_MS)
                        }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
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
            when {
                noSensor -> NoSensorContent(onDismiss)
                done -> PushUpSuccessContent(onDismiss)
                else -> PushUpCountingContent(repCount, onDismiss)
            }
        }
    }
}

@Composable
private fun PushUpCountingContent(repCount: Int, onDismiss: () -> Unit) {
    val progress = repCount.toFloat() / REQUIRED_REPS
    val circleScale by animateFloatAsState(
        targetValue = 1f + progress * 0.3f,
        animationSpec = tween(300),
        label = "scale"
    )

    Text(
        "Push-up",
        style = FocusTheme.typography.title.copy(
            color = FocusTheme.colors.primary,
            fontSize = 26.sp
        )
    )

    Spacer(Modifier.height(8.dp))

    Text(
        "Đặt điện thoại lên lưng hoặc sàn dưới ngực.\nLàm $REQUIRED_REPS push-up để mở khóa 3 phút.",
        style = FocusTheme.typography.body.copy(
            color = FocusTheme.colors.secondary,
            textAlign = TextAlign.Center
        )
    )

    Spacer(Modifier.height(48.dp))

    Box(
        modifier = Modifier
            .size(140.dp)
            .scale(circleScale)
            .background(FocusTheme.colors.primary.copy(alpha = 0.1f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$repCount",
            style = FocusTheme.typography.title.copy(
                color = FocusTheme.colors.primary,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }

    Spacer(Modifier.height(16.dp))

    Text(
        "/ $REQUIRED_REPS reps",
        style = FocusTheme.typography.caption.copy(color = FocusTheme.colors.secondary)
    )

    Spacer(Modifier.height(32.dp))

    TextButton(onClick = onDismiss) {
        Text(
            "Huỷ",
            style = FocusTheme.typography.body.copy(color = FocusTheme.colors.secondary)
        )
    }
}

@Composable
private fun PushUpSuccessContent(onDismiss: () -> Unit) {
    Text(
        "Xuất sắc! 💪",
        style = FocusTheme.typography.title.copy(
            color = FocusTheme.colors.success,
            fontSize = 26.sp
        )
    )

    Spacer(Modifier.height(12.dp))

    Text(
        "3 phút nghỉ ngơi",
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
        Text("💪", fontSize = 44.sp)
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
private fun NoSensorContent(onDismiss: () -> Unit) {
    Text(
        "Thiết bị không có cảm biến gia tốc",
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
