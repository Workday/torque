package com.workday.torque

data class AdbDevice(
        val id: String,
        val model: String = "unknown",
        val online: Boolean,
        val installedPackages: MutableSet<String> = mutableSetOf()
) {
    val isEmulator = id.startsWith("emulator-")
    val tag = "$model [$id]"
}
