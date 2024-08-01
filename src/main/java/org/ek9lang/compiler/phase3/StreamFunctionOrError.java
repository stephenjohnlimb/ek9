package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.CANNOT_CALL_ABSTRACT_TYPE;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.FUNCTION_OR_DELEGATE_REQUIRED;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;

final class StreamFunctionOrError extends TypedSymbolAccess
    implements Function<EK9Parser.PipelinePartContext, Optional<FunctionSymbol>> {

  StreamFunctionOrError(final SymbolsAndScopes symbolsAndScopes,
                        final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public Optional<FunctionSymbol> apply(final EK9Parser.PipelinePartContext ctx) {

    final var expectedMappingFunction = getRecordedAndTypedSymbol(ctx);

    if (expectedMappingFunction != null && expectedMappingFunction.getType().isPresent()) {

      if (!expectedMappingFunction.isMarkedAbstract()) {
        return functionOrError(ctx, expectedMappingFunction.getType().get());
      } else {
        errorListener.semanticError(ctx.start, "", CANNOT_CALL_ABSTRACT_TYPE);
      }
    }

    return Optional.empty();
  }

  private Optional<FunctionSymbol> functionOrError(final EK9Parser.PipelinePartContext ctx,
                                                   final ISymbol expectedFunctionReturnType) {

    if (expectedFunctionReturnType instanceof FunctionSymbol functionSymbol) {
      return Optional.of(functionSymbol);
    }

    final var msg = "type '" + expectedFunctionReturnType.getFriendlyName() + "':";
    errorListener.semanticError(ctx.start, msg, FUNCTION_OR_DELEGATE_REQUIRED);

    return Optional.empty();
  }
}
