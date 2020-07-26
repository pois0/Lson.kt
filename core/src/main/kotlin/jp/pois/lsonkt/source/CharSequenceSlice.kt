package jp.pois.lsonkt.source

class CharSequenceSlice internal constructor(val original: CharSequence, val startIndex: Int, val endIndex: Int) :
    CharSequence {
    override val length: Int
        get() = endIndex - startIndex

    override fun get(index: Int): Char =
        if (index in 0 until length) original[startIndex + index] else throw IndexOutOfBoundsException()

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequenceSlice {
        if (startIndex !in 0 until endIndex || endIndex <= length) {
            throw IndexOutOfBoundsException()
        }

        return CharSequenceSlice(
            original,
            this.startIndex + startIndex,
            this.startIndex + endIndex
        )
    }

    override fun toString(): String = original.substring(startIndex, endIndex)

    override fun equals(other: Any?): Boolean =
        if (other is CharSequenceSlice) original == other.original && startIndex == other.startIndex && endIndex == other.endIndex else false

    override fun hashCode(): Int = original.hashCode()
}
