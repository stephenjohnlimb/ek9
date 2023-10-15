package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.core.CompilerException;

/**
 * Check the stream pipeline part.
 */
final class CheckPipelinePart extends TypedSymbolAccess implements Consumer<EK9Parser.PipelinePartContext> {
  private final ResolveFunctionOrDelegateByNameOrError resolveFunctionOrDelegateByNameOrError;

  /**
   * Create a new consumer to handle stream pipeline parts.
   */
  CheckPipelinePart(final SymbolAndScopeManagement symbolAndScopeManagement,
                    final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.resolveFunctionOrDelegateByNameOrError =
        new ResolveFunctionOrDelegateByNameOrError(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.PipelinePartContext ctx) {
    //TODO consider the other possibilities here.
    //for example the ctx.objectAccessExpression() and ctx.call()

    if (ctx.identifier() != null) {
      //This will resolve or issue an error.
      resolveFunctionOrDelegateByNameOrError.accept(ctx.identifier());
    } else if (ctx.objectAccessExpression() != null) {
      throw new CompilerException("objectAccessExpression() not implemented in " + this.getClass().getName());
    } else if (ctx.call() != null) {
      var resolvedCall = getRecordedAndTypedSymbol(ctx.call());
      if (resolvedCall == null) {
        errorListener.semanticError(ctx.call().start, "", ErrorListener.SemanticClassification.NOT_RESOLVED);
      }
    }
  }
}
