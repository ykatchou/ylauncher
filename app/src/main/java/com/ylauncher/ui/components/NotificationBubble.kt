package com.ylauncher.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ylauncher.data.model.AppNotification
import com.ylauncher.ui.theme.HomeTextColor
import com.ylauncher.ui.theme.HomeTextColorDim
import kotlinx.coroutines.delay

private val BubbleShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cornerRadius = with(density) { 16.dp.toPx() }
        val tailWidth = with(density) { 14.dp.toPx() }
        val tailHeight = with(density) { 10.dp.toPx() }

        val path = Path().apply {
            // Main rounded rect
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = Rect(0f, 0f, size.width, size.height - tailHeight),
                    radiusX = cornerRadius,
                    radiusY = cornerRadius,
                )
            )
            // Tail pointing bottom-left (towards clock)
            moveTo(tailWidth * 2.5f, size.height - tailHeight)
            lineTo(tailWidth * 0.5f, size.height)
            lineTo(tailWidth * 3.5f, size.height - tailHeight)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun NotificationBubble(
    notifications: List<AppNotification>,
    resolveAppLabel: (String) -> String?,
    onClickNotification: (packageName: String) -> Unit,
    onDismissNotification: (packageName: String) -> Unit,
    modifier: Modifier = Modifier,
    maxItems: Int = 5,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }

    val items = remember(notifications, now) {
        notifications
            .sortedByDescending { it.timestamp }
            .take(maxItems)
            .map { notif ->
                val timeAgo = DateUtils.getRelativeTimeSpanString(
                    notif.timestamp, now,
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE,
                ).toString()
                val content = if (notif.title.isNotBlank() && notif.text.isNotBlank()) {
                    "${notif.title}: ${notif.text}"
                } else {
                    notif.title.ifBlank { notif.text }
                }
                Triple(notif.packageName, content, timeAgo)
            }
    }

    Box(
        modifier = modifier
            .heightIn(max = screenHeight / 5)
            .clip(BubbleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(bottom = 10.dp) // space for the tail
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (items.isEmpty()) {
                Text(
                    text = "No notifications",
                    style = MaterialTheme.typography.bodySmall,
                    color = HomeTextColorDim,
                )
            } else {
                items.forEachIndexed { index, (packageName, content, timeAgo) ->
                    key(packageName) {
                    val appLabel = remember(packageName) { resolveAppLabel(packageName) }
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .height(0.5.dp)
                                .background(Color.White.copy(alpha = 0.15f)),
                        )
                    }
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value != SwipeToDismissBoxValue.Settled) {
                                onDismissNotification(packageName)
                                true
                            } else false
                        },
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {},
                        enableDismissFromStartToEnd = true,
                        enableDismissFromEndToStart = true,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClickNotification(packageName) },
                        ) {
                            Text(
                                text = "${appLabel ?: packageName.substringAfterLast('.')} · $timeAgo",
                                style = MaterialTheme.typography.labelSmall,
                                color = HomeTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = content,
                                style = MaterialTheme.typography.bodySmall,
                                color = HomeTextColorDim,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    }
                }
            }
        }
    }
}
