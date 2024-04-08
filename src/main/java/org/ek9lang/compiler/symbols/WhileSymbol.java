package org.ek9lang.compiler.symbols;

import java.io.Serial;

/**
 * EK9 'while' or 'do/while' type symbol - we need a scope because we can declare new variables as part of the
 * pre-flow semantics.
 * This can be used as a 'while' or a 'do/while'.
 */
public class WhileSymbol extends ScopedSymbol {

  @Serial
  private static final long serialVersionUID = 1L;

  private WhileSymbol(final IScope enclosingScope) {

    //The name will be set later
    super("?", enclosingScope);
    super.setCategory(SymbolCategory.CONTROL);

  }

  public WhileSymbol(final IScope enclosingScope, final boolean doWhile) {

    super(doWhile ? "Do/While" : "While", enclosingScope);
    super.setCategory(SymbolCategory.CONTROL);

  }

  @Override
  public WhileSymbol clone(final IScope withParentAsAppropriate) {

    return cloneIntoWhileSymbol(new WhileSymbol(withParentAsAppropriate));
  }

  protected WhileSymbol cloneIntoWhileSymbol(final WhileSymbol newCopy) {

    super.cloneIntoScopeSymbol(newCopy);

    return newCopy;
  }

}