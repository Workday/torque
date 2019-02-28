package com.workday.torque

import com.gojuno.commander.os.Notification
import com.workday.torque.pooling.ModuleInfo
import com.workday.torque.pooling.TestChunk
import com.workday.torque.pooling.TestModuleInfo
import io.reactivex.Completable
import io.reactivex.Observable
import kotlinx.coroutines.rx2.await
import java.util.concurrent.TimeUnit

class Installer(private val adbDevice: AdbDevice, private val processRunner: ProcessRunner = ProcessRunner()) {

    suspend fun ensureTestPackageInstalled(args: Args, testChunk: TestChunk) {
        val testPackage = testChunk.testModuleInfo.moduleInfo.apkPackage.value
        if (adbDevice.installedPackages.contains(testPackage)) {
            adbDevice.log("Packaged installed, package: $testPackage")
        } else {
            installModuleApks(testChunk.testModuleInfo, args)
        }
    }

    private suspend fun installModuleApks(testModuleInfo: TestModuleInfo, args: Args) {
        val targetModuleInfo = testModuleInfo.appModuleInfo

        if (args.enforceSingleModule) {
            adbDevice.installedPackages.forEach { modulePackage ->
                uninstallAndUpdateInstalledPackages(modulePackage, args)
            }
        }

        installAndUpdateInstalledPackages(testModuleInfo.moduleInfo, args)
        if (targetModuleInfo != null) {
            installAndUpdateInstalledPackages(targetModuleInfo, args)
        }
    }

    private suspend fun uninstallAndUpdateInstalledPackages(modulePackage: String, args: Args) {
        uninstallApk(modulePackage, args).await()
        adbDevice.installedPackages.remove(modulePackage)
    }

    private fun uninstallApk(modulePackage: String, args: Args): Completable {
        val installTimeout = Timeout(args.installTimeoutSeconds, TimeUnit.SECONDS)
        var retryCount = 0

        return Observable
                .fromCallable { System.currentTimeMillis() }
                .flatMap { startTimeMillis ->
                    adbUninstallApk(modulePackage,
                                    installTimeout).map { it to startTimeMillis }
                }
                .map { (exit, startTimeMillis) ->
                    val success = exit
                            .preprocessOutput()
                            .filter { it.isNotEmpty() }
                            .firstOrNull { it.equals("Success", ignoreCase = true) } != null

                    val duration = System.currentTimeMillis() - startTimeMillis

                    when (success) {
                        true -> {
                            adbDevice.log("Successfully uninstalled package in ${duration.millisToHoursMinutesSeconds()}, modulePackage = $modulePackage")
                        }

                        false -> {
                            throw IllegalStateException("Package uninstall failed")
                        }
                    }
                }
                .ignoreElements()
                .doOnSubscribe { adbDevice.log("Uninstalling package... modulePackage = $modulePackage") }
                .doOnError { adbDevice.log("Error during uninstalling package: $it, modulePackage = $modulePackage, retry count: ${retryCount++}/${args.retriesInstallPerApk}") }
                .retry(args.retriesInstallPerApk)
    }

    private fun adbUninstallApk(modulePackage: String, installTimeout: Timeout): Observable<Notification.Exit> {
        return processRunner.runAdb(
                commandAndArgs = listOf("-s", adbDevice.id, "uninstall", modulePackage),
                timeout = installTimeout,
                unbufferedOutput = true)
                .ofType(Notification.Exit::class.java)
    }

    private suspend fun installAndUpdateInstalledPackages(moduleInfo: ModuleInfo, args: Args) {
        installApk(moduleInfo.pathToApk, args).await()
        adbDevice.installedPackages.add(moduleInfo.apkPackage.value)
    }

    private fun installApk(pathToApk: String, args: Args): Completable {
        val installTimeout = Timeout(args.installTimeoutSeconds, TimeUnit.SECONDS)
        var retryCount = 0
        return Observable
                .fromCallable { System.currentTimeMillis() }
                .flatMap { startTimeMillis -> adbInstallApk(pathToApk, installTimeout).map { it to startTimeMillis } }
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
                            throw IllegalStateException("Apk install failed")
                        }
                    }
                }
                .ignoreElements()
                .doOnSubscribe { adbDevice.log("Installing apk... pathToApk = $pathToApk") }
                .doOnError { adbDevice.log("Error during installing apk: $it, pathToApk = $pathToApk, retry count: ${retryCount++}/${args.retriesInstallPerApk}") }
                .retry(args.retriesInstallPerApk)
    }

    private fun adbInstallApk(pathToApk: String, installTimeout: Timeout): Observable<Notification.Exit> {
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
