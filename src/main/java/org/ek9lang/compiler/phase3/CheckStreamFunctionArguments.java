package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.INCOMPATIBLE_TYPE_ARGUMENTS;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.SymbolTypeExtractor;

/**
 * Checks that the arguments in the function are compatible with the current pipeline type.
 */
final class CheckStreamFunctionArguments extends TypedSymbolAccess implements Consumer<StreamFunctionCheckData> {
  private final SymbolTypeExtractor symbolTypeExtractor = new SymbolTypeExtractor();

  CheckStreamFunctionArguments(final SymbolAndScopeManagement symbolAndScopeManagement,
                               final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(StreamFunctionCheckData functionData) {
    var argumentTypes = symbolTypeExtractor.apply(functionData.functionSymbol().getCallParameters());
    //Now check those types are compatible with symbolType
    argumentTypes.forEach(argumentType -> {
      if (!functionData.currentStreamType().isAssignableTo(argumentType)) {
        var typeErrorMsg = "wrt '" + functionData.functionSymbol().getFriendlyName()
            + "' and pipeline type '" + functionData.currentStreamType().getFriendlyName() + "':";
        errorListener.semanticError(functionData.errorLocation(), typeErrorMsg, INCOMPATIBLE_TYPE_ARGUMENTS);
      }
    });
  }
}
