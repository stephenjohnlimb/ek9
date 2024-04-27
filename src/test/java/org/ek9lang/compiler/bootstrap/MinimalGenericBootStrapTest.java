package org.ek9lang.compiler.bootstrap;

import static org.ek9lang.compiler.support.AggregateFactory.EK9_LANG;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.ek9lang.compiler.search.TemplateTypeSymbolSearch;
import org.ek9lang.compiler.search.TypeSymbolSearch;
import org.junit.jupiter.api.Test;

/**
 * Design just to check that it is possible with just one generic Type and one standard type
 * to be able to create a polymorphic parameterized type.
 * I've struggled with this quite a bit and now I'm starting to see what's wrong (I think).
 * So this test is designed to ensure that given a Generic Type all the operations on that type
 * are correctly generated on the new type when it is parameterized and importantly that they can be resolved.
 */
class MinimalGenericBootStrapTest {
  private final CompilerReporter reporter = new CompilerReporter(false, false);
  private final Supplier<CompilationPhaseListener> listener
      = () -> compilationEvent -> {
    var source = compilationEvent.source();
    if (source.getErrorListener().hasErrors()) {
      source.getErrorListener().getErrors().forEachRemaining(reporter::report);
    }
  };

  final Supplier<List<CompilableSource>> sourceSupplier =
      () -> List.of(new CompilableSource(Objects.requireNonNull(getClass().getResource(
          "/examples/bootstrap/minimalGenerics.ek9")).getPath()));

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

    var moduleName = EK9_LANG;
    var scope = program.getParsedModules(moduleName).get(0).getModuleScope();
    assertNotNull(scope);

    assertTrue(scope.resolve(new TypeSymbolSearch("String")).isPresent());
    assertTrue(scope.resolve(new TypeSymbolSearch("Integer")).isPresent());
    assertTrue(scope.resolve(new TypeSymbolSearch("Boolean")).isPresent());

    //Ensure not just a 'type' but known to be a template (generic) type.
    assertFalse(scope.resolve(new TypeSymbolSearch("List")).isPresent());
    assertTrue(scope.resolve(new TemplateTypeSymbolSearch("List")).isPresent());

    //Ensure that Float for example cannot be found as that is not included in the minimal source
    assertFalse(scope.resolve(new TypeSymbolSearch("Float")).isPresent());

  }


}
