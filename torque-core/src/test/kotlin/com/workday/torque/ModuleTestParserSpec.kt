package com.workday.torque

import com.linkedin.dex.parser.TestMethod
import com.workday.torque.pooling.ModuleInfo
import com.workday.torque.pooling.TestModule
import com.workday.torque.pooling.TestModuleInfo
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it

class ModuleTestParserSpec : Spek(
{
    context("parse multiple tests modules from apks") {
        val moduleCount = 10
        val testApkPath = fileFromJarResources<InstrumentationSpec>("instrumentation-test.apk").absolutePath
        val args = Args().apply {
            testApkPaths = MutableList(moduleCount) { testApkPath }
        }

        it("parses TestModules correctly") {
            val expectedTestModules = createExpectedTestModules(testApkPath, moduleCount)

            assertThat(ModuleTestParser(args).parseTestsFromModuleApks()).isEqualTo(expectedTestModules)
        }
    }

    context("parse tests with different test annotations") {
        val noAnnotationsTest = TestMethod("com.company.mymodule.test#testNoAnnotations",
                                           listOf("org.junit.Test", "kotlin.Metadata"))
        val ignoredTest = TestMethod("com.company.mymodule.test#testIgnored",
                                     listOf("org.junit.Ignore", "org.junit.Test", "kotlin.Metadata"))
        val mediumTest = TestMethod("com.company.mymodule.test#testMediumTest",
                                    listOf("android.support.test.filters.MediumTest", "org.junit.Test",
                                           "kotlin.Metadata"))
        val flakyMediumTest = TestMethod("com.company.mymodule.test#testMultiAnnotations",
                                              listOf("android.support.test.filters.MediumTest",
                                                     "android.support.test.filters.FlakyTest", "org.junit.Test",
                                                     "kotlin.Metadata"))
        val ignoredFlakyMediumTest = TestMethod("com.company.mymodule.test#testPositiveNegativeAnnotations",
                                                         listOf("org.junit.Ignore",
                                                            "android.support.test.filters.MediumTest",
                                                            "android.support.test.filters.FlakyTest", "org.junit.Test",
                                                            "kotlin.Metadata"))
        val testMethods = listOf(
                noAnnotationsTest,
                ignoredTest,
                mediumTest,
                flakyMediumTest,
                ignoredFlakyMediumTest)
        val testPackage = ApkPackage.Valid("com.company.mymodule.test")
        val targetPackage = ApkPackage.Valid("test.test.myapplication")
        val testRunner = TestRunner.Valid("android.support.test.runner.AndroidJUnitRunner")
        val apkTestParser = mockk<ApkTestParser> {
            every { getValidatedTestPackage(any()) } returns testPackage
            every { getValidatedTargetPackage(any()) } returns targetPackage
            every { getValidatedTestRunner(any()) } returns testRunner
            every { getTests(any()) } returns testMethods
        }
        val targetApkPath = "some_app_path"
        val testApkPath = "some_path"
        val testModuleInfo = TestModuleInfo(ModuleInfo(testPackage, testApkPath), testRunner, ModuleInfo(targetPackage, targetApkPath))

        given("no Annotations and with default Ignore NotAnnotations") {
            val args = Args().apply {
                testApkPaths = listOf(testApkPath)
                appApkPath = targetApkPath
            }

            it("filters out Ignored tests only") {
                val expectedTestMethods = listOf(
                        noAnnotationsTest,
                        mediumTest,
                        flakyMediumTest)
                val expectedTestModules = listOf(TestModule(testModuleInfo, expectedTestMethods))

                assertThat(ModuleTestParser(args, apkTestParser).parseTestsFromModuleApks()).isEqualTo(expectedTestModules)
            }
        }

        given("MediumTest Annotations and with default Ignore NotAnnotations") {
            val args = Args().apply {
                testApkPaths = listOf(testApkPath)
                appApkPath = targetApkPath
                annotations = listOf("MediumTest")
            }

            it("filters to MediumTests only") {
                val expectedTestMethods = listOf(mediumTest, flakyMediumTest)
                val expectedTestModules = listOf(TestModule(testModuleInfo, expectedTestMethods))

                assertThat(ModuleTestParser(args, apkTestParser).parseTestsFromModuleApks()).isEqualTo(expectedTestModules)
            }
        }

        given("MediumTest FlakyTest Annotations and with default Ignore NotAnnotations") {
            val args = Args().apply {
                testApkPaths = listOf(testApkPath)
                appApkPath = targetApkPath
                annotations = listOf("MediumTest", "FlakyTest")
            }

            it("filters to MediumTest FlakyTest tests only") {
                val expectedTestMethods = listOf(flakyMediumTest)
                val expectedTestModules = listOf(TestModule(testModuleInfo, expectedTestMethods))

                assertThat(ModuleTestParser(args, apkTestParser).parseTestsFromModuleApks()).isEqualTo(expectedTestModules)
            }
        }

        given("FlakyTest NotAnnotations") {
            val args = Args().apply {
                testApkPaths = listOf(testApkPath)
                appApkPath = targetApkPath
                notAnnotations = listOf("FlakyTest")
            }

            it("filters out FlakyTest tests only") {
                val expectedTestMethods = listOf(noAnnotationsTest, ignoredTest, mediumTest)
                val expectedTestModules = listOf(TestModule(testModuleInfo, expectedTestMethods))

                assertThat(ModuleTestParser(args, apkTestParser).parseTestsFromModuleApks()).isEqualTo(expectedTestModules)
            }
        }
    }

    context("parse tests with test class regex") {
        val prefixSomeSpecificTest = TestMethod("com.company.mymodule1.PrefixSomeSpecificTest#someTestMethod", emptyList())
        val prefixSpecificTest = TestMethod("com.company.mymodule2.PrefixSpecificTest#someTestMethod", emptyList())
        val someOtherTest = TestMethod("com.company.myapp.SomeOtherTest#someTestMethod", emptyList())
        val testMethods = listOf(prefixSomeSpecificTest, prefixSpecificTest, someOtherTest)
        val testPackage = ApkPackage.Valid("com.company.myapp.test")
        val targetPackage = ApkPackage.Valid("test.test.myapplication")
        val testRunner = TestRunner.Valid("android.support.test.runner.AndroidJUnitRunner")
        val apkTestParser = mockk<ApkTestParser> {
            every { getValidatedTestPackage(any()) } returns testPackage
            every { getValidatedTargetPackage(any()) } returns targetPackage
            every { getValidatedTestRunner(any()) } returns testRunner
            every { getTests(any()) } returns testMethods
        }
        val targetApkPath = "some_app_path"
        val testApkPath = "some_test_path"
        val testModuleInfo = TestModuleInfo(ModuleInfo(testPackage, testApkPath), testRunner, ModuleInfo(targetPackage, targetApkPath))

        given("no regex provided ") {
            val args = Args().apply {
                testApkPaths = listOf(testApkPath)
                appApkPath = targetApkPath
            }

            it("filters to all tests") {
                val expectedTestMethods = listOf(prefixSomeSpecificTest, prefixSpecificTest, someOtherTest)
                val expectedTestModules = listOf(TestModule(testModuleInfo, expectedTestMethods))

                assertThat(ModuleTestParser(args, apkTestParser).parseTestsFromModuleApks()).isEqualTo(expectedTestModules)
            }
        }

        given("single regex string") {
            val args = Args().apply {
                testApkPaths = listOf(testApkPath)
                appApkPath = targetApkPath
                testClassRegexes = listOf("(Prefix[a-zA-Z]*SpecificTest)+")
            }

            it("filters to regex matching tests only") {
                val expectedTestMethods = listOf(prefixSomeSpecificTest, prefixSpecificTest)
                val expectedTestModules = listOf(TestModule(testModuleInfo, expectedTestMethods))

                assertThat(ModuleTestParser(args, apkTestParser).parseTestsFromModuleApks()).isEqualTo(expectedTestModules)
            }
        }

        given("multiple regex strings") {
            val args = Args().apply {
                testApkPaths = listOf(testApkPath)
                appApkPath = targetApkPath
                testClassRegexes = listOf("(PrefixSpecificTest)+", "(SomeOtherTest)+")
            }

            it("filters to regex matching tests only") {
                val expectedTestMethods = listOf(prefixSpecificTest, someOtherTest)
                val expectedTestModules = listOf(TestModule(testModuleInfo, expectedTestMethods))

                assertThat(ModuleTestParser(args, apkTestParser).parseTestsFromModuleApks()).isEqualTo(expectedTestModules)
            }
        }
    }

    context("parse tests with annotations and regex") {
        val noAnnotationsSpecificTest = TestMethod("com.company.mymodule.SomeSpecificTest#someTest",
                listOf("org.junit.Test", "kotlin.Metadata"))
        val mediumSpecificTest = TestMethod("com.company.mymodule.SomeSpecificTest#someTest",
                listOf("android.support.test.filters.MediumTest", "org.junit.Test",
                        "kotlin.Metadata"))
        val mediumOtherTest = TestMethod("com.company.mymodule.SomeOtherTest#someTest",
                listOf("android.support.test.filters.MediumTest", "org.junit.Test",
                        "kotlin.Metadata"))
        val flakyMediumTest = TestMethod("com.company.mymodule.SomeSpecificTest#someTest",
                listOf("android.support.test.filters.MediumTest",
                        "android.support.test.filters.FlakyTest", "org.junit.Test",
                        "kotlin.Metadata"))
        val testMethods = listOf(
                noAnnotationsSpecificTest,
                mediumSpecificTest,
                mediumOtherTest,
                flakyMediumTest)
        val testPackage = ApkPackage.Valid("com.company.mymodule.test")
        val targetPackage = ApkPackage.Valid("test.test.myapplication")
        val testRunner = TestRunner.Valid("android.support.test.runner.AndroidJUnitRunner")
        val apkTestParser = mockk<ApkTestParser> {
            every { getValidatedTestPackage(any()) } returns testPackage
            every { getValidatedTargetPackage(any()) } returns targetPackage
            every { getValidatedTestRunner(any()) } returns testRunner
            every { getTests(any()) } returns testMethods
        }
        val targetApkPath = "some_app_path"
        val testApkPath = "some_test_path"
        val testModuleInfo = TestModuleInfo(ModuleInfo(testPackage, testApkPath), testRunner, ModuleInfo(targetPackage, targetApkPath))

        given("MediumTest Annotations, FlakyTest NotAnnotations and regex") {
            val args = Args().apply {
                testApkPaths = listOf(testApkPath)
                appApkPath = targetApkPath
                annotations = listOf("MediumTest")
                notAnnotations = listOf("FlakyTest")
                testClassRegexes = listOf("[a-zA-Z]+SpecificTest")
            }

            it("filters to MediumTests Non-FlakyTest SpecificTest only") {
                val expectedTestMethods = listOf(mediumSpecificTest)
                val expectedTestModules = listOf(TestModule(testModuleInfo, expectedTestMethods))

                assertThat(ModuleTestParser(args, apkTestParser).parseTestsFromModuleApks()).isEqualTo(expectedTestModules)
            }
        }
    }
})

private fun createExpectedTestModules(testApkPath: String, moduleCount: Int): List<TestModule> {
    val testPackage = ApkPackage.Valid("test.test.myapplication.test")
    val targetPackage = ApkPackage.Valid("test.test.myapplication")
    val testRunner = TestRunner.Valid("android.support.test.runner.AndroidJUnitRunner")
    val testMethods = listOf(
            TestMethod("test.test.myapplication.ExampleInstrumentedTest#useAppContext",
                       listOf("dalvik.annotation.Throws", "org.junit.Test", "org.junit.runner.RunWith")
                      ))
    val moduleInfo = TestModuleInfo(ModuleInfo(testPackage, testApkPath), testRunner, ModuleInfo(targetPackage, ""))
    return MutableList(moduleCount) { TestModule(moduleInfo, testMethods) }
}
