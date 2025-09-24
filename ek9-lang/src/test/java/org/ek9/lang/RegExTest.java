package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Test RegEx functionality including EK9 semantics for unset values and operator behavior.
 */
class RegExTest extends Common {

  @Test
  void testConstructors() {
    // Default constructor creates unset RegEx
    final var unsetRegex = new RegEx();
    assertUnset.accept(unsetRegex);

    // String constructor with valid pattern
    final var validRegex = new RegEx(String._of("\\d+"));
    assertSet.accept(validRegex);
    assertEquals("\\d+", validRegex._string().state);

    // String constructor with invalid pattern
    final var invalidRegex = new RegEx(String._of("[invalid"));
    assertUnset.accept(invalidRegex);

    // Copy constructor with set value
    final var copiedRegex = new RegEx(validRegex);
    assertSet.accept(copiedRegex);
    assertEquals("\\d+", copiedRegex._string().state);

    // JSON operations
    final var validRegexJson = validRegex._json();
    assertSet.accept(validRegexJson);

    assertUnset.accept(unsetRegex._json());

    // Copy constructor with unset value
    final var copiedUnset = new RegEx(unsetRegex);
    assertUnset.accept(copiedUnset);

    // String constructor with unset String
    final var fromUnsetString = new RegEx(new String());
    assertUnset.accept(fromUnsetString);
  }

  @Test
  void testSplitMethod() {
    final var regex = new RegEx(String._of("\\s+"));
    final var input = String._of("hello world test");

    // Valid split operation
    final var result = regex.split(input);
    assertSet.accept(result);
    assertEquals(3, result._len().state);

    // Split with unset regex
    final var unsetRegex = new RegEx();
    final var unsetResult = unsetRegex.split(input);
    assertSet.accept(unsetResult);
    assertTrue.accept(unsetResult._empty());

    // Split with unset input
    final var unsetInput = new String();
    final var unsetInputResult = regex.split(unsetInput);
    assertSet.accept(unsetInputResult);
    assertTrue.accept(unsetInputResult._empty());
  }

  @Test
  void testEqualityOperators() {
    final var regex1 = new RegEx(String._of("\\d+"));
    assertNotNull(regex1);
    final var regex2 = new RegEx(String._of("\\d+"));
    final var regex3 = new RegEx(String._of("\\w+"));
    final var unsetRegex = new RegEx();

    // Equality tests
    assertTrue.accept(regex1._eq(regex2));
    assertFalse.accept(regex1._eq(regex3));
    assertUnset.accept(unsetRegex._eq(new RegEx()));
    assertUnset.accept(regex1._eq(unsetRegex));

    // Inequality tests
    assertFalse.accept(regex1._neq(regex2));
    assertTrue.accept(regex1._neq(regex3));
    assertUnset.accept(unsetRegex._neq(new RegEx()));
    assertUnset.accept(regex1._neq(unsetRegex));

    // Test with unset operands
    assertUnset.accept(regex1._eq(null));
    assertUnset.accept(unsetRegex._eq(null));
  }

  @Test
  void testComparisonOperators() {
    final var regexA = new RegEx(STR_ABC);
    assertNotNull(regexA);
    final var regexB = new RegEx(String._of("def"));
    final var regexC = new RegEx(STR_ABC);
    final var unsetRegex = new RegEx();

    // Comparison tests
    assertTrue.accept(regexA._cmp(regexB)._lt(Integer._of(0)));
    assertTrue.accept(regexB._cmp(regexA)._gt(Integer._of(0)));
    assertTrue.accept(regexA._cmp(regexC)._eq(Integer._of(0)));

    // Comparison with unset values
    assertUnset.accept(unsetRegex._cmp(new RegEx()));
    assertUnset.accept(regexA._cmp(unsetRegex));
    assertUnset.accept(unsetRegex._cmp(regexA));
    assertUnset.accept(regexA._cmp(null));

    // Fuzzy compare should be same as regular compare for RegEx
    assertTrue.accept(regexA._fuzzyCompare(regexB)._eq(regexA._cmp(regexB)));
  }

