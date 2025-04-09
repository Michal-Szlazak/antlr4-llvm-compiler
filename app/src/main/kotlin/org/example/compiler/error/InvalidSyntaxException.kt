package org.example.compiler.error

class InvalidSyntaxException(val syntaxErrors: List<SyntaxErrorWithLineData>) : RuntimeException()
