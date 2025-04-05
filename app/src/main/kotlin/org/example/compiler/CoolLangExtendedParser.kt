package org.example.compiler

import CoolLangLexer
import CoolLangParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RuleContext
import org.antlr.v4.runtime.TokenStream
import org.antlr.v4.runtime.misc.Utils
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.TerminalNode
import org.antlr.v4.runtime.tree.Tree

class CoolLangExtendedParser private constructor(
    tokenStream: TokenStream,
) : CoolLangParser(tokenStream) {

    companion object {
        fun fromStringInput(input: String): CoolLangExtendedParser {
            val lexer = CoolLangLexer(CharStreams.fromString(input))
            val tokenStream = CommonTokenStream(lexer)
            return CoolLangExtendedParser(tokenStream)
        }
    }

    fun toSyntaxTreeString(): String {
        val resultSB = StringBuilder()
        createPrettyStringTree(program(), resultSB, StringBuilder(), true).toString()
        return resultSB.removeSuffix("\n").toString()
    }

    private fun createPrettyStringTree(
        node: Tree,
        resultSB: StringBuilder,
        indentSB: StringBuilder,
        isLast: Boolean
    ) {
        resultSB.append(indentSB, "+- ", getNodeName(node), "\n")
        indentSB.append(if (isLast) "   " else "|  ")

        for (i in 0..<node.childCount) {
            createPrettyStringTree(node.getChild(i), resultSB, StringBuilder(indentSB), i == node.childCount - 1)
        }
    }

    private fun getNodeName(node: Tree): String = when (node) {
        is RuleContext -> ruleNames[node.ruleIndex]
        is ErrorNode -> "Error:${Utils.escapeWhitespace(node.symbol.text, false)}"
        is TerminalNode -> "\"${Utils.escapeWhitespace(node.symbol.text, false)}\""
        else -> throw IllegalArgumentException("Unknown node: $node")
    }
}