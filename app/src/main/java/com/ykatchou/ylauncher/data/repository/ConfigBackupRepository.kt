package com.ykatchou.ylauncher.data.repository

import androidx.room.withTransaction
import com.ykatchou.ylauncher.data.db.FavoriteDao
import com.ykatchou.ylauncher.data.db.FolderDao
import com.ykatchou.ylauncher.data.db.PanelDao
import com.ykatchou.ylauncher.data.db.YLauncherDatabase
import com.ykatchou.ylauncher.data.model.FavoriteApp
import com.ykatchou.ylauncher.data.model.Folder
import com.ykatchou.ylauncher.data.model.FolderApp
import com.ykatchou.ylauncher.data.model.Panel
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

private const val SCHEMA_VERSION = 1

/**
 * Exports/imports panels, favorites (incl. folders) and home-screen settings as a single
 * human-readable JSON document — the closest thing this app has to a backup, since Android's
 * automatic allowBackup restore only fires on a fresh install with the same Google account and
 * isn't user-triggerable or portable across a signing-key change.
 *
 * Import fully replaces existing panels/folders/favorites, so the whole file is parsed and
 * validated into [ParsedBackup] *before* any database write starts — a malformed file throws
 * without touching existing data.
 */
@Singleton
class ConfigBackupRepository @Inject constructor(
    private val database: YLauncherDatabase,
    private val panelDao: PanelDao,
    private val folderDao: FolderDao,
    private val favoriteDao: FavoriteDao,
    private val prefsRepository: PrefsRepository,
) {
    suspend fun exportJson(): String {
        val panels = panelDao.getAllPanelsOnce()
        val panelIndexById = panels.withIndex().associate { (i, p) -> p.id to i }
        val folders = folderDao.getAllFolders().first()
        val folderIndexById = folders.withIndex().associate { (i, f) -> f.id to i }
        val favorites = favoriteDao.getAllFavoritesOnce()

        val root = JSONObject()
        root.put("schemaVersion", SCHEMA_VERSION)

        root.put(
            "panels",
            JSONArray(panels.map { p ->
                JSONObject()
                    .put("name", p.name)
                    .put("position", p.position)
                    .put("enabled", p.enabled)
            }),
        )

        root.put(
            "folders",
            JSONArray(folders.map { f ->
                val apps = folderDao.getAppsInFolderOnce(f.id)
                JSONObject()
                    .put("name", f.name)
                    .put("position", f.position)
                    .put("isExpanded", f.isExpanded)
                    .put("iconEmoji", f.iconEmoji)
                    .put(
                        "apps",
                        JSONArray(apps.map { a ->
                            JSONObject()
                                .put("packageName", a.packageName)
                                .put("activityClassName", a.activityClassName)
                                .put("displayName", a.displayName)
                                .put("position", a.position)
                                .put("userHandleString", a.userHandleString)
                        }),
                    )
            }),
        )

        root.put(
            "favorites",
            JSONArray(favorites.map { fav ->
                JSONObject()
                    .put("position", fav.position)
                    .put("packageName", fav.packageName)
                    .put("activityClassName", fav.activityClassName)
                    .put("displayName", fav.displayName)
                    .put("userHandleString", fav.userHandleString)
                    .put("iconEmoji", fav.iconEmoji)
                    .put("folderIndex", fav.folderId?.let { folderIndexById[it] } ?: JSONObject.NULL)
                    .put("panelIndex", panelIndexById[fav.panelId] ?: 0)
            }),
        )

        root.put(
            "settings",
            JSONObject()
                .put("showClock", prefsRepository.showClock.first())
                .put("showNotifBubble", prefsRepository.showNotifBubble.first())
                .put("showNotifPreview", prefsRepository.showNotifPreview.first())
                .put("showNotifBadge", prefsRepository.showNotifBadge.first())
                .put("showDonation", prefsRepository.showDonation.first())
                .put("autoShowKeyboard", prefsRepository.autoShowKeyboard.first())
                .put("leftHandMode", prefsRepository.leftHandMode.first())
                .put("textSizeScalePercent", (prefsRepository.textSizeScale.first() * 100).roundToInt())
                .put("suggestionCount", prefsRepository.suggestionCount.first())
                .put("recentAppsCount", prefsRepository.recentAppsCount.first())
                .put("autoLaunchDelayTenths", (prefsRepository.autoLaunchDelay.first() * 10).roundToInt())
                .put("swipeLeftEnabled", prefsRepository.swipeLeftEnabled.first())
                .put("swipeRightEnabled", prefsRepository.swipeRightEnabled.first())
                .put("swipeLeftPackage", prefsRepository.swipeLeftPackage.first())
                .put("swipeLeftActivity", prefsRepository.swipeLeftActivity.first())
                .put("swipeLeftName", prefsRepository.swipeLeftName.first())
                .put("swipeRightPackage", prefsRepository.swipeRightPackage.first())
                .put("swipeRightActivity", prefsRepository.swipeRightActivity.first())
                .put("swipeRightName", prefsRepository.swipeRightName.first())
                .put("halAssistantPackage", prefsRepository.halAssistantPackage.first())
                .put("halTapAction", prefsRepository.halTapAction.first())
                .put("halLongPressAction", prefsRepository.halLongPressAction.first())
                .put("halDoubleTapAction", prefsRepository.halDoubleTapAction.first())
                .put("hiddenApps", JSONArray(prefsRepository.hiddenApps.first().toList())),
        )

        return root.toString(2)
    }

    /** Throws [IllegalArgumentException]/[org.json.JSONException] on a malformed file — nothing is written when it does. */
    suspend fun importJson(json: String) {
        val parsed = parseBackup(json)

        database.withTransaction {
            favoriteDao.deleteAll()
            folderDao.deleteAllFolderApps()
            folderDao.deleteAllFolders()
            panelDao.deleteAll()

            val newPanelIds = parsed.panels.map { p ->
                panelDao.insertPanel(Panel(name = p.name, position = p.position, enabled = p.enabled))
            }

            val newFolderIds = parsed.folders.map { f ->
                val folderId = folderDao.insertFolder(
                    Folder(name = f.name, position = f.position, isExpanded = f.isExpanded, iconEmoji = f.iconEmoji),
                )
                f.apps.forEach { a ->
                    folderDao.insertFolderApp(
                        FolderApp(
                            folderId = folderId,
                            packageName = a.packageName,
                            activityClassName = a.activityClassName,
                            displayName = a.displayName,
                            position = a.position,
                            userHandleString = a.userHandleString,
                        ),
                    )
                }
                folderId
            }

            parsed.favorites.forEach { fav ->
                favoriteDao.insertFavorite(
                    FavoriteApp(
                        position = fav.position,
                        packageName = fav.packageName,
                        activityClassName = fav.activityClassName,
                        displayName = fav.displayName,
                        userHandleString = fav.userHandleString,
                        folderId = fav.folderIndex?.let { newFolderIds[it] },
                        iconEmoji = fav.iconEmoji,
                        panelId = newPanelIds[fav.panelIndex],
                    ),
                )
            }
        }

        parsed.settings?.let { applySettings(it) }

        // The active panel may no longer exist under its old id — pin it to the first panel.
        val firstPanelId = panelDao.getAllPanelsOnce().minByOrNull { it.position }?.id
        if (firstPanelId != null) prefsRepository.setActivePanel(firstPanelId)
    }

    private suspend fun applySettings(s: ParsedSettings) {
        prefsRepository.setShowClock(s.showClock)
        prefsRepository.setShowNotifBubble(s.showNotifBubble)
        prefsRepository.setShowNotifPreview(s.showNotifPreview)
        prefsRepository.setShowNotifBadge(s.showNotifBadge)
        prefsRepository.setShowDonation(s.showDonation)
        prefsRepository.setAutoShowKeyboard(s.autoShowKeyboard)
        prefsRepository.setLeftHandMode(s.leftHandMode)
        prefsRepository.setTextSizeScale(s.textSizeScalePercent)
        prefsRepository.setSuggestionCount(s.suggestionCount)
        prefsRepository.setRecentAppsCount(s.recentAppsCount)
        prefsRepository.setAutoLaunchDelay(s.autoLaunchDelayTenths)
        prefsRepository.setSwipeLeftEnabled(s.swipeLeftEnabled)
        prefsRepository.setSwipeRightEnabled(s.swipeRightEnabled)
        prefsRepository.setSwipeLeft(s.swipeLeftPackage, s.swipeLeftActivity, s.swipeLeftName)
        prefsRepository.setSwipeRight(s.swipeRightPackage, s.swipeRightActivity, s.swipeRightName)
        prefsRepository.setHalAssistantPackage(s.halAssistantPackage)
        prefsRepository.setHalTapAction(s.halTapAction)
        prefsRepository.setHalLongPressAction(s.halLongPressAction)
        prefsRepository.setHalDoubleTapAction(s.halDoubleTapAction)
        prefsRepository.setHiddenApps(s.hiddenApps)
    }

    private fun parseBackup(json: String): ParsedBackup {
        val root = JSONObject(json)
        require(root.optInt("schemaVersion", -1) == SCHEMA_VERSION) {
            "Unsupported or missing schemaVersion (expected $SCHEMA_VERSION)"
        }

        val panelsJson = root.optJSONArray("panels") ?: JSONArray()
        require(panelsJson.length() > 0) { "Backup must contain at least one panel" }
        val panels = (0 until panelsJson.length()).map { i ->
            val p = panelsJson.getJSONObject(i)
            ParsedPanel(
                name = p.getString("name"),
                position = p.getInt("position"),
                enabled = p.optBoolean("enabled", true),
            )
        }

        val foldersJson = root.optJSONArray("folders") ?: JSONArray()
        val folders = (0 until foldersJson.length()).map { i ->
            val f = foldersJson.getJSONObject(i)
            val appsJson = f.optJSONArray("apps") ?: JSONArray()
            val apps = (0 until appsJson.length()).map { j ->
                val a = appsJson.getJSONObject(j)
                ParsedFolderApp(
                    packageName = a.getString("packageName"),
                    activityClassName = a.optStringOrNull("activityClassName"),
                    displayName = a.getString("displayName"),
                    position = a.getInt("position"),
                    userHandleString = a.optString("userHandleString", ""),
                )
            }
            ParsedFolder(
                name = f.getString("name"),
                position = f.getInt("position"),
                isExpanded = f.optBoolean("isExpanded", false),
                iconEmoji = f.optString("iconEmoji", "📁"),
                apps = apps,
            )
        }

        val favoritesJson = root.optJSONArray("favorites") ?: JSONArray()
        val favorites = (0 until favoritesJson.length()).map { i ->
            val fav = favoritesJson.getJSONObject(i)
            val folderIndex = if (fav.isNull("folderIndex")) null else fav.getInt("folderIndex")
            val panelIndex = fav.optInt("panelIndex", 0)
            require(folderIndex == null || folderIndex in folders.indices) {
                "favorites[$i].folderIndex $folderIndex is out of range"
            }
            require(panelIndex in panels.indices) {
                "favorites[$i].panelIndex $panelIndex is out of range"
            }
            ParsedFavorite(
                position = fav.getInt("position"),
                packageName = fav.getString("packageName"),
                activityClassName = fav.optStringOrNull("activityClassName"),
                displayName = fav.getString("displayName"),
                userHandleString = fav.optString("userHandleString", ""),
                iconEmoji = fav.optStringOrNull("iconEmoji"),
                folderIndex = folderIndex,
                panelIndex = panelIndex,
            )
        }

        val settingsJson = root.optJSONObject("settings")
        val settings = settingsJson?.let { s ->
            ParsedSettings(
                showClock = s.optBoolean("showClock", true),
                showNotifBubble = s.optBoolean("showNotifBubble", true),
                showNotifPreview = s.optBoolean("showNotifPreview", true),
                showNotifBadge = s.optBoolean("showNotifBadge", true),
                showDonation = s.optBoolean("showDonation", true),
                autoShowKeyboard = s.optBoolean("autoShowKeyboard", true),
                leftHandMode = s.optBoolean("leftHandMode", false),
                textSizeScalePercent = s.optInt("textSizeScalePercent", 100),
                suggestionCount = s.optInt("suggestionCount", 3),
                recentAppsCount = s.optInt("recentAppsCount", 0),
                autoLaunchDelayTenths = s.optInt("autoLaunchDelayTenths", 10),
                swipeLeftEnabled = s.optBoolean("swipeLeftEnabled", true),
                swipeRightEnabled = s.optBoolean("swipeRightEnabled", true),
                swipeLeftPackage = s.optString("swipeLeftPackage", ""),
                swipeLeftActivity = s.optString("swipeLeftActivity", ""),
                swipeLeftName = s.optString("swipeLeftName", "Camera"),
                swipeRightPackage = s.optString("swipeRightPackage", ""),
                swipeRightActivity = s.optString("swipeRightActivity", ""),
                swipeRightName = s.optString("swipeRightName", "Phone"),
                halAssistantPackage = s.optString("halAssistantPackage", ""),
                halTapAction = s.optString("halTapAction", "ASSISTANT"),
                halLongPressAction = s.optString("halLongPressAction", "SETTINGS"),
                halDoubleTapAction = s.optString("halDoubleTapAction", "APP_DRAWER"),
                hiddenApps = s.optJSONArray("hiddenApps")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }.toSet()
                } ?: emptySet(),
            )
        }

        return ParsedBackup(panels, folders, favorites, settings)
    }
}

