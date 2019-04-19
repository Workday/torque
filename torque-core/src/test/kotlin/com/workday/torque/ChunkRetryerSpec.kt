package com.workday.torque

import com.workday.torque.pooling.TestChunk
import com.workday.torque.utils.FakeAdbTestResultFactory.createCrashedTests
import com.workday.torque.utils.FakeAdbTestResultFactory.createFailedTest
import com.workday.torque.utils.FakeAdbTestResultFactory.createPassedTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import kotlin.test.assertTrue

class ChunkRetryerSpec : Spek(
{
    context("Run test chunk") {
        val adbDevice = AdbDevice("id", "model", online = true)
        val logcatFileIO = mockk<LogcatFileIO>(relaxed = true)

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
                val chunkRetryer = ChunkRetryer(adbDevice, args, logcatFileIO, testChunkRunner, installer)

                it("Retries until success and outputs test results with all passed tests") {
                    runBlocking {
                        val testResults = chunkRetryer.runTestChunkWithRetry(TestChunk(0, mockk(), mockk()))
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
                val chunkRetryer = ChunkRetryer(adbDevice, args, logcatFileIO, testChunkRunner, installer)

                it("Retries until max retries and outputs test results with failed tests") {
                    runBlocking {
                        val testResults = chunkRetryer.runTestChunkWithRetry(TestChunk(0, mockk(), mockk()))
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
                val chunkRetryer = ChunkRetryer(adbDevice, args, logcatFileIO, testChunkRunner, installer)

                it("Retries until max retries and outputs test results with failed crashed tests") {
                    runBlocking {
                        val testResults = chunkRetryer.runTestChunkWithRetry(TestChunk(0, mockk(), mockk()))
                        assertTrue {
                            testResults.containsAll(crashedTestChunkResults)
                        }
                    }
                }
            }
        }
    }
})