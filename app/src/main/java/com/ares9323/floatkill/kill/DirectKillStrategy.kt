package com.ares9323.floatkill.kill

import android.app.ActivityManager
import android.content.Context
import android.util.Log

/**
 * Stops the target package by invoking the hidden ActivityManager.forceStopPackage(String)
 * method via reflection. Requires android.permission.FORCE_STOP_PACKAGES to have been
 * granted via `adb shell pm grant`.
 */
class DirectKillStrategy(private val context: Context) : KillStrategy {

    override val name: String = "direct"
    override var lastError: String? = null

    private val activityManager: ActivityManager =
        context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    override fun kill(packageName: String): Boolean {
        lastError = null
        return try {
            val method = ActivityManager::class.java.getMethod(
                "forceStopPackage",
                String::class.java
            )
            method.invoke(activityManager, packageName)
            Log.i(TAG, "forceStopPackage($packageName) invoked successfully")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "forceStopPackage($packageName) failed", t)
            lastError = "${t.javaClass.simpleName}: ${t.message}"
            false
        }
    }

    override fun killAndRelaunch(packageName: String): Boolean {
        val killed = kill(packageName)
        if (!killed) return false
        return relaunch(packageName)
    }

    private fun relaunch(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            Log.w(TAG, "No launch intent for $packageName, skipping relaunch")
            lastError = "no launch intent for $packageName"
            return false
        }
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to relaunch $packageName", t)
            lastError = "relaunch: ${t.message}"
            false
        }
    }

    companion object {
        private const val TAG = "DirectKillStrategy"
    }
}
