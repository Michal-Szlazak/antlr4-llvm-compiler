package org.example.compiler

import CoolLangBaseListener
import CoolLangParser
import org.example.compiler.error.*
import java.util.ArrayDeque

class CoolLangListenerImpl : CoolLangBaseListener() {

    private lateinit var llvmBuilder: LLVMBuilder
    private val syntaxErrors = mutableListOf<SyntaxErrorWithLineData>()
    private val scopes = ArrayDeque<String>()

    private val functions = mutableSetOf<String>()
    var function: String = ""
    var currentScope: String = "main"

    fun getLLVM(): String {

        val llvmBuilderErrors = llvmBuilder.getSyntaxErrors()
        syntaxErrors.addAll(llvmBuilderErrors)

        if (syntaxErrors.isNotEmpty()) {
            throw InvalidSyntaxException(syntaxErrors)
        }

        return llvmBuilder.build()
    }

    override fun exitStruct(ctx: CoolLangParser.StructContext) {

        val variables : MutableList<Pair<String, Type>> = ArrayList()

        for(declarationCtx in ctx.structVariableDeclaration()) {
            val typeNode = declarationCtx.type().ID()

            val idNode = declarationCtx.ID()
            val type = Type.fromTypeName(typeNode?.text)

            variables.add(Pair(idNode.text, type!!))
        }

        llvmBuilder.declareStruct(
            ctx.ID().text,
            variables
        )

    }

    override fun exitStructDeclaration(ctx: CoolLangParser.StructDeclarationContext) {

        val struct = llvmBuilder.getStructDefinition(ctx.structName().ID().text)

        if(struct == null) {
            syntaxErrors.add(SyntaxErrorWithLineData(
                "Struct type ${ctx.structName().ID().text} does not exist",
                ctx.structName().ID().symbol.line,
                ctx.structName().ID().symbol.charPositionInLine
            ))
            return
        } else if(struct.scope != currentScope && struct.scope != "main") {
            syntaxErrors.add(SyntaxErrorWithLineData(
                "Struct type ${ctx.structName().ID().text}, defined in scope ${struct.scope} is outside of scope $currentScope",
                ctx.structName().ID().symbol.line,
                ctx.structName().ID().symbol.charPositionInLine
            ))
            return
        }

        if(llvmBuilder.doesVariableExistInExactScope(ctx.ID().text, currentScope)) {
            syntaxErrors.add(SyntaxErrorWithLineData(
                "redefinition of '${ctx.ID().text}'",
                ctx.structName().ID().symbol.line,
                ctx.structName().ID().symbol.charPositionInLine
            ))
            return
        }

        llvmBuilder.structDeclaration(ctx.structName().ID().text, ctx.ID().text)
    }

    override fun exitStructVariableAssignment(ctx: CoolLangParser.StructVariableAssignmentContext) {

        val structNameIdNode = ctx.structVariableCall().structName().ID()
        val structVariableNameIdNode = ctx.structVariableCall().structVariable().ID()

        val structType = llvmBuilder.getStructTypeFromStructName(
            ctx.structVariableCall().structName().ID().text,
            currentScope
        )

        if(structType == null) {
            syntaxErrors.add(SyntaxErrorWithLineData(
                "Undeclared struct variable ${structNameIdNode.text}",
                structNameIdNode.symbol.line,
                structNameIdNode.symbol.charPositionInLine
            ))

            return
        }

        val struct = llvmBuilder.getStructDefinition(structType)

        if(struct == null) {
            syntaxErrors.add(SyntaxErrorWithLineData(
                "Struct type ${structNameIdNode.text} does not exist",
                structNameIdNode.symbol.line,
                structNameIdNode.symbol.charPositionInLine
            ))
            return
        } else if(struct.scope != currentScope && struct.scope != "main") {
            syntaxErrors.add(SyntaxErrorWithLineData(
                "Struct type ${structNameIdNode.text}, defined in scope ${struct.scope} is outside of scope $currentScope",
                structNameIdNode.symbol.line,
                structNameIdNode.symbol.charPositionInLine
            ))
            return
        }

        var structVariableIndex = llvmBuilder.getIndexOfStructVariable(struct, structVariableNameIdNode.text)

        if(structVariableIndex == null) {
            syntaxErrors.add(SyntaxErrorWithLineData(
                "Struct type ${structNameIdNode.text} does not have variable ${structVariableNameIdNode.text}",
                structVariableNameIdNode.symbol.line,
                structVariableNameIdNode.symbol.charPositionInLine
            ))
            return
        }

        llvmBuilder.structVariableAssignment(
            structNameIdNode.text,
            struct,
            structVariableNameIdNode.text,
            structVariableIndex
        )
    }

    override fun enterProgram(ctx: CoolLangParser.ProgramContext?) {
        llvmBuilder = LLVMBuilder()
        scopes.push("main")
        llvmBuilder.setScope("main")
    }

    override fun exitProgram(ctx: CoolLangParser.ProgramContext) {
        llvmBuilder.closeMain()
    }

    override fun exitFunctionName(ctx: CoolLangParser.FunctionNameContext) {

        if(currentScope != "main") {
            syntaxErrors.add(SyntaxErrorWithLineData(
                "Nested functions are not supported. Cannot define function ${ctx.ID().text} in function $currentScope", ctx.ID().symbol.line, ctx.ID().symbol.charPositionInLine
            ))
            return
        }

        val name = ctx.ID().text
        functions.add(name)
        function = name
        llvmBuilder.functionStart(name)
        currentScope = function
        llvmBuilder.setScope(function)
        scopes.push(function)
    }

