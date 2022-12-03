package org.ek9lang.compiler.main;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.ek9lang.compiler.errors.CompilationListener;
import org.ek9lang.compiler.files.Workspace;
import org.ek9lang.compiler.main.phases.Ek9Phase0Parsing;
import org.ek9lang.compiler.main.phases.Ek9Phase10CodeGenerationAggregates;
import org.ek9lang.compiler.main.phases.Ek9Phase10CodeGenerationConstants;
import org.ek9lang.compiler.main.phases.Ek9Phase10CodeGenerationFunctions;
import org.ek9lang.compiler.main.phases.Ek9Phase10CodeGenerationPreparation;
import org.ek9lang.compiler.main.phases.Ek9Phase11CodeOptimisation;
import org.ek9lang.compiler.main.phases.Ek9Phase12Packaging;
import org.ek9lang.compiler.main.phases.Ek9Phase12PackagingPostProcessing;
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

/**
 * The main EK9 compiler (HERE FOR COMPILER ENTRY).
 * In parts this will be multithreaded - mainly by using parallel stream.
 * But may use CompletableFutures and when virtual threads are available with Java 19+
 * we should see significant speed improvement in file (IO) operations.
 * I've had a tinker, and it's about 3 times faster.
 * This compiler actually just coordinates all the 'phases' of compilation.
 */
public class Ek9Compiler implements Compiler {

  private final CompilationListener listener;

  private CompilerReporter reporter = new CompilerReporter(false);

  /**
   * Create a new compiler. Configure it with your event listener and flags.
   */
  public Ek9Compiler(CompilationListener listener) {
    AssertValue.checkNotNull("Compilation Listener must be provided", listener);
    this.listener = listener;
  }

  /**
   * HERE for the definitions of each phase of compilation.
   * Provides all the phases of compilation as a list of functions.
   * Will need to add lots more functions in here for the various phases.
   * Clearly the order is important, if we need a new phase. Just define the function in phases
   * and plug it in (in the correct spot).
   */
  private List<BiFunction<Workspace, CompilerFlags, CompilationPhaseResult>>
      getCompilationPhases() {

    //Parsing, symbol definition and basic resolution prior to having an IR.
    var frontEnd = List.of(new Ek9Phase0Parsing(listener, reporter),
        new Ek9Phase1SymbolDefinition(listener, reporter),
        new Ek9Phase1ReferenceChecks(listener, reporter),
        new Ek9Phase1SymbolDuplicationChecks(listener, reporter),
        new Ek9Phase2TemplateDefinitionAndPartialResolution(listener, reporter),
        new Ek9Phase3FurtherSymbolDefinitionResolution(listener, reporter),
        new Ek9Phase4TemplateExpansion(listener, reporter),
        new Ek9Phase5SymbolResolution(listener, reporter),
        new Ek9Phase6PluginResolution(listener, reporter));

    //Yes the notorious 'middle-end' - build the intermediate representation, analyse and optimise.
    var middleEnd = List.of(new Ek9Phase7IRGeneration(listener, reporter),
        new Ek9Phase7ProgramWithIR(listener, reporter),
        new Ek9Phase8IRTemplateGeneration(listener, reporter),
        new Ek9Phase9IRAnalysis(listener, reporter),
        new Ek9Phase9IROptimisation(listener, reporter));

    var backEnd = List.of(new Ek9Phase10CodeGenerationPreparation(listener, reporter),
        new Ek9Phase10CodeGenerationAggregates(listener, reporter),
        new Ek9Phase10CodeGenerationConstants(listener, reporter),
        new Ek9Phase10CodeGenerationFunctions(listener, reporter),
        new Ek9Phase11CodeOptimisation(listener, reporter),
        new Ek9Phase12Packaging(listener, reporter),
        new Ek9Phase12PackagingPostProcessing(listener, reporter));

    return Stream.of(frontEnd, middleEnd, backEnd).flatMap(List::stream).toList();
  }

  /**
   * HERE FOR COMPILER ENTRY.
   * This is THE main entry point in to initiating compilation.
   * It basically just runs through each of the phases configured.
   */
  @Override
  public boolean compile(Workspace workspace, CompilerFlags flags) {
    AssertValue.checkNotNull("Workspace must be provided", workspace);
    AssertValue.checkNotNull("Compiler Flags must be provided", flags);
    reporter = new CompilerReporter(flags.isVerbose());

    //Get all the phases and process in turn, if phase fails then stop (false)
    //If the phase was the final one required, then also stop (but true).
    for (var phase : getCompilationPhases()) {
      long before = System.nanoTime();
      var phaseResult = phase.apply(workspace, flags);
      long after = System.nanoTime();
      reporter.log(String.format("%s duration %d ns", phaseResult.phase(), after - before));
      if (!phaseResult.phaseSuccess()) {
        return false;
      } else if (phaseResult.phaseMatch()) {
        break;
      }
    }
    return true;
  }
}
