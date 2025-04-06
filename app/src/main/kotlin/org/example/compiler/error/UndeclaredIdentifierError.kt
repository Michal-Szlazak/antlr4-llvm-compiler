package org.example.compiler.error

class UndeclaredIdentifierError(undeclaredIdentifier: String) :
    SyntaxError("use of undeclared identifier '$undeclaredIdentifier'")