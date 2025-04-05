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
                   |  |  +- value
                   |  |     +- ""Enter two numbers: ""
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
                   |  |  +- value
                   |  |     +- ""You have entered: ""
                   |  +- ";"
                   +- statement
                   |  +- writeOperation
                   |  |  +- "write"
                   |  |  +- value
                   |  |     +- "x"
                   |  +- ";"
                   +- statement
                   |  +- writeOperation
                   |  |  +- "write"
                   |  |  +- value
                   |  |     +- "", ""
                   |  +- ";"
                   +- statement
                      +- writeOperation
                      |  +- "write"
                      |  +- value
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
    fun `type that doesn't exist`() {
        givenCodeExpectSyntaxTree(
            input = """
                type x
            """.trimIndent(),
            expectedTree = """
                +- program
            """.trimIndent()
        )
    }

    private fun givenCodeExpectSyntaxTree(input: String, expectedTree: String) {
        val actualTree = CoolLangExtendedParser.fromStringInput(input).toSyntaxTreeString()
        assertEquals(expectedTree, actualTree)
    }

}