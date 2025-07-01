package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class VoidTest extends Common {

  final Boolean falseBoolean = Boolean._of(false);

  @Test
  void generalTest() {
    assertUnset.accept(new Void());

    assertEquals(falseBoolean, new Void()._isSet());
    //A couple of checks to to ensure code is covered in hard to reach circumstances.
    assertFalse(new Void().equals(null));
    assertFalse(BuiltinType.isValid(null));

    assertNotEquals(0, new Void().hashCode());
  }

}
