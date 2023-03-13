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

  private final DirectiveSpec spec;

  /**
   * A new resolution style directive.
   * The symbol Name can be single like Integer or parameterised like List of (String).
   * If parameterised it must have the parenthesis.
   */
  protected ResolutionDirective(final DirectiveSpec spec) {
    this.spec = spec;
  }

  public boolean isForPhase(final CompilationPhase phase) {
    return spec.phase() == phase;
  }

  @Override
  public Token getDirectiveToken() {
    return spec.token();
  }

  @Override
  public int getAppliesToLineNumber() {
    return spec.lineNumber();
  }

  public ISymbol.SymbolCategory getSymbolCategory() {
    return spec.symbolCategory();
  }

  public String getSymbolName() {
    return spec.symbolName();
  }

  public String getAdditionalName() {
    return spec.additionalName();
  }

  @Override
  public String toString() {
    var base = type() + ": " + spec.phase() + ": " + spec.symbolCategory() + ": \"" + spec.symbolName() + "\"";

    if (spec.additionalName() != null) {
      base += ": \"" + getAdditionalName() + "\"";
    }
    return base + ": Line: " + getAppliesToLineNumber();
  }
}
