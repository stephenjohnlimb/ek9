package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;

abstract class StdCommon extends Common {

  //The data used for testing.
  private final Boolean unsetBoolean = new Boolean();
  private final String testString = String._of("Steve");
  private final Time endOfTheDay = Time._of("23:59:59");

  void assertPrintln(StringOutput underTest, ByteArrayOutputStream outputStream) {
    //So here is a valid string and will use the println(String) method
    underTest.println(testString);
    assertEquals("Steve\n", outputStream.toString());

    //Here is a value that is unset, and will go via the println(Any) method
    //So there should be no additional output
    underTest.println(unsetBoolean);
    assertEquals("Steve\n", outputStream.toString());

    //Now another test via the println(Any) method but this time a value that is set.
    underTest.println(endOfTheDay);
    assertEquals("Steve\n23:59:59\n", outputStream.toString());

    assertDoesNotThrow(underTest::_close);
  }

  void assertPipe(StringOutput underTest, ByteArrayOutputStream outputStream) {

    //So here is a valid string and will use the _pipe(String) method
    underTest._pipe(testString);
    assertEquals("Steve\n", outputStream.toString());

    //Here is a value that is unset, and will go via the _pipe(Any) method
    //So there should be no additional output
    underTest._pipe(unsetBoolean);
    assertEquals("Steve\n", outputStream.toString());

    //Now another test via the _pipe(Any) method but this time a value that is set.
    underTest._pipe(endOfTheDay);
    assertEquals("Steve\n23:59:59\n", outputStream.toString());
    assertDoesNotThrow(underTest::_close);
  }

  void assertPrint(StringOutput underTest, ByteArrayOutputStream outputStream) {
    //So here is a valid string and will use the print(String) method
    underTest.print(testString);
    assertEquals("Steve", outputStream.toString());

    //Here is a value that is unset, and will go via the print(Any) method
    //So there should be no additional output
    underTest.print(unsetBoolean);
    assertEquals("Steve", outputStream.toString());

    underTest.print(String._of(" - "));
    //Now another test via the print(Any) method but this time a value that is set.
    underTest.print(endOfTheDay);
    assertEquals("Steve - 23:59:59", outputStream.toString());
    assertDoesNotThrow(underTest::_close);
  }
}
