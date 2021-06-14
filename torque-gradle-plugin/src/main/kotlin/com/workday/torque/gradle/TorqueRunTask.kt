package com.workday.torque.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option

open class TorqueRunTask: DefaultTask() {

    @get: Input
    @set: Option(description = "annotations for tests to be ran")
    var includedAnnotations: List<String> = emptyList()

    @get: Input
    @set: Option(description = "annotations for tests not to be ran")
    var excludedAnnotations: List<String> = emptyList()

    @get: Input
    @set: Option(description = "regex for tests to be ran")
    var testClassRegexes: List<String> = emptyList()
}
