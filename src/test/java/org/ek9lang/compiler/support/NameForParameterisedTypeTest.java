package org.ek9lang.compiler.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.SymbolTable;
import org.junit.jupiter.api.Test;

final class NameForParameterisedTypeTest {
  private final AggregateManipulator aggregateManipulator = new AggregateManipulator();

  private final NameForParameterisedType underTest = new NameForParameterisedType();

  @Test
  void testConceptualTypeArgument() {
    SymbolTable symbolTable = new SymbolTable();

    var t = aggregateManipulator.createGenericT("T", symbolTable);
    var aGenericType = new AggregateSymbol("aGenericType", symbolTable, List.of(t));

    //Now because we have attempted to parameterize this generic type with just a conceptual type argument
    //it is still 'generic'. therefore nothing has changed and we expect the same name!

    var u = aggregateManipulator.createGenericT("U", symbolTable);
    var nameWithU = underTest.apply(aGenericType, List.of(u));

    assertEquals(aGenericType.getFullyQualifiedName(), nameWithU);

    var v = aggregateManipulator.createGenericT("V", symbolTable);
    var nameWithV = underTest.apply(aGenericType, List.of(v));

    assertEquals(aGenericType.getFullyQualifiedName(), nameWithV);

  }

  @Test
  void testConcreteTypeArgument() {
    SymbolTable symbolTable = new SymbolTable();
    AggregateSymbol integerType = new AggregateSymbol("Integer", symbolTable);
    symbolTable.define(integerType);

    var t = aggregateManipulator.createGenericT("T", symbolTable);
    var aGenericType = new AggregateSymbol("aGenericType", symbolTable, List.of(t));

    //This time use a real concrete type.

    var nameWithIntegerType = underTest.apply(aGenericType, List.of(integerType));
    assertNotEquals(aGenericType.getFullyQualifiedName(), nameWithIntegerType);
  }

  @Test
  void testConceptualTypeArguments() {
    SymbolTable symbolTable = new SymbolTable();

    var s = aggregateManipulator.createGenericT("S", symbolTable);
    var t = aggregateManipulator.createGenericT("T", symbolTable);
    var aGenericType = new AggregateSymbol("aGenericType", symbolTable, List.of(s, t));

    //Also check with multiple type arguments. Still both conceptual.

    var u = aggregateManipulator.createGenericT("U", symbolTable);
    var v = aggregateManipulator.createGenericT("V", symbolTable);
    var nameWithUV = underTest.apply(aGenericType, List.of(u, v));

    assertEquals(aGenericType.getFullyQualifiedName(), nameWithUV);

  }

  @Test
  void testConcreteTypeArguments() {
    SymbolTable symbolTable = new SymbolTable();
    AggregateSymbol integerType = new AggregateSymbol("Integer", symbolTable);
    symbolTable.define(integerType);
    AggregateSymbol dateType = new AggregateSymbol("Date", symbolTable);
    symbolTable.define(dateType);

    var s = aggregateManipulator.createGenericT("S", symbolTable);
    var t = aggregateManipulator.createGenericT("T", symbolTable);
    var aGenericType = new AggregateSymbol("aGenericType", symbolTable, List.of(s, t));

    //Now use two real concrete types.
    var nameWithIntegerDate = underTest.apply(aGenericType, List.of(integerType, dateType));
    assertNotEquals(aGenericType.getFullyQualifiedName(), nameWithIntegerDate);

    //Also swap the order and ensure each name is not the same.
    var nameWithDateInteger = underTest.apply(aGenericType, List.of(dateType, integerType));
    assertNotEquals(aGenericType.getFullyQualifiedName(), nameWithDateInteger);

    assertNotEquals(nameWithDateInteger, nameWithIntegerDate);

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

    var s = aggregateManipulator.createGenericT("S", symbolTable);
    var t = aggregateManipulator.createGenericT("T", symbolTable);
    var aGenericType = new AggregateSymbol("aGenericType", symbolTable, List.of(s, t));

    //Now use one real concrete type and one conceptual type.
    var u = aggregateManipulator.createGenericT("U", symbolTable);
    var nameWithIntegerU = underTest.apply(aGenericType, List.of(integerType, u));
    assertNotEquals(aGenericType.getFullyQualifiedName(), nameWithIntegerU);

    var nameWithUInteger = underTest.apply(aGenericType, List.of(u, integerType));
    assertNotEquals(aGenericType.getFullyQualifiedName(), nameWithUInteger);

    assertNotEquals(nameWithIntegerU, nameWithUInteger);

    //Now we should also be able to check that if we use an alternative to U (i.e. V)
    //We still get the same names.
    var v = aggregateManipulator.createGenericT("V", symbolTable);
    var nameWithIntegerV = underTest.apply(aGenericType, List.of(integerType, v));
    assertNotEquals(aGenericType.getFullyQualifiedName(), nameWithIntegerV);

    var nameWithVInteger = underTest.apply(aGenericType, List.of(v, integerType));
    assertNotEquals(aGenericType.getFullyQualifiedName(), nameWithVInteger);

    assertNotEquals(nameWithIntegerV, nameWithVInteger);

    //But we expect that nameWithIntegerV and nameWithIntegerU are the same
    //also nameWithVInteger and nameWithUInteger are the same.
    //This is because U, V or T whatever it is just a conceptual type.
    assertEquals(nameWithIntegerV, nameWithIntegerU);
    assertEquals(nameWithVInteger, nameWithUInteger);

  }
}
