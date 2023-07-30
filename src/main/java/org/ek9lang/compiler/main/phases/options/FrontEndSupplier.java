package org.ek9lang.compiler.main.phases.options;

import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.errors.CompilationPhaseListener;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.main.phases.Ek9Phase0Parsing;
import org.ek9lang.compiler.main.phases.Ek9Phase1ModuleDuplicateSymbolChecks;
import org.ek9lang.compiler.main.phases.Ek9Phase1ReferenceChecks;
import org.ek9lang.compiler.main.phases.Ek9Phase1SymbolDefinition;
import org.ek9lang.compiler.main.phases.Ek9Phase2NonInferredTypeDefinition;
import org.ek9lang.compiler.main.phases.Ek9Phase2TypeHierarchyChecks;
import org.ek9lang.compiler.main.phases.Ek9Phase3SymbolResolution;
import org.ek9lang.compiler.main.phases.Ek9Phase6PluginResolution;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
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
        new Ek9Phase0Parsing(listener, reporter),
        new Ek9Phase1SymbolDefinition(multiThread, compilableProgramAccess, listener, reporter),
        new Ek9Phase1ModuleDuplicateSymbolChecks(compilableProgramAccess, listener, reporter),
        new Ek9Phase1ReferenceChecks(compilableProgramAccess, listener, reporter),
        new Ek9Phase2NonInferredTypeDefinition(multiThread, compilableProgramAccess, listener, reporter),
        new Ek9Phase2TypeHierarchyChecks(compilableProgramAccess, listener, reporter),
        new Ek9Phase3SymbolResolution(multiThread, compilableProgramAccess, listener, reporter),
        new Ek9Phase6PluginResolution(compilableProgramAccess, listener, reporter));
  }
}
