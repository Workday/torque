package com.workday.torque

import com.workday.torque.utils.FakeSessionFactory.deviceA
import com.workday.torque.utils.FakeSessionFactory.deviceB
import com.workday.torque.utils.FakeSessionFactory.deviceC
import com.workday.torque.utils.FakeSessionFactory.deviceD
import com.workday.torque.utils.FakeSessionFactory.failedResultC
import com.workday.torque.utils.FakeSessionFactory.failedResultD
import com.workday.torque.utils.FakeSessionFactory.sessionA
import com.workday.torque.utils.FakeSessionFactory.sessionB
import com.workday.torque.utils.FakeSessionFactory.sessionC
import com.workday.torque.utils.FakeSessionFactory.sessionD
import com.workday.torque.utils.FakeSessionFactory.successResultA
import com.workday.torque.utils.FakeSessionFactory.successResultB
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.it
import kotlin.test.assertEquals

class SuiteSpec : Spek(
{
    context("test session results convert to suites") {
        val sessions = listOf(sessionA, sessionB, sessionC, sessionD)
        it("should have aggregated results and the duration of the entire suite") {
            val devices = listOf(Device(deviceA.id, deviceA.model, sessionA.logcatFile),
                                 Device(deviceB.id, deviceB.model, sessionB.logcatFile),
                                 Device(deviceC.id, deviceC.model, sessionC.logcatFile),
                                 Device(deviceD.id, deviceD.model, sessionD.logcatFile))
            val expectedSuites = listOf(Suite(
                    devices = devices,
                    tests = listOf(successResultA, successResultB, failedResultC, failedResultD),
                    passedCount = 2,
                    ignoredCount = 0,
                    failedCount = 2,
                    durationMillis = 230))

            val suites = sessions.toSuites()

            assertEquals(expectedSuites, suites)
        }
    }
})
