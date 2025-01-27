package org.ek9lang.compiler.phase3;

import java.util.List;

/**
 * Tests bad variable usage, like use before definition, not initialised etc.
 * Now also check for duplicated properties fields in record hierarchy.
 */
class BadVariableUsesTest extends BadFullResolutionTest {

  public BadVariableUsesTest() {
    super("/examples/parseButFailCompile/badVariableUses",
        List.of("bad.blockvariable.uses", "bad.duplicateproperties.uses"));
  }

}
