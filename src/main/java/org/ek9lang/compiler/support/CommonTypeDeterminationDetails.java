package org.ek9lang.compiler.support;

import java.util.List;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * For use when looking to find common types.
 * The main reason for separating the arguments from their types it to support
 * additional functionality for finding iterators - when may not be directly on the argument.
 *
 * @param lineToken       - Typically where you want to display errors.
 * @param argumentSymbols The argument symbols - these are separated from their types
 * @param argumentTypes   The actual argument types.
 */
public record CommonTypeDeterminationDetails(IToken lineToken,
                                             List<ISymbol> argumentSymbols,
                                             List<ISymbol> argumentTypes) {
}
