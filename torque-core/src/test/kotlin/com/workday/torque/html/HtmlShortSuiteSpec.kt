package com.workday.torque.html

import com.workday.torque.AdbDevice
import com.workday.torque.AdbDeviceTestResult
import com.workday.torque.Device
import com.workday.torque.Suite
import com.workday.torque.testFile
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.it

class HtmlShortSuiteSpec : Spek({

    context("Suite.toHtmlShortSuite") {
        val suite by memoized {
            Suite(
                    devices = listOf(
                            Device(id = "device1", logcatFile = testFile(), model = "model1"),
                            Device(id = "device2", logcatFile = testFile(), model = "model2")
                    ),
                    tests = listOf(
                            AdbDeviceTestResult(
                                    adbDevice = AdbDevice(id = "device1", online = true),
                                    className = "c",
                                    testName = "t1",
                                    status = AdbDeviceTestResult.Status.Passed,
                                    durationMillis = 200,
                                    logcatFile = testFile()
                            ),
                            AdbDeviceTestResult(
                                    adbDevice = AdbDevice(id = "device2", online = true),
                                    className = "c",
                                    testName = "t2",
                                    status = AdbDeviceTestResult.Status.Passed,
                                    durationMillis = 300,
                                    logcatFile = testFile()
                            )
                    ),
                    passedCount = 2,
                    ignoredCount = 0,
                    failedCount = 0,
                    durationMillis = 400
            )
        }

        val htmlShortSuite by memoized { suite.toHtmlShortSuite(id = "testSuite", htmlReportDir = testFile().parentFile) }

        it("converts Suite to HtmlShortSuite") {
            assertThat(htmlShortSuite).isEqualTo(HtmlShortSuite(
                    id = "testSuite",
                    passedCount = suite.passedCount,
                    ignoredCount = suite.ignoredCount,
                    failedCount = suite.failedCount,
                    durationMillis = suite.durationMillis,
                    devices = suite.devices.map { it.toHtmlDevice(htmlReportDir = testFile().parentFile) }
            ))
        }
    }
})
