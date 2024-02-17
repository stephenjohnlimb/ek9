package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
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

    //We expect these to be defined and typed.
    var source = (StreamCallSymbol) getRecordedAndTypedSymbol(ctx.streamSource());
    var termination = (StreamCallSymbol) getRecordedAndTypedSymbol(ctx.streamStatementTermination());
    var streamParts = ctx.streamPart();

    //Otherwise there will have been errors emitted.
    if (source != null && source.getType().isPresent()
        && source.getProducesSymbolType() != null
        && termination != null && termination.getType().isPresent()) {
      processStreamAssembly.accept(new StreamAssembly(source, streamParts, termination));
    }

  }
}
