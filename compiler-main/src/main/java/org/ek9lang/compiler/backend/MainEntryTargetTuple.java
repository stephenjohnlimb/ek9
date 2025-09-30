package org.ek9lang.compiler.backend;

import java.io.File;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.ir.instructions.ProgramEntryPointInstr;
import org.ek9lang.core.FileHandling;

/**
 * Contains all the information needed for target-specific visitors to generate main entry points.
 * Each visitor can determine its own file naming conventions and output handling.
 */
public record MainEntryTargetTuple(ProgramEntryPointInstr programEntryPoint,
                                   CompilerFlags compilerFlags,
                                   FileHandling fileHandling,
                                   File outputDirectory) {
}