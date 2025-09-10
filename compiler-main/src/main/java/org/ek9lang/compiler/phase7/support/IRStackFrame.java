package org.ek9lang.compiler.phase7.support;

import org.ek9lang.compiler.ir.DebugInfo;

/**
 * Represents a single frame on the IR generation stack.
 * 
 * <p>Each frame captures the essential context information needed
 * for IR generation at a specific scope level (method, function,
 * block, expression, etc.).</p>
 * 
 * <p>This eliminates the need for parameter threading by providing
 * all contextual information through the stack frame.</p>
 * 
 * @param scopeId The unique identifier for this scope
 * @param debugInfo Debug information for this scope (may be null if debug disabled)
 * @param frameType The type of frame (method, function, block, etc.)
 * @param hasLeftHandSide Whether this scope has a left-hand side for result variables
 * @param contextData Additional context data specific to this frame type
 */
public record IRStackFrame(
    String scopeId,
    DebugInfo debugInfo,
    IRFrameType frameType,
    boolean hasLeftHandSide,
    Object contextData
) {

  /**
   * Create a basic frame with just scope and debug info.
   */
  public static IRStackFrame basic(String scopeId, DebugInfo debugInfo, IRFrameType frameType) {
    return new IRStackFrame(scopeId, debugInfo, frameType, false, null);
  }

  /**
   * Create a frame with left-hand side indication.
   */
  public static IRStackFrame withLeftHandSide(String scopeId, DebugInfo debugInfo, 
                                              IRFrameType frameType, boolean hasLeftHandSide) {
    return new IRStackFrame(scopeId, debugInfo, frameType, hasLeftHandSide, null);
  }

  /**
   * Create a frame with additional context data.
   */
  public static IRStackFrame withContext(String scopeId, DebugInfo debugInfo, 
                                         IRFrameType frameType, Object contextData) {
    return new IRStackFrame(scopeId, debugInfo, frameType, false, contextData);
  }

  /**
   * Create a full frame with all parameters.
   */
  public static IRStackFrame full(String scopeId, DebugInfo debugInfo, IRFrameType frameType,
                                  boolean hasLeftHandSide, Object contextData) {
    return new IRStackFrame(scopeId, debugInfo, frameType, hasLeftHandSide, contextData);
  }

  /**
   * Get typed context data if available and of expected type.
   */
  @SuppressWarnings("unchecked")
  public <T> T getContextData(Class<T> expectedType) {
    if (contextData != null && expectedType.isInstance(contextData)) {
      return (T) contextData;
    }
    return null;
  }

  /**
   * Check if this frame has context data of the specified type.
   */
  public boolean hasContextData(Class<?> expectedType) {
    return contextData != null && expectedType.isInstance(contextData);
  }
}