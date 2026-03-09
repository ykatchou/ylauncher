package com.ylauncher.ui.home

import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.ylauncher.data.model.AppInfo
import com.ylauncher.data.model.AppNotification
import com.ylauncher.ui.theme.HomeTextColor
import com.ylauncher.ui.theme.HomeTextColorDim

private val wallpaperShadow = Shadow(
    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
    offset = Offset(1f, 1f),
    blurRadius = 4f,
)

@Composable
fun FavoriteItem(
    appInfo: AppInfo?,
    displayName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    notification: AppNotification? = null,
) {
    Row(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        appInfo?.icon?.let { drawable ->
            val bitmap = remember(drawable) {
                drawable.toBitmap(width = 44, height = 44).asImageBitmap()
            }
            Image(
                bitmap = bitmap,
                contentDescription = displayName,
                modifier = Modifier.size(44.dp),
            )
        }

        Column {
            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineMedium.copy(shadow = wallpaperShadow),
                color = HomeTextColor,
            )

            if (notification != null) {
                val timeAgo = remember(notification.timestamp) {
                    DateUtils.getRelativeTimeSpanString(
                        notification.timestamp,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE,
                    ).toString()
                }
                val notifText = if (notification.title.isNotBlank() && notification.text.isNotBlank()) {
                    "${notification.title}: ${notification.text}"
                } else {
                    notification.title.ifBlank { notification.text }
                }

                Text(
                    text = "$notifText · $timeAgo",
                    style = MaterialTheme.typography.bodySmall.copy(shadow = wallpaperShadow),
                    color = HomeTextColorDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
