package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.support.SymbolFactory;
import org.ek9lang.compiler.symbols.StreamCallSymbol;

/**
 * Now does the check/processing of the whole stream pipeline.
 */
final class ProcessStreamStatement extends TypedSymbolAccess implements Consumer<EK9Parser.StreamStatementContext> {
  private final ProcessStreamAssembly processStreamAssembly;

  ProcessStreamStatement(final SymbolAndScopeManagement symbolAndScopeManagement,
                         final SymbolFactory symbolFactory,
                         final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);
    this.processStreamAssembly = new ProcessStreamAssembly(symbolAndScopeManagement, symbolFactory, errorListener);

  }

  @Override
  public void accept(final EK9Parser.StreamStatementContext ctx) {

    final var streamStatementSymbol = symbolAndScopeManagement.getRecordedSymbol(ctx);
    final var source = (StreamCallSymbol) getRecordedAndTypedSymbol(ctx.streamSource());
    final var termination = (StreamCallSymbol) getRecordedAndTypedSymbol(ctx.streamStatementTermination());
    final var streamParts = ctx.streamPart();

    //Always returns a void because it is a statement - not an expression - that has separate syntax.
    streamStatementSymbol.setType(symbolAndScopeManagement.getEk9Types().ek9Void());

    //Otherwise there will have been errors emitted.
    if (source != null && source.getType().isPresent() && source.getProducesSymbolType() != null
        && termination != null && termination.getType().isPresent()) {
      processStreamAssembly.accept(new StreamAssemblyData(source, streamParts, termination));
    }

  }
}
