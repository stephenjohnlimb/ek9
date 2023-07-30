package org.ek9lang.core.exception;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class AssertValueTest {

  @Test
  void testNullRange() {
    assertThrows(IllegalArgumentException.class, () -> AssertValue.checkRange("Outside range", null, 1, 10));
  }

  @Test
  void testLowerRange() {
    assertThrows(IllegalArgumentException.class, () -> AssertValue.checkRange("Outside range", -1, 1, null));
  }

  @Test
  void testUpperRange() {
    assertThrows(IllegalArgumentException.class, () -> AssertValue.checkRange("Outside range", 11, null, 10));
  }

  @Test
  void testRange() {
    AssertValue.checkRange("Outside range", 1, 0, 10);
    //No exception
  }

  @Test
  void testOptionNotEmpty() {
    var empty = Optional.empty();
    assertThrows(IllegalArgumentException.class, () -> AssertValue.checkNotEmpty("Should not be empty", empty));
  }

  @Test
  void testOption() {
    AssertValue.checkNotEmpty("Should not be empty", Optional.of(5));
  }

  @Test
  void testCollectionEmpty() {
    AssertValue.checkNotEmpty("Should not be empty",
        Arrays.asList("Buenos Aires", "Córdoba", "La Plata"));
  }

  @Test
  void testCollectionNotEmpty() {
    final var toCheck = new ArrayList<String>();
    assertThrows(IllegalArgumentException.class,
        () -> AssertValue.checkNotEmpty("Should not be empty", toCheck));
  }

  @Test
  void testNullDate() {
    assertThrows(IllegalArgumentException.class, () -> {
      Date toCheck = null;
      AssertValue.checkNotNull("Some Message", toCheck);
    });
  }

  @Test
  void testNoneDate() {
    Date toCheck = new Date();
    AssertValue.checkNotNull("Some Message", toCheck);
  }

  @Test
  void testCheckTrue() {
    assertThrows(IllegalArgumentException.class, () -> AssertValue.checkTrue("Cannot be false", false));
  }

  @Test
  void testCheckFalse() {
    assertThrows(IllegalArgumentException.class, () -> AssertValue.checkFalse("Cannot be false", true));
  }

  @Test
  void testTrue() {
    AssertValue.checkTrue("Cannot be false", true);
  }

  @Test
  void testNullString() {
    assertThrows(IllegalArgumentException.class, () -> {
      String toCheck = null;
      AssertValue.checkNotEmpty("Some Message", toCheck);
    });
  }

  @Test
  void testEmptyString() {
    assertThrows(IllegalArgumentException.class, () -> {
      String toCheck = "";
      AssertValue.checkNotEmpty("Some Message", toCheck);
    });
  }

  @Test
  void testNoneEmptyString() {
    String toCheck = "Some Text";
    AssertValue.checkNotEmpty("Some Message", toCheck);
  }

  @Test
  void testNullStrings() {
    assertThrows(IllegalArgumentException.class, () -> {
      String[] toCheck = null;
      AssertValue.checkNotEmpty("Some Message", toCheck);
    });
  }

  @Test
  void testEmptyStrings() {
    assertThrows(IllegalArgumentException.class, () -> {
      String[] toCheck = new String[0];
      AssertValue.checkNotEmpty("Some Message", toCheck);
    });
  }

  @Test
  void testPartEmptyStrings() {
    assertThrows(IllegalArgumentException.class, () -> {
      String[] toCheck = {"Some Text", null};
      AssertValue.checkNotEmpty("Some Message", toCheck);
    });
  }

  @Test
  void testNoneEmptyStrings() {
    String[] toCheck = {"Some Text", "other text"};
    AssertValue.checkNotEmpty("Some Message", toCheck);
  }

  @Test
  void testNullNotFoundFile() {
    assertThrows(IllegalArgumentException.class,
        () -> AssertValue.checkCanReadFile("File Cannot be found", (File) null));
  }

  @Test
  void testNotFoundFileByName() {
    assertThrows(IllegalArgumentException.class,
        () -> AssertValue.checkCanReadFile("File Cannot be found", "nosuchfile"));
  }

  @Test
  void testNotFoundFile() {
    File file = new File("/nosuchfile");
    assertThrows(IllegalArgumentException.class,
        () -> AssertValue.checkCanReadFile("File Cannot be found", file));
  }

  @Test
  void testInAccessibleReadableDirectory() {
    assertThrows(IllegalArgumentException.class, () -> {
      String dir = "D";
      AssertValue.checkDirectoryReadable("Must be able to read from", dir);
    });
  }

  @Test
  void testInAccessibleWritableDirectory() {
    assertThrows(IllegalArgumentException.class, () -> {
      String dir = "D";
      AssertValue.checkDirectoryWritable("Must be able to read from", dir);
    });
  }

  @Test
  void testInAccessibleFileReadableDirectory() {
    final File dir = new File("D");
    assertThrows(IllegalArgumentException.class,
        () -> AssertValue.checkDirectoryReadable("Must be able to read from", dir));
  }

  @Test
  void testInAccessibleFileWritableDirectory() {
    final File dir = new File("D");
    assertThrows(IllegalArgumentException.class,
        () -> AssertValue.checkDirectoryWritable("Must be able to read from", dir));
  }

  @Test
  void testAccessibleDirectory() {
    String tempDir = System.getProperty("java.io.tmpdir");

    AssertValue.checkDirectoryReadable("Must be able to read from", tempDir);
    AssertValue.checkDirectoryWritable("Must be able to write to", tempDir);

    File tempDirFile = new File(tempDir);
    AssertValue.checkDirectoryReadable("Must be able to read from", tempDirFile);
    AssertValue.checkDirectoryWritable("Must be able to write to", tempDirFile);
  }

  @Test
  void testNonReadableDirectory() {
    assertThrows(IllegalArgumentException.class, () -> {
      String tempDir = " no such directory/";
      AssertValue.checkDirectoryReadable("Must be able to read", tempDir);
    });
  }

  @Test
  void testNonWritableDirectory() {
    assertThrows(IllegalArgumentException.class, () -> {
      String tempDir = " no such directory/";
      AssertValue.checkDirectoryWritable("Must be able to write", tempDir);
    });
  }

  @Test
  void testFoundFile() throws IOException {
    String tempDir = System.getProperty("java.io.tmpdir");

    File newFile = new File(tempDir, "assertTest.txt");
    //Remove it if already there
    if (newFile.exists()) {
      assertTrue(newFile.delete());
    }

    //make the file check can access
    assertTrue(newFile.createNewFile());
    AssertValue.checkCanReadFile("File Cannot be found", newFile.getAbsolutePath());
    if (newFile.exists()) {
      assertTrue(newFile.delete());
    }
  }
}
