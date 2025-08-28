package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.METHOD_NOT_RESOLVED;

import java.util.HashMap;
import java.util.function.Consumer;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.LhsFromPreFlowOrError;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.support.ToCommaSeparated;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Deals with checking if the switch can be used as an expression and whether
 * the variable being switched on has the appropriate operators to meet the case criteria.
 */
final class SwitchStatementExpressionOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.SwitchStatementExpressionContext> {

  private final SetTypeFromReturningParam setTypeFromReturningParam;
  private final LhsFromPreFlowOrError lhsFromPreFlowOrError;

  SwitchStatementExpressionOrError(SymbolsAndScopes symbolsAndScopes,
                                   ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.setTypeFromReturningParam = new SetTypeFromReturningParam(symbolsAndScopes, errorListener);
    this.lhsFromPreFlowOrError = new LhsFromPreFlowOrError(symbolsAndScopes, errorListener);
  }

  @Override
  public void accept(final EK9Parser.SwitchStatementExpressionContext ctx) {

    //Now the return type (if viable)
    setTypeFromReturningParam.accept(symbolsAndScopes.getRecordedSymbol(ctx), ctx.returningParam());

    //Then check the cases all make sense.
    switchCasesOrError(ctx);

  }


  /**
   * Checks the cases in terms of types and operators.
   */
  private void switchCasesOrError(final EK9Parser.SwitchStatementExpressionContext ctx) {

    if (ctx.preFlowAndControl().control == null) {
      //If no control has been defined, then we take a look at the guard
      final var effectiveControlSymbol = lhsFromPreFlowOrError.apply(ctx.preFlowAndControl().preFlowStatement());
      validSwitchVariableOrError(ctx, effectiveControlSymbol);
    } else {
      final var controlSymbol = getRecordedAndTypedSymbol(ctx.preFlowAndControl().control);
      validSwitchVariableOrError(ctx, controlSymbol);
    }
  }

  private void validSwitchVariableOrError(final EK9Parser.SwitchStatementExpressionContext ctx,
                                          final ISymbol controlSymbol) {
    if (controlSymbol != null && controlSymbol.getType().isPresent()) {
      operatorsPresentOrError(ctx, controlSymbol);
      enumerationUseValidOrError(ctx, controlSymbol);
      defaultStatementOrError(ctx, controlSymbol);
    }
  }

  private void operatorsPresentOrError(final EK9Parser.SwitchStatementExpressionContext ctx,
                                       final ISymbol controlSymbol) {
    ctx.caseStatement()
        .forEach(caseStatement -> caseStatement.caseExpression()
            .forEach(caseExpression -> operatorExistsOrError(caseExpression, controlSymbol)));
  }

  /**
   * Now if all part of the cases are just ObjectAccessExpressions and the start is the
   * same type as the control type - then it is just a simple explicit switch on each enumerated
   * value. So we can issue error if an enumerated values is missing.
   * But it has to be all the cases, if any one is a form of expression then the developers
   * intent is not just to switch on each enumerated value.
   */
  private void enumerationUseValidOrError(final EK9Parser.SwitchStatementExpressionContext ctx,
                                          final ISymbol controlSymbol) {

    controlSymbol.getType().ifPresent(controlType -> {

      if (controlType.getGenus().equals(SymbolGenus.CLASS_ENUMERATION)
          && controlType instanceof IAggregateSymbol controlAsAggregate) {
        enumerationCaseUseOrError(ctx, controlAsAggregate);
      }
    });

  }

  private void defaultStatementOrError(final EK9Parser.SwitchStatementExpressionContext ctx,
                                       final ISymbol controlSymbol) {

    final var stockMsg = "wrt '" + controlSymbol.getFriendlyName() + "':";
    final var isAStatement = ctx.parent instanceof EK9Parser.StatementContext;

    if (ctx.DEFAULT() == null) {
      if (isAStatement) {
        errorListener.semanticError(ctx.start, stockMsg,
            ErrorListener.SemanticClassification.DEFAULT_REQUIRED_IN_SWITCH_STATEMENT);
      } else {
        errorListener.semanticError(ctx.start, stockMsg,
            ErrorListener.SemanticClassification.DEFAULT_REQUIRED_IN_SWITCH_EXPRESSION);
      }
    }

  }

