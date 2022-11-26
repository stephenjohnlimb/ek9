package org.ek9lang.cli.support;

import org.ek9lang.cli.CommandLineDetails;
import org.ek9lang.compiler.main.Compiler;

/**
 * A context to hold all the essential objects needed for compiling.
 */
public record CompilationContext(CommandLineDetails commandLine,
                                 Compiler compiler,
                                 FileCache sourceFileCache) {
}
