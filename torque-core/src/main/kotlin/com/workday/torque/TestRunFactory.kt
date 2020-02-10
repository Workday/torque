package com.workday.torque

import com.workday.torque.dagger.RootComponent
import com.workday.torque.pooling.TestPool

class TestRunFactory {

    fun createTestSession(adbDevice: AdbDevice, testPool: TestPool, rootComponent: RootComponent): DeviceTestRunSession {
        return DeviceTestRunSession(adbDevice, testPool, rootComponent)
    }
}
