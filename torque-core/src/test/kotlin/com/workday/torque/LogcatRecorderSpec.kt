package com.workday.torque

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.runBlocking
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it

class LogcatRecorderSpec : Spek(
{
    val fullLogcatLines = listOf("some logcat line",
                                 "11-09 16:13:17.968 21283 21301 I TestRunner: run started: 2 tests",
                                 "TestRunner: started: someTestMethod1(someTestClass1)",
                                 "some logcat line 1-1",
                                 "some logcat line 1-2",
                                 "TestRunner: finished: someTestMethod1(someTestClass1)",
                                 "some logcat line",
                                 "TestRunner: started: someTestMethod2(someTestClass2)",
                                 "some logcat line 2",
                                 "TestRunner: finished: someTestMethod2(someTestClass2)",
                                 "some logcat line",
                                 "11-09 16:13:22.746 21283 21301 I TestRunner: run finished: 2 tests, 0 failed, 0 ignored",
                                 "some logcat line",
                                 "11-09 16:13:17.968 21283 21301 I TestRunner: run started: 2 tests",
                                 "TestRunner: started: someTestMethod3(someTestClass3)",
                                 "some logcat line 3",
                                 "TestRunner: finished: someTestMethod3(someTestClass3)",
                                 "some logcat line",
                                 "TestRunner: started: someTestMethod4(someTestClass4)",
                                 "some logcat line 4-1",
                                 "some logcat line 4-2",
                                 "TestRunner: finished: someTestMethod4(someTestClass4)",
                                 "some logcat line",
                                 "11-09 16:13:22.746 21283 21301 I TestRunner: run finished: 2 tests, 0 failed, 0 ignored",
                                 "some logcat line"
    )
    val fullLogcatLinesInvalidStart = listOf("some logcat line",
            "TestRunner: started: someTestMethod1(someTestClass1)",
            "some logcat line 1-1",
            "some logcat line 1-2",
            "TestRunner: started: someTestMethod2(someTestClass2)",
            "some logcat line 2",
            "TestRunner: finished: someTestMethod2(someTestClass2)"
    )
    val fullLogcatLinesInvalidFinish1 = listOf("some logcat line",
            "TestRunner: finished: someTestMethod1(someTestClass1)",
            "some logcat line"
    )
    val fullLogcatLinesInvalidFinish2 = listOf("some logcat line",
            "TestRunner: started: someTestMethod1(someTestClass1)",
            "some logcat line 1-1",
            "some logcat line 1-2",
            "TestRunner: finished: someTestMethod2(someTestClass2)"
    )
    context("LogcatRecorder parses and saves logcat for each test") {
        given("a Channel of newlines from the full logcat file with 2 chunks runs with 2 tests each") {
            val fullLogcatTailChannel by memoized {
                Channel<String>(capacity = UNLIMITED)
                        .apply {
                            fullLogcatLines.forEach { line ->
                                offer(line)
                            }
                            close()
                        }
            }
            val logcatFileIo by memoized {
                mockk<LogcatFileIO> {
                    every { writeLogcatFileForTest(any()) } just Runs
                    coEvery { redirectLogcatToFile() } just Runs
                    every { tailFile() } returns fullLogcatTailChannel
                }
            }
            val logcatRecorder by memoized {
                LogcatRecorder().apply {
                    this.adbDevice = mockk(relaxed = true)
                    this.logcatFileIO = logcatFileIo
                }
            }

            it("redirects logcat, tails that file and emits 4 TestLogcat with correct fields") {
                val testLogcat1 = TestLogcat(testDetails = TestDetails("someTestClass1", "someTestMethod1"),
                                             logcat = "TestRunner: started: someTestMethod1(someTestClass1)\n" +
                                                     "some logcat line 1-1\n" +
                                                     "some logcat line 1-2\n" +
                                                     "TestRunner: finished: someTestMethod1(someTestClass1)\n")

                val testLogcat2 = TestLogcat(testDetails = TestDetails("someTestClass2", "someTestMethod2"),
                                             logcat = "TestRunner: started: someTestMethod2(someTestClass2)\n" +
                                                     "some logcat line 2\n" +
                                                     "TestRunner: finished: someTestMethod2(someTestClass2)\n")
                val testLogcat3 = TestLogcat(testDetails = TestDetails("someTestClass3", "someTestMethod3"),
                                             logcat = "TestRunner: started: someTestMethod3(someTestClass3)\n" +
                                                     "some logcat line 3\n" +
                                                     "TestRunner: finished: someTestMethod3(someTestClass3)\n")
                val testLogcat4 = TestLogcat(testDetails = TestDetails("someTestClass4", "someTestMethod4"),
                                             logcat = "TestRunner: started: someTestMethod4(someTestClass4)\n" +
                                                     "some logcat line 4-1\n" +
                                                     "some logcat line 4-2\n" +
                                                     "TestRunner: finished: someTestMethod4(someTestClass4)\n")

                runBlocking {
                    logcatRecorder.start(this)
                }

                coVerifyOrder {
                    logcatFileIo.redirectLogcatToFile()
                    logcatFileIo.tailFile()
                    logcatFileIo.writeLogcatFileForTest(testLogcat1)
                    logcatFileIo.writeLogcatFileForTest(testLogcat2)
                    logcatFileIo.writeLogcatFileForTest(testLogcat3)
                    logcatFileIo.writeLogcatFileForTest(testLogcat4)
                }
            }
        }

        given("a Channel of newlines from the full logcat file with invalid start") {
            val fullLogcatTailChannel by memoized {
                Channel<String>(capacity = UNLIMITED)
                        .apply {
                            fullLogcatLinesInvalidStart.forEach { line ->
                                offer(line)
                            }
                            close()
                        }
            }
            val logcatFileIo by memoized {
                mockk<LogcatFileIO> {
                    every { writeLogcatFileForTest(any()) } just Runs
                    coEvery { redirectLogcatToFile() } just Runs
                    every { tailFile() } returns fullLogcatTailChannel
                }
            }
            val logcatRecorder by memoized {
                LogcatRecorder().apply {
                    this.adbDevice = mockk(relaxed = true)
                    this.logcatFileIO = logcatFileIo
                }
            }

            it("redirects logcat, tails that file and emits 2 TestLogcat with the first having an artificial end log and logs the next one normally") {
                val testLogcat1 = TestLogcat(testDetails = TestDetails("someTestClass1", "someTestMethod1"),
                        logcat = "TestRunner: started: someTestMethod1(someTestClass1)\n" +
                                "some logcat line 1-1\n" +
                                "some logcat line 1-2\n" +
                                "[Torque]: Started next test without finishing the last one. Unfinished test: someTestClass1.someTestMethod1\n")

                val testLogcat2 = TestLogcat(testDetails = TestDetails("someTestClass2", "someTestMethod2"),
                        logcat = "TestRunner: started: someTestMethod2(someTestClass2)\n" +
                                "some logcat line 2\n" +
                                "TestRunner: finished: someTestMethod2(someTestClass2)\n")

                runBlocking {
                    logcatRecorder.start(this)
                }

                coVerifyOrder {
                    logcatFileIo.redirectLogcatToFile()
                    logcatFileIo.tailFile()
                    logcatFileIo.writeLogcatFileForTest(testLogcat1)
                    logcatFileIo.writeLogcatFileForTest(testLogcat2)
                }
            }
        }

        given("a Channel of newlines from the full logcat file with invalid finish that had no start") {
            val fullLogcatTailChannel by memoized {
                Channel<String>(capacity = UNLIMITED)
                        .apply {
                            fullLogcatLinesInvalidFinish1.forEach { line ->
                                offer(line)
                            }
                            close()
                        }
            }
            val logcatFileIo by memoized {
                mockk<LogcatFileIO> {
                    every { writeLogcatFileForTest(any()) } just Runs
                    coEvery { redirectLogcatToFile() } just Runs
                    every { tailFile() } returns fullLogcatTailChannel
                }
            }
            val logcatRecorder by memoized {
                LogcatRecorder().apply {
                    this.adbDevice = mockk(relaxed = true)
                    this.logcatFileIO = logcatFileIo
                }
            }

            it("does not call writeLogcatFileForTest") {
                runBlocking {
                    logcatRecorder.start(this)
                }

                coVerify(exactly = 0) { logcatFileIo.writeLogcatFileForTest(any()) }
            }
        }

        given("a Channel of newlines from the full logcat file with invalid finish that had wrong start") {
            val fullLogcatTailChannel by memoized {
                Channel<String>(capacity = UNLIMITED)
                        .apply {
                            fullLogcatLinesInvalidFinish2.forEach { line ->
                                offer(line)
                            }
                            close()
                        }
            }
            val logcatFileIo by memoized {
                mockk<LogcatFileIO> {
                    every { writeLogcatFileForTest(any()) } just Runs
                    coEvery { redirectLogcatToFile() } just Runs
                    every { tailFile() } returns fullLogcatTailChannel
                }
            }
            val logcatRecorder by memoized {
                LogcatRecorder().apply {
                    this.adbDevice = mockk(relaxed = true)
                    this.logcatFileIO = logcatFileIo
                }
            }

            it("does not call writeLogcatFileForTest") {
                runBlocking {
                    logcatRecorder.start(this)
                }

                coVerify(exactly = 0) { logcatFileIo.writeLogcatFileForTest(any()) }
            }
        }
    }
})
