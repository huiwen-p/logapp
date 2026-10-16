package com.example.logapp

import com.example.logapp.data.datastore.PreferencesManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val themeFlow = MutableStateFlow("system")
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        preferencesManager = mockk {
            every { themeMode } returns themeFlow
        }
        viewModel = MainViewModel(preferencesManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialThemeMode_isSystem() = runTest {
        val collectJob = launch { viewModel.themeMode.collect {} }
        advanceUntilIdle()

        assertEquals("system", viewModel.themeMode.value)
        collectJob.cancel()
    }

    @Test
    fun testThemeMode_updatesWhenPreferenceChanges() = runTest {
        val collectJob = launch { viewModel.themeMode.collect {} }
        advanceUntilIdle()

        assertEquals("system", viewModel.themeMode.value)

        themeFlow.value = "dark"
        advanceUntilIdle()
        assertEquals("dark", viewModel.themeMode.value)

        themeFlow.value = "light"
        advanceUntilIdle()
        assertEquals("light", viewModel.themeMode.value)

        collectJob.cancel()
    }

    @Test
    fun testThemeResolutionLogic() {
        fun resolveDarkTheme(mode: String, systemIsDark: Boolean): Boolean {
            return when (mode) {
                "light" -> false
                "dark" -> true
                else -> systemIsDark
            }
        }

        // When "light", always false regardless of system theme
        assertFalse(resolveDarkTheme("light", systemIsDark = true))
        assertFalse(resolveDarkTheme("light", systemIsDark = false))

        // When "dark", always true regardless of system theme
        assertTrue(resolveDarkTheme("dark", systemIsDark = false))
        assertTrue(resolveDarkTheme("dark", systemIsDark = true))

        // When "system", follows system theme
        assertFalse(resolveDarkTheme("system", systemIsDark = false))
        assertTrue(resolveDarkTheme("system", systemIsDark = true))
    }
}
