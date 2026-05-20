package com.ykatchou.ylauncher.ui.home

import android.os.UserHandle
import app.cash.turbine.test
import com.ykatchou.ylauncher.data.db.FavoriteDao
import com.ykatchou.ylauncher.data.db.FolderDao
import com.ykatchou.ylauncher.data.model.AppInfo
import com.ykatchou.ylauncher.data.model.FavoriteApp
import com.ykatchou.ylauncher.data.repository.AppRepository
import com.ykatchou.ylauncher.data.repository.HomePrefs
import com.ykatchou.ylauncher.data.repository.PrefsRepository
import com.ykatchou.ylauncher.util.MainDispatcherRule
import com.ykatchou.ylauncher.util.UsageStatsHelper
import com.ykatchou.ylauncher.widget.LauncherWidgetHost
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val appListFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    private val allFavoritesFlow = MutableStateFlow<List<FavoriteApp>>(emptyList())

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        mockkObject(UsageStatsHelper)
        every { UsageStatsHelper.hasPermission(any()) } returns false
        every { UsageStatsHelper.requestPermission(any()) } just runs

        val appRepository = mockk<AppRepository>(relaxed = true) {
            every { appList } returns appListFlow
        }
        val prefsRepository = mockk<PrefsRepository>(relaxed = true) {
            every { activePanel } returns flowOf(0)
            every { panelNames } returns flowOf(listOf("Perso"))
            every { suggestionCount } returns flowOf(3)
            every { recentAppsCount } returns flowOf(3)
            every { homeWidgetIds } returns flowOf(emptyList())
            every { homePrefs } returns flowOf(HomePrefs())
        }
        val favoriteDao = mockk<FavoriteDao>(relaxed = true) {
            every { getAllFavorites() } returns allFavoritesFlow
            coEvery { count() } returns 1 // non-zero → skip auto-populate
        }
        val folderDao = mockk<FolderDao>(relaxed = true) {
            every { getAllFolders() } returns flowOf(emptyList())
        }

        viewModel = HomeViewModel(
            context = mockk(relaxed = true),
            appRepository = appRepository,
            favoriteDao = favoriteDao,
            folderDao = folderDao,
            prefsRepository = prefsRepository,
            widgetHost = mockk<LauncherWidgetHost>(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        unmockkObject(UsageStatsHelper)
    }

    // --- Regression: startup race — icons and right column missing until restart ---
    // NOTE: The full reactive regression test (proving suggestedApps/recentApps
    // re-emit when appList changes on real Android dispatchers) lives in the
    // instrumented test: HomeViewModelStartupRaceTest in androidTest/.

    /**
     * suggestedApps and recentApps start with emptyList() before refreshApps() completes.
     * The reactive re-emission test is in HomeViewModelStartupRaceTest (instrumented).
     */
    @Test
    fun `suggestedApps starts with empty list`() {
        assertEquals(emptyList<AppInfo>(), viewModel.suggestedApps.value)
    }

    @Test
    fun `recentApps starts with empty list`() {
        assertEquals(emptyList<AppInfo>(), viewModel.recentApps.value)
    }

    // --- Favorites basic behaviour ---

    @Test
    fun `favorites starts empty`() = runTest {
        viewModel.favorites.test {
            assertEquals(emptyList<FavoriteApp>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `favorites reflects DB changes on active panel`() = runTest {
        val fav = FavoriteApp(0, "com.test.app", null, "TestApp", panelId = 0)
        viewModel.favorites.test {
            awaitItem() // initial empty
            allFavoritesFlow.value = listOf(fav)
            assertEquals(listOf(fav), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `favorites filters out entries from other panels`() = runTest {
        val panelZero = FavoriteApp(0, "com.a", null, "A", panelId = 0)
        val panelOne = FavoriteApp(1, "com.b", null, "B", panelId = 1)
        viewModel.favorites.test {
            awaitItem() // initial empty
            allFavoritesFlow.value = listOf(panelZero, panelOne)
            val result = awaitItem()
            assertEquals(listOf(panelZero), result)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
