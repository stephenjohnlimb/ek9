package org.ek9lang.compiler.symbol.support;

import junit.framework.TestCase;
import org.ek9lang.compiler.symbol.*;
import org.ek9lang.compiler.tokenizer.SyntheticToken;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

public class SymbolsTest
{
    private AggregateSupport support = new AggregateSupport();
    private IScope symbolTable = new SymbolTable();

    @Before
    public void setupBasicSymbols()
    {
        symbolTable = new SymbolTable();
        AggregateSymbol integerType = new AggregateSymbol("Integer", symbolTable);
        symbolTable.define(integerType);
        AggregateSymbol stringType = new AggregateSymbol("String", symbolTable);
        symbolTable.define(stringType);
        symbolTable.define(new AggregateSymbol("Boolean", symbolTable));
        symbolTable.define(new AggregateSymbol("Void", symbolTable));
    }

    @Test
    public void testFullyQualifiedName()
    {
        TestCase.assertFalse(support.isSymbolNameFullyQualified("name"));
        TestCase.assertTrue(support.isSymbolNameFullyQualified("com.name::name"));

        TestCase.assertTrue("com.part".equals(support.getModuleNameIfPresent("com.part::name")));
        TestCase.assertTrue("".equals(support.getModuleNameIfPresent("name")));

        TestCase.assertTrue("name".equals(support.getUnqualifiedName("com.part::name")));
        TestCase.assertTrue("name".equals(support.getUnqualifiedName("name")));
    }
    @Test
    public void testCreateGenericTypeT()
    {
        AggregateSymbol t = support.createGenericT("T", symbolTable);
        TestCase.assertNotNull(t);
        TestCase.assertFalse(t.getSymbolsForThisScope().isEmpty());
    }

    @Test
    public void testCreateEnumerationType()
    {
        AggregateSymbol e = new AggregateSymbol("CheckEnum", symbolTable);
        support.addSyntheticEnumerationMethods(e);
        TestCase.assertFalse(e.getSymbolsForThisScope().isEmpty());
    }

    @Test
    public void testAggregateCreation()
    {
        AggregateSymbol underTest = new AggregateSymbol("UnderTest", symbolTable);
        //Add some fields/properties.
        Optional<ISymbol> stringType = symbolTable.resolve(new TypeSymbolSearch("String"));
        Optional<ISymbol> integerType = symbolTable.resolve(new TypeSymbolSearch("Integer"));
        TestCase.assertTrue(stringType.isPresent());
        TestCase.assertTrue(integerType.isPresent());

        underTest.define(new VariableSymbol("v1", stringType));
        underTest.define(new VariableSymbol("v2", integerType));

        support.addConstructor(underTest);

        Optional<ISymbol> resolvedMethod = underTest.resolveInThisScopeOnly(new MethodSymbolSearch("UnderTest"));
        TestCase.assertTrue(resolvedMethod.isPresent());

        //Add another constructor that takes a String as an argument
        MethodSymbol constructor2 = support.addConstructor(underTest, stringType);
        resolvedMethod = underTest.resolveInThisScopeOnly(new MethodSymbolSearch(constructor2));
        TestCase.assertTrue(resolvedMethod.isPresent());

        //check constructor with String,Integer params does not exist then create it and check it does.
        SymbolSearch search = new MethodSymbolSearch("UnderTest")
                .addParameter(new VariableSymbol("arg1", stringType))
                .addParameter(new VariableSymbol("arg2", integerType));
        resolvedMethod = underTest.resolveInThisScopeOnly(search);
        TestCase.assertFalse(resolvedMethod.isPresent());
        support.addSyntheticConstructorIfRequired(underTest);
        //Now that construtor should exist
        resolvedMethod = underTest.resolveInThisScopeOnly(search);
        TestCase.assertTrue(resolvedMethod.isPresent());
    }

    @Test
    public void testVariableSymbol()
    {
        IScope symbolTable = new SymbolTable();
        ISymbol integerType = new AggregateSymbol("Integer", symbolTable);

        //define without type first
        VariableSymbol v1 = new VariableSymbol("v1");
        assertVariable1(v1);
        assertVariable1(v1.clone(symbolTable));

        VariableSymbol v2 = new VariableSymbol("v2", Optional.of(integerType));
        v2.setInitialisedBy(new SyntheticToken());
        v2.setIncomingParameter(true);
        v2.setRestrictedToPureCalls(true);

        TestCase.assertTrue(v2.isInitialised());
        TestCase.assertTrue(v2.isRestrictedToPureCalls());

        VariableSymbol v3 = new VariableSymbol("v3", Optional.of(integerType));
        v3.setReturningParameter(true);
        v3.setPrivate(true);
        TestCase.assertTrue(v3.isReturningParameter());
        TestCase.assertNotNull(v3.toString());
    }

    @Test
    public void testConstantSymbol()
    {
        IScope symbolTable = new SymbolTable();
        ISymbol integerType = new AggregateSymbol("Integer", symbolTable);
        ConstantSymbol c1 = new ConstantSymbol("1", true);
        c1.setType(integerType);
        c1.setAtModuleScope(true);
        c1.setNotMutable();

        assertConstant1(c1);
        assertConstant1(c1.clone(symbolTable));

        ConstantSymbol c2 = new ConstantSymbol("1", integerType, true);
        c2.setAtModuleScope(false);

        TestCase.assertTrue(c2.isAConstant());
        TestCase.assertTrue(c2.isMutable());
        TestCase.assertFalse(c2.isAtModuleScope());
        TestCase.assertTrue(c2.isFromLiteral());
        TestCase.assertTrue(c2.getGenus().equals(ISymbol.SymbolGenus.VALUE));

        ConstantSymbol c3 = new ConstantSymbol("1", integerType);
        TestCase.assertTrue(c3.getGenus().equals(ISymbol.SymbolGenus.VALUE));

    }

    private void assertConstant1(ConstantSymbol c)
    {
        TestCase.assertNotNull(c.getFriendlyName());
        TestCase.assertTrue(c.isAConstant());
        TestCase.assertFalse(c.isMutable());
        TestCase.assertTrue(c.isAtModuleScope());
        TestCase.assertTrue(c.isFromLiteral());
        TestCase.assertTrue(c.getGenus().equals(ISymbol.SymbolGenus.VALUE));
    }

    private void assertVariable1(VariableSymbol v)
    {
        TestCase.assertNotNull(v.getFriendlyName());
        TestCase.assertFalse(v.isAtModuleScope());
        TestCase.assertFalse(v.isAggregatePropertyField());
        TestCase.assertTrue(v.isMutable());
        TestCase.assertFalse(v.isIncomingParameter());
        TestCase.assertFalse(v.isPrivate());
        TestCase.assertTrue(v.isPublic());
        TestCase.assertFalse(v.isAggregatePropertyField());
        TestCase.assertFalse(v.isIncomingParameter());
        TestCase.assertFalse(v.isReturningParameter());
        TestCase.assertFalse(v.isInitialised());
        TestCase.assertFalse(v.isRestrictedToPureCalls());

    }
}
