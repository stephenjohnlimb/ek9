package org.ek9lang.compiler.symbols.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Converts a list of symbol using their friendly names in to a comma separated list.
 * Optionally wraps in parenthesis.
 */
public class ToCommaSeparated implements Function<List<ISymbol>, String> {
  private final boolean includeParenthesis;
  /**
   * Used when creating friendly name, because it can be recursive.
   * Imaging a function that returns itself or accepts a function of itself as a parameter.
   * When making a friendly name you'd get a stack overflow.
   */
  private final ISymbol fromType;

  public ToCommaSeparated(final boolean includeParenthesis) {
    this(null, includeParenthesis);
  }

  public ToCommaSeparated(final ISymbol fromType, final boolean includeParenthesis) {
    this.fromType = fromType;
    this.includeParenthesis = includeParenthesis;
  }

  @Override
  public String apply(List<ISymbol> params) {
    List<String> names = new ArrayList<>();

    for (var param : params) {
      var paramType = param.getType();
      if (paramType.isEmpty() || (fromType != null && paramType.get().isExactSameType(fromType))) {
        names.add(param.getName());
      } else {
        names.add(param.getFriendlyName());
      }
    }

    var commaSeparated = String.join(", ", names);
    if (!includeParenthesis) {
      return commaSeparated;
    }
    return "(" + commaSeparated + ")";
  }
}
