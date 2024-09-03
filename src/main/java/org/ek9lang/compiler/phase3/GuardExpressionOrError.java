package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.core.AssertValue;

/**
 * Checks that the guard expression is valid.
 */
final class GuardExpressionOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.GuardExpressionContext> {
  private final SymbolFromContextOrError symbolFromContextOrError;
  private final IdentifierOrError identifierOrError;

  private final IdentifierAssignmentOrError identifierAssignmentOrError;

  /**
   * Check on validity of assignments.
   */
  GuardExpressionOrError(final SymbolsAndScopes symbolsAndScopes,
                         final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.symbolFromContextOrError
        = new SymbolFromContextOrError(symbolsAndScopes, errorListener);
    this.identifierOrError
        = new IdentifierOrError(symbolsAndScopes, errorListener);
    this.identifierAssignmentOrError
        = new IdentifierAssignmentOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.GuardExpressionContext ctx) {

    final var expressionSymbol = symbolFromContextOrError.apply(ctx.expression());
    if (expressionSymbol == null) {
      //So not resolved and an error will have been emitted.
      return;
    }

    if (ctx.identifier() != null) {
      final var data = new TypeCompatibilityData(new Ek9Token(ctx.op),
          identifierOrError.apply(ctx.identifier()), expressionSymbol);
      processByIdentifierOrError(data);

    } else {
      AssertValue.fail("Expecting finite set of operations on assignment " + ctx.start.getLine());
    }

  }

  private void processByIdentifierOrError(final TypeCompatibilityData typeData) {

    if (typeData.lhs() != null) {
      final var data = new AssignmentData(false, typeData);
      identifierAssignmentOrError.accept(data);
    }

  }

}
