package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
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
 * Just test basics all compile.
 */
class BasicsCompilationTest {

  private static final Supplier<SharedThreadContext<CompilableProgram>> sharedContext
      = new CompilableProgramSuitable();

  private static final WorkSpaceFromResourceDirectoryFiles workspaceLoader = new WorkSpaceFromResourceDirectoryFiles();
  private static final Workspace ek9Workspace = workspaceLoader.apply("/examples/basics");

  @Test
  void testReferencePhasedDevelopment() {
    //Just start with the basics and most on to the next phase one implemented.
    CompilationPhase upToPhase = CompilationPhase.REFERENCE_CHECKS;
    var errors = new ArrayList<String>();
    CompilationPhaseListener listener = (phase, source) -> {
      if (!source.getErrorListener().isErrorFree()) {
        var errorMessage = "Errors  : " + phase + ", source: " + source.getFileName();
        errors.add(errorMessage);
        System.out.println(errorMessage);
        source.getErrorListener().getErrors().forEachRemaining(msg ->
        {
          System.out.println(msg);
          errors.add(msg.toString());
        });
      }
    };
    var sharedCompilableProgram = sharedContext.get();

    FullPhaseSupplier allPhases = new FullPhaseSupplier(sharedCompilableProgram,
        listener, new CompilerReporter(true));

    var compiler = new Ek9Compiler(allPhases);
    var compilationResult = compiler.compile(ek9Workspace, new CompilerFlags(upToPhase, true));
    //Seem to get period failures here cannot quite work out why.
    //I had defined HelloWorld in the sma e module name but different files.
    //So this indicates there is an ability to define the same token in a module part.
    //So References phase maybe need to ensure uniqueness.
    if (!compilationResult) {
      sharedCompilableProgram.accept(program -> {
        program.getParsedModuleNames().forEach(moduleName -> {
          var mod = program.getParsedModules(moduleName);
          assertNotNull(mod);
          System.err.println("Module [" + mod + "] [" + mod.size() + "]");
          mod.forEach(loadedModule -> {
            var errorListener = loadedModule.getSource().getErrorListener();
            if (errorListener.hasErrors()) {
              System.err.println("Errors in [" + loadedModule + "]");
              var iter = errorListener.getErrors();
              while (iter.hasNext()) {
                System.err.println(iter.next());
              }
            }
          });
        });

      });
      System.err.println("There are unexpected errors terminating [" + errors.size() + "]");
      errors.forEach(System.err::println);
      System.exit(2);
    }
    assertTrue(compilationResult);

  }
}
