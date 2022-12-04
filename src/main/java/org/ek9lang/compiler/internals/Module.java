package org.ek9lang.compiler.internals;

/**
 * Defines a module scope, the main concept for namespaces in EK9.
 */
public interface Module {
  Source getSource();

  String getScopeName();
}
