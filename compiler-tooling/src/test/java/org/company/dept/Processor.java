package org.company.dept;

import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Trait;

/**
 * Trait implemented in Java with other traits but exposed as EK9.
 */
@Ek9Trait("Processor with trait of Monitorable, CostAssessment")
public interface Processor extends Monitorable, CostAssessment {
  @Ek9Method("""
      process() as pure
        <- rtn as Boolean?""")
  default Boolean process() {
    return true;
  }

  @Override
  @Ek9Method("""
      override lowCost() as pure
        <- rtn as Boolean?""")
  default Boolean lowCost() {
    return true;
  }
}
