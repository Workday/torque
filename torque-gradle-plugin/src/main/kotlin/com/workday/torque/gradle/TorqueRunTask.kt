package com.workday.torque.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.options.Option

open class TorqueRunTask: DefaultTask() {

    var annotations: List<String> = emptyList()
        @Option(option = "annotations", description = "annotations for tests to be ran") set
    var notAnnotations: List<String> = emptyList()
        @Option(option = "notAnnotations", description = "annotations for tests not to be ran") set
    var testClassRegexes: List<String> = emptyList()
        @Option(option = "testClassRegexes", description = "regex for tests to be ran") set
}
