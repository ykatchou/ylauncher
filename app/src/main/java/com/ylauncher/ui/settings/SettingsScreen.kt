package com.ylauncher.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ylauncher.data.repository.PrefsRepository
import com.ylauncher.util.openDefaultLauncherSettings
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    prefsRepository: PrefsRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val showClock by prefsRepository.showClock.collectAsState(initial = true)
    val autoShowKeyboard by prefsRepository.autoShowKeyboard.collectAsState(initial = true)
    val leftHandMode by prefsRepository.leftHandMode.collectAsState(initial = false)
    val swipeLeftEnabled by prefsRepository.swipeLeftEnabled.collectAsState(initial = true)
    val swipeRightEnabled by prefsRepository.swipeRightEnabled.collectAsState(initial = true)
    val swipeLeftName by prefsRepository.swipeLeftName.collectAsState(initial = "Camera")
    val swipeRightName by prefsRepository.swipeRightName.collectAsState(initial = "Phone")
    val textSizeScale by prefsRepository.textSizeScale.collectAsState(initial = 1f)
    val suggestionCount by prefsRepository.suggestionCount.collectAsState(initial = 3)
    val recentAppsCount by prefsRepository.recentAppsCount.collectAsState(initial = 0)
    val panelNames by prefsRepository.panelNames.collectAsState(initial = listOf("Perso", "Pro"))

    // Local state for sliders to avoid excessive DataStore writes during drag
    var sliderValue by remember(textSizeScale) { mutableFloatStateOf(textSizeScale) }
    var suggestionSlider by remember(suggestionCount) { mutableFloatStateOf(suggestionCount.toFloat()) }
    var recentSlider by remember(recentAppsCount) { mutableFloatStateOf(recentAppsCount.toFloat()) }
    var panelNamesText by remember(panelNames) { mutableStateOf(panelNames.joinToString(", ")) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader("Display")
            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggle(
                title = "Show clock",
                subtitle = "Display clock & date on home screen",
                checked = showClock,
                onCheckedChange = { scope.launch { prefsRepository.setShowClock(it) } },
            )

            SettingsToggle(
                title = "Left-hand mode",
                subtitle = "Move alphabet sidebar to the left",
                checked = leftHandMode,
                onCheckedChange = { scope.launch { prefsRepository.setLeftHandMode(it) } },
            )

            // Text size slider — writes only on release
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Text size",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "${(sliderValue * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = {
                        scope.launch { prefsRepository.setTextSizeScale((sliderValue * 100).roundToInt()) }
                    },
                    valueRange = 0.8f..1.4f,
                    steps = 5,
                )
            }

            // Suggestion count slider
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Suggested apps",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = if (suggestionSlider.roundToInt() == 0) "Off" else "${suggestionSlider.roundToInt()} most used",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
                Slider(
                    value = suggestionSlider,
                    onValueChange = { suggestionSlider = it },
                    onValueChangeFinished = {
                        scope.launch { prefsRepository.setSuggestionCount(suggestionSlider.roundToInt()) }
                    },
                    valueRange = 0f..8f,
                    steps = 7,
                )
            }

            // Recent apps count slider
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Recent apps",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = if (recentSlider.roundToInt() == 0) "Off" else "${recentSlider.roundToInt()} last used",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
                Slider(
                    value = recentSlider,
                    onValueChange = { recentSlider = it },
                    onValueChangeFinished = {
                        scope.launch { prefsRepository.setRecentAppsCount(recentSlider.roundToInt()) }
                    },
                    valueRange = 0f..8f,
                    steps = 7,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader("Panels")
            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Panel names",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Comma-separated list (e.g. Perso, Pro)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = panelNamesText,
                    onValueChange = { panelNamesText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            val names = panelNamesText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            if (names.isNotEmpty()) {
                                scope.launch { prefsRepository.setPanelNames(names) }
                            }
                        }
                        .padding(vertical = 8.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader("Gestures")
            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggle(
                title = "Swipe left → $swipeLeftName",
                subtitle = "Swipe left on home to open $swipeLeftName",
                checked = swipeLeftEnabled,
                onCheckedChange = { scope.launch { prefsRepository.setSwipeLeftEnabled(it) } },
            )

            SettingsToggle(
                title = "Swipe right → $swipeRightName",
                subtitle = "Swipe right on home to open $swipeRightName",
                checked = swipeRightEnabled,
                onCheckedChange = { scope.launch { prefsRepository.setSwipeRightEnabled(it) } },
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader("Behavior")
            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggle(
                title = "Auto-show keyboard",
                subtitle = "Open keyboard when app drawer opens",
                checked = autoShowKeyboard,
                onCheckedChange = { scope.launch { prefsRepository.setAutoShowKeyboard(it) } },
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader("System")
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Default launcher",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Change which launcher is used as your home screen",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                text = "Open launcher settings →",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { context.openDefaultLauncherSettings() }
                    .padding(vertical = 8.dp),
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "← Back",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
