package com.workday.torque.dagger

import com.workday.torque.AdbDevice
import com.workday.torque.DeviceTestRunSession
import dagger.Component
import dagger.BindsInstance

@SessionScope
@Component(dependencies = [RootComponent::class], modules = [SessionModule::class])
interface SessionComponent {

    @Component.Builder
    interface Builder {
        @BindsInstance fun adbDevice(adbDevice: AdbDevice): Builder
        fun rootComponent(rootComponent: RootComponent): Builder
        fun build(): SessionComponent
    }

    fun injectDeviceTestRunSession(deviceTestRunSession: DeviceTestRunSession)
}