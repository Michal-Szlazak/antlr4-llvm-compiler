package org.example.compiler

import org.antlr.v4.runtime.tree.TerminalNode
import org.example.compiler.error.MatchingOperatorNotFoundError
import org.example.compiler.error.MatchingOperatorNotFoundException
import org.example.compiler.error.SyntaxErrorWithLineData
import java.lang.Exception


enum class Type(val typeName: String, val llvm: String, val size: Int, val default: String) {
    I32("i32", "i32", 4, "0"),
    I64("i64", "i64", 8, "0.0"),
    F32("f32", "float", 4, "0.0"),
    F64("f64", "double", 8, "0.0"),
    BOOL("bool", "i1", 1, "false"),
    STRUCT("struct", "", 0, "struct");

    companion object {
        fun fromTypeName(typeName: String?): Type? {
            return entries.firstOrNull { it.typeName == typeName }
        }
    }

    fun isFloat(): Boolean {
        return this in listOf(F32, F64)
    }

    fun isInt(): Boolean {
        return this in listOf(I32, I64)
    }

    fun isBoolean(): Boolean {
        return this in listOf(BOOL)
    }
}

enum class Operation(val declaration: String) {
    READ("declare i32 @scanf(ptr noundef, ...)"),
    WRITE("declare i32 @printf(ptr noundef, ...)"),
}

abstract class StackValue(val type: Type) {
    open fun toValueString(): String {
        error("should be overridden")
    }
}

class Variable(val id: Int, type: Type, val scope: String) : StackValue(type) {

    override fun toValueString(): String {
        return "%${id}"
    }
}

class Constant(type: Type, val value: String) : StackValue(type) {
    override fun toValueString(): String {
        return value
    }
}

class Struct(val name: String, val scope: String, val variables: List<Pair<String, Type>>)
data class StringRepresentation(val id: Int)

class LLVMBuilder {
    private val MAIN_SCOPE = "main"
    private val statementsMain = mutableListOf<String>()
    private val statements = mutableListOf<String>()
    private val operations = mutableSetOf<Operation>()
    private val headers = mutableListOf<String>()
    private val syntaxErrors = mutableListOf<SyntaxErrorWithLineData>()

    private val structs = mutableMapOf<String, Struct>()
    private val variableNameToStruct = mutableMapOf<Pair<String, String>, String>()

    private val constantStringToStringRepresentation = mutableMapOf<String, StringRepresentation>()
    private var currentConstantStringId = 1

    private val variableNameToVariable = mutableMapOf<Pair<String, String>, Variable>()
    private var currentInstructionId = 1
    private var mainCurrentInstructionId = 1


    private val tempVariableStack = ArrayDeque<StackValue>()
    private val brStack = ArrayDeque<Int>()
    private var currentBr = 0

    private var currentScope = "main"

    private fun <T> ArrayDeque<T>.push(element: T) = addLast(element)
    private fun <T> ArrayDeque<T>.pop() = removeLastOrNull()

    fun setScope(scope: String) {
        currentScope = scope
    }

    fun getSyntaxErrors(): MutableList<SyntaxErrorWithLineData> {
        return syntaxErrors
    }

    fun declareStruct(name: String, variables: MutableList<Pair<String, Type>> ) {
        var newHeader = "%$name = type {"

        for(variable in variables) {
            newHeader += " ${variable.second.llvm},"
        }

        newHeader = newHeader.substring(0, newHeader.length - 1) //delete last comma
        newHeader += " } ;"

        headers += newHeader

        structs[name] = Struct(name, currentScope, variables)
    }

    fun getStructDefinition(structType: String): Struct? {

        return structs[structType]
    }

    fun structVariableExists(structName: String): Boolean {
        return structs[structName] != null
    }

