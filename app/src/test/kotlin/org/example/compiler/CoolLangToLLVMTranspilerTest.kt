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

    @Test
    fun `handling type casting works correctly`() {
        val coolLangCode = """
            i32 x;
            i64 y;
            write x + y;
            f64 a;
            f32 b;
            write a + b;
            x = a * x + b * x;
            write x;
        """.trimIndent()

        val llvm = CoolLangToLLVMTranspiler().transpile(coolLangCode)

        val expectedLLVM = """
            @.str.1 = private unnamed_addr constant [4 x i8] c"%ld\00", align 1
            @.str.2 = private unnamed_addr constant [4 x i8] c"%lf\00", align 1
            @.str.3 = private unnamed_addr constant [3 x i8] c"%d\00", align 1

            define i32 @main() {
                %1 = alloca i32, align 4
                %2 = alloca i64, align 8
                %3 = load i32, ptr %1, align 4
                %4 = load i64, ptr %2, align 8
                %5 = sext i32 %3 to i64
                %6 = add nsw i64 %5, %4
                %7 = call i32 (ptr, ...) @printf(ptr noundef @.str.1, i64 noundef %6)
                %8 = alloca double, align 8
                %9 = alloca float, align 4
                %10 = load double, ptr %8, align 8
                %11 = load float, ptr %9, align 4
                %12 = fpext float %11 to double
                %13 = fadd double %10, %12
                %14 = call i32 (ptr, ...) @printf(ptr noundef @.str.2, double noundef %13)
                %15 = load double, ptr %8, align 8
                %16 = load i32, ptr %1, align 4
                %17 = sitofp i32 %16 to double
                %18 = fmul double %15, %17
                %19 = load float, ptr %9, align 4
                %20 = load i32, ptr %1, align 4
                %21 = sitofp i32 %20 to float
                %22 = fmul float %19, %21
                %23 = fpext float %22 to double
                %24 = fadd double %18, %23
                %25 = fptosi double %24 to i32
                store i32 %25, ptr %1, align 4
                %26 = load i32, ptr %1, align 4
                %27 = call i32 (ptr, ...) @printf(ptr noundef @.str.3, i32 noundef %26)
                ret i32 0
            }

            declare i32 @printf(ptr noundef, ...)
        """.trimIndent()
        assertEquals(expectedLLVM, llvm)
    }
}