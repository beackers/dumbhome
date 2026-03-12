package com.beackers.dumbhome

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private lateinit var list: RecyclerView
    private lateinit var adapter: SimpleTextAdapter
    private val rows = mutableListOf<String>()

    private val openFilePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data ?: return@registerForActivityResult
        prefs.setWallpaper(uri)
        // Validate file can be decoded before leaving settings.
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = Prefs(this)
        list = findViewById(R.id.settingsList)
        list.layoutManager = LinearLayoutManager(this)
        adapter = SimpleTextAdapter(rows) { onClickRow(it) }
        list.adapter = adapter

        refreshRows()
    }

    private fun refreshRows() {
        rows.clear()
        rows += "Change home image (file picker)"
        rows += "Set live wallpaper"
        rows += "Configure F11 (${prefs.getShortcut(Prefs.KEY_F11).displayName})"
        rows += "Configure Menu (${prefs.getShortcut(Prefs.KEY_MENU).displayName})"
        rows += "Configure Up (${prefs.getShortcut(Prefs.KEY_UP).displayName})"
        rows += "Configure Down (${prefs.getShortcut(Prefs.KEY_DOWN).displayName})"
        rows += "Configure Left (${prefs.getShortcut(Prefs.KEY_LEFT).displayName})"
        rows += "Configure Right (${prefs.getShortcut(Prefs.KEY_RIGHT).displayName})"
        rows += "Close"
        adapter.submit(rows)
    }

    private fun onClickRow(position: Int) {
        when (position) {
            0 -> openFilePicker.launch(Intent(this, FilePickerActivity::class.java))
            1 -> openLiveWallpaperPicker()
            2 -> pickAction(Prefs.KEY_F11)
            3 -> pickAction(Prefs.KEY_MENU)
            4 -> pickAction(Prefs.KEY_UP)
            5 -> pickAction(Prefs.KEY_DOWN)
            6 -> pickAction(Prefs.KEY_LEFT)
            7 -> pickAction(Prefs.KEY_RIGHT)
            8 -> finish()
        }
    }

    private fun pickAction(prefKey: String) {
        val actions = ShortcutAction.entries
        AlertDialog.Builder(this)
            .setTitle("Shortcut action")
            .setItems(actions.map { it.displayName }.toTypedArray()) { _, which ->
                prefs.setShortcut(prefKey, actions[which])
                refreshRows()
            }
            .show()
    }

    private fun openLiveWallpaperPicker() {
        try {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER))
        }
    }
}
