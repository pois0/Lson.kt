@file:Suppress("NOTHING_TO_INLINE")

package jp.pois.lsonkt.util

internal const val JsonWhitespaceSpace = '\u0020'
internal const val JsonWhitespaceLinefeed = '\u000A'
internal const val JsonWhitespaceCarriageReturn = '\u000D'
internal const val JsonWhitespaceHorizontalTab = '\u0009'
private const val JsonWhitespaceMask =
    JsonWhitespaceSpace.toInt() or JsonWhitespaceLinefeed.toInt() or JsonWhitespaceCarriageReturn.toInt() or JsonWhitespaceHorizontalTab.toInt()

internal inline fun Char.isJsonWhitespace(): Boolean {
    return (this.toInt() and JsonWhitespaceMask) != 0
            && (
            this == JsonWhitespaceSpace
                    || this == JsonWhitespaceLinefeed
                    || this == JsonWhitespaceCarriageReturn
                    || this == JsonWhitespaceHorizontalTab)
}

internal inline fun Char.isControlCharacter(): Boolean {
    return this < 0x20.toChar()
}
