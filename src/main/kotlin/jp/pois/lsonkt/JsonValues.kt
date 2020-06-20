@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package jp.pois.lsonkt

import jp.pois.lsonkt.source.CharSequenceSlice

sealed class JsonValue {
    protected var failed = false

    @Suppress("NOTHING_TO_INLINE")
    protected inline fun fail() {
        failed = true
        throw ParsingFailedException()
    }

    @Suppress("NOTHING_TO_INLINE")
    protected inline fun fail(e: Throwable) {
        failed = true
        throw e
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
    val value: Double by lazy {
        var cursor = 0
        var value = if (rawCharSeq[0] == '-') -0.0 else {
            cursor--
            0.0
        }

        while (++cursor < rawCharSeq.length) {
            when (rawCharSeq[cursor]) {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {

                }
            }
        }

        value
    }

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
            tmp[tmpCursor++] = if (c == '\\') {
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
                        rawCharSeq.substring(cursor + 1, cursor + 5).toInt(16).toChar().also {
                            cursor += 4
                        }
                    }
                    else -> {
                        failed = true
                        throw ParsingFailedException()
                    }
                }
            } else c
            cursor++
        }

        return String(tmp, 0, tmpCursor)
    }

    override fun get(index: Int): Char = value[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = value.subSequence(startIndex, endIndex)

    override fun toString(): String = value
}

@Suppress("MemberVisibilityCanBePrivate")
open class ArrayValue(val rawCharSeq: CharSequenceSlice) : JsonValue(), List<JsonValue> {
    private var cursor: Int = 0

    private val evaluatedList = ArrayList<JsonValue>()

    private var currentState = ArrayStarted

    override val size: Int
        get() {
            parseAll()
            return evaluatedList.size
        }

    val currentSize: Int
        get() = evaluatedList.size

    private val isScannedAll
        get() = currentState == ArrayClosed

    override fun contains(element: JsonValue): Boolean = indexOf(element) >= 0

    override fun containsAll(elements: Collection<JsonValue>): Boolean = elements.all { contains(it) }

    override fun get(index: Int): JsonValue {
        return if (index < evaluatedList.size) {
            evaluatedList[index]
        } else {
            if (!parseUntil(index + 1)) throw ParsingFailedException()

            return evaluatedList.last()
        }
    }

    override fun indexOf(element: JsonValue): Int {
        val check = evaluatedList.indexOf(element)
        if (check >= 0) return check

        if (!isScannedAll) {
            while (parseNext()) {
                if (evaluatedList.last() == element) {
                    return evaluatedList.lastIndex
                }
            }
        }

        return -1
    }

    override fun isEmpty(): Boolean {
        if (cursor != rawCharSeq.length) parseAll()
        return evaluatedList.isEmpty()
    }

    override fun iterator(): Iterator<JsonValue> = object : Iterator<JsonValue> {
        private var index = 0

        override fun hasNext(): Boolean {
            if (index < currentSize) return true

            return parseUntil(index + 1)
        }

        override fun next(): JsonValue {
            if (index < currentSize || parseUntil(index + 1)) return evaluatedList[index++]

            throw IndexOutOfBoundsException()
        }
    }

    override fun lastIndexOf(element: JsonValue): Int {
        var check = evaluatedList.lastIndexOf(element)

        while (parseNext()) {
            if (evaluatedList.last() == element) {
                check = evaluatedList.lastIndex
            }
        }

        return check
    }

    override fun listIterator(): ListIterator<JsonValue> = listIterator(0)

    override fun listIterator(index: Int): ListIterator<JsonValue> = object : ListIterator<JsonValue> {
        private var currentIndex = index

        init {
            if (currentIndex > currentSize) {
                if (isScannedAll) {
                    throw IndexOutOfBoundsException()
                }

                if (!parseUntil(currentIndex + 1)) {
                    throw IndexOutOfBoundsException()
                }
            }
        }

        override fun hasNext(): Boolean {
            if (currentIndex < currentSize) return true

            return parseUntil(currentIndex + 1)
        }

        override fun hasPrevious(): Boolean = currentIndex > 0

        override fun next(): JsonValue {
            if (currentIndex < currentSize || parseUntil(currentIndex + 1)) return evaluatedList[currentIndex++]

            throw IndexOutOfBoundsException()
        }

        override fun nextIndex(): Int = currentIndex

        override fun previous(): JsonValue {
            if (currentIndex <= 0) throw IndexOutOfBoundsException()

            return evaluatedList[--currentIndex]
        }

        override fun previousIndex(): Int = currentIndex - 1

    }


    override fun subList(fromIndex: Int, toIndex: Int): List<JsonValue> {
        if (toIndex > evaluatedList.size && !parseUntil(toIndex)) throw IndexOutOfBoundsException()

        return evaluatedList.subList(fromIndex, toIndex)
    }

