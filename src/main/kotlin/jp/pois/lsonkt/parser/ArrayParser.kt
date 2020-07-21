package jp.pois.lsonkt.parser

import jp.pois.lsonkt.JsonValue

internal class ArrayParser(rawCharSeq: CharSequence) : IteratorParser<JsonValue>(rawCharSeq) {
    override fun parseNext(): JsonValue? = if (skipUntilReadyForNextEntry()) parseValue() else null
}
