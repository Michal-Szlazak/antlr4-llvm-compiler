package org.example.compiler

import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.example.compiler.error.InvalidSyntaxException

class CoolLangToLLVMTranspiler {

    fun transpile(coolLangCode: String): String {
        val parser = CoolLangExtendedParser.fromStringInput(coolLangCode)
        val programContext = parser.program()

        val listener = CoolLangListenerImpl()
        val walker = ParseTreeWalker()
        walker.walk(listener, programContext)

        val llvm = listener.getLLVM()
        // TODO: Refactor when storing antlr4 instead of printing them directly
        if(parser.numberOfSyntaxErrors > 0) {
            throw InvalidSyntaxException(emptyList())
        }

        return llvm
    }
}