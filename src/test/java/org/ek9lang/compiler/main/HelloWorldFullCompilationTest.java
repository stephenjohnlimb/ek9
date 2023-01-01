package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Supplier;
import org.ek9lang.compiler.errors.CompilationPhaseListener;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.Workspace;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.main.phases.options.FullPhaseSupplier;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.compiler.testsupport.PathToSourceFromName;
import org.ek9lang.core.threads.SharedThreadContext;
import org.junit.jupiter.api.Test;

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
 * This will be the main test bed to take the hello work EK9 source right through from
 * parsing, all the phases to a program that can be executed.
 * It is the first full end-to-end compilation.
 * After this the rest of the constructs can be added - as these have been prototyped already.
 * But what has not been prototyped is the full set of phases all joined up to produce a final program.
 * So a very thin slice through the EK9 language - just to support 'Hello World'.
 */
class HelloWorldFullCompilationTest {

  private static final Supplier<SharedThreadContext<CompilableProgram>> sharedContext
      = new CompilableProgramSuitable();

  private static final Supplier<Workspace> helloWorldEk9Workspace = () -> {
    Workspace rtn = new Workspace();

    var constantPath = new PathToSourceFromName().apply("/examples/simpleReference/ExternalReference.ek9");
    var programPath = new PathToSourceFromName().apply("/examples/simpleReference/HelloReference.ek9");
    rtn.addSource(constantPath);
    rtn.addSource(programPath);
    return rtn;
  };

  @Test
  void testHelloWorldPhasedDevelopment() {
    //Just start with the basics and most on to the next phase one implemented.
    CompilationPhase upToPhase = CompilationPhase.REFERENCE_CHECKS;

    CompilationPhaseListener listener = (phase, source) -> {
      if (!source.getErrorListener().isErrorFree()) {
        System.out.println("Errors in phase: " + phase);
        source.getErrorListener().getErrors().forEachRemaining(System.out::println);
      }
    };
    var sharedCompilableProgram= sharedContext.get();

    FullPhaseSupplier allPhases = new FullPhaseSupplier(sharedCompilableProgram,
        listener, new CompilerReporter(true));

    var compiler = new Ek9Compiler(allPhases);
    assertTrue(compiler.compile(helloWorldEk9Workspace.get(), new CompilerFlags(upToPhase, true)));

    sharedCompilableProgram.accept(program -> {
      var helloWorldModule = program.getParsedModules("introduction");
      assertNotNull(helloWorldModule);

      var externalReferenceModule = program.getParsedModules("some.external");
      assertNotNull(externalReferenceModule);
      System.out.println("STEVE YOU ARE HERE: Got hello world module");
    });
  }
}