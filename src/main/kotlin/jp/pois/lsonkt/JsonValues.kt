@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package jp.pois.lsonkt

import jp.pois.lsonkt.source.CharSequenceSlice

sealed class JsonValue {
    protected var failed = false

    @Suppress("NOTHING_TO_INLINE")
    protected inline fun fail(): Nothing {
        failed = true
        throw ParsingFailedException()
    }

    @Suppress("NOTHING_TO_INLINE")
    protected inline fun fail(e: Throwable): Nothing {
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
                        if (cursor + 5 > rawCharSeq.length) throw IndexOutOfBoundsException()

                        rawCharSeq.substring(cursor + 1, cursor + 5).toInt(16).toChar().also {
                            cursor += 4
                        }
                    }
                    else -> fail()
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

    private val scannedList = ArrayList<JsonValue>()

    private var currentState = ArrayStarted

    override val size: Int
        get() {
            parseAll()
            return scannedList.size
        }

    val scannedSize: Int
        get() = scannedList.size

    private val isScannedAll
        get() = currentState == ArrayClosed

    override fun contains(element: JsonValue): Boolean = indexOf(element) >= 0

    override fun containsAll(elements: Collection<JsonValue>): Boolean = elements.all { contains(it) }

    override fun get(index: Int): JsonValue {
        return if (index < scannedList.size) {
            scannedList[index]
        } else {
            if (!parseUntil(index + 1)) throw ParsingFailedException()
            return scannedList.last()
        }
    }

    override fun indexOf(element: JsonValue): Int {
        val check = scannedList.indexOf(element)
        if (check >= 0) return check

        if (!isScannedAll) {
            while (parseNext()) {
                if (scannedList.last() == element) {
                    return scannedList.lastIndex
                }
            }
        }

        return -1
    }

    override fun isEmpty(): Boolean {
        if (cursor != rawCharSeq.length) parseAll()
        return scannedList.isEmpty()
    }

    override fun iterator(): Iterator<JsonValue> = object : Iterator<JsonValue> {
        private var index = 0

        override fun hasNext(): Boolean {
            if (index < scannedSize) return true

            return parseUntil(index + 1)
        }

        override fun next(): JsonValue {
            if (index < scannedSize || parseUntil(index + 1)) return scannedList[index++]

            throw IndexOutOfBoundsException()
        }
    }

    override fun lastIndexOf(element: JsonValue): Int {
        var check = scannedList.lastIndexOf(element)

        while (parseNext()) {
            if (scannedList.last() == element) {
                check = scannedList.lastIndex
            }
        }

        return check
    }

    override fun listIterator(): ListIterator<JsonValue> = listIterator(0)

    override fun listIterator(index: Int): ListIterator<JsonValue> = object : ListIterator<JsonValue> {
        private var currentIndex = index

        init {
            if (currentIndex > scannedSize) {
                if (isScannedAll) {
                    throw IndexOutOfBoundsException()
                }

                if (!parseUntil(currentIndex + 1)) {
                    throw IndexOutOfBoundsException()
                }
            }
        }

        override fun hasNext(): Boolean {
            if (currentIndex < scannedSize) return true

            return parseUntil(currentIndex + 1)
        }

        override fun hasPrevious(): Boolean = currentIndex > 0

        override fun next(): JsonValue {
            if (currentIndex < scannedSize || parseUntil(currentIndex + 1)) return scannedList[currentIndex++]

            throw IndexOutOfBoundsException()
        }

        override fun nextIndex(): Int = currentIndex

        override fun previous(): JsonValue {
            if (currentIndex <= 0) throw IndexOutOfBoundsException()

            return scannedList[--currentIndex]
        }

        override fun previousIndex(): Int = currentIndex - 1

    }


    override fun subList(fromIndex: Int, toIndex: Int): List<JsonValue> {
        if (toIndex > scannedList.size && !parseUntil(toIndex)) throw IndexOutOfBoundsException()

        return scannedList.subList(fromIndex, toIndex)
    }

    fun parseAll() {
        @Suppress("ControlFlowWithEmptyBody")
        while (parseNext()) {
        }
    }

