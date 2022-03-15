package org.ek9lang.compiler.symbol.support;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.ek9lang.compiler.symbol.*;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;

import java.util.Optional;

/**
 * Designed to test a range of aggregate class constructs, such as EK9
 * classes, traits, components - all use the AggregateSymbol
 *
 * This is a major test as it tests Symbol tables, variable symbols and method symbols
 */
public class AggregateSymbolTest
{
    @Test
    public void testEmptyAggregate()
    {
        SymbolTable symbolTable1 = new SymbolTable();

        AggregateSymbol underTest = new AggregateSymbol("UnderTest", symbolTable1);
        assertTrue("UnderTest".equals(underTest.getAggregateDescription()));

        SymbolTable symbolTable2 = new SymbolTable();

        AggregateSymbol cloned1 = underTest.clone(symbolTable2);

        //Alter name to check cloned version is truly cloned.
        //Also check that the type has changed
        underTest.setName("Modified");
        //Just set a range of flags positive and check they stick and cloned version does not have them
        underTest.setAggregateDescription("SomeModifiedAggregate");
        underTest.setMarkedAbstract(true);
        underTest.setInjectable(true);
        underTest.setMarkedAsDispatcher(true);
        underTest.setOpenForExtension(true);
        underTest.setPipeSinkType(Optional.of("ASinkType"));
        underTest.setPipeSourceType(Optional.of("ASourceType"));

        //Now make another clone with values set.
        SymbolTable symbolTable3 = new SymbolTable();
        AggregateSymbol cloned2 = underTest.clone(symbolTable3);

        assertTrue("Modified".equals(underTest.getName()));
        assertTrue("Modified".equals(underTest.getType().get().getName()));
        assertTrue(underTest.isMarkedAbstract());
        assertTrue(underTest.isInjectable());
        assertTrue(underTest.isMarkedAsDispatcher());
        assertTrue(underTest.isOpenForExtension());
        assertTrue(underTest.getPipeSinkType().isPresent());
        assertTrue("ASinkType".equals(underTest.getPipeSinkType().get()));
        assertTrue(underTest.getPipeSourceType().isPresent());
        assertTrue("ASourceType".equals(underTest.getPipeSourceType().get()));

        assertNotNull(cloned1);
        assertTrue("UnderTest".equals(cloned1.getName()));
        assertTrue("UnderTest".equals(cloned1.getType().get().getName()));
        assertFalse(cloned1.isMarkedAbstract());
        assertFalse(cloned1.isInjectable());
        assertFalse(cloned1.isMarkedAsDispatcher());
        assertFalse(cloned1.isOpenForExtension());
        assertFalse(cloned1.getPipeSinkType().isPresent());
        assertFalse(cloned1.getPipeSourceType().isPresent());

        assertNotNull(cloned2);
        assertTrue("Modified".equals(cloned2.getName()));
        assertTrue("Modified".equals(cloned2.getType().get().getName()));
        assertTrue(cloned2.isMarkedAbstract());
        assertTrue(cloned2.isInjectable());
        assertTrue(cloned2.isMarkedAsDispatcher());
        assertTrue(cloned2.isOpenForExtension());
        assertTrue(cloned2.getPipeSinkType().isPresent());
        assertTrue("ASinkType".equals(cloned2.getPipeSinkType().get()));
        assertTrue(cloned2.getPipeSourceType().isPresent());
        assertTrue("ASourceType".equals(cloned2.getPipeSourceType().get()));
    }

