package org.ek9lang.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * See FileHandling for a full context use of ZipBinaryContent
 */
final class ZipBinaryContentTest {

  @Test
  @SuppressWarnings({"java:S5845", "java:S5863", "java:S3415"})
  void testEmptyBinaryContent() {

    ZipBinaryContent underTest = new ZipBinaryContent("entry1", new byte[0]);
    ZipBinaryContent compareTo = new ZipBinaryContent("entry1", new byte[0]);

    assertNotEquals(0, underTest.hashCode());
    assertEquals(underTest, underTest);
    assertEquals(underTest, compareTo);
    assertNotEquals(underTest, "String");

    assertNotNull(underTest.toString());
  }
}
