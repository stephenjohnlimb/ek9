package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.UNABLE_TO_FIND_PIPE_FOR_TYPE;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.support.TypeCanReceivePipedData;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Process and ensure that the stream termination can function correctly.
 * But note this is only a basic check, only at the end of the whole Stream pipeline 'exit'
 * can the whole 'typed flow be checked.
 */
final class StreamStatementTerminationOrError extends TypedSymbolAccess implements
    Consumer<EK9Parser.StreamStatementTerminationContext> {
  private final TypeCanReceivePipedData typeCanReceivePipedData = new TypeCanReceivePipedData();

  StreamStatementTerminationOrError(final SymbolsAndScopes symbolsAndScopes,
                                    final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.StreamStatementTerminationContext ctx) {

    final var pipelinePartSymbol = getRecordedAndTypedSymbol(ctx.pipelinePart());
    //Errors will have been emitted if pipeline part has not been defined or is still 'untyped'
    if (pipelinePartSymbol != null) {
      pipeLinePartOrError(ctx, pipelinePartSymbol);
    }
  }

  private void pipeLinePartOrError(final EK9Parser.StreamStatementTerminationContext ctx,
                                   final ISymbol pipeLinePartSymbol) {

    pipeLinePartSymbol.getType().ifPresent(pipeLinePartType -> {

      final var sourceToken = new Ek9Token(ctx.pipelinePart().start);
      if (typeCanBePipedIntoOrError(sourceToken, pipeLinePartType)) {
        updateTerminationSymbol(symbolsAndScopes.getRecordedSymbol(ctx), pipeLinePartSymbol);
      }
    });

  }

  private boolean typeCanBePipedIntoOrError(final IToken sourceToken, final ISymbol pipeLinePartType) {

    final var rtn = typeCanReceivePipedData.test(pipeLinePartType);
    if (!rtn) {
      final var msg = "wrt '" + pipeLinePartType.getFriendlyName() + "':";
      errorListener.semanticError(sourceToken, msg, UNABLE_TO_FIND_PIPE_FOR_TYPE);
    }

    return rtn;
  }

  private void updateTerminationSymbol(final ISymbol terminationSymbol, final ISymbol pipeLinePartSymbol) {

    terminationSymbol.setGenus(pipeLinePartSymbol.getGenus());
    terminationSymbol.setType(pipeLinePartSymbol.getType());

  }

}
