package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NAMED_PARAMETERS_MUST_MATCH_ARGUMENTS;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * If the Ek9 developer has used named parameters, then they must be validated.
 * <p>
 * There won't even be a symbol match if the wrong number or type of argument have been
 * employed. So that error condition will have been identified in earlier phases.
 * </p>
 * <p>
 * But if there are arguments and they are named then they must be named correctly
 * and in the right order. EK9 does not support missing arguments (even when named).
 * Nor does it support passing arguments in a different order to declaration.
 * </p>
 * <p>
 * The main purpose of named arguments, is to support a more obvious and declarative
 * syntax when large numbers of arguments are used.
 * </p>
 */
final class ValidNamedArgumentsOrError extends RuleSupport
    implements BiConsumer<EK9Parser.ParamExpressionContext, List<ISymbol>> {
  ValidNamedArgumentsOrError(final SymbolsAndScopes symbolsAndScopes,
                             final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);
  }

  @Override
  public void accept(final EK9Parser.ParamExpressionContext ctx,
                     final List<ISymbol> parameters) {

    //There might not be any call arguments being passed
    //Or there might not be any parameters that should be accepted.
    if (ctx != null && !parameters.isEmpty()) {
      final var callArguments = getCallArguments(ctx);

      //Might not have be coded with named arguments, so nothing to validate.
      if (!callArguments.isEmpty()) {
        validArgumentsForParametersOrError(callArguments, parameters);
      }
    }
  }

  private void validArgumentsForParametersOrError(final List<EK9Parser.IdentifierContext> callArguments,
                                                  final List<ISymbol> parameters) {

    //Now for the check that each named parameter matches with the order and name of the parameter.
    for (int i = 0; i < callArguments.size(); i++) {
      final var arg = callArguments.get(i);
      final var param = parameters.get(i);

      if (!param.getName().equals(arg.getText())) {
        final var msg = "wrt: '" + param.getName() + "':";
        errorListener.semanticError(arg.start, msg, NAMED_PARAMETERS_MUST_MATCH_ARGUMENTS);
      }
    }
  }

  private List<EK9Parser.IdentifierContext> getCallArguments(EK9Parser.ParamExpressionContext ctx) {

    return ctx.expressionParam()
        .stream()
        .map(EK9Parser.ExpressionParamContext::identifier)
        .filter(Objects::nonNull)
        .toList();
  }
}
