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
final class FunctionDelegateOrError extends TypedSymbolAccess implements
    Function<DelegateFunctionData, FunctionSymbol> {
  private final SymbolTypeExtractor symbolTypeExtractor = new SymbolTypeExtractor();
  private final ResolveFunctionOrError resolveFunctionOrError;

  FunctionDelegateOrError(final SymbolsAndScopes symbolsAndScopes,
                          final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.resolveFunctionOrError =
        new ResolveFunctionOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public FunctionSymbol apply(final DelegateFunctionData data) {

    final var delegateType = data.delegateSymbol().getType();

    if (delegateType.isEmpty()) {
      final var msg = "'" + data.delegateSymbol().getName() + "' :";
      errorListener.semanticError(data.token(), msg,
          ErrorListener.SemanticClassification.TYPE_NOT_RESOLVED);
      return null;
    }
    return validFunctionOfError(delegateType.get(), data);
  }

  private FunctionSymbol validFunctionOfError(final ISymbol delegateType, final DelegateFunctionData data) {

    if (delegateType instanceof FunctionSymbol function) {
      return resolveFunctionOrError(data.token(), function, data.callArgumentTypes());
    }
    emitNotAFunctionDelegateError(data);

    return null;
  }

  private FunctionSymbol resolveFunctionOrError(final IToken token, final FunctionSymbol function,
                                                final List<ISymbol> parameters) {

    final var paramTypes = symbolTypeExtractor.apply(parameters);
    //maybe earlier types were not defined by the ek9 developer so let's not look at it would be misleading.
    if (parameters.size() == paramTypes.size()) {
      return resolveFunctionOrError.apply(new FunctionData(token, function, paramTypes));
    }

    return null;
  }

  private void emitNotAFunctionDelegateError(final DelegateFunctionData data) {
    final var params = new ToCommaSeparated(true).apply(data.callArgumentTypes());
    final var msg = "'"
        + data.delegateSymbol().getFriendlyName()
        + "' used with supplied arguments '"
        + params
        + "':";
    errorListener.semanticError(data.token(), msg, ErrorListener.SemanticClassification.NOT_A_FUNCTION_DELEGATE);

  }
}
