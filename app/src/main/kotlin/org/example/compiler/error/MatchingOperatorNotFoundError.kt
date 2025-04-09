package org.example.compiler.error

import org.example.compiler.Type

class MatchingOperatorNotFoundError(operator: String, firstOperandType: Type, secondOperandType: Type)
    : SyntaxError("Operator '$operator' is not supported between operands of types '${firstOperandType.typeName}' and '${secondOperandType.typeName}'")