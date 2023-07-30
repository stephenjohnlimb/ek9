package org.ek9lang.compiler;

/**
 * Defines a module scope, the main concept for namespaces in EK9.
 */
public interface Module {
  Source getSource();

  String getScopeName();

  default boolean isEk9Core() {
    return false;
  }
}
