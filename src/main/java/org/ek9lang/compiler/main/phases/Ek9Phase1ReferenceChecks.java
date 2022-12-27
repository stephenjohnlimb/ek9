package org.ek9lang.compiler.main.phases;

import java.util.function.BiFunction;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.ek9lang.compiler.errors.CompilationPhaseListener;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.CompilableSource;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.CompilerFlags;
import org.ek9lang.compiler.main.phases.resolution.ReferencesPhase1Listener;
import org.ek9lang.compiler.main.phases.result.CompilableSourceErrorCheck;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.threads.SharedThreadContext;

/**
 * SINGLE THREADED - because we only want one reference shorthand for an item.
 * We don't want two different files reference com.abc.Item and another file
 * com.def.Item in the same module - even though different source files.
 */
public class Ek9Phase1ReferenceChecks implements BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {
  private final CompilationPhaseListener listener;
  private final CompilerReporter reporter;
  private final SharedThreadContext<CompilableProgram> compilableProgramAccess;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  /**
   * Create a new reference checker for modules contained in the compilable program.
   */
  public Ek9Phase1ReferenceChecks(SharedThreadContext<CompilableProgram> compilableProgramAccess,
                                  CompilationPhaseListener listener, CompilerReporter reporter) {
    this.listener = listener;
    this.reporter = reporter;
    this.compilableProgramAccess = compilableProgramAccess;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {
    final var thisPhase = CompilationPhase.REFERENCE_CHECKS;
    reporter.log(thisPhase);
    workspace
        .getSources()
        .stream()
        .forEach(this::resolveReferencedSymbols);

    workspace.getSources().forEach(source -> listener.accept(thisPhase, source));
    var errorFree = !sourceHaveErrors.test(workspace.getSources());

    return new CompilationPhaseResult(thisPhase, errorFree,
        compilerFlags.getCompileToPhase() == thisPhase);
  }

  /**
   * THIS IS WHERE THE REFERENCES PHASE 1 LISTENER IS CREATED AND USED.
   * It's a sort on mini resolution phase just for references.
   * Note that this code is designed to be singled threaded and so get a lock for the duration
   * of processing each source. It could be widened for the whole listener/apply method.
   */
  private void resolveReferencedSymbols(CompilableSource source) {
    compilableProgramAccess.accept(program -> {
      var parsedModule = program.getParsedModuleForCompilableSource(source);
      AssertValue.checkNotNull("ParsedModule must be present for source", parsedModule);

      parsedModule.acceptCompilationUnitContext(source.getCompilationUnitContext());

      ReferencesPhase1Listener phaseListener = new ReferencesPhase1Listener(program, parsedModule);
      ParseTreeWalker walker = new ParseTreeWalker();
      walker.walk(phaseListener, source.getCompilationUnitContext());
    });
  }
}