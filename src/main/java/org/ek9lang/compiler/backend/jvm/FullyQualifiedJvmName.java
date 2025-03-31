package org.ek9lang.compiler.backend.jvm;

import java.io.File;
import java.util.function.UnaryOperator;


/**
 * Takes a fully qualified ek9 name and produces a name that is suitable for jvm internal use.
 * This involves converting something like "some.module.name::ConstructItem" into
 * something that can be used for a filename with the jvm like "com/module/name/ConstructItem".
 */
final class FullyQualifiedJvmName implements UnaryOperator<String> {
  @Override
  public String apply(final String ek9FullyQualifiedName) {
    return ek9FullyQualifiedName.replace(".", File.separator).replace("::", File.separator);
  }
}
