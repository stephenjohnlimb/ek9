package org.company.dept;

import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Trait;

/**
 * Example of a Trait implemented in Java but exposed as EK9.
 */
@Ek9Trait
public interface CostAssessment {
  @Ek9Method("""
      lowCost() as pure
        <- rtn as Boolean?""")
  default Boolean lowCost() {
    return true;
  }
}
