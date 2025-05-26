package org.ek9lang.compiler.phase2;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;

/**
 * Configures the dynamic function with it's 'super', but also checks that the super can be used in the
 * way the EK9 source code has been defined.
 */
final class ProcessDynamicFunctionOrError extends RuleSupport implements
    Consumer<EK9Parser.DynamicFunctionDeclarationContext> {

  private final SuitableToExtendOrError functionSuitableToExtendOrError;

  ProcessDynamicFunctionOrError(final SymbolsAndScopes symbolsAndScopes,
                                final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.functionSuitableToExtendOrError =
        new SuitableToExtendOrError(symbolsAndScopes, errorListener,
            List.of(SymbolGenus.FUNCTION, SymbolGenus.FUNCTION_TRAIT), true);
  }

  @Override
  public void accept(final EK9Parser.DynamicFunctionDeclarationContext ctx) {

    if (symbolsAndScopes.getRecordedSymbol(ctx) instanceof FunctionSymbol asFunction) {
      if (ctx.identifierReference() != null) {
        final var resolved = functionSuitableToExtendOrError.apply(ctx.identifierReference());
        resolved.ifPresent(theSuper -> asFunction.setSuperFunction((FunctionSymbol) theSuper));
      } else if (ctx.parameterisedType() != null) {
        final var resolved = functionSuitableToExtendOrError.apply(ctx.parameterisedType());
        resolved.ifPresent(theSuper -> asFunction.setSuperFunction((FunctionSymbol) theSuper));
      }
    }
  }

}
