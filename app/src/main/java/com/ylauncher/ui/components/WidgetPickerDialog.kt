package com.ylauncher.ui.components

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap

@Composable
fun WidgetPickerDialog(
    maxWidthDp: Int,
    onWidgetSelected: (ComponentName) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }

    val density = context.resources.displayMetrics.density
    val widgets = remember(maxWidthDp) {
        appWidgetManager.installedProviders
            .filter { (it.minWidth / density).toInt() <= maxWidthDp }
            .sortedBy {
                try {
                    pm.getApplicationLabel(
                        pm.getApplicationInfo(it.provider.packageName, 0)
                    ).toString().lowercase()
                } catch (_: Exception) { it.provider.packageName }
            }
    }

    var query by remember { mutableStateOf("") }
    val filtered = remember(widgets, query) {
        if (query.isBlank()) widgets
        else widgets.filter { info ->
            val appName = try {
                pm.getApplicationLabel(
                    pm.getApplicationInfo(info.provider.packageName, 0)
                ).toString()
            } catch (_: Exception) { "" }
            val label = info.loadLabel(pm)
            appName.contains(query, ignoreCase = true) ||
                label.contains(query, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Choose a widget",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 400.dp),
                ) {
                    items(filtered, key = { it.provider.flattenToString() }) { info ->
                        WidgetPickerItem(
                            info = info,
                            onClick = { onWidgetSelected(info.provider) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun WidgetPickerItem(
    info: AppWidgetProviderInfo,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val pm = context.packageManager

    val appName = remember(info) {
        try {
            pm.getApplicationLabel(
                pm.getApplicationInfo(info.provider.packageName, 0)
            ).toString()
        } catch (_: Exception) { info.provider.packageName.substringAfterLast('.') }
    }

    val widgetLabel = remember(info) { info.loadLabel(pm) }

    val density = context.resources.displayMetrics.density
    val sizeText = remember(info) {
        val minWDp = (info.minWidth / density).toInt()
        val minHDp = (info.minHeight / density).toInt()
        // Convert dp to rough grid cells (each cell ~74dp)
        val cellsW = ((minWDp + 30) / 74).coerceAtLeast(1)
        val cellsH = ((minHDp + 30) / 74).coerceAtLeast(1)
        "${cellsW}x${cellsH} (${minWDp}x${minHDp}dp)"
    }

    val preview = remember(info) {
        try {
            val drawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                info.loadPreviewImage(context, context.resources.displayMetrics.densityDpi)
            } else {
                @Suppress("DEPRECATION")
                if (info.previewImage != 0) {
                    pm.getDrawable(info.provider.packageName, info.previewImage, null)
                } else null
            }
            drawable?.toBitmap(width = 120, height = 80)?.asImageBitmap()
        } catch (_: Exception) { null }
    }

    val appIcon = remember(info) {
        try {
            pm.getApplicationIcon(info.provider.packageName)
                .toBitmap(width = 36, height = 36)
                .asImageBitmap()
        } catch (_: Exception) { null }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Preview or app icon
        if (preview != null) {
            Image(
                bitmap = preview,
                contentDescription = widgetLabel,
                modifier = Modifier.size(width = 80.dp, height = 54.dp),
            )
        } else if (appIcon != null) {
            Image(
                bitmap = appIcon,
                contentDescription = appName,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = widgetLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = appName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = sizeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
        }
    }
}
