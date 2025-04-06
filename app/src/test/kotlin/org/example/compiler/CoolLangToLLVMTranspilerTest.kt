package org.example.compiler

import org.example.compiler.error.InvalidSyntaxException
import org.example.compiler.error.SyntaxError
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.*

class CoolLangToLLVMTranspilerTest {

    private val stdOut = ByteArrayOutputStream()
    private val stdErr = ByteArrayOutputStream()
    private lateinit var originalOut: PrintStream
    private lateinit var originalErr: PrintStream

    @BeforeTest
    fun setUpStreams() {
        originalOut = System.out
        originalErr = System.err
        System.setOut(PrintStream(stdOut))
        System.setErr(PrintStream(stdErr))
    }

    @AfterTest
    fun tearDownStreams() {
        System.setOut(originalOut)
        System.setErr(originalErr)
    }

    @Test
    fun `redefining variable should throw exception`() {
        val coolLangCode = """
            i32 x;
            i32 x;
        """.trimIndent()

        val exception = assertFailsWith<InvalidSyntaxException> {
            CoolLangToLLVMTranspiler().transpile(coolLangCode)
        }

        val expectedSyntaxErrors = exception.syntaxErrors.map { it.message }
        assertEquals(expectedSyntaxErrors, listOf("redefinition of 'x'"))
    }
}