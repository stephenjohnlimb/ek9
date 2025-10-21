package org.ek9lang.compiler.phase7.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.instructions.IRInstr;

/**
 * CONCERN: Comparison evaluation with boolean primitive extraction.
 * RESPONSIBILITY: Compare two values and extract primitive boolean result.
 * REUSABILITY: ALL control flow generators needing comparison conditions.
 * <p>
 * Composite helper that combines:
 * 1. BinaryOperatorInvoker (for comparison operator call)
 * 2. PrimitiveBooleanExtractor (for UNBOX to primitive boolean)
 * </p>
 * <p>
 * This pattern appears in:
 * - ForStatementGenerator (loop condition checks)
 * - WhileStatementGenerator (loop condition)
 * - IfStatementGenerator (conditional evaluation)
 * - SwitchStatementGenerator (case comparisons)
 * </p>
 */
public final class ComparisonEvaluator implements Function<ComparisonParams, List<IRInstr>> {
  private final BinaryOperatorInvoker binaryOperatorInvoker;
  private final PrimitiveBooleanExtractor primitiveBooleanExtractor;

  public ComparisonEvaluator(
      final BinaryOperatorInvoker binaryOperatorInvoker,
      final PrimitiveBooleanExtractor primitiveBooleanExtractor) {
    this.binaryOperatorInvoker = binaryOperatorInvoker;
    this.primitiveBooleanExtractor = primitiveBooleanExtractor;
  }

  /**
   * Evaluate comparison and extract primitive boolean.
   * <p>
   * Handles:
   * 1. Binary operator invocation (e.g., "&lt;", "&lt;=", "&gt;", "&gt;=", "==")
   * 2. UNBOX of EK9 Boolean result to primitive boolean
   * 3. Memory management for both operations
   * </p>
   *
   * @param params Comparison parameters
   * @return Instructions ending with primitive boolean in params.primitiveBooleanTemp()
   */
  @Override
  public List<IRInstr> apply(final ComparisonParams params) {

    // Step 1: Call comparison operator (returns EK9 Boolean object)
    final var binaryParams = new BinaryOperatorParams(
        params.leftOperand(),
        params.rightOperand(),
        params.operator(),
        params.leftType(),
        params.rightType(),
        params.booleanType(),
        params.booleanObjectTemp(),
        params.scopeId(),
        params.debugInfo()
    );

    final var instructions = new ArrayList<>(binaryOperatorInvoker.apply(binaryParams));

    // Step 2: Extract primitive boolean from EK9 Boolean object
    final var extractionParams = new BooleanExtractionParams(
        params.booleanObjectTemp(),
        params.primitiveBooleanTemp(),
        params.debugInfo()
    );
    instructions.addAll(primitiveBooleanExtractor.apply(extractionParams));

    return instructions;
  }
}
