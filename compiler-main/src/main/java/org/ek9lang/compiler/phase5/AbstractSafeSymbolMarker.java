package org.ek9lang.compiler.phase5;

import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Once an expression has been assessed as being simple enough to process, this
 * consumer is called to mark the appropriate symbol(s) used in the expression as safe if the
 * appropriate methods are called.
 */
abstract class AbstractSafeSymbolMarker extends TypedSymbolAccess {

  protected final HasTypeOfGeneric resultTypeCheck;
  protected final HasTypeOfGeneric optionalTypeCheck;
  protected final HasTypeOfGeneric iteratorTypeCheck;

  /**
   * Constructor to provided typed access.
   */
  protected AbstractSafeSymbolMarker(final SymbolsAndScopes symbolsAndScopes, final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);
    this.resultTypeCheck = new HasTypeOfGeneric(symbolsAndScopes.getEk9Types().ek9Result());
    this.optionalTypeCheck = new HasTypeOfGeneric(symbolsAndScopes.getEk9Types().ek9Optional());
    this.iteratorTypeCheck = new HasTypeOfGeneric(symbolsAndScopes.getEk9Types().ek9Iterator());
  }

  protected void assessIsSetCall(final ISymbol toBeAssessed, final IScope scopeMadeSafe) {
    if (resultTypeCheck.test(toBeAssessed)) {
      symbolsAndScopes.markOkResultAccessSafe(toBeAssessed, scopeMadeSafe);
    } else if (optionalTypeCheck.test(toBeAssessed)) {
      symbolsAndScopes.markGetOptionalAccessSafe(toBeAssessed, scopeMadeSafe);
    } else if (iteratorTypeCheck.test(toBeAssessed)) {
      symbolsAndScopes.markNextIteratorAccessSafe(toBeAssessed, scopeMadeSafe);
    }

    //Else nothing to assess just a normal variable
  }
}
