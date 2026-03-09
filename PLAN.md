# yLauncher — Implementation Plan

## Overview

Build an original Android launcher from scratch, inspired by OLauncher's minimalist philosophy and Niagara Launcher's aesthetic. The app uses **Kotlin + Jetpack Compose** with modern Android architecture.

### Design DNA
- **OLauncher**: Swipe gestures (left→Phone, right→Camera, up→App drawer, down→Notifications), minimalist text-based home, hidden apps
- **Niagara Launcher**: Josefin Sans / Work Sans typography, app icons alongside text, folders, alphabetical sidebar
- **HAL 9000**: The iconic AI button — a glowing sentinel at the bottom of the screen, gateway to the assistant
- **yLauncher (ours)**: Combines all three — clean text+icon favorites, swipe gestures, folders, "all apps" FAB, and a HAL-inspired AI button

---

## Architecture

```
com.ylauncher/
├── MainActivity.kt                  # Single activity, Compose host
├── YLauncherApp.kt                  # Application class (Hilt entry)
├── data/
│   ├── model/
│   │   ├── AppInfo.kt               # Installed app model (label, package, icon, user)
│   │   ├── FavoriteApp.kt           # Favorite with position/order
│   │   ├── Folder.kt                # Folder containing app references
│   │   └── SwipeAction.kt           # Swipe direction → app mapping
│   ├── repository/
│   │   ├── AppRepository.kt         # Query installed apps via LauncherApps API
│   │   └── PrefsRepository.kt       # DataStore preferences wrapper
│   └── db/
│       ├── YLauncherDatabase.kt     # Room database
│       ├── FavoriteDao.kt           # CRUD for favorites + ordering
│       └── FolderDao.kt             # CRUD for folders + contents
├── ui/
│   ├── theme/
│   │   ├── Theme.kt                 # Material3 dynamic theme (system light/dark)
│   │   ├── Type.kt                  # Josefin Sans + Work Sans typography
│   │   └── Color.kt                 # Minimal palette
│   ├── home/
│   │   ├── HomeScreen.kt            # Main home screen composable
│   │   ├── HomeViewModel.kt         # State: favorites, clock, swipe config
│   │   ├── FavoriteItem.kt          # Single favorite: icon + text (Josefin Sans)
│   │   ├── FolderItem.kt            # Expandable folder on home screen
│   │   └── SwipeGestureHandler.kt   # Compose pointer input for L/R/U/D swipes
│   ├── hal/
│   │   ├── HalButton.kt             # The HAL 9000 AI button composable
│   │   └── HalAnimations.kt         # Glow pulse, breathing, activation ring animations
│   ├── drawer/
│   │   ├── AppDrawerScreen.kt       # Full app list (scrollable, searchable)
│   │   ├── AppDrawerViewModel.kt    # All apps state, search filter
│   │   └── AppDrawerItem.kt         # Single app row: icon + label
│   ├── settings/
│   │   ├── SettingsScreen.kt        # All settings
│   │   └── SettingsViewModel.kt     # Settings state
│   └── components/
│       ├── AllAppsButton.kt          # Bottom-right FAB
│       ├── ClockWidget.kt            # Date/time display
│       ├── SearchBar.kt              # In-drawer search
│       └── ReorderableList.kt        # Drag-to-reorder favorites
├── gesture/
│   └── SwipeDetector.kt             # Compose-native swipe detection (like OLauncher's OnSwipeTouchListener)
├── util/
│   ├── AppLauncher.kt               # Launch app by package/activity/shortcut
│   └── Extensions.kt                # Context extensions (expand notifications, open dialer, etc.)
└── di/
    └── AppModule.kt                  # Hilt DI module
```

### Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Architecture**: MVVM (ViewModel + StateFlow)
- **Persistence**: Room (favorites, folders) + DataStore (preferences/settings)
- **DI**: Hilt
- **Min SDK**: 26 (Android 8.0) — **Target SDK**: 36
- **Build**: Gradle KTS

---

## Features & Todos

### Phase 1 — Project Scaffolding
1. **`project-setup`**: Initialize Android project with Gradle KTS, configure compileSdk 36, minSdk 26, targetSdk 36. Add dependencies: Compose BOM, Material3, Hilt, Room, DataStore, Navigation Compose, Activity Compose, Lifecycle.
2. **`launcher-manifest`**: Configure AndroidManifest.xml — declare as home launcher (`CATEGORY_HOME` + `CATEGORY_DEFAULT`), add `QUERY_ALL_PACKAGES`, `EXPAND_STATUS_BAR` permissions. Single activity, `launchMode=singleTask`.
3. **`theme-typography`**: Set up Material3 theme with system light/dark support. Bundle Josefin Sans (Regular, Light, Medium, Bold) + Work Sans (Regular, Medium) as Google Fonts. Define typography scale: home app labels → Josefin Sans Medium 20sp, clock → Josefin Sans Light 48sp, drawer items → Work Sans Regular 16sp.

### Phase 2 — Core Home Screen
4. **`app-repository`**: Implement `AppRepository` using `LauncherApps` API + `PackageManager` to query all installed apps. Return `AppInfo` (label, packageName, activityClassName, icon as `Drawable`, userHandle). Listen for package install/uninstall broadcasts to keep list fresh.
5. **`home-screen`**: Build `HomeScreen` composable — vertical list of 6–8 favorite app slots (icon + Josefin Sans label), centered or left-aligned. Show date/time at top (tappable → clock app). Bottom-right floating "all apps" button (grid icon or ≡).
6. **`favorites-system`**: Room database with `FavoriteApp` entity (position 1–8, packageName, activityClassName, userHandle, displayName). DAO for CRUD + reorder. On first launch, auto-populate with: Phone, Messages, Browser, Camera, Gallery, Settings (detected by intent categories).
7. **`clock-widget`**: Date/time composable at top of home screen. Tap → open clock app (configurable). Show date in Work Sans, time in Josefin Sans Light. Follow Niagara's clean clock style.

