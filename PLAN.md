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

### Phase 7 — HAL 9000 AI Button
17. **`hal-button-ui`**: The HAL button — a signature UI element, floating at bottom-center of the home screen.

#### Visual Design — Full Spec

The button is a direct homage to the HAL 9000 camera eye from *2001: A Space Odyssey*:

```
    ┌─────────────────────────────┐
    │      outer ring (dark       │
    │      gunmetal, 1.5dp)       │
    │   ┌─────────────────────┐   │
    │   │   gradient ring     │   │
    │   │   (amber → deep     │   │
    │   │    red radial)      │   │
    │   │   ┌─────────────┐   │   │
    │   │   │  INNER EYE  │   │   │
    │   │   │  (solid     │   │   │
    │   │   │   #CC0000   │   │   │
    │   │   │   glowing)  │   │   │
    │   │   │   ● lens    │   │   │
    │   │   │   highlight │   │   │
    │   │   └─────────────┘   │   │
    │   └─────────────────────┘   │
    └─────────────────────────────┘
           48dp total diameter
```

**Layers (drawn with Compose `Canvas`):**
1. **Outer bezel** — `Circle`, 48dp, fill `#2A2A2E` (dark gunmetal), 1.5dp stroke `#444`
2. **Amber-red gradient ring** — `Circle`, 40dp, `RadialGradient` from `#FF6600` (amber edge) → `#CC0000` (deep red center)
3. **Inner eye** — `Circle`, 24dp, solid `#CC0000`, with a soft `shadow(color = #FF0000, radius = 12.dp, blurRadius = 8.dp)` for the glow
4. **Lens highlight** — small off-center `Circle` (6dp), white at 30% opacity, offset top-left to simulate light reflection (like a camera lens)

**Animations:**
- **Idle breathing**: the red glow `shadow` radius pulses slowly between 8dp and 14dp using `infiniteTransition` + `animateFloat` (2.5s period, `EaseInOut`). Subtle — the eye appears to softly breathe.
- **Tap feedback**: on press, the inner eye scales to 1.15x over 100ms (`spring` animation), glow intensifies to radius 20dp, then returns. Quick, satisfying.
- **Active/listening state** (when Gemini is processing): the gradient ring rotates slowly (6s full rotation) using `animateFloat` with `LinearEasing` on the gradient angle. The glow turns brighter (`#FF2222`) and pulsing accelerates to 1s period. Conveys "I'm thinking."
- **Long-press hint**: after 300ms hold, a subtle ring expansion ripple emanates outward (like a sonar ping) — single ring that fades out at 64dp diameter.

**Placement:**
- Bottom-center of home screen, 16dp above the navigation bar inset
- Uses `Modifier.navigationBarsPadding()` for edge-to-edge support
- Z-order: above favorites list, below any overlays/dialogs
- Does **not** conflict with the "All Apps" FAB (which is bottom-right)

**Interaction:**
- **Tap** → launch Gemini assistant (`com.google.android.apps.googleassistant` or `com.google.android.googlequicksearchbox` with assistant intent)
- **Long-press** → show a tooltip: *"AI Assistant"* in Josefin Sans, floating above the button for 2s
- If Gemini is not installed → show a toast: *"Please install Google Gemini"* and open Play Store link
- The target assistant app is **configurable** in Settings (default: Gemini)

18. **`hal-button-integration`**: Wire HAL button into `HomeScreen` layout. Store chosen assistant package in DataStore. Add Settings entry under "AI Assistant" section to pick which app the button triggers. Detect if target app is installed, show fallback gracefully.

### Phase 8 — About Screen
19. **`about-screen`**: Dedicated About screen accessible from Settings. Composable layout:

#### Content & Layout
```
┌──────────────────────────────────┐
│                                  │
│          yLauncher               │  ← Josefin Sans Bold, 28sp
│          v1.0.0                  │  ← Work Sans Regular, 14sp, 50% opacity
│                                  │
│  ─────────────────────────────── │
│                                  │
│  Created by                      │  ← section header, Work Sans Medium 12sp, uppercase
│  Yoann Katchourine               │  ← Josefin Sans Medium, 18sp
│  github.com/ykatchou             │  ← Work Sans, 14sp, tappable link (opens browser)
│                                  │
│  ─────────────────────────────── │
│                                  │
│  Inspirations                    │  ← section header
│                                  │
│  ◉ OLauncher                     │  ← each with icon + description
│    Minimal AF Launcher by        │
│    tanujnotes — GPL v3           │
│    github.com/tanujnotes/        │
│    Olauncher                     │  ← tappable link
│                                  │
│  ◉ Niagara Launcher              │
│    Minimalist one-hand launcher  │
│    by Peter Huber (Bitpit)       │
│    play.google.com/store/apps/   │
│    details?id=bitpit.launcher    │  ← tappable link
│                                  │
│  ─────────────────────────────── │
│                                  │
│  Typography                      │  ← section header
│  Josefin Sans by Santiago        │
│  Orozco — OFL license            │
│  Work Sans by Wei Huang — OFL    │
│                                  │
│  ─────────────────────────────── │
│                                  │
│  Built with                      │  ← section header
│  Kotlin · Jetpack Compose        │
│  Material3 · Room · Hilt         │
│                                  │
│  ─────────────────────────────── │
│                                  │
│  Source code                     │  ← tappable → opens repo
│  github.com/ykatchou/ylauncher  │
│                                  │
│  License: GPL v3                 │
│                                  │
└──────────────────────────────────┘
```

All links are tappable and open in the default browser. The screen scrolls if content exceeds viewport. Design matches the overall Josefin Sans / Work Sans aesthetic of the launcher.

### Phase 9 — Settings & Polish
20. **`settings-screen`**: Settings screen with sections:
    - **Home**: Number of favorites (6 or 8), show/hide clock, text alignment
    - **Gestures**: Swipe left/right app, swipe down action (notifications or search), enable/disable individual swipes
    - **Appearance**: Theme (follow system), font size scale
    - **AI Assistant**: Choose which app the HAL button launches (default: Gemini)
    - **Apps**: Hidden apps list, manage folders
    - **About**: → opens the About screen (see Phase 8)
21. **`hidden-apps`**: Allow hiding apps from the drawer (stored in DataStore). Hidden apps still launchable via search.
22. **`final-polish`**: Animations (app launch transitions, drawer open/close, folder expand/collapse). Edge-to-edge display. Handle back button (close drawer → go home). Handle "set as default launcher" flow.

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
