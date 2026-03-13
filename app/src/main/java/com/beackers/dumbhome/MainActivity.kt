package com.beackers.dumbhome

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beackers.dumbhome.notifications.NotificationStore

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private lateinit var wallpaper: ImageView
    private lateinit var shade: View
    private lateinit var notificationList: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = Prefs(this)
        prefs.initializeDefaultsIfNeeded()

        wallpaper = findViewById(R.id.backgroundImage)
        shade = findViewById(R.id.notificationShade)
        notificationList = findViewById(R.id.notificationList)
        notificationList.layoutManager = LinearLayoutManager(this)

        loadWallpaper()
        ensurePermissions()
        ensureNotificationAccess()
    }

    override fun onResume() {
        super.onResume()
        loadWallpaper()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (shade.visibility == View.VISIBLE && keyCode == KeyEvent.KEYCODE_BACK) {
            shade.visibility = View.GONE
            return true
        }

        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            showAppLauncher()
            return true
        }

        val shortcut = when (keyCode) {
            KeyEvent.KEYCODE_F11 -> prefs.getShortcut(Prefs.KEY_F11)
            KeyEvent.KEYCODE_MENU -> prefs.getShortcut(Prefs.KEY_MENU)
            KeyEvent.KEYCODE_DPAD_UP -> prefs.getShortcut(Prefs.KEY_UP)
            KeyEvent.KEYCODE_DPAD_DOWN -> prefs.getShortcut(Prefs.KEY_DOWN)
            KeyEvent.KEYCODE_DPAD_LEFT -> prefs.getShortcut(Prefs.KEY_LEFT)
            KeyEvent.KEYCODE_DPAD_RIGHT -> prefs.getShortcut(Prefs.KEY_RIGHT)
            else -> null
        }
        if (shortcut != null) {
            runShortcut(shortcut)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun runShortcut(action: ShortcutAction) {
        when (action) {
            ShortcutAction.OPEN_NOTIFICATIONS -> toggleNotifications()
            ShortcutAction.OPEN_SETTINGS_APP -> {
                val launch = packageManager.getLaunchIntentForPackage("com.android.settings")
                    ?: Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launch)
            }
            ShortcutAction.OPEN_DUMBHOME_SETTINGS -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            ShortcutAction.OPEN_APP_LAUNCHER -> showAppLauncher()
            ShortcutAction.NONE -> Unit
        }
    }

    private fun toggleNotifications() {
        if (shade.visibility == View.VISIBLE) {
            shade.visibility = View.GONE
            return
        }
        val rows = NotificationStore.list().map {
            "${it.packageName}: ${it.title} ${it.text}".trim()
        }.ifEmpty { listOf("No notifications. Enable notification access in system settings.") }
        notificationList.adapter = SimpleTextAdapter(rows)
        shade.visibility = View.VISIBLE
        shade.requestFocus()
    }

    private fun showAppLauncher() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(intent, 0).sortedBy { it.loadLabel(pm).toString().lowercase() }
        val labels = apps.map { it.loadLabel(pm).toString() }

        AlertDialog.Builder(this)
            .setTitle("Launch app")
            .setItems(labels.toTypedArray()) { _, which ->
                val pkg = apps[which].activityInfo.packageName
                pm.getLaunchIntentForPackage(pkg)?.let { launchIntent ->
                    startActivity(launchIntent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadWallpaper() {
        wallpaper.setImageBitmap(WallpaperStorage.load(this))
    }

    private fun ensurePermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions += Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), 11)
        }
    }

    private fun ensureNotificationAccess() {
        if (hasNotificationListenerAccess() || hasAccessibilityNotificationAccess()) {
            return
        }

        val listenerIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        if (listenerIntent.resolveActivity(packageManager) != null) {
            Toast.makeText(this, "Enable DumbHome notification access.", Toast.LENGTH_LONG).show()
            startActivity(listenerIntent)
            return
        }

        val accessibilityIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        if (accessibilityIntent.resolveActivity(packageManager) != null) {
            Toast.makeText(
                this,
                "Enable DumbHome Notification Accessibility service to read notifications.",
                Toast.LENGTH_LONG
            ).show()
            startActivity(accessibilityIntent)
        }
    }

    private fun hasNotificationListenerAccess(): Boolean {
        val listeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return listeners.contains(packageName)
    }

    private fun hasAccessibilityNotificationAccess(): Boolean {
        val enabled = Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1
        if (!enabled) {
            return false
        }

        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val expectedService = TextUtils.SimpleStringSplitter(':')
        expectedService.setString(enabledServices)
        val target = "${packageName}/${com.beackers.dumbhome.notifications.DumbNotificationAccessibilityService::class.java.name}"
        while (expectedService.hasNext()) {
            if (expectedService.next().equals(target, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun openWallpaperPicker() {
        openFilePicker.launch(Intent(this, FilePickerActivity::class.java))
    }

    fun openLiveWallpaperPicker() {
        try {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER))
        }
    }
}
