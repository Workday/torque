package com.workday.torque.utils

import com.workday.torque.AdbDevice
import com.workday.torque.AdbDeviceTestResult
import com.workday.torque.AdbDeviceTestSession
import io.mockk.mockk
import java.io.File

object FakeSessionFactory {
    val deviceA = AdbDevice("deviceA", "modelA", true)
    val deviceB = AdbDevice("deviceB", "modelB", true)
    val deviceC = AdbDevice("deviceC", "modelC", true)
    val deviceD = AdbDevice("deviceD", "modelD", true)

    val successResultA = AdbDeviceTestResult(deviceA, "some.cool.class1", "testOne", AdbDeviceTestResult.Status.Passed, 10, createLogcatFile())
    val successResultB = AdbDeviceTestResult(deviceB, "some.cool.class2", "testTwo", AdbDeviceTestResult.Status.Passed, 10, createLogcatFile())
    val failedResultC = AdbDeviceTestResult(deviceC, "some.cool.class3", "testThree", AdbDeviceTestResult.Status.Failed("someTrace"), 1, createLogcatFile())
    val failedResultD = AdbDeviceTestResult(deviceC, "some.cool.class4", "testFour", AdbDeviceTestResult.Status.Failed("someTrace"), 1, createLogcatFile())

    val sessionA = AdbDeviceTestSession(adbDevice = deviceA,
                                        testResults = mutableListOf(successResultA),
                                        logcatFile = createLogcatFile(),
                                        passedCount = 1,
                                        ignoredCount = 0,
                                        failedCount = 0,
                                        startTimestampMillis = 0,
                                        endTimestampMillis = 200)
    val sessionB = AdbDeviceTestSession(adbDevice = deviceB,
                                        testResults = mutableListOf(successResultB),
                                        logcatFile = createLogcatFile(),
                                        passedCount = 1,
                                        ignoredCount = 0,
                                        failedCount = 0,
                                        startTimestampMillis = 30,
                                        endTimestampMillis = 180)
    val sessionC = AdbDeviceTestSession(adbDevice = deviceC,
                                        testResults = mutableListOf(failedResultC),
                                        logcatFile = createLogcatFile(),
                                        passedCount = 0,
                                        ignoredCount = 0,
                                        failedCount = 1,
                                        startTimestampMillis = 20,
                                        endTimestampMillis = 230)
    val sessionD = AdbDeviceTestSession(adbDevice = deviceD,
                                        testResults = mutableListOf(failedResultD),
                                        logcatFile = createLogcatFile(),
                                        passedCount = 0,
                                        ignoredCount = 0,
                                        failedCount = 1,
                                        startTimestampMillis = 15,
                                        endTimestampMillis = 185)

    private fun createLogcatFile(): File = mockk()
}
