package com.workday.torque

sealed class ApkPackage {
    data class Valid(val value: String) : ApkPackage()
    data class ParseError(val error: String) : ApkPackage()
}

sealed class TestRunner {
    data class Valid(val value: String) : TestRunner()
    data class ParseError(val error: String) : TestRunner()
}
