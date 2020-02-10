package com.workday.torque

import com.gojuno.commander.android.connectedAdbDevices
import com.gojuno.commander.os.log
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdbDeviceFinder @Inject constructor() {

    fun onlineAdbDevices(): Single<List<AdbDevice>> {
        return connectedAdbDevices()
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
