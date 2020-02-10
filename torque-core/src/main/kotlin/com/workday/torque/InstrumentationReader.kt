package com.workday.torque

import io.reactivex.Observable
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstrumentationReader @Inject constructor() {
    fun readTestResults(output: File, timeout: Long = DEFAULT_PER_CHUNK_TIMEOUT_SECONDS): Observable<InstrumentationTestResult> {
        val timeOutException = TimeoutException("InstrumentationReader timeout after $timeout seconds")
        return readInstrumentationOutput(output)
                .asTests()
                .toV2Observable()
                .timeout(timeout, TimeUnit.SECONDS, Observable.error(timeOutException))
    }
}
