package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class DimensionTest extends Common {

  final Integer int2 = Integer._of(2);
  final Integer int3 = Integer._of(3);
  final Float float2 = Float._of(2.0);

  @Test
  void testConstruction() {
    // Default constructor
    final var defaultConstructor = new Dimension();
    assertUnset.accept(defaultConstructor);

    // String constructor - valid and invalid
    final var unset1 = Dimension._of("not-a-dimension");
    assertUnset.accept(unset1);
    final var unset2 = new Dimension(new String());
    assertUnset.accept(unset2);
    final var unset3 = Dimension._of("100"); // Missing suffix
    assertUnset.accept(unset3);
    final var unset4 = Dimension._of("px100"); // Wrong format
    assertUnset.accept(unset4);

    final var checkPx1 = new Dimension(String._of("100px"));
    assertSet.accept(checkPx1);
    assertEquals("100.0px", checkPx1.toString());

    final var checkM1 = Dimension._of("50.5m");
    assertSet.accept(checkM1);
    assertEquals("50.5m", checkM1.toString());

    // Copy constructor
    final var copyPx = new Dimension(DIMENSION_100PX);
    assertSet.accept(copyPx);
    assertEquals("100.0px", copyPx.toString());

    final var copyUnset = new Dimension(unsetDimension);
    assertUnset.accept(copyUnset);

    // Float + String constructor
    final var fromFloatString = new Dimension(Float._of(25.5), String._of("em"));
    assertSet.accept(fromFloatString);
    assertEquals("25.5em", fromFloatString.toString());

    final var fromInvalidSuffix = new Dimension(Float._of(25.5), String._of("invalid"));
    assertUnset.accept(fromInvalidSuffix);

    // Static factory methods
    final var factoryPx = Dimension._of("150px");
    assertSet.accept(factoryPx);
    assertEquals("150.0px", factoryPx.toString());

    final var factoryPercent = Dimension._of("75%");
    assertSet.accept(factoryPercent);
    assertEquals("75.0%", factoryPercent.toString());
  }

  @Test
  void testSuffixValidation() {
    // Test all valid suffixes
    final var validSuffixes = List.of(
        "km", "m", "cm", "mm", "mile", "in", "pc", "pt", "px", "em", "ex", "ch", "rem", "vw", "vh", "vmin", "vmax", "%"
    );

    for (java.lang.String suffix : validSuffixes) {
      final var dimension = Dimension._of("10" + suffix);
      assertSet.accept(dimension);
      assertEquals("10.0" + suffix, dimension.toString());
    }

    // Test invalid suffixes
    final var invalidSuffixes = List.of("invalid", "xyz", "dpi", "ms", "s");
    for (java.lang.String suffix : invalidSuffixes) {
      final var dimension = Dimension._of("10" + suffix);
      assertUnset.accept(dimension);
    }
  }

  @Test
  void testEquality() {
    final var px1 = Dimension._of("100px");
    final var px2 = Dimension._of("100px");
    final var px3 = Dimension._of("200px");
    final var m1 = Dimension._of("100m");

    // Equality with same suffix
    assertTrue.accept(px1._eq(px2));
    assertFalse.accept(px1._eq(px3));
    assertUnset.accept(unsetDimension._eq(px1));
    assertUnset.accept(px1._eq(unsetDimension));

    // Equality with different suffix should return unset
    assertUnset.accept(px1._eq(m1));

    // Inequality
    assertFalse.accept(px1._neq(px2));
    assertTrue.accept(px1._neq(px3));
    assertUnset.accept(unsetDimension._neq(px1));
    assertUnset.accept(px1._neq(unsetDimension));
    assertUnset.accept(px1._neq(m1));

    // Java equals and hashCode
    assertEquals(px1, px2);
    assertNotEquals(px1, px3);
    assertNotEquals(px1, m1);
    assertEquals(px1.hashCode(), px2.hashCode());
    assertNotEquals(px1.hashCode(), px3.hashCode());
  }

  @Test
  void testComparison() {
    // Less than
    assertTrue.accept(DIMENSION_50PX._lt(DIMENSION_100PX));
    assertFalse.accept(DIMENSION_100PX._lt(DIMENSION_50PX));
    assertFalse.accept(DIMENSION_100PX._lt(DIMENSION_100PX));
    assertUnset.accept(unsetDimension._lt(DIMENSION_100PX));
    assertUnset.accept(DIMENSION_100PX._lt(unsetDimension));
    assertUnset.accept(DIMENSION_50PX._lt(DIMENSION_50M)); // Different suffix

    // Less than or equal
    assertTrue.accept(DIMENSION_50PX._lteq(DIMENSION_100PX));
    assertTrue.accept(DIMENSION_100PX._lteq(DIMENSION_100PX));
    assertFalse.accept(DIMENSION_150PX._lteq(DIMENSION_100PX));
    assertUnset.accept(unsetDimension._lteq(DIMENSION_100PX));
    assertUnset.accept(DIMENSION_100PX._lteq(unsetDimension));
    assertUnset.accept(DIMENSION_50PX._lteq(DIMENSION_50M)); // Different suffix

    // Greater than
    assertTrue.accept(DIMENSION_150PX._gt(DIMENSION_100PX));
    assertFalse.accept(DIMENSION_50PX._gt(DIMENSION_100PX));
    assertFalse.accept(DIMENSION_100PX._gt(DIMENSION_100PX));
    assertUnset.accept(unsetDimension._gt(DIMENSION_100PX));
    assertUnset.accept(DIMENSION_100PX._gt(unsetDimension));
    assertUnset.accept(DIMENSION_150PX._gt(DIMENSION_50M)); // Different suffix

    // Greater than or equal
    assertTrue.accept(DIMENSION_150PX._gteq(DIMENSION_100PX));
    assertTrue.accept(DIMENSION_100PX._gteq(DIMENSION_100PX));
    assertFalse.accept(DIMENSION_50PX._gteq(DIMENSION_100PX));
    assertUnset.accept(unsetDimension._gteq(DIMENSION_100PX));
    assertUnset.accept(DIMENSION_100PX._gteq(unsetDimension));
    assertUnset.accept(DIMENSION_150PX._gteq(DIMENSION_50M)); // Different suffix

    // Compare
    assertEquals(-1, (int) DIMENSION_50PX._cmp(DIMENSION_100PX).state);
    assertEquals(0, (int) DIMENSION_100PX._cmp(DIMENSION_100PX).state);
    assertEquals(1, (int) DIMENSION_150PX._cmp(DIMENSION_100PX).state);
    assertUnset.accept(unsetDimension._cmp(DIMENSION_100PX));
    assertUnset.accept(DIMENSION_100PX._cmp(unsetDimension));
    assertUnset.accept(DIMENSION_50PX._cmp(DIMENSION_50M)); // Different suffix
    assertUnset.accept(DIMENSION_100PX._cmp(new Any(){}));
  }

  @Test
  void testArithmeticOperators() {
    // Addition with same suffix
    final var addResult = DIMENSION_100PX._add(DIMENSION_50PX);
    assertEquals(150.0, addResult._prefix().state);
    assertEquals("px", addResult._suffix().state);
    assertUnset.accept(unsetDimension._add(DIMENSION_100PX));
    assertUnset.accept(DIMENSION_100PX._add(unsetDimension));

    // Addition with different suffix should return unset
    assertUnset.accept(DIMENSION_100PX._add(DIMENSION_100M));

    // Subtraction with same suffix
    final var subResult = DIMENSION_200PX._sub(DIMENSION_50PX);
    assertEquals(150.0, subResult._prefix().state);
    assertEquals("px", subResult._suffix().state);
    assertUnset.accept(unsetDimension._sub(DIMENSION_100PX));
    assertUnset.accept(DIMENSION_100PX._sub(unsetDimension));

    // Subtraction with different suffix should return unset
    assertUnset.accept(DIMENSION_200PX._sub(DIMENSION_100M));

    // Multiplication with Integer
    final var mulIntResult = DIMENSION_100PX._mul(int2);
    assertEquals(200.0, mulIntResult._prefix().state);
    assertEquals("px", mulIntResult._suffix().state);
    assertUnset.accept(unsetDimension._mul(int2));
    assertUnset.accept(DIMENSION_100PX._mul(new Integer()));

    // Division with Integer
    final var divIntResult = DIMENSION_200PX._div(int2);
    assertEquals(100.0, divIntResult._prefix().state);
    assertEquals("px", divIntResult._suffix().state);
    assertUnset.accept(unsetDimension._div(int2));
    assertUnset.accept(DIMENSION_200PX._div(new Integer()));

    // Multiplication with Float
    final var mulFloatResult = DIMENSION_100PX._mul(float2);
    assertEquals(200.0, mulFloatResult._prefix().state);
    assertEquals("px", mulFloatResult._suffix().state);
    assertUnset.accept(unsetDimension._mul(float2));
    assertUnset.accept(DIMENSION_100PX._mul(new Float()));

    // Division with Float
    final var divFloatResult = DIMENSION_200PX._div(float2);
    assertEquals(100.0, divFloatResult._prefix().state);
    assertEquals("px", divFloatResult._suffix().state);
    assertUnset.accept(unsetDimension._div(float2));
    assertUnset.accept(DIMENSION_200PX._div(new Float()));

    // Division returning Float (same suffix)
    final var divDimResult = DIMENSION_200PX._div(DIMENSION_100PX);
    assertEquals(2.0, divDimResult.state, 0.001);
    assertUnset.accept(unsetDimension._div(DIMENSION_100PX));
    assertUnset.accept(DIMENSION_200PX._div(unsetDimension));

    // Division with different suffix should return unset
    assertUnset.accept(DIMENSION_200PX._div(DIMENSION_100M));

    // Power with Integer
    final var powIntResult1 = DIMENSION_2PX._pow(int2);
    assertEquals(4.0, powIntResult1._prefix().state);
    assertEquals("px", powIntResult1._suffix().state);
    
    final var powIntResult2 = DIMENSION_2PX._pow(int3);
    assertEquals(8.0, powIntResult2._prefix().state);
    assertEquals("px", powIntResult2._suffix().state);
    
    final var powIntResult3 = DIMENSION_1PX._pow(int2);
    assertEquals(1.0, powIntResult3._prefix().state);
    assertEquals("px", powIntResult3._suffix().state);
    
    final var powIntResult4 = DIMENSION_2PX._pow(INT_0);
    assertEquals(1.0, powIntResult4._prefix().state);
    assertEquals("px", powIntResult4._suffix().state);
    
    assertUnset.accept(unsetDimension._pow(int2));
    assertUnset.accept(DIMENSION_2PX._pow(new Integer()));

    // Power with Float
    final var powFloatResult1 = DIMENSION_2PX._pow(float2);
    assertEquals(4.0, powFloatResult1._prefix().state);
    assertEquals("px", powFloatResult1._suffix().state);
    
    final var powFloatResult2 = DIMENSION_2PX._pow(FLOAT_1);
    assertEquals(2.0, powFloatResult2._prefix().state);
    assertEquals("px", powFloatResult2._suffix().state);
    
    final var powFloatResult3 = DIMENSION_4PX._pow(FLOAT_0_5);
    assertEquals(2.0, powFloatResult3._prefix().state, 0.001);
    assertEquals("px", powFloatResult3._suffix().state);
    
    final var powFloatResult4 = DIMENSION_2PX._pow(FLOAT_0);
    assertEquals(1.0, powFloatResult4._prefix().state);
    assertEquals("px", powFloatResult4._suffix().state);
    
    assertUnset.accept(unsetDimension._pow(float2));
    assertUnset.accept(DIMENSION_2PX._pow(new Float()));
  }

  @Test
  void testAssignmentOperators() {
    // Addition assignment with same suffix
    final var px1 = Dimension._of("100px");
    px1._addAss(Dimension._of("50px"));
    assertEquals(150.0, px1._prefix().state);
    assertEquals("px", px1._suffix().state);

    // Addition assignment with different suffix should unset
    final var px2 = Dimension._of("100px");
    px2._addAss(Dimension._of("50m"));
    assertUnset.accept(px2);

    // Subtraction assignment with same suffix
    final var px3 = Dimension._of("200px");
    px3._subAss(Dimension._of("50px"));
    assertEquals(150.0, px3._prefix().state);
    assertEquals("px", px3._suffix().state);

    // Subtraction assignment with different suffix should unset
    final var px4 = Dimension._of("200px");
    px4._subAss(Dimension._of("50m"));
    assertUnset.accept(px4);

    // Multiplication assignment with Integer
    final var px5 = Dimension._of("100px");
    px5._mulAss(int3);
    assertEquals(300.0, px5._prefix().state);
    assertEquals("px", px5._suffix().state);

    // Division assignment with Integer
    final var px6 = Dimension._of("300px");
    px6._divAss(int3);
    assertEquals(100.0, px6._prefix().state);
    assertEquals("px", px6._suffix().state);

    // Multiplication assignment with Float
    final var px7 = Dimension._of("100px");
    px7._mulAss(float2);
    assertEquals(200.0, px7._prefix().state, 0.001);
    assertEquals("px", px7._suffix().state);

    // Division assignment with Float
    final var px8 = Dimension._of("200px");
    px8._divAss(float2);
    assertEquals(100.0, px8._prefix().state, 0.001);
    assertEquals("px", px8._suffix().state);

    // Test unset propagation
    final var dimUnset = new Dimension();
    dimUnset._addAss(Dimension._of("100px"));
    assertUnset.accept(dimUnset);
  }

  @Test
  void testUnaryOperators() {
    // Negation
    final var px100 = Dimension._of("100px");
    final var negated = px100._negate();
    assertEquals(-100.0, negated._prefix().state);
    assertEquals("px", negated._suffix().state);
    assertUnset.accept(unsetDimension._negate());

    assertFalse.accept(unsetDimension._isSet());
    assertTrue.accept(px100._isSet());

    // Increment
    final var px1 = Dimension._of("100px");
    px1._inc();
    assertEquals(101.0, px1._prefix().state);
    assertEquals("px", px1._suffix().state);
    final var unsetInc = new Dimension();
    unsetInc._inc();
    assertUnset.accept(unsetInc);

    // Decrement
    final var px2 = Dimension._of("100px");
    px2._dec();
    assertEquals(99.0, px2._prefix().state);
    assertEquals("px", px2._suffix().state);
    final var unsetDec = new Dimension();
    unsetDec._dec();
    assertUnset.accept(unsetDec);

    // Absolute value
    final var absResult = DIMENSION_NEG_100PX._abs();
    assertEquals(100.0, absResult._prefix().state);
    assertEquals("px", absResult._suffix().state);
    final var pxPos = Dimension._of("100px");
    final var absResult2 = pxPos._abs();
    assertEquals(100.0, absResult2._prefix().state);
    assertEquals("px", absResult2._suffix().state);
    assertUnset.accept(unsetDimension._abs());

    // Square root
    final var px100Sqrt = Dimension._of("100px");
    final var sqrtResult = px100Sqrt._sqrt();
    assertEquals(10.0, sqrtResult._prefix().state, 0.001);
    assertEquals("px", sqrtResult._suffix().state);
    assertUnset.accept(unsetDimension._sqrt());

    // Square root of negative should return unset
    final var sqrtNegResult = DIMENSION_NEG_100PX._sqrt();
    assertUnset.accept(sqrtNegResult);
  }

  @Test
  void testUtilityMethods() {

    // Prefix (numeric value)
    assertEquals(1000.0, DIMENSION_1000PX._prefix().state);
    assertUnset.accept(unsetDimension._prefix());

    // Suffix (unit string)
    assertEquals("px", DIMENSION_1000PX._suffix().state);
    assertUnset.accept(unsetDimension._suffix());

    // Length
    assertEquals(8, DIMENSION_1000PX._len().state); // "1000.0px" = 8 characters
    assertEquals(0, unsetDimension._len().state);

    // String conversion
    assertEquals("1000.0px", DIMENSION_1000PX._string().state);
    assertEquals("", unsetDimension._string().state);

    // JSON operations
    final var px1000Json = DIMENSION_1000PX._json();
    assertSet.accept(px1000Json);

    assertUnset.accept(unsetDimension._json());
    assertEquals("1000.0px", DIMENSION_1000PX.toString());
    assertEquals("", unsetDimension.toString());

    // Test floating point precision
    final var pxFloat = Dimension._of("123.456px");
    assertEquals("123.456px", pxFloat.toString());
  }

  @Test
  void testCopyAndAssignmentOperators() {
    final var px100 = Dimension._of("100px");
    final var px200 = Dimension._of("200px");

    // Copy operation
    px100._copy(px200);
    assertEquals(200.0, px100._prefix().state);
    assertEquals("px", px100._suffix().state);

    // Copy unset
    final var pxSet = Dimension._of("300px");
    pxSet._copy(unsetDimension);
    assertUnset.accept(pxSet);

    // Replace operation
    final var px1 = Dimension._of("100px");
    final var px2 = Dimension._of("200px");
    px1._replace(px2);
    assertEquals(200.0, px1._prefix().state);
    assertEquals("px", px1._suffix().state);

    // Pipe operation (merge)
    final var px3 = Dimension._of("100px");
    final var px4 = Dimension._of("200px");
    px3._pipe(px4);
    assertEquals(300.0, px3._prefix().state); // Should be added
    assertEquals("px", px3._suffix().state);

    // Pipe with unset target
    final var dimUnset = new Dimension();
    final var px5 = Dimension._of("100px");
    dimUnset._pipe(px5);
    assertEquals(100.0, dimUnset._prefix().state); // Should assign
    assertEquals("px", dimUnset._suffix().state);

    // Merge operation
    final var px6 = Dimension._of("100px");
    final var px7 = Dimension._of("200px");
    px6._merge(px7);
    assertEquals(300.0, px6._prefix().state);
    assertEquals("px", px6._suffix().state);
  }

  @Test
  void testEdgeCases() {
    // Division by zero with Integer
    final var px100 = Dimension._of("100px");
    final var zeroInt = Integer._of(0);
    final var divByZeroResult = px100._div(zeroInt);
    assertUnset.accept(divByZeroResult);

    // Division assignment by zero
    final var px200 = Dimension._of("200px");
    px200._divAss(zeroInt);
    assertUnset.accept(px200);

    // Division by zero with Float
    final var zeroFloat = Float._of(0.0);
    final var divByZeroFloatResult = px100._div(zeroFloat);
    assertUnset.accept(divByZeroFloatResult);

    // Large values
    final var largeDim = Dimension._of("1000000px");
    assertSet.accept(largeDim);

    // Negative values
    assertEquals(-1000.0, DIMENSION_NEG_1000PX._prefix().state);
    assertTrue.accept(DIMENSION_NEG_1000PX._lt(DIMENSION_0PX));

    // String parsing edge cases
    final var invalidString1 = Dimension._of("invalid");
    assertUnset.accept(invalidString1);

    final var invalidString2 = Dimension._of("100"); // Missing suffix
    assertUnset.accept(invalidString2);

    final var invalidString3 = Dimension._of("px100"); // Wrong format
    assertUnset.accept(invalidString3);

    final var invalidString4 = Dimension._of("100.px"); // Invalid number
    assertUnset.accept(invalidString4);

    final var invalidString5 = Dimension._of("100invalidSuffix");
    assertUnset.accept(invalidString5);

    // Test null handling
    final var nullDim = Dimension._of(null);
    assertUnset.accept(nullDim);

    // Test floating point precision
    final var precisionDim = Dimension._of("123.456789px");
    assertEquals(123.456789, precisionDim._prefix().state, 0.000001);

    // Power edge cases
    
    // Negative base with even integer exponent should be positive
    final var negPowEven = DIMENSION_NEG_2PX._pow(int2);
    assertEquals(4.0, negPowEven._prefix().state);
    assertEquals("px", negPowEven._suffix().state);
    
    // Negative base with odd integer exponent should be negative
    final var negPowOdd = DIMENSION_NEG_2PX._pow(int3);
    assertEquals(-8.0, negPowOdd._prefix().state);
    assertEquals("px", negPowOdd._suffix().state);
    
    // Large exponent test
    final var largePow = DIMENSION_2PX._pow(Integer._of(10));
    assertEquals(1024.0, largePow._prefix().state);
    assertEquals("px", largePow._suffix().state);
    
    // Fractional exponent (cube root: 8^(1/3) = 2)
    final var cubeRoot = DIMENSION_8PX._pow(Float._of(1.0/3.0));
    assertEquals(2.0, cubeRoot._prefix().state, 0.001);
    assertEquals("px", cubeRoot._suffix().state);
    
    // Zero base with positive exponent
    final var zeroPowPos = DIMENSION_0PX._pow(int2);
    assertEquals(0.0, zeroPowPos._prefix().state);
    assertEquals("px", zeroPowPos._suffix().state);
  }

  @Test
  void testPipeLogic() {
    var mutatedValue = new Dimension();
    assertUnset.accept(mutatedValue);

    mutatedValue._pipe(unsetDimension);
    assertUnset.accept(mutatedValue);

    mutatedValue._pipe(DIMENSION_1PX);
    assertEquals(DIMENSION_1PX, mutatedValue);

    // Keep adding
    mutatedValue._pipe(DIMENSION_2PX);
    assertEquals(DIMENSION_3PX, mutatedValue);

    // Even if we pipe in something unset, for pipes this is ignored
    mutatedValue._pipe(unsetDimension);
    assertEquals(DIMENSION_3PX, mutatedValue);

    // Now just show a negative being added
    mutatedValue._pipe(DIMENSION_4PX._negate());
    assertEquals(Dimension._of("-1px"), mutatedValue);
  }

  private List<Consumer<Dimension>> getDimensionAssignmentOperations(final Dimension from) {
    return List.of(from::_addAss, from::_subAss);
  }

  private List<Consumer<Integer>> getIntegerAssignmentOperations(final Dimension from) {
    return List.of(from::_mulAss, from::_divAss);
  }

  private List<Consumer<Float>> getFloatAssignmentOperations(final Dimension from) {
    return List.of(from::_mulAss, from::_divAss);
  }

  @Test
  void testMutationOperatorsWithUnsetDimension() {
    final var mutatedValue = Dimension._of("100px");

    for (var operator : getDimensionAssignmentOperations(mutatedValue)) {
      operator.accept(unsetDimension);
      assertUnset.accept(mutatedValue);
      // Set it back again for next time around loop
      mutatedValue._copy(DIMENSION_100PX);
      assertEquals(DIMENSION_100PX, mutatedValue);
    }
  }

  @Test
  void testMutationOperatorsWithUnsetInteger() {
    final var mutatedValue = Dimension._of("100px");

    for (var operator : getIntegerAssignmentOperations(mutatedValue)) {
      operator.accept(new Integer());
      assertUnset.accept(mutatedValue);
      // Set it back again for next time around loop
      mutatedValue._copy(DIMENSION_100PX);
      assertEquals(DIMENSION_100PX, mutatedValue);
    }
  }

  @Test
  void testMutationOperatorsWithUnsetFloat() {
    final var mutatedValue = Dimension._of("100px");

    for (var operator : getFloatAssignmentOperations(mutatedValue)) {
      operator.accept(new Float());
      assertUnset.accept(mutatedValue);
      // Set it back again for next time around loop
      mutatedValue._copy(DIMENSION_100PX);
      assertEquals(DIMENSION_100PX, mutatedValue);
    }
  }

  @Test
  void testMutationOperators() {
    final var mutatedValue = Dimension._of("100px");
    mutatedValue._addAss(DIMENSION_1PX);
    assertEquals(Dimension._of("101px"), mutatedValue);

    mutatedValue._mulAss(int2);
    assertEquals(Dimension._of("202px"), mutatedValue);

    mutatedValue._divAss(int2);
    assertEquals(Dimension._of("101px"), mutatedValue);

    mutatedValue._subAss(DIMENSION_1PX);
    assertEquals(DIMENSION_100PX, mutatedValue);

    // Now Float args
    mutatedValue._mulAss(float2);
    assertEquals(Dimension._of("200px"), mutatedValue);

    mutatedValue._divAss(float2);
    assertEquals(DIMENSION_100PX, mutatedValue);
  }

  @Test
  void testReplaceAndCopyLogic() {
    var mutatedValue = Dimension._of("100px");
    assertEquals(DIMENSION_100PX, mutatedValue);

    mutatedValue._replace(DIMENSION_1PX);
    assertEquals(DIMENSION_1PX, mutatedValue);

    mutatedValue._replace(DIMENSION_2PX);
    assertEquals(DIMENSION_2PX, mutatedValue);

    mutatedValue._replace(unsetDimension);
    assertUnset.accept(mutatedValue);

    // Now just check that it can take a value after being unset
    mutatedValue._replace(DIMENSION_4PX);
    assertEquals(DIMENSION_4PX, mutatedValue);
  }

  @Test
  void testDifferentSuffixTypes() {
    // Test various suffix types work correctly
    final var kmDim = Dimension._of("5km");
    assertNotNull(kmDim);

    final var mDim = Dimension._of("5000m");
    final var cmDim = Dimension._of("500000cm");
    final var mmDim = Dimension._of("5000000mm");
    final var mileDim = Dimension._of("3.107mile");
    final var inDim = Dimension._of("196850in");
    final var pcDim = Dimension._of("11811pc");
    final var ptDim = Dimension._of("141732pt");
    final var pxDim = Dimension._of("188976px");
    final var emDim = Dimension._of("315em");
    final var exDim = Dimension._of("630ex");
    final var chDim = Dimension._of("567ch");
    final var remDim = Dimension._of("315rem");
    final var vwDim = Dimension._of("26.04vw");
    final var vhDim = Dimension._of("14.65vh");
    final var vminDim = Dimension._of("14.65vmin");
    final var vmaxDim = Dimension._of("26.04vmax");
    final var percentDim = Dimension._of("100%");

    // All should be set
    assertSet.accept(kmDim);
    assertSet.accept(mDim);
    assertSet.accept(cmDim);
    assertSet.accept(mmDim);
    assertSet.accept(mileDim);
    assertSet.accept(inDim);
    assertSet.accept(pcDim);
    assertSet.accept(ptDim);
    assertSet.accept(pxDim);
    assertSet.accept(emDim);
    assertSet.accept(exDim);
    assertSet.accept(chDim);
    assertSet.accept(remDim);
    assertSet.accept(vwDim);
    assertSet.accept(vhDim);
    assertSet.accept(vminDim);
    assertSet.accept(vmaxDim);
    assertSet.accept(percentDim);

    // Test that different suffixes cannot be combined
    assertUnset.accept(kmDim._add(mDim));
    assertUnset.accept(pxDim._add(percentDim));
    assertUnset.accept(emDim._add(vwDim));
  }
}