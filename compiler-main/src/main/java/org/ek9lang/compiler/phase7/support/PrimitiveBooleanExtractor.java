package org.ek9lang.compiler.phase7.support;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.calls.CallDetailsForIsTrue;

/**
 * CONCERN: Boolean primitive extraction from EK9 Boolean objects.
 * RESPONSIBILITY: Convert EK9 Boolean to primitive boolean via _true() with ARC compliance.
 * REUSABILITY: ALL generators needing primitive boolean values for control flow.
 * <p>
 * Encapsulates the pattern:
 * 1. Call _true() method on EK9 Boolean object (returns primitive boolean)
 * 2. No memory management needed - primitive booleans are value types
 * </p>
 * <p>
 * Used extensively in control flow generators (if, while, for) where
 * conditional expressions must be primitive booleans.
 * </p>
 */
public final class PrimitiveBooleanExtractor implements Function<BooleanExtractionParams, List<IRInstr>> {
  private final CallDetailsForIsTrue callDetailsForIsTrue = new CallDetailsForIsTrue();

  /**
   * Extract primitive boolean from EK9 Boolean object.
   * <p>
   * Handles:
   * - CALL _true() method (EK9 Boolean → primitive boolean)
   * - No memory management needed (primitive value)
   * </p>
   *
   * @param params Boolean extraction parameters
   * @return Instructions with primitive boolean in params.resultTemp()
   */
  @Override
  public List<IRInstr> apply(final BooleanExtractionParams params) {
    final var instruction = CallInstr.operator(
        new VariableDetails(params.resultTemp(), params.debugInfo()),
        callDetailsForIsTrue.apply(params.booleanObjectVar())
    );
    return List.of(instruction);
  }
}
