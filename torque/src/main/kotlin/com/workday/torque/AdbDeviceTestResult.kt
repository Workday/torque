package com.workday.torque

import java.io.File

data class AdbDeviceTestResult(
        val adbDevice: AdbDevice,
        val className: String,
        val testName: String,
        val status: Status,
        val durationMillis: Long,
        val logcatFile: File
) {
    sealed class Status {
        object Passed : Status()
        data class Ignored(val stacktrace: String) : Status()
        data class Failed(val stacktrace: String) : Status()
    }
}
