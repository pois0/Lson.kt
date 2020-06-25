package jp.pois.lsonkt.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import jp.pois.lsonkt.*
import jp.pois.lsonkt.source.StringSlice

class NumberParseTest : StringSpec({
    val legalStrings =
        listOf("0", "-0", "1", "3", "100", "-100", "1234567890", "-1234567890", "9876543210", "-9876543210")

    legalStrings.forEach {
        it {
            IntegerValue(StringSlice(it)).value shouldBe it.toLong()
        }
    }

    val formatExceptionStrings = listOf(
        "00",
        "01",
        "-00",
        "-01",
        Long.MAX_VALUE.toString() + "0",
        "a",
        "theString",
        "+4",
        "--0",
        "0.3",
        "-0.3",
        "1.3",
        "-1.3",
        "0e1",
        "-0e1",
        "0.3e1",
        "-0.3e1"
    )

    formatExceptionStrings.forEach {
        it {
            shouldThrow<NumberFormatException> { IntegerValue(StringSlice(it)).value }
        }
    }
})

class StringParseTest : StringSpec({

    val legalStrings = listOf(
        "abcde" to "abcde",
        "1234567890" to "1234567890"
    )

    legalStrings.forEach { (value, expected) ->
        expected {
            StringValue(StringSlice(value)).value shouldBe expected
        }
    }

    val backQuoteStrings = listOf(
        "\"" to "\"",
        "\\" to "\\",
        "/" to "/",
        "b" to "\b",
        "u000C" to "\u000C",
        "n" to "\n",
        "r" to "\r",
        "t" to "\t"
    )

    backQuoteStrings.forEach { (value, expected) ->
        val str = "\\" + value
        str {
            StringValue(StringSlice(str)).value shouldBe expected
        }
    }

})

class ArrayTest : StringSpec({
    "emptyArray" {
        ArrayValue(StringSlice("")).isEmpty() shouldBe true
    }

    "booleanArray" {
        val original = booleanArrayOf(true, false, true, false)
        val arr = ArrayValue(StringSlice(original.joinToString()))

        for ((i, b) in original.withIndex()) {
            arr[i].asBoolean().value shouldBe b
        }
    }

    "integerArray" {
        val range = 0 until 100
        val arr = ArrayValue(StringSlice(range.joinToString()))

        for (i in range) {
            arr[i].asInteger().value shouldBe i
        }
    }

    "stringArray" {
        val data = arrayOf("" to "", "hello, " to "hello, ", "world!" to "world!", "\\\"" to "\"", "\\\\" to "\\")
        val arr = ArrayValue(StringSlice(data.joinToString { "\"${it.first}\"" }))

        for ((i, str) in data.map { it.second }.withIndex()) {
            arr[i].asString().value shouldBe str
        }
    }

    "arrayInArray" {
        val arr = ArrayValue(StringSlice("[], [[]], [[], [[]]]"))

        arr[0].asArray().isEmpty() shouldBe true
        arr[1].asArray()[0].asArray().isEmpty() shouldBe true
        arr[2].asArray().let {
            it.size shouldBe 2
            it[0].asArray().isEmpty() shouldBe true
            it[1].asArray()[0].asArray().isEmpty() shouldBe true
        }
    }
})
