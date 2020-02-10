package com.workday.torque

import com.gojuno.commander.os.Notification
import com.workday.torque.dagger.SessionScope
import com.workday.torque.pooling.TestChunk
import io.reactivex.Single
import kotlinx.coroutines.rx2.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@SessionScope
class TestChunkRunner @Inject constructor() {

    @Inject internal lateinit var adbDevice: AdbDevice
    @Inject internal lateinit var logcatFileIO: LogcatFileIO
    @Inject internal lateinit var installer: Installer
    @Inject internal lateinit var processRunner: ProcessRunner
    @Inject internal lateinit var instrumentationReader: InstrumentationReader

    suspend fun run(args: Args, testChunk: TestChunk): List<AdbDeviceTestResult> {
        return try {
            adbDevice.log("Starting tests...")
            installer.ensureTestPackageInstalled(args, testChunk)
            runAndParseTests(args.chunkTimeoutSeconds, testChunk).await()
        } catch (e: Exception) {
            adbDevice.log("TestChunk run crashed with exception: ${e.message}")
            createCrashedAdbDeviceTestResults(testChunk, e)
        }
    }

    private fun runAndParseTests(chunkTimeoutSeconds: Long, testChunk: TestChunk): Single<List<AdbDeviceTestResult>> {
        val testPackageName = testChunk.testModuleInfo.moduleInfo.apkPackage.value
        val testRunnerClass = testChunk.testModuleInfo.testRunner.value
        val testMethodsArgs = "-e class " + testChunk.testMethods.joinToString(",") { it.testName }
        val timeout = Timeout(chunkTimeoutSeconds.toInt(), TimeUnit.SECONDS)

        return processRunner.runAdb(
                commandAndArgs = listOf(
                        "-s", adbDevice.id,
                        "shell", "am instrument -w -r $testMethodsArgs $testPackageName/$testRunnerClass"
                ),
                timeout = timeout)
                .ofType(Notification.Start::class.java)
                .flatMap { instrumentationReader.readTestResults(it.output, chunkTimeoutSeconds) }
                .doOnNext { instrumentationTestResult -> logTestResult(instrumentationTestResult) }
                .map { instrumentationTestResult -> createAdbDeviceTestResult(instrumentationTestResult) }
                .toList()
    }

    private fun logTestResult(testResult: InstrumentationTestResult) {
        val status = when (testResult.status) {
            is InstrumentationTestResult.Status.Passed -> "passed"
            is InstrumentationTestResult.Status.Ignored -> "ignored"
            is InstrumentationTestResult.Status.Failed -> "failed"
        }

        adbDevice.log(
                "Test ${testResult.index}/${testResult.total} $status in " +
                        "${testResult.durationMillis.millisToHoursMinutesSeconds()}: " +
                        "${testResult.testDetails.testClass}.${testResult.testDetails.testName}")
    }

    private fun createAdbDeviceTestResult(testResult: InstrumentationTestResult): AdbDeviceTestResult {
        return AdbDeviceTestResult(
                adbDevice = adbDevice,
                className = testResult.testDetails.testClass,
                testName = testResult.testDetails.testName,
                status = when (testResult.status) {
                    is InstrumentationTestResult.Status.Passed -> AdbDeviceTestResult.Status.Passed
                    is InstrumentationTestResult.Status.Ignored -> AdbDeviceTestResult.Status.Ignored(
                            testResult.status.stacktrace)
                    is InstrumentationTestResult.Status.Failed -> AdbDeviceTestResult.Status.Failed(
                            testResult.status.stacktrace)
                },
                durationMillis = testResult.durationMillis,
                logcatFile = logcatFileIO.getLogcatFileForTest(testResult.testDetails))
    }

    private fun createCrashedAdbDeviceTestResults(
            testChunk: TestChunk,
            e: Exception
    ): List<AdbDeviceTestResult> {
        return testChunk.testMethods
                .map { it.toTestDetails() }
                .map { testDetails: TestDetails ->
                    AdbDeviceTestResult(
                            adbDevice = adbDevice,
                            className = testDetails.testClass,
                            testName = testDetails.testName,
                            status = AdbDeviceTestResult.Status.Failed("Crashed with exception: ${e.message}"),
                            durationMillis = 0,
                            logcatFile = logcatFileIO.getLogcatFileForTest(testDetails))
                }
    }
}
