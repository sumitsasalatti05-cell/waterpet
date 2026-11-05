package com.waterpet.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.ServiceCompat

/**
 * Foreground service that keeps the app alive and triggers the pet
 * every N minutes (only when the screen is on, inside active hours,
 * and not during calls / Do Not Disturb).
 */
class ReminderService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val tick = Runnable { onTick() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = Prefs(this)
        if (intent?.action == ACTION_STOP) {
            prefs.enabled = false
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        prefs.enabled = true
        startInForeground(prefs)
        schedule(prefs.intervalMin)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        PetOverlay.hide(animated = false)
        super.onDestroy()
    }

    private fun schedule(minutes: Int) {
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, minutes * 60_000L)
    }

    private fun onTick() {
        val prefs = Prefs(this)
        schedule(prefs.intervalMin) // next regular reminder
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isInteractive && prefs.isActiveNow() && !inCall() && !doNotDisturb()) {
            // "Later" replaces the next reminder with a short snooze
            PetOverlay.show(this) { schedule(SNOOZE_MIN) }
        }
    }

    private fun inCall(): Boolean {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.mode == AudioManager.MODE_IN_CALL ||
            am.mode == AudioManager.MODE_IN_COMMUNICATION
    }

    private fun doNotDisturb(): Boolean {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.currentInterruptionFilter in
            NotificationManager.INTERRUPTION_FILTER_PRIORITY..NotificationManager.INTERRUPTION_FILTER_ALARMS
    }

    private fun startInForeground(prefs: Prefs) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Pet is watching", NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, ReminderService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("${prefs.petEmoji} ${prefs.petName} is keeping watch")
            .setContentText("Reminding you to drink water every ${prefs.intervalMin} min")
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "Stop", stop).build())
            .setOngoing(true)
            .build()
        val type = if (Build.VERSION.SDK_INT >= 34) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else 0
        ServiceCompat.startForeground(this, NOTIF_ID, notification, type)
    }

    companion object {
        const val ACTION_STOP = "com.waterpet.app.STOP"
        private const val CHANNEL_ID = "waterpet_service"
        private const val NOTIF_ID = 1
        private const val SNOOZE_MIN = 10
    }
}
