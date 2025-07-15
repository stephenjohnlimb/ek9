package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class StringOutputTest extends Common {

  @Test
  void testAllMethodsResultInUnset() {
    final var theDefault = new StringOutput() {
    };
    assertNotNull(theDefault);

    assertUnset.accept(theDefault._isSet());
    assertDoesNotThrow(() -> theDefault.println(new String()));
    assertDoesNotThrow(() -> theDefault.println(new Any() {}));
    assertDoesNotThrow(() -> theDefault.print(new String()));
    assertDoesNotThrow(() -> theDefault.print(new Any() {}));
    assertDoesNotThrow(() -> theDefault._pipe(new String()));
    assertDoesNotThrow(() -> theDefault._pipe(new Any() {}));
  }
}
