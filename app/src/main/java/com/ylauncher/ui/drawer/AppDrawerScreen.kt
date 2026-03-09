package com.ylauncher.ui.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.ylauncher.data.model.AppInfo
import com.ylauncher.ui.components.AlphabetSidebar
import com.ylauncher.util.AppLauncher
import com.ylauncher.util.openAppInfo
import com.ylauncher.util.openSearch
import com.ylauncher.util.uninstallApp
import kotlinx.coroutines.launch

@Composable
fun AppDrawerScreen(
    onDismiss: () -> Unit,
    onAppSelected: ((AppInfo) -> Unit)? = null,
    viewModel: AppDrawerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val autoShowKeyboard by viewModel.autoShowKeyboard.collectAsState()
    val leftHandMode by viewModel.leftHandMode.collectAsState()
    val listState = rememberLazyListState()

    val scope = rememberCoroutineScope()

    BackHandler { onDismiss() }

    // Auto-launch when single match
    LaunchedEffect(filteredApps, searchQuery) {
        if (filteredApps.size == 1 && viewModel.shouldAutoLaunch()) {
            val app = filteredApps[0]
            if (onAppSelected != null) {
                onAppSelected(app)
            } else {
                AppLauncher.launch(context, app.packageName, app.activityClassName, app.userHandle)
            }
            onDismiss()
        }
    }

    // Auto-show keyboard
    LaunchedEffect(autoShowKeyboard) {
        if (autoShowKeyboard) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // Search bar
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = {
                    Text(
                        "Search apps...",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = {
                        if (searchQuery.startsWith("!")) {
                            context.openSearch(searchQuery.removePrefix("!").trim())
                            onDismiss()
                        } else if (filteredApps.isNotEmpty()) {
                            val app = filteredApps[0]
                            if (onAppSelected != null) {
                                onAppSelected(app)
                            } else {
                                AppLauncher.launch(context, app.packageName, app.activityClassName, app.userHandle)
                            }
                            onDismiss()
                        } else {
                            context.openSearch(searchQuery.trim())
                            onDismiss()
                        }
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                ),
            )

            // App list + alphabet sidebar
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = if (leftHandMode) 32.dp else 16.dp,
                            end = if (leftHandMode) 16.dp else 32.dp,
                        ),
                ) {
                    items(
                        items = filteredApps,
                        key = { "${it.packageName}|${it.activityClassName}|${it.userHandle}" },
                    ) { app ->
                        AppDrawerItem(
                            app = app,
                            onClick = {
                                keyboardController?.hide()
                                if (onAppSelected != null) {
                                    onAppSelected(app)
                                } else {
                                    AppLauncher.launch(context, app.packageName, app.activityClassName, app.userHandle)
                                }
                                onDismiss()
                            },
                            onAppInfo = { context.openAppInfo(app.packageName) },
                            onUninstall = { context.uninstallApp(app.packageName) },
                        )
                    }
                }

                // Alphabet quick-access sidebar
                AlphabetSidebar(
                    onLetterSelected = { letter ->
                        keyboardController?.hide()
                        val index = if (letter == '#') {
                            filteredApps.indexOfFirst { !it.appLabel.first().isLetter() }
                        } else {
                            filteredApps.indexOfFirst {
                                it.appLabel.firstOrNull()?.uppercaseChar() == letter
                            }
                        }
                        if (index >= 0) {
                            scope.launch { listState.animateScrollToItem(index) }
                        }
                    },
                    modifier = Modifier
                        .align(if (leftHandMode) Alignment.CenterStart else Alignment.CenterEnd)
                        .padding(
                            start = if (leftHandMode) 2.dp else 0.dp,
                            end = if (leftHandMode) 0.dp else 2.dp,
                        ),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerItem(
    app: AppInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAppInfo: (() -> Unit)? = null,
    onUninstall: (() -> Unit)? = null,
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true },
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // App icon
        app.icon?.let { drawable ->
            val bitmap = remember(drawable) {
                drawable.toBitmap(width = 40, height = 40).asImageBitmap()
            }
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = app.appLabel,
                modifier = Modifier.size(40.dp),
            )
        }

        Text(
            text = app.appLabel,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            if (onAppInfo != null) {
                DropdownMenuItem(
                    text = { Text("App info") },
                    onClick = { showMenu = false; onAppInfo() },
                )
            }
            if (onUninstall != null) {
                DropdownMenuItem(
                    text = { Text("Uninstall") },
                    onClick = { showMenu = false; onUninstall() },
                )
            }
        }
    }
}
