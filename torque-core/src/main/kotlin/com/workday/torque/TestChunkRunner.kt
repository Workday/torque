package com.workday.torque

import com.gojuno.commander.os.Notification
import com.workday.torque.pooling.TestChunk
import io.reactivex.Completable
import io.reactivex.Single
import kotlinx.coroutines.rx2.await
import java.util.concurrent.TimeUnit

class TestChunkRunner(
        private val args: Args,
        private val adbDevice: AdbDevice,
        private val logcatFileIO: LogcatFileIO,
        private val installer: Installer,
        private val processRunner: ProcessRunner = ProcessRunner(),
        private val instrumentationReader: InstrumentationReader = InstrumentationReader()
) {
    suspend fun run(args: Args, testChunk: TestChunk): List<AdbDeviceTestResult> {
        return try {
            adbDevice.log("Starting tests...")
            installer.ensureTestPackageInstalled(args, testChunk)
            makeTestFileDirectories(args).await()
            runAndParseTests(args.chunkTimeoutSeconds, testChunk).await()
        } catch (e: Exception) {
            adbDevice.log("TestChunk run crashed with exception: ${e.message}")
            createCrashedAdbDeviceTestResults(testChunk, e)
        }
    }

    private fun makeTestFileDirectories(args: Args): Completable {
        return processRunner.runAdb(commandAndArgs = listOf(
                        "-s", adbDevice.id,
                        "shell", "mkdir -p ${args.testFilesPullDeviceDirectory}/coverage-reports"
                ),
                        destroyOnUnsubscribe = true)
                .ofType(Notification.Exit::class.java)
                .doOnError { error -> adbDevice.log("Failed to mkdir on ${adbDevice.tag}, filepath: ${args.testFilesPullDeviceDirectory}/coverage-reports, failed: $error") }
                .ignoreElements()
    }

    private fun runAndParseTests(chunkTimeoutSeconds: Long, testChunk: TestChunk): Single<List<AdbDeviceTestResult>> {
        val testPackageName = testChunk.testModuleInfo.moduleInfo.apkPackage.value
        val testRunnerClass = testChunk.testModuleInfo.testRunner.value
        val coverageFileName = testChunk.testMethods.joinToString(",") { it.testName } + ".ec"
        val testMethodsArgs = "-e class " + testChunk.testMethods.joinToString(",") { it.testName }
        val timeout = Timeout(chunkTimeoutSeconds.toInt(), TimeUnit.SECONDS)
        val runCommand = if(args.testCoverageEnabled && shouldPullTestFiles(args)) {
            "am instrument -w -r -e coverage true -e \"coverageFile ${args.testFilesPullDeviceDirectory}/coverage-reports/$coverageFileName\" \"$testMethodsArgs $testPackageName/$testRunnerClass\""
        } else {
            "am instrument -w -r \"$testMethodsArgs $testPackageName/$testRunnerClass\""
        }
        return processRunner.runAdb(commandAndArgs = listOf("-s", adbDevice.id, "shell", runCommand), timeout = timeout)
                .ofType(Notification.Start::class.java)
                .flatMap { instrumentationReader.readTestResults(it.output, chunkTimeoutSeconds) }
                .doOnNext { instrumentationTestResult -> logTestResult(instrumentationTestResult) }
                .map { instrumentationTestResult -> createAdbDeviceTestResult(instrumentationTestResult) }
                .toList()
    }

    private fun shouldPullTestFiles(args: Args) : Boolean {
        return args.testFilesPullDeviceDirectory.isNotEmpty() && args.testFilesPullHostDirectory.isNotEmpty()
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
