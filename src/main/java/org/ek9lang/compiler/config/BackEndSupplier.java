package org.ek9lang.compiler.config;

import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhaseResult;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.common.CompilationPhaseListener;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.compiler.phase10.CodeGenerationAggregates;
import org.ek9lang.compiler.phase10.CodeGenerationConstants;
import org.ek9lang.compiler.phase10.CodeGenerationFunctions;
import org.ek9lang.compiler.phase10.CodeGenerationPreparation;
import org.ek9lang.compiler.phase11.CodeOptimisation;
import org.ek9lang.compiler.phase12.Packaging;
import org.ek9lang.compiler.phase12.PackagingPostProcessing;
import org.ek9lang.compiler.phase12.PluginLinkage;
import org.ek9lang.core.SharedThreadContext;

/**
 * Supplies the compiler phases for the back end. Everything after IR.
 */
public class BackEndSupplier extends PhaseSupplier {

  /**
   * Create a new supplier of back-end compiler phases.
   */
  public BackEndSupplier(SharedThreadContext<CompilableProgram> compilableProgramAccess,
                         CompilationPhaseListener listener,
                         CompilerReporter reporter) {
    super(compilableProgramAccess, listener, reporter);
  }

  @Override
  public List<BiFunction<Workspace, CompilerFlags, CompilationPhaseResult>> get() {
    return List.of(
        new CodeGenerationPreparation(listener, reporter),
        new CodeGenerationAggregates(listener, reporter),
        new CodeGenerationConstants(listener, reporter),
        new CodeGenerationFunctions(listener, reporter),
        new CodeOptimisation(listener, reporter),
        new PluginLinkage(listener, reporter),
        new Packaging(listener, reporter),
        new PackagingPostProcessing(listener, reporter));
  }
}
