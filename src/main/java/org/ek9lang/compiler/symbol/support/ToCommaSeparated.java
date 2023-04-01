package org.ek9lang.compiler.symbol.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Converts a list of symbol using their friendly names in to a comma separated list.
 * Optionally wraps in parenthesis.
 */
public class ToCommaSeparated implements Function<List<ISymbol>, String> {
  private final boolean includeParenthesis;

  public ToCommaSeparated(final boolean includeParenthesis) {
    this.includeParenthesis = includeParenthesis;
  }

  @Override
  public String apply(List<ISymbol> params) {
    List<String> names = new ArrayList<>();

    for (var param : params) {
      var paramType = param.getType();
      if (paramType.isPresent()) {
        names.add(param.getFriendlyName());
      } else {
        names.add(param.getName());
      }
    }

    var commaSeparated = String.join(", ", names);
    if (!includeParenthesis) {
      return commaSeparated;
    }
    return "(" + commaSeparated + ")";
  }
}
