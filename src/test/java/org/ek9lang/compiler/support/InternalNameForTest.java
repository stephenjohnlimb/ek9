package org.ek9lang.compiler.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.SymbolTable;
import org.junit.jupiter.api.Test;

/**
 * Tests the naming of parameterised types, this is essential for looking up types when they have been
 * parameterised or partially parameterised.
 */
final class InternalNameForTest {
  private final AggregateManipulator aggregateManipulator = new AggregateManipulator();

  private final InternalNameFor underTest = new InternalNameFor();

  @Test
  void testWithOwnTypeParameterAsArgument() {
    SymbolTable symbolTable = new SymbolTable();

    var aGenericType = new AggregateSymbol("aGenericType", symbolTable);
    var t = aggregateManipulator.createGenericT("T", aGenericType.getFullyQualifiedName(), symbolTable);
    aGenericType.addTypeParameterOrArgument(t);

    //Now if we try and use its own 'T' with it - it should detect this and workout that it is still just the same
    //generic type
    final var withSameArguments = underTest.apply(aGenericType, List.of(t));
    assertEquals("aGenericType", withSameArguments);

  }

  @Test
  void testConcreteTypeArgument() {
    SymbolTable symbolTable = new SymbolTable();
    AggregateSymbol integerType = new AggregateSymbol("Integer", symbolTable);
    symbolTable.define(integerType);

    var aGenericType = new AggregateSymbol("aGenericType", symbolTable);
    var t = aggregateManipulator.createGenericT("T", aGenericType.getFullyQualifiedName(), symbolTable);
    aGenericType.addTypeParameterOrArgument(t);

    //This time use a real concrete type.

    var nameWithIntegerType = underTest.apply(aGenericType, List.of(integerType));
    assertNotEquals(aGenericType.getFullyQualifiedName(), nameWithIntegerType);
  }

  @Test
  void testConcreteTypeArguments() {
    SymbolTable symbolTable = new SymbolTable();
    AggregateSymbol integerType = new AggregateSymbol("Integer", symbolTable);
    symbolTable.define(integerType);
    AggregateSymbol dateType = new AggregateSymbol("Date", symbolTable);
    symbolTable.define(dateType);

    var aGenericType = new AggregateSymbol("aGenericType", symbolTable);
    var s = aggregateManipulator.createGenericT("S", aGenericType.getFullyQualifiedName(), symbolTable);
    var t = aggregateManipulator.createGenericT("T", aGenericType.getFullyQualifiedName(), symbolTable);
    aGenericType.addTypeParameterOrArgument(s);
    aGenericType.addTypeParameterOrArgument(t);

    //Now use two real concrete types.
    var nameWithIntegerDate = underTest.apply(aGenericType, List.of(integerType, dateType));
    assertNotEquals(aGenericType.getFullyQualifiedName(), nameWithIntegerDate);

    //Also swap the order and ensure each name is not the same.
    var nameWithDateInteger = underTest.apply(aGenericType, List.of(dateType, integerType));
    assertNotEquals(aGenericType.getFullyQualifiedName(), nameWithDateInteger);

    assertNotEquals(nameWithDateInteger, nameWithIntegerDate);

  }

  @Test
  void testConceptualTypeArguments() {
    SymbolTable symbolTable = new SymbolTable();

    var aGenericType = new AggregateSymbol("aGenericType", symbolTable);
    var s = aggregateManipulator.createGenericT("S", aGenericType.getFullyQualifiedName(), symbolTable);
    var t = aggregateManipulator.createGenericT("T", aGenericType.getFullyQualifiedName(), symbolTable);
    aGenericType.addTypeParameterOrArgument(s);
    aGenericType.addTypeParameterOrArgument(t);

    //Also check with multiple type arguments. Still both conceptual.
    var outerGenericType = new AggregateSymbol("outerGenericType", symbolTable);

    var u = aggregateManipulator.createGenericT("U", outerGenericType.getFullyQualifiedName(), symbolTable);
    var v = aggregateManipulator.createGenericT("V", outerGenericType.getFullyQualifiedName(), symbolTable);
    outerGenericType.addTypeParameterOrArgument(u);
    outerGenericType.addTypeParameterOrArgument(v);

    //So now the model is to simulate an outer generic with U and V, then inside that use 'aGeneric' (which has S, T)
    //But parameterise it with U and V so it's all still conceptual.

    var nameWithUV = underTest.apply(aGenericType, List.of(u, v));

    //The digest raw text will be: [aGenericType_outerGenericType::U_outerGenericType::V]
    assertEquals("_aGenericType_8696C9A9E36401C089D1A448DF10B09FF3194BE91DCBF1DC219E15404E019152", nameWithUV);

  }

  /**
   * This is the tricky one, we take a generic type that accepts two type arguments.
   * Use one conceptual type and one real concrete type to create a new 'still generic type'.
   * But note that we must incorporate some nature of place-holder for the conceptual type.
   * This is because we need to ensure that the unique name generated cannot be confused.
   * i.e. aGenericType of (T, Integer) and aGenericType of (Integer, T) must differ.
   * But aGenericType of (T, Integer) and aGenericType(P, Integer) are really the same.
   * The 'T' or 'P' is just a 'variable' naming mechanism - they are both just 'CONCEPTUAL'.
   */
  @Test
  void testConcreteAndConceptualTypeArguments() {
    SymbolTable symbolTable = new SymbolTable();
    AggregateSymbol integerType = new AggregateSymbol("Integer", symbolTable);
    symbolTable.define(integerType);

    var aGenericType = new AggregateSymbol("aGenericType", symbolTable);
    var s = aggregateManipulator.createGenericT("S", aGenericType.getFullyQualifiedName(), symbolTable);
    var t = aggregateManipulator.createGenericT("T", aGenericType.getFullyQualifiedName(), symbolTable);
    aGenericType.addTypeParameterOrArgument(s);
    aGenericType.addTypeParameterOrArgument(t);

    //Now use one real concrete type and one conceptual type.
    var wrappingGenericType = new AggregateSymbol("wrappingGenericType", symbolTable);
    var u = aggregateManipulator.createGenericT("U", wrappingGenericType.getFullyQualifiedName(), symbolTable);
    var v = aggregateManipulator.createGenericT("V", wrappingGenericType.getFullyQualifiedName(), symbolTable);
    wrappingGenericType.addTypeParameterOrArgument(u);
    wrappingGenericType.addTypeParameterOrArgument(v);

    var nameWithIntegerU = underTest.apply(aGenericType, List.of(integerType, u));
    assertNotEquals(aGenericType.getFullyQualifiedName(), nameWithIntegerU);

    var nameWithUInteger = underTest.apply(aGenericType, List.of(u, integerType));
    assertNotEquals(aGenericType.getFullyQualifiedName(), nameWithUInteger);

    assertNotEquals(nameWithIntegerU, nameWithUInteger);

    var nameWithIntegerV = underTest.apply(aGenericType, List.of(integerType, v));
    assertNotEquals(aGenericType.getFullyQualifiedName(), nameWithIntegerV);

    var nameWithVInteger = underTest.apply(aGenericType, List.of(v, integerType));
    assertNotEquals(aGenericType.getFullyQualifiedName(), nameWithVInteger);

    assertNotEquals(nameWithIntegerV, nameWithVInteger);

    assertNotEquals(nameWithIntegerV, nameWithIntegerU);
    assertNotEquals(nameWithVInteger, nameWithUInteger);

  }
}
