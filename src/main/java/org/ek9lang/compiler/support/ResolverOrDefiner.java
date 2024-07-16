package org.ek9lang.compiler.support;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NOT_A_TEMPLATE;

import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.AssertValue;

/**
 * Used as an abstract base for parameterised types.
 */
public abstract class ResolverOrDefiner extends RuleSupport {

  protected final SymbolFactory symbolFactory;
  protected final boolean errorIfNotDefinedOrResolved;

  protected ResolverOrDefiner(final SymbolsAndScopes symbolsAndScopes,
                              final SymbolFactory symbolFactory,
                              final ErrorListener errorListener,
                              final boolean errorIfNotDefinedOrResolved) {

    super(symbolsAndScopes, errorListener);
    AssertValue.checkNotNull("symbolFactory cannot be null", symbolFactory);

    this.symbolFactory = symbolFactory;
    this.errorIfNotDefinedOrResolved = errorIfNotDefinedOrResolved;

  }

  /**
   * Attempts to resolve the parameterised type first and return that, else returns a new symbol.
   */
  public Optional<ISymbol> resolveOrDefine(final ParameterisedTypeData details) {

    if (details.genericTypeSymbol() instanceof PossibleGenericSymbol genericTypeSymbol) {

      if (errorIfNotGeneric(details.location(), genericTypeSymbol)
          || errorIfInvalidParameters(details.location(), genericTypeSymbol, details.typeArguments())) {
        return Optional.empty();
      }

      final var theType = symbolFactory.newParameterisedSymbol(genericTypeSymbol, details.typeArguments());
      theType.setInitialisedBy(details.location());

      return symbolsAndScopes.resolveOrDefine(theType, errorListener);
    }

    return Optional.empty();
  }

  private boolean errorIfNotGeneric(final IToken token, final ISymbol genericType) {

    final var notGeneric = !genericType.isGenericInNature();

    if (notGeneric && errorIfNotDefinedOrResolved) {
      final var msg = "cannot be used to parameterize '" + genericType.getFriendlyName() + "':";
      errorListener.semanticError(token, msg, NOT_A_TEMPLATE);
    }

    return notGeneric;
  }

  private boolean errorIfInvalidParameters(final IToken token, final PossibleGenericSymbol genericTypeSymbol,
                                           final List<ISymbol> parameterizingTypes) {
    var acceptsNParameters = genericTypeSymbol.getAnyConceptualTypeParameters().size();
    var providedWithNParameters = parameterizingTypes.size();
    if (acceptsNParameters != providedWithNParameters) {
      errorListener.semanticError(token, "'"
              + providedWithNParameters + "' parameters were supplied but '"
              + genericTypeSymbol.getFriendlyName() + "' requires '"
              + acceptsNParameters + "':",
          ErrorListener.SemanticClassification.GENERIC_TYPE_OR_FUNCTION_PARAMETERS_INCORRECT);
      return true;
    }
    return false;
  }
}
