package com.workday.torque

import com.gojuno.commander.os.Notification
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.rx2.awaitFirst
import kotlinx.coroutines.rx2.openSubscription
import java.io.File
import java.util.concurrent.TimeUnit

private const val FULL_LOGCAT_FILE_NAME = "full.logcat"

class LogcatFileIO(
    private val adbDevice: AdbDevice,
    private val timeoutSeconds: Int,
    outputDirPath: String,
    private val processRunner: ProcessRunner = ProcessRunner(),
    private val verboseOutput: Boolean = false,
) {

    private val logsDir = File(File(outputDirPath, "logs"), adbDevice.id)
    val fullLogcatFile = File(logsDir, FULL_LOGCAT_FILE_NAME)

    fun tailFile(): ReceiveChannel<String> = tail(fullLogcatFile).toV2Observable().openSubscription()

    internal suspend fun redirectLogcatToFile() {
        fullLogcatFile.parentFile.mkdirs()
        clearLogcat()
        processRunner.runAdb(
            commandAndArgs = listOf("-s", adbDevice.id, "logcat"),
            timeout = Timeout(timeoutSeconds, TimeUnit.SECONDS),
            redirectOutputTo = fullLogcatFile,
            keepOutputOnExit = true
        )
            .ofType(Notification.Start::class.java)
            .doOnError {
                when (it) {
                    is InterruptedException -> Unit // Expected case, interrupt comes from System.exit(0).
                    else -> adbDevice.log("Error during redirecting logcat to file $fullLogcatFile, error = $it")
                }
            }
            .awaitFirst()
    }

    private suspend fun clearLogcat() {
        try {
            processRunner.runAdb(
                commandAndArgs = listOf("-s", adbDevice.id, "logcat", "-c"),
                timeout = Timeout(timeoutSeconds, TimeUnit.SECONDS),
                print = verboseOutput,
            )
                .ofType(Notification.Exit::class.java)
                .awaitFirst()
        } catch (e: Exception) {
            adbDevice.log("Could not clear logcat on device. $e")
        }
    }

    fun writeLogcatFileForTest(testLogcat: TestLogcat) {
        getLogcatFileForTest(testLogcat.testDetails)
            .apply { parentFile.mkdirs() }
            .writeText(testLogcat.logcat)
    }

    fun getLogcatFileForTest(testDetails: TestDetails): File {
        return File(File(logsDir, testDetails.testClass), "${testDetails.testName}.logcat")
    }
}
