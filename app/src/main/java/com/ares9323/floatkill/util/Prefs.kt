package com.ares9323.floatkill.util

import android.content.Context

object Prefs {

    enum class KillMode(val key: String) {
        RECENTS("recents"),
        SETTINGS("settings");

        companion object {
            fun fromKey(k: String?): KillMode =
                values().firstOrNull { it.key == k } ?: RECENTS
        }
    }

    fun killMode(context: Context): KillMode =
        KillMode.fromKey(sp(context).getString(KEY_KILL_MODE, KillMode.RECENTS.key))

    fun setKillMode(context: Context, mode: KillMode) {
        sp(context).edit().putString(KEY_KILL_MODE, mode.key).apply()
    }

    private fun sp(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private const val PREFS = "float_kill_prefs"
    private const val KEY_KILL_MODE = "kill_mode"
}
