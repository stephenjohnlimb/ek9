package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.core.AssertValue;

/**
 * Checks that the pre-flow expressions are valid.
 */
final class PreFlowStatementOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.PreFlowStatementContext> {
  private final SymbolFromContextOrError symbolFromContextOrError;
  private final IdentifierOrError identifierOrError;

  private final IsSetPresentOrError isSetPresentOrError;

  /**
   * Check on validity of assignments.
   */
  PreFlowStatementOrError(final SymbolsAndScopes symbolsAndScopes,
                          final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.symbolFromContextOrError
        = new SymbolFromContextOrError(symbolsAndScopes, errorListener);
    this.identifierOrError
        = new IdentifierOrError(symbolsAndScopes, errorListener);
    this.isSetPresentOrError
        = new IsSetPresentOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.PreFlowStatementContext ctx) {

    if (ctx.assignmentStatement() != null) {
      //There is nothing to check, just normal assignment checks will cover that
      return;
    }

    if (ctx.guardExpression() != null) {

      final var data = new TypeCompatibilityData(
          new Ek9Token(ctx.guardExpression().op),
          identifierOrError.apply(ctx.guardExpression().identifier()),
          symbolFromContextOrError.apply(ctx.guardExpression().expression())
      );
      processReturnFromExpressionOrError(data);

    } else if (ctx.variableDeclaration() != null) {

      final var data = new TypeCompatibilityData(
          new Ek9Token(ctx.variableDeclaration().op),
          identifierOrError.apply(ctx.variableDeclaration().identifier()),
          symbolFromContextOrError.apply(ctx.variableDeclaration().assignmentExpression())
      );
      processReturnFromExpressionOrError(data);

    } else {
      AssertValue.fail("Expecting finite set of operations on assignment " + ctx.start.getLine());
    }

  }

  private void processReturnFromExpressionOrError(final TypeCompatibilityData typeData) {

    //If it is null then an error will have been emitted, likewise if it is un-typed there will have been and error.
    if (typeData.rhs() != null) {
      typeData.rhs().getType().ifPresent(returnType -> isSetPresentOrError.test(typeData.location(), returnType));
    }
  }
}
