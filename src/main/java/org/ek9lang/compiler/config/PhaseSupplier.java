package org.ek9lang.compiler.config;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhaseResult;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.CompilationPhaseListener;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.compiler.directives.DirectiveListenerSupplier;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.SharedThreadContext;

/**
 * Abstract concept of a supplier of phases for a compilation.
 */
public abstract class PhaseSupplier
    implements Supplier<List<BiFunction<Workspace, CompilerFlags, CompilationPhaseResult>>> {

  protected final Consumer<CompilationEvent> listener;
  protected final SharedThreadContext<CompilableProgram> compilableProgramAccess;
  protected final CompilerReporter reporter;

  /**
   * Create a new supplier of a full set of compiler phases.
   */
  protected PhaseSupplier(SharedThreadContext<CompilableProgram> compilableProgramAccess,
                          CompilationPhaseListener listener,
                          CompilerReporter reporter) {
    AssertValue.checkNotNull("CompilableProgramAccess Listener must be provided", compilableProgramAccess);
    AssertValue.checkNotNull("Compilation Listener must be provided", listener);
    AssertValue.checkNotNull("Compilation Reporter must be provided", reporter);

    this.listener = new DirectiveListenerSupplier().get().andThen(listener);
    this.compilableProgramAccess = compilableProgramAccess;
    this.reporter = reporter;
  }
}
