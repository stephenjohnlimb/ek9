package org.ek9lang.compiler.phase3;

import org.ek9lang.compiler.symbols.ScopedSymbol;

/**
 * For doing pure checks on method and functions.
 */
record PureCheckData(String errorMessage, ScopedSymbol superSymbol, ScopedSymbol thisSymbol) {
}
