package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class StringTest extends Common {

  final String unset = new String();
  final Integer unsetInteger = new Integer();
  final Boolean trueBoolean = Boolean._of(true);
  final Boolean falseBoolean = Boolean._of(false);

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
    final var underTest = String._of("");
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
    //If unset trim should produce something that is also unset.
    assertUnset.accept(new String().trim());

    //String with some white space at the start and end, to be trimmed.
    final var validString = String._of("\tA simple value ");
    assertSet.accept(validString);
    assertEquals(String._of("A simple value"), validString.trim());
  }

  @Test
  void testCaseTransform() {
    //If unset trim should produce something that is also unset.
    assertUnset.accept(unset.upperCase());
    assertUnset.accept(unset.lowerCase());

    //String with some white space at the start and end, to be trimmed.
    final var validString = String._of("A simple value");

    assertEquals(String._of("A SIMPLE VALUE"), validString.upperCase());
    assertEquals(String._of("a simple value"), validString.lowerCase());
  }

  @Test
  void testPadding() {
    final var pad20 = Integer._of(20);
    final var validString = String._of("A simple value");
    final var tooLongString = String._of("A value that is longer than the padding of 20");

    //First the combination of invalid unset variables/argument.
    assertUnset.accept(new String().rightPadded(pad20));
    assertUnset.accept(new String().leftPadded(pad20));

    assertUnset.accept(validString.rightPadded(unsetInteger));
    assertUnset.accept(validString.leftPadded(unsetInteger));

    assertEquals(String._of("A simple value      "), validString.rightPadded(pad20));
    assertEquals(String._of("      A simple value"), validString.leftPadded(pad20));

    //Does not truncate, leaves it as ia, as it does not need padding.
    assertEquals(tooLongString, tooLongString.rightPadded(pad20));
    assertEquals(tooLongString, tooLongString.leftPadded(pad20));
  }

  @Test
  void testEquality() {

    final var aSimpleValue = String._of("A simple value");
    final var aSimpleValueButLonger = String._of("A simple value ");
    final var bSimpleValue = String._of("B simple value");

    //First all the unset combinations.
    assertUnset.accept(aSimpleValue._lt(unset));
    assertUnset.accept(unset._lt(aSimpleValue));

    assertUnset.accept(aSimpleValue._lteq(unset));
    assertUnset.accept(unset._lteq(aSimpleValue));

    assertUnset.accept(aSimpleValue._gt(unset));
    assertUnset.accept(unset._gt(aSimpleValue));

    assertUnset.accept(aSimpleValue._gteq(unset));
    assertUnset.accept(unset._gteq(aSimpleValue));

    assertUnset.accept(aSimpleValue._eq(unset));
    assertUnset.accept(unset._eq(aSimpleValue));

    assertUnset.accept(aSimpleValue._neq(unset));
    assertUnset.accept(unset._neq(aSimpleValue));

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
    assertNotNull(unset);
    assertFalse.accept(unset._isSet());

    final var v1 = String._of("Some valid value");
    assertNotNull(v1);
    assertTrue.accept(v1._isSet());
  }

  @Test
  void testComparison() {

    final var i0 = Integer._of(0);
    final var iMinus1 = Integer._of(-1);
    final var i1 = Integer._of(1);
    final var i2 = Integer._of(2);
    final var i3 = Integer._of(3);
    final var steve = String._of("Steve");
    final var steven = String._of("Steven");

    //First check with unset values.
    assertUnset.accept(steve._cmp(unset));
    assertUnset.accept(unset._cmp(steve));
    assertUnset.accept(steve._fuzzy(unset));
    assertUnset.accept(unset._fuzzy(steve));

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
    final var steve = String._of("Steve");
    final var limb = String._of("Limb");
    final var steveLimb = String._of("Steve Limb");

    assertUnset.accept(unset._add(unset));
    assertUnset.accept(unset._add(steve));
    assertUnset.accept(steve._add(unset));

    assertEquals(steveLimb, steve._add(String._of(" "))._add(limb));

  }

  @Test
  void testAsString() {
    final var steve = String._of("Steve");

    assertUnset.accept(unset._string());
    assertSet.accept(steve._string());
    assertEquals("Steve", steve._string().toString());

  }

  @Test
  void testHashCode() {
    final var steve = String._of("Steve");
    final var steven = String._of("Steven");

    assertUnset.accept(unset._hashcode());
    assertEquals(steve._hashcode(), steve._hashcode());
    assertNotEquals(steven._hashcode(), steve._hashcode());
  }

  @Test
  void testUtilityOperators() {
    final var steve = String._of("Steve");
    final var whitespace = String._of(" \n \t ");

    assertUnset.accept(unset._empty());
    assertUnset.accept(unset._len());

    assertFalse.accept(steve._empty());
    assertEquals(Integer._of(5), steve._len());

    assertTrue.accept(whitespace._empty());
    assertEquals(Integer._of(5), whitespace._len());

    //Check contains mechanism
    assertUnset.accept(unset._contains(steve));
    assertEquals(trueBoolean, steve._contains(steve));
    assertEquals(trueBoolean, steve._contains(String._of("t")));
    assertEquals(trueBoolean, steve._contains(String._of("teve")));

    assertEquals(falseBoolean, steve._contains(String._of("teven")));

    //Note thet S is a lowercase s - so does not match.
    assertEquals(falseBoolean, steve._contains(String._of("steve")));

  }

  @Test
  void testPipeLogic() {
    final var steve = String._of("Steve");
    final var limb = String._of("Limb");
    final var steveLimb = String._of("Steve Limb 21");
    final var twentyOne = Integer._of(21);

    final var mutatedValue = new String();
    assertUnset.accept(mutatedValue);

    mutatedValue._pipe(unset);
    assertUnset.accept(mutatedValue);

    //Example of how an unset value can become set when adding values
    mutatedValue._pipe(steve);
    mutatedValue._pipe(String._of(" "));
    mutatedValue._pipe(limb);

    //Note that unlike other operators the pipe ignores unset values.
    mutatedValue._pipe(unset);

    mutatedValue._pipe(String._of(" "));
    mutatedValue._pipe(twentyOne);

    assertEquals(steveLimb, mutatedValue._string());

  }

  @Test
  void testReplaceAndCopyLogic() {

    final var steve = String._of("Steve");
    final var limb = String._of("Limb");

    var mutatedValue = String._of("Steve");
    assertEquals(steve, mutatedValue);

    mutatedValue._replace(limb);
    assertEquals(limb, mutatedValue);

    mutatedValue._replace(unset);
    assertUnset.accept(mutatedValue);

    //Now just check that it can take a value after being unset
    mutatedValue._replace(steve);
    assertEquals(steve, mutatedValue);
  }

  @Test
  void testMutationOperators() {

    final var steveLimb = String._of("Steve Limb");

    var mutatedValue = String._of("Steve");
    mutatedValue._addAss(String._of(" "));
    mutatedValue._addAss(String._of("Limb"));

    assertEquals(steveLimb, mutatedValue._string());

    //Check that is something unset is added then it results in the mutated value being unset.
    mutatedValue._addAss(unset);
    assertUnset.accept(mutatedValue);

  }

  @Test
  void testFirstAndLastMethods() {
    // Test with unset string
    assertUnset.accept(unset.first());
    assertUnset.accept(unset.last());

    // Test with empty string
    final var emptyString = String._of("");
    assertSet.accept(emptyString);
    assertUnset.accept(emptyString.first());
    assertUnset.accept(emptyString.last());

    // Test with single character string
    final var singleChar = String._of("A");
    assertSet.accept(singleChar);
    final var firstChar = singleChar.first();
    final var lastChar = singleChar.last();
    assertSet.accept(firstChar);
    assertSet.accept(lastChar);
    assertEquals(Character._of('A'), firstChar);
    assertEquals(Character._of('A'), lastChar);
    assertEquals(firstChar, lastChar); // Should be the same character

    // Test with multi-character string
    final var steve = String._of("Steve");
    assertSet.accept(steve);
    final var steveFirst = steve.first();
    final var steveLast = steve.last();
    assertSet.accept(steveFirst);
    assertSet.accept(steveLast);
    assertEquals(Character._of('S'), steveFirst);
    assertEquals(Character._of('e'), steveLast);
    assertNotEquals(steveFirst, steveLast);

    // Test with special characters and numbers
    final var special = String._of("@123!");
    final var specialFirst = special.first();
    final var specialLast = special.last();
    assertEquals(Character._of('@'), specialFirst);
    assertEquals(Character._of('!'), specialLast);
  }

  @Test
  void testPrefixAndSuffixOperators() {
    // Test with unset string
    assertUnset.accept(unset._prefix());
    assertUnset.accept(unset._suffix());

    // Test with empty string
    final var emptyString = String._of("");
    assertSet.accept(emptyString);
    assertUnset.accept(emptyString._prefix());
    assertUnset.accept(emptyString._suffix());

    // Test with single character string
    final var singleChar = String._of("Z");
    assertSet.accept(singleChar);
    final var prefixChar = singleChar._prefix();
    final var suffixChar = singleChar._suffix();
    assertSet.accept(prefixChar);
    assertSet.accept(suffixChar);
    assertEquals(Character._of('Z'), prefixChar);
    assertEquals(Character._of('Z'), suffixChar);
    assertEquals(prefixChar, suffixChar); // Should be the same character

    // Test with multi-character string
    final var steve = String._of("Steve");
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
    final var unicode = String._of("αβγ");
    assertEquals(Character._of('α'), unicode._prefix());
    assertEquals(Character._of('γ'), unicode._suffix());
  }

  @Test
  void testFirstLastConsistencyWithIterator() {
    // Test consistency between first()/last() and iterator
    final var testString = String._of("Hello");
    
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
    
    // Unset String with valid Character should return unset String
    assertUnset.accept(unset.trim(Character._of('a')));
    
    // Valid String with unset Character should return unset String  
    final var validString = String._of("hello");
    assertUnset.accept(validString.trim(new Character()));
    
    // Both unset should return unset String
    assertUnset.accept(unset.trim(new Character()));
    
    // Basic functionality tests
    
    // Trim spaces from both ends
    final var spacePadded = String._of("  hello world  ");
    assertEquals(String._of("hello world"), spacePadded.trim(Character._of(' ')));
    
    // Trim specific character from both ends
    final var aPadded = String._of("aaahelloaaa");
    assertEquals(String._of("hello"), aPadded.trim(Character._of('a')));
    
    // No change when character not present at ends
    final var noChange = String._of("hello");
    assertEquals(String._of("hello"), noChange.trim(Character._of('x')));
    
    // Empty string stays empty
    final var emptyString = String._of("");
    assertEquals(String._of(""), emptyString.trim(Character._of('a')));
    
    // String with only trim character becomes empty
    final var onlyTrimChar = String._of("aaaa");
    assertEquals(String._of(""), onlyTrimChar.trim(Character._of('a')));
    
    // Single character - trim itself
    final var singleChar = String._of("a");
    assertEquals(String._of(""), singleChar.trim(Character._of('a')));
    
    // Single character - different char (no change)
    final var singleDifferentChar = String._of("a");
    assertEquals(String._of("a"), singleDifferentChar.trim(Character._of('b')));
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
    assertEquals(String._of("test"), questionPadded.trim(Character._of('?')));
    
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
    assertEquals(String._of("hello"), singleAtEnds.trim(Character._of('a')));
    
    // Multiple chars at each end  
    final var multipleAtEnds = String._of("aaaahelloaaaa");
    assertEquals(String._of("hello"), multipleAtEnds.trim(Character._of('a')));
    
    // Uneven amounts at ends
    final var unevenEnds = String._of("ahelloaaaaa");
    assertEquals(String._of("hello"), unevenEnds.trim(Character._of('a')));
    
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
    assertEquals(String._of("hello"), mixedWhitespace.trim());
    // trim(Character(' ')) only removes spaces, leaving tabs
    assertEquals(String._of("\t hello \t"), mixedWhitespace.trim(Character._of(' ')));
    // trim(Character('\t')) only removes tabs, leaving spaces  
    assertEquals(String._of(" \t hello \t "), mixedWhitespace.trim(Character._of('\t')));
  }

  @Test
  void testCountBasicFunctionality() {
    // Test basic character counting functionality
    
    // Multiple occurrences
    final var hello = String._of("hello");
    assertEquals(Integer._of(2), hello.count(Character._of('l')));
    assertEquals(Integer._of(1), hello.count(Character._of('h')));
    assertEquals(Integer._of(1), hello.count(Character._of('e')));
    assertEquals(Integer._of(1), hello.count(Character._of('o')));
    
    // Character not present
    assertEquals(Integer._of(0), hello.count(Character._of('x')));
    assertEquals(Integer._of(0), hello.count(Character._of('z')));
    
    // Single character string
    final var singleChar = String._of("a");
    assertEquals(Integer._of(1), singleChar.count(Character._of('a')));
    assertEquals(Integer._of(0), singleChar.count(Character._of('b')));
    
    // String with only repeated character
    final var repeated = String._of("aaaa");
    assertEquals(Integer._of(4), repeated.count(Character._of('a')));
    assertEquals(Integer._of(0), repeated.count(Character._of('b')));
    
    // Case sensitivity
    final var mixed = String._of("Hello");
    assertEquals(Integer._of(1), mixed.count(Character._of('H')));
    assertEquals(Integer._of(0), mixed.count(Character._of('h')));  // Different case - not present
    assertEquals(Integer._of(2), mixed.count(Character._of('l')));
  }

  @Test
  void testCountEdgeCases() {
    // Test canProcess scenarios (EK9 pattern validation)
    
    // Unset String with valid Character should return unset Integer
    assertUnset.accept(unset.count(Character._of('a')));
    
    // Valid String with unset Character should return unset Integer  
    final var validString = String._of("hello");
    assertUnset.accept(validString.count(new Character()));
    
    // Both unset should return unset Integer
    assertUnset.accept(unset.count(new Character()));
    
    // Empty string should return 0 (not unset)
    final var emptyString = String._of("");
    final var emptyResult = emptyString.count(Character._of('a'));
    assertSet.accept(emptyResult);
    assertEquals(Integer._of(0), emptyResult);
  }

  @Test
  void testCountSpecialCharacters() {
    // Test counting with various special characters
    
    // Whitespace characters
    final var spaces = String._of("a b c d");
    assertEquals(Integer._of(3), spaces.count(Character._of(' ')));
    
    // Newline characters
    final var newlines = String._of("a\nb\nc");
    assertEquals(Integer._of(2), newlines.count(Character._of('\n')));
    
    // Tab characters
    final var tabs = String._of("a\tb\tc");
    assertEquals(Integer._of(2), tabs.count(Character._of('\t')));
    
    // Punctuation
    final var punctuation = String._of("Hello, world! How are you?");
    assertEquals(Integer._of(1), punctuation.count(Character._of(',')));
    assertEquals(Integer._of(1), punctuation.count(Character._of('!')));
    assertEquals(Integer._of(1), punctuation.count(Character._of('?')));
    
    // Special regex characters (should work as literals)
    final var dots = String._of("a.b.c.d");
    assertEquals(Integer._of(3), dots.count(Character._of('.')));
    
    final var stars = String._of("a*b*c*");
    assertEquals(Integer._of(3), stars.count(Character._of('*')));
    
    // Unicode characters
    final var unicode = String._of("αβγαβα");
    assertEquals(Integer._of(3), unicode.count(Character._of('α')));
    assertEquals(Integer._of(2), unicode.count(Character._of('β')));
    assertEquals(Integer._of(1), unicode.count(Character._of('γ')));
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
    assertEquals(Integer._of(0), longWithNone.count(Character._of('a')));
    
    // String with only target character
    final var onlyTarget = String._of("xxxxxxxxxx");
    assertEquals(Integer._of(10), onlyTarget.count(Character._of('x')));
    
    // Very repetitive pattern
    final var pattern = String._of("ababababab");
    assertEquals(Integer._of(5), pattern.count(Character._of('a')));
    assertEquals(Integer._of(5), pattern.count(Character._of('b')));
    assertEquals(Integer._of(0), pattern.count(Character._of('c')));
  }

  @Test
  void testCountConsistencyWithOtherMethods() {
    // Test consistency with other String methods
    
    final var testString = String._of("hello world");
    
    // Count spaces should match what we expect
    assertEquals(Integer._of(1), testString.count(Character._of(' ')));
    
    // Count first character should be 1
    final var firstChar = testString.first();
    assertSet.accept(firstChar);
    assertEquals(Integer._of(1), testString.count(firstChar));
    
    // Count last character
    final var lastChar = testString.last();
    assertSet.accept(lastChar);
    assertEquals(Integer._of(1), testString.count(lastChar));
    
    // For empty string, count should be 0
    final var empty = String._of("");
    assertEquals(Integer._of(0), empty.count(Character._of('a')));
    
    // Single character string
    final var single = String._of("z");
    assertEquals(Integer._of(1), single.count(Character._of('z')));
    assertEquals(Integer._of(0), single.count(Character._of('y')));
  }
}
