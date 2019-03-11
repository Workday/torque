package com.workday.torque.html

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.it

class HtmlShortTestSpec : Spek({

    context("HtmlFullTest.toHtmlShortTest") {

        val htmlFullTest by memoized {
            HtmlFullTest(
                    suiteId = "testSuite",
                    packageName = "com.example",
                    className = "TestClass",
                    name = "test1",
                    deviceModel = "test-device-model",
                    status = HtmlFullTest.Status.Passed,
                    durationMillis = 1234,
                    stacktrace = null,
                    logcatPath = "testLogcatPath",
                    screenshotDetails = listOf(HtmlFullTest.ScreenshotDetails(path = "testScreenshotPath1", title = "testScreenshot1"), HtmlFullTest.ScreenshotDetails(path = "testScreenshotPath2", title = "testScreenshot2")),
                    deviceId = "test-device-id"
            )
        }

        val htmlShortTest by memoized { htmlFullTest.toHtmlShortTest() }

        it("converts HtmlFullTest to HtmlShortTest") {
            assertThat(htmlShortTest).isEqualTo(HtmlShortTest(
                    id = htmlFullTest.id,
                    packageName = "com.example",
                    className = "TestClass",
                    name = "test1",
                    status = HtmlFullTest.Status.Passed,
                    durationMillis = 1234,
                    deviceId = "test-device-id",
                    deviceModel = "test-device-model"
            ))
        }
    }
})
