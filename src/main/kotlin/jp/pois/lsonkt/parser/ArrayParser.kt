package jp.pois.lsonkt.parser

import jp.pois.lsonkt.JsonValue
import jp.pois.lsonkt.source.CharSequenceSlice

internal class ArrayParser(rawCharSeq: CharSequenceSlice) : IteratorParser<JsonValue>(rawCharSeq) {
    override fun parseNext(): JsonValue? = if (skipUntilReadyForNextEntry()) parseValue() else null
}
