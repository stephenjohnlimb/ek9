package org.ek9lang.compiler.symbols.simple;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.ek9lang.compiler.support.SymbolMatcher;
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
 * Coercion in EK9 requires explicit promotion operators, there is no implicit coercion at all.
 */
final class TypeCoercionsTest {

  @Test
  void testUnrelatedTypes() {
    SymbolTable symbolTable = new SymbolTable();
    //two unrelated types
    ISymbol typeA = new AggregateSymbol("TypeA", symbolTable);
    ISymbol typeB = new AggregateSymbol("TypeB", symbolTable);

    Assertions.assertFalse(TypeCoercions.isCoercible(Optional.of(typeA), Optional.of(typeB)));
    assertFalse(TypeCoercions.isCoercible(Optional.of(typeA), Optional.empty()));

    assertFalse(TypeCoercions.isCoercible(typeB, typeA));

    assertTrue(typeA.getAssignableCostTo(typeB) < 0.0);
    assertTrue(typeB.getAssignableCostTo(typeA) < 0.0);
  }

  @Test
  void testNoneAggregate() {
    SymbolTable symbolTable = new SymbolTable();
    //two unrelated types
    ISymbol typeA = new AggregateSymbol("TypeA", symbolTable);
    ISymbol typeB = new FunctionSymbol("Func1", symbolTable);
    assertFalse(TypeCoercions.isCoercible(typeB, typeA));
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
    assertTrue(TypeCoercions.isCoercible(typeA, typeB));
    assertTrue(typeA.getAssignableCostTo(typeB) > SymbolMatcher.INVALID_COST);

    //But only one way
    assertFalse(TypeCoercions.isCoercible(typeB, typeA));
    assertTrue(typeB.getAssignableCostTo(typeA) < 0.0);
  }
}
