package com.beackers.dumbhome

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

object WallpaperStorage {
    private const val FILE_NAME = "homescreen_wallpaper.jpg"

    private fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    fun save(context: Context, bitmap: Bitmap) {
        file(context).outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
    }

    fun load(context: Context): Bitmap? {
        val wallpaperFile = file(context)
        if (!wallpaperFile.exists()) return null
        return BitmapFactory.decodeFile(wallpaperFile.absolutePath)
    }
}
