package com.ylauncher.ui.home

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ylauncher.data.db.FavoriteDao
import com.ylauncher.data.model.AppInfo
import com.ylauncher.data.model.FavoriteApp
import com.ylauncher.data.repository.AppRepository
import com.ylauncher.data.repository.PrefsRepository
import com.ylauncher.util.UsageStatsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val favoriteDao: FavoriteDao,
    private val prefsRepository: PrefsRepository,
) : ViewModel() {

    val favorites = favoriteDao.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val showClock = prefsRepository.showClock
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val swipeLeftEnabled = prefsRepository.swipeLeftEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val swipeRightEnabled = prefsRepository.swipeRightEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val swipeLeftPackage = prefsRepository.swipeLeftPackage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val swipeRightPackage = prefsRepository.swipeRightPackage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val swipeLeftActivity = prefsRepository.swipeLeftActivity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val swipeRightActivity = prefsRepository.swipeRightActivity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val halAssistantPackage = prefsRepository.halAssistantPackage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "com.google.android.apps.googleassistant")

    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen.asStateFlow()

    init {
        viewModelScope.launch {
            appRepository.refreshApps()
            appRepository.registerCallback()
            autoPopulateFavoritesIfNeeded()
        }
    }

    private suspend fun autoPopulateFavoritesIfNeeded() {
        if (favoriteDao.count() > 0) return

        // Try usage stats first (imports your real most-used apps)
        val topApps = UsageStatsHelper.getTopApps(context, appRepository, count = 6)
        if (topApps.isNotEmpty()) {
            val favorites = topApps.mapIndexed { index, app ->
                FavoriteApp(index, app.packageName, app.activityClassName, app.appLabel, app.userHandle.toString())
            }
            favoriteDao.insertAll(favorites)
            prefsRepository.setFirstLaunchDone()
            return
        }

        // Fallback: resolve default apps by intent category
        val defaults = mutableListOf<FavoriteApp>()
        var position = 0

        // Phone
        appRepository.resolveDefaultApp(Intent(Intent.ACTION_DIAL))?.let {
            defaults.add(FavoriteApp(position++, it.packageName, it.activityClassName, "Phone", it.userHandle.toString()))
        }
        // Messages
        appRepository.resolveDefaultApp(Intent(Intent.ACTION_SENDTO).apply { data = android.net.Uri.parse("smsto:") })?.let {
            defaults.add(FavoriteApp(position++, it.packageName, it.activityClassName, "Messages", it.userHandle.toString()))
        }
        // Browser
        appRepository.resolveDefaultApp(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://example.com")))?.let {
            defaults.add(FavoriteApp(position++, it.packageName, it.activityClassName, "Browser", it.userHandle.toString()))
        }
        // Camera
        appRepository.resolveDefaultApp(Intent(MediaStore.ACTION_IMAGE_CAPTURE))?.let {
            defaults.add(FavoriteApp(position++, it.packageName, it.activityClassName, "Camera", it.userHandle.toString()))
        }
        // Gallery
        appRepository.resolveDefaultApp(Intent(Intent.ACTION_VIEW).apply { type = "image/*" })?.let {
            defaults.add(FavoriteApp(position++, it.packageName, it.activityClassName, "Gallery", it.userHandle.toString()))
        }
        // Settings
        appRepository.resolveDefaultApp(Intent(android.provider.Settings.ACTION_SETTINGS))?.let {
            defaults.add(FavoriteApp(position++, it.packageName, it.activityClassName, "Settings", it.userHandle.toString()))
        }

        if (defaults.isNotEmpty()) {
            favoriteDao.insertAll(defaults)
        }
        prefsRepository.setFirstLaunchDone()
    }

    fun reimportFromUsageStats() {
        viewModelScope.launch {
            val topApps = UsageStatsHelper.getTopApps(context, appRepository, count = 6)
            if (topApps.isNotEmpty()) {
                favoriteDao.deleteAll()
                val favorites = topApps.mapIndexed { index, app ->
                    FavoriteApp(index, app.packageName, app.activityClassName, app.appLabel, app.userHandle.toString())
                }
                favoriteDao.insertAll(favorites)
            }
        }
    }

    fun hasUsageStatsPermission(): Boolean = UsageStatsHelper.hasPermission(context)

    fun requestUsageStatsPermission() = UsageStatsHelper.requestPermission(context)

    fun openDrawer() { _isDrawerOpen.value = true }
    fun closeDrawer() { _isDrawerOpen.value = false }

    override fun onCleared() {
        super.onCleared()
        appRepository.unregisterCallback()
    }
}
