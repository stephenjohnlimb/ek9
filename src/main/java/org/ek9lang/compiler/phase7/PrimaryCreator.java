package org.ek9lang.compiler.phase7;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ParsedModule;
import org.ek9lang.compiler.ir.INode;
import org.ek9lang.compiler.ir.Literal;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Creates the appropriate IR Node for a 'primary'.
 * <pre>
 *   primary
 *     : LPAREN expression RPAREN
 *     | primaryReference
 *     | literal
 *     | identifierReference
 *     ;
 * </pre>
 */
final class PrimaryCreator implements Function<EK9Parser.PrimaryContext, INode> {

  private final ParsedModule parsedModule;

  public PrimaryCreator(final ParsedModule parsedModule) {
    this.parsedModule = parsedModule;
  }

  @Override
  public INode apply(final EK9Parser.PrimaryContext ctx) {
    if (ctx.literal() != null) {
      final var symbol = parsedModule.getRecordedSymbol(ctx);
      AssertValue.checkNotNull("Literal symbol should not be null", symbol);
      return new Literal(symbol);
    }

    throw new CompilerException("Primary not fully implemented yet");

  }
}
