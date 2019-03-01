package com.workday.torque

data class Suite(
        val devices: List<Device>,
        val tests: List<AdbDeviceTestResult>,
        val passedCount: Int,
        val ignoredCount: Int,
        val failedCount: Int,
        val durationMillis: Long
)
