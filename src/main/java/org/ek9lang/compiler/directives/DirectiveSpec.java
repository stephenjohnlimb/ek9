package org.ek9lang.compiler.directives;

import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Specification of a directive, all the common details used in a range of directives.
 */
public record DirectiveSpec(Token token,
                            CompilationPhase phase,
                            ISymbol.SymbolCategory symbolCategory,
                            String symbolName,
                            String additionalName, int lineNumber) {
}