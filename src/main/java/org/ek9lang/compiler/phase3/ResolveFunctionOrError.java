package org.ek9lang.compiler.phase3;

import java.util.function.Function;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.SymbolTypeExtractor;
import org.ek9lang.compiler.support.ToCommaSeparated;
import org.ek9lang.compiler.symbols.FunctionSymbol;

/**
 * Check for valid function parameters.
 */
final class ResolveFunctionOrError extends TypedSymbolAccess implements Function<FunctionCheckData, FunctionSymbol> {

  private final SymbolTypeExtractor symbolTypeExtractor = new SymbolTypeExtractor();

  ResolveFunctionOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                         final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);

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
