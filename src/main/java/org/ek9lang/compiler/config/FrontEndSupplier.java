package org.ek9lang.compiler.config;

import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhaseResult;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.common.CompilationPhaseListener;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.compiler.phase0.Parsing;
import org.ek9lang.compiler.phase1.ModuleDuplicateSymbolChecks;
import org.ek9lang.compiler.phase1.ReferenceChecks;
import org.ek9lang.compiler.phase1.SymbolDefinition;
import org.ek9lang.compiler.phase2.NonInferredTypeDefinition;
import org.ek9lang.compiler.phase2.TypeHierarchyChecks;
import org.ek9lang.compiler.phase3.SymbolResolution;
import org.ek9lang.compiler.phase4.PostSymbolResolutionChecks;
import org.ek9lang.compiler.phase5.PreIntermediateRepresentationChecks;
import org.ek9lang.compiler.phase6.PluginResolution;
import org.ek9lang.core.SharedThreadContext;

/**
 * Supplies just the front end of the compiler.
 * This tends to be used for just the bootstrap process for the builtin and maybe
 * pluggable modules that are defined externally.
 * Could be used in lsp, or we could just create another supplier and the appropriate list of phases.
 */
public class FrontEndSupplier extends PhaseSupplier {


  private final boolean multiThread;

  /**
   * Create a new supplier of front end only compiler phases.
   */
  public FrontEndSupplier(SharedThreadContext<CompilableProgram> compilableProgramAccess,
                          CompilationPhaseListener listener,
                          CompilerReporter reporter, boolean multiThread) {

    super(compilableProgramAccess, listener, reporter);
    this.multiThread = multiThread;
  }

  @Override
  public List<BiFunction<Workspace, CompilerFlags, CompilationPhaseResult>> get() {
    return List.of(
        new Parsing(listener, reporter),
        new SymbolDefinition(multiThread, compilableProgramAccess, listener, reporter),
        new ModuleDuplicateSymbolChecks(compilableProgramAccess, listener, reporter),
        new ReferenceChecks(compilableProgramAccess, listener, reporter),
        new NonInferredTypeDefinition(multiThread, compilableProgramAccess, listener, reporter),
        new TypeHierarchyChecks(compilableProgramAccess, listener, reporter),
        new SymbolResolution(multiThread, compilableProgramAccess, listener, reporter),
        new PostSymbolResolutionChecks(compilableProgramAccess, listener, reporter),
        new PreIntermediateRepresentationChecks(compilableProgramAccess, listener, reporter),
        new PluginResolution(compilableProgramAccess, listener, reporter));
  }
}
