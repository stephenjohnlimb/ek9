package org.ek9lang.compiler.backend.llvm;

import java.util.function.UnaryOperator;

/**
 * Takes a fully qualified ek9 name and produces a name that is suitable for llvm use.
 * This involves converting something like "some.module.name::ConstructItem" into
 * something that can be used for a filename with the llvm like "com_module_name_ConstructItem.ll".
 */
final class FullyQualifiedFileName implements UnaryOperator<String> {
  private final FullyQualifiedLlvmName fullyQualifiedLlvmName = new FullyQualifiedLlvmName();

  @Override
  public String apply(final String ek9FullyQualifiedName) {
    return fullyQualifiedLlvmName.apply(ek9FullyQualifiedName) + ".ll";
  }
}
