package com.workday.torque

import com.linkedin.dex.parser.TestMethod
import com.workday.torque.utils.RUNS_WITH_ANDROID_ANNOTATION
import com.workday.torque.utils.TEST_ANNOTATION
import com.workday.torque.utils.THROWS_EXCEPTION_ANNOTATION
import com.workday.torque.utils.assertAnnotationArrays
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.it

class ApkTestParserSpec : Spek({
    context("parse package from apk") {
        val apkTestParser = ApkTestParser()
        val testApkPath = fileFromJarResources<InstrumentationSpec>("instrumentation-test.apk").absolutePath

        it("parses test runner correctly") {

            assertThat(apkTestParser.getValidatedTestRunner(testApkPath)).isEqualTo(TestRunner.Valid("android.support.test.runner.AndroidJUnitRunner"))
        }

        it("parses test package correctly") {
            assertThat(apkTestParser.getValidatedTestPackage(testApkPath)).isEqualTo(ApkPackage.Valid("test.test.myapplication.test"))
        }

        it("parses target package correctly") {
            assertThat(apkTestParser.getValidatedTargetPackage(testApkPath)).isEqualTo(ApkPackage.Valid("test.test.myapplication"))
        }

        it("parses tests list correctly") {
            val expectedTestMethods = listOf(
                    TestMethod("test.test.myapplication.ExampleInstrumentedTest#useAppContext",
                            listOf(RUNS_WITH_ANDROID_ANNOTATION,
                                    THROWS_EXCEPTION_ANNOTATION,
                                    TEST_ANNOTATION)
                    ))
            val actualTestMethods = apkTestParser.getTests(testApkPath)
            expectedTestMethods.zip(actualTestMethods).forEach { (expected, actual) ->
                assertThat(expected.testName).isEqualTo(actual.testName)
                assertAnnotationArrays(expected.annotations, actual.annotations)
            }
        }
    }
})
