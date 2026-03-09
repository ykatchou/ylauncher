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
}
