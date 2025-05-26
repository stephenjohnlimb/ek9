package org.ek9lang.compiler.phase7;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.Argument;

/**
 * Given a ParamExpressionContext gets all the symbols and creates the appropriate 'Argument' for each.
 */
public final class ArgumentsCreator implements Function<EK9Parser.ParamExpressionContext, List<Argument>> {
  private final ParsedModule parsedModule;

  public ArgumentsCreator(final ParsedModule parsedModule) {
    this.parsedModule = parsedModule;
  }

  @Override
  public List<Argument> apply(final EK9Parser.ParamExpressionContext ctx) {
    return ctx.expressionParam()
        .stream()
        .map(EK9Parser.ExpressionParamContext::expression)
        .map(parsedModule::getRecordedSymbol)
        .map(Argument::new)
        .toList();
  }
}
