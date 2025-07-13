package org.ek9lang.compiler.bootstrap;

import static org.ek9lang.compiler.support.AggregateManipulator.EK9_LANG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Supplier;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.Ek9BuiltinIntrospectionSupplier;
import org.ek9lang.compiler.Ek9LanguageBootStrap;
import org.ek9lang.compiler.common.CompilationPhaseListener;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.compiler.support.SimpleResolverForTesting;
import org.junit.jupiter.api.Test;

/**
 * Uses the Java introspection supplier to get the source 'extern' interface.
 * If this fails, use Ek9BuiltinIntrospectionSupplierTest and check the actual source code generated.
 * Because internal built-in source is supplied as an inputStream it get consumed.
 */
class Ek9IntrospectedBootStrapTest {

  private final Ek9BuiltinIntrospectionSupplier sourceSupplier = new Ek9BuiltinIntrospectionSupplier();
  //If you need to see the errors here for debugging just alter the muteReportedError flag.
  private final CompilerReporter reporter = new CompilerReporter(false, false);
  private final Supplier<CompilationPhaseListener> listener
      = () -> compilationEvent -> {
    var source = compilationEvent.source();
    if (source.getErrorListener().hasErrors()) {
      source.getErrorListener().getErrors().forEachRemaining(reporter::report);
    }
  };

  @Test
  void testBasicBootStrap() {

    final var underTest = new Ek9LanguageBootStrap(sourceSupplier, listener.get(), reporter);

    final var sharedContext = underTest.get();

    sharedContext.accept(compilableProgram
        -> assertEquals(1, compilableProgram.getParsedModules(EK9_LANG).size()));

    sharedContext.accept(this::assertEk9);

  }

  private void assertEk9(final CompilableProgram program) {

    var scope = program.getParsedModules(EK9_LANG).getFirst().getModuleScope();
    var resolver = new SimpleResolverForTesting(scope, true);
    var resolved = resolver.apply("Boolean");
    assertTrue(resolved.isPresent());
  }
}
