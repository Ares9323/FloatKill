package com.ares9323.floatkill.kill

interface KillStrategy {
    val name: String
    var lastError: String?
    fun kill(packageName: String): Boolean
    fun killAndRelaunch(packageName: String): Boolean
}
