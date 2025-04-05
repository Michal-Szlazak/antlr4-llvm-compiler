package org.example.compiler

import kotlin.test.Test
import kotlin.test.assertEquals

class LLVMBuilderTest {

    @Test
    fun `write read operations`() {
        val llvmBuilder = LLVMBuilder()
        val actualLLVM = llvmBuilder
            .declaration(Type.I32, "x")
            .declaration(Type.I32, "y")
            .writeString("Enter two numbers: ")
            .read("x")
            .read("y")
            .writeString("You have entered: ")
            .writeVariable("x")
            .writeString(", ")
            .writeVariable("y")
            .build()

        val expectedLLVM = """
            @.str.1 = private unnamed_addr constant [20 x i8] c"Enter two numbers: \00", align 1
            @.str.2 = private unnamed_addr constant [3 x i8] c"%d\00", align 1
            @.str.3 = private unnamed_addr constant [19 x i8] c"You have entered: \00", align 1
            @.str.4 = private unnamed_addr constant [3 x i8] c", \00", align 1

            define i32 @main() {
                %1 = alloca i32, align 4
                %2 = alloca i32, align 4
                %3 = call i32 (ptr, ...) @printf(ptr noundef @.str.1)
                %4 = call i32 (ptr, ...) @scanf(ptr noundef @.str.2, ptr noundef %1)
                %5 = call i32 (ptr, ...) @scanf(ptr noundef @.str.2, ptr noundef %2)
                %6 = call i32 (ptr, ...) @printf(ptr noundef @.str.3)
                %7 = load i32, ptr %1, align 4
                %8 = call i32 (ptr, ...) @printf(ptr noundef @.str.2, i32 noundef %7)
                %9 = call i32 (ptr, ...) @printf(ptr noundef @.str.4)
                %10 = load i32, ptr %2, align 4
                %11 = call i32 (ptr, ...) @printf(ptr noundef @.str.2, i32 noundef %10)
                ret i32 0
            }

            declare i32 @printf(ptr noundef, ...)
            declare i32 @scanf(ptr noundef, ...)
        """.trimIndent()

        assertEquals(expectedLLVM, actualLLVM)
    }

    @Test
    fun `different size integers`() {
        val llvmBuilder = LLVMBuilder()
        val actualLLVM = llvmBuilder
            .declaration(Type.I64, "x")
            .declaration(Type.I32, "y")
            .read("x")
            .read("y")
            .writeVariable("x")
            .writeVariable("y")
            .build()

        val expectedLLVM = """
            @.str.1 = private unnamed_addr constant [4 x i8] c"%ld\00", align 1
            @.str.2 = private unnamed_addr constant [3 x i8] c"%d\00", align 1

            define i32 @main() {
                %1 = alloca i64, align 8
                %2 = alloca i32, align 4
                %3 = call i32 (ptr, ...) @scanf(ptr noundef @.str.1, ptr noundef %1)
                %4 = call i32 (ptr, ...) @scanf(ptr noundef @.str.2, ptr noundef %2)
                %5 = load i64, ptr %1, align 8
                %6 = call i32 (ptr, ...) @printf(ptr noundef @.str.1, i64 noundef %5)
                %7 = load i32, ptr %2, align 4
                %8 = call i32 (ptr, ...) @printf(ptr noundef @.str.2, i32 noundef %7)
                ret i32 0
            }

            declare i32 @scanf(ptr noundef, ...)
            declare i32 @printf(ptr noundef, ...)
        """.trimIndent()

        assertEquals(expectedLLVM, actualLLVM)
    }
}