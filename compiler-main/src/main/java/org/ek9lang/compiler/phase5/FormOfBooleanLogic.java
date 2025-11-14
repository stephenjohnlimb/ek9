package org.ek9lang.compiler.phase5;

import java.util.List;
import java.util.function.Predicate;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.SymbolTypeOrException;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.Ek9Types;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Predicate to determine if an expression is a Boolean logical operator (and/or)
 * with Boolean type operands.
 * <p>
 * This is critical for complexity calculation because:
 * - Boolean and/or creates branching (short-circuit evaluation) → adds complexity
 * - Bits and/or is bitwise operation (no branching) → does NOT add complexity
 * </p>
 * <p>
 * Only returns true when:
 * 1. Operator is "and" or "or"
 * 2. BOTH operands are Boolean type (not Bits type)
 * </p>
 */
final class FormOfBooleanLogic implements Predicate<EK9Parser.ExpressionContext> {

  private final SymbolsAndScopes symbolsAndScopes;
  private final Ek9Types ek9Types;
  private final SymbolTypeOrException symbolTypeOrException = new SymbolTypeOrException();

  FormOfBooleanLogic(final SymbolsAndScopes symbolsAndScopes, final Ek9Types ek9Types) {

    this.symbolsAndScopes = symbolsAndScopes;
    this.ek9Types = ek9Types;

  }

  @Override
  public boolean test(final EK9Parser.ExpressionContext ctx) {

    if (ctx.op == null) {
      return false;
    }

    final String op = ctx.op.getText();
    if (!("and".equals(op) || "or".equals(op))) {
      return false;
    }

    final List<EK9Parser.ExpressionContext> operands = ctx.expression();
    if (operands.size() != 2) {
      return false;
    }

    return areBothOperandsBoolean(operands.get(0), operands.get(1));

  }

  private boolean areBothOperandsBoolean(final EK9Parser.ExpressionContext left,
                                         final EK9Parser.ExpressionContext right) {

    final var leftSymbol = symbolsAndScopes.getRecordedSymbol(left);
    final var rightSymbol = symbolsAndScopes.getRecordedSymbol(right);

    if (leftSymbol == null || rightSymbol == null) {
      return false;
    }

    return isBooleanType(leftSymbol) && isBooleanType(rightSymbol);

  }

  private boolean isBooleanType(final ISymbol symbol) {
    // SAFE pattern: Use ek9Types and isExactSameType() (not string comparison)
    return symbolTypeOrException.apply(symbol).isExactSameType(ek9Types.ek9Boolean());

  }

}
