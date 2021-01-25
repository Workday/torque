package com.workday.torque

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter

val DEFAULT_NOT_ANNOTATIONS = listOf("Ignore")
const val DEFAULT_INSTALL_TIMEOUT_SECONDS = 30
const val DEFAULT_MAX_INSTALL_RETRIES_PER_APK = 2L
const val DEFAULT_OUTPUT_DIR_PATH = "torque-output"
const val DEFAULT_RESULT_FILE_PATH = "torque-result.txt"
const val DEFAULT_CHUNK_SIZE = 1
const val DEFAULT_PER_CHUNK_TIMEOUT_SECONDS = 120L
const val DEFAULT_MAX_RETRIES_PER_CHUNK = 1
const val DEFAULT_TORQUE_TIMEOUT_MINUTES = 60L
const val DEFAULT_FILES_PULL_DEVICE_DIR_PATH = "/sdcard/test-files"

data class Args(
        @Parameter(
                names = ["--test-apks"],
                required = true,
                variableArity = true,
                description = "Path to module test apks. Usage example: `--test-apks some-module-test1.apk some-module-test2.apk`.",
                order = 0
        )
        var testApkPaths: List<String> = emptyList(),

        @Parameter(
                names = ["--annotations"],
                variableArity = true,
                description = "Run only test methods WITH specified annotations. If multiple annotations are specified, will only run tests that are annotated with all of them. " +
                        "None by default and runs all tests. If used with other options, will run the intersection of them."
        )
        var annotations: List<String> = emptyList(),

        @Parameter(
                names = ["--not-annotations"],
                variableArity = true,
                description = "Run only test methods WITHOUT specified annotations. If multiple annotations are specified, will only run tests that are not annotated with any of them. " +
                        "Contains @Ignore by default. If used with other options, will run the intersection of them."
        )
        var notAnnotations: List<String> = DEFAULT_NOT_ANNOTATIONS,

        @Parameter(
                names = ["--class-regex"],
                variableArity = true,
                description = "Regex matching for test classes. Supports multiple regex strings. Will only run tests in classes that match ANY of the regex strings provided." +
                        "None by default and runs all tests. If used with other options, will run the intersection of them."
        )
        var testClassRegexes: List<String> = emptyList(),

        @Parameter(
                names = ["--app-apk"],
                description = "Path to application apk. Module Test Apks contains implementation code themselves and does not need this."
        )
        var appApkPath: String = "",

        @Parameter(
                names = ["--chunk-size"],
                arity = 1,
                description = "$DEFAULT_CHUNK_SIZE by default."
        )
        var chunkSize: Int = DEFAULT_CHUNK_SIZE,

        @Parameter(
                names = ["--chunk-max-retries"],
                description = "Max retries per chunk. `$DEFAULT_MAX_RETRIES_PER_CHUNK` by default."
        )
        var retriesPerChunk: Int = DEFAULT_MAX_RETRIES_PER_CHUNK,

        @Parameter(
                names = ["--chunk-timeout"],
                description = "Per chunk timeout in seconds. If omitted, defaults to $DEFAULT_PER_CHUNK_TIMEOUT_SECONDS seconds. Also used as upper ceiling timeouts for granular steps in a chunk run." +
                        "(e.g. reading the instrumentation test results, running adb commands)"
        )
        var chunkTimeoutSeconds: Long = DEFAULT_PER_CHUNK_TIMEOUT_SECONDS,

        @Parameter(
                names = ["--verbose-output"],
                arity = 1,
                description = "Either `true` or `false` to enable/disable verbose output for Torque. `false` by default."
        )
        var verboseOutput: Boolean = false,

        @Parameter(
                names = ["--install-timeout"],
                description = "APK installation timeout in seconds. If omitted, defaults to $DEFAULT_INSTALL_TIMEOUT_SECONDS seconds. Applicable to both test APK and APK under test. As installation happens within a chunk run, this should be shorter than --chunk-timeout."
        )
        var installTimeoutSeconds: Int = DEFAULT_INSTALL_TIMEOUT_SECONDS,

        @Parameter(
                names = ["--install-max-retries"],
                description = "Max install retries per apk. `$DEFAULT_MAX_INSTALL_RETRIES_PER_APK` by default."
        )
        var retriesInstallPerApk: Long = DEFAULT_MAX_INSTALL_RETRIES_PER_APK,

        @Parameter(
                names = ["--output-directory"],
                description = "Either relative or absolute path to directory for output: reports, files from devices and so on. $DEFAULT_OUTPUT_DIR_PATH by default."
        )
        var outputDirectory: String = DEFAULT_OUTPUT_DIR_PATH,

        @Parameter(
                names = ["--result-file"],
                description = "Path to result output file. `$DEFAULT_RESULT_FILE_PATH` by default."
        )
        var resultFilePath: String = DEFAULT_RESULT_FILE_PATH,

        @Parameter(
                names = ["--timeout"],
                description = "Timeout for the entire Torque run in minutes. If omitted, defaults to $DEFAULT_TORQUE_TIMEOUT_MINUTES minutes."
        )
        var timeoutMinutes: Long = DEFAULT_TORQUE_TIMEOUT_MINUTES,

        @Parameter(
                names = ["--test-files-pull-device-directory"],
                description = "Directory on device to pull test files from. Setting this directory and --file-pull-host-directory will enable recursive pulling of the folders."
        )
        var testFilesPullDeviceDirectory: String = DEFAULT_FILES_PULL_DEVICE_DIR_PATH,

        @Parameter(
                names = ["--test-files-pull-host-directory"],
                description = "Directory on the Torque run host machine to pull test files into. Setting this and --file-pull-device-directory will enable pulling of the folders." +
                        "This folder would have the structure of hostDirectory\\deviceDirectory"
        )
        var testFilesPullHostDirectory: String = "",

        @Parameter(
                names = ["--uninstall-apk-after-test"],
                arity = 1,
                description = "Always only have one module's test apk and app apk installed per device. Uninstalls the current test modules and app modules when starting a different test module." +
                        "This is required when multiple apks contain the same intent filters due to AndroidManifest.xml merging."
        )
        var uninstallApkAfterTest: Boolean = false,

        @Parameter(
                names = ["--record-failed-tests"],
                arity = 1,
                description = "Screen record failed tests on last try, file will be under"
        )
        var recordFailedTests: Boolean = false
)

fun parseArgs(rawArgs: Array<String>): Args {
    return Args().also { args ->
        JCommander.newBuilder()
                .addObject(args)
                .build()
                .parse(*rawArgs)
    }
}

fun showUsage() {
    JCommander(Args()).usage()
}
