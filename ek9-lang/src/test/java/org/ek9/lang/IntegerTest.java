package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class IntegerTest extends Common {

  final Boolean true1 = Boolean._of("true");
  final Boolean false1 = Boolean._of("false");

  final Integer unset = new Integer();
  final Integer i0 = Integer._of(0);
  final Integer iMinus1 = Integer._of(-1);
  final Integer i1 = Integer._of(1);
  final Integer i2 = Integer._of(2);
  final Integer iMinus2 = Integer._of(-2);
  final Integer i3 = Integer._of(3);
  final Integer i4 = Integer._of(4);

  @Test
  void testConstruction() {
    final var defaultConstructor = new Integer();
    assertUnset.accept(defaultConstructor);

    final var unset1 = Integer._of("not parsable");
    assertUnset.accept(unset1);

    final var unset2 = Integer._of((java.lang.String) null);
    assertUnset.accept(unset2);

    assertSet.accept(i0);
    assertEquals(0, i0.state);

    assertSet.accept(i1);
    assertEquals(1, i1.state);

    assertSet.accept(i2);
    assertEquals(2, i2.state);

    assertSet.accept(i3);
    assertSet.accept(i3);
    assertEquals(3L, i3.state);

    final var also3 = Integer._of(i3);
    assertSet.accept(also3);
    assertEquals(3L, also3.state);

    final var again3 = new Integer(i3);
    assertSet.accept(again3);
    assertEquals(3L, again3.state);

    assertSet.accept(i4);
    assertEquals(4L, i4.state);
  }

  @Test
  void testEquality() {
    final var i00 = Integer._of(0);

    //Eq
    assertEquals(i00, i0);
    assertEquals(true1, i0._eq(i00));

    assertUnset.accept(i0._eq(unset));
    assertUnset.accept(unset._eq(unset));
    assertUnset.accept(unset._eq(i0));

    //Neq
    assertEquals(false1, i0._neq(i00));
    assertUnset.accept(i0._neq(unset));
    assertUnset.accept(unset._neq(unset));
    assertUnset.accept(unset._neq(i0));

    //Lt
    assertTrue.accept(i0._lt(i1));
    assertFalse.accept(i1._lt(i0));
    assertUnset.accept(unset._lt(i1));
    assertUnset.accept(i0._lt(unset));

    //gt
    assertTrue.accept(i1._gt(i0));
    assertFalse.accept(i0._gt(i1));
    assertUnset.accept(unset._gt(i1));
    assertUnset.accept(i0._gt(unset));

    //Lteq
    assertTrue.accept(i0._lteq(i00));
    assertTrue.accept(i0._lteq(i1));
    assertFalse.accept(i1._lteq(i0));
    assertUnset.accept(unset._lteq(i0));
    assertUnset.accept(i0._lteq(unset));

    //Gteq
    assertTrue.accept(i0._gteq(i00));
    assertTrue.accept(i1._gteq(i0));
    assertFalse.accept(i0._gteq(i1));
    assertUnset.accept(unset._gteq(i0));
    assertUnset.accept(i0._gteq(unset));
  }

  @Test
  void testComparison() {
    final var i00 = Integer._of(0);

    assertEquals(Integer._of(0), i0._fuzzy(i00));
    assertEquals(Integer._of(-1), i0._fuzzy(i1));
    assertEquals(Integer._of(1), i1._fuzzy(i0));
    assertUnset.accept(unset._fuzzy(i0));
    assertUnset.accept(i0._fuzzy(unset));
  }

  @Test
  void testIsSet() {
    assertNotNull(unset);
    assertFalse.accept(unset._isSet());

    final var v1 = Integer._of(90);
    assertNotNull(v1);
    assertTrue.accept(v1._isSet());

  }

  /**
   * Excludes the Float operators those are tested elsewhere.
   */
  @Test
  void testSimpleMathematics() {

    final var minusZero = i0._negate();
    assertEquals(i0, minusZero);

    final var stillUnset = unset._negate();
    assertUnset.accept(stillUnset);

    //Negate
    assertEquals(iMinus1, i1._negate());
    assertEquals(unset, unset._negate());

    //Addition
    assertEquals(iMinus1, i0._add(iMinus1));
    assertEquals(i0, i1._add(iMinus1));
    assertEquals(i0, iMinus1._add(i1));
    assertEquals(unset, unset._add(i1));
    assertEquals(unset, i0._add(unset));

    //Substraction
    assertEquals(iMinus1, i0._sub(i1));
    assertEquals(i0, i1._sub(i1));
    assertEquals(i0, iMinus1._sub(iMinus1));
    assertEquals(unset, unset._sub(i1));
    assertEquals(unset, i0._sub(unset));

    //Multiplication
    assertEquals(i0, i0._mul(i1));
    assertEquals(i0, i0._mul(iMinus1));
    assertEquals(i2, i1._mul(i2));
    assertEquals(iMinus2, i2._mul(iMinus1));
    assertEquals(i4, i2._mul(i2));
    assertEquals(i4, iMinus2._mul(iMinus2));
    assertEquals(unset, unset._mul(i1));
    assertEquals(unset, i0._mul(unset));

    //Division
    assertEquals(i1, i2._div(i2));

    assertEquals(i2, i4._div(i2));
    assertEquals(iMinus2, i4._div(iMinus2));
    assertEquals(i0, i0._div(i2));

    assertEquals(unset, i0._div(i0));
    assertEquals(unset, i0._div(unset));
    assertEquals(unset, unset._div(i2));

  }

  @Test
  void testAsString() {

    assertUnset.accept(unset._string());

    assertEquals(String._of("0"), i0._string());
    assertEquals(String._of("1"), i1._string());
    assertEquals(String._of("-1"), iMinus1._string());

  }

  @Test
  void testHashCode() {
    assertUnset.accept(unset._hashcode());
    assertEquals(i0._hashcode(), i0._hashcode());
    assertNotEquals(i0._hashcode(), i1._hashcode());
    assertNotEquals(iMinus1._hashcode(), i1._hashcode());
  }

  /**
   * Really for use with Integers greater than ten or less than -ten.
   * So sort of stringy, get the first and the last integer value.  i.e.</br>
   * 1234 : prefix is 1, suffix is 4<br/>
   * -1234 : prefix is -1 and suffix is -4</br>
   */
  @Test
  void testPrefixSuffix() {
    assertUnset.accept(unset._prefix());
    assertUnset.accept(unset._suffix());

    assertEquals(i0, i0._prefix());
    assertEquals(i0, i0._suffix());

    final var example = Integer._of(1234);
    assertEquals(i1, example._prefix());
    assertEquals(i4, example._suffix());

    final var minusExample = example._negate();
    assertEquals(iMinus1, minusExample._prefix());
    assertEquals(i4._negate(), minusExample._suffix());
  }

  @Test
  void testAdditionalMathematicalOperators() {

    assertEquals(i0, i0._abs());
    assertEquals(i2, i2._abs());
    assertEquals(i2, iMinus2._abs());
    assertEquals(unset, unset._abs());
  }

  @Test
  void testUtilityOperators() {
    assertUnset.accept(unset._empty());

    assertEquals(true1, i0._empty());
    assertEquals(false1, i1._empty());
    assertEquals(false1, iMinus2._empty());

    assertUnset.accept(unset._len());
    assertEquals(i1, i0._len());
    assertEquals(i1, i1._len());
    //The length includes the minua sign.
    assertEquals(i2, iMinus1._len());

    assertEquals(i4, Integer._of(1234)._len());
  }

  @Test
  void testBitWiseOnIntegers() {
    assertUnset.accept(unset._and(i1));
    assertUnset.accept(unset._or(i1));
    assertUnset.accept(unset._xor(i1));

    assertEquals(i3, i1._or(i2));
    assertEquals(i3, i3._or(i2));

    assertEquals(i2, i3._and(i2));
    assertEquals(i0, i1._and(i2));

    assertEquals(i1, i3._xor(i2));
  }
}
