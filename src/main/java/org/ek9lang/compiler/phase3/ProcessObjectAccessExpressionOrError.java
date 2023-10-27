package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.CompilerException;

/**
 * This is the second part of the method/function chaining resolution and can be recursive via objectAccess.
 * <pre>
 *   objectAccessExpression
 *     : objectAccessStart objectAccess
 *     ;
 * objectAccessStart
 *     :  (primaryReference | identifier | call)
 *     ;
 * objectAccess
 *     : objectAccessType objectAccess?
 *     ;
 * objectAccessType
 *     : DOT (identifier | operationCall)
 * </pre>
 */
final class ProcessObjectAccessExpressionOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.ObjectAccessExpressionContext> {
  private final ProcessFieldOrError processFieldOrError;
  private final ProcessOperationCallOrError processOperationCallOrError;

  ProcessObjectAccessExpressionOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                                       final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);

    this.processFieldOrError = new ProcessFieldOrError(symbolAndScopeManagement, errorListener);
    this.processOperationCallOrError = new ProcessOperationCallOrError(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.ObjectAccessExpressionContext ctx) {

    var objectAccessStartSymbol = getRecordedAndTypedSymbol(ctx.objectAccessStart());
    if (objectAccessStartSymbol != null) {
      //If it is null then there will have already been an error

      //Now we have to follow the objectAccess and objectAccessType objectAccess?
      //But this has to be driven from here - rather than bottom up, because the context of resolution
      //is driven that way.
      var searchOnThisSymbol = objectAccessStartSymbol;
      var accessContext = ctx.objectAccess();

      boolean hasMoreInAccessChain;
      do {
        //Keep resolving in the chain until end of chain or a failure to resolve.
        resolveObjectAccess(accessContext, searchOnThisSymbol);

        searchOnThisSymbol = getRecordedAndTypedSymbol(accessContext);
        hasMoreInAccessChain = accessContext.objectAccess() != null && searchOnThisSymbol != null;
        if (hasMoreInAccessChain) {
          accessContext = accessContext.objectAccess();
        }
      } while (hasMoreInAccessChain);

      //Now whatever is left in searchOnThisSymbol is the end of the chain and if not null we record it.
      if (searchOnThisSymbol != null) {
        recordATypedSymbol(searchOnThisSymbol, ctx);
      }
    }
  }

  private void resolveObjectAccess(final EK9Parser.ObjectAccessContext ctx, final ISymbol inThisSymbol) {

    inThisSymbol.getType().ifPresent(type -> {
      if (type instanceof IAggregateSymbol aggregate) {
        if (ctx.objectAccessType().identifier() != null) {
          processIdentifierUse(ctx, aggregate);
        } else {
          processOperationCallUse(ctx, aggregate);
        }
      }
    });
  }

  private void processIdentifierUse(final EK9Parser.ObjectAccessContext ctx, final IAggregateSymbol aggregate) {
    var resolved = processFieldOrError.apply(ctx.objectAccessType().identifier(), aggregate);
    if (resolved != null) {
      //For completeness record against the identifier as well
      recordATypedSymbol(resolved, ctx.objectAccessType().identifier());
      //Now also record the same call against the ctx.
      recordATypedSymbol(resolved, ctx);
    }
  }

  private void processOperationCallUse(final EK9Parser.ObjectAccessContext ctx, final IAggregateSymbol aggregate) {
    var resolved = processOperationCallOrError.apply(ctx.objectAccessType().operationCall(), aggregate);
    if (resolved != null) {
      var existingCallSymbol = symbolAndScopeManagement.getRecordedSymbol(ctx.objectAccessType().operationCall());
      if (existingCallSymbol instanceof CallSymbol callSymbol) {
        callSymbol.setResolvedSymbolToCall(resolved);
        //Now also record the same call against the ctx.
        recordATypedSymbol(existingCallSymbol, ctx);
      } else {
        throw new CompilerException("Expecting a callSymbol 'receptacle' to be present");
      }
    }
  }
}
