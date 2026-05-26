package com.ares9323.floatkill.kill

import android.content.Context
import android.util.Log
import com.ares9323.floatkill.service.ForegroundDetectorService

/**
 * Force-stops a target package by driving the system Settings UI through the
 * AccessibilityService.
 *
 * Pros: hard kill via the system Force stop button; works on any Android.
 * Cons: visible UI flash on Settings while the operation runs (~1–2 s).
 *
 * The actual UI walk lives in [ForegroundDetectorService]; this class is a
 * thin façade that schedules the operation and surfaces a deferred result via
 * [lastError]. `kill`/`killAndRelaunch` return `true` to mean "operation
 * accepted"; if the UI walk fails the watchdog inside the service will populate
 * [lastError] before the next click.
 */
class SettingsKillStrategy(private val context: Context) : KillStrategy {

    override val name: String = "settings"
    override var lastError: String? = null

    override fun kill(packageName: String): Boolean = enqueue(packageName, relaunch = false)
    override fun killAndRelaunch(packageName: String): Boolean = enqueue(packageName, relaunch = true)

    private fun enqueue(packageName: String, relaunch: Boolean): Boolean {
        lastError = null
        val service = ForegroundDetectorService.instance()
        if (service == null) {
            lastError = "accessibility service not connected"
            Log.w(TAG, lastError!!)
            return false
        }
        service.startKillOperation(packageName, relaunch) { success, error ->
            if (!success) {
                lastError = error ?: "unknown failure"
                Log.w(TAG, "kill op finished: success=$success error=$error")
            }
        }
        return true
    }

    companion object {
        private const val TAG = "SettingsKill"
    }
}
