package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class IntegerTest extends Common {

  // Helper methods for eliminating duplication

  /**
   * Helper method to test comparison operations with unset values.
   */
  private void assertComparisonOperatorsWithUnset(Integer validValue) {
    // Test all comparison operators with unset
    assertUnset.accept(validValue._eq(unsetInteger));
    assertUnset.accept(unsetInteger._eq(validValue));
    assertUnset.accept(unsetInteger._eq(unsetInteger));

    assertUnset.accept(validValue._neq(unsetInteger));
    assertUnset.accept(unsetInteger._neq(validValue));
    assertUnset.accept(unsetInteger._neq(unsetInteger));

    assertUnset.accept(validValue._lt(unsetInteger));
    assertUnset.accept(unsetInteger._lt(validValue));

    assertUnset.accept(validValue._gt(unsetInteger));
    assertUnset.accept(unsetInteger._gt(validValue));

    assertUnset.accept(validValue._lteq(unsetInteger));
    assertUnset.accept(unsetInteger._lteq(validValue));

    assertUnset.accept(validValue._gteq(unsetInteger));
    assertUnset.accept(unsetInteger._gteq(validValue));
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

    assertSet.accept(INT_1);
    assertEquals(1, INT_1.state);

    assertSet.accept(INT_2);
    assertEquals(2, INT_2.state);

    assertSet.accept(INT_3);
    assertEquals(3L, INT_3.state);

    final var also3 = Integer._of(INT_3);
    assertSet.accept(also3);
    assertEquals(3L, also3.state);

    final var again3 = new Integer(INT_3);
    assertSet.accept(again3);
    assertEquals(3L, again3.state);

    final var check4 = new Integer(INT_4);
    assertSet.accept(check4);
    assertEquals(INT_4, check4);
  }

  @Test
  void testEquality() {
    final var intZero = Integer._of(0);

    // Test all comparison operators with unset values using helper
    assertComparisonOperatorsWithUnset(INT_0);

    //Eq
    assertEquals(intZero, INT_0);
    assertEquals(trueBoolean, INT_0._eq(intZero));

    //Neq
    assertEquals(falseBoolean, INT_0._neq(intZero));

    //Lt
    assertTrue.accept(INT_0._lt(INT_1));
    assertFalse.accept(INT_1._lt(INT_0));

    //gt
    assertTrue.accept(INT_1._gt(INT_0));
    assertFalse.accept(INT_0._gt(INT_1));

    //Lteq
    assertTrue.accept(INT_0._lteq(intZero));
    assertTrue.accept(INT_0._lteq(INT_1));
    assertFalse.accept(INT_1._lteq(INT_0));

    //Gteq
    assertTrue.accept(INT_0._gteq(intZero));
    assertTrue.accept(INT_1._gteq(INT_0));
    assertFalse.accept(INT_0._gteq(INT_1));
  }

  @Test
  void testComparison() {
    final var int00 = Integer._of(0);

    assertEquals(INT_0, INT_0._fuzzy(int00));
    assertEquals(INT_MINUS_1, INT_0._fuzzy(INT_1));
    assertEquals(INT_1, INT_1._fuzzy(INT_0));
    // Test unset behavior for fuzzy comparison
    assertUnset.accept(unsetInteger._fuzzy(INT_0));
    assertUnset.accept(INT_0._fuzzy(unsetInteger));
  }

  @Test
  void testIsSet() {
    assertNotNull(unsetInteger);
    assertFalse.accept(unsetInteger._isSet());

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
    assertUnset.accept(unsetInteger._negate());
    assertUnset.accept(unsetInteger._inc());
    assertUnset.accept(unsetInteger._dec());

    final var minusZero = INT_0._negate();
    assertEquals(INT_0, minusZero);

    //Negate
    assertEquals(INT_MINUS_1, INT_1._negate());

    // Create fresh objects for mutating inc/dec operations to avoid corrupting constants
    final var freshI1 = Integer._of(1);
    assertEquals(INT_2, freshI1._inc());

    final var freshI0 = Integer._of(0);
    assertEquals(INT_MINUS_1, freshI0._dec());

    //Addition - test unset behavior and specific examples
    assertUnset.accept(unsetInteger._add(INT_1));
    assertUnset.accept(INT_0._add(unsetInteger));
    assertEquals(INT_MINUS_1, INT_0._add(INT_MINUS_1));
    assertEquals(INT_0, INT_1._add(INT_MINUS_1));
    assertEquals(INT_0, INT_MINUS_1._add(INT_1));

    //Subtraction - test unset behavior and specific examples  
    assertUnset.accept(unsetInteger._sub(INT_1));
    assertUnset.accept(INT_0._sub(unsetInteger));
    assertEquals(INT_MINUS_1, INT_0._sub(INT_1));
    assertEquals(INT_0, INT_1._sub(INT_1));
    assertEquals(INT_0, INT_MINUS_1._sub(INT_MINUS_1));

    //Multiplication - test unset behavior and specific examples
    assertUnset.accept(unsetInteger._mul(INT_1));
    assertUnset.accept(INT_0._mul(unsetInteger));
    assertEquals(INT_0, INT_0._mul(INT_1));
    assertEquals(INT_0, INT_0._mul(INT_MINUS_1));
    assertEquals(INT_2, INT_1._mul(INT_2));
    assertEquals(INT_MINUS_2, INT_2._mul(INT_MINUS_1));
    assertEquals(INT_4, INT_2._mul(INT_2));
    assertEquals(INT_4, INT_MINUS_2._mul(INT_MINUS_2));

    //Division - test unset behavior and specific examples
    assertUnset.accept(INT_0._div(unsetInteger));
    assertUnset.accept(unsetInteger._div(INT_2));
    assertEquals(INT_1, INT_2._div(INT_2));
    assertEquals(INT_2, INT_4._div(INT_2));
    assertEquals(INT_MINUS_2, INT_4._div(INT_MINUS_2));
    assertEquals(INT_0, INT_0._div(INT_2));

    // Special division cases (divide by zero)
    assertUnset.accept(INT_0._div(INT_0));

  }

  @Test
  void testFloatingPointMathematics() {

    //Sqrt - test unset behavior
    assertUnset.accept(unsetInteger._sqrt());
    assertEquals(INT_2._promote(), INT_4._sqrt());
    assertUnset.accept(INT_0._sqrt());

    //Pow - test unset behavior for both Integer and Float variants
    assertUnset.accept(INT_2._pow(unsetInteger));
    assertUnset.accept(INT_2._pow(unsetInteger._promote()));
    assertEquals(INT_2._promote(), INT_2._pow(INT_1));
    assertEquals(INT_4._promote(), INT_2._pow(INT_2));
    assertEquals(INT_2._promote(), INT_2._pow(INT_1._promote()));
    assertEquals(INT_4._promote(), INT_2._pow(INT_2._promote()));

    //Addition with Float - test unset behavior
    assertUnset.accept(unsetInteger._add(INT_1._promote()));
    assertUnset.accept(INT_0._add(unsetInteger._promote()));
    assertEquals(INT_MINUS_1._promote(), INT_0._add(INT_MINUS_1._promote()));
    assertEquals(INT_0._promote(), INT_1._add(INT_MINUS_1._promote()));
    assertEquals(INT_0._promote(), INT_MINUS_1._add(INT_1._promote()));

    //Specific calculations
    assertEquals(Float._of(2.456), INT_1._add(Float._of(1.456)));

    assertEquals(Float._of(-0.3999999999999999), INT_1._sub(Float._of(1.400)));
    assertUnset.accept(INT_0._sub(unsetInteger._promote()));

    assertEquals(Float._of(2.8), INT_2._mul(Float._of(1.400)));
    assertUnset.accept(INT_0._mul(unsetInteger._promote()));

    assertEquals(Float._of(0.9955201592832256), INT_2._div(Float._of(2.0090)));
    assertEquals(Float._of(1.9999999999999998E15), INT_2._div(Float._of(0.000000000000001)));
    assertUnset.accept(INT_0._div(unsetInteger._promote()));

    //Check division of small numbers by smaller numbers.
    assertEquals(Float._of(1.0019913530064882E122), Float._of(10E-200)._div(Float._of(10E-322)));
    //Check loss of precision leading to infinity.
    assertUnset.accept(INT_2._div(Float._of(10E-322)));

    //Divide by zero check
    assertUnset.accept(INT_2._div(INT_0._promote()));
  }

  @Test
  void testAsString() {
    // Test string conversion with unset values
    assertUnset.accept(unsetInteger._string());

    assertEquals(String._of("0"), INT_0._string());
    assertEquals(String._of("1"), INT_1._string());
    assertEquals(String._of("-1"), INT_MINUS_1._string());

    assertEquals("1", INT_1.toString());
  }

  @Test
  void testAsJson() {
    // Test JSON conversion with set values
    final var zeroJson = INT_0._json();
    assertNotNull(zeroJson);
    assertSet.accept(zeroJson);
    
    final var oneJson = INT_1._json();
    assertSet.accept(oneJson);
    
    final var minusOneJson = INT_MINUS_1._json();
    assertSet.accept(minusOneJson);
    
    // Test JSON conversion with unset value
    assertUnset.accept(unsetInteger._json());
  }

  @Test
  void testHashCode() {
    // Test hash code with unset values
    assertUnset.accept(unsetInteger._hashcode());
    assertEquals(INT_0._hashcode(), INT_0._hashcode());
    assertNotEquals(INT_0._hashcode(), INT_1._hashcode());
    assertNotEquals(INT_MINUS_1._hashcode(), INT_1._hashcode());
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
    assertUnset.accept(unsetInteger._prefix());
    assertUnset.accept(unsetInteger._suffix());

    assertEquals(INT_0, INT_0._prefix());
    assertEquals(INT_0, INT_0._suffix());

    final var example = Integer._of(1234);
    assertEquals(INT_1, example._prefix());
    assertEquals(INT_4, example._suffix());

    final var minusExample = example._negate();
    assertEquals(INT_MINUS_1, minusExample._prefix());
    assertEquals(INT_4._negate(), minusExample._suffix());
  }

  @Test
  void testAdditionalMathematicalOperators() {
    // Test absolute value with unset and specific examples
    assertUnset.accept(unsetInteger._abs());
    assertEquals(INT_0, INT_0._abs());
    assertEquals(INT_2, INT_2._abs());
    assertEquals(INT_2, INT_MINUS_2._abs());

    // Test factorial with unset and specific examples
    assertEquals(Integer._of(24L), INT_4._fac());
    //Not logical to be able to get the factorial of a negative number.
    assertUnset.accept(INT_MINUS_2._fac());
    assertEquals(INT_1, INT_0._fac());
    assertUnset.accept(unsetInteger._fac());
  }

  @Test
  void testUtilityOperators() {
    // Test utility operations with unset values
    assertUnset.accept(unsetInteger._empty());
    assertUnset.accept(unsetInteger._len());

    assertEquals(trueBoolean, INT_0._empty());
    assertEquals(falseBoolean, INT_1._empty());
    assertEquals(falseBoolean, INT_MINUS_2._empty());

    assertEquals(INT_1, INT_0._len());
    assertEquals(INT_1, INT_1._len());
    //The length includes the minus sign.
    assertEquals(INT_2, INT_MINUS_1._len());

    assertEquals(INT_4, Integer._of(1234)._len());
  }

  @Test
  void testBitWiseOnIntegers() {
    // Test bitwise operations with unset values
    assertUnset.accept(unsetInteger._and(INT_1));
    assertUnset.accept(unsetInteger._or(INT_1));
    assertUnset.accept(unsetInteger._xor(INT_1));

    assertUnset.accept(INT_1._and(unsetInteger));
    assertUnset.accept(INT_1._or(unsetInteger));
    assertUnset.accept(INT_1._xor(unsetInteger));

    assertEquals(INT_3, INT_1._or(INT_2));
    assertEquals(INT_3, INT_3._or(INT_2));

    assertEquals(INT_2, INT_3._and(INT_2));
    assertEquals(INT_0, INT_1._and(INT_2));

    assertEquals(INT_1, INT_3._xor(INT_2));
    assertEquals(INT_4._negate(), INT_MINUS_2._xor(INT_2));
    assertEquals(INT_MINUS_1, INT_2._xor(INT_3._negate()));
  }

  @Test
  void testModulusAndRemainder() {
    // Test modulus and remainder operations with unset values
    assertUnset.accept(unsetInteger._mod(INT_1));
    assertUnset.accept(unsetInteger._rem(INT_1));

    assertUnset.accept(INT_1._mod(unsetInteger));
    assertUnset.accept(INT_1._rem(unsetInteger));

    assertEquals(INT_0, INT_1._rem(INT_1));
    assertEquals(INT_0, INT_1._mod(INT_1));

    //Firstly the negative case
    final var charlie1 = Integer._of(-21);
    //Remainder

    assertEquals(INT_MINUS_1, charlie1._rem(INT_4));
    assertEquals(INT_MINUS_1, charlie1._rem(INT_4._negate()));
    //Modulus

    assertEquals(INT_3, charlie1._mod(INT_4));
    assertEquals(INT_MINUS_1, charlie1._mod(INT_4._negate()));

    final var charlie2 = Integer._of(21);

    //Remainder
    assertEquals(INT_1, charlie2._rem(INT_4));
    assertEquals(INT_1, charlie2._rem(INT_4._negate()));
    //Modulus
    assertEquals(INT_1, charlie2._mod(INT_4));
    assertEquals(INT_3._negate(), charlie2._mod(INT_4._negate()));

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

    mutatedValue._pipe(unsetInteger);
    assertUnset.accept(mutatedValue);

    mutatedValue._pipe(INT_1);
    assertEquals(INT_1, mutatedValue);

    //basically just keep adding
    mutatedValue._pipe(INT_2);
    assertEquals(INT_3, mutatedValue);

    //Even if we pipe in something unset, for pipes this is ignored.
    //This is the main different over addAssign. With that the result becomes unset.
    //But with pipes you can pipe anything in.
    mutatedValue._pipe(unsetInteger);
    assertEquals(INT_3, mutatedValue);

    //Now just show a negative being added.
    mutatedValue._pipe(INT_4._negate());
    assertEquals(INT_MINUS_1, mutatedValue);
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
      mutatedValue._copy(INT_0);
      assertEquals(INT_0, mutatedValue);
    }
  }

  @Test
  void testMutationOperators() {

    final var mutatedValue = Integer._of(0);
    mutatedValue._addAss(INT_1);
    assertEquals(INT_1, mutatedValue);

    mutatedValue._mulAss(INT_4);
    assertEquals(INT_4, mutatedValue);

    mutatedValue._divAss(INT_2);
    assertEquals(INT_2, mutatedValue);

    mutatedValue._subAss(INT_2);
    assertEquals(INT_0, mutatedValue);
  }


  @Test
  void testReplaceAndCopyLogic() {

    var mutatedValue = Integer._of(0L);
    assertEquals(INT_0, mutatedValue);

    mutatedValue._replace(INT_1);
    assertEquals(INT_1, mutatedValue);

    mutatedValue._replace(INT_2);
    assertEquals(INT_2, mutatedValue);

    mutatedValue._replace(unsetInteger);
    assertUnset.accept(mutatedValue);

    //Now just check that it can take a value after being unset
    mutatedValue._replace(INT_4);
    assertEquals(INT_4, mutatedValue);
  }

}
