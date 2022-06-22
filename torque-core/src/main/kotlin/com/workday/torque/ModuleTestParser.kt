package com.workday.torque

import com.gojuno.commander.os.log
import com.linkedin.dex.parser.TestAnnotation
import com.linkedin.dex.parser.TestMethod
import com.workday.torque.pooling.ModuleInfo
import com.workday.torque.pooling.TestModule
import com.workday.torque.pooling.TestModuleInfo
import java.io.File
import java.util.concurrent.TimeUnit

class ModuleTestParser(private val args: Args, private val apkTestParser: ApkTestParser = ApkTestParser()) {
    fun parseTestsFromModuleApks(): List<TestModule> {
        return args.testApkPaths.fold(mutableListOf()) { accumulatedModules, testApkPath ->
            accumulatedModules.apply {
                val testMethods = apkTestParser.getTests(testApkPath)
                        .filterAnnotations(includedAnnotations = args.includedAnnotations, excludedAnnotations = args.excludedAnnotations)
                        .filterClassRegexes(args.testClassRegexes)
                println("Filtered tests count: ${testMethods.size}")
                val testApkFile = File(testApkPath)

                val time = System.currentTimeMillis()
                val expired = time + TimeUnit.SECONDS.toMillis(5)
                while((!testApkFile.exists() || testApkFile.length() < 1024) && System.currentTimeMillis() < expired) {
                    Thread.sleep(1000)
                }
                val moduleInfo = createModuleInfo(testApkPath)
                val isMyModule = moduleInfo.moduleInfo.pathToApk.contains(other = "-DEBUG.apk", ignoreCase = false)
                if (!isMyModule) {
                    add(TestModule(moduleInfo, testMethods))
                }
            }
        }
    }

    private fun List<TestMethod>.filterAnnotations(
			includedAnnotations: List<String>,
			excludedAnnotations: List<String>
    ): List<TestMethod> {
        return filter { it.isValidAfterAnnotationsCheck(includedAnnotations = includedAnnotations, excludedAnnotations = excludedAnnotations) }
    }

    private fun TestMethod.isValidAfterAnnotationsCheck(
			includedAnnotations: List<String>,
			excludedAnnotations: List<String>
    ): Boolean {
        return includedAnnotations.all { annotations.hasAnnotation(it) } && excludedAnnotations.all { annotations.hasAnnotation(it).not() }
    }

    private fun List<TestAnnotation>.hasAnnotation(annotation: String): Boolean {
        return any { it.name.contains(annotation) }
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
