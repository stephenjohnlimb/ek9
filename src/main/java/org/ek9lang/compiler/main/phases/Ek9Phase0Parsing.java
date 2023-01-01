package org.ek9lang.compiler.main.phases;

import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import org.ek9lang.compiler.errors.CompilationPhaseListener;
import org.ek9lang.compiler.internals.CompilableSource;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.CompilerFlags;
import org.ek9lang.compiler.main.phases.result.CompilableSourceErrorCheck;
import org.ek9lang.compiler.main.phases.result.CompilationPhaseResult;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;

/**
 * Can be MULTI THREADED.
 * This class just deals with the parsing phase of compilation.
 * It is NOT a parser itself, but is used by the Ek9Compiler to initiate the
 * parsing.
 */
public class Ek9Phase0Parsing
    implements BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {

  private final CompilationPhaseListener listener;
  private final CompilerReporter reporter;
  private final CompilableSourceErrorCheck sourceHaveErrors = new CompilableSourceErrorCheck();
  private final RequiredCompilableSourcesForParsing sourcesToBeParsed =
      new RequiredCompilableSourcesForParsing();

  public Ek9Phase0Parsing(CompilationPhaseListener listener, CompilerReporter reporter) {
    this.listener = listener;
    this.reporter = reporter;
  }

  @Override
  public CompilationPhaseResult apply(Workspace workspace, CompilerFlags compilerFlags) {
    final var phaseResult = prepare(workspace, compilerFlags);
    if (!phaseResult.phaseSuccess() || phaseResult.phaseMatch()) {
      return phaseResult;
    }
    return parseSources(workspace, compilerFlags);
  }

  private CompilationPhaseResult prepare(Workspace workspace, CompilerFlags compilerFlags) {
    final var thisPhase = CompilationPhase.PREPARE_PARSE;
    reporter.log(thisPhase);
    final var result = underTakeParsing(workspace, thisPhase, CompilableSource::prepareToParse);

    return new CompilationPhaseResult(thisPhase, result,
        compilerFlags.getCompileToPhase() == thisPhase);
  }

  private CompilationPhaseResult parseSources(Workspace workspace, CompilerFlags compilerFlags) {
    final var thisPhase = CompilationPhase.PARSING;
    reporter.log(thisPhase);
    final var result = underTakeParsing(workspace, thisPhase, CompilableSource::completeParsing);

    return new CompilationPhaseResult(thisPhase, result,
        compilerFlags.getCompileToPhase() == thisPhase);
  }

  private boolean underTakeParsing(Workspace workspace, CompilationPhase phase,
                                   UnaryOperator<CompilableSource> operator) {
    //May consider moving to Executor model
    final var affectedSources = sourcesToBeParsed
        .apply(workspace.getSources())
        .stream()
        .map(operator).toList();

    affectedSources.forEach(source -> listener.accept(phase, source));
    return !sourceHaveErrors.test(affectedSources);
  }
}