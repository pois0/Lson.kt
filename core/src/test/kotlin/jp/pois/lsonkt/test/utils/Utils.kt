package jp.pois.lsonkt.test.utils

import kotlin.reflect.KFunction
import kotlin.reflect.jvm.isAccessible

val <R> KFunction<R>.access
    get() = apply {
        isAccessible = true
    }
