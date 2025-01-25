package org.ek9lang.compiler.phase3;

import org.ek9lang.compiler.symbols.IScopedSymbol;

/**
 * For doing pure checks on method and functions.
 */
record PureCheckData(String errorMessage, IScopedSymbol superSymbol, IScopedSymbol thisSymbol) {
}
