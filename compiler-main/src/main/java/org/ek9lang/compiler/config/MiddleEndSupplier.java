package org.ek9lang.compiler.config;

import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhaseResult;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.common.CompilationPhaseListener;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.compiler.phase7.IRGenerator;
import org.ek9lang.compiler.phase9.IRAnalysis;
import org.ek9lang.compiler.phase9.IROptimisation;
import org.ek9lang.core.SharedThreadContext;

/**
 * Supplies the middle end phases for compilation..
 */
public class MiddleEndSupplier extends PhaseSupplier {

  /**
   * Create a new supplier of a middle-end set of compiler phases.
   */
  public MiddleEndSupplier(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                           final CompilationPhaseListener listener,
                           final CompilerReporter reporter) {

    super(compilableProgramAccess, listener, reporter);

  }

  @Override
  public List<BiFunction<Workspace, CompilerFlags, CompilationPhaseResult>> get() {

    //Yes the notorious 'middle-end' - build the intermediate representation, analyse and optimise.
    return List.of(
        new IRGenerator(compilableProgramAccess, listener, reporter),
        new IRAnalysis(compilableProgramAccess, listener, reporter),
        new IROptimisation(compilableProgramAccess, listener, reporter));
  }
}
