package com.ylauncher.ui.home

import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ylauncher.data.model.AppInfo
import com.ylauncher.data.repository.AppRepository
import com.ylauncher.ui.components.AllAppsButton
import com.ylauncher.ui.components.ClockWidget
import com.ylauncher.ui.drawer.AppDrawerScreen
import com.ylauncher.ui.hal.HalButton
import com.ylauncher.util.AppLauncher
import com.ylauncher.util.expandNotificationDrawer
import com.ylauncher.util.openCameraApp
import com.ylauncher.util.openDialerApp
import kotlin.math.abs

private const val SWIPE_THRESHOLD = 100f

@Composable
fun HomeScreen(
    onNavigateToAbout: () -> Unit,
    appRepository: AppRepository,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val favorites by viewModel.favorites.collectAsState()
    val showClock by viewModel.showClock.collectAsState()
    val isDrawerOpen by viewModel.isDrawerOpen.collectAsState()
    val swipeLeftEnabled by viewModel.swipeLeftEnabled.collectAsState()
    val swipeRightEnabled by viewModel.swipeRightEnabled.collectAsState()
    val swipeLeftPackage by viewModel.swipeLeftPackage.collectAsState()
    val swipeRightPackage by viewModel.swipeRightPackage.collectAsState()
    val swipeLeftActivity by viewModel.swipeLeftActivity.collectAsState()
    val swipeRightActivity by viewModel.swipeRightActivity.collectAsState()
    val halAssistantPackage by viewModel.halAssistantPackage.collectAsState()

    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main home content with swipe detection
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(swipeLeftEnabled, swipeRightEnabled) {
                    detectDragGestures(
                        onDragStart = {
                            totalDragX = 0f
                            totalDragY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y
                        },
                        onDragEnd = {
                            val absX = abs(totalDragX)
                            val absY = abs(totalDragY)

                            if (absX > absY && absX > SWIPE_THRESHOLD) {
                                // Horizontal swipe
                                if (totalDragX > 0 && swipeRightEnabled) {
                                    // Swipe right → Phone (or configured app)
                                    if (swipeRightPackage.isNotBlank()) {
                                        AppLauncher.launch(context, swipeRightPackage, swipeRightActivity.ifBlank { null })
                                    } else {
                                        context.openDialerApp()
                                    }
                                } else if (totalDragX < 0 && swipeLeftEnabled) {
                                    // Swipe left → Camera (or configured app)
                                    if (swipeLeftPackage.isNotBlank()) {
                                        AppLauncher.launch(context, swipeLeftPackage, swipeLeftActivity.ifBlank { null })
                                    } else {
                                        context.openCameraApp()
                                    }
                                }
                            } else if (absY > absX && absY > SWIPE_THRESHOLD) {
                                if (totalDragY > 0) {
                                    // Swipe down → notification shade
                                    context.expandNotificationDrawer()
                                } else {
                                    // Swipe up → app drawer
                                    viewModel.openDrawer()
                                }
                            }
                        },
                    )
                },
            color = androidx.compose.ui.graphics.Color.Transparent,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(vertical = 48.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top: Clock
                if (showClock) {
                    ClockWidget(
                        onClockClick = {
                            try {
                                context.startActivity(
                                    Intent(AlarmClock.ACTION_SHOW_ALARMS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (_: Exception) { }
                        },
                        onDateClick = {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW)
                                        .setData(CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build())
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (_: Exception) { }
                        },
                    )
                }

                // Middle: Favorites list
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    favorites.forEach { favorite ->
                        val appInfo: AppInfo? = remember(favorite.packageName, appRepository.appList.value) {
                            appRepository.findAppByPackage(favorite.packageName)
                        }
                        FavoriteItem(
                            appInfo = appInfo,
                            displayName = favorite.displayName,
                            onClick = {
                                AppLauncher.launch(
                                    context,
                                    favorite.packageName,
                                    favorite.activityClassName,
                                )
                            },
                        )
                    }
                }

                // Bottom bar: HAL button (center) + All Apps (right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    HalButton(
                        onClick = {
                            if (halAssistantPackage.isNotBlank()) {
                                AppLauncher.launch(context, halAssistantPackage)
                            }
                        },
                        onLongClick = onNavigateToAbout,
                    )

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        AllAppsButton(onClick = { viewModel.openDrawer() })
                    }
                }
            }
        }

        // App drawer overlay
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            AppDrawerScreen(onDismiss = { viewModel.closeDrawer() })
        }
    }
}
