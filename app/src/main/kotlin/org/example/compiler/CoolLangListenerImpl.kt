package org.example.compiler

import CoolLangBaseListener
import CoolLangParser
import org.example.compiler.error.InvalidSyntaxException
import org.example.compiler.error.RedefinitionError
import org.example.compiler.error.SyntaxErrorWithLineData
import org.example.compiler.error.UndeclaredIdentifierError

class CoolLangListenerImpl : CoolLangBaseListener() {

    private lateinit var llvmBuilder: LLVMBuilder
    private val syntaxErrors = mutableListOf<SyntaxErrorWithLineData>()

    fun getLLVM(): String {
        if (syntaxErrors.isNotEmpty()) {
            throw InvalidSyntaxException(syntaxErrors)
        }
        return llvmBuilder.build()
    }

    override fun enterProgram(ctx: CoolLangParser.ProgramContext?) {
        llvmBuilder = LLVMBuilder()
    }

    override fun exitDeclaration(ctx: CoolLangParser.DeclarationContext) {
        val typeNode = ctx.type().ID()
        val idNode = ctx.ID()

        val type = Type.fromTypeName(typeNode?.text)

        val errors = buildList {
            if (typeNode != null && type == null) {
                add(SyntaxErrorWithLineData(UndeclaredIdentifierError(typeNode.text), typeNode))
            }

            if (idNode != null && llvmBuilder.doesVariableExist(idNode.text)) {
                add(SyntaxErrorWithLineData(RedefinitionError(idNode.text), idNode))
            }
        }

        if (errors.isNotEmpty() || type == null || idNode == null) {
            syntaxErrors.addAll(errors)
            return
        }

        llvmBuilder.declaration(
            type,
            idNode.text,
        )
    }

    override fun exitReadOperation(ctx: CoolLangParser.ReadOperationContext) {
        val idToken = ctx.ID() ?: return

        if (!llvmBuilder.doesVariableExist(idToken.text)) {
            syntaxErrors.add(SyntaxErrorWithLineData(UndeclaredIdentifierError(idToken.text), idToken))
        }
        llvmBuilder.read(idToken.text)
    }

    override fun exitWriteOperation(ctx: CoolLangParser.WriteOperationContext) {
        val valueContext = ctx.value() ?: return

        when {
            valueContext.ID() != null -> if (llvmBuilder.doesVariableExist(valueContext.text)) {
                llvmBuilder.writeVariable(valueContext.ID().text)
            } else {
                syntaxErrors.add(
                    SyntaxErrorWithLineData(
                        UndeclaredIdentifierError(valueContext.text),
                        valueContext.ID()
                    )
                )
            }

            valueContext.STRING() != null -> llvmBuilder.writeString(valueContext.STRING().text.trim('"'))
        }
    }

}