package org.ek9lang.compiler.phase5;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;
import org.ek9lang.core.AssertValue;

/**
 * Designed to give a broad indication of complexity, this is not true pure Cyclometric Complexity.
 * But it works on a region or code structure approach, looking at anything that may contribute to code
 * being harder to read or taking more thought than just a simple sequence of statements.
 * It takes into account boolean expressions, exception blocks and case statement for example.
 */
class ComplexityCounter {

  /**
   * Used to hold the value of the current area that complexity is being calculated for.
   * So for example an aggregate/class/record/component is entered a new zero value is pushed on.
   * A new zero value is pushed when methods are encountered. As further aspects of code are encountered
   * by the listener the current stack value increased. As the end of the 'method' the value is extracted
   * from the stack and 'squirrelled' away against that method/function. It is also added to the item on the stack
   * above (if there is one). In this way a class (for example) that defines methods that define 'dynamic classes'
   * and even more methods will have a high complexity value. Because that code block really is more complex.
   */
  private final Deque<AtomicInteger> complexityCounterStack = new ArrayDeque<>();

  /**
   * Push a zero value on to this stack.
   */
  public AtomicInteger push() {

    var value = new AtomicInteger();
    complexityCounterStack.push(value);

    return value;
  }

  /**
   * Push a zero value on to this stack.
   */
  public AtomicInteger push(final int initialValue) {

    var value = new AtomicInteger(initialValue);
    complexityCounterStack.push(value);

    return value;
  }

  /**
   * Take a look at the top of the stack.
   */
  public AtomicInteger peek() {

    AssertValue.checkTrue("ComplexityCounter cannot be empty for peek", !complexityCounterStack.isEmpty());

    return complexityCounterStack.peekFirst();
  }

  /**
   * Pop a value of the stack (exception is empty.
   */
  public AtomicInteger pop() {

    AssertValue.checkTrue("ComplexityCounter cannot be empty for pop", !complexityCounterStack.isEmpty());
    return complexityCounterStack.pop();
  }

  public void incrementComplexity() {
    AssertValue.checkTrue("Complexity Stack is empty", !isEmpty());
    peek().getAndAdd(1);
  }

  public void incrementComplexity(final AtomicInteger byValue) {
    incrementComplexity(byValue.intValue());
  }

  public void incrementComplexity(final int byValue) {
    AssertValue.checkTrue("Complexity Stack is empty", !isEmpty());
    peek().getAndAdd(byValue);
  }

  /**
   * True if stack has no contents.
   */
  public boolean isEmpty() {

    return complexityCounterStack.isEmpty();
  }
}
