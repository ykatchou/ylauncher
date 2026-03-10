package com.ylauncher.ui.hal

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

@Composable
fun HalButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    modifier: Modifier = Modifier,
    icon: Drawable? = null,
) {
    var isPressed by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "hal_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_alpha",
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.1f else 1f,
        animationSpec = spring(stiffness = 800f),
        label = "press_scale",
    )

    val glowColor = Color(0xFFFF5500)

    Box(
        modifier = modifier
            .size(52.dp)
            .scale(scale)
            .drawBehind {
                // Subtle ambient glow behind the icon
                drawCircle(
                    color = glowColor.copy(alpha = glowAlpha * 0.4f),
                    radius = size.minDimension / 2 * 1.35f,
                )
                drawCircle(
                    color = glowColor.copy(alpha = glowAlpha * 0.15f),
                    radius = size.minDimension / 2 * 1.6f,
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() },
                    onLongPress = { onLongClick() },
                    onDoubleTap = { onDoubleTap() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            val bitmap = remember(icon) {
                icon.toBitmap(width = 48, height = 48).asImageBitmap()
            }
            Image(
                bitmap = bitmap,
                contentDescription = "Action button",
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
            )
        }
    }
}
