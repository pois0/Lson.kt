package jp.pois.lsonkt.parser.error

class ParsingFailedException : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
}
