package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.support.SymbolTypeExtractor;
import org.ek9lang.compiler.support.ToCommaSeparated;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Check is the data passed in enabled a delegate function to be resolved and if so return that Function.
 */
final class CheckValidFunctionDelegateOrError extends TypedSymbolAccess implements
    Function<DelegateFunctionCheckData, FunctionSymbol> {
  private final SymbolTypeExtractor symbolTypeExtractor = new SymbolTypeExtractor();
  private final ResolveFunctionOrError resolveFunctionOrError;

  CheckValidFunctionDelegateOrError(final SymbolsAndScopes symbolsAndScopes,
                                    final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.resolveFunctionOrError =
        new ResolveFunctionOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public FunctionSymbol apply(final DelegateFunctionCheckData data) {

    final var delegateType = data.delegateSymbol().getType();

    if (delegateType.isEmpty()) {
      final var msg = "'" + data.delegateSymbol().getName() + "' :";
      errorListener.semanticError(data.token(), msg,
          ErrorListener.SemanticClassification.TYPE_NOT_RESOLVED);
      return null;
    }

    if (delegateType.get() instanceof FunctionSymbol function) {
      return checkFunctionParameters(data.token(), function, data.callArgumentTypes());
    } else {
      final var params = new ToCommaSeparated(true).apply(data.callArgumentTypes());
      final var msg = "'"
          + data.delegateSymbol().getFriendlyName()
          + "' used with supplied arguments '"
          + params
          + "':";
      errorListener.semanticError(data.token(), msg, ErrorListener.SemanticClassification.NOT_A_FUNCTION_DELEGATE);
    }

    return null;
  }

  private FunctionSymbol checkFunctionParameters(final IToken token, final FunctionSymbol function,
                                                 final List<ISymbol> parameters) {

    final var paramTypes = symbolTypeExtractor.apply(parameters);
    //maybe earlier types were not defined by the ek9 developer so let's not look at it would be misleading.
    if (parameters.size() == paramTypes.size()) {
      return resolveFunctionOrError.apply(new FunctionCheckData(token, function, paramTypes));
    }

    return null;
  }
}
