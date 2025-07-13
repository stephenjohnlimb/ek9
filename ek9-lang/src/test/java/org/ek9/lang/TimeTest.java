package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class TimeTest extends Common {

  final Boolean true1 = Boolean._of("true");
  final Boolean false1 = Boolean._of("false");

  final Time unset = new Time();
  final Time startOfDay = Time._of("00:00:00");
  final Time endOfDay = Time._of("23:59:59");
  final Time noon = Time._of("12:00:00");
  final Time time1 = Time._of("09:30:15");
  final Time time2 = Time._of("14:45:30");
  final Time time3 = Time._of("18:20:45");
  final Time almostEndOfDay = Time._of("23:59:58");

  @Test
  void testConstruction() {
    // Default constructor
    final var defaultConstructor = new Time();
    assertUnset.accept(defaultConstructor);

    // String constructor - valid and invalid
    final var unset1 = Time._of("not-a-time");
    assertUnset.accept(unset1);
    final var unset2 = Time._of((java.lang.String) null);
    assertUnset.accept(unset2);
    final var unset3 = new Time(new String());
    assertUnset.accept(unset3);

    final var checkTime1 = new Time(String._of("09:30:15"));
    assertSet.accept(checkTime1);
    assertEquals("09:30:15", checkTime1.toString());

    // Copy constructor
    final var againTime1 = new Time(time1);
    assertSet.accept(againTime1);
    assertEquals(time1, againTime1);

    // Second of day constructor
    final var noonSeconds = new Time(Integer._of(12 * 3600)); // 12:00:00
    assertSet.accept(noonSeconds);
    assertEquals(Integer._of(12), noonSeconds.hour());
    assertEquals(Integer._of(0), noonSeconds.minute());
    assertEquals(Integer._of(0), noonSeconds.second());

    final var unsetSeconds = new Time(new Integer());
    assertUnset.accept(unsetSeconds);

    // Hour, minute constructor
    final var timeHourMin = new Time(Integer._of(15), Integer._of(30));
    assertSet.accept(timeHourMin);
    assertEquals(Integer._of(15), timeHourMin.hour());
    assertEquals(Integer._of(30), timeHourMin.minute());
    assertEquals(Integer._of(0), timeHourMin.second());

    // Hour, minute, second constructor
    final var timeHourMinSec = new Time(Integer._of(9), Integer._of(30), Integer._of(15));
    assertSet.accept(timeHourMinSec);
    assertEquals(time1, timeHourMinSec);

    // Test _of(java.time.LocalTime) static method
    final var javaLocalTime = java.time.LocalTime.of(14, 45, 30);
    final var fromJavaLocalTime = Time._of(javaLocalTime);
    assertSet.accept(fromJavaLocalTime);
    assertEquals("14:45:30", fromJavaLocalTime.toString());
    assertEquals(Integer._of(14), fromJavaLocalTime.hour());
    assertEquals(Integer._of(45), fromJavaLocalTime.minute());
    assertEquals(Integer._of(30), fromJavaLocalTime.second());
  }

  @Test
  void testInvalidTimes() {
    // Test with unset parameters instead of invalid ones to avoid exceptions
    final var unsetHour = new Time(new Integer(), Integer._of(0));
    assertNotNull(unsetHour);
    assertUnset.accept(unsetHour);

    final var unsetMinute = new Time(Integer._of(12), new Integer());
    assertUnset.accept(unsetMinute);

    final var unsetSecond = new Time(Integer._of(12), Integer._of(30), new Integer());
    assertUnset.accept(unsetSecond);

    final var unsetSecondOfDay = new Time(new Integer());
    assertUnset.accept(unsetSecondOfDay);
  }

  @Test
  void testOperators() {
    final var time1Again = Time._of("09:30:15");

    // Equality operators
    assertEquals(time1Again, time1);
    assertEquals(true1, time1._eq(time1Again));
    assertEquals(false1, time1._neq(time1Again));

    assertUnset.accept(time1._eq(unset));
    assertUnset.accept(unset._eq(unset));

    assertUnset.accept(time1._neq(unset));
    assertUnset.accept(unset._neq(unset));

    // Comparison operators
    assertTrue.accept(time1._lt(time2));
    assertFalse.accept(time2._lt(time1));
    assertUnset.accept(time1._lt(unset));
    assertUnset.accept(unset._lt(unset));

    assertTrue.accept(time2._gt(time1));
    assertUnset.accept(time2._gt(unset));
    assertUnset.accept(unset._gt(time1));

    assertTrue.accept(time1._lteq(time1Again));
    assertUnset.accept(time2._lteq(unset));
    assertUnset.accept(unset._lteq(time1));

    assertTrue.accept(time1._gteq(time1Again));
    assertUnset.accept(time2._gteq(unset));
    assertUnset.accept(unset._gteq(time1));

    // Comparison operator
    assertEquals(Integer._of(0), time1._cmp(time1Again));
    assertEquals(Integer._of(-1), time1._cmp(time2));
    assertEquals(Integer._of(1), time2._cmp(time1));

    assertUnset.accept(time1._cmp(unset));
    assertUnset.accept(time1._cmp(new Any(){}));
  }

  @Test
  void testCopyOperations() {
    // Copy logic
    var mutatedValue = Time._of("09:30:15");
    mutatedValue._copy(time2);
    assertEquals(time2, mutatedValue);
    mutatedValue._copy(unset);
    assertUnset.accept(mutatedValue);

    // Set logic
    mutatedValue = new Time();
    mutatedValue.set(time1);
    assertEquals(time1, mutatedValue);
  }

  @Test
  void testTimeSpecificMethods() {
    // Test with unset
    assertUnset.accept(unset.hour());
    assertUnset.accept(unset.minute());
    assertUnset.accept(unset.second());

    // Test various times
    assertEquals(Integer._of(9), time1.hour());
    assertEquals(Integer._of(30), time1.minute());
    assertEquals(Integer._of(15), time1.second());

    assertEquals(Integer._of(14), time2.hour());
    assertEquals(Integer._of(45), time2.minute());
    assertEquals(Integer._of(30), time2.second());

    assertEquals(Integer._of(0), startOfDay.hour());
    assertEquals(Integer._of(0), startOfDay.minute());
    assertEquals(Integer._of(0), startOfDay.second());

    assertEquals(Integer._of(23), endOfDay.hour());
    assertEquals(Integer._of(59), endOfDay.minute());
    assertEquals(Integer._of(59), endOfDay.second());

    // Start and end of day operations
    final var testStartOfDay = new Time().startOfDay();
    assertSet.accept(testStartOfDay);
    assertEquals(startOfDay, testStartOfDay);

    final var testEndOfDay = new Time().endOfDay();
    assertSet.accept(testEndOfDay);
    // The endOfDay might have nanoseconds, so just check hours, minutes, seconds
    assertEquals(Integer._of(23), testEndOfDay.hour());
    assertEquals(Integer._of(59), testEndOfDay.minute());
    assertEquals(Integer._of(59), testEndOfDay.second());

    // Clear operation
    final var clearedTime = Time._of("12:30:45");
    clearedTime.clear();
    assertUnset.accept(clearedTime);

    // Set operations
    final var mutableTime = new Time();
    mutableTime.setStartOfDay();
    assertSet.accept(mutableTime);
    assertEquals(startOfDay, mutableTime);

    mutableTime.setEndOfDay();
    assertSet.accept(mutableTime);
    // Check components instead of exact equality due to potential nanoseconds
    assertEquals(Integer._of(23), mutableTime.hour());
    assertEquals(Integer._of(59), mutableTime.minute());
    assertEquals(Integer._of(59), mutableTime.second());
  }

  @Test
  void testPrefixSuffixOperators() {
    // Prefix operator (hour)
    assertEquals(Integer._of(9), time1._prefix());
    assertEquals(Integer._of(14), time2._prefix());
    assertEquals(Integer._of(0), startOfDay._prefix());
    assertEquals(Integer._of(23), endOfDay._prefix());
    assertUnset.accept(unset._prefix());

    // Suffix operator (second)
    assertEquals(Integer._of(15), time1._suffix());
    assertEquals(Integer._of(30), time2._suffix());
    assertEquals(Integer._of(0), startOfDay._suffix());
    assertEquals(Integer._of(59), endOfDay._suffix());
    assertUnset.accept(unset._suffix());
  }

  @Test
  void testTimeRanges() {
    // Test chronological ordering: startOfDay < time1 < noon < time2 < time3 < almostEndOfDay < endOfDay
    assertTrue.accept(startOfDay._lt(time1));
    assertTrue.accept(time1._lt(noon));
    assertTrue.accept(noon._lt(time2));
    assertTrue.accept(time2._lt(time3));
    assertTrue.accept(time3._lt(almostEndOfDay));
    assertTrue.accept(almostEndOfDay._lt(endOfDay));

    // Test comparison operators across ranges
    assertEquals(Integer._of(1), noon._cmp(startOfDay));
    assertEquals(Integer._of(-1), time1._cmp(time2));
    assertEquals(Integer._of(1), endOfDay._cmp(time1));

    // Test copy operations across ranges
    var copyTest = new Time();
    copyTest._copy(startOfDay);
    assertEquals(startOfDay, copyTest);
    copyTest._copy(endOfDay);
    assertEquals(endOfDay, copyTest);
  }

  @Test
  void testBoundaryConditions() {
    // Test edge cases around day boundaries
    final var lastSecond = Time._of("23:59:59");
    final var firstSecond = Time._of("00:00:00");

    assertTrue.accept(firstSecond._lt(lastSecond));
    assertFalse.accept(lastSecond._lt(firstSecond));

    // Test second-of-day boundaries
    final var firstSecondOfDay = new Time(Integer._of(0));
    final var lastSecondOfDay = new Time(Integer._of(86399)); // 23:59:59

    assertSet.accept(firstSecondOfDay);
    assertSet.accept(lastSecondOfDay);
    assertEquals(firstSecond, firstSecondOfDay);
    assertEquals(lastSecond, lastSecondOfDay);

    // Test one second past midnight
    final var oneSecondPast = new Time(Integer._of(1));
    assertSet.accept(oneSecondPast);
    assertEquals(Time._of("00:00:01"), oneSecondPast);
  }

  @Test
  void testUtilityMethods() {
    // String operations
    assertUnset.accept(unset._string());
    assertEquals(String._of("09:30:15"), time1._string());
    assertEquals(String._of("14:45:30"), time2._string());
    assertEquals(String._of("00:00:00"), startOfDay._string());
    assertEquals(String._of("23:59:59"), endOfDay._string());

    assertEquals("09:30:15", time1.toString());
    assertEquals("14:45:30", time2.toString());
    assertEquals("", unset.toString());

    // Hash code
    assertEquals(time1.hashCode(), time1.hashCode());
    assertNotEquals(time1.hashCode(), time2.hashCode());

    // Test _getAsJavaTemporalAccessor
    final var javaTime1 = time1._getAsJavaTemporalAccessor();
    assertNotNull(javaTime1);
    assertEquals(java.time.LocalTime.of(9, 30, 15), javaTime1);

    final var javaTime2 = endOfDay._getAsJavaTemporalAccessor();
    assertNotNull(javaTime2);
    assertEquals(java.time.LocalTime.of(23, 59, 59), javaTime2);

    // Test round-trip conversion: Time -> LocalTime -> Time
    final var originalTime = Time._of("16:45:20");
    final var asJavaTime = originalTime._getAsJavaTemporalAccessor();
    final var backToTime = Time._of(asJavaTime);
    assertEquals(originalTime, backToTime);
    assertEquals(originalTime.toString(), backToTime.toString());
  }

  @Test
  void testStringFormatting() {
    // Test proper HH:MM:SS formatting
    final var singleDigits = new Time(Integer._of(1), Integer._of(2), Integer._of(3));
    assertSet.accept(singleDigits);
    assertEquals("01:02:03", singleDigits._string().state);

    final var doubleDigits = new Time(Integer._of(12), Integer._of(34), Integer._of(56));
    assertSet.accept(doubleDigits);
    assertEquals("12:34:56", doubleDigits._string().state);

    // Test edge cases
    assertEquals("00:00:00", startOfDay._string().state);
    assertEquals("23:59:59", endOfDay._string().state);
  }
}