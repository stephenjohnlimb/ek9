package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Validates that the statement is OK and emits errors if this is not the case.
 */
final class CheckValidStatement extends TypedSymbolAccess implements Consumer<EK9Parser.StatementContext> {
  private final CheckForOperator checkForOperator;
  private final CheckMutableOrError checkMutableOrError;

  CheckValidStatement(final SymbolAndScopeManagement symbolAndScopeManagement,
                      final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);
    this.checkForOperator = new CheckForOperator(symbolAndScopeManagement, errorListener);
    this.checkMutableOrError = new CheckMutableOrError(errorListener);

  }

  @Override
  public void accept(final EK9Parser.StatementContext statementContext) {

    if (statementContext.identifierReference() != null) {
      final var exprSymbol = getRecordedAndTypedSymbol(statementContext.identifierReference());
      if (exprSymbol != null) {

        exprSymbol.getType().ifPresent(exprType -> {
          final var op = new Ek9Token(statementContext.op);
          checkMutableOrError.accept(op, exprSymbol);

          final var search = new MethodSymbolSearch(op.getText());
          checkForOperator.apply(new CheckOperatorData(exprSymbol, op, search));
        });
      }
    }
  }
}
