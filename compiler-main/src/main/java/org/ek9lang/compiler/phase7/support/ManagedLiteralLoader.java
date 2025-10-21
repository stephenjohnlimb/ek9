package org.ek9lang.compiler.phase7.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.LiteralInstr;

/**
 * CONCERN: Literal loading with memory management.
 * RESPONSIBILITY: Load literal value with ARC compliance.
 * REUSABILITY: ALL generators using literals.
 * <p>
 * Encapsulates the pattern:
 * 1. Create LOAD_LITERAL instruction
 * 2. Apply memory management (RETAIN/SCOPE_REGISTER)
 * </p>
 * <p>
 * Ensures all literal values have consistent memory management,
 * preventing reference counting bugs.
 * </p>
 */
public final class ManagedLiteralLoader implements Function<LiteralParams, List<IRInstr>> {
  private final VariableMemoryManagement variableMemoryManagement;

  public ManagedLiteralLoader(final VariableMemoryManagement variableMemoryManagement) {
    this.variableMemoryManagement = variableMemoryManagement;
  }

  /**
   * Load literal value into temp variable with memory management.
   * <p>
   * Handles:
   * - LOAD_LITERAL instruction
   * - RETAIN/SCOPE_REGISTER via variableMemoryManagement
   * </p>
   *
   * @param params Literal loading parameters
   * @return Instructions with literal loaded into params.tempName()
   */
  @Override
  public List<IRInstr> apply(final LiteralParams params) {
    final var literalInstructions = new ArrayList<IRInstr>();
    literalInstructions.add(LiteralInstr.literal(
        params.tempName(),
        params.literalValue(),
        params.literalType().getFullyQualifiedName(),
        params.debugInfo()
    ));

    return variableMemoryManagement.apply(
        () -> literalInstructions,
        new VariableDetails(params.tempName(), params.debugInfo())
    );
  }
}
