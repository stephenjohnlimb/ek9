package org.ek9lang.compiler.support;

import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * To be used in EK9 source code to assert that a type can or cannot be resolved.
 * //@Resolved: EXPLICIT_TYPE_SYMBOL_DEFINITION: TEMPLATE_TYPE: "List"
 */
public class ResolvedDirective extends ResolutionDirective {

  public ResolvedDirective(Token token,
                           CompilationPhase phase,
                           ISymbol.SymbolCategory symbolCategory,
                           String symbolName, int lineNumber) {
    super(token, phase, symbolCategory, symbolName, lineNumber);
  }

  @Override
  public DirectiveType type() {
    return DirectiveType.Resolved;
  }
}
