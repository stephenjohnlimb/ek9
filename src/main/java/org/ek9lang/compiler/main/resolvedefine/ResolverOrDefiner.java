package org.ek9lang.compiler.main.resolvedefine;

import java.util.List;
import java.util.Optional;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.PossibleGenericSymbol;
import org.ek9lang.compiler.symbol.support.SymbolFactory;
import org.ek9lang.core.exception.AssertValue;

/**
 * Used as an abstract base for parameterised types.
 */
public abstract class ResolverOrDefiner {

  protected final SymbolAndScopeManagement symbolAndScopeManagement;
  protected final SymbolFactory symbolFactory;
  protected final ErrorListener errorListener;
  protected final boolean errorIfNotDefinedOrResolved;

  protected ResolverOrDefiner(final SymbolAndScopeManagement symbolAndScopeManagement,
                                 final SymbolFactory symbolFactory, final ErrorListener errorListener,
                                 final boolean errorIfNotDefinedOrResolved) {
    AssertValue.checkNotNull("symbolAndScopeManagement cannot be null", symbolAndScopeManagement);
    AssertValue.checkNotNull("symbolFactory cannot be null", symbolFactory);
    AssertValue.checkNotNull("errorListener cannot be null", errorListener);

    this.symbolAndScopeManagement = symbolAndScopeManagement;
    this.symbolFactory = symbolFactory;
    this.errorListener = errorListener;
    this.errorIfNotDefinedOrResolved = errorIfNotDefinedOrResolved;
  }

  protected Optional<ISymbol> resolveOrDefine(final ParameterisedTypeData details) {
    Optional<ISymbol> rtn = Optional.empty();

    if (details.genericTypeSymbol() instanceof PossibleGenericSymbol genericTypeSymbol) {
      if (errorIfNotGeneric(details.location(), genericTypeSymbol)
          || errorIfInvalidParameters(details.location(), genericTypeSymbol, details.typeArguments())) {
        return rtn;
      }

      var theType = symbolFactory.newParameterisedSymbol(genericTypeSymbol, details.typeArguments());
      rtn = symbolAndScopeManagement.resolveOrDefine(theType);
    }

    return rtn;
  }

  private boolean errorIfNotGeneric(final Token token, final ISymbol genericType) {
    var notGeneric = !genericType.isGenericInNature();
    if (notGeneric && errorIfNotDefinedOrResolved) {
      errorListener.semanticError(token, "cannot be used to parameterize '" + genericType.getFriendlyName() + "':",
          ErrorListener.SemanticClassification.NOT_A_TEMPLATE);
    }
    return notGeneric;
  }

  private boolean errorIfInvalidParameters(final Token token, final PossibleGenericSymbol genericTypeSymbol,
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
