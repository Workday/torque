package com.workday.torque

import java.nio.file.Paths
import kotlin.system.exitProcess

private val PARAMETER_HELP_NAMES = setOf("--help", "-help", "help", "-h")

fun main(rawArgs: Array<String>) {
    val currentDirectoryPath = Paths.get("./").toAbsolutePath()
    val args = parseProcessArgs(rawArgs)
    Torque(args = args, workingDirectory = currentDirectoryPath.toString()).run()

    exitProcess(0)
}

private fun parseProcessArgs(rawArgs: Array<String>): Args {
    if (PARAMETER_HELP_NAMES.any { rawArgs.contains(it) }) {
        showUsage()
        exitProcess(0)
    } else {
        return parseArgs(rawArgs)
    }
}
