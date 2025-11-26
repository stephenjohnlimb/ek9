package org.ek9lang.compiler.phase7.generator;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.core.AssertValue;

/**
 * Coordinates for-statement IR generation by delegating to specialized generators.
 * <p>
 * Handles two distinct loop types:
 * </p>
 * <ul>
 *   <li>For-range loops (for i in 1..10): Delegates to ForRangeGenerator</li>
 *   <li>For-in loops (for item in collection): Delegates to ForInGenerator</li>
 * </ul>
 * <p>
 * ARCHITECTURE: Follows ExprInstrGenerator coordinator pattern - thin orchestration
 * layer that delegates complex implementation to focused, specialized generators.
 * </p>
 */
public final class ForStatementGenerator extends AbstractGenerator
    implements Function<EK9Parser.ForStatementExpressionContext, List<IRInstr>> {

  private final GeneratorSet generators;

  public ForStatementGenerator(final IRGenerationContext stackContext,
                               final GeneratorSet generators) {
    super(stackContext);
    AssertValue.checkNotNull("GeneratorSet cannot be null", generators);
    this.generators = generators;
  }

  /**
   * Main entry point for for-statement IR generation.
   * <p>
   * Routes to appropriate specialized generator based on loop type:
   * - forLoop() present → for-in loop (iterator-based collection iteration)
   * - forRange() present → for-range loop (polymorphic range iteration)
   * - returningParam() present → expression form (not yet implemented)
   * </p>
   *
   * @param ctx ForStatementExpressionContext from parser
   * @return IR instructions for the complete for-statement
   */
  @Override
  public List<IRInstr> apply(final EK9Parser.ForStatementExpressionContext ctx) {
    AssertValue.checkNotNull("ForStatementExpressionContext cannot be null", ctx);

    if (ctx.forLoop() != null) {
      // For-in loop (iterator-based): for item in collection
      // Expression form is supported via returningParam (accumulator pattern)
      return generators.forInGenerator.apply(ctx);
    }

    // For-range loop: for i in 1..10 or for i in start..end BY step
    // Expression form is supported via returningParam (accumulator pattern)
    return generators.forRangeGenerator.apply(ctx);
  }
}
