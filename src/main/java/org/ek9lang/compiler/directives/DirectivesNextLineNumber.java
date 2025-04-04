package org.ek9lang.compiler.directives;

import java.util.function.Function;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.ek9lang.antlr.EK9Parser;

/**
 * Finds the next line number from where a directive has been used.
 * This is useful for error detection as it enables the location of the directive to indicate where the
 * error condition is located.
 */
@SuppressWarnings("java:S4276")
public class DirectivesNextLineNumber implements Function<EK9Parser.DirectiveContext, Integer> {
  @Override
  public Integer apply(final EK9Parser.DirectiveContext ctx) {

    final var parent = ctx.getParent();
    //Need to get the next ctx to find the line the errors should appear.
    for (int i = 0; i < parent.children.size(); i++) {
      final var child = parent.getChild(i);

      if (child == ctx) {
        final var nextChild = parent.getChild(i + 1);
        if (nextChild instanceof ParserRuleContext ruleCtx) {
          return ruleCtx.start.getLine();
        } else if (nextChild instanceof TerminalNode terminalCtx) {
          return terminalCtx.getSymbol().getLine();
        }
      }
    }

    throw new IllegalArgumentException("Directives need to get next symbol to apply rule to, this has failed.");
  }
}
