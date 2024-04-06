package org.ek9lang.compiler.config;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhaseResult;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.common.CompilationPhaseListener;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.core.SharedThreadContext;

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
  public FullPhaseSupplier(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                           final CompilationPhaseListener listener,
                           final CompilerReporter reporter) {

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
