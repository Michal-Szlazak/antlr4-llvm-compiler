package org.example.compiler

enum class Type(val typeName: String, val llvm: String, val size: Int) {
    I32("i32", "i32", 4),
    I64("i64", "i64", 8),
    F32("f32", "float", 4),
    F64("f64", "double", 8);

    companion object {
        fun fromTypeName(typeName: String): Type? {
            return entries.firstOrNull { it.typeName == typeName }
        }
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

    fun declaration(type: Type, name: String): LLVMBuilder {
        require(name !in variableNameToVariable)

        val instructionId = emitInstruction("alloca ${type.llvm}, align ${type.size}")
        variableNameToVariable[name] = Variable(
            id = instructionId,
            type = type,
        )

        return this
    }

    fun read(variableName: String): LLVMBuilder {
        require(variableName in variableNameToVariable)

        operations.add(Operation.READ)

        val variable = variableNameToVariable.getValue(variableName)
        val constantStringId = addConstantString(createFormatForType(variable.type))
        emitInstruction("call i32 (ptr, ...) @scanf(ptr noundef $constantStringId, ptr noundef %${variable.id})")

        return this
    }

    fun writeVariable(variableName: String): LLVMBuilder {
        require(variableNameToVariable.containsKey(variableName))

        operations.add(Operation.WRITE)

        val variable = variableNameToVariable.getValue(variableName)
        val instructionId =
            emitInstruction("load ${variable.type.llvm}, ptr %${variable.id}, align ${variable.type.size}")

        // TODO: Maybe refactor when function calling will be handled?
        if (variable.type == Type.F32) {
            // According to Clang compiler implementation floats are extended to double for printf method call
            val extensionInstructionId = emitInstruction("fpext float %$instructionId to ${Type.F64.llvm}")
            val constantStringId = addConstantString(createFormatForType(variable.type))
            emitInstruction("call i32 (ptr, ...) @printf(ptr noundef $constantStringId, ${Type.F64.llvm} noundef %$extensionInstructionId)")
        } else {
            val constantStringId = addConstantString(createFormatForType(variable.type))
            emitInstruction("call i32 (ptr, ...) @printf(ptr noundef $constantStringId, ${variable.type.llvm} noundef %$instructionId)")
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
        sb.append("")

        return sb.toString()
    }

}