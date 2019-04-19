package com.workday.torque.utils

import com.workday.torque.AdbDevice
import com.workday.torque.AdbDeviceTestResult
import io.mockk.mockk

object FakeAdbTestResultFactory {
    fun createCrashedTests(adbDevice: AdbDevice): List<AdbDeviceTestResult> {
        return List(4) {
            AdbDeviceTestResult(adbDevice, "someClass", "someTest", AdbDeviceTestResult.Status.Failed("crashed stackTrace"), 1, mockk())
        }
    }

    fun createFailedTest(adbDevice: AdbDevice): AdbDeviceTestResult {
        return AdbDeviceTestResult(adbDevice, "someClass", "someTest", AdbDeviceTestResult.Status.Failed("stackTrace"), 1, mockk())
    }

    fun createPassedTest(adbDevice: AdbDevice): AdbDeviceTestResult {
        return AdbDeviceTestResult(adbDevice, "someClass", "someTest", AdbDeviceTestResult.Status.Passed, 1, mockk())
    }

    fun createIgnoredTest(adbDevice: AdbDevice): AdbDeviceTestResult {
        return AdbDeviceTestResult(adbDevice, "someClass", "someTest", AdbDeviceTestResult.Status.Ignored("stackTrace"), 1, mockk())
    }
}