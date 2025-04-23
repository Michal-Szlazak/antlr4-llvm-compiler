package org.example.compiler

import CoolLangBaseListener
import CoolLangParser
import CoolLangParser.IfStatementContext
import org.example.compiler.error.*

class CoolLangListenerImpl : CoolLangBaseListener() {

    private lateinit var llvmBuilder: LLVMBuilder
    private val syntaxErrors = mutableListOf<SyntaxErrorWithLineData>()

    private val functions = mutableSetOf<String>()
    var function: String = ""
    var global: Boolean = true

    fun getLLVM(): String {
        if (syntaxErrors.isNotEmpty()) {
            throw InvalidSyntaxException(syntaxErrors)
        }
        return llvmBuilder.build()
    }

    override fun enterProgram(ctx: CoolLangParser.ProgramContext?) {
        global = true
        llvmBuilder = LLVMBuilder()
    }

    override fun exitProgram(ctx: CoolLangParser.ProgramContext) {
        llvmBuilder.closeMain()
    }

    override fun exitFunctionName(ctx: CoolLangParser.FunctionNameContext) {
        val name = ctx.ID().text
        functions.add(name)
        function = name
        llvmBuilder.functionStart(name)

        global = false
    }

    override fun exitFunctionCall(ctx: CoolLangParser.FunctionCallContext) {
        llvmBuilder.functionCall(ctx.ID().text)
    }

    override fun exitFunctionBody(ctx: CoolLangParser.FunctionBodyContext) {

        llvmBuilder.functionEnd(function)
        global = true
    }

    override fun enterIfBody(ctx: CoolLangParser.IfBodyContext?) {
        llvmBuilder.enterIfBody()
    }

    override fun exitLoopCondition(ctx: CoolLangParser.LoopConditionContext) {

        when {
            ctx.boolExpression() != null -> {
                llvmBuilder.startLoopBoolExpression()
            }

            ctx.ID() != null -> {
                llvmBuilder.startLoopVariable(ctx.ID().text, global)
            }

            ctx.INT() != null -> {
                llvmBuilder.startLoopInt(ctx.INT().text)
            }
        }
    }
    override fun exitLoopBody(ctx: CoolLangParser.LoopBodyContext) {

        llvmBuilder.endLoopInt()
    }

    override fun exitIfBody(ctx: CoolLangParser.IfBodyContext?) {
        llvmBuilder.exitIfBody()
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
            global
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

            ctx.boolExpression() != null -> {
                llvmBuilder.writeLastCalculated()
            }
        }
    }

    override fun exitExpression(ctx: CoolLangParser.ExpressionContext) {
        when {
            ctx.ID() != null -> if (llvmBuilder.doesVariableExist(ctx.ID().text)) {
                llvmBuilder.loadVariableToStack(ctx.ID().text, global)
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

            ctx.REAL() != null -> {
                llvmBuilder.loadRealToStack(ctx.REAL().text)
            }

            ctx.INT() != null -> {
                llvmBuilder.loadIntToStack(ctx.INT().text)
            }
        }
    }

    override fun exitBoolOrExpr(ctx: CoolLangParser.BoolOrExprContext?) {

        if (ctx == null) return
        for (i in 1 until ctx.boolXorExpr().size) {
            llvmBuilder.orBoolean()
        }
    }

    override fun exitBoolXorExpr(ctx: CoolLangParser.BoolXorExprContext?) {
        if (ctx == null) return
        for (i in 1 until ctx.boolAndExpr().size) {
            llvmBuilder.xorBoolean()
        }
    }

    override fun exitBoolAndExpr(ctx: CoolLangParser.BoolAndExprContext?) {
        if (ctx == null) return
        for (i in 1 until ctx.boolNotExpr().size) {
            llvmBuilder.andBoolean()
        }
    }

    override fun exitBoolNotExpr(ctx: CoolLangParser.BoolNotExprContext?) {
        if (ctx == null) return
        if (ctx.getChildCount() == 2) {
            llvmBuilder.negateBoolean()
        }
    }

    override fun exitBoolPrimary(ctx: CoolLangParser.BoolPrimaryContext) {

        when {

            ctx.ID() != null -> if (llvmBuilder.doesVariableExist(ctx.ID().text)) {
                llvmBuilder.loadVariableToStack(ctx.ID().text, global)
            } else {
                syntaxErrors.add(
                    SyntaxErrorWithLineData(
                        UndeclaredIdentifierError(ctx.ID().text),
                        ctx.ID()
                    )
                )
            }

            ctx.boolean_() != null -> {
                llvmBuilder.loadBooleanToStack(ctx.boolean_().text)
            }

            ctx.boolExpression() != null -> {
                //TODO
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

        if(global) {
            llvmBuilder.storeToGlobal(idNode.text)
        } else {
            llvmBuilder.storeTo(idNode.text)
        }
    }

}