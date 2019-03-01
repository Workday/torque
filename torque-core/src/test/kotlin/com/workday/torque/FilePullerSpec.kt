package com.workday.torque

import io.mockk.mockk
import io.mockk.verify
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it

class FilePullerSpec : Spek(
{
    val adbDevice = AdbDevice("id", "model", true)
    val processRunner by memoized {
        mockk<ProcessRunner>(relaxed = true)
    }
    val filePuller by memoized {
        FilePuller(adbDevice, processRunner)
    }

    given("A test detail") {
        val hostDirectory = "someHostDir"
        val deviceDirectory = "someDeviceDir"
        val testDetails = TestDetails("com.some.package.class", "SomeTest")
        it("Runs adb pull command for that test folder") {
            filePuller.pullTestFolder(hostDirectory, deviceDirectory, testDetails, mockk())

            verify {
                val commandAndArgsMatcher = match<List<String>> {
                    it[0] == "-s" && it[1] == adbDevice.id && it[2] == "pull" &&
                            it[3] == "someDeviceDir/com.some.package.class/SomeTest" &&
                            it[4].endsWith("someHostDir/com.some.package.class/SomeTest")
                }
                processRunner.runAdb(commandAndArgsMatcher, any(), any(), any(), any(), any(), any())
            }
        }
    }
})
