package com.workday.torque

import io.mockk.Ordering
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Observable
import kotlinx.coroutines.runBlocking
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import java.io.File

class ScreenRecorderSpec : Spek(
{
    val adbDevice = AdbDevice("id", "model", true)
    val processRunner by memoized {
        mockk<ProcessRunner>(relaxed = true) {
            every { runAdb(allAny()) } returns Observable.empty()
        }
    }
    val args = Args().apply {
        testFilesPullDeviceDirectory = "someDeviceDir"
        recordFailedTests = true
    }
    val videoRecorder by memoized {
        ScreenRecorder().apply {
            this.args = args
            this.adbDevice = adbDevice
            this.processRunner = processRunner
        }
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
                val videoDir = File("someDeviceDir/videos/id/someTestClass/someTestMethod")
                val mkdirCommandAndArgsMatcher = match<List<String>> {
                    it[0] == "-s" && it[1] == adbDevice.id && it[2] == "shell" &&
                            it[3] == "mkdir -p ${videoDir.path}"
                }
                processRunner.runAdb(mkdirCommandAndArgsMatcher, any(), any(), any(), any(), any(), any())
                val videoFile = File(videoDir, "test_recording.mp4")
                val recordCommandAndArgsMatcher = match<List<String>> {
                    it[0] == "-s" && it[1] == adbDevice.id && it[2] == "shell" &&
                            it[3] == "screenrecord ${videoFile.path} --time-limit $DEFAULT_PER_CHUNK_TIMEOUT_SECONDS --size 720x1440"
                }
                processRunner.runAdb(recordCommandAndArgsMatcher, any(), any(), any(), any(), any(), any())
                val rmCommandAndArgsMatcher = match<List<String>> {
                    it[0] == "-s" && it[1] == adbDevice.id && it[2] == "shell" &&
                            it[3] == "rm ${videoFile.path}"
                }
                processRunner.runAdb(rmCommandAndArgsMatcher, any(), any(), any(), any(), any(), any())
            }
        }
    }
})