package com.ylauncher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClockWidget(
    onClockClick: () -> Unit,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            // Update every 30 seconds (enough for HH:mm display)
            delay(30_000L)
        }
    }

    val date = remember(currentTime / 60_000) { Date(currentTime) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }

    Column(modifier = modifier.padding(horizontal = 24.dp)) {
        Text(
            text = timeFormat.format(date),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.clickable { onClockClick() },
        )
        Text(
            text = dateFormat.format(date),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier
                .padding(top = 2.dp)
                .clickable { onDateClick() },
        )
    }
}
