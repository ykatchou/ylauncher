package com.ylauncher.ui.hal

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun HalButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
) {
    var isPressed by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "hal_breathing")

    // Idle breathing: glow radius pulses between 0.3 and 0.55 of the total size
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isActive) 1000 else 2500,
                easing = EaseInOut,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathing_alpha",
    )

    // Active state: rotate the gradient
    val gradientRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "gradient_rotation",
    )

    // Press scale animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.15f else 1f,
        animationSpec = spring(stiffness = 800f),
        label = "press_scale",
    )

    Box(
        modifier = modifier.size(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(48.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = { onClick() },
                        onLongPress = { onLongClick() },
                    )
                }
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val center = Offset(centerX, centerY)

            // Scale all radii
            val outerRadius = (size.minDimension / 2) * scale
            val gradientRingRadius = outerRadius * 0.833f  // ~40/48
            val innerEyeRadius = outerRadius * 0.5f        // ~24/48
            val highlightRadius = outerRadius * 0.125f      // ~6/48

            // Layer 1: Outer bezel
            drawCircle(
                color = Color(0xFF2A2A2E),
                radius = outerRadius,
                center = center,
            )
            drawCircle(
                color = Color(0xFF444444),
                radius = outerRadius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()),
            )

            // Layer 2: Amber-red gradient ring
            val glowColor = if (isActive) Color(0xFFFF2222) else Color(0xFFCC0000)
            val amberColor = Color(0xFFFF6600)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, amberColor.copy(alpha = 0.6f), Color(0xFF441100)),
                    center = center,
                    radius = gradientRingRadius,
                ),
                radius = gradientRingRadius,
                center = center,
            )

            // Layer 3: Inner eye with glow
            drawCircle(
                color = glowColor.copy(alpha = breathingAlpha * 0.3f),
                radius = innerEyeRadius * 1.4f,
                center = center,
            )
            drawCircle(
                color = glowColor,
                radius = innerEyeRadius,
                center = center,
            )

            // Layer 4: Lens highlight (off-center, top-left)
            val highlightOffset = Offset(
                centerX - innerEyeRadius * 0.3f,
                centerY - innerEyeRadius * 0.3f,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = highlightRadius,
                center = highlightOffset,
            )
        }
    }
}
