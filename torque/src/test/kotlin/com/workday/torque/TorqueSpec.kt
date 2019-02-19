package com.workday.torque

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Single
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import kotlin.test.assertFailsWith

class TorqueSpec : Spek (
{
    context("Run Torque") {
        val args = Args()
        val moduleTestParser = mockk<ModuleTestParser> {
            every { parseTestsFromModuleApks() } returns listOf()
        }
        val adbDeviceA = AdbDevice("aId", "model", online = true)
        val adbDeviceB = AdbDevice("bId", "model", online = true)
        val adbDeviceFinder = mockk<AdbDeviceFinder> {
            every { onlineAdbDevices() } returns Single.just(listOf(adbDeviceA, adbDeviceB))
        }
        val testSessionA = AdbDeviceTestSession(adbDeviceA, logcatFile = mockk())
        val testSessionB = AdbDeviceTestSession(adbDeviceB, logcatFile = mockk())
        val testRunFactory = mockk<TestRunFactory> {
            every { runTestSession(adbDeviceA, any(), any(), any(), any(), any(), any(), any()) } returns Single.just(testSessionA)
            every { runTestSession(adbDeviceB, any(), any(), any(), any(), any(), any(), any()) } returns Single.just(testSessionB)
        }
        val resultWriter = mockk<ResultWriter>(relaxed = true)

        given("completed test sessions") {
            val torque = Torque(args, moduleTestParser, adbDeviceFinder, testRunFactory, resultWriter)
            it("Parses tests from apks and starts TestRuns on connectedDevices and writes the results") {
                torque.run()

                verify {
                    moduleTestParser.parseTestsFromModuleApks()
                    resultWriter.clearOutputDirectory()
                    adbDeviceFinder.onlineAdbDevices()
                    testRunFactory.runTestSession(adbDeviceA, any(), any(), any(), any(), any(), any(), any())
                    testRunFactory.runTestSession(adbDeviceB, any(), any(), any(), any(), any(), any(), any())
                    resultWriter.write(any(), listOf(testSessionA, testSessionB))
                }
            }
        }

        given("ModuleTestParser failed with exception") {
            val failedModuleTestParser = mockk<ModuleTestParser> {
                every { parseTestsFromModuleApks() } throws IllegalStateException("apk parse error")
            }
            val torque = Torque(args, failedModuleTestParser, adbDeviceFinder, testRunFactory, resultWriter)
            it("passes that exception to the caller") {
                assertFailsWith(IllegalStateException::class, "apk parse error") {
                    torque.run()
                }
            }
        }

        given("AdbDeviceFinder failed with exception") {
            val failedAdbDeviceFinder = mockk<AdbDeviceFinder> {
                every { onlineAdbDevices() } throws IllegalStateException("Error: No devices available for tests.")
            }
            val torque = Torque(args, moduleTestParser, failedAdbDeviceFinder, testRunFactory, resultWriter)
            it("passes that exception to the caller") {
                assertFailsWith(IllegalStateException::class, "Error: No devices available for tests.") {
                    torque.run()
                }
            }
        }

        given("ResultWriter failed with exception") {
            val failedResultWriter = mockk<ResultWriter>(relaxed = true) {
                every { write(any(), any()) } throws IllegalStateException("Error: 0 tests were run.")
            }
            val torque = Torque(args, moduleTestParser, adbDeviceFinder, testRunFactory, failedResultWriter)
            it("passes that exception to the caller") {
                assertFailsWith(IllegalStateException::class, "Error: 0 tests were run.") {
                    torque.run()
                }
            }
        }
    }
})
