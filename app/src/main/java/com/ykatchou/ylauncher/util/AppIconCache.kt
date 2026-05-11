package com.ykatchou.ylauncher.util

import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/**
 * Process-wide LRU cache for app icon bitmaps.
 * Keyed by "packageName|sizePx" so the same app appearing in favorites, folder popup,
 * and the drawer all share a single ImageBitmap allocation.
 * Evict a package entry when its app is updated or uninstalled.
 */
object AppIconCache {
    // 150 entries × ~7 KB (44×44 ARGB_8888) ≈ 1 MB upper bound
    private val cache = LruCache<String, ImageBitmap>(150)

    fun get(drawable: Drawable, packageName: String, sizePx: Int): ImageBitmap {
        val key = "$packageName|$sizePx"
        return cache[key] ?: drawable.toBitmap(width = sizePx, height = sizePx)
            .asImageBitmap()
            .also { cache.put(key, it) }
    }

    fun evict(packageName: String) {
        cache.snapshot().keys
            .filter { it.startsWith("$packageName|") }
            .forEach { cache.remove(it) }
    }
}
