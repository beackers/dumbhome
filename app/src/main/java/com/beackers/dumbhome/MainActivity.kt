package com.beackers.dumbhome

import android.Manifest
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.app.PendingIntent          
import android.provider.Settings

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.beackers.dumbhome.notifications.NotificationStore
import com.beackers.dumbhome.notifications.NotificationRow
import com.beackers.dumbhome.notifications.NotificationAdapter


class MainActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private lateinit var wallpaper: ImageView
    private lateinit var shade: View
    private lateinit var notificationList: RecyclerView

    private val openFilePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data ?: return@registerForActivityResult
        prefs.setWallpaper(uri)
        loadWallpaper()
    }

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
        if (!hasNotificationAccess()) {
          requestNotificationsPermissions()
          return
        }
        val rows = NotificationStore.rows(this)
          .ifEmpty { listOf(NotificationRow(
            key = "",
            appName = "",
            title = "",
            text = "All caught up :)",
            intent = null
          )) }
        notificationList.adapter = NotificationAdapter(rows)
        shade.visibility = View.VISIBLE
        shade.requestFocus()
        return
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
        return
    }

    private fun loadWallpaper() {
        val uri = prefs.getWallpaperUri()
        if (uri == null) {
            wallpaper.setImageDrawable(null)
            return
        }
        runCatching {
            contentResolver.openInputStream(uri)?.use { stream ->
                wallpaper.setImageBitmap(BitmapFactory.decodeStream(stream))
            }
        }
        return
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
        return
    }

    fun openWallpaperPicker() {
        openFilePicker.launch(Intent(this, FilePickerActivity::class.java))
        return
    }

    fun openLiveWallpaperPicker() {
        try {
            startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER))
        }
    }

    private fun hasNotificationAccess(): Boolean {
      val enabled = Settings.Secure.getString(
        contentResolver,
        "enabled_notification_listeners"
      ) ?: return false
      return enabled.contains(packageName)
    }

    private fun requestNotificationsPermissions() {
      AlertDialog.Builder(this)
        .setTitle("DumbHome is requesting permissions")
        .setMessage("DumbHome is requestiong access to read your notifications. \nDumbHome does not collect or share your information.")
        .setPositiveButton("Open settings", { _, _ ->
      startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
        .setNegativeButton("Cancel", null)
        .show()
    }
}
