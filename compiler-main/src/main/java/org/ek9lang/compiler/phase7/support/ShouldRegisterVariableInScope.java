package org.ek9lang.compiler.phase7.support;

import java.util.function.Predicate;

/**
 * Determine if a variable should be registered in scope for cleanup.
 * While we will be targeting JVM, so memory management is not needed
 * we may also want to target LLVM-C++ where we will need to reference count objects.
 * <p>
 * Variables that should NOT be registered:
 * - Parameters (caller-managed): _param_1, _param_2, etc.
 * - Return variables (transferred to caller): _return_1, _return_2, etc.
 * </p>
 * Variables that SHOULD be registered:
 * - Local variables (function-managed): _scope_1, _scope_2, etc.
 */
public final class ShouldRegisterVariableInScope implements Predicate<String> {

  @Override
  public boolean test(final String scopeId) {
    // Don't register parameters - managed by caller
    if (scopeId.startsWith("_param_")) {
      return false;
    }

    // Don't register return variables - ownership transferred to caller
    if (scopeId.startsWith("_return_")) {
      return false;
    }

    // Register local variables - managed by this function
    return scopeId.startsWith("_scope_");

    // Default to false for unknown scope types
  }
}
