package com.workday.torque

import hu.akarnokd.rxjava.interop.RxJavaInterop
import io.reactivex.BackpressureStrategy

fun <T : Any> rx.Observable<T>.toV2Observable(): io.reactivex.Observable<T> {
    return  RxJavaInterop.toV2Observable(this)
}

fun <T : Any> rx.Observable<T>.toV2Flowable(): io.reactivex.Flowable<T> {
    return RxJavaInterop.toV2Flowable(this)
}

fun <T : Any> io.reactivex.Observable<T>.toV1Observable(): rx.Observable<T> {
    return RxJavaInterop.toV1Observable(this, BackpressureStrategy.BUFFER)
}

fun <T : Any> io.reactivex.Flowable<T>.toV1Observable(): rx.Observable<T> {
    return RxJavaInterop.toV1Observable(this)
}

fun <T : Any> rx.Single<T>.toV2Single(): io.reactivex.Single<T> {
    return RxJavaInterop.toV2Single(this)
}

fun <T : Any> io.reactivex.Single<T>.toV1Single(): rx.Single<T> {
    return RxJavaInterop.toV1Single(this)
}

fun <T : Any> rx.Single<T>.toV2Maybe(): io.reactivex.Maybe<T> {
    return RxJavaInterop.toV2Maybe(this)
}

fun <T : Any> io.reactivex.Maybe<T>.toV1Single(): rx.Single<T> {
    return RxJavaInterop.toV1Single(this)
}

fun io.reactivex.disposables.Disposable.toV1Subscription(): rx.Subscription {
    return RxJavaInterop.toV1Subscription(this)
}

fun rx.Subscription.toV2Disposable(): io.reactivex.disposables.Disposable {
    return RxJavaInterop.toV2Disposable(this)
}

fun rx.Scheduler.toV2Scheduler(): io.reactivex.Scheduler {
    return RxJavaInterop.toV2Scheduler(this)
}

fun io.reactivex.Scheduler.toV1Scheduler(): rx.Scheduler {
    return RxJavaInterop.toV1Scheduler(this)
}

fun io.reactivex.Completable.toV1Completable() : rx.Completable{
    return RxJavaInterop.toV1Completable(this)
}

fun rx.Completable.toV2Completable() : io.reactivex.Completable{
    return RxJavaInterop.toV2Completable(this)
}
