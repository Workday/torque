package com.workday.torque.dagger

import com.workday.torque.AdbDevice
import com.workday.torque.Args
import com.workday.torque.LogcatFileIO
import dagger.Module
import dagger.Provides

@Module
class SessionModule {

    @Provides @SessionScope
    fun provideLogcatFileIO(adbDevice: AdbDevice, args: Args): LogcatFileIO {
        return LogcatFileIO(
                adbDevice = adbDevice,
                timeoutSeconds = args.chunkTimeoutSeconds.toInt(),
                outputDirPath = args.outputDirectory)
    }
}
