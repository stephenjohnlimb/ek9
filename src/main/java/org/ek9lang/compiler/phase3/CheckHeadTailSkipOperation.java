package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.CANNOT_CALL_ABSTRACT_TYPE;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.FUNCTION_MUST_HAVE_NO_PARAMETERS;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.INTEGER_VAR_OR_FUNCTION_OR_DELEGATE_REQUIRED;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.MUST_BE_INTEGER_GREATER_THAN_ZERO;
import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.MUST_RETURN_INTEGER;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.StreamCallSymbol;

/**
 * Checks the possible operation variable, literal or function for suitability for use with
 * 'head', 'tail' and 'skip'.
 * Emits errors if the configuration is not valid.
 */
final class CheckHeadTailSkipOperation extends TypedSymbolAccess implements Consumer<EK9Parser.StreamPartContext> {

  CheckHeadTailSkipOperation(final SymbolAndScopeManagement symbolAndScopeManagement,
                             final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);

  }

  @Override
  public void accept(final EK9Parser.StreamPartContext ctx) {

    final var streamCallPart = (StreamCallSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);

    if (ctx.pipelinePart().size() == 1) {
      checkVariableOrFunctionUse(ctx.pipelinePart().get(0));
    } else if (ctx.literal() != null) {
      checkValidIntegerValueUse(ctx, streamCallPart);
    } else {
      //Just record that this is a fixed value and use 1 my default.
      streamCallPart.putSquirrelledData("FIXED", Integer.toString(1));
    }

  }

  private void checkValidIntegerValueUse(final EK9Parser.StreamPartContext ctx, final StreamCallSymbol streamCallPart) {

    final var possibleIntegerSymbol = getRecordedAndTypedSymbol(ctx.literal());

    possibleIntegerSymbol.getType().ifPresent(type -> {

      if (isTypeNotAnInteger(type)) {

        final var msg =
            "for '" + ctx.op.getText() + "' and value/type '" + possibleIntegerSymbol.getFriendlyName() + "':";
        errorListener.semanticError(possibleIntegerSymbol.getSourceToken(), msg, MUST_BE_INTEGER_GREATER_THAN_ZERO);

      } else {

        final var asInteger = Integer.parseInt(possibleIntegerSymbol.getName());
        if (asInteger < 1) {

          final var msg = "for '" + ctx.op.getText() + "' and value '" + possibleIntegerSymbol.getName() + "':";
          errorListener.semanticError(possibleIntegerSymbol.getSourceToken(), msg, MUST_BE_INTEGER_GREATER_THAN_ZERO);

        } else {
          //OK so again record this is a fixed value and record the value.
          streamCallPart.putSquirrelledData("FIXED", possibleIntegerSymbol.getName());
        }
      }
    });

  }

  private void checkVariableOrFunctionUse(final EK9Parser.PipelinePartContext ctx) {

    final var operationOperand = getRecordedAndTypedSymbol(ctx);

    if (operationOperand != null) {

      operationOperand.getType().ifPresent(type -> {

        if (type instanceof FunctionSymbol functionSymbol) {
          if (!operationOperand.getGenus().equals(ISymbol.SymbolGenus.VALUE)
              && type.isMarkedAbstract()) {
            final var msg = "wrt '" + operationOperand.getFriendlyName() + "':";
            errorListener.semanticError(ctx.start, msg, CANNOT_CALL_ABSTRACT_TYPE);

          }
          checkIsSuitableFunctionOrError(new StreamFunctionCheckData(ctx.start, functionSymbol, null));
        } else if (isTypeNotAnInteger(type)) {
          final var msg = "wrt '" + operationOperand.getFriendlyName() + "':";
          errorListener.semanticError(ctx.start, msg, INTEGER_VAR_OR_FUNCTION_OR_DELEGATE_REQUIRED);
        }

      });
    }
  }

  private void checkIsSuitableFunctionOrError(final StreamFunctionCheckData functionData) {

    final var errorMsg = "'" + functionData.functionSymbol().getFriendlyName() + "':";

    if (!functionData.functionSymbol().getCallParameters().isEmpty()) {
      errorListener.semanticError(functionData.errorLocation(), errorMsg, FUNCTION_MUST_HAVE_NO_PARAMETERS);
    }

    if (functionData.functionSymbol().getReturningSymbol().getType().isPresent()) {
      final var returnType = functionData.functionSymbol().getReturningSymbol().getType().get();
      if (isTypeNotAnInteger(returnType)) {
        errorListener.semanticError(functionData.errorLocation(), errorMsg, MUST_RETURN_INTEGER);
      }
    }

  }

  private boolean isTypeNotAnInteger(final ISymbol type) {

    return !type.isExactSameType(symbolAndScopeManagement.getEk9Types().ek9Integer());
  }
}