    @Test
    public void testAggregateMethod()
    {
        IScope symbolTable = new SymbolTable();

        AggregateSymbol integerType = new AggregateSymbol("Integer", symbolTable);
        symbolTable.define(integerType);
        AggregateSymbol stringType = new AggregateSymbol("String", symbolTable);
        symbolTable.define(stringType);

        AggregateSymbol underTest = new AggregateSymbol("UnderTest", symbolTable);

        MethodSymbol method = new MethodSymbol("method1", integerType, underTest);
        method.define(new VariableSymbol("arg1", stringType));
        underTest.define(method);

        //Now search and find that method, must add in the param to be able to resolve it.
        Optional<ISymbol> resolvedMethod = underTest.resolveInThisScopeOnly(new MethodSymbolSearch("method1").addParameter(new VariableSymbol("arg1", stringType)));
        assertTrue(resolvedMethod.isPresent());
        assertSymbol(resolvedMethod, "method1", "Integer");

        //Now clone and check again
        AggregateSymbol cloned = underTest.clone(symbolTable);
        assertNotNull(cloned);
        resolvedMethod = cloned.resolveInThisScopeOnly(new MethodSymbolSearch("method1").addParameter(new VariableSymbol("arg1", stringType)));
        assertTrue(resolvedMethod.isPresent());
        assertSymbol(resolvedMethod, "method1", "Integer");

        //Now alter method1 cloned version but original should stay as is.
        MethodSymbol toAlter = (MethodSymbol)resolvedMethod.get();
        toAlter.setType(stringType);
        toAlter.define(new VariableSymbol("arg2", integerType));

        //Test original is still OK
        resolvedMethod = underTest.resolveInThisScopeOnly(new MethodSymbolSearch("method1").addParameter(new VariableSymbol("arg1", stringType)));
        assertTrue(resolvedMethod.isPresent());
        assertSymbol(resolvedMethod, "method1", "Integer");

        //Now check cloned one has been altered
        resolvedMethod = cloned.resolveInThisScopeOnly(new MethodSymbolSearch("method1").addParameter(new VariableSymbol("arg1", stringType)));
        assertFalse(resolvedMethod.isPresent());

        resolvedMethod = cloned.resolveInThisScopeOnly(
                new MethodSymbolSearch("method1")
                        .addParameter(new VariableSymbol("arg1", stringType))
                        .addParameter(new VariableSymbol("arg2", integerType)));
        assertTrue(resolvedMethod.isPresent());
        assertSymbol(resolvedMethod, "method1", "String");

    }

    @Test
    public void testAggregateProperty()
    {
        IScope symbolTable = new SymbolTable();

        AggregateSymbol integerType = new AggregateSymbol("Integer", symbolTable);
        symbolTable.define(integerType);
        AggregateSymbol stringType = new AggregateSymbol("String", symbolTable);
        symbolTable.define(stringType);

        AggregateSymbol underTest = new AggregateSymbol("UnderTest", symbolTable);
        assertTrue("UnderTest".equals(underTest.getAggregateDescription()));

        VariableSymbol prop1 = new VariableSymbol("prop1", integerType);
        underTest.define(prop1);

        Optional<ISymbol> resolvedProperty = underTest.resolveInThisScopeOnly(new SymbolSearch("prop1"));
        assertSymbol(resolvedProperty, "prop1", "Integer");

        AggregateSymbol cloned = underTest.clone(symbolTable);
        assertNotNull(cloned);

        //Now check same can be found on the cloned version
        resolvedProperty = cloned.resolveInThisScopeOnly(new SymbolSearch("prop1"));
        assertSymbol(resolvedProperty, "prop1", "Integer");

        //Now alter prop1 type on the cloned version but original should stay as is.
        resolvedProperty.get().setType(stringType);

        //Search and find again to check.
        resolvedProperty = underTest.resolveInThisScopeOnly(new SymbolSearch("prop1"));
        assertSymbol(resolvedProperty, "prop1", "Integer");

        //But cloned should be String
        resolvedProperty = cloned.resolveInThisScopeOnly(new SymbolSearch("prop1"));
        assertSymbol(resolvedProperty, "prop1", "String");
    }

    private void assertSymbol(Optional<ISymbol> resolvedSymbol, String expectedName, String expectedTypeName)
    {
        assertTrue(resolvedSymbol.isPresent());
        assertTrue(expectedName.equals(resolvedSymbol.get().getName()));
        assertTrue(expectedTypeName.equals(resolvedSymbol.get().getType().get().getName()));
    }
}
