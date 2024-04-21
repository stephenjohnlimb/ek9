package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

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

      //For methods we can just check the parent scopes match
      if (checkSymbolAccessData.symbol() instanceof MethodSymbol methodSymbol
          && methodSymbol.getParentScope().equals(checkSymbolAccessData.fromScope())) {
        return;
      }

      //But for Properties we must look up again.
      final var toResolveIn = checkSymbolAccessData.inScope();
      final var search = new SymbolSearch(checkSymbolAccessData.symbolName());

      final var resolved = toResolveIn.resolveInThisScopeOnly(search);
      if (resolved.isEmpty()) {
        emitNoAccessFromThisContext(errorMsgBase, checkSymbolAccessData);
      }
    } else {
      //Error access to private data is only allowed in that same type
      emitNoAccessFromThisContext(errorMsgBase, checkSymbolAccessData);
    }
  }

  private void checkProtectedAccess(final String errorMsgBase, final CheckSymbolAccessData checkSymbolAccessData) {

    //Need to deal with calls from outside of aggregates
    if (checkSymbolAccessData.fromScope() instanceof IAggregateSymbol calledFromAggregate) {
      if (checkSymbolAccessData.inScope() instanceof IAggregateSymbol resolvedInAggregate
          && !calledFromAggregate.isInAggregateHierarchy(resolvedInAggregate)) {
        //This will be OK as long as the fromScope and the inScope are in the same hierarchy.
        emitNoAccessFromThisContext(errorMsgBase, checkSymbolAccessData);
      }
    } else {
      //Not even from with any sort of aggregate (so a function).
      emitNoAccessFromThisContext(errorMsgBase, checkSymbolAccessData);
    }

  }

  private void emitNoAccessFromThisContext(final String errorMsgBase,
                                           final CheckSymbolAccessData checkSymbolAccessData) {

    final var msg = "access from '" + checkSymbolAccessData.fromScope().getFriendlyScopeName() + "' to " + errorMsgBase;
    errorListener.semanticError(checkSymbolAccessData.token(), msg,
        ErrorListener.SemanticClassification.NOT_ACCESSIBLE);

  }
}
