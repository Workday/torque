package com.workday.torque

import com.workday.torque.AdbDeviceTestResult.Status.Failed
import com.workday.torque.AdbDeviceTestResult.Status.Ignored
import com.workday.torque.AdbDeviceTestResult.Status.Passed
import org.apache.commons.lang3.StringEscapeUtils
import java.io.File

fun writeJunit4Report(suite: Suite, outputFile: File) {
    outputFile.parentFile.mkdirs()
    fun Long.toJunitSeconds(): String = (this / 1000.0).toString()

    val xml = buildString(capacity = suite.tests.size * 150) {
        appendln("""<?xml version="1.0" encoding="UTF-8"?>""")

        append("<testsuite ")
        apply {
            append("""tests="${suite.tests.size}" """)
            append("""failures="${suite.failedCount}" """)

            // We can try to parse logcat output to get this info. See `android.support.test.internal.runner.listener.LogRunListener`.
            append("""errors="0" """)
            append("""skipped="${suite.ignoredCount}" """)

            append("""time="${suite.durationMillis.toJunitSeconds()}" """)
            append("""hostname="localhost"""")
        }
        appendln(">")

        apply {
            appendln("<properties/>")
            suite.tests.forEach { test ->
                append("<testcase ")
                append("""classname="${test.className}" """)
                append("""name="${test.testName}" """)
                append("""time="${test.durationMillis.toJunitSeconds()}"""")

                when (test.status) {
                    Passed -> {
                        appendln("/>")
                    }
                    is Ignored -> {
                        appendln(">")
                        if (test.status.stacktrace.isEmpty()) {
                            appendln("<skipped/>")
                        } else {
                            appendln("<skipped>")
                            appendln(StringEscapeUtils.escapeXml10(test.status.stacktrace))
                            appendln("</skipped>")
                        }
                        appendln("</testcase>")
                    }
                    is Failed -> {
                        appendln(">")

                        appendln("<failure>")
                        appendln(StringEscapeUtils.escapeXml10(test.status.stacktrace))
                        appendln("</failure>")

                        appendln("</testcase>")
                    }
                }
            }
        }

        appendln("</testsuite>")
    }
    outputFile.writeText(xml)
}
