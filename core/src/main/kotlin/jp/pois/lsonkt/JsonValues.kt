@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package jp.pois.lsonkt

import jp.pois.liter.literList
import jp.pois.liter.literMap
import jp.pois.lsonkt.parser.ArrayParser
import jp.pois.lsonkt.parser.ObjectParser
import jp.pois.lsonkt.parser.StringParser
import jp.pois.lsonkt.parser.error.ParseErrorHandler
import jp.pois.lsonkt.parser.error.ParseErrorHandlerImpl

sealed class JsonValue

object NullValue : JsonValue()

data class BooleanValue(val value: Boolean) : JsonValue() {
    override fun toString() = value.toString()
}

data class IntegerValue(val rawCharSeq: CharSequence) : JsonValue(), ParseErrorHandler by ParseErrorHandlerImpl() {
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

data class FloatValue(val rawCharSeq: CharSequence) : JsonValue() {
    val value: Double by lazy { parseToDouble() }

    // TODO: Parse the string as a double strictly
    private fun parseToDouble(): Double = rawCharSeq.toString().toDouble()

    override fun toString(): String = value.toString()
}

class StringValue(val rawCharSeq: CharSequence) : JsonValue(), CharSequence {
    private val parser = StringParser(rawCharSeq)

    override val length: Int
        get() = parser.length

    override fun get(index: Int): Char = parser[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = parser.subSequence(startIndex, endIndex)

    override fun toString(): String = parser.toString()
}

class ArrayValue(val rawCharSeq: CharSequence) : JsonValue(),
    List<JsonValue> by ArrayParser(rawCharSeq).literList()

class ObjectValue(val rawCharSeq: CharSequence) : JsonValue(),
    Map<String, JsonValue> by ObjectParser(rawCharSeq).literMap()
