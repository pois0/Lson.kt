@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package jp.pois.lsonkt

import jp.pois.liter.literList
import jp.pois.liter.literMap
import jp.pois.lsonkt.source.CharSequenceSlice
import jp.pois.lsonkt.util.*

sealed class JsonValue {
    protected var failed = false

    protected fun fail(): Nothing {
        failed = true
        throw ParsingFailedException()
    }

    protected fun fail(str: String): Nothing {
        failed = true
        throw ParsingFailedException(str)
    }

    protected fun fail(e: Throwable): Nothing {
        failed = true
        throw e
    }

    protected fun failUnexpected(expected: Char, actual: Char, cursor: Int): Nothing {
        failed = true
        throw ParsingFailedException("Expected: '$expected', Actual: '$actual' at cursor: $cursor")
    }
}

object NullValue : JsonValue()

data class BooleanValue(val value: Boolean) : JsonValue() {
    override fun toString() = value.toString()
}

data class IntegerValue(val rawCharSeq: CharSequenceSlice) : JsonValue() {
    companion object {
        private const val Limit = Long.MAX_VALUE / 10
    }

    val value: Long by lazy { parseToInteger() }

    private fun parseToInteger(): Long {
        var value = 0L
        var cursor = 0
        val negative = if (rawCharSeq[cursor] == '-') {
            if (rawCharSeq.length == 1) fail(NumberFormatException())
            cursor++
            true
        } else false

        if (rawCharSeq[cursor] == '0') {
            if (cursor + 1 < rawCharSeq.length) fail(NumberFormatException())

            return 0L
        } else {
            cursor--
        }

        while (++cursor < rawCharSeq.length) {
            val c = rawCharSeq[cursor] - '0'
            if (c !in 0..9) fail(NumberFormatException())
            if (value >= Limit) fail(NumberFormatException())
            value *= 10
            value += c
        }

        return if (negative) -value else value
    }

    override fun toString(): String = value.toString()
}

data class FloatValue(val rawCharSeq: CharSequenceSlice) : JsonValue() {
    val value: Double by lazy { parseToDouble() }

    // TODO: Parse the string as a double strictly
    private fun parseToDouble(): Double = rawCharSeq.toString().toDouble()

    override fun toString(): String = value.toString()
}

data class StringValue(val rawCharSeq: CharSequenceSlice) : JsonValue(), CharSequence {
    val value: String by lazy { parseToString() }

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

abstract class IteratorValue<T>(val rawCharSeq: CharSequenceSlice) : JsonValue() {
    protected var cursor: Int = 0

    protected val isScannedAll
        get() = currentState == IteratorClosed

    internal var currentState = IteratorStarted

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

    protected fun classifyNumber(): Boolean {
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
                        FloatValue(rawCharSeq.slice(startAt, cursor))
                    } else {
                        IntegerValue(rawCharSeq.slice(startAt, cursor))
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
                                return@run StringValue(rawCharSeq.slice(startAt, cursor++))
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
                            return@run ObjectValue(rawCharSeq.slice(startAt, cursor++))
                        }
                    }
                    ']' -> {
                        --nest
                        if (nest == 0) {
                            if (c != '[') fail()
                            return@run ArrayValue(rawCharSeq.slice(startAt, cursor++))
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

    fun rawIterator(): Iterator<T> = JsonIterator()

    abstract fun parseNext(): T?

    protected inner class JsonIterator : Iterator<T> {
        override fun hasNext(): Boolean = skipUntilReadyForNextEntry()

        override fun next(): T {
            return parseNext() ?: throw NoSuchElementException()
        }
    }
}

@Suppress("MemberVisibilityCanBePrivate")
class ArrayValue(rawCharSeq: CharSequenceSlice) : IteratorValue<JsonValue>(rawCharSeq), List<JsonValue> {
    private val literList by lazy {
        if (currentState != IteratorStarted) throw IllegalStateException()
        rawIterator().literList()
    }

    override val size: Int
        get() = literList.size

    override fun contains(element: JsonValue): Boolean = literList.contains(element)

    override fun containsAll(elements: Collection<JsonValue>): Boolean = literList.containsAll(elements)

    override fun get(index: Int): JsonValue = literList[index]

    override fun indexOf(element: JsonValue): Int = literList.indexOf(element)

    override fun isEmpty(): Boolean = literList.isEmpty()

    override fun iterator(): Iterator<JsonValue> = literList.iterator()

    override fun lastIndexOf(element: JsonValue): Int = literList.lastIndexOf(element)

    override fun listIterator(): ListIterator<JsonValue> = literList.listIterator()

    override fun listIterator(index: Int): ListIterator<JsonValue> = literList.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int): List<JsonValue> = literList.subList(fromIndex, toIndex)

    override fun parseNext(): JsonValue? = if (skipUntilReadyForNextEntry()) parseValue() else null
}

typealias NameValuePair = Pair<String, JsonValue>

class ObjectValue(rawCharSeq: CharSequenceSlice) : IteratorValue<NameValuePair>(rawCharSeq), Map<String, JsonValue> {
    private val literMap by lazy {
        if (currentState != IteratorStarted) throw IllegalStateException()
        rawIterator().literMap()
    }

    override val entries: Set<Map.Entry<String, JsonValue>>
        get() = literMap.entries

    override val keys: Set<String>
        get() = literMap.keys

    override val size: Int
        get() = literMap.size

    override val values: Collection<JsonValue>
        get() = literMap.values

    override fun containsKey(key: String): Boolean = literMap.containsKey(key)

    override fun containsValue(value: JsonValue): Boolean = literMap.containsValue(value)

    override fun get(key: String): JsonValue? = literMap[key]

    override fun isEmpty(): Boolean = literMap.isEmpty()

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
            val c = rawCharSeq[cursor++]
            when (c) {
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

        this@ObjectValue.cursor = cursor
    }
}
