package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Supplier;
import java.util.stream.Stream;
import org.ek9lang.compiler.errors.CompilationListener;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.Ek9BuiltinLangSupplier;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.main.phases.options.FullPhaseSupplier;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.compiler.testsupport.PathToSourceFromName;
import org.ek9lang.core.threads.SharedThreadContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * HERE FOR THE PRIMARY INITIAL TESTING OF THE COMPILER END TO END.
 * ALBEIT WITH JUST A SIMPLE HelloWorld.ek9 SOURCE FILE.
 * <p>
 * OK so here it is, the first test of the compiler.
 * Obviously in an agile manner I'll have to build this up in stages.
 * Then eventually I'll need a fuzzer.
 * But first lets just take single source file and cut a thin slice right through to a final outputted application.
 * This will involve all the structures and phases - but they will be minimal and thin.
 * <p>
 * We need to process just this for now.
 * <pre>
 * #!ek9
 * defines module introduction
 *   defines program
 *     HelloWorld()
 *       stdout <- Stdout()
 *       stdout.println("Hello, World")
 * </pre>
 */
class Ek9CompilerTest {

  private static final Supplier<SharedThreadContext<CompilableProgram>> sharedContext = () -> {
    CompilerReporter compilerReporter = new CompilerReporter(false);
    Ek9BuiltinLangSupplier sourceSupplier = new Ek9BuiltinLangSupplier();
    Ek9LanguageBootStrap bootStrap =
        new Ek9LanguageBootStrap(sourceSupplier, (phase, compilableSource) -> {
        }, compilerReporter);

    return bootStrap.get();
  };


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


  @ParameterizedTest
  @MethodSource("allCompilationPhases")
  void testSimpleSuccessfulParsing(final CompilationPhase upToPhase) {
    CompilationListener listener = (phase, source) -> {
    };
    FullPhaseSupplier allPhases = new FullPhaseSupplier(sharedContext.get(),
        listener, new CompilerReporter(true));

    var compiler = new Ek9Compiler(allPhases);
    assertTrue(compiler.compile(validEk9Workspace.get(), new CompilerFlags(upToPhase, true)));
  }

  @Test
  void testSimpleUnSuccessfulParsing() {
    CompilationListener listener = (phase, source) -> {
    };
    FullPhaseSupplier allPhases = new FullPhaseSupplier(sharedContext.get(),
        listener, new CompilerReporter(true));

    var compiler = new Ek9Compiler(allPhases);
    assertFalse(compiler.compile(inValidEk9Workspace.get(), new CompilerFlags()));
  }

  private static Stream<CompilationPhase> allCompilationPhases() {
    return Stream.of(CompilationPhase.values());
  }
}
