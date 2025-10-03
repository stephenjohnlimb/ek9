package org.ek9lang.cli;

import org.ek9lang.compiler.Compiler;

/**
 * A context to hold all the essential objects needed for compiling.
 * Includes mutable CompilationResult for tracking compilation state.
 */
record CompilationContext(CommandLine commandLine,
                          Compiler compiler,
                          FileCache sourceFileCache,
                          boolean muteReportedErrors,
                          CompilationResult compilationResult) {
}
