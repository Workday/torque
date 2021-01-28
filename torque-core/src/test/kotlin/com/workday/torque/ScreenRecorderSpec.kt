package com.workday.torque

import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Observable
import kotlinx.coroutines.runBlocking
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import java.util.concurrent.TimeUnit

class ScreenRecorderSpec : Spek(
{
    val adbDevice = AdbDevice("id", "model", true)
    val processRunner by memoized {
        mockk<ProcessRunner>(relaxed = true) {
            coEvery { runAdb(allAny()) } returns Observable.empty()
        }
    }
    val args = Args().apply {
        testFilesPullDeviceDirectory = "someDeviceDir"
        recordFailedTests = true
    }
    val videoRecorder by memoized {
        ScreenRecorder(adbDevice, args, processRunner)
    }

    val testDetails = TestDetails("someTestClass", "someTestMethod")

    given("ScreenRecorder session") {
        it("start calls adb mkdir screen record and rm command for a test with correct path") {
            runBlocking {
                videoRecorder.start(this, testDetails)
            }
            runBlocking {
                videoRecorder.removeLastFile()
            }

            verify(ordering = Ordering.ORDERED) {
                val mkdirCommandAndArgsMatcher = match<List<String>> {
                    it[0] == "-s" && it[1] == adbDevice.id && it[2] == "shell" &&
                            it[3] == "mkdir -p someDeviceDir/videos/id/someTestClass/someTestMethod"
                }
                processRunner.runAdb(mkdirCommandAndArgsMatcher, any(), any(), any(), any(), any(), any())
                val recordCommandAndArgsMatcher = match<List<String>> {
                    it[0] == "-s" && it[1] == adbDevice.id && it[2] == "shell" &&
                            it[3] == "screenrecord someDeviceDir/videos/id/someTestClass/someTestMethod/test_recording.mp4 --time-limit $DEFAULT_PER_CHUNK_TIMEOUT_SECONDS --size 720x1440"
                }
                val expectedTimeout = Timeout(
                        DEFAULT_PER_CHUNK_TIMEOUT_SECONDS.toInt() + TIMEOUT_BUFFER_SECONDS,
                        TimeUnit.SECONDS)

                processRunner.runAdb(commandAndArgs = recordCommandAndArgsMatcher,
                        timeout = expectedTimeout,
                        redirectOutputTo = any(),
                        keepOutputOnExit = any(),
                        unbufferedOutput = any(),
                        print = any(),
                        destroyOnUnsubscribe = any())

                val rmCommandAndArgsMatcher = match<List<String>> {
                    it[0] == "-s" && it[1] == adbDevice.id && it[2] == "shell" &&
                            it[3] == "rm someDeviceDir/videos/id/someTestClass/someTestMethod/test_recording.mp4"
                }
                processRunner.runAdb(rmCommandAndArgsMatcher, any(), any(), any(), any(), any(), any())
            }
        }
    }
})