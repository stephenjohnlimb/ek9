package org.ek9lang.compiler.symbol.support;

import junit.framework.TestCase;
import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.MethodSymbol;
import org.ek9lang.compiler.symbol.VariableSymbol;
import org.junit.Test;

import java.util.Optional;

/**
 * Some simple tests on coercions, for this set of tests
 * a number of simulated Aggregates are created with the 'promote' operator added in.
 */
public class TypeCoercionsTest
{
    @Test
    public void testUnrelatedTypes()
    {
        SymbolTable symbolTable = new SymbolTable();
        //two unrelated types
        ISymbol typeA = new AggregateSymbol("TypeA", symbolTable);
        ISymbol typeB = new AggregateSymbol("TypeB", symbolTable);

        TestCase.assertFalse(TypeCoercions.get().isCoercible(Optional.of(typeA), Optional.of(typeB)));
        TestCase.assertFalse(TypeCoercions.get().isCoercible(Optional.of(typeA), Optional.empty()));

        TestCase.assertFalse(TypeCoercions.get().isCoercible(typeB, typeA));
    }

    @Test
    public void testPromotionTypes()
    {
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
        TestCase.assertTrue(TypeCoercions.get().isCoercible(typeA, typeB));

        //But only one way
        TestCase.assertFalse(TypeCoercions.get().isCoercible(typeB, typeA));
    }
}
