package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.AnySymbolSearch;
import org.ek9lang.compiler.search.MethodSearchInScope;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.PossibleMatchingMethods;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.support.MostSpecificScope;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Ensures that 'identifierReference' is now resolved and hangs together and is 'typed' or emit "not resolved" error.
 * Note that while the identifierReference may get resolved also see ResolveIdentifierReferenceCallOrError.
 * This is because the context of the identifierReference may mean that it is important to 're-resolve' it.
 * Indeed, for overloaded methods it probably will end up resolving to a different method.
 * {@link CallOrError}
 * {@link org.ek9lang.compiler.phase3.ResolveIdentifierReferenceCallOrError}
 */
final class IdentifierReferenceOrError extends TypedSymbolAccess
    implements Function<EK9Parser.IdentifierReferenceContext, Optional<ISymbol>> {

  private final MostSpecificScope mostSpecificScope;
  private final PossibleMatchingMethods possibleMatchingMethods = new PossibleMatchingMethods();

  /**
   * This is the order we need to try and resolve the identifierReference.
   */
  private final List<Function<EK9Parser.IdentifierReferenceContext, ISymbol>> identifierReferenceResolvers;

  /**
   * Checks identifier reference now resolves.
   */
  IdentifierReferenceOrError(final SymbolsAndScopes symbolsAndScopes,
                             final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.mostSpecificScope = new MostSpecificScope(symbolsAndScopes);

    this.identifierReferenceResolvers = List.of(
        symbolsAndScopes::getRecordedSymbol,
        this::resolveViaCurrentScope,
        this::resolveMethodByNameInNearestBlockScope);

  }

  @Override
  public Optional<ISymbol> apply(final EK9Parser.IdentifierReferenceContext ctx) {

    final var identifierReference = resolveIdentifierReference(ctx);

    //Above will issue errors if needs be
    if (identifierReference != null) {
      identifierReferenceUseValidOrError(ctx, identifierReference);
      //Note that we must also record this against the underlying identifier as well
      final var identifierSymbol = symbolsAndScopes.getRecordedSymbol(ctx.identifier());
      if (identifierSymbol == null) {
        symbolsAndScopes.recordSymbol(identifierReference, ctx.identifier());
      }
    }

    return Optional.ofNullable(identifierReference);
  }

  private ISymbol resolveIdentifierReference(final EK9Parser.IdentifierReferenceContext ctx) {

    //Try each until a resolved or null if not resolved.
    return identifierReferenceResolvers
        .stream()
        .map(resolver -> resolver.apply(ctx))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private ISymbol resolveViaCurrentScope(final EK9Parser.IdentifierReferenceContext ctx) {

    final var currentScope = symbolsAndScopes.getTopScope();

    //Might only be a variable/delegate property reference
    //OK if in a call context it would only be appropriate to search for functions calls

    Optional<ISymbol> resolved;

    if (ctx.getParent() instanceof EK9Parser.CallContext) {
      //Even though this is a call it could be a delegate which is variable.
      resolved = currentScope.resolve(new AnySymbolSearch(ctx.getText()));
    } else {
      resolved = currentScope.resolve(new SymbolSearch(ctx.getText()));
    }

    resolved.ifPresent(identifierReference -> recordATypedSymbol(identifierReference, ctx));
    return resolved.orElse(null);
  }

  private ISymbol resolveMethodByNameInNearestBlockScope(final EK9Parser.IdentifierReferenceContext ctx) {

    final var scope = mostSpecificScope.get();
    final var searchDetails = new MethodSearchInScope(scope, new MethodSymbolSearch(ctx.getText()));

    //At this point with method overload there could be multiple with the right name.
    //Just use the first for the time being, but ResolveIdentifierReferenceCallOrError will do the
    //check by resolving with the name of this method and the correct parameters.
    final var possibleMethods = possibleMatchingMethods.apply(searchDetails);

    if (possibleMethods.isEmpty()) {
      return null;
    }

    final var identifierReference = possibleMethods.get(0);
    recordATypedSymbol(identifierReference, ctx);

    return identifierReference;
  }

  /**
   * Complete various check and updates to the identifierReference now that it has been resolved.
   */
  private void identifierReferenceUseValidOrError(final EK9Parser.IdentifierReferenceContext ctx,
                                                  ISymbol identifierReference) {

    //It has now been looked up and resolved in some way and so has been referenced.
    identifierReference.setReferenced(true);

    //Only run these checks on variables within a block. Due to multi-pass we need to check that
    //a variable is actually defined before it is referenced and used.
    if (identifierReference.isVariable()) {
      identifierReferenceAccessOrError(ctx, identifierReference);
      identifierReferenceDefinedBeforeUseOrError(ctx, identifierReference);
    }

  }

  private void identifierReferenceDefinedBeforeUseOrError(final EK9Parser.IdentifierReferenceContext ctx,
                                                          final ISymbol identifierReference) {

    //Firstly check in the same source - else it means its on a type or constant and that's fine.
    final var startToken = new Ek9Token(ctx.start);
    final var identifierReferenceToken = identifierReference.getSourceToken();

    if (identifierReferenceIsToBeChecked(identifierReference)
        && identifierReferenceUsedBeforeDefinition(startToken, identifierReferenceToken)) {
      errorListener.semanticError(ctx.start, errorMessageForIdentifierReference(identifierReference),
          ErrorListener.SemanticClassification.USED_BEFORE_DEFINED);
    }

  }

  private boolean identifierReferenceUsedBeforeDefinition(final IToken startToken,
                                                          final IToken identifierReferenceToken) {

    return startToken.getSourceName().equals(identifierReferenceToken.getSourceName())
        && startToken.getTokenIndex() <= identifierReferenceToken.getTokenIndex();
  }

  private boolean identifierReferenceIsToBeChecked(final ISymbol identifierReference) {

    return !identifierReference.isConstant()
        && !identifierReference.isPropertyField()
        && !identifierReference.isIncomingParameter();
  }

  private void identifierReferenceAccessOrError(final EK9Parser.IdentifierReferenceContext ctx,
                                                final ISymbol identifierReference) {

    if (!identifierReference.isPublic() && identifierReference.isPropertyField()) {

      //Might still be accessible if within the same aggregate/function/dynamic
      final var resolvedDynamicScope = symbolsAndScopes.traverseBackUpStack(IScope.ScopeType.DYNAMIC_BLOCK);

      resolvedDynamicScope.ifPresentOrElse(scope -> accessToScopeOrError(scope, ctx, identifierReference),
          () -> {
            var resolvedMainScope = symbolsAndScopes.traverseBackUpStack(IScope.ScopeType.NON_BLOCK);
            resolvedMainScope.ifPresent(scope -> accessToScopeOrError(scope, ctx, identifierReference));
          });
    }

  }

  private void accessToScopeOrError(final IScope scope,
                                    final EK9Parser.IdentifierReferenceContext ctx,
                                    final ISymbol identifierReference) {

    final var resolved = scope.resolveInThisScopeOnly(new SymbolSearch(identifierReference.getName()));

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