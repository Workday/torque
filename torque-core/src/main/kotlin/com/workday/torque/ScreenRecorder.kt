package com.workday.torque

import com.gojuno.commander.os.Notification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import java.io.File
import java.util.concurrent.TimeUnit

const val TIMEOUT_BUFFER_SECONDS = 10

class ScreenRecorder(private val adbDevice: AdbDevice,
                     args: Args,
                     private val processRunner: ProcessRunner = ProcessRunner()) {
    private val videosDir = File(File(args.testFilesPullDeviceDirectory, "videos"), adbDevice.id)
    private val timeoutSeconds = args.chunkTimeoutSeconds
    private var videoRecordJob: Job? = null
    private var videoFileName: File? = null

    suspend fun start(coroutineScope: CoroutineScope, testDetails: TestDetails) {
        videoRecordJob = coroutineScope.launch { startRecordTestRun(testDetails) }
    }

    private suspend fun startRecordTestRun(testDetails: TestDetails) {
        videoFileName = getVideoFile(testDetails)

        processRunner.runAdb(commandAndArgs = listOf(
                "-s", adbDevice.id,
                "shell", "mkdir -p ${videoFileName!!.parentFile}"
        ),
                destroyOnUnsubscribe = true)
                .ofType(Notification.Exit::class.java)
                .map { true }
                .doOnError { error -> adbDevice.log("Failed to mkdir on ${adbDevice.tag}, filepath: ${videoFileName!!.parentFile}, failed: $error") }
                .ignoreElements()
                .await()

        processRunner.runAdb(commandAndArgs = listOf(
                "-s", adbDevice.id,
                "shell", "screenrecord $videoFileName --time-limit $timeoutSeconds --size 720x1440"
        ),
                destroyOnUnsubscribe = true,
                timeout = Timeout(timeoutSeconds.toInt() + TIMEOUT_BUFFER_SECONDS, TimeUnit.SECONDS))
                .ofType(Notification.Exit::class.java)
                .map { true }
                .doOnSubscribe { adbDevice.log("Started recording on ${adbDevice.tag}, filename: $videoFileName") }
                .doOnComplete { adbDevice.log("Ended recording on ${adbDevice.tag}, filename: $videoFileName") }
                .doOnError { error -> adbDevice.log("Failed to record on ${adbDevice.tag}, filename: $videoFileName, failed: $error") }
                .ignoreElements()
                .await()
    }

    private fun getVideoFile(testDetails: TestDetails): File {
        val testFolder = File(File(videosDir, testDetails.testClass), testDetails.testName)
        return File(testFolder, "test_recording.mp4")
    }

    fun stop() = videoRecordJob?.cancel()

    suspend fun removeLastFile() {
        checkNotNull(videoFileName) { "Filename cannot be null, must call start() first" }
        processRunner.runAdb(commandAndArgs = listOf(
                "-s", adbDevice.id,
                "shell", "rm $videoFileName"
        ),
                destroyOnUnsubscribe = true)
                .ofType(Notification.Exit::class.java)
                .map { true }
                .ignoreElements()
                .await()
    }
}