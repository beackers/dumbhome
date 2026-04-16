package com.beackers.dumbhome

enum class ShortcutAction(val displayName: String) {
    OPEN_NOTIFICATIONS("Show notifications"),
    OPEN_SETTINGS_APP("Open Android Settings"),
    OPEN_DUMBHOME_SETTINGS("Open DumbHome settings"),
    OPEN_APP_LAUNCHER("Open app launcher"),
    OPEN_OPEN_APPS("Open in-memory apps"),
    OPEN_ACTIVITY("Open an app"),
    OPEN_ASSISTANT("Open default assistant"),
    OPEN_CAMERA("Open camera app"),
    NONE("Do nothing");

    companion object {
        fun fromName(name: String?): ShortcutAction =
            entries.firstOrNull { it.name == name } ?: NONE
    }
}
