package org.ek9lang.compiler.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.ek9lang.compiler.common.TypeDefResolver;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.SymbolTable;
import org.junit.jupiter.api.Test;

/**
 * Tests for GeneralTypeResolver in isolation.
 * Also tests TypeDefResolver - which is a thin wrapper.
 */
class GeneralTypeResolverTest {

  private final AggregateManipulator support = new AggregateManipulator();

  private final ParameterizedSymbolCreator creator = new ParameterizedSymbolCreator(new InternalNameFor());

  /**
   * Just a simple type resolution.
   */
  @Test
  void testSimpleTypeResolution() {
    SymbolTable scope = new SymbolTable("some.test");
    AggregateSymbol someType = new AggregateSymbol("SomeType", scope);
    someType.setModuleScope(scope);
    scope.define(someType);

    GeneralTypeResolver resolver = new GeneralTypeResolver(scope);

    var notResolved = resolver.apply(new SymbolSearchConfiguration("NonSuch"));
    assertFalse(notResolved.isPresent());

    var resolved = resolver.apply(new SymbolSearchConfiguration("SomeType"));
    assertTrue(resolved.isPresent());

    TypeDefResolver resolveByTypeDef = new TypeDefResolver(scope);

    notResolved = resolveByTypeDef.typeDefToSymbol("NonSuch");
    assertFalse(notResolved.isPresent());

    resolved = resolveByTypeDef.typeDefToSymbol("SomeType");
    assertTrue(resolved.isPresent());
  }

  /**
   * A generic type resolution.
   */
  @Test
  void testGenericTypeResolution() {
    SymbolTable scope = new SymbolTable("some.test");

    AggregateSymbol someGenericType = new AggregateSymbol("SomeGenericType", scope);
    someGenericType.setModuleScope(scope);

    var t = support.createGenericT("T", someGenericType);
    someGenericType.addTypeParameterOrArgument(t);
    scope.define(someGenericType);

    AggregateSymbol someType = new AggregateSymbol("SomeType", scope);
    someType.setModuleScope(scope);
    scope.define(someType);

    GeneralTypeResolver resolver = new GeneralTypeResolver(scope);

    var resolved = resolver.apply(new SymbolSearchConfiguration("SomeGenericType"));
    assertTrue(resolved.isPresent());
    assertTrue(resolved.get().isGenericInNature());
    //Now make a parameterised 'SomeGenericType' of type 'T' with a 'SomeType'.
    var parameterizedType = creator.apply(someGenericType, List.of(someType));
    assertNotNull(parameterizedType);
    assertFalse(parameterizedType.isGenericInNature());
    parameterizedType.setModuleScope(scope);
    //So now define it in the symbol table and then see if it can be found.
    scope.define(parameterizedType);

    //Now lets go one step further
    var parameterisedTypeOfParameterisedType = creator.apply(someGenericType, List.of(parameterizedType));
    parameterisedTypeOfParameterisedType.setModuleScope(scope);
    //So now define it in the symbol table and then see if it can be found.
    scope.define(parameterisedTypeOfParameterisedType);

    var parameterizedNotResolved = resolver.apply(
        new SymbolSearchConfiguration("SomeGenericType",
            new SymbolSearchConfiguration("NonSuch")));
    assertFalse(parameterizedNotResolved.isPresent());

    //OK now check if it can be found.
    var parameterizedResolved = resolver.apply(
        new SymbolSearchConfiguration("SomeGenericType",
            new SymbolSearchConfiguration("SomeType")));
    assertTrue(parameterizedResolved.isPresent());

    TypeDefResolver resolveByTypeDef = new TypeDefResolver(scope);
    resolved = resolveByTypeDef.typeDefToSymbol("SomeGenericType");
    assertTrue(resolved.isPresent());

    //Note we must use parenthesis here.
    parameterizedNotResolved = resolveByTypeDef.typeDefToSymbol("SomeGenericType of (NonSuch)");
    assertFalse(parameterizedNotResolved.isPresent());

    parameterizedResolved = resolveByTypeDef.typeDefToSymbol("SomeGenericType of (SomeType)");
    assertTrue(parameterizedResolved.isPresent());

    var doubleParameterizedResolved = resolveByTypeDef
        .typeDefToSymbol("SomeGenericType of (SomeGenericType of (SomeType))");
    assertTrue(doubleParameterizedResolved.isPresent());
  }
}
