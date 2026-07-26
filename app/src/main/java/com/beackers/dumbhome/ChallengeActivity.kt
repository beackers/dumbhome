package com.beackers.dumbhome

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.KeyEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class ChallengeActivity : AppCompatActivity() {
    private lateinit var countdownNumber: TextView
    private lateinit var countdownProgress: ProgressBar
    private lateinit var timer: CountDownTimer
    private var isCancelled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        setContentView(R.layout.activity_challenge)

        countdownNumber = findViewById(R.id.countdownNumber)
        countdownProgress = findViewById(R.id.countdownProgress)
        countdownProgress.max = CHALLENGE_MILLIS.toInt()
        countdownProgress.progress = CHALLENGE_MILLIS.toInt()

        timer = object : CountDownTimer(CHALLENGE_MILLIS, TICK_MILLIS) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = ((millisUntilFinished + 999L) / 1000L).toInt()
                countdownNumber.text = secondsLeft.toString()
                countdownProgress.progress = millisUntilFinished.toInt()
            }

            override fun onFinish() {
                countdownNumber.text = "0"
                countdownProgress.progress = 0
                dialEmergency()
            }
        }.start()
    }

    override fun onDestroy() {
        timer.cancel()
        super.onDestroy()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            cancelEmergencyDial()
            return true
        }
        return true
    }

    private fun cancelEmergencyDial() {
        if (isCancelled) return
        isCancelled = true
        timer.cancel()
        Toast.makeText(this, "Emergency dial cancelled", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun dialEmergency() {
        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse(EMERGENCY_NUMBER_URI))
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            startActivity(callIntent)
        } else {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(EMERGENCY_NUMBER_URI)))
        }
        finish()
    }

    companion object {
        private const val CHALLENGE_MILLIS = 5_000L
        private const val TICK_MILLIS = 100L
        private const val EMERGENCY_NUMBER_URI = "tel:911"
    }
}
