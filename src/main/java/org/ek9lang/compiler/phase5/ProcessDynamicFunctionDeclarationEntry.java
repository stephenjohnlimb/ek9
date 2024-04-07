package org.ek9lang.compiler.phase5;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.FunctionSymbol;

/**
 * Deals with handling any returning values in a dynamic function.
 */
final class ProcessDynamicFunctionDeclarationEntry extends TypedSymbolAccess
    implements Consumer<EK9Parser.DynamicFunctionDeclarationContext> {
  ProcessDynamicFunctionDeclarationEntry(final SymbolAndScopeManagement symbolAndScopeManagement,
                                         final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);

  }

  @Override
  public void accept(final EK9Parser.DynamicFunctionDeclarationContext ctx) {

    final var functionSymbol = (FunctionSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    if (functionSymbol.isReturningSymbolPresent()) {
      final var variable = functionSymbol.getReturningSymbol();
      symbolAndScopeManagement.recordSymbolDeclaration(variable, functionSymbol);
    }

  }
}
