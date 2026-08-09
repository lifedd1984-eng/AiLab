package com.simpleflashlight

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Setup screen. The widget is the everyday way to use the light; this screen exists to place
 * that widget, to change the auto-off time, and as a fallback big button.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var lamp: Button
    private lateinit var state: TextView
    private lateinit var autoOffGroup: RadioGroup

    /** Guards against a second tap while the torch state is still being read. */
    private var busy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lamp = findViewById(R.id.lamp)
        state = findViewById(R.id.state)
        autoOffGroup = findViewById(R.id.auto_off_group)

        if (Torch.isAvailable(this)) {
            lamp.setOnClickListener { toggle() }
        } else {
            lamp.isEnabled = false
            state.setText(R.string.no_flash)
        }

        buildAutoOffChoices()
        findViewById<Button>(R.id.add_widget).setOnClickListener { requestPinWidget() }
    }

    override fun onResume() {
        super.onResume()
        // The light may have been toggled from the widget or quick settings while we were away.
        if (Torch.isAvailable(this)) Torch.readState(this) { render(it) }
    }

    private fun toggle() {
        if (busy) return
        busy = true
        Flashlight.toggle(this) { isOn ->
            busy = false
            render(isOn)
        }
    }

    private fun render(isOn: Boolean) {
        lamp.setText(if (isOn) R.string.button_turn_off else R.string.button_turn_on)
        lamp.setBackgroundResource(if (isOn) R.drawable.lamp_on else R.drawable.lamp_off)
        lamp.setTextColor(color(if (isOn) R.color.on_foreground else R.color.off_foreground))
        state.setText(if (isOn) R.string.state_on else R.string.state_off)
    }

    private fun buildAutoOffChoices() {
        val selected = Prefs.autoOffMinutes(this)

        Prefs.AUTO_OFF_CHOICES.forEach { minutes ->
            val button = RadioButton(this).apply {
                id = View.generateViewId()
                tag = minutes
                text = if (minutes == 0) {
                    getString(R.string.auto_off_never)
                } else {
                    getString(R.string.auto_off_minutes_fmt, minutes)
                }
                textSize = 20f
                setTextColor(color(R.color.off_foreground))
                setPadding(paddingLeft, dp(12), paddingRight, dp(12))
                isChecked = minutes == selected
            }
            autoOffGroup.addView(button)
        }

        autoOffGroup.setOnCheckedChangeListener { group, checkedId ->
            val minutes = group.findViewById<RadioButton>(checkedId)?.tag as? Int ?: return@setOnCheckedChangeListener
            Prefs.setAutoOffMinutes(this, minutes)
            // Re-arm so a change takes effect on a light that is already on.
            Torch.readState(this) { isOn -> if (isOn) AutoOff.schedule(this) }
        }
    }

    private fun requestPinWidget() {
        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, FlashWidgetProvider::class.java)
        if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(provider, null, null)
        } else {
            Toast.makeText(this, R.string.pin_unsupported, Toast.LENGTH_LONG).show()
        }
    }

    private fun color(res: Int) = ContextCompat.getColor(this, res)

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
