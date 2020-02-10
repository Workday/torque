package com.workday.torque

import com.workday.torque.dagger.DaggerSessionComponent
import com.workday.torque.dagger.RootComponent
import com.workday.torque.pooling.TestPool
import io.reactivex.Single
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.rx2.asSingle
import kotlinx.coroutines.rx2.await
import javax.inject.Inject

class DeviceTestRunSession(adbDevice: AdbDevice, private val testPool: TestPool, rootComponent: RootComponent) {

    @Inject internal lateinit var adbDevice: AdbDevice
    @Inject internal lateinit var args: Args
    @Inject internal lateinit var logcatFileIO: LogcatFileIO
    @Inject internal lateinit var logcatRecorder: LogcatRecorder
    @Inject internal lateinit var filePuller: FilePuller
    @Inject internal lateinit var testChunkRetryer: TestChunkRetryer

    init {
        DaggerSessionComponent.builder()
                .rootComponent(rootComponent)
                .adbDevice(adbDevice)
                .build()
                .injectDeviceTestRunSession(this)
    }

    fun run(): Single<AdbDeviceTestSession> {
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
