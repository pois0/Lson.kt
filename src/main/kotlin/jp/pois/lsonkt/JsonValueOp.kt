@file:Suppress("unused", "NOTHING_TO_INLINE")

package jp.pois.lsonkt

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

fun JsonValue.parseDescendant() {
    parseAll()
    if (this.isArray()) {
        this.forEach {
            it.parseDescendant()
        }
    } else if (this.isObject()) {
        this.values.forEach {
            it.parseDescendant()
        }
    }
}
