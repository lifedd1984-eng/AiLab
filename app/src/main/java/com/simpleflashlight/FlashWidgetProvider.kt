package com.simpleflashlight

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat

/**
 * Home screen widget. One tap toggles the light without opening anything — the tap arrives here
 * as a broadcast, the torch flips, and the widget redraws itself.
 */
class FlashWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Draw immediately from the remembered state; onReceive follows up with the real one.
        val views = buildViews(context, Prefs.lastTorchState(context))
        appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE -> {
                val pending = goAsync()
                Flashlight.toggle(context) { pending.finish() }
            }

            ACTION_AUTO_OFF -> Flashlight.apply(context, false)

            else -> {
                super.onReceive(context, intent)
                if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
                    val pending = goAsync()
                    Torch.readState(context) { isOn ->
                        render(context, isOn)
                        pending.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.simpleflashlight.action.TOGGLE"
        const val ACTION_AUTO_OFF = "com.simpleflashlight.action.AUTO_OFF"

        private const val TOGGLE_REQUEST_CODE = 1

        /** Redraws every placed widget in the given state. Safe to call from anywhere. */
        fun render(context: Context, isOn: Boolean) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context.applicationContext, FlashWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return
            val views = buildViews(context, isOn)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun buildViews(context: Context, isOn: Boolean): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_flash)

            views.setInt(
                R.id.widget_root,
                "setBackgroundResource",
                if (isOn) R.drawable.widget_bg_on else R.drawable.widget_bg_off
            )
            views.setImageViewResource(
                R.id.widget_icon,
                if (isOn) R.drawable.ic_flash_dark else R.drawable.ic_flash_amber
            )
            views.setTextViewText(
                R.id.widget_label,
                context.getString(if (isOn) R.string.state_on else R.string.state_off)
            )
            views.setTextColor(
                R.id.widget_label,
                ContextCompat.getColor(
                    context,
                    if (isOn) R.color.on_foreground else R.color.off_foreground
                )
            )
            views.setContentDescription(
                R.id.widget_root,
                context.getString(if (isOn) R.string.widget_cd_on else R.string.widget_cd_off)
            )
            views.setOnClickPendingIntent(R.id.widget_root, togglePendingIntent(context))
            return views
        }

        private fun togglePendingIntent(context: Context): PendingIntent {
            val intent = Intent(context.applicationContext, FlashWidgetProvider::class.java)
                .setAction(ACTION_TOGGLE)
            return PendingIntent.getBroadcast(
                context.applicationContext,
                TOGGLE_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