    fun structDeclaration(structType: String, structName: String) {

        //TODO delete - as the validation is in listenerImpl
//        val struct = structs[structType]

//        if(struct == null) {
//
//            syntaxErrors.add(
//                SyntaxErrorWithLineData(
//                "Undefined struct $structType", -1, -1
//            ))
//
//            return
//        }
//
//        if(struct.scope != currentScope && struct.scope != "main") {
//            syntaxErrors.add(SyntaxErrorWithLineData(
//                "Struct $structType is outside scope", -1, -1
//            ))
//
//            return
//        }

        val instructionId: Int
        if(currentScope == "main") {
            instructionId = emitGlobalStructure(structType)
        } else {
            instructionId = emitInstruction("alloca %$structType")
        }

        variableNameToVariable[Pair(structName, currentScope)] = Variable(
            instructionId,
            Type.STRUCT,
            currentScope
        )
        variableNameToStruct[Pair(structName, currentScope)] = structType
    }

    fun getIndexOfStructVariable(struct: Struct, variableName: String): Int? {

        var i = 0

        for(structVariable in struct.variables) {

            if(structVariable.first == variableName) {
                break
            }
            i++
        }

        if(i == struct.variables.size) {
            return null
        }
        return i
    }

    fun structVariableAssignment(structName: String, struct: Struct, variableName: String, structVariableId: Int): LLVMBuilder {

//        val structType = getStructTypeFromStructName(structName, currentScope)
//        if(structType == null) {
//            syntaxErrors.add(SyntaxErrorWithLineData(
//                "Undeclared struct variable $structName", -1, -1
//            ))
//
//            return this
//        }

//        val struct = structs[structType]
//
//        if(struct == null) {
//            syntaxErrors.add(SyntaxErrorWithLineData(
//                "Undefined struct $structType", -1, -1
//            ))
//
//            return this
//        }


//
//        var i = 0
//
//        for(structVariable in struct!!.variables) {
//
//            if(structVariable.first == variableName) {
//                break
//            }
//            i++
//        }
//
//        if(i == struct.variables.size) {
//            syntaxErrors.add(SyntaxErrorWithLineData(
//                "Undefined variable $variableName in struct $structName", -1, -1
//            ))
//        }

        val variable = getVariableFromVariableName(structName, currentScope)
        val structVariableType = struct.variables.get(structVariableId).second

        var instructionId: Int

        if(struct.scope == "main") {
            instructionId = emitInstruction("getelementptr %${struct.name}, %${struct.name}* @.global.${variable!!.id}, i32 0, i32 $structVariableId ;")
        } else {
            instructionId = emitInstruction("getelementptr %${struct.name}, %${struct.name}* %${variable!!.id}, i32 0, i32 $structVariableId ;")
        }

        val expressionResult = tempVariableStack.pop()?.let {
            if (it.type != structVariableType) {
                castVariableToType(it, structVariableType)
            } else {
                it
            }
        } ?: return this

        emitVoidInstruction("store ${expressionResult.type.llvm} ${expressionResult.toValueString()}, ${structVariableType.llvm}* %$instructionId")

        return this
    }

    fun declaration(type: Type, name: String, global: Boolean): LLVMBuilder {

        if(global) {
            val instructionId = emitGlobalVariable(type)
            variableNameToVariable[Pair(name, MAIN_SCOPE)] = Variable(
                id = instructionId,
                type = type,
                currentScope
            )

        } else {
            val instructionId = emitInstruction("alloca ${type.llvm}, align ${type.size}")
            variableNameToVariable[Pair(name, currentScope)] = Variable(
                id = instructionId,
                type = type,
                currentScope
            )

        }

        return this
    }

    fun emitGlobalVariable(type: Type): Int {
        headers += "@.global.$currentInstructionId = global ${type.llvm} ${type.default}"
        return currentInstructionId++
    }

    fun emitGlobalStructure(structType: String): Int {
        headers += "@.global.$currentInstructionId = global %$structType zeroinitializer"
        return currentInstructionId++
    }

    fun functionStart(funName: String) {

        statementsMain.addAll(statements)

        mainCurrentInstructionId = currentInstructionId

        statements.clear()

        emitVoidInstruction("define i32 @$funName() nounwind {\n")
        currentInstructionId = 1
    }

    fun functionEnd() {

//        emitVoidInstruction("ret i32 %${currentInstructionId-1}\n}\n")
        emitVoidInstruction("ret i32 0\n}\n")
        headers.addAll(statements)
        statements.clear()
        currentInstructionId = mainCurrentInstructionId
    }

    fun closeMain() {

        statementsMain.addAll(statements)
    }

