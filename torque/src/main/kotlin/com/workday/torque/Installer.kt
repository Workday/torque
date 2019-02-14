package com.workday.torque

import com.gojuno.commander.os.Notification
import com.workday.torque.pooling.TestChunk
import com.workday.torque.pooling.TestModuleInfo
import io.reactivex.Completable
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

class Installer(private val adbDevice: AdbDevice, private val processRunner: ProcessRunner = ProcessRunner()) {

    fun ensureTestPackageInstalled(args: Args, testChunk: TestChunk): Completable {
        val testPackage = testChunk.testModuleInfo.testPackage.value
        return if (adbDevice.installedPackages.contains(testPackage)) {
            adbDevice.log("Packaged installed, package: $testPackage")
            Completable.complete()
        } else {
            installApk(testChunk.testModuleInfo, args)
        }
    }

    private fun installApk(testModuleInfo: TestModuleInfo, args: Args): Completable {
        val pathToApk = testModuleInfo.pathToApk
        val testPackage = testModuleInfo.testPackage.value

        return installApk(pathToApk, args)
                .doOnComplete { adbDevice.installedPackages.add(testPackage) }
    }

    fun installApk(pathToApk: String, args: Args): Completable {
        val installTimeout = Timeout(args.installTimeoutSeconds, TimeUnit.SECONDS)
        var retryCount = 0

        return Observable
                .fromCallable { System.currentTimeMillis() }
                .flatMap { startTimeMillis -> installApk(pathToApk, installTimeout).map { it to startTimeMillis } }
                .map { (exit, startTimeMillis) ->
                    val success = exit
                            .preprocessOutput()
                            .filter { it.isNotEmpty() }
                            .firstOrNull { it.equals("Success", ignoreCase = true) } != null

                    val duration = System.currentTimeMillis() - startTimeMillis

                    when (success) {
                        true -> {
                            adbDevice.log("Successfully installed apk in ${duration.millisToHoursMinutesSeconds()}, pathToApk = $pathToApk")
                        }

                        false -> {
                            adbDevice.log("Failed to install apk $pathToApk")
                            throw IllegalStateException("Apk install failed")
                        }
                    }
                }
                .ignoreElements()
                .doOnSubscribe { adbDevice.log("Installing apk... pathToApk = $pathToApk") }
                .doOnError { adbDevice.log("Error during installing apk: $it, pathToApk = $pathToApk, retry count: ${retryCount++}/${args.retriesInstallPerApk}") }
                .retry(args.retriesInstallPerApk)
    }

    private fun installApk(pathToApk: String, installTimeout: Timeout): Observable<Notification.Exit> {
        return processRunner.runAdb(
                commandAndArgs = listOf("-s", adbDevice.id, "install", "-r", "-g", pathToApk),
                timeout = installTimeout,
                unbufferedOutput = true)
                .ofType(Notification.Exit::class.java)
    }

    private fun Notification.Exit.preprocessOutput(): List<String> {
        return output
                .readText()
                .split(System.lineSeparator())
                .map { it.trim() }
    }
}

data class Timeout(val number: Int, val timeUnit: TimeUnit) {
    fun toPair() = number to timeUnit
}
