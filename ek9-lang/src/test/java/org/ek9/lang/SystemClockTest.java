package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Now test a real SystemClock with an actual implementation.
 */
class SystemClockTest extends Common {

  @Test
  void testAllMethodsResultInUnset() {
    final var theDefault = new SystemClock() {
    };
    assertNotNull(theDefault);

    //The clock itself is set and usable - but now these method will return real live values.
    //always a bit difficult to test anything with a real clock time.
    assertTrue.accept(theDefault._isSet());

    final var nanos = theDefault.nanos();
    assertSet.accept(nanos);
    assertTrue(nanos.state > 0L);

    final var millis = theDefault.millisecond();
    assertSet.accept(millis);
    assertTrue(millis.state > 0L);

    final var time = theDefault.time();
    assertSet.accept(time);

    final var date = theDefault.date();
    assertSet.accept(date);

    final var dateTime = theDefault.dateTime();
    assertSet.accept(dateTime);
  }

}
