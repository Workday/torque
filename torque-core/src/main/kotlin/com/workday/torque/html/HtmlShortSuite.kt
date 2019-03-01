package com.workday.torque.html

import com.google.gson.annotations.SerializedName
import com.workday.torque.Suite
import java.io.File

data class HtmlShortSuite(

        @SerializedName("id")
        val id: String,

        @SerializedName("passed_count")
        val passedCount: Int,

        @SerializedName("ignored_count")
        val ignoredCount: Int,

        @SerializedName("failed_count")
        val failedCount: Int,

        @SerializedName("duration_millis")
        val durationMillis: Long,

        @SerializedName("devices")
        val devices: List<HtmlDevice>
)

fun Suite.toHtmlShortSuite(id: String, htmlReportDir: File) = HtmlShortSuite(
        id = id,
        passedCount = passedCount,
        ignoredCount = ignoredCount,
        failedCount = failedCount,
        durationMillis = durationMillis,
        devices = devices.map { it.toHtmlDevice(htmlReportDir) }
)
