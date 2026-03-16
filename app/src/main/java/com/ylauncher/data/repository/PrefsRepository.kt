package com.ylauncher.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ylauncher_prefs")

@Singleton
class PrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.dataStore

    companion object Keys {
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val AUTO_SHOW_KEYBOARD = booleanPreferencesKey("auto_show_keyboard")
        val SWIPE_LEFT_ENABLED = booleanPreferencesKey("swipe_left_enabled")
        val SWIPE_RIGHT_ENABLED = booleanPreferencesKey("swipe_right_enabled")
        val SWIPE_LEFT_PACKAGE = stringPreferencesKey("swipe_left_package")
        val SWIPE_LEFT_ACTIVITY = stringPreferencesKey("swipe_left_activity")
        val SWIPE_LEFT_NAME = stringPreferencesKey("swipe_left_name")
        val SWIPE_RIGHT_PACKAGE = stringPreferencesKey("swipe_right_package")
        val SWIPE_RIGHT_ACTIVITY = stringPreferencesKey("swipe_right_activity")
        val SWIPE_RIGHT_NAME = stringPreferencesKey("swipe_right_name")
        val SWIPE_DOWN_ACTION = intPreferencesKey("swipe_down_action")
        val CLOCK_APP_PACKAGE = stringPreferencesKey("clock_app_package")
        val CLOCK_APP_ACTIVITY = stringPreferencesKey("clock_app_activity")
        val SHOW_CLOCK = booleanPreferencesKey("show_clock")
        val TEXT_SIZE_SCALE = intPreferencesKey("text_size_scale")
        val HAL_ASSISTANT_PACKAGE = stringPreferencesKey("hal_assistant_package")
        val HIDDEN_APPS = stringPreferencesKey("hidden_apps")
        val LEFT_HAND_MODE = booleanPreferencesKey("left_hand_mode")
        val SUGGESTION_COUNT = intPreferencesKey("suggestion_count")
        val RECENT_APPS_COUNT = intPreferencesKey("recent_apps_count")
        val ACTIVE_PANEL = intPreferencesKey("active_panel")
        val PANEL_NAMES = stringPreferencesKey("panel_names")
        val HAL_TAP_ACTION = stringPreferencesKey("hal_tap_action")
        val HAL_LONG_PRESS_ACTION = stringPreferencesKey("hal_long_press_action")
        val HAL_DOUBLE_TAP_ACTION = stringPreferencesKey("hal_double_tap_action")
        val AUTO_LAUNCH_DELAY = intPreferencesKey("auto_launch_delay")
        val SHOW_NOTIF_BUBBLE = booleanPreferencesKey("show_notif_bubble")
        val SHOW_NOTIF_PREVIEW = booleanPreferencesKey("show_notif_preview")
        val SHOW_NOTIF_BADGE = booleanPreferencesKey("show_notif_badge")
    }

    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { it[FIRST_LAUNCH] ?: true }
    val autoShowKeyboard: Flow<Boolean> = dataStore.data.map { it[AUTO_SHOW_KEYBOARD] ?: true }
    val showClock: Flow<Boolean> = dataStore.data.map { it[SHOW_CLOCK] ?: true }
    val swipeLeftEnabled: Flow<Boolean> = dataStore.data.map { it[SWIPE_LEFT_ENABLED] ?: true }
    val swipeRightEnabled: Flow<Boolean> = dataStore.data.map { it[SWIPE_RIGHT_ENABLED] ?: true }
    val swipeLeftName: Flow<String> = dataStore.data.map { it[SWIPE_LEFT_NAME] ?: "Camera" }
    val swipeRightName: Flow<String> = dataStore.data.map { it[SWIPE_RIGHT_NAME] ?: "Phone" }
    val swipeLeftPackage: Flow<String> = dataStore.data.map { it[SWIPE_LEFT_PACKAGE] ?: "" }
    val swipeRightPackage: Flow<String> = dataStore.data.map { it[SWIPE_RIGHT_PACKAGE] ?: "" }
    val swipeLeftActivity: Flow<String> = dataStore.data.map { it[SWIPE_LEFT_ACTIVITY] ?: "" }
    val swipeRightActivity: Flow<String> = dataStore.data.map { it[SWIPE_RIGHT_ACTIVITY] ?: "" }
    val halAssistantPackage: Flow<String> = dataStore.data.map { it[HAL_ASSISTANT_PACKAGE] ?: "com.google.android.apps.googleassistant" }

    val hiddenApps: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[HIDDEN_APPS]?.split("|")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    }

    val textSizeScale: Flow<Float> = dataStore.data.map { prefs ->
        (prefs[TEXT_SIZE_SCALE] ?: 100) / 100f  // stored as int 80-140, returned as 0.8-1.4
    }

    val leftHandMode: Flow<Boolean> = dataStore.data.map { it[LEFT_HAND_MODE] ?: false }

    val suggestionCount: Flow<Int> = dataStore.data.map { it[SUGGESTION_COUNT] ?: 3 }
    val recentAppsCount: Flow<Int> = dataStore.data.map { it[RECENT_APPS_COUNT] ?: 0 }
    val activePanel: Flow<Int> = dataStore.data.map { it[ACTIVE_PANEL] ?: 0 }
    val panelNames: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[PANEL_NAMES]?.split("|")?.filter { it.isNotBlank() } ?: listOf("Perso", "Pro")
    }

    // Stored as tenths of a second (0–50 = 0.0–5.0s), default 10 = 1.0s
    val autoLaunchDelay: Flow<Float> = dataStore.data.map { (it[AUTO_LAUNCH_DELAY] ?: 10) / 10f }

    val showNotifBubble: Flow<Boolean> = dataStore.data.map { it[SHOW_NOTIF_BUBBLE] ?: true }
    val showNotifPreview: Flow<Boolean> = dataStore.data.map { it[SHOW_NOTIF_PREVIEW] ?: true }
    val showNotifBadge: Flow<Boolean> = dataStore.data.map { it[SHOW_NOTIF_BADGE] ?: true }

    val halTapAction: Flow<String> = dataStore.data.map { it[HAL_TAP_ACTION] ?: "ASSISTANT;;ASSISTANT" }
    val halLongPressAction: Flow<String> = dataStore.data.map { it[HAL_LONG_PRESS_ACTION] ?: "SETTINGS;;SETTINGS" }
    val halDoubleTapAction: Flow<String> = dataStore.data.map { it[HAL_DOUBLE_TAP_ACTION] ?: "APP_DRAWER;;APP_DRAWER" }

    fun halTapActionForPanel(panelId: Int): Flow<String> = halTapAction.map { it.split(";;").getOrElse(panelId) { "ASSISTANT" } }
    fun halLongPressActionForPanel(panelId: Int): Flow<String> = halLongPressAction.map { it.split(";;").getOrElse(panelId) { "SETTINGS" } }
    fun halDoubleTapActionForPanel(panelId: Int): Flow<String> = halDoubleTapAction.map { it.split(";;").getOrElse(panelId) { "APP_DRAWER" } }

    suspend fun setFirstLaunchDone() {
        dataStore.edit { it[FIRST_LAUNCH] = false }
    }

    suspend fun setAutoShowKeyboard(value: Boolean) {
        dataStore.edit { it[AUTO_SHOW_KEYBOARD] = value }
    }

    suspend fun setShowClock(value: Boolean) {
        dataStore.edit { it[SHOW_CLOCK] = value }
    }

    suspend fun setSwipeLeft(packageName: String, activityName: String, appName: String) {
        dataStore.edit {
            it[SWIPE_LEFT_PACKAGE] = packageName
            it[SWIPE_LEFT_ACTIVITY] = activityName
            it[SWIPE_LEFT_NAME] = appName
        }
    }

    suspend fun setSwipeRight(packageName: String, activityName: String, appName: String) {
        dataStore.edit {
            it[SWIPE_RIGHT_PACKAGE] = packageName
            it[SWIPE_RIGHT_ACTIVITY] = activityName
            it[SWIPE_RIGHT_NAME] = appName
        }
    }

    suspend fun setSwipeLeftEnabled(enabled: Boolean) {
        dataStore.edit { it[SWIPE_LEFT_ENABLED] = enabled }
    }

    suspend fun setSwipeRightEnabled(enabled: Boolean) {
        dataStore.edit { it[SWIPE_RIGHT_ENABLED] = enabled }
    }

    suspend fun setHalAssistantPackage(packageName: String) {
        dataStore.edit { it[HAL_ASSISTANT_PACKAGE] = packageName }
    }

    suspend fun setHiddenApps(apps: Set<String>) {
        dataStore.edit { it[HIDDEN_APPS] = apps.joinToString("|") }
    }

    suspend fun setTextSizeScale(scale: Int) {
        dataStore.edit { it[TEXT_SIZE_SCALE] = scale }
    }

    suspend fun setLeftHandMode(enabled: Boolean) {
        dataStore.edit { it[LEFT_HAND_MODE] = enabled }
    }

    suspend fun setSuggestionCount(count: Int) {
        dataStore.edit { it[SUGGESTION_COUNT] = count }
    }

    suspend fun setRecentAppsCount(count: Int) {
        dataStore.edit { it[RECENT_APPS_COUNT] = count }
    }

    suspend fun setActivePanel(panelId: Int) {
        dataStore.edit { it[ACTIVE_PANEL] = panelId }
    }

    suspend fun setPanelNames(names: List<String>) {
        dataStore.edit { it[PANEL_NAMES] = names.joinToString("|") }
    }

    suspend fun setHalTapAction(panelId: Int, action: String) {
        dataStore.edit { prefs ->
            val parts = (prefs[HAL_TAP_ACTION] ?: "ASSISTANT;;ASSISTANT").split(";;").toMutableList()
            while (parts.size <= panelId) parts.add("ASSISTANT")
            parts[panelId] = action
            prefs[HAL_TAP_ACTION] = parts.joinToString(";;")
        }
    }

    suspend fun setHalLongPressAction(panelId: Int, action: String) {
        dataStore.edit { prefs ->
            val parts = (prefs[HAL_LONG_PRESS_ACTION] ?: "SETTINGS;;SETTINGS").split(";;").toMutableList()
            while (parts.size <= panelId) parts.add("SETTINGS")
            parts[panelId] = action
            prefs[HAL_LONG_PRESS_ACTION] = parts.joinToString(";;")
        }
    }

    suspend fun setAutoLaunchDelay(tenths: Int) {
        dataStore.edit { it[AUTO_LAUNCH_DELAY] = tenths.coerceIn(0, 50) }
    }

    suspend fun setShowNotifBubble(value: Boolean) {
        dataStore.edit { it[SHOW_NOTIF_BUBBLE] = value }
    }

    suspend fun setShowNotifPreview(value: Boolean) {
        dataStore.edit { it[SHOW_NOTIF_PREVIEW] = value }
    }

    suspend fun setShowNotifBadge(value: Boolean) {
        dataStore.edit { it[SHOW_NOTIF_BADGE] = value }
    }

    suspend fun setHalDoubleTapAction(panelId: Int, action: String) {
        dataStore.edit { prefs ->
            val parts = (prefs[HAL_DOUBLE_TAP_ACTION] ?: "APP_DRAWER;;APP_DRAWER").split(";;").toMutableList()
            while (parts.size <= panelId) parts.add("APP_DRAWER")
            parts[panelId] = action
            prefs[HAL_DOUBLE_TAP_ACTION] = parts.joinToString(";;")
        }
    }
}
