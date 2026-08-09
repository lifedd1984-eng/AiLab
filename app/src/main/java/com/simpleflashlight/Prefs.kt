package com.simpleflashlight

import android.content.Context

/** Small wrapper around the single SharedPreferences file the app uses. */
object Prefs {

    private const val FILE = "flashlight"
    private const val KEY_TORCH_ON = "torch_on"
    private const val KEY_AUTO_OFF_MINUTES = "auto_off_minutes"

    /** Auto-off choices offered in the app, in minutes. 0 means "never turn off by itself". */
    val AUTO_OFF_CHOICES = intArrayOf(3, 5, 10, 30, 0)

    const val DEFAULT_AUTO_OFF_MINUTES = 5

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** The last torch state this app applied. Only a fallback — see [Torch.readState]. */
    fun lastTorchState(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TORCH_ON, false)

    fun setLastTorchState(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean(KEY_TORCH_ON, on).apply()
    }

    fun autoOffMinutes(context: Context): Int =
        prefs(context).getInt(KEY_AUTO_OFF_MINUTES, DEFAULT_AUTO_OFF_MINUTES)

    fun setAutoOffMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_AUTO_OFF_MINUTES, minutes).apply()
    }
}
