@file:Suppress("unused", "NOTHING_TO_INLINE")

package jp.pois.lsonkt

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun JsonValue.isNull(): Boolean {
    contract {
        returns(true) implies (this@isNull is NullValue)
    }
    return this is NullValue
}

@OptIn(ExperimentalContracts::class)
inline fun JsonValue.isBoolean(): Boolean {
    contract {
        returns(true) implies (this@isBoolean is BooleanValue)
    }
    return this is BooleanValue
}

@OptIn(ExperimentalContracts::class)
inline fun JsonValue.isInteger(): Boolean {
    contract {
        returns(true) implies (this@isInteger is IntegerValue)
    }
    return this is IntegerValue
}

@OptIn(ExperimentalContracts::class)
inline fun JsonValue.isFloat(): Boolean {
    contract {
        returns(true) implies (this@isFloat is FloatValue)
    }
    return this is FloatValue
}

@OptIn(ExperimentalContracts::class)
inline fun JsonValue.isString(): Boolean {
    contract {
        returns(true) implies (this@isString is StringValue)
    }
    return this is StringValue
}

@OptIn(ExperimentalContracts::class)
inline fun JsonValue.isArray(): Boolean {
    contract {
        returns(true) implies (this@isArray is ArrayValue)
    }
    return this is ArrayValue
}

@OptIn(ExperimentalContracts::class)
inline fun JsonValue.isObject(): Boolean {
    contract {
        returns(true) implies (this@isObject is ObjectValue)
    }
    return this is ObjectValue
}

inline fun JsonValue?.asBoolean(): BooleanValue = this as BooleanValue

inline fun JsonValue?.asBooleanOrNull(): BooleanValue? = this as? BooleanValue

inline fun JsonValue?.asInteger(): IntegerValue = this as IntegerValue

inline fun JsonValue?.asIntegerOrNull(): IntegerValue? = this as? IntegerValue

inline fun JsonValue?.asFloat(): FloatValue = this as FloatValue

inline fun JsonValue?.asFloatOrNull(): FloatValue? = this as? FloatValue?

inline fun JsonValue?.asString(): StringValue = this as StringValue

inline fun JsonValue?.asStringOrNull(): StringValue? = this as? StringValue

inline fun JsonValue?.asArray(): ArrayValue = this as ArrayValue

inline fun JsonValue?.asArrayOrNull(): ArrayValue? = this as? ArrayValue

inline fun JsonValue?.asObject(): ObjectValue = this as ObjectValue

inline fun JsonValue?.asObjectOrNull(): ObjectValue? = this as? ObjectValue

inline operator fun JsonValue?.get(index: Int): JsonValue {
    if (this !is ArrayValue) throw UnsupportedOperationException()

    return get(index)
}

@OptIn(ExperimentalContracts::class)
inline fun JsonValue?.getOrNull(index: Int): JsonValue? {
    contract {
        returnsNotNull() implies (this@getOrNull is ArrayValue)
    }

    return (this as? ArrayValue)?.get(index)
}

inline operator fun JsonValue?.get(key: String): JsonValue? {
    if (this !is ObjectValue) throw UnsupportedOperationException()

    return get(key)
}

inline fun JsonValue?.getOrNull(key: String): JsonValue? = (this as? ObjectValue)?.get(key)

fun JsonValue.parseDescendant() {
    when (this) {
        is IntegerValue -> value
        is FloatValue -> value
        is StringValue -> toString()
        is ArrayValue -> {
            forEach {
                it.parseDescendant()
            }
        }
        is ObjectValue -> {
            forEach { _, value ->
                value.parseDescendant()
            }
        }
        else -> {
        }
    }
}

private const val defaultStopAsync = 2000
private const val defaultCapacity = 3

suspend fun JsonValue.parseDescendantAsync(stopAsync: Int = defaultStopAsync, capacity: Int = defaultCapacity): Unit =
    coroutineScope<Unit> {
        when (this@parseDescendantAsync) {
            is IntegerValue -> value
            is FloatValue -> value
            is StringValue -> toString()
            is ArrayValue -> {
                if (this@parseDescendantAsync.rawCharSeq.length < stopAsync) {
                    parseDescendant()
                } else {
                    this@parseDescendantAsync.asFlow().buffer(capacity).collect { value ->
                        launch { value.parseDescendantAsync(stopAsync, capacity) }
                    }
                }
            }
            is ObjectValue -> {
                if (this@parseDescendantAsync.rawCharSeq.length < stopAsync) {
                    parseDescendant()
                } else {
                    this@parseDescendantAsync.asIterable().asFlow().buffer(capacity).collect { (_, value) ->
                        launch { value.parseDescendantAsync(stopAsync, capacity) }
                    }
                }
            }
            else -> {
        }
    }
}
