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
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import android.media.session.MediaController
import android.media.session.MediaSessionManager
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
import com.beackers.dumbhome.notifications.DumbNotificationListener
import com.beackers.dumbhome.launcher.LauncherActivity
import com.beackers.dumbhome.openapps.OpenAppsActivity

import java.util.Locale
import java.util.TimeZone
import java.util.Date
import java.text.SimpleDateFormat

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private lateinit var wallpaper: ImageView
    private lateinit var shade: View
    private lateinit var clockView: TextView
    private lateinit var utcView: TextView
    private lateinit var dateView: TextView
    private lateinit var lockScreen: View
    private lateinit var lockBackground: ImageView
    private lateinit var lockNowPlaying: TextView
    private lateinit var lockMusicIcon: TextView
    private lateinit var lockPrompt: TextView
    private lateinit var lockChallenge: TextView
    private var lockChallengeStarted = false
    private var dpadProgress = mutableListOf<String>()
    private var pinProgress = ""
    private var shouldLockOnResume = false

    private var cellLevel: Int? = null
    private var cellDbm: Int? = null
    private var cellType: String = ""
    private var wifiSsid: String? = null
    private var wifiDbm: Int? = null

    private lateinit var notificationList: RecyclerView
    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // if adding summary,
            // always update summary.
            // update shade live if it's open.
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                shouldLockOnResume = prefs.getLockStrength() != LockStrength.NONE
                return
            }
            if (shade.visibility == View.VISIBLE) {
                notificationList.adapter = NotificationAdapter(NotificationStore.rows(this@MainActivity))
            }
        }
    }
    private val handler = Handler(Looper.getMainLooper())

    private val utcFmt = SimpleDateFormat("HH:mm:ss 'UTC'", Locale.US).apply {
        this.timeZone = TimeZone.getTimeZone("UTC")
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

        // Keep these so the image draws
        // behind the transparent nav/status
        // bars.
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.setDecorFitsSystemWindows(false)

        setContentView(R.layout.activity_main)

        // actually make the nav bar transparent
        // may need to add something to
        // contrast the status bar
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        prefs = Prefs(this)
        prefs.initializeDefaultsIfNeeded()

        wallpaper = findViewById(R.id.backgroundImage)
        shade = findViewById(R.id.notificationShade)
        notificationList = findViewById(R.id.notificationList)
        notificationList.layoutManager = LinearLayoutManager(this)

        // focus when unscrollable
        notificationList.isFocusable = true
        notificationList.isFocusableInTouchMode = true
        notificationList.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        notificationList.overScrollMode = View.OVER_SCROLL_ALWAYS
        notificationList.setOnFocusChangeListener { _, hasFocus ->
          if (hasFocus && notificationList.childCount > 0) {
            notificationList.getChildAt(0).requestFocus()
          }
        }

        clockView = findViewById(R.id.clockText)
        utcView = findViewById(R.id.utcText)
        dateView = findViewById(R.id.dateText)
        lockScreen = findViewById(R.id.lockScreen)
        lockBackground = findViewById(R.id.lockBackgroundImage)
        lockNowPlaying = findViewById(R.id.lockNowPlaying)
        lockMusicIcon = findViewById(R.id.lockMusicIcon)
        lockPrompt = findViewById(R.id.lockPrompt)
        lockChallenge = findViewById(R.id.lockChallenge)

        loadWallpaper()
        ensurePermissions()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, IntentFilter().apply {
            addAction("com.beackers.dumbhome.NOTIFICATIONS_UPDATED")
            addAction(Intent.ACTION_SCREEN_OFF)
        })
        loadWallpaper()
        if (prefs.getLockStrength() == LockStrength.NONE) {
            hideLockScreen()
        } else if (shouldLockOnResume) {
            showLockScreen()
        }
        handler.post(clockRunnable)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
        handler.removeCallbacks(clockRunnable)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (lockScreen.visibility == View.VISIBLE) {
            return handleLockKey(keyCode)
        }

        if (shade.visibility == View.VISIBLE && keyCode == KeyEvent.KEYCODE_BACK) {
            shade.visibility = View.GONE
            return true
        }

        if (shade.visibility == View.VISIBLE && keyCode == KeyEvent.KEYCODE_MENU) {
            DumbNotificationListener.instance?.clearAll()
            return true
        }

        if (shade.visibility == View.VISIBLE && (keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_MENU)) {
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
            ShortcutAction.OPEN_OPEN_APPS -> startActivity(Intent(this, OpenAppsActivity::class.java))
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
        notificationList.post {
          if (notificationList.childCount > 0) {
            notificationList.getChildAt(0).requestFocus()
          }
        }
        return
    }

    private fun showAppLauncher() {
        startActivity(Intent(this, LauncherActivity::class.java))
        return
    }

    private fun loadWallpaper() {
        wallpaper.setImageBitmap(WallpaperStorage.load(this))
    }

    private fun showLockScreen() {
        lockChallengeStarted = false
        dpadProgress.clear()
        pinProgress = ""
        refreshLockBackground()
        lockPrompt.text = "Press any button to unlock"
        lockChallenge.visibility = View.GONE
        lockScreen.visibility = View.VISIBLE
        lockScreen.requestFocus()
    }

    private fun hideLockScreen() {
        lockScreen.visibility = View.GONE
        shouldLockOnResume = false
    }

    private fun refreshLockBackground() {
        val media = currentMedia()
        val art = if (prefs.useMediaArtOnLockScreen()) media?.metadata?.description?.iconBitmap else null
        lockBackground.setImageBitmap(art ?: WallpaperStorage.load(this, WallpaperStorage.WallpaperTarget.LOCK) ?: WallpaperStorage.load(this))
        val title = media?.metadata?.description?.title?.toString().orEmpty()
        val subtitle = media?.metadata?.description?.subtitle?.toString().orEmpty()
        val showMedia = prefs.useMediaArtOnLockScreen() && (title.isNotBlank() || subtitle.isNotBlank())
        lockNowPlaying.text = listOf(title, subtitle).filter { it.isNotBlank() }.joinToString("\n")
        lockNowPlaying.visibility = if (showMedia) View.VISIBLE else View.GONE
        lockMusicIcon.visibility = if (prefs.useMediaArtOnLockScreen() && art == null) View.VISIBLE else View.GONE
    }

    private fun currentMedia(): MediaController? {
        if (!hasNotificationAccess()) return null
        val manager = getSystemService(MediaSessionManager::class.java)
        return manager.getActiveSessions(android.content.ComponentName(this, DumbNotificationListener::class.java)).firstOrNull()
    }

    private fun handleLockKey(keyCode: Int): Boolean {
        if (!lockChallengeStarted) {
            lockChallengeStarted = true
            lockChallenge.visibility = View.VISIBLE
            lockPrompt.text = when (prefs.getLockStrength()) {
                LockStrength.NONE -> "Unlocked"
                LockStrength.DPAD_SEQUENCE -> "Enter D-pad sequence"
                LockStrength.PIN -> "Enter PIN"
            }
            if (prefs.getLockStrength() == LockStrength.NONE) hideLockScreen()
            return true
        }
        when (prefs.getLockStrength()) {
            LockStrength.NONE -> hideLockScreen()
            LockStrength.DPAD_SEQUENCE -> handleDpadChallenge(keyCode)
            LockStrength.PIN -> handlePinChallenge(keyCode)
        }
        return true
    }

    private fun handleDpadChallenge(keyCode: Int) {
        val token = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> "↑"
            KeyEvent.KEYCODE_DPAD_DOWN -> "↓"
            KeyEvent.KEYCODE_DPAD_LEFT -> "←"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "→"
            else -> null
        } ?: return
        dpadProgress += token
        val expected = prefs.getLockDpadSequence().split(" ").filter { it.isNotBlank() }
        lockChallenge.text = dpadProgress.joinToString(" ")
        if (dpadProgress == expected) hideLockScreen()
        if (expected.take(dpadProgress.size) != dpadProgress || dpadProgress.size > expected.size) {
            dpadProgress.clear()
            lockChallenge.text = "Try again"
        }
    }

    private fun handlePinChallenge(keyCode: Int) {
        val digit = keyCode - KeyEvent.KEYCODE_0
        if (digit !in 0..9) return
        pinProgress += digit.toString()
        lockChallenge.text = "•".repeat(pinProgress.length)
        val expected = prefs.getLockPin()
        if (pinProgress == expected) hideLockScreen()
        if (!expected.startsWith(pinProgress) || pinProgress.length > expected.length) {
            pinProgress = ""
            lockChallenge.text = "Try again"
        }
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
        .setMessage("DumbHome is requesting access to read your notifications. \nDumbHome does not collect or share your information. \n To allow notification access, click 'Open settings' and tap DumbHome, then select Allowed and return home.")
        .setPositiveButton("Open settings", { _, _ ->
      startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
        .setNegativeButton("Cancel", null)
        .show()
    }
}
