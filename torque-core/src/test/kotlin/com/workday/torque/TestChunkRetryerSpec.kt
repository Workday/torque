package com.workday.torque

import com.workday.torque.pooling.TestChunk
import com.workday.torque.utils.FakeAdbTestResultFactory.createCrashedTests
import com.workday.torque.utils.FakeAdbTestResultFactory.createFailedTest
import com.workday.torque.utils.FakeAdbTestResultFactory.createPassedTest
import com.workday.torque.utils.createTestMethodsList
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import kotlin.test.assertTrue

class TestChunkRetryerSpec : Spek(
{
    context("Run test chunk") {
        val adbDevice = AdbDevice("id", "model", online = true)
        val logcatFileIO = mockk<LogcatFileIO>(relaxed = true)

        val installer = mockk<Installer>(relaxed = true)
        val screenRecorder = mockk<ScreenRecorder>(relaxed = true)

        val crashedTestChunkResults1 = createCrashedTests(adbDevice)
        val crashedTestChunkResults2 = createCrashedTests(adbDevice)
        val failedTestChunkResults = listOf(createPassedTest(adbDevice),
                createFailedTest(adbDevice),
                createPassedTest(adbDevice),
                createFailedTest(adbDevice))
        val passedTestChunkResults = listOf(createPassedTest(adbDevice),
                createPassedTest(adbDevice),
                createPassedTest(adbDevice),
                createPassedTest(adbDevice))

        given("A twice crashed then failed then passed test sequence") {
            val testChunkRunner by memoized {
                mockk<TestChunkRunner> {
                    coEvery { run(args = any(), testChunk = any()) } returnsMany listOf(crashedTestChunkResults1,
                            crashedTestChunkResults2,
                            failedTestChunkResults,
                            passedTestChunkResults)
                }
            }

            on("Max retry count 3") {
                val args = Args().apply {
                    retriesPerChunk = 3
                }
                val chunkRetryer = TestChunkRetryer().apply {
                    this.adbDevice = adbDevice
                    this.args = args
                    this.logcatFileIO = logcatFileIO
                    this.testChunkRunner = testChunkRunner
                    this.installer = installer
                    this.screenRecorder = screenRecorder
                }

                it("Retries until success and outputs test results with all passed tests") {
                    runBlocking {
                        val testResults = chunkRetryer.runTestChunkWithRetry(TestChunk(index = 0, testModuleInfo = mockk(), testMethods = mockk()))
                        assertTrue {
                            testResults.containsAll(passedTestChunkResults)
                        }
                    }
                }
            }

            on("Max retry count 2") {
                val args = Args().apply {
                    retriesPerChunk = 2
                }
                val chunkRetryer = TestChunkRetryer().apply {
                    this.adbDevice = adbDevice
                    this.args = args
                    this.logcatFileIO = logcatFileIO
                    this.testChunkRunner = testChunkRunner
                    this.installer = installer
                    this.screenRecorder = screenRecorder
                }

                it("Retries until max retries and outputs test results with failed tests") {
                    runBlocking {
                        val testResults = chunkRetryer.runTestChunkWithRetry(TestChunk(index = 0, testModuleInfo = mockk(), testMethods = mockk()))
                        assertTrue {
                            testResults.containsAll(failedTestChunkResults)
                        }
                    }
                }
            }

            on("Max retry count 1") {
                val args = Args().apply {
                    retriesPerChunk = 1
                }
                val chunkRetryer = TestChunkRetryer().apply {
                    this.adbDevice = adbDevice
                    this.args = args
                    this.logcatFileIO = logcatFileIO
                    this.testChunkRunner = testChunkRunner
                    this.installer = installer
                    this.screenRecorder = screenRecorder
                }

                it("Retries until max retries and outputs test results with failed crashed tests") {
                    runBlocking {
                        val testResults = chunkRetryer.runTestChunkWithRetry(TestChunk(index = 0, testModuleInfo = mockk(), testMethods = mockk()))
                        assertTrue {
                            testResults.containsAll(crashedTestChunkResults2)
                        }
                    }
                }
            }

            on("Max retry count 0") {
                val args = Args().apply {
                    retriesPerChunk = 0
                }
                val chunkRetryer = TestChunkRetryer().apply {
                    this.adbDevice = adbDevice
                    this.args = args
                    this.logcatFileIO = logcatFileIO
                    this.testChunkRunner = testChunkRunner
                    this.installer = installer
                    this.screenRecorder = screenRecorder
                }

                it("Doesn't retry and outputs test results with failed crashed tests and records screen") {
                    runBlocking {
                        val testResults = chunkRetryer.runTestChunkWithRetry(TestChunk(index = 0, testModuleInfo = mockk(), testMethods = mockk()))
                        assertTrue {
                            testResults.containsAll(crashedTestChunkResults1)
                        }
                    }
                }
            }
        }

        given("A twice crashed then failed test sequence") {
            val testChunkRunner by memoized {
                mockk<TestChunkRunner> {
                    coEvery { run(args = any(), testChunk = any()) } returnsMany listOf(crashedTestChunkResults1,
                            crashedTestChunkResults2,
                            failedTestChunkResults)
                }
            }
            on("Max retry count 2") {
                val args = Args().apply {
                    retriesPerChunk = 2
                    recordFailedTests = true
                }
                val chunkRetryer = TestChunkRetryer().apply {
                    this.adbDevice = adbDevice
                    this.args = args
                    this.logcatFileIO = logcatFileIO
                    this.testChunkRunner = testChunkRunner
                    this.installer = installer
                    this.screenRecorder = screenRecorder
                }

                it("Retries until last retry and then start and stop screen recorder") {
                    val testChunk = TestChunk(0, mockk(), createTestMethodsList(4))
                    runBlocking {
                        chunkRetryer.runTestChunkWithRetry(testChunk)
                    }

                    coVerify {
                        screenRecorder.start(any(), any())
                        screenRecorder.stop()
                    }
                }
            }
        }
    }
})