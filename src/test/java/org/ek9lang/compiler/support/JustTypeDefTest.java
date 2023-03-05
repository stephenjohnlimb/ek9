package org.ek9lang.compiler.support;

import static org.ek9lang.compiler.symbol.support.AggregateFactory.EK9_LANG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.ek9lang.compiler.internals.Ek9BuiltinLangSupplier;
import org.ek9lang.compiler.main.Ek9LanguageBootStrap;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.main.phases.result.CompilerReporter;
import org.ek9lang.compiler.main.resolvedefine.ResolveOrDefineTypeDef;
import org.ek9lang.compiler.symbol.support.ScopeStack;
import org.ek9lang.compiler.symbol.support.SymbolFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class JustTypeDefTest {

  private JustTypeDef underTest;

  private ResolveOrDefineTypeDef resolveOrDefineTypeDef;

  private final PartialEk9StringToTypeDef partialEk9StringToTypeDef = new PartialEk9StringToTypeDef();

  public JustTypeDefTest() {
    //OK boot up the ek9 main module scope loaded for testing resolutions.
    final var ek9 = new Ek9LanguageBootStrap(new Ek9BuiltinLangSupplier(), compilationEvent -> {
    },
        new CompilerReporter(false));

    final var sharedContext = ek9.get();

    sharedContext.accept(compilableProgram -> {
      var parsedModule = compilableProgram.getParsedModules(EK9_LANG).get(0);
      if (parsedModule == null) {
        Assertions.fail("Unable to load ek9 scope");
      }
      this.underTest = new JustTypeDef(parsedModule.getModuleScope());
      var symbolAndScopeManagement = new SymbolAndScopeManagement(parsedModule,
          new ScopeStack(parsedModule.getModuleScope()));
      var errorListener = parsedModule.getSource().getErrorListener();
      var symbolFactory = new SymbolFactory(parsedModule);

      resolveOrDefineTypeDef =
          new ResolveOrDefineTypeDef(symbolAndScopeManagement, symbolFactory, errorListener, false);
    });
  }

  /**
   * Find a simple type.
   */
  @ParameterizedTest
  @CsvSource({"Integer", "DateTime"})
  void testResolveSimpleType(final String typeName) {
    var resolved = underTest.typeDefToSymbol(typeName);
    assertTrue(resolved.isPresent());
    assertEquals(typeName, resolved.get().getName());
    assertFalse(resolved.get().isGenericInNature());
    assertFalse(resolved.get().isGenericInNature());
  }

  /**
   * Find the generic type. Not yet parameterised.
   */
  @ParameterizedTest
  @CsvSource({"List", "Iterator", "Dict", "Optional",
      "Supplier", "Consumer", "Function"})
  void testResolveGenericType(final String genericTypeName) {
    var resolved = underTest.typeDefToSymbol(genericTypeName);
    assertTrue(resolved.isPresent());
    assertEquals(genericTypeName, resolved.get().getName());
    assertTrue(resolved.get().isGenericInNature());
  }

  /**
   * Note that using commas in this test for things like
   * Dict of (Integer, String) - confuses junit.
   */
  @ParameterizedTest
  @CsvSource({"List of (DateTime)", "Iterator of (Date)"})
  void testResolveParameterizedType(final String parameterizedType) {
    var failToResolved = underTest.typeDefToSymbol(parameterizedType);
    //Firstly check it is not resolved
    assertTrue(failToResolved.isEmpty());

    //Now do what the compiler will do and 'resolve of define it'.
    assertTrue(Optional.of(parameterizedType).map(partialEk9StringToTypeDef).map(resolveOrDefineTypeDef).isPresent());

    //Now check it can just be resolved.
    var resolved = underTest.typeDefToSymbol(parameterizedType);
    assertTrue(resolved.isPresent());
    //But as a parameterized type - it is not generic in nature.
    assertFalse(resolved.get().isGenericInNature());
  }

  @Test
  void testResolveDictOfIntegerString() {
    var parameterizedType = "Dict of (Integer, String)";
    testResolveParameterizedType(parameterizedType);
  }

  @Test
  void testResolveDictOfIntegerListOfString() {
    var parameterizedType = "Dict of (Integer, List of String)";
    testResolveParameterizedType(parameterizedType);
  }
}
