package com.beackers.dumbhome

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import android.widget.ImageView
import android.app.PendingIntent          
import android.provider.Settings
import android.provider.MediaStore
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.text.format.DateFormat

import androidx.activity.result.contract.ActivityResultContracts
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.beackers.dumbhome.notifications.NotificationStore
import com.beackers.dumbhome.notifications.NotificationRow
import com.beackers.dumbhome.notifications.NotificationAdapter
import com.beackers.dumbhome.launcher.LauncherActivity

import java.util.Locale
import java.util.TimeZone
import java.util.Date

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private lateinit var wallpaper: ImageView
    private lateinit var shade: View
    private lateinit var clockView: TextView
    private lateinit var utcView: TextView
    private lateinit var dateView: TextView
    private lateinit var notificationList: RecyclerView
    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // if adding summary,
            // always update summary.
            // update shade live if it's open.
            if (shade.visibility == View.VISIBLE) {
                notificationList.adapter = NotificationAdapter(NotificationStore.rows(this@MainActivity))
            }
        }
    }
    private val handler = Handler(Looper.getMainLooper())

    private val utcFmt = SimpleDateFormat("HH:mm:ss 'UTC'", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
    private val clockRunnable = object : Runnable {
      override fun run() {
        val now = Date()
        val time = DateFormat.format("HH:mm:ss", now)
        val date = DateFormat.format("EEE, MMM d yyyy", now)
        val utc = utcFmt.format(Date())

        clockView.text = time
        utcView.text = utc
        dateView.text = date
        val delay = 1000 - (System.currentTimeMillis() % 1000)
        handler.postDelayed(this, delay)
      }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.setDecorFitsSystemWindows(false)

        setContentView(R.layout.activity_main)

        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        prefs = Prefs(this)
        prefs.initializeDefaultsIfNeeded()

        wallpaper = findViewById(R.id.backgroundImage)
        shade = findViewById(R.id.notificationShade)
        notificationList = findViewById(R.id.notificationList)
        notificationList.layoutManager = LinearLayoutManager(this)
        clockView = findViewById(R.id.clockText)
        utcView = findViewById(R.id.utcText)
        dateView = findViewById(R.id.dateText)

        loadWallpaper()
        ensurePermissions()

    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, IntentFilter("com.beackers.dumbhome.NOTIFICATIONS_UPDATED"))
        loadWallpaper()
        handler.post(clockRunnable)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
        handler.removeCallbacks(clockRunnable)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (shade.visibility == View.VISIBLE && keyCode == KeyEvent.KEYCODE_BACK) {
            shade.visibility = View.GONE
            return true
        }

        if (shade.visibility == View.VISIBLE && keyCode != KeyEvent.KEYCODE_BACK) {
            return super.onKeyDown(keyCode, event)
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
            runShortcut(shortcut, keyCode)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun runShortcut(action: ShortcutAction, keyCode: Int) {
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
            ShortcutAction.OPEN_ACTIVITY -> {
                val prefKey = when (keyCode) {
                    KeyEvent.KEYCODE_F11 -> Prefs.KEY_F11
                    KeyEvent.KEYCODE_MENU -> Prefs.KEY_MENU
                    KeyEvent.KEYCODE_DPAD_UP -> Prefs.KEY_UP
                    KeyEvent.KEYCODE_DPAD_DOWN -> Prefs.KEY_DOWN
                    KeyEvent.KEYCODE_DPAD_LEFT -> Prefs.KEY_LEFT
                    KeyEvent.KEYCODE_DPAD_RIGHT -> Prefs.KEY_RIGHT
                    else -> null
                }
                val packageName = prefKey?.let { prefs.getShortcutApp(it) }
                if (packageName != null) {
                  val intent = packageManager.getLaunchIntentForPackage(packageName)
                  intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                  if (intent != null) {
                    startActivity(intent)
                  } else {
                    Toast.makeText(this, "App not found", Toast.LENGTH_SHORT).show()
                  }
                }
            }
            ShortcutAction.OPEN_ASSISTANT -> {
                val intent = Intent(Intent.ACTION_ASSIST)
                startActivity(intent)
            }
            ShortcutAction.OPEN_CAMERA -> {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivity(intent)
            }
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
        startActivity(Intent(this, LauncherActivity::class.java))
        return
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
        return
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
