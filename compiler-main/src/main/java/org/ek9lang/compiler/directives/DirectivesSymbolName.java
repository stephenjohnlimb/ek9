package org.ek9lang.compiler.directives;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;

/**
 * Extracts the symbol name or throws an illegal argument exception.
 */
public class DirectivesSymbolName implements Function<EK9Parser.DirectiveContext, String> {

  private final int position;

  /**
   * New extractor of symbol name for a position in the source.
   */
  public DirectivesSymbolName(final int position) {

    this.position = position;

  }

  @Override
  public String apply(final EK9Parser.DirectiveContext ctx) {

    final var symbolName = ctx.directivePart(position).getText();
    if (!symbolName.startsWith("\"") && !symbolName.startsWith("`")) {
      //If not quoted then expect only integer value
      final var intValue = Integer.parseInt(symbolName);
      return Integer.toString(intValue);
    }

    final var result = symbolName.substring(1, symbolName.length() - 1);
    if (result.isEmpty()) {
      throw new IllegalArgumentException("Symbol name is empty");
    }

    return processEscapeSequences(result);
  }

  /**
   * Process escape sequences in the string content.
   * Handles: \$ -> $, \` -> `, \\ -> \, \n -> newline, \t -> tab
   */
  private String processEscapeSequences(final String input) {

    final var sb = new StringBuilder();
    var i = 0;
    while (i < input.length()) {
      final var c = input.charAt(i);
      if (c == '\\' && i + 1 < input.length()) {
        final var next = input.charAt(i + 1);
        switch (next) {
          case '$' -> {
            sb.append('$');
            i += 2;
          }
          case '`' -> {
            sb.append('`');
            i += 2;
          }
          case '\\' -> {
            sb.append('\\');
            i += 2;
          }
          case 'n' -> {
            sb.append('\n');
            i += 2;
          }
          case 't' -> {
            sb.append('\t');
            i += 2;
          }
          case 'r' -> {
            sb.append('\r');
            i += 2;
          }
          default -> {
            sb.append(c);
            i++;
          }
        }
      } else {
        sb.append(c);
        i++;
      }
    }
    return sb.toString();
  }

}
