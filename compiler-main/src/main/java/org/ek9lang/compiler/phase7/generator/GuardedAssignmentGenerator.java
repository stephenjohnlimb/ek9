package org.ek9lang.compiler.phase7.generator;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Generates IR instructions for guarded assignment operations (:=? - ASSIGN_UNSET).
 * <p>
 * This generator now uses composition with GUARDED_ASSIGNMENT_BLOCK for declarative,
 * backend-optimizable IR generation. The complex branching logic has been replaced
 * with a high-level block construct that reuses proven QUESTION_BLOCK semantics.
 * </p>
 * <p>
 * Guarded assignment only assigns to the left-hand side if:
 * 1. LHS is null/uninitialized, OR
 * 2. LHS is not null but _isSet() returns false
 * </p>
 * <p>
 * The GUARDED_ASSIGNMENT_BLOCK construct uses QUESTION_BLOCK composition internally
 * to ensure consistent null-safety behavior across the EK9 compiler.
 * </p>
 */
final class GuardedAssignmentGenerator extends AbstractGenerator
    implements Function<GuardedAssignmentGenerator.GuardedAssignmentDetails, List<IRInstr>> {

  private final GuardedAssignmentBlockGenerator guardedAssignmentBlockGenerator;

  public GuardedAssignmentGenerator(final IRGenerationContext stackContext,
                                    final QuestionBlockGenerator questionBlockGenerator,
                                    final AssignExpressionToSymbol assignExpressionToSymbol) {
    super(stackContext);
    this.guardedAssignmentBlockGenerator = new GuardedAssignmentBlockGenerator(
        stackContext, questionBlockGenerator, assignExpressionToSymbol);
  }

  @Override
  public List<IRInstr> apply(final GuardedAssignmentDetails details) {
    // Delegate to the block generator using composition
    return guardedAssignmentBlockGenerator.apply(details);
  }

  /**
   * Data class to hold parameters for guarded assignment generation.
   * STACK-BASED: Scope information is now managed via stack context.
   */
  public record GuardedAssignmentDetails(
      ISymbol lhsSymbol,
      EK9Parser.AssignmentExpressionContext assignmentExpression
  ) {
  }
}