package com.workday.torque

import java.io.File

data class Device(
        val id: String,
        val model: String,
        val logcatFile: File
)
