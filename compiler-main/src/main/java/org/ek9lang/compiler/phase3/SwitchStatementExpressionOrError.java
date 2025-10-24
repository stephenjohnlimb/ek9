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
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.support.ToCommaSeparated;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.core.CompilerException;

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
      //if it is null - then an error will have been emitted.
      if (effectiveControlSymbol != null) {
        validSwitchVariableOrError(ctx, effectiveControlSymbol);
      }
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

    // Get the CallSymbol that was created in Phase 1
    final var symbol = symbolsAndScopes.getRecordedSymbol(ctx);

    if (!(symbol instanceof CallSymbol callSymbol)) {
      // This should not happen if Phase 1 worked correctly
      throw new CompilerException("Expected CallSymbol for case expression, but got: "
          + (symbol != null ? symbol.getClass().getSimpleName() : "null"));
    }

    // Get the case value symbol from the child context (not from caseExpression itself)
    final var caseVariable = getValueSymbolFromChildContext(ctx);

    // If it is null or untyped then we'd get errors by the call above.
    if (caseVariable != null && caseVariable.getType().isPresent()) {

      controlSymbol.getType().ifPresent(controlType -> {
        if (controlType instanceof IAggregateSymbol aggregate) {
          // Ensure parameterized types are fully resolved before method search
          if (aggregate.isParameterisedType() && aggregate instanceof org.ek9lang.compiler.symbols.PossibleGenericSymbol possibleGeneric) {
            symbolsAndScopes.resolveOrDefine(possibleGeneric, errorListener);
          }

          // Create MethodSymbolSearch with operator name and case value type for fuzzy matching with promotion
          // Use the operator name as-is (e.g., "==", "contains", "<"), NOT the method name
          final var search = new MethodSymbolSearch(callSymbol.getName()).addTypeParameter(caseVariable.getType());

          // Use resolveMatchingMethods for cost-based fuzzy matching (handles promotion)
          // IMPORTANT: Use the return value, not the passed-in object
          final var result = aggregate.resolveMatchingMethods(search, new MethodSymbolSearchResult());
          final var bestMatch = result.getSingleBestMatchSymbol();

          if (bestMatch.isPresent()) {
            // Set the resolved method on the existing CallSymbol (handles promotion if needed)
            final var resolvedMethod = bestMatch.get();
            callSymbol.setResolvedSymbolToCall(resolvedMethod);
          } else {
            emitMethodNotResolvedError(ctx, controlSymbol, search);
          }
        }
      });
    }

  }

  /**
   * Gets the value symbol from the appropriate child context of a caseExpression.
   * A caseExpression can have: call, objectAccessExpression, expression (with op), or primary.
   * The value symbol is recorded on these child contexts, NOT on the caseExpression itself.
   */
  private ISymbol getValueSymbolFromChildContext(final EK9Parser.CaseExpressionContext ctx) {
    // Grammar: caseExpression : call | objectAccessExpression | op=(operators) expression | primary

    if (ctx.call() != null) {
      return getRecordedAndTypedSymbol(ctx.call());
    } else if (ctx.objectAccessExpression() != null) {
      return getRecordedAndTypedSymbol(ctx.objectAccessExpression());
    } else if (ctx.expression() != null) {
      // Case with explicit operator: case <= 10
      return getRecordedAndTypedSymbol(ctx.expression());
    } else if (ctx.primary() != null) {
      // Case with literal: case 10 or case 'D'
      return getRecordedAndTypedSymbol(ctx.primary());
    }

    return null;
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
