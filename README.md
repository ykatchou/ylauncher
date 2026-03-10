# yLauncher

A minimalist, gesture-driven Android launcher that blends the best ideas from [OLauncher](https://github.com/tanujnotes/Olauncher), [Niagara Launcher](https://play.google.com/store/apps/details?id=bitpit.launcher), and a touch of HAL 9000.

Clean typography · Swipe gestures · Instant search · Configurable magic button

## Features

**Home Screen** — A handful of favorite apps displayed with beautiful typography (Josefin Sans + Work Sans) and icons. A live clock sits at the top; real-time notification summaries appear below each app. Organize favorites across multiple panels (e.g. Personal / Work).

**Swipe Gestures** — Swipe left for Camera, right for Phone, up to open the app drawer, down to pull notifications. Every gesture is fully configurable or can be disabled.

**App Drawer** — Full app list with instant search. Typing filters results live and **auto-launches on a single match** — type `spo` and Spotify opens immediately. Prefix a query with `!` for a DuckDuckGo bang search. An alphabet sidebar lets you jump quickly through the list.

**Folders** — Tap to expand inline on the home screen. Assign an emoji icon, reorder apps, and manage contents from a long-press menu.

**Magic Button** — A glowing HAL 9000–inspired button at the bottom of the screen. Tap, long-press, and double-tap actions are each independently configurable: launch an assistant, toggle the flashlight, lock the screen, open any app, and more.

**Settings** — Tweak everything: number of favorites, gesture targets, magic button actions, hidden apps, font size, dark mode, and left-hand mode.

## Screenshots

_Coming soon._

## Building

```bash
# Clone the repository
git clone https://github.com/ykatchou/ylauncher.git
cd ylauncher

# Build a debug APK
./gradlew assembleDebug

# Run tests
./gradlew test
```

Requires **JDK 17** and the **Android SDK** (compile SDK 36, min SDK 26).

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose · Material 3 · Dynamic Color |
| Architecture | MVVM · StateFlow · ViewModel |
| Persistence | Room (favorites, folders) · DataStore (preferences) |
| DI | Hilt |
| Navigation | Navigation Compose |
| Language | Kotlin 2.1 |
| Testing | JUnit · MockK · Turbine · Robolectric |

## Inspirations

- **[OLauncher](https://github.com/tanujnotes/Olauncher)** — Swipe gestures, text-focused minimalism, auto-launch on single match
- **[Niagara Launcher](https://play.google.com/store/apps/details?id=bitpit.launcher)** — Typography, icons alongside labels, aesthetic design
- **HAL 9000** — The glowing red eye as a floating action gateway

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).

Fonts bundled under the [SIL Open Font License](https://openfontlicense.org/):
[Josefin Sans](https://fonts.google.com/specimen/Josefin+Sans) by Santiago Orozco ·
[Work Sans](https://fonts.google.com/specimen/Work+Sans) by Wei Huang