private fun JSONObject.optStringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) getString(key) else null

private data class ParsedPanel(val name: String, val position: Int, val enabled: Boolean)

private data class ParsedFolderApp(
    val packageName: String,
    val activityClassName: String?,
    val displayName: String,
    val position: Int,
    val userHandleString: String,
)

private data class ParsedFolder(
    val name: String,
    val position: Int,
    val isExpanded: Boolean,
    val iconEmoji: String,
    val apps: List<ParsedFolderApp>,
)

private data class ParsedFavorite(
    val position: Int,
    val packageName: String,
    val activityClassName: String?,
    val displayName: String,
    val userHandleString: String,
    val iconEmoji: String?,
    val folderIndex: Int?,
    val panelIndex: Int,
)

private data class ParsedSettings(
    val showClock: Boolean,
    val showNotifBubble: Boolean,
    val showNotifPreview: Boolean,
    val showNotifBadge: Boolean,
    val showDonation: Boolean,
    val autoShowKeyboard: Boolean,
    val leftHandMode: Boolean,
    val textSizeScalePercent: Int,
    val suggestionCount: Int,
    val recentAppsCount: Int,
    val autoLaunchDelayTenths: Int,
    val swipeLeftEnabled: Boolean,
    val swipeRightEnabled: Boolean,
    val swipeLeftPackage: String,
    val swipeLeftActivity: String,
    val swipeLeftName: String,
    val swipeRightPackage: String,
    val swipeRightActivity: String,
    val swipeRightName: String,
    val halAssistantPackage: String,
    val halTapAction: String,
    val halLongPressAction: String,
    val halDoubleTapAction: String,
    val hiddenApps: Set<String>,
)

private data class ParsedBackup(
    val panels: List<ParsedPanel>,
    val folders: List<ParsedFolder>,
    val favorites: List<ParsedFavorite>,
    val settings: ParsedSettings?,
)
