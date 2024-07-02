package org.ek9lang.compiler.directives;

import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

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
  ResolutionDirective(final DirectiveSpec spec) {

    this.spec = spec;

  }

  /**
   * returns true if the directive is for the phase passed in.
   */
  public boolean isForPhase(final CompilationPhase phase) {

    return spec.phase() == phase;

  }

  @Override
  public IToken getDirectiveToken() {

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

  public boolean isForVariable() {

    return ISymbol.SymbolCategory.VARIABLE.equals(getSymbolCategory());
  }

  public SymbolSearch getSymbolSearch() {

    return new SymbolSearch(getSymbolName()).setSearchType(getSymbolCategory());
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
