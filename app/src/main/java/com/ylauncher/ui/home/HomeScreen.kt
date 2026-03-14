package com.ylauncher.ui.home

import android.app.WallpaperManager
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ylauncher.data.model.AppInfo
import com.ylauncher.data.model.FavoriteApp
import com.ylauncher.data.repository.AppRepository
import com.ylauncher.service.NotificationService
import com.ylauncher.service.ScreenLockService
import com.ylauncher.ui.components.AllAppsButton
import com.ylauncher.ui.components.ClockWidget
import com.ylauncher.ui.drawer.AppDrawerScreen
import com.ylauncher.ui.hal.HalAction
import com.ylauncher.ui.hal.HalActionExecutor
import com.ylauncher.ui.hal.HalButton
import com.ylauncher.ui.theme.HomeTextColor
import com.ylauncher.ui.theme.HomeTextColorDim
import com.ylauncher.ui.theme.WallpaperTextShadow
import com.ylauncher.util.AppLauncher
import com.ylauncher.util.expandNotificationDrawer
import com.ylauncher.util.openAppInfo
import com.ylauncher.util.openCameraApp
import com.ylauncher.util.openDialerApp
import com.ylauncher.util.showToast
import com.ylauncher.util.uninstallApp
import kotlin.math.abs

private const val SWIPE_THRESHOLD = 100f

