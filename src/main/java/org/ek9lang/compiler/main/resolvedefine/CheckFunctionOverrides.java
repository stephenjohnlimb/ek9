package org.ek9lang.compiler.main.resolvedefine;

import java.io.File;
import java.util.function.Consumer;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.main.rules.CheckParameterTypesExactMatch;
import org.ek9lang.compiler.main.rules.CheckTypeCovariance;
import org.ek9lang.compiler.main.rules.CovarianceCheckData;
import org.ek9lang.compiler.main.rules.ParametersCheckData;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.symbol.FunctionSymbol;

/**
 * Checks that the function correctly overrides the signature if it has a super.
 */
public class CheckFunctionOverrides extends RuleSupport implements Consumer<FunctionSymbol> {
  private final CheckParameterTypesExactMatch checkParameterTypesExactMatch;
  private final CheckTypeCovariance checkTypeCovariance;

  /**
   * Create a new function to check overriding of super (method parameters and covariance returns).
   */
  public CheckFunctionOverrides(final SymbolAndScopeManagement symbolAndScopeManagement,
                                final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.checkTypeCovariance = new CheckTypeCovariance(symbolAndScopeManagement, errorListener);
    this.checkParameterTypesExactMatch = new CheckParameterTypesExactMatch(symbolAndScopeManagement, errorListener);
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
    });
  }

  private String getErrorMessageFor(final FunctionSymbol functionSymbol,
                                    final FunctionSymbol matchedFunctionSymbol) {
    String message = String.format("'%s' on line %d in %s:",
        matchedFunctionSymbol.getFriendlyName(), matchedFunctionSymbol.getSourceToken().getLine(),
        new File(matchedFunctionSymbol.getSourceFileLocation()).getName());

    return "'" + functionSymbol.getFriendlyName() + "' and " + message;
  }
}
