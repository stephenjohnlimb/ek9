package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class AnyTest extends Common {

  @Test
  void testAnyInstanceUse() {
    final Any any = Any._new();
    assertNotNull(any);

    //Check if is Set
    final var isSet = any._isSet();
    assertSet.accept(isSet);
    //The boolean should be set but with a state of false.
    assertFalse(isSet.state);

    final var asString = any._string();
    assertUnset.accept(asString);

  }
}
