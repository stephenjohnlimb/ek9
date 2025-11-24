package org.ek9lang.compiler.backend.jvm;

import java.util.Objects;

/**
 * Single frame on bytecode generation context stack.
 * Mirrors IRStackFrame pattern from IR generation phase.
 * <p>
 * Each frame represents a control flow construct (loop, switch, try-catch, if-chain)
 * and carries type-specific context data accessed via generic getContextData().
 * </p>
 */
public record BytecodeStackFrame(
    String scopeId,              // Unique identifier for this scope (e.g., "for_loop_123")
    BytecodeFrameType frameType, // Type of control flow (LOOP, SWITCH, etc.)
    Object contextData           // Type-specific data (LoopContextData, SwitchContextData, etc.)
) {

  /**
   * Create frame with type-specific context data.
   */
  public static BytecodeStackFrame withContext(
      final String scopeId,
      final BytecodeFrameType frameType,
      final Object contextData) {
    Objects.requireNonNull(scopeId, "scopeId cannot be null");
    Objects.requireNonNull(frameType, "frameType cannot be null");
    return new BytecodeStackFrame(scopeId, frameType, contextData);
  }

  /**
   * Type-safe context data retrieval.
   *
   * @param expectedType Class of expected context data type
   * @return Context data cast to expected type, or null if not assignable
   */
  @SuppressWarnings("unchecked")
  public <T> T getContextData(final Class<T> expectedType) {
    if (expectedType.isInstance(contextData)) {
      return (T) contextData;
    }
    return null;
  }

}
