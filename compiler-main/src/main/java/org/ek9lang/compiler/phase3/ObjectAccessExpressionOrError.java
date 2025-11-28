package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NOT_MUTABLE;

import java.util.function.Consumer;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.SymbolCategory;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;
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
final class ObjectAccessExpressionOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.ObjectAccessExpressionContext> {
  private final PropertyFieldOrError propertyFieldOrError;
  private final OperationCallOrError operationCallOrError;

  ObjectAccessExpressionOrError(final SymbolsAndScopes symbolsAndScopes,
                                final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

    this.propertyFieldOrError = new PropertyFieldOrError(symbolsAndScopes, errorListener);
    this.operationCallOrError = new OperationCallOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.ObjectAccessExpressionContext ctx) {

    final var objectAccessStartSymbol = getRecordedAndTypedSymbol(ctx.objectAccessStart());
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
        if (searchOnThisSymbol != null) {
          notOperatorOnEnumerationTypeOrError(accessContext.start, objectAccessStartSymbol, searchOnThisSymbol);
        }
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

  private void notOperatorOnEnumerationTypeOrError(final Token errorLocation,
                                                   final ISymbol varOrTypeSymbol,
                                                   final ISymbol locatedSymbol) {

    if (varOrTypeSymbol.getGenus().equals(SymbolGenus.CLASS_ENUMERATION)
        && varOrTypeSymbol.getCategory().equals(SymbolCategory.TYPE)
        && !locatedSymbol.getCategory().equals(SymbolCategory.VARIABLE)) {

      final var msg = "'" + varOrTypeSymbol.getName() + "' and '" + locatedSymbol.getName() + ":";
      errorListener.semanticError(errorLocation, msg,
          ErrorListener.SemanticClassification.OPERATOR_CANNOT_BE_USED_ON_ENUMERATION);
    }

  }


  private void resolveObjectAccess(final EK9Parser.ObjectAccessContext ctx, final ISymbol inThisSymbol) {


    inThisSymbol.getType().ifPresent(type -> {
      if (type instanceof IAggregateSymbol aggregate) {
        if (ctx.objectAccessType().identifier() != null) {
          identifierUse(ctx, aggregate);
        } else {
          operationCallUse(ctx, inThisSymbol, aggregate);
        }
      }
    });

  }

  private void identifierUse(final EK9Parser.ObjectAccessContext ctx, final IAggregateSymbol aggregate) {

    final var resolved = propertyFieldOrError.apply(ctx.objectAccessType().identifier(), aggregate);

    if (resolved != null) {
      //For completeness record against the identifier as well
      recordATypedSymbol(resolved, ctx.objectAccessType().identifier());
      //Now also record the same call against the ctx.
      recordATypedSymbol(resolved, ctx);
    }

  }

  private void operationCallUse(final EK9Parser.ObjectAccessContext ctx,
                                final ISymbol fromSymbol,
                                final IAggregateSymbol aggregate) {

    final var resolved = operationCallOrError.apply(ctx.objectAccessType().operationCall(), aggregate);

    if (resolved != null) {

      //Now when an ek9 developer has a real ek9 constant and calls a mutable method on it
      //we must emit an error indicating that the constant cannot be mutated.
      final var isConstantVariable = fromSymbol.isConstant() && fromSymbol.isVariable();
      if (isConstantVariable && resolved.isNotMarkedPure()) {
        emitNotMutableError(new Ek9Token(ctx.start), fromSymbol);
      }

      final var existingCallSymbol = symbolsAndScopes.getRecordedSymbol(ctx.objectAccessType().operationCall());

      if (existingCallSymbol instanceof CallSymbol callSymbol) {
        callSymbol.setResolvedSymbolToCall(resolved);
        //Now also record the same call against the ctx.
        recordATypedSymbol(existingCallSymbol, ctx);
      } else {
        throw new CompilerException("Expecting a callSymbol 'receptacle' to be present");
      }
    }
  }

  private void emitNotMutableError(final IToken locationForError, final ISymbol symbol) {
    errorListener.semanticError(locationForError, "'" + symbol.getFriendlyName() + "' cannot be changed:",
        NOT_MUTABLE);
  }
}
