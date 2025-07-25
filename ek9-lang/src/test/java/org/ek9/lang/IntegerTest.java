package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.function.Consumer;
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

  // Helper methods for eliminating duplication

  /**
   * Helper method to test comparison operations with unset values.
   */
  private void assertComparisonOperatorsWithUnset(Integer validValue) {
    // Test all comparison operators with unset
    assertUnset.accept(validValue._eq(unset));
    assertUnset.accept(unset._eq(validValue));
    assertUnset.accept(unset._eq(unset));

    assertUnset.accept(validValue._neq(unset));
    assertUnset.accept(unset._neq(validValue));
    assertUnset.accept(unset._neq(unset));

    assertUnset.accept(validValue._lt(unset));
    assertUnset.accept(unset._lt(validValue));

    assertUnset.accept(validValue._gt(unset));
    assertUnset.accept(unset._gt(validValue));

    assertUnset.accept(validValue._lteq(unset));
    assertUnset.accept(unset._lteq(validValue));

    assertUnset.accept(validValue._gteq(unset));
    assertUnset.accept(unset._gteq(validValue));
  }

  @Test
  void testConstruction() {
    final var defaultConstructor = new Integer();
    assertUnset.accept(defaultConstructor);

    final var unset1 = Integer._of("not parsable");
    assertUnset.accept(unset1);

    final var unset2 = Integer._of((java.lang.String) null);
    assertUnset.accept(unset2);

    final var unset3 = new Integer(new String());
    assertUnset.accept(unset3);

    final var checkZero = new Integer(String._of("0"));
    assertEquals(0, checkZero.state);

    assertSet.accept(i1);
    assertEquals(1, i1.state);

    assertSet.accept(i2);
    assertEquals(2, i2.state);

    assertSet.accept(i3);
    assertEquals(3L, i3.state);

    final var also3 = Integer._of(i3);
    assertSet.accept(also3);
    assertEquals(3L, also3.state);

    final var again3 = new Integer(i3);
    assertSet.accept(again3);
    assertEquals(3L, again3.state);

    final var check4 = new Integer(i4);
    assertSet.accept(check4);
    assertEquals(i4, check4);
  }

  @Test
  void testEquality() {
    final var i00 = Integer._of(0);

    // Test all comparison operators with unset values using helper
    assertComparisonOperatorsWithUnset(i0);

    //Eq
    assertEquals(i00, i0);
    assertEquals(true1, i0._eq(i00));

    //Neq
    assertEquals(false1, i0._neq(i00));

    //Lt
    assertTrue.accept(i0._lt(i1));
    assertFalse.accept(i1._lt(i0));

    //gt
    assertTrue.accept(i1._gt(i0));
    assertFalse.accept(i0._gt(i1));

    //Lteq
    assertTrue.accept(i0._lteq(i00));
    assertTrue.accept(i0._lteq(i1));
    assertFalse.accept(i1._lteq(i0));

    //Gteq
    assertTrue.accept(i0._gteq(i00));
    assertTrue.accept(i1._gteq(i0));
    assertFalse.accept(i0._gteq(i1));
  }

  @Test
  void testComparison() {
    final var i00 = Integer._of(0);

    assertEquals(Integer._of(0), i0._fuzzy(i00));
    assertEquals(Integer._of(-1), i0._fuzzy(i1));
    assertEquals(Integer._of(1), i1._fuzzy(i0));
    // Test unset behavior for fuzzy comparison
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

    // Test unary operations with unset values
    assertUnset.accept(unset._negate());
    assertUnset.accept(unset._inc());
    assertUnset.accept(unset._dec());

    final var minusZero = i0._negate();
    assertEquals(i0, minusZero);

    //Negate
    assertEquals(iMinus1, i1._negate());

    // Create fresh objects for mutating inc/dec operations to avoid corrupting constants
    final var freshI1 = Integer._of(1);
    assertEquals(i2, freshI1._inc());

    final var freshI0 = Integer._of(0);
    assertEquals(iMinus1, freshI0._dec());

    //Addition - test unset behavior and specific examples
    assertUnset.accept(unset._add(i1));
    assertUnset.accept(i0._add(unset));
    assertEquals(iMinus1, i0._add(iMinus1));
    assertEquals(i0, i1._add(iMinus1));
    assertEquals(i0, iMinus1._add(i1));

    //Subtraction - test unset behavior and specific examples  
    assertUnset.accept(unset._sub(i1));
    assertUnset.accept(i0._sub(unset));
    assertEquals(iMinus1, i0._sub(i1));
    assertEquals(i0, i1._sub(i1));
    assertEquals(i0, iMinus1._sub(iMinus1));

    //Multiplication - test unset behavior and specific examples
    assertUnset.accept(unset._mul(i1));
    assertUnset.accept(i0._mul(unset));
    assertEquals(i0, i0._mul(i1));
    assertEquals(i0, i0._mul(iMinus1));
    assertEquals(i2, i1._mul(i2));
    assertEquals(iMinus2, i2._mul(iMinus1));
    assertEquals(i4, i2._mul(i2));
    assertEquals(i4, iMinus2._mul(iMinus2));

    //Division - test unset behavior and specific examples
    assertUnset.accept(i0._div(unset));
    assertUnset.accept(unset._div(i2));
    assertEquals(i1, i2._div(i2));
    assertEquals(i2, i4._div(i2));
    assertEquals(iMinus2, i4._div(iMinus2));
    assertEquals(i0, i0._div(i2));

    // Special division cases (divide by zero)
    assertUnset.accept(i0._div(i0));

  }

  @Test
  void testFloatingPointMathematics() {

    //Sqrt - test unset behavior
    assertUnset.accept(unset._sqrt());
    assertEquals(i2._promote(), i4._sqrt());
    assertUnset.accept(i0._sqrt());

    //Pow - test unset behavior for both Integer and Float variants
    assertUnset.accept(i2._pow(unset));
    assertUnset.accept(i2._pow(unset._promote()));
    assertEquals(i2._promote(), i2._pow(i1));
    assertEquals(i4._promote(), i2._pow(i2));
    assertEquals(i2._promote(), i2._pow(i1._promote()));
    assertEquals(i4._promote(), i2._pow(i2._promote()));

    //Addition with Float - test unset behavior
    assertUnset.accept(unset._add(i1._promote()));
    assertUnset.accept(i0._add(unset._promote()));
    assertEquals(iMinus1._promote(), i0._add(iMinus1._promote()));
    assertEquals(i0._promote(), i1._add(iMinus1._promote()));
    assertEquals(i0._promote(), iMinus1._add(i1._promote()));

    //Specific calculations
    assertEquals(Float._of(2.456), i1._add(Float._of(1.456)));

    assertEquals(Float._of(-0.3999999999999999), i1._sub(Float._of(1.400)));
    assertUnset.accept(i0._sub(unset._promote()));

    assertEquals(Float._of(2.8), i2._mul(Float._of(1.400)));
    assertUnset.accept(i0._mul(unset._promote()));

    assertEquals(Float._of(0.9955201592832256), i2._div(Float._of(2.0090)));
    assertEquals(Float._of(1.9999999999999998E15), i2._div(Float._of(0.000000000000001)));
    assertUnset.accept(i0._div(unset._promote()));

    //Check division of small numbers by smaller numbers.
    assertEquals(Float._of(1.0019913530064882E122), Float._of(10E-200)._div(Float._of(10E-322)));
    //Check loss of precision leading to infinity.
    assertUnset.accept(i2._div(Float._of(10E-322)));

    //Divide by zero check
    assertUnset.accept(i2._div(i0._promote()));
  }

  @Test
  void testAsString() {
    // Test string conversion with unset values
    assertUnset.accept(unset._string());

    assertEquals(String._of("0"), i0._string());
    assertEquals(String._of("1"), i1._string());
    assertEquals(String._of("-1"), iMinus1._string());

    assertEquals("1", i1.toString());
  }

  @Test
  void testAsJson() {
    // Test JSON conversion with set values
    final var zeroJson = i0._json();
    assertNotNull(zeroJson);
    assertSet.accept(zeroJson);
    
    final var oneJson = i1._json();
    assertSet.accept(oneJson);
    
    final var minusOneJson = iMinus1._json();
    assertSet.accept(minusOneJson);
    
    // Test JSON conversion with unset value
    assertUnset.accept(unset._json());
  }

  @Test
  void testHashCode() {
    // Test hash code with unset values
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
    // Test prefix/suffix operations with unset values
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
    // Test absolute value with unset and specific examples
    assertUnset.accept(unset._abs());
    assertEquals(i0, i0._abs());
    assertEquals(i2, i2._abs());
    assertEquals(i2, iMinus2._abs());

    // Test factorial with unset and specific examples
    assertEquals(Integer._of(24L), i4._fac());
    //Not logical to be able to get the factorial of a negative number.
    assertUnset.accept(iMinus2._fac());
    assertEquals(i1, i0._fac());
    assertUnset.accept(unset._fac());
  }

  @Test
  void testUtilityOperators() {
    // Test utility operations with unset values
    assertUnset.accept(unset._empty());
    assertUnset.accept(unset._len());

    assertEquals(true1, i0._empty());
    assertEquals(false1, i1._empty());
    assertEquals(false1, iMinus2._empty());

    assertEquals(i1, i0._len());
    assertEquals(i1, i1._len());
    //The length includes the minus sign.
    assertEquals(i2, iMinus1._len());

    assertEquals(i4, Integer._of(1234)._len());
  }

  @Test
  void testBitWiseOnIntegers() {
    // Test bitwise operations with unset values
    assertUnset.accept(unset._and(i1));
    assertUnset.accept(unset._or(i1));
    assertUnset.accept(unset._xor(i1));

    assertUnset.accept(i1._and(unset));
    assertUnset.accept(i1._or(unset));
    assertUnset.accept(i1._xor(unset));

    assertEquals(i3, i1._or(i2));
    assertEquals(i3, i3._or(i2));

    assertEquals(i2, i3._and(i2));
    assertEquals(i0, i1._and(i2));

    assertEquals(i1, i3._xor(i2));
    assertEquals(i4._negate(), iMinus2._xor(i2));
    assertEquals(iMinus1, i2._xor(i3._negate()));
  }

  @Test
  void testModulusAndRemainder() {
    // Test modulus and remainder operations with unset values
    assertUnset.accept(unset._mod(i1));
    assertUnset.accept(unset._rem(i1));

    assertUnset.accept(i1._mod(unset));
    assertUnset.accept(i1._rem(unset));

    assertEquals(i0, i1._rem(i1));
    assertEquals(i0, i1._mod(i1));

    //Firstly the negative case
    final var charlie1 = Integer._of(-21);
    //Remainder

    assertEquals(iMinus1, charlie1._rem(i4));
    assertEquals(iMinus1, charlie1._rem(i4._negate()));
    //Modulus

    assertEquals(i3, charlie1._mod(i4));
    assertEquals(iMinus1, charlie1._mod(i4._negate()));

    final var charlie2 = Integer._of(21);

    //Remainder
    assertEquals(i1, charlie2._rem(i4));
    assertEquals(i1, charlie2._rem(i4._negate()));
    //Modulus
    assertEquals(i1, charlie2._mod(i4));
    assertEquals(i3._negate(), charlie2._mod(i4._negate()));

  }
  //Additional tests

  /**
   * Example of Remainder:
   * <br/>
   * 10 % 3 = 1 [here divisible is 10 which is positively signed so the result will also be positively signed]
   * <br/>
   * -10 % 3 = -1 [here divisible is -10 which is negatively signed so the result will also be negatively signed]
   * <br/>
   * 10 % -3 = 1 [here divisible is 10 which is positively signed so the result will also be positively signed]
   * <br/>
   * -10 % -3 = -1 [here divisible is -10 which is negatively signed so the result will also be negatively signed]
   * <br/>
   * Example of Modulus:
   * <br/>
   * 5 % 3 = 2 [here divisible is 5 which is positively signed so the remainder will also be positively
   * signed and the divisor is also positively signed. As both remainder and divisor are of same sign the
   * result will be same as remainder]
   * <br/>
   * -5 % 3 = 1 [here divisible is -5 which is negatively signed so the remainder will also be negatively
   * signed and the divisor is positively signed. As both remainder and divisor are of opposite sign the
   * result will be sum of remainder and divisor -2 + 3 = 1]
   * <br/>
   * 5 % -3 = -1 [here divisible is 5 which is positively signed so the remainder will also be positively
   * signed and the divisor is negatively signed. As both remainder and divisor are of opposite sign the
   * result will be sum of remainder and divisor 2 + -3 = -1]
   * <br/>
   * -5 % -3 = -2 [here divisible is -5 which is negatively signed so the remainder will also be negatively
   * signed and the divisor is also negatively signed. As both remainder and divisor are of same sign the
   * result will be same as remainder]
   */
  @Test
  void testModulusAndRemainderByHand() {
    final var ten = Integer._of(10);
    final var minusTen = Integer._of(-10);
    final var one = Integer._of(1);
    final var minusOne = Integer._of(-1);
    final var two = Integer._of(2);
    final var minusTwo = Integer._of(-2);
    final var three = Integer._of(3);
    final var minusThree = Integer._of(-3);
    final var five = Integer._of(5);
    final var minusFive = Integer._of(-5);

    //Remainder tests from above
    assertEquals(one, ten._rem(three));
    assertEquals(minusOne, minusTen._rem(three));
    assertEquals(one, ten._rem(minusThree));
    assertEquals(minusOne, minusTen._rem(minusThree));

    //Now Modulus
    assertEquals(two, five._mod(three));
    assertEquals(one, minusFive._mod(three));
    assertEquals(minusOne, five._mod(minusThree));
    assertEquals(minusTwo, minusFive._mod(minusThree));
  }

  @Test
  void testPipeLogic() {

    var mutatedValue = new Integer();
    assertUnset.accept(mutatedValue);

    mutatedValue._pipe(unset);
    assertUnset.accept(mutatedValue);

    mutatedValue._pipe(i1);
    assertEquals(i1, mutatedValue);

    //basically just keep adding
    mutatedValue._pipe(i2);
    assertEquals(i3, mutatedValue);

    //Even if we pipe in something unset, for pipes this is ignored.
    //This is the main different over addAssign. With that the result becomes unset.
    //But with pipes you can pipe anything in.
    mutatedValue._pipe(unset);
    assertEquals(i3, mutatedValue);

    //Now just show a negative being added.
    mutatedValue._pipe(i4._negate());
    assertEquals(iMinus1, mutatedValue);
  }

  private List<Consumer<Integer>> getIntegerAssignmentOperations(final Integer from) {
    return List.of(from::_addAss, from::_subAss, from::_mulAss, from::_divAss);
  }

  @Test
  void testMutationOperatorsWithUnsetInteger() {

    final var mutatedValue = Integer._of(0);

    for (var operator : getIntegerAssignmentOperations(mutatedValue)) {
      operator.accept(new Integer());
      assertUnset.accept(mutatedValue);
      //Now set it back again for next time around loop.
      mutatedValue._copy(i0);
      assertEquals(i0, mutatedValue);
    }
  }

  @Test
  void testMutationOperators() {

    final var mutatedValue = Integer._of(0);
    mutatedValue._addAss(i1);
    assertEquals(i1, mutatedValue);

    mutatedValue._mulAss(i4);
    assertEquals(i4, mutatedValue);

    mutatedValue._divAss(i2);
    assertEquals(i2, mutatedValue);

    mutatedValue._subAss(i2);
    assertEquals(i0, mutatedValue);
  }


  @Test
  void testReplaceAndCopyLogic() {

    var mutatedValue = Integer._of(0L);
    assertEquals(i0, mutatedValue);

    mutatedValue._replace(i1);
    assertEquals(i1, mutatedValue);

    mutatedValue._replace(i2);
    assertEquals(i2, mutatedValue);

    mutatedValue._replace(unset);
    assertUnset.accept(mutatedValue);

    //Now just check that it can take a value after being unset
    mutatedValue._replace(i4);
    assertEquals(i4, mutatedValue);
  }

}
