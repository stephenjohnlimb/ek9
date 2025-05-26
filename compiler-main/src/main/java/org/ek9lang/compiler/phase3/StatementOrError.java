package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Validates that the statement is OK and emits errors if this is not the case.
 */
final class StatementOrError extends TypedSymbolAccess implements Consumer<EK9Parser.StatementContext> {
  private final RequiredOperatorPresentOrError requiredOperatorPresentOrError;
  private final MutableOrError mutableOrError;

  StatementOrError(final SymbolsAndScopes symbolsAndScopes,
                   final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.requiredOperatorPresentOrError = new RequiredOperatorPresentOrError(symbolsAndScopes, errorListener);
    this.mutableOrError = new MutableOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.StatementContext statementContext) {

    if (statementContext.identifierReference() != null) {
      final var exprSymbol = getRecordedAndTypedSymbol(statementContext.identifierReference());
      if (exprSymbol != null) {

        exprSymbol.getType().ifPresent(exprType -> {
          final var op = new Ek9Token(statementContext.op);
          mutableOrError.accept(op, exprSymbol);

          final var search = new MethodSymbolSearch(op.getText());
          requiredOperatorPresentOrError.apply(new CheckOperatorData(exprSymbol, op, search));
        });
      }
    }
  }
}
