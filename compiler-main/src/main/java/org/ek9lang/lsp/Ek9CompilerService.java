package org.ek9lang.lsp;

import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.Ek9Compiler;
import org.ek9lang.compiler.common.CompilationPhaseListener;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.compiler.config.FrontEndSupplier;
import org.ek9lang.core.Logger;

/**
 * Designed to be the bridge between LSP mechanisms and the Ek9Compiler itself.
 */
final class Ek9CompilerService extends Ek9Service {

  //This triggers the loading via introspection of the EK9 builtin types interface.
  private final InitialCompilableProgramSupplier initialCompilableProgramSupplier =
      new InitialCompilableProgramSupplier();

  //We mute errors and switch verbose off - because we do not want anything going to stdout or stderr for lsp use.
  //But when you are debugging - you may with to alter this, to see what is happening, if there are any mismatches
  //in your expectations of what should come back via diagnostics with the lsp server.
  private final CompilerReporter reporter = new CompilerReporter(false, true);

  //We still need a listener to wire through to the reporter
  private final CompilationPhaseListener listener = compilationEvent -> {
    var source = compilationEvent.source();
    if (source.getErrorListener().hasErrors()) {
      source.getErrorListener().getErrors().forEachRemaining(reporter::report);
    }
  };

  public Ek9CompilerService(final Ek9LanguageServer languageServer) {
    super(languageServer);

    //This triggers the loading via introspection of the EK9 builtin types interface.
    //Quite expensive, as this has to use introspection to find the classes, then
    //it has to extract the EK9 interface from that. Then it has to actually compile
    //that interface. So while it does not look like this code actually does anything
    //with the 'sharedProgram'. it actually has done a lot, more importantly the
    //initialCompilableProgramSupplier caches the compilableProgram in its initial state.
    //So subsequent calls to initialCompilableProgramSupplier.get() are very fast.

    //This is so that when we get file events, we can quickly get back to a base shared program
    //Then quickly compile the workspace again. It may be possible to retain parts of code
    //so stop the full recompilation. But let's do the easiest thing first and get something working.

    Logger.debug("About to bootstrap EK9 Builtin Language constructs");
    final var sharedProgram = initialCompilableProgramSupplier.get();
    sharedProgram.accept(program -> {
      final var modules = program.getParsedModules("org.ek9.lang");
      if (modules.isEmpty()) {
        Logger.error("Failed to load org.ek9.lang Builtin Types");
      } else {
        Logger.debug("Bootstrapped EK9 Builtin Language constructs");
        initialCompilableProgramSupplier.get();
        Logger.debug("Checked get is faster second time");
      }
    });

  }

  void recompileWorkSpace() {

    final var sharedCompilableProgram = initialCompilableProgramSupplier.get();
    final var frontEndSupplier = new FrontEndSupplier(sharedCompilableProgram, listener, reporter, true);

    final var compiler = new Ek9Compiler(frontEndSupplier, true);

    compiler.compile(getWorkspace(), getCompilerFlags());

    reportAnyErrors();
  }

  private void reportAnyErrors() {
    getWorkspace()
        .getSources()
        .stream()
        .map(CompilableSource::getErrorListener)
        .forEach(this::reportOnCompiledSource);
  }
}