@Composable
fun HomeScreen(
    onNavigateToAbout: () -> Unit,
    onNavigateToSettings: () -> Unit,
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
    val halTapAction by viewModel.halTapAction.collectAsState()
    val halLongPressAction by viewModel.halLongPressAction.collectAsState()
    val halDoubleTapAction by viewModel.halDoubleTapAction.collectAsState()
    val notifications by NotificationService.notifications.collectAsState()
    val activePanel by viewModel.activePanel.collectAsState()
    val panelNames by viewModel.panelNames.collectAsState()

    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    var showEditFavorites by remember { mutableStateOf(false) }
    var showBackgroundMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    val density = LocalDensity.current

    // Folder state
    var openFolderId by remember { mutableStateOf<Long?>(null) }
    var editingFolderId by remember { mutableStateOf<Long?>(null) }
    var addingAppToFolderId by remember { mutableStateOf<Long?>(null) }
    var movingFavorite by remember { mutableStateOf<FavoriteApp?>(null) }
    var movingFavoriteToPanel by remember { mutableStateOf<FavoriteApp?>(null) }
    val allFolders by viewModel.getAllFolders().collectAsState(initial = emptyList())

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
                        onDrag = { _, dragAmount ->
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
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            ScreenLockService.instance?.lockScreen()
                        },
                        onLongPress = { offset ->
                            with(density) {
                                menuOffset = DpOffset(offset.x.toDp(), offset.y.toDp())
                            }
                            showBackgroundMenu = true
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

                // Middle: Favorites list with subtle scrim
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    // Subtle gradient scrim for readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.15f),
                                        Color.Black.copy(alpha = 0.25f),
                                        Color.Black.copy(alpha = 0.15f),
                                        Color.Transparent,
                                    ),
                                )
                            )
                    )
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        favorites.forEach { favorite ->
                            if (favorite.isFolder && favorite.folderId != null) {
                                // Folder item
                                FavoriteItem(
                                    appInfo = null,
                                    displayName = favorite.displayName,
                                    iconEmoji = favorite.iconEmoji,
                                    isFolder = true,
                                    onClick = { openFolderId = favorite.folderId },
                                    onEditFavorites = { showEditFavorites = true },
                                    onEditFolder = { editingFolderId = favorite.folderId },
                                    onMoveToPanel = if (panelNames.size > 1) {
                                        { movingFavoriteToPanel = favorite }
                                    } else null,
                                )
                            } else {
                                // Regular app item
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
                                    notification = notifications[favorite.packageName],
                                    onEditFavorites = { showEditFavorites = true },
                                    onMoveToFolder = if (allFolders.isNotEmpty()) {
                                        { movingFavorite = favorite }
                                    } else null,
                                    onMoveToPanel = if (panelNames.size > 1) {
                                        { movingFavoriteToPanel = favorite }
                                    } else null,
                                    onAppInfo = { context.openAppInfo(favorite.packageName) },
                                    onUninstall = { context.uninstallApp(favorite.packageName) },
                                )
                            }
                        }

                        // App suggestions (most-used non-favorites)
                        val suggestedApps by viewModel.suggestedApps.collectAsState()
                        if (suggestedApps.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            suggestedApps.forEach { app ->
                                FavoriteItem(
                                    appInfo = app,
                                    displayName = app.appLabel,
                                    onClick = {
                                        AppLauncher.launch(context, app.packageName, app.activityClassName, app.userHandle)
                                    },
                                    modifier = Modifier.alpha(0.6f),
                                )
                            }
                        }

                        // Recently used apps
                        val recentApps by viewModel.recentApps.collectAsState()
                        if (recentApps.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            recentApps.forEach { app ->
                                FavoriteItem(
                                    appInfo = app,
                                    displayName = app.appLabel,
                                    onClick = {
                                        AppLauncher.launch(context, app.packageName, app.activityClassName, app.userHandle)
                                    },
                                    modifier = Modifier.alpha(0.4f),
                                )
                            }
                        }
                    }
                }

                // Bottom bar: Panel switcher (left) + HAL button (center) + All Apps (right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Panel radio buttons
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        panelNames.forEachIndexed { index, name ->
                            val isActive = index == activePanel
                            Text(
                                text = if (isActive) "● $name" else "○ $name",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    shadow = WallpaperTextShadow,
                                ),
                                color = if (isActive) HomeTextColor else HomeTextColorDim,
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = null,
                                        indication = null,
                                    ) {
                                        val target = if (index == activePanel) (activePanel + 1) % panelNames.size else index
                                        viewModel.switchPanel(target)
                                    }
                                    .padding(end = 10.dp, top = 4.dp, bottom = 4.dp),
                            )
                        }
                    }

                    Box {
                        // Resolve icon for the tap action
                        val tapIcon = remember(halTapAction) {
                            val decoded = HalAction.decodeApp(halTapAction)
                            if (decoded != null) {
                                try { context.packageManager.getApplicationIcon(decoded.first) } catch (_: Exception) { null }
                            } else when (HalAction.fromKey(halTapAction)) {
                                HalAction.ASSISTANT -> try { context.packageManager.getApplicationIcon("com.google.android.googlequicksearchbox") } catch (_: Exception) { null }
                                HalAction.CAMERA -> try { context.packageManager.getApplicationIcon(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(context.packageManager)?.packageName ?: "") } catch (_: Exception) { null }
                                HalAction.PHONE -> try { context.packageManager.getApplicationIcon(Intent(Intent.ACTION_DIAL).resolveActivity(context.packageManager)?.packageName ?: "") } catch (_: Exception) { null }
                                else -> null
                            }
                        }

                        HalButton(
                            icon = tapIcon,
                            onClick = {
                                HalActionExecutor.execute(
                                    context, halTapAction,
                                    onOpenDrawer = { viewModel.openDrawer() },
                                    onOpenSettings = onNavigateToSettings,
                                    onEditFavorites = { showEditFavorites = true },
                                )
                            },
                            onLongClick = {
                                HalActionExecutor.execute(
                                    context, halLongPressAction,
                                    onOpenDrawer = { viewModel.openDrawer() },
                                    onOpenSettings = onNavigateToSettings,
                                    onEditFavorites = { showEditFavorites = true },
                                )
                            },
                            onDoubleTap = {
                                HalActionExecutor.execute(
                                    context, halDoubleTapAction,
                                    onOpenDrawer = { viewModel.openDrawer() },
                                    onOpenSettings = onNavigateToSettings,
                                    onEditFavorites = { showEditFavorites = true },
                                )
                            },
                        )
                    }

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        AllAppsButton(onClick = { viewModel.openDrawer() })
                    }
                }
            }
        }

        // Background long-press context menu
        DropdownMenu(
            expanded = showBackgroundMenu,
            onDismissRequest = { showBackgroundMenu = false },
            offset = menuOffset,
        ) {
            DropdownMenuItem(
                text = { Text("Edit favorites") },
                onClick = { showBackgroundMenu = false; showEditFavorites = true },
            )
            DropdownMenuItem(
                text = { Text("Add folder") },
                onClick = {
                    showBackgroundMenu = false
                    viewModel.createFolder()
                },
            )
            DropdownMenuItem(
                text = { Text("Change wallpaper") },
                onClick = {
                    showBackgroundMenu = false
                    try {
                        context.startActivity(
                            Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Exception) {
                        try {
                            context.startActivity(
                                Intent(WallpaperManager.ACTION_CROP_AND_SET_WALLPAPER)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                            context.showToast("No wallpaper picker found")
                        }
                    }
                },
            )
            DropdownMenuItem(
                text = { Text("Reimport favorites") },
                onClick = {
                    showBackgroundMenu = false
                    if (viewModel.hasUsageStatsPermission()) {
                        viewModel.reimportFromUsageStats()
                    } else {
                        viewModel.requestUsageStatsPermission()
                    }
                },
            )
            DropdownMenuItem(
                text = { Text("Settings") },
                onClick = { showBackgroundMenu = false; onNavigateToSettings() },
            )
            DropdownMenuItem(
                text = { Text("About") },
                onClick = { showBackgroundMenu = false; onNavigateToAbout() },
            )
        }

        // App drawer overlay
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            AppDrawerScreen(onDismiss = { viewModel.closeDrawer() })
        }

        // Edit favorites overlay
        if (showEditFavorites) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = null,
                        indication = null,
                    ) { showEditFavorites = false },
                contentAlignment = Alignment.Center,
            ) {
                EditFavoritesSheet(
                    favorites = favorites,
                    resolveApp = { appRepository.findAppByPackage(it) },
                    onSave = { reordered ->
                        viewModel.saveFavorites(reordered)
                        showEditFavorites = false
                    },
                    onAddFolder = {
                        viewModel.createFolder()
                        showEditFavorites = false
                    },
                    onDismiss = { showEditFavorites = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp)
                        .clickable(
                            interactionSource = null,
                            indication = null,
                        ) { /* consume clicks on sheet, don't dismiss */ },
                )
            }
        }

        // Folder popup
        if (openFolderId != null) {
            val folderId = openFolderId!!
            val folderApps by viewModel.getFolderApps(folderId).collectAsState(initial = emptyList())
            val folderFav = favorites.find { it.folderId == folderId }
            FolderPopup(
                folderName = folderFav?.displayName ?: "Folder",
                folderEmoji = folderFav?.iconEmoji ?: "📁",
                folderApps = folderApps,
                resolveApp = { appRepository.findAppByPackage(it.packageName) },
                onLaunchApp = { folderApp ->
                    AppLauncher.launch(context, folderApp.packageName, folderApp.activityClassName)
                    openFolderId = null
                },
                onEdit = {
                    openFolderId = null
                    editingFolderId = folderId
                },
                onDismiss = { openFolderId = null },
            )
        }

        // Edit folder dialog
        if (editingFolderId != null) {
            val folderId = editingFolderId!!
            val folderApps by viewModel.getFolderApps(folderId).collectAsState(initial = emptyList())
            val folderFav = favorites.find { it.folderId == folderId }
            EditFolderDialog(
                initialName = folderFav?.displayName ?: "Folder",
                initialEmoji = folderFav?.iconEmoji ?: "📁",
                folderApps = folderApps,
                resolveApp = { appRepository.findAppByPackage(it.packageName) },
                onSave = { name, emoji, apps ->
                    viewModel.updateFolder(folderId, name, emoji, apps)
                    editingFolderId = null
                },
                onDelete = {
                    viewModel.deleteFolder(folderId)
                    editingFolderId = null
                },
                onAddApp = {
                    addingAppToFolderId = folderId
                    editingFolderId = null
                },
                onDismiss = { editingFolderId = null },
            )
        }

        // Drawer in selection mode (for adding app to folder)
        if (addingAppToFolderId != null) {
            val folderId = addingAppToFolderId!!
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                AppDrawerScreen(
                    onDismiss = {
                        addingAppToFolderId = null
                        editingFolderId = folderId
                    },
                    onAppSelected = { app ->
                        viewModel.addAppToFolder(folderId, app)
                        addingAppToFolderId = null
                        editingFolderId = folderId
                    },
                )
            }
        }

        // Folder picker for "Move to folder"
        if (movingFavorite != null) {
            FolderPickerDialog(
                folders = allFolders,
                onFolderSelected = { folder ->
                    viewModel.moveFavoriteToFolder(movingFavorite!!, folder.id)
                    movingFavorite = null
                },
                onDismiss = { movingFavorite = null },
            )
        }

        // Panel picker for "Move to panel"
        if (movingFavoriteToPanel != null) {
            PanelPickerDialog(
                panelNames = panelNames,
                currentPanelId = activePanel,
                onPanelSelected = { targetPanel ->
                    viewModel.moveFavoriteToPanel(movingFavoriteToPanel!!, targetPanel)
                    movingFavoriteToPanel = null
                },
                onDismiss = { movingFavoriteToPanel = null },
            )
        }
    }
}
