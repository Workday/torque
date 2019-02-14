package com.workday.torque

import com.gojuno.commander.os.log
import com.workday.torque.pooling.TestPool
import io.reactivex.Single
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class Torque(private val args: Args,
             private val moduleTestParser: ModuleTestParser = ModuleTestParser(args),
             private val adbDeviceFinder: AdbDeviceFinder = AdbDeviceFinder(),
             private val testRunFactory: TestRunFactory = TestRunFactory(),
             private val resultWriter: ResultWriter = ResultWriter(args)) {

    fun run() {
        val startTime = System.currentTimeMillis()
        if (args.verboseOutput) {
            log("$args")
        }
        val testModules = moduleTestParser.parseTestsFromModuleApks()
        val testPool = TestPool(testModules, args.chunkSize)
        resultWriter.clearOutputDirectory()
        val timeOutException = TimeoutException("Torque run timeout after ${args.timeoutMinutes} minutes")
        val adbDeviceTestSessions = adbDeviceFinder.onlineAdbDevices()
                .flatMap { connectedAdbDevices ->
                    val runTestsOnDevices: List<Single<AdbDeviceTestSession>> = connectedAdbDevices.map { adbDevice ->
                        testRunFactory.runTestSession(adbDevice, args, testPool)
                    }
                    Single.zip(runTestsOnDevices) { emissionArray ->
                        emissionArray.map { it as AdbDeviceTestSession }
                    }
                }
                .timeout(args.timeoutMinutes, TimeUnit.MINUTES, Single.error(timeOutException))
                .blockingGet()
        resultWriter.write(startTime, adbDeviceTestSessions)
    }
}
