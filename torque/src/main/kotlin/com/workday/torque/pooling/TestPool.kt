package com.workday.torque.pooling

import com.linkedin.dex.parser.TestMethod
import com.workday.torque.ApkPackage
import com.workday.torque.TestRunner
import java.util.LinkedList

private const val TEST_CHUNK_SIZE = 1

data class TestModuleInfo(val moduleInfo: ModuleInfo,
                          val testRunner: TestRunner.Valid,
                          val appModuleInfo: ModuleInfo? = null)

data class ModuleInfo(val apkPackage: ApkPackage.Valid,
                      val pathToApk: String)

data class TestModule(val testModuleInfo: TestModuleInfo,
                      val testMethods: List<TestMethod>)

data class TestChunk(val index: Int,
                     val testModuleInfo: TestModuleInfo,
                     val testMethods: List<TestMethod>,
                     var retryCount: Int = 0)

class TestPool(testModules: List<TestModule>, testChunkSize: Int = TEST_CHUNK_SIZE) {

    private val testChunks = testModules.getTestChunks(testChunkSize)

    private fun List<TestModule>.getTestChunks(testChunkSize: Int): LinkedList<TestChunk> {
        return fold(LinkedList()) { accumulatedChunks, testModule ->
            accumulatedChunks.addTestChunksFromModule(testModule, testChunkSize)
        }
    }

    private fun LinkedList<TestChunk>.addTestChunksFromModule(testModule: TestModule,
                                                              testChunkSize: Int): LinkedList<TestChunk> {
        testModule.testMethods.chunked(testChunkSize)
                .forEach { testMethodsChunk -> appendTestChunk(testModule, testMethodsChunk) }
        return this
    }

    private fun LinkedList<TestChunk>.appendTestChunk(testModule: TestModule, testMethodsChunk: List<TestMethod>) {
        add(TestChunk(index = size,
                      testModuleInfo = testModule.testModuleInfo,
                      testMethods = testMethodsChunk))
    }

    @Synchronized fun getNextTestChunk(): TestChunk? = testChunks.poll()
}
