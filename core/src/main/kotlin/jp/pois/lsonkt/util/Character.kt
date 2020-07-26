@file:Suppress("NOTHING_TO_INLINE")

package jp.pois.lsonkt.util

internal const val JsonWhitespaceSpace = '\u0020'
internal const val JsonWhitespaceLinefeed = '\u000A'
internal const val JsonWhitespaceCarriageReturn = '\u000D'
internal const val JsonWhitespaceHorizontalTab = '\u0009'

internal inline fun Char.isJsonWhitespace(): Boolean {
    return this < JsonWhitespaceSpace + 1
            && (
            this == JsonWhitespaceSpace
                    || this == JsonWhitespaceLinefeed
                    || this == JsonWhitespaceCarriageReturn
                    || this == JsonWhitespaceHorizontalTab)
}

internal inline fun Char.isControlCharacter(): Boolean {
    isWhitespace()
    return this < 0x20.toChar()
}
