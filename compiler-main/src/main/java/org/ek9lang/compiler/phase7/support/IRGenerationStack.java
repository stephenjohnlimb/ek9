package org.ek9lang.compiler.phase7.support;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import org.ek9lang.compiler.ir.DebugInfo;
import org.ek9lang.core.AssertValue;

/**
 * Stack-based scope, debug, and context management for IR generation.
 *
 * <p>This stack provides the transient context management that eliminates
 * parameter threading throughout the IR generation process. Modeled after
 * the successful ScopeStack pattern used in phases 1-6.</p>
 *
 * <p>Each frame on the stack represents a scope context (method, function,
 * block, expression) with its associated scope ID, debug information, and
 * other contextual information needed for IR generation.</p>
 *
 * <p>The stack automatically handles nested scope management and provides
 * navigation capabilities to find containing scopes of specific types.</p>
 */
public class IRGenerationStack {

  private final Deque<IRStackFrame> actualStack = new ArrayDeque<>();

  /**
   * Create a new IR generation stack with a base frame.
   */
  public IRGenerationStack(final IRStackFrame baseFrame) {
    AssertValue.checkNotNull("Base frame cannot be null", baseFrame);
    push(baseFrame);
  }

  /**
   * Get the very base frame of the stack (usually module level).
   */
  public IRStackFrame getVeryBaseFrame() {
    return actualStack.getFirst();
  }

  /**
   * Push a new frame onto the stack.
   */
  public IRStackFrame push(final IRStackFrame frame) {
    AssertValue.checkNotNull("Frame cannot be null", frame);
    actualStack.push(frame);
    return frame;
  }

  /**
   * Look at the top frame without removing it.
   */
  public IRStackFrame peek() {
    AssertValue.checkTrue("Stack cannot be empty for peek", !actualStack.isEmpty());
    return actualStack.peekFirst();
  }

  /**
   * Pop a frame off the stack.
   */
  public IRStackFrame pop() {
    AssertValue.checkTrue("Stack cannot be empty for pop", !actualStack.isEmpty());
    return actualStack.pop();
  }

  /**
   * Check if the stack is empty.
   */
  public boolean isEmpty() {
    return actualStack.isEmpty();
  }

  /**
   * Get current scope ID from the top frame.
   */
  public String currentScopeId() {
    return peek().scopeId();
  }

  /**
   * Get current debug info from the top frame.
   */
  public Optional<DebugInfo> currentDebugInfo() {
    return Optional.ofNullable(peek().debugInfo());
  }

  /**
   * Check if current scope has a left-hand side (for result variable decisions).
   */
  public boolean hasLeftHandSide() {
    return peek().hasLeftHandSide();
  }

  /**
   * Get current frame type from the top frame.
   */
  public IRFrameType currentFrameType() {
    return peek().frameType();
  }

  /**
   * Navigate back up the stack to find the first frame of the specified type.
   * Useful for finding containing method/function contexts from nested blocks.
   */
  public Optional<IRStackFrame> traverseBackToFrameType(final IRFrameType frameType) {
    for (IRStackFrame frame : actualStack) {
      if (frame.frameType().equals(frameType)) {
        return Optional.of(frame);
      }
    }
    return Optional.empty();
  }

  /**
   * Navigate back up the stack to find the first method or function frame.
   * Used for capturing context information in nested expressions.
   */
  public Optional<IRStackFrame> traverseBackToMethodOrFunction() {
    for (IRStackFrame frame : actualStack) {
      if (frame.frameType() == IRFrameType.METHOD || frame.frameType() == IRFrameType.FUNCTION) {
        return Optional.of(frame);
      }
    }
    return Optional.empty();
  }

  /**
   * Navigate back up the stack to find the containing aggregate (class/record/trait).
   * Used for synthesis operations that need access to the containing type.
   */
  public Optional<IRStackFrame> traverseBackToAggregate() {
    for (IRStackFrame frame : actualStack) {
      if (frame.frameType() == IRFrameType.CLASS
          || frame.frameType() == IRFrameType.RECORD
          || frame.frameType() == IRFrameType.TRAIT) {
        return Optional.of(frame);
      }
    }
    return Optional.empty();
  }

  /**
   * Get the depth of the stack (for debugging/testing).
   */
  public int depth() {
    return actualStack.size();
  }

  /**
   * Navigate back up the stack to find the first frame with context data of the specified type.
   * Used to find IRContext or other contextual information stored in stack frames.
   */
  public <T> Optional<T> traverseBackToContextData(final Class<T> contextType) {
    for (IRStackFrame frame : actualStack) {
      T contextData = frame.getContextData(contextType);
      if (contextData != null) {
        return Optional.of(contextData);
      }
    }
    return Optional.empty();
  }

  /**
   * Check if we're currently in a specific frame type.
   */
  public boolean isInFrameType(final IRFrameType frameType) {
    return !actualStack.isEmpty() && peek().frameType() == frameType;
  }

  /**
   * Check if we're currently in any of the specified frame types.
   */
  public boolean isInAnyFrameType(final IRFrameType... frameTypes) {
    if (actualStack.isEmpty()) {
      return false;
    }
    IRFrameType currentType = peek().frameType();
    for (IRFrameType type : frameTypes) {
      if (currentType == type) {
        return true;
      }
    }
    return false;
  }
}