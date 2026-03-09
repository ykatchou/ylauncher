package com.ylauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ylauncher.data.repository.AppRepository
import com.ylauncher.ui.about.AboutScreen
import com.ylauncher.ui.home.HomeScreen
import com.ylauncher.ui.theme.YLauncherTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appRepository: AppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YLauncherTheme {
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
                            appRepository = appRepository,
                        )
                    }
                    composable("about") {
                        AboutScreen(
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
