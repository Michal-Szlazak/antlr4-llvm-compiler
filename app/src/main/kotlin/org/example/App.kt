package org.example

import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.example.compiler.CoolLangExtendedParser
import org.example.compiler.CoolLangListenerImpl
import org.example.compiler.LLVMCompiler
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

    val parser = CoolLangExtendedParser.fromStringInput(codeToCompile)
    val programContext = parser.program()

    if(parser.numberOfSyntaxErrors != 0) {
        exitProcess(1)
    }

    val listener = CoolLangListenerImpl()
    val walker = ParseTreeWalker()
    walker.walk(listener, programContext)

    val llvmCode = listener.getLLVM()

    val llvmFile = File(parsedArgs.outputLLVMFilename)
    llvmFile.writeText(llvmCode, Charsets.UTF_8)

    val compiler = LLVMCompiler()

    try {
        compiler.compile(llvmFile, Path(parsedArgs.outputBinaryFilename))
    } catch (e: Exception) {
        println("Compilation failed: ${e.message}")
    }
}
