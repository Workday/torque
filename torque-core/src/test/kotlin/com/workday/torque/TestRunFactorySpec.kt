package com.workday.torque

import com.workday.torque.pooling.TestChunk
import com.workday.torque.pooling.TestPool
import com.workday.torque.utils.FakeAdbTestResultFactory.createFailedTest
import com.workday.torque.utils.FakeAdbTestResultFactory.createIgnoredTest
import com.workday.torque.utils.FakeAdbTestResultFactory.createPassedTest
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
import java.nio.file.Paths

class TestRunFactorySpec : Spek(
{
    val workingDirectory = Paths.get("./").toAbsolutePath().toString()
    context("Run test chunk") {
        val adbDevice = AdbDevice("id", "model", online = true)
        val logcatFileIO = mockk<LogcatFileIO>(relaxed = true)
        val logcatRecorder = mockk<LogcatRecorder>(relaxed = true)
        val filePuller by memoized {
            mockk<FilePuller> {
                every { pullFolder(any(), any()) } returns Completable.complete()
            }
        }
        val installer = mockk<Installer>(relaxed = true)
        val testChunkRunner = mockk<TestChunkRunner>(relaxed = true)

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
            val chunkRetryer by memoized {
                mockk<TestChunkRetryer> {
                    coEvery { runTestChunkWithRetry(testChunk = any()) } returnsMany listOf(passedTestChunkResults)
                }
            }

            it("Starts logcatRecorder then installs app apk then runs testChunk then stops logcatRecorder") {
                val args = Args().apply {
                    appApkPath = "somePath"
                }
                TestRunFactory(workingDirectory).runTestSession(adbDevice, args, testPool, logcatFileIO, logcatRecorder, installer, filePuller, testChunkRunner, chunkRetryer)
                        .test()
                        .await()

                coVerify(ordering = Ordering.SEQUENCE) {
                    logcatRecorder.start(any())
                    chunkRetryer.runTestChunkWithRetry(any())
                    logcatRecorder.stop()
                }
            }
        }

        given("A single passed+ignored+failed test sequence") {
            val testChunkResults = listOf(createPassedTest(adbDevice),
                                          createIgnoredTest(adbDevice),
                                          createFailedTest(adbDevice))
            val chunkRetryer by memoized {
                mockk<TestChunkRetryer> {
                    coEvery { runTestChunkWithRetry(testChunk = any()) } returnsMany listOf(testChunkResults)
                }
            }
            given("defined test files folders") {
                it("pulls test files") {
                    val args = Args().apply {
                        appApkPath = "somePath"
                        testFilesPullDeviceDirectory = "somePath"
                        testFilesPullHostDirectory = "somePath"
                    }
                    TestRunFactory(workingDirectory).runTestSession(adbDevice, args, testPool, logcatFileIO, logcatRecorder, installer, filePuller, testChunkRunner, chunkRetryer)
                            .test()
                            .await()

                    coVerify(ordering = Ordering.ALL) {
                        filePuller.pullFolder(args, any())
                    }
                }
            }

            given("no defined test files folders") {
                it("does not pull test files") {
                    val args = Args().apply {
                        appApkPath = "somePath"
                    }

                    TestRunFactory(workingDirectory).runTestSession(adbDevice, args, testPool, logcatFileIO, logcatRecorder, installer, filePuller, testChunkRunner, chunkRetryer)
                            .test()
                            .await()

                    coVerify(inverse = true) {
                        filePuller.pullFolder(args, any())
                    }
                }
            }
        }
    }
})
