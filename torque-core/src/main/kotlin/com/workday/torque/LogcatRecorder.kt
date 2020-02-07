package com.workday.torque

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val TEST_RUNNER_STRING = "TestRunner"
private const val STARTED_STRING = "started"
private const val FINISHED_STRING = "finished"

private const val TEST_RUNNER_STARTED_STRING = "$TEST_RUNNER_STRING: $STARTED_STRING:"
private const val TEST_RUNNER_FINISHED_STRING = "$TEST_RUNNER_STRING: $FINISHED_STRING:"

data class TestDetails(val testClass: String, val testName: String)
data class TestLogcat(val testDetails: TestDetails, var logcat: String = "")

class LogcatRecorder(
        private val adbDevice: AdbDevice,
        private val logcatFileIO: LogcatFileIO) {
    private var tailAndSaveLogcatJob: Job? = null

    suspend fun start(coroutineScope: CoroutineScope) {
        logcatFileIO.redirectLogcatToFile()
        tailAndSaveLogcatJob = coroutineScope.launch { tailAndSaveLogcatFiles() }
    }

    private suspend fun tailAndSaveLogcatFiles() {
        val testLogcatChannel = logcatFileIO.tailFile()
        var currentTestLogcat: TestLogcat? = null
        for (newLine in testLogcatChannel) {
            when {
                newLine.contains(TEST_RUNNER_STARTED_STRING) -> {
                    if (currentTestLogcat != null) {
                        val artificialLog = "[Torque]: Started next test without finishing the last one." +
                                " Unfinished test: ${currentTestLogcat.testDetails.testClass}.${currentTestLogcat.testDetails.testName}"
                        adbDevice.log(artificialLog)
                        currentTestLogcat.finishAndWriteLogcatFile(artificialLog)
                    }
                    currentTestLogcat = createAndInitTestLogcat(newLine)
                }
                newLine.contains(TEST_RUNNER_FINISHED_STRING) -> {
                    when {
                        currentTestLogcat == null -> {
                            adbDevice.log("Finished a test before starting one, logcat cannot be recorded")
                        }
                        !currentTestLogcat.isMatchingFinishTestLogcat(newLine) -> {
                            adbDevice.log("Finished a different test from the started one, logcat cannot be recorded")
                        }
                        else -> {
                            currentTestLogcat.finishAndWriteLogcatFile(newLine)
                        }
                    }
                    currentTestLogcat = null
                }
                else -> currentTestLogcat?.appendNewLogLine(newLine)
            }
        }
    }

    private fun createAndInitTestLogcat(newLine: String): TestLogcat {
        val testDetails = newLine.parseTestDetails()
                ?: TestDetails("CorruptFormatClass", "CorruptFormatMethod")
        return TestLogcat(testDetails = testDetails)
                .apply { appendNewLogLine(newLine) }
    }

    private fun TestLogcat.isMatchingFinishTestLogcat(newLine: String): Boolean {
        return testDetails == newLine.parseTestDetails()
    }

    private fun TestLogcat.finishAndWriteLogcatFile(newLine: String) {
        appendNewLogLine(newLine)
        logcatFileIO.writeLogcatFileForTest(this)
    }

    /**
     * Log examples:
     *  "11-12 14:37:16.234 24855 24870 I TestRunner: started: some_test_method(com.mycompany.mymodule.tests.SomeTestClass)"
     *  "11-12 14:37:17.161 24855 24870 I TestRunner: finished: some_test_method(com.mycompany.mymodule.tests.SomeTestClass)"
     */
    private fun String.parseTestDetails(): TestDetails? {
        val index = indexOf(TEST_RUNNER_STRING)
        if (index < 0) return null

        val tokens = substring(index, length).split(':')
        if (tokens.size != 3) return null

        val startedOrFinished = tokens[1].trimStart()
        if (startedOrFinished == STARTED_STRING || startedOrFinished == FINISHED_STRING) {
            val testClass = tokens[2].substringAfter("(").removeSuffix(")")
            val testName = tokens[2].substringBefore("(").trim()
            return TestDetails(testClass, testName)
        }
        return null
    }

    private fun TestLogcat.appendNewLogLine(newLine: String) {
        logcat += "$newLine\n"
    }

    fun stop() = tailAndSaveLogcatJob?.cancel()
}

