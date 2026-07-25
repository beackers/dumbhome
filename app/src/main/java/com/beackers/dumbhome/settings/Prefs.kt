package com.beackers.dumbhome

import android.content.Context
import android.net.Uri

class Prefs(context: Context) {
    private val prefs = context.getSharedPreferences("dumbhome_prefs", Context.MODE_PRIVATE)

    fun initializeDefaultsIfNeeded() {
        if (prefs.getBoolean(KEY_INITIALIZED, false)) return
        prefs.edit()
            .putBoolean(KEY_INITIALIZED, true)
            .putString(KEY_F11, ShortcutAction.OPEN_NOTIFICATIONS.name)
            .putString(KEY_DOWN, ShortcutAction.OPEN_SETTINGS_APP.name)
            .putString(KEY_MENU, ShortcutAction.OPEN_DUMBHOME_SETTINGS.name)
            .putString(KEY_UP, ShortcutAction.NONE.name)
            .putString(KEY_LEFT, ShortcutAction.NONE.name)
            .putString(KEY_RIGHT, ShortcutAction.NONE.name)
            .apply()
    }

    fun getShortcut(key: String): ShortcutAction = ShortcutAction.fromName(prefs.getString(key, ShortcutAction.NONE.name))

    fun setShortcut(key: String, action: ShortcutAction) {
        prefs.edit().putString(key, action.name).apply()
    }

    fun setShortcutApp(key: String, packageName: String) {
        prefs.edit().putString("${key}_app", packageName).apply()
    }

    fun getShortcutApp(key: String): String? {
        return prefs.getString("${key}_app", null)
    }

    fun setWallpaper(uri: Uri?) {
        prefs.edit().putString(KEY_WALLPAPER_URI, uri?.toString()).apply()
    }

    fun getWallpaperUri(): Uri? = prefs.getString(KEY_WALLPAPER_URI, null)?.let(Uri::parse)

    fun getLockStrength(): LockStrength = LockStrength.fromName(prefs.getString(KEY_LOCK_STRENGTH, LockStrength.NONE.name))

    fun setLockStrength(strength: LockStrength) {
        prefs.edit().putString(KEY_LOCK_STRENGTH, strength.name).apply()
    }

    fun getLockDpadSequence(): String = prefs.getString(KEY_LOCK_DPAD_SEQUENCE, DEFAULT_DPAD_SEQUENCE) ?: DEFAULT_DPAD_SEQUENCE

    fun setLockDpadSequence(sequence: String) {
        prefs.edit().putString(KEY_LOCK_DPAD_SEQUENCE, sequence).apply()
    }

    fun getLockPin(): String = prefs.getString(KEY_LOCK_PIN, DEFAULT_PIN) ?: DEFAULT_PIN

    fun setLockPin(pin: String) {
        prefs.edit().putString(KEY_LOCK_PIN, pin).apply()
    }

    fun setLockWallpaper(uri: Uri?) {
        prefs.edit().putString(KEY_LOCK_WALLPAPER_URI, uri?.toString()).apply()
    }

    fun getLockWallpaperUri(): Uri? = prefs.getString(KEY_LOCK_WALLPAPER_URI, null)?.let(Uri::parse)

    fun useMediaArtOnLockScreen(): Boolean = prefs.getBoolean(KEY_LOCK_USE_MEDIA_ART, false)

    fun setUseMediaArtOnLockScreen(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_USE_MEDIA_ART, enabled).apply()
    }

    companion object {
        const val KEY_INITIALIZED = "initialized"
        const val KEY_F11 = "f11"
        const val KEY_MENU = "menu"
        const val KEY_UP = "up"
        const val KEY_DOWN = "down"
        const val KEY_LEFT = "left"
        const val KEY_RIGHT = "right"
        const val KEY_WALLPAPER_URI = "wallpaper_uri"
        const val KEY_LOCK_STRENGTH = "lock_strength"
        const val KEY_LOCK_DPAD_SEQUENCE = "lock_dpad_sequence"
        const val KEY_LOCK_PIN = "lock_pin"
        const val KEY_LOCK_WALLPAPER_URI = "lock_wallpaper_uri"
        const val KEY_LOCK_USE_MEDIA_ART = "lock_use_media_art"
        const val DEFAULT_DPAD_SEQUENCE = "↑ ↓ ← →"
        const val DEFAULT_PIN = "1234"

        val shortcutKeys = listOf(KEY_F11, KEY_MENU, KEY_UP, KEY_DOWN, KEY_LEFT, KEY_RIGHT)
    }
}
