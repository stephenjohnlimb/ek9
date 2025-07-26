package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class StringTest extends Common {
  

  /**
   * Helper method to test all comparison operators with unsetString values.
   */
  private void assertComparisonOperatorsWithUnset(String validValue) {
    // Test _lt operator with unsetString values
    assertUnset.accept(validValue._lt(unsetString));
    assertUnset.accept(unsetString._lt(validValue));
    assertUnset.accept(unsetString._lt(unsetString));

    // Test _lteq operator with unsetString values
    assertUnset.accept(validValue._lteq(unsetString));
    assertUnset.accept(unsetString._lteq(validValue));
    assertUnset.accept(unsetString._lteq(unsetString));

    // Test _gt operator with unsetString values
    assertUnset.accept(validValue._gt(unsetString));
    assertUnset.accept(unsetString._gt(validValue));
    assertUnset.accept(unsetString._gt(unsetString));

    // Test _gteq operator with unsetString values
    assertUnset.accept(validValue._gteq(unsetString));
    assertUnset.accept(unsetString._gteq(validValue));
    assertUnset.accept(unsetString._gteq(unsetString));

    // Test _eq operator with unsetString values
    assertUnset.accept(validValue._eq(unsetString));
    assertUnset.accept(unsetString._eq(validValue));
    assertUnset.accept(unsetString._eq(unsetString));

    // Test _neq operator with unsetString values
    assertUnset.accept(validValue._neq(unsetString));
    assertUnset.accept(unsetString._neq(validValue));
    assertUnset.accept(unsetString._neq(unsetString));
  }

  /**
   * Helper method to create test strings consistently.
   */
  private String createTestString(java.lang.String value) {
    return String._of(value);
  }

  @Test
  void testConstruction() {

    assertUnset.accept(String._of((java.lang.String) null));
    assertUnset.accept(String._of(new String()));
    assertUnset.accept(String._of(new Integer()));
    assertUnset.accept(String._of(new Float()));

    assertUnset.accept(new String());
    assertUnset.accept(new String(new String()));

    final var validString = new String(String._of("A simple value"));
    assertSet.accept(validString);
    assertEquals("A simple value", validString.state);
  }

  @Test
  void testEmptyIterator() {
    final var underTest = EMPTY_STRING;
    assertNotNull(underTest);

    final var iterator = underTest.iterator();
    assertUnset.accept(iterator);

  }

  @Test
  void testIterator() {
    final var underTest = String._of("Steve");
    assertNotNull(underTest);

    final var iterator = underTest.iterator();
    assertSet.accept(iterator);

    //Now lets just go through one character at a time.
    final var expect = new char[] {'S', 't', 'e', 'v', 'e'};
    for (final char c : expect) {
      assertTrue.accept(iterator.hasNext());
      final var ch = iterator.next();
      assertSet.accept(ch);
      assertEquals(Character._of(c), ch);
    }
    assertFalse.accept(iterator.hasNext());

  }

  @Test
  void testTrimming() {
    // Test unsetString behavior for trimming
    assertUnset.accept(new String().trim());

    //String with some white space at the start and end, to be trimmed.
    final var validString = createTestString("\tA simple value ");
    assertSet.accept(validString);
    assertEquals(createTestString("A simple value"), validString.trim());
  }

  @Test
  void testCaseTransform() {
    // Test unsetString behavior for case transformation
    assertUnset.accept(unsetString.upperCase());
    assertUnset.accept(unsetString.lowerCase());

    //String with some white space at the start and end, to be trimmed.
    final var validString = createTestString("A simple value");

    assertEquals(createTestString("A SIMPLE VALUE"), validString.upperCase());
    assertEquals(createTestString("a simple value"), validString.lowerCase());
  }

  @Test
  void testPadding() {
    final var pad20 = Integer._of(20);
    final var validString = createTestString("A simple value");
    final var tooLongString = createTestString("A value that is longer than the padding of 20");

    // Test unsetString behavior for padding operations
    assertUnset.accept(new String().rightPadded(pad20));
    assertUnset.accept(new String().leftPadded(pad20));
    assertUnset.accept(validString.rightPadded(unsetInteger));
    assertUnset.accept(validString.leftPadded(unsetInteger));

    assertEquals(createTestString("A simple value      "), validString.rightPadded(pad20));
    assertEquals(createTestString("      A simple value"), validString.leftPadded(pad20));

    //Does not truncate, leaves it as ia, as it does not need padding.
    assertEquals(tooLongString, tooLongString.rightPadded(pad20));
    assertEquals(tooLongString, tooLongString.leftPadded(pad20));
  }

  @Test
  void testEquality() {

    final var aSimpleValue = createTestString("A simple value");
    final var aSimpleValueButLonger = createTestString("A simple value ");
    final var bSimpleValue = createTestString("B simple value");

    // Test all unsetString combinations using helper method
    assertComparisonOperatorsWithUnset(aSimpleValue);

    //Now test the actual values

    assertEquals(trueBoolean, aSimpleValue._eq(aSimpleValue));
    assertEquals(trueBoolean, aSimpleValue._lteq(aSimpleValue));
    assertEquals(trueBoolean, aSimpleValue._gteq(aSimpleValue));

    assertEquals(falseBoolean, aSimpleValue._neq(aSimpleValue));
    assertEquals(falseBoolean, aSimpleValue._gt(aSimpleValue));
    assertEquals(falseBoolean, aSimpleValue._lt(aSimpleValue));

    //Now a String that is gt or lt. i.e. lexigraphically before or after.
    assertEquals(trueBoolean, aSimpleValue._lt(bSimpleValue));
    assertEquals(trueBoolean, aSimpleValue._lteq(bSimpleValue));
    assertEquals(trueBoolean, bSimpleValue._gt(aSimpleValue));
    assertEquals(trueBoolean, bSimpleValue._gteq(aSimpleValue));

    assertEquals(trueBoolean, aSimpleValue._lt(aSimpleValueButLonger));

    //Now a simple value might be longer but b simple value comes after it lexicographically.
    assertEquals(trueBoolean, bSimpleValue._gt(aSimpleValueButLonger));
  }

  @Test
  void testIsSet() {
    assertNotNull(unsetString);
    assertFalse.accept(unsetString._isSet());

    final var v1 = String._of("Some valid value");
    assertNotNull(v1);
    assertTrue.accept(v1._isSet());
  }

  @Test
  void testComparison() {

    final var i0 = INT_0;
    final var iMinus1 = Integer._of(-1);
    final var i1 = INT_1;
    final var i2 = INT_2;
    final var i3 = Integer._of(3);
    final var steve = createTestString("Steve");
    final var steven = createTestString("Steven");

    // Test unsetString behavior for comparison methods
    assertUnset.accept(steve._cmp(unsetString));
    assertUnset.accept(unsetString._cmp(steve));
    assertUnset.accept(steve._fuzzy(unsetString));
    assertUnset.accept(unsetString._fuzzy(steve));

    //Normal comparison
    assertEquals(i0, steve._cmp(steve));
    //Steve is lexicographically less than Steven
    assertEquals(iMinus1, steve._cmp(steven));
    assertEquals(i1, steven._cmp(steve));

    //Now with fuzzy match, there is no greater than or less than zero idea.
    //It is based on the weight of how many steps it takes to transform one string into the other.

    //Fuzzy match
    assertEquals(i0, steve._fuzzy(steve));
    assertEquals(i1, steve._fuzzy(String._of("steve"))); //lowercase 's'
    assertEquals(i2, steve._fuzzy(steven));
    assertEquals(i1, steve._fuzzy(String._of("teve"))); //missing 's'
    assertEquals(i3, steve._fuzzy(String._of("steven")));

    //Now check the other way.
    assertEquals(i2, steven._fuzzy(steve));
    assertEquals(i1, String._of("teve")._fuzzy(steve)); //missing 's'
    assertEquals(i3, String._of("steven")._fuzzy(steve));
  }

  @Test
  void testStringConcatenation() {
    final var steve = createTestString("Steve");
    final var limb = createTestString("Limb");
    final var steveLimb = createTestString("Steve Limb");

    // Test unsetString behavior for string concatenation
    assertUnset.accept(unsetString._add(unsetString));
    assertUnset.accept(unsetString._add(steve));
    assertUnset.accept(steve._add(unsetString));

    assertEquals(steveLimb, steve._add(createTestString(" "))._add(limb));

  }

  @Test
  void testAsString() {
    final var steve = createTestString("Steve");

    // Test unsetString behavior
    assertUnset.accept(unsetString._string());
    assertSet.accept(steve._string());
    assertEquals("Steve", steve._string().toString());

  }

  @Test
  void testAsJson() {
    // Test JSON conversion with set values
    final var steve = createTestString("Steve");
    assertNotNull(steve);
    final var steveJson = steve._json();
    assertSet.accept(steveJson);

    final var empty = createTestString("");
    final var emptyJson = empty._json();
    assertSet.accept(emptyJson);

    // Test JSON conversion with unsetString value
    assertUnset.accept(unsetString._json());
  }

  @Test
  void testHashCode() {
    final var steve = createTestString("Steve");
    final var steven = createTestString("Steven");

    // Test unsetString behavior
    assertUnset.accept(unsetString._hashcode());
    assertEquals(steve._hashcode(), steve._hashcode());
    assertNotEquals(steven._hashcode(), steve._hashcode());
  }

  @Test
  void testUtilityOperators() {
    final var steve = createTestString("Steve");
    final var whitespace = createTestString(" \n \t ");

    // Test unsetString behavior for utility operators
    assertUnset.accept(unsetString._empty());
    assertUnset.accept(unsetString._len());
    assertUnset.accept(unsetString._contains(steve));

    assertFalse.accept(steve._empty());
    assertEquals(Integer._of(5), steve._len());

    assertTrue.accept(whitespace._empty());
    assertEquals(Integer._of(5), whitespace._len());

    //Check contains mechanism
    assertEquals(trueBoolean, steve._contains(steve));
    assertEquals(trueBoolean, steve._contains(createTestString("t")));
    assertEquals(trueBoolean, steve._contains(createTestString("teve")));

    assertEquals(falseBoolean, steve._contains(createTestString("teven")));

    //Note thet S is a lowercase s - so does not match.
    assertEquals(falseBoolean, steve._contains(createTestString("steve")));

  }

  @Test
  void testPipeLogic() {
    final var steve = createTestString("Steve");
    final var limb = createTestString("Limb");
    final var steveLimb = createTestString("Steve Limb 21");
    final var twentyOne = Integer._of(21);

    final var mutatedValue = new String();
    assertUnset.accept(mutatedValue);

    mutatedValue._pipe(unsetString);
    assertUnset.accept(mutatedValue);

    //Example of how an unsetString value can become set when adding values
    mutatedValue._pipe(steve);
    mutatedValue._pipe(createTestString(" "));
    mutatedValue._pipe(limb);

    //Note that unlike other operators the pipe ignores unsetString values.
    mutatedValue._pipe(unsetString);

    mutatedValue._pipe(createTestString(" "));
    mutatedValue._pipe(twentyOne);

    assertEquals(steveLimb, mutatedValue._string());

  }

  @Test
  void testReplaceAndCopyLogic() {

    final var steve = createTestString("Steve");
    final var limb = createTestString("Limb");

    var mutatedValue = createTestString("Steve");
    assertEquals(steve, mutatedValue);

    mutatedValue._replace(limb);
    assertEquals(limb, mutatedValue);

    mutatedValue._replace(unsetString);
    assertUnset.accept(mutatedValue);

    //Now just check that it can take a value after being unsetString
    mutatedValue._replace(steve);
    assertEquals(steve, mutatedValue);
  }

  @Test
  void testMutationOperators() {

    final var steveLimb = createTestString("Steve Limb");

    var mutatedValue = createTestString("Steve");
    mutatedValue._addAss(createTestString(" "));
    mutatedValue._addAss(createTestString("Limb"));

    assertEquals(steveLimb, mutatedValue._string());

    //Check that is something unsetString is added then it results in the mutated value being unsetString.
    mutatedValue._addAss(unsetString);
    assertUnset.accept(mutatedValue);

  }

  @Test
  void testFirstAndLastMethods() {
    // Test unsetString behavior for first/last methods
    assertUnset.accept(unsetString.first());
    assertUnset.accept(unsetString.last());

    // Test with empty string
    final var emptyString = createTestString("");
    assertSet.accept(emptyString);
    assertUnset.accept(emptyString.first());
    assertUnset.accept(emptyString.last());

    // Test with single character string
    final var singleChar = createTestString("A");
    assertSet.accept(singleChar);
    final var firstChar = singleChar.first();
    final var lastChar = singleChar.last();
    assertSet.accept(firstChar);
    assertSet.accept(lastChar);
    assertEquals(Character._of('A'), firstChar);
    assertEquals(Character._of('A'), lastChar);
    assertEquals(firstChar, lastChar); // Should be the same character

    // Test with multi-character string
    final var steve = createTestString("Steve");
    assertSet.accept(steve);
    final var steveFirst = steve.first();
    final var steveLast = steve.last();
    assertSet.accept(steveFirst);
    assertSet.accept(steveLast);
    assertEquals(Character._of('S'), steveFirst);
    assertEquals(Character._of('e'), steveLast);
    assertNotEquals(steveFirst, steveLast);

    // Test with special characters and numbers
    final var special = createTestString("@123!");
    final var specialFirst = special.first();
    final var specialLast = special.last();
    assertEquals(Character._of('@'), specialFirst);
    assertEquals(Character._of('!'), specialLast);
  }

  @Test
  void testPrefixAndSuffixOperators() {
    // Test unsetString behavior for prefix/suffix operators
    assertUnset.accept(unsetString._prefix());
    assertUnset.accept(unsetString._suffix());

    // Test with empty string
    final var emptyString = createTestString("");
    assertSet.accept(emptyString);
    assertUnset.accept(emptyString._prefix());
    assertUnset.accept(emptyString._suffix());

    // Test with single character string
    final var singleChar = createTestString("Z");
    assertSet.accept(singleChar);
    final var prefixChar = singleChar._prefix();
    final var suffixChar = singleChar._suffix();
    assertSet.accept(prefixChar);
    assertSet.accept(suffixChar);
    assertEquals(Character._of('Z'), prefixChar);
    assertEquals(Character._of('Z'), suffixChar);
    assertEquals(prefixChar, suffixChar); // Should be the same character

    // Test with multi-character string
    final var steve = createTestString("Steve");
    final var stevePrefix = steve._prefix();
    final var steveSuffix = steve._suffix();
    assertSet.accept(stevePrefix);
    assertSet.accept(steveSuffix);
    assertEquals(Character._of('S'), stevePrefix);
    assertEquals(Character._of('e'), steveSuffix);

    // Verify operators delegate to first() and last() methods
    assertEquals(steve.first(), steve._prefix());
    assertEquals(steve.last(), steve._suffix());

    // Test with Unicode characters
    final var unicode = createTestString("αβγ");
    assertEquals(Character._of('α'), unicode._prefix());
    assertEquals(Character._of('γ'), unicode._suffix());
  }

  @Test
  void testFirstLastConsistencyWithIterator() {
    // Test consistency between first()/last() and iterator
    final var testString = createTestString("Hello");

    // Get first character via first() method
    final var firstViaMethod = testString.first();
    assertSet.accept(firstViaMethod);
    assertEquals(Character._of('H'), firstViaMethod);

    // Get first character via iterator
    final var iterator = testString.iterator();
    assertSet.accept(iterator);
    assertTrue.accept(iterator.hasNext());
    final var firstViaIterator = iterator.next();
    assertEquals(firstViaMethod, firstViaIterator);

    // Get last character via last() method  
    final var lastViaMethod = testString.last();
    assertSet.accept(lastViaMethod);
    assertEquals(Character._of('o'), lastViaMethod);

    // Consume iterator to get last character
    Character lastViaIterator = null;
    while (iterator.hasNext().state) {
      lastViaIterator = iterator.next();
    }
    assertNotNull(lastViaIterator);
    assertEquals(lastViaMethod, lastViaIterator);
  }

  @Test
  void testTrimWithCharacter() {
    // Test canProcess scenarios (EK9 pattern validation)

    // Test unsetString behavior for trim with character parameter
    final var testChar = Character._of('a');
    assertUnset.accept(unsetString.trim(testChar));
    assertUnset.accept(unsetString.trim(new Character()));

    // Valid String with unsetString Character should return unsetString String  
    final var validString = createTestString("hello");
    assertUnset.accept(validString.trim(new Character()));

    // Basic functionality tests

    // Trim spaces from both ends
    final var spacePadded = String._of("  hello world  ");
    assertEquals(String._of("hello world"), spacePadded.trim(Character._of(' ')));

    // Trim specific character from both ends
    final var aPadded = String._of("aaahelloaaa");
    assertEquals(STR_HELLO, aPadded.trim(Character._of('a')));

    // No change when character not present at ends
    assertEquals(STR_HELLO, STR_HELLO.trim(Character._of('x')));

    // Empty string stays empty
    assertEquals(EMPTY_STRING, EMPTY_STRING.trim(Character._of('a')));

    // String with only trim character becomes empty
    final var onlyTrimChar = String._of("aaaa");
    assertEquals(EMPTY_STRING, onlyTrimChar.trim(Character._of('a')));

    // Single character - trim itself
    assertEquals(EMPTY_STRING, STR_LETTER_A.trim(Character._of('a')));

    // Single character - different char (no change)
    assertEquals(STR_LETTER_A, STR_LETTER_A.trim(Character._of('b')));
  }

  @Test
  void testTrimWithSpecialCharacters() {
    // Test regex special characters are properly escaped

    // Period character (regex special)
    final var dotPadded = String._of("...hello world...");
    assertEquals(String._of("hello world"), dotPadded.trim(Character._of('.')));

    // Plus character (regex special)
    final var plusPadded = String._of("+++text+++");
    assertEquals(String._of("text"), plusPadded.trim(Character._of('+')));

    // Star character (regex special)
    final var starPadded = String._of("***word***");
    assertEquals(String._of("word"), starPadded.trim(Character._of('*')));

    // Question mark (regex special)
    final var questionPadded = String._of("???test???");
    assertEquals(STR_TEST, questionPadded.trim(Character._of('?')));

    // Caret character (regex special)
    final var caretPadded = String._of("^^^data^^^");
    assertEquals(String._of("data"), caretPadded.trim(Character._of('^')));

    // Dollar character (regex special)
    final var dollarPadded = String._of("$$$value$$$");
    assertEquals(String._of("value"), dollarPadded.trim(Character._of('$')));
  }

  @Test
  void testTrimPreservesInternalCharacters() {
    // Verify that trim only removes from start/end, not internal characters

    // Spaces within string are preserved
    final var internalSpaces = String._of("  hello world  ");
    assertEquals(String._of("hello world"), internalSpaces.trim(Character._of(' ')));

    // Character appears internally - should be preserved
    final var internalA = String._of("a hello a world a");
    assertEquals(String._of(" hello a world "), internalA.trim(Character._of('a')));

    // Multiple internal occurrences
    final var multipleInternal = String._of("xxx hello xxx world xxx");
    assertEquals(String._of(" hello xxx world "), multipleInternal.trim(Character._of('x')));

    // Only at start
    final var onlyStart = String._of("aaa hello world");
    assertEquals(String._of(" hello world"), onlyStart.trim(Character._of('a')));

    // Only at end  
    final var onlyEnd = String._of("hello world aaa");
    assertEquals(String._of("hello world "), onlyEnd.trim(Character._of('a')));
  }

  @Test
  void testTrimVariousLengths() {
    // Test different amounts of trimming needed

    // Single char at each end
    final var singleAtEnds = String._of("ahelloa");
    assertEquals(STR_HELLO, singleAtEnds.trim(Character._of('a')));

    // Multiple chars at each end  
    final var multipleAtEnds = String._of("aaaahelloaaaa");
    assertEquals(STR_HELLO, multipleAtEnds.trim(Character._of('a')));

    // Uneven amounts at ends
    final var unevenEnds = String._of("ahelloaaaaa");
    assertEquals(STR_HELLO, unevenEnds.trim(Character._of('a')));

    // Very long string with trim chars
    final var longString = String._of("xxxxxxxxxxxxxxxxhello worldxxxxxxxxxxxxxxxx");
    assertEquals(String._of("hello world"), longString.trim(Character._of('x')));
  }

  @Test
  void testTrimConsistencyWithOriginalTrim() {
    // Verify that trim(Character(' ')) behaves like trim() for spaces only

    final var spacePadded = String._of("  hello world  ");
    assertEquals(spacePadded.trim(), spacePadded.trim(Character._of(' ')));

    // Test difference: trim() removes all whitespace, trim(Character) only removes specific char
    final var mixedWhitespace = String._of(" \t hello \t ");
    // trim() removes all whitespace (spaces and tabs)
    assertEquals(STR_HELLO, mixedWhitespace.trim());
    // trim(Character(' ')) only removes spaces, leaving tabs
    assertEquals(String._of("\t hello \t"), mixedWhitespace.trim(Character._of(' ')));
    // trim(Character('\t')) only removes tabs, leaving spaces  
    assertEquals(String._of(" \t hello \t "), mixedWhitespace.trim(Character._of('\t')));
  }

  @Test
  void testCountBasicFunctionality() {
    // Test basic character counting functionality

    // Multiple occurrences
    final var hello = STR_HELLO;
    assertEquals(INT_2, hello.count(Character._of('l')));
    assertEquals(INT_1, hello.count(Character._of('h')));
    assertEquals(INT_1, hello.count(Character._of('e')));
    assertEquals(INT_1, hello.count(Character._of('o')));

    // Character not present
    assertEquals(INT_0, hello.count(Character._of('x')));
    assertEquals(INT_0, hello.count(Character._of('z')));

    // Single character string
    final var singleChar = STR_LETTER_A;
    assertEquals(INT_1, singleChar.count(Character._of('a')));
    assertEquals(INT_0, singleChar.count(Character._of('b')));

    // String with only repeated character
    final var repeated = String._of("aaaa");
    assertEquals(INT_4, repeated.count(Character._of('a')));
    assertEquals(INT_0, repeated.count(Character._of('b')));

    // Case sensitivity
    final var mixed = String._of("Hello");
    assertEquals(INT_1, mixed.count(Character._of('H')));
    assertEquals(INT_0, mixed.count(Character._of('h')));  // Different case - not present
    assertEquals(INT_2, mixed.count(Character._of('l')));
  }

  @Test
  void testCountEdgeCases() {
    // Test canProcess scenarios (EK9 pattern validation)

    // unsetString String with valid Character should return unsetString Integer
    assertUnset.accept(unsetString.count(Character._of('a')));

    // Valid String with unsetString Character should return unsetString Integer  
    assertUnset.accept(STR_HELLO.count(new Character()));

    // Both unsetString should return unsetString Integer
    assertUnset.accept(unsetString.count(new Character()));

    // Empty string should return 0 (not unsetString)
    final var emptyResult = EMPTY_STRING.count(Character._of('a'));
    assertSet.accept(emptyResult);
    assertEquals(INT_0, emptyResult);
  }

  @Test
  void testCountSpecialCharacters() {
    // Test counting with various special characters

    // Whitespace characters
    final var spaces = String._of("a b c d");
    assertEquals(Integer._of(3), spaces.count(Character._of(' ')));

    // Newline characters
    final var newlines = String._of("a\nb\nc");
    assertEquals(INT_2, newlines.count(Character._of('\n')));

    // Tab characters
    final var tabs = String._of("a\tb\tc");
    assertEquals(INT_2, tabs.count(Character._of('\t')));

    // Punctuation
    final var punctuation = String._of("Hello, world! How are you?");
    assertEquals(INT_1, punctuation.count(Character._of(',')));
    assertEquals(INT_1, punctuation.count(Character._of('!')));
    assertEquals(INT_1, punctuation.count(Character._of('?')));

    // Special regex characters (should work as literals)
    final var dots = String._of("a.b.c.d");
    assertEquals(Integer._of(3), dots.count(Character._of('.')));

    final var stars = String._of("a*b*c*");
    assertEquals(Integer._of(3), stars.count(Character._of('*')));

    // Unicode characters
    final var unicode = String._of("αβγαβα");
    assertEquals(Integer._of(3), unicode.count(Character._of('α')));
    assertEquals(INT_2, unicode.count(Character._of('β')));
    assertEquals(INT_1, unicode.count(Character._of('γ')));
  }

  @Test
  void testCountPerformance() {
    // Test performance with longer strings

    // Long string with many occurrences
    final var longWithMany = String._of("abcabcabcabcabc");
    assertEquals(Integer._of(5), longWithMany.count(Character._of('a')));
    assertEquals(Integer._of(5), longWithMany.count(Character._of('b')));
    assertEquals(Integer._of(5), longWithMany.count(Character._of('c')));

    // Long string with no occurrences
    final var longWithNone = String._of("bcdefghijklmnopqrstuvwxyz");
    assertEquals(INT_0, longWithNone.count(Character._of('a')));

    // String with only target character
    final var onlyTarget = String._of("xxxxxxxxxx");
    assertEquals(Integer._of(10), onlyTarget.count(Character._of('x')));

    // Very repetitive pattern
    final var pattern = String._of("ababababab");
    assertEquals(Integer._of(5), pattern.count(Character._of('a')));
    assertEquals(Integer._of(5), pattern.count(Character._of('b')));
    assertEquals(INT_0, pattern.count(Character._of('c')));
  }

  @Test
  void testCountConsistencyWithOtherMethods() {
    // Test consistency with other String methods

    final var testString = String._of("hello world");

    // Count spaces should match what we expect
    assertEquals(INT_1, testString.count(Character._of(' ')));

    // Count first character should be 1
    final var firstChar = testString.first();
    assertSet.accept(firstChar);
    assertEquals(INT_1, testString.count(firstChar));

    // Count last character
    final var lastChar = testString.last();
    assertSet.accept(lastChar);
    assertEquals(INT_1, testString.count(lastChar));

    // For empty string, count should be 0
    assertEquals(INT_0, EMPTY_STRING.count(Character._of('a')));

    // Single character string
    final var single = String._of("z");
    assertEquals(INT_1, single.count(Character._of('z')));
    assertEquals(INT_0, single.count(Character._of('y')));
  }

  @Test
  void testAddCharacterOperator() {
    // Test operator + with Character (non-mutating)

    // Test canProcess scenarios (EK9 pattern validation)

    // unsetString String with valid Character should return unsetString String
    assertUnset.accept(unsetString._add(Character._of('a')));

    // Valid String with unsetString Character should return unsetString String
    assertUnset.accept(STR_HELLO._add(new Character()));

    // Both unsetString should return unsetString String
    assertUnset.accept(unsetString._add(new Character()));

    // Basic functionality tests

    // Add single character to empty string
    assertEquals(STR_LETTER_A, EMPTY_STRING._add(Character._of('a')));

    // Add character to existing string
    final var hello = STR_HELLO;
    assertEquals(String._of("hello!"), hello._add(Character._of('!')));

    // Add space character
    assertEquals(String._of("hello "), hello._add(Character._of(' ')));

    // Add number character
    assertEquals(String._of("hello1"), hello._add(Character._of('1')));

    // Original string should remain unchanged (non-mutating)
    assertEquals(STR_HELLO, hello);

    // Special characters
    final var test = STR_TEST;
    assertEquals(String._of("test."), test._add(Character._of('.')));
    assertEquals(String._of("test*"), test._add(Character._of('*')));
    assertEquals(String._of("test?"), test._add(Character._of('?')));

    // Unicode characters
    assertEquals(String._of("testα"), test._add(Character._of('α')));
    assertEquals(String._of("testβ"), test._add(Character._of('β')));
  }

  @Test
  void testAddCharacterConsistencyWithStringAdd() {
    // Verify that adding Character behaves consistently with adding String

    final var base = String._of("hello");
    final var charToAdd = Character._of('!');
    final var stringToAdd = String._of("!");

    // Adding Character should produce same result as adding single-char String
    assertEquals(base._add(stringToAdd), base._add(charToAdd));

    // Test with space
    final var space = Character._of(' ');
    final var spaceString = String._of(" ");
    assertEquals(base._add(spaceString), base._add(space));

    // Test with various characters
    final var chars = new char[] {'a', 'Z', '1', '@', '.', '*'};
    for (char c : chars) {
      final var character = Character._of(c);
      final var string = String._of(java.lang.String.valueOf(c));
      assertEquals(base._add(string), base._add(character));
    }
  }

  @Test
  void testAddAssignCharacterOperator() {
    // Test operator += with Character (mutating)

    // Test basic functionality

    // Add character to empty string
    var empty = String._of("");
    empty._addAss(Character._of('a'));
    assertEquals(STR_LETTER_A, empty);

    // Add character to existing string
    var hello = String._of("hello");
    hello._addAss(Character._of('!'));
    assertEquals(String._of("hello!"), hello);

    // Add multiple characters sequentially
    var build = String._of("test");
    build._addAss(Character._of(' '));
    assertEquals(String._of("test "), build);
    build._addAss(Character._of('1'));
    assertEquals(String._of("test 1"), build);
    build._addAss(Character._of('.'));
    assertEquals(String._of("test 1."), build);

    // Test canProcess scenarios (EK9 pattern validation)

    // Valid String with unsetString Character should become unsetString
    var validString = String._of("hello");
    validString._addAss(new Character());
    assertUnset.accept(validString);

    // Already unsetString String with valid Character should remain unsetString
    var alreadyUnset = new String();
    assertUnset.accept(alreadyUnset);
    alreadyUnset._addAss(Character._of('a'));
    assertUnset.accept(alreadyUnset);

    // Test that mutation corrupts with unsetString
    var mutableString = String._of("hello");
    assertSet.accept(mutableString);
    mutableString._addAss(new Character()); // Add unsetString Character
    assertUnset.accept(mutableString); // Should become unsetString
  }

  @Test
  void testAddAssignCharacterSpecialCases() {
    // Test += operator with various special characters

    // Whitespace characters
    var whitespace = String._of("text");
    whitespace._addAss(Character._of(' '));
    assertEquals(String._of("text "), whitespace);

    var tabs = String._of("text");
    tabs._addAss(Character._of('\t'));
    assertEquals(String._of("text\t"), tabs);

    var newlines = String._of("text");
    newlines._addAss(Character._of('\n'));
    assertEquals(String._of("text\n"), newlines);

    // Special regex characters
    var dots = String._of("file");
    dots._addAss(Character._of('.'));
    assertEquals(String._of("file."), dots);

    var stars = String._of("glob");
    stars._addAss(Character._of('*'));
    assertEquals(String._of("glob*"), stars);

    // Punctuation
    var punctuation = String._of("hello");
    punctuation._addAss(Character._of(','));
    assertEquals(String._of("hello,"), punctuation);
    punctuation._addAss(Character._of(' '));
    assertEquals(String._of("hello, "), punctuation);
    punctuation._addAss(Character._of('w'));
    assertEquals(String._of("hello, w"), punctuation);

    // Unicode characters
    var unicode = String._of("test");
    unicode._addAss(Character._of('α'));
    assertEquals(String._of("testα"), unicode);
    unicode._addAss(Character._of('β'));
    assertEquals(String._of("testαβ"), unicode);
  }

  @Test
  void testAddAssignCharacterConsistencyWithStringAddAssign() {
    // Verify that += Character behaves consistently with += String

    var stringVersion = String._of("hello");
    var characterVersion = String._of("hello");

    // Add exclamation mark both ways
    stringVersion._addAss(String._of("!"));
    characterVersion._addAss(Character._of('!'));
    assertEquals(stringVersion, characterVersion);

    // Add space both ways
    var stringSpace = String._of("test");
    var characterSpace = String._of("test");
    stringSpace._addAss(String._of(" "));
    characterSpace._addAss(Character._of(' '));
    assertEquals(stringSpace, characterSpace);

    // Test corruption behavior with unsetString values
    var stringCorrupt = String._of("hello");
    var characterCorrupt = String._of("hello");

    stringCorrupt._addAss(new String()); // Add unsetString String
    characterCorrupt._addAss(new Character()); // Add unsetString Character

    assertUnset.accept(stringCorrupt);
    assertUnset.accept(characterCorrupt);

    // Both should be unsetString - can't use assertEquals on unsetString values as they are different instances
    // But their EK9 equality should both return unsetString (not comparable)
    assertUnset.accept(stringCorrupt._eq(characterCorrupt));
  }

  @Test
  void testMatchesWithRegEx() {
    // Test basic pattern matching functionality with RegEx delegation

    // Test canProcess scenarios (EK9 pattern validation)

    // unsetString String with valid RegEx should return unsetString Boolean
    final var digitRegex = new RegEx(String._of("\\d+"));
    assertNotNull(digitRegex);
    assertUnset.accept(unsetString._matches(digitRegex));

    // Valid String with unsetString RegEx should return unsetString Boolean
    final var numberString = createTestString("123");
    assertUnset.accept(numberString._matches(new RegEx()));

    // Both unsetString should return unsetString Boolean
    assertUnset.accept(unsetString._matches(new RegEx()));

    // Basic functionality tests

    // Simple hit - string matches pattern
    assertTrue.accept(numberString._matches(digitRegex));

    // Simple miss - string doesn't match pattern
    final var textString = createTestString("abc");
    assertFalse.accept(textString._matches(digitRegex));

    // Test with empty string
    final var emptyString = createTestString("");
    final var emptyRegex = new RegEx(EMPTY_STRING);
    assertTrue.accept(emptyString._matches(emptyRegex));

    // Test with more patterns
    final var wordRegex = new RegEx(String._of("\\w+"));
    assertTrue.accept(textString._matches(wordRegex));
    assertFalse.accept(createTestString("@#$")._matches(wordRegex));
  }

  @Test
  void testSplitWithRegEx() {
    // Test basic string splitting functionality with RegEx delegation

    // Test canProcess scenarios (EK9 pattern validation)

    // unsetString String with valid RegEx should return empty List
    final var commaRegex = new RegEx(String._of(","));
    final var unsetResult = unsetString.split(commaRegex);
    assertSet.accept(unsetResult);
    assertTrue.accept(unsetResult._empty());

    // Valid String with unsetString RegEx should return empty List
    final var csvString = createTestString("a,b,c");
    final var unsetRegexResult = csvString.split(new RegEx());
    assertSet.accept(unsetRegexResult);
    assertTrue.accept(unsetRegexResult._empty());

    // Both unsetString should return empty List
    final var bothUnsetResult = unsetString.split(new RegEx());
    assertSet.accept(bothUnsetResult);
    assertTrue.accept(bothUnsetResult._empty());

    // Basic functionality tests

    // No match split - pattern doesn't match, should return single-item list
    final var noMatchString = createTestString("hello");
    final var noMatchResult = noMatchString.split(commaRegex);
    assertSet.accept(noMatchResult);
    assertEquals(1, noMatchResult._len().state);
    assertEquals(noMatchString, noMatchResult.get(INT_0));

    // Three-group split - split on comma should produce 3 parts
    final var threeGroupResult = csvString.split(commaRegex);
    assertSet.accept(threeGroupResult);
    assertEquals(3, threeGroupResult._len().state);
    assertEquals(createTestString("a"), threeGroupResult.get(INT_0));
    assertEquals(createTestString("b"), threeGroupResult.get(INT_1));
    assertEquals(createTestString("c"), threeGroupResult.get(INT_2));

    // Test with whitespace splitting
    final var spaceString = createTestString("hello world test");
    final var whitespaceRegex = new RegEx(String._of("\\s+"));
    final var wordResult = spaceString.split(whitespaceRegex);
    assertSet.accept(wordResult);
    assertEquals(3, wordResult._len().state);
    assertEquals(createTestString("hello"), wordResult.get(INT_0));
    assertEquals(createTestString("world"), wordResult.get(INT_1));
    assertEquals(createTestString("test"), wordResult.get(INT_2));

    // Test with empty string
    final var emptyString = createTestString("");
    final var emptyResult = emptyString.split(commaRegex);
    assertSet.accept(emptyResult);
    assertEquals(1, emptyResult._len().state);
    assertEquals(emptyString, emptyResult.get(INT_0));
  }
}
