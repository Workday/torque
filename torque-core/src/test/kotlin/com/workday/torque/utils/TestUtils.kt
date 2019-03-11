package com.workday.torque.utils

import com.linkedin.dex.parser.TestMethod

fun createTestMethodsList(size: Int): MutableList<TestMethod> {
    return MutableList(size) { TestMethod("test#somemethod", listOf("org.junit.Test")) }
}
