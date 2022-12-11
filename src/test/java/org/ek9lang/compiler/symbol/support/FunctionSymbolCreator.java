package org.ek9lang.compiler.symbol.support;

import java.util.Optional;
import java.util.function.BiFunction;
import org.ek9lang.compiler.internals.Module;
import org.ek9lang.compiler.internals.Source;
import org.ek9lang.compiler.symbol.FunctionSymbol;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.tokenizer.SyntheticToken;

/**
 * Just used in testing to create a new function type
 */
public class FunctionSymbolCreator implements BiFunction<String, SymbolTable, ISymbol> {
  @Override
  public ISymbol apply(String functionName, SymbolTable inSymbolTable) {
    var newType = new FunctionSymbol(functionName, inSymbolTable);
    return new SyntheticSymbolCompletion().apply(newType, inSymbolTable.getScopeName());
  }
}
