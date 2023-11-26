package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.core.AssertValue;

/**
 * Checks that the guard expression is valid.
 */
final class ProcessGuardExpression extends TypedSymbolAccess
    implements Consumer<EK9Parser.GuardExpressionContext> {
  private final SymbolFromContextOrError symbolFromContextOrError;
  private final ProcessIdentifierOrError processIdentifierOrError;

  private final ProcessIdentifierAssignment processIdentifierAssignment;

  /**
   * Check on validity of assignments.
   */
  ProcessGuardExpression(final SymbolAndScopeManagement symbolAndScopeManagement,
                         final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.symbolFromContextOrError
        = new SymbolFromContextOrError(symbolAndScopeManagement, errorListener);
    this.processIdentifierOrError
        = new ProcessIdentifierOrError(symbolAndScopeManagement, errorListener);
    this.processIdentifierAssignment
        = new ProcessIdentifierAssignment(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.GuardExpressionContext ctx) {

    var expressionSymbol = symbolFromContextOrError.apply(ctx.expression());
    if (expressionSymbol == null) {
      //So not resolved and an error will have been emitted.
      return;
    }
    if (ctx.identifier() != null) {
      var data = new TypeCompatibilityData(new Ek9Token(ctx.op),
          processIdentifierOrError.apply(ctx.identifier()), expressionSymbol);
      processByIdentifier(data);
    } else {
      AssertValue.fail("Expecting finite set of operations on assignment " + ctx.start.getLine());
    }
  }

  private void processByIdentifier(final TypeCompatibilityData typeData) {
    if (typeData.lhs() != null) {
      var data = new AssignmentData(false, typeData);
      //Make a note that an assignment has taken place
      if (typeData.lhs() instanceof VariableSymbol variable) {
        symbolAndScopeManagement.recordAssignmentToIdentifierSymbol(typeData.location(), variable);
      }
      processIdentifierAssignment.accept(data);
    }
  }
}
