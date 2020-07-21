package jp.pois.lsonkt.parser

import jp.pois.lsonkt.*
import jp.pois.lsonkt.parser.error.ParseErrorHandler
import jp.pois.lsonkt.parser.error.ParseErrorHandlerImpl
import jp.pois.lsonkt.util.*

internal abstract class IteratorParser<T>(protected val rawCharSeq: CharSequence) : Iterator<T>,
    ParseErrorHandler by ParseErrorHandlerImpl() {
    protected var cursor: Int = 0

    private val isScannedAll
        get() = currentState == IteratorClosed

    internal var currentState = IteratorStarted

    abstract fun parseNext(): T?

    override fun hasNext(): Boolean = skipUntilReadyForNextEntry()

    override fun next(): T {
        return parseNext() ?: throw NoSuchElementException()
    }

    protected fun skipUntilReadyForNextEntry(): Boolean {
        if (currentState.readyForNextEntry) return true
        if (isScannedAll) return false
        var cursor = cursor

        while (true) {
            if (cursor >= rawCharSeq.length) {
                currentState = IteratorClosed
                return false
            }

            val c = rawCharSeq[cursor]

            if (c == ',') {
                currentState = IteratorAfterComma
                this.cursor = cursor + 1
                return true
            } else if (!c.isJsonWhitespace()) failUnexpected(',', c, cursor)

            cursor++
        }
    }

    private fun classifyNumber(): Boolean {
        var cursor = cursor

        var isFloat = false
        loop@ while (++cursor < rawCharSeq.length) {
            when (rawCharSeq[cursor]) {
                JsonWhitespaceSpace, JsonWhitespaceLinefeed, JsonWhitespaceCarriageReturn, JsonWhitespaceHorizontalTab, ',' -> break@loop
                '.', 'e', 'E' -> isFloat = true
            }
        }

        this.cursor = cursor
        return isFloat
    }

    protected fun parseValue(): JsonValue? {
        var cursor = cursor

        while (true) {
            if (cursor >= rawCharSeq.length) {
                if (currentState.readyToClose) return null
                else fail()
            }

            if (!rawCharSeq[cursor].isJsonWhitespace()) break

            cursor++
        }

        val value = run {
            val c = rawCharSeq[cursor]
            when (c) {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-' -> {
                    val startAt = cursor

                    this.cursor = cursor
                    val cond = classifyNumber()
                    cursor = this.cursor

                    return@run if (cond) {
                        FloatValue(rawCharSeq.subSequence(startAt, cursor))
                    } else {
                        IntegerValue(rawCharSeq.subSequence(startAt, cursor))
                    }
                }
                't' -> {
                    if (rawCharSeq.substring(cursor, cursor + 4) == "true") {
                        cursor += 4
                        return@run BooleanValue(true)
                    }
                    fail()
                }
                'f' -> {
                    if (rawCharSeq.substring(cursor, cursor + 5) == "false") {
                        cursor += 5
                        return@run BooleanValue(false)
                    }
                    fail()
                }
                'n' -> {
                    if (rawCharSeq.substring(cursor, cursor + 4) == "null") {
                        cursor += 4
                        return@run NullValue
                    }
                    fail()
                }
                '"' -> {
                    val startAt = ++cursor

                    while (cursor < rawCharSeq.length) {
                        when (rawCharSeq[cursor]) {
                            '"' -> {
                                return@run StringValue(rawCharSeq.subSequence(startAt, cursor++))
                            }
                            '\\' -> cursor += 2
                            else -> cursor++
                        }
                    }

                    fail()
                }
                '{', '[' -> {
                }
                else -> fail("Unexpected character: '$c'")
            }

            cursor++
            while (true) {
                if (cursor >= rawCharSeq.length) fail()
                if (!rawCharSeq[cursor].isJsonWhitespace()) break
                cursor++
            }

            val startAt = cursor

            var nest = 1

            while (cursor < rawCharSeq.length) {
                when (rawCharSeq[cursor]) {
                    '[', '{' -> nest++
                    '}' -> {
                        --nest
                        if (nest == 0) {
                            if (c != '{') fail()
                            return@run ObjectValue(rawCharSeq.subSequence(startAt, cursor++))
                        }
                    }
                    ']' -> {
                        --nest
                        if (nest == 0) {
                            if (c != '[') fail()
                            return@run ArrayValue(rawCharSeq.subSequence(startAt, cursor++))
                        }
                    }
                    '"' -> {
                        stringLoop@ while (true) {
                            if (cursor >= rawCharSeq.length) fail()
                            when (rawCharSeq[cursor++]) {
                                '"' -> break@stringLoop
                                '\\' -> {
                                    cursor++
                                }
                            }
                        }
                    }
                }
                cursor++
            }

            fail()
        }

        currentState = IteratorElementFinished
        this.cursor = cursor

        return value
    }
}
