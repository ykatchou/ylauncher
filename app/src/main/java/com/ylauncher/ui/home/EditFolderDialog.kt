package com.ylauncher.ui.home

import androidx.compose.foundation.Image
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.ylauncher.data.model.AppInfo
import com.ylauncher.data.model.FolderApp

@Composable
fun EditFolderDialog(
    initialName: String,
    initialEmoji: String,
    folderApps: List<FolderApp>,
    resolveApp: (FolderApp) -> AppInfo?,
    onSave: (name: String, emoji: String, apps: List<FolderApp>) -> Unit,
    onDelete: () -> Unit,
    onAddApp: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var emoji by remember { mutableStateOf(initialEmoji) }
    val editApps = remember(folderApps) { folderApps.toMutableStateList() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Edit Folder",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name + emoji fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { if (it.length <= 2) emoji = it },
                        label = { Text("Icon") },
                        singleLine = true,
                        modifier = Modifier.width(72.dp),
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "APPS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (editApps.isEmpty()) {
                    Text(
                        text = "No apps in this folder yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        items(editApps, key = { "${it.folderId}|${it.packageName}" }) { folderApp ->
                            val appInfo = remember(folderApp.packageName) {
                                resolveApp(folderApp)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                appInfo?.icon?.let { drawable ->
                                    val bitmap = remember(drawable) {
                                        drawable.toBitmap(width = 36, height = 36).asImageBitmap()
                                    }
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = folderApp.displayName,
                                        modifier = Modifier.size(36.dp),
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Text(
                                    text = folderApp.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { editApps.remove(folderApp) }) {
                                    Text("✕", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                // Add app button
                TextButton(
                    onClick = onAddApp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("+ Add app")
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = {
                                val trimmedName = name.trim().ifBlank { "Folder" }
                                val trimmedEmoji = emoji.trim().ifBlank { "📁" }
                                onSave(trimmedName, trimmedEmoji, editApps.toList())
                            },
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}
