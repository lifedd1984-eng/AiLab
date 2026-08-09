package com.simpleflashlight

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper

/**
 * Torch control built on [CameraManager.setTorchMode], which needs no CAMERA permission and
 * does not hold the camera open, so the widget can toggle the light without any prompts.
 */
object Torch {

    /** How long to wait for the system to report the current torch state before giving up. */
    private const val STATE_QUERY_TIMEOUT_MS = 700L

    fun isAvailable(context: Context): Boolean = flashCameraId(context) != null

    private fun manager(context: Context): CameraManager =
        context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private fun flashCameraId(context: Context): String? = try {
        val cm = manager(context)
        cm.cameraIdList.firstOrNull { id ->
            cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    } catch (e: CameraAccessException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    /** Returns true when the torch actually reached [on]. */
    fun set(context: Context, on: Boolean): Boolean {
        val id = flashCameraId(context) ?: return false
        return try {
            manager(context).setTorchMode(id, on)
            Prefs.setLastTorchState(context, on)
            true
        } catch (e: CameraAccessException) {
            // Another app holds the camera, or the torch is otherwise unavailable right now.
            Prefs.setLastTorchState(context, false)
            false
        } catch (e: IllegalArgumentException) {
            Prefs.setLastTorchState(context, false)
            false
        }
    }

    /**
     * Reports the torch state the system currently sees, so the widget stays correct even when
     * the light was toggled somewhere else (quick settings, the camera app). Falls back to the
     * last state this app applied if the system does not answer within
     * [STATE_QUERY_TIMEOUT_MS]. [onResult] always runs exactly once, on the main thread.
     */
    fun readState(context: Context, onResult: (Boolean) -> Unit) {
        val id = flashCameraId(context)
        if (id == null) {
            onResult(false)
            return
        }

        val cm = manager(context)
        val handler = Handler(Looper.getMainLooper())
        var settled = false
        var callback: CameraManager.TorchCallback? = null

        val timeout = Runnable {
            if (settled) return@Runnable
            settled = true
            callback?.let { runCatching { cm.unregisterTorchCallback(it) } }
            onResult(Prefs.lastTorchState(context))
        }

        callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) =
                settle(cameraId, enabled)

            override fun onTorchModeUnavailable(cameraId: String) = settle(cameraId, false)

            private fun settle(cameraId: String, enabled: Boolean) {
                if (settled || cameraId != id) return
                settled = true
                handler.removeCallbacks(timeout)
                runCatching { cm.unregisterTorchCallback(this) }
                onResult(enabled)
            }
        }

        // Registering delivers the current state right away, which is what we are after.
        val registered = runCatching { cm.registerTorchCallback(callback, handler) }.isSuccess
        if (!registered) {
            settled = true
            onResult(Prefs.lastTorchState(context))
            return
        }
        handler.postDelayed(timeout, STATE_QUERY_TIMEOUT_MS)
    }
}
