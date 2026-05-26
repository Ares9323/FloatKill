package com.ares9323.floatkill.util

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.ares9323.floatkill.kill.DirectKillStrategy
import com.ares9323.floatkill.kill.KillStrategy
import com.ares9323.floatkill.kill.RecentsKillStrategy
import com.ares9323.floatkill.kill.SettingsKillStrategy
import com.ares9323.floatkill.service.ForegroundDetectorService

/**
 * Resolves which [KillStrategy] is usable on this device.
 *
 * Priority (best UX first):
 *   1. Direct — only on ROMs where FORCE_STOP_PACKAGES is pre-granted (rare)
 *   2. Accessibility-driven — Recents (soft) or Settings (hard) depending on
 *      the user's preference in [Prefs]
 */
object PermissionHelper {

    sealed class Status {
        data class Ready(val strategy: KillStrategy) : Status()
        object SetupRequired : Status()
    }

    fun resolve(context: Context): Status {
        if (hasDirectPermission(context)) {
            return Status.Ready(DirectKillStrategy(context))
        }
        if (accessibilityEnabled(context)) {
            return Status.Ready(
                when (Prefs.killMode(context)) {
                    Prefs.KillMode.RECENTS -> RecentsKillStrategy(context)
                    Prefs.KillMode.SETTINGS -> SettingsKillStrategy(context)
                }
            )
        }
        return Status.SetupRequired
    }

    fun hasDirectPermission(context: Context): Boolean =
        context.checkSelfPermission(PERMISSION_FORCE_STOP_PACKAGES) ==
            PackageManager.PERMISSION_GRANTED

    private const val PERMISSION_FORCE_STOP_PACKAGES =
        "android.permission.FORCE_STOP_PACKAGES"

    fun canDrawOverlays(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    fun accessibilityEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false
        val enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val target = ComponentName(context, ForegroundDetectorService::class.java)
        return enabled.any { info ->
            val resolveInfo = info.resolveInfo ?: return@any false
            val si = resolveInfo.serviceInfo
            si.packageName == target.packageName && si.name == target.className
        }
    }

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
}
