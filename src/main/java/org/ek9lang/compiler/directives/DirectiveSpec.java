package org.ek9lang.compiler.directives;

import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Specification of a directive, all the common details used in a range of directives.
 */
public record DirectiveSpec(IToken token,
                            CompilationPhase phase,
                            ISymbol.SymbolCategory symbolCategory,
                            String symbolName,
                            String additionalName,
                            int lineNumber) {
}
