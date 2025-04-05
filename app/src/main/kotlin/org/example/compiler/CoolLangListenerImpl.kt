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
        val typeContext = ctx.type()
        val idToken = ctx.ID()

        llvmBuilder.declaration(
            Type.fromTypeName(typeContext.text)!!,
            idToken.text,
        )
    }

    override fun exitReadOperation(ctx: CoolLangParser.ReadOperationContext) {
        val idToken = ctx.ID()!!

        llvmBuilder.read(idToken.text)
    }

    override fun exitWriteOperation(ctx: CoolLangParser.WriteOperationContext) {
        val valueContext = ctx.value()!!

        when {
            valueContext.ID() != null -> llvmBuilder.writeVariable(valueContext.ID().text)
            valueContext.STRING() != null -> llvmBuilder.writeString(valueContext.STRING().text.trim('"'))
        }
    }

}