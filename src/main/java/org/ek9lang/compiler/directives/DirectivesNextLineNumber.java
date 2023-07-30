package org.ek9lang.compiler.directives;

import java.util.function.Function;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.ek9lang.antlr.EK9Parser;

/**
 * Finds the next line number from where a directive has been used.
 */
@SuppressWarnings("java:S4276")
public class DirectivesNextLineNumber implements Function<EK9Parser.DirectiveContext, Integer> {
  @Override
  public Integer apply(EK9Parser.DirectiveContext ctx) {
    var parent = ctx.getParent();
    //Need to get the next ctx to find the line the errors should appear.
    for (int i = 0; i < parent.children.size(); i++) {
      var child = parent.getChild(i);

      if (child == ctx) {
        var nextChild = parent.getChild(i + 1);
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
