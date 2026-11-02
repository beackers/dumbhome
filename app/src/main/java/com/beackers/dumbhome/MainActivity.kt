package com.beackers.dumbhome

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.MediaStore
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.beackers.dumbhome.notifications.NotificationStore
import com.beackers.dumbhome.notifications.NotificationAdapter
import com.beackers.dumbhome.notifications.DumbNotificationListener
import com.beackers.dumbhome.notifications.NotificationEvents
import com.beackers.dumbhome.notifications.NotificationMapper
import com.beackers.dumbhome.launcher.LauncherActivity


class MainActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private lateinit var wallpaper: ImageView
    private lateinit var shade: View
    private lateinit var recentAppsShade: View
    private lateinit var clockView: TextView
    private lateinit var utcView: TextView
    private lateinit var dateView: TextView
    private lateinit var clockController: ClockController
    private lateinit var notificationAdapter: NotificationAdapter
    private val recentAppsRepository by lazy { RecentAppsRepository(this) }

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (shade.visibility == View.VISIBLE) {
                refreshNotificationList()
            }
        }
    }

    private val emergencySequence = EmergencySequence()

    private lateinit var notificationList: RecyclerView
    private lateinit var recentCenterIcon: ImageView
    private lateinit var recentLeftIcon: ImageView
    private lateinit var recentRightIcon: ImageView
    private lateinit var recentUpIcon: ImageView
    private lateinit var recentDownIcon: ImageView

    private var centerRecentApp: RecentApp? = null
    private var leftRecentApp: RecentApp? = null
    private var rightRecentApp: RecentApp? = null
    private var upRecentApp: RecentApp? = null
    private var downRecentApp: RecentApp? = null

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
        recentAppsShade = findViewById(R.id.recentAppsShade)

        notificationList = findViewById(R.id.notificationList)
        notificationList.layoutManager = LinearLayoutManager(this)
        notificationList.isFocusable = true
        notificationList.isFocusableInTouchMode = true
        notificationList.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        notificationList.overScrollMode = View.OVER_SCROLL_ALWAYS
        notificationList.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && notificationList.childCount > 0) {
                notificationList.getChildAt(0).requestFocus()
            }
        }

        recentCenterIcon = findViewById(R.id.recentCenterIcon)
        recentLeftIcon = findViewById(R.id.recentLeftIcon)
        recentRightIcon = findViewById(R.id.recentRightIcon)
        recentUpIcon = findViewById(R.id.recentUpIcon)
        recentDownIcon = findViewById(R.id.recentDownIcon)

        clockView = findViewById(R.id.clockText)
        utcView = findViewById(R.id.utcText)
        dateView = findViewById(R.id.dateText)
        clockController = ClockController(clockView, utcView, dateView)

        notificationAdapter = NotificationAdapter {
            DumbNotificationListener.instance?.clearNotification(it)
        }
        notificationList.adapter = notificationAdapter

        loadWallpaper()
        ensurePermissions()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(notificationReceiver, IntentFilter(NotificationEvents.ACTION_UPDATED))
        loadWallpaper()
        clockController.start()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(notificationReceiver)
        clockController.stop()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (recentAppsShade.visibility == View.VISIBLE) {
            val app = when (keyCode) {
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_DPAD_CENTER -> centerRecentApp
                KeyEvent.KEYCODE_DPAD_RIGHT -> rightRecentApp
                KeyEvent.KEYCODE_DPAD_LEFT -> leftRecentApp
                KeyEvent.KEYCODE_DPAD_DOWN -> downRecentApp
                KeyEvent.KEYCODE_DPAD_UP -> upRecentApp
                KeyEvent.KEYCODE_BACK,
                KeyEvent.KEYCODE_MENU -> null
                else -> return true
            }

            if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
                recentAppsShade.visibility = View.GONE
                return true
            }

            app?.let {
                recentAppsShade.visibility = View.GONE
                startActivity(it.intent)
                return true
            }
            return true
        }

        if (shade.visibility == View.VISIBLE && keyCode == KeyEvent.KEYCODE_BACK) {
            shade.visibility = View.GONE
            return true
        }

        if (shade.visibility == View.VISIBLE && keyCode == KeyEvent.KEYCODE_MENU) {
            DumbNotificationListener.instance?.clearAll()
            refreshNotificationList()
            return true
        }

        if (shade.visibility == View.VISIBLE) {
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
            KeyEvent.KEYCODE_9 -> stepToEmergencyDial()
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
            ShortcutAction.OPEN_OPEN_APPS -> toggleRecentApps()
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
                        Toast.makeText(this, R.string.app_not_found, Toast.LENGTH_SHORT).show()
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
        submitNotificationRows()
        shade.visibility = View.VISIBLE
        focusFirstNotification()
    }

    private fun refreshNotificationList() {
        submitNotificationRows()
        focusFirstNotification()
    }

    private fun submitNotificationRows() {
        val rows = NotificationMapper.rows(this, NotificationStore.snapshot())
            .ifEmpty { listOf(NotificationMapper.emptyRow()) }
        notificationAdapter.submitList(rows)
    }

    private fun focusFirstNotification() {
        notificationList.post {
            if (notificationList.childCount > 0) {
                notificationList.getChildAt(0).requestFocus()
            } else {
                notificationList.requestFocus()
            }
        }
    }

    private fun toggleRecentApps() {
        if (recentAppsShade.visibility == View.VISIBLE) {
            recentAppsShade.visibility = View.GONE
            return
        }

        if (!recentAppsRepository.hasUsageAccess()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            return
        }

        val apps = recentAppsRepository.getTopRecentApps()
        centerRecentApp = apps.getOrNull(0)
        rightRecentApp = apps.getOrNull(1)
        downRecentApp = apps.getOrNull(2)
        leftRecentApp = apps.getOrNull(3)
        upRecentApp = apps.getOrNull(4)

        recentCenterIcon.setImageDrawable(centerRecentApp?.icon)
        recentLeftIcon.setImageDrawable(leftRecentApp?.icon)
        recentRightIcon.setImageDrawable(rightRecentApp?.icon)
        recentUpIcon.setImageDrawable(upRecentApp?.icon)
        recentDownIcon.setImageDrawable(downRecentApp?.icon)

        recentCenterIcon.visibility = if (centerRecentApp != null) View.VISIBLE else View.INVISIBLE
        recentLeftIcon.visibility = if (leftRecentApp != null) View.VISIBLE else View.INVISIBLE
        recentRightIcon.visibility = if (rightRecentApp != null) View.VISIBLE else View.INVISIBLE
        recentUpIcon.visibility = if (upRecentApp != null) View.VISIBLE else View.INVISIBLE
        recentDownIcon.visibility = if (downRecentApp != null) View.VISIBLE else View.INVISIBLE

        recentAppsShade.visibility = View.VISIBLE
        recentCenterIcon.requestFocus()
    }

    private fun showAppLauncher() {
        startActivity(Intent(this, LauncherActivity::class.java))
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissions += Manifest.permission.CALL_PHONE
        }
        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), 11)
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
            .setTitle(R.string.notification_permission_title)
            .setMessage("DumbHome is requesting access to read your notifications. \nDumbHome does not collect or share your information. \n To allow notification access, click 'Open settings' and tap DumbHome, then select Allowed and return home.")
            .setPositiveButton(R.string.open_settings) { _, _ ->
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun stepToEmergencyDial(): ShortcutAction? {
        if (emergencySequence.press()) {
            startActivity(Intent(this, ChallengeActivity::class.java))
        }
        return null
    }
}