    fun functionCall(funName: String) {
        emitInstruction("call i32 @$funName()")
    }

    fun enterIfBody(): LLVMBuilder {
        currentBr++
        val lastCalculated = tempVariableStack.pop() ?: return this
        emitVoidInstruction("br i1 ${lastCalculated.toValueString()}, label %true$currentBr, label %false$currentBr")
        emitVoidInstruction("true$currentBr:")
        brStack.push(currentBr)

        return this
    }

    fun exitIfBody() {
        val br = brStack.pop()
        emitVoidInstruction("br label %false$br")
        emitVoidInstruction("false$br:")
    }

    fun startLoopInt(repetitions: String): LLVMBuilder {

        // Step 1: Allocate and initialize the loop counter (let's call it %tmp)
        val tmpId = emitInstruction("alloca i32, align 4")
        emitVoidInstruction("store i32 0, ptr %$tmpId, align 4")

        // Step 2: Create a new branch label for the loop condition
        currentBr++
        val loopBr = currentBr

        emitVoidInstruction("br label %cond$loopBr")
        emitVoidInstruction("cond$loopBr:")

        // Step 3: Load loop counter
        val loadedTmpId = emitInstruction("load i32, ptr %$tmpId, align 4")

        // Step 4: Increment the loop counter
        val oneConstId = emitInstruction("add nsw i32 %$loadedTmpId, 1")

        // Step 5: Store the incremented counter back
        emitVoidInstruction("store i32 %$oneConstId, ptr %$tmpId, align 4")

        // Step 6: Compare old counter value with `repetitions`
        val comparisonId = emitInstruction("icmp slt i32 %$loadedTmpId, $repetitions")

        // Step 7: Emit the conditional branch
        emitVoidInstruction("br i1 %$comparisonId, label %true$loopBr, label %false$loopBr")
        emitVoidInstruction("true$loopBr:")

        // Step 8: Push the branch number to stack for later use
        brStack.push(loopBr)

        return this
    }

    fun startLoopVariable(variable: String): LLVMBuilder {

        this.loadVariableToStack(variable)
        val lastCalculated = tempVariableStack.pop()

        if(lastCalculated!!.type.isInt()) {
            val repetitions = lastCalculated.toValueString()
            this.startLoopInt(repetitions)
        } else if(lastCalculated!!.type.isBoolean()) {
            tempVariableStack.push(lastCalculated)
            startLoopBoolExpression()
        } else {
            TODO()
        }

        return this
    }

    fun startLoopBoolExpression(): LLVMBuilder {

        currentBr++
        val loopBr = currentBr

        emitVoidInstruction("br label %cond$loopBr")
        emitVoidInstruction("cond$loopBr:")

        // Step 2: Evaluate the boolean expression (assumes it's already loaded)
        val condition = tempVariableStack.pop()?.toValueString() ?: error("No boolean condition found on stack")

        // Step 3: Branch based on the condition
        emitVoidInstruction("br i1 $condition, label %true$loopBr, label %false$loopBr")

        // Step 4: Emit true branch label (i.e., loop body entry)
        emitVoidInstruction("true$loopBr:")

        // Step 5: Push branch ID to stack for endLoop handling
        brStack.push(loopBr)

        return this
    }

    fun endLoopInt() {
        val b = brStack.pop()
        emitVoidInstruction("br label %cond$b")
        emitVoidInstruction("false$b:")
    }



    fun read(variableName: String): LLVMBuilder {
        operations.add(Operation.READ)

        val variable = getVariableFromVariableName(variableName, currentScope) ?: return this
        val constantStringId = addConstantString(createFormatForType(variable.type))
        emitInstruction("call i32 (ptr, ...) @scanf(ptr noundef $constantStringId, ptr noundef %${variable.id})")

        return this
    }

    fun getVariableType(variableName: String): Type? {
        val variable = getVariableFromVariableName(variableName, currentScope) ?: return null
        return variable.type
    }

    fun getVariable(variableName: String): Variable? {
        return getVariableFromVariableName(variableName, currentScope)
    }

