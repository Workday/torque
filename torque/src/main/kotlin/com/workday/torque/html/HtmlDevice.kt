package com.workday.torque.html

import com.google.gson.annotations.SerializedName
import com.workday.torque.Device
import java.io.File

data class HtmlDevice(

        @SerializedName("id")
        val id: String,

        @SerializedName("model")
        val model: String,

        @SerializedName("logcat_path")
        val logcatPath: String
)

fun Device.toHtmlDevice(htmlReportDir: File) = HtmlDevice(
        id = id,
        model = model,
        logcatPath = logcatFile.relativePathTo(htmlReportDir)
)
