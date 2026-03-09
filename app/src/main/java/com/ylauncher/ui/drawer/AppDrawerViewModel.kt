package com.ylauncher.ui.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ylauncher.data.db.FavoriteDao
import com.ylauncher.data.model.AppInfo
import com.ylauncher.data.model.FavoriteApp
import com.ylauncher.data.repository.AppRepository
import com.ylauncher.data.repository.PrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val prefsRepository: PrefsRepository,
    private val favoriteDao: FavoriteDao,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val autoShowKeyboard = prefsRepository.autoShowKeyboard
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val leftHandMode = prefsRepository.leftHandMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hiddenApps = prefsRepository.hiddenApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val filteredApps: StateFlow<List<AppInfo>> = combine(
        appRepository.appList,
        _searchQuery,
        prefsRepository.hiddenApps,
    ) { apps, query, hidden ->
        val visible = apps.filter { app ->
            val key = "${app.packageName}|${app.userHandle}"
            key !in hidden
        }
        if (query.isBlank()) visible
        else appRepository.filterApps(query).filter { app ->
            val key = "${app.packageName}|${app.userHandle}"
            key !in hidden
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun shouldAutoLaunch(): Boolean {
        val query = _searchQuery.value
        return query.isNotBlank() && !query.startsWith(" ") && !query.startsWith("!")
    }

    fun hideApp(app: AppInfo) {
        viewModelScope.launch {
            val current = prefsRepository.hiddenApps.first().toMutableSet()
            current.add("${app.packageName}|${app.userHandle}")
            prefsRepository.setHiddenApps(current)
        }
    }

    fun addToHome(app: AppInfo) {
        viewModelScope.launch {
            val currentFavs = favoriteDao.getAllFavoritesOnce()
            val nextPosition = (currentFavs.maxOfOrNull { it.position } ?: -1) + 1
            favoriteDao.insertFavorite(
                FavoriteApp(
                    position = nextPosition,
                    packageName = app.packageName,
                    activityClassName = app.activityClassName,
                    displayName = app.appLabel,
                    userHandleString = app.userHandle.toString(),
                )
            )
        }
    }
}