    fun loadVariableToStack(variableName: String): LLVMBuilder {

        val variable = getVariableFromVariableName(variableName, currentScope) ?: return this

        if(variable.scope == "main") {

            val instructionId =
                emitInstruction("load ${variable.type.llvm}, ptr @.global.${variable.id}, align ${variable.type.size}")
            tempVariableStack.push(Variable(instructionId, variable.type, variable.scope))
        } else {

//            if(variable.scope != currentScope) {
//                syntaxErrors.add(
//                    SyntaxErrorWithLineData(
//                    "variable $variableName with scope ${variable.scope} cannot be used in $currentScope", -1, -1)
//                )
//            }

            val instructionId =
                emitInstruction("load ${variable.type.llvm}, ptr %${variable.id}, align ${variable.type.size}")
            tempVariableStack.push(Variable(instructionId, variable.type, variable.scope))
        }

        return this
    }

    fun loadStructVariableToStack(structName: String, struct: Struct, structVariableIndex: Int): LLVMBuilder {

//        val structType = getStructTypeFromStructName(structName, currentScope)
//        if(structType == null) {
//            syntaxErrors.add(SyntaxErrorWithLineData(
//                "Undeclared struct variable $structName", -1, -1
//            ))
//
//            return this
//        }
//
//        val struct = structs[structType]
//
//        if(struct == null) {
//            syntaxErrors.add(SyntaxErrorWithLineData(
//                "Undefined struct $structType", -1, -1
//            ))
//
//            return this
//        }
//
////        val variable = variableNameToVariable[Pair(structName, currentScope)]
//        var i = 0
//
//        for(structVariable in struct.variables) {
//
//            if(structVariable.first == variableName) {
//                break
//            }
//            i++
//        }
//
//        if(i == struct.variables.size) {
//            syntaxErrors.add(SyntaxErrorWithLineData(
//                "Undefined variable $variableName in struct $structName", -1, -1
//            ))
//        }

        val variable = getVariableFromVariableName(structName, currentScope) ?: return this

        val structVariableType = struct!!.variables.get(structVariableIndex).second

        var instructionId: Int

        if(struct.scope == "main") {
            instructionId = emitInstruction("getelementptr %${struct.name}, %${struct.name}* @.global.${variable!!.id}, i32 0, i32 $structVariableIndex ;")
        } else {
            instructionId = emitInstruction("getelementptr %${struct.name}, %${struct.name}* %${variable!!.id}, i32 0, i32 $structVariableIndex ;")
        }

        instructionId = emitInstruction("load ${structVariableType.llvm}, ${structVariableType.llvm}* %$instructionId")
        tempVariableStack.push(Variable(instructionId, structVariableType, variable.scope))

        return this
    }

    fun multiply(): LLVMBuilder {
        return twoOperandOperation("*", "mul nsw", "fmul")
    }

    fun divide(): LLVMBuilder {
        return twoOperandOperation("/", "sdiv", "fdiv")
    }

    fun add(): LLVMBuilder {
        return twoOperandOperation("+", "add nsw", "fadd")
    }

    fun subtract(): LLVMBuilder {
        return twoOperandOperation("-", "sub nsw", "fsub")
    }

    fun loadIntToStack(value: String): LLVMBuilder {
        tempVariableStack.push(
            Constant(
                type = Type.I32,
                value = value,
            )
        )

        return this
    }

    fun loadRealToStack(value: String): LLVMBuilder {
        tempVariableStack.push(
            Constant(
                type = Type.F32,
                value = value
            )
        )

        return this
    }

    fun loadBooleanToStack(value: String): LLVMBuilder {
        tempVariableStack.push(
            Constant(
                type = Type.BOOL,
                value = value
            )
        )

        return this
    }

