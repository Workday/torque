package com.workday.torque

import com.workday.torque.AdbDeviceTestResult.Status.Failed
import com.workday.torque.AdbDeviceTestResult.Status.Ignored
import com.workday.torque.AdbDeviceTestResult.Status.Passed
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.it
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

class JUnitReportSpec : Spek({

    val LF = System.getProperty("line.separator")

    context("write test run result as junit4 report to file") {

        val adbDevice by memoized { AdbDevice(id = "testDevice", online = true) }
        val outputFile by memoized { testFile() }

        perform {
            writeJunit4Report(
                    suite = Suite(
                            devices = listOf(Device(
                                    id = adbDevice.id,
                                    logcatFile = testFile(),
                                    model = adbDevice.model
                            )),
                            tests = listOf(
                                    AdbDeviceTestResult(
                                            adbDevice = adbDevice,
                                            className = "test.class.name1",
                                            testName = "test1",
                                            status = Passed,
                                            durationMillis = SECONDS.toMillis(2),
                                            logcatFile = testFile()
                                    ),
                                    AdbDeviceTestResult(
                                            adbDevice = adbDevice,
                                            className = "test.class.name2",
                                            testName = "test2",
                                            status = Failed(stacktrace = "multi${LF}line${LF}stacktrace"),
                                            durationMillis = MILLISECONDS.toMillis(3250),
                                            logcatFile = testFile()
                                    ),
                                    AdbDeviceTestResult(
                                            adbDevice = adbDevice,
                                            className = "test.class.name3",
                                            testName = "test3",
                                            status = Passed,
                                            durationMillis = SECONDS.toMillis(1),
                                            logcatFile = testFile()
                                    ),
                                    AdbDeviceTestResult(
                                            adbDevice = adbDevice,
                                            className = "test.class.name4",
                                            testName = "test4",
                                            status = Ignored(""),
                                            durationMillis = SECONDS.toMillis(0),
                                            logcatFile = testFile()
                                    ),
                                    AdbDeviceTestResult(
                                            adbDevice = adbDevice,
                                            className = "test.class.name5",
                                            testName = "test5",
                                            status = Ignored("multi${LF}line${LF}stacktrace"),
                                            durationMillis = SECONDS.toMillis(0),
                                            logcatFile = testFile()
                                    )
                            ),
                            passedCount = 2,
                            ignoredCount = 2,
                            failedCount = 1,
                            durationMillis = 6250
                    ),
                    outputFile = outputFile
            )
        }

        it("produces correct xml report") {
            var expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <testsuite tests="5" failures="1" errors="0" skipped="2" time="6.25" hostname="localhost">
                <properties/>
                <testcase classname="test.class.name1" name="test1" time="2.0"/>
                <testcase classname="test.class.name2" name="test2" time="3.25">
                <failure>
                multi
                line
                stacktrace
                </failure>
                </testcase>
                <testcase classname="test.class.name3" name="test3" time="1.0"/>
                <testcase classname="test.class.name4" name="test4" time="0.0">
                <skipped/>
                </testcase>
                <testcase classname="test.class.name5" name="test5" time="0.0">
                <skipped>
                multi
                line
                stacktrace
                </skipped>
                </testcase>
                </testsuite>
                """.trimIndent() + "\n"
            expected = normalizeLinefeed(expected)
            val actual = outputFile.readText()
            assertThat(actual).isEqualTo(expected)
        }
    }
})
