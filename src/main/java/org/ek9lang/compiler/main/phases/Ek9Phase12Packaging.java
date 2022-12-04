package org.ek9lang.compiler.main.phases;

import java.util.function.BiFunction;
import org.ek9lang.compiler.errors.CompilationListener;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.CompilerFlags;
import org.ek9lang.compiler.main.phases.result.CompilableSourceErrorCheck;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;

/**
 * SINGLE THREADED
 * Take the final optimised generated code and package it in to some type of release vessel.
 * See compilationContext.commandLine().targetArchitecture to determine what to prepare to optimise.
 * For java this will most likely be a jar.
 */
public class Ek9Phase12Packaging implements
    BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {
  private final CompilationListener listener;
  private final CompilerReporter reporter;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  public Ek9Phase12Packaging(CompilationListener listener, CompilerReporter reporter) {
    this.listener = listener;
    this.reporter = reporter;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {
    final var thisPhase = CompilationPhase.APPLICATION_PACKAGING;
    return new CompilationPhaseResult(thisPhase, true,
        compilerFlags.getCompileToPhase() == thisPhase);
  }

}
