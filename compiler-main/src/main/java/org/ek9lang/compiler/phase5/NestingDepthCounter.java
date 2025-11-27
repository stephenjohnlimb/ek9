package org.ek9lang.compiler.phase5;

import java.util.ArrayDeque;
import java.util.Deque;
import org.ek9lang.core.AssertValue;

/**
 * Tracks nesting depth within functions/methods/operators.
 * Increments on entering control structures (if, while, for, switch, try, etc.),
 * decrements on exit. Records maximum depth reached per function scope.
 * This is used to enforce nesting depth limits, preventing deeply nested code
 * that becomes hard to read and maintain.
 */
class NestingDepthCounter {

  /**
   * Stack of nesting scopes, one per function/method/operator.
   * Each scope tracks current depth and maximum depth reached.
   */
  private final Deque<NestingScope> scopeStack = new ArrayDeque<>();

  /**
   * Represents a single function/method scope for tracking nesting.
   */
  private static class NestingScope {
    private int currentDepth = 0;
    private int maxDepth = 0;
  }

  /**
   * Push new scope when entering function/method/operator.
   */
  void pushScope() {
    scopeStack.push(new NestingScope());
  }

  /**
   * Pop scope and return max depth reached.
   * Called when exiting function/method/operator.
   *
   * @return the maximum nesting depth reached in this scope
   */
  int popScope() {
    AssertValue.checkTrue("Nesting scope stack is empty for pop", !scopeStack.isEmpty());
    return scopeStack.pop().maxDepth;
  }

  /**
   * Increment depth when entering a control structure.
   * Updates max depth if this is the deepest we've been.
   */
  void enterNesting() {
    AssertValue.checkTrue("Nesting scope stack is empty for enter", !scopeStack.isEmpty());
    final var scope = scopeStack.peek();
    scope.currentDepth++;
    if (scope.currentDepth > scope.maxDepth) {
      scope.maxDepth = scope.currentDepth;
    }
  }

  /**
   * Decrement depth when exiting a control structure.
   */
  void exitNesting() {
    AssertValue.checkTrue("Nesting scope stack is empty for exit", !scopeStack.isEmpty());
    final var scope = scopeStack.peek();
    AssertValue.checkTrue("Nesting depth already zero", scope.currentDepth > 0);
    scope.currentDepth--;
  }

  /**
   * Check if there are any scopes on the stack.
   *
   * @return true if stack has no contents
   */
  boolean isEmpty() {
    return scopeStack.isEmpty();
  }
}
