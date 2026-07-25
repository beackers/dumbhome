package com.beackers.dumbhome

enum class LockStrength(val displayName: String) {
    NONE("None"),
    DPAD_SEQUENCE("D-pad sequence"),
    PIN("PIN");

    companion object {
        fun fromName(name: String?): LockStrength = entries.firstOrNull { it.name == name } ?: NONE
    }
}
