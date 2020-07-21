package jp.pois.lsonkt.parser

import jp.pois.lsonkt.parser.error.ParseErrorHandler
import jp.pois.lsonkt.parser.error.ParseErrorHandlerImpl
import jp.pois.lsonkt.util.isControlCharacter

internal class StringParser(private val rawCharSeq: CharSequence) : CharSequence,
    ParseErrorHandler by ParseErrorHandlerImpl() {
    private val value: String by lazy { parseToString() }

    override val length: Int
        get() = value.length

    private fun parseToString(): String {
        var cursor = 0

        val tmp = CharArray(rawCharSeq.length)
        var tmpCursor = 0

        while (cursor < rawCharSeq.length) {
            val c = rawCharSeq[cursor]
            tmp[tmpCursor++] = when {
                c == '\\' -> {
                    when (rawCharSeq[++cursor]) {
                        '"' -> '"'
                        '\\' -> '\\'
                        '/' -> '/'
                        'b' -> '\b'
                        'f' -> '\u000C'
                        'n' -> '\n'
                        'r' -> '\r'
                        't' -> '\t'
                        'u' -> {
                            if (cursor + 5 > rawCharSeq.length) throw IndexOutOfBoundsException()

                            rawCharSeq.substring(cursor + 1, cursor + 5).toInt(16).toChar().also {
                                cursor += 4
                            }
                        }
                        else -> fail()
                    }
                }
                c.isControlCharacter() -> fail()
                else -> c
            }
            cursor++
        }

        return String(tmp, 0, tmpCursor)
    }

    override fun get(index: Int): Char = value[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = value.subSequence(startIndex, endIndex)

    override fun toString(): String = value
}
