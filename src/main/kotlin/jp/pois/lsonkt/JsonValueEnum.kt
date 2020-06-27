package jp.pois.lsonkt

enum class JsonValueEnum {
    String, Number, Object, Array, True, False, Null
}

internal inline class JsonParserState(val state: Int) {
    @Suppress("NOTHING_TO_INLINE")
    inline operator fun compareTo(other: JsonParserState) = state - other.state
}

private const val typeDigit = 16

internal const val innerMask = (1 shl typeDigit) - 1
internal const val typeMask = -1 xor innerMask
internal const val KeywordState = 1 shl 0
internal const val NumberState = 2 shl typeDigit
internal const val StringState = 3 shl typeDigit
internal const val ArrayState = 4 shl typeDigit
internal const val ObjectState = 5 shl typeDigit
internal const val ObjectKeyState = 1 shl (typeDigit - 3)

internal const val ReadyForNextValue = 1 shl (typeDigit - 1)
internal const val ReadyToClose = 1 shl (typeDigit - 2)

internal val Initial = JsonParserState(0)

internal val KeywordFinished = JsonParserState(0 or KeywordState)

internal val NumberAfterMinus = JsonParserState(1 or NumberState)
internal val NumberPositive = JsonParserState(2 or NumberState)
internal val NumberZeroInt = JsonParserState(3 or NumberState or ReadyToClose)
internal val NumberNotZeroInt = JsonParserState(4 or NumberState or ReadyToClose)
internal val NumberAfterDot = JsonParserState(5 or NumberState)
internal val NumberInFracDigit = JsonParserState(6 or NumberState or ReadyToClose)
internal val NumberAfterE = JsonParserState(7 or NumberState)
internal val NumberAfterSign = JsonParserState(8 or NumberState)
internal val NumberInExpDigit = JsonParserState(9 or NumberState or ReadyToClose)
internal val NumberClosed = JsonParserState(0 or NumberState)

internal val StringStarted = JsonParserState(1 or StringState)
internal val StringAfterBackSlash = JsonParserState(2 or StringState)
internal val StringUnicode = JsonParserState(3 or StringState)
internal val StringClosed = JsonParserState(0 or StringState)

internal val ArrayStarted = JsonParserState(1 or ArrayState or ReadyForNextValue or ReadyToClose)
internal val ArrayValueFinished = JsonParserState(2 or ArrayState)
internal val ArrayAfterComma = JsonParserState(3 or ArrayState or ReadyForNextValue)
internal val ArrayClosed = JsonParserState(0 or ArrayState)

internal val ObjectStarted = JsonParserState(1 or ObjectState or ReadyForNextValue or ReadyToClose)
internal val ObjectInKey = JsonParserState(ObjectState or ObjectKeyState)
internal val ObjectKeyFinished = JsonParserState(2 or ObjectState)
internal val ObjectAfterColon = JsonParserState(3 or ObjectState)
internal val ObjectEntryFinished = JsonParserState(4 or ObjectState or ReadyToClose)
internal val ObjectAfterComma = JsonParserState(5 or ObjectState or ReadyForNextValue)
internal val ObjectClosed = JsonParserState(0 or ObjectState)

internal inline val JsonParserState.isFinished
    get() = (state and innerMask) == 0

private inline val JsonParserState.type
    get() = state and typeMask

internal inline val JsonParserState.isNumber
    get() = type == NumberState

internal inline val JsonParserState.isString
    get() = type == StringState

internal inline val JsonParserState.isArray
    get() = type == ArrayState

internal inline val JsonParserState.isObject
    get() = type == ObjectState

internal inline val JsonParserState.readyForNextValue
    get() = (state and ReadyForNextValue) != 0

internal inline val JsonParserState.readyToClose
    get() = (state and ReadyToClose) != 0
