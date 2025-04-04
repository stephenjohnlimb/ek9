package org.ek9lang.compiler.phase0;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.CompilationPhaseResult;
import org.ek9lang.compiler.CompilerFlags;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.common.CompilableSourceHasErrors;
import org.ek9lang.compiler.common.CompilationEvent;
import org.ek9lang.compiler.common.CompilerReporter;

/**
 * Can be MULTI THREADED.
 * This class just deals with the parsing phase of compilation.
 * It is NOT a parser itself, but is used by the Ek9Compiler to initiate the
 * parsing. The Lexer and Parser are held in the CompilableSource.
 */
public final class Parsing
    implements BiFunction<Workspace, CompilerFlags, CompilationPhaseResult> {

  private static final CompilationPhase thisPhase = CompilationPhase.PARSING;
  private final Consumer<CompilationEvent> listener;
  private final CompilerReporter reporter;
  private final CompilableSourceHasErrors sourceHasErrors = new CompilableSourceHasErrors();
  private final UnaryOperator<Collection<CompilableSource>> sourcesToBeParsed
      = compilableSources -> compilableSources.stream()
      .filter(source -> source.hasNotBeenSuccessfullyParsed() || source.isModified())
      .toList();

  public Parsing(Consumer<CompilationEvent> listener, CompilerReporter reporter) {
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

    reporter.log(thisPhase);
    final var result = underTakeParsingOperation(workspace, CompilableSource::prepareToParse);
    final var compilationPhase = compilerFlags.getCompileToPhase();
    final var phaseMatch = compilationPhase == thisPhase;
    return new CompilationPhaseResult(thisPhase, result,  phaseMatch);
  }

  private CompilationPhaseResult parseSources(Workspace workspace, CompilerFlags compilerFlags) {

    reporter.log(thisPhase);
    final var result = underTakeParsingOperation(workspace, CompilableSource::completeParsing);

    return new CompilationPhaseResult(thisPhase, result, compilerFlags.getCompileToPhase() == thisPhase);
  }

  private boolean underTakeParsingOperation(Workspace workspace,
                                            UnaryOperator<CompilableSource> operator) {

    final var affectedSources = sourcesToBeParsed
        .apply(workspace.getSources())
        .parallelStream()
        .map(operator).toList();
    affectedSources.forEach(source -> listener.accept(new CompilationEvent(thisPhase, null, source)));

    return !sourceHasErrors.test(affectedSources);
  }
}
