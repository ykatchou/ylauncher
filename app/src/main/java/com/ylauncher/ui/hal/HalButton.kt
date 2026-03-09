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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HalButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
) {
    var isPressed by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "hal_breathing")

    // Slow breathing glow
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isActive) 800 else 2200,
                easing = EaseInOut,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathing_alpha",
    )

    // Outer ring pulse (slower, out-of-phase with breathing)
    val ringPulse by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ring_pulse",
    )

    // Highlight rotation for metallic sheen
    val sheenAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sheen_rotation",
    )

    // Press scale animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.12f else 1f,
        animationSpec = spring(stiffness = 800f),
        label = "press_scale",
    )

    Box(
        modifier = modifier.size(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(56.dp)
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
            val bezelOuterRadius = outerRadius * 0.95f
            val bezelInnerRadius = outerRadius * 0.78f
            val gradientRingRadius = outerRadius * 0.75f
            val innerEyeRadius = outerRadius * 0.42f
            val highlightRadius = outerRadius * 0.10f

            // Layer 0: Soft outer ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF3300).copy(alpha = ringPulse * 0.5f),
                        Color(0xFFFF3300).copy(alpha = ringPulse * 0.2f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = outerRadius * 1.5f,
                ),
                radius = outerRadius * 1.5f,
                center = center,
            )

            // Layer 1: Silver metallic bezel (outer ring)
            // Dark base
            drawCircle(
                color = Color(0xFF3A3A3E),
                radius = outerRadius,
                center = center,
            )

            // Metallic gradient — brushed silver with rotating sheen
            val sheenRad = Math.toRadians(sheenAngle.toDouble())
            val sheenOffsetX = (outerRadius * 0.3f * cos(sheenRad)).toFloat()
            val sheenOffsetY = (outerRadius * 0.3f * sin(sheenRad)).toFloat()
            val sheenCenter = Offset(centerX + sheenOffsetX, centerY + sheenOffsetY)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFD0D0D8).copy(alpha = 0.5f),
                        Color(0xFF8A8A92).copy(alpha = 0.3f),
                        Color(0xFF4A4A52).copy(alpha = 0.1f),
                    ),
                    center = sheenCenter,
                    radius = outerRadius,
                ),
                radius = outerRadius,
                center = center,
            )

            // Silver edge highlight (top)
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE8E8F0).copy(alpha = 0.45f),
                        Color.Transparent,
                    ),
                    start = Offset(centerX, centerY - outerRadius),
                    end = Offset(centerX, centerY),
                ),
                radius = outerRadius,
                center = center,
            )

            // Outer bevel ring — bright silver stroke
            drawCircle(
                color = Color(0xFFB0B0B8).copy(alpha = 0.6f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 1.8.dp.toPx()),
            )

            // Inner bevel — darker edge between bezel and eye cavity
            drawCircle(
                color = Color(0xFF1A1A1E),
                radius = bezelInnerRadius,
                center = center,
            )
            drawCircle(
                color = Color(0xFF555560).copy(alpha = 0.5f),
                radius = bezelInnerRadius,
                center = center,
                style = Stroke(width = 0.8.dp.toPx()),
            )

            // Layer 2: Amber-red gradient ring (the warm glow around the eye)
            val glowColor = if (isActive) Color(0xFFFF2222) else Color(0xFFCC0000)
            val amberColor = Color(0xFFFF6600)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor,
                        amberColor.copy(alpha = 0.7f),
                        Color(0xFF551100).copy(alpha = 0.9f),
                        Color(0xFF1A1A1E),
                    ),
                    center = center,
                    radius = gradientRingRadius,
                ),
                radius = gradientRingRadius,
                center = center,
            )

            // Layer 3: Inner eye — outer glow halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = breathingAlpha * 0.5f),
                        glowColor.copy(alpha = breathingAlpha * 0.15f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = innerEyeRadius * 1.8f,
                ),
                radius = innerEyeRadius * 1.8f,
                center = center,
            )

            // Inner eye — solid core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF4422).copy(alpha = breathingAlpha * 0.9f + 0.1f),
                        glowColor,
                        glowColor.copy(alpha = 0.85f),
                    ),
                    center = center,
                    radius = innerEyeRadius,
                ),
                radius = innerEyeRadius,
                center = center,
            )

            // Layer 4: Lens highlight (off-center, top-left) — crisp specular
            val highlightOffset = Offset(
                centerX - innerEyeRadius * 0.28f,
                centerY - innerEyeRadius * 0.28f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.1f),
                        Color.Transparent,
                    ),
                    center = highlightOffset,
                    radius = highlightRadius * 1.5f,
                ),
                radius = highlightRadius * 1.5f,
                center = highlightOffset,
            )

            // Secondary tiny highlight (bottom-right)
            val secondaryHighlight = Offset(
                centerX + innerEyeRadius * 0.2f,
                centerY + innerEyeRadius * 0.18f,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = highlightRadius * 0.6f,
                center = secondaryHighlight,
            )
        }
    }
}
