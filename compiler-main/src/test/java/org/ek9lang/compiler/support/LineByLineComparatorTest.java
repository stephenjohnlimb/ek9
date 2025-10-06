package org.ek9lang.compiler.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for LineByLineComparator utility.
 */
class LineByLineComparatorTest {

  private final LineByLineComparator comparator = new LineByLineComparator();

  @Test
  void testIdenticalStrings() {
    final String text = "line1\nline2\nline3";
    assertTrue(comparator.test(text, text));
  }

  @Test
  void testIdenticalStringsWithWhitespace() {
    final String expected = "line1  \n  line2\nline3  ";
    final String actual = "  line1\nline2  \n  line3";
    assertTrue(comparator.test(expected, actual), "Should match after trimming whitespace");
  }

  @Test
  void testDifferentLineCount() {
    final String expected = "line1\nline2";
    final String actual = "line1\nline2\nline3";
    assertFalse(comparator.test(expected, actual), "Should fail when line counts differ");
  }

  @Test
  void testDifferentContent() {
    final String expected = "line1\nline2\nline3";
    final String actual = "line1\nDIFFERENT\nline3";
    assertFalse(comparator.test(expected, actual), "Should fail when line content differs");
  }

  @Test
  void testEmptyStrings() {
    assertTrue(comparator.test("", ""), "Empty strings should match");
  }

  @Test
  void testEmptyVsNonEmpty() {
    assertFalse(comparator.test("", "content"), "Empty vs non-empty should not match");
    assertFalse(comparator.test("content", ""), "Non-empty vs empty should not match");
  }

  @Test
  void testNullStrings() {
    assertFalse(comparator.test(null, null), "Both null should match");
    assertFalse(comparator.test(null, "content"), "Null vs non-null should not match");
    assertFalse(comparator.test("content", null), "Non-null vs null should not match");
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
  }

  @Test
  void testMixedLineEndings() {
    final String mixed = "line1\r\nline2\nline3\r\n";
    final String unix = "line1\nline2\nline3";
    assertTrue(comparator.test(mixed, unix), "Should handle mixed line endings");
  }

  @Test
  void testDifferenceAtFirstLine() {
    final String expected = "DIFFERENT\nline2\nline3";
    final String actual = "line1\nline2\nline3";
    assertFalse(comparator.test(expected, actual), "Should detect difference at first line");
  }

  @Test
  void testDifferenceAtLastLine() {
    final String expected = "line1\nline2\nline3";
    final String actual = "line1\nline2\nDIFFERENT";
    assertFalse(comparator.test(expected, actual), "Should detect difference at last line");
  }
}
