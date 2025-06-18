package org.company.dept;

import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Trait;

/**
 * Another example of a Trait implemented in Java but exposed as EK9.
 */
@Ek9Trait
public interface Monitorable {
  @Ek9Method("""
      available() as pure
        <- rtn as Boolean?""")
  default Boolean available() {
    return true;
  }

  @Ek9Method("""
      lowCost() as pure
        <- rtn as Boolean?""")
  default Boolean lowCost() {
    return true;
  }
}
