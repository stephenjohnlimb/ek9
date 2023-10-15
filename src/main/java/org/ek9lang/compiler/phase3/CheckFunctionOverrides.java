package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.LocationExtractor;
import org.ek9lang.compiler.symbols.FunctionSymbol;

/**
 * Checks that the function correctly overrides the signature if it has a super.
 */
final class CheckFunctionOverrides extends TypedSymbolAccess implements Consumer<FunctionSymbol> {
  private final CheckParameterTypesExactMatch checkParameterTypesExactMatch;
  private final CheckTypeCovariance checkTypeCovariance;
  private final CheckFunctionAbstractness checkFunctionAbstractness;
  private final CheckPureModifier checkPureModifier;
  private final LocationExtractor locationExtractor = new LocationExtractor();

  /**
   * Create a new function to check overriding of super (method parameters and covariance returns).
   */
  CheckFunctionOverrides(final SymbolAndScopeManagement symbolAndScopeManagement,
                         final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.checkTypeCovariance = new CheckTypeCovariance(symbolAndScopeManagement, errorListener);
    this.checkParameterTypesExactMatch = new CheckParameterTypesExactMatch(symbolAndScopeManagement, errorListener);
    this.checkFunctionAbstractness = new CheckFunctionAbstractness(symbolAndScopeManagement, errorListener);
    this.checkPureModifier = new CheckPureModifier(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final FunctionSymbol functionSymbol) {
    //Only if there is a super function, do we execute this.
    functionSymbol.getSuperFunctionSymbol().ifPresent(superFunction -> {
      var errorMessage = getErrorMessageFor(functionSymbol, superFunction);

      var paramData = new ParametersCheckData(functionSymbol.getSourceToken(), errorMessage,
          functionSymbol.getCallParameters(), superFunction.getCallParameters());
      var returnData = new CovarianceCheckData(functionSymbol.getSourceToken(), errorMessage,
          functionSymbol.getReturningSymbol(), superFunction.getReturningSymbol());

      checkParameterTypesExactMatch.accept(paramData);
      checkTypeCovariance.accept(returnData);
      checkFunctionAbstractness.accept(functionSymbol);
      checkPureModifier.accept(new PureCheckData(errorMessage, superFunction, functionSymbol));
    });
  }

  private String getErrorMessageFor(final FunctionSymbol functionSymbol,
                                    final FunctionSymbol matchedFunctionSymbol) {
    String message = String.format("'%s' %s:",
        matchedFunctionSymbol.getFriendlyName(), locationExtractor.apply(matchedFunctionSymbol));

    return "'" + functionSymbol.getFriendlyName() + "' and " + message;
  }
}
