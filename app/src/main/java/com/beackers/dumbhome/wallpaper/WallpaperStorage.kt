package com.beackers.dumbhome

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

object WallpaperStorage {
    private const val HOME_FILE_NAME = "homescreen_wallpaper.jpg"
    private const val LOCK_FILE_NAME = "lockscreen_wallpaper.jpg"

    private fun file(context: Context, target: WallpaperTarget): File = File(context.filesDir, target.fileName)

    fun save(context: Context, bitmap: Bitmap, target: WallpaperTarget = WallpaperTarget.HOME) {
        file(context, target).outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
    }

    fun load(context: Context, target: WallpaperTarget = WallpaperTarget.HOME): Bitmap? {
        val wallpaperFile = file(context, target)
        if (!wallpaperFile.exists()) return null
        return BitmapFactory.decodeFile(wallpaperFile.absolutePath)
    }

    enum class WallpaperTarget(val fileName: String) {
        HOME("homescreen_wallpaper.jpg"),
        LOCK("lockscreen_wallpaper.jpg")
    }
}
