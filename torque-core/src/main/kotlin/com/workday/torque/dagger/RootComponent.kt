package com.workday.torque.dagger

import com.workday.torque.Args
import com.workday.torque.InstrumentationReader
import com.workday.torque.ProcessRunner
import com.workday.torque.Torque
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton @Component
interface RootComponent {
    val args: Args
    val processRunner: ProcessRunner
    val instrumentationReader: InstrumentationReader

    @Component.Builder
    interface Builder {
        @BindsInstance fun args(args: Args): Builder
        fun build():RootComponent
    }

    fun injectTorque(torque: Torque)
}