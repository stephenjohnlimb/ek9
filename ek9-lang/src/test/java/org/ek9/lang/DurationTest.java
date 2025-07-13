package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class DurationTest extends Common {

  final Duration unset = new Duration();
  final Duration zeroDuration = Duration._of("PT0S");
  final Duration oneSecond = Duration._of("PT1S");
  final Duration oneMinute = Duration._of("PT1M");
  final Duration oneHour = Duration._of("PT1H");
  final Duration oneDay = Duration._of("P1D");
  final Duration oneMonth = Duration._of("P1M");
  final Duration oneYear = Duration._of("P1Y");
  final Duration complex = Duration._of("P1Y2M3DT4H5M6S");
  final Duration negative30Seconds = Duration._of("-PT30S");
  final Duration alsoNegative30Seconds = Duration._of("PT-30S");
  final Duration largeValue = Duration._of("P10Y5M15DT12H30M45S");

  //OK so what does this actually mean!
  //It means: P2Y5MT11H30M
  final Duration mixedNegative = Duration._of("-P-2Y-5MT-12H30M");

  final Integer int1 = Integer._of(1);
  final Integer int2 = Integer._of(2);
  final Integer int3 = Integer._of(3);

  final Float float2 = Float._of(2.0);
  final Float float0Point5 = Float._of(0.5);

  @Test
  void testConstruction() {
    // Default constructor
    final var defaultConstructor = new Duration();
    assertUnset.accept(defaultConstructor);

    // String constructor - valid and invalid
    final var unset1 = Duration._of("not-a-duration");
    assertUnset.accept(unset1);
    final var unset2 = new Duration(new String());
    assertUnset.accept(unset2);

    final var checkDuration1 = new Duration(String._of("PT1H30M"));
    assertSet.accept(checkDuration1);
    assertEquals("PT1H30M", checkDuration1.toString());

    // Copy constructor
    final var againOneHour = new Duration(oneHour);
    assertSet.accept(againOneHour);
    assertTrue.accept(oneHour._eq(againOneHour));

    // Test various ISO 8601 formats
    final var seconds30 = Duration._of("PT30S");
    assertSet.accept(seconds30);
    assertEquals(Integer._of(30), seconds30.seconds());

    final var minutes5 = Duration._of("PT5M");
    assertSet.accept(minutes5);
    assertEquals(Integer._of(5), minutes5.minutes());

    final var hours2 = Duration._of("PT2H");
    assertSet.accept(hours2);
    assertEquals(Integer._of(2), hours2.hours());

    final var days7 = Duration._of("P7D");
    assertSet.accept(days7);
    assertEquals(Integer._of(7), days7.days());
  }

  @Test
  void testArithmeticOperators() {

    // Addition with Duration
    final var sum1 = oneSecond._add(oneMinute);
    assertSet.accept(sum1);
    assertEquals(Integer._of(61), sum1.seconds());

    final var sum2 = oneHour._add(oneDay);
    assertSet.accept(sum2);
    assertEquals(Integer._of(25), sum2.hours());

    assertUnset.accept(oneSecond._add(unset));
    assertUnset.accept(unset._add(oneSecond));

    // Subtraction with Duration
    final var diff1 = oneMinute._sub(oneSecond);
    assertSet.accept(diff1);
    assertEquals(Integer._of(59), diff1.seconds());

    final var diff2 = oneDay._sub(oneHour);
    assertSet.accept(diff2);
    assertEquals(Integer._of(23), diff2.hours());

    assertUnset.accept(oneSecond._sub(unset));
    assertUnset.accept(unset._sub(oneSecond));

    // Multiplication with Integer
    final var mult1 = oneSecond._mul(int3);
    assertSet.accept(mult1);
    assertEquals(Integer._of(3), mult1.seconds());

    final var mult2 = oneHour._mul(int2);
    assertSet.accept(mult2);
    assertEquals(Integer._of(2), mult2.hours());

    assertUnset.accept(oneSecond._mul(new Integer()));
    assertUnset.accept(unset._mul(int2));

    // Multiplication with Float
    final var multFloat1 = oneSecond._mul(float2);
    assertSet.accept(multFloat1);
    assertEquals(Integer._of(2), multFloat1.seconds());

    final var multFloat2 = oneHour._mul(float0Point5);
    assertSet.accept(multFloat2);
    assertEquals(Integer._of(30), multFloat2.minutes());

    assertUnset.accept(oneSecond._mul(new Float()));
    assertUnset.accept(unset._mul(float2));

    // Division with Integer
    final var div1 = Duration._of("PT6S")._div(int3);
    assertSet.accept(div1);
    assertEquals(Integer._of(2), div1.seconds());

    final var div2 = Duration._of("PT4H")._div(int2);
    assertSet.accept(div2);
    assertEquals(Integer._of(2), div2.hours());

    //Check division by zero
    assertUnset.accept(oneSecond._div(Integer._of(0)));
    assertUnset.accept(oneSecond._div(new Integer()));
    assertUnset.accept(unset._div(int2));

    // Division with Float
    final var divFloat1 = Duration._of("PT4S")._div(float2);
    assertSet.accept(divFloat1);
    assertEquals(Integer._of(2), divFloat1.seconds());

    //Check division by zero
    assertUnset.accept(oneSecond._div(Float._of(0.0)));
    assertUnset.accept(oneSecond._div(new Float()));
    assertUnset.accept(unset._div(float2));
  }

  @Test
  void testNegation() {
    //If we add a negative version of the value we should end up with no duration (in terms of time).
    final var expectNoDuration = mixedNegative._add(mixedNegative._negate());
    assertEquals(Duration._of("PT0S"), expectNoDuration);
  }

  @Test
  void testAssignmentOperators() {

    // Addition assignment
    var mutable1 = Duration._of("PT30S");
    mutable1._addAss(Duration._of("PT30S"));
    assertEquals(Integer._of(60), mutable1.seconds());

    mutable1 = new Duration();
    mutable1._addAss(oneSecond);
    assertUnset.accept(mutable1);

    // Subtraction assignment
    var mutable2 = Duration._of("PT2M");
    mutable2._subAss(Duration._of("PT30S"));
    assertEquals(Integer._of(90), mutable2.seconds()); // 1 minute 30 seconds = 90 seconds

    mutable2 = new Duration();
    mutable2._subAss(oneSecond);
    assertUnset.accept(mutable2);

    // Multiplication assignment with Integer
    var mutable3 = Duration._of("PT15S");
    mutable3._mulAss(int2);
    assertEquals(Duration._of("PT30S"), mutable3);

    mutable3 = new Duration();
    mutable3._mulAss(int2);
    assertUnset.accept(mutable3);

    // Multiplication assignment with Float
    var mutable4 = Duration._of("PT20S");
    mutable4._mulAss(float0Point5);
    assertEquals(Duration._of("PT10S"), mutable4);

    mutable4 = new Duration();
    mutable4._mulAss(float2);
    assertUnset.accept(mutable4);

    // Division assignment with Integer
    var mutable5 = Duration._of("PT60S");
    mutable5._divAss(int2);
    assertEquals(Duration._of("PT30S"), mutable5);

    mutable5 = new Duration();
    mutable5._divAss(int2);
    assertUnset.accept(mutable5);

    // Division assignment with Float
    var mutable6 = Duration._of("PT40S");
    mutable6._divAss(float2);
    assertEquals(Duration._of("PT20S"), mutable6);

    mutable6 = new Duration();
    mutable6._divAss(float2);
    assertUnset.accept(mutable6);

    //Check that div assignment with zero results in unset

    var mutable7 = Duration._of("PT40S");
    mutable7._divAss(Integer._of(0));
    assertUnset.accept(mutable7);

    var mutable8 = Duration._of("PT40S");
    mutable8._divAss(Integer._of(0));
    assertUnset.accept(mutable8);

  }

  @Test
  void testTimeUnitConversion() {
    // Test with unset
    assertUnset.accept(unset.seconds());
    assertUnset.accept(unset.minutes());
    assertUnset.accept(unset.hours());
    assertUnset.accept(unset.days());
    assertUnset.accept(unset.months());
    assertUnset.accept(unset.years());

    // Test simple conversions
    assertEquals(Integer._of(1), oneSecond.seconds());
    assertEquals(Integer._of(60), oneMinute.seconds());
    assertEquals(Integer._of(3600), oneHour.seconds());
    assertEquals(Integer._of(86400), oneDay.seconds());

    assertEquals(Integer._of(1), oneMinute.minutes());
    assertEquals(Integer._of(60), oneHour.minutes());
    assertEquals(Integer._of(1440), oneDay.minutes());

    assertEquals(Integer._of(1), oneHour.hours());
    assertEquals(Integer._of(24), oneDay.hours());

    assertEquals(Integer._of(1), oneDay.days());

    // Test complex duration - just verify it's set and positive
    final var complexSeconds = complex.seconds();
    assertSet.accept(complexSeconds);
    // The exact calculation depends on the Duration implementation, but it should be a large positive number
    assertTrue.accept(complexSeconds._gt(Integer._of(0)));
  }

  @Test
  void testComparison() {
    final var oneSecondAgain = Duration._of("PT1S");

    // Equality operators
    assertTrue.accept(oneSecond._eq(oneSecondAgain));
    assertFalse.accept(oneSecond._neq(oneSecondAgain));

    assertUnset.accept(oneSecond._eq(unset));
    assertUnset.accept(unset._eq(unset));

    assertUnset.accept(oneSecond._neq(unset));
    assertUnset.accept(unset._neq(unset));

    // Comparison operators
    assertTrue.accept(oneSecond._lt(oneMinute));
    assertFalse.accept(oneMinute._lt(oneSecond));
    assertUnset.accept(oneSecond._lt(unset));
    assertUnset.accept(unset._lt(unset));

    assertTrue.accept(oneMinute._gt(oneSecond));
    assertUnset.accept(oneMinute._gt(unset));
    assertUnset.accept(unset._gt(oneSecond));

    assertTrue.accept(oneSecond._lteq(oneSecondAgain));
    assertUnset.accept(oneMinute._lteq(unset));
    assertUnset.accept(unset._lteq(oneSecond));

    assertTrue.accept(oneSecond._gteq(oneSecondAgain));
    assertUnset.accept(oneMinute._gteq(unset));
    assertUnset.accept(unset._gteq(oneSecond));

    // Comparison operator
    assertEquals(Integer._of(0), oneSecond._cmp(oneSecondAgain));
    assertEquals(Integer._of(-1), oneSecond._cmp(oneMinute));
    assertEquals(Integer._of(1), oneMinute._cmp(oneSecond));

    assertUnset.accept(unset._cmp(oneSecond));
    assertUnset.accept(oneMinute._cmp(new Any() {
    }));
  }

  @Test
  void testPipeLogic() {
    // Pipe into unset duration (should assign)
    var mutable1 = new Duration();
    mutable1._pipe(oneSecond);
    assertTrue.accept(mutable1._eq(oneSecond));

    // Pipe into set duration (should add)
    var mutable2 = Duration._of("PT30S");
    mutable2._pipe(Duration._of("PT30S"));
    assertEquals(Integer._of(60), mutable2.seconds());

    // Pipe with unset source
    var mutable3 = Duration._of("PT1S");
    mutable3._pipe(unset);
    assertEquals(Integer._of(1), mutable3.seconds()); // Should remain unchanged

    // Chain piping
    var mutable4 = new Duration();
    mutable4._pipe(oneSecond);
    mutable4._pipe(oneSecond);
    mutable4._pipe(oneSecond);
    assertEquals(Integer._of(3), mutable4.seconds());
  }

  @Test
  void testStringParsing() {
    // Test various ISO 8601 formats
    assertEquals("PT0S", zeroDuration.toString());
    assertEquals("PT1S", oneSecond.toString());
    assertEquals("PT1M", oneMinute.toString());
    assertEquals("PT1H", oneHour.toString());
    assertEquals("P1D", oneDay.toString());
    assertEquals("P1M", oneMonth.toString());
    assertEquals("P1Y", oneYear.toString());
    assertEquals(alsoNegative30Seconds.toString(), negative30Seconds.toString());
    assertEquals("P2Y5MT11H30M", mixedNegative.toString());


    // Test complex format
    assertSet.accept(complex);
    assertEquals("P1Y2M3DT4H5M6S", complex.toString());

    // Test round-trip parsing
    final var originalString = "P2Y6M15DT8H45M30S";
    final var parsed = Duration._of(originalString);
    assertSet.accept(parsed);
    assertEquals(originalString, parsed.toString());

    // Test partial formats
    final var onlyDays = Duration._of("P5D");
    assertSet.accept(onlyDays);
    assertEquals("P5D", onlyDays.toString());

    final var onlyTime = Duration._of("PT2H30M");
    assertSet.accept(onlyTime);
    assertEquals("PT2H30M", onlyTime.toString());
  }

  @Test
  void testEdgeCases() {
    // Zero duration
    assertSet.accept(zeroDuration);
    assertEquals(Integer._of(0), zeroDuration.seconds());
    assertEquals(Integer._of(0), zeroDuration.minutes());
    assertEquals(Integer._of(0), zeroDuration.hours());

    // Very large duration
    assertSet.accept(largeValue);
    final var largeSeconds = largeValue.seconds();
    assertSet.accept(largeSeconds);

    // Test static factory methods
    final var fromSeconds = Duration._of(3661L); // 1 hour, 1 minute, 1 second
    assertSet.accept(fromSeconds);
    assertEquals(Integer._of(3661), fromSeconds.seconds());
    assertEquals(Integer._of(61), fromSeconds.minutes());
    assertEquals(Integer._of(1), fromSeconds.hours());

    final var fromJavaDuration = Duration._of(java.time.Duration.ofMinutes(90));
    assertSet.accept(fromJavaDuration);
    assertEquals(Integer._of(90), fromJavaDuration.minutes());

    // Test with Period and Duration
    final var fromBoth = Duration._of(java.time.Duration.ofHours(2), java.time.Period.ofDays(1));
    assertSet.accept(fromBoth);
    assertEquals(Integer._of(26), fromBoth.hours()); // 24 + 2
  }

  @Test
  void testCopyOperations() {
    // Copy logic
    var mutatedValue = Duration._of("PT30S");
    assertNotNull(mutatedValue);
    mutatedValue._copy(oneMinute);
    assertTrue.accept(mutatedValue._eq(oneMinute));
    mutatedValue._copy(unset);
    assertUnset.accept(mutatedValue);
  }

  @Test
  void testDurationRanges() {
    // Test chronological ordering: oneSecond < oneMinute < oneHour < oneDay
    assertTrue.accept(oneSecond._lt(oneMinute));
    assertTrue.accept(oneMinute._lt(oneHour));
    assertTrue.accept(oneHour._lt(oneDay));
    // Note: Month and year comparisons may not work as expected due to approximations

    // Test comparison operators across ranges
    assertEquals(Integer._of(1), oneMinute._cmp(oneSecond));
    assertEquals(Integer._of(-1), oneSecond._cmp(oneMinute));

    // Test arithmetic across ranges
    final var dayPlusHour = oneDay._add(oneHour);
    assertSet.accept(dayPlusHour);
    assertEquals(Integer._of(25), dayPlusHour.hours());
  }

  @Test
  void testUtilityMethods() {
    // String operations
    assertUnset.accept(unset._string());
    assertEquals(String._of("PT1S"), oneSecond._string());
    assertEquals(String._of("PT1M"), oneMinute._string());
    assertEquals(String._of("P1Y2M3DT4H5M6S"), complex._string());

    assertEquals("PT1S", oneSecond.toString());
    assertEquals("PT1M", oneMinute.toString());
    assertEquals("", unset.toString());

    // Hash code
    assertEquals(oneSecond.hashCode(), oneSecond.hashCode());
    assertNotEquals(oneSecond.hashCode(), oneMinute.hashCode());

    // Test equality with different but equivalent durations
    final var minute60Seconds = Duration._of("PT60S");
    final var minute1Minute = Duration._of("PT1M");
    assertEquals(minute60Seconds.seconds(), minute1Minute.seconds());
    // Note: These might not be equal objects due to internal representation
    // but they should have the same seconds value
  }

  @Test
  void testMathematicalProperties() {
    // Test associativity: (a + b) + c = a + (b + c)
    final var a = Duration._of("PT10S");
    final var b = Duration._of("PT20S");
    final var c = Duration._of("PT30S");

    final var left = a._add(b)._add(c);
    final var right = a._add(b._add(c));
    assertEquals(left.seconds(), right.seconds());

    // Test commutativity: a + b = b + a
    final var sum1 = a._add(b);
    final var sum2 = b._add(a);
    assertEquals(sum1.seconds(), sum2.seconds());

    // Test distributivity: a * (b + c) = a * b + a * c (conceptually)
    final var sum = Duration._of("PT50S"); // 20 + 30
    final var mult1 = sum._mul(int2);
    final var mult2 = b._mul(int2)._add(c._mul(int2));
    assertEquals(mult1.seconds(), mult2.seconds());

    // Test identity: a + 0 = a
    final var identity = a._add(zeroDuration);
    assertEquals(a.seconds(), identity.seconds());

    // Test multiplication by 1: a * 1 = a
    final var mult = a._mul(int1);
    assertEquals(a.seconds(), mult.seconds());
  }
}