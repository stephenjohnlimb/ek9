package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.UNABLE_TO_FIND_PIPE_FOR_TYPE;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.TypeCanReceivePipedData;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.StreamCallSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Process and ensure that the stream termination can function correctly.
 * But note this is only a basic check, only at the end of the whole Stream pipeline 'exit'
 * can the whole 'typed flow be checked.
 */
final class ProcessStreamStatementTermination extends TypedSymbolAccess implements
    Consumer<EK9Parser.StreamStatementTerminationContext> {
  private final TypeCanReceivePipedData typeCanReceivePipedData = new TypeCanReceivePipedData();

  ProcessStreamStatementTermination(final SymbolAndScopeManagement symbolAndScopeManagement,
                                    final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.StreamStatementTerminationContext ctx) {

    var pipelinePartSymbol = getRecordedAndTypedSymbol(ctx.pipelinePart());
    if (pipelinePartSymbol != null) {
      processPipeLinePart(ctx, pipelinePartSymbol);
    }
  }

  private void processPipeLinePart(final EK9Parser.StreamStatementTerminationContext ctx,
                                   final ISymbol pipeLinePartSymbol) {

    var terminationSymbol = (StreamCallSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);

    pipeLinePartSymbol.getType().ifPresent(pipeLinePartType -> {
      var sourceToken = new Ek9Token(ctx.pipelinePart().start);
      if (checkTypeCouldBePipedInto(sourceToken, pipeLinePartType)) {
        updateTerminationSymbol(terminationSymbol, pipeLinePartSymbol);
      }
    });

  }

  private boolean checkTypeCouldBePipedInto(final IToken sourceToken, final ISymbol pipeLinePartType) {

    var rtn = typeCanReceivePipedData.test(pipeLinePartType);
    if (!rtn) {
      var msg = "wrt '" + pipeLinePartType.getFriendlyName() + "':";
      errorListener.semanticError(sourceToken, msg, UNABLE_TO_FIND_PIPE_FOR_TYPE);
    }
    return rtn;

  }

  private void updateTerminationSymbol(ISymbol terminationSymbol, final ISymbol pipeLinePartSymbol) {

    terminationSymbol.setGenus(pipeLinePartSymbol.getGenus());
    terminationSymbol.setType(pipeLinePartSymbol.getType());

  }
}
