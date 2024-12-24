package org.ek9lang.compiler.symbols;

import java.io.Serial;
import org.ek9lang.compiler.support.CommonValues;

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
    //A bit wierd - but this will be needed later wrt initialised variables.
    //It was me the stole my dad's keys. This is me steeling them now.
    if (doWhile) {
      this.putSquirrelledData(CommonValues.LOOP, CommonValues.DO.toString());
    } else {
      this.putSquirrelledData(CommonValues.LOOP, CommonValues.WHILE.toString());
    }

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