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

    @Test
    fun `handling expressions works correctly`() {
        val coolLangCode = """
            i64 x;
            i64 y;
            
            write (x + y) * x * ((y-x));
        """.trimIndent()

        val llvm = CoolLangToLLVMTranspiler().transpile(coolLangCode)

        val expectedLLVM = """
            @.str.1 = private unnamed_addr constant [4 x i8] c"%ld\00", align 1

            define i32 @main() {
                %1 = alloca i64, align 8
                %2 = alloca i64, align 8
                %3 = load i64, ptr %1, align 8
                %4 = load i64, ptr %2, align 8
                %5 = add nsw i64 %3, %4
                %6 = load i64, ptr %1, align 8
                %7 = mul nsw i64 %5, %6
                %8 = load i64, ptr %2, align 8
                %9 = load i64, ptr %1, align 8
                %10 = sub nsw i64 %8, %9
                %11 = mul nsw i64 %7, %10
                %12 = call i32 (ptr, ...) @printf(ptr noundef @.str.1, i64 noundef %11)
                ret i32 0
            }

            declare i32 @printf(ptr noundef, ...)
        """.trimIndent()
        assertEquals(expectedLLVM, llvm)
    }
}