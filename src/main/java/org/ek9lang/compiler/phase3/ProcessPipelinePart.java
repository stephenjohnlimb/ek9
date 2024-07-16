package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Process the stream pipeline part, ensuring that it can be resolved.
 * Record it against the appropriate context.
 */
final class ProcessPipelinePart extends TypedSymbolAccess implements Consumer<EK9Parser.PipelinePartContext> {
  private final ResolveFunctionOrDelegateByNameOrError resolveFunctionOrDelegateByNameOrError;

  /**
   * Create a new consumer to handle stream pipeline parts.
   */
  ProcessPipelinePart(final SymbolsAndScopes symbolsAndScopes,
                      final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.resolveFunctionOrDelegateByNameOrError =
        new ResolveFunctionOrDelegateByNameOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.PipelinePartContext ctx) {

    final var resolvedSymbol = pipeLinePartSymbol(ctx);

    if (resolvedSymbol != null) {
      recordATypedSymbol(resolvedSymbol, ctx);
    }

  }

  private ISymbol pipeLinePartSymbol(final EK9Parser.PipelinePartContext ctx) {

    if (ctx.identifier() != null) {
      //This will resolve or issue an error.
      resolveFunctionOrDelegateByNameOrError.accept(ctx.identifier());
      return getRecordedAndTypedSymbol(ctx.identifier());
    }

    if (ctx.objectAccessExpression() != null) {
      return getRecordedAndTypedSymbol(ctx.objectAccessExpression());
    }

    return getRecordedAndTypedSymbol(ctx.call());
  }
}
