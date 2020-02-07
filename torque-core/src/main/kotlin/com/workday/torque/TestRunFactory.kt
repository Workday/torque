package com.workday.torque

import com.workday.torque.pooling.TestPool
import io.reactivex.Single
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.rx2.asSingle
import kotlinx.coroutines.rx2.await

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
            logcatRecorder: LogcatRecorder = LogcatRecorder(adbDevice, logcatFileIO),
            installer: Installer = Installer(adbDevice),
            filePuller: FilePuller = FilePuller(adbDevice),
            testChunkRunner: TestChunkRunner = TestChunkRunner(adbDevice, logcatFileIO, installer),
            testChunkRetryer: TestChunkRetryer = TestChunkRetryer(adbDevice, args, logcatFileIO, testChunkRunner, installer)
    ): Single<AdbDeviceTestSession> {
        val testSession = AdbDeviceTestSession(adbDevice = adbDevice,
                                               logcatFile = logcatFileIO.fullLogcatFile)
        return GlobalScope.async(
                context = Dispatchers.Default,
                start = CoroutineStart.DEFAULT,
                block = {
                    testSession.startTimestampMillis = System.currentTimeMillis()
                    logcatRecorder.start(coroutineScope = this)
                    do {
                        val chunk = testPool.getNextTestChunk()
                        if (chunk != null) {
                            val deviceTestsResults = testChunkRetryer.runTestChunkWithRetry(chunk)
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
                    pullDeviceFiles(args, filePuller)

                    testSession
                }
        ).asSingle(Dispatchers.Default)
    }

    private suspend fun pullDeviceFiles(args: Args, filePuller: FilePuller) {
        if (args.testFilesPullDeviceDirectory.isEmpty() || args.testFilesPullHostDirectory.isEmpty()) {
            return
        }

        filePuller.pullFolder(args).await()
    }
}
