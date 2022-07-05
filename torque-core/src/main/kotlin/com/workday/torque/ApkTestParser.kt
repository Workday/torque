package com.workday.torque

import com.gojuno.commander.android.androidHome
import com.gojuno.commander.os.Notification
import com.gojuno.commander.os.log
import com.gojuno.commander.os.process
import com.linkedin.dex.parser.DexParser
import com.linkedin.dex.parser.TestMethod
import java.io.File
import java.util.concurrent.TimeUnit

class ApkTestParser(private val verboseOutput: Boolean = false) {
    fun getValidatedTestPackage(testApkPath: String): ApkPackage.Valid {
        return parseTestPackage(testApkPath).validateApkPackage()
    }

    private val buildTools: String? by lazy {
        File(androidHome, "build-tools")
            .listFiles()
            .sortedArray()
            .lastOrNull()
            ?.absolutePath
    }
    private val aapt: String by lazy { buildTools?.let { "$buildTools/aapt2" } ?: "" }

    private fun parseTestPackage(testApkPath: String): ApkPackage {
        return process(
            print = verboseOutput,
            commandAndArgs = listOf(
                aapt, "dump", "badging", testApkPath
            ),
            unbufferedOutput = false, // Note: flipping this flag may cause strange errors, due to OS-specific buffer flushing
        )
            .ofType(Notification.Exit::class.java)
            .map { (output) ->
                val rawOutput = output
                    .readText()
                val packageText = rawOutput
                    .split(System.lineSeparator())
                    .firstOrNull { it.contains("package") }

                if (packageText.isNullOrEmpty()) {
                    if (verboseOutput) {
                        log("parseTestPackage, raw output: $rawOutput")
                    }
                    return@map ApkPackage.ParseError("parseTestPackage, 'package' token was null")
                }
                val name = packageText
                    .split(" ")
                    .firstOrNull { it.startsWith("name=") }

                if (name.isNullOrEmpty()) {
                    if (verboseOutput) {
                        log("parseTestPackage, name token was null. package token: $packageText")
                    }
                    return@map ApkPackage.ParseError("'name' token was null")
                }

                name
                    .split("'")
                    .getOrNull(1)
                    ?.let(ApkPackage::Valid)
                    ?: ApkPackage.ParseError("Cannot parse test package from `aapt2 dump badging \$APK` output.")
            }
            .toSingle()
            .toBlocking()
            .value()
    }

    fun getValidatedTargetPackage(testApkPath: String): ApkPackage.Valid {
        return parseTargetPackage(testApkPath).validateApkPackage()
    }

    private fun parseTargetPackage(testApkPath: String): ApkPackage {
        return process(
            print = verboseOutput,
            commandAndArgs = listOf(
                aapt, "dump", "xmltree", testApkPath, "--file", "AndroidManifest.xml"
            ),
            unbufferedOutput = false, // Note: flipping this flag may cause strange errors, due to OS-specific buffer flushing
            keepOutputOnExit = false,
        )
            .ofType(Notification.Exit::class.java)
            .map { (output) ->
                val initialOutput = output
                    .readText()
                    .split(System.lineSeparator())
                    .firstOrNull { it.contains("android:targetPackage") }

                if (verboseOutput) {
                    log("parseTargetPackage, output file path: ${output.absolutePath}")
                    log("parseTargetPackage, initialOutput: $initialOutput")
                }

                val finalOutput = initialOutput
                    ?.split(" ")
                    ?.firstOrNull { it.contains("android:targetPackage") }
                    ?.substringAfter("=")
                    ?.trim('"')

                if (verboseOutput) {
                    log("parseTargetPackage finalOutput: $finalOutput")
                }
                finalOutput
                    ?.let(ApkPackage::Valid)
                    ?: ApkPackage.ParseError("Cannot parse target package from `aapt dump xmltree ${testApkPath} --file AndroidManifest.xml` output.")
            }
            .toSingle()
            .toBlocking()
            .value()
    }

    private fun ApkPackage.validateApkPackage(): ApkPackage.Valid {
        return let {
            when (it) {
                is ApkPackage.Valid -> it
                is ApkPackage.ParseError -> {
                    throw IllegalStateException(it.error)
                }
            }
        }
    }

    fun getValidatedTestRunner(testApkPath: String): TestRunner.Valid {
        return parseTestRunner(testApkPath).validateTestRunner()
    }

    private fun parseTestRunner(testApkPath: String): TestRunner =
        process(
            print = verboseOutput,
            commandAndArgs = listOf(
                aapt, "dump", "xmltree", testApkPath, "--file", "AndroidManifest.xml"
            ),
            unbufferedOutput = false, // Note: flipping this flag may cause strange errors, due to OS-specific buffer flushing
            keepOutputOnExit = false,
        )
            .ofType(Notification.Exit::class.java)
            .map {
                val expiration = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10)
                Pair(it, expiration)
            }
            .map { it.first }
            .map { (output) ->
                val rawOutput = output
                    .readText()
                val dropWhile = rawOutput
                    .split(System.lineSeparator())
                    .dropWhile { !it.contains("instrumentation") }
                val firstOrNull = dropWhile
                    .firstOrNull { it.contains("android:name") }

                if (firstOrNull.isNullOrEmpty() && verboseOutput) {
                    log("parseTestRunner, raw output: $rawOutput")
                }

                val testRunnerText = firstOrNull
                    ?.split("\"")
                    ?.getOrNull(1)

                if (verboseOutput) {
                    log("parseTestRunner, identified test runner: $testRunnerText")
                }

                testRunnerText
                    ?.let(TestRunner::Valid)
                    ?: TestRunner.ParseError("Cannot parse test runner from `aapt dump xmltree $testApkPath --file AndroidManifest.xml` output.")
            }
            .toSingle()
            .toBlocking()
            .value()


    private fun TestRunner.validateTestRunner(): TestRunner.Valid {
        return let {
            when (it) {
                is TestRunner.Valid -> it
                is TestRunner.ParseError -> {
                    throw IllegalStateException(it.error)
                }
            }
        }
    }

    fun getTests(testApkPath: String) = parseTests(testApkPath)

    private fun parseTests(testApkPath: String): List<TestMethod> =
        DexParser.findTestMethods(testApkPath).map { TestMethod(it.testName, it.annotations) }

}
