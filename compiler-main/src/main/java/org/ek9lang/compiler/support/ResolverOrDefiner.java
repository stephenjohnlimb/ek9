package org.ek9lang.compiler.support;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NOT_A_TEMPLATE;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.RESULT_MUST_HAVE_DIFFERENT_TYPES;

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

  private final SameGenericConceptualParameters sameGenericConceptualParameters = new SameGenericConceptualParameters();

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

      if (ek9ResultWithSameParameterizingTypes(details.location(), genericTypeSymbol, details.typeArguments())) {
        return Optional.empty();
      }
      if (errorIfNotGeneric(details.location(), genericTypeSymbol)
          || errorIfInvalidParameters(details.location(), genericTypeSymbol, details.typeArguments())) {
        return Optional.empty();
      }

      if (sameGenericConceptualParameters.test(details.genericTypeSymbol(), details.typeArguments())) {
        //It's not really been parameterised, as all the arguments are just the same as the generic type
        return Optional.of(genericTypeSymbol);
      }

      //What if this is referenced from within the same type and, it just looks like it is being parameterised.
      //i.e. within List of type T we refer to List of T? We don't want a "List of T of T".
      //Only if the 'T' is really from some other generic type do we want that.
      //i.e. List of T used in the context of Generic of type T - that's not actually the same 'T'.
      //If I rewrite as Generic of type G and List of type L - perhaps you can see what I mean.
      //Now when I say I want a Generic of Integer, that means that List of type L of type G now gets flattened
      //to a List of Integer, because the G was replaced with the Integer and then the L was replaced with the Integer.

      //TODO, precondition to check that the generic type has all return and argument types defined.
      //TODO If not then it cannot be parameterised yet so return Optional.empty.

      final var theType = symbolFactory.newParameterisedSymbol(genericTypeSymbol, details.typeArguments());
      theType.setInitialisedBy(details.location());

      return symbolsAndScopes.resolveOrDefine(theType, errorListener);
    }

    return Optional.empty();
  }

  private boolean ek9ResultWithSameParameterizingTypes(final IToken errorLocation,
                                                       final PossibleGenericSymbol genericTypeSymbol,
                                                       final List<ISymbol> types) {
    //If early in processing might not be set yet.
    final var ek9Types = symbolsAndScopes.getEk9Types();
    if (ek9Types != null
        && ek9Types.ek9Result().isExactSameType(genericTypeSymbol) && types.size() == 2
        && types.get(0).isExactSameType(types.get(1))) {

      final var params = new ToCommaSeparated(true).apply(types);
      final var msg = params + " cannot be used to parameterize: '" + genericTypeSymbol.getFriendlyName() + "':";
      errorListener.semanticError(errorLocation, msg, RESULT_MUST_HAVE_DIFFERENT_TYPES);
      return true;


    }
    return false;
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
