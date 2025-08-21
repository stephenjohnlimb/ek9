package org.ek9lang.compiler.phase7.support;

import org.ek9lang.compiler.ir.IROpcode;
import org.ek9lang.compiler.ir.LogicalOperationInstr;

/**
 * Record to group logical operation context information.
 * <p>
 * This pattern centralizes operation metadata that defines the identity and 
 * result of a logical operation (AND/OR):
 * - IROpcode: The specific IR operation code (LOGICAL_AND_BLOCK/LOGICAL_OR_BLOCK)
 * - Result: The variable name that will hold the operation result
 * - Operation: The logical operation type (AND/OR enum)
 * </p>
 * <p>
 * Provides a foundation for consistent operation context handling across
 * logical operations and future similar constructs.
 * </p>
 */
public record LogicalOperationContext(
    IROpcode opcode,
    String result,
    LogicalOperationInstr.Operation operation) {

  public LogicalOperationContext {
    if (opcode == null) {
      throw new IllegalArgumentException("Opcode cannot be null");
    }
    if (result == null || result.isBlank()) {
      throw new IllegalArgumentException("Result cannot be null or blank");
    }
    if (operation == null) {
      throw new IllegalArgumentException("Operation cannot be null");
    }
  }
}