  @Test
  void testIsSetOperator() {
    final var setRegex = new RegEx(String._of("\\d+"));
    assertNotNull(setRegex);
    final var unsetRegex = new RegEx();

    assertTrue.accept(setRegex._isSet());
    assertFalse.accept(unsetRegex._isSet());
  }

  @Test
  void testAdditionOperators() {
    final var regex1 = new RegEx(STR_ABC);
    final var regex2 = new RegEx(String._of("def"));
    final var unsetRegex = new RegEx();

    // RegEx + RegEx
    final var addedRegex = regex1._add(regex2);
    assertSet.accept(addedRegex);
    assertEquals("abcdef", addedRegex._string().state);

    // RegEx + String (should be quoted)
    final var addedString = regex1._add(String._of("def"));
    assertSet.accept(addedString);
    assertEquals("abc\\Qdef\\E", addedString._string().state);

    // RegEx + Character (should be quoted)
    final var addedChar = regex1._add(Character._of('x'));
    assertSet.accept(addedChar);
    assertEquals("abc\\Qx\\E", addedChar._string().state);

    // Addition with unset operands
    assertUnset.accept(regex1._add(unsetRegex));
    assertUnset.accept(unsetRegex._add(regex1));
    assertUnset.accept(regex1._add(new String()));
    assertUnset.accept(regex1._add(new Character()));
  }

  @Test
  void testConversionOperators() {
    final var regex = new RegEx(String._of("\\d+"));
    final var unsetRegex = new RegEx();

    // Promotion operator
    assertSet.accept(regex._promote());
    assertEquals("\\d+", regex._promote().state);
    assertUnset.accept(unsetRegex._promote());

    // String operator
    assertSet.accept(regex._string());
    assertEquals("\\d+", regex._string().state);
    assertUnset.accept(unsetRegex._string());

    // Hashcode operator
    assertSet.accept(regex._hashcode());
    assertUnset.accept(unsetRegex._hashcode());
  }

  @Test
  void testUtilityOperators() {
    final var emptyRegex = new RegEx(String._of(""));
    assertNotNull(emptyRegex);
    final var nonEmptyRegex = new RegEx(String._of("\\d+"));
    final var unsetRegex = new RegEx();

    // Empty operator
    assertTrue.accept(emptyRegex._empty());
    assertFalse.accept(nonEmptyRegex._empty());
    assertUnset.accept(unsetRegex._empty());

    // Length operator
    assertTrue.accept(emptyRegex._length()._eq(Integer._of(0)));
    assertTrue.accept(nonEmptyRegex._length()._eq(Integer._of(3)));
    assertUnset.accept(unsetRegex._length());
  }

