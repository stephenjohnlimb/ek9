package org.ek9lang.compiler.phase2;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;

/**
 * Configures a function's 'super' in the most appropriate way.
 */
final class ProcessFunctionDeclarationOrError extends RuleSupport
    implements Consumer<EK9Parser.FunctionDeclarationContext> {

  private final SuitableToExtendOrError functionSuitableToExtendOrError;
  private final SynthesizeSuperFunction synthesizeSuperFunction;

  ProcessFunctionDeclarationOrError(final SymbolsAndScopes symbolsAndScopes,
                                    final SymbolFactory symbolFactory,
                                    final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.functionSuitableToExtendOrError =
        new SuitableToExtendOrError(symbolsAndScopes, errorListener,
            List.of(SymbolGenus.FUNCTION, SymbolGenus.FUNCTION_TRAIT), true);
    this.synthesizeSuperFunction =
        new SynthesizeSuperFunction(symbolsAndScopes, symbolFactory, errorListener);

  }

  @Override
  public void accept(final EK9Parser.FunctionDeclarationContext ctx) {
    final var symbol = (FunctionSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
    //Could be null if there was a duplicate reference.

    if (symbol != null) {
      //Normal functions can extend other normal functions if open/abstract - this code below checks.
      if (ctx.identifierReference() != null) {
        final var resolved = functionSuitableToExtendOrError.apply(ctx.identifierReference());
        resolved.ifPresent(theSuper -> symbol.setSuperFunction((FunctionSymbol) theSuper));
      } else if (!symbol.isGenericInNature()) {
        synthesizeSuperFunction.accept(symbol);
      }
    }
  }
}
