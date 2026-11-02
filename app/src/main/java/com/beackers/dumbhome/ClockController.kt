package com.beackers.dumbhome

import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ClockController(
    private val clockView: TextView,
    private val utcView: TextView,
    private val dateView: TextView
) {
    private val handler = Handler(Looper.getMainLooper())
    private val utcFormat = SimpleDateFormat("HH:mm:ss 'UTC'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            val now = Date()
            clockView.text = DateFormat.format("HH:mm:ss", now)
            utcView.text = utcFormat.format(now)
            dateView.text = DateFormat.format("EEE, MMM d yyyy", now)

            val delay = 1000L - (System.currentTimeMillis() % 1000L)
            handler.postDelayed(this, delay)
        }
    }

    fun start() {
        handler.removeCallbacks(updateRunnable)
        updateRunnable.run()
    }

    fun stop() {
        handler.removeCallbacks(updateRunnable)
    }
}
