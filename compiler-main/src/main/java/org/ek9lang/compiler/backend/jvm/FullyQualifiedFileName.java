package org.ek9lang.compiler.backend.jvm;

import java.util.function.UnaryOperator;

/**
 * Takes a fully qualified ek9 name and produces a name that is suitable for jvm use.
 * This involves converting something like "some.module.name::ConstructItem" into
 * something that can be used for a filename with the jvm like "com/module/name/ConstructItem.class".
 */
final class FullyQualifiedFileName implements UnaryOperator<String> {
  private final FullyQualifiedJvmName fullyQualifiedJvmName = new FullyQualifiedJvmName();

  @Override
  public String apply(final String ek9FullyQualifiedName) {
    return fullyQualifiedJvmName.apply(ek9FullyQualifiedName) + ".class";
  }
}
