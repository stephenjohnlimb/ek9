package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.function.Supplier;
import org.ek9lang.compiler.errors.CompilationListener;
import org.ek9lang.compiler.internals.CompilableSource;
import org.ek9lang.compiler.internals.Ek9BuiltinLangSupplier;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.core.exception.CompilerException;
import org.junit.jupiter.api.Test;

class Ek9BootStrapTest {

  private final Supplier<CompilationListener> listener
      = () -> (phase, source) -> {
    if (source.getErrorListener().hasErrors()) {
      source.getErrorListener().getErrors().forEachRemaining(System.err::println);
    }
  };

  @Test
  void testBasicBootStrap() {

    final var sourceSupplier = new Ek9BuiltinLangSupplier();
    final var underTest = new Ek9LanguageBootStrap(sourceSupplier, listener.get(), new CompilerReporter(true));

    final var sharedContext = underTest.get();

    sharedContext.accept(compilableProgram -> {
      assertEquals(1, compilableProgram.getParsedModules("org.ek9.lang").size());
    });
  }

  @Test
  void testBadBootstrapSource() {
    final Supplier<List<CompilableSource>> sourceSupplier =
        () -> List.of(new CompilableSource(getClass().getResource(
            "/examples/parseButFailCompile/builtin/badBuiltin.ek9").getPath()));

    final var underTest = new Ek9LanguageBootStrap(sourceSupplier, listener.get(), new CompilerReporter(true));

    assertThrows(CompilerException.class, () -> underTest.get());
  }
}
