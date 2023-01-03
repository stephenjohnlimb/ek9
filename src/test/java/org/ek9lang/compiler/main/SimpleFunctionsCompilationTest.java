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
 * Just test simple functions all compile.
 */
class SimpleFunctionsCompilationTest {

  private static final Supplier<SharedThreadContext<CompilableProgram>> sharedContext
      = new CompilableProgramSuitable();

  private static final WorkSpaceFromResourceDirectoryFiles workspaceLoader = new WorkSpaceFromResourceDirectoryFiles();
  private static final Workspace ek9Workspace = workspaceLoader.apply("/examples/constructs/functions");

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
      //Now this should have some enumerations and records/functions.

      new SymbolCountCheck("test.functions", 2).test(program);

      new SymbolCountCheck("com.customer.just.functions", 24).test(program);

      new SymbolCountCheck("com.customer.just", 5).test(program);

      new SymbolCountCheck("net.customer", 5).test(program);
    });

  }
}
