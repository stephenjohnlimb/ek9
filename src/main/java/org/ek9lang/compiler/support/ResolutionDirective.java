package org.ek9lang.compiler.support;

import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * To be used in EK9 source code to assert that a type can or cannot be resolved.
 * //@Resolved: EXPLICIT_TYPE_SYMBOL_DEFINITION: TEMPLATE_TYPE: "List"
 * //@NotResolved: EXPLICIT_TYPE_SYMBOL_DEFINITION: TYPE: "List of Integer"
 */
public abstract class ResolutionDirective implements Directive {
  private final CompilationPhase phase;

  private final Token directiveToken;

  private final ISymbol.SymbolCategory symbolCategory;

  private final String symbolName;
  private final int lineNumber;

  /**
   * A new resolution style directive.
   * The symbol Name can be single like Integer or parameterised like List of (String).
   * If parameterised it must have the parenthesis.
   */
  protected ResolutionDirective(final Token token, final CompilationPhase phase,
                             final ISymbol.SymbolCategory symbolCategory,
                             final String symbolName,
                             final int lineNumber) {
    this.directiveToken = token;
    this.symbolCategory = symbolCategory;
    this.symbolName = symbolName;
    this.phase = phase;
    this.lineNumber = lineNumber;
  }

  public boolean isForPhase(final CompilationPhase phase) {
    return this.phase == phase;
  }

  @Override
  public Token getDirectiveToken() {
    return directiveToken;
  }

  @Override
  public int getAppliesToLineNumber() {
    return lineNumber;
  }

  public ISymbol.SymbolCategory getSymbolCategory() {
    return symbolCategory;
  }

  public String getSymbolName() {
    return symbolName;
  }

  @Override
  public String toString() {
    return type() + ": " + phase + ": " + symbolCategory + ": \"" + symbolName + "\": Line: " + lineNumber;
  }
}
