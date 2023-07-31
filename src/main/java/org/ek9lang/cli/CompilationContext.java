package org.ek9lang.cli;

import org.ek9lang.compiler.Compiler;

/**
 * A context to hold all the essential objects needed for compiling.
 */
record CompilationContext(CommandLineDetails commandLine,
                                 Compiler compiler,
                                 FileCache sourceFileCache) {
}
