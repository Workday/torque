package com.workday.torque

import com.linkedin.dex.parser.TestMethod
import com.workday.torque.pooling.ModuleInfo
import com.workday.torque.pooling.TestModule
import com.workday.torque.pooling.TestModuleInfo

class ModuleTestParser(private val args: Args, private val apkTestParser: ApkTestParser = ApkTestParser()) {
    fun parseTestsFromModuleApks(): List<TestModule> {
        return args.testApkPaths.fold(mutableListOf()) { accumulatedModules, testApkPath ->
            accumulatedModules.apply {
                val testMethods = apkTestParser.getTests(testApkPath)
                        .filterAnnotations(allowedAnnotations = args.allowedAnnotations, prohibitedAnnotations = args.prohibitedAnnotations)
                        .filterClassRegexes(args.testClassRegexes)
                println("Filtered tests count: ${testMethods.size}")
                val moduleInfo = createModuleInfo(testApkPath)
                add(TestModule(moduleInfo, testMethods))
            }
        }
    }

    private fun List<TestMethod>.filterAnnotations(
            allowedAnnotations: List<String>,
            prohibitedAnnotations: List<String>
    ): List<TestMethod> {
        return filter { it.isValidAfterAnnotationsCheck(allowedAnnotations = allowedAnnotations, prohibitedAnnotations = prohibitedAnnotations) }
    }

    private fun TestMethod.isValidAfterAnnotationsCheck(
            allowedAnnotations: List<String>,
            prohibitedAnnotations: List<String>
    ): Boolean {
        return annotations.all { allowedAnnotations.hasAnnotation(it.name) }
                && prohibitedAnnotations.all { allowedAnnotations.hasAnnotation(it).not() }
    }

    private fun List<String>.hasAnnotation(annotation: String): Boolean {
        return any { it.contains(annotation) }
    }

    private fun List<TestMethod>.filterClassRegexes(regexStrings: List<String>): List<TestMethod> {
        if (regexStrings.isEmpty()) {
            return this
        }
        val patterns = regexStrings.map { it.toPattern() }
        return filter { testMethod ->
            patterns.any { pattern ->
                pattern.matcher(testMethod.getTestClassName()).matches()
            }
        }
    }

    private fun createModuleInfo(testApkPath: String): TestModuleInfo {
        val testPackage = apkTestParser.getValidatedTestPackage(testApkPath)
        val targetPackage = apkTestParser.getValidatedTargetPackage(testApkPath)
        val testRunner = apkTestParser.getValidatedTestRunner(testApkPath)
        return if (isAppTestApk(testPackage, targetPackage)) {
            val appModuleInfo = ModuleInfo(targetPackage, args.appApkPath)
            TestModuleInfo(ModuleInfo(testPackage, testApkPath), testRunner, appModuleInfo)
        } else {
            TestModuleInfo(ModuleInfo(testPackage, testApkPath), testRunner)
        }
    }

    private fun isAppTestApk(
            testPackage: ApkPackage.Valid,
            targetPackage: ApkPackage.Valid
    ) = testPackage != targetPackage
}
