package com.workday.torque.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option

open class TorqueRunTask: DefaultTask() {

    @get: Input
    @set: Option(option = "annotations", description = "annotations for tests to be ran")
    var includedAnnotations: List<String> = emptyList()

    @get: Input
    @set: Option(option = "notAnnotations", description = "annotations for tests not to be ran")
    var excludedAnnotations: List<String> = emptyList()

    @get: Input
    @set: Option(option = "testClassRegexes", description = "regex for tests to be ran")
    var testClassRegexes: List<String> = emptyList()
}