    /**
     * @return If the number of elements in this array is less than `until` , returns false
     */
    fun parseUntil(until: Int): Boolean {
        val n = until - scannedList.size
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
    fun parseNext(): Boolean {
        var cursor = cursor
        while (!currentState.readyForNextValue) {
            if (cursor >= rawCharSeq.length) {
                currentState = ArrayClosed
                return false
            }

            val c = rawCharSeq[cursor++]

            if (c == ',') {
                currentState = ArrayAfterComma
            } else if (!c.isWhitespace()) {
                println(rawCharSeq)
                println(cursor - 1)
                println(c)
                fail()
            }
        }

        cursor--

        var nest = 0
        var startAt = -1

        val value: JsonValue

        loop@ while (true) {
            if (++cursor >= rawCharSeq.length) {
                if (nest > 0 || !currentState.readyToClose) fail()
                else return false
            }
            val c = rawCharSeq[cursor]
            if (c.isWhitespace()) continue
            if (nest == 0) {
                when (c) {
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-' -> {
                        startAt = cursor

                        this.cursor = cursor
                        val cond = classifyNumber()
                        cursor = this.cursor

                        value = if (cond) {
                            FloatValue(rawCharSeq.slice(startAt, cursor))
                        } else {
                            IntegerValue(rawCharSeq.slice(startAt, cursor))
                        }

                        break@loop
                    }
                    't' -> {
                        if (rawCharSeq.substring(cursor, cursor + 4) == "true") {
                            value = BooleanValue(true)
                            cursor += 4
                            break@loop
                        }
                        fail()
                    }
                    'f' -> {
                        if (rawCharSeq.substring(cursor, cursor + 5) == "false") {
                            value = BooleanValue(false)
                            cursor += 5
                            break@loop
                        }
                        fail()
                    }
                    'n' -> {
                        if (rawCharSeq.substring(cursor, cursor + 4) == "null") {
                            value = BooleanValue(true)
                            cursor += 4
                            break@loop
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

                        value = ObjectValue(rawCharSeq.slice(startAt, cursor++))
                        break@loop
                    } else if (nest < 0) fail()
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

                        value = ArrayValue(rawCharSeq.slice(startAt, cursor++))
                        break@loop
                    } else if (nest < 0) fail()
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
                        value = StringValue(rawCharSeq.slice(startAt, cursor++))
                        break@loop
                    }
                }
            }
        }

        scannedList.add(value)
        currentState = ArrayValueFinished
        this.cursor = cursor

