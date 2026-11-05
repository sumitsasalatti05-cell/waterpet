package com.waterpet.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Restarts reminders after a reboot if the user had them turned on. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED &&
            Prefs(context).enabled && Settings.canDrawOverlays(context)
        ) {
            ContextCompat.startForegroundService(
                context, Intent(context, ReminderService::class.java)
            )
        }
    }
}
