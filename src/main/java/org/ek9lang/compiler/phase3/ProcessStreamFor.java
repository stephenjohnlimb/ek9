package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.StreamCallSymbol;

/**
 * Deals with working out what the type being "for'd" is.
 * Unlike 'cat' this is basically the same type as the 'loop' iterator.
 * So most of this work has been done in the 'for range' part when produces a type.
 * But obviously if the 'for range' is broken then the type will not be known.
 */
final class ProcessStreamFor extends TypedSymbolAccess implements Consumer<EK9Parser.StreamForContext> {

  ProcessStreamFor(final SymbolAndScopeManagement symbolAndScopeManagement,
                   final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);

  }

  @Override
  public void accept(final EK9Parser.StreamForContext ctx) {

    final var streamFor = (StreamCallSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
    final var forRange = getRecordedAndTypedSymbol(ctx.forRange());

    if (streamFor != null && forRange != null) {
      forRange.getType().ifPresent(type -> {
        streamFor.setProducesSymbolType(type);
        streamFor.setType(type);
      });
    }

  }

}
