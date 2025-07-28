package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class FloatTest extends Common {

  // Use true1, false1, and Float constants from Common base class

  // Helper methods for eliminating duplication

  /**
   * Helper method to test comparison operations with unsetFloat values.
   */
  private void assertComparisonOperatorsWithUnset(Float validValue) {
    // Test all comparison operators with unsetFloat
    assertUnset.accept(validValue._eq(unsetFloat));
    assertUnset.accept(unsetFloat._eq(validValue));
    assertUnset.accept(unsetFloat._eq(unsetFloat));
    
    assertUnset.accept(validValue._neq(unsetFloat));
    assertUnset.accept(unsetFloat._neq(validValue));
    assertUnset.accept(unsetFloat._neq(unsetFloat));
    
    assertUnset.accept(validValue._lt(unsetFloat));
    assertUnset.accept(unsetFloat._lt(validValue));
    
    assertUnset.accept(validValue._gt(unsetFloat));
    assertUnset.accept(unsetFloat._gt(validValue));
    
    assertUnset.accept(validValue._lteq(unsetFloat));
    assertUnset.accept(unsetFloat._lteq(validValue));
    
    assertUnset.accept(validValue._gteq(unsetFloat));
    assertUnset.accept(unsetFloat._gteq(validValue));
  }

  @Test
  void testConstruction() {
    final var defaultConstructor = new Float();
    assertUnset.accept(defaultConstructor);

    final var unset1 = Float._of("not parsable");
    assertUnset.accept(unset1);

    final var unset2 = Float._of((java.lang.String) null);
    assertUnset.accept(unset2);

    final var unset3 = new Float(new String());
    assertUnset.accept(unset3);

    final var checkZero1 = new Float(String._of("0"));
    assertEquals(0.0, checkZero1.state);

    final var checkZero2 = new Float(String._of("0.0"));
    assertEquals(0.0, checkZero2.state);

    final var checkZero3 = Float._of(0f);
    assertEquals(0.0, checkZero3.state);

    final var checkZero4 = new Float(Integer._of("0"));
    assertEquals(0.0, checkZero4.state);

    assertSet.accept(FLOAT_1);
    assertEquals(1.0, FLOAT_1.state);

    assertSet.accept(FLOAT_2);
    assertEquals(2.0, FLOAT_2.state);

    assertSet.accept(FLOAT_3);
    assertEquals(3.0, FLOAT_3.state);

    final var also3 = Float._of(FLOAT_3);
    assertSet.accept(also3);
    assertEquals(3.0, also3.state);

    final var again3 = new Float(FLOAT_3);
    assertSet.accept(again3);
    assertEquals(3.0, again3.state);

    final var check4 = new Float(FLOAT_4);
    assertSet.accept(check4);
    assertEquals(FLOAT_4, check4);
  }

  @Test
  void testEquality() {
    final var f00 = Float._of(0);

    // Test all comparison operators with unsetFloat values using helper
    assertComparisonOperatorsWithUnset(FLOAT_0);

    //Eq
    assertEquals(f00, FLOAT_0);
    assertEquals(trueBoolean, FLOAT_0._eq(f00));

    //Neq
    assertEquals(falseBoolean, FLOAT_0._neq(f00));

    //Lt
    assertTrue.accept(FLOAT_0._lt(FLOAT_1));
    assertFalse.accept(FLOAT_1._lt(FLOAT_0));

    //gt
    assertTrue.accept(FLOAT_1._gt(FLOAT_0));
    assertFalse.accept(FLOAT_0._gt(FLOAT_1));

    //Lteq
    assertTrue.accept(FLOAT_0._lteq(f00));
    assertTrue.accept(FLOAT_0._lteq(FLOAT_1));
    assertFalse.accept(FLOAT_1._lteq(FLOAT_0));

    //Gteq
    assertTrue.accept(FLOAT_0._gteq(f00));
    assertTrue.accept(FLOAT_1._gteq(FLOAT_0));
    assertFalse.accept(FLOAT_0._gteq(FLOAT_1));
  }

  @Test
  void testComparison() {
    final var i00 = Float._of(0);

    assertEquals(INT_0, FLOAT_0._fuzzy(i00));
    assertEquals(INT_MINUS_1, FLOAT_0._fuzzy(FLOAT_1));
    assertEquals(INT_1, FLOAT_1._fuzzy(FLOAT_0));
    assertUnset.accept(unsetFloat._fuzzy(FLOAT_0));
    assertUnset.accept(FLOAT_0._fuzzy(unsetFloat));
    assertUnset.accept(FLOAT_0._cmp(new Any(){}));

  }

  @Test
  void testIsSet() {
    assertNotNull(unsetFloat);
    assertFalse.accept(unsetFloat._isSet());

    final var v1 = Float._of(90);
    assertNotNull(v1);
    assertTrue.accept(v1._isSet());

  }

  /**
   * Excludes the Float operators those are tested elsewhere.
   */
  @Test
  void testSimpleMathematics() {

    final var minusZero = FLOAT_0._negate();
    assertEquals(FLOAT_0, minusZero);

    final var stillUnset = unsetFloat._negate();
    assertUnset.accept(stillUnset);

    //Negate
    assertEquals(FLOAT_MINUS_1, FLOAT_1._negate());
    assertUnset.accept( unsetFloat._negate());

    //Addition
    assertEquals(FLOAT_MINUS_1, FLOAT_0._add(FLOAT_MINUS_1));
    assertEquals(FLOAT_0, FLOAT_1._add(FLOAT_MINUS_1));
    assertEquals(FLOAT_0, FLOAT_MINUS_1._add(FLOAT_1));
    assertUnset.accept(unsetFloat._add(FLOAT_1));
    assertUnset.accept(FLOAT_0._add(unsetFloat));

    //Substraction
    assertEquals(FLOAT_MINUS_1, FLOAT_0._sub(FLOAT_1));
    assertEquals(FLOAT_0, FLOAT_1._sub(FLOAT_1));
    assertEquals(FLOAT_0, FLOAT_MINUS_1._sub(FLOAT_MINUS_1));
    assertUnset.accept(unsetFloat._sub(FLOAT_1));
    assertUnset.accept(FLOAT_0._sub(unsetFloat));

    //Multiplication
    assertEquals(FLOAT_0, FLOAT_0._mul(FLOAT_1));
    assertEquals(FLOAT_0, FLOAT_0._mul(FLOAT_MINUS_1));
    assertEquals(FLOAT_2, FLOAT_1._mul(FLOAT_2));
    assertEquals(FLOAT_MINUS_2, FLOAT_2._mul(FLOAT_MINUS_1));
    assertEquals(FLOAT_4, FLOAT_2._mul(FLOAT_2));
    assertEquals(FLOAT_4, FLOAT_MINUS_2._mul(FLOAT_MINUS_2));
    assertUnset.accept(unsetFloat._mul(FLOAT_1));
    assertUnset.accept(FLOAT_0._mul(unsetFloat));

    //Division
    assertEquals(FLOAT_1, FLOAT_2._div(FLOAT_2));

    assertEquals(FLOAT_2, FLOAT_4._div(FLOAT_2));
    assertEquals(FLOAT_MINUS_2, FLOAT_4._div(FLOAT_MINUS_2));
    assertEquals(FLOAT_0, FLOAT_0._div(FLOAT_2));

    assertUnset.accept(FLOAT_0._div(FLOAT_0));
    assertUnset.accept( FLOAT_0._div(unsetFloat));
    assertUnset.accept(unsetFloat._div(FLOAT_2));

    // Create fresh objects for mutating inc/dec operations to avoid corrupting constants
    final var freshF1 = Float._of(1);
    assertEquals(FLOAT_2, freshF1._inc());
    assertUnset.accept(unsetFloat._inc());

    final var freshF0 = Float._of(0);
    assertEquals(FLOAT_MINUS_1, freshF0._dec());
    assertUnset.accept(unsetFloat._dec());

  }

  @Test
  void testIntegerCombinedFloatingPointMathematics() {

    assertEquals(Float._of(4.5f), FLOAT_4_5._add(INT_0));
    assertEquals(Float._of(5.5f), FLOAT_4_5._add(INT_1));
    assertUnset.accept(FLOAT_4_5._add(new Integer()));
    assertUnset.accept(unsetFloat._add(INT_0));


    assertEquals(Float._of(3.5f), FLOAT_4_5._sub(INT_1));
    assertEquals(Float._of(5.5f), FLOAT_4_5._sub(INT_MINUS_1));
    assertUnset.accept(FLOAT_4_5._sub(new Integer()));
    assertUnset.accept(unsetFloat._sub(INT_0));

    assertEquals(Float._of(9.0f), FLOAT_4_5._mul(INT_2));
    assertEquals(Float._of(-9.0f), FLOAT_4_5._mul(Integer._of(-2)));
    assertUnset.accept(FLOAT_4_5._mul(new Integer()));
    assertUnset.accept(unsetFloat._mul(INT_0));

    assertEquals(Float._of(2.25f), FLOAT_4_5._div(INT_2));
    assertEquals(Float._of(-2.25f), FLOAT_4_5._div(Integer._of(-2)));
    //Now try divide by zero and divide by unsetFloat
    assertUnset.accept(FLOAT_4_5._div(INT_0));
    assertUnset.accept(FLOAT_4_5._div(new Integer()));
    assertUnset.accept(unsetFloat._div(INT_0));

  }

  @Test
  void testFloatingPointMathematics() {

    //Sqrt
    assertUnset.accept(unsetFloat._sqrt());
    assertEquals(FLOAT_2, FLOAT_4._sqrt());
    assertUnset.accept(FLOAT_0._sqrt());

    //Pow
    assertUnset.accept(FLOAT_2._pow(unsetFloat));
    assertEquals(FLOAT_2, FLOAT_2._pow(FLOAT_1));
    assertEquals(FLOAT_4, FLOAT_2._pow(FLOAT_2));

    //Addition
    assertEquals(FLOAT_MINUS_1, FLOAT_0._add(FLOAT_MINUS_1));
    assertEquals(FLOAT_0, FLOAT_1._add(FLOAT_MINUS_1));
    assertEquals(FLOAT_0, FLOAT_MINUS_1._add(FLOAT_1));

    assertUnset.accept(unsetFloat._add(FLOAT_1));
    assertUnset.accept(FLOAT_0._add(unsetFloat));

    //Specific calculations
    assertEquals(Float._of(2.456), FLOAT_1._add(Float._of(1.456)));

    assertEquals(Float._of(-0.3999999999999999), FLOAT_1._sub(Float._of(1.400)));
    assertUnset.accept(FLOAT_0._sub(unsetFloat));

    assertEquals(Float._of(2.8), FLOAT_2._mul(Float._of(1.400)));
    assertUnset.accept(FLOAT_0._mul(unsetFloat));

    assertEquals(Float._of(0.9955201592832256), FLOAT_2._div(Float._of(2.0090)));
    assertEquals(Float._of(1.9999999999999998E15), FLOAT_2._div(Float._of(0.000000000000001)));
    assertUnset.accept(FLOAT_0._div(unsetFloat));

    //Check division of small numbers by smaller numbers.
    assertEquals(Float._of(1.0019913530064882E122), Float._of(10E-200)._div(Float._of(10E-322)));
    //Check loss of precision leading to infinity.
    assertUnset.accept(FLOAT_2._div(Float._of(10E-322)));

    //Divide by zero check
    assertUnset.accept(FLOAT_2._div(FLOAT_0));
  }

  @Test
  void testAsString() {

    assertUnset.accept(unsetFloat._string());

    assertEquals(String._of("0.0"), FLOAT_0._string());
    assertEquals(String._of("1.0"), FLOAT_1._string());
    assertEquals(String._of("-1.0"), FLOAT_MINUS_1._string());

    assertEquals("1.0", FLOAT_1.toString());
  }

  @Test
  void testAsJson() {
    // Test JSON conversion with set values
    final var zeroJson = FLOAT_0._json();
    assertNotNull(zeroJson);
    assertSet.accept(zeroJson);
    
    final var oneJson = FLOAT_1._json();
    assertSet.accept(oneJson);
    
    final var minusOneJson = FLOAT_MINUS_1._json();
    assertSet.accept(minusOneJson);
    
    // Test JSON conversion with unsetFloat value
    assertUnset.accept(unsetFloat._json());
  }

  @Test
  void testHashCode() {
    assertUnset.accept(unsetFloat._hashcode());
    assertEquals(FLOAT_0._hashcode(), FLOAT_0._hashcode());
    assertNotEquals(FLOAT_0._hashcode(), FLOAT_1._hashcode());
    assertNotEquals(FLOAT_MINUS_1._hashcode(), FLOAT_1._hashcode());
  }

  /**
   * Different from Integer prefix suffix, this just gives the whole
   * Integer value of anything before the decimal point.
   * Then for suffix gives everything after the decimal point.<br/>
   * 1234.77 : prefix is 1234, suffix is 0.77<br/>
   * -1234.77 : prefix is -1234 and suffix is -0.77</br>
   */
  @Test
  void testPrefixSuffix() {
    assertUnset.accept(unsetFloat._prefix());
    assertUnset.accept(unsetFloat._suffix());

    assertEquals(INT_0, FLOAT_0._prefix());
    assertEquals(Float._of(0), FLOAT_0._suffix());

    final var example = Float._of(1234.77);
    assertEquals(Integer._of(1234), example._prefix());
    assertEquals(Float._of(0.77), example._suffix());

    final var minusExample = example._negate();
    assertEquals(Integer._of(-1234), minusExample._prefix());
    assertEquals(Float._of(-0.77), minusExample._suffix());
  }

  @Test
  void testAdditionalMathematicalOperators() {

    assertEquals(FLOAT_0, FLOAT_0._abs());
    assertEquals(FLOAT_2, FLOAT_2._abs());
    assertEquals(FLOAT_2, FLOAT_MINUS_2._abs());
    assertUnset.accept(unsetFloat._abs());
  }

  @Test
  void testUtilityOperators() {
    assertUnset.accept(unsetFloat._empty());

    assertEquals(trueBoolean, FLOAT_0._empty());
    assertEquals(falseBoolean, FLOAT_1._empty());
    assertEquals(falseBoolean, FLOAT_MINUS_2._empty());

    assertUnset.accept(unsetFloat._len());
    assertEquals(Integer._of(3), FLOAT_0._len());
    assertEquals(Integer._of(3), FLOAT_1._len());
    //The length includes the minus sign.
    assertEquals(INT_4, FLOAT_MINUS_1._len());

    assertEquals(Integer._of(6), Float._of(1234.9)._len());
  }

  @Test
  void testPipeLogic() {

    var mutatedValue = new Float();
    assertUnset.accept(mutatedValue);

    mutatedValue._pipe(unsetFloat);
    assertUnset.accept(mutatedValue);

    mutatedValue._pipe(FLOAT_1);
    assertEquals(FLOAT_1, mutatedValue);

    //basically just keep adding
    mutatedValue._pipe(FLOAT_2);
    assertEquals(FLOAT_3, mutatedValue);

    //Even if we pipe in something unsetFloat, for pipes this is ignored.
    //This is the main different over addAssign. With that the result becomes unsetFloat.
    //But with pipes you can pipe anything in.
    mutatedValue._pipe(unsetFloat);
    assertEquals(FLOAT_3, mutatedValue);

    //Now just show a negative being added.
    mutatedValue._pipe(FLOAT_4._negate());
    assertEquals(FLOAT_MINUS_1, mutatedValue);
  }

  private List<Consumer<Float>> getFloatAssignmentOperations(final Float from) {
    return List.of(from::_addAss, from::_subAss, from::_mulAss, from::_divAss);
  }

  private List<Consumer<Integer>> getIntegerAssignmentOperations(final Float from) {
    return List.of(from::_addAss, from::_subAss, from::_mulAss, from::_divAss);
  }

  @Test
  void testMutationOperatorsWithUnsetFloat() {

    final var mutatedValue = Float._of(0);

    for (var operator : getFloatAssignmentOperations(mutatedValue)) {
      operator.accept(unsetFloat);
      assertUnset.accept(mutatedValue);
      //Now set it back again for next time around loop.
      mutatedValue._copy(FLOAT_0);
      assertEquals(FLOAT_0, mutatedValue);
    }
  }

  @Test
  void testMutationOperatorsWithUnsetInteger() {

    final var mutatedValue = Float._of(0);

    for (var operator : getIntegerAssignmentOperations(mutatedValue)) {
      operator.accept(new Integer());
      assertUnset.accept(mutatedValue);
      //Now set it back again for next time around loop.
      mutatedValue._copy(FLOAT_0);
      assertEquals(FLOAT_0, mutatedValue);
    }
  }

  @Test
  void testMutationOperators() {

    final var mutatedValue = Float._of(0);
    mutatedValue._addAss(FLOAT_1);
    assertEquals(FLOAT_1, mutatedValue);

    mutatedValue._mulAss(FLOAT_4);
    assertEquals(FLOAT_4, mutatedValue);

    mutatedValue._divAss(FLOAT_2);
    assertEquals(FLOAT_2, mutatedValue);

    mutatedValue._subAss(FLOAT_2);
    assertEquals(FLOAT_0, mutatedValue);

    //Now Integer args
    mutatedValue._addAss(INT_1);
    assertEquals(FLOAT_1, mutatedValue);

    mutatedValue._mulAss(INT_4);
    assertEquals(FLOAT_4, mutatedValue);

    mutatedValue._divAss(INT_2);
    assertEquals(FLOAT_2, mutatedValue);

    mutatedValue._subAss(INT_2);
    assertEquals(FLOAT_0, mutatedValue);
  }

  @Test
  void testReplaceAndCopyLogic() {

    var mutatedValue = Float._of(0.0);
    assertEquals(FLOAT_0, mutatedValue);

    mutatedValue._replace(FLOAT_1);
    assertEquals(FLOAT_1, mutatedValue);

    mutatedValue._replace(FLOAT_2);
    assertEquals(FLOAT_2, mutatedValue);

    mutatedValue._replace(unsetFloat);
    assertUnset.accept(mutatedValue);

    //Now just check that it can take a value after being unsetFloat
    mutatedValue._replace(FLOAT_4);
    assertEquals(FLOAT_4, mutatedValue);
  }

  @Test
  void testSimplePipedJSONValue() {
    // Test piping individual JSON float values
    final var mutatedFloat = new Float();
    final var jsonOne = new JSON(FLOAT_1);
    final var jsonTwo = new JSON(FLOAT_2);
    final var jsonStringPi = new JSON(String._of("3.14159"));
    final var jsonInvalidString = new JSON(String._of("not a number")); // Should be ignored

    // Start unset
    assertUnset.accept(mutatedFloat);

    // Pipe 1.0 - should become 1.0
    mutatedFloat._pipe(jsonOne);
    assertSet.accept(mutatedFloat);
    assertEquals(FLOAT_1, mutatedFloat);

    // Pipe 2.0 - should accumulate to 3.0
    mutatedFloat._pipe(jsonTwo);
    assertEquals(FLOAT_3, mutatedFloat);

    // Test string parsing - should add pi
    final var stringTest = new Float();
    stringTest._pipe(jsonStringPi);
    assertEquals(3.14159, stringTest.state, 0.00001);

    // Test invalid string - should be ignored
    final var beforeInvalid = stringTest.state;
    stringTest._pipe(jsonInvalidString);
    assertEquals(beforeInvalid, stringTest.state, 0.00001); // Should remain unchanged
  }

  @Test
  void testSimplePipedJSONArray() {
    final var mutatedFloat = new Float();
    final var json1Result = new JSON().parse(String._of("[1.0, 2.0]"));
    final var json2Result = new JSON().parse(String._of("[10.0, 5.0]"));

    // Check that the JSON text was parsed
    assertSet.accept(json1Result);
    assertSet.accept(json2Result);

    // Pipe array with 1.0 and 2.0 - should end up as 3.0
    mutatedFloat._pipe(json1Result.ok());
    assertSet.accept(mutatedFloat);
    assertEquals(3.0, mutatedFloat.state, 0.00001);

    // Pipe second array - should add 15.0 to get 18.0
    mutatedFloat._pipe(json2Result.ok());
    assertEquals(18.0, mutatedFloat.state, 0.00001);
  }

  @Test
  void testStructuredPipedJSONObject() {
    final var mutatedFloat = new Float();
    final var jsonStr = """
        {
          "prop1": 2.0,
          "prop2": 3.0
        }""";
    final var jsonResult = new JSON().parse(String._of(jsonStr));
    
    // Pre-condition check that parsing succeeded
    assertSet.accept(jsonResult);
    mutatedFloat._pipe(jsonResult.ok());

    assertSet.accept(mutatedFloat);
    assertEquals(5.0, mutatedFloat.state, 0.00001); // 2.0 + 3.0 = 5.0
  }

  @Test
  void testNestedPipedJSONObject() {
    final var mutatedFloat = new Float();
    final var jsonStr = """
        {
          "prop1": [1.0, 2.0],
          "prop2": 3.0,
          "prop3": "Just a String",
          "prop4": [{"val1": 4.0, "val2": 5.0}, {"other": 6.0}]
        }""";
    final var jsonResult = new JSON().parse(String._of(jsonStr));
    
    // Pre-condition check that parsing succeeded
    assertSet.accept(jsonResult);
    mutatedFloat._pipe(jsonResult.ok());

    assertSet.accept(mutatedFloat);
    // Should be 21.0 (1.0 + 2.0 + 3.0 + 4.0 + 5.0 + 6.0 = 21.0)
    assertEquals(21.0, mutatedFloat.state, 0.00001);
  }

}
