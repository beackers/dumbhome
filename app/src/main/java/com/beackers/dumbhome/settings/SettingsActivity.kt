package com.beackers.dumbhome

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.graphics.drawable.BitmapDrawable
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.beackers.dumbhome.launcher.LauncherActivity
import com.beackers.dumbhome.R
import com.beackers.dumbhome.databinding.ActivitySettingsBinding
import com.beackers.dumbhome.WallpaperStorage

class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private var currentPrefKey: String? = null
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var list: RecyclerView
    private lateinit var adapter: SimpleTextAdapter
    private val rows = mutableListOf<String>()

    private val cropWallpaper = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshRows()
    }

    private val pickWallpaper = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult

        cropWallpaper.launch(
            Intent(this, WallpaperCropActivity::class.java)
                .putExtra(WallpaperCropActivity.EXTRA_IMAGE_URI, uri)
        )
    }

    private val pickShortcutApp = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult

        val packageName = result.data?.getStringExtra(LauncherActivity.EXTRA_SELECTED_PACKAGE)
            ?: return@registerForActivityResult
        val key = currentPrefKey ?: return@registerForActivityResult

        prefs.setShortcut(key, ShortcutAction.OPEN_ACTIVITY)
        prefs.setShortcutApp(key, packageName)
        refreshRows()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.setDecorFitsSystemWindows(false)
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        WallpaperStorage.load(this)?.let {
            binding.settingsLayout.background = BitmapDrawable(resources, it)
        }

        prefs = Prefs(this)
        list = findViewById(R.id.settingsList)
        list.layoutManager = LinearLayoutManager(this)
        adapter = SimpleTextAdapter(rows) { onClickRow(it) }
        list.adapter = adapter

        refreshRows()
    }

    private fun refreshRows() {
        rows.clear()
        rows += "Change home image"
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
            0 -> pickWallpaper.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
            1 -> pickAction(Prefs.KEY_F11)
            2 -> pickAction(Prefs.KEY_MENU)
            3 -> pickAction(Prefs.KEY_UP)
            4 -> pickAction(Prefs.KEY_DOWN)
            5 -> pickAction(Prefs.KEY_LEFT)
            6 -> pickAction(Prefs.KEY_RIGHT)
            7 -> finish()
        }
    }

    private fun pickAction(prefKey: String) {
        val actions = ShortcutAction.entries

        AlertDialog.Builder(this)
            .setTitle("Shortcut action")
            .setItems(actions.map { it.displayName }.toTypedArray()) { _, which ->
                if (actions[which] == ShortcutAction.OPEN_ACTIVITY) {
                    currentPrefKey = prefKey
                    pickShortcutApp.launch(
                        Intent(this, LauncherActivity::class.java).apply {
                            putExtra(LauncherActivity.EXTRA_PICK_MODE, true)
                        }
                    )
                } else {
                    prefs.setShortcut(prefKey, actions[which])
                    refreshRows()
                }
            }
            .show()
    }
}
