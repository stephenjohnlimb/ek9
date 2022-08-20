package org.ek9lang.compiler.symbol.support;

import java.io.Serial;
import java.util.Stack;
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
public class ScopeStack extends Stack<IScope> {
  @Serial
  private static final long serialVersionUID = 1L;

  public ScopeStack(IScope base) {
    push(base);
  }

  public IScope getVeryBaseScope() {
    return super.firstElement();
  }

  @Override
  public IScope push(IScope scope) {
    AssertValue.checkNotNull("Scope Cannot be null", scope);
    return super.push(scope);
  }

  @Override
  public synchronized IScope pop() {
    AssertValue.checkTrue("ScopeStack cannot be empty for pop", !empty());
    return super.pop();
  }
}
