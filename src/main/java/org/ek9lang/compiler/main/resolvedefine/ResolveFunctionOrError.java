package org.ek9lang.compiler.main.resolvedefine;

import java.util.function.Function;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbol.FunctionSymbol;
import org.ek9lang.compiler.symbol.support.SymbolTypeExtractor;
import org.ek9lang.compiler.symbol.support.ToCommaSeparated;

/**
 * Check for valid function parameters.
 */
public class ResolveFunctionOrError implements Function<FunctionCheckData, FunctionSymbol> {

  private final SymbolTypeExtractor symbolTypeExtractor = new SymbolTypeExtractor();
  private final SymbolAndScopeManagement symbolAndScopeManagement;
  private final ErrorListener errorListener;

  public ResolveFunctionOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                                final ErrorListener errorListener) {
    this.symbolAndScopeManagement = symbolAndScopeManagement;
    this.errorListener = errorListener;
  }

  @Override
  public FunctionSymbol apply(FunctionCheckData functionCheckData) {
    //maybe earlier types were not defined by the ek9 developer so let's not look at it would be misleading.
    //Use the type of the function located here, but employ the parameters
    var parameters = functionCheckData.callArgumentTypes();
    var function = functionCheckData.function();

    if (parameters.size() == symbolTypeExtractor.apply(parameters).size()) {
      if (function.isSignatureMatchTo(function.getType(), parameters)) {
        return function;
      } else {
        var params = new ToCommaSeparated(true).apply(parameters);
        var msg = "found function '"
            + function.getFriendlyName()
            + "' but used with supplied arguments '"
            + params
            + "' not appropriate:";
        errorListener.semanticError(functionCheckData.token(), msg,
            ErrorListener.SemanticClassification.FUNCTION_PARAMETER_MISMATCH);
      }
    }
    return null;
  }
}
