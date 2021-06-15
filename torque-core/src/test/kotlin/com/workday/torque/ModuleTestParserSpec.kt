package com.workday.torque

import com.linkedin.dex.parser.TestMethod
import com.workday.torque.pooling.ModuleInfo
import com.workday.torque.pooling.TestModule
import com.workday.torque.pooling.TestModuleInfo
import com.workday.torque.utils.*
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
			val actualTestModules = ModuleTestParser(args).parseTestsFromModuleApks()
			expectedTestModules.zip(actualTestModules).forEach { (expected, actual) ->
				assertThat(expected.testModuleInfo).isEqualTo(actual.testModuleInfo)
				expected.testMethods.zip(actual.testMethods).forEach { (expected, actual) ->
					assertThat(expected.testName).isEqualTo(actual.testName)
					assertAnnotationArrays(expected.annotations, actual.annotations)
				}
			}
		}
	}

	context("parse tests with different qualified test annotations") {
		val noAnnotationsTest = TestMethod("com.company.mymodule.test#testNoAnnotations",
				listOf(TEST_ANNOTATION_QUALIFIED,
						METADATA_ANNOTATION_QUALIFIED))
		val ignoredTest = TestMethod("com.company.mymodule.test#testIgnored",
				listOf(IGNORE_TEST_ANNOTATION_QUALIFIED,
						TEST_ANNOTATION_QUALIFIED,
						METADATA_ANNOTATION_QUALIFIED))
		val mediumTest = TestMethod("com.company.mymodule.test#testMediumTest",
				listOf(MEDIUM_TEST_ANNOTATION_QUALIFIED,
						TEST_ANNOTATION_QUALIFIED,
						METADATA_ANNOTATION_QUALIFIED))
		val flakyMediumTest = TestMethod("com.company.mymodule.test#testMultiAnnotations",
				listOf(MEDIUM_TEST_ANNOTATION_QUALIFIED,
						FLAKY_TEST_ANNOTATION_QUALIFIED,
						TEST_ANNOTATION_QUALIFIED,
						METADATA_ANNOTATION_QUALIFIED))
		val ignoredFlakyMediumTest = TestMethod("com.company.mymodule.test#testPositiveNegativeAnnotations",
				listOf(IGNORE_TEST_ANNOTATION_QUALIFIED,
						MEDIUM_TEST_ANNOTATION_QUALIFIED,
						FLAKY_TEST_ANNOTATION_QUALIFIED,
						TEST_ANNOTATION_QUALIFIED,
						METADATA_ANNOTATION_QUALIFIED))
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

		given("no Annotations and with default org.junit.Ignore Excluded Annotations") {
			val args = Args().apply {
				testApkPaths = listOf(testApkPath)
				appApkPath = targetApkPath
			}

			it("filters out org.junit.Ignored tests only") {
				val expectedTestMethods = listOf(
						noAnnotationsTest,
						mediumTest,
						flakyMediumTest)
				val expectedTestModules = listOf(TestModule(testModuleInfo, expectedTestMethods))
				val actualTestModules = ModuleTestParser(args, apkTestParser).parseTestsFromModuleApks()
				assertTestModules(expectedTestModules, actualTestModules)
			}
		}

		given("androidx.test.filters.MediumTest Annotations and with default Ignore Excluded Annotations") {
			val args = Args().apply {
				testApkPaths = listOf(testApkPath)
				appApkPath = targetApkPath
				includedAnnotations = listOf("androidx.test.filters.MediumTest")
			}

			it("filters to androidx.test.filters.MediumTest only") {
				val expectedTestMethods = listOf(mediumTest, flakyMediumTest)
				val expectedTestModules = listOf(TestModule(testModuleInfo, expectedTestMethods))
				val actualTestModules = ModuleTestParser(args, apkTestParser).parseTestsFromModuleApks()
				assertTestModules(expectedTestModules, actualTestModules)
			}
		}

		given("androidx.test.filters.MediumTest androidx.test.filters.FlakyTest Annotations and with default Ignore Excluded Annotations") {
			val args = Args().apply {
				testApkPaths = listOf(testApkPath)
				appApkPath = targetApkPath
				includedAnnotations = listOf("androidx.test.filters.MediumTest", "androidx.test.filters.FlakyTest")
			}

			it("filters to androidx.test.filters.MediumTest androidx.test.filters.FlakyTest tests only") {
				val expectedTestMethods = listOf(flakyMediumTest)
				val expectedTestModules = listOf(TestModule(testModuleInfo, expectedTestMethods))
				val actualTestModules = ModuleTestParser(args, apkTestParser).parseTestsFromModuleApks()
				assertTestModules(expectedTestModules, actualTestModules)
			}
		}

		given("FlakyTest Excluded Annotations") {
			val args = Args().apply {
				testApkPaths = listOf(testApkPath)
				appApkPath = targetApkPath
				excludedAnnotations = listOf("androidx.test.filters.FlakyTest")
			}

			it("filters out androidx.test.filters.FlakyTest tests only") {
				val expectedTestMethods = listOf(noAnnotationsTest, ignoredTest, mediumTest)
				val expectedTestModules = listOf(TestModule(testModuleInfo, expectedTestMethods))
				val actualTestModules = ModuleTestParser(args, apkTestParser).parseTestsFromModuleApks()
				assertTestModules(expectedTestModules, actualTestModules)
			}
		}
	}

	context("parse tests with different unqualified test annotations") {
		val noAnnotationsTest = TestMethod("com.company.mymodule.test#testNoAnnotations",
				listOf(TEST_ANNOTATION_QUALIFIED,
						METADATA_ANNOTATION_QUALIFIED))
		val ignoredTest = TestMethod("com.company.mymodule.test#testIgnored",
				listOf(IGNORE_TEST_ANNOTATION_QUALIFIED,
						TEST_ANNOTATION_QUALIFIED,
						METADATA_ANNOTATION_QUALIFIED))
		val mediumTest = TestMethod("com.company.mymodule.test#testMediumTest",
				listOf(MEDIUM_TEST_ANNOTATION_QUALIFIED,
						TEST_ANNOTATION_QUALIFIED,
						METADATA_ANNOTATION_QUALIFIED))
		val flakyMediumTest = TestMethod("com.company.mymodule.test#testMultiAnnotations",
				listOf(MEDIUM_TEST_ANNOTATION_QUALIFIED,
						FLAKY_TEST_ANNOTATION_QUALIFIED,
						TEST_ANNOTATION_QUALIFIED,
						METADATA_ANNOTATION_QUALIFIED))
		val ignoredFlakyMediumTest = TestMethod("com.company.mymodule.test#testPositiveNegativeAnnotations",
				listOf(IGNORE_TEST_ANNOTATION_QUALIFIED,
						MEDIUM_TEST_ANNOTATION_QUALIFIED,
						FLAKY_TEST_ANNOTATION_QUALIFIED,
						TEST_ANNOTATION_QUALIFIED,
						METADATA_ANNOTATION_QUALIFIED))
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

		given("no Annotations and with default Ignore Excluded Annotations") {
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
				val actualTestModules = ModuleTestParser(args, apkTestParser).parseTestsFromModuleApks()
				assertTestModules(expectedTestModules, actualTestModules)
			}
		}

		given("MediumTest Annotations and with default Ignore Excluded Annotations") {
			val args = Args().apply {
				testApkPaths = listOf(testApkPath)
				appApkPath = targetApkPath
				includedAnnotations = listOf("MediumTest")
			}

			it("filters to MediumTest only") {
				val expectedTestMethods = listOf(mediumTest, flakyMediumTest)
				val expectedTestModules = listOf(TestModule(testModuleInfo, expectedTestMethods))
				val actualTestModules = ModuleTestParser(args, apkTestParser).parseTestsFromModuleApks()
				assertTestModules(expectedTestModules, actualTestModules)
			}
		}

		given("MediumTest FlakyTest Annotations and with default Ignore Excluded Annotations") {
			val args = Args().apply {
				testApkPaths = listOf(testApkPath)
				appApkPath = targetApkPath
				includedAnnotations = listOf("MediumTest", "FlakyTest")
			}

			it("filters to MediumTest FlakyTest tests only") {
				val expectedTestMethods = listOf(flakyMediumTest)
				val expectedTestModules = listOf(TestModule(testModuleInfo, expectedTestMethods))
				val actualTestModules = ModuleTestParser(args, apkTestParser).parseTestsFromModuleApks()
				assertTestModules(expectedTestModules, actualTestModules)
			}
		}

		given("FlakyTest Excluded Annotations") {
			val args = Args().apply {
				testApkPaths = listOf(testApkPath)
				appApkPath = targetApkPath
				excludedAnnotations = listOf("FlakyTest")
			}

			it("filters out FlakyTest tests only") {
				val expectedTestMethods = listOf(noAnnotationsTest, ignoredTest, mediumTest)
				val expectedTestModules = listOf(TestModule(testModuleInfo, expectedTestMethods))
				val actualTestModules = ModuleTestParser(args, apkTestParser).parseTestsFromModuleApks()
				assertTestModules(expectedTestModules, actualTestModules)
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
				listOf(TEST_ANNOTATION_QUALIFIED,
						METADATA_ANNOTATION_QUALIFIED))
		val mediumSpecificTest = TestMethod("com.company.mymodule.SomeSpecificTest#someTest",
				listOf(MEDIUM_TEST_ANNOTATION_QUALIFIED,
						TEST_ANNOTATION_QUALIFIED,
						METADATA_ANNOTATION_QUALIFIED))
		val mediumOtherTest = TestMethod("com.company.mymodule.SomeOtherTest#someTest",
				listOf(MEDIUM_TEST_ANNOTATION_QUALIFIED,
						TEST_ANNOTATION_QUALIFIED,
						METADATA_ANNOTATION_QUALIFIED))
		val flakyMediumTest = TestMethod("com.company.mymodule.SomeSpecificTest#someTest",
				listOf(MEDIUM_TEST_ANNOTATION_QUALIFIED,
						FLAKY_TEST_ANNOTATION_QUALIFIED,
						TEST_ANNOTATION_QUALIFIED,
						METADATA_ANNOTATION_QUALIFIED))
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

		given("MediumTest Annotations, FlakyTest ExcludedAnnotations and regex") {
			val args = Args().apply {
				testApkPaths = listOf(testApkPath)
				appApkPath = targetApkPath
				includedAnnotations = listOf("MediumTest")
				excludedAnnotations = listOf("FlakyTest")
				testClassRegexes = listOf("[a-zA-Z]+SpecificTest")
			}

			it("filters to MediumTests Non-FlakyTest SpecificTest only") {
				val expectedTestMethods = listOf(mediumSpecificTest)
				val expectedTestModules = listOf(TestModule(testModuleInfo, expectedTestMethods))
				val actualTestModules = ModuleTestParser(args, apkTestParser).parseTestsFromModuleApks()
				assertTestModules(expectedTestModules, actualTestModules)
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
					listOf(RUNS_WITH_ANDROID_ANNOTATION_QUALIFIED,
							THROWS_EXCEPTION_ANNOTATION_QUALIFIED,
							TEST_ANNOTATION_QUALIFIED)
			))
	val moduleInfo = TestModuleInfo(ModuleInfo(testPackage, testApkPath), testRunner, ModuleInfo(targetPackage, ""))
	return MutableList(moduleCount) { TestModule(moduleInfo, testMethods) }
}
