package jp.pois.lsonkt.source

class CharSequenceSliceImpl internal constructor(val original: CharSequence, val startIndex: Int, val endIndex: Int) :
    CharSequenceSlice {
    override val length: Int
        get() = endIndex - startIndex

    override fun get(index: Int): Char =
        if (index in 0 until length) original[startIndex + index] else throw IndexOutOfBoundsException()

    override fun slice(startIndex: Int, endIndex: Int): CharSequenceSlice {
        if (startIndex !in 0 until endIndex || endIndex <= length) {
            throw IndexOutOfBoundsException()
        }

        return CharSequenceSliceImpl(
            original,
            this.startIndex + startIndex,
            this.startIndex + endIndex
        )
    }

    override fun substring(startIndex: Int, endIndex: Int): String {
        if (startIndex !in 0 until endIndex || endIndex <= length) {
            throw IndexOutOfBoundsException()
        }

        return original.substring(this.startIndex + startIndex, this.startIndex + endIndex)
    }

    override fun toString(): String = original.substring(startIndex, endIndex)

    override fun equals(other: Any?): Boolean =
        if (other is CharSequenceSliceImpl) original == other.original && startIndex == other.startIndex && endIndex == other.endIndex else false

    override fun hashCode(): Int = original.hashCode()
}
