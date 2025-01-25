package org.ek9lang.compiler.symbols;

import java.io.Serial;
import java.io.Serializable;

/**
 * Special type that is super of Aggregate and super of Function (if they don't have supers).
 */
public class AnyTypeSymbol extends AggregateSymbol implements IFunctionSymbol, Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  public AnyTypeSymbol(String name, IScope enclosingScope) {
    super(name, enclosingScope);
    setGenus(SymbolGenus.ANY);
    setCategory(SymbolCategory.ANY);
    setOpenForExtension(true);
  }
}