    private fun twoOperandOperation(
        operator: String,
        intCommandPrefix: String,
        floatCommandPrefix: String
    ): LLVMBuilder {
        var second = tempVariableStack.pop() ?: return this
        var first = tempVariableStack.pop() ?: return this

        when {
            first.type.isInt() && second.type.isInt() || first.type.isFloat() && second.type.isFloat() -> {
                if (first.type.size < second.type.size) {
                    first = castVariableToType(first, second.type)
                } else if (first.type.size > second.type.size) {
                    second = castVariableToType(second, first.type)
                }
            }

            first.type.isInt() && second.type.isFloat() -> {
                first = castVariableToType(first, second.type)
            }

            first.type.isFloat() && second.type.isInt() -> {
                second = castVariableToType(second, first.type)
            }

            else -> if (first.type != second.type) {
                throw MatchingOperatorNotFoundException(
                    MatchingOperatorNotFoundError(
                        operator,
                        first.type,
                        second.type,
                    )
                )
            }
        }

        assert(first.type == second.type) { "after casting both operands should be the same type" }
        val instructionId = emitInstruction(
            "${
                when {
                    first.type.isInt() -> intCommandPrefix
                    second.type.isFloat() -> floatCommandPrefix
                    else -> error("There is currently no type which is neither int nor float")
                }
            } ${first.type.llvm} ${first.toValueString()}, ${second.toValueString()}"
        )
        tempVariableStack.push(Variable(instructionId, first.type, currentScope))

