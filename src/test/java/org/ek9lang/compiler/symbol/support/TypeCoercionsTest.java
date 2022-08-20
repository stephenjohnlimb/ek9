package org.ek9lang.compiler.symbol.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.FunctionSymbol;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.MethodSymbol;
import org.ek9lang.compiler.symbol.VariableSymbol;
import org.junit.jupiter.api.Test;

/**
 * Some simple tests on coercions, for this set of tests
 * a number of simulated Aggregates are created with the 'promote' operator added in.
 */
class TypeCoercionsTest {
  @Test
  void testUnrelatedTypes() {
    SymbolTable symbolTable = new SymbolTable();
    //two unrelated types
    ISymbol typeA = new AggregateSymbol("TypeA", symbolTable);
    ISymbol typeB = new AggregateSymbol("TypeB", symbolTable);

    assertFalse(TypeCoercions.get().isCoercible(Optional.of(typeA), Optional.of(typeB)));
    assertFalse(TypeCoercions.get().isCoercible(Optional.of(typeA), Optional.empty()));

    assertFalse(TypeCoercions.get().isCoercible(typeB, typeA));
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

    //But only one way
    assertFalse(TypeCoercions.get().isCoercible(typeB, typeA));
  }
}
