package org.ek9lang.compiler.bootstrap;

import static org.ek9lang.compiler.support.EK9TypeNames.EK9_LANG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.Supplier;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilableSource;
import org.ek9lang.compiler.Ek9BuiltinIntrospectionSupplier;
import org.ek9lang.compiler.Ek9LanguageBootStrap;
import org.ek9lang.compiler.common.CompilationPhaseListener;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.compiler.support.SimpleResolverForTesting;
import org.ek9lang.core.CompilerException;
import org.junit.jupiter.api.Test;

/**
 * Uses the Java introspection supplier to get the source 'extern' interface.
 * If this fails, the Java code is introspected again and displayed.
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

    try {
      final var sharedContext = underTest.get();

      sharedContext.accept(compilableProgram
          -> assertEquals(1, compilableProgram.getParsedModules(EK9_LANG).size()));

      sharedContext.accept(this::assertEk9);
    } catch (CompilerException _) {
      //Now we have reload the introspected classes again, so we can display them.
      final var sources = new Ek9BuiltinIntrospectionSupplier().get();
      sources.stream().map(CompilableSource::getSourceAsStringForDebugging).forEach(System.out::println);
      fail("Unable to load introspected Java->EK9 interface definition, check reported error and look for that line.");
    }

  }

  private void assertEk9(final CompilableProgram program) {

    var scope = program.getParsedModules(EK9_LANG).getFirst().getModuleScope();
    var resolver = new SimpleResolverForTesting(scope, true);
    var resolved = resolver.apply("Boolean");
    assertTrue(resolved.isPresent());
  }
}
