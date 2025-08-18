package org.ek9lang.compiler.phase7.support;

import org.ek9lang.compiler.symbols.ISymbol;

/**
 * A simple record that can be used when processing literals.
 */
public record LiteralProcessingDetails(ISymbol literalSymbol,
                                       String literalResult,
                                       String scopeId) {
}
