package org.ek9lang.compiler.phase3;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.AnySymbolSearch;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Ensures that 'identifierReference' is now resolved and hangs together and 'typed' or a not resolved error.
 */
final class CheckValidIdentifierReference extends TypedSymbolAccess
    implements Function<EK9Parser.IdentifierReferenceContext, Optional<ISymbol>> {


  /**
   * Checks identifier reference now resolves.
   */
  CheckValidIdentifierReference(final SymbolAndScopeManagement symbolAndScopeManagement,
                                final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public Optional<ISymbol> apply(final EK9Parser.IdentifierReferenceContext ctx) {
    var identifierReference = getRecordedAndTypedSymbol(ctx);

    if (identifierReference == null) {
      //This has not yet been resolved.
      //We now must resolve this, it's an error and compilation cannot continue if not resolved.
      var currentScope = symbolAndScopeManagement.getTopScope();
      var resolved = currentScope.resolve(new AnySymbolSearch(ctx.getText()));
      if (resolved.isPresent()) {
        identifierReference = resolved.get();
        recordATypedSymbol(identifierReference, ctx);
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
    var startToken = new Ek9Token(ctx.start);
    var token = identifierReference.getInitialisedBy();

    if (isIdentifierReferenceToBeChecked(identifierReference)
        && startToken.getSourceName().equals(identifierReference.getSourceToken().getSourceName())
        && (token == null || startToken.getTokenIndex() <= token.getTokenIndex())) {
      errorListener.semanticError(ctx.start, errorMessageForIdentifierReference(identifierReference),
          ErrorListener.SemanticClassification.USED_BEFORE_INITIALISED);
    }
  }

  private void identifierReferenceDefinedBeforeUseCheck(final EK9Parser.IdentifierReferenceContext ctx,
                                                        final ISymbol identifierReference) {
    //Firstly check in the same source - else it means its on a type or constant and that's fine.
    var startToken = new Ek9Token(ctx.start);
    var token = identifierReference.getSourceToken();
    if (isIdentifierReferenceToBeChecked(identifierReference)
        && token != null
        && startToken.getSourceName().equals(token.getSourceName())
        && startToken.getTokenIndex() <= token.getTokenIndex()) {
      errorListener.semanticError(ctx.start, errorMessageForIdentifierReference(identifierReference),
          ErrorListener.SemanticClassification.USED_BEFORE_DEFINED);
    }
  }

  private boolean isIdentifierReferenceToBeChecked(final ISymbol identifierReference) {
    return !identifierReference.isConstant()
        && !identifierReference.isPropertyField()
        && !identifierReference.isIncomingParameter();
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