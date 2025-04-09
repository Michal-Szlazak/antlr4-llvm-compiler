package org.example.compiler

import org.example.compiler.error.InvalidSyntaxException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CoolLangToLLVMTranspilerTest {

    @Test
    fun `redefining variable should throw exception`() {
        val coolLangCode = """
            i32 x;
            f32 x;
        """.trimIndent()

        val exception = assertFailsWith<InvalidSyntaxException> {
            CoolLangToLLVMTranspiler().transpile(coolLangCode)
        }

        val actualSyntaxErrors = exception.syntaxErrors.map { it.toString() }
        assertEquals(listOf("line 2:4 redefinition of 'x'"), actualSyntaxErrors)
    }
}