package org.ek9lang.compiler.main.phases;

import java.util.function.BiFunction;
import org.ek9lang.compiler.errors.CompilationListener;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.CompilerFlags;
import org.ek9lang.compiler.main.phases.result.CompilableSourceErrorCheck;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;

/**
 * MULTI THREADED
 * Now try check that all symbols used have a type. THIS IS MAJOR MILESTONE!
 * Create expression symbols in here; so we can pass the type base and also enable type checking.
 * Do some basic semantic checks in here before developing an IR.
 * Doing too much focus on the type resolution only we need to call this many times when there a
 * circular type loops.
 * The main complexity in here is building the return types for expressions and resolving types
 * for classes and specific methods.
 * So quite a bit of the semantic checking can go on in here. as always fail as early as possible
 * in the phases. But process that phase to the end.
 * Note that CallId is particularly complex to deal with all the ways to handle TextForSomething()
 * because it could be.
 * A. new instance of class TextForSomething i.e. new TextForSomething()
 * - then we have to check it has a constructor with those arguments.
 * B. new instance of a template type that has not had parameters supplied like item := List()
 * - so can we infer its type from the variable assigned to?
 * C. new instance of a template type that has had parameters supplied like item := List(23)
 * - so we can infer from '23' this is a List of integer
 * D. Just a normal method call
 * - so find the method and check the parameters match.
 * E. call to a Function with a number of parameters.
 * F. call to a variable (C# delegate) that points to a function
 * - with a number of parameters.
 * And probably more by the time we've finished.
 */
public class Ek9Phase5SymbolResolution implements
    BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {
  private final CompilationListener listener;
  private final CompilerReporter reporter;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  public Ek9Phase5SymbolResolution(CompilationListener listener, CompilerReporter reporter) {
    this.listener = listener;
    this.reporter = reporter;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {
    final var thisPhase = CompilationPhase.FULL_RESOLUTION;
    return new CompilationPhaseResult(thisPhase, true,
        compilerFlags.getCompileToPhase() == thisPhase);
  }
}
