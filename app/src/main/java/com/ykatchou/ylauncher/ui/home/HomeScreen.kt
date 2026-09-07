package com.ykatchou.ylauncher.ui.home

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.service.notification.NotificationListenerService.requestRebind
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ykatchou.ylauncher.data.model.AppInfo
import com.ykatchou.ylauncher.data.model.FavoriteApp
import com.ykatchou.ylauncher.data.repository.AppRepository
import com.ykatchou.ylauncher.service.NotificationService
import com.ykatchou.ylauncher.billing.BillingManager
import com.ykatchou.ylauncher.billing.BillingState
import com.ykatchou.ylauncher.ui.components.AllAppsButton
import com.ykatchou.ylauncher.ui.components.AppWidgetContainer
import com.ykatchou.ylauncher.ui.components.ClockWidget
import com.ykatchou.ylauncher.ui.components.NotificationBubble
import com.ykatchou.ylauncher.ui.components.ReviewPromptDialog
import com.ykatchou.ylauncher.ui.components.WidgetPickerDialog
import com.ykatchou.ylauncher.ui.drawer.AppDrawerScreen
import com.ykatchou.ylauncher.ui.hal.HalAction
import com.ykatchou.ylauncher.ui.hal.HalActionExecutor
import com.ykatchou.ylauncher.ui.hal.HalButton
import com.ykatchou.ylauncher.ui.onboarding.OnboardingTourOverlay
import com.ykatchou.ylauncher.ui.theme.HomeTextColor
import com.ykatchou.ylauncher.ui.theme.HomeTextColorDim
import com.ykatchou.ylauncher.ui.theme.WallpaperTextShadow
import com.ykatchou.ylauncher.util.AppLauncher
import com.ykatchou.ylauncher.util.ONE_WEEK_MS
import com.ykatchou.ylauncher.util.expandNotificationDrawer
import com.ykatchou.ylauncher.util.openAppInfo
import com.ykatchou.ylauncher.util.openCameraApp
import com.ykatchou.ylauncher.util.openDialerApp
import com.ykatchou.ylauncher.util.openPlayStoreListing
import com.ykatchou.ylauncher.util.sendFeedbackEmail
import com.ykatchou.ylauncher.util.shouldShowReviewPrompt
import com.ykatchou.ylauncher.util.showToast
import com.ykatchou.ylauncher.util.uninstallApp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.abs
import kotlin.math.ceil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SWIPE_THRESHOLD = 100f
private const val PANEL_SWITCHER_TAP_SLOP = 20f
private const val PANEL_SWITCHER_LONG_PRESS_MS = 500L
// How far a single slide must travel, as a fraction of the screen height, to loop once
// through the whole panel list. The per-panel step is derived from this at gesture time,
// so a lap never costs more than a third of a screen no matter how many panels exist.
private const val PANEL_SWEEP_HEIGHT_FRACTION = 1f / 3f
// Upper bound on that derived step, so a short panel list still flips fast instead of
// spreading one lap across the full sweep budget.
private const val PANEL_MAX_STEP_DISTANCE = 60f

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onNavigateToAbout: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRequestWidgetPicker: () -> Unit,
    onWidgetSelected: (ComponentName) -> Unit,
    onWidgetPickerDismiss: () -> Unit,
    appRepository: AppRepository,
    billingManager: BillingManager,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val favorites by viewModel.favorites.collectAsState()
    val homePrefs by viewModel.homePrefs.collectAsState()
    val isDrawerOpen by viewModel.isDrawerOpen.collectAsState()
    val halTapAction by viewModel.halTapAction.collectAsState()
    val halLongPressAction by viewModel.halLongPressAction.collectAsState()
    val halDoubleTapAction by viewModel.halDoubleTapAction.collectAsState()
    val notifications by NotificationService.notifications.collectAsState()
    val activePanel by viewModel.activePanel.collectAsState()
    val panels by viewModel.enabledPanels.collectAsState()
    val allPanels by viewModel.panels.collectAsState()
    val homeWidgetIds by viewModel.homeWidgetIds.collectAsState()
    val appList by appRepository.appList.collectAsState()
    // Unpack frequently-used prefs as local vals for readability
    val showClock = homePrefs.showClock
    val showNotifBubble = homePrefs.showNotifBubble
    val showNotifPreview = homePrefs.showNotifPreview
    val showNotifBadge = homePrefs.showNotifBadge
    val showDonation = homePrefs.showDonation
    val firstLaunchTimestamp = homePrefs.firstLaunchTimestamp
    val swipeLeftEnabled = homePrefs.swipeLeftEnabled
    val swipeRightEnabled = homePrefs.swipeRightEnabled
    val swipeLeftPackage = homePrefs.swipeLeftPackage
    val swipeRightPackage = homePrefs.swipeRightPackage
    val swipeLeftActivity = homePrefs.swipeLeftActivity
    val swipeRightActivity = homePrefs.swipeRightActivity
    val halAssistantPackage = homePrefs.halAssistantPackage
    val reviewNeverAsk = homePrefs.reviewNeverAsk
    val reviewSnoozedUntil = homePrefs.reviewSnoozedUntil
    val billingState by billingManager.billingState.collectAsState()
    val showCoffeeFab = showDonation && firstLaunchTimestamp > 0L &&
        (System.currentTimeMillis() - firstLaunchTimestamp) >= ONE_WEEK_MS
    val showWidgetPicker by com.ykatchou.ylauncher.MainActivity.showWidgetPicker.collectAsState()
    val hasSeenOnboardingTour by viewModel.hasSeenOnboardingTour.collectAsState()
    val isReviewPromptEligible = shouldShowReviewPrompt(
        hasSeenOnboardingTour = hasSeenOnboardingTour == true,
        reviewNeverAsk = reviewNeverAsk,
        firstLaunchTimestamp = firstLaunchTimestamp,
        reviewSnoozedUntil = reviewSnoozedUntil,
        now = System.currentTimeMillis(),
    )

    // Ensure notification listener is connected and seeded
    LaunchedEffect(Unit) {
        // Request rebind in case the listener was disconnected after reinstall
        try {
            requestRebind(
                ComponentName(context, NotificationService::class.java)
            )
        } catch (_: Exception) { }
        // Also reseed if already connected
        NotificationService.reseed()
    }

    // Prompt for usage stats permission if suggested/recent apps are enabled.
    // Gated on the onboarding tour so it never fires during/alongside it.
    // Holds the action to run once the user accepts the rationale (request permission, or reimport).
    var usageStatsRationaleAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    LaunchedEffect(hasSeenOnboardingTour) {
        if (hasSeenOnboardingTour == true && !viewModel.hasUsageStatsPermission()) {
            usageStatsRationaleAction = { viewModel.requestUsageStatsPermission() }
        }
    }

    var showReviewDialog by remember { mutableStateOf(false) }
    LaunchedEffect(isReviewPromptEligible) {
        if (isReviewPromptEligible) showReviewDialog = true
    }

    // Refresh usage stats every time the home screen resumes (after switching apps, closing app menu, etc.)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAppsIfEmpty()
                viewModel.refreshUsageStats()
                viewModel.autoPopulateFromUsageStatsIfEmpty()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val widgetColumnWidthDp = (screenWidthDp * 0.45f).toInt()

    var showEditFavorites by remember { mutableStateOf(false) }
    var showEditPanels by remember { mutableStateOf(false) }
    var showBackgroundMenu by remember { mutableStateOf(false) }
    var showRemoveAllWidgetsConfirm by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    val density = LocalDensity.current
    val panelSweepDistancePx =
        with(density) { configuration.screenHeightDp.dp.toPx() } * PANEL_SWEEP_HEIGHT_FRACTION
    // Bounds of the notification bubble, so swipe-to-dismiss inside it doesn't also
    // trigger the whole-screen swipe-to-launch-camera/phone gesture below.
    var notifBubbleBounds by remember { mutableStateOf<Rect?>(null) }
    LaunchedEffect(showClock, showNotifBubble) {
        if (!showClock || !showNotifBubble) notifBubbleBounds = null
    }
    // Bounds of the panel switcher row, so its own drag-to-switch/long-press-to-edit
    // gestures don't also trigger the whole-screen swipe-to-launch-camera/phone gesture below.
    var panelSwitcherBounds by remember { mutableStateOf<Rect?>(null) }
    // Center-screen label previewing the panel a drag-to-switch gesture is about to land on.
    var panelPreviewLabel by remember { mutableStateOf<String?>(null) }
    var panelPreviewHideJob by remember { mutableStateOf<Job?>(null) }
    val panelPreviewScope = rememberCoroutineScope()

    // Folder state
    var openFolderId by remember { mutableStateOf<Long?>(null) }
    var editingFolderId by remember { mutableStateOf<Long?>(null) }
    var addingAppToFolderId by remember { mutableStateOf<Long?>(null) }
    var addingAppToFavorites by remember { mutableStateOf(false) }
    var movingFavorite by remember { mutableStateOf<FavoriteApp?>(null) }
    var movingFavoriteToPanel by remember { mutableStateOf<FavoriteApp?>(null) }
    val allFolders by viewModel.getAllFolders().collectAsState(initial = emptyList())

    Box(modifier = Modifier.fillMaxSize()) {
        // Main home content with swipe detection
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(swipeLeftEnabled, swipeRightEnabled) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val bubbleRect = notifBubbleBounds
                        if (bubbleRect != null && bubbleRect.contains(down.position)) {
                            // Let the notification bubble handle its own swipe-to-dismiss/scroll.
                            return@awaitEachGesture
                        }
                        val switcherRect = panelSwitcherBounds
                        if (switcherRect != null && switcherRect.contains(down.position)) {
                            // Let the panel switcher handle its own drag-to-switch/long-press-to-edit.
                            return@awaitEachGesture
                        }
                        var totalDragX = 0f
                        var totalDragY = 0f
                        do {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            event.changes.forEach { change ->
                                totalDragX += change.positionChange().x
                                totalDragY += change.positionChange().y
                            }
                        } while (event.changes.any { it.pressed })

                        val absX = abs(totalDragX)
                        val absY = abs(totalDragY)

                        if (absX > absY && absX > SWIPE_THRESHOLD) {
                            // Horizontal swipe
                            if (totalDragX > 0 && swipeRightEnabled) {
                                // Swipe right → Phone (or configured app)
                                if (swipeRightPackage.isNotBlank()) {
                                    if (!AppLauncher.launch(context, swipeRightPackage, swipeRightActivity.ifBlank { null })) {
                                        context.showToast("App not found")
                                    }
                                } else {
                                    context.openDialerApp()
                                }
                            } else if (totalDragX < 0 && swipeLeftEnabled) {
                                // Swipe left → Camera (or configured app)
                                if (swipeLeftPackage.isNotBlank()) {
                                    if (!AppLauncher.launch(context, swipeLeftPackage, swipeLeftActivity.ifBlank { null })) {
                                        context.showToast("App not found")
                                    }
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
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
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
                    .padding(vertical = if (isLandscape) 8.dp else 48.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top: Clock + notification bubble
                if (showClock) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isLandscape) Modifier.heightIn(max = 60.dp) else Modifier)
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.Top,
                    ) {
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
                        if (showNotifBubble) {
                            NotificationBubble(
                                notifications = notifications.values.toList(),
                                resolveAppLabel = { pkg -> appRepository.findAppByPackage(pkg)?.appLabel },
                                onClickNotification = { pkg ->
                                    if (!AppLauncher.launch(context, pkg)) context.showToast("App not found")
                                },
                                onDismissNotification = { pkg -> NotificationService.dismiss(pkg) },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 16.dp, top = 4.dp)
                                    .onGloballyPositioned { notifBubbleBounds = it.boundsInRoot() },
                            )
                        }
                    }
                }

                // Middle: Favorites (left) + Widgets (right)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    // Left: Favorites
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
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
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (favorites.isEmpty()) {
                                Text(
                                    text = "No favorites yet\nLong-press here to add apps",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = HomeTextColorDim,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            }
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                            ) {
                                favorites.forEach { favorite ->
                                    if (favorite.isFolder && favorite.folderId != null) {
                                        FavoriteItem(
                                            appInfo = null,
                                            displayName = favorite.displayName,
                                            iconEmoji = favorite.iconEmoji,
                                            isFolder = true,
                                            onClick = { openFolderId = favorite.folderId },
                                            onEditFavorites = { showEditFavorites = true },
                                            onEditFolder = { editingFolderId = favorite.folderId },
                                            onMoveToPanel = if (panels.size > 1) {
                                                { movingFavoriteToPanel = favorite }
                                            } else null,
                                        )
                                    } else {
                                        val appInfo: AppInfo? = remember(favorite.packageName, appList) {
                                            appRepository.findAppByPackage(favorite.packageName)
                                        }
                                        FavoriteItem(
                                            appInfo = appInfo,
                                            displayName = favorite.displayName,
                                            onClick = {
                                                val launched = AppLauncher.launch(
                                                    context,
                                                    favorite.packageName,
                                                    favorite.activityClassName,
                                                )
                                                if (!launched) context.showToast("App not found")
                                            },
                                            notification = notifications[favorite.packageName],
                                            showNotifPreview = showNotifPreview,
                                            showNotifBadge = showNotifBadge,
                                            onDismissNotification = { NotificationService.dismiss(favorite.packageName) },
                                            onEditFavorites = { showEditFavorites = true },
                                            onMoveToFolder = if (allFolders.isNotEmpty()) {
                                                { movingFavorite = favorite }
                                            } else null,
                                            onMoveToPanel = if (panels.size > 1) {
                                                { movingFavoriteToPanel = favorite }
                                            } else null,
                                            onAppInfo = { context.openAppInfo(favorite.packageName) },
                                            onUninstall = { context.uninstallApp(favorite.packageName) },
                                        )
                                    }
                                }

                                if (showCoffeeFab) {
                                    FavoriteItem(
                                        appInfo = null,
                                        displayName = "Buy me a coffee",
                                        iconEmoji = "☕",
                                        isFolder = true,
                                        onClick = {
                                            if (billingState == BillingState.READY) {
                                                (context as? Activity)?.let {
                                                    billingManager.launchTipPurchase(it)
                                                }
                                            } else {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://ko-fi.com/ykatchou"))
                                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                )
                                            }
                                        },
                                        modifier = Modifier.alpha(0.5f),
                                    )
                                }
                            }
                        }
                    }

                    // Right: Widgets + Suggested/Recent apps
                    val suggestedApps by viewModel.suggestedApps.collectAsState()
                    val recentApps by viewModel.recentApps.collectAsState()
                    if (homeWidgetIds.isNotEmpty() || suggestedApps.isNotEmpty() || recentApps.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.45f)
                                .verticalScroll(rememberScrollState())
                                .padding(end = 8.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            homeWidgetIds.forEach { widgetId ->
                                AppWidgetContainer(
                                    widgetId = widgetId,
                                    widgetHost = viewModel.widgetHost,
                                    onLongClick = { onRequestWidgetPicker() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                )
                            }
                            val allSuggestedRecent = suggestedApps + recentApps
                            if (allSuggestedRecent.isNotEmpty()) {
                                if (homeWidgetIds.isNotEmpty()) Spacer(modifier = Modifier.height(12.dp))
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    allSuggestedRecent.forEach { app ->
                                        val isSuggested = app in suggestedApps
                                        val bitmap = remember(app.icon) {
                                            app.icon?.toBitmap(width = 48, height = 48)?.asImageBitmap()
                                        }
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap,
                                                contentDescription = app.appLabel,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .alpha(if (isSuggested) 0.7f else 0.5f)
                                                    .clickable {
                                                        val launched = AppLauncher.launch(context, app.packageName, app.activityClassName, app.userHandle)
                                                        if (!launched) context.showToast("App not found")
                                                    },
                                            )
                                        }
                                    }
                                }
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
                    FlowRow(
                        modifier = Modifier
                            .weight(1f)
                            .onGloballyPositioned { panelSwitcherBounds = it.boundsInRoot() }
                            .pointerInput(panels, activePanel, panelSweepDistancePx) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                    val downTime = down.uptimeMillis
                                    var totalDragX = 0f
                                    var totalDragY = 0f
                                    var lastUptime = downTime
                                    var pendingNextIndex = -1
                                    do {
                                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                        event.changes.forEach { change ->
                                            totalDragX += change.positionChange().x
                                            totalDragY += change.positionChange().y
                                            lastUptime = change.uptimeMillis
                                        }

                                        val liveAbsX = abs(totalDragX)
                                        val liveAbsY = abs(totalDragY)
                                        if (panels.size > 1 && maxOf(liveAbsX, liveAbsY) > SWIPE_THRESHOLD) {
                                            // Slide across panels in the direction of the drag: up/left → previous, down/right → next.
                                            // The further the finger travels, the more panels the single slide crosses.
                                            // Uncapped: the step count wraps around the list, so a long drag keeps
                                            // looping through the panels instead of stopping at either end.
                                            val currentIndex = panels.indexOfFirst { it.id == activePanel }
                                            if (currentIndex >= 0) {
                                                val primaryDelta = if (liveAbsX >= liveAbsY) totalDragX else totalDragY
                                                val direction = if (primaryDelta < 0) -1 else 1
                                                // The gesture arms at SWIPE_THRESHOLD, then every stepDistance past it
                                                // advances one panel, sized so a full lap of the list costs at most
                                                // panelSweepDistancePx. PANEL_MAX_STEP_DISTANCE keeps short lists from
                                                // stretching a lap out to fill that whole budget.
                                                val stepDistance =
                                                    ((panelSweepDistancePx - SWIPE_THRESHOLD) / panels.size)
                                                        .coerceIn(1f, PANEL_MAX_STEP_DISTANCE)
                                                val steps =
                                                    ceil((abs(primaryDelta) - SWIPE_THRESHOLD) / stepDistance)
                                                        .toInt()
                                                        .coerceAtLeast(1)
                                                pendingNextIndex =
                                                    ((currentIndex + direction * steps) % panels.size + panels.size) % panels.size
                                                panelPreviewHideJob?.cancel()
                                                panelPreviewLabel = panels[pendingNextIndex].name
                                            }
                                        } else {
                                            pendingNextIndex = -1
                                            panelPreviewHideJob?.cancel()
                                            panelPreviewLabel = null
                                        }
                                    } while (event.changes.any { it.pressed })

                                    val absX = abs(totalDragX)
                                    val absY = abs(totalDragY)

                                    if (pendingNextIndex >= 0) {
                                        viewModel.switchPanel(panels[pendingNextIndex].id)
                                        panelPreviewHideJob = panelPreviewScope.launch {
                                            delay(600)
                                            panelPreviewLabel = null
                                        }
                                    } else if (
                                        absX < PANEL_SWITCHER_TAP_SLOP &&
                                        absY < PANEL_SWITCHER_TAP_SLOP &&
                                        lastUptime - downTime >= PANEL_SWITCHER_LONG_PRESS_MS
                                    ) {
                                        showEditPanels = true
                                    }
                                }
                            },
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        panels.forEachIndexed { index, panel ->
                            val isActive = panel.id == activePanel
                            Text(
                                text = if (isActive) "● ${panel.name}" else "○ ${panel.name}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    shadow = WallpaperTextShadow,
                                ),
                                color = if (isActive) HomeTextColor else HomeTextColorDim,
                                modifier = Modifier
                                    .clickable(
                                        interactionSource = null,
                                        indication = null,
                                    ) {
                                        val target = if (isActive) {
                                            panels[(index + 1) % panels.size].id
                                        } else {
                                            panel.id
                                        }
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
                                    assistantPackage = halAssistantPackage,
                                    onOpenDrawer = { viewModel.openDrawer() },
                                    onOpenSettings = onNavigateToSettings,
                                    onEditFavorites = { showEditFavorites = true },
                                )
                            },
                            onLongClick = {
                                HalActionExecutor.execute(
                                    context, halLongPressAction,
                                    assistantPackage = halAssistantPackage,
                                    onOpenDrawer = { viewModel.openDrawer() },
                                    onOpenSettings = onNavigateToSettings,
                                    onEditFavorites = { showEditFavorites = true },
                                )
                            },
                            onDoubleTap = {
                                HalActionExecutor.execute(
                                    context, halDoubleTapAction,
                                    assistantPackage = halAssistantPackage,
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
            // Add content
            DropdownMenuItem(
                text = { Text("Add app") },
                leadingIcon = { Text("📱") },
                onClick = { showBackgroundMenu = false; addingAppToFavorites = true },
            )
            DropdownMenuItem(
                text = { Text("Add folder") },
                leadingIcon = { Text("📁") },
                onClick = {
                    showBackgroundMenu = false
                    viewModel.createFolder()
                },
            )
            DropdownMenuItem(
                text = { Text("Add widget") },
                leadingIcon = { Text("🧩") },
                onClick = {
                    showBackgroundMenu = false
                    onRequestWidgetPicker()
                },
            )

            HorizontalDivider()

            // Manage existing content
            DropdownMenuItem(
                text = { Text("Edit favorites") },
                leadingIcon = { Text("✏️") },
                onClick = { showBackgroundMenu = false; showEditFavorites = true },
            )
            DropdownMenuItem(
                text = { Text("Reimport favorites") },
                leadingIcon = { Text("🔄") },
                onClick = {
                    showBackgroundMenu = false
                    if (viewModel.hasUsageStatsPermission()) {
                        viewModel.reimportFromUsageStats()
                    } else {
                        usageStatsRationaleAction = { viewModel.requestUsageStatsPermission() }
                    }
                },
            )
            if (homeWidgetIds.isNotEmpty()) {
                DropdownMenuItem(
                    text = { Text("Remove all widgets") },
                    leadingIcon = { Text("🗑️") },
                    onClick = {
                        showBackgroundMenu = false
                        showRemoveAllWidgetsConfirm = true
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("Change wallpaper") },
                leadingIcon = { Text("🖼️") },
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

            HorizontalDivider()

            // App-level
            DropdownMenuItem(
                text = { Text("Settings") },
                leadingIcon = { Text("⚙️") },
                onClick = { showBackgroundMenu = false; onNavigateToSettings() },
            )
            DropdownMenuItem(
                text = { Text("About") },
                leadingIcon = { Text("ℹ️") },
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
                    onAddApp = {
                        showEditFavorites = false
                        addingAppToFavorites = true
                    },
                    onMoveToFolder = { favorite, folderId ->
                        viewModel.moveFavoriteToFolder(favorite, folderId)
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

        // Manage panels dialog (long-press on the panel switcher)
        if (showEditPanels) {
            EditPanelsDialog(
                panels = allPanels,
                onRename = viewModel::renamePanel,
                onReorder = viewModel::reorderPanels,
                onDelete = viewModel::deletePanel,
                onAdd = viewModel::addPanel,
                onToggleEnabled = viewModel::setPanelEnabled,
                onDismiss = { showEditPanels = false },
            )
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
                    val launched = AppLauncher.launch(context, folderApp.packageName, folderApp.activityClassName)
                    if (!launched) context.showToast("App not found")
                    openFolderId = null
                },
                onDismissNotification = { pkg -> NotificationService.dismiss(pkg) },
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

        // Drawer in selection mode (for adding app to favorites)
        if (addingAppToFavorites) {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                AppDrawerScreen(
                    onDismiss = {
                        addingAppToFavorites = false
                        showEditFavorites = true
                    },
                    onAppSelected = { app ->
                        viewModel.addFavoriteApp(app)
                        addingAppToFavorites = false
                        showEditFavorites = true
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
                panels = panels,
                currentPanelId = activePanel,
                onPanelSelected = { targetPanel ->
                    viewModel.moveFavoriteToPanel(movingFavoriteToPanel!!, targetPanel)
                    movingFavoriteToPanel = null
                },
                onDismiss = { movingFavoriteToPanel = null },
            )
        }

        // Widget picker dialog
        if (showWidgetPicker) {
            WidgetPickerDialog(
                maxWidthDp = widgetColumnWidthDp,
                onWidgetSelected = { provider -> onWidgetSelected(provider) },
                onDismiss = { onWidgetPickerDismiss() },
            )
        }

        // Remove-all-widgets confirmation
        if (showRemoveAllWidgetsConfirm) {
            AlertDialog(
                onDismissRequest = { showRemoveAllWidgetsConfirm = false },
                title = { Text("Remove all widgets?") },
                text = { Text("This removes every widget from your home screen. You can add them back anytime.") },
                confirmButton = {
                    TextButton(onClick = {
                        showRemoveAllWidgetsConfirm = false
                        viewModel.removeAllWidgets()
                    }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveAllWidgetsConfirm = false }) { Text("Cancel") }
                },
            )
        }

        // Usage-stats permission rationale, shown once before the settings intent fires
        usageStatsRationaleAction?.let { action ->
            AlertDialog(
                onDismissRequest = { usageStatsRationaleAction = null },
                title = { Text("Show your recommended apps?") },
                text = { Text("YLauncher can show your last-used and most-recommended apps on the home screen, but needs Usage Access permission to see what you open most.") },
                confirmButton = {
                    TextButton(onClick = {
                        usageStatsRationaleAction = null
                        action()
                    }) { Text("Continue") }
                },
                dismissButton = {
                    TextButton(onClick = { usageStatsRationaleAction = null }) { Text("Not now") }
                },
            )
        }

        // Review prompt — only eligible once the tour has been seen
        if (showReviewDialog) {
            ReviewPromptDialog(
                onRateHigh = {
                    context.openPlayStoreListing()
                    viewModel.onReviewRateHighConfirmed()
                    showReviewDialog = false
                },
                onSendFeedback = { text ->
                    context.sendFeedbackEmail(text)
                    viewModel.onReviewSnoozed()
                    showReviewDialog = false
                },
                onSnooze = {
                    viewModel.onReviewSnoozed()
                    showReviewDialog = false
                },
            )
        }

        // Center-screen preview of the panel a drag-to-switch gesture is about to land on.
        AnimatedVisibility(
            visible = panelPreviewLabel != null,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = panelPreviewLabel.orEmpty(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }

        // First-run onboarding tour — always highest z-order, shown once.
        // hasSeenOnboardingTour == null means DataStore hasn't emitted yet; show nothing until known.
        if (hasSeenOnboardingTour == false) {
            OnboardingTourOverlay(onFinish = viewModel::onOnboardingTourFinished)
        }
    }
}

