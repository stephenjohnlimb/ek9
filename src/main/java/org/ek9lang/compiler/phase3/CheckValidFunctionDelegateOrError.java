package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.Function;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.SymbolTypeExtractor;
import org.ek9lang.compiler.support.ToCommaSeparated;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Check is the data passed in enabled a delegate function to be resolved and if so return that Function.
 */
final class CheckValidFunctionDelegateOrError extends RuleSupport implements
    Function<DelegateFunctionCheckData, FunctionSymbol> {

  private final SymbolTypeExtractor symbolTypeExtractor = new SymbolTypeExtractor();
  private final ResolveFunctionOrError resolveFunctionOrError;

  CheckValidFunctionDelegateOrError(SymbolAndScopeManagement symbolAndScopeManagement,
                                    ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);

    this.resolveFunctionOrError =
        new ResolveFunctionOrError(symbolAndScopeManagement, errorListener);
  }

  @Override
  public FunctionSymbol apply(final DelegateFunctionCheckData data) {
    var delegateType = data.delegateSymbol().getType();

    if (delegateType.isEmpty()) {
      var msg = "'" + data.delegateSymbol().getName() + "' :";
      errorListener.semanticError(data.token(), msg,
          ErrorListener.SemanticClassification.TYPE_NOT_RESOLVED);
      return null;
    }

    if (delegateType.get() instanceof FunctionSymbol function) {
      return checkFunctionParameters(data.token(), function, data.callArgumentTypes());
    } else {
      var params = new ToCommaSeparated(true).apply(data.callArgumentTypes());
      var msg = "'"
          + data.delegateSymbol().getFriendlyName()
          + "' used with supplied arguments '"
          + params
          + "':";
      errorListener.semanticError(data.token(), msg, ErrorListener.SemanticClassification.NOT_A_FUNCTION_DELEGATE);
    }

    return null;
  }

  private FunctionSymbol checkFunctionParameters(Token token, FunctionSymbol function,
                                                 List<ISymbol> parameters) {

    var paramTypes = symbolTypeExtractor.apply(parameters);
    //maybe earlier types were not defined by the ek9 developer so let's not look at it would be misleading.
    if (parameters.size() == paramTypes.size()) {
      return resolveFunctionOrError.apply(new FunctionCheckData(token, function, paramTypes));
    }
    return null;
  }
}
