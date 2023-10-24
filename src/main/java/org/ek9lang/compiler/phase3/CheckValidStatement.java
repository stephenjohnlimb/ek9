package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Validates that the statement is OK and emits errors if this is not the case.
 */
final class CheckValidStatement extends TypedSymbolAccess implements Consumer<EK9Parser.StatementContext> {
  private final CheckForOperator checkForOperator;

  CheckValidStatement(final SymbolAndScopeManagement symbolAndScopeManagement,
                      final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.checkForOperator = new CheckForOperator(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.StatementContext statementContext) {
    if (statementContext.expression() != null) {
      var exprSymbol = getRecordedAndTypedSymbol(statementContext.expression());
      if (exprSymbol != null) {
        exprSymbol.getType().ifPresent(exprType -> {
          var op = new Ek9Token(statementContext.op);
          var search = new MethodSymbolSearch(op.getText());
          checkForOperator.apply(new CheckOperatorData(exprSymbol, op, search));
        });
      }
    }
  }
}
