package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class DurationTest extends Common {

  final Duration unsetDuration = new Duration();
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

  // Use INT_1, INT_2, INT_3, FLOAT_2_0, FLOAT_0_5 from Common base class

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
    assertEquals(INT_30, seconds30.seconds());

    final var minutes5 = Duration._of("PT5M");
    assertSet.accept(minutes5);
    assertEquals(INT_5, minutes5.minutes());

    final var hours2 = Duration._of("PT2H");
    assertSet.accept(hours2);
    assertEquals(INT_2, hours2.hours());

    final var days7 = Duration._of("P7D");
    assertSet.accept(days7);
    assertEquals(INT_7, days7.days());
  }

  @Test
  void testArithmeticOperators() {

    // Addition with Duration
    final var sum1 = oneSecond._add(oneMinute);
    assertSet.accept(sum1);
    assertEquals(INT_61, sum1.seconds());

    final var sum2 = oneHour._add(oneDay);
    assertSet.accept(sum2);
    assertEquals(INT_25, sum2.hours());

    assertUnset.accept(oneSecond._add(unsetDuration));
    assertUnset.accept(unsetDuration._add(oneSecond));

    // Subtraction with Duration
    final var diff1 = oneMinute._sub(oneSecond);
    assertSet.accept(diff1);
    assertEquals(INT_59, diff1.seconds());

    final var diff2 = oneDay._sub(oneHour);
    assertSet.accept(diff2);
    assertEquals(INT_23, diff2.hours());

    assertUnset.accept(oneSecond._sub(unsetDuration));
    assertUnset.accept(unsetDuration._sub(oneSecond));

    // Multiplication with Integer
    final var mult1 = oneSecond._mul(INT_3);
    assertSet.accept(mult1);
    assertEquals(INT_3, mult1.seconds());

    final var mult2 = oneHour._mul(INT_2);
    assertSet.accept(mult2);
    assertEquals(INT_2, mult2.hours());

    assertUnset.accept(oneSecond._mul(new Integer()));
    assertUnset.accept(unsetDuration._mul(INT_2));

    // Multiplication with Float
    final var multFloat1 = oneSecond._mul(FLOAT_2_0);
    assertSet.accept(multFloat1);
    assertEquals(INT_2, multFloat1.seconds());

    final var multFloat2 = oneHour._mul(FLOAT_0_5);
    assertSet.accept(multFloat2);
    assertEquals(INT_30, multFloat2.minutes());

    assertUnset.accept(oneSecond._mul(new Float()));
    assertUnset.accept(unsetDuration._mul(FLOAT_2_0));

    // Division with Integer
    final var div1 = Duration._of("PT6S")._div(INT_3);
    assertSet.accept(div1);
    assertEquals(INT_2, div1.seconds());

    final var div2 = Duration._of("PT4H")._div(INT_2);
    assertSet.accept(div2);
    assertEquals(INT_2, div2.hours());

    //Check division by zero
    assertUnset.accept(oneSecond._div(INT_0));
    assertUnset.accept(oneSecond._div(new Integer()));
    assertUnset.accept(unsetDuration._div(INT_2));

    // Division with Float
    final var divFloat1 = Duration._of("PT4S")._div(FLOAT_2_0);
    assertSet.accept(divFloat1);
    assertEquals(INT_2, divFloat1.seconds());

    //Check division by zero
    assertUnset.accept(oneSecond._div(Float._of(0.0)));
    assertUnset.accept(oneSecond._div(new Float()));
    assertUnset.accept(unsetDuration._div(FLOAT_2_0));
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
    assertEquals(INT_60, mutable1.seconds());

    mutable1 = new Duration();
    mutable1._addAss(oneSecond);
    assertUnset.accept(mutable1);

    // Subtraction assignment
    var mutable2 = Duration._of("PT2M");
    mutable2._subAss(Duration._of("PT30S"));
    assertEquals(INT_90, mutable2.seconds()); // 1 minute 30 seconds = 90 seconds

    mutable2 = new Duration();
    mutable2._subAss(oneSecond);
    assertUnset.accept(mutable2);

    // Multiplication assignment with Integer
    var mutable3 = Duration._of("PT15S");
    mutable3._mulAss(INT_2);
    assertEquals(Duration._of("PT30S"), mutable3);

    mutable3 = new Duration();
    mutable3._mulAss(INT_2);
    assertUnset.accept(mutable3);

    // Multiplication assignment with Float
    var mutable4 = Duration._of("PT20S");
    mutable4._mulAss(FLOAT_0_5);
    assertEquals(Duration._of("PT10S"), mutable4);

    mutable4 = new Duration();
    mutable4._mulAss(FLOAT_2_0);
    assertUnset.accept(mutable4);

    // Division assignment with Integer
    var mutable5 = Duration._of("PT60S");
    mutable5._divAss(INT_2);
    assertEquals(Duration._of("PT30S"), mutable5);

    mutable5 = new Duration();
    mutable5._divAss(INT_2);
    assertUnset.accept(mutable5);

    // Division assignment with Float
    var mutable6 = Duration._of("PT40S");
    mutable6._divAss(FLOAT_2_0);
    assertEquals(Duration._of("PT20S"), mutable6);

    mutable6 = new Duration();
    mutable6._divAss(FLOAT_2_0);
    assertUnset.accept(mutable6);

    //Check that div assignment with zero results in unset

    var mutable7 = Duration._of("PT40S");
    mutable7._divAss(INT_0);
    assertUnset.accept(mutable7);

    var mutable8 = Duration._of("PT40S");
    mutable8._divAss(INT_0);
    assertUnset.accept(mutable8);

  }

  @Test
  void testTimeUnitConversion() {
    // Test with unset
    assertUnset.accept(unsetDuration.seconds());
    assertUnset.accept(unsetDuration.minutes());
    assertUnset.accept(unsetDuration.hours());
    assertUnset.accept(unsetDuration.days());
    assertUnset.accept(unsetDuration.months());
    assertUnset.accept(unsetDuration.years());

    // Test simple conversions
    assertEquals(INT_1, oneSecond.seconds());
    assertEquals(INT_60, oneMinute.seconds());
    assertEquals(INT_3600, oneHour.seconds());
    assertEquals(INT_86400, oneDay.seconds());

    assertEquals(INT_1, oneMinute.minutes());
    assertEquals(INT_60, oneHour.minutes());
    assertEquals(Integer._of(1440), oneDay.minutes());

    assertEquals(INT_1, oneHour.hours());
    assertEquals(INT_24, oneDay.hours());

    assertEquals(INT_1, oneDay.days());

    // Test complex duration - just verify it's set and positive
    final var complexSeconds = complex.seconds();
    assertSet.accept(complexSeconds);
    // The exact calculation depends on the Duration implementation, but it should be a large positive number
    assertTrue.accept(complexSeconds._gt(INT_0));
  }

  @Test
  void testComparison() {
    final var oneSecondAgain = Duration._of("PT1S");

    // Equality operators
    assertTrue.accept(oneSecond._eq(oneSecondAgain));
    assertFalse.accept(oneSecond._neq(oneSecondAgain));

    assertUnset.accept(oneSecond._eq(unsetDuration));
    assertUnset.accept(unsetDuration._eq(unsetDuration));

    assertUnset.accept(oneSecond._neq(unsetDuration));
    assertUnset.accept(unsetDuration._neq(unsetDuration));

    // Comparison operators
    assertTrue.accept(oneSecond._lt(oneMinute));
    assertFalse.accept(oneMinute._lt(oneSecond));
    assertUnset.accept(oneSecond._lt(unsetDuration));
    assertUnset.accept(unsetDuration._lt(unsetDuration));

    assertTrue.accept(oneMinute._gt(oneSecond));
    assertUnset.accept(oneMinute._gt(unsetDuration));
    assertUnset.accept(unsetDuration._gt(oneSecond));

    assertTrue.accept(oneSecond._lteq(oneSecondAgain));
    assertUnset.accept(oneMinute._lteq(unsetDuration));
    assertUnset.accept(unsetDuration._lteq(oneSecond));

    assertTrue.accept(oneSecond._gteq(oneSecondAgain));
    assertUnset.accept(oneMinute._gteq(unsetDuration));
    assertUnset.accept(unsetDuration._gteq(oneSecond));

    // Comparison operator
    assertEquals(INT_0, oneSecond._cmp(oneSecondAgain));
    assertEquals(INT_MINUS_1, oneSecond._cmp(oneMinute));
    assertEquals(INT_1, oneMinute._cmp(oneSecond));

    assertUnset.accept(unsetDuration._cmp(oneSecond));
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
    assertEquals(INT_60, mutable2.seconds());

    // Pipe with unset source
    var mutable3 = Duration._of("PT1S");
    mutable3._pipe(unsetDuration);
    assertEquals(INT_1, mutable3.seconds()); // Should remain unchanged

    // Chain piping
    var mutable4 = new Duration();
    mutable4._pipe(oneSecond);
    mutable4._pipe(oneSecond);
    mutable4._pipe(oneSecond);
    assertEquals(INT_3, mutable4.seconds());
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
    assertEquals(INT_0, zeroDuration.seconds());
    assertEquals(INT_0, zeroDuration.minutes());
    assertEquals(INT_0, zeroDuration.hours());

    // Very large duration
    assertSet.accept(largeValue);
    final var largeSeconds = largeValue.seconds();
    assertSet.accept(largeSeconds);

    // Test static factory methods
    final var fromSeconds = Duration._of(3661L); // 1 hour, 1 minute, 1 second
    assertSet.accept(fromSeconds);
    assertEquals(Integer._of(3661), fromSeconds.seconds());
    assertEquals(INT_61, fromSeconds.minutes());
    assertEquals(INT_1, fromSeconds.hours());

    final var fromJavaDuration = Duration._of(java.time.Duration.ofMinutes(90));
    assertSet.accept(fromJavaDuration);
    assertEquals(INT_90, fromJavaDuration.minutes());

    // Test with Period and Duration
    final var fromBoth = Duration._of(java.time.Duration.ofHours(2), java.time.Period.ofDays(1));
    assertSet.accept(fromBoth);
    assertEquals(INT_26, fromBoth.hours()); // 24 + 2
  }

  @Test
  void testCopyOperations() {
    // Copy logic
    var mutatedValue = Duration._of("PT30S");
    assertNotNull(mutatedValue);
    mutatedValue._copy(oneMinute);
    assertTrue.accept(mutatedValue._eq(oneMinute));
    mutatedValue._copy(unsetDuration);
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
    assertEquals(INT_1, oneMinute._cmp(oneSecond));
    assertEquals(INT_MINUS_1, oneSecond._cmp(oneMinute));

    // Test arithmetic across ranges
    final var dayPlusHour = oneDay._add(oneHour);
    assertSet.accept(dayPlusHour);
    assertEquals(INT_25, dayPlusHour.hours());
  }

  @Test
  void testUtilityMethods() {
    // String operations
    assertUnset.accept(unsetDuration._string());
    assertEquals(String._of("PT1S"), oneSecond._string());
    assertEquals(String._of("PT1M"), oneMinute._string());
    assertEquals(String._of("P1Y2M3DT4H5M6S"), complex._string());

    assertEquals("PT1S", oneSecond.toString());
    assertEquals("PT1M", oneMinute.toString());
    assertEquals("", unsetDuration.toString());

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
    final var mult1 = sum._mul(INT_2);
    final var mult2 = b._mul(INT_2)._add(c._mul(INT_2));
    assertEquals(mult1.seconds(), mult2.seconds());

    // Test identity: a + 0 = a
    final var identity = a._add(zeroDuration);
    assertEquals(a.seconds(), identity.seconds());

    // Test multiplication by 1: a * 1 = a
    final var mult = a._mul(INT_1);
    assertEquals(a.seconds(), mult.seconds());
  }

  @Test
  void testAsJson() {
    // Test JSON conversion with set values
    final var oneSecondJson = oneSecond._json();
    assertNotNull(oneSecondJson);
    assertSet.accept(oneSecondJson);
    
    final var oneMinuteJson = oneMinute._json();
    assertSet.accept(oneMinuteJson);
    
    final var complexJson = complex._json();
    assertSet.accept(complexJson);
    
    // Test JSON conversion with unset value
    assertUnset.accept(unsetDuration._json());
  }

  @Test
  void testSimplePipedJSONValue() {
    // Test piping individual JSON duration and millisecond values
    final var mutatedDuration = new Duration();
    final var jsonDuration1 = new JSON(String._of("PT1H"));
    final var jsonDuration2 = new JSON(String._of("PT30M"));
    final var jsonMillisecond = new JSON(String._of("1800000ms")); // 30 minutes in ms
    final var jsonInvalid = new JSON(String._of("invalid")); // Should be ignored
    final var jsonPlainNumber = new JSON(String._of("3600000")); // No "ms" suffix, should be ignored

    // Start unset
    assertUnset.accept(mutatedDuration);

    // Pipe duration "PT1H" - should become 1 hour
    mutatedDuration._pipe(jsonDuration1);
    assertSet.accept(mutatedDuration);
    assertEquals(INT_1, mutatedDuration.hours());

    // Pipe duration "PT30M" - should add to get 1.5 hours (additive behavior)
    mutatedDuration._pipe(jsonDuration2);
    assertEquals(INT_90, mutatedDuration.minutes()); // 60 + 30 = 90 minutes

    // Pipe millisecond "1800000ms" - should add 30 more minutes to get 2 hours
    mutatedDuration._pipe(jsonMillisecond);
    assertEquals(INT_2, mutatedDuration.hours()); // 90 + 30 = 120 minutes = 2 hours

    // Test invalid string - should be ignored
    final var beforeInvalid = mutatedDuration.hours().state;
    mutatedDuration._pipe(jsonInvalid);
    assertEquals(beforeInvalid, mutatedDuration.hours().state); // Should remain unchanged

    // Test plain number without "ms" - should be ignored
    mutatedDuration._pipe(jsonPlainNumber);
    assertEquals(beforeInvalid, mutatedDuration.hours().state); // Should remain unchanged
  }

  @Test
  void testSimplePipedJSONArray() {
    final var mutatedDuration = new Duration();
    final var json1Result = new JSON().parse(String._of("[\"PT1H\", \"3600000ms\"]"));
    final var json2Result = new JSON().parse(String._of("[\"PT2H\", \"1800000ms\"]"));

    // Check that the JSON text was parsed
    assertSet.accept(json1Result);
    assertSet.accept(json2Result);

    // Pipe array with duration and milliseconds - should be 1h + 1h = 2 hours
    mutatedDuration._pipe(json1Result.ok());
    assertSet.accept(mutatedDuration);
    assertEquals(INT_2, mutatedDuration.hours()); // 1h + 3600000ms(1h) = 2h

    // Pipe second array - should add 2h + 30min = 2.5h total = 4.5h
    mutatedDuration._pipe(json2Result.ok());
    assertEquals(Integer._of(270), mutatedDuration.minutes()); // 4.5 hours = 270 minutes
  }

  @Test
  void testStructuredPipedJSONObject() {
    final var mutatedDuration = new Duration();
    final var jsonStr = """
        {
          "duration": "PT2H",
          "delay": "1800000ms"
        }""";
    final var jsonResult = new JSON().parse(String._of(jsonStr));
    
    // Pre-condition check that parsing succeeded
    assertSet.accept(jsonResult);
    mutatedDuration._pipe(jsonResult.ok());

    assertSet.accept(mutatedDuration);
    // Should add the durations: 2h + 30min = 2.5 hours = 150 minutes
    assertEquals(Integer._of(150), mutatedDuration.minutes());
  }

  @Test
  void testNestedPipedJSONObject() {
    final var mutatedDuration = new Duration();
    final var jsonStr = """
        {
          "timeouts": ["PT1H", "1800000ms"],
          "interval": "PT30M",
          "buffer": "900000ms",
          "nested": {"wait": "PT15M", "pause": "600000ms"}
        }""";
    final var jsonResult = new JSON().parse(String._of(jsonStr));
    
    // Pre-condition check that parsing succeeded
    assertSet.accept(jsonResult);
    mutatedDuration._pipe(jsonResult.ok());

    assertSet.accept(mutatedDuration);
    // Should add all durations: 1h + 30min(ms) + 30min + 15min(ms) + 15min + 10min(ms) = 2h 40min = 160min
    assertEquals(Integer._of(160), mutatedDuration.minutes());
  }
}