package org.example.compiler

import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class LLVMCompiler {

    fun compile(llvmFile: File, outputBinaryPath: Path) {
        val tempObjectFileAbsolutePathString = outputBinaryPath.toAbsolutePath().parent.absolutePathString() + File.separator + ".temp.o"

        val llcCommand = listOf(
            "llc",
            "-opaque-pointers",
            "-filetype=obj",
            llvmFile.absolutePath,
            "-o",
            tempObjectFileAbsolutePathString,
        )

        println("Running llc: ${llcCommand.joinToString(" ")}")
        val llcProcess = ProcessBuilder(llcCommand)
            .inheritIO()
            .start()
        val llcExitCode = llcProcess.waitFor()
        if (llcExitCode != 0) {
            throw RuntimeException("llc failed with exit code $llcExitCode")
        }

        val clangCommand = listOf(
            "clang",
            tempObjectFileAbsolutePathString,
            "-o",
            outputBinaryPath.absolutePathString(),
            "-no-pie"
        )
        println("Running clang: ${clangCommand.joinToString(" ")}")
        val clangProcess = ProcessBuilder(clangCommand)
            .inheritIO()
            .start()
        val clangExitCode = clangProcess.waitFor()
        if (clangExitCode != 0) {
            throw RuntimeException("clang failed with exit code $clangExitCode")
        }

        val assemblyFile = File(tempObjectFileAbsolutePathString)
        assemblyFile.delete()

        println("Compilation successful! Binary created at $outputBinaryPath")
    }
}