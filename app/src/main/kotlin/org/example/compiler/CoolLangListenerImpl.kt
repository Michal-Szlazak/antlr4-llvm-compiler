package org.example.compiler

import CoolLangBaseListener
import CoolLangParser
import org.example.compiler.error.*

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
            return
        }
        llvmBuilder.read(idToken.text)
    }

    override fun exitWriteOperation(ctx: CoolLangParser.WriteOperationContext) {
        when {
            ctx.expression() != null -> {
                llvmBuilder.writeLastCalculated()
            }

            ctx.STRING() != null -> llvmBuilder.writeString(ctx.STRING().text.trim('"'))
        }
    }

    override fun exitExpression(ctx: CoolLangParser.ExpressionContext) {
        when {
            ctx.ID() != null -> if (llvmBuilder.doesVariableExist(ctx.ID().text)) {
                llvmBuilder.loadVariableToStack(ctx.ID().text)
            } else {
                syntaxErrors.add(
                    SyntaxErrorWithLineData(
                        UndeclaredIdentifierError(ctx.ID().text),
                        ctx.ID()
                    )
                )
            }

            ctx.op != null -> try {
                when (ctx.op.text) {
                    "*" -> llvmBuilder.multiply()
                    "/" -> llvmBuilder.divide()
                    "+" -> llvmBuilder.add()
                    "-" -> llvmBuilder.subtract()
                }
            } catch (e: MatchingOperatorNotFoundException) {
                syntaxErrors.add(
                    SyntaxErrorWithLineData(
                        e.error,
                        ctx.op
                    )
                )
            }
        }
    }

    override fun exitAssignment(ctx: CoolLangParser.AssignmentContext) {
        val idNode = ctx.ID() ?: return
        if (!llvmBuilder.doesVariableExist(idNode.text)) {
            syntaxErrors.add(
                SyntaxErrorWithLineData(UndeclaredIdentifierError(idNode.text), idNode)
            )
            return
        }

        llvmBuilder.storeTo(idNode.text)
    }

}