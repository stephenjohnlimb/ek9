package org.ek9lang.compiler;

/**
 * Populated in the compilable program and altered during each of the phases of compilation.
 * This is to enable some components to initially do part processing then in later phases complete that full processing.
 * Specifically @link org.ek9lang.compiler.support.TypeSubstitution.
 */
public record CompilationData(CompilationPhase phase, CompilerFlags compilerFlags) {

}
