package org.example

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required

class ArgsParser {

    data class ParsedArgs(
        val codeFilename: String,
        val outputBinaryFilename: String,
        val outputLLVMFilename: String,
    )

    companion object {
        fun parse(args: Array<String>): ParsedArgs {
            val parser = ArgParser("clc")
            val codeFilename by parser.option(
                ArgType.String,
                fullName = "filename",
                shortName = "f",
                description = "File name to compile"
            ).required()
            val outputBinaryFilename by parser.option(
                ArgType.String,
                fullName = "output",
                shortName = "o",
                description = "File name to output binary"
            )
            val outputLLVMFilename by parser.option(
                ArgType.String,
                fullName = "llvm-output",
                shortName = "l",
                description = "File name to output LLVM"
            )

            parser.parse(args)

            return ParsedArgs(
                codeFilename = codeFilename,

                outputBinaryFilename = outputBinaryFilename ?: "cl.out",
                outputLLVMFilename = outputLLVMFilename ?: "cl.ll",
            )
        }
    }
}