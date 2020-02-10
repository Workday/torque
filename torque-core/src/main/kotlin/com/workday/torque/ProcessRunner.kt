package com.workday.torque

import com.gojuno.commander.android.adb
import com.gojuno.commander.os.Notification
import com.gojuno.commander.os.process
import io.reactivex.Observable
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessRunner @Inject constructor() {
    fun run(commandAndArgs: List<String>,
            timeout: Timeout? = Timeout(DEFAULT_PER_CHUNK_TIMEOUT_SECONDS.toInt(), TimeUnit.SECONDS),
            redirectOutputTo: File? = null,
            keepOutputOnExit: Boolean = false,
            unbufferedOutput: Boolean = false,
            print: Boolean = false,
            destroyOnUnsubscribe: Boolean = false): Observable<Notification> {
        return process(commandAndArgs, timeout?.toPair(), redirectOutputTo, keepOutputOnExit, unbufferedOutput, print, destroyOnUnsubscribe)
                .toV2Observable()
    }

    fun runAdb(commandAndArgs: List<String>,
               timeout: Timeout? = Timeout(DEFAULT_PER_CHUNK_TIMEOUT_SECONDS.toInt(), TimeUnit.SECONDS),
               redirectOutputTo: File? = null,
               keepOutputOnExit: Boolean = false,
               unbufferedOutput: Boolean = false,
               print: Boolean = false,
               destroyOnUnsubscribe: Boolean = false
    ): Observable<Notification> {
        return run(listOf(adb) + commandAndArgs,
                   timeout,
                   redirectOutputTo,
                   keepOutputOnExit,
                   unbufferedOutput,
                   print,
                   destroyOnUnsubscribe)
    }
}
