# ylauncher

## Releases

**Every release gets a git tag.** Steps, in order:

1. Bump `versionCode` (+1) and `versionName` in `app/build.gradle.kts`.
2. Run the tests, then build the signed bundle:
   `./gradlew :app:testDebugUnitTest :app:bundleRelease`
   Output: `app/build/outputs/bundle/release/app-release.aab`. Signing reads `key.properties`
   (untracked — kept locally, not in the repo).
3. Commit as `Bump version to <versionName> (versionCode <code>)`.
4. Push `main`, then tag that commit and push the tag:
   `git tag v<versionName> && git push origin v<versionName>`
   Tags are lightweight and `v`-prefixed, e.g. `v1.8.3`. (`v1.8.0`–`v1.8.2` were never tagged.)

`app/build/` is wiped by `./gradlew clean` — upload the `.aab` to Play, or copy it somewhere
durable, before cleaning.

The mapping file and baseline profile are embedded in the bundle
(`BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map`), so Play deobfuscates
crash traces on its own — there is nothing to upload by hand.

## Building

Toolchain: Gradle 9.7.1, AGP 9.3.2, Kotlin 2.4.10 (built into AGP), JDK 25, `compileSdk = 37`.
Java/Kotlin language level is 21.

Pin JDK 25 for every Gradle invocation:

```
JAVA_HOME=$(/usr/libexec/java_home -v 25) ./gradlew <task>
```

That resolves to `~/Library/Java/JavaVirtualMachines/temurin-25.jdk` — a user-level install,
so no `sudo` and no Homebrew cask. `local.properties` (untracked) pins the same path via
`org.gradle.java.home` for the daemon; that does not cover the launcher JVM, hence both.

**JDK 26 does not work** — the build succeeds but 19 unit tests fail with
`IllegalArgumentException at ClassReader.java:200`, because mockk's bundled ASM cannot read
class file v70. JDK 26 also warns that reflective final-field mutation (which mockk relies on)
will be blocked outright in a future release. Stay on 25 until mockk catches up.

The language level stays at 21 even though JDK 25 can emit v69 — verified working through
D8/R8, but it buys nothing here (there is almost no Java source) and puts the build ahead of
what AGP documents as tested.

AGP 9 compiles Kotlin itself: **do not** re-apply `org.jetbrains.kotlin.android`, and there is
no `kotlinOptions {}` block — `compileOptions` drives the Kotlin JVM target too. The Compose
compiler plugin (`org.jetbrains.kotlin.plugin.compose`) is still applied separately, and it is
what pulls KGP up to the catalog's `kotlin` version over AGP's bundled 2.2.10. If that plugin
ever goes away, Kotlin silently drops back to AGP's bundled version.

`targetSdk` stays at 36 deliberately. Bumping it opts the launcher into Android 17 runtime
behavior changes and needs on-device testing — it is not part of a toolchain upgrade.

## Database

`favorite_apps.panelId` and `folder_apps.folderId` are real foreign keys. Never insert or
update through the raw Room methods — `FavoriteDao`/`FolderDao` expose transactional wrappers
that resolve the parent row first, because a write against a deleted panel or folder crashes
the launcher with `SQLITE_CONSTRAINT_FOREIGNKEY`. The `panels` table must never be empty; it is
seeded idempotently in `AppModule` on create, on destructive migration, and on every open.
