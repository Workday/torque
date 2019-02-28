package com.workday.torque

import com.workday.torque.pooling.TestChunk
import com.workday.torque.pooling.TestPool
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Completable
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

class TestRunFactorySpec : Spek(
{
    context("Run test chunk") {
        val adbDevice = AdbDevice("id", "model", online = true)
        val logcatFileIO = mockk<LogcatFileIO>(relaxed = true)
        val logcatRecorder = mockk<LogcatRecorder>(relaxed = true)
        val filePuller by memoized {
            mockk<FilePuller> {
                every { pullTestFolder(any(), any(), any(), any()) } returns Completable.complete()
            }
        }
        val installer = mockk<Installer>(relaxed = true)

        val crashedTestChunkResults = createCrashedTests(adbDevice)
        val failedTestChunkResults = listOf(createPassedTest(adbDevice),
                                            createFailedTest(adbDevice),
                                            createPassedTest(adbDevice),
                                            createFailedTest(adbDevice))
        val passedTestChunkResults = listOf(createPassedTest(adbDevice),
                                            createPassedTest(adbDevice),
                                            createPassedTest(adbDevice),
                                            createPassedTest(adbDevice))

        val testPool by memoized {
            mockk<TestPool> {
                every { getNextTestChunk() } returnsMany listOf(TestChunk(0, mockk(), mockk()), null)
            }
        }

        given("A single passed test sequence") {
            val testChunkRunner by memoized {
                mockk<TestChunkRunner> {
                    coEvery { run(args = any(), testChunk = any()) } returnsMany listOf(passedTestChunkResults)
                }
            }

            it("Starts logcatRecorder then installs app apk then runs testChunk then stops logcatRecorder") {
                val args = Args().apply {
                    appApkPath = "somePath"
                }
                TestRunFactory().runTestSession(adbDevice, args, testPool, logcatFileIO, logcatRecorder, installer, filePuller, testChunkRunner)
                        .test()
                        .await()

                coVerify(ordering = Ordering.SEQUENCE) {
                    logcatRecorder.start()
                    testChunkRunner.run(any(), any())
                    logcatRecorder.stop()
                }
            }
        }

        given("A single passed+ignored+failed test sequence") {
            val testChunkResults = listOf(createPassedTest(adbDevice),
                                          createIgnoredTest(adbDevice),
                                          createFailedTest(adbDevice))
            val testChunkRunner by memoized {
                mockk<TestChunkRunner> {
                    coEvery { run(args = any(), testChunk = any()) } returnsMany listOf(failedTestChunkResults)
                }
            }
            val passedTestDetails = failedTestChunkResults
                    .filter { it.status is AdbDeviceTestResult.Status.Passed }
                    .map { TestDetails(it.className, it.testName) }

            given("defined test files folders") {
                it("It pulls test files only on non-ignored tests") {
                    val args = Args().apply {
                        appApkPath = "somePath"
                        testFilesPullDeviceDirectory = "somePath"
                        testFilesPullHostDirectory = "somePath"
                    }
                    TestRunFactory().runTestSession(adbDevice, args, testPool, logcatFileIO, logcatRecorder, installer, filePuller, testChunkRunner)
                            .test()
                            .await()

                    coVerify(ordering = Ordering.ALL) {
                        passedTestDetails.forEach {
                            filePuller.pullTestFolder(any(), any(), eq(it), any())
                        }
                    }
                }
            }

            given("no defined test files folders") {
                it("It does not pull test files") {
                    val args = Args().apply {
                        appApkPath = "somePath"
                    }

                    TestRunFactory().runTestSession(adbDevice, args, testPool, logcatFileIO, logcatRecorder, installer, filePuller, testChunkRunner)
                            .test()
                            .await()

                    coVerify(ordering = Ordering.UNORDERED, inverse = true) {
                        passedTestDetails.forEach {
                            filePuller.pullTestFolder(any(), any(), eq(it), any())
                        }
                    }
                }
            }
        }

        given("A twice crashed then failed then passed test sequence") {
            val testChunkRunner by memoized {
                mockk<TestChunkRunner> {
                    coEvery { run(args = any(), testChunk = any()) } returnsMany listOf(crashedTestChunkResults,
                                                                                        crashedTestChunkResults,
                                                                                        failedTestChunkResults,
                                                                                        passedTestChunkResults)
                }
            }

            on("Max retry count 3") {
                val args = Args().apply {
                    retriesPerChunk = 3
                }

                it("Retries until success and outputs test results with all passed tests") {
                    TestRunFactory().runTestSession(adbDevice, args, testPool, logcatFileIO, logcatRecorder, installer, filePuller, testChunkRunner)
                            .test()
                            .await()
                            .assertValue { it.testResults.containsAll(passedTestChunkResults) }
                }
            }

            on("Max retry count 2") {
                val args = Args().apply {
                    retriesPerChunk = 2
                }

                it("Retries until max retries and outputs test results with failed tests") {
                    TestRunFactory().runTestSession(adbDevice, args, testPool, logcatFileIO, logcatRecorder, installer, filePuller, testChunkRunner)
                            .test()
                            .await()
                            .assertValue { it.testResults.containsAll(failedTestChunkResults) }
                }
            }

            on("Max retry count 1") {
                val args = Args().apply {
                    retriesPerChunk = 1
                }

                it("Retries until max retries and outputs test results with failed crashed tests") {
                    TestRunFactory().runTestSession(adbDevice, args, testPool, logcatFileIO, logcatRecorder, installer, filePuller, testChunkRunner)
                            .test()
                            .await()
                            .assertValue { it.testResults.containsAll(crashedTestChunkResults) }
                }
            }
        }
    }
})

private fun createCrashedTests(adbDevice: AdbDevice): List<AdbDeviceTestResult> {
    return List(4) {
        AdbDeviceTestResult(adbDevice, "someClass", "someTest", AdbDeviceTestResult.Status.Failed("crashed stackTrace"), 1, mockk())
    }
}

private fun createFailedTest(adbDevice: AdbDevice): AdbDeviceTestResult {
    return AdbDeviceTestResult(adbDevice, "someClass", "someTest", AdbDeviceTestResult.Status.Failed("stackTrace"), 1, mockk())
}

private fun createPassedTest(adbDevice: AdbDevice): AdbDeviceTestResult {
    return AdbDeviceTestResult(adbDevice, "someClass", "someTest", AdbDeviceTestResult.Status.Passed, 1, mockk())
}

private fun createIgnoredTest(adbDevice: AdbDevice): AdbDeviceTestResult {
    return AdbDeviceTestResult(adbDevice, "someClass", "someTest", AdbDeviceTestResult.Status.Ignored("stackTrace"), 1, mockk())
}
