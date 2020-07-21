package jp.pois.lsonkt.parser

import jp.pois.lsonkt.JsonValue
import jp.pois.lsonkt.readyToClose
import jp.pois.lsonkt.util.isJsonWhitespace

internal typealias NameValuePair = Pair<String, JsonValue>

internal class ObjectParser(rawCharSeq: CharSequence) : IteratorParser<NameValuePair>(rawCharSeq) {
    override fun parseNext(): NameValuePair? {
        if (!skipUntilReadyForNextEntry()) return null

        var cursor = cursor

        while (true) {
            if (cursor >= rawCharSeq.length) {
                if (currentState.readyToClose) return null
                else fail()
            }

            if (!rawCharSeq[cursor].isJsonWhitespace()) break

            cursor++
        }

        if (rawCharSeq[cursor++] != '"') fail()

        this.cursor = cursor

        val key = parseKey()

        cursor = this.cursor
        loop@ while (true) {
            if (cursor >= rawCharSeq.length) fail()
            val c = rawCharSeq[cursor++]
            when {
                c == ':' -> break@loop
                !c.isJsonWhitespace() -> failUnexpected(':', c, cursor)
            }
        }

        while (true) {
            if (cursor >= rawCharSeq.length) fail()
            if (!rawCharSeq[cursor].isJsonWhitespace()) break
            cursor++
        }

        this.cursor = cursor

        val value = parseValue() ?: return null

        return key to value
    }

    private fun parseKey() = buildString {
        var cursor = cursor
        loop@ while (cursor < rawCharSeq.length) {
            when (val c = rawCharSeq[cursor++]) {
                '"' -> break@loop
                '\\' -> {
                    append(
                        when (rawCharSeq[cursor++]) {
                            '"' -> '"'
                            '\\' -> '\\'
                            '/' -> '/'
                            'b' -> '\b'
                            'f' -> '\u000C'
                            'n' -> '\n'
                            'r' -> '\r'
                            't' -> '\t'
                            'u' -> {
                                if (cursor + 5 > rawCharSeq.length) fail()

                                rawCharSeq.substring(cursor, cursor + 4).toInt(16).toChar().also {
                                    cursor += 4
                                }
                            }
                            else -> fail()
                        })
                }
                else -> append(c)
            }
        }

        this@ObjectParser.cursor = cursor
    }
}
