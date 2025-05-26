package org.ek9lang.compiler;

import java.util.List;
import java.util.function.Supplier;
import org.ek9lang.compiler.common.CompilationPhaseListener;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.compiler.config.FrontEndSupplier;
import org.ek9lang.core.CompilerException;
import org.ek9lang.core.SharedThreadContext;

/**
 * Loads the language basics into a Compilable Program.
 * Then this can be used by the compiler to actually compile and manage developers
 * source and parsed modules.
 */
public class Ek9LanguageBootStrap implements Supplier<SharedThreadContext<CompilableProgram>> {

  private final CompilationPhaseListener listener;
  private final CompilerReporter reporter;
  private final Supplier<List<CompilableSource>> sourceSupplier;

  /**
   * Create a language bootstrap with a set of ek9 language files.
   */
  public Ek9LanguageBootStrap(final Supplier<List<CompilableSource>> sourceSupplier,
                              final CompilationPhaseListener listener,
                              final CompilerReporter reporter) {

    this.sourceSupplier = sourceSupplier;
    this.listener = listener;
    this.reporter = reporter;

  }

  /**
   * Provide the access to a compilable program, this is preconfigured with all
   * the ek9 built in symbols, in the appropriate modules.
   */
  public SharedThreadContext<CompilableProgram> get() {

    final var sharedCompilableProgram = new SharedThreadContext<>(new CompilableProgram());
    addBuiltInEk9LanguageModules(sharedCompilableProgram);

    return sharedCompilableProgram;
  }

  //This needs more of the compiler phases!
  private void addBuiltInEk9LanguageModules(final SharedThreadContext<CompilableProgram> sharedContext) {

    final var supplier = new FrontEndSupplier(sharedContext, listener, reporter, false);
    final var compiler = new Ek9Compiler(supplier, reporter.isMuteReportedErrors());
    final var sources = sourceSupplier.get();
    final var workspace = new Workspace();

    sources.forEach(workspace::addSource);

    //Set compiler flags to only go as far as plugin resolution.
    final var compilationSuccess =
        compiler.compile(workspace, new CompilerFlags(CompilationPhase.PLUGIN_RESOLUTION, reporter.isVerbose()));
    if (!compilationSuccess) {
      displayErrors(workspace);
      reporter.report("Source code follows");
      displaySources(workspace);
      throw new CompilerException("Unable to bootstrap EK9 language, error in " + sources);
    }

  }

  private void displayErrors(final Workspace workspace) {
    workspace.getSources().stream().map(CompilableSource::getErrorListener).forEach(errorListener -> {
      if (errorListener.hasErrors()) {
        reporter.report("Code errors detected");
      } else {
        reporter.report("No code errors detected");
      }
      if (errorListener.hasDirectiveErrors()) {
        reporter.report("Directive errors detected");
        errorListener.getDirectiveErrors().forEachRemaining(reporter::report);
      } else {
        reporter.report("No directive errors detected");
      }
    });
  }

  @SuppressWarnings("java:S106")
  private void displaySources(final Workspace workspace) {

    workspace.getSources().stream().map(CompilableSource::getSourceAsStringForDebugging).forEach(reporter::report);

  }
}
