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
internal const val IteratorState = 4 shl typeDigit

internal const val ReadyForNextEntry = 1 shl (typeDigit - 1)
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
internal val NumberClosed = JsonParserState(NumberState)

internal val IteratorStarted = JsonParserState(1 or IteratorState or ReadyForNextEntry or ReadyToClose)
internal val IteratorElementFinished = JsonParserState(2 or IteratorState or ReadyToClose)
internal val IteratorAfterComma = JsonParserState(3 or IteratorState or ReadyForNextEntry)
internal val IteratorClosed = JsonParserState(IteratorState)

internal inline val JsonParserState.readyForNextEntry
    get() = (state and ReadyForNextEntry) != 0

internal inline val JsonParserState.readyToClose
    get() = (state and ReadyToClose) != 0
