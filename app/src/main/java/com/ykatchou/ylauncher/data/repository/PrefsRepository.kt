package com.ykatchou.ylauncher.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * All home-screen display/swipe/HAL preferences in one snapshot.
 * Backed by a single dataStore.data map so downstream only needs one
 * collectAsState() instead of thirteen separate subscriptions.
 */
data class HomePrefs(
    val showClock: Boolean = true,
    val showNotifBubble: Boolean = true,
    val showNotifPreview: Boolean = true,
    val showNotifBadge: Boolean = true,
    val showDonation: Boolean = true,
    val firstLaunchTimestamp: Long = 0L,
    val swipeLeftEnabled: Boolean = true,
    val swipeRightEnabled: Boolean = true,
    val swipeLeftPackage: String = "",
    val swipeRightPackage: String = "",
    val swipeLeftActivity: String = "",
    val swipeRightActivity: String = "",
    val halAssistantPackage: String = "",
    val halTapActionRaw: String = "ASSISTANT",
    val halLongPressActionRaw: String = "SETTINGS",
    val halDoubleTapActionRaw: String = "APP_DRAWER",
    val reviewNeverAsk: Boolean = false,
    val reviewSnoozedUntil: Long = 0L,
)

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
        val PANEL_NAMES_LEGACY = stringPreferencesKey("panel_names")
        val PANEL_NAMES_MIGRATED = booleanPreferencesKey("panel_names_migrated")
        val HAL_TAP_ACTION = stringPreferencesKey("hal_tap_action")
        val HAL_LONG_PRESS_ACTION = stringPreferencesKey("hal_long_press_action")
        val HAL_DOUBLE_TAP_ACTION = stringPreferencesKey("hal_double_tap_action")
        val AUTO_LAUNCH_DELAY = intPreferencesKey("auto_launch_delay")
        val HOME_WIDGET_IDS = stringPreferencesKey("home_widget_ids")
        val SHOW_NOTIF_BUBBLE = booleanPreferencesKey("show_notif_bubble")
        val SHOW_NOTIF_PREVIEW = booleanPreferencesKey("show_notif_preview")
        val SHOW_NOTIF_BADGE = booleanPreferencesKey("show_notif_badge")
        val SHOW_DONATION = booleanPreferencesKey("show_donation")
        val FIRST_LAUNCH_TIMESTAMP = longPreferencesKey("first_launch_timestamp")
        val HAS_SEEN_ONBOARDING_TOUR = booleanPreferencesKey("has_seen_onboarding_tour")
        val REVIEW_NEVER_ASK = booleanPreferencesKey("review_never_ask")
        val REVIEW_SNOOZED_UNTIL = longPreferencesKey("review_snoozed_until")
    }

    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { it[FIRST_LAUNCH] ?: true }
    val hasSeenOnboardingTour: Flow<Boolean> = dataStore.data.map { it[HAS_SEEN_ONBOARDING_TOUR] ?: false }
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
    val halAssistantPackage: Flow<String> = dataStore.data.map { it[HAL_ASSISTANT_PACKAGE] ?: "" }

    val hiddenApps: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[HIDDEN_APPS]?.split("|")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    }.distinctUntilChanged()

    val textSizeScale: Flow<Float> = dataStore.data.map { prefs ->
        (prefs[TEXT_SIZE_SCALE] ?: 100) / 100f  // stored as int 80-140, returned as 0.8-1.4
    }

    val leftHandMode: Flow<Boolean> = dataStore.data.map { it[LEFT_HAND_MODE] ?: false }

    val suggestionCount: Flow<Int> = dataStore.data.map { it[SUGGESTION_COUNT] ?: 3 }
    val recentAppsCount: Flow<Int> = dataStore.data.map { it[RECENT_APPS_COUNT] ?: 0 }
    val activePanel: Flow<Long> = dataStore.data.map { (it[ACTIVE_PANEL] ?: 0).toLong() }

    /** Single snapshot of all home-screen prefs — subscribe once instead of 13×. */
    val homePrefs: Flow<HomePrefs> = dataStore.data.map { p ->
        HomePrefs(
            showClock = p[SHOW_CLOCK] ?: true,
            showNotifBubble = p[SHOW_NOTIF_BUBBLE] ?: true,
            showNotifPreview = p[SHOW_NOTIF_PREVIEW] ?: true,
            showNotifBadge = p[SHOW_NOTIF_BADGE] ?: true,
            showDonation = p[SHOW_DONATION] ?: true,
            firstLaunchTimestamp = p[FIRST_LAUNCH_TIMESTAMP] ?: 0L,
            swipeLeftEnabled = p[SWIPE_LEFT_ENABLED] ?: true,
            swipeRightEnabled = p[SWIPE_RIGHT_ENABLED] ?: true,
            swipeLeftPackage = p[SWIPE_LEFT_PACKAGE] ?: "",
            swipeRightPackage = p[SWIPE_RIGHT_PACKAGE] ?: "",
            swipeLeftActivity = p[SWIPE_LEFT_ACTIVITY] ?: "",
            swipeRightActivity = p[SWIPE_RIGHT_ACTIVITY] ?: "",
            halAssistantPackage = p[HAL_ASSISTANT_PACKAGE] ?: "",
            halTapActionRaw = (p[HAL_TAP_ACTION] ?: "ASSISTANT").split(";;").first(),
            halLongPressActionRaw = (p[HAL_LONG_PRESS_ACTION] ?: "SETTINGS").split(";;").first(),
            halDoubleTapActionRaw = (p[HAL_DOUBLE_TAP_ACTION] ?: "APP_DRAWER").split(";;").first(),
            reviewNeverAsk = p[REVIEW_NEVER_ASK] ?: false,
            reviewSnoozedUntil = p[REVIEW_SNOOZED_UNTIL] ?: 0L,
        )
    }.distinctUntilChanged()

    // Stored as tenths of a second (0–50 = 0.0–5.0s), default 10 = 1.0s
    val autoLaunchDelay: Flow<Float> = dataStore.data.map { (it[AUTO_LAUNCH_DELAY] ?: 10) / 10f }

    val homeWidgetIds: Flow<List<Int>> = dataStore.data.map { prefs ->
        prefs[HOME_WIDGET_IDS]?.split("|")?.mapNotNull { it.toIntOrNull() }?.filter { it > 0 } ?: emptyList()
    }.distinctUntilChanged()

    val showNotifBubble: Flow<Boolean> = dataStore.data.map { it[SHOW_NOTIF_BUBBLE] ?: true }
    val showNotifPreview: Flow<Boolean> = dataStore.data.map { it[SHOW_NOTIF_PREVIEW] ?: true }
    val showNotifBadge: Flow<Boolean> = dataStore.data.map { it[SHOW_NOTIF_BADGE] ?: true }
    val showDonation: Flow<Boolean> = dataStore.data.map { it[SHOW_DONATION] ?: true }
    val firstLaunchTimestamp: Flow<Long> = dataStore.data.map { it[FIRST_LAUNCH_TIMESTAMP] ?: 0L }

    // Global Magic-button config, shared across all panels. Legacy values may still be
    // ";;"-joined per-panel segments from before panels/HAL config were decoupled — take
    // the first segment (the old panel 0's value) as the single global value going forward.
    val halTapAction: Flow<String> = dataStore.data.map { (it[HAL_TAP_ACTION] ?: "ASSISTANT").split(";;").first() }
    val halLongPressAction: Flow<String> = dataStore.data.map { (it[HAL_LONG_PRESS_ACTION] ?: "SETTINGS").split(";;").first() }
    val halDoubleTapAction: Flow<String> = dataStore.data.map { (it[HAL_DOUBLE_TAP_ACTION] ?: "APP_DRAWER").split(";;").first() }

    suspend fun setFirstLaunchDone() {
        dataStore.edit {
            it[FIRST_LAUNCH] = false
            if (it[FIRST_LAUNCH_TIMESTAMP] == null || it[FIRST_LAUNCH_TIMESTAMP] == 0L) {
                it[FIRST_LAUNCH_TIMESTAMP] = System.currentTimeMillis()
            }
        }
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

    suspend fun setActivePanel(panelId: Long) {
        dataStore.edit { it[ACTIVE_PANEL] = panelId.toInt() }
    }

    /** Reads the legacy comma/pipe-joined panel names once, for the one-time Room reconcile pass. Null if never set. */
    suspend fun legacyPanelNamesCsvOnce(): String? = dataStore.data.map { it[PANEL_NAMES_LEGACY] }.first()

    suspend fun legacyPanelNamesMigrated(): Boolean = dataStore.data.map { it[PANEL_NAMES_MIGRATED] ?: false }.first()

    suspend fun markLegacyPanelNamesMigrated() {
        dataStore.edit { it[PANEL_NAMES_MIGRATED] = true }
    }

    suspend fun setHalTapAction(action: String) {
        dataStore.edit { it[HAL_TAP_ACTION] = action }
    }

    suspend fun setHalLongPressAction(action: String) {
        dataStore.edit { it[HAL_LONG_PRESS_ACTION] = action }
    }

    suspend fun setAutoLaunchDelay(tenths: Int) {
        dataStore.edit { it[AUTO_LAUNCH_DELAY] = tenths.coerceIn(0, 50) }
    }

    suspend fun homeWidgetIdsOnce(): List<Int> = homeWidgetIds.first()

    suspend fun addHomeWidgetId(widgetId: Int) {
        dataStore.edit { prefs ->
            val current = prefs[HOME_WIDGET_IDS]?.split("|")?.mapNotNull { it.toIntOrNull() }?.filter { it > 0 } ?: emptyList()
            prefs[HOME_WIDGET_IDS] = (current + widgetId).joinToString("|")
        }
    }

    suspend fun removeHomeWidgetId(widgetId: Int) {
        dataStore.edit { prefs ->
            val current = prefs[HOME_WIDGET_IDS]?.split("|")?.mapNotNull { it.toIntOrNull() }?.filter { it > 0 } ?: emptyList()
            prefs[HOME_WIDGET_IDS] = current.filter { it != widgetId }.joinToString("|")
        }
    }

    suspend fun clearHomeWidgetIds() {
        dataStore.edit { it[HOME_WIDGET_IDS] = "" }
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

    suspend fun setShowDonation(value: Boolean) {
        dataStore.edit { it[SHOW_DONATION] = value }
    }

    suspend fun recordFirstLaunchIfNeeded() {
        dataStore.edit { prefs ->
            if (prefs[FIRST_LAUNCH_TIMESTAMP] == null || prefs[FIRST_LAUNCH_TIMESTAMP] == 0L) {
                prefs[FIRST_LAUNCH_TIMESTAMP] = System.currentTimeMillis()
            }
        }
    }

    suspend fun setHalDoubleTapAction(action: String) {
        dataStore.edit { it[HAL_DOUBLE_TAP_ACTION] = action }
    }

    suspend fun setOnboardingTourSeen() {
        dataStore.edit { it[HAS_SEEN_ONBOARDING_TOUR] = true }
    }

    suspend fun resetOnboardingTour() {
        dataStore.edit { it[HAS_SEEN_ONBOARDING_TOUR] = false }
    }

    suspend fun setReviewNeverAsk(value: Boolean) {
        dataStore.edit { it[REVIEW_NEVER_ASK] = value }
    }

    suspend fun setReviewSnoozedUntil(timestampMs: Long) {
        dataStore.edit { it[REVIEW_SNOOZED_UNTIL] = timestampMs }
    }
}
