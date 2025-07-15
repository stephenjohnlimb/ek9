package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ClockTest extends Common {

  @Test
  void testAllMethodsResultInUnset() {
    final var theDefault = new Clock() {
    };
    assertNotNull(theDefault);

    //The clock itself is set and usable - but by default its values are not.
    assertTrue.accept(theDefault._isSet());

    assertUnset.accept(theDefault.nanos());
    assertUnset.accept(theDefault.millisecond());
    assertUnset.accept(theDefault.time());
    assertUnset.accept(theDefault.date());
    assertUnset.accept(theDefault.dateTime());
  }

  @Test
  void testAsPartMock() {

    final var clockMock = new Clock() {
      @Override
      public Millisecond millisecond() {
        return Millisecond._of(200);
      }
    };

    assertSet.accept(clockMock.millisecond());
    assertEquals(Millisecond._of(200), clockMock.millisecond());
  }

}
