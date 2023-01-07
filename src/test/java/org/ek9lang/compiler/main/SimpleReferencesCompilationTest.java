package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Supplier;
import org.ek9lang.compiler.errors.CompilationPhaseListener;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.main.phases.options.FullPhaseSupplier;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.compiler.parsing.WorkSpaceFromResourceDirectoryFiles;
import org.ek9lang.core.threads.SharedThreadContext;
import org.junit.jupiter.api.Test;

/**
 * Just test references all compile and resolve.
 */
class SimpleReferencesCompilationTest {

  private static final Supplier<SharedThreadContext<CompilableProgram>> sharedContext
      = new CompilableProgramSuitable();

  private static final WorkSpaceFromResourceDirectoryFiles workspaceLoader = new WorkSpaceFromResourceDirectoryFiles();
  private static final Workspace ek9Workspace = workspaceLoader.apply("/examples/constructs/references");

  @Test
  void testReferencePhasedDevelopment() {
    //Just start with the basics and most on to the next phase one implemented.
    CompilationPhase upToPhase = CompilationPhase.REFERENCE_CHECKS;

    CompilationPhaseListener listener = (phase, source) -> {
      if (!source.getErrorListener().isErrorFree()) {
        System.out.println("Errors  : " + phase + ", source: " + source.getFileName());
        source.getErrorListener().getErrors().forEachRemaining(System.out::println);
      }
    };
    var sharedCompilableProgram = sharedContext.get();

    FullPhaseSupplier allPhases = new FullPhaseSupplier(sharedCompilableProgram,
        listener, new CompilerReporter(true));

    var compiler = new Ek9Compiler(allPhases);
    assertTrue(compiler.compile(ek9Workspace, new CompilerFlags(upToPhase, true)));

    sharedCompilableProgram.accept(program -> {

      //TODO get and check symbols
      new SymbolCountCheck(2,"net.customer.geometry", 5).test(program);

      new SymbolCountCheck(2, "net.customer.specials", 5).test(program);

      new SymbolCountCheck(1, "net.customer.pair.dev", 1).test(program);

      new SymbolCountCheck(1,"net.customer.pair", 8).test(program);

      new SymbolCountCheck(2, "net.customer.some", 3).test(program);

      new SymbolCountCheck(1,"ekopen.std.incs", 1).test(program);
    });

  }
}