  @Test
  void testMatchesOperators() {
    final var digitRegex = new RegEx(String._of("\\d+"));
    assertNotNull(digitRegex);
    final var unsetRegex = new RegEx();

    // Matches with String
    assertTrue.accept(digitRegex._matches(String._of("123")));
    assertFalse.accept(digitRegex._matches(STR_ABC));
    assertUnset.accept(digitRegex._matches(new String()));
    assertUnset.accept(unsetRegex._matches(String._of("123")));

    // Matches with Path (starts with $?)
    // Note: Path constructor may reject certain formats, making paths unset
    final var pathRegex = new RegEx(String._of("\\$\\?.*"));
    final var validPath = new Path(String._of("$?.name"));
    final var anotherPath = new Path(String._of("$?.config"));

    // Test with valid paths if they are set, otherwise expect unset
    if (validPath._isSet().state) {
      assertTrue.accept(pathRegex._matches(validPath));
    } else {
      assertUnset.accept(pathRegex._matches(validPath));
    }

    if (anotherPath._isSet().state) {
      assertTrue.accept(pathRegex._matches(anotherPath));
    } else {
      assertUnset.accept(pathRegex._matches(anotherPath));
    }

    assertUnset.accept(pathRegex._matches(new Path()));
    assertUnset.accept(unsetRegex._matches(validPath));

    // Matches with FileSystemPath
    final var filePathRegex = new RegEx(String._of(".*\\.txt"));
    final var txtPath = new FileSystemPath(String._of("/test/file.txt"));
    final var binPath = new FileSystemPath(String._of("/test/file.bin"));

    assertTrue.accept(filePathRegex._matches(txtPath));
    assertFalse.accept(filePathRegex._matches(binPath));
    assertUnset.accept(filePathRegex._matches(new FileSystemPath()));
    assertUnset.accept(unsetRegex._matches(txtPath));

    // Matches with Locale
    final var localeRegex = new RegEx(String._of("en.*"));
    final var enLocale = new Locale(String._of("en-US"));
    final var frLocale = new Locale(String._of("fr-FR"));

    assertTrue.accept(localeRegex._matches(enLocale));
    assertFalse.accept(localeRegex._matches(frLocale));
    assertUnset.accept(localeRegex._matches(new Locale()));
    assertUnset.accept(unsetRegex._matches(enLocale));
  }

  @Test
  void testMutatingOperators() {
    // Test merge operator (:~:)
    final var regex1 = new RegEx(STR_ABC);
    final var regex2 = new RegEx(String._of("def"));
    regex1._merge(regex2);
    assertSet.accept(regex1);
    assertEquals("(abc)|(def)", regex1._string().state);

    // Test merge with unset
    final var unsetRegex = new RegEx();
    final var validRegex = new RegEx(String._of("test"));
    unsetRegex._merge(validRegex);
    assertSet.accept(unsetRegex);
    assertEquals("test", unsetRegex._string().state);

    // Test merge unset into set corrupts
    validRegex._merge(new RegEx());
    assertUnset.accept(validRegex);

    // Test replace operator (:^:)
    final var replaceRegex = new RegEx(String._of("original"));
    final var newRegex = new RegEx(String._of("replacement"));
    replaceRegex._replace(newRegex);
    assertSet.accept(replaceRegex);
    assertEquals("replacement", replaceRegex._string().state);

    // Test copy operator (:=:)
    final var copyRegex = new RegEx(String._of("original"));
    final var sourceRegex = new RegEx(String._of("copied"));
    copyRegex._copy(sourceRegex);
    assertSet.accept(copyRegex);
    assertEquals("copied", copyRegex._string().state);
  }

  @Test
  void testOrOperator() {
    final var regex1 = new RegEx(STR_ABC);
    final var regex2 = new RegEx(String._of("def"));

    regex1._or(regex2);
    assertSet.accept(regex1);
    assertEquals("abc|def", regex1._string().state);

    // Test OR with unset
    final var unsetRegex = new RegEx();
    final var validRegex = new RegEx(String._of("test"));
    unsetRegex._or(validRegex);
    assertSet.accept(unsetRegex);
    assertEquals("test", unsetRegex._string().state);

    // Test OR unset into set corrupts
    validRegex._or(new RegEx());
    assertUnset.accept(validRegex);
  }

  @Test
  void testAddAssignmentOperators() {
    // Test += with RegEx
    final var regex1 = new RegEx(STR_ABC);
    final var regex2 = new RegEx(String._of("def"));
    regex1._addAss(regex2);
    assertSet.accept(regex1);
    assertEquals("abcdef", regex1._string().state);

    // Test += with String
    final var regex3 = new RegEx(String._of("prefix"));
    regex3._addAss(String._of("suffix"));
    assertSet.accept(regex3);
    assertEquals("prefix\\Qsuffix\\E", regex3._string().state);

    // Test += with Character
    final var regex4 = new RegEx(String._of("base"));
    regex4._addAss(Character._of('x'));
    assertSet.accept(regex4);
    assertEquals("base\\Qx\\E", regex4._string().state);

    // Test += with unset operands
    final var validRegex = new RegEx(String._of("test"));
    validRegex._addAss(new RegEx());
    assertUnset.accept(validRegex);

    final var validRegex2 = new RegEx(String._of("test"));
    validRegex2._addAss(new String());
    assertUnset.accept(validRegex2);

    final var validRegex3 = new RegEx(String._of("test"));
    validRegex3._addAss(new Character());
    assertUnset.accept(validRegex3);

    // Test += on unset regex
    final var unsetRegex = new RegEx();
    unsetRegex._addAss(String._of("test"));
    assertUnset.accept(unsetRegex);
  }

