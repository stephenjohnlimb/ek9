package org.ek9lang.compiler.symbol.support;

import java.util.List;

/**
 * Can be used for a single Simple named search.
 * Or with parameter types for a generic type.
 */
public record SymbolSearchForTest(String mainSymbolName, List<SymbolSearchForTest> parameterizingArguments) {

  public SymbolSearchForTest(String mainSymbolName, SymbolSearchForTest parameter) {
    this(mainSymbolName, List.of(parameter));
  }
  /**
   * Basic non parametric constructor, so just simple types.
   */
  public SymbolSearchForTest(String mainSymbolName) {
    this(mainSymbolName, List.of());
  }

  /**
   * Simple search by name, or generic with polymorphic parameterization.
   */
  public boolean isParametric() {
    return parameterizingArguments.size() > 0;
  }

  @Override
  public String toString() {
    if(parameterizingArguments().isEmpty()) {
      return mainSymbolName;
    }
    return mainSymbolName + " " + parameterizingArguments();
  }
}