  private void enumerationCaseUseOrError(final EK9Parser.SwitchStatementExpressionContext ctx,
                                         final IAggregateSymbol controlSymbolType) {

    final var enumValues = controlSymbolType.getProperties();
    final HashMap<ISymbol, Token> enumValuesEncountered = new HashMap<>();

    for (var caseStatement : ctx.caseStatement()) {
      for (var caseExpression : caseStatement.caseExpression()) {

        if (!isCompatibleObjectAccessExpression(caseExpression, controlSymbolType)) {
          return;
        }

        final var caseObjectStart = symbolsAndScopes.getRecordedSymbol(caseExpression.objectAccessExpression());
        if (enumValuesEncountered.containsKey(caseObjectStart)) {
          emitDuplicateCaseError(caseStatement.CASE().getSymbol(), caseObjectStart,
              enumValuesEncountered.get(caseObjectStart));
        }

        enumValuesEncountered.put(caseObjectStart, caseExpression.start);
      }

    }

    if (!enumValuesEncountered.keySet().containsAll(enumValues)) {
      final var listOfEnumValues = new ToCommaSeparated(true).apply(enumValues);

      errorListener.semanticError(ctx.start, "should cover values: " + listOfEnumValues + " :",
          ErrorListener.SemanticClassification.NOT_ALL_ENUMERATED_VALUES_PRESENT_IN_SWITCH);

    }

  }

  private boolean isCompatibleObjectAccessExpression(final EK9Parser.CaseExpressionContext caseExpression,
                                                     final IAggregateSymbol controlSymbolType) {

    if (caseExpression.objectAccessExpression() == null) {
      return false;
    }

    final var caseObjectStart = symbolsAndScopes.getRecordedSymbol(caseExpression.objectAccessExpression());

    if (caseObjectStart == null || caseObjectStart.getType().isEmpty()) {
      return false;
    }

    return caseObjectStart.getType().get().isExactSameType(controlSymbolType);
  }

  private void operatorExistsOrError(final EK9Parser.CaseExpressionContext ctx, final ISymbol controlSymbol) {

    //Assume an equality check - unless an operator has been employed.
    final var caseVariable = getRecordedAndTypedSymbol(ctx);

    //If it is null or untyped then we'd get errors by the call above.
    if (caseVariable != null && caseVariable.getType().isPresent()) {
      final var operator = ctx.op != null ? new Ek9Token(ctx.op.getText()) : new Ek9Token("==");

      controlSymbol.getType().ifPresent(controlType -> {
        if (controlType instanceof IAggregateSymbol aggregate) {
          final var search = new MethodSymbolSearch(operator.getText()).addTypeParameter(caseVariable.getType());
          if (aggregate.resolve(search).isEmpty()) {
            emitMethodNotResolvedError(ctx, controlSymbol, search);
          }
        }
      });
    }

  }

  private void emitDuplicateCaseError(final Token errorLocation, final ISymbol caseObjectStart,
                                      final Token duplicateToken) {

    final var lineNoOfDuplicate = duplicateToken.getLine();
    final var msg = "wrt: '" + caseObjectStart.getFriendlyName() + "' already encountered on line " + lineNoOfDuplicate
        + " :";
    errorListener.semanticError(errorLocation, msg,
        ErrorListener.SemanticClassification.DUPLICATE_ENUMERATED_VALUES_PRESENT_IN_SWITCH);

  }

  private void emitMethodNotResolvedError(final EK9Parser.CaseExpressionContext ctx,
                                          final ISymbol controlSymbol,
                                          final MethodSymbolSearch search) {

    errorListener.semanticError(ctx.start, "wrt '" + controlSymbol.getFriendlyName() + "' and '" + search + "':",
        METHOD_NOT_RESOLVED);

  }
}