    override fun exitFunctionCall(ctx: CoolLangParser.FunctionCallContext) {
        llvmBuilder.functionCall(ctx.ID().text)
    }

    override fun exitFunctionBody(ctx: CoolLangParser.FunctionBodyContext) {

        llvmBuilder.functionEnd()

        scopes.pop()

        if(scopes.isNotEmpty()) {
            currentScope = scopes.first
        } else {
            currentScope = "main"
        }

        llvmBuilder.setScope(currentScope)
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
                llvmBuilder.startLoopVariable(ctx.ID().text)
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

            if (idNode != null && llvmBuilder.doesVariableExistInExactScope(idNode.text, currentScope)) {
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
            currentScope == "main"
        )
    }

    override fun exitReadOperation(ctx: CoolLangParser.ReadOperationContext) {
        val idToken = ctx.ID() ?: return

        if (!llvmBuilder.doesVariableExist(idToken.text, currentScope)) {
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
            ctx.ID() != null -> if (llvmBuilder.doesVariableExist(ctx.ID().text, currentScope)) {

                val type = llvmBuilder.getVariableType(ctx.ID().text)
                if(type == Type.STRUCT) {
                    syntaxErrors.add(
                        SyntaxErrorWithLineData(
                            "Raw struct cannot be used in expression. User struct variable call instead",
                            ctx.ID().symbol.line,
                            ctx.ID().symbol.charPositionInLine
                        )
                    )
                    return
                }

                val variable = llvmBuilder.getVariable(ctx.ID().text)
                if(variable == null) {
                    syntaxErrors.add(
                        SyntaxErrorWithLineData(
                            "undef variable ${ctx.ID().text} in scope $currentScope",
                            ctx.ID().symbol.line,
                            ctx.ID().symbol.charPositionInLine)
                    )
                    return
                }

                llvmBuilder.loadVariableToStack(ctx.ID().text)
            } else {
                syntaxErrors.add(
                    SyntaxErrorWithLineData(
                        UndeclaredIdentifierError(ctx.ID().text),
                        ctx.ID()
                    )
                )
                return
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

            ctx.structVariableCall() != null -> {

                loadStructVariableToStack(ctx.structVariableCall())
            }
        }
    }

    fun loadStructVariableToStack(ctx: CoolLangParser.StructVariableCallContext) {
        val structNameIdNode = ctx.structName().ID()
        val structVariableNameIdNode = ctx.structVariable().ID()

        val structType = llvmBuilder.getStructTypeFromStructName(
            ctx.structName().ID().text,
            currentScope
        )

        if(structType == null) {
            syntaxErrors.add(SyntaxErrorWithLineData(
                "Undeclared struct variable ${structNameIdNode.text}",
                structNameIdNode.symbol.line,
                structNameIdNode.symbol.charPositionInLine
            ))

            return
        }

        val struct = llvmBuilder.getStructDefinition(structType)

        if(struct == null) {
            syntaxErrors.add(SyntaxErrorWithLineData(
                "Struct type ${structNameIdNode.text} does not exist",
                structNameIdNode.symbol.line,
                structNameIdNode.symbol.charPositionInLine
            ))
            return
        } else if(struct.scope != currentScope && struct.scope != "main") {
            syntaxErrors.add(SyntaxErrorWithLineData(
                "Struct type ${structNameIdNode.text}, defined in scope ${struct.scope} is outside of scope $currentScope",
                structNameIdNode.symbol.line,
                structNameIdNode.symbol.charPositionInLine
            ))
            return
        }

        var structVariableIndex = llvmBuilder.getIndexOfStructVariable(struct, structVariableNameIdNode.text)

        if(structVariableIndex == null) {
            syntaxErrors.add(SyntaxErrorWithLineData(
                "Struct type ${structNameIdNode.text} does not have variable ${structVariableNameIdNode.text}",
                structVariableNameIdNode.symbol.line,
                structVariableNameIdNode.symbol.charPositionInLine
            ))
            return
        }

        llvmBuilder.loadStructVariableToStack(
            structNameIdNode.text,
            struct,
            structVariableIndex
        )
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

            ctx.ID() != null -> if (llvmBuilder.doesVariableExist(ctx.ID().text, currentScope)) {
                llvmBuilder.loadVariableToStack(ctx.ID().text)
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

            ctx.structVariableCall() != null -> {
                loadStructVariableToStack(ctx.structVariableCall())
            }
        }

    }

    override fun exitAssignment(ctx: CoolLangParser.AssignmentContext) {
        val idNode = ctx.ID() ?: return

        if (!llvmBuilder.doesVariableExist(idNode.text, currentScope)) {
            syntaxErrors.add(
                SyntaxErrorWithLineData(UndeclaredIdentifierError(idNode.text), idNode)
            )
            return
        }

        val variableType = llvmBuilder.getVariableType(idNode.text)
        if(variableType == Type.STRUCT) {
            syntaxErrors.add(SyntaxErrorWithLineData(
                "Cannot assign variable with type struct. Assign struct variables instead.",
                idNode.symbol.line,
                idNode.symbol.charPositionInLine
            ))
            return
        }

        llvmBuilder.storeTo(idNode.text)
    }

}