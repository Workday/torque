package com.workday.torque.pooling

import com.workday.torque.TestPackage
import com.workday.torque.TestRunner
import com.workday.torque.utils.createTestMethodsList
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import java.util.LinkedList
import kotlin.test.assertEquals

class TestPoolSpec : Spek(
{

    context("split modular tests into poolable chunks") {
        val testPackage1 = TestPackage.Valid("com.company.mymodule.test")
        val testRunner1 = TestRunner.Valid("android.support.test.runner.AndroidJUnitRunner")
        val moduleInfo1 = TestModuleInfo(testPackage1, testRunner1, "")
        val testPackage2 = TestPackage.Valid("com.company.myapp.debug.test")
        val testRunner2 = TestRunner.Valid("com.company.myapp.testrunner.AppApplicationRunner ")
        val moduleInfo2 = TestModuleInfo(testPackage2, testRunner2, "")

        given("pooledChunks from modules with tests count more than chunk size") {
            val testMethods1 = createTestMethodsList(37)
            val testMethods2 = createTestMethodsList(23)
            val testModules = listOf(TestModule(moduleInfo1, testMethods1), TestModule(moduleInfo2, testMethods2))
            val testPool = TestPool(testModules, testChunkSize = 5)

            val pooledChunks = testPool.getPooledChunks()

            it("should have correct number of chunks") {
                assertEquals(13, pooledChunks.size)
            }

            it("should have accumulated number of tests") {
                val accTestsCount = pooledChunks.sumBy { it.testMethods.size }
                assertEquals(60, accTestsCount)
            }
        }

        given("pooledChunks from modules with tests count less than chunk size") {
            val testMethods1 = createTestMethodsList(3)
            val testMethods2 = createTestMethodsList(2)
            val testModules = listOf(TestModule(moduleInfo1, testMethods1), TestModule(moduleInfo2, testMethods2))
            val testPool = TestPool(testModules, testChunkSize = 5)

            val pooledChunks = testPool.getPooledChunks()

            it("should have at least one chunk for each module") {
                assertEquals(2, pooledChunks.size)
            }

            it("should have accumulated number of tests") {
                val accTestsCount = pooledChunks.sumBy { it.testMethods.size }
                assertEquals(5, accTestsCount)
            }
        }

        given("pooledChunks with multiple chunks") {
            val testMethods1 = createTestMethodsList(100)
            val testModules = listOf(TestModule(moduleInfo1, testMethods1))
            val testPool = TestPool(testModules)

            it("should always be able to grab all of the chunks without concurrency issues") {
                val deviceCount = 10
                val deferredDeviceRuns = (0..deviceCount).map {
                    GlobalScope.async(
                            context = Dispatchers.Default,
                            start = CoroutineStart.DEFAULT,
                            block = { testPool.getPooledChunks() }
                    )
                }
                runBlocking {
                    val pooledChunksSize = deferredDeviceRuns.sumBy {
                        it.await().size
                    }
                    assertEquals(100, pooledChunksSize)
                }
            }
        }
    }
})

private fun TestPool.getPooledChunks(): List<TestChunk> {
    return LinkedList<TestChunk>().apply {
        do {
            val chunk = getNextTestChunk()
            if (chunk != null) {
                add(chunk)
            }
        } while (chunk != null)
    }
}
