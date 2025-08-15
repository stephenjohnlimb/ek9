package org.ek9lang.compiler.phase7.support;

import java.util.function.Function;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.core.AssertValue;

/**
 * Get the name of the variable suitable for use in the IR.
 * Note that is a variable and property then prefixed with "this.".
 */
public final class VariableNameForIR implements Function<ISymbol, String> {

  @Override
  public String apply(final ISymbol symbol) {

    AssertValue.checkNotNull("Symbol cannot be null", symbol);

    if (symbol instanceof VariableSymbol varSymbol && varSymbol.isPropertyField()) {
      // Use "this.fieldName" for property fields
      return "this." + symbol.getName();
    }

    return symbol.getName();
  }
}
