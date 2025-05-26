package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Checks that variable only and variable declarations have symbols that have been referenced.
 * This is a bit like 'golang', it is considered an error if accessed before initialising or if not used.
 * There are a couple of exceptions around 'injection' as that can only really be detected at runtime (hence avoid).
 */
final class InstructionBlockVariablesOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.InstructionBlockContext> {

  private final Consumer<ISymbol> symbolCheck;

  /**
   * Check on references to variables in blocks.
   */
  InstructionBlockVariablesOrError(final SymbolsAndScopes symbolsAndScopes,
                                   final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.symbolCheck = new SymbolReferencedOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.InstructionBlockContext ctx) {

    ctx.blockStatement().forEach(blockStatement -> {
      if (blockStatement.variableOnlyDeclaration() != null) {
        symbolCheck.accept(getRecordedAndTypedSymbol(blockStatement.variableOnlyDeclaration()));
      } else if (blockStatement.variableDeclaration() != null) {
        symbolCheck.accept(getRecordedAndTypedSymbol(blockStatement.variableDeclaration()));
      }
    });
  }
}
