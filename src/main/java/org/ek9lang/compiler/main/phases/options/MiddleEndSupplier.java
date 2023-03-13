package org.ek9lang.compiler.main.phases.options;

import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.compiler.errors.CompilationPhaseListener;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.CompilerFlags;
import org.ek9lang.compiler.main.phases.Ek9Phase7IRGeneration;
import org.ek9lang.compiler.main.phases.Ek9Phase7ProgramWithIR;
import org.ek9lang.compiler.main.phases.Ek9Phase8IRTemplateGeneration;
import org.ek9lang.compiler.main.phases.Ek9Phase9IRAnalysis;
import org.ek9lang.compiler.main.phases.Ek9Phase9IROptimisation;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.core.threads.SharedThreadContext;

/**
 * Supplies the middle end phases for compilation..
 */
public class MiddleEndSupplier extends PhaseSupplier {


  /**
   * Create a new supplier of a middle-end set of compiler phases.
   */
  public MiddleEndSupplier(SharedThreadContext<CompilableProgram> compilableProgramAccess,
                           CompilationPhaseListener listener,
                           CompilerReporter reporter) {
    super(compilableProgramAccess, listener, reporter);
  }

  @Override
  public List<BiFunction<Workspace, CompilerFlags, CompilationPhaseResult>> get() {

    //Yes the notorious 'middle-end' - build the intermediate representation, analyse and optimise.
    return List.of(
        new Ek9Phase7IRGeneration(listener, reporter),
        new Ek9Phase7ProgramWithIR(listener, reporter),
        new Ek9Phase8IRTemplateGeneration(listener, reporter),
        new Ek9Phase9IRAnalysis(listener, reporter),
        new Ek9Phase9IROptimisation(listener, reporter));
  }
}
