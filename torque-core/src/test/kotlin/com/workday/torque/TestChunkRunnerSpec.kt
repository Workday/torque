package com.workday.torque

import com.gojuno.commander.os.Notification
import com.workday.torque.pooling.ModuleInfo
import com.workday.torque.pooling.TestChunk
import com.workday.torque.pooling.TestModuleInfo
import com.workday.torque.utils.createTestMethodsList
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Observable
import kotlinx.coroutines.runBlocking
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class TestChunkRunnerSpec : Spek(
{
    context("Running a test chunk") {
        val args = Args()
        val adbDevice = mockk<AdbDevice>(relaxed = true)
        val logcatFileIO by memoized { mockk<LogcatFileIO>() }
        val installer = mockk<Installer>(relaxed = true)
        val processRunner by memoized { mockk<ProcessRunner>() }
        val instrumentationReader by memoized { mockk<InstrumentationReader>() }
        val testChunkRunner by memoized {
            TestChunkRunner(args,
                            adbDevice,
                            logcatFileIO,
                            installer,
                            processRunner,
                            instrumentationReader)
        }

        given("a chunk of 3 tests") {
            val testPackage = ApkPackage.Valid("com.company.mymodule.test")
            val testRunner = TestRunner.Valid("android.support.test.runner.AndroidJUnitRunner")
            val moduleInfo = TestModuleInfo(ModuleInfo(testPackage, ""), testRunner)
            val testChunk by memoized {
                TestChunk(index = 0,
                          testModuleInfo = moduleInfo,
                          testMethods = createTestMethodsList(3))
            }
            val testLogcatFile = mockk<File>()
            every { logcatFileIO.getLogcatFileForTest(any()) } returns testLogcatFile
            val instrumentationTestResult = InstrumentationTestResult(0,
                                                                      0,
                                                                      TestDetails("className", "testName"),
                                                                      InstrumentationTestResult.Status.Passed,
                                                                      0)
            every { instrumentationReader.readTestResults(any(), any()) } returns Observable.just(instrumentationTestResult,
                                                                                           instrumentationTestResult,
                                                                                           instrumentationTestResult)
            every { processRunner.runAdb(allAny()) } returns Observable.just(Notification.Start(mockk(),
                                                                                                mockk()))


            on("timeout per chunk of 75 seconds") {
                args.chunkTimeoutSeconds = 75

                it("runs the process with 75 seconds timeout") {
                    runBlocking {
                        testChunkRunner.run(args, testChunk)
                    }

                    verify {
                        processRunner.runAdb(any(), Timeout(75, TimeUnit.SECONDS))
                    }
                }

                it("runs successfully and returns AdbDeviceTestResult") {
                    val adbTestResult = AdbDeviceTestResult(
                            adbDevice = adbDevice,
                            className = instrumentationTestResult.testDetails.testClass,
                            testName = instrumentationTestResult.testDetails.testName,
                            status = AdbDeviceTestResult.Status.Passed,
                            durationMillis = 0,
                            logcatFile = testLogcatFile)
                    val expectedTestResults = listOf(adbTestResult, adbTestResult, adbTestResult)

                    val adbDeviceTestResults = runBlocking {
                        testChunkRunner.run(args, testChunk)
                    }

                    assertEquals(expectedTestResults, adbDeviceTestResults)
                }
            }
        }
    }
})
