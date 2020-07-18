package jp.pois.lsonkt.source

class StringSlice internal constructor(val original: String, val startIndex: Int, val endIndex: Int) :
    CharSequenceSlice {
    internal constructor(original: String) : this(original, 0, original.length)

    override val length: Int = endIndex - startIndex

    override operator fun get(index: Int): Char =
        if (index in 0 until length) original[startIndex + index] else throw IndexOutOfBoundsException()

    override fun slice(startIndex: Int, endIndex: Int): StringSlice {
        if (startIndex !in 0..endIndex || endIndex > length) {
            throw IndexOutOfBoundsException("length: $length, range: $startIndex until $endIndex")
        }

        return StringSlice(
            original,
            this.startIndex + startIndex,
            this.startIndex + endIndex
        )
    }

    override fun substring(startIndex: Int, endIndex: Int): String {
        if (startIndex !in 0 until endIndex || endIndex > length) {
            throw IndexOutOfBoundsException()
        }

        return original.substring(this.startIndex + startIndex, this.startIndex + endIndex)
    }

    override fun toString(): String = original.substring(startIndex, endIndex)

    override fun equals(other: Any?): Boolean =
        if (other is StringSlice) original == other.original && startIndex == other.startIndex && endIndex == other.endIndex else false

    override fun hashCode(): Int = original.hashCode()
}

fun String.lsonSlice(startIndex: Int, endIndex: Int): StringSlice {
    if (startIndex !in 0 until length || endIndex <= length) {
        throw IndexOutOfBoundsException()
    }

    return StringSlice(this, startIndex, endIndex)
}

fun String.lsonSlice(range: IntRange): StringSlice {
    if (range.first !in 0 until length || range.last < length) {
        throw IndexOutOfBoundsException()
    }

    return StringSlice(this, range.first, range.last + 1)
}
