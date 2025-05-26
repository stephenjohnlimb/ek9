package org.ek9lang.compiler.bootstrap;

import static org.ek9lang.compiler.support.AggregateManipulator.EK9_LANG;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.Ek9LanguageBootStrap;
import org.ek9lang.compiler.common.CompilationPhaseListener;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.compiler.search.TypeSymbolSearch;
import org.junit.jupiter.api.Test;

/**
 * To be used with debugging-org-ek9-lang.ek9 (add what you need in here and delete other stuff)
 * TO see what happens in specific scenarios - normally when a defect has been found elsewhere and,
 * you need a really cut down language to just problem those parts.
 */
class DebuggingBootStrapTest {

  //Note need to use this name so that ek9 types can be used in compiler.
  //As not actually loading the basics of EK9 source.
  final Supplier<List<CompilableSource>> sourceSupplier =
      () -> List.of(new CompilableSource(Objects.requireNonNull(getClass().getResource(
          "/examples/bootstrap/debugging-org-ek9-lang.ek9")).getPath()));
  private final CompilerReporter reporter = new CompilerReporter(false, true);
  private final Supplier<CompilationPhaseListener> listener
      = () -> compilationEvent -> {
    var source = compilationEvent.source();
    if (source.getErrorListener().hasErrors()) {
      source.getErrorListener().getErrors().forEachRemaining(reporter::report);
    }
  };

  @Test
  void basicMinimalBootStrap() {
    final var underTest = new Ek9LanguageBootStrap(sourceSupplier, listener.get(), reporter);
    final var sharedContext = underTest.get();

    //Precondition before we even start to try and really test anything.
    sharedContext.accept(this::assertMinimalEk9);

  }

  /**
   * Asserts that the types and template types defined in the minimal source have been
   * compiled and exist inside the minimal EK9 module scope.
   */
  private void assertMinimalEk9(final CompilableProgram program) {

    var scope = program.getParsedModules(EK9_LANG).get(0).getModuleScope();
    assertNotNull(scope);

    assertTrue(scope.resolve(new TypeSymbolSearch("String")).isPresent());
    assertTrue(scope.resolve(new TypeSymbolSearch("Integer")).isPresent());
    assertTrue(scope.resolve(new TypeSymbolSearch("Boolean")).isPresent());

  }


}
