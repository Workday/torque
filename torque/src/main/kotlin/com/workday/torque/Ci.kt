package com.workday.torque

import java.io.File

const val ALL_TESTS_PASSED_STRING = "All Tests Passed!"
const val FAILED_TESTS_STRING = "Failed Tests"

internal fun List<AdbDeviceTestSession>.writeCiResultToOutputFile(resultFilePath: String) {
    val resultFile = clearAndCreateNewFile(resultFilePath)
    val failedTests = fold(mutableListOf<AdbDeviceTestResult>()) { accumulatedTests, session ->
        accumulatedTests.apply { addSessionFailedTests(session) }
    }

    if (failedTests.isNotEmpty()) {
        failedTests.forEach {
            resultFile.appendText("#${it.className}##${it.testName}\n")
        }
        resultFile.appendText("${failedTests.count()} $FAILED_TESTS_STRING\n")
    } else {
        resultFile.appendText("$ALL_TESTS_PASSED_STRING\n")
    }
}

private fun clearAndCreateNewFile(resultFilePath: String): File {
    return File(resultFilePath).apply {
        delete()
        createNewFile()
    }
}

private fun MutableList<AdbDeviceTestResult>.addSessionFailedTests(session: AdbDeviceTestSession) {
    this += session.testResults.filter { testResults -> testResults.status is AdbDeviceTestResult.Status.Failed }
}
