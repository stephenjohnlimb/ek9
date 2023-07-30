package org.ek9lang.compiler.main.resolvedefine;

import org.ek9lang.compiler.symbols.ScopedSymbol;

/**
 * For doing pure checks on method and functions.
 */
public record PureCheckData(String errorMessage, ScopedSymbol superSymbol, ScopedSymbol thisSymbol) {
}
