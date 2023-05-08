package org.ek9lang.compiler.main.resolvedefine;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.search.AnySymbolSearch;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;

/**
 * Ensures that 'identifierReference' is now resolved and hangs together and 'typed' or a not resolved error.
 */
public class CheckValidIdentifierReference
    implements Function<EK9Parser.IdentifierReferenceContext, Optional<ISymbol>> {

  private final SymbolAndScopeManagement symbolAndScopeManagement;

  private final ErrorListener errorListener;

  /**
   * Checks identifier reference now resolves.
   */
  public CheckValidIdentifierReference(final SymbolAndScopeManagement symbolAndScopeManagement,
                                       final ErrorListener errorListener) {
    this.symbolAndScopeManagement = symbolAndScopeManagement;
    this.errorListener = errorListener;
  }

  @Override
  public Optional<ISymbol> apply(final EK9Parser.IdentifierReferenceContext ctx) {
    var identifierReference = symbolAndScopeManagement.getRecordedSymbol(ctx);

    if (identifierReference == null) {
      //This has not yet been resolved.
      //We now must resolve this, it's an error and compilation cannot continue if not resolved.
      var currentScope = symbolAndScopeManagement.getTopScope();
      var resolved = currentScope.resolve(new AnySymbolSearch(ctx.getText()));
      if (resolved.isPresent()) {
        identifierReference = resolved.get();
        symbolAndScopeManagement.recordSymbol(identifierReference, ctx);
      } else {
        errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.NOT_RESOLVED);
      }
    }
    if (identifierReference != null) {
      identifierReferenceChecks(ctx, identifierReference);
    }
    return Optional.ofNullable(identifierReference);
  }

  /**
   * Complete various check and updates to the identifierReference now that it has been resolved.
   */
  private void identifierReferenceChecks(final EK9Parser.IdentifierReferenceContext ctx, ISymbol identifierReference) {
    //It has now been looked up and resolved in some way and so has been referenced.
    identifierReference.setReferenced(true);

    if (identifierReference.isVariable()) {
      identifierReferenceAccessCheck(ctx, identifierReference);
      identifierReferenceDefinedBeforeUseCheck(ctx, identifierReference);
      identifierReferenceInitialisedBeforeUseCheck(ctx, identifierReference);
    }
  }

  private void identifierReferenceInitialisedBeforeUseCheck(final EK9Parser.IdentifierReferenceContext ctx,
                                                            final ISymbol identifierReference) {
    //Only interested finite range of checks here.

    var initialisedByToken = identifierReference.getInitialisedBy();

    if (!identifierReference.isConstant()
        && ctx.start.getTokenSource().equals(identifierReference.getSourceToken().getTokenSource())
        && (initialisedByToken == null || ctx.start.getTokenIndex() <= initialisedByToken.getTokenIndex())) {
      errorListener.semanticError(ctx.start, errorMessageForIdentifierReference(identifierReference),
          ErrorListener.SemanticClassification.USED_BEFORE_INITIALISED);
    }
  }

  private void identifierReferenceDefinedBeforeUseCheck(final EK9Parser.IdentifierReferenceContext ctx,
                                                        final ISymbol identifierReference) {
    //Firstly check in the same source - else it means its on a type or constant and that's fine.
    var sourceToken = identifierReference.getSourceToken();
    if (!identifierReference.isConstant() && sourceToken != null
        && ctx.start.getTokenSource().equals(sourceToken.getTokenSource())
        && ctx.start.getTokenIndex() <= sourceToken.getTokenIndex()) {
      errorListener.semanticError(ctx.start, errorMessageForIdentifierReference(identifierReference),
          ErrorListener.SemanticClassification.USED_BEFORE_DEFINED);
    }
  }

  private void identifierReferenceAccessCheck(final EK9Parser.IdentifierReferenceContext ctx,
                                              final ISymbol identifierReference) {
    if (!identifierReference.isPublic() && identifierReference.isPropertyField()) {
      //Might still be accessible if within the same aggregate/function/dynamic
      var resolvedDynamicScope = symbolAndScopeManagement.traverseBackUpStack(IScope.ScopeType.DYNAMIC_BLOCK);
      resolvedDynamicScope.ifPresentOrElse(scope -> accessCheckToScope(scope, ctx, identifierReference),
          () -> {
            var resolvedMainScope = symbolAndScopeManagement.traverseBackUpStack(IScope.ScopeType.NON_BLOCK);
            resolvedMainScope.ifPresent(scope -> accessCheckToScope(scope, ctx, identifierReference));
          });
    }
  }

  private void accessCheckToScope(final IScope scope,
                                  final EK9Parser.IdentifierReferenceContext ctx,
                                  final ISymbol identifierReference) {

    var resolved = scope.resolveInThisScopeOnly(new SymbolSearch(identifierReference.getName()));
    if (resolved.isEmpty()) {
      errorListener.semanticError(ctx.start, errorMessageForIdentifierReference(identifierReference),
          ErrorListener.SemanticClassification.NOT_ACCESSIBLE);
    }
  }

  private String errorMessageForIdentifierReference(final ISymbol identifierReference) {
    return "reference to '"
        + identifierReference.getFriendlyName()
        + "' on line: "
        + identifierReference.getSourceToken().getLine() + ":";
  }
}