### Phase 3 — Swipe Gestures (OLauncher-inspired)
8. **`swipe-detector`**: Implement Compose-native swipe detection using `pointerInput` + `detectDragGestures`. Detect 4 directions with configurable velocity/distance thresholds. Support both full-screen swipes and per-item swipes (like OLauncher's `ViewSwipeTouchListener`).
9. **`swipe-actions`**: Wire swipe gestures:
   - **Swipe Right** → Open Phone (default, configurable)
   - **Swipe Left** → Open Camera (default, configurable)
   - **Swipe Up** → Open app drawer
   - **Swipe Down** → Expand notification shade
   - Store swipe app assignments in DataStore. Settings screen to change them.

### Phase 4 — App Drawer (OLauncher-style instant search)
10. **`app-drawer`**: Full-screen app drawer triggered by swipe-up or FAB tap. Implements OLauncher's keyboard-first UX:
    - **Keyboard auto-shows** when drawer opens (configurable in settings via `autoShowKeyboard` pref)
    - **Live filter**: as user types, app list filters instantly (case-insensitive, diacritics-normalized)
    - **Auto-launch on single match**: when exactly 1 app remains after filtering, it launches immediately (no tap needed) — this is OLauncher's killer feature. Prefix search with a space to disable auto-launch.
    - **Enter key**: submits search — launches first match, or falls back to web search if no match
    - **`!` prefix**: DuckDuckGo bang search (e.g., `!kotlin coroutines` opens web search)
    - Scrollable vertical list of all apps (icon + Work Sans label)
    - Alphabetical section headers or side index
    - Scroll up past top → dismiss drawer (like OLauncher's overscroll-to-exit)
    - Long-press an app → context menu: App Info, Hide, Uninstall, Add to Folder, Add to Home
11. **`app-launch`**: Utility to launch any app by packageName + activityClassName + userHandle. Handle work profile apps. Fallback to `packageManager.getLaunchIntentForPackage()`.

### Phase 5 — Folders
12. **`folder-model`**: Room entities: `Folder` (id, name, position, isExpanded) + `FolderApp` (folderId, packageName, activityClassName, position). DAOs for CRUD.
13. **`folder-ui`**: Folder item on home screen — shows folder name (Josefin Sans), tap to expand/collapse inline list of contained apps (with icons). Long-press to rename/edit. Folders can occupy a favorite slot.
14. **`folder-management`**: Add/remove apps to folders from app drawer (long-press → "Add to folder" option) or from folder edit screen.

### Phase 6 — Favorites Configuration
15. **`favorites-settings`**: Settings section to manage home screen favorites:
    - **Add**: Tap empty slot → opens app picker (filtered drawer)
    - **Remove**: Long-press favorite → "Remove" option
    - **Reorder**: Drag-and-drop reorder in a dedicated "Edit home" mode (long-press home → enter edit mode, drag handles appear)
    - **Replace**: Tap existing favorite → opens app picker to swap
16. **`swipe-settings`**: Settings to configure swipe left/right app assignments. Pick from installed apps list.

### Phase 7 — Settings & Polish
17. **`settings-screen`**: Settings screen with sections:
    - **Home**: Number of favorites (6 or 8), show/hide clock, text alignment
    - **Gestures**: Swipe left/right app, swipe down action (notifications or search), enable/disable individual swipes
    - **Appearance**: Theme (follow system), font size scale
    - **Apps**: Hidden apps list, manage folders
    - **About**: App version, source link
18. **`hidden-apps`**: Allow hiding apps from the drawer (stored in DataStore). Hidden apps still launchable via search.
19. **`final-polish`**: Animations (app launch transitions, drawer open/close, folder expand/collapse). Edge-to-edge display. Handle back button (close drawer → go home). Handle "set as default launcher" flow.

---

## Favorites Identification Strategy

How the home screen picks which apps to show:

1. **First launch auto-detect**: On first install, scan for common apps by intent category:
   - `Intent.ACTION_DIAL` → Phone app
   - `MediaStore.ACTION_IMAGE_CAPTURE` → Camera
   - `Intent.CATEGORY_APP_MESSAGING` → Messages
   - `Intent.CATEGORY_APP_BROWSER` → Browser
   - `Intent.CATEGORY_APP_GALLERY` → Gallery
   - Fall back to most-used apps via `UsageStatsManager` if permission granted
2. **Manual selection**: User taps an empty slot → app picker opens → select any installed app
3. **Long-press from drawer**: In the app drawer, long-press any app → "Add to home" option
4. **Drag from drawer**: (Future) drag an app from drawer onto home to add as favorite

### Reordering
- **Edit mode**: Long-press on home screen → favorites get drag handles → drag to reorder → tap "Done"
- Order is stored in Room DB (`FavoriteApp.position` column), so it persists across reboots
- Removing a favorite shifts others up automatically

---

## Key Decisions
- **No wallpaper download feature** (unlike OLauncher) — keep it simple, use system wallpaper
- **No device admin / screen lock** — avoid complex permissions for v1
- **No accessibility service** — avoid for v1
- **App icons always shown** (unlike OLauncher which is text-only) — this is the Niagara influence
- **Folders are inline-expandable** on home screen, not separate pages
- **DataStore for simple prefs**, Room for structured data (favorites, folders)
