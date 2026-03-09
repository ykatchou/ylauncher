package com.ylauncher.ui.drawer

import com.ylauncher.data.repository.AppRepository
import com.ylauncher.data.repository.PrefsRepository
import com.ylauncher.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppDrawerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var appRepository: AppRepository
    private lateinit var prefsRepository: PrefsRepository
    private lateinit var viewModel: AppDrawerViewModel

    @Before
    fun setUp() {
        appRepository = mockk(relaxed = true) {
            every { appList } returns MutableStateFlow(emptyList())
        }
        prefsRepository = mockk(relaxed = true) {
            every { autoShowKeyboard } returns flowOf(true)
            every { hiddenApps } returns flowOf(emptySet())
        }
        viewModel = AppDrawerViewModel(appRepository, prefsRepository)
    }

    // --- updateSearchQuery ---

    @Test
    fun `initial search query is empty`() {
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun `updateSearchQuery updates the search query`() {
        viewModel.updateSearchQuery("browser")
        assertEquals("browser", viewModel.searchQuery.value)
    }

    @Test
    fun `updateSearchQuery can be called multiple times`() {
        viewModel.updateSearchQuery("first")
        viewModel.updateSearchQuery("second")
        assertEquals("second", viewModel.searchQuery.value)
    }

    @Test
    fun `updateSearchQuery can set back to empty`() {
        viewModel.updateSearchQuery("something")
        viewModel.updateSearchQuery("")
        assertEquals("", viewModel.searchQuery.value)
    }

    // --- shouldAutoLaunch ---

    @Test
    fun `shouldAutoLaunch returns false when query is blank`() {
        viewModel.updateSearchQuery("")
        assertFalse(viewModel.shouldAutoLaunch())
    }

    @Test
    fun `shouldAutoLaunch returns false when query is only whitespace`() {
        viewModel.updateSearchQuery("   ")
        assertFalse(viewModel.shouldAutoLaunch())
    }

    @Test
    fun `shouldAutoLaunch returns true for normal query`() {
        viewModel.updateSearchQuery("chrome")
        assertTrue(viewModel.shouldAutoLaunch())
    }

    @Test
    fun `shouldAutoLaunch returns false when query starts with space`() {
        viewModel.updateSearchQuery(" chrome")
        assertFalse(viewModel.shouldAutoLaunch())
    }

    @Test
    fun `shouldAutoLaunch returns false when query starts with bang`() {
        viewModel.updateSearchQuery("!search term")
        assertFalse(viewModel.shouldAutoLaunch())
    }

    @Test
    fun `shouldAutoLaunch returns true when bang is not first character`() {
        viewModel.updateSearchQuery("hello!")
        assertTrue(viewModel.shouldAutoLaunch())
    }

    @Test
    fun `shouldAutoLaunch returns true for single character query`() {
        viewModel.updateSearchQuery("a")
        assertTrue(viewModel.shouldAutoLaunch())
    }
}
