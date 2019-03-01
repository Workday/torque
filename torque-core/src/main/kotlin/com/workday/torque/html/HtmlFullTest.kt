package com.workday.torque.html

import com.google.gson.annotations.SerializedName
import com.workday.torque.AdbDeviceTestResult
import java.io.File

data class HtmlFullTest(

        @SerializedName("suite_id")
        val suiteId: String,

        @SerializedName("package_name")
        val packageName: String,

        @SerializedName("class_name")
        val className: String,

        @SerializedName("name")
        val name: String,

        @SerializedName("id")
        val id: String = "$packageName$className$name",

        @SerializedName("duration_millis")
        val durationMillis: Long,

        @SerializedName("status")
        val status: Status,

        @SerializedName("stacktrace")
        val stacktrace: String?,

        @SerializedName("logcat_path")
        val logcatPath: String,

        @SerializedName("deviceId")
        val deviceId: String,

        @SerializedName("deviceModel")
        val deviceModel: String,

        @SerializedName("screenshots")
        val screenshotDetails: List<ScreenshotDetails>
) {
    enum class Status {

        @SerializedName("passed")
        Passed,

        @SerializedName("failed")
        Failed,

        @SerializedName("ignored")
        Ignored
    }

    data class ScreenshotDetails(

            @SerializedName("path")
            val path: String,

            @SerializedName("title")
            val title: String
    )
}

fun AdbDeviceTestResult.toHtmlFullTest(suiteId: String, htmlReportDir: File) = HtmlFullTest(
        suiteId = suiteId,
        packageName = className.substringBeforeLast("."),
        className = className.substringAfterLast("."),
        name = testName,
        durationMillis = durationMillis,
        status = when (status) {
            AdbDeviceTestResult.Status.Passed -> HtmlFullTest.Status.Passed
            is AdbDeviceTestResult.Status.Ignored -> HtmlFullTest.Status.Ignored
            is AdbDeviceTestResult.Status.Failed -> HtmlFullTest.Status.Failed
        },
        stacktrace = when (status) {
            is AdbDeviceTestResult.Status.Ignored -> status.stacktrace
            is AdbDeviceTestResult.Status.Failed  -> status.stacktrace
            else -> null
        },
        logcatPath = logcatFile.relativePathTo(htmlReportDir),
        deviceId = adbDevice.id,
        deviceModel = adbDevice.model,
        screenshotDetails = emptyList()
)
