package com.ykatchou.ylauncher.ui.home

import android.os.UserHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ykatchou.ylauncher.data.db.FavoriteDao
import com.ykatchou.ylauncher.data.db.FolderDao
import com.ykatchou.ylauncher.data.model.AppInfo
import com.ykatchou.ylauncher.data.model.FavoriteApp
import com.ykatchou.ylauncher.data.repository.AppRepository
import com.ykatchou.ylauncher.data.repository.HomePrefs
import com.ykatchou.ylauncher.data.repository.PrefsRepository
import com.ykatchou.ylauncher.util.UsageStatsHelper
import com.ykatchou.ylauncher.widget.LauncherWidgetHost
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * Instrumented regression tests for the startup race condition fixed in HomeViewModel.
 *
 * Root cause: appRepository.appList was not a source in the suggestedApps combine chain.
 * When refreshApps() completed on startup, those flows never recomputed, leaving the
 * right column empty and favorites appearing centered.
 *
 * Fix: appRepository.appList added as third source in the suggestedApps combine.
 *
 * Test strategy: collect on Dispatchers.Main (= UnconfinedTestDispatcher) so the initial
 * StateFlow replay value is delivered synchronously. Then wait (via polling) until the
 * combine upstream has actually fired once — confirming it has subscribed to appListFlow.
 * Only THEN update appListFlow and assert a second emission arrives.
 *
 * Without the fix, appList is not in the combine, so updating it doesn't re-trigger the
 * combine and no second emission arrives → polling loop times out → test fails.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class HomeViewModelStartupRaceTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val appListFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    private val allFavoritesFlow = MutableStateFlow<List<FavoriteApp>>(emptyList())

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

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
        Dispatchers.resetMain()
        unmockkObject(UsageStatsHelper)
    }

    /**
     * Regression: suggestedApps must re-emit when appList is populated.
     *
     * Without the fix: combine(allFavorites, suggestionCount) — appList NOT wired in.
     * After the fix: combine(allFavorites, suggestionCount, appList) — re-fires on change.
     */
    @Test
    fun suggestedApps_reEmitsWhenAppListPopulated() {
        val fakeApp = AppInfo("TestApp", "com.test.app", null, mockk<UserHandle>())

        every { UsageStatsHelper.hasPermission(any()) } returns true
        every { UsageStatsHelper.getRecentApps(any(), any(), any(), any()) } returns emptyList()
        val callCount = AtomicInteger(0)
        every { UsageStatsHelper.getTopApps(any(), any(), any()) } answers {
            if (callCount.incrementAndGet() > 1) listOf(fakeApp) else emptyList()
        }

        val emissions = CopyOnWriteArrayList<List<AppInfo>>()

        // Subscribe on Dispatchers.Main (= UnconfinedTestDispatcher): initial StateFlow
        // replay value is delivered synchronously to the subscriber before launch returns.
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch { viewModel.suggestedApps.collect { emissions.add(it) } }

        assertTrue("Expected initial emission from replay buffer", emissions.isNotEmpty())
        assertEquals(emptyList<AppInfo>(), emissions.first())

        // The combine's upstream was dispatched to the real IO pool when the subscriber
        // arrived. Wait until it has actually fired once — confirming it has subscribed
        // to appListFlow and is ready to react to updates.
        val upstreamStarted = pollUntil(timeoutMs = 3_000) { callCount.get() > 0 }
        assertTrue("Combine upstream never started (getTopApps never called)", upstreamStarted)

        // Simulate refreshApps() completing.
        appListFlow.value = listOf(fakeApp)

        // Poll for the second emission.
        // Without the fix (appList not in combine), the combine never re-fires and
        // this condition never becomes true → test fails with a clear message.
        val gotSecond = pollUntil(timeoutMs = 5_000) { emissions.size >= 2 }
        assertTrue(
            "suggestedApps did not re-emit after appList changed. " +
                "Fix: add appRepository.appList to the suggestedApps combine chain.",
            gotSecond,
        )
        assertEquals(listOf(fakeApp), emissions.last())

        scope.cancel()
    }

    /**
     * Regression: recentApps re-emits transitively when appList changes.
     * recentApps combines (allFavorites, recentCount, suggestedApps), so the re-execution
     * propagates automatically once appList is wired into suggestedApps.
     */
    @Test
    fun recentApps_reEmitsWhenAppListPopulated() {
        val fakeApp = AppInfo("TestApp", "com.test.app", null, mockk<UserHandle>())

        every { UsageStatsHelper.hasPermission(any()) } returns true
        val topAppsCallCount = AtomicInteger(0)
        every { UsageStatsHelper.getTopApps(any(), any(), any()) } answers {
            if (topAppsCallCount.incrementAndGet() > 1) listOf(fakeApp) else emptyList()
        }
        val recentCallCount = AtomicInteger(0)
        every { UsageStatsHelper.getRecentApps(any(), any(), any(), any()) } answers {
            if (recentCallCount.incrementAndGet() > 1) listOf(fakeApp) else emptyList()
        }

        val emissions = CopyOnWriteArrayList<List<AppInfo>>()
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch { viewModel.recentApps.collect { emissions.add(it) } }

        assertTrue("Expected initial emission from replay buffer", emissions.isNotEmpty())

        // Wait for both suggestedApps and recentApps upstreams to start.
        val upstreamStarted = pollUntil(3_000) { topAppsCallCount.get() > 0 }
        assertTrue("suggestedApps upstream never started", upstreamStarted)

        appListFlow.value = listOf(fakeApp)

        val gotSecond = pollUntil(5_000) { emissions.size >= 2 }
        assertTrue(
            "recentApps did not re-emit after appList changed (propagated via suggestedApps → recentApps).",
            gotSecond,
        )

        scope.cancel()
    }

    // --- helpers ---

    /** Polls [condition] every 50 ms until it returns true or [timeoutMs] elapses. */
    private fun pollUntil(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(50)
        }
        return condition()
    }
}
