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
                        .filterAnnotations(annotations = args.annotations, notAnnotations = args.notAnnotations)
                        .filterClassRegexes(args.testClassRegexes)
                println("Filtered tests count: ${testMethods.size}")
                val moduleInfo = createModuleInfo(testApkPath)
                add(TestModule(moduleInfo, testMethods))
            }
        }
    }

    private fun List<TestMethod>.filterAnnotations(
            annotations: List<String>,
            notAnnotations: List<String>
    ): List<TestMethod> {
        return filter { it.isValidAfterAnnotationsCheck(annotations = annotations, notAnnotations = notAnnotations) }
    }

    private fun TestMethod.isValidAfterAnnotationsCheck(
            annotations: List<String>,
            notAnnotations: List<String>
    ): Boolean {
        return annotations.all { annotationNames.hasAnnotation(it) }
                && notAnnotations.all { annotationNames.hasAnnotation(it).not() }
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
        return if (testPackage != targetPackage) {
            val appModuleInfo = ModuleInfo(targetPackage, args.appApkPath)
            TestModuleInfo(ModuleInfo(testPackage, testApkPath), testRunner, appModuleInfo)
        } else {
            TestModuleInfo(ModuleInfo(testPackage, testApkPath), testRunner)
        }
    }
}
