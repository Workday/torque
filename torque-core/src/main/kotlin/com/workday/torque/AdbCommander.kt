package com.workday.torque

import com.gojuno.commander.android.AdbDevice
import com.gojuno.commander.android.adb
import com.gojuno.commander.android.deviceModel
import com.gojuno.commander.os.Notification
import com.gojuno.commander.os.log
import com.gojuno.commander.os.process
import rx.Observable

fun connectedAdbDevices(unbufferedOutput: Boolean = true): Observable<Set<AdbDevice>> {
    return process(
        commandAndArgs = listOf(adb, "devices"),
        unbufferedOutput = unbufferedOutput
    )
        .ofType(Notification.Exit::class.java)
        .map { it.output.readText() }
        .map {
            when (it.contains("List of devices attached")) {
                true -> it
                false -> throw IllegalStateException("Adb output is not correct: $it.")
            }
        }
        .retry { retryCount, exception ->
            val shouldRetry = retryCount < 5 && exception is IllegalStateException
            if (shouldRetry) {
                log("runningEmulators: retrying $exception.")
            }

            shouldRetry
        }
        .flatMapIterable {
            it
                .substringAfter("List of devices attached")
                .split(System.lineSeparator())
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .filter { it.contains("online") || it.contains("device") }
        }
        .map { line ->
            val serial = line.substringBefore("\t")
            val online = when {
                line.contains("offline", ignoreCase = true) -> false
                line.contains("device", ignoreCase = true) -> true
                else -> throw IllegalStateException("Unknown adb output for device: $line")
            }
            AdbDevice(id = serial, online = online)
        }
        .flatMapSingle { device ->
            deviceModel(device).map { model ->
                device.copy(model = model)
            }
        }
        .toList()
        .map { it.toSet() }
        .doOnError { log("Error during getting connectedAdbDevices, error = $it") }
}