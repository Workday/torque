package com.workday.torque

import com.gojuno.commander.os.log
import com.workday.torque.dagger.DaggerRootComponent
import com.workday.torque.dagger.RootComponent
import com.workday.torque.pooling.TestPool
import io.reactivex.Single
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.inject.Inject

class Torque(args: Args, private val testRunFactory: TestRunFactory = TestRunFactory()) {

    @Inject internal lateinit var args: Args
    @Inject internal lateinit var moduleTestParser: ModuleTestParser
    @Inject internal lateinit var adbDeviceFinder: AdbDeviceFinder
    @Inject internal lateinit var resultWriter: ResultWriter

    private val rootComponent: RootComponent

    init {
        rootComponent = DaggerRootComponent.builder()
                .args(args)
                .build()

        rootComponent.injectTorque(this)
    }

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
                        testRunFactory.createTestSession(adbDevice, testPool, rootComponent)
                                .run()
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
