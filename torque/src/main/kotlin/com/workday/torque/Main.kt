package com.workday.torque

import kotlin.system.exitProcess

private val PARAMETER_HELP_NAMES = setOf("--help", "-help", "help", "-h")

fun main(rawArgs: Array<String>) {
    val args = parseProcessArgs(rawArgs)
    Torque(args).run()

    System.exit(0)
}

private fun parseProcessArgs(rawArgs: Array<String>): Args {
    if (PARAMETER_HELP_NAMES.any { rawArgs.contains(it) }) {
        showUsage()
        exitProcess(0)
    } else {
        return parseArgs(rawArgs)
    }
}
