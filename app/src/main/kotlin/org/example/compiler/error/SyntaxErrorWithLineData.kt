package org.example.compiler.error

import org.antlr.v4.runtime.tree.TerminalNode

class SyntaxErrorWithLineData(
    syntaxError: SyntaxError,
    terminalNode: TerminalNode
) {
    private val message: String = syntaxError.message
    private val lineNumber: Int = terminalNode.symbol.line
    private val characterPosition: Int = terminalNode.symbol.charPositionInLine

    override fun toString(): String {
        return "line $lineNumber:$characterPosition $message"
    }
}