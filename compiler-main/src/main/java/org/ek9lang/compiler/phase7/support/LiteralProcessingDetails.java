package org.ek9lang.compiler.phase7.support;

import org.ek9lang.compiler.symbols.ISymbol;

/**
 * A simple record that can be used when processing literals.
 * STACK-BASED: Scope information is now managed via stack context.
 */
public record LiteralProcessingDetails(ISymbol literalSymbol,
                                       String literalResult) {
}
