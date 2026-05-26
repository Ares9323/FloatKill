package com.ares9323.floatkill

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.ares9323.floatkill.databinding.ActivityMainBinding
import com.ares9323.floatkill.service.FloatingButtonService
import com.ares9323.floatkill.util.PermissionHelper
import com.ares9323.floatkill.util.Prefs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshUi() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toggleService.setOnClickListener { onToggleServiceClicked() }
        binding.btnOpenAccessibility.setOnClickListener { openAccessibilitySettings() }
        binding.btnOpenOverlay.setOnClickListener { openOverlaySettings() }

        binding.killModeGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.modeRecents -> Prefs.KillMode.RECENTS
                R.id.modeSettings -> Prefs.KillMode.SETTINGS
                else -> Prefs.KillMode.RECENTS
            }
            Prefs.setKillMode(this, mode)
            refreshUi()
        }

        maybeRequestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        val status = PermissionHelper.resolve(this)
        when (status) {
            is PermissionHelper.Status.Ready -> {
                binding.statusText.text =
                    getString(R.string.status_ready, status.strategy.name)
            }
            PermissionHelper.Status.SetupRequired -> {
                binding.statusText.text = getString(R.string.status_setup_required)
            }
        }

        val expectedId = when (Prefs.killMode(this)) {
            Prefs.KillMode.RECENTS -> R.id.modeRecents
            Prefs.KillMode.SETTINGS -> R.id.modeSettings
        }
        if (binding.killModeGroup.checkedRadioButtonId != expectedId) {
            binding.killModeGroup.setOnCheckedChangeListener(null)
            binding.killModeGroup.check(expectedId)
            binding.killModeGroup.setOnCheckedChangeListener { _, checkedId ->
                val mode = when (checkedId) {
                    R.id.modeRecents -> Prefs.KillMode.RECENTS
                    R.id.modeSettings -> Prefs.KillMode.SETTINGS
                    else -> Prefs.KillMode.RECENTS
                }
                Prefs.setKillMode(this, mode)
                refreshUi()
            }
        }

        val overlayOk = PermissionHelper.canDrawOverlays(this)
        binding.overlayState.text =
            getString(if (overlayOk) R.string.overlay_granted else R.string.overlay_missing)
        binding.btnOpenOverlay.visibility = if (overlayOk) View.GONE else View.VISIBLE

        val a11yOk = PermissionHelper.accessibilityEnabled(this)
        binding.accessibilityState.text =
            getString(if (a11yOk) R.string.accessibility_enabled else R.string.accessibility_disabled)
        binding.btnOpenAccessibility.visibility = if (a11yOk) View.GONE else View.VISIBLE

        val running = FloatingButtonService.running
        binding.toggleService.text =
            getString(if (running) R.string.stop_service else R.string.start_service)
        binding.runningState.text =
            getString(if (running) R.string.service_running else R.string.service_stopped)

        val canStart = (status is PermissionHelper.Status.Ready) && overlayOk && a11yOk
        binding.toggleService.isEnabled = canStart || running
    }

    private fun onToggleServiceClicked() {
        if (FloatingButtonService.running) {
            stopService(Intent(this, FloatingButtonService::class.java))
            binding.toggleService.postDelayed({ refreshUi() }, 200)
            return
        }
        if (!PermissionHelper.canDrawOverlays(this)) {
            openOverlaySettings(); return
        }
        if (!PermissionHelper.accessibilityEnabled(this)) {
            openAccessibilitySettings(); return
        }
        val intent = Intent(this, FloatingButtonService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        binding.toggleService.postDelayed({ refreshUi() }, 400)
    }

    private fun openAccessibilitySettings() {
        startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (_: Throwable) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (PermissionHelper.canPostNotifications(this)) return
        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
