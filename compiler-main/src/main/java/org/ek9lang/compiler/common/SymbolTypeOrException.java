package org.ek9lang.compiler.common;

import java.util.function.UnaryOperator;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.CompilerException;

/**
 * Just gets the type from a symbol, or throws a compiler exception if missing.
 * Missing types should have been detected in earlier stages.
 * So be carful which phase you use this with.
 */
public final class SymbolTypeOrException implements UnaryOperator<ISymbol> {

  @Override
  public ISymbol apply(final ISymbol symbol) {

    return symbol.getType().orElseThrow(() -> new CompilerException("Type should be known"));
  }
}
