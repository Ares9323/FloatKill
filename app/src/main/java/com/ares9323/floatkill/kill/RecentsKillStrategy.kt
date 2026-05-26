package com.ares9323.floatkill.kill

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.ares9323.floatkill.service.ForegroundDetectorService

/**
 * Soft-kill: dismisses the target app's task from the Recents screen via
 * AccessibilityNodeInfo.ACTION_DISMISS (or the OEM equivalent custom action).
 *
 * Faster and less visually disruptive than [SettingsKillStrategy], but apps
 * with foreground services may survive. If the Recents dismiss fails (no
 * card found, OEM doesn't expose dismiss action), this strategy
 * automatically retries with [SettingsKillStrategy] as a hard-kill fallback.
 */
class RecentsKillStrategy(private val context: Context) : KillStrategy {

    override val name: String = "recents"
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
        val label = resolveAppLabel(packageName)
        service.startRecentsDismiss(packageName, label, relaunch) { success, error ->
            if (success) return@startRecentsDismiss
            Log.w(TAG, "recents-dismiss failed: $error — falling back to Settings")
            // Recents path didn't work on this OEM / app — retry as Settings.
            service.startKillOperation(packageName, relaunch) { s2, e2 ->
                if (!s2) {
                    lastError = "recents failed ($error); settings also failed ($e2)"
                    Log.w(TAG, lastError!!)
                }
            }
        }
        return true
    }

    private fun resolveAppLabel(packageName: String): String {
        labelCache[packageName]?.let { return it }
        val resolved = try {
            val pm = context.packageManager
            val ai = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        } catch (_: Throwable) {
            packageName
        }
        labelCache[packageName] = resolved
        return resolved
    }

    companion object {
        private const val TAG = "RecentsKill"

        // App labels never change at runtime in any practical scenario, so a
        // process-lifetime cache is fine. Bounded indirectly by the number of
        // distinct packages the user actually kills.
        private val labelCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    }
}
