package org.example.compiler.error

class RedefinitionError(redefinedName: String) :
    SyntaxError("redefinition of '$redefinedName'")
