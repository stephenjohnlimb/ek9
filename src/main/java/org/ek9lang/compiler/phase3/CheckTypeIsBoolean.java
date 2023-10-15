package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.MUST_RESULT_IN_A_BOOLEAN;

import java.util.function.BiConsumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Just checks the symbol has a type of Boolean, can be used in control statements.
 */
public class CheckTypeIsBoolean extends TypedSymbolAccess implements BiConsumer<IToken, ISymbol> {
  protected CheckTypeIsBoolean(SymbolAndScopeManagement symbolAndScopeManagement,
                               ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final IToken lineToken, final ISymbol symbol) {
    if (symbol != null) {
      symbol.getType().ifPresent(symbolType -> {
        if (!symbolAndScopeManagement.getEk9Types().ek9Boolean().isExactSameType(symbolType)) {
          errorListener.semanticError(lineToken, "", MUST_RESULT_IN_A_BOOLEAN);
        }
      });
    }
  }
}
