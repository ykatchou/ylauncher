package com.ylauncher.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ylauncher.data.model.FavoriteApp

@Composable
fun EditFavoritesSheet(
    favorites: List<FavoriteApp>,
    onSave: (List<FavoriteApp>) -> Unit,
    onAddFolder: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editList = remember(favorites) { favorites.toMutableStateList() }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 6.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Edit Favorites",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = {
                    val reordered = editList.mapIndexed { index, fav ->
                        fav.copy(position = index)
                    }
                    onSave(reordered)
                }) {
                    Text("Save")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(editList, key = { "${it.packageName}_${it.position}_${it.folderId}" }) { favorite ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconButton(
                            onClick = {
                                val idx = editList.indexOf(favorite)
                                if (idx > 0) {
                                    val item = editList.removeAt(idx)
                                    editList.add(idx - 1, item)
                                }
                            },
                            enabled = editList.indexOf(favorite) > 0,
                        ) {
                            Text("▲", style = MaterialTheme.typography.bodyLarge)
                        }

                        IconButton(
                            onClick = {
                                val idx = editList.indexOf(favorite)
                                if (idx < editList.size - 1) {
                                    val item = editList.removeAt(idx)
                                    editList.add(idx + 1, item)
                                }
                            },
                            enabled = editList.indexOf(favorite) < editList.size - 1,
                        ) {
                            Text("▼", style = MaterialTheme.typography.bodyLarge)
                        }

                        // Icon prefix (emoji for folders)
                        if (favorite.isFolder) {
                            Text(
                                text = favorite.iconEmoji ?: "📁",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }

                        Text(
                            text = favorite.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )

                        IconButton(onClick = { editList.remove(favorite) }) {
                            Text("✕", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onAddFolder) {
                    Text("+ Add folder")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    }
}
