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
import org.ek9lang.compiler.phase10.CodeGenerationPreparation;
import org.ek9lang.compiler.phase11.CodeOptimisation;
import org.ek9lang.compiler.phase12.Packaging;
import org.ek9lang.compiler.phase12.PackagingPostProcessing;
import org.ek9lang.compiler.phase12.PluginLinkage;
import org.ek9lang.core.FileHandling;
import org.ek9lang.core.SharedThreadContext;

/**
 * Supplies the compiler phases for the back end. Everything after IR.
 */
public class BackEndSupplier extends PhaseSupplier {

  private final FileHandling fileHandling;

  /**
   * Create a new supplier of back-end compiler phases.
   */
  public BackEndSupplier(final SharedThreadContext<CompilableProgram> compilableProgramAccess,
                         final FileHandling fileHandling,
                         final CompilationPhaseListener listener,
                         final CompilerReporter reporter) {

    super(compilableProgramAccess, listener, reporter);
    this.fileHandling = fileHandling;
  }

  @Override
  public List<BiFunction<Workspace, CompilerFlags, CompilationPhaseResult>> get() {

    return List.of(
        new CodeGenerationPreparation(compilableProgramAccess, fileHandling, listener, reporter),
        new CodeGenerationAggregates(compilableProgramAccess, fileHandling, listener, reporter),
        new CodeGenerationConstants(compilableProgramAccess, fileHandling, listener, reporter),
        new CodeOptimisation(compilableProgramAccess, fileHandling, listener, reporter),
        new PluginLinkage(compilableProgramAccess, fileHandling, listener, reporter),
        new Packaging(compilableProgramAccess, fileHandling, listener, reporter),
        new PackagingPostProcessing(compilableProgramAccess, listener, reporter));
  }
}
