package com.ylauncher

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import com.ylauncher.widget.LauncherWidgetHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appRepository: AppRepository
    @Inject lateinit var prefsRepository: PrefsRepository
    @Inject lateinit var widgetHost: LauncherWidgetHost

    private var pendingWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    private lateinit var widgetConfigLauncher: ActivityResultLauncher<Intent>
    private lateinit var widgetBindLauncher: ActivityResultLauncher<Intent>

    companion object {
        private val _homePressed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val homePressed = _homePressed.asSharedFlow()

        private val _showWidgetPicker = MutableStateFlow(false)
        val showWidgetPicker = _showWidgetPicker.asStateFlow()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        widgetConfigLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val widgetId = result.data?.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                pendingWidgetId
            ) ?: pendingWidgetId
            if (result.resultCode == RESULT_OK && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                saveWidget(widgetId)
            } else {
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    widgetHost.deleteAppWidgetId(widgetId)
                }
            }
            pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        }

        widgetBindLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK && pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                finishWidgetSetup(pendingWidgetId)
            } else {
                if (pendingWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    widgetHost.deleteAppWidgetId(pendingWidgetId)
                    pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
                }
            }
        }

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
                            onRequestWidgetPicker = { _showWidgetPicker.value = true },
                            onWidgetSelected = { provider -> bindAndConfigureWidget(provider) },
                            onWidgetPickerDismiss = { _showWidgetPicker.value = false },
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

    override fun onStart() {
        super.onStart()
        widgetHost.startListening()
    }

    override fun onStop() {
        super.onStop()
        widgetHost.stopListening()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        _homePressed.tryEmit(Unit)
    }

    private fun bindAndConfigureWidget(provider: ComponentName) {
        _showWidgetPicker.value = false
        val widgetId = widgetHost.allocateAppWidgetId()
        val appWidgetManager = AppWidgetManager.getInstance(this)

        if (appWidgetManager.bindAppWidgetIdIfAllowed(widgetId, provider)) {
            finishWidgetSetup(widgetId)
        } else {
            // Need user permission to bind
            pendingWidgetId = widgetId
            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
            }
            widgetBindLauncher.launch(bindIntent)
        }
    }

    private fun finishWidgetSetup(widgetId: Int) {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val providerInfo = appWidgetManager.getAppWidgetInfo(widgetId)

        if (providerInfo?.configure != null) {
            pendingWidgetId = widgetId
            val configIntent = Intent().apply {
                component = providerInfo.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            widgetConfigLauncher.launch(configIntent)
        } else {
            saveWidget(widgetId)
        }
    }

    private fun saveWidget(widgetId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            prefsRepository.addHomeWidgetId(widgetId)
        }
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    }
}
