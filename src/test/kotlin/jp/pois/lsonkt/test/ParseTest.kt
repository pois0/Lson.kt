package jp.pois.lsonkt.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import jp.pois.lsonkt.*
import jp.pois.lsonkt.source.StringSlice

class NumberParseTest : StringSpec({
    val legalStrings =
        listOf("0", "-0", "1", "3", "100", "-100", "1234567890", "-1234567890", "9876543210", "-9876543210")

    "legalInteger" {
        legalStrings.forEach {
            IntegerValue(StringSlice(it)).value shouldBe it.toLong()
        }
    }

    "parse(legalInteger)" {
        legalStrings.forEach {
            parse(it).isInteger() shouldBe true
            parse(it).asInteger().value shouldBe it.toLong()
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

    "formatExceptionStrings" {
        formatExceptionStrings.forEach {
            shouldThrow<NumberFormatException> { IntegerValue(StringSlice(it)).value }
        }
    }
})

class StringParseTest : StringSpec({

    val legalStrings = listOf(
        "abcde" to "abcde",
        "1234567890" to "1234567890"
    )

    "legalStrings" {
        legalStrings.forEach { (value, expected) ->
            StringValue(StringSlice(value)).toString() shouldBe expected
        }
    }

    "parse(legalStrings)" {
        legalStrings.forEach { (value, expected) ->
            val str = parse("\"$value\"")
            str.shouldBeInstanceOf<StringValue>()
            str.asString().toString() shouldBe expected
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

    "backQuoteStrings" {
        backQuoteStrings.forEach { (value, expected) ->
            StringValue(StringSlice("\\" + value)).toString() shouldBe expected
        }
    }

    "parse(backQuoteStrings)" {
        backQuoteStrings.forEach { (value, expected) ->
            val str = parse("\"\\$value\"")
            str.shouldBeInstanceOf<StringValue>()
            str.asString().toString() shouldBe expected
        }
    }
})

class ArrayTest : StringSpec({
    "emptyArray" {
        ArrayValue(StringSlice("")).shouldBeEmpty()
    }

    "parse(emptyArray)" {
        val arr = parse("[]")
        arr.shouldBeInstanceOf<ArrayValue>()
        arr.asArray().shouldBeEmpty()
    }

    val booleanArray = booleanArrayOf(true, false, true, false)

    "booleanArray" {
        val arr = ArrayValue(StringSlice(booleanArray.joinToString()))

        for ((i, b) in booleanArray.withIndex()) {
            arr[i].asBoolean().value shouldBe b
        }
    }

    "parse(booleanArray)" {
        val arr = parse(booleanArray.joinToString(prefix = "[", postfix = "]"))
        arr.shouldBeInstanceOf<ArrayValue>()
        for ((i, b) in booleanArray.withIndex()) {
            arr[i].asBoolean().value shouldBe b
        }
    }

    val intArray = 0 until 100

    "integerArray" {
        val arr = ArrayValue(StringSlice(intArray.joinToString()))

        for (i in intArray) {
            arr[i].asInteger().value shouldBe i
        }
    }

    "parse(integerArray)" {
        val arr = parse(intArray.joinToString(prefix = "[", postfix = "]"))
        arr.shouldBeInstanceOf<ArrayValue>()

        for (i in intArray) {
            arr[i].asInteger().value shouldBe i
        }
    }

    val stringArray = arrayOf("" to "", "hello, " to "hello, ", "world!" to "world!", "\\\"" to "\"", "\\\\" to "\\")

    "stringArray" {
        val arr = ArrayValue(StringSlice(stringArray.joinToString { "\"${it.first}\"" }))

        for ((i, str) in stringArray.map { it.second }.withIndex()) {
            arr[i].asString().toString() shouldBe str
        }
    }

    "parse(stringArray)" {
        val arr = parse(stringArray.joinToString(prefix = "[", postfix = "]") { "\"${it.first}\"" })
        arr.shouldBeInstanceOf<ArrayValue>()

        for ((i, str) in stringArray.map { it.second }.withIndex()) {
            arr[i].asString().toString() shouldBe str
        }
    }

    val arrayOfArray = "[], [[]], [[], [[]]]"

    "arrayInArray" {
        val arr = ArrayValue(StringSlice(arrayOfArray))
        arr[0].asArray().isEmpty() shouldBe true
        arr[1][0].asArray().isEmpty() shouldBe true
        arr[2].asArray().let {
            it.size shouldBe 2
            it[0].asArray().isEmpty() shouldBe true
            it[1][0].asArray().isEmpty() shouldBe true
        }
    }

    "parse(arrayInArray)" {
        val arr = parse("[$arrayOfArray]")
        arr.shouldBeInstanceOf<ArrayValue>()

        arr.asArray()[0].asArray().isEmpty() shouldBe true
        arr.asArray()[1].asArray()[0].asArray().isEmpty() shouldBe true
        arr.asArray()[2].asArray().let {
            it.size shouldBe 2
            it[0].asArray().isEmpty() shouldBe true
            it[1].asArray()[0].asArray().isEmpty() shouldBe true
        }
    }
})

class ObjectTest : StringSpec({
    "emptyObject" {
        val obj = ObjectValue(StringSlice(""))
        obj.shouldBeEmpty()
    }

    "parse(emptyObject)" {
        val obj = parse("{}")
        obj.shouldBeInstanceOf<ObjectValue>()
        obj.asObject().isEmpty()
    }

    val simpleObject = """
        "key1": null,
        "key2": true,
        "key3": false,
        "key4": 320,
        "key5": "Keys to Ascension",
        "key6": [],
        "key7": {}
    """

    "simpleObject" {
        val obj = ObjectValue(StringSlice(simpleObject))
        obj["key1"].shouldBeInstanceOf<NullValue>()
        obj["key2"].asBoolean().value shouldBe true
        obj["key3"].asBoolean().value shouldBe false
        obj["key4"].asInteger().value shouldBe 320
        obj["key5"].asString().toString() shouldBe "Keys to Ascension"
        obj["key6"].asArray().shouldBeEmpty()
        obj["key7"].asObject().shouldBeEmpty()
    }

    "parse(simpleObject)" {
        val _obj = parse("{$simpleObject}")
        _obj.shouldBeInstanceOf<ObjectValue>()
        val obj = _obj.asObject()
        obj["key1"].shouldBeInstanceOf<NullValue>()
        obj["key2"].asBoolean().value shouldBe true
        obj["key3"].asBoolean().value shouldBe false
        obj["key4"].asInteger().value shouldBe 320
        obj["key5"].asString().toString() shouldBe "Keys to Ascension"
        obj["key6"].asArray().shouldBeEmpty()
        obj["key7"].asObject().shouldBeEmpty()
    }

    val backslashKey = """
        "\\": null,
        "\n\t": false
    """

    "backslashKey" {
        val obj = ObjectValue(StringSlice(backslashKey))
        obj["\\"].shouldBeInstanceOf<NullValue>()
        obj["\n\t"].asBoolean().value shouldBe false
    }

    "parse(backslashKey)" {
        val _obj = parse("{$backslashKey}")
        _obj.shouldBeInstanceOf<ObjectValue>()
        val obj = _obj.asObject()
        obj["\\"].shouldBeInstanceOf<NullValue>()
        obj["\n\t"].asBoolean().value shouldBe false
    }

    val objectInObject = """
        "object": {
            "object": {},
            "{}": {}
        },
        "second": {}
    """.trimIndent()

    "objectInObject" {
        val obj = ObjectValue(StringSlice(objectInObject))
        obj["object"]["object"].asObject().shouldBeEmpty()
        obj["object"]["{}"].asObject().shouldBeEmpty()
        obj["second"].asObject().shouldBeEmpty()
    }
})
