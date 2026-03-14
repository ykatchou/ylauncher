package com.ylauncher.ui.home

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.ylauncher.data.model.AppInfo
import com.ylauncher.data.model.AppNotification
import com.ylauncher.ui.theme.HomeTextColor
import com.ylauncher.ui.theme.HomeTextColorDim
import com.ylauncher.ui.theme.WallpaperTextShadow
import kotlinx.coroutines.delay

private val wallpaperShadow = WallpaperTextShadow

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoriteItem(
    appInfo: AppInfo?,
    displayName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconEmoji: String? = null,
    isFolder: Boolean = false,
    notification: AppNotification? = null,
    onDismissNotification: (() -> Unit)? = null,
    onEditFavorites: (() -> Unit)? = null,
    onEditFolder: (() -> Unit)? = null,
    onMoveToFolder: (() -> Unit)? = null,
    onMoveToPanel: (() -> Unit)? = null,
    onAppInfo: (() -> Unit)? = null,
    onUninstall: (() -> Unit)? = null,
) {
    var showMenu by remember { mutableStateOf(false) }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }

    val timeAgo = remember(notification?.timestamp, now) {
        notification?.let {
            DateUtils.getRelativeTimeSpanString(
                it.timestamp,
                now,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE,
            ).toString()
        } ?: ""
    }

    val notifDisplayText = if (notification != null) {
        val base = if (notification.title.isNotBlank() && notification.text.isNotBlank()) {
            "${notification.title}: ${notification.text}"
        } else {
            notification.title.ifBlank { notification.text }
        }
        "$base · $timeAgo"
    } else null

    val rowModifier = Modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = onClick,
            onLongClick = { showMenu = true },
        )
        .padding(vertical = 6.dp, horizontal = 24.dp)

    Box(modifier = modifier) {
        if (notification != null && onDismissNotification != null) {
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value != SwipeToDismissBoxValue.Settled) {
                        onDismissNotification()
                        true
                    } else {
                        false
                    }
                },
            )
            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {},
                enableDismissFromStartToEnd = true,
                enableDismissFromEndToStart = true,
            ) {
                Row(
                    modifier = rowModifier,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isFolder && iconEmoji != null) {
                        Text(
                            text = iconEmoji,
                            fontSize = 32.sp,
                            modifier = Modifier.size(44.dp),
                        )
                    } else {
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
                    }
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineMedium.copy(shadow = wallpaperShadow),
                        color = HomeTextColor,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 14.dp),
                    )
                    if (notifDisplayText != null) {
                        Text(
                            text = notifDisplayText,
                            style = MaterialTheme.typography.bodySmall.copy(shadow = wallpaperShadow),
                            color = HomeTextColorDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = rowModifier,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isFolder && iconEmoji != null) {
                    Text(
                        text = iconEmoji,
                        fontSize = 32.sp,
                        modifier = Modifier.size(44.dp),
                    )
                } else {
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
                }
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineMedium.copy(shadow = wallpaperShadow),
                    color = HomeTextColor,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp),
                )
                if (notifDisplayText != null) {
                    Text(
                        text = notifDisplayText,
                        style = MaterialTheme.typography.bodySmall.copy(shadow = wallpaperShadow),
                        color = HomeTextColorDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            if (isFolder && onEditFolder != null) {
                DropdownMenuItem(
                    text = { Text("Edit folder") },
                    onClick = { showMenu = false; onEditFolder() },
                )
            }
            if (!isFolder && onMoveToFolder != null) {
                DropdownMenuItem(
                    text = { Text("Move to folder") },
                    onClick = { showMenu = false; onMoveToFolder() },
                )
            }
            if (onMoveToPanel != null) {
                DropdownMenuItem(
                    text = { Text("Move to panel…") },
                    onClick = { showMenu = false; onMoveToPanel() },
                )
            }
            if (onEditFavorites != null) {
                DropdownMenuItem(
                    text = { Text("Edit favorites") },
                    onClick = { showMenu = false; onEditFavorites() },
                )
            }
            if (!isFolder && onAppInfo != null) {
                DropdownMenuItem(
                    text = { Text("App info") },
                    onClick = { showMenu = false; onAppInfo() },
                )
            }
            if (!isFolder && onUninstall != null) {
                DropdownMenuItem(
                    text = { Text("Uninstall") },
                    onClick = { showMenu = false; onUninstall() },
                )
            }
        }
    }
}
