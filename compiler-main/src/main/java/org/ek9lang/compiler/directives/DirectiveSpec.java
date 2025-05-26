package org.ek9lang.compiler.directives;

import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.symbols.SymbolCategory;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Specification of a directive, all the common details used in a range of directives.
 */
public record DirectiveSpec(IToken token,
                            CompilationPhase phase,
                            SymbolCategory symbolCategory,
                            String symbolName,
                            String additionalName,
                            int lineNumber) {
}
