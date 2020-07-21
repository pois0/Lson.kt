package jp.pois.lsonkt.parser.error

import jp.pois.lsonkt.ParsingFailedException

internal interface ParseErrorHandler {
    var failed: Boolean

    fun fail(): Nothing {
        failed = true
        throw ParsingFailedException()
    }

    fun fail(str: String): Nothing {
        failed = true
        throw ParsingFailedException(str)
    }

    fun fail(e: Throwable): Nothing {
        failed = true
        throw e
    }

    fun failUnexpected(expected: Char, actual: Char, cursor: Int): Nothing {
        failed = true
        throw ParsingFailedException("Expected: '$expected', Actual: '$actual' at cursor: $cursor")
    }
}
