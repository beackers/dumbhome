package com.beackers.dumbhome

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.beackers.dumbhome.launcher.LauncherActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private var currentPrefKey: String? = null
    private var pendingWallpaperTarget = WallpaperStorage.WallpaperTarget.HOME
    private lateinit var list: RecyclerView
    private lateinit var adapter: SimpleTextAdapter
    private val rows = mutableListOf<String>()

    private val cropWallpaper = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshRows()
    }

    private val pickWallpaperLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        cropWallpaper.launch(
            Intent(this, WallpaperCropActivity::class.java)
                .putExtra(WallpaperCropActivity.EXTRA_IMAGE_URI, uri)
                .putExtra(WallpaperCropActivity.EXTRA_TARGET, pendingWallpaperTarget.name),
        )
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK) {
          val packageName = data?.getStringExtra("package") ?: return
          val key = currentPrefKey ?: return
          prefs.setShortcut(key, ShortcutAction.OPEN_ACTIVITY)
          prefs.setShortcutApp(key, packageName)
          refreshRows()
      }
    }

    private fun refreshRows() {
        rows.clear()
        rows += "Change home image"
        rows += "Lock strength (${prefs.getLockStrength().displayName})"
        rows += "Change lock image"
        rows += "Lock media art (${if (prefs.useMediaArtOnLockScreen()) "on" else "off"})"
        rows += "Set D-pad unlock sequence (${prefs.getLockDpadSequence()})"
        rows += "Set PIN unlock (${"•".repeat(prefs.getLockPin().length)})"
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
            0 -> pickWallpaper(WallpaperStorage.WallpaperTarget.HOME)
            1 -> pickLockStrength()
            2 -> pickWallpaper(WallpaperStorage.WallpaperTarget.LOCK)
            3 -> toggleLockMediaArt()
            4 -> editDpadSequence()
            5 -> editPin()
            6 -> pickAction(Prefs.KEY_F11)
            7 -> pickAction(Prefs.KEY_MENU)
            8 -> pickAction(Prefs.KEY_UP)
            9 -> pickAction(Prefs.KEY_DOWN)
            10 -> pickAction(Prefs.KEY_LEFT)
            11 -> pickAction(Prefs.KEY_RIGHT)
            12 -> finish()
        }
    }

    private fun pickWallpaper(target: WallpaperStorage.WallpaperTarget) {
        pendingWallpaperTarget = target
        pickWallpaperLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun pickLockStrength() {
        val strengths = LockStrength.entries
        AlertDialog.Builder(this)
            .setTitle("Lock strength")
            .setItems(strengths.map { it.displayName }.toTypedArray()) { _, which ->
                prefs.setLockStrength(strengths[which])
                refreshRows()
            }
            .show()
    }

    private fun toggleLockMediaArt() {
        prefs.setUseMediaArtOnLockScreen(!prefs.useMediaArtOnLockScreen())
        refreshRows()
    }

    private fun editDpadSequence() {
        val input = android.widget.EditText(this).apply {
            setText(prefs.getLockDpadSequence())
            hint = "Example: ↑ ↓ ← →"
        }
        AlertDialog.Builder(this)
            .setTitle("D-pad sequence")
            .setMessage("Use ↑ ↓ ← → tokens separated by spaces.")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                prefs.setLockDpadSequence(input.text.toString().ifBlank { Prefs.DEFAULT_DPAD_SEQUENCE })
                refreshRows()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun editPin() {
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setText(prefs.getLockPin())
        }
        AlertDialog.Builder(this)
            .setTitle("PIN unlock")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                prefs.setLockPin(input.text.toString().ifBlank { Prefs.DEFAULT_PIN })
                refreshRows()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun pickAction(prefKey: String) {
        val actions = ShortcutAction.entries
        AlertDialog.Builder(this)
            .setTitle("Shortcut action")
            .setItems(actions.map { it.displayName }.toTypedArray()) { _, which ->
                if (actions[which] == ShortcutAction.OPEN_ACTIVITY) {
                    currentPrefKey = prefKey
                    pickApp(prefKey)
                } else {
                    prefs.setShortcut(prefKey, actions[which])
                    refreshRows()
                }
            }
            .show()
    }

    private fun pickApp(prefKey: String) {
        val intent = Intent(this, LauncherActivity::class.java)
        intent.putExtra("pick_mode", true)
        startActivityForResult(intent, 100)
    }
}
