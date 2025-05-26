package org.ek9lang.compiler.symbols.generics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.compiler.search.AnyTypeSymbolSearch;
import org.ek9lang.compiler.support.AggregateManipulator;
import org.ek9lang.compiler.support.InternalNameFor;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;
import org.ek9lang.compiler.symbols.SymbolTable;
import org.junit.jupiter.api.Test;

/**
 * TODO add in some assertions around generic 'type parameters'.
 * For example what 'type' is a 'T' and 'P'? Are they both just an 'Unconstrained type'.
 * Therefore List of type T and Optional of type O are they both of 'Unconstrained type'?
 * What is a SomeGenericType of type 'Q constrain by Integer' versus
 * AnotherGenericType of type 'Z constrain by Integer'?
 * <p>
 * While they are type parameters and can only be resolved in the Generic type in which they are declared.
 * What happens if you parameterize 'List of T' with a 'P' (where P is an 'Unconstrained type')
 * Also what happens if you parameterize 'List of T' with a 'Q' (where Q is constrain by Integer)?
 * Is it only when you try to apply String of Float in this latter case you get errors?
 * What happens with a Dict of type (K, V) when you half parameterize it with one concrete and one conceptual type
 * i.e. Dict of (Integer, T) or Dict of (T, Integer) for example. Is a new GenericType created that now only needs
 * one more type parameter to make it concrete (i.e. a replacement for K or V in the second case).
 * </p>
 * <p>
 * Does 'List of type T' when parameterised with a conceptual type parameter 'N' just remain a 'List of T'?
 * Nothing has really changed, it still has to be parameterised.
 * Also how about a 'SomeGenericType of type Q constrain by Integer' with a conceptual type 'M',
 * again nothing has changed except we now know that 'M' must also be 'constrain by Integer' (or a subclass of Integer).
 * So we must enforce that latter point.
 * </p>
 */
final class SimpleGenericsTest {

  private final AggregateManipulator aggregateManipulator = new AggregateManipulator();

  /**
   * This test focuses on extension of a generic type.
   */
  @Test
  void testExtendingAGenericType() {
    SymbolTable symbolTable = new SymbolTable();

    var aGenericBaseType = new AggregateSymbol("GenericBase", symbolTable);
    var t = aggregateManipulator.createGenericT("T", aGenericBaseType.getFullyQualifiedName(), symbolTable);
    aGenericBaseType.addTypeParameterOrArgument(t);

    var anotherGenericType = new AggregateSymbol("AnotherGenericBase", symbolTable);
    var r = aggregateManipulator.createGenericT("R", anotherGenericType.getFullyQualifiedName(), symbolTable);
    var s = aggregateManipulator.createGenericT("S", anotherGenericType.getFullyQualifiedName(), symbolTable);
    anotherGenericType.addTypeParameterOrArgument(r);
    anotherGenericType.addTypeParameterOrArgument(s);

    //Check not equal
    assertNotEquals(aGenericBaseType, anotherGenericType);

    //Now make a plain aggregate and extend from the generic base.
    AggregateSymbol cls1 = new AggregateSymbol("cls1", symbolTable);
    cls1.setSuperAggregate(aGenericBaseType);

    //Is it possible to resolve 'T'?
    var resolvedT = cls1.resolve(new AnyTypeSymbolSearch("T"));
    assertTrue(resolvedT.isPresent());
    assertEquals(t, resolvedT.get());

  }

  @Test
  void testExtendingAParameterisedGenericType() {
    SymbolTable symbolTable = new SymbolTable();
    AggregateSymbol integerType = new AggregateSymbol("Integer", symbolTable);
    symbolTable.define(integerType);

    //Make the 'T' and generic type that will use the 'T' as a conceptual type parameter
    var aGenericType = new AggregateSymbol("GenericType", symbolTable);
    var t = aggregateManipulator.createGenericT("T", aGenericType.getFullyQualifiedName(), symbolTable);
    aGenericType.addTypeParameterOrArgument(t);

    //Make the parameterised type using the generic type and parameterize it with concrete type Integer.
    var parameterisedType1 = new AggregateSymbol("ParameterisedWithInteger", symbolTable);
    parameterisedType1.setGenericType(aGenericType);
    parameterisedType1.addTypeParameterOrArgument(integerType);

    //Now make a plain aggregate and extend from the generic base.
    AggregateSymbol cls1 = new AggregateSymbol("cls1", symbolTable);
    cls1.setSuperAggregate(parameterisedType1);

    //Is it possible to resolve 'T', it should NOT be possible as 'T' is hidden within the parameterised type.
    var resolvedT = cls1.resolve(new AnyTypeSymbolSearch("T"));
    assertFalse(resolvedT.isPresent());

  }

