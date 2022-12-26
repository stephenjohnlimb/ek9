package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.ek9lang.compiler.errors.CompilationPhaseListener;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.main.phases.options.FullPhaseSupplier;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.compiler.parsing.SourceFileList;
import org.ek9lang.compiler.testsupport.PathToSourceFromName;
import org.ek9lang.core.threads.SharedThreadContext;
import org.ek9lang.core.utils.Holder;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad references usage.
 */
class BadReferencesFullCompilationTest {


  private static final Supplier<SharedThreadContext<CompilableProgram>> sharedContext
      = new CompilableProgramSuitable();

  private static final Supplier<Workspace> ek9Workspace = () -> {
    final SourceFileList sourceFileList = new SourceFileList();
    Workspace rtn = new Workspace();
    sourceFileList.apply("/examples/parseButFailCompile/multipleReferences")
        .parallelStream()
        .forEach(rtn::addSource);

    return rtn;
  };

  @Test
  void testReferencePhasedDevelopment() {
    //Just start with the basics and most on to the next phase one implemented.
    CompilationPhase upToPhase = CompilationPhase.REFERENCE_CHECKS;

    AtomicInteger counter = new AtomicInteger(0);
    CompilationPhaseListener listener = (phase, source) -> {
      if (!source.getErrorListener().isErrorFree()) {

        System.out.println("Errors  : " + phase + ", source: " + source.getFileName());
        source.getErrorListener().getErrors().forEachRemaining(error -> {
          counter.getAndIncrement();
          System.out.println(error);
        });
      }
    };

    var sharedCompilableProgram= sharedContext.get();

    FullPhaseSupplier allPhases = new FullPhaseSupplier(sharedCompilableProgram,
        listener, new CompilerReporter(true));

    var compiler = new Ek9Compiler(allPhases);
    var compilationResult = compiler.compile(ek9Workspace.get(), new CompilerFlags(upToPhase, true));

    assertEquals(24, counter.get());
    sharedCompilableProgram.accept(program -> {
      var alpha = program.getParsedModules("alpha");
      assertNotNull(alpha);
      var beta = program.getParsedModules("beta");
      assertNotNull(beta);

      var failsToCompile =  program.getParsedModules("fails.to.compile");
      assertNotNull(failsToCompile);
    });
  }
}
