package org.ek9lang.compiler.main;

import java.util.List;
import java.util.function.Supplier;
import org.ek9lang.compiler.errors.CompilationPhaseListener;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.CompilableSource;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.phases.options.FrontEndOnlySupplier;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.core.exception.CompilerException;
import org.ek9lang.core.threads.SharedThreadContext;

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
   * Create a language boot strap with a set of ek9 language files.
   */
  public Ek9LanguageBootStrap(Supplier<List<CompilableSource>> sourceSupplier, CompilationPhaseListener listener,
                              CompilerReporter reporter) {
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
  private void addBuiltInEk9LanguageModules(SharedThreadContext<CompilableProgram> sharedContext) {

    Ek9Compiler compiler = new Ek9Compiler(new FrontEndOnlySupplier(sharedContext, listener, reporter));
    final var sources = sourceSupplier.get();

    Workspace workspace = new Workspace();
    sources.forEach(workspace::addSource);
    var compilationSuccess = compiler.compile(workspace, new CompilerFlags(false));

    if (!compilationSuccess) {
      displaySources();
      throw new CompilerException("Unable to bootstrap EK9 language, error in " + sources);
    }
  }

  @SuppressWarnings("java:S106")
  private void displaySources() {
    sourceSupplier.get().stream().map(CompilableSource::getSourceAsStringForDebugging).forEach(System.err::println);
  }
}
