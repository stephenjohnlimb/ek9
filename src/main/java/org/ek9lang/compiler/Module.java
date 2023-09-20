package org.ek9lang.compiler;

import java.io.Serializable;

/**
 * Defines a module scope, the main concept for namespaces in EK9.
 */
public interface Module extends Serializable {
  Source getSource();

  String getScopeName();

  default boolean isEk9Core() {
    return false;
  }
}
