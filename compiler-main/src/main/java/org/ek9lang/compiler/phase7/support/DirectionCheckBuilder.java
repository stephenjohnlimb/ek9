package org.ek9lang.compiler.phase7.support;

import java.util.ArrayList;
import java.util.function.Function;

/**
 * CONCERN: Direction check evaluation for polymorphic for-range loops.
 * RESPONSIBILITY: Build direction check IR (direction &lt; 0 or direction &gt; 0).
 * REUSABILITY: FOR_RANGE_POLYMORPHIC ascending/descending case generation.
 * <p>
 * Domain-specific helper that eliminates the symmetric duplication between:
 * - generateDirectionLessThanZero (direction &lt; 0)
 * - generateDirectionGreaterThanZero (direction &gt; 0)
 * </p>
 * <p>
 * The only difference between these methods was the comparison operator ("&lt;" vs "&gt;").
 * This helper makes that difference explicit via DirectionCheckParams.
 * </p>
 * <p>
 * Pattern:
 * 1. Load literal 0
 * 2. Call direction &lt; 0 (or &gt; 0)
 * 3. Extract primitive boolean result
 * All wrapped in proper scope management and memory management.
 * </p>
 */
public final class DirectionCheckBuilder implements Function<DirectionCheckParams, ConditionEvaluationResult> {
  private final ScopedInstructionExecutor scopedInstructionExecutor;
  private final ManagedLiteralLoader managedLiteralLoader;
  private final ComparisonEvaluator comparisonEvaluator;

  public DirectionCheckBuilder(
      final ScopedInstructionExecutor scopedInstructionExecutor,
      final ManagedLiteralLoader managedLiteralLoader,
      final ComparisonEvaluator comparisonEvaluator) {
    this.scopedInstructionExecutor = scopedInstructionExecutor;
    this.managedLiteralLoader = managedLiteralLoader;
    this.comparisonEvaluator = comparisonEvaluator;
  }

  /**
   * Build direction check IR with scoped execution.
   * <p>
   * Handles:
   * 1. Scope enter/exit for all temporary variables
   * 2. Load literal 0 with memory management
   * 3. Compare direction &lt; 0 (or &gt; 0) with memory management
   * 4. Extract primitive boolean for branching
   * </p>
   *
   * @param params Direction check parameters
   * @return Result containing instructions and primitive boolean variable name
   */
  @Override
  public ConditionEvaluationResult apply(final DirectionCheckParams params) {
    final var instructions = scopedInstructionExecutor.execute(() -> {

      // Step 1: Load literal 0 with memory management
      final var literalParams = new LiteralParams(
          params.zeroTemp(),
          "0",
          params.integerType(),
          params.debugInfo()
      );

      final var scopedInstructions = new ArrayList<>(managedLiteralLoader.apply(literalParams));

      // Step 2: Evaluate direction comparison (&lt; 0 or &gt; 0) and extract primitive boolean
      final var comparisonParams = new ComparisonParams(
          params.directionTemp(),
          params.zeroTemp(),
          params.comparisonOperator(),
          params.integerType(),
          params.integerType(),
          params.booleanType(),
          params.booleanObjectTemp(),
          params.primitiveBooleanTemp(),
          params.scopeId(),
          params.debugInfo()
      );
      scopedInstructions.addAll(comparisonEvaluator.apply(comparisonParams));

      return scopedInstructions;
    }, params.debugInfo());

    return new ConditionEvaluationResult(instructions, params.primitiveBooleanTemp());
  }
}
