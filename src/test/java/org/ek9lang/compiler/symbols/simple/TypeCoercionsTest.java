package org.ek9lang.compiler.symbols.simple;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.ek9lang.compiler.support.TypeCoercions;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.SymbolTable;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Some simple tests on coercions, for this set of tests
 * a number of simulated Aggregates are created with the 'promote' operator added in.
 */
final class TypeCoercionsTest {
  @Test
  void testUnrelatedTypes() {
    SymbolTable symbolTable = new SymbolTable();
    //two unrelated types
    ISymbol typeA = new AggregateSymbol("TypeA", symbolTable);
    ISymbol typeB = new AggregateSymbol("TypeB", symbolTable);

    Assertions.assertFalse(TypeCoercions.get().isCoercible(Optional.of(typeA), Optional.of(typeB)));
    assertFalse(TypeCoercions.get().isCoercible(Optional.of(typeA), Optional.empty()));

    assertFalse(TypeCoercions.get().isCoercible(typeB, typeA));

    assertTrue(typeA.getAssignableWeightTo(typeB) < -1000.0);
    assertTrue(typeB.getAssignableWeightTo(typeA) < -1000.0);
  }

  @Test
  void testNoneAggregate() {
    SymbolTable symbolTable = new SymbolTable();
    //two unrelated types
    ISymbol typeA = new AggregateSymbol("TypeA", symbolTable);
    ISymbol typeB = new FunctionSymbol("Func1", symbolTable);
    assertFalse(TypeCoercions.get().isCoercible(typeB, typeA));
  }

  @Test
  void testPromotionTypes() {
    SymbolTable symbolTable = new SymbolTable();
    //two unrelated types
    AggregateSymbol typeA = new AggregateSymbol("TypeA", symbolTable);
    AggregateSymbol typeB = new AggregateSymbol("TypeB", symbolTable);

    //Now create a method on TypeA that enables it to be promoted to TypeB
    MethodSymbol promotion = new MethodSymbol("#^", typeA);
    promotion.setOperator(true);
    promotion.setReturningSymbol(new VariableSymbol("rtn", typeB));
    typeA.define(promotion);

    //OK now check it can be coerced
    assertTrue(TypeCoercions.get().isCoercible(typeA, typeB));
    assertTrue(typeA.getAssignableWeightTo(typeB) > ISymbol.NOT_ASSIGNABLE);

    //But only one way
    assertFalse(TypeCoercions.get().isCoercible(typeB, typeA));
    assertTrue(typeB.getAssignableWeightTo(typeA) < -1000.0);
  }
}
