package com.workday.torque

import java.io.File

data class AdbDeviceTestSession(
        val adbDevice: AdbDevice,
        val testResults: MutableList<AdbDeviceTestResult> = mutableListOf(),
        val logcatFile: File,
        var passedCount: Int = 0,
        var ignoredCount: Int = 0,
        var failedCount: Int = 0,
        var startTimestampMillis: Long = 0,
        var endTimestampMillis: Long = 0
) {
    val durationMillis
        get() = endTimestampMillis - startTimestampMillis
}
