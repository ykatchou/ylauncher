package com.ylauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ylauncher.data.repository.AppRepository
import com.ylauncher.data.repository.PrefsRepository
import com.ylauncher.ui.about.AboutScreen
import com.ylauncher.ui.home.HomeScreen
import com.ylauncher.ui.settings.SettingsScreen
import com.ylauncher.ui.theme.YLauncherTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appRepository: AppRepository
    @Inject lateinit var prefsRepository: PrefsRepository

    companion object {
        private val _homePressed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val homePressed = _homePressed.asSharedFlow()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val fontScale by prefsRepository.textSizeScale.collectAsState(initial = 1f)
            YLauncherTheme(fontScale = fontScale) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "home",
                ) {
                    composable("home") {
                        BackHandler { }
                        HomeScreen(
                            onNavigateToAbout = { navController.navigate("about") },
                            onNavigateToSettings = { navController.navigate("settings") },
                            appRepository = appRepository,
                        )
                    }
                    composable("about") {
                        AboutScreen(
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            prefsRepository = prefsRepository,
                            appRepository = appRepository,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Home button pressed while we're already the foreground launcher
        _homePressed.tryEmit(Unit)
    }
}
