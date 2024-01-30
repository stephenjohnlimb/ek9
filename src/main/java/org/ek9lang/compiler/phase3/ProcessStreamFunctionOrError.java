package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.CANNOT_CALL_ABSTRACT_TYPE;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.FUNCTION_OR_DELEGATE_REQUIRED;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.FunctionSymbol;

final class ProcessStreamFunctionOrError extends TypedSymbolAccess
    implements Function<EK9Parser.PipelinePartContext, Optional<FunctionSymbol>> {

  ProcessStreamFunctionOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                               final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public Optional<FunctionSymbol> apply(EK9Parser.PipelinePartContext ctx) {
    var expectedMappingFunction = getRecordedAndTypedSymbol(ctx);

    if (expectedMappingFunction != null && expectedMappingFunction.getType().isPresent()) {
      var expectedFunctionType = expectedMappingFunction.getType().get();

      if (!expectedMappingFunction.isMarkedAbstract()) {
        if (expectedFunctionType instanceof FunctionSymbol functionSymbol) {
          return Optional.of(functionSymbol);
        } else {
          var msg = "type '" + expectedFunctionType.getFriendlyName() + "':";
          errorListener.semanticError(ctx.start, msg, FUNCTION_OR_DELEGATE_REQUIRED);
        }
      } else {
        errorListener.semanticError(ctx.start, "", CANNOT_CALL_ABSTRACT_TYPE);
      }
    }
    return Optional.empty();
  }
}
