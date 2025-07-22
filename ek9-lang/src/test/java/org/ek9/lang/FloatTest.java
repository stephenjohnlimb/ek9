package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class FloatTest extends Common {

  final Boolean true1 = Boolean._of("true");
  final Boolean false1 = Boolean._of("false");

  final Float unset = new Float();
  final Float f0 = Float._of(0);
  final Float fMinus1 = Float._of(-1);
  final Float f1 = Float._of(1);
  final Float f2 = Float._of(2);
  final Float fMinus2 = Float._of(-2);
  final Float f3 = Float._of(3);
  final Float f4 = Float._of(4);
  final Float f4Point5 = Float._of(4.5);

  // Helper methods for eliminating duplication

  /**
   * Helper method to test comparison operations with unset values.
   */
  private void assertComparisonOperatorsWithUnset(Float validValue) {
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

    assertSet.accept(f1);
    assertEquals(1.0, f1.state);

    assertSet.accept(f2);
    assertEquals(2.0, f2.state);

    assertSet.accept(f3);
    assertEquals(3.0, f3.state);

    final var also3 = Float._of(f3);
    assertSet.accept(also3);
    assertEquals(3.0, also3.state);

    final var again3 = new Float(f3);
    assertSet.accept(again3);
    assertEquals(3.0, again3.state);

    final var check4 = new Float(f4);
    assertSet.accept(check4);
    assertEquals(f4, check4);
  }

  @Test
  void testEquality() {
    final var f00 = Float._of(0);

    // Test all comparison operators with unset values using helper
    assertComparisonOperatorsWithUnset(f0);

    //Eq
    assertEquals(f00, f0);
    assertEquals(true1, f0._eq(f00));

    //Neq
    assertEquals(false1, f0._neq(f00));

    //Lt
    assertTrue.accept(f0._lt(f1));
    assertFalse.accept(f1._lt(f0));

    //gt
    assertTrue.accept(f1._gt(f0));
    assertFalse.accept(f0._gt(f1));

    //Lteq
    assertTrue.accept(f0._lteq(f00));
    assertTrue.accept(f0._lteq(f1));
    assertFalse.accept(f1._lteq(f0));

    //Gteq
    assertTrue.accept(f0._gteq(f00));
    assertTrue.accept(f1._gteq(f0));
    assertFalse.accept(f0._gteq(f1));
  }

  @Test
  void testComparison() {
    final var i00 = Float._of(0);

    assertEquals(Integer._of(0), f0._fuzzy(i00));
    assertEquals(Integer._of(-1), f0._fuzzy(f1));
    assertEquals(Integer._of(1), f1._fuzzy(f0));
    assertUnset.accept(unset._fuzzy(f0));
    assertUnset.accept(f0._fuzzy(unset));
    assertUnset.accept(f0._cmp(new Any(){}));

  }

  @Test
  void testIsSet() {
    assertNotNull(unset);
    assertFalse.accept(unset._isSet());

    final var v1 = Float._of(90);
    assertNotNull(v1);
    assertTrue.accept(v1._isSet());

  }

  /**
   * Excludes the Float operators those are tested elsewhere.
   */
  @Test
  void testSimpleMathematics() {

    final var minusZero = f0._negate();
    assertEquals(f0, minusZero);

    final var stillUnset = unset._negate();
    assertUnset.accept(stillUnset);

    //Negate
    assertEquals(fMinus1, f1._negate());
    assertUnset.accept( unset._negate());

    //Addition
    assertEquals(fMinus1, f0._add(fMinus1));
    assertEquals(f0, f1._add(fMinus1));
    assertEquals(f0, fMinus1._add(f1));
    assertUnset.accept(unset._add(f1));
    assertUnset.accept(f0._add(unset));

    //Substraction
    assertEquals(fMinus1, f0._sub(f1));
    assertEquals(f0, f1._sub(f1));
    assertEquals(f0, fMinus1._sub(fMinus1));
    assertUnset.accept(unset._sub(f1));
    assertUnset.accept(f0._sub(unset));

    //Multiplication
    assertEquals(f0, f0._mul(f1));
    assertEquals(f0, f0._mul(fMinus1));
    assertEquals(f2, f1._mul(f2));
    assertEquals(fMinus2, f2._mul(fMinus1));
    assertEquals(f4, f2._mul(f2));
    assertEquals(f4, fMinus2._mul(fMinus2));
    assertUnset.accept(unset._mul(f1));
    assertUnset.accept(f0._mul(unset));

    //Division
    assertEquals(f1, f2._div(f2));

    assertEquals(f2, f4._div(f2));
    assertEquals(fMinus2, f4._div(fMinus2));
    assertEquals(f0, f0._div(f2));

    assertUnset.accept(f0._div(f0));
    assertUnset.accept( f0._div(unset));
    assertUnset.accept(unset._div(f2));

    // Create fresh objects for mutating inc/dec operations to avoid corrupting constants
    final var freshF1 = Float._of(1);
    assertEquals(f2, freshF1._inc());
    assertUnset.accept(unset._inc());

    final var freshF0 = Float._of(0);
    assertEquals(fMinus1, freshF0._dec());
    assertUnset.accept(unset._dec());

  }

  @Test
  void testIntegerCombinedFloatingPointMathematics() {

    assertEquals(Float._of(4.5f), f4Point5._add(Integer._of(0)));
    assertEquals(Float._of(5.5f), f4Point5._add(Integer._of(1)));
    assertUnset.accept(f4Point5._add(new Integer()));
    assertUnset.accept(unset._add(Integer._of(0)));


    assertEquals(Float._of(3.5f), f4Point5._sub(Integer._of(1)));
    assertEquals(Float._of(5.5f), f4Point5._sub(Integer._of(-1)));
    assertUnset.accept(f4Point5._sub(new Integer()));
    assertUnset.accept(unset._sub(Integer._of(0)));

    assertEquals(Float._of(9.0f), f4Point5._mul(Integer._of(2)));
    assertEquals(Float._of(-9.0f), f4Point5._mul(Integer._of(-2)));
    assertUnset.accept(f4Point5._mul(new Integer()));
    assertUnset.accept(unset._mul(Integer._of(0)));

    assertEquals(Float._of(2.25f), f4Point5._div(Integer._of(2)));
    assertEquals(Float._of(-2.25f), f4Point5._div(Integer._of(-2)));
    //Now try divide by zero and divide by unset
    assertUnset.accept(f4Point5._div(Integer._of(0)));
    assertUnset.accept(f4Point5._div(new Integer()));
    assertUnset.accept(unset._div(Integer._of(0)));

  }

  @Test
  void testFloatingPointMathematics() {

    //Sqrt
    assertUnset.accept(unset._sqrt());
    assertEquals(f2, f4._sqrt());
    assertUnset.accept(f0._sqrt());

    //Pow
    assertUnset.accept(f2._pow(unset));
    assertEquals(f2, f2._pow(f1));
    assertEquals(f4, f2._pow(f2));

    //Addition
    assertEquals(fMinus1, f0._add(fMinus1));
    assertEquals(f0, f1._add(fMinus1));
    assertEquals(f0, fMinus1._add(f1));

    assertUnset.accept(unset._add(f1));
    assertUnset.accept(f0._add(unset));

    //Specific calculations
    assertEquals(Float._of(2.456), f1._add(Float._of(1.456)));

    assertEquals(Float._of(-0.3999999999999999), f1._sub(Float._of(1.400)));
    assertUnset.accept(f0._sub(unset));

    assertEquals(Float._of(2.8), f2._mul(Float._of(1.400)));
    assertUnset.accept(f0._mul(unset));

    assertEquals(Float._of(0.9955201592832256), f2._div(Float._of(2.0090)));
    assertEquals(Float._of(1.9999999999999998E15), f2._div(Float._of(0.000000000000001)));
    assertUnset.accept(f0._div(unset));

    //Check division of small numbers by smaller numbers.
    assertEquals(Float._of(1.0019913530064882E122), Float._of(10E-200)._div(Float._of(10E-322)));
    //Check loss of precision leading to infinity.
    assertUnset.accept(f2._div(Float._of(10E-322)));

    //Divide by zero check
    assertUnset.accept(f2._div(f0));
  }

  @Test
  void testAsString() {

    assertUnset.accept(unset._string());

    assertEquals(String._of("0.0"), f0._string());
    assertEquals(String._of("1.0"), f1._string());
    assertEquals(String._of("-1.0"), fMinus1._string());

    assertEquals("1.0", f1.toString());
  }

  @Test
  void testHashCode() {
    assertUnset.accept(unset._hashcode());
    assertEquals(f0._hashcode(), f0._hashcode());
    assertNotEquals(f0._hashcode(), f1._hashcode());
    assertNotEquals(fMinus1._hashcode(), f1._hashcode());
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
    assertUnset.accept(unset._prefix());
    assertUnset.accept(unset._suffix());

    assertEquals(Integer._of(0), f0._prefix());
    assertEquals(Float._of(0), f0._suffix());

    final var example = Float._of(1234.77);
    assertEquals(Integer._of(1234), example._prefix());
    assertEquals(Float._of(0.77), example._suffix());

    final var minusExample = example._negate();
    assertEquals(Integer._of(-1234), minusExample._prefix());
    assertEquals(Float._of(-0.77), minusExample._suffix());
  }

  @Test
  void testAdditionalMathematicalOperators() {

    assertEquals(f0, f0._abs());
    assertEquals(f2, f2._abs());
    assertEquals(f2, fMinus2._abs());
    assertUnset.accept(unset._abs());
  }

  @Test
  void testUtilityOperators() {
    assertUnset.accept(unset._empty());

    assertEquals(true1, f0._empty());
    assertEquals(false1, f1._empty());
    assertEquals(false1, fMinus2._empty());

    assertUnset.accept(unset._len());
    assertEquals(Integer._of(3), f0._len());
    assertEquals(Integer._of(3), f1._len());
    //The length includes the minus sign.
    assertEquals(Integer._of(4), fMinus1._len());

    assertEquals(Integer._of(6), Float._of(1234.9)._len());
  }

  @Test
  void testPipeLogic() {

    var mutatedValue = new Float();
    assertUnset.accept(mutatedValue);

    mutatedValue._pipe(unset);
    assertUnset.accept(mutatedValue);

    mutatedValue._pipe(f1);
    assertEquals(f1, mutatedValue);

    //basically just keep adding
    mutatedValue._pipe(f2);
    assertEquals(f3, mutatedValue);

    //Even if we pipe in something unset, for pipes this is ignored.
    //This is the main different over addAssign. With that the result becomes unset.
    //But with pipes you can pipe anything in.
    mutatedValue._pipe(unset);
    assertEquals(f3, mutatedValue);

    //Now just show a negative being added.
    mutatedValue._pipe(f4._negate());
    assertEquals(fMinus1, mutatedValue);
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
      operator.accept(unset);
      assertUnset.accept(mutatedValue);
      //Now set it back again for next time around loop.
      mutatedValue._copy(f0);
      assertEquals(f0, mutatedValue);
    }
  }

  @Test
  void testMutationOperatorsWithUnsetInteger() {

    final var mutatedValue = Float._of(0);

    for (var operator : getIntegerAssignmentOperations(mutatedValue)) {
      operator.accept(new Integer());
      assertUnset.accept(mutatedValue);
      //Now set it back again for next time around loop.
      mutatedValue._copy(f0);
      assertEquals(f0, mutatedValue);
    }
  }

  @Test
  void testMutationOperators() {

    final var mutatedValue = Float._of(0);
    mutatedValue._addAss(f1);
    assertEquals(f1, mutatedValue);

    mutatedValue._mulAss(f4);
    assertEquals(f4, mutatedValue);

    mutatedValue._divAss(f2);
    assertEquals(f2, mutatedValue);

    mutatedValue._subAss(f2);
    assertEquals(f0, mutatedValue);

    //Now Integer args
    mutatedValue._addAss(Integer._of(1));
    assertEquals(f1, mutatedValue);

    mutatedValue._mulAss(Integer._of(4));
    assertEquals(f4, mutatedValue);

    mutatedValue._divAss(Integer._of(2));
    assertEquals(f2, mutatedValue);

    mutatedValue._subAss(Integer._of(2));
    assertEquals(f0, mutatedValue);
  }

  @Test
  void testReplaceAndCopyLogic() {

    var mutatedValue = Float._of(0.0);
    assertEquals(f0, mutatedValue);

    mutatedValue._replace(f1);
    assertEquals(f1, mutatedValue);

    mutatedValue._replace(f2);
    assertEquals(f2, mutatedValue);

    mutatedValue._replace(unset);
    assertUnset.accept(mutatedValue);

    //Now just check that it can take a value after being unset
    mutatedValue._replace(f4);
    assertEquals(f4, mutatedValue);
  }

}
