package org.ek9lang.compiler.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for LineByLineComparator utility.
 */
class LineByLineComparatorTest {

  private ByteArrayOutputStream baos;
  private LineByLineComparator comparator;

  @BeforeEach
  void setupPrintStream() {
    baos = new ByteArrayOutputStream();
    final var ps = new PrintStream(baos, true, StandardCharsets.UTF_8);
    comparator = new LineByLineComparator(ps);
  }

  @Test
  void testIdenticalStrings() {
    final String text = "line1\nline2\nline3";
    assertTrue(comparator.test(text, text));
    assertTrue(baos.toString().isEmpty());
  }

  @Test
  void testIdenticalStringsWithWhitespace() {
    final String expected = "line1  \n  line2\nline3  ";
    final String actual = "  line1\nline2  \n  line3";
    assertTrue(comparator.test(expected, actual), "Should match after trimming whitespace");
    assertTrue(baos.toString().isEmpty());
  }

  @Test
  void testDifferentLineCount() {
    final String expected = "line1\nline2";
    final String actual = "line1\nline2\nline3";
    assertFalse(comparator.test(expected, actual), "Should fail when line counts differ");
    assertFalse(baos.toString().isEmpty());
  }

  @Test
  void testDifferentContent() {
    final String expected = "line1\nline2\nline3";
    final String actual = "line1\nDIFFERENT\nline3";
    assertFalse(comparator.test(expected, actual), "Should fail when line content differs");
    assertFalse(baos.toString().isEmpty());
  }

  @Test
  void testEmptyStrings() {
    assertTrue(comparator.test("", ""), "Empty strings should match");
    assertTrue(baos.toString().isEmpty());
  }

  @Test
  void testEmptyVsNonEmpty() {
    assertFalse(comparator.test("", "content"), "Empty vs non-empty should not match");
    assertFalse(comparator.test("content", ""), "Non-empty vs empty should not match");
    assertFalse(baos.toString().isEmpty());
  }

  @Test
  void testNullStrings() {
    assertFalse(comparator.test(null, null), "Both null should match");
    assertFalse(comparator.test(null, "content"), "Null vs non-null should not match");
    assertFalse(comparator.test("content", null), "Non-null vs null should not match");
    assertFalse(baos.toString().isEmpty());
  }

  @Test
  void testSingleLine() {
    assertTrue(comparator.test("single line", "single line"));
    assertTrue(comparator.test("  single line  ", "single line"), "Should match after trimming");
    assertFalse(comparator.test("line1", "line2"));
  }

  @Test
  void testWindowsLineEndings() {
    final String windows = "line1\r\nline2\r\nline3";
    final String unix = "line1\nline2\nline3";
    assertTrue(comparator.test(windows, unix), "Should handle different line endings");
    assertTrue(baos.toString().isEmpty());
  }

  @Test
  void testMixedLineEndings() {
    final String mixed = "line1\r\nline2\nline3\r\n";
    final String unix = "line1\nline2\nline3";
    assertTrue(comparator.test(mixed, unix), "Should handle mixed line endings");
    assertTrue(baos.toString().isEmpty());
  }

  @Test
  void testDifferenceAtFirstLine() {
    final String expected = "DIFFERENT\nline2\nline3";
    final String actual = "line1\nline2\nline3";
    assertFalse(comparator.test(expected, actual), "Should detect difference at first line");
    assertFalse(baos.toString().isEmpty());
  }

  @Test
  void testDifferenceAtLastLine() {
    final String expected = "line1\nline2\nline3";
    final String actual = "line1\nline2\nDIFFERENT";
    assertFalse(comparator.test(expected, actual), "Should detect difference at last line");
    assertFalse(baos.toString().isEmpty());
  }
}