    private fun parseAll() {
        @Suppress("ControlFlowWithEmptyBody")
        while (parseNext()) {
        }
    }

    /**
     * @return If the number of elements in this array is less than `until` , returns false
     */
    private fun parseUntil(until: Int): Boolean {
        val n = until - evaluatedList.size
        if (n <= 0) return true
        if (isScannedAll) return false

        for (i in 0 until n) {
            if (!parseNext()) return false
        }

        return true
    }

    /**
     * @return If no element remains, returns false
     */
    private fun parseNext(): Boolean {
        while (currentState.readyForNextValue) {
            if (cursor >= rawCharSeq.length) {
                currentState = ArrayClosed
                return false
            }

            val c = rawCharSeq[cursor++]

            if (c == ',') {
                currentState = ArrayAfterComma
            }

            if (!c.isWhitespace()) fail()
        }

        var nest = 0
        var startAt = -1

        while (++cursor < rawCharSeq.length) {
            val c = rawCharSeq[cursor]
            if (c.isWhitespace()) continue
            if (nest == 0) {
                when (c) {
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-' -> {
                        startAt = cursor
                        if (parseNumber()) {
                            evaluatedList.add(FloatValue(rawCharSeq.slice(startAt, cursor)))
                        } else {
                            evaluatedList.add(IntegerValue(rawCharSeq.slice(startAt, cursor)))
                        }
                        currentState = ArrayValueFinished
                        return true
                    }
                    't' -> {
                        if (rawCharSeq.substring(cursor, cursor + 4) == "true") {
                            evaluatedList.add(BooleanValue(true))
                            cursor += 4
                            currentState = ArrayValueFinished
                            return true
                        }
                        fail()
                    }
                    'f' -> {
                        if (rawCharSeq.substring(cursor, cursor + 5) == "false") {
                            evaluatedList.add(BooleanValue(true))
                            cursor += 5
                            currentState = ArrayValueFinished
                            return true
                        }
                        fail()
                    }
                    'n' -> {
                        if (rawCharSeq.substring(cursor, cursor + 4) == "null") {
                            evaluatedList.add(BooleanValue(true))
                            cursor += 4
                            currentState = ArrayValueFinished
                            return true
                        }
                        fail()
                    }
                }
            }
            when (c) {
                '{' -> {
                    if (nest == 0) {
                        if (!currentState.readyForNextValue) fail()

                        startAt = cursor + 1
                        currentState = ObjectStarted
                    }
                    nest++
                }
                '}' -> {
                    nest--
                    if (nest == 0) {
                        if (currentState != ObjectStarted) fail()

                        evaluatedList.add(ObjectValue(rawCharSeq.slice(startAt, cursor)))
                        currentState = ArrayValueFinished
                        return true
                    }
                }
                '[' -> {
                    if (nest == 0) {
                        if (!currentState.readyForNextValue) fail()

                        startAt = cursor + 1
                        currentState = ArrayStarted
                    }
                    nest++
                }
                ']' -> {
                    nest--
                    if (nest == 0) {
                        if (currentState != ArrayStarted) fail()

                        evaluatedList.add(ArrayValue(rawCharSeq.slice(startAt, cursor)))
                        currentState = ArrayValueFinished
                        return true
                    }
                }
                '"' -> {
                    if (nest == 0) {
                        if (!currentState.readyForNextValue) fail()

                        startAt = cursor + 1
                    }

                    stringLoop@ while (true) {
                        if (++cursor >= rawCharSeq.length) fail()
                        when (rawCharSeq[cursor]) {
                            '"' -> break@stringLoop
                            '\\' -> {
                                cursor++
                            }
                        }
                    }

                    if (nest == 0) {
                        evaluatedList.add(StringValue(rawCharSeq.slice(startAt, cursor)))
                        return true
                    }
                }
            }
        }

        if (nest > 0 || !currentState.readyToClose) fail()

        return false
    }

    private fun parseNumber(): Boolean {
        var isFloat = false
        while (++cursor < rawCharSeq.length) {
            val c = rawCharSeq[cursor]
            if (c.isWhitespace() || c == ',') break
            if (c == '.' || c == 'e') isFloat = true
        }
        return isFloat
    }
}

class ObjectValue(val rawCharSeq: CharSequenceSlice) : JsonValue(), Map<String, JsonValue> {
    override val entries: Set<Map.Entry<String, JsonValue>>
        get() = TODO("Not yet implemented")
    override val keys: Set<String>
        get() = TODO("Not yet implemented")
    override val size: Int
        get() = TODO("Not yet implemented")
    override val values: Collection<JsonValue>
        get() = TODO("Not yet implemented")

    override fun containsKey(key: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsValue(value: JsonValue): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(key: String): JsonValue? {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

}