  /**
   * A bit of a long test, checking the naming (internal) when a generic type has been parameterised.
   */
  @Test
  void testGenericTypeInternalName() {
    //Just to hold the simulated types.
    SymbolTable symbolTable = new SymbolTable();

    //Will use these to parameterize the generic type.
    AggregateSymbol integerType = new AggregateSymbol("Integer", symbolTable);
    symbolTable.define(integerType);
    AggregateSymbol dateType = new AggregateSymbol("Date", symbolTable);
    symbolTable.define(dateType);

    //This is what we will use to test the naming.
    BiFunction<PossibleGenericSymbol, List<ISymbol>, String> namer = new InternalNameFor();

    var someDualGenericType = new AggregateSymbol("SomeDualGeneric", symbolTable);
    var k = aggregateManipulator.createGenericT("K", someDualGenericType.getFullyQualifiedName(), symbolTable);
    var v = aggregateManipulator.createGenericT("V", someDualGenericType.getFullyQualifiedName(), symbolTable);
    someDualGenericType.addTypeParameterOrArgument(k);
    someDualGenericType.addTypeParameterOrArgument(v);

    assertNotNull(someDualGenericType);

    var someDualGenericTypeName = someDualGenericType.getName();

    var someOuterGenericType = new AggregateSymbol("SomeOuterGenericType", symbolTable);
    var r = aggregateManipulator.createGenericT("R", someOuterGenericType.getFullyQualifiedName(), symbolTable);
    var s = aggregateManipulator.createGenericT("S", someOuterGenericType.getFullyQualifiedName(), symbolTable);
    someOuterGenericType.addTypeParameterOrArgument(r);
    someOuterGenericType.addTypeParameterOrArgument(s);

    //I'm thinking that really this should be the same as someDualGenericTypeName. because it is still fully generic
    var parameterisedWithMoreGenericTypes = namer.apply(someDualGenericType, List.of(r, s));
    assertEquals("_SomeDualGeneric_A8119AC781DF1B471442DAC7CD4E7ACD9134BA8ED65A9DC2BBF2461A5A4E91E0",
        parameterisedWithMoreGenericTypes);

    //Parameterize with two concrete types
    var parameterisedWithConcreteTypes1 = namer.apply(someDualGenericType, List.of(integerType, dateType));
    assertNotEquals(someDualGenericTypeName, parameterisedWithConcreteTypes1);

    //Now do the other way, i.e. switch the order.
    var parameterisedWithConcreteTypes2 = namer.apply(someDualGenericType, List.of(dateType, integerType));
    assertNotEquals(someDualGenericTypeName, parameterisedWithConcreteTypes2);

    //Check that the two internal names are different - because the order is different.
    assertNotEquals(parameterisedWithConcreteTypes1, parameterisedWithConcreteTypes2);

    //Now what happens if parameterized with one concrete and on conceptual type
    //So make another generic type parameter.
    var anotherOuterGenericType = new AggregateSymbol("AnotherOuterGenericType", symbolTable);
    var t = aggregateManipulator.createGenericT("T", anotherOuterGenericType.getFullyQualifiedName(), symbolTable);
    anotherOuterGenericType.addTypeParameterOrArgument(t);

    var parameterisedWithHalfConcreteTypes1 = namer.apply(someDualGenericType, List.of(t, dateType));
    assertNotEquals(someDualGenericTypeName, parameterisedWithHalfConcreteTypes1);
    assertNotEquals(parameterisedWithConcreteTypes1, parameterisedWithHalfConcreteTypes1);
    assertNotEquals(parameterisedWithConcreteTypes2, parameterisedWithHalfConcreteTypes1);

    var parameterisedWithHalfConcreteTypes2 = namer.apply(someDualGenericType, List.of(integerType, t));
    assertNotEquals(someDualGenericTypeName, parameterisedWithHalfConcreteTypes2);
    assertNotEquals(parameterisedWithConcreteTypes1, parameterisedWithHalfConcreteTypes2);
    assertNotEquals(parameterisedWithConcreteTypes2, parameterisedWithHalfConcreteTypes2);

    assertNotEquals(parameterisedWithHalfConcreteTypes1, parameterisedWithHalfConcreteTypes2);
  }

}
