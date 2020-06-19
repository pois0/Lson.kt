package jp.pois.lsonkt.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import jp.pois.lsonkt.IntegerValue
import jp.pois.lsonkt.source.StringSlice
import jp.pois.lsonkt.test.utils.access
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.declaredFunctions

class NumberParseTest : StringSpec({
    val obj = IntegerValue::class.companionObjectInstance

    fun parseToInteger(value: String): Long =
        IntegerValue.Companion::class.declaredFunctions.find { it.name == "parseToInteger" }!!.access.call(
            obj,
            StringSlice(value)
        ) as Long

    val legalStrings =
        listOf("0", "-0", "1", "3", "100", "-100", "1234567890", "-1234567890", "9876543210", "-9876543210")

    legalStrings.forEach {
        it {
            parseToInteger(it) shouldBe it.toLong()
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
            shouldThrow<NumberFormatException> { runCatching { parseToInteger(it) }.exceptionOrNull()?.cause?.let { throw it } }
        }
    }
})
