package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class ResolutionTest extends Common {
  final Resolution unset = new Resolution();
  final Resolution zeroDpi = Resolution._of("0dpi");
  final Resolution oneDpi = Resolution._of("1dpi");
  final Resolution twoDpi = Resolution._of("2dpi");
  final Resolution threeDpi = Resolution._of("3dpi");
  final Resolution fourDpi = Resolution._of("4dpi");
  final Resolution oneHundredDpi = Resolution._of("100dpi");

  final Integer int2 = Integer._of(2);
  final Integer int3 = Integer._of(3);
  final Float float2 = Float._of(2.0);

  @Test
  void testConstruction() {
    // Default constructor
    final var defaultConstructor = new Resolution();
    assertUnset.accept(defaultConstructor);

    // String constructor - valid and invalid
    final var unset1 = Resolution._of("not-a-resolution");
    assertUnset.accept(unset1);
    final var unset2 = new Resolution(new String());
    assertUnset.accept(unset2);
    final var unset3 = Resolution._of("100"); // Missing suffix
    assertUnset.accept(unset3);
    final var unset4 = Resolution._of("dpi100"); // Wrong format
    assertUnset.accept(unset4);
    final var unset5 = Resolution._of("100px"); // Invalid suffix
    assertUnset.accept(unset5);

    final var checkDpi1 = new Resolution(String._of("100dpi"));
    assertSet.accept(checkDpi1);
    assertEquals("100dpi", checkDpi1.toString());

    final var checkDpc1 = Resolution._of("25dpc");
    assertSet.accept(checkDpc1);
    assertEquals("25dpc", checkDpc1.toString());

    // Copy constructor
    final var copyDpi = new Resolution(oneHundredDpi);
    assertSet.accept(copyDpi);
    assertEquals("100dpi", copyDpi.toString());

    final var copyUnset = new Resolution(unset);
    assertUnset.accept(copyUnset);

    // Static factory methods
    final var factoryDpi = Resolution._of("150dpi");
    assertSet.accept(factoryDpi);
    assertEquals("150dpi", factoryDpi.toString());

    final var factoryDpc = Resolution._of("50dpc");
    assertSet.accept(factoryDpc);
    assertEquals("50dpc", factoryDpc.toString());
  }

  @Test
  void testSuffixValidation() {
    // Test valid suffixes
    final var validDpi = Resolution._of("72dpi");
    assertSet.accept(validDpi);
    assertEquals("72dpi", validDpi.toString());

    final var validDpc = Resolution._of("30dpc");
    assertSet.accept(validDpc);
    assertEquals("30dpc", validDpc.toString());

    // Test invalid suffixes
    final var invalidSuffixes = List.of("px", "pt", "em", "m", "cm", "invalid", "dp", "dps");
    for (java.lang.String suffix : invalidSuffixes) {
      final var resolution = Resolution._of("10" + suffix);
      assertUnset.accept(resolution);
    }

    // Test that only exact "dpi" and "dpc" are valid
    final var almostValid1 = Resolution._of("100dpix");
    assertUnset.accept(almostValid1);

    final var almostValid2 = Resolution._of("100dpcs");
    assertUnset.accept(almostValid2);
  }

  @Test
  void testEquality() {
    final var dpi1 = Resolution._of("100dpi");
    final var dpi2 = Resolution._of("100dpi");
    final var dpi3 = Resolution._of("200dpi");
    final var dpc1 = Resolution._of("100dpc");

    // Equality with same suffix
    assertTrue.accept(dpi1._eq(dpi2));
    assertFalse.accept(dpi1._eq(dpi3));
    assertUnset.accept(unset._eq(dpi1));
    assertUnset.accept(dpi1._eq(unset));

    // Equality with different suffix should return unset
    assertUnset.accept(dpi1._eq(dpc1));

    // Inequality
    assertFalse.accept(dpi1._neq(dpi2));
    assertTrue.accept(dpi1._neq(dpi3));
    assertUnset.accept(unset._neq(dpi1));
    assertUnset.accept(dpi1._neq(unset));
    assertUnset.accept(dpi1._neq(dpc1));

    // Java equals and hashCode
    assertEquals(dpi1, dpi2);
    assertNotEquals(dpi1, dpi3);
    assertNotEquals(dpi1, dpc1);
    assertEquals(dpi1.hashCode(), dpi2.hashCode());
    assertNotEquals(dpi1.hashCode(), dpi3.hashCode());
  }

  @Test
  void testComparison() {
    final var dpi50 = Resolution._of("50dpi");
    final var dpi100 = Resolution._of("100dpi");
    final var dpi150 = Resolution._of("150dpi");
    final var dpc50 = Resolution._of("50dpc");

    // Less than
    assertTrue.accept(dpi50._lt(dpi100));
    assertFalse.accept(dpi100._lt(dpi50));
    assertFalse.accept(dpi100._lt(dpi100));
    assertUnset.accept(unset._lt(dpi100));
    assertUnset.accept(dpi100._lt(unset));
    assertUnset.accept(dpi50._lt(dpc50)); // Different suffix

    // Less than or equal
    assertTrue.accept(dpi50._lteq(dpi100));
    assertTrue.accept(dpi100._lteq(dpi100));
    assertFalse.accept(dpi150._lteq(dpi100));
    assertUnset.accept(unset._lteq(dpi100));
    assertUnset.accept(dpi100._lteq(unset));
    assertUnset.accept(dpi50._lteq(dpc50)); // Different suffix

    // Greater than
    assertTrue.accept(dpi150._gt(dpi100));
    assertFalse.accept(dpi50._gt(dpi100));
    assertFalse.accept(dpi100._gt(dpi100));
    assertUnset.accept(unset._gt(dpi100));
    assertUnset.accept(dpi100._gt(unset));
    assertUnset.accept(dpi150._gt(dpc50)); // Different suffix

    // Greater than or equal
    assertTrue.accept(dpi150._gteq(dpi100));
    assertTrue.accept(dpi100._gteq(dpi100));
    assertFalse.accept(dpi50._gteq(dpi100));
    assertUnset.accept(unset._gteq(dpi100));
    assertUnset.accept(dpi100._gteq(unset));
    assertUnset.accept(dpi150._gteq(dpc50)); // Different suffix

    // Compare
    assertEquals(-1, (int) dpi50._cmp(dpi100).state);
    assertEquals(0, (int) dpi100._cmp(dpi100).state);
    assertEquals(1, (int) dpi150._cmp(dpi100).state);
    assertUnset.accept(unset._cmp(dpi100));
    assertUnset.accept(dpi100._cmp(unset));
    assertUnset.accept(dpi50._cmp(dpc50)); // Different suffix
    assertUnset.accept(dpi100._cmp(new Any(){}));
  }

  @Test
  void testArithmeticOperators() {
    final var dpi100 = Resolution._of("100dpi");
    final var dpi200 = Resolution._of("200dpi");
    final var dpi50 = Resolution._of("50dpi");
    final var dpc100 = Resolution._of("100dpc");

    // Addition with same suffix
    final var addResult = dpi100._add(dpi50);
    assertEquals(150, addResult._prefix().state);
    assertEquals("dpi", addResult._suffix().state);
    assertUnset.accept(unset._add(dpi100));
    assertUnset.accept(dpi100._add(unset));

    // Addition with different suffix should return unset
    assertUnset.accept(dpi100._add(dpc100));

    // Subtraction with same suffix
    final var subResult = dpi200._sub(dpi50);
    assertEquals(150, subResult._prefix().state);
    assertEquals("dpi", subResult._suffix().state);
    assertUnset.accept(unset._sub(dpi100));
    assertUnset.accept(dpi100._sub(unset));

    // Subtraction with different suffix should return unset
    assertUnset.accept(dpi200._sub(dpc100));

    // Multiplication with Integer
    final var mulIntResult = dpi100._mul(int2);
    assertEquals(200, mulIntResult._prefix().state);
    assertEquals("dpi", mulIntResult._suffix().state);
    assertUnset.accept(unset._mul(int2));
    assertUnset.accept(dpi100._mul(new Integer()));

    // Division with Integer
    final var divIntResult = dpi200._div(int2);
    assertEquals(100, divIntResult._prefix().state);
    assertEquals("dpi", divIntResult._suffix().state);
    assertUnset.accept(unset._div(int2));
    assertUnset.accept(dpi200._div(new Integer()));

    // Multiplication with Float
    final var mulFloatResult = dpi100._mul(float2);
    assertEquals(200, mulFloatResult._prefix().state);
    assertEquals("dpi", mulFloatResult._suffix().state);
    assertUnset.accept(unset._mul(float2));
    assertUnset.accept(dpi100._mul(new Float()));

    // Division with Float
    final var divFloatResult = dpi200._div(float2);
    assertEquals(100, divFloatResult._prefix().state);
    assertEquals("dpi", divFloatResult._suffix().state);
    assertUnset.accept(unset._div(float2));
    assertUnset.accept(dpi200._div(new Float()));

    // Division returning Float (same suffix)
    final var divResResult = dpi200._div(dpi100);
    assertEquals(2.0, divResResult.state, 0.001);
    assertUnset.accept(unset._div(dpi100));
    assertUnset.accept(dpi200._div(unset));

    // Division with different suffix should return unset
    assertUnset.accept(dpi200._div(dpc100));
  }

  @Test
  void testModulusAndRemainder() {
    final var dpi300 = Resolution._of("300dpi");
    final var dpi100 = Resolution._of("100dpi");
    final var dpi250 = Resolution._of("250dpi");
    final var dpi17 = Resolution._of("17dpi");
    final var dpi5 = Resolution._of("5dpi");
    final var dpiNeg17 = Resolution._of("-17dpi");
    final var dpiNeg5 = Resolution._of("-5dpi");
    final var dpc100 = Resolution._of("100dpc");

    // Modulus with positive numbers (same suffix)
    final var modResult1 = dpi300._mod(dpi100);
    assertEquals(0, modResult1.state);
    final var modResult2 = dpi250._mod(dpi100);
    assertEquals(50, modResult2.state);
    assertUnset.accept(unset._mod(dpi100));
    assertUnset.accept(dpi300._mod(unset));

    // Modulus with different suffix should return unset
    assertUnset.accept(dpi300._mod(dpc100));

    // Modulus with negative numbers (following Integer modulus behavior)
    final var modNegResult1 = dpiNeg17._mod(dpi5);  // -17 % 5 = 3
    assertEquals(3, modNegResult1.state);
    final var modNegResult2 = dpi17._mod(dpiNeg5);  // 17 % -5 = -3
    assertEquals(-3, modNegResult2.state);
    final var modNegResult3 = dpiNeg17._mod(dpiNeg5);  // -17 % -5 = -2
    assertEquals(-2, modNegResult3.state);

    // Remainder with positive numbers (same suffix)
    final var remResult1 = dpi300._rem(dpi100);
    assertEquals(0, remResult1.state);
    final var remResult2 = dpi250._rem(dpi100);
    assertEquals(50, remResult2.state);
    assertUnset.accept(unset._rem(dpi100));
    assertUnset.accept(dpi300._rem(unset));

    // Remainder with different suffix should return unset
    assertUnset.accept(dpi300._rem(dpc100));

    // Remainder with negative numbers (following Integer remainder behavior)
    final var remNegResult1 = dpiNeg17._rem(dpi5);  // -17 rem 5 = -2
    assertEquals(-2, remNegResult1.state);
    final var remNegResult2 = dpi17._rem(dpiNeg5);  // 17 rem -5 = 2
    assertEquals(2, remNegResult2.state);
    final var remNegResult3 = dpiNeg17._rem(dpiNeg5);  // -17 rem -5 = -2
    assertEquals(-2, remNegResult3.state);
  }

  @Test
  void testAssignmentOperators() {
    // Addition assignment with same suffix
    final var dpi1 = Resolution._of("100dpi");
    dpi1._addAss(Resolution._of("50dpi"));
    assertEquals(150, dpi1._prefix().state);
    assertEquals("dpi", dpi1._suffix().state);

    // Addition assignment with different suffix should unset
    final var dpi2 = Resolution._of("100dpi");
    dpi2._addAss(Resolution._of("50dpc"));
    assertUnset.accept(dpi2);

    // Subtraction assignment with same suffix
    final var dpi3 = Resolution._of("200dpi");
    dpi3._subAss(Resolution._of("50dpi"));
    assertEquals(150, dpi3._prefix().state);
    assertEquals("dpi", dpi3._suffix().state);

    // Subtraction assignment with different suffix should unset
    final var dpi4 = Resolution._of("200dpi");
    dpi4._subAss(Resolution._of("50dpc"));
    assertUnset.accept(dpi4);

    // Multiplication assignment with Integer
    final var dpi5 = Resolution._of("100dpi");
    dpi5._mulAss(int3);
    assertEquals(300, dpi5._prefix().state);
    assertEquals("dpi", dpi5._suffix().state);

    // Division assignment with Integer
    final var dpi6 = Resolution._of("300dpi");
    dpi6._divAss(int3);
    assertEquals(100, dpi6._prefix().state);
    assertEquals("dpi", dpi6._suffix().state);

    // Multiplication assignment with Float
    final var dpi7 = Resolution._of("100dpi");
    dpi7._mulAss(float2);
    assertEquals(200, dpi7._prefix().state);
    assertEquals("dpi", dpi7._suffix().state);

    // Division assignment with Float
    final var dpi8 = Resolution._of("200dpi");
    dpi8._divAss(float2);
    assertEquals(100, dpi8._prefix().state);
    assertEquals("dpi", dpi8._suffix().state);

    // Test unset propagation
    final var resUnset = new Resolution();
    resUnset._addAss(Resolution._of("100dpi"));
    assertUnset.accept(resUnset);
  }

  @Test
  void testUnaryOperators() {
    // Negation
    final var dpi100 = Resolution._of("100dpi");
    final var negated = dpi100._negate();
    assertEquals(-100, negated._prefix().state);
    assertEquals("dpi", negated._suffix().state);
    assertUnset.accept(unset._negate());

    assertFalse.accept(unset._isSet());
    assertTrue.accept(dpi100._isSet());
    // Increment
    final var dpi1 = Resolution._of("100dpi");
    dpi1._inc();
    assertEquals(101, dpi1._prefix().state);
    assertEquals("dpi", dpi1._suffix().state);
    final var unsetInc = new Resolution();
    unsetInc._inc();
    assertUnset.accept(unsetInc);

    // Decrement
    final var dpi2 = Resolution._of("100dpi");
    dpi2._dec();
    assertEquals(99, dpi2._prefix().state);
    assertEquals("dpi", dpi2._suffix().state);
    final var unsetDec = new Resolution();
    unsetDec._dec();
    assertUnset.accept(unsetDec);

    // Absolute value
    final var dpiNeg = Resolution._of("-100dpi");
    final var absResult = dpiNeg._abs();
    assertEquals(100, absResult._prefix().state);
    assertEquals("dpi", absResult._suffix().state);
    final var dpiPos = Resolution._of("100dpi");
    final var absResult2 = dpiPos._abs();
    assertEquals(100, absResult2._prefix().state);
    assertEquals("dpi", absResult2._suffix().state);
    assertUnset.accept(unset._abs());

    // Square root (returns Float, not Resolution)
    final var dpi100Sqrt = Resolution._of("100dpi");
    final var sqrtResult = dpi100Sqrt._sqrt();
    assertEquals(10.0, sqrtResult.state, 0.001);
    assertUnset.accept(unset._sqrt());

    // Square root of negative number
    final var dpiNegSqrt = Resolution._of("-100dpi");
    final var sqrtNegResult = dpiNegSqrt._sqrt();
    assertUnset.accept(sqrtNegResult);
  }

  @Test
  void testUtilityMethods() {
    final var dpi1000 = Resolution._of("1000dpi");

    // Prefix (numeric value)
    assertEquals(1000, dpi1000._prefix().state);
    assertUnset.accept(unset._prefix());

    // Suffix (unit string)
    assertEquals("dpi", dpi1000._suffix().state);
    assertUnset.accept(unset._suffix());

    // Length
    assertEquals(7, dpi1000._len().state); // "1000dpi" = 7 characters
    assertEquals(0, unset._len().state);

    // String conversion
    assertEquals("1000dpi", dpi1000._string().state);
    assertEquals("", unset._string().state);

    // JSON operations
    final var dpi1000Json = dpi1000._json();
    assertSet.accept(dpi1000Json);

    assertUnset.accept(unset._json());
    assertEquals("1000dpi", dpi1000.toString());
    assertEquals("", unset.toString());

    // Test with dpc
    final var dpc100 = Resolution._of("100dpc");
    assertEquals("100dpc", dpc100.toString());
    assertEquals(100, dpc100._prefix().state);
    assertEquals("dpc", dpc100._suffix().state);
  }

  @Test
  void testCopyAndAssignmentOperators() {
    final var dpi100 = Resolution._of("100dpi");
    final var dpi200 = Resolution._of("200dpi");

    // Copy operation
    dpi100._copy(dpi200);
    assertEquals(200, dpi100._prefix().state);
    assertEquals("dpi", dpi100._suffix().state);

    // Copy unset
    final var dpiSet = Resolution._of("300dpi");
    dpiSet._copy(unset);
    assertUnset.accept(dpiSet);

    // Replace operation
    final var dpi1 = Resolution._of("100dpi");
    final var dpi2 = Resolution._of("200dpi");
    dpi1._replace(dpi2);
    assertEquals(200, dpi1._prefix().state);
    assertEquals("dpi", dpi1._suffix().state);

    // Pipe operation (merge)
    final var dpi3 = Resolution._of("100dpi");
    final var dpi4 = Resolution._of("200dpi");
    dpi3._pipe(dpi4);
    assertEquals(300, dpi3._prefix().state); // Should be added
    assertEquals("dpi", dpi3._suffix().state);

    // Pipe with unset target
    final var resUnset = new Resolution();
    final var dpi5 = Resolution._of("100dpi");
    resUnset._pipe(dpi5);
    assertEquals(100, resUnset._prefix().state); // Should assign
    assertEquals("dpi", resUnset._suffix().state);

    // Merge operation
    final var dpi6 = Resolution._of("100dpi");
    final var dpi7 = Resolution._of("200dpi");
    dpi6._merge(dpi7);
    assertEquals(300, dpi6._prefix().state);
    assertEquals("dpi", dpi6._suffix().state);
  }

  @Test
  void testEdgeCases() {
    // Division by zero with Integer
    final var dpi100 = Resolution._of("100dpi");
    final var zeroInt = Integer._of(0);
    final var divByZeroResult = dpi100._div(zeroInt);
    assertUnset.accept(divByZeroResult);

    // Division assignment by zero
    final var dpi200 = Resolution._of("200dpi");
    dpi200._divAss(zeroInt);
    assertUnset.accept(dpi200);

    // Division by zero with Float
    final var zeroFloat = Float._of(0.0);
    final var divByZeroFloatResult = dpi100._div(zeroFloat);
    assertUnset.accept(divByZeroFloatResult);

    // Large values
    final var largeRes = Resolution._of("1000000dpi");
    assertSet.accept(largeRes);

    // Negative values
    final var negRes = Resolution._of("-1000dpi");
    assertEquals(-1000, negRes._prefix().state);
    assertTrue.accept(negRes._lt(zeroDpi));

    // String parsing edge cases
    final var invalidString1 = Resolution._of("invalid");
    assertUnset.accept(invalidString1);

    final var invalidString2 = Resolution._of("100"); // Missing suffix
    assertUnset.accept(invalidString2);

    final var invalidString3 = Resolution._of("dpi100"); // Wrong format
    assertUnset.accept(invalidString3);

    final var invalidString4 = Resolution._of("100.5dpi"); // Decimal not allowed in Resolution
    assertUnset.accept(invalidString4);

    final var invalidString5 = Resolution._of("100invalidSuffix");
    assertUnset.accept(invalidString5);

    // Test null handling
    final var nullRes = Resolution._of(null);
    assertUnset.accept(nullRes);

    // Test regex boundary cases
    final var almostValidDpi = Resolution._of("100dpix");
    assertUnset.accept(almostValidDpi);

    final var almostValidDpc = Resolution._of("100dpcs");
    assertUnset.accept(almostValidDpc);

    // Test leading zeros
    final var leadingZeros = Resolution._of("0072dpi");
    assertSet.accept(leadingZeros);
    assertEquals(72, leadingZeros._prefix().state);
  }

  @Test
  void testPipeLogic() {
    var mutatedValue = new Resolution();
    assertUnset.accept(mutatedValue);

    mutatedValue._pipe(unset);
    assertUnset.accept(mutatedValue);

    mutatedValue._pipe(oneDpi);
    assertEquals(oneDpi, mutatedValue);

    // Keep adding
    mutatedValue._pipe(twoDpi);
    assertEquals(threeDpi, mutatedValue);

    // Even if we pipe in something unset, for pipes this is ignored
    mutatedValue._pipe(unset);
    assertEquals(threeDpi, mutatedValue);

    // Now just show a negative being added
    mutatedValue._pipe(fourDpi._negate());
    assertEquals(Resolution._of("-1dpi"), mutatedValue);
  }

  private List<Consumer<Resolution>> getResolutionAssignmentOperations(final Resolution from) {
    return List.of(from::_addAss, from::_subAss);
  }

  private List<Consumer<Integer>> getIntegerAssignmentOperations(final Resolution from) {
    return List.of(from::_mulAss, from::_divAss);
  }

  private List<Consumer<Float>> getFloatAssignmentOperations(final Resolution from) {
    return List.of(from::_mulAss, from::_divAss);
  }

  @Test
  void testMutationOperatorsWithUnsetResolution() {
    final var mutatedValue = Resolution._of("100dpi");

    for (var operator : getResolutionAssignmentOperations(mutatedValue)) {
      operator.accept(unset);
      assertUnset.accept(mutatedValue);
      // Set it back again for next time around loop
      mutatedValue._copy(oneHundredDpi);
      assertEquals(oneHundredDpi, mutatedValue);
    }
  }

  @Test
  void testMutationOperatorsWithUnsetInteger() {
    final var mutatedValue = Resolution._of("100dpi");

    for (var operator : getIntegerAssignmentOperations(mutatedValue)) {
      operator.accept(new Integer());
      assertUnset.accept(mutatedValue);
      // Set it back again for next time around loop
      mutatedValue._copy(oneHundredDpi);
      assertEquals(oneHundredDpi, mutatedValue);
    }
  }

  @Test
  void testMutationOperatorsWithUnsetFloat() {
    final var mutatedValue = Resolution._of("100dpi");

    for (var operator : getFloatAssignmentOperations(mutatedValue)) {
      operator.accept(new Float());
      assertUnset.accept(mutatedValue);
      // Set it back again for next time around loop
      mutatedValue._copy(oneHundredDpi);
      assertEquals(oneHundredDpi, mutatedValue);
    }
  }

  @Test
  void testMutationOperators() {
    final var mutatedValue = Resolution._of("100dpi");
    mutatedValue._addAss(oneDpi);
    assertEquals(Resolution._of("101dpi"), mutatedValue);

    mutatedValue._mulAss(int2);
    assertEquals(Resolution._of("202dpi"), mutatedValue);

    mutatedValue._divAss(int2);
    assertEquals(Resolution._of("101dpi"), mutatedValue);

    mutatedValue._subAss(oneDpi);
    assertEquals(oneHundredDpi, mutatedValue);

    // Now Float args
    mutatedValue._mulAss(float2);
    assertEquals(Resolution._of("200dpi"), mutatedValue);

    mutatedValue._divAss(float2);
    assertEquals(oneHundredDpi, mutatedValue);
  }

  @Test
  void testReplaceAndCopyLogic() {
    var mutatedValue = Resolution._of("100dpi");
    assertEquals(oneHundredDpi, mutatedValue);

    mutatedValue._replace(oneDpi);
    assertEquals(oneDpi, mutatedValue);

    mutatedValue._replace(twoDpi);
    assertEquals(twoDpi, mutatedValue);

    mutatedValue._replace(unset);
    assertUnset.accept(mutatedValue);

    // Now just check that it can take a value after being unset
    mutatedValue._replace(fourDpi);
    assertEquals(fourDpi, mutatedValue);
  }

  @Test
  void testDifferentSuffixTypes() {
    // Test both valid suffix types work correctly
    final var dpiRes = Resolution._of("72dpi");
    final var dpcRes = Resolution._of("30dpc");

    // Both should be set
    assertSet.accept(dpiRes);
    assertSet.accept(dpcRes);

    // Verify values
    assertEquals(72, dpiRes._prefix().state);
    assertEquals("dpi", dpiRes._suffix().state);
    assertEquals(30, dpcRes._prefix().state);
    assertEquals("dpc", dpcRes._suffix().state);

    // Test that different suffixes cannot be combined
    assertUnset.accept(dpiRes._add(dpcRes));
    assertUnset.accept(dpiRes._sub(dpcRes));
    assertUnset.accept(dpiRes._eq(dpcRes));
    assertUnset.accept(dpiRes._lt(dpcRes));
    assertUnset.accept(dpiRes._mod(dpcRes));
    assertUnset.accept(dpiRes._rem(dpcRes));

    // But same suffix operations should work
    final var anotherDpi = Resolution._of("144dpi");
    final var addResult = dpiRes._add(anotherDpi);
    assertSet.accept(addResult);
    assertEquals(216, addResult._prefix().state);
    assertEquals("dpi", addResult._suffix().state);
  }

  @Test
  void testTypicalResolutionValues() {
    // Test common resolution values
    final var screenRes = Resolution._of("96dpi");  // Common screen resolution
    final var printRes = Resolution._of("300dpi");  // Common print resolution
    final var lowRes = Resolution._of("72dpi");     // Traditional screen resolution
    final var highRes = Resolution._of("600dpi");   // High quality print

    assertSet.accept(screenRes);
    assertSet.accept(printRes);
    assertSet.accept(lowRes);
    assertSet.accept(highRes);

    // Test some calculations
    final var doubleScreen = screenRes._mul(int2);
    assertEquals(192, doubleScreen._prefix().state);

    final var halfPrint = printRes._div(int2);
    assertEquals(150, halfPrint._prefix().state);

    // Test dpc values (dots per centimeter)
    final var dpcLow = Resolution._of("28dpc");    // ~72 dpi
    final var dpcHigh = Resolution._of("118dpc");  // ~300 dpi

    assertSet.accept(dpcLow);
    assertSet.accept(dpcHigh);
  }
}