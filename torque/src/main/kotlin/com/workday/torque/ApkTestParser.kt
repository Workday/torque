package com.workday.torque

import com.gojuno.commander.android.aapt
import com.gojuno.commander.os.Notification
import com.gojuno.commander.os.process
import com.linkedin.dex.parser.DexParser
import com.linkedin.dex.parser.TestMethod

class ApkTestParser {
    fun getValidatedTestPackage(testApkPath: String): ApkPackage.Valid {
        return parseTestPackage(testApkPath).validateApkPackage()
    }

    private fun parseTestPackage(testApkPath: String): ApkPackage {
        return process(
                commandAndArgs = listOf(
                        aapt, "dump", "badging", testApkPath
                ),
                unbufferedOutput = true
        )
                .ofType(Notification.Exit::class.java)
                .map { (output) ->
                    output
                            .readText()
                            .split(System.lineSeparator())
                            // output format `package: name='$testPackage' versionCode='' versionName='' platformBuildVersionName='xxx'`
                            .firstOrNull { it.contains("package") }
                            ?.split(" ")
                            ?.firstOrNull { it.startsWith("name=") }
                            ?.split("'")
                            ?.getOrNull(1)
                            ?.let(ApkPackage::Valid)
                            ?: ApkPackage.ParseError("Cannot parse test package from `aapt dump badging \$APK` output.")
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
                commandAndArgs = listOf(
                        aapt, "dump", "xmltree", testApkPath, "AndroidManifest.xml"
                ),
                unbufferedOutput = true
        )
                .ofType(Notification.Exit::class.java)
                .map { (output) ->
                    output
                            .readText()
                            .split(System.lineSeparator())
                            // output format `A: android:targetPackage(0x01010021)="$targetPackage" (Raw: "$targetPackag")`
                            .firstOrNull { it.contains("android:targetPackage") }
                            ?.split(" ")
                            ?.firstOrNull { it.startsWith("android:targetPackage") }
                            ?.substringAfter("=")
                            ?.trim('"')
                            ?.let(ApkPackage::Valid)
                            ?: ApkPackage.ParseError("Cannot parse target package from `aapt dump xmltree \$TEST_APK AndroidManifest.xml` output.")
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
                            aapt, "dump", "xmltree", testApkPath, "AndroidManifest.xml"
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
                                // output format : `A: android:name(0x01010003)="$testRunner" (Raw: "$testRunner")`
                                ?.split("\"")
                                ?.getOrNull(1)
                                ?.let(TestRunner::Valid)
                                ?: TestRunner.ParseError("Cannot parse test runner from `aapt dump xmltree \$TEST_APK AndroidManifest.xml` output.")
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
            DexParser.findTestMethods(testApkPath).map { TestMethod(it.testName, it.annotationNames) }

}
