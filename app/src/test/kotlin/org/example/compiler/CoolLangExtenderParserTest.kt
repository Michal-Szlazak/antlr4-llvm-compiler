package org.example.compiler

import kotlin.test.Test
import kotlin.test.assertEquals

class CoolLangExtenderParserTest {

    @Test
    fun `write read operations`() {
        givenCodeExpectSyntaxTree(
            input = """
                i32 x;
                i32 y;
                
                write "Enter two numbers: ";
                read x;
                read y;
                
                write "You have entered: ";
                write x;
                write ", ";
                write y;
            """.trimIndent(),
            expectedTree = """
                +- program
                   +- statement
                   |  +- declaration
                   |  |  +- type
                   |  |  |  +- "i32"
                   |  |  +- "x"
                   |  +- ";"
                   +- statement
                   |  +- declaration
                   |  |  +- type
                   |  |  |  +- "i32"
                   |  |  +- "y"
                   |  +- ";"
                   +- statement
                   |  +- writeOperation
                   |  |  +- "write"
                   |  |  +- ""Enter two numbers: ""
                   |  +- ";"
                   +- statement
                   |  +- readOperation
                   |  |  +- "read"
                   |  |  +- "x"
                   |  +- ";"
                   +- statement
                   |  +- readOperation
                   |  |  +- "read"
                   |  |  +- "y"
                   |  +- ";"
                   +- statement
                   |  +- writeOperation
                   |  |  +- "write"
                   |  |  +- ""You have entered: ""
                   |  +- ";"
                   +- statement
                   |  +- writeOperation
                   |  |  +- "write"
                   |  |  +- expression
                   |  |     +- "x"
                   |  +- ";"
                   +- statement
                   |  +- writeOperation
                   |  |  +- "write"
                   |  |  +- "", ""
                   |  +- ";"
                   +- statement
                      +- writeOperation
                      |  +- "write"
                      |  +- expression
                      |     +- "y"
                      +- ";"
            """.trimIndent()
        )
    }

    @Test
    fun `different size integers`() {
        givenCodeExpectSyntaxTree(
            input = """
                i64 x;
                i32 y;
            """.trimIndent(),
            expectedTree = """
                +- program
                   +- statement
                   |  +- declaration
                   |  |  +- type
                   |  |  |  +- "i64"
                   |  |  +- "x"
                   |  +- ";"
                   +- statement
                      +- declaration
                      |  +- type
                      |  |  +- "i32"
                      |  +- "y"
                      +- ";"
            """.trimIndent()
        )
    }

    @Test
    fun `different size float numbers`() {
        givenCodeExpectSyntaxTree(
            input = """
                f64 x;
                f32 y;
            """.trimIndent(),
            expectedTree = """
                +- program
                   +- statement
                   |  +- declaration
                   |  |  +- type
                   |  |  |  +- "f64"
                   |  |  +- "x"
                   |  +- ";"
                   +- statement
                      +- declaration
                      |  +- type
                      |  |  +- "f32"
                      |  +- "y"
                      +- ";"
            """.trimIndent()
        )
    }

    @Test
    fun `writing expressions`() {
        givenCodeExpectSyntaxTree(
            input = """
                f64 x;
                f64 y;
                
                write x * (y + x);
            """.trimIndent(),
            expectedTree = """
                +- program
                   +- statement
                   |  +- declaration
                   |  |  +- type
                   |  |  |  +- "f64"
                   |  |  +- "x"
                   |  +- ";"
                   +- statement
                   |  +- declaration
                   |  |  +- type
                   |  |  |  +- "f64"
                   |  |  +- "y"
                   |  +- ";"
                   +- statement
                      +- writeOperation
                      |  +- "write"
                      |  +- expression
                      |     +- expression
                      |     |  +- "x"
                      |     +- "*"
                      |     +- expression
                      |        +- "("
                      |        +- expression
                      |        |  +- expression
                      |        |  |  +- "y"
                      |        |  +- "+"
                      |        |  +- expression
                      |        |     +- "x"
                      |        +- ")"
                      +- ";"
            """.trimIndent()
        )
    }

    private fun givenCodeExpectSyntaxTree(input: String, expectedTree: String) {
        val actualTree = CoolLangExtendedParser.fromStringInput(input).toSyntaxTreeString()
        assertEquals(expectedTree, actualTree)
    }

}