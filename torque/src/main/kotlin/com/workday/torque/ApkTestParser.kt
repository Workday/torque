package com.workday.torque

class ApkTestParser {
    fun getValidatedTestPackage(testApkPath: String): TestPackage.Valid {
        return parseTestPackage(testApkPath).validateTestPackage()
    }

    private fun TestPackage.validateTestPackage(): TestPackage.Valid {
        return let {
            when (it) {
                is TestPackage.Valid -> it
                is TestPackage.ParseError -> {
                    throw IllegalStateException(it.error)
                }
            }
        }
    }

    fun getValidatedTestRunner(testApkPath: String): TestRunner.Valid {
        return parseTestRunner(testApkPath).validateTestRunner()
    }

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
}
