package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class MillisecondTest extends Common {

  final Millisecond unset = new Millisecond();
  final Millisecond zeroMs = Millisecond._of(0);
  final Millisecond oneThousandMs = Millisecond._of(1000);

  final Duration oneSecond = Duration._of("PT1S");

  final Integer int2 = Integer._of(2);
  final Integer int3 = Integer._of(3);
  final Float float2 = Float._of(2.0);

  @Test
  void testConstruction() {
    // Default constructor
    final var defaultConstructor = new Millisecond();
    assertUnset.accept(defaultConstructor);

    // String constructor - valid and invalid
    final var unset1 = Millisecond._of("not-a-millisecond");
    assertUnset.accept(unset1);
    final var unset2 = new Millisecond(new String());
    assertUnset.accept(unset2);

    final var checkMs1 = new Millisecond(String._of("100ms"));
    assertSet.accept(checkMs1);
    assertEquals("100ms", checkMs1.toString());

    // Copy constructor
    final var copyMs = new Millisecond(oneThousandMs);
    assertSet.accept(copyMs);
    assertEquals("1000ms", copyMs.toString());

    final var copyUnset = new Millisecond(unset);
    assertUnset.accept(copyUnset);

    // Duration constructor
    final var fromDuration = new Millisecond(oneSecond);
    assertSet.accept(fromDuration);
    assertEquals(1000, fromDuration._prefix().state);

    final var fromUnsetDuration = new Millisecond(new Duration());
    assertUnset.accept(fromUnsetDuration);

    // Integer constructor
    final var fromInteger = new Millisecond(Integer._of(750));
    assertSet.accept(fromInteger);
    assertEquals("750ms", fromInteger.toString());
    assertEquals(750, fromInteger._prefix().state);

    final var fromUnsetInteger = new Millisecond(new Integer());
    assertUnset.accept(fromUnsetInteger);

    // Static factory methods
    final var factoryMs = Millisecond._of(500);
    assertSet.accept(factoryMs);
    assertEquals("500ms", factoryMs.toString());

    final var factoryStringMs = Millisecond._of("750ms");
    assertSet.accept(factoryStringMs);
    assertEquals("750ms", factoryStringMs.toString());
  }

  @Test
  void testEquality() {
    final var ms1 = Millisecond._of(100);
    final var ms2 = Millisecond._of(100);
    final var ms3 = Millisecond._of(200);

    // Equality
    assertTrue.accept(ms1._eq(ms2));
    assertFalse.accept(ms1._eq(ms3));
    assertUnset.accept(unset._eq(ms1));
    assertUnset.accept(ms1._eq(unset));

    // Inequality
    assertFalse.accept(ms1._neq(ms2));
    assertTrue.accept(ms1._neq(ms3));
    assertUnset.accept(unset._neq(ms1));
    assertUnset.accept(ms1._neq(unset));

    // Java equals and hashCode
    assertEquals(ms1, ms2);
    assertNotEquals(ms1, ms3);
    assertEquals(ms1.hashCode(), ms2.hashCode());
    assertNotEquals(ms1.hashCode(), ms3.hashCode());
  }

  @Test
  void testComparison() {
    final var ms50 = Millisecond._of(50);
    final var ms100 = Millisecond._of(100);
    final var ms150 = Millisecond._of(150);

    // Less than
    assertTrue.accept(ms50._lt(ms100));
    assertFalse.accept(ms100._lt(ms50));
    assertFalse.accept(ms100._lt(ms100));
    assertUnset.accept(unset._lt(ms100));
    assertUnset.accept(ms100._lt(unset));

    // Less than or equal
    assertTrue.accept(ms50._lteq(ms100));
    assertTrue.accept(ms100._lteq(ms100));
    assertFalse.accept(ms150._lteq(ms100));
    assertUnset.accept(unset._lteq(ms100));
    assertUnset.accept(ms100._lteq(unset));

    // Greater than
    assertTrue.accept(ms150._gt(ms100));
    assertFalse.accept(ms50._gt(ms100));
    assertFalse.accept(ms100._gt(ms100));
    assertUnset.accept(unset._gt(ms100));
    assertUnset.accept(ms100._gt(unset));

    // Greater than or equal
    assertTrue.accept(ms150._gteq(ms100));
    assertTrue.accept(ms100._gteq(ms100));
    assertFalse.accept(ms50._gteq(ms100));
    assertUnset.accept(unset._gteq(ms100));
    assertUnset.accept(ms100._gteq(unset));

    // Compare
    assertEquals(-1, (int) ms50._cmp(ms100).state);
    assertEquals(0, (int) ms100._cmp(ms100).state);
    assertEquals(1, (int) ms150._cmp(ms100).state);
    assertUnset.accept(unset._cmp(ms100));
    assertUnset.accept(ms100._cmp(unset));
    assertUnset.accept(ms100._cmp(new Any(){}));
  }

  @Test
  void testArithmeticOperators() {
    final var ms100 = Millisecond._of(100);
    final var ms200 = Millisecond._of(200);
    final var ms50 = Millisecond._of(50);

    // Addition with Millisecond
    final var addResult = ms100._add(ms50);
    assertEquals(150, addResult._prefix().state);
    assertUnset.accept(unset._add(ms100));
    assertUnset.accept(ms100._add(unset));

    // Subtraction with Millisecond
    final var subResult = ms200._sub(ms50);
    assertEquals(150, subResult._prefix().state);
    assertUnset.accept(unset._sub(ms100));
    assertUnset.accept(ms100._sub(unset));

    // Addition with Duration
    final var addDurationResult = ms100._add(oneSecond);
    assertEquals(1100, addDurationResult._prefix().state);
    assertUnset.accept(unset._add(oneSecond));
    assertUnset.accept(ms100._add(new Duration()));

    // Subtraction with Duration
    final var subDurationResult = oneThousandMs._sub(oneSecond);
    assertEquals(0, subDurationResult._prefix().state);
    assertUnset.accept(unset._sub(oneSecond));
    assertUnset.accept(ms100._sub(new Duration()));

    // Multiplication with Integer
    final var mulIntResult = ms100._mul(int2);
    assertEquals(200, mulIntResult._prefix().state);
    assertUnset.accept(unset._mul(int2));
    assertUnset.accept(ms100._mul(new Integer()));

    // Division with Integer
    final var divIntResult = ms200._div(int2);
    assertEquals(100, divIntResult._prefix().state);
    assertUnset.accept(unset._div(int2));
    assertUnset.accept(ms200._div(new Integer()));

    // Multiplication with Float
    final var mulFloatResult = ms100._mul(float2);
    assertEquals(200, mulFloatResult._prefix().state);
    assertUnset.accept(unset._mul(float2));
    assertUnset.accept(ms100._mul(new Float()));

    // Division with Float
    final var divFloatResult = ms200._div(float2);
    assertEquals(100, divFloatResult._prefix().state);
    assertUnset.accept(unset._div(float2));
    assertUnset.accept(ms200._div(new Float()));

    // Division returning Float
    final var divMsResult = ms200._div(ms100);
    assertEquals(2.0, divMsResult.state, 0.001);
    assertUnset.accept(unset._div(ms100));
    assertUnset.accept(ms200._div(unset));

    // Modulus with positive numbers
    final var ms300 = Millisecond._of(300);
    final var ms100Mod = Millisecond._of(100);
    final var modResult = ms300._mod(ms100Mod);
    assertEquals(0, modResult.state);
    final var modResult2 = Millisecond._of(250)._mod(ms100Mod);
    assertEquals(50, modResult2.state);
    assertUnset.accept(unset._mod(ms100Mod));
    assertUnset.accept(ms300._mod(unset));

    // Modulus with negative numbers
    final var negMs300 = Millisecond._of(-300);
    final var negMs100 = Millisecond._of(-100);
    final var modNegResult1 = negMs300._mod(ms100Mod);  // -300 % 100
    assertEquals(0, modNegResult1.state);
    final var modNegResult2 = Millisecond._of(-250)._mod(ms100Mod);  // -250 % 100
    assertEquals(50, modNegResult2.state);
    final var modNegResult3 = ms300._mod(negMs100);  // 300 % -100
    assertEquals(0, modNegResult3.state);
    final var modNegResult4 = negMs300._mod(negMs100);  // -300 % -100
    assertEquals(0, modNegResult4.state);
    // Additional negative modulus edge cases
    final var modNegResult5 = Millisecond._of(-17)._mod(Millisecond._of(5));  // -17 % 5
    assertEquals(3, modNegResult5.state);
    final var modNegResult6 = Millisecond._of(17)._mod(Millisecond._of(-5));  // 17 % -5
    assertEquals(-3, modNegResult6.state);
    final var modNegResult7 = Millisecond._of(-17)._mod(Millisecond._of(-5));  // -17 % -5
    assertEquals(-2, modNegResult7.state);

    // Remainder with positive numbers
    final var remResult = ms300._rem(ms100Mod);
    assertEquals(0, remResult.state);
    assertUnset.accept(unset._rem(ms100Mod));
    assertUnset.accept(ms300._rem(unset));

    // Remainder with negative numbers
    final var remNegResult1 = negMs300._rem(ms100Mod);  // -300 rem 100
    assertEquals(0, remNegResult1.state);
    final var remNegResult2 = Millisecond._of(-250)._rem(ms100Mod);  // -250 rem 100
    assertEquals(-50, remNegResult2.state);
    final var remNegResult3 = ms300._rem(negMs100);  // 300 rem -100
    assertEquals(0, remNegResult3.state);
    final var remNegResult4 = negMs300._rem(negMs100);  // -300 rem -100
    assertEquals(0, remNegResult4.state);
    // Additional negative remainder edge cases
    final var remNegResult5 = Millisecond._of(-17)._rem(Millisecond._of(5));  // -17 rem 5
    assertEquals(-2, remNegResult5.state);
    final var remNegResult6 = Millisecond._of(17)._rem(Millisecond._of(-5));  // 17 rem -5
    assertEquals(2, remNegResult6.state);
    final var remNegResult7 = Millisecond._of(-17)._rem(Millisecond._of(-5));  // -17 rem -5
    assertEquals(-2, remNegResult7.state);
  }

  @Test
  void testAssignmentOperators() {
    // Addition assignment with Millisecond
    final var ms1 = Millisecond._of(100);
    ms1._addAss(Millisecond._of(50));
    assertEquals(150, ms1._prefix().state);

    // Subtraction assignment with Millisecond
    final var ms2 = Millisecond._of(200);
    ms2._subAss(Millisecond._of(50));
    assertEquals(150, ms2._prefix().state);

    // Addition assignment with Duration
    final var ms3 = Millisecond._of(100);
    ms3._addAss(oneSecond);
    assertEquals(1100, ms3._prefix().state);

    // Subtraction assignment with Duration
    final var ms4 = Millisecond._of(1500);
    ms4._subAss(oneSecond);
    assertEquals(500, ms4._prefix().state);

    // Multiplication assignment with Integer
    final var ms5 = Millisecond._of(100);
    ms5._mulAss(int3);
    assertEquals(300, ms5._prefix().state);

    // Division assignment with Integer
    final var ms6 = Millisecond._of(300);
    ms6._divAss(int3);
    assertEquals(100, ms6._prefix().state);

    // Multiplication assignment with Float
    final var ms7 = Millisecond._of(100);
    ms7._mulAss(float2);
    assertEquals(200, ms7._prefix().state);

    // Division assignment with Float
    final var ms8 = Millisecond._of(200);
    ms8._divAss(float2);
    assertEquals(100, ms8._prefix().state);

    // Test unset propagation
    final var msUnset = new Millisecond();
    msUnset._addAss(Millisecond._of(100));
    assertUnset.accept(msUnset);
  }

  @Test
  void testUnaryOperators() {
    // Negation
    final var ms100 = Millisecond._of(100);
    final var negated = ms100._negate();
    assertEquals(-100, negated._prefix().state);
    assertUnset.accept(unset._negate());

    assertFalse.accept(unset._isSet());
    assertTrue.accept(ms100._isSet());

    // Increment
    final var ms1 = Millisecond._of(100);
    ms1._inc();
    assertEquals(101, ms1._prefix().state);
    final var unsetInc = new Millisecond();
    unsetInc._inc();
    assertUnset.accept(unsetInc);

    // Decrement
    final var ms2 = Millisecond._of(100);
    ms2._dec();
    assertEquals(99, ms2._prefix().state);
    final var unsetDec = new Millisecond();
    unsetDec._dec();
    assertUnset.accept(unsetDec);

    // Absolute value
    final var msNeg = Millisecond._of(-100);
    final var absResult = msNeg._abs();
    assertEquals(100, absResult._prefix().state);
    final var msPos = Millisecond._of(100);
    final var absResult2 = msPos._abs();
    assertEquals(100, absResult2._prefix().state);
    assertUnset.accept(unset._abs());

    // Square root
    final var ms100Sqrt = Millisecond._of(100);
    final var sqrtResult = ms100Sqrt._sqrt();
    assertEquals(10.0, sqrtResult.state, 0.001);
    assertUnset.accept(unset._sqrt());
  }

  @Test
  void testUtilityMethods() {
    // Duration conversion
    final var duration = oneThousandMs.duration();
    assertSet.accept(duration);
    assertEquals(1, duration._getAsSeconds());
    assertUnset.accept(unset.duration());

    // Promotion to Duration
    final var promoted = oneThousandMs._promote();
    assertSet.accept(promoted);
    assertEquals(1, promoted._getAsSeconds());

    // Prefix (numeric value)
    assertEquals(1000, oneThousandMs._prefix().state);
    assertUnset.accept(unset._prefix());

    // Suffix (unit string)
    assertEquals("ms", oneThousandMs._suffix().state);
    assertUnset.accept(unset._suffix());

    // Length
    assertEquals(6, oneThousandMs._len().state); // "1000ms" = 6 characters
    assertEquals(0, unset._len().state);

    // String conversion
    assertEquals("1000ms", oneThousandMs._string().state);
    assertEquals("", unset._string().state);
    assertEquals("1000ms", oneThousandMs.toString());
    assertEquals("", unset.toString());
  }

  @Test
  void testAsJson() {
    // Test JSON conversion with set values
    final var oneThousandMsJson = oneThousandMs._json();
    assertNotNull(oneThousandMsJson);
    assertSet.accept(oneThousandMsJson);

    final var zeroMsJson = zeroMs._json();
    assertSet.accept(zeroMsJson);

    // Test JSON conversion with unset value
    assertUnset.accept(unset._json());
  }

  @Test
  void testCopyAndAssignmentOperators() {
    final var ms100 = Millisecond._of(100);
    final var ms200 = Millisecond._of(200);

    // Copy operation
    ms100._copy(ms200);
    assertEquals(200, ms100._prefix().state);

    // Copy unset
    final var msSet = Millisecond._of(300);
    msSet._copy(unset);
    assertUnset.accept(msSet);

    // Replace operation
    final var ms1 = Millisecond._of(100);
    final var ms2 = Millisecond._of(200);
    ms1._replace(ms2);
    assertEquals(200, ms1._prefix().state);

    // Pipe operation (merge)
    final var ms3 = Millisecond._of(100);
    final var ms4 = Millisecond._of(200);
    ms3._pipe(ms4);
    assertEquals(300, ms3._prefix().state); // Should be added

    // Pipe with unset target
    final var msUnset = new Millisecond();
    final var ms5 = Millisecond._of(100);
    msUnset._pipe(ms5);
    assertEquals(100, msUnset._prefix().state); // Should assign

    // Merge operation
    final var ms6 = Millisecond._of(100);
    final var ms7 = Millisecond._of(200);
    ms6._merge(ms7);
    assertEquals(300, ms6._prefix().state);
  }

  @Test
  void testEdgeCases() {
    // Division by zero with Integer
    final var ms100 = Millisecond._of(100);
    final var zeroInt = Integer._of(0);
    final var divByZeroResult = ms100._div(zeroInt);
    assertUnset.accept(divByZeroResult);

    // Division assignment by zero
    final var ms200 = Millisecond._of(200);
    ms200._divAss(zeroInt);
    assertUnset.accept(ms200);

    // Division by zero with Float
    final var zeroFloat = Float._of(0.0);
    final var divByZeroFloatResult = ms100._div(zeroFloat);
    assertUnset.accept(divByZeroFloatResult);

    // Large values
    final var largeMs = Millisecond._of(Long.MAX_VALUE / 2);
    assertSet.accept(largeMs);

    // Negative values
    final var negMs = Millisecond._of(-1000);
    assertEquals(-1000, negMs._prefix().state);
    assertTrue.accept(negMs._lt(zeroMs));

    // String parsing edge cases
    final var invalidString1 = Millisecond._of("invalid");
    assertUnset.accept(invalidString1);

    final var invalidString2 = Millisecond._of("100"); // Missing "ms"
    assertUnset.accept(invalidString2);

    final var invalidString3 = Millisecond._of("ms100"); // Wrong format
    assertUnset.accept(invalidString3);
  }

}