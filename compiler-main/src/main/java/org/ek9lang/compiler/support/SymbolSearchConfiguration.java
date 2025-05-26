package org.ek9lang.compiler.support;

import java.util.List;

/**
 * Can be used for a single Simple named search.
 * Or with parameter types for a generic type.
 */
public record SymbolSearchConfiguration(String mainSymbolName,
                                        List<SymbolSearchConfiguration> parameterizingArguments) {

  public SymbolSearchConfiguration(String mainSymbolName, SymbolSearchConfiguration parameter) {
    this(mainSymbolName, List.of(parameter));
  }

  /**
   * Basic non parametric constructor, so just simple types.
   */
  public SymbolSearchConfiguration(String mainSymbolName) {
    this(mainSymbolName, List.of());
  }

  /**
   * Simple search by name, or generic with polymorphic parameterization.
   */
  public boolean isParametric() {
    return !parameterizingArguments.isEmpty();
  }

  @Override
  public String toString() {
    if (parameterizingArguments().isEmpty()) {
      return mainSymbolName;
    }
    return mainSymbolName + " " + parameterizingArguments();
  }
}
