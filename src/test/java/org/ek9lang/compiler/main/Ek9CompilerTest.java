package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Supplier;
import java.util.stream.Stream;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.Workspace;
import org.ek9lang.compiler.errors.CompilationPhaseListener;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.main.phases.options.FullPhaseSupplier;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.compiler.symbols.support.PathToSourceFromName;
import org.ek9lang.core.SharedThreadContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;


/**
 * Checks both valid and invalid parsing. For valid source cycles through each of the compilation phases.
 */
class Ek9CompilerTest {

  private static final Supplier<SharedThreadContext<CompilableProgram>> sharedContext
      = new CompilableProgramSuitable();

  private static final Supplier<Workspace> validEk9Workspace = () -> {
    var fullPath = new PathToSourceFromName().apply("/examples/basics/HelloWorld.ek9");
    Workspace rtn = new Workspace();
    rtn.addSource(fullPath);
    return rtn;
  };

  private static final Supplier<Workspace> inValidEk9Workspace = () -> {
    var fullPath = new PathToSourceFromName().apply("/badExamples/basics/missingModuleKeyWord.ek9");
    Workspace rtn = new Workspace();
    rtn.addSource(fullPath);
    return rtn;
  };

  private static Stream<CompilationPhase> allCompilationPhases() {
    return Stream.of(CompilationPhase.values());
  }

  @ParameterizedTest
  @MethodSource("allCompilationPhases")
  void testSimpleSuccessfulParsing(final CompilationPhase upToPhase) {
    CompilationPhaseListener listener = compilationEvent -> {
      var source = compilationEvent.source();
      var phase = compilationEvent.phase();
      if (source.getErrorListener().hasErrors()) {
        System.out.println("Errors  : " + phase + ", source: " + source.getFileName());
        source.getErrorListener().getErrors().forEachRemaining(System.out::println);
      }
    };

    var sharedCompilableProgram = sharedContext.get();

    FullPhaseSupplier allPhases = new FullPhaseSupplier(sharedCompilableProgram,
        listener, new CompilerReporter(true));

    var compiler = new Ek9Compiler(allPhases);
    var result = compiler.compile(validEk9Workspace.get(), new CompilerFlags(upToPhase, true));
    assertTrue(result);

    sharedCompilableProgram.accept(program -> program.getParsedModuleNames().forEach(System.out::println));
  }

  @Test
  void testSimpleUnSuccessfulParsing() {
    CompilationPhaseListener listener = compilationEvent -> {
    };
    FullPhaseSupplier allPhases = new FullPhaseSupplier(sharedContext.get(),
        listener, new CompilerReporter(true));

    var compiler = new Ek9Compiler(allPhases);
    assertFalse(compiler.compile(inValidEk9Workspace.get(), new CompilerFlags()));
  }
}
