package org.ek9lang.compiler.common;

import static org.ek9lang.compiler.support.AggregateFactory.EK9_LANG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.Ek9BuiltinLangSupplier;
import org.ek9lang.compiler.Ek9LanguageBootStrap;
import org.ek9lang.compiler.support.ResolveOrDefineTypeDef;
import org.ek9lang.compiler.support.SymbolFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Not intended to be an exhaustive test of resolving existing ek9 types.
 * But does check a handful to check resolution actually works and non-resolution also works.
 * Then goes on to define some new parameterized types like List/Iterator with concrete simple types.
 */
class TypeDefTest {

  private final PartialEk9StringToTypeDef partialEk9StringToTypeDef = new PartialEk9StringToTypeDef();
  private TypeDefResolver underTest;
  private ResolveOrDefineTypeDef resolveOrDefineTypeDef;

  public TypeDefTest() {
    //OK boot up the ek9 main module scope loaded for testing resolutions.
    final var ek9 = new Ek9LanguageBootStrap(new Ek9BuiltinLangSupplier(), compilationEvent -> {
    }, new CompilerReporter(false, true));

    final var sharedContext = ek9.get();

    sharedContext.accept(compilableProgram -> {
      var parsedModule = compilableProgram.getParsedModules(EK9_LANG).get(0);
      if (parsedModule == null) {
        Assertions.fail("Unable to load ek9 scope");
      }
      var errorListener = parsedModule.getSource().getErrorListener();

      this.underTest = new TypeDefResolver(parsedModule.getModuleScope());
      var symbolAndScopeManagement = new SymbolAndScopeManagement(parsedModule,
          new ScopeStack(parsedModule.getModuleScope()));

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
  }

  /**
   * Find the generic type. Not yet parameterised with specific types.
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

  /**
   * Note that using commas in this test for things like
   * Dict of (Integer, String) - confuses junit.
   * See methods above that just call this directly.
   */
  @ParameterizedTest
  @CsvSource({"List of (DateTime)", "Iterator of (Date)"})
  void testResolveParameterizedType(final String parameterizedType) {
    var failToResolved = underTest.typeDefToSymbol(parameterizedType);
    //Firstly check it is not resolved
    assertTrue(failToResolved.isEmpty());

    //Now do what the compiler will do and 'resolve or define it'.
    //So as it was not previously resolved - it will now get defined.
    var toTypeDefCtx = partialEk9StringToTypeDef.apply(parameterizedType);
    var theType = resolveOrDefineTypeDef.apply(toTypeDefCtx);
    assertTrue(theType.isPresent());

    //Now check it can just be resolved.
    var resolved = underTest.typeDefToSymbol(parameterizedType);
    assertTrue(resolved.isPresent());
    //But as a parameterized type - it is not generic in nature.
    assertFalse(resolved.get().isGenericInNature());
  }
}
