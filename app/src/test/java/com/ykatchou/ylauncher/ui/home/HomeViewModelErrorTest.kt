package com.ykatchou.ylauncher.ui.home

import android.util.Log
import app.cash.turbine.test
import com.ykatchou.ylauncher.data.db.FavoriteDao
import com.ykatchou.ylauncher.data.db.FolderDao
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
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
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
class HomeViewModelErrorTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val allFavoritesFlow = MutableStateFlow<List<FavoriteApp>>(emptyList())
    private lateinit var appRepository: AppRepository
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var folderDao: FolderDao
    private lateinit var prefsRepository: PrefsRepository

    @Before
    fun setUp() {
        // android.util.Log is not available in unit tests — stub it out
        mockkStatic(Log::class)
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0

        mockkObject(UsageStatsHelper)
        every { UsageStatsHelper.hasPermission(any()) } returns false
        every { UsageStatsHelper.requestPermission(any()) } just runs

        appRepository = mockk<AppRepository>(relaxed = true) {
            every { appList } returns MutableStateFlow(emptyList())
        }
        prefsRepository = mockk<PrefsRepository>(relaxed = true) {
            every { activePanel } returns flowOf(0)
            every { panelNames } returns flowOf(listOf("Perso"))
            every { suggestionCount } returns flowOf(3)
            every { recentAppsCount } returns flowOf(3)
            every { homeWidgetIds } returns flowOf(emptyList())
            every { homePrefs } returns flowOf(HomePrefs())
        }
        favoriteDao = mockk<FavoriteDao>(relaxed = true) {
            every { getAllFavorites() } returns allFavoritesFlow
            coEvery { count() } returns 1 // non-zero → skip auto-populate
        }
        folderDao = mockk<FolderDao>(relaxed = true) {
            every { getAllFolders() } returns flowOf(emptyList())
        }
    }

    @After
    fun tearDown() {
        unmockkObject(UsageStatsHelper)
        unmockkStatic(Log::class)
    }

    private fun buildViewModel() = HomeViewModel(
        context = mockk(relaxed = true),
        appRepository = appRepository,
        favoriteDao = favoriteDao,
        folderDao = folderDao,
        prefsRepository = prefsRepository,
        widgetHost = mockk<LauncherWidgetHost>(relaxed = true),
    )

    @Test
    fun `ViewModel does not crash when refreshApps throws`() {
        coEvery { appRepository.refreshApps() } throws RuntimeException("disk error")

        val viewModel = buildViewModel()

        assertEquals(emptyList<Any>(), viewModel.suggestedApps.value)
    }

    @Test
    fun `favorites still collects after refreshApps exception`() = runTest {
        coEvery { appRepository.refreshApps() } throws RuntimeException("disk error")

        val viewModel = buildViewModel()

        viewModel.favorites.test {
            assertEquals(emptyList<FavoriteApp>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ViewModel does not crash when autoPopulate throws`() {
        // count() == 0 triggers auto-populate, which will call insertAll — make it throw
        coEvery { favoriteDao.count() } returns 0
        coEvery { favoriteDao.insertAll(any()) } throws RuntimeException("DB locked")

        val viewModel = buildViewModel()

        // ViewModel must still be functional
        assertEquals(emptyList<FavoriteApp>(), viewModel.favorites.value)
    }
}
