package com.workday.torque

import com.linkedin.dex.parser.TestMethod

fun TestMethod.getTestClass(): String {
    return testName.substringAfterLast('.').substringBeforeLast('#')
}

fun TestMethod.getTestMethod(): String {
    return testName.substringAfterLast('#')
}

fun TestMethod.toTestDetails(): TestDetails {
    return TestDetails(getTestClass(), getTestMethod())
}
