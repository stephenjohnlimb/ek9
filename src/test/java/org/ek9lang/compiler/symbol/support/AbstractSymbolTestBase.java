package org.ek9lang.compiler.symbol.support;

import org.ek9lang.compiler.symbol.AggregateSymbol;
import org.ek9lang.compiler.symbol.IScope;
import org.junit.Before;

public class AbstractSymbolTestBase
{
	protected AggregateSupport support = new AggregateSupport();
	protected IScope symbolTable = new SymbolTable();

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
}
