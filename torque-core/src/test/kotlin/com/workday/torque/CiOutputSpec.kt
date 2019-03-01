package com.workday.torque

import com.workday.torque.utils.FakeSessionFactory.sessionA
import com.workday.torque.utils.FakeSessionFactory.sessionB
import com.workday.torque.utils.FakeSessionFactory.sessionC
import com.workday.torque.utils.FakeSessionFactory.sessionD
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import java.io.File
import kotlin.test.assertEquals

class CiOutputSpec : Spek(
{
    context("test session results generates output file for CI ") {
        val resultFilePath = "test-result.txt"
        val resultFile = File(resultFilePath)

        given("all successful results") {
            val sessions = listOf(sessionA, sessionB)
            it("should write success message to result file") {
                sessions.writeCiResultToOutputFile(resultFilePath)

                val actualResult = resultFile.readLines()[0]

                val expectedResult = ALL_TESTS_PASSED_STRING
                assertEquals(expectedResult, actualResult)
            }
        }

        given("successful and failed mixed results") {
            val sessions = listOf(sessionA, sessionC)
            it("should write failed test class#method to result file") {
                sessions.writeCiResultToOutputFile(resultFilePath)

                val actualResult = resultFile.readLines()[0]

                val expectedResult = "#some.cool.class3##testThree"
                assertEquals(expectedResult, actualResult)
            }

            it("should write failed message to result file") {
                sessions.writeCiResultToOutputFile(resultFilePath)

                val actualResult = resultFile.readLines()[1]

                val expectedResult = "1 $FAILED_TESTS_STRING"
                assertEquals(expectedResult, actualResult)
            }
        }

        given("all failed results") {
            val sessions = listOf(sessionC, sessionD)
            it("should write failed test class#methods to result file") {
                sessions.writeCiResultToOutputFile(resultFilePath)

                val actualResult = resultFile.readLines().subList(0, 2)

                val expectedResult = listOf("#some.cool.class3##testThree", "#some.cool.class4##testFour")
                assertEquals(expectedResult, actualResult)
            }

            it("should write failed message to result file") {
                sessions.writeCiResultToOutputFile(resultFilePath)

                val actualResult = resultFile.readLines()[2]

                val expectedResult = "2 $FAILED_TESTS_STRING"
                assertEquals(expectedResult, actualResult)
            }
        }
    }
})
