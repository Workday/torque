package com.workday.torque

import com.gojuno.commander.os.Notification
import com.workday.torque.pooling.TestChunk
import com.workday.torque.pooling.TestModuleInfo
import com.workday.torque.utils.createTestMethodsList
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.junit.Assert.assertTrue
import java.io.File

class InstallerSpec : Spek(
{
    context("Installing apk") {
        val adbDevice by memoized { AdbDevice("id", "model", online = true) }

        val testPackage = TestPackage.Valid("com.company.mymodule.test")
        val testRunner = TestRunner.Valid("android.support.test.runner.AndroidJUnitRunner")
        val pathToApk = "test.apk"
        val moduleInfo = TestModuleInfo(testPackage, testRunner, pathToApk)
        val testChunk = TestChunk(0, moduleInfo, createTestMethodsList(5))
        val successfulInstallOutputFile = File("successful-install-output.txt")
        successfulInstallOutputFile.createNewFile()
        successfulInstallOutputFile.writeText("Success")
        val successfulNotification: Observable<Notification> = Observable.just(Notification.Exit(successfulInstallOutputFile))
        val failedInstallOutputFile = File("failed-install-output.txt")
        failedInstallOutputFile.createNewFile()
        failedInstallOutputFile.writeText("Failed")
        val failedNotification: Observable<Notification> = Observable.just(Notification.Exit(failedInstallOutputFile))

        given("Successful install") {
            val installer by memoized {
                val processRunner = mockk<ProcessRunner> {
                    every {
                        runAdb(allAny())
                    } returns successfulNotification
                }
                Installer(adbDevice, processRunner)
            }


            it("Adds installed package to installedPackages on AdbDevice") {
                installer.ensureTestPackageInstalled(Args(), testChunk)
                        .test()
                        .await()

                assertTrue(adbDevice.installedPackages.contains(testPackage.value))
            }
        }

        given("A twice failed then succeeded install sequence") {
            val installer by memoized {
                val processRunner = mockk<ProcessRunner> {
                    every {
                        runAdb(allAny())
                    } returnsMany listOf(failedNotification, failedNotification, successfulNotification)
                }
                Installer(adbDevice, processRunner)
            }

            on("Max retry count higher than failed occurrences") {
                val args = Args().apply {
                    retriesInstallPerApk = 2
                }

                it("Retries until success and adds installed package to installedPackages on AdbDevice") {
                    installer.ensureTestPackageInstalled(args, testChunk)
                            .test()
                            .await()

                    assertTrue(adbDevice.installedPackages.contains(testPackage.value))
                }
            }

            on("Max retry count lower than failed occurrences") {
                val args = Args().apply {
                    retriesInstallPerApk = 1
                }

                it("Retries until max retries and errors out") {
                    installer.ensureTestPackageInstalled(args, testChunk)
                            .test()
                            .assertError { it is IllegalStateException && it.message.equals("Apk install failed") }
                }
            }
        }
    }
})

