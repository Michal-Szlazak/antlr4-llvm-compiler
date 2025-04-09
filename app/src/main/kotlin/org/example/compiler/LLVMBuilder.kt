package org.example.compiler

import org.example.compiler.error.MatchingOperatorNotFoundError
import org.example.compiler.error.MatchingOperatorNotFoundException


enum class Type(val typeName: String, val llvm: String, val size: Int) {
    I32("i32", "i32", 4),
    I64("i64", "i64", 8),
    F32("f32", "float", 4),
    F64("f64", "double", 8);

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

data class Variable(val id: Int, val type: Type)

data class StringRepresentation(val id: Int)

class LLVMBuilder {
    private val statements = mutableSetOf<String>()
    private val operations = mutableSetOf<Operation>()

    private val constantStringToStringRepresentation = mutableMapOf<String, StringRepresentation>()
    private var currentConstantStringId = 1

    private val variableNameToVariable = mutableMapOf<String, Variable>()
    private var currentInstructionId = 1
    private val tempVariableStack = ArrayDeque<Variable>()
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

    private fun twoOperandOperation(
        operator: String,
        intCommandPrefix: String,
        floatCommandPrefix: String
    ): LLVMBuilder {
        val second = tempVariableStack.pop() ?: return this
        val first = tempVariableStack.pop() ?: return this

        if (first.type == second.type) {
            val instructionId = emitInstruction(
                "${
                    when {
                        first.type.isInt() -> intCommandPrefix
                        second.type.isFloat() -> floatCommandPrefix
                        else -> error("There is currently no type which is neither int nor float")
                    }
                } ${first.type.llvm} %${first.id}, %${second.id}"
            )
            tempVariableStack.push(Variable(instructionId, first.type))
        } else {
            throw MatchingOperatorNotFoundException(
                MatchingOperatorNotFoundError(
                    operator,
                    first.type,
                    second.type,
                )
            )
        }

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

    private fun emitInstruction(instruction: String, isVoid: Boolean = false): Int {
        statements += if (isVoid) instruction else "%$currentInstructionId = $instruction"
        return currentInstructionId++
    }

    private fun createFormatForType(type: Type): String {
        return when (type) {
            Type.I32 -> "%d"
            Type.I64 -> "%ld"
            Type.F32 -> "%f"
            Type.F64 -> "%lf"
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
        val lastCalculated = tempVariableStack.pop() ?: return this

        // TODO: Maybe refactor when function calling will be handled?
        if (lastCalculated.type == Type.F32) {
            // According to Clang compiler implementation floats are extended to double for printf call
            val extensionInstructionId =
                emitInstruction("fpext float %${lastCalculated.id} to ${Type.F64.llvm}")
            val constantStringId = addConstantString(createFormatForType(lastCalculated.type))
            emitInstruction("call i32 (ptr, ...) @printf(ptr noundef $constantStringId, ${Type.F64.llvm} noundef %$extensionInstructionId)")
        } else {
            val constantStringId = addConstantString(createFormatForType(lastCalculated.type))
            emitInstruction("call i32 (ptr, ...) @printf(ptr noundef $constantStringId, ${lastCalculated.type.llvm} noundef %${lastCalculated.id})")
        }

        return this
    }

}