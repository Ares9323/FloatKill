package com.ares9323.floatkill.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ares9323.floatkill.MainActivity
import com.ares9323.floatkill.util.PermissionHelper

/**
 * Quick Settings tile that toggles the floating button service.
 *
 * If required permissions are missing the tile opens [MainActivity] instead of
 * blindly starting the service — that avoids "Bottone fluttuante in esecuzione"
 * lies when the overlay or accessibility permission isn't granted.
 */
class FloatKillTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val running = FloatingButtonService.running
        if (running) {
            stopService(Intent(this, FloatingButtonService::class.java))
            updateTile()
            return
        }
        val overlayOk = PermissionHelper.canDrawOverlays(this)
        val a11yOk = PermissionHelper.accessibilityEnabled(this)
        if (!overlayOk || !a11yOk) {
            val intent = Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            launchAndCollapse(intent)
            return
        }
        val svc = Intent(this, FloatingButtonService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc)
        } else {
            startService(svc)
        }
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.state = if (FloatingButtonService.running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    @Suppress("DEPRECATION")
    private fun launchAndCollapse(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                android.app.PendingIntent.getActivity(
                    this, 0, intent,
                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        } else {
            // Deprecated Intent overload, only on API < 34.
            startActivityAndCollapse(intent)
        }
    }
}
