package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.instructions.BranchInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.BooleanExtractionParams;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.RecordExprProcessing;
import org.ek9lang.core.AssertValue;

/**
 * gIR Generation for the assert statement.
 */
public final class AssertStmtGenerator extends AbstractGenerator
    implements Function<EK9Parser.AssertStatementContext, List<IRInstr>> {

  private final GeneratorSet generators;
  private final RecordExprProcessing recordExprProcessing;

  /**
   * Constructor accepting injected GeneratorSet and RecordExprProcessing.
   * Eliminates internal generator creation for better object reuse.
   */
  AssertStmtGenerator(final IRGenerationContext stackContext,
                      final GeneratorSet generators,
                      final RecordExprProcessing recordExprProcessing) {
    super(stackContext);
    AssertValue.checkNotNull("GeneratorSet cannot be null", generators);
    this.generators = generators;
    this.recordExprProcessing = recordExprProcessing;
  }

  @Override
  public List<IRInstr> apply(final EK9Parser.AssertStatementContext ctx) {

    AssertValue.checkNotNull("Ctx cannot be null", ctx);

    final var assertStmtDebugInfo = stackContext.createDebugInfo(ctx.ASSERT().getSymbol());

    // Use helper to create temp variable
    final var exprDetails = createTempVariable(assertStmtDebugInfo);

    final var instructions = new ArrayList<>(recordExprProcessing.apply(
        new ExprProcessingDetails(ctx.expression(), exprDetails)));

    // Use helper to convert to primitive boolean
    final var primitiveResult = stackContext.generateTempName();
    final var extractionParams = new BooleanExtractionParams(
        exprDetails.resultVariable(), primitiveResult, assertStmtDebugInfo);
    instructions.addAll(generators.primitiveBooleanExtractor.apply(extractionParams));

    // Assert on the primitive boolean result - back-end will then implement that.
    instructions.add(BranchInstr.assertValue(primitiveResult, assertStmtDebugInfo));

    return instructions;
  }
}
