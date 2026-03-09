package com.ylauncher.ui.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ylauncher.data.db.FavoriteDao
import com.ylauncher.data.db.FolderDao
import com.ylauncher.data.model.AppInfo
import com.ylauncher.data.model.FavoriteApp
import com.ylauncher.data.model.Folder
import com.ylauncher.data.model.FolderApp
import com.ylauncher.data.repository.AppRepository
import com.ylauncher.data.repository.PrefsRepository
import com.ylauncher.util.UsageStatsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val favoriteDao: FavoriteDao,
    private val folderDao: FolderDao,
    private val prefsRepository: PrefsRepository,
) : ViewModel() {

    val favorites = favoriteDao.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suggestedApps: StateFlow<List<AppInfo>> = combine(
        favorites,
        appRepository.appList,
    ) { favs, allApps ->
        val favPackages = favs.map { it.packageName }.toSet()
        val topApps = UsageStatsHelper.getTopApps(context, appRepository, count = 12)
        topApps.filter { it.packageName !in favPackages }.take(3)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        appRepository.resolveDefaultApp(Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))?.let {
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

    fun saveFavorites(favorites: List<FavoriteApp>) {
        viewModelScope.launch {
            favoriteDao.deleteAll()
            favoriteDao.insertAll(favorites)
        }
    }

    // Folder operations

    fun createFolder(name: String = "New Folder", emoji: String = "📁") {
        viewModelScope.launch {
            val currentFavs = favoriteDao.getAllFavoritesOnce()
            val nextPosition = (currentFavs.maxOfOrNull { it.position } ?: -1) + 1
            val folderId = folderDao.insertFolder(Folder(name = name, position = nextPosition, iconEmoji = emoji))
            favoriteDao.insertFavorite(
                FavoriteApp(
                    position = nextPosition,
                    packageName = "",
                    displayName = name,
                    folderId = folderId,
                    iconEmoji = emoji,
                )
            )
        }
    }

    fun getFolderApps(folderId: Long): kotlinx.coroutines.flow.Flow<List<FolderApp>> {
        return folderDao.getAppsInFolder(folderId)
    }

    suspend fun getFolderAppsOnce(folderId: Long): List<FolderApp> {
        return folderDao.getAppsInFolderOnce(folderId)
    }

    fun updateFolder(folderId: Long, name: String, emoji: String, apps: List<FolderApp>) {
        viewModelScope.launch {
            // Update folder entity
            val existing = folderDao.getFolderById(folderId) ?: return@launch
            folderDao.updateFolder(existing.copy(name = name, iconEmoji = emoji))

            // Update the favorite entry to keep display in sync
            val favs = favoriteDao.getAllFavoritesOnce()
            val folderFav = favs.find { it.folderId == folderId }
            if (folderFav != null) {
                favoriteDao.updateFavorite(folderFav.copy(displayName = name, iconEmoji = emoji))
            }

            // Replace folder apps
            folderDao.deleteAllAppsInFolder(folderId)
            apps.forEachIndexed { index, app ->
                folderDao.insertFolderApp(app.copy(folderId = folderId, position = index))
            }
        }
    }

    fun addAppToFolder(folderId: Long, app: AppInfo) {
        viewModelScope.launch {
            val existingApps = folderDao.getAppsInFolderOnce(folderId)
            val nextPos = (existingApps.maxOfOrNull { it.position } ?: -1) + 1
            folderDao.insertFolderApp(
                FolderApp(
                    folderId = folderId,
                    packageName = app.packageName,
                    activityClassName = app.activityClassName,
                    displayName = app.appLabel,
                    position = nextPos,
                    userHandleString = app.userHandle.toString(),
                )
            )
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            folderDao.deleteFolder(folderId)
            // Remove the favorite entry
            val favs = favoriteDao.getAllFavoritesOnce()
            val folderFav = favs.find { it.folderId == folderId }
            if (folderFav != null) {
                favoriteDao.deleteFavoriteAt(folderFav.position)
            }
        }
    }

    fun getAllFolders(): kotlinx.coroutines.flow.Flow<List<Folder>> {
        return folderDao.getAllFolders()
    }

    fun moveFavoriteToFolder(favorite: FavoriteApp, folderId: Long) {
        viewModelScope.launch {
            // Add the app into the folder
            val existingApps = folderDao.getAppsInFolderOnce(folderId)
            val nextPos = (existingApps.maxOfOrNull { it.position } ?: -1) + 1
            folderDao.insertFolderApp(
                FolderApp(
                    folderId = folderId,
                    packageName = favorite.packageName,
                    activityClassName = favorite.activityClassName,
                    displayName = favorite.displayName,
                    position = nextPos,
                    userHandleString = favorite.userHandleString,
                )
            )
            // Remove it from favorites
            favoriteDao.deleteFavoriteAt(favorite.position)
        }
    }

    override fun onCleared() {
        super.onCleared()
        appRepository.unregisterCallback()
    }
}
