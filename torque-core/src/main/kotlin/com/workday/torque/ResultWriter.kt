package com.workday.torque

import com.gojuno.commander.os.log
import com.google.gson.Gson
import com.workday.torque.html.writeHtmlReport
import java.io.File
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResultWriter @Inject constructor() {

    @Inject internal lateinit var args: Args

    fun clearOutputDirectory() {
        File(args.outputDirectory).deleteRecursively()
    }

    fun write(startTime: Long,  adbDeviceTestSessions: List<AdbDeviceTestSession>) {
        adbDeviceTestSessions.writeCiResultToOutputFile(args.resultFilePath)
        val suites = adbDeviceTestSessions.toSuites()
        suites.run {
            generateHtmlReport(args.outputDirectory)
            generateJunit4Report(args.outputDirectory)
            printFinalResult(startTime)
        }
    }

    private fun List<Suite>.generateHtmlReport(outputDirectoryPath: String) {
        log("Generating HTML report...")
        val htmlReportStartTime = System.currentTimeMillis()
        writeHtmlReport(Gson(), this, File(outputDirectoryPath, "html-report"), Date())
        log("HTML report generated, took ${(System.currentTimeMillis() - htmlReportStartTime).millisToHoursMinutesSeconds()}.")
    }

    private fun List<Suite>.generateJunit4Report(outputDirectoryPath: String) {
        log("Generating JUnit4 report...")
        val junit4ReportStartTime = System.currentTimeMillis()
        mapIndexed { index: Int, suite: Suite ->
            writeJunit4Report(
                    suite = suite,
                    outputFile = File(File(outputDirectoryPath, "junit4-reports"), "suite_$index.xml")
            )
        }
        log("JUnit4 report generated, took ${(System.currentTimeMillis() - junit4ReportStartTime).millisToHoursMinutesSeconds()}.")
    }

    private fun List<Suite>.printFinalResult(startTime: Long) {
        val totalPassed = sumBy { it.passedCount }
        val totalFailed = sumBy { it.failedCount }
        val totalIgnored = sumBy { it.ignoredCount }
        val duration = System.currentTimeMillis() - startTime

        log("Test run finished, total passed = $totalPassed, total failed = $totalFailed, total ignored = $totalIgnored, took ${duration.millisToHoursMinutesSeconds()}.")

        if (totalPassed == 0 && totalFailed == 0) {
            throw IllegalStateException("Error: 0 tests were run.")
        }
    }
}

internal fun List<AdbDeviceTestSession>.toSuites(): List<Suite> {
    if (isEmpty()) {
        throw IllegalStateException("Should have at least one Suite")
    }

    return listOf(Suite(
            devices = fold(emptyList()) { devices, adbDeviceTestSession ->
                devices + Device(
                        id = adbDeviceTestSession.adbDevice.id,
                        model = adbDeviceTestSession.adbDevice.model,
                        logcatFile = adbDeviceTestSession.logcatFile
                )
            },
            tests = map { it.testResults }.fold(emptyList()) { result, tests ->
                result + tests
            },
            passedCount = sumBy { it.passedCount },
            ignoredCount = sumBy { it.ignoredCount },
            failedCount = sumBy { it.failedCount },
            durationMillis = map { it.endTimestampMillis }.max()!! - map { it.startTimestampMillis }.min()!!))
}
