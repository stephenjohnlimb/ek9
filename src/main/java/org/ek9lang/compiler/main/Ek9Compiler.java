package org.ek9lang.compiler.main;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import org.ek9lang.compiler.errors.CompilationListener;
import org.ek9lang.compiler.files.CompilableSource;
import org.ek9lang.compiler.files.Workspace;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.core.exception.AssertValue;

/**
 * The main EK9 compiler.
 * In parts this will be multithreaded - mainly by using parallel stream.
 * But may use CompletableFutures and when virtual threads are available with Java 19+
 * we should see significant speed improvement in file (IO) operations.
 * I've had a tinker, and it's about 3 times faster.
 */
public class Ek9Compiler implements Compiler {

  private final CompilationListener listener;
  private final CompilerFlags flags;

  /**
   * Check if any of the sources have errors.
   */
  private final Predicate<Collection<CompilableSource>> sourceHaveErrors =
      sources -> sources.parallelStream().map(source -> source.getErrorListener().hasErrors())
          .findFirst().orElse(false);

  /**
   * Filter to find the sources than need to be parsed.
   */
  private final UnaryOperator<Collection<CompilableSource>> sourcesToBeParsed =
      sources -> sources.parallelStream()
          .filter(source -> source.hasNotBeenSuccessfullyParsed() || source.isModified())
          .toList();

  /**
   * Create a new compiler. Configure it with your event listener and flags.
   */
  public Ek9Compiler(CompilationListener listener, CompilerFlags flags) {
    AssertValue.checkNotNull("Compilation Listener must be provided", listener);
    AssertValue.checkNotNull("Compilation Flags must be provided", flags);

    this.listener = listener;
    this.flags = flags;
  }

  @Override
  public boolean compile(Workspace workspace) {
    AssertValue.checkNotNull("Workspace must be provided", workspace);

    prepareToParseSources(workspace);

    if (flags.getCompileToPhase() == CompilationPhase.PREPARE_PARSE) {
      return true;
    }

    if (!parseSources(workspace)) {
      return false;
    }
    return true;
  }

  private void prepareToParseSources(Workspace workspace) {
    underTakeParsing(workspace, CompilationPhase.PREPARE_PARSE, CompilableSource::prepareToParse);
  }

  private boolean parseSources(Workspace workspace) {
    return underTakeParsing(workspace, CompilationPhase.PARSING, CompilableSource::completeParsing);
  }

  private boolean underTakeParsing(Workspace workspace, CompilationPhase phase,
                                   UnaryOperator<CompilableSource> operator) {
    var affectedSources = sourcesToBeParsed.apply(workspace.getSources())
        .parallelStream()
        .map(operator).toList();
    informListener(phase, affectedSources);
    return !sourceHaveErrors.test(affectedSources);
  }

  private void informListener(CompilationPhase phase, Collection<CompilableSource> sources) {
    sources.forEach(
        source -> listener.processed(phase, source));
  }
}
