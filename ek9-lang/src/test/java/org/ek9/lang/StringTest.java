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

    //Now a simple value might be longer but b simple value comes after it lexagraphically.
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
}
