package jp.pois.lsonkt.source

interface CharSequenceSlice {
    val length: Int

    operator fun get(index: Int): Char

    fun slice(startIndex: Int, endIndex: Int): CharSequenceSlice

    fun substring(startIndex: Int, endIndex: Int): String
}
