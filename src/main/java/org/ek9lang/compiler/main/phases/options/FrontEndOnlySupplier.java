package org.ek9lang.compiler.main.phases.options;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.ek9lang.compiler.errors.CompilationListener;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.CompilerFlags;
import org.ek9lang.compiler.main.phases.Ek9Phase0Parsing;
import org.ek9lang.compiler.main.phases.Ek9Phase1ReferenceChecks;
import org.ek9lang.compiler.main.phases.Ek9Phase1SymbolDefinition;
import org.ek9lang.compiler.main.phases.Ek9Phase1SymbolDuplicationChecks;
import org.ek9lang.compiler.main.phases.Ek9Phase2TemplateDefinitionAndPartialResolution;
import org.ek9lang.compiler.main.phases.Ek9Phase3FurtherSymbolDefinitionResolution;
import org.ek9lang.compiler.main.phases.Ek9Phase4TemplateExpansion;
import org.ek9lang.compiler.main.phases.Ek9Phase5SymbolResolution;
import org.ek9lang.compiler.main.phases.Ek9Phase6PluginResolution;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.threads.SharedThreadContext;

/**
 * Supplies just the front end of the compiler.
 * This tends to be used for just the bootstrap process for the builtin and maybe
 * pluggable modules that are defined externally.
 */
public class FrontEndOnlySupplier
    implements Supplier<List<BiFunction<Workspace, CompilerFlags, CompilationPhaseResult>>> {

  private final CompilationListener listener;
  private final SharedThreadContext<CompilableProgram> compilableProgramAccess;
  private final CompilerReporter reporter;

  /**
   * Create a new supplier of front end only compiler phases.
   */
  public FrontEndOnlySupplier(SharedThreadContext<CompilableProgram> compilableProgramAccess,
                              CompilationListener listener,
                              CompilerReporter reporter) {
    AssertValue.checkNotNull("CompilableProgramAccess Listener must be provided", compilableProgramAccess);
    AssertValue.checkNotNull("Compilation Listener must be provided", listener);
    AssertValue.checkNotNull("Compilation Reporter must be provided", reporter);
    this.compilableProgramAccess = compilableProgramAccess;
    this.listener = listener;
    this.reporter = reporter;
  }

  @Override
  public List<BiFunction<Workspace, CompilerFlags, CompilationPhaseResult>> get() {
    //Parsing, symbol definition and basic resolution prior to having an IR.

    //This is duplicated by design at this point because I may need to vary the functionality.
    return List.of(
        new Ek9Phase0Parsing(listener, reporter),
        new Ek9Phase1SymbolDefinition(false, compilableProgramAccess, listener, reporter),
        new Ek9Phase1ReferenceChecks(compilableProgramAccess, listener, reporter),
        new Ek9Phase1SymbolDuplicationChecks(compilableProgramAccess, listener, reporter),
        new Ek9Phase2TemplateDefinitionAndPartialResolution(compilableProgramAccess, listener, reporter),
        new Ek9Phase3FurtherSymbolDefinitionResolution(compilableProgramAccess, listener, reporter),
        new Ek9Phase4TemplateExpansion(compilableProgramAccess, listener, reporter),
        new Ek9Phase5SymbolResolution(compilableProgramAccess, listener, reporter),
        new Ek9Phase6PluginResolution(compilableProgramAccess, listener, reporter));
  }
}
