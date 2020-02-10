package com.workday.torque

import com.gojuno.commander.os.Notification
import com.workday.torque.dagger.SessionScope
import io.reactivex.Completable
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val MAX_RETRIES: Long = 3

@SessionScope
class FilePuller @Inject constructor() {

    @Inject internal lateinit var adbDevice: AdbDevice
    @Inject internal lateinit var processRunner: ProcessRunner

    fun pullFolder(args: Args, subFolder: String = ""): Completable {
        val pullFileTimeout = Timeout(args.installTimeoutSeconds, TimeUnit.SECONDS)
        val folderOnHostMachine = args.testFilesPullHostDirectory
        val folderOnHostMachineFile = File(folderOnHostMachine)
        folderOnHostMachineFile.mkdirs()
        val folderOnDevice = "${args.testFilesPullDeviceDirectory}/$subFolder"
        val pullFiles = processRunner.runAdb(
                commandAndArgs = listOf("-s", adbDevice.id, "pull", folderOnDevice, folderOnHostMachineFile.absolutePath),
                timeout = pullFileTimeout,
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
}