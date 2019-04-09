package com.workday.torque

import com.linkedin.dex.parser.TestMethod
import com.workday.torque.pooling.TestChunk
import com.workday.torque.pooling.TestPool
import io.reactivex.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asSingle
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit

class TestRunFactory {

    fun runTestSession(
            adbDevice: AdbDevice,
            args: Args,
            testPool: TestPool,
            logcatFileIO: LogcatFileIO = LogcatFileIO(
                    adbDevice = adbDevice,
                    timeoutSeconds = args.chunkTimeoutSeconds.toInt(),
                    outputDirPath = args.outputDirectory
            ),
            logcatRecorder: LogcatRecorder = LogcatRecorder(logcatFileIO),
            installer: Installer = Installer(adbDevice),
            filePuller: FilePuller = FilePuller(adbDevice),
            testChunkRunner: TestChunkRunner = TestChunkRunner(adbDevice, logcatFileIO, installer)
    ): Single<AdbDeviceTestSession> {
        val testSession = AdbDeviceTestSession(adbDevice = adbDevice,
                logcatFile = logcatFileIO.fullLogcatFile)
        return GlobalScope.async(
                context = Dispatchers.Default,
                start = CoroutineStart.DEFAULT,
                block = {
                    testSession.startTimestampMillis = System.currentTimeMillis()
                    logcatRecorder.start()
                    do {
                        val chunk = testPool.getNextTestChunk()
                        if (chunk != null) {
                            val deviceTestsResults = adbDevice.runTestChunkWithRetry(args, logcatFileIO, chunk, testChunkRunner, installer)
                            pullChunkTestFiles(args, filePuller, deviceTestsResults)
                            testSession.apply {
                                testResults.addAll(deviceTestsResults)
                                passedCount += deviceTestsResults.count { it.status is AdbDeviceTestResult.Status.Passed }
                                ignoredCount += deviceTestsResults.count { it.status is AdbDeviceTestResult.Status.Ignored }
                                failedCount += deviceTestsResults.count { it.status is AdbDeviceTestResult.Status.Failed }
                            }
                        }
                    } while (chunk != null)

                    logcatRecorder.stop()
                    testSession.apply {
                        endTimestampMillis = System.currentTimeMillis()
                    }

                    adbDevice.log("Device Test Session complete, " +
                            "${testSession.passedCount} passed, " +
                            "${testSession.failedCount} failed, took " +
                            "${testSession.durationMillis.millisToHoursMinutesSeconds()}."
                    )

                    testSession
                }
        ).asSingle(Dispatchers.Default)
    }

    private suspend fun AdbDevice.runTestChunkWithRetry(
            args: Args,
            logcatFileIO: LogcatFileIO,
            testChunk: TestChunk,
            testChunkRunner: TestChunkRunner,
            installer: Installer
    ): List<AdbDeviceTestResult> {
        val timeoutMillis = getTimeoutMillis(args, installer, testChunk)

        return try {
            withTimeout(timeoutMillis) {
                var resultsResponse = testChunkRunner.run(args, testChunk)
                while (resultsResponse.needToRetry(testChunk, args.retriesPerChunk)) {
                    log("Chunk has failed tests, retry count: ${testChunk.retryCount++}/${args.retriesPerChunk}, retrying...")
                    resultsResponse = testChunkRunner.run(args, testChunk)
                }
                resultsResponse
            }
        } catch (e: TimeoutCancellationException) {
            log("TestChunk retry timeout on $tag, tests ran in chunk: ${testChunk.testMethods.getTestNames()}")
            createTimedOutAdbDeviceTestResults(this, logcatFileIO, testChunk, timeoutMillis, e)
        }
    }

    private fun getTimeoutMillis(args: Args, installer: Installer, testChunk: TestChunk): Long {
        val chunkTimeOutWithRetries = TimeUnit.SECONDS.toMillis(args.chunkTimeoutSeconds) * args.retriesPerChunk
        val installTimeOutWithRetries = TimeUnit.SECONDS.toMillis(args.installTimeoutSeconds.toLong()) * args.retriesInstallPerApk

        return if (installer.isChunkApkInstalled(testChunk))
            chunkTimeOutWithRetries
        else
            chunkTimeOutWithRetries + installTimeOutWithRetries
    }


    private fun List<TestMethod>.getTestNames() = map { it.testName }

    private fun List<AdbDeviceTestResult>?.needToRetry(testChunk: TestChunk, retriesPerChunk: Int): Boolean {
        return (this == null || hasFailedTests()) && isRetryable(testChunk.retryCount, retriesPerChunk)
    }

    private fun List<AdbDeviceTestResult>.hasFailedTests(): Boolean {
        return any { it.status is AdbDeviceTestResult.Status.Failed }
    }

    private fun isRetryable(retryCount: Int, maxRetryCount: Int) = retryCount < maxRetryCount

    private fun createTimedOutAdbDeviceTestResults(
            adbDevice: AdbDevice,
            logcatFileIO: LogcatFileIO,
            testChunk: TestChunk,
            timeoutMillis: Long,
            e: TimeoutCancellationException
    ): List<AdbDeviceTestResult> {
        return testChunk.testMethods
                .map { it.toTestDetails() }
                .map { testDetails: TestDetails ->
                    AdbDeviceTestResult(
                            adbDevice = adbDevice,
                            className = testDetails.testClass,
                            testName = testDetails.testName,
                            status = AdbDeviceTestResult.Status.Failed("Timed out with exception: ${e.message}"),
                            durationMillis = timeoutMillis,
                            logcatFile = logcatFileIO.getLogcatFileForTest(testDetails))
                }
    }

    private fun CoroutineScope.pullChunkTestFiles(
            args: Args, filePuller: FilePuller,
            deviceTestsResults: List<AdbDeviceTestResult>
    ) {
        if (args.testFilesPullDeviceDirectory.isEmpty() || args.testFilesPullHostDirectory.isEmpty()) {
            return
        }
        val completedTestResults = deviceTestsResults.filter { it.status !is AdbDeviceTestResult.Status.Ignored }
        if (completedTestResults.isEmpty()) {
            return
        }

        launch {
            val pullFileTimeout = Timeout(args.installTimeoutSeconds, TimeUnit.SECONDS)
            completedTestResults
                    .map { TestDetails(it.className, it.testName) }
                    .forEach { testDetails: TestDetails ->
                        filePuller.pullTestFolder(args.testFilesPullDeviceDirectory,
                                args.testFilesPullHostDirectory,
                                testDetails,
                                pullFileTimeout).await()
                    }
        }
    }
}
