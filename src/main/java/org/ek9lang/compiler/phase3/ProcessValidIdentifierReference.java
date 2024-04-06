package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
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

/**
 * Ensures that 'identifierReference' is now resolved and hangs together and 'typed' or a not resolved error.
 * Note that while the identifierReference may get resolved also see ResolveIdentifierReferenceCallOrError.
 * This is because the context of the identifierReference may mean that it is important to 're-resolve' it.
 * Indeed, for overloaded methods it probably will end up resolving to a different method.
 * The same applies to it resolving to a function or even a function delegate.
 * {@link org.ek9lang.compiler.phase3.CheckValidCall}
 * {@link org.ek9lang.compiler.phase3.ResolveIdentifierReferenceCallOrError}
 */
final class ProcessValidIdentifierReference extends TypedSymbolAccess
    implements Function<EK9Parser.IdentifierReferenceContext, Optional<ISymbol>> {

  private final MostSpecificScope mostSpecificScope;
  private final PossibleMatchingMethods possibleMatchingMethods = new PossibleMatchingMethods();

  /**
   * Checks identifier reference now resolves.
   */
  ProcessValidIdentifierReference(final SymbolAndScopeManagement symbolAndScopeManagement,
                                  final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);
    this.mostSpecificScope = new MostSpecificScope(symbolAndScopeManagement);

  }

  @Override
  public Optional<ISymbol> apply(final EK9Parser.IdentifierReferenceContext ctx) {

    final var identifierReference = resolveIdentifierReference(ctx);

    //Above will issue errors if needs be
    if (identifierReference != null) {
      identifierReferenceChecks(ctx, identifierReference);
      //Note that we must also record this against the underlying identifier as well
      final var identifierSymbol = symbolAndScopeManagement.getRecordedSymbol(ctx.identifier());
      if (identifierSymbol == null) {
        symbolAndScopeManagement.recordSymbol(identifierReference, ctx.identifier());
      }
    }

    return Optional.ofNullable(identifierReference);
  }

  private ISymbol resolveIdentifierReference(final EK9Parser.IdentifierReferenceContext ctx) {

    //TODO I think there is a bug in here because some resolutions seem incorrect - investigate.

    //Order of resolution attempts.
    //See if already resolved, else try and resolve in current scope (this will escalate out to cover types)
    //If still not located then try and find at least one method with that name.
    List<Function<EK9Parser.IdentifierReferenceContext, ISymbol>> resolvers = List.of(
        symbolAndScopeManagement::getRecordedSymbol,
        this::tryToResolveViaCurrentScope,
        this::tryToResolveMethodByNameInNearestBlockScope
    );

    //Try each until a resolved or null if not resolved.
    return resolvers
        .stream()
        .map(resolver -> resolver.apply(ctx))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);

  }

  private ISymbol tryToResolveViaCurrentScope(final EK9Parser.IdentifierReferenceContext ctx) {

    final var currentScope = symbolAndScopeManagement.getTopScope();
    final var resolved = currentScope.resolve(new AnySymbolSearch(ctx.getText()));

    resolved.ifPresent(identifierReference -> recordATypedSymbol(identifierReference, ctx));
    return resolved.orElse(null);
  }

  private ISymbol tryToResolveMethodByNameInNearestBlockScope(final EK9Parser.IdentifierReferenceContext ctx) {

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
  private void identifierReferenceChecks(final EK9Parser.IdentifierReferenceContext ctx, ISymbol identifierReference) {

    //It has now been looked up and resolved in some way and so has been referenced.
    identifierReference.setReferenced(true);

    if (identifierReference.isVariable()) {
      identifierReferenceAccessCheck(ctx, identifierReference);
      identifierReferenceDefinedBeforeUseCheck(ctx, identifierReference);
    }

  }

  private void identifierReferenceDefinedBeforeUseCheck(final EK9Parser.IdentifierReferenceContext ctx,
                                                        final ISymbol identifierReference) {

    //Firstly check in the same source - else it means its on a type or constant and that's fine.
    final var startToken = new Ek9Token(ctx.start);
    final var token = identifierReference.getSourceToken();

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
      final var resolvedDynamicScope = symbolAndScopeManagement.traverseBackUpStack(IScope.ScopeType.DYNAMIC_BLOCK);

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