        return true
    }

    private fun classifyNumber(): Boolean {
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
    private var cursor: Int = 0

    private var scannedMap = HashMap<String, JsonValue>()

    private var currentState = ObjectStarted

    private val isScannedAll
        get() = currentState == ObjectClosed

    override val entries: Set<Map.Entry<String, JsonValue>>
        get() {
            if (!isScannedAll) parseAll()
            return scannedMap.entries
        }

    val scannedEntries: Set<Map.Entry<String, JsonValue>>
        get() = scannedMap.entries

    override val keys: Set<String>
        get() {
            if (!isScannedAll) parseAll()
            return scannedMap.keys
        }

    val scannedKeys: Set<String>
        get() = scannedMap.keys

    override val size: Int
        get() {
            if (!isScannedAll) parseAll()
            return scannedMap.size
        }

    val scannedSize: Int
        get() = scannedMap.size

    override val values: Collection<JsonValue>
        get() {
            if (!isScannedAll) parseAll()
            return scannedMap.values
        }

    val scannedValues: Collection<JsonValue>
        get() = scannedMap.values

    override fun containsKey(key: String): Boolean {
        if (scannedMap.containsKey(key)) return true
        if (isScannedAll) return false

        var e = parseNext()
        while (e != null) {
            val (k, _) = e
            if (k == key) return true
            e = parseNext()
        }

        return false
    }

    override fun containsValue(value: JsonValue): Boolean {
        if (scannedMap.containsValue(value)) return true
        if (isScannedAll) return false

        var e = parseNext()
        while (e != null) {
            val (_, v) = e
            if (v == value) return true
            e = parseNext()
        }

        return false
    }

    override fun get(key: String): JsonValue? {
        val value = scannedMap[key]
        if (value != null || isScannedAll) return value

        var e = parseNext()
        while (e != null) {
            val (k, v) = e
            if (k == key) return v
            e = parseNext()
        }

        return null
    }

    override fun isEmpty(): Boolean {
        if (!isScannedAll) parseAll()

        return scannedMap.isEmpty()
    }

    fun parseAll() {
        @Suppress("ControlFlowWithEmptyBody")
        while (parseNext() != null) {
        }
    }

    fun parseNext(): Pair<String, JsonValue>? {
        var cursor = cursor

        while (!currentState.readyForNextValue) {
            if (cursor >= rawCharSeq.length) {
                currentState = ObjectClosed
                return null
            }

            val c = rawCharSeq[cursor++]

            if (c == ',') {
                currentState = ObjectAfterComma
            } else if (!c.isWhitespace()) fail()
        }

        var nest = 0
        var startAt = -1

        val key: String

        cursor--

        while (true) {
            if (++cursor >= rawCharSeq.length) {
                if (currentState.readyToClose) return null else fail()
            }

            val c = rawCharSeq[cursor]
            if (c.isWhitespace()) continue
            if (c == '"') {
                this.cursor = cursor
                key = parseKey()
                cursor = this.cursor
                break
            }
        }

        val value: JsonValue

        loop@ while (true) {
            if (++cursor >= rawCharSeq.length) fail()
            val c = rawCharSeq[cursor]
            if (c.isWhitespace()) continue
            if (nest == 0) {
                when (c) {
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-' -> {
                        startAt = cursor

                        this.cursor = cursor
                        val cond = classifyNumber()
                        cursor = this.cursor

                        value = if (cond) {
                            FloatValue(rawCharSeq.slice(startAt, cursor))
                        } else {
                            IntegerValue(rawCharSeq.slice(startAt, cursor))
                        }
                        currentState = ObjectEntryFinished
                        break@loop
                    }
                    't' -> {
                        if (rawCharSeq.substring(cursor, cursor + 4) == "true") {
                            value = BooleanValue(true)
                            cursor += 4
                            currentState = ArrayValueFinished
                            break@loop
                        }
                        fail()
                    }
                    'f' -> {
                        if (rawCharSeq.substring(cursor, cursor + 5) == "false") {
                            value = BooleanValue(true)
                            cursor += 5
                            currentState = ArrayValueFinished
                            break@loop
                        }
                        fail()
                    }
                    'n' -> {
                        if (rawCharSeq.substring(cursor, cursor + 4) == "null") {
                            value = BooleanValue(true)
                            cursor += 4
                            currentState = ArrayValueFinished
                            break@loop
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

                        value = ObjectValue(rawCharSeq.slice(startAt, cursor++))
                        currentState = ArrayValueFinished
                        break@loop
                    } else if (nest < 0) fail()
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

                        value = ArrayValue(rawCharSeq.slice(startAt, cursor++))
                        currentState = ArrayValueFinished
                        break@loop
                    } else if (nest < 0) fail()
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
                        value = StringValue(rawCharSeq.slice(startAt, cursor++))
                        break@loop
                    }
                }
            }
        }

        scannedMap[key] = value
        this.cursor = cursor
        return key to value
    }


    private fun parseKey() = buildString {
        var cursor = cursor
        loop@ while (++cursor < rawCharSeq.length) {
            val c = rawCharSeq[cursor]
            when (c) {
                '"' -> {
                    currentState = ObjectKeyFinished
                    break@loop
                }
                '\\' -> {
                    append(when (rawCharSeq[++cursor]) {
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

                            rawCharSeq.substring(cursor + 1, cursor + 5).toInt(16).toChar().also {
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

    private fun classifyNumber(): Boolean {
        var cursor = cursor

        var isFloat = false
        while (++cursor < rawCharSeq.length) {
            val c = rawCharSeq[cursor]
            if (c.isWhitespace() || c == ',') break
            if (c == '.' || c == 'e') isFloat = true
        }

        this.cursor = cursor
        return isFloat
    }
}