        return this
    }

    fun writeString(string: String): LLVMBuilder {
        operations.add(Operation.WRITE)

        val constantStringId = addConstantString(string)
        emitInstruction("call i32 (ptr, ...) @printf(ptr noundef $constantStringId)")

        return this
    }

    private fun addConstantString(string: String): String {
        constantStringToStringRepresentation.computeIfAbsent(string) {
            StringRepresentation(
                id = currentConstantStringId++
            )
        }

        return "@.str.${constantStringToStringRepresentation.getValue(string).id}"
    }

    private fun emitInstruction(instruction: String): Int {
        statements += "%$currentInstructionId = $instruction"
        return currentInstructionId++
    }

    private fun emitVoidInstruction(instruction: String) {
        statements += instruction
    }

    private fun createFormatForType(type: Type): String {
        return when (type) {
            Type.I32 -> "%d"
            Type.I64 -> "%ld"
            Type.F32 -> "%f"
            Type.F64 -> "%lf"
            Type.BOOL -> "%b"
            Type.STRUCT -> "" //TODO
        }
    }

    fun build(): String {
        val sb = StringBuilder()

        sb.appendLine(constantStringToStringRepresentation.toList().joinToString("\n") { (string, representation) ->
            "@.str.${representation.id} = private unnamed_addr constant [${string.length + 2} x i8] c\"$string\\0A\\00\", align 1"
        })
        sb.appendLine()

        sb.appendLine(headers.joinToString("\n"))

        sb.appendLine("define i32 @main() {")
        sb.appendLine((statementsMain + "ret i32 0").joinToString("\n") {
            "${" ".repeat(4)}$it"
        })
        sb.appendLine("}")
        sb.appendLine()

        sb.append(operations.joinToString("\n") { it.declaration })

        return sb.toString()
    }

    fun doesVariableExist(variableName: String, scope: String): Boolean {

        return getVariableFromVariableName(variableName, scope) != null
    }

    fun doesVariableExistInExactScope(variableName: String, scope: String): Boolean {

        return Pair(variableName, scope) in variableNameToVariable
    }

    fun writeLastCalculated(): LLVMBuilder {
        operations.add(Operation.WRITE)
        val lastCalculated = tempVariableStack.pop()?.let {
            if (it.type.isFloat()) {
                castVariableToType(it, Type.F64)
            } else {
                it
            }
        } ?: return this

        val constantStringId = addConstantString(createFormatForType(lastCalculated.type))
        emitInstruction("call i32 (ptr, ...) @printf(ptr noundef $constantStringId, ${lastCalculated.type.llvm} noundef ${lastCalculated.toValueString()})")

        return this
    }

    private fun castVariableToType(stackValue: StackValue, to: Type): StackValue {
        if (stackValue.type == to) return stackValue

        return Variable(
            id = emitInstruction(
                "${
                    when {
                        stackValue.type.isInt() && to.isInt() -> "sext"
                        stackValue.type.isFloat() && to.isFloat() -> "fpext"
                        stackValue.type.isInt() && to.isFloat() -> "sitofp"
                        stackValue.type.isFloat() && to.isInt() -> "fptosi"
                        else -> error("unhandled variable combination")
                    }
                } ${stackValue.type.llvm} ${stackValue.toValueString()} to ${to.llvm}"
            ),
            type = to,
            currentScope
        )
    }

    fun storeTo(variableName: String): LLVMBuilder {

        val variable = getVariableFromVariableName(variableName, currentScope) ?: return this
        val expressionResult = tempVariableStack.pop()?.let {
            if (it.type != variable.type) {
                castVariableToType(it, variable.type)
            } else {
                it
            }
        } ?: return this

        if (variable.scope == "main") {
            emitVoidInstruction("store ${expressionResult.type.llvm} ${expressionResult.toValueString()}, ptr @.global.${variable.id}, align ${variable.type.size}")
        } else {
            emitVoidInstruction("store ${expressionResult.type.llvm} ${expressionResult.toValueString()}, ptr %${variable.id}, align ${variable.type.size}")
        }


        return this
    }

    fun andBoolean(): LLVMBuilder {
        val rhs = tempVariableStack.pop() ?: return this
        val lhs = tempVariableStack.pop() ?: return this

        if(lhs.type != Type.BOOL || rhs.type != Type.BOOL) {
            syntaxErrors.add(SyntaxErrorWithLineData(
             "boolean AND operator does not support types ${rhs.type.name} AND ${lhs.type.name}", -1, -1
            ))

            return this
        }

        val result = emitInstruction("and i1 ${lhs.toValueString()}, ${rhs.toValueString()}")
        tempVariableStack.push(Variable(result, Type.BOOL, currentScope))

        return this
    }
    fun orBoolean(): LLVMBuilder {
        val rhs = tempVariableStack.pop() ?: return this
        val lhs = tempVariableStack.pop() ?: return this

        if(lhs.type != Type.BOOL || rhs.type != Type.BOOL) {
            syntaxErrors.add(SyntaxErrorWithLineData(
                "boolean OR operator does not support types ${rhs.type.name} AND ${lhs.type.name}", -1, -1
            ))

            return this
        }

        val result = emitInstruction("or i1 ${lhs.toValueString()}, ${rhs.toValueString()}")
        tempVariableStack.push(Variable(result, Type.BOOL, currentScope))

        return this
    }
    fun xorBoolean(): LLVMBuilder {
        val rhs = tempVariableStack.pop() ?: return this
        val lhs = tempVariableStack.pop() ?: return this

        if(lhs.type != Type.BOOL || rhs.type != Type.BOOL) {
            syntaxErrors.add(SyntaxErrorWithLineData(
                "boolean XOR operator does not support types ${rhs.type.name} AND ${lhs.type.name}", -1, -1
            ))

            return this
        }

        val result = emitInstruction("xor i1 ${lhs.toValueString()}, ${rhs.toValueString()}")
        tempVariableStack.push(Variable(result, Type.BOOL, currentScope))

        return this
    }
    fun negateBoolean(): LLVMBuilder {
        val operand = tempVariableStack.pop() ?: return this

        if(operand.type != Type.BOOL) {
            syntaxErrors.add(SyntaxErrorWithLineData(
                "boolean NEG operator does not support type ${operand.type.name}", -1, -1
            ))

            return this
        }

        // Logical NOT: xor with 1
        val result = emitInstruction("xor i1 ${operand.toValueString()}, 1")
        tempVariableStack.push(Variable(result, Type.BOOL, currentScope))

        return this
    }

    fun getVariableFromVariableName(variableName: String, currentScope: String): Variable? {

        if(Pair(variableName, currentScope) in variableNameToVariable) {
            return variableNameToVariable.getValue(Pair(variableName, currentScope))
        }

        if(Pair(variableName, MAIN_SCOPE) in variableNameToVariable) {
            return variableNameToVariable.getValue(Pair(variableName, MAIN_SCOPE))
        }

        return null
    }

    fun getStructTypeFromStructName(structName: String, currentScope: String): String? {

        if(Pair(structName, currentScope) in variableNameToStruct) {
            return variableNameToStruct.get(Pair(structName, currentScope))
        }

        if(Pair(structName, MAIN_SCOPE) in variableNameToStruct) {
            return variableNameToStruct.get(Pair(structName, MAIN_SCOPE))
        }

        return null
    }

}