package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Check that a symbol can be accessed issues error if not possible.
 */
final class AccessToSymbolOrError extends TypedSymbolAccess implements Consumer<SymbolAccessData> {

  AccessToSymbolOrError(final SymbolsAndScopes symbolsAndScopes,
                        final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final SymbolAccessData symbolAccessData) {

    final var errorMsgBase = "'" + symbolAccessData.symbolName()
        + "' on '"
        + symbolAccessData.inScope().getFriendlyScopeName() + "':";

    if (symbolAccessData.symbol().isPrivate()) {
      //As it is private the only place access is allowed is within the same 'type'
      checkPrivateAccess(errorMsgBase, symbolAccessData);
    } else if (symbolAccessData.symbol().isProtected()) {
      checkProtectedAccess(errorMsgBase, symbolAccessData);
    }
    //else it is public and can just be accessed.

  }

  private void checkPrivateAccess(final String errorMsgBase, final SymbolAccessData symbolAccessData) {

    if (symbolAccessData.fromScope() == symbolAccessData.inScope()) {

      //For methods we can just check the parent scopes match
      if (symbolAccessData.symbol() instanceof MethodSymbol methodSymbol
          && methodSymbol.getParentScope().equals(symbolAccessData.fromScope())) {
        return;
      }

      //But for Properties we must look up again.
      final var toResolveIn = symbolAccessData.inScope();
      final var search = new SymbolSearch(symbolAccessData.symbolName());

      if (toResolveIn.resolveInThisScopeOnly(search).isEmpty()) {
        emitNoAccessFromThisContext(errorMsgBase, symbolAccessData);
      }
    } else {
      //Error access to private data is only allowed in that same type
      emitNoAccessFromThisContext(errorMsgBase, symbolAccessData);
    }
  }

  private void checkProtectedAccess(final String errorMsgBase, final SymbolAccessData symbolAccessData) {

    //Need to deal with calls from outside of aggregates
    if (symbolAccessData.fromScope() instanceof IAggregateSymbol calledFromAggregate) {
      if (symbolAccessData.inScope() instanceof IAggregateSymbol resolvedInAggregate
          && !calledFromAggregate.isInAggregateHierarchy(resolvedInAggregate)) {
        //This will be OK as long as the fromScope and the inScope are in the same hierarchy.
        emitNoAccessFromThisContext(errorMsgBase, symbolAccessData);
      }
    } else {
      //Not even from with any sort of aggregate (so a function).
      emitNoAccessFromThisContext(errorMsgBase, symbolAccessData);
    }

  }

  private void emitNoAccessFromThisContext(final String errorMsgBase,
                                           final SymbolAccessData symbolAccessData) {

    final var msg = "access from '" + symbolAccessData.fromScope().getFriendlyScopeName() + "' to " + errorMsgBase;
    errorListener.semanticError(symbolAccessData.token(), msg,
        ErrorListener.SemanticClassification.NOT_ACCESSIBLE);

  }
}
