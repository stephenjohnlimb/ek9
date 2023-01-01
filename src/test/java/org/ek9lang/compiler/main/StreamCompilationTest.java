package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Supplier;
import org.ek9lang.compiler.errors.CompilationPhaseListener;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.main.phases.options.FullPhaseSupplier;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.compiler.parsing.SourceFileList;
import org.ek9lang.core.threads.SharedThreadContext;
import org.junit.jupiter.api.Test;

/**
 * Just test streams all compile.
 */
class StreamCompilationTest {

  private static final Supplier<SharedThreadContext<CompilableProgram>> sharedContext
      = new CompilableProgramSuitable();

  private static final Supplier<Workspace> ek9Workspace = () -> {
    final SourceFileList sourceFileList = new SourceFileList();
    Workspace rtn = new Workspace();
    sourceFileList.apply("/examples/streams")
        .stream()
        .forEach(rtn::addSource);

    return rtn;
  };

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
    assertTrue(compiler.compile(ek9Workspace.get(), new CompilerFlags(upToPhase, true)));

    sharedCompilableProgram.accept(program -> {
      //Now this should have some constructs.

      new SymbolCountCheck("com.customer.justcat", 5).test(program);

      new SymbolCountCheck("com.customer.justparagraphs", 3).test(program);

      new SymbolCountCheck("com.customer.justmoney", 1).test(program);

      new SymbolCountCheck("ekopen.io.file.examples", 1).test(program);

      new SymbolCountCheck("com.customer.books", 39).test(program);

      new SymbolCountCheck("com.customer.streams.collectas", 2).test(program);

      new SymbolCountCheck("com.customer.streams.splitter", 7).test(program);

    });
  }
}
