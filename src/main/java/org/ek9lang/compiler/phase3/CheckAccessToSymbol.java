package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.search.SymbolSearch;

/**
 * Check that a symbol can be accessed issues error if not possible.
 */
final class CheckAccessToSymbol extends RuleSupport implements Consumer<CheckSymbolAccessData> {

  CheckAccessToSymbol(final SymbolAndScopeManagement symbolAndScopeManagement,
                      final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(CheckSymbolAccessData checkSymbolAccessData) {
    final var errorMsgBase = "'" + checkSymbolAccessData.symbolName()
        + "' on '"
        + checkSymbolAccessData.inScope().getFriendlyScopeName() + "':";

    //TODO protected symbols.

    if (checkSymbolAccessData.symbol().isPrivate()) {
      //As it is private the only place access is allowed is within the same 'type'
      if (checkSymbolAccessData.fromScope() == checkSymbolAccessData.inScope()) {
        //The next question is, is the identifier resolved actually in this type or private in another type.
        var resolved = checkSymbolAccessData.inScope()
            .resolveInThisScopeOnly(new SymbolSearch(checkSymbolAccessData.symbolName()));
        if (resolved.isEmpty()) {
          //Error while trying to access in the same type as resolving on, the data is private in another type.
          errorListener.semanticError(checkSymbolAccessData.token(), errorMsgBase,
              ErrorListener.SemanticClassification.NOT_ACCESSIBLE);
        }
      } else {
        //Error access to private data is only allowed in that same type
        var msg = "access from '" + checkSymbolAccessData.fromScope().getFriendlyScopeName() + "' to " + errorMsgBase;
        errorListener.semanticError(checkSymbolAccessData.token(), msg,
            ErrorListener.SemanticClassification.NOT_ACCESSIBLE);
      }
    }
  }
}
