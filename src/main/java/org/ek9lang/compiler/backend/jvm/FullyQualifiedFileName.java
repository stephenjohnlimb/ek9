package org.ek9lang.compiler.backend.jvm;

import java.io.File;
import java.util.function.UnaryOperator;

final class FullyQualifiedFileName implements UnaryOperator<String> {
  @Override
  public String apply(final String ek9FullyQualifiedName) {
    return ek9FullyQualifiedName.replace(".", File.separator).replace("::", File.separator) + ".class";
  }
}
