package org.ek9lang.compiler.phase5;

import java.util.List;

/**
 * Focus on using variables before they have been initialised.
 * EK9 focuses on always initialising variables to give them memory and a location to
 * hold data. So in the main this is fine, but there are times in Generics or when using
 * abstract base types/traits when it is not possible.
 * Hence, the syntax: someVariable as Integer? (or any type).
 * While this syntax use has been discouraged - it is a 'necessary evil'.
 * So to cater for this, it is essential that the compiler does everything it can to ensure
 * that variables that start out un initialised, are always initialised before they are used.
 * This is non-trivial, and quite expensive to do in processing terms.
 */
class UsedBeforeInitialisedTest extends BadPreIRChecksTest {

  public UsedBeforeInitialisedTest() {
    super("/examples/parseButFailCompile/usedBeforeInitialised",
        List.of("badifelse.functions", "badclass.method.initialisations",
            "badclass.property.initialisations",
            "bad.operators.return", "bad.serviceoperation.return",
            "bad.overriding.classmethods6", "bad.guardassignment.uninitialised.use",
            "bad.uninitialised.functionparts",
            "bad.overriding.componentmethods2", "badswitch.initialisations",
            "badtry.initialisations", "badwhile.initialisations", "badfor.initialisations",
            "simple.conditional.assignment", "uninitialized.properties"));
  }
}
