package org.ek9lang.compiler.common;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.IScopedSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.core.AssertValue;

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
 * This class also has logic and code to be able to traverse back up the 'structure stack'.
 */
public class ScopeStack {
  private final Deque<IScope> actualStack = new ArrayDeque<>();

  public ScopeStack(final IScope base) {

    push(base);
  }

  public IScope getVeryBaseScope() {

    return actualStack.getFirst();
  }

  /**
   * Push a scope on to this stack.
   */
  public IScope push(final IScope scope) {

    AssertValue.checkNotNull("Scope Cannot be null", scope);
    actualStack.push(scope);

    return scope;
  }

  /**
   * Take a look at the top of the stack.
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

  /**
   * Navigates back up the scope stack to find the first match of the scope type passed in.
   * This is useful because dynamic classes and functions do not use an enclosing scope of the
   * scope they are in. They get pulled out to module level (by design).
   */
  public Optional<IScope> traverseBackUpStack(final IScope.ScopeType scopeType) {

    for (IScope scopeForConsideration : actualStack) {
      if (scopeForConsideration.getScopeType().equals(scopeType)) {
        return Optional.of(scopeForConsideration);
      }
    }

    return Optional.empty();
  }

  /**
   * Locates the outer function or method (if possible, not always possible Applications for example).
   */
  public Optional<IScopedSymbol> traverseBackUpStackToMethodOrFunction() {

    for (IScope scopeForConsideration : actualStack) {
      if (scopeForConsideration instanceof MethodSymbol methodSymbol) {
        return Optional.of(methodSymbol);
      }
      if (scopeForConsideration instanceof FunctionSymbol functionSymbol) {
        return Optional.of(functionSymbol);
      }
    }

    return Optional.empty();
  }

  /**
   * Navigate back up the scope stack to find the first enclosing method (there may not be one).
   * So if this were called in a function or an application for example - there will not be an enclosing method.
   *
   * @return The Optional method located.
   */
  public Optional<MethodSymbol> traverseBackUpStackToEnclosingMethod() {

    for (IScope scopeForConsideration : actualStack) {
      if (scopeForConsideration instanceof MethodSymbol methodSymbol) {
        return Optional.of(methodSymbol);
      }
    }

    return Optional.empty();
  }
}
