package org.example.compiler

import CoolLangBaseListener
import CoolLangParser

class CoolLangListenerImpl : CoolLangBaseListener() {

    private lateinit var llvmBuilder: LLVMBuilder

    fun getLLVM(): String {
        return llvmBuilder.build()
    }

    override fun enterProgram(ctx: CoolLangParser.ProgramContext?) {
        llvmBuilder = LLVMBuilder()
    }

    override fun exitDeclaration(ctx: CoolLangParser.DeclarationContext) {
        val typeNode = ctx.type().ID() ?: return
        val idNode = ctx.ID() ?: return

        llvmBuilder.declaration(
            typeNode.text,
            idNode.text,
        )
    }

    override fun exitReadOperation(ctx: CoolLangParser.ReadOperationContext) {
        val idToken = ctx.ID() ?: return

        llvmBuilder.read(idToken.text)
    }

    override fun exitWriteOperation(ctx: CoolLangParser.WriteOperationContext) {
        val valueContext = ctx.value() ?: return

        when {
            valueContext.ID() != null -> llvmBuilder.writeVariable(valueContext.ID().text)
            valueContext.STRING() != null -> llvmBuilder.writeString(valueContext.STRING().text.trim('"'))
        }
    }

}