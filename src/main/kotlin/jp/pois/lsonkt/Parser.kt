@file:Suppress("NOTHING_TO_INLINE", "unused")

package jp.pois.lsonkt

import jp.pois.lsonkt.source.StringSlice
import jp.pois.lsonkt.util.*

fun parse(rawString: String): JsonValue {
    return parseRoot(StringSlice(rawString))
}

internal fun parseRoot(rawCharSeq: StringSlice): JsonValue {
    var cursor = -1

    while (++cursor < rawCharSeq.length) {
        val c = rawCharSeq[cursor]
        if (c.isJsonWhitespace()) continue

        when (c) {
            '"' -> {
                val end = lastIndexOf(rawCharSeq, cursor + 1, '"')
                return StringValue(rawCharSeq.subSequence(cursor + 1, end))
            }
            '[' -> {
                val end = lastIndexOf(rawCharSeq, cursor + 1, ']')
                return ArrayValue(rawCharSeq.subSequence(cursor + 1, end))
            }
            '{' -> {
                val end = lastIndexOf(rawCharSeq, cursor + 1, '}')
                return ObjectValue(rawCharSeq.subSequence(cursor + 1, end))
            }
            't' -> {
                if (rawCharSeq.substring(cursor + 1, cursor + 4) != "rue") {
                    throw ParsingFailedException()
                }

                if (!isBlank(rawCharSeq, cursor + 4)) throw ParsingFailedException()
                return BooleanValue(true)
            }
            'f' -> {
                if (rawCharSeq.substring(cursor + 1, cursor + 5) != "alse") {
                    throw ParsingFailedException()
                }

                if (!isBlank(rawCharSeq, cursor + 5)) throw ParsingFailedException()
                return BooleanValue(false)
            }
            'n' -> {
                if (rawCharSeq.substring(cursor + 1, cursor + 4) != "ull") {
                    throw ParsingFailedException()
                }

                if (!isBlank(rawCharSeq, cursor + 4)) throw ParsingFailedException()
                return NullValue
            }
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-' -> {
                val startAt = cursor

                var isFloat = false
                loop@ while (++cursor < rawCharSeq.length) {
                    when (rawCharSeq[cursor]) {
                        JsonWhitespaceSpace, JsonWhitespaceLinefeed, JsonWhitespaceCarriageReturn, JsonWhitespaceHorizontalTab, ',' -> break@loop
                        '.', 'e', 'E' -> isFloat = true
                    }
                }

                val slice = rawCharSeq.subSequence(startAt, cursor)
                return if (isFloat) FloatValue(slice) else IntegerValue(slice)
            }
            else -> throw ParsingFailedException()
        }
    }
    throw ParsingFailedException()
}

internal inline fun lastIndexOf(rawCharSeq: StringSlice, start: Int, expected: Char): Int {
    var cursor = rawCharSeq.length
    while (true) {
        if (--cursor < start) throw ParsingFailedException()
        if (!rawCharSeq[cursor].isJsonWhitespace()) break
    }

    if (rawCharSeq[cursor] == expected) {
        return cursor
    } else {
        throw ParsingFailedException()
    }
}

internal inline fun isBlank(rawCharSeq: StringSlice, start: Int): Boolean {
    var cursor = start - 1
    while (++cursor < rawCharSeq.length) {
        if (rawCharSeq[cursor].isJsonWhitespace()) return false
    }
    return true
}
