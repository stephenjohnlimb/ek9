package org.ek9lang.compiler.main.phases.options;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.ek9lang.compiler.errors.CompilationListener;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.CompilerFlags;
import org.ek9lang.compiler.main.phases.Ek9Phase0Parsing;
import org.ek9lang.compiler.main.phases.Ek9Phase10CodeGenerationAggregates;
import org.ek9lang.compiler.main.phases.Ek9Phase10CodeGenerationConstants;
import org.ek9lang.compiler.main.phases.Ek9Phase10CodeGenerationFunctions;
import org.ek9lang.compiler.main.phases.Ek9Phase10CodeGenerationPreparation;
import org.ek9lang.compiler.main.phases.Ek9Phase11CodeOptimisation;
import org.ek9lang.compiler.main.phases.Ek9Phase12Packaging;
import org.ek9lang.compiler.main.phases.Ek9Phase12PackagingPostProcessing;
import org.ek9lang.compiler.main.phases.Ek9Phase12PluginLinkage;
import org.ek9lang.compiler.main.phases.Ek9Phase1ReferenceChecks;
import org.ek9lang.compiler.main.phases.Ek9Phase1SymbolDefinition;
import org.ek9lang.compiler.main.phases.Ek9Phase1SymbolDuplicationChecks;
import org.ek9lang.compiler.main.phases.Ek9Phase2TemplateDefinitionAndPartialResolution;
import org.ek9lang.compiler.main.phases.Ek9Phase3FurtherSymbolDefinitionResolution;
import org.ek9lang.compiler.main.phases.Ek9Phase4TemplateExpansion;
import org.ek9lang.compiler.main.phases.Ek9Phase5SymbolResolution;
import org.ek9lang.compiler.main.phases.Ek9Phase6PluginResolution;
import org.ek9lang.compiler.main.phases.Ek9Phase7IRGeneration;
import org.ek9lang.compiler.main.phases.Ek9Phase7ProgramWithIR;
import org.ek9lang.compiler.main.phases.Ek9Phase8IRTemplateGeneration;
import org.ek9lang.compiler.main.phases.Ek9Phase9IRAnalysis;
import org.ek9lang.compiler.main.phases.Ek9Phase9IROptimisation;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.core.exception.AssertValue;
import org.ek9lang.core.threads.SharedThreadContext;

/**
 * Supplies all the phases for a full compilation from end to end.
 */
public class FullPhaseSupplier implements Supplier<List<BiFunction<Workspace, CompilerFlags, CompilationPhaseResult>>> {

  private final CompilationListener listener;
  private final SharedThreadContext<CompilableProgram> compilableProgramAccess;
  private final CompilerReporter reporter;

  /**
   * Create a new supplier of a full set of compiler phases.
   */
  public FullPhaseSupplier(SharedThreadContext<CompilableProgram> compilableProgramAccess,
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
    var frontEnd = List.of(
        new Ek9Phase0Parsing(listener, reporter),
        new Ek9Phase1SymbolDefinition(compilableProgramAccess, listener, reporter),
        new Ek9Phase1ReferenceChecks(compilableProgramAccess, listener, reporter),
        new Ek9Phase1SymbolDuplicationChecks(compilableProgramAccess, listener, reporter),
        new Ek9Phase2TemplateDefinitionAndPartialResolution(compilableProgramAccess, listener, reporter),
        new Ek9Phase3FurtherSymbolDefinitionResolution(compilableProgramAccess, listener, reporter),
        new Ek9Phase4TemplateExpansion(compilableProgramAccess, listener, reporter),
        new Ek9Phase5SymbolResolution(compilableProgramAccess, listener, reporter),
        new Ek9Phase6PluginResolution(compilableProgramAccess, listener, reporter));

    //Yes the notorious 'middle-end' - build the intermediate representation, analyse and optimise.
    var middleEnd = List.of(
        new Ek9Phase7IRGeneration(listener, reporter),
        new Ek9Phase7ProgramWithIR(listener, reporter),
        new Ek9Phase8IRTemplateGeneration(listener, reporter),
        new Ek9Phase9IRAnalysis(listener, reporter),
        new Ek9Phase9IROptimisation(listener, reporter));

    var backEnd = List.of(
        new Ek9Phase10CodeGenerationPreparation(listener, reporter),
        new Ek9Phase10CodeGenerationAggregates(listener, reporter),
        new Ek9Phase10CodeGenerationConstants(listener, reporter),
        new Ek9Phase10CodeGenerationFunctions(listener, reporter),
        new Ek9Phase11CodeOptimisation(listener, reporter),
        new Ek9Phase12PluginLinkage(listener, reporter),
        new Ek9Phase12Packaging(listener, reporter),
        new Ek9Phase12PackagingPostProcessing(listener, reporter));

    return Stream.of(frontEnd, middleEnd, backEnd).flatMap(List::stream).toList();
  }
}
