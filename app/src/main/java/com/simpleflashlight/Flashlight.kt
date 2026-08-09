package com.simpleflashlight

import android.content.Context

/**
 * The one place that changes the light: flips the torch, arms or cancels the auto-off alarm,
 * and repaints the widget. Used by both the widget and the app screen so they cannot disagree.
 */
object Flashlight {

    /** Returns the state the light actually ended up in. */
    fun apply(context: Context, on: Boolean): Boolean {
        val nowOn = Torch.set(context, on) && on
        if (nowOn) AutoOff.schedule(context) else AutoOff.cancel(context)
        FlashWidgetProvider.render(context, nowOn)
        return nowOn
    }

    /** Reads the live torch state, flips it, and reports the result on the main thread. */
    fun toggle(context: Context, onDone: (Boolean) -> Unit) {
        Torch.readState(context) { isOn -> onDone(apply(context, !isOn)) }
    }
}
