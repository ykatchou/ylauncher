package com.ylauncher.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.ylauncher.data.model.AppInfo

@Composable
fun FavoriteItem(
    appInfo: AppInfo?,
    displayName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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

        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
