package org.ek9lang.compiler.files;

/**
 * Defines a module scope, the main concept for namespaces in EK9.
 */
public interface Module {
  Source getSource();

  String getScopeName();
}
