package org.ek9lang.compiler.phase1;

import java.util.function.Consumer;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.CompilerPhase;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.common.CompilableSourceErrorCheck;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.SharedThreadContext;

/**
 * SINGLE THREADED - because we only want one reference shorthand for an item.
 * We don't want two different files reference com.abc.Item and another file
 * com.def.Item in the same module - even though different source files.
 */
public final class ReferenceChecks extends CompilerPhase {
  private static final CompilationPhase thisPhase = CompilationPhase.REFERENCE_CHECKS;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();

  /**
   * Create a new reference checker for modules contained in the compilable program.
   */
  public ReferenceChecks(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                         final Consumer<CompilationEvent> listener,
                         final CompilerReporter reporter) {

    super(thisPhase, compilableProgramAccess, listener, reporter);
  }

  @Override
  public boolean doApply(final Workspace workspace, final CompilerFlags compilerFlags) {

    workspace
        .getSources()
        .forEach(this::resolveReferencedSymbols);

    return !sourceHaveErrors.test(workspace.getSources());
  }

  /**
   * THIS IS WHERE THE REFERENCES PHASE 1 LISTENER IS CREATED AND USED.
   * It's a sort on mini resolution phase just for references.
   * Note that this code is designed to be singled threaded and so get a lock for the duration
   * of processing each source. It could be widened for the whole listener/apply method.
   */
  private void resolveReferencedSymbols(final CompilableSource source) {

    compilableProgramAccess.accept(program -> {
      final var parsedModule = program.getParsedModuleForCompilableSource(source);
      AssertValue.checkNotNull("ParsedModule must be present for source", parsedModule);

      parsedModule.acceptCompilationUnitContext(source.getCompilationUnitContext());

      final var phaseListener = new ReferencesPhase1Listener(program, parsedModule);
      final var walker = new ParseTreeWalker();
      walker.walk(phaseListener, source.getCompilationUnitContext());
      listener.accept(new CompilationEvent(thisPhase, parsedModule, source));
    });
  }
}