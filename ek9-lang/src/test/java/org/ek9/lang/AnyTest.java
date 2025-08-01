package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    final var asJSON = any._json();
    assertUnset.accept(asJSON);
  }

  @Test
  void testAnyComparisons() {
    final var a1 = Any._new();
    final var a2 = Any._new();

    assertEquals(0, a1._cmp(a1).state);
    assertUnset.accept(a1._cmp(null));
    assertUnset.accept(a1._cmp(a2));
    assertUnset.accept(a2._cmp(a1));
    assertUnset.accept(a1._eq(a2));

  }
}
