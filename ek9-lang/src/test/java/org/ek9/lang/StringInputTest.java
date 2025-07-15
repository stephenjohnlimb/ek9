package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class StringInputTest extends Common {

  @Test
  void testAllMethodsResultInUnset() {
    final var theDefault = new StringInput() {
    };
    assertNotNull(theDefault);

    assertUnset.accept(theDefault._isSet());
    assertUnset.accept(theDefault.hasNext());
    assertUnset.accept(theDefault.next());
    assertDoesNotThrow(theDefault::_close);
  }
}
