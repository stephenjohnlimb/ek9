package org.ek9lang.compiler.support;

import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Specification of a directive, all the common details used in a range of directives.
 */
public record DirectiveSpec(Token token,
                            CompilationPhase phase,
                            ISymbol.SymbolCategory symbolCategory,
                            String symbolName,
                            String additionalName, int lineNumber) {
}
