package com.ylauncher

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
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appRepository: AppRepository
    @Inject lateinit var prefsRepository: PrefsRepository

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
                        // Disable back on home — we ARE the launcher
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
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
