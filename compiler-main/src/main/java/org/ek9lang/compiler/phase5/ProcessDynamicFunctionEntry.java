package org.ek9lang.compiler.phase5;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.FunctionSymbol;

/**
 * Deals with handling any returning values in a dynamic function.
 * This is the entry handler into a dynamic function.
 */
final class ProcessDynamicFunctionEntry extends TypedSymbolAccess
    implements Consumer<EK9Parser.DynamicFunctionDeclarationContext> {
  ProcessDynamicFunctionEntry(final SymbolsAndScopes symbolsAndScopes,
                              final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.DynamicFunctionDeclarationContext ctx) {

    final var functionSymbol = (FunctionSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    if (functionSymbol.isReturningSymbolPresent()) {
      final var variable = functionSymbol.getReturningSymbol();
      symbolsAndScopes.recordSymbolDeclaration(variable, functionSymbol);
    }

  }
}
