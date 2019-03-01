package com.workday.torque

import com.gojuno.commander.os.Notification
import io.reactivex.Completable
import java.io.File

private const val MAX_RETRIES: Long = 3

class FilePuller(private val adbDevice: AdbDevice,
                 private val processRunner: ProcessRunner = ProcessRunner()) {

    fun pullTestFolder(
            hostDirectory: String,
            deviceDirectory: String,
            testDetails: TestDetails,
            timeout: Timeout
    ): Completable {
        val folderOnHostMachine = hostDirectory.setupFolderPathForTestDetails(testDetails)
        val folderOnHostMachineFile = File(folderOnHostMachine)
        folderOnHostMachineFile.mkdirs()
        val folderOnDevice = deviceDirectory.setupFolderPathForTestDetails(testDetails)
        val pullFiles = processRunner.runAdb(
                commandAndArgs = listOf("-s", adbDevice.id, "pull", folderOnDevice, folderOnHostMachineFile.absolutePath),
                timeout = timeout,
                unbufferedOutput = true
        )

        return pullFiles
                .ofType(Notification.Exit::class.java)
                .retry(MAX_RETRIES)
                .map { true }
                .doOnComplete { adbDevice.log("Pulled files from $folderOnDevice to $folderOnHostMachine") }
                .doOnError { error -> adbDevice.log("Failed to pull files from $folderOnDevice to $folderOnHostMachine failed: $error") }
                .ignoreElements()
                .onErrorComplete()
    }

    private fun String.setupFolderPathForTestDetails(testDetails: TestDetails): String {
        return "$this/${testDetails.testClass}/${testDetails.testName}"
    }
}