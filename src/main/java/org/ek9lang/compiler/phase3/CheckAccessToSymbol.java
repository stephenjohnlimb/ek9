package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.AnySymbolSearch;
import org.ek9lang.compiler.symbols.IAggregateSymbol;

/**
 * Check that a symbol can be accessed issues error if not possible.
 */
final class CheckAccessToSymbol extends TypedSymbolAccess implements Consumer<CheckSymbolAccessData> {

  CheckAccessToSymbol(final SymbolAndScopeManagement symbolAndScopeManagement,
                      final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final CheckSymbolAccessData checkSymbolAccessData) {
    final var errorMsgBase = "'" + checkSymbolAccessData.symbolName()
        + "' on '"
        + checkSymbolAccessData.inScope().getFriendlyScopeName() + "':";

    if (checkSymbolAccessData.symbol().isPrivate()) {
      //As it is private the only place access is allowed is within the same 'type'
      checkPrivateAccess(errorMsgBase, checkSymbolAccessData);
    } else if (checkSymbolAccessData.symbol().isProtected()) {
      checkProtectedAccess(errorMsgBase, checkSymbolAccessData);
    }
    //else it is public and can just be accessed.
  }

  private void checkPrivateAccess(final String errorMsgBase, final CheckSymbolAccessData checkSymbolAccessData) {
    if (checkSymbolAccessData.fromScope() == checkSymbolAccessData.inScope()) {
      //The next question is, is the identifier resolved actually in this type or private in another type.
      var resolved = checkSymbolAccessData.inScope()
          .resolveInThisScopeOnly(new AnySymbolSearch(checkSymbolAccessData.symbolName()));
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

  private void checkProtectedAccess(final String errorMsgBase, final CheckSymbolAccessData checkSymbolAccessData) {
    //Need to deal with calls from outside of aggregates
    if (checkSymbolAccessData.fromScope() instanceof IAggregateSymbol calledFromAggregate) {
      if (checkSymbolAccessData.inScope() instanceof IAggregateSymbol resolvedInAggregate
          && !calledFromAggregate.isInAggregateHierarchy(resolvedInAggregate)) {
        //This will be OK as long as the fromScope and the inScope are in the same hierarchy.
        emitNoAccessToProtectedMethod(errorMsgBase, checkSymbolAccessData);
      }
    } else {
      //Not even from with any sort of aggregate (so a function).
      emitNoAccessToProtectedMethod(errorMsgBase, checkSymbolAccessData);
    }
  }

  private void emitNoAccessToProtectedMethod(final String errorMsgBase,
                                             final CheckSymbolAccessData checkSymbolAccessData) {
    var msg = "access from '" + checkSymbolAccessData.fromScope().getFriendlyScopeName() + "' to " + errorMsgBase;
    errorListener.semanticError(checkSymbolAccessData.token(), msg,
        ErrorListener.SemanticClassification.NOT_ACCESSIBLE);
  }
}
