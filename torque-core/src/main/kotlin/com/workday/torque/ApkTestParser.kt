package com.workday.torque

import com.gojuno.commander.android.androidHome
import com.gojuno.commander.os.Notification
import com.gojuno.commander.os.process
import com.linkedin.dex.parser.DexParser
import com.linkedin.dex.parser.TestMethod
import java.io.File
import java.util.*

class ApkTestParser {
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
    val aapt: String by lazy { buildTools?.let { "$buildTools/aapt2" } ?: "" }

    private fun parseTestPackage(testApkPath: String): ApkPackage {
        val commandAndArgs = listOf(
            aapt, "dump", "badging", testApkPath
        )
        return process(
                commandAndArgs = commandAndArgs,
                unbufferedOutput = true,
        )
                .ofType(Notification.Exit::class.java)
                .map { (output) ->
                    val unTouched = output
                        .readText()
                    val packageText = unTouched
                        .split(System.lineSeparator())
                        .firstOrNull { it.contains("package") }

                    if (packageText.isNullOrEmpty()) {
                        return@map ApkPackage.ParseError("'package' token was null")
                    }
                    val splitPackageText = packageText
                        ?.split(" ")
                    val name = splitPackageText
                        ?.firstOrNull { it.startsWith("name=") }

                    if (name.isNullOrEmpty()) {
                        return@map ApkPackage.ParseError("'name' token was null")
                    }

                    name
                            ?.split("'")
                            ?.getOrNull(1)
                            ?.let(ApkPackage::Valid)
                            ?: ApkPackage.ParseError("Cannot parse test package from `aapt dump badging \$APK` output.")
                }
                .toSingle()
                .toBlocking()
                .value()
    }

    private fun makeOutputFile(): File {
        return Random()
            .nextInt()
            .let { System.nanoTime() + it }
            .let { name ->
                File("$name.output").apply {
                    createNewFile()
                    deleteOnExit()
                }
            }
    }

    fun getValidatedTargetPackage(testApkPath: String): ApkPackage.Valid {
        return parseTargetPackage(testApkPath).validateApkPackage()
    }

    private fun parseTargetPackage(testApkPath: String): ApkPackage {
        return process(
                commandAndArgs = listOf(
                        aapt, "dump", "xmltree", testApkPath, "--file", "AndroidManifest.xml"
                ),
                unbufferedOutput = true,
                keepOutputOnExit = true,
        )
                .ofType(Notification.Exit::class.java)
                .map { (output) ->
                    val initialOutput = output
                        .readText()
                        .split(System.lineSeparator())
                        .firstOrNull { it.contains("android:targetPackage") }

                    val secondaryOutput = initialOutput
                        ?.split(" ")
                    val finalOutput = secondaryOutput
                        ?.firstOrNull {
                            it.contains("android:targetPackage")
                        }
                        ?.substringAfter("=")
                        ?.trim('"')
                    finalOutput
                            ?.let(ApkPackage::Valid)
                            ?: ApkPackage.ParseError("Cannot parse target package from `aapt dump xmltree ${testApkPath} AndroidManifest.xml` output.")
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
                    commandAndArgs = listOf(
                            aapt, "dump", "xmltree", testApkPath, "--file", "AndroidManifest.xml"
                    ),
                    unbufferedOutput = true
            )
                    .ofType(Notification.Exit::class.java)
                    .map { (output) ->
                        output
                                .readText()
                                .split(System.lineSeparator())
                                .dropWhile { !it.contains("instrumentation") }
                                .firstOrNull { it.contains("android:name") }
                                ?.split("\"")
                                ?.getOrNull(1)
                                ?.let(TestRunner::Valid)
                                ?: TestRunner.ParseError("Cannot parse test runner from `aapt dump xmltree ${testApkPath} AndroidManifest.xml` output.")
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
