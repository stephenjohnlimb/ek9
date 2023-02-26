package org.ek9lang.compiler.main;

import static org.ek9lang.compiler.internals.Ek9BuiltinLangSupplier.NUMBER_OF_EK9_SYMBOLS;
import static org.ek9lang.compiler.symbol.support.AggregateFactory.EK9_LANG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.function.Supplier;
import org.ek9lang.compiler.errors.CompilationPhaseListener;
import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.internals.CompilableSource;
import org.ek9lang.compiler.internals.Ek9BuiltinLangSupplier;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.compiler.symbol.IAggregateSymbol;
import org.ek9lang.compiler.symbol.support.SimpleResolverForTesting;
import org.ek9lang.compiler.symbol.support.SymbolCountCheck;
import org.ek9lang.compiler.symbol.support.search.AnySymbolSearch;
import org.ek9lang.core.exception.CompilerException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class Ek9BootStrapTest {
  final Ek9BuiltinLangSupplier sourceSupplier = new Ek9BuiltinLangSupplier();

  private final Supplier<CompilationPhaseListener> listener
      = () -> (phase, source) -> {
    if (source.getErrorListener().hasErrors()) {
      source.getErrorListener().getErrors().forEachRemaining(System.err::println);
    }
  };

  @Test
  void testBasicBootStrap() {

    final var underTest = new Ek9LanguageBootStrap(sourceSupplier, listener.get(), new CompilerReporter(true));

    final var sharedContext = underTest.get();

    sharedContext.accept(compilableProgram
        -> assertEquals(1, compilableProgram.getParsedModules(EK9_LANG).size()));

    sharedContext.accept(compilableProgram -> assertEk9(compilableProgram));
  }

  private void assertEk9(final CompilableProgram program) {
    //This is just the initial number of symbols we'd expect
    var moduleName = EK9_LANG;

    //Now lets get the TEMPLATE_TYPE 'List' and see if we can resolve the conceptual 'T' in it.
    // 'List of type T' because this T can be used inside that template type as if it were an actual type.
    //So it must resolve! But it's only a conceptual type within the class!

    var scope = program.getParsedModules(moduleName).get(0).getModuleScope();
    var resolver = new SimpleResolverForTesting(scope, true);
    var resolved = resolver.apply("List");

    resolved.ifPresentOrElse(listSymbol -> {
      if (listSymbol instanceof IAggregateSymbol list) {
        var resolvedT = list.resolve(new AnySymbolSearch("T"));
        System.out.println("Resolved T is [" + resolvedT + "]");
      }
    }, () -> Assertions.fail("Expecting 'List' to be found"));

    new SymbolCountCheck(moduleName, NUMBER_OF_EK9_SYMBOLS).test(program);

  }

  @Test
  void testBadBootstrapSource() {
    final Supplier<List<CompilableSource>> sourceSupplier =
        () -> List.of(new CompilableSource(getClass().getResource(
            "/examples/parseButFailCompile/builtin/badBuiltin.ek9").getPath()));

    final var underTest = new Ek9LanguageBootStrap(sourceSupplier, listener.get(), new CompilerReporter(true));

    assertThrows(CompilerException.class, underTest::get);
  }
}
