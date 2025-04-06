package org.example

import org.example.compiler.CoolLangToLLVMTranspiler
import org.example.compiler.LLVMCompiler
import org.example.compiler.error.InvalidSyntaxException
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val parsedArgs = ArgsParser.parse(args)

    val codeToCompile = Paths.get(parsedArgs.codeFilename)
        .toAbsolutePath()
        .toFile()
        .readText(Charsets.UTF_8)

    val llvmCode = try {
        CoolLangToLLVMTranspiler().transpile(codeToCompile)
    } catch (e: InvalidSyntaxException) {
        e.syntaxErrors.forEach(System.err::println)
        exitProcess(1)
    }

    val llvmFile = File(parsedArgs.outputLLVMFilename)
    llvmFile.writeText(llvmCode, Charsets.UTF_8)

    val llvmCompiler = LLVMCompiler()

    try {
        llvmCompiler.compile(llvmFile, Path(parsedArgs.outputBinaryFilename))
    } catch (e: Exception) {
        println("Compilation failed: ${e.message}")
    }
}
