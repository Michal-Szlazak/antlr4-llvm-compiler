package org.example.compiler.error

import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.TerminalNode

class SyntaxErrorWithLineData(
    private val message: String,
    private val lineNumber: Int,
    private val characterPosition: Int,
) {
    constructor(syntaxError: SyntaxError, terminalNode: TerminalNode) : this(
        message = syntaxError.message,
        lineNumber = terminalNode.symbol.line,
        characterPosition = terminalNode.symbol.charPositionInLine
    )

    constructor(syntaxError: SyntaxError, token: Token) : this(
        message = syntaxError.message,
        lineNumber = token.line,
        characterPosition = token.charPositionInLine
    )

    override fun toString(): String {
        return "line $lineNumber:$characterPosition $message"
    }
}