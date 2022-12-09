package org.ek9lang.compiler.symbol.support;

import java.io.Serial;
import java.util.ArrayDeque;
import java.util.Deque;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.core.exception.AssertValue;

/**
 * This stack object is only designed to be used when first parsing.
 * The idea is to actually associate the scope with the specific contexts by
 * putting them in a ParseTreeProperty of Scope, this tree is held in the
 * ParsedModule. So why do we need this stack? That is because as we
 * listen/visit the parse tree we need to both create the scope and record it in
 * the parsed module. But also put it on to a stack so that when we return for
 * each level of the parse tree we can get back to the right scope. I don't want
 * to use a 'getEnclosingScope' method on each scope as the very top level has
 * to be thread safe. Plus it exposes information that should only be available
 * to the scope itself - not any other bit of code.
 */
public class ScopeStack {
  @Serial
  private static final long serialVersionUID = 1L;

  private final Deque<IScope> actualStack = new ArrayDeque<>();

  public ScopeStack(IScope base) {
    push(base);
  }

  public IScope getVeryBaseScope() {
    return actualStack.getFirst();
  }

  /**
   * Push a scope on to this stack.
   */
  public IScope push(IScope scope) {
    AssertValue.checkNotNull("Scope Cannot be null", scope);
    actualStack.push(scope);

    return scope;
  }

  /**
   * Take a look at the top of the stack.
   *
   * @return
   */
  public IScope peek() {
    return actualStack.peekFirst();
  }

  /**
   * Pop a scope of the stack (exception is empty.
   */
  public IScope pop() {
    AssertValue.checkTrue("ScopeStack cannot be empty for pop", !actualStack.isEmpty());
    return actualStack.pop();
  }

  /**
   * True if stack has no contents.
   */
  public boolean isEmpty() {
    return actualStack.isEmpty();
  }

}
