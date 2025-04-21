package org.example.compiler

import org.example.compiler.error.MatchingOperatorNotFoundError
import org.example.compiler.error.MatchingOperatorNotFoundException


enum class Type(val typeName: String, val llvm: String, val size: Int) {
    I32("i32", "i32", 4),
    I64("i64", "i64", 8),
    F32("f32", "float", 4),
    F64("f64", "double", 8),
    BOOL("bool", "i1", 1);

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

class Variable(val id: Int, type: Type) : StackValue(type) {
    override fun toValueString(): String {
        return "%${id}"
    }
}

class Constant(type: Type, val value: String) : StackValue(type) {
    override fun toValueString(): String {
        return value
    }
}

data class StringRepresentation(val id: Int)

class LLVMBuilder {
    private val statements = mutableListOf<String>()
    private val operations = mutableSetOf<Operation>()

    private val constantStringToStringRepresentation = mutableMapOf<String, StringRepresentation>()
    private var currentConstantStringId = 1

    private val variableNameToVariable = mutableMapOf<String, Variable>()
    private var currentInstructionId = 1
    private val tempVariableStack = ArrayDeque<StackValue>()
    private val brStack = ArrayDeque<Int>()
    private var tmp = 1;
    private var mainTmp = 1;
    private var currentBr = 0
    private fun <T> ArrayDeque<T>.push(element: T) = addLast(element)
    private fun <T> ArrayDeque<T>.pop() = removeLastOrNull()

    fun declaration(type: Type, name: String): LLVMBuilder {
        val instructionId = emitInstruction("alloca ${type.llvm}, align ${type.size}")
        variableNameToVariable[name] = Variable(
            id = instructionId,
            type = type,
        )

        return this
    }

    fun functionStart(funName: String) {


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
        val repetitions = lastCalculated!!.toValueString()

        this.startLoopInt(repetitions)

        return this
    }

    fun endLoopInt() {
        val b = brStack.pop()
        emitVoidInstruction("br label %cond$b")
        emitVoidInstruction("false$b:")
    }



    fun read(variableName: String): LLVMBuilder {
        operations.add(Operation.READ)

        val variable = variableNameToVariable.getValue(variableName)
        val constantStringId = addConstantString(createFormatForType(variable.type))
        emitInstruction("call i32 (ptr, ...) @scanf(ptr noundef $constantStringId, ptr noundef %${variable.id})")

        return this
    }

    fun writeVariable(variableName: String): LLVMBuilder {
        operations.add(Operation.WRITE)

        loadVariableToStack(variableName)
        writeLastCalculated()

        return this
    }

    fun loadVariableToStack(variableName: String): LLVMBuilder {
        val variable = variableNameToVariable.getValue(variableName)
        val instructionId =
            emitInstruction("load ${variable.type.llvm}, ptr %${variable.id}, align ${variable.type.size}")
        tempVariableStack.push(Variable(instructionId, variable.type))

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
        tempVariableStack.push(Variable(instructionId, first.type))

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
        }
    }

    fun build(): String {
        val sb = StringBuilder()

        sb.appendLine(constantStringToStringRepresentation.toList().joinToString("\n") { (string, representation) ->
            "@.str.${representation.id} = private unnamed_addr constant [${string.length + 1} x i8] c\"$string\\00\", align 1"
        })
        sb.appendLine()

        sb.appendLine("define i32 @main() {")
        sb.appendLine((statements + "ret i32 0").joinToString("\n") {
            "${" ".repeat(4)}$it"
        })
        sb.appendLine("}")
        sb.appendLine()
        sb.append(operations.joinToString("\n") { it.declaration })

        return sb.toString()
    }

    fun doesVariableExist(variableName: String): Boolean {
        return variableName in variableNameToVariable
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
        )
    }

    fun storeTo(variableName: String): LLVMBuilder {
        val variable = variableNameToVariable.getValue(variableName)
        val expressionResult = tempVariableStack.pop()?.let {
            if (it.type != variable.type) {
                castVariableToType(it, variable.type)
            } else {
                it
            }
        } ?: return this

        emitVoidInstruction("store ${expressionResult.type.llvm} ${expressionResult.toValueString()}, ptr %${variable.id}, align ${variable.type.size}")

        return this
    }

    fun andBoolean(): LLVMBuilder {
        val rhs = tempVariableStack.pop() ?: return this
        val lhs = tempVariableStack.pop() ?: return this

        require(lhs.type == Type.BOOL && rhs.type == Type.BOOL) {
            "andBoolean expects two boolean operands"
        }

        val result = emitInstruction("and i1 ${lhs.toValueString()}, ${rhs.toValueString()}")
        tempVariableStack.push(Variable(result, Type.BOOL))

        return this
    }
    fun orBoolean(): LLVMBuilder {
        val rhs = tempVariableStack.pop() ?: return this
        val lhs = tempVariableStack.pop() ?: return this

        require(lhs.type == Type.BOOL && rhs.type == Type.BOOL) {
            "orBoolean expects two boolean operands"
        }

        val result = emitInstruction("or i1 ${lhs.toValueString()}, ${rhs.toValueString()}")
        tempVariableStack.push(Variable(result, Type.BOOL))

        return this
    }
    fun xorBoolean(): LLVMBuilder {
        val rhs = tempVariableStack.pop() ?: return this
        val lhs = tempVariableStack.pop() ?: return this

        require(lhs.type == Type.BOOL && rhs.type == Type.BOOL) {
            "xorBoolean expects two boolean operands"
        }

        val result = emitInstruction("xor i1 ${lhs.toValueString()}, ${rhs.toValueString()}")
        tempVariableStack.push(Variable(result, Type.BOOL))

        return this
    }
    fun negateBoolean(): LLVMBuilder {
        val operand = tempVariableStack.pop() ?: return this

        require(operand.type == Type.BOOL) {
            "negateBoolean expects a boolean operand"
        }

        // Logical NOT: xor with 1
        val result = emitInstruction("xor i1 ${operand.toValueString()}, 1")
        tempVariableStack.push(Variable(result, Type.BOOL))

        return this
    }

}