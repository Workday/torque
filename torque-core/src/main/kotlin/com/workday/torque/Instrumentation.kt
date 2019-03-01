package com.workday.torque

import com.workday.torque.InstrumentationTestResult.Status.Failed
import com.workday.torque.InstrumentationTestResult.Status.Ignored
import com.workday.torque.InstrumentationTestResult.Status.Passed
import rx.Observable
import java.io.File

data class InstrumentationTestResult(
        val index: Int,
        val total: Int,
        val testDetails: TestDetails,
        val status: Status,
        val durationMillis: Long
) {

    sealed class Status {
        object Passed : Status()
        data class Ignored(val stacktrace: String = "") : Status()
        data class Failed(val stacktrace: String) : Status()
    }
}

/**
 * @see android.support.test.internal.runner.listener.InstrumentationResultPrinter
 */
enum class StatusCode(val code: Int) {
    Start(1),
    Ok(0),
    Failure(-2),
    Ignored(-3),
    AssumptionFailure(-4)
}

data class InstrumentationEntry(
        val numTests: Int,
        val stream: String,
        val id: String,
        val test: String,
        val clazz: String,
        val current: Int,
        val stack: String,
        val statusCode: StatusCode,
        val timestampMillis: Long
)

private fun String.substringBetween(first: String, second: String): String {
    val indexOfFirst = indexOf(first)

    if (indexOfFirst < 0) {
        return ""
    }

    val startIndex = indexOfFirst + first.length
    val endIndex = indexOf(second, startIndex).let { if (it <= 0) length else it }

    return substring(startIndex, endIndex)
}

private fun String.parseInstrumentationStatusValue(key: String): String = this
        .substringBetween("INSTRUMENTATION_STATUS: $key=", "INSTRUMENTATION_STATUS")
        .trim()

private fun String.throwIfError(output: File) = when {
    contains("INSTRUMENTATION_RESULT: shortMsg=") -> {
        throw Exception("Application process crashed. Check Logcat output for more details.")
    }

    contains("INSTRUMENTATION_STATUS: Error=Unable to find instrumentation info for") -> {
        val runner = substringBetween("ComponentInfo{", "}").substringAfter("/")
        throw Exception(
                "Instrumentation was unable to run tests using runner $runner.\n" +
                "Most likely you forgot to declare test runner in AndroidManifest.xml or build.gradle.\n" +
                "Detailed log can be found in ${output.path} or Logcat output."
        )
    }

    else -> this
}

private fun parseInstrumentationEntry(str: String): InstrumentationEntry =
        InstrumentationEntry(
                numTests = str.parseInstrumentationStatusValue("numtests").toInt(),
                stream = str.parseInstrumentationStatusValue("stream"),
                stack = str.parseInstrumentationStatusValue("stack"),
                id = str.parseInstrumentationStatusValue("id"),
                test = str.parseInstrumentationStatusValue("test"),
                clazz = str.parseInstrumentationStatusValue("class"),
                current = str.parseInstrumentationStatusValue("current").toInt(),
                statusCode = str.substringBetween("INSTRUMENTATION_STATUS_CODE: ", "INSTRUMENTATION_STATUS")
                        .trim()
                        .toInt()
                        .let { code ->
                            StatusCode.values().firstOrNull { it.code == code }
                        }
                        .let { statusCode ->
                            when (statusCode) {
                                null -> throw IllegalStateException("Unknown test status code [$statusCode], please report that to Torque maintainers $str")
                                else -> statusCode
                            }
                        },
                timestampMillis = System.currentTimeMillis()
        )

// Reads stream in "tail -f" mode.
fun readInstrumentationOutput(output: File): Observable<InstrumentationEntry> {
    data class Result(val buffer: String = "", val readyForProcessing: Boolean = false)

    return tail(output)
            .map(String::trim)
            .map { it.throwIfError(output) }
            .takeWhile {
                // `INSTRUMENTATION_CODE: <code>` is the last line printed by instrumentation, even if 0 tests were run.
                !it.startsWith("INSTRUMENTATION_CODE")
            }
            .scan(Result()) { previousResult, newLine ->
                val buffer = when (previousResult.readyForProcessing) {
                    true -> newLine
                    false -> "${previousResult.buffer}${System.lineSeparator()}$newLine"
                }

                Result(buffer = buffer, readyForProcessing = newLine.startsWith("INSTRUMENTATION_STATUS_CODE"))
            }
            .filter { it.readyForProcessing }
            .map { it.buffer }
            .map(::parseInstrumentationEntry)
}

fun Observable<InstrumentationEntry>.asTests(): Observable<InstrumentationTestResult> {
    data class Result(val entries: List<InstrumentationEntry> = emptyList(), val testResults: List<InstrumentationTestResult> = emptyList(), val totalTestsCount: Int = 0)

    return this
            .scan(Result()) { previousResult, newEntry ->
                val entries = previousResult.entries + newEntry
                val testResults: List<InstrumentationTestResult> = entries
                        .mapIndexed { index, first ->
                            val second = entries
                                    .subList(index + 1, entries.size)
                                    .firstOrNull {
                                        first.clazz == it.clazz &&
                                        first.test == it.test &&
                                        first.current == it.current &&
                                        first.statusCode != it.statusCode
                                    }

                            if (second == null) null else first to second
                        }
                        .filterNotNull()
                        .map { (first, second) ->
                            InstrumentationTestResult(
                                    index = first.current,
                                    total = first.numTests,
                                    testDetails = TestDetails(first.clazz, first.test),
                                    status = when (second.statusCode) {
                                        StatusCode.Ok -> Passed
                                        StatusCode.Ignored  -> Ignored()
                                        StatusCode.AssumptionFailure -> Ignored(stacktrace = second.stack)
                                        StatusCode.Failure -> Failed(stacktrace = second.stack)
                                        StatusCode.Start -> throw IllegalStateException(
                                                "Unexpected status code [Start] in second entry, " +
                                                "please report that to Torque maintainers ($first, $second)"
                                        )
                                    },
                                    durationMillis = second.timestampMillis - first.timestampMillis
                            )
                        }

                Result(
                        entries = entries.filter { entry ->
                            testResults.firstOrNull { it.testDetails.testClass == entry.clazz && it.testDetails.testName == entry.test } == null },
                        testResults = testResults,
                        totalTestsCount = previousResult.totalTestsCount + testResults.size
                )
            }
            .takeUntil {
                if (it.entries.count { it.current == it.numTests } == 2) {
                    if (it.totalTestsCount < it.entries.first().numTests) {
                        throw IllegalStateException("Less tests were emitted than Instrumentation reported: $it")
                    }

                    true
                } else {
                    false
                }
            }
            .filter { it.testResults.isNotEmpty() }
            .flatMap { Observable.from(it.testResults) }
}
