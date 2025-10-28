package org.ek9lang.compiler.backend.jvm;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.objectweb.asm.Label;

/**
 * Context stack for bytecode generation control flow.
 * Mirrors IRGenerationContext pattern from IR generation phase.
 * <p>
 * Manages stack of control flow contexts (loop, switch, try-catch, if-chain)
 * allowing nested generators to query enclosing context for label information.
 * </p>
 * <p>
 * Phase 1: Loop support only (fixes nested if in for-range bug).
 * </p>
 */
public class BytecodeGenerationContext {

  /**
   * Dispatch case for FOR_RANGE_POLYMORPHIC three-way generation.
   * Tracks which dispatch case (equal, ascending, descending) is currently being generated.
   * Used to make label names unique when the same body IR is processed three times.
   */
  public enum DispatchCase {
    NONE,       // Not in a FOR_RANGE dispatch case
    EQUAL,      // FOR_RANGE equal case (single iteration, direction == 0)
    ASCENDING,  // FOR_RANGE ascending loop case (direction < 0, increment)
    DESCENDING  // FOR_RANGE descending loop case (direction > 0, decrement)
  }

  private final Deque<BytecodeStackFrame> stack = new ArrayDeque<>();
  private DispatchCase currentDispatchCase = DispatchCase.NONE;

  // ==================== LOOP OPERATIONS ====================

  /**
   * Enter loop context BEFORE processing loop body.
   *
   * @param scopeId       Unique scope identifier (e.g., "for_loop_123")
   * @param continueLabel Where to jump for next iteration (increment point)
   * @param exitLabel     Where to jump when loop exits naturally
   */
  public void enterLoop(final String scopeId, final Label continueLabel, final Label exitLabel) {
    final var loopData = new LoopContextData(continueLabel, exitLabel);
    final var frame = BytecodeStackFrame.withContext(scopeId, BytecodeFrameType.LOOP, loopData);
    stack.push(frame);
  }

  /**
   * Exit loop context AFTER processing loop body.
   * Must be called in finally block to ensure cleanup.
   */
  public void exitLoop() {
    validateAndPop(BytecodeFrameType.LOOP, "loop");
  }

  /**
   * Query: Are we currently inside a loop.
   *
   * @return true if any enclosing frame is a loop
   */
  public boolean isInsideLoop() {
    return findFrame(BytecodeFrameType.LOOP).isPresent();
  }

  /**
   * Get enclosing loop's continue label.
   * Used by nested control flow (if statements) to determine where to jump after body.
   *
   * @return Continue label of nearest enclosing loop, or empty if not in loop
   */
  public Optional<Label> getLoopContinueLabel() {
    return findContextData(BytecodeFrameType.LOOP, LoopContextData.class)
        .map(LoopContextData::continueLabel);
  }

  /**
   * Get enclosing loop's exit label.
   * Used for early loop exit scenarios (future).
   *
   * @return Exit label of nearest enclosing loop, or empty if not in loop
   */
  public Optional<Label> getLoopExitLabel() {
    return findContextData(BytecodeFrameType.LOOP, LoopContextData.class)
        .map(LoopContextData::exitLabel);
  }

  // ==================== FOR_RANGE DISPATCH OPERATIONS ====================

  /**
   * Enter FOR_RANGE dispatch case BEFORE processing body for that case.
   * Makes label names unique when same body IR is processed three times.
   *
   * @param dispatchCase Which dispatch case is being generated (EQUAL, ASCENDING, DESCENDING)
   */
  public void enterDispatchCase(final DispatchCase dispatchCase) {
    if (dispatchCase == null) {
      throw new IllegalArgumentException("dispatchCase cannot be null");
    }
    if (dispatchCase == DispatchCase.NONE) {
      throw new IllegalArgumentException("Use exitDispatchCase() to clear dispatch case");
    }
    this.currentDispatchCase = dispatchCase;
  }

  /**
   * Exit FOR_RANGE dispatch case AFTER processing body.
   * Must be called in finally block to ensure cleanup.
   */
  public void exitDispatchCase() {
    this.currentDispatchCase = DispatchCase.NONE;
  }

  /**
   * Query: Which FOR_RANGE dispatch case are we in?
   * Used by label generation to make names unique per dispatch case.
   *
   * @return Current dispatch case, or empty if not in FOR_RANGE dispatch
   */
  public Optional<DispatchCase> getCurrentDispatchCase() {
    return currentDispatchCase == DispatchCase.NONE
        ? Optional.empty()
        : Optional.of(currentDispatchCase);
  }

  // ==================== GENERIC OPERATIONS ====================

  /**
   * Query: Are we inside a specific frame type.
   *
   * @param frameType Frame type to check for
   * @return true if any enclosing frame matches type
   */
  public boolean isInside(final BytecodeFrameType frameType) {
    return findFrame(frameType).isPresent();
  }

  /**
   * Find nearest enclosing frame of specific type.
   *
   * @param frameType Frame type to search for
   * @return Nearest matching frame, or empty if not found
   */
  public Optional<BytecodeStackFrame> findFrame(final BytecodeFrameType frameType) {
    return stack.stream()
        .filter(frame -> frame.frameType() == frameType)
        .findFirst();
  }

  /**
   * Get current scope ID (top of stack).
   *
   * @return Current scope ID, or empty if stack is empty
   */
  public Optional<String> getCurrentScopeId() {
    return stack.isEmpty() ? Optional.empty() : Optional.of(stack.peek().scopeId());
  }

  /**
   * Get all scope IDs in stack order (for debugging).
   *
   * @return List of scope IDs from top to bottom of stack
   */
  public List<String> getAllScopeIds() {
    return stack.stream()
        .map(BytecodeStackFrame::scopeId)
        .toList();
  }

  /**
   * Check if stack is empty (for validation).
   *
   * @return true if no frames on stack
   */
  public boolean isEmpty() {
    return stack.isEmpty();
  }

  // ==================== INTERNAL HELPERS ====================

  /**
   * Find context data of specific type from nearest matching frame.
   */
  private <T> Optional<T> findContextData(final BytecodeFrameType frameType,
                                          final Class<T> dataType) {
    return findFrame(frameType)
        .map(frame -> frame.getContextData(dataType))
        .filter(Objects::nonNull);
  }

  /**
   * Validate top frame matches expected type, then pop.
   * Used by exit methods to ensure stack discipline.
   */
  private void validateAndPop(final BytecodeFrameType expectedType, final String contextName) {
    if (stack.isEmpty()) {
      throw new IllegalStateException(
          String.format("Cannot exit %s - stack is empty", contextName));
    }

    final var top = stack.peek();
    if (top.frameType() != expectedType) {
      throw new IllegalStateException(
          String.format("Cannot exit %s - expected %s but found %s",
              contextName, expectedType, top.frameType()));
    }

    stack.pop();
  }

}