  @Test
  void testFactoryMethods() {
    // Test _of factory method - note this need prefix and suffix '/'
    //as it is the main entry in from ek9 source.
    final var regexFromFactory = RegEx._of("/\\w+/");
    assertSet.accept(regexFromFactory);
    assertEquals("\\w+", regexFromFactory._string().state);

    // Test _new factory method
    final var newRegex = new RegEx();
    assertUnset.accept(newRegex);
  }

  @Test
  void testComplexPatterns() {
    // Test complex regex patterns
    final var emailRegex = new RegEx(String._of("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"));
    assertNotNull(emailRegex);
    assertSet.accept(emailRegex);

    assertTrue.accept(emailRegex._matches(String._of("test@example.com")));
    assertFalse.accept(emailRegex._matches(String._of("invalid-email")));

    // Test pattern with groups
    final var groupRegex = new RegEx(String._of("(\\d{4})-(\\d{2})-(\\d{2})"));
    assertSet.accept(groupRegex);

    assertTrue.accept(groupRegex._matches(String._of("2023-12-25")));
    assertFalse.accept(groupRegex._matches(String._of("invalid-date")));
  }

  @Test
  void testInvalidPatternHandling() {
    // Test various invalid regex patterns
    final var invalidPatterns = new java.lang.String[] {
        "[unclosed",
        "(?invalid-group)",
        "*invalid-quantifier",
        "(?<incomplete-named-group"
    };
    assertNotNull(invalidPatterns);

    for (final var pattern : invalidPatterns) {
      final var regex = new RegEx(String._of(pattern));
      assertNotNull(regex);
      assertUnset.accept(regex);
    }
  }

  @Test
  void testEdgeCases() {
    // Test empty pattern
    final var emptyPattern = new RegEx(String._of(""));
    assertNotNull(emptyPattern);
    assertSet.accept(emptyPattern);
    assertTrue.accept(emptyPattern._matches(String._of("")));
    assertFalse.accept(emptyPattern._matches(String._of("non-empty")));

    // Test single character patterns
    final var dotPattern = new RegEx(String._of("."));
    assertSet.accept(dotPattern);
    assertTrue.accept(dotPattern._matches(String._of("a")));
    assertTrue.accept(dotPattern._matches(String._of("1")));
    assertFalse.accept(dotPattern._matches(String._of("")));
    assertFalse.accept(dotPattern._matches(String._of("ab")));

    // Test whitespace patterns
    final var whitespacePattern = new RegEx(String._of("\\s+"));
    assertSet.accept(whitespacePattern);
    assertTrue.accept(whitespacePattern._matches(String._of(" ")));
    assertTrue.accept(whitespacePattern._matches(String._of("\t\n")));
    assertFalse.accept(whitespacePattern._matches(STR_ABC));
  }

  @Test
  void testAssignMethod() {
    final var regex1 = new RegEx();
    final var regex2 = new RegEx(String._of("test"));

    // Test assignment from set regex
    regex1.assign(regex2);
    assertSet.accept(regex1);
    assertEquals("test", regex1._string().state);

    // Test assignment from unset regex
    regex1.assign(new RegEx());
    assertUnset.accept(regex1);

    // Test assignment from different type
    // Test assignment from different type would need cast, so test with null
    regex1.assign(null);
    assertUnset.accept(regex1);
  }
}