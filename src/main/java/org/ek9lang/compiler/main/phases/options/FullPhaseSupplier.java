package org.ek9lang.compiler.main.phases.options;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.ek9lang.compiler.errors.CompilationPhaseListener;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.CompilerFlags;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.core.threads.SharedThreadContext;

/**
 * Supplies all the phases for a full compilation from end to end.
 */
public class FullPhaseSupplier extends PhaseSupplier {

  private final FrontEndSupplier frontEndSupplier;
  private final MiddleEndSupplier middleEndSupplier;
  private final BackEndSupplier backEndSupplier;

  /**
   * Create a new supplier of a full set of compiler phases.
   */
  public FullPhaseSupplier(SharedThreadContext<CompilableProgram> compilableProgramAccess,
                           CompilationPhaseListener listener,
                           CompilerReporter reporter) {
    super(compilableProgramAccess, listener, reporter);
    frontEndSupplier = new FrontEndSupplier(compilableProgramAccess, listener, reporter, true);
    middleEndSupplier = new MiddleEndSupplier(compilableProgramAccess, listener, reporter);
    backEndSupplier = new BackEndSupplier(compilableProgramAccess, listener, reporter);
  }

  @Override
  public List<BiFunction<Workspace, CompilerFlags, CompilationPhaseResult>> get() {
    return Stream.of(frontEndSupplier, middleEndSupplier, backEndSupplier)
        .map(Supplier::get)
        .flatMap(List::stream)
        .toList();
  }
}
