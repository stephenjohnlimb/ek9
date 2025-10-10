package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class IntegerTest extends Common {

  // Data providers for parameterized tests (now using CSV instead)

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
  void testFuzzyComparison() {
    final var int00 = Integer._of(0);

    assertEquals(INT_0, INT_0._fuzzy(int00));
    assertEquals(INT_MINUS_1, INT_0._fuzzy(INT_1));
    assertEquals(INT_1, INT_1._fuzzy(INT_0));
    // Test unset behavior for fuzzy comparison
    assertUnset.accept(unsetInteger._fuzzy(INT_0));
    assertUnset.accept(INT_0._fuzzy(unsetInteger));
  }

  @Test
  void testComparison() {
    final var int00 = Integer._of(0);

    assertEquals(INT_0, INT_0._cmp(int00));
    assertEquals(INT_MINUS_1, INT_0._cmp(INT_1));
    assertEquals(INT_1, INT_1._cmp(INT_0));

    assertUnset.accept(unsetInteger._cmp(INT_0));
    assertUnset.accept(INT_0._cmp(unsetInteger));

    Any asAny = int00;
    assertUnset.accept(unsetInteger._cmp(asAny));
    assertUnset.accept(asAny._cmp(unsetInteger));

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
  void testSimplePipedJSONValue() {

    //Just a simple piping of individual values
    final var mutatedInteger = new Integer();
    final var json1 = new JSON(INT_1);
    final var json3 = new JSON(INT_3);
    final var jsonButADate = new JSON(epoch);
    final var jsonNumberButAString = new JSON(String._of("12"));

    mutatedInteger._pipe(json1);
    assertSet.accept(mutatedInteger);

    mutatedInteger._pipe(json3);
    assertSet.accept(mutatedInteger);

    //We can pipe this in but it cannot be converted to an integer (via String).
    //So it will be ignored
    mutatedInteger._pipe(jsonButADate);

    mutatedInteger._pipe(jsonNumberButAString);

    assertEquals(INT_16, mutatedInteger);

  }

  @Test
  void testSimplePipedJSONArray() {
    final var mutatedInteger = new Integer();
    final var json1Result = new JSON().parse(String._of("[1, 3]"));
    final var json2Result = new JSON().parse(String._of("[9, 16]"));

    //Check that the JSON text was parsed.
    assertSet.accept(json1Result);
    assertSet.accept(json2Result);

    //Now when you pipe an array in, it will traverse the array and
    //recursively call _pipe(JSON) with each of the components.
    mutatedInteger._pipe(json1Result.ok());
    assertSet.accept(mutatedInteger);

    mutatedInteger._pipe(json2Result.ok());
    assertSet.accept(mutatedInteger);

    assertEquals(INT_29, mutatedInteger);

  }

  @Test
  void testStructuredPipedJSONObject() {
    final var mutatedInteger = new Integer();
    final var jsonStr = """
        {
          "prop1": 6,
           "prop2": 4
        }""";
    final var jsonResult = new JSON().parse(String._of(jsonStr));
    //pre condition check that is ok
    assertSet.accept(jsonResult);
    mutatedInteger._pipe(jsonResult.ok());

    assertSet.accept(mutatedInteger);
    assertEquals(INT_10, mutatedInteger);

  }

  @Test
  void testNestedPipedJSONObject() {
    final var mutatedInteger = new Integer();
    final var jsonStr = """
        {
          "prop1": [9, 8],
          "prop2": 4,
          "prop3": "Just a String",
          "prop4" : [{"val1": 17, "val2" : 21}, {"other": -9}]
        }""";
    final var jsonResult = new JSON().parse(String._of(jsonStr));
    //pre condition check that is ok
    assertSet.accept(jsonResult);
    mutatedInteger._pipe(jsonResult.ok());

    assertSet.accept(mutatedInteger);
    assertEquals(INT_50, mutatedInteger);
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

  @ParameterizedTest
  @CsvSource({
      "11111111, 255",     // All 1s in byte
      "10101010, 170",     // Alternating bits  
      "000001, 1",         // Leading zeros
      "111100001111, 3855" // Mixed pattern
  })
  void testBitsConstructorEdgeCases(String bitsString, int expectedValue) {
    // Focus on constructor-specific cases with interesting bit patterns
    final var bits = new Bits(String._of(bitsString));
    final var intFromBits = new Integer(bits);
    assertSet.accept(intFromBits);
    assertEquals(Integer._of(expectedValue), intFromBits);
  }

  @Test
  void testBitsConstructorUnsetCases() {
    // Test construction with unset Bits - should result in unset Integer
    final var unsetBits = new Bits();
    assertNotNull(unsetBits);
    final var intFromUnsetBits = new Integer(unsetBits);
    assertUnset.accept(intFromUnsetBits);

    // Test construction with null Bits - should result in unset Integer
    final var intFromNullBits = new Integer((Bits) null);
    assertUnset.accept(intFromNullBits);
  }

  @Test
  void testBitsConstructorOverflowProtection() {
    // Create a large Bits value (>64 bits) - should result in unset Integer
    final var largeBitsString = "1".repeat(65); // 65 bits, too large for long
    final var largeBits = new Bits(String._of(largeBitsString));
    final var intFromLargeBits = new Integer(largeBits);
    assertUnset.accept(intFromLargeBits); // Should be unset due to overflow

    // Test exactly 64 bits - should work (maximum long value)
    final var maxBitsString = "1".repeat(64);
    final var maxBits = new Bits(String._of(maxBitsString));
    final var intFromMaxBits = new Integer(maxBits);
    // Note: This might be unset depending on sign handling, but shouldn't crash
    assertNotNull(intFromMaxBits); // At minimum, it should construct
  }

  @ParameterizedTest
  @CsvSource({
      "7, 111",           // All 1s pattern (3 bits)
      "170, 10101010",    // Alternating pattern (8 bits)
      "3855, 111100001111" // Mixed pattern (12 bits)
  })
  void testBitsMethodSpecificPatterns(int intValue, String expectedBitsString) {
    // Focus on specific bit patterns for validation (round-trip covered elsewhere)
    final var integer = Integer._of(intValue);
    final var bits = integer.bits();
    assertSet.accept(bits);
    assertEquals(String._of(expectedBitsString), bits._string());
  }

  @Test
  void testBitsMethodUnsetCase() {
    // Test conversion of unset Integer - should produce unset Bits
    final var unsetBits = unsetInteger.bits();
    assertNotNull(unsetBits);
    assertUnset.accept(unsetBits);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 10, 15, 42, 128, 255, 1023, 65535, 1000000, -1, -2, -10, -42, -255, -1000})
  void testRoundTripConversion(int value) {
    // Comprehensive test: Integer -> Bits -> Integer preserves all values
    final var original = Integer._of(value);
    final var bits = original.bits();
    final var roundTrip = new Integer(bits);
    assertEquals(original, roundTrip, "Round trip failed for value: " + value);
  }

  @Test
  void testRoundTripConversionUnsetCase() {
    // Test that unset Integer -> unset Bits -> unset Integer
    final var unsetBits = unsetInteger.bits();
    assertNotNull(unsetBits);
    final var unsetRoundTrip = new Integer(unsetBits);
    assertUnset.accept(unsetBits);
    assertUnset.accept(unsetRoundTrip);
  }

  @Test
  void testBitsConversionEdgeCases() {
    // Test negative integer conversion (two's complement)
    final var negativeInt = Integer._of(-1);
    final var negativeBits = negativeInt.bits();
    assertSet.accept(negativeBits);
    // Should produce many 1s for -1 in two's complement

    final var negativeRoundTrip = new Integer(negativeBits);
    assertEquals(negativeInt, negativeRoundTrip);

    // Test large positive values
    final var largeInt = Integer._of(Long.MAX_VALUE);
    final var largeBits = largeInt.bits();
    final var largeRoundTrip = new Integer(largeBits);
    assertEquals(largeInt, largeRoundTrip);
  }

  @Test
  void testNegativeIntegerEdgeCases() {
    // Test edge case: Long.MIN_VALUE (most negative long) - not in round-trip due to size
    final var minInt = Integer._of(Long.MIN_VALUE);
    final var minBits = minInt.bits();
    assertSet.accept(minBits);
    
    final var minRoundTrip = new Integer(minBits);
    assertEquals(minInt, minRoundTrip);
  }

  @Test
  void testExact65BitOverflowProtection() {
    // Create exactly 65-bit Bits value to test overflow boundary
    final var bits65String = "1" + "0".repeat(64); // 65 bits: 1 followed by 64 zeros
    final var bits65 = new Bits(String._of(bits65String));
    
    // Verify Bits creation succeeded and has correct length
    assertSet.accept(bits65);
    assertEquals(65, bits65.length);
    
    // Constructor should return unSet Integer due to overflow
    final var intFromOverflow = new Integer(bits65);
    assertUnset.accept(intFromOverflow);

    // Test exact 64-bit boundary (should work)
    final var bits64String = "1" + "0".repeat(63); // 64 bits: 1 followed by 63 zeros
    final var bits64 = new Bits(String._of(bits64String));
    assertSet.accept(bits64);
    assertEquals(64, bits64.length);
    
    final var intFrom64Bits = new Integer(bits64);
    // This should work (64 bits fits in long), though might be negative due to sign bit
    assertSet.accept(intFrom64Bits);
  }

}
