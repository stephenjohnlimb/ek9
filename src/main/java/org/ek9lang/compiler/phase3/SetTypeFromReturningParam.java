package org.ek9lang.compiler.phase3;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Looks at the returning param (if not null) and sets the symbol type as appropriate.
 */
public class SetTypeFromReturningParam extends TypedSymbolAccess
    implements BiConsumer<ISymbol, EK9Parser.ReturningParamContext> {
  protected SetTypeFromReturningParam(SymbolAndScopeManagement symbolAndScopeManagement,
                                      ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final ISymbol symbol, final EK9Parser.ReturningParamContext ctx) {
    if (ctx != null) {
      var returning = getRecordedAndTypedSymbol(ctx);
      if (returning != null) {
        symbol.setType(returning.getType());
      }
    } else {
      symbol.setType(symbolAndScopeManagement.getEk9Types().ek9Void());
    }
  }
}
