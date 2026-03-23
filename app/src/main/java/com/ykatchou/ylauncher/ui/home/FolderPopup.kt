package com.ykatchou.ylauncher.ui.home

import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.ykatchou.ylauncher.data.model.AppInfo
import com.ykatchou.ylauncher.data.model.FolderApp
import com.ykatchou.ylauncher.service.NotificationService
import kotlinx.coroutines.delay

@Composable
fun FolderPopup(
    folderName: String,
    folderEmoji: String,
    folderApps: List<FolderApp>,
    resolveApp: (FolderApp) -> AppInfo?,
    onLaunchApp: (FolderApp) -> Unit,
    onDismissNotification: (packageName: String) -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val notifications by NotificationService.notifications.collectAsState()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(text = folderEmoji, fontSize = 28.sp)
                    Text(
                        text = folderName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))

                if (folderApps.isEmpty()) {
                    Text(
                        text = "No apps yet — edit to add some",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        items(folderApps, key = { "${it.folderId}|${it.packageName}" }) { folderApp ->
                            val appInfo = remember(folderApp.packageName) { resolveApp(folderApp) }
                            val notification = notifications[folderApp.packageName]

                            val notifDisplayText = if (notification != null) {
                                val timeAgo = remember(notification.timestamp, now) {
                                    DateUtils.getRelativeTimeSpanString(
                                        notification.timestamp,
                                        now,
                                        DateUtils.MINUTE_IN_MILLIS,
                                        DateUtils.FORMAT_ABBREV_RELATIVE,
                                    ).toString()
                                }
                                val base = if (notification.title.isNotBlank() && notification.text.isNotBlank()) {
                                    "${notification.title}: ${notification.text}"
                                } else {
                                    notification.title.ifBlank { notification.text }
                                }
                                "$base · $timeAgo"
                            } else null

                            if (notification != null) {
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value != SwipeToDismissBoxValue.Settled) {
                                            onDismissNotification(folderApp.packageName)
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
                                    FolderAppRow(
                                        folderApp = folderApp,
                                        appInfo = appInfo,
                                        notifDisplayText = notifDisplayText,
                                        onLaunchApp = onLaunchApp,
                                    )
                                }
                            } else {
                                FolderAppRow(
                                    folderApp = folderApp,
                                    appInfo = appInfo,
                                    notifDisplayText = null,
                                    onLaunchApp = onLaunchApp,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Edit ✏\uFE0F")
                }
            }
        }
    }
}

@Composable
private fun FolderAppRow(
    folderApp: FolderApp,
    appInfo: AppInfo?,
    notifDisplayText: String?,
    onLaunchApp: (FolderApp) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLaunchApp(folderApp) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        appInfo?.icon?.let { drawable ->
            val bitmap = remember(drawable) {
                drawable.toBitmap(width = 40, height = 40).asImageBitmap()
            }
            Image(
                bitmap = bitmap,
                contentDescription = folderApp.displayName,
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folderApp.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (notifDisplayText != null) {
                Text(
                    text = notifDisplayText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
