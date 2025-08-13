package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Just tests bad examples of overriding methods.
 */
class BadMethodOverridingTest extends BadFullResolutionTest {

  public BadMethodOverridingTest() {
    super("/examples/parseButFailCompile/phase3/badOverridingMethodsAndFunctions",
        List.of("bad.overriding.componentmethods1",
            "bad.overriding.classmethods1",
            "bad.overriding.classmethods2",
            "bad.overriding.classmethods3",
            "bad.overriding.classmethods4",
            "bad.overriding.classmethods5",
            "bad.overriding.traitmethods1",
            "bad.overriding.traitmethods2",
            "bad.traits.covariance.examples",
            "bad.classes.covariance.examples",
            "bad.components.covariance.examples",
            "bad.functions.covariance.examples",
            "bad.overriding.functions"));
  }

}
