package com.workday.torque

import com.gojuno.commander.os.log
import io.reactivex.Single

class AdbDeviceFinder {

    fun onlineAdbDevices(): Single<List<AdbDevice>> {
        return connectedAdbDevices(unbufferedOutput = false)
                .toSingle()
                .toV2Single()
                .map {
                    it.filter { it.online }.apply {
                        if (isEmpty()) {
                            throw IllegalStateException("Error: No devices available for tests.")
                        }
                    }.map { it.toAdbDeviceWithInstalledPackages() }
                }
                .doOnSuccess { log("${it.size} connected online adb device(s): $it") }
    }

    private fun com.gojuno.commander.android.AdbDevice.toAdbDeviceWithInstalledPackages() = AdbDevice(id, model, online)

}
