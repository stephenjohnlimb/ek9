package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.METHOD_NOT_RESOLVED;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.SwitchSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Deals with checking if the switch can be used as an expression and whether
 * the variable being switched on has the appropriate operators to meet the case criteria.
 */
final class ProcessSwitchStatementExpression extends TypedSymbolAccess
    implements Consumer<EK9Parser.SwitchStatementExpressionContext> {

  private final SetTypeFromReturningParam setTypeFromReturningParam;

  ProcessSwitchStatementExpression(SymbolAndScopeManagement symbolAndScopeManagement,
                                   ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.setTypeFromReturningParam = new SetTypeFromReturningParam(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.SwitchStatementExpressionContext ctx) {

    var switchExpression = (SwitchSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    setTypeFromReturningParam.accept(switchExpression, ctx.returningParam());
    processSwitchCases(ctx);
  }


  /**
   * Checks the cases in terms of types and operators.
   */
  private void processSwitchCases(final EK9Parser.SwitchStatementExpressionContext ctx) {

    var controlSymbol = super.getRecordedAndTypedSymbol(ctx.preFlowAndControl().control);
    if (controlSymbol != null) {
      ctx.caseStatement()
          .forEach(caseStatement -> caseStatement.caseExpression()
              .forEach(caseExpression -> checkOperatorExistsOrError(caseExpression, controlSymbol)));
    }

  }

  private void checkOperatorExistsOrError(final EK9Parser.CaseExpressionContext ctx, final ISymbol controlSymbol) {
    //Assume an equality check - unless an operator has been employed.
    var caseVariable = getRecordedAndTypedSymbol(ctx);
    //If it is null or untyped then we'd get errors by the call above.
    if (caseVariable != null && caseVariable.getType().isPresent()) {
      final var operator = ctx.op != null ? new Ek9Token(ctx.op.getText()) : new Ek9Token("==");

      controlSymbol.getType().ifPresent(controlType -> {
        if (controlType instanceof IAggregateSymbol aggregate) {
          var search = new MethodSymbolSearch(operator.getText()).addTypeParameter(caseVariable.getType());
          if (aggregate.resolve(search).isEmpty()) {
            emitError(ctx, controlSymbol, search);
          }
        }
      });
    }

  }

  private void emitError(final EK9Parser.CaseExpressionContext ctx,
                         final ISymbol controlSymbol,
                         final MethodSymbolSearch search) {

    errorListener.semanticError(ctx.start, "wrt '" + controlSymbol.getFriendlyName() + "' and '" + search + "':",
        METHOD_NOT_RESOLVED);

  }
}
