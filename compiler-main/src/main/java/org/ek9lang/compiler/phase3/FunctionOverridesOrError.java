package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.support.LocationExtractorFromSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IFunctionSymbol;

/**
 * Checks that the function correctly overrides the signature if it has a super.
 */
final class FunctionOverridesOrError extends TypedSymbolAccess implements Consumer<FunctionSymbol> {
  private final ParameterTypesExactMatchOrError parameterTypesExactMatchOrError;
  private final TypeCovarianceOrError typeCovarianceOrError;
  private final ValidFunctionAbstractnessOrError validFunctionAbstractnessOrError;
  private final PureModifierOrError pureModifierOrError;
  private final LocationExtractorFromSymbol locationExtractorFromSymbol = new LocationExtractorFromSymbol();

  /**
   * Create a new function to check overriding of super (method parameters and covariance returns).
   */
  FunctionOverridesOrError(final SymbolsAndScopes symbolsAndScopes,
                           final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.typeCovarianceOrError = new TypeCovarianceOrError(symbolsAndScopes, errorListener);
    this.parameterTypesExactMatchOrError = new ParameterTypesExactMatchOrError(symbolsAndScopes, errorListener);
    this.validFunctionAbstractnessOrError = new ValidFunctionAbstractnessOrError(symbolsAndScopes, errorListener);
    this.pureModifierOrError = new PureModifierOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final FunctionSymbol functionSymbol) {

    //Only if there is a super function, do we execute this.
    //Also, if the super function is 'Any' then we cannot check - because 'Any' clearly
    //cannot match everything! Also, the 'Any' cannot be directly called in any way.
    //It has to go through a dispatcher to be correctly 'typed' and then can be executed.

    functionSymbol.getSuperFunction().ifPresent(superFunction -> {

      if (!superFunction.isExactSameType(symbolsAndScopes.getEk9Any())) {
        final var errorMessage = getErrorMessageFor(functionSymbol, superFunction);
        final var paramData = new ParametersData(functionSymbol.getSourceToken(), errorMessage,
            functionSymbol.getCallParameters(), superFunction.getCallParameters());
        final var returnData = new CovarianceData(functionSymbol.getSourceToken(), errorMessage,
            functionSymbol.getReturningSymbol(), superFunction.getReturningSymbol());

        parameterTypesExactMatchOrError.accept(paramData);
        typeCovarianceOrError.accept(returnData);
        validFunctionAbstractnessOrError.accept(functionSymbol);
        pureModifierOrError.accept(new PureCheckData(errorMessage, superFunction, functionSymbol));
      }

    });

  }

  private String getErrorMessageFor(final IFunctionSymbol functionSymbol,
                                    final IFunctionSymbol matchedFunctionSymbol) {

    final var message = String.format("'%s' %s:",
        matchedFunctionSymbol.getFriendlyName(), locationExtractorFromSymbol.apply(matchedFunctionSymbol));

    return "'" + functionSymbol.getFriendlyName() + "' and " + message;
  }
}
