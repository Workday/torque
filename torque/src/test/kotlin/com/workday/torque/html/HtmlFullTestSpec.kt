package com.workday.torque.html

import com.workday.torque.AdbDevice
import com.workday.torque.AdbDeviceTestResult
import com.workday.torque.testFile
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.it

class HtmlFullTestSpec : Spek({

    context("AdbDeviceTest.toHtmlTest") {

        val adbDeviceTestResult = AdbDeviceTestResult(
                adbDevice = AdbDevice(id = "testDevice", online = true, model = "testModel"),
                className = "com.example.TestClass",
                testName = "test1",
                status = AdbDeviceTestResult.Status.Passed,
                durationMillis = 230,
                logcatFile = testFile()
        )

        val htmlTest = adbDeviceTestResult.toHtmlFullTest(suiteId = "testSuite", htmlReportDir = testFile().parentFile)

        it("converts AdbDeviceTest to HtmlFullTest") {
            assertThat(htmlTest).isEqualTo(HtmlFullTest(
                    suiteId = "testSuite",
                    packageName = "com.example",
                    className = "TestClass",
                    name = adbDeviceTestResult.testName,
                    deviceModel = "testModel",
                    status = HtmlFullTest.Status.Passed,
                    durationMillis = adbDeviceTestResult.durationMillis,
                    stacktrace = null,
                    screenshotDetails = emptyList(),
                    logcatPath = adbDeviceTestResult.logcatFile.name,
                    deviceId = adbDeviceTestResult.adbDevice.id
            ))
        }
    }
})
