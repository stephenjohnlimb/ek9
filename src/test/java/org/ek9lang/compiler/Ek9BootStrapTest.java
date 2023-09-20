package org.ek9lang.compiler;

import static org.ek9lang.compiler.Ek9BuiltinLangSupplier.NUMBER_OF_EK9_SYMBOLS;
import static org.ek9lang.compiler.support.AggregateFactory.EK9_LANG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.ek9lang.compiler.common.CompilationPhaseListener;
import org.ek9lang.compiler.common.CompilerReporter;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.search.AnyTypeSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.support.SimpleResolverForTesting;
import org.ek9lang.compiler.support.SymbolCountCheck;
import org.ek9lang.core.CompilerException;
import org.ek9lang.core.SharedThreadContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class Ek9BootStrapTest {
  final Ek9BuiltinLangSupplier sourceSupplier = new Ek9BuiltinLangSupplier();

  private final Supplier<CompilationPhaseListener> listener
      = () -> compilationEvent -> {
    var source = compilationEvent.source();
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

    sharedContext.accept(this::assertEk9);

    assertSerialisation(sharedContext);
  }

  /**
   * Checks that a fully populated compilable program with basic ek9 built-in symbols can be serialised.
   */
  private void assertSerialisation(final SharedThreadContext<CompilableProgram> sharedContext) {
    //So serialise a program and then deserialize it.
    var serializer = new Serializer();
    var deserializer = new DeSerializer();

    var start = System.currentTimeMillis();
    var justTheBytes = serializer.apply(sharedContext);
    var afterSerialize = System.currentTimeMillis();
    //OK now lets try and reload it.

    var reloadedProgram = deserializer.apply(justTheBytes);
    var afterDeserialize = System.currentTimeMillis();
    reloadedProgram.accept(this::assertEk9);

    System.out.printf("Serialize CompilableProgram took %d ms, Deserialize took %d ms\n", (afterSerialize-start), (afterDeserialize-afterSerialize));
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
        var resolvedT = list.resolve(new AnyTypeSymbolSearch("T"));
        resolvedT.ifPresentOrElse(t -> {
        }, () -> Assertions.fail("Expecting 'T' to be found"));
      }
    }, () -> Assertions.fail("Expecting 'List' to be found"));

    new SymbolCountCheck(moduleName, NUMBER_OF_EK9_SYMBOLS).test(program);

    var resolvedInteger = resolver.apply("Integer");
    resolvedInteger.ifPresentOrElse(integerSymbol -> {
      AggregateSymbol asAggregate = (AggregateSymbol) integerSymbol;

      MethodSymbolSearchResult result = new MethodSymbolSearchResult();
      result = asAggregate.resolveMatchingMethods(new MethodSymbolSearch("#^"), result);
      if (!result.isSingleBestMatchPresent()) {
        Assertions.fail("Expecting method 'Integer.#^()' to be found");
      }
    }, () -> Assertions.fail("Expecting 'Integer' to be found"));
  }

  @Test
  void testBadBootstrapSource() {
    final Supplier<List<CompilableSource>> sourceSupplier =
        () -> List.of(new CompilableSource(Objects.requireNonNull(getClass().getResource(
            "/examples/parseButFailCompile/builtin/badBuiltin.ek9")).getPath()));

    final var underTest = new Ek9LanguageBootStrap(sourceSupplier, listener.get(), new CompilerReporter(true));

    assertThrows(CompilerException.class, underTest::get);
  }
}
