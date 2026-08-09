package com.simpleflashlight

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock

/**
 * Turns the light off by itself after a while, so a forgotten flashlight cannot drain the
 * battery. Backed by an alarm rather than a running service: nothing of the app stays alive
 * while the torch is on.
 */
object AutoOff {

    private const val REQUEST_CODE = 1001

    fun schedule(context: Context) {
        cancel(context)

        val minutes = Prefs.autoOffMinutes(context)
        if (minutes <= 0) return

        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val firesAt = SystemClock.elapsedRealtime() + minutes * 60_000L
        val intent = pendingIntent(context)

        // Exact alarms need a user-granted permission from Android 12 onwards. Being a couple of
        // minutes late is harmless here, so fall back instead of sending the user to settings.
        val exactAllowed =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

        runCatching {
            if (exactAllowed) {
                alarms.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP, firesAt, intent
                )
            } else {
                alarms.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP, firesAt, intent
                )
            }
        }
    }

    fun cancel(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        alarms.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context.applicationContext, FlashWidgetProvider::class.java)
            .setAction(FlashWidgetProvider.ACTION_AUTO_OFF)
        return PendingIntent.getBroadcast(
            context.applicationContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
