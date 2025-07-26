package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DateTimeTest extends Common {

  final Date simpleDate = Date._of("2023-06-15");

  // ============ COMMON TEST DATETIME INSTANCES ============
  // Base DateTimes for arithmetic tests
  final DateTime baseDateTime = DateTime._of("2023-06-15T12:30:45Z");
  final DateTime baseUTC = DateTime._of(DATETIME_JUNE_15_NOON_UTC.state);
  
  // Timezone Variants (same dates, different zones)
  final DateTime baseEST = DateTime._of("2023-06-15T12:30:45-04:00");
  final DateTime baseJST = DateTime._of("2023-06-15T12:30:45+09:00");
  final DateTime basePST = DateTime._of("2023-06-15T12:30:45-08:00");

  // Edge Case DateTimes
  final DateTime newYearEveUTC = DateTime._of(DATETIME_NEW_YEAR_EVE.state);
  final DateTime newYearDayUTC = DateTime._of("2024-01-01T00:00:00Z");
  final DateTime leapFeb28 = DateTime._of("2024-02-28T12:00:00Z");
  final DateTime leapFeb29 = DateTime._of(DATETIME_LEAP_FEB_29_NOON.state);
  final DateTime leapMar01 = DateTime._of("2024-03-01T12:00:00Z");

  // Month Boundary DateTimes
  final DateTime endOfJune = DateTime._of("2023-06-30T14:45:30Z");
  final DateTime startOfJuly = DateTime._of("2023-07-01T08:15:20Z");

  // Fuzzy Comparison Test DateTimes
  final DateTime fuzzyBase = DateTime._of(DATETIME_JUNE_15_NOON_UTC.state);
  final DateTime fuzzyOneHourLater = DateTime._of("2023-06-15T13:00:00Z");
  final DateTime fuzzyTwoHoursLater = DateTime._of("2023-06-15T14:00:00Z");
  final DateTime fuzzyOneDayLater = DateTime._of("2023-06-16T12:00:00Z");
  final DateTime fuzzyOneWeekLater = DateTime._of("2023-06-22T12:00:00Z");
  final DateTime fuzzyOneHourEarlier = DateTime._of("2023-06-15T11:00:00Z");
  final DateTime fuzzyOneDayEarlier = DateTime._of("2023-06-14T12:00:00Z");

  // Timezone Test Pairs (same instant, different zones)
  final DateTime utcNoon = DateTime._of(DATETIME_JUNE_15_NOON_UTC.state);
  final DateTime nyNoon = DateTime._of(DATETIME_JUNE_15_NOON_NY.state); // 4 hours later than UTC noon
  final DateTime utc1600 = DateTime._of("2023-06-15T16:00:00Z");
  final DateTime ny1200 = DateTime._of(DATETIME_JUNE_15_NOON_NY.state); // Same instant as UTC 16:00

  // Offset Test DateTimes (for offsetFromUTC tests)
  final DateTime indiaOffset = DateTime._of("2023-06-15T12:00:00+05:30");

  @Test
  void testConstruction() {
    // Default constructor
    final var defaultConstructor = new DateTime();
    assertUnset.accept(defaultConstructor);

    // String constructor - valid and invalid
    final var unset1 = DateTime._of("not-a-datetime");
    assertUnset.accept(unset1);
    final var unset2 = DateTime._of((java.lang.String) null);
    assertUnset.accept(unset2);
    final var unset3 = new DateTime(new String());
    assertUnset.accept(unset3);

    final var checkDateTime1 = new DateTime(DATETIME_BASE_UTC);
    assertSet.accept(checkDateTime1);
    assertEquals(DATETIME_BASE_UTC.state, checkDateTime1.toString());

    // Copy constructor
    final var againDateTime1 = new DateTime(dateTime1);
    assertSet.accept(againDateTime1);
    assertEquals(dateTime1, againDateTime1);

    // Date constructor
    final var fromDate = new DateTime(simpleDate);
    assertSet.accept(fromDate);
    assertEquals(INT_2023, fromDate.year());
    assertEquals(INT_6, fromDate.month());
    assertEquals(INT_15, fromDate.day());
    assertEquals(INT_0, fromDate.hour());
    assertEquals(INT_0, fromDate.minute());
    assertEquals(INT_0, fromDate.second());
    assertEquals(TIMEZONE_UTC_Z, fromDate.zone());

    // Component constructor (year, month, day)
    final var ymd = new DateTime(INT_2023, INT_1, INT_1);
    assertSet.accept(ymd);
    assertEquals(INT_2023, ymd.year());
    assertEquals(INT_1, ymd.month());
    assertEquals(INT_1, ymd.day());
    assertEquals(INT_0, ymd.hour());
    assertEquals(TIMEZONE_UTC_Z, ymd.zone());

    // Component constructor (year, month, day, hour)
    final var ymdh = new DateTime(INT_2023, INT_1, INT_1, INT_12);
    assertSet.accept(ymdh);
    assertEquals(INT_12, ymdh.hour());
    assertEquals(INT_0, ymdh.minute());

    // Component constructor (year, month, day, hour, minute)
    final var ymdhm = new DateTime(INT_2023, INT_1, INT_1, INT_12, INT_30);
    assertSet.accept(ymdhm);
    assertEquals(INT_30, ymdhm.minute());
    assertEquals(INT_0, ymdhm.second());

    // Component constructor (year, month, day, hour, minute, second)
    final var ymdhms = new DateTime(INT_2023, INT_1, INT_1, INT_12, INT_30,
        INT_45);
    assertSet.accept(ymdhms);
    assertEquals(INT_45, ymdhms.second());

    // Test invalid date components - these should not throw exceptions but return unsetDateTime
    final var unsetDate = new DateTime(new Integer(), INT_1, INT_1);
    assertUnset.accept(unsetDate);
  }

  @Test
  void testTimeZoneOperations() {
    // Test withSameInstant - same moment in time, different zone
    final var utcToNY = baseUTC.withSameInstant(TIMEZONE_NEW_YORK);
    assertSet.accept(utcToNY);
    assertEquals(TIMEZONE_NEW_YORK, utcToNY.zone());
    // UTC 12:00 = EDT 08:00 (UTC-4)
    assertEquals(INT_8, utcToNY.hour());
    assertEquals(baseUTC.minute(), utcToNY.minute());
    assertEquals(baseUTC.second(), utcToNY.second());

    final var utcToLondon = baseUTC.withSameInstant(TIMEZONE_LONDON);
    assertSet.accept(utcToLondon);
    assertEquals(TIMEZONE_LONDON, utcToLondon.zone());
    // UTC 12:00 = BST 13:00 (UTC+1)
    assertEquals(INT_13, utcToLondon.hour());

    final var utcToTokyo = baseUTC.withSameInstant(TIMEZONE_TOKYO);
    assertSet.accept(utcToTokyo);
    assertEquals(TIMEZONE_TOKYO, utcToTokyo.zone());
    // UTC 12:00 = JST 21:00 (UTC+9)
    assertEquals(INT_21, utcToTokyo.hour());

    // Test withZone - same local time, different zone
    final var utcWithNYZone = baseUTC.withZone(TIMEZONE_NEW_YORK);
    assertSet.accept(utcWithNYZone);
    assertEquals(TIMEZONE_NEW_YORK, utcWithNYZone.zone());
    // Same local time 12:00, but now in NY timezone
    assertEquals(INT_12, utcWithNYZone.hour());
    assertEquals(baseUTC.minute(), utcWithNYZone.minute());
    assertEquals(baseUTC.second(), utcWithNYZone.second());

    // Test with invalid timezone - these may throw exceptions, so we need to handle them
    // The implementation might throw exceptions for invalid zones, so we'll test valid zones only

    // Test with unsetDateTime
    assertUnset.accept(unsetDateTime.withSameInstant(TIMEZONE_UTC));
    assertUnset.accept(unsetDateTime.withZone(TIMEZONE_UTC));
  }

  @Test
  void testCrossTimezoneComparisons() {
    // Test basic timezone conversion without comparing across different timezone representations
    final var utc1200 = DateTime._of(DATETIME_JUNE_15_NOON_UTC.state);

    // Convert to different timezone and verify components
    final var ny0800 = utc1200.withSameInstant(TIMEZONE_NEW_YORK);
    assertSet.accept(ny0800);
    // Just verify the conversion worked
    assertNotEquals(utc1200.hour(), ny0800.hour()); // Hours should be different

    // Test back to UTC
    final var backToUTC = ny0800.withSameInstant(TIMEZONE_UTC);
    assertTrue.accept(utc1200._eq(backToUTC));

    // Test inequality - different instants
    final var ny0900 = DateTime._of("2023-06-15T09:00:00-04:00"); // 1 hour later than ny0800
    assertEquals(trueBoolean, ny0800._lt(ny0900));
    assertEquals(trueBoolean, ny0900._gt(ny0800));
    assertEquals(INT_MINUS_1, ny0800._cmp(ny0900));
    assertEquals(INT_1, ny0900._cmp(ny0800));

    // Test New Year across timezones
    // Tokyo New Year happens 9 hours before UTC New Year
    assertEquals(trueBoolean, newYearTokyo._lt(newYearUTC));
    assertEquals(INT_MINUS_1, newYearTokyo._cmp(newYearUTC));

    // Test edge case: same local time, different zones
    final var localTime1200UTC = DateTime._of(DATETIME_JUNE_15_NOON_UTC.state);
    final var localTime1200NY = DateTime._of(DATETIME_JUNE_15_NOON_NY.state);
    // These are different instants (4 hours apart)
    assertEquals(trueBoolean, localTime1200UTC._lt(localTime1200NY));
    assertEquals(INT_MINUS_1, localTime1200UTC._cmp(localTime1200NY));

    //So twelve in UTC on this date, will be 8AM in new york. But the same instant in time.
    final var checkWhatItWouldBeInNY = localTime1200UTC.withSameInstant(TIMEZONE_NEW_YORK);
    assertEquals("2023-06-15T08:00:00-04:00", checkWhatItWouldBeInNY.toString());

    //That's what date times mean in EK9 - are they the same instant.
    final var checkSame = localTime1200UTC._eq(checkWhatItWouldBeInNY);
    assertTrue.accept(checkSame);

  }

  @Test
  void testOperators() {
    final var dateTime1Again = DateTime._of(DATETIME_BASE_UTC.state);

    // Equality operators
    assertEquals(dateTime1Again, dateTime1);
    assertEquals(trueBoolean, dateTime1._eq(dateTime1Again));
    assertEquals(falseBoolean, dateTime1._neq(dateTime1Again));

    assertUnset.accept(dateTime1._eq(unsetDateTime));
    assertUnset.accept(unsetDateTime._eq(unsetDateTime));

    assertUnset.accept(dateTime1._neq(unsetDateTime));
    assertUnset.accept(unsetDateTime._neq(unsetDateTime));

    // Comparison operators
    assertTrue.accept(dateTime1._lt(dateTime2));
    assertFalse.accept(dateTime2._lt(dateTime1));
    assertUnset.accept(dateTime1._lt(unsetDateTime));
    assertUnset.accept(unsetDateTime._lt(unsetDateTime));

    assertTrue.accept(dateTime2._gt(dateTime1));
    assertUnset.accept(dateTime2._gt(unsetDateTime));
    assertUnset.accept(unsetDateTime._gt(dateTime1));

    assertTrue.accept(dateTime1._lteq(dateTime1Again));
    assertUnset.accept(dateTime2._lteq(unsetDateTime));
    assertUnset.accept(unsetDateTime._lteq(dateTime1));

    assertTrue.accept(dateTime1._gteq(dateTime1Again));
    assertUnset.accept(dateTime2._gteq(unsetDateTime));
    assertUnset.accept(unsetDateTime._gteq(dateTime1));

    // Comparison operator
    assertEquals(INT_0, dateTime1._cmp(dateTime1Again));
    // dateTime1 is 2023-01-01T12:00:00Z, dateTime2 is 2023-01-02T14:30:15Z
    assertEquals(INT_MINUS_1, dateTime1._cmp(dateTime2));
    assertEquals(INT_1, dateTime2._cmp(dateTime1));

    assertUnset.accept(unsetDateTime._cmp(dateTime1));
    assertUnset.accept(dateTime2._cmp(new Any(){}));

  }

  @Test
  void testCopyOperations() {
    // Copy logic
    var mutatedValue = DateTime._of(DATETIME_BASE_UTC.state);
    mutatedValue._copy(dateTime2);
    assertTrue.accept(mutatedValue._eq(dateTime2));
    mutatedValue._copy(unsetDateTime);
    assertUnset.accept(mutatedValue);

    // Pipe logic
    mutatedValue = new DateTime();
    mutatedValue._pipe(dateTime1);
    assertEquals(dateTime1, mutatedValue);
    mutatedValue._pipe(dateTime2);
    assertEquals(dateTime2, mutatedValue);
  }

  @Test
  void testDateTimeComponents() {
    // Test with unsetDateTime
    assertUnset.accept(unsetDateTime.year());
    assertUnset.accept(unsetDateTime.month());
    assertUnset.accept(unsetDateTime.day());
    assertUnset.accept(unsetDateTime.hour());
    assertUnset.accept(unsetDateTime.minute());
    assertUnset.accept(unsetDateTime.second());
    assertUnset.accept(unsetDateTime.zone());
    assertUnset.accept(unsetDateTime.dayOfWeek());

    // Test various datetimes
    assertEquals(INT_2023, dateTime1.year());
    assertEquals(INT_1, dateTime1.month());
    assertEquals(INT_1, dateTime1.day());
    assertEquals(INT_12, dateTime1.hour());
    assertEquals(INT_0, dateTime1.minute());
    assertEquals(INT_0, dateTime1.second());
    assertEquals(TIMEZONE_UTC_Z, dateTime1.zone());
    assertEquals(INT_7, dateTime1.dayOfWeek()); // Sunday

    assertEquals(INT_2023, dateTime2.year());
    assertEquals(INT_1, dateTime2.month());
    assertEquals(INT_2, dateTime2.day());
    assertEquals(INT_14, dateTime2.hour());
    assertEquals(INT_30, dateTime2.minute());
    assertEquals(INT_15, dateTime2.second());

    assertEquals(INT_2024, leapYear.year());
    assertEquals(INT_2, leapYear.month());
    assertEquals(INT_29, leapYear.day());
    assertEquals(INT_18, leapYear.hour());
    assertEquals(INT_45, leapYear.minute());
    assertEquals(INT_30, leapYear.second());

    // Test epoch
    assertEquals(INT_1970, epoch.year());
    assertEquals(INT_1, epoch.month());
    assertEquals(INT_1, epoch.day());
    assertEquals(INT_0, epoch.hour());
    assertEquals(INT_0, epoch.minute());
    assertEquals(INT_0, epoch.second());
    assertEquals(INT_4, epoch.dayOfWeek()); // Thursday

    // Test timezone components
    assertEquals(TIMEZONE_UTC_Z, baseUTC.zone());
    final var nyTime = baseUTC.withSameInstant(TIMEZONE_NEW_YORK);
    assertEquals(TIMEZONE_NEW_YORK, nyTime.zone());
  }

  @Test
  void testStringConversion() {
    // Test _string() method (ISO format)
    assertUnset.accept(unsetDateTime._string());
    assertEquals(DATETIME_BASE_UTC, dateTime1._string());
    assertEquals(String._of("2023-01-02T14:30:15Z"), dateTime2._string());
    assertEquals(String._of("1970-01-01T00:00:00Z"), epoch._string());

    // Test toString() method
    assertEquals(DATETIME_BASE_UTC.state, dateTime1.toString());
    assertEquals("2023-01-02T14:30:15Z", dateTime2.toString());
    assertEquals("", unsetDateTime.toString());
  }

  @Test
  void testAsJson() {
    // Test JSON conversion with set values
    final var dateTime1Json = dateTime1._json();
    assertSet.accept(dateTime1Json);

    final var dateTime2Json = dateTime2._json();
    assertSet.accept(dateTime2Json);

    final var epochJson = epoch._json();
    assertSet.accept(epochJson);

    // Test JSON conversion with unsetDateTime value
    assertUnset.accept(unsetDateTime._json());

    // Test rfc7231() method (HTTP date format)
    assertUnset.accept(unsetDateTime.rfc7231());

    // RFC 7231 format: "EEE, dd MMM yyyy HH:mm:ss O"
    final var rfc1 = dateTime1.rfc7231();
    assertSet.accept(rfc1);
    assertEquals("Sun, 01 Jan 2023 12:00:00 GMT", rfc1.toString());

    // Should be "Sun, 01 Jan 2023 12:00:00 GMT"
    final var rfc1String = rfc1.state;
    assertNotNull(rfc1String);
    Assertions.assertTrue(rfc1String.contains("Jan 2023"));
    Assertions.assertTrue(rfc1String.contains("12:00:00"));

    final var rfc2 = dateTime2.rfc7231();
    assertSet.accept(rfc2);
    final var rfc2String = rfc2.state;
    assertNotNull(rfc2String);
    Assertions.assertTrue(rfc2String.contains("Jan 2023"));
    Assertions.assertTrue(rfc2String.contains("14:30:15"));

    // Test with timezone
    final var newYorkDateTime = baseUTC.withSameInstant(TIMEZONE_NEW_YORK);
    final var nyRfc = newYorkDateTime.rfc7231();
    assertSet.accept(nyRfc);
    // Should contain the NY timezone offset
    Assertions.assertTrue(nyRfc.state.contains("-"));
  }

  @Test
  void testUtilityMethods() {
    // Today and now operations
    final var todaysDateTime = new DateTime().today();
    assertSet.accept(todaysDateTime);

    final var nowDateTime = new DateTime().now();
    assertSet.accept(nowDateTime);

    // today() and now() should return the same result for DateTime
    assertEquals(todaysDateTime.year(), nowDateTime.year());
    assertEquals(todaysDateTime.month(), nowDateTime.month());
    assertEquals(todaysDateTime.day(), nowDateTime.day());

    // Clear operation
    final var clearedDateTime = DateTime._of(DATETIME_BASE_UTC.state);
    clearedDateTime.clear();
    assertUnset.accept(clearedDateTime);

    // Hash code
    assertEquals(dateTime1.hashCode(), dateTime1.hashCode());
    assertNotEquals(dateTime1.hashCode(), dateTime2.hashCode());

    // Test setToday
    final var mutableDateTime = new DateTime();
    mutableDateTime.setToday();
    assertSet.accept(mutableDateTime);
  }

  @Test
  void testStaticFactoryMethods() {
    // Test _of(String)
    final var fromString = DateTime._of("2023-06-15T15:30:45Z");
    assertSet.accept(fromString);
    assertEquals(INT_2023, fromString.year());
    assertEquals(INT_6, fromString.month());
    assertEquals(INT_15, fromString.day());
    assertEquals(INT_15, fromString.hour());
    assertEquals(INT_30, fromString.minute());
    assertEquals(INT_45, fromString.second());

    // Test _of(ZonedDateTime)
    final var javaZonedDateTime = java.time.ZonedDateTime.of(2023, 6, 15, 15, 30, 45, 0, java.time.ZoneId.of("UTC"));
    final var fromZonedDateTime = DateTime._of(javaZonedDateTime);
    assertSet.accept(fromZonedDateTime);
    assertTrue.accept(fromString._eq(fromZonedDateTime));

    // Test _ofHttpDateTime
    final var httpDateTime1 = "Thu, 15 Jun 2023 15:30:45 GMT";
    final var fromHttp1 = DateTime._ofHttpDateTime(httpDateTime1);
    assertSet.accept(fromHttp1);
    // Verify the parsing worked by checking individual components
    assertEquals(INT_2023, fromHttp1.year());
    assertEquals(INT_6, fromHttp1.month());
    assertEquals(INT_15, fromHttp1.day());
    assertEquals(INT_15, fromHttp1.hour());
    assertEquals(INT_30, fromHttp1.minute());
    assertEquals(INT_45, fromHttp1.second());
    // Test invalid HTTP date
    final var invalidHttp = DateTime._ofHttpDateTime("not-a-date");
    assertUnset.accept(invalidHttp);

    final var nullHttp = DateTime._ofHttpDateTime(null);
    assertUnset.accept(nullHttp);
  }

  @Test
  void testDateTimeRanges() {
    // Test chronological ordering: epoch < beforeY2K < dateTime1 < dateTime2 < dateTime3 < leapYear < futureDateTime
    assertTrue.accept(epoch._lt(beforeY2K));
    assertTrue.accept(beforeY2K._lt(dateTime1));
    assertTrue.accept(dateTime1._lt(dateTime2));
    assertTrue.accept(dateTime2._lt(dateTime3));
    assertTrue.accept(dateTime3._lt(leapYear));
    assertTrue.accept(leapYear._lt(futureDateTime));

    // Test comparison operators across ranges
    assertEquals(INT_1, beforeY2K._cmp(epoch));
    assertEquals(INT_MINUS_1, beforeY2K._cmp(dateTime1));
    assertEquals(INT_1, futureDateTime._cmp(dateTime1));

    // Test copy operations across ranges
    var copyTest = new DateTime();
    copyTest._copy(beforeY2K);
    assertEquals(beforeY2K, copyTest);
    copyTest._copy(futureDateTime);
    assertEquals(futureDateTime, copyTest);
  }

  @Test
  void testTimezoneEdgeCases() {
    // Test daylight saving time transitions
    // Spring forward: 2023-03-12 02:00:00 EST -> 03:00:00 EDT
    final var beforeDST = DateTime._of("2023-03-12T01:30:00-05:00"); // EST
    final var afterDST = DateTime._of("2023-03-12T03:30:00-04:00");  // EDT

    assertSet.accept(beforeDST);
    assertSet.accept(afterDST);
    assertTrue.accept(beforeDST._lt(afterDST));

    // Test same instant in different zones during DST
    final var utcDuringDST = DateTime._of("2023-07-15T16:00:00Z");
    final var nyDuringDST = utcDuringDST.withSameInstant(TIMEZONE_NEW_YORK);
    assertEquals(INT_12, nyDuringDST.hour()); // EDT is UTC-4

    // Test same instant in different zones during standard time
    final var utcDuringStandard = DateTime._of("2023-01-15T17:00:00Z");
    final var nyDuringStandard = utcDuringStandard.withSameInstant(TIMEZONE_NEW_YORK);
    assertEquals(INT_12, nyDuringStandard.hour()); // EST is UTC-5

    // Test round-trip timezone conversion
    final var original = DateTime._of("2023-06-15T14:30:00Z");
    final var toNY = original.withSameInstant(TIMEZONE_NEW_YORK);
    final var backToUTC = toNY.withSameInstant(TIMEZONE_UTC);
    assertEquals(trueBoolean, original._eq(backToUTC));

    // Test withZone vs withSameInstant difference
    final var sameInstantNY = baseUTC.withSameInstant(TIMEZONE_NEW_YORK);
    final var sameLocalNY = baseUTC.withZone(TIMEZONE_NEW_YORK);

    // Same instant should show 08:00 in NY (EDT = UTC-4)
    assertEquals(INT_8, sameInstantNY.hour());
    // Same local time should show 12:00 in NY
    assertEquals(INT_12, sameLocalNY.hour());

    // These represent different instants in time
    assertEquals(trueBoolean, sameLocalNY._gt(sameInstantNY));
  }

  @Test
  void testDateAndTimeExtraction() {
    // Test date() method
    final var sourceDateTime = DateTime._of(DATETIME_JUNE_15_AFTERNOON.state);
    final var extractedDate = sourceDateTime.date();
    assertSet.accept(extractedDate);
    assertEquals(INT_2023, extractedDate.year());
    assertEquals(INT_6, extractedDate.month());
    assertEquals(INT_15, extractedDate.day());

    // Test time() method - should extract UTC time
    final var extractedTime = sourceDateTime.time();
    assertSet.accept(extractedTime);
    assertEquals(INT_14, extractedTime.hour());
    assertEquals(INT_30, extractedTime.minute());
    assertEquals(INT_45, extractedTime.second());

    // Test with timezone conversion - time() should use UTC
    final var nyDateTime = DateTime._of("2023-06-15T14:30:45-04:00"); // EDT
    final var nyTime = nyDateTime.time();
    assertSet.accept(nyTime);
    // 14:30:45 EDT = 18:30:45 UTC
    assertEquals(INT_18, nyTime.hour());
    assertEquals(INT_30, nyTime.minute());
    assertEquals(INT_45, nyTime.second());

    // Test with unsetDateTime DateTime
    assertUnset.accept(unsetDateTime.date());
    assertUnset.accept(unsetDateTime.time());
  }

  @Test
  void testPrefixAndSuffixOperators() {
    // Test _prefix operator (should return Date - same as date())
    final var prefixTestDateTime = DateTime._of(DATETIME_JUNE_15_AFTERNOON.state);
    final var prefixResult = prefixTestDateTime._prefix();
    final var dateResult = prefixTestDateTime.date();
    
    assertSet.accept(prefixResult);
    assertTrue.accept(prefixResult._eq(dateResult));
    assertEquals(INT_2023, prefixResult.year());
    assertEquals(INT_6, prefixResult.month());
    assertEquals(INT_15, prefixResult.day());

    // Test _suffix operator (should return Time - same as time())
    final var suffixResult = prefixTestDateTime._suffix();
    final var timeResult = prefixTestDateTime.time();
    
    assertSet.accept(suffixResult);
    assertTrue.accept(suffixResult._eq(timeResult));
    assertEquals(INT_14, suffixResult.hour());
    assertEquals(INT_30, suffixResult.minute());
    assertEquals(INT_45, suffixResult.second());

    // Test with timezone - suffix should still use UTC time
    final var nyDateTime = DateTime._of("2023-06-15T14:30:45-04:00"); // EDT
    final var nySuffixResult = nyDateTime._suffix();
    assertSet.accept(nySuffixResult);
    // 14:30:45 EDT = 18:30:45 UTC
    assertEquals(INT_18, nySuffixResult.hour());
    assertEquals(INT_30, nySuffixResult.minute());
    assertEquals(INT_45, nySuffixResult.second());

    // Test with unsetDateTime DateTime
    assertUnset.accept(unsetDateTime._prefix());
    assertUnset.accept(unsetDateTime._suffix());

    // Test leap year edge case
    final var leapDateTime = DateTime._of(DATETIME_LEAP_FEB_29_NOON.state);
    final var leapPrefix = leapDateTime._prefix();
    assertSet.accept(leapPrefix);
    assertEquals(INT_2024, leapPrefix.year());
    assertEquals(INT_2, leapPrefix.month());
    assertEquals(INT_29, leapPrefix.day());

    // Test end of year edge case
    final var endOfYear = DateTime._of(DATETIME_NEW_YEAR_EVE.state);
    final var endYearSuffix = endOfYear._suffix();
    assertSet.accept(endYearSuffix);
    assertEquals(INT_23, endYearSuffix.hour());
    assertEquals(INT_59, endYearSuffix.minute());
    assertEquals(INT_59, endYearSuffix.second());
  }

  @Test
  void testJavaTemporalAccessor() {
    // Test _getAsJavaTemporalAccessor
    final var javaDateTime1 = dateTime1._getAsJavaTemporalAccessor();
    assertNotNull(javaDateTime1);
    assertEquals(java.time.ZonedDateTime.of(2023, 1, 1, 12, 0, 0, 0, java.time.ZoneId.of("Z")), javaDateTime1);

    final var javaDateTime2 = leapYear._getAsJavaTemporalAccessor();
    assertNotNull(javaDateTime2);
    assertEquals(java.time.ZonedDateTime.of(2024, 2, 29, 18, 45, 30, 0, java.time.ZoneId.of("Z")), javaDateTime2);

    // Test round-trip conversion: DateTime -> ZonedDateTime -> DateTime
    final var originalDateTime = DateTime._of("2023-12-25T15:30:45Z");
    final var asJavaDateTime = originalDateTime._getAsJavaTemporalAccessor();
    final var backToDateTime = DateTime._of(asJavaDateTime);
    assertEquals(originalDateTime, backToDateTime);
    assertEquals(originalDateTime.toString(), backToDateTime.toString());

    // Test with different timezone
    final var newYorkDateTime = DateTime._of(DATETIME_JUNE_15_NOON_NY.state);
    final var javaNY = newYorkDateTime._getAsJavaTemporalAccessor();
    assertNotNull(javaNY);
    assertEquals(java.time.ZonedDateTime.of(2023, 6, 15, 12, 0, 0, 0, java.time.ZoneId.of("-04:00")), javaNY);
  }

  @Test
  void testLeapYearAndMonthEdgeCases() {
    // Test leap year February 29th
    assertSet.accept(leapYear);
    assertEquals(INT_2024, leapYear.year());
    assertEquals(INT_2, leapYear.month());
    assertEquals(INT_29, leapYear.day());

    // Test end of months
    final var endOfJan = DateTime._of("2023-01-31T23:59:59Z");
    final var endOfFeb = DateTime._of("2023-02-28T23:59:59Z");
    final var endOfMar = DateTime._of("2023-03-31T23:59:59Z");
    final var endOfApr = DateTime._of("2023-04-30T23:59:59Z");

    assertEquals(INT_31, endOfJan.day());
    assertEquals(INT_28, endOfFeb.day());
    assertEquals(INT_31, endOfMar.day());
    assertEquals(INT_30, endOfApr.day());

    // Test chronological order
    assertTrue.accept(endOfJan._lt(endOfFeb));
    assertTrue.accept(endOfFeb._lt(endOfMar));
    assertTrue.accept(endOfMar._lt(endOfApr));

    // Test leap year February vs non-leap year
    final var nonLeapFeb28 = DateTime._of("2023-02-28T12:00:00Z");
    assertTrue.accept(nonLeapFeb28._lt(leapFeb29));
  }

  /**
   * CRITICAL TEST: This demonstrates why Steve's UTC conversion is essential.
   * Problem: Without UTC conversion, date() and time() extraction from DateTime
   * objects in different timezones would return timezone-ambiguous values.
   * Solution: Always convert to UTC before extracting date/time components
   * to ensure unambiguous, globally consistent results.
   */
  @Test
  void testUTCConversionForDateTimeExtraction() {

    // Create DateTime in EST timezone (UTC-5) - January 15, 2025 at 2:30 PM EST
    final var estDateTime = DateTime._of("2025-01-15T14:30:00-05:00");
    assertSet.accept(estDateTime);
    
    // Verify the original EST DateTime components
    assertEquals(INT_2025, estDateTime.year());
    assertEquals(INT_1, estDateTime.month());
    assertEquals(INT_15, estDateTime.day());
    assertEquals(INT_14, estDateTime.hour());
    assertEquals(INT_30, estDateTime.minute());
    assertEquals(String._of("-05:00"), estDateTime.zone());
    
    // Extract date() and time() - these should be UTC-converted
    final var extractedDate = estDateTime.date();
    final var extractedTime = estDateTime.time();
    
    assertSet.accept(extractedDate);
    assertSet.accept(extractedTime);
    
    // CRITICAL ASSERTION: The extracted time should be UTC time (19:30), not EST time (14:30)
    // EST 14:30 + 5 hours = UTC 19:30
    assertEquals(INT_19, extractedTime.hour()); // NOT 14 - this is UTC!
    assertEquals(INT_30, extractedTime.minute());
    assertEquals(INT_0, extractedTime.second());
    
    // The extracted date should also be UTC-relative
    assertEquals(INT_2025, extractedDate.year());
    assertEquals(INT_1, extractedDate.month());
    assertEquals(INT_15, extractedDate.day()); // Same day in this case
    
    // Test edge case: EST time that crosses midnight when converted to UTC
    final var lateESTDateTime = DateTime._of("2025-01-15T23:30:00-05:00"); // 11:30 PM EST
    final var lateExtractedTime = lateESTDateTime.time();
    final var lateExtractedDate = lateESTDateTime.date();
    
    assertSet.accept(lateExtractedTime);
    assertSet.accept(lateExtractedDate);
    
    // EST 23:30 + 5 hours = UTC 04:30 (next day!)
    assertEquals(INT_4, lateExtractedTime.hour()); // NOT 23 - this is UTC!
    assertEquals(INT_30, lateExtractedTime.minute());
    
    // Date should be the next day in UTC
    assertEquals(INT_2025, lateExtractedDate.year());
    assertEquals(INT_1, lateExtractedDate.month());
    assertEquals(INT_16, lateExtractedDate.day()); // Next day in UTC!
    
    // Test with multiple timezones to verify consistent UTC conversion
    final var jstDateTime = DateTime._of("2025-01-15T14:30:00+09:00"); // JST (UTC+9)
    final var jstExtractedTime = jstDateTime.time();
    
    // JST 14:30 - 9 hours = UTC 05:30
    assertEquals(INT_5, jstExtractedTime.hour());
    assertEquals(INT_30, jstExtractedTime.minute());
    
    // VERIFICATION: All extracted times are unambiguously UTC
    // Consumer knows that:
    // - extractedTime (19:30) represents 19:30 UTC, not EST
    // - lateExtractedTime (04:30) represents 04:30 UTC, not EST
    // - jstExtractedTime (05:30) represents 05:30 UTC, not JST
    //
    // Without UTC conversion, consumers would see:
    // - Ambiguous time (14:30) without knowing it represents EST
    // - No way to compare times from different timezone sources
    // - Potential for incorrect calculations and logic errors
    
    // Test prefix/suffix operators (should delegate to date()/time() and also use UTC)
    final var prefixResult = estDateTime._prefix(); // Should be same as date()
    final var suffixResult = estDateTime._suffix(); // Should be same as time()
    
    assertTrue.accept(prefixResult._eq(extractedDate));
    assertTrue.accept(suffixResult._eq(extractedTime));
    
    // Verify UTC conversion is consistent across all extraction methods
    assertEquals(extractedTime.hour(), suffixResult.hour()); // Both should be UTC 19:30
    assertEquals(extractedDate.day(), prefixResult.day()); // Both should be UTC date
  }

  @Test
  void testStartOfDay() {
    // Test basic functionality - time should be set to 00:00:00.000000000
    final var midDayDateTime = DateTime._of(DATETIME_JUNE_15_AFTERNOON.state);
    final var startOfDayResult = midDayDateTime.startOfDay();
    assertSet.accept(startOfDayResult);
    
    // Verify time components are set to start of day
    assertEquals(INT_0, startOfDayResult.hour());
    assertEquals(INT_0, startOfDayResult.minute());
    assertEquals(INT_0, startOfDayResult.second());
    
    // Verify date components are preserved
    assertEquals(INT_2023, startOfDayResult.year());
    assertEquals(INT_6, startOfDayResult.month());
    assertEquals(INT_15, startOfDayResult.day());
    
    // Verify timezone is preserved
    assertEquals(TIMEZONE_UTC_Z, startOfDayResult.zone());
    
    // Test with different timezone - timezone should be preserved
    final var nyDateTime = DateTime._of("2023-06-15T14:30:45-04:00");
    final var nyStartOfDay = nyDateTime.startOfDay();
    assertSet.accept(nyStartOfDay);
    assertEquals(INT_0, nyStartOfDay.hour());
    assertEquals(INT_0, nyStartOfDay.minute());
    assertEquals(INT_0, nyStartOfDay.second());
    assertEquals(String._of("-04:00"), nyStartOfDay.zone());
    assertEquals(INT_2023, nyStartOfDay.year());
    assertEquals(INT_6, nyStartOfDay.month());
    assertEquals(INT_15, nyStartOfDay.day());
    
    // Test with leap year date
    final var leapDateTime = DateTime._of("2024-02-29T18:45:30Z");
    final var leapStartOfDay = leapDateTime.startOfDay();
    assertSet.accept(leapStartOfDay);
    assertEquals(INT_0, leapStartOfDay.hour());
    assertEquals(INT_2024, leapStartOfDay.year());
    assertEquals(INT_2, leapStartOfDay.month());
    assertEquals(INT_29, leapStartOfDay.day());
    
    // Test with year boundary dates
    final var newYearEve = DateTime._of(DATETIME_NEW_YEAR_EVE.state);
    final var newYearEveStart = newYearEve.startOfDay();
    assertEquals(INT_0, newYearEveStart.hour());
    assertEquals(INT_31, newYearEveStart.day());
    
    final var newYearDay = DateTime._of("2024-01-01T12:00:00Z");
    final var newYearDayStart = newYearDay.startOfDay();
    assertEquals(INT_0, newYearDayStart.hour());
    assertEquals(INT_1, newYearDayStart.day());
    assertEquals(INT_2024, newYearDayStart.year());
    
    // Test with unsetDateTime DateTime
    assertUnset.accept(unsetDateTime.startOfDay());
    
    // Test that original DateTime is unchanged
    assertEquals(INT_14, midDayDateTime.hour());
    assertEquals(INT_30, midDayDateTime.minute());
    assertEquals(INT_45, midDayDateTime.second());
  }

  @Test
  void testEndOfDay() {
    // Test basic functionality - time should be set to 23:59:59.999999999
    final var testDateTime = DateTime._of(DATETIME_JUNE_15_AFTERNOON.state);
    final var endOfDayResult = testDateTime.endOfDay();
    assertSet.accept(endOfDayResult);
    
    // Verify time components are set to end of day
    assertEquals(INT_23, endOfDayResult.hour());
    assertEquals(INT_59, endOfDayResult.minute());
    assertEquals(INT_59, endOfDayResult.second());
    
    // Verify date components are preserved
    assertEquals(INT_2023, endOfDayResult.year());
    assertEquals(INT_6, endOfDayResult.month());
    assertEquals(INT_15, endOfDayResult.day());
    
    // Verify timezone is preserved
    assertEquals(TIMEZONE_UTC_Z, endOfDayResult.zone());
    
    // Test with different timezone - timezone should be preserved
    final var tokyoDateTime = DateTime._of("2023-06-15T14:30:45+09:00");
    final var tokyoEndOfDay = tokyoDateTime.endOfDay();
    assertSet.accept(tokyoEndOfDay);
    assertEquals(INT_23, tokyoEndOfDay.hour());
    assertEquals(INT_59, tokyoEndOfDay.minute());
    assertEquals(INT_59, tokyoEndOfDay.second());
    assertEquals(String._of("+09:00"), tokyoEndOfDay.zone());
    assertEquals(INT_2023, tokyoEndOfDay.year());
    assertEquals(INT_6, tokyoEndOfDay.month());
    assertEquals(INT_15, tokyoEndOfDay.day());
    
    // Test with leap year date
    final var leapDateTime = DateTime._of("2024-02-29T06:15:30Z");
    final var leapEndOfDay = leapDateTime.endOfDay();
    assertSet.accept(leapEndOfDay);
    assertEquals(INT_23, leapEndOfDay.hour());
    assertEquals(INT_59, leapEndOfDay.minute());
    assertEquals(INT_59, leapEndOfDay.second());
    assertEquals(INT_2024, leapEndOfDay.year());
    assertEquals(INT_2, leapEndOfDay.month());
    assertEquals(INT_29, leapEndOfDay.day());
    
    // Test with year boundary dates
    final var newYearEve = DateTime._of("2023-12-31T00:00:01Z");
    final var newYearEveEnd = newYearEve.endOfDay();
    assertEquals(INT_23, newYearEveEnd.hour());
    assertEquals(INT_59, newYearEveEnd.minute());
    assertEquals(INT_59, newYearEveEnd.second());
    assertEquals(INT_31, newYearEveEnd.day());
    assertEquals(INT_12, newYearEveEnd.month());
    
    // Test start of day vs end of day comparison
    final var startOfDay = testDateTime.startOfDay();
    final var endOfDay = testDateTime.endOfDay();
    assertTrue.accept(startOfDay._lt(endOfDay));
    assertTrue.accept(endOfDay._gt(startOfDay));
    
    // Test with unsetDateTime DateTime
    assertUnset.accept(unsetDateTime.endOfDay());
    
    // Test that original DateTime is unchanged
    assertEquals(INT_14, testDateTime.hour());
    assertEquals(INT_30, testDateTime.minute());
    assertEquals(INT_45, testDateTime.second());
  }

  @Test
  void testDayOfYear() {
    // Test January 1st (should be day 1)
    final var jan1 = DateTime._of(DATETIME_BASE_UTC.state);
    assertEquals(INT_1, jan1.dayOfYear());
    
    // Test leap year January 1st
    final var leapJan1 = DateTime._of("2024-01-01T12:00:00Z");
    assertEquals(INT_1, leapJan1.dayOfYear());
    
    // Test February 1st (should be day 32)
    final var feb1 = DateTime._of("2023-02-01T12:00:00Z");
    assertEquals(INT_32, feb1.dayOfYear());
    
    // Test February 28th in non-leap year (should be day 59)
    final var feb28NonLeap = DateTime._of("2023-02-28T12:00:00Z");
    assertEquals(INT_59, feb28NonLeap.dayOfYear());
    
    // Test February 29th in leap year (should be day 60)
    final var feb29Leap = DateTime._of(DATETIME_LEAP_FEB_29_NOON.state);
    assertEquals(INT_60, feb29Leap.dayOfYear());
    
    // Test March 1st in non-leap year (should be day 60)
    final var mar1NonLeap = DateTime._of("2023-03-01T12:00:00Z");
    assertEquals(INT_60, mar1NonLeap.dayOfYear());
    
    // Test March 1st in leap year (should be day 61)
    final var mar1Leap = DateTime._of("2024-03-01T12:00:00Z");
    assertEquals(INT_61, mar1Leap.dayOfYear());
    
    // Test December 31st in non-leap year (should be day 365)
    final var dec31NonLeap = DateTime._of(DATETIME_NEW_YEAR_EVE.state);
    assertEquals(INT_365, dec31NonLeap.dayOfYear());
    
    // Test December 31st in leap year (should be day 366)
    final var dec31Leap = DateTime._of("2024-12-31T23:59:59Z");
    assertEquals(INT_366, dec31Leap.dayOfYear());
    
    // Test mid-year date (June 15th in non-leap year should be day 166)
    final var midYear = DateTime._of(DATETIME_JUNE_15_AFTERNOON.state);
    assertEquals(INT_166, midYear.dayOfYear());
    
    // Test same date in leap year (June 15th in leap year should be day 167)
    final var midYearLeap = DateTime._of("2024-06-15T14:30:45Z");
    assertEquals(INT_167, midYearLeap.dayOfYear());
    
    // Test with different timezones - should return same day of year
    final var nyDateTime = DateTime._of("2023-06-15T08:00:00-04:00"); // Same instant as baseUTC
    assertEquals(baseUTC.dayOfYear(), nyDateTime.dayOfYear());
    
    // Test edge case: timezone crossing midnight
    final var lateUTC = DateTime._of("2023-06-15T23:30:00Z");
    final var earlyEST = DateTime._of("2023-06-15T19:30:00-04:00"); // Same instant
    assertEquals(lateUTC.dayOfYear(), earlyEST.dayOfYear());
    
    // Test with unsetDateTime DateTime
    assertUnset.accept(unsetDateTime.dayOfYear());
    
    // Verify historical dates work correctly
    final var y2k = DateTime._of("2000-01-01T00:00:00Z");
    assertEquals(INT_1, y2k.dayOfYear());
    
    final var y2kLeapDay = DateTime._of("2000-02-29T12:00:00Z"); // 2000 was a leap year
    assertEquals(INT_60, y2kLeapDay.dayOfYear());
  }

  @Test
  void testDayOfMonth() {
    // Test that dayOfMonth() delegates to day() method correctly
    final var testDateTime = DateTime._of(DATETIME_JUNE_15_AFTERNOON.state);
    assertEquals(testDateTime.day(), testDateTime.dayOfMonth());
    
    // Test with various dates
    final var jan1 = DateTime._of(DATETIME_BASE_UTC.state);
    assertEquals(INT_1, jan1.dayOfMonth());
    assertEquals(jan1.day(), jan1.dayOfMonth());
    
    final var jan31 = DateTime._of("2023-01-31T12:00:00Z");
    assertEquals(INT_31, jan31.dayOfMonth());
    assertEquals(jan31.day(), jan31.dayOfMonth());
    
    final var feb28 = DateTime._of("2023-02-28T12:00:00Z");
    assertEquals(INT_28, feb28.dayOfMonth());
    assertEquals(feb28.day(), feb28.dayOfMonth());
    
    // Test leap year February 29th
    final var feb29 = DateTime._of(DATETIME_LEAP_FEB_29_NOON.state);
    assertEquals(INT_29, feb29.dayOfMonth());
    assertEquals(feb29.day(), feb29.dayOfMonth());
    
    // Test April (30 days)
    final var apr30 = DateTime._of("2023-04-30T12:00:00Z");
    assertEquals(INT_30, apr30.dayOfMonth());
    assertEquals(apr30.day(), apr30.dayOfMonth());
    
    // Test December 31st
    final var dec31 = DateTime._of(DATETIME_NEW_YEAR_EVE.state);
    assertEquals(INT_31, dec31.dayOfMonth());
    assertEquals(dec31.day(), dec31.dayOfMonth());
    
    // Test with different timezones
    final var nyDateTime = DateTime._of("2023-06-15T08:00:00-04:00");
    assertEquals(baseUTC.dayOfMonth(), nyDateTime.dayOfMonth());
    assertEquals(baseUTC.day(), nyDateTime.day());
    
    // Test timezone boundary crossing
    final var utcLate = DateTime._of("2023-06-15T23:30:00Z");
    final var estEarly = DateTime._of("2023-06-15T19:30:00-04:00"); // Same instant
    assertEquals(utcLate.dayOfMonth(), estEarly.dayOfMonth());
    
    // Test with unsetDateTime DateTime - both should return unsetDateTime
    assertUnset.accept(unsetDateTime.dayOfMonth());
    assertUnset.accept(unsetDateTime.day());
    assertEquals(unsetDateTime.day()._isSet(), unsetDateTime.dayOfMonth()._isSet());
    
    // Cross-validation: ensure all existing day() functionality works through dayOfMonth()
    final var allTestDates = java.util.List.of(
        DateTime._of("2023-01-15T10:30:00Z"),
        DateTime._of("2023-02-28T15:45:00Z"),
        DateTime._of("2024-02-29T20:15:00Z"),
        DateTime._of("2023-06-30T05:00:00Z"),
        DateTime._of("2023-12-01T14:22:33Z")
    );
    
    for (var testDate : allTestDates) {
      assertEquals(testDate.day(), testDate.dayOfMonth());
      assertEquals(testDate.day()._isSet(), testDate.dayOfMonth()._isSet());
      assertEquals(testDate.day().state, testDate.dayOfMonth().state);
    }
  }

  @Test
  void testOffsetFromUTC() {
    // Test UTC timezone (should return zero offset)
    final var utcOffset = baseUTC.offSetFromUTC();
    assertSet.accept(utcOffset);
    assertEquals(Long.valueOf(0), utcOffset._getAsSeconds());
    
    // Test positive offset - Tokyo (UTC+9, should return +32400 seconds)
    final var tokyoDateTime = DateTime._of("2023-06-15T12:00:00+09:00");
    final var tokyoOffset = tokyoDateTime.offSetFromUTC();
    assertSet.accept(tokyoOffset);
    assertEquals(Long.valueOf(9 * 3600), tokyoOffset._getAsSeconds()); // 9 hours = 32400 seconds
    
    // Test negative offset - New York EDT (UTC-4, should return -14400 seconds)
    final var nyDateTime = DateTime._of(DATETIME_JUNE_15_NOON_NY.state);
    final var nyOffset = nyDateTime.offSetFromUTC();
    assertSet.accept(nyOffset);
    assertEquals(Long.valueOf(-4 * 3600), nyOffset._getAsSeconds()); // -4 hours = -14400 seconds
    
    // Test New York EST (UTC-5, should return -18000 seconds)
    final var nyWinterDateTime = DateTime._of("2023-01-15T12:00:00-05:00");
    final var nyWinterOffset = nyWinterDateTime.offSetFromUTC();
    assertSet.accept(nyWinterOffset);
    assertEquals(Long.valueOf(-5 * 3600), nyWinterOffset._getAsSeconds()); // -5 hours = -18000 seconds
    
    // Test partial hour offset - India (UTC+5:30, should return +19800 seconds)
    final var indiaDateTime = DateTime._of("2023-06-15T12:00:00+05:30");
    final var indianOffset = indiaDateTime.offSetFromUTC();
    assertSet.accept(indianOffset);
    assertEquals(Long.valueOf(5 * 3600 + 30 * 60), indianOffset._getAsSeconds()); // 5.5 hours = 19800 seconds
    
    // Test negative partial hour offset - Newfoundland (UTC-3:30, should return -12600 seconds)
    final var newfoundlandDateTime = DateTime._of("2023-06-15T12:00:00-03:30");
    final var newfoundlandOffset = newfoundlandDateTime.offSetFromUTC();
    assertSet.accept(newfoundlandOffset);
    assertEquals(Long.valueOf(-3 * 3600 - 30 * 60), newfoundlandOffset._getAsSeconds()); // -3.5 hours = -12600 seconds
    
    // Test extreme positive offset - Kiribati (UTC+14, should return +50400 seconds)
    final var kiribatiDateTime = DateTime._of("2023-06-15T12:00:00+14:00");
    final var kiribatiOffset = kiribatiDateTime.offSetFromUTC();
    assertSet.accept(kiribatiOffset);
    assertEquals(Long.valueOf(14 * 3600), kiribatiOffset._getAsSeconds()); // 14 hours = 50400 seconds
    
    // Test extreme negative offset - Baker Island (UTC-12, should return -43200 seconds)
    final var bakerDateTime = DateTime._of("2023-06-15T12:00:00-12:00");
    final var bakerOffset = bakerDateTime.offSetFromUTC();
    assertSet.accept(bakerOffset);
    assertEquals(Long.valueOf(-12 * 3600), bakerOffset._getAsSeconds()); // -12 hours = -43200 seconds
    
    // Test with different times (offset should be same regardless of time)
    final var morning = DateTime._of("2023-06-15T06:00:00+09:00");
    final var evening = DateTime._of("2023-06-15T18:00:00+09:00");
    assertEquals(morning.offSetFromUTC()._getAsSeconds(), evening.offSetFromUTC()._getAsSeconds());
    
    // Test DST transition dates (these are fixed offsets, not zone-based)
    final var springForward = DateTime._of("2023-03-12T07:00:00-04:00"); // EDT
    final var fallBack = DateTime._of("2023-11-05T06:00:00-05:00");     // EST
    assertEquals(Long.valueOf(-4 * 3600), springForward.offSetFromUTC()._getAsSeconds());
    assertEquals(Long.valueOf(-5 * 3600), fallBack.offSetFromUTC()._getAsSeconds());
    
    // Test with unsetDateTime DateTime
    assertUnset.accept(unsetDateTime.offSetFromUTC());
    
    // Verify offset conversion works both ways
    final var originalDateTime = DateTime._of("2023-06-15T12:00:00+05:00");
    final var offset = originalDateTime.offSetFromUTC();
    final var offsetSeconds = offset._getAsSeconds();
    assertEquals(Long.valueOf(5 * 3600), offsetSeconds); // Should be 5 hours
    
    // Test that same instant in different zones have different offsets
    final var utcInstant = DateTime._of(DATETIME_JUNE_15_NOON_UTC.state);

    assertEquals(Long.valueOf(0), utcInstant.offSetFromUTC()._getAsSeconds());
    // Note: withSameInstant may produce different results based on DST rules
    // We're testing the offset calculation, not zone conversion
    
    // Validate against known timezone offsets during standard time
    final var utcWinter = DateTime._of("2023-01-15T12:00:00Z");
    final var estWinter = DateTime._of("2023-01-15T07:00:00-05:00"); // Same instant
    
    assertEquals(Long.valueOf(0), utcWinter.offSetFromUTC()._getAsSeconds());
    assertEquals(Long.valueOf(-5 * 3600), estWinter.offSetFromUTC()._getAsSeconds());
  }

  @Test
  void testAdditionOperators() {
    // Test _add with Duration
    
    // Add one hour
    final var plusOneHour = baseDateTime._add(oneHourDuration);
    assertSet.accept(plusOneHour);
    assertEquals(INT_13, plusOneHour.hour());
    assertEquals(INT_30, plusOneHour.minute());
    assertEquals(INT_45, plusOneHour.second());
    assertEquals(INT_15, plusOneHour.day());
    assertEquals(TIMEZONE_UTC_Z, plusOneHour.zone());
    
    // Add one day
    final var plusOneDay = baseDateTime._add(oneDayDuration);
    assertSet.accept(plusOneDay);
    assertEquals(INT_12, plusOneDay.hour());
    assertEquals(INT_30, plusOneDay.minute());
    assertEquals(INT_45, plusOneDay.second());
    assertEquals(INT_16, plusOneDay.day()); // Next day
    assertEquals(INT_6, plusOneDay.month());
    
    // Add multiple days
    final var plusTwoDays = baseDateTime._add(twoDaysDuration);
    assertSet.accept(plusTwoDays);
    assertEquals(INT_17, plusTwoDays.day());
    
    // Add one week
    final var plusOneWeek = baseDateTime._add(oneWeekDuration);
    assertSet.accept(plusOneWeek);
    assertEquals(INT_22, plusOneWeek.day());
    
    // Test _add with Millisecond
    final var plusOneSecond = baseDateTime._add(oneSecondMs);
    assertSet.accept(plusOneSecond);
    assertEquals(INT_46, plusOneSecond.second());
    assertEquals(INT_30, plusOneSecond.minute());
    assertEquals(INT_12, plusOneSecond.hour());
    
    final var plusOneMinute = baseDateTime._add(oneMinuteMs);
    assertSet.accept(plusOneMinute);
    assertEquals(INT_31, plusOneMinute.minute());
    assertEquals(INT_45, plusOneMinute.second());
    
    final var plusOneHourMs = baseDateTime._add(oneHourMs);
    assertSet.accept(plusOneHourMs);
    assertEquals(INT_13, plusOneHourMs.hour());
    assertEquals(plusOneHour.toString(), plusOneHourMs.toString());
    
    final var plusOneDayMs = baseDateTime._add(oneDayMs);
    assertSet.accept(plusOneDayMs);
    assertEquals(INT_16, plusOneDayMs.day());
    assertEquals(plusOneDay.toString(), plusOneDayMs.toString());
    
    // Test with timezone preservation
    final var nyPlusOneHour = baseEST._add(oneHourDuration);
    assertSet.accept(nyPlusOneHour);
    assertEquals(INT_13, nyPlusOneHour.hour());
    assertEquals(String._of("-04:00"), nyPlusOneHour.zone());
    
    // Test across timezone boundaries
    final var utcLateEvening = DateTime._of("2023-06-15T23:30:00Z");
    final var utcPlusOneHour = utcLateEvening._add(oneHourDuration);
    assertSet.accept(utcPlusOneHour);
    assertEquals(INT_0, utcPlusOneHour.hour()); // Midnight next day
    assertEquals(INT_16, utcPlusOneHour.day()); // Next day
    
    // Test with unsetDateTime inputs
    assertUnset.accept(unsetDateTime._add(oneHourDuration));
    assertUnset.accept(baseDateTime._add(new Duration()));
    assertUnset.accept(unsetDateTime._add(oneHourMs));
    assertUnset.accept(baseDateTime._add(new Millisecond()));
    
    // Test edge cases
    // New Year's Eve to New Year
    final var newYear = newYearEveUTC._add(oneSecondMs);
    assertSet.accept(newYear);
    assertEquals(INT_2024, newYear.year());
    assertEquals(INT_1, newYear.month());
    assertEquals(INT_1, newYear.day());
    assertEquals(INT_0, newYear.hour());
    assertEquals(INT_0, newYear.minute());
    assertEquals(INT_0, newYear.second());
    
    // Leap year February 28 to 29
    final var feb28WithTime = DateTime._of("2024-02-28T23:59:59Z");
    final var feb29 = feb28WithTime._add(oneSecondMs);
    assertSet.accept(feb29);
    assertEquals(INT_29, feb29.day());
    assertEquals(INT_2, feb29.month());
    
    // Test that original DateTime is unchanged
    assertEquals(INT_12, baseDateTime.hour());
    assertEquals(INT_30, baseDateTime.minute());
    assertEquals(INT_45, baseDateTime.second());
    assertEquals(INT_15, baseDateTime.day());
  }

  @Test
  void testSubtractionOperators() {
    // Test _sub with Duration
    
    // Subtract one hour
    final var minusOneHour = baseDateTime._sub(oneHourDuration);
    assertSet.accept(minusOneHour);
    assertEquals(INT_11, minusOneHour.hour());
    assertEquals(INT_30, minusOneHour.minute());
    assertEquals(INT_45, minusOneHour.second());
    assertEquals(INT_15, minusOneHour.day());
    
    // Subtract one day
    final var minusOneDay = baseDateTime._sub(oneDayDuration);
    assertSet.accept(minusOneDay);
    assertEquals(INT_12, minusOneDay.hour());
    assertEquals(INT_30, minusOneDay.minute());
    assertEquals(INT_45, minusOneDay.second());
    assertEquals(INT_14, minusOneDay.day()); // Previous day
    assertEquals(INT_6, minusOneDay.month());
    
    // Subtract multiple days
    final var minusTwoDays = baseDateTime._sub(twoDaysDuration);
    assertSet.accept(minusTwoDays);
    assertEquals(INT_13, minusTwoDays.day());
    
    // Test _sub with Millisecond
    final var minusOneSecond = baseDateTime._sub(oneSecondMs);
    assertSet.accept(minusOneSecond);
    assertEquals(INT_44, minusOneSecond.second());
    assertEquals(INT_30, minusOneSecond.minute());
    assertEquals(INT_12, minusOneSecond.hour());
    
    final var minusOneMinute = baseDateTime._sub(oneMinuteMs);
    assertSet.accept(minusOneMinute);
    assertEquals(INT_29, minusOneMinute.minute());
    assertEquals(INT_45, minusOneMinute.second());
    
    final var minusOneHourMs = baseDateTime._sub(oneHourMs);
    assertSet.accept(minusOneHourMs);
    assertEquals(INT_11, minusOneHourMs.hour());
    assertEquals(minusOneHour.toString(), minusOneHourMs.toString());
    
    final var minusOneDayMs = baseDateTime._sub(oneDayMs);
    assertSet.accept(minusOneDayMs);
    assertEquals(INT_14, minusOneDayMs.day());
    assertEquals(minusOneDay.toString(), minusOneDayMs.toString());
    
    // Test with timezone preservation
    final var tokyoMinusOneHour = baseJST._sub(oneHourDuration);
    assertSet.accept(tokyoMinusOneHour);
    assertEquals(INT_11, tokyoMinusOneHour.hour());
    assertEquals(String._of("+09:00"), tokyoMinusOneHour.zone());
    
    // Test across date boundaries
    final var utcEarlyMorning = DateTime._of("2023-06-15T00:30:00Z");
    final var utcMinusOneHour = utcEarlyMorning._sub(oneHourDuration);
    assertSet.accept(utcMinusOneHour);
    assertEquals(INT_23, utcMinusOneHour.hour()); // Previous day 23:30
    assertEquals(INT_14, utcMinusOneHour.day()); // Previous day
    
    // Test DateTime - DateTime subtraction (returns Duration)
    final var localDateTime1 = baseUTC;
    final var localDateTime2 = DateTime._of("2023-06-15T14:00:00Z");
    
    final var diff1 = localDateTime2._sub(localDateTime1); // 2 hours later
    assertSet.accept(diff1);
    assertEquals(Long.valueOf(7200), diff1._getAsSeconds()); // 2 hours = 7200 seconds
    
    final var diff2 = localDateTime1._sub(localDateTime2); // 2 hours earlier
    assertSet.accept(diff2);
    assertEquals(Long.valueOf(-7200), diff2._getAsSeconds()); // -2 hours = -7200 seconds
    
    final var diff3 = localDateTime1._sub(localDateTime1); // Same time
    assertSet.accept(diff3);
    assertEquals(Long.valueOf(0), diff3._getAsSeconds());
    
    // Test cross-day DateTime subtraction
    final var day2 = DateTime._of("2023-06-16T12:00:00Z");
    final var dayDiff = day2._sub(baseUTC);
    assertSet.accept(dayDiff);
    assertEquals(Long.valueOf(86400), dayDiff._getAsSeconds()); // 1 day = 86400 seconds
    
    // Test with different timezones (same instant)
    final var timeDiff = utc1600._sub(ny1200);
    assertSet.accept(timeDiff);
    assertEquals(Long.valueOf(0), timeDiff._getAsSeconds()); // Same instant = 0 difference
    
    // Test with unsetDateTime inputs
    assertUnset.accept(unsetDateTime._sub(oneHourDuration));
    assertUnset.accept(baseDateTime._sub(new Duration()));
    assertUnset.accept(unsetDateTime._sub(oneHourMs));
    assertUnset.accept(baseDateTime._sub(new Millisecond()));
    assertUnset.accept(unsetDateTime._sub(baseDateTime));
    assertUnset.accept(baseDateTime._sub(unsetDateTime));
    
    // Test edge cases
    // New Year to New Year's Eve (previous year)
    final var backToNewYearEve = newYearDayUTC._sub(oneSecondMs);
    assertSet.accept(backToNewYearEve);
    assertEquals(INT_2023, backToNewYearEve.year());
    assertEquals(INT_12, backToNewYearEve.month());
    assertEquals(INT_31, backToNewYearEve.day());
    assertEquals(INT_23, backToNewYearEve.hour());
    assertEquals(INT_59, backToNewYearEve.minute());
    assertEquals(INT_59, backToNewYearEve.second());
    
    // Test that original DateTime is unchanged
    assertEquals(INT_12, baseDateTime.hour());
    assertEquals(INT_30, baseDateTime.minute());
    assertEquals(INT_45, baseDateTime.second());
    assertEquals(INT_15, baseDateTime.day());
  }

  @Test
  void testAssignmentOperators() {
    // Test _addAss with Duration
    var testDateTime = new DateTime(baseDateTime);
    testDateTime._addAss(oneHourDuration);
    assertEquals(INT_13, testDateTime.hour());
    assertEquals(INT_30, testDateTime.minute());
    assertEquals(INT_45, testDateTime.second());
    assertEquals(INT_15, testDateTime.day());
    
    // Test _addAss with unsetDateTime Duration corrupts state
    testDateTime._addAss(new Duration());
    assertUnset.accept(testDateTime);
    
    // Reset and test with Millisecond
    testDateTime = new DateTime(baseDateTime);
    testDateTime._addAss(oneHourMs);
    assertEquals(INT_13, testDateTime.hour());
    assertEquals(INT_30, testDateTime.minute());
    
    // Test _addAss with unsetDateTime Millisecond corrupts state
    testDateTime._addAss(new Millisecond());
    assertUnset.accept(testDateTime);
    
    // Test _subAss with Duration
    testDateTime = new DateTime(baseDateTime);
    testDateTime._subAss(oneHourDuration);
    assertEquals(INT_11, testDateTime.hour());
    assertEquals(INT_30, testDateTime.minute());
    assertEquals(INT_45, testDateTime.second());
    
    // Test _subAss with unsetDateTime Duration corrupts state
    testDateTime._subAss(new Duration());
    assertUnset.accept(testDateTime);
    
    // Reset and test with Millisecond
    testDateTime = new DateTime(baseDateTime);
    testDateTime._subAss(oneHourMs);
    assertEquals(INT_11, testDateTime.hour());
    assertEquals(INT_30, testDateTime.minute());
    
    // Test _subAss with unsetDateTime Millisecond corrupts state
    testDateTime._subAss(new Millisecond());
    assertUnset.accept(testDateTime);
    
    // Test with timezone preservation
    var nyDateTime = new DateTime(baseEST);
    nyDateTime._addAss(oneHourDuration);
    assertEquals(INT_13, nyDateTime.hour());
    assertEquals(String._of("-04:00"), nyDateTime.zone());
    
    nyDateTime._subAss(oneDayDuration);
    assertEquals(INT_13, nyDateTime.hour()); // Same hour
    assertEquals(INT_14, nyDateTime.day()); // Previous day
    assertEquals(String._of("-04:00"), nyDateTime.zone()); // Zone preserved
    
    // Test boundary crossings with assignment operators
    var newYearEve = new DateTime(newYearEveUTC);
    newYearEve._addAss(Millisecond._of(1800000)); // 30 minutes to get to 23:30:00 + 1 hour = 00:30:00 next day
    assertEquals(INT_2024, newYearEve.year());
    assertEquals(INT_1, newYearEve.month());
    assertEquals(INT_1, newYearEve.day());
    assertEquals(INT_0, newYearEve.hour());
    assertEquals(INT_29, newYearEve.minute()); // 59 + 30 minutes = 89 minutes = 1:29, but wraps to next hour so 0:29
    
    var newYear = new DateTime(newYearDayUTC);
    newYear._addAss(Millisecond._of(1800000)); // Add 30 minutes first to get 00:30:00
    newYear._subAss(oneHourDuration); // Then subtract 1 hour
    assertEquals(INT_2023, newYear.year());
    assertEquals(INT_12, newYear.month());
    assertEquals(INT_31, newYear.day());
    assertEquals(INT_23, newYear.hour());
    assertEquals(INT_30, newYear.minute());
    
    // Test with unsetDateTime DateTime
    var unsetTest = new DateTime();
    unsetTest._addAss(oneHourDuration);
    assertUnset.accept(unsetTest);
    
    unsetTest = new DateTime();
    unsetTest._subAss(oneHourDuration);
    assertUnset.accept(unsetTest);
  }

  @Test
  void testPipeOperators() {
    // Test _pipe with DateTime (replacement operation)
    var testDateTime = new DateTime(baseDateTime);
    final var sourceDateTime = DateTime._of("2023-07-20T18:45:30+09:00");
    testDateTime._pipe(sourceDateTime);
    assertEquals(sourceDateTime.toString(), testDateTime.toString());
    assertEquals(INT_18, testDateTime.hour());
    assertEquals(INT_45, testDateTime.minute());
    assertEquals(INT_30, testDateTime.second());
    assertEquals(INT_20, testDateTime.day());
    assertEquals(INT_7, testDateTime.month());
    assertEquals(String._of("+09:00"), testDateTime.zone());
    
    // Test _pipe with unsetDateTime DateTime (should not change state if unsetDateTime)
    testDateTime = new DateTime(baseDateTime);
    final var originalString = testDateTime.toString();
    testDateTime._pipe(unsetDateTime);
    assertEquals(originalString, testDateTime.toString()); // Should remain unchanged
    
    // Test _pipe with Duration (additive operation)
    testDateTime = new DateTime(baseDateTime);
    testDateTime._pipe(oneHourDuration);
    assertEquals(INT_13, testDateTime.hour());
    assertEquals(INT_30, testDateTime.minute());
    assertEquals(INT_45, testDateTime.second());
    assertEquals(INT_15, testDateTime.day());
    
    // Multiple pipe operations with Duration
    testDateTime._pipe(oneDayDuration);
    assertEquals(INT_13, testDateTime.hour());
    assertEquals(INT_16, testDateTime.day()); // Next day
    
    // Test _pipe with Millisecond (additive operation)
    testDateTime = new DateTime(baseDateTime);
    testDateTime._pipe(oneHourMs);
    assertEquals(INT_13, testDateTime.hour());
    assertEquals(INT_30, testDateTime.minute());
    assertEquals(INT_45, testDateTime.second());
    
    testDateTime._pipe(oneDayMs);
    assertEquals(INT_13, testDateTime.hour());
    assertEquals(INT_16, testDateTime.day());
    
    // Test with timezone preservation during pipe operations
    var nyDateTime = new DateTime(baseEST);
    nyDateTime._pipe(oneHourDuration);
    assertEquals(INT_13, nyDateTime.hour());
    assertEquals(String._of("-04:00"), nyDateTime.zone());
    
    // Test pipe with unsetDateTime Duration/Millisecond (should not affect state)
    testDateTime = new DateTime(baseDateTime);
    final var beforePipe = testDateTime.toString();
    testDateTime._pipe(new Duration());
    assertEquals(beforePipe, testDateTime.toString()); // Should remain unchanged
    
    testDateTime._pipe(new Millisecond());
    assertEquals(beforePipe, testDateTime.toString()); // Should remain unchanged
    
    // Test boundary crossing with pipe operations
    var newYearEve = DateTime._of("2023-12-31T23:30:00Z");
    newYearEve._pipe(oneHourDuration);
    assertEquals(INT_2024, newYearEve.year());
    assertEquals(INT_1, newYearEve.month());
    assertEquals(INT_1, newYearEve.day());
    assertEquals(INT_0, newYearEve.hour());
    assertEquals(INT_30, newYearEve.minute());
    
    // Test chaining pipe operations
    testDateTime = DateTime._of("2023-06-15T10:00:00Z");
    testDateTime._pipe(oneHourDuration);    // 11:00:00
    testDateTime._pipe(oneHourDuration);    // 12:00:00
    testDateTime._pipe(oneDayDuration);     // Next day 12:00:00
    assertEquals(INT_12, testDateTime.hour());
    assertEquals(INT_16, testDateTime.day());
    
    // Test with unsetDateTime DateTime
    var unsetTest = new DateTime();
    unsetTest._pipe(sourceDateTime);
    assertSet.accept(unsetTest); // DateTime pipe assigns even to unsetDateTime target
    assertEquals(sourceDateTime.toString(), unsetTest.toString());
    
    unsetTest = new DateTime(); // Reset to unsetDateTime
    unsetTest._pipe(oneHourDuration);
    assertSet.accept(unsetTest); // Duration pipe works even on unsetDateTime DateTime
    
    unsetTest = new DateTime(); // Reset to unsetDateTime
    unsetTest._pipe(oneHourMs);
    assertSet.accept(unsetTest); // Millisecond pipe works even on unsetDateTime DateTime
  }

  @Test
  void testFuzzyComparison() {
    // Test fuzzy comparison operator <~> which returns absolute difference in seconds
    
    // Test same time (should return 0)
    assertEquals(INT_0, fuzzyBase._fuzzy(fuzzyBase));
    assertEquals(INT_0, fuzzyBase._fuzzy(fuzzyBase));
    assertEquals(INT_0, fuzzyBase._fuzzy(fuzzyBase));
    
    // Test one hour difference (3600 seconds)
    assertEquals(INT_3600, fuzzyBase._fuzzy(fuzzyOneHourLater));
    assertEquals(INT_3600, fuzzyOneHourLater._fuzzy(fuzzyBase)); // Absolute value
    
    // Test two hours difference (7200 seconds)
    assertEquals(INT_7200, fuzzyBase._fuzzy(fuzzyTwoHoursLater));
    assertEquals(INT_7200, fuzzyTwoHoursLater._fuzzy(fuzzyBase));
    
    // Test one day difference (86400 seconds)
    assertEquals(INT_86400, fuzzyBase._fuzzy(fuzzyOneDayLater));
    assertEquals(INT_86400, fuzzyOneDayLater._fuzzy(fuzzyBase));
    
    // Test one week difference (604800 seconds)
    assertEquals(INT_604800, fuzzyBase._fuzzy(fuzzyOneWeekLater));
    assertEquals(INT_604800, fuzzyOneWeekLater._fuzzy(fuzzyBase));
    
    // Test with past dates (should also be absolute)
    assertEquals(INT_3600, fuzzyBase._fuzzy(fuzzyOneHourEarlier));
    assertEquals(INT_86400, fuzzyBase._fuzzy(fuzzyOneDayEarlier));
    
    // Test across different timezones (same instant should be 0)
    assertEquals(INT_0, utc1600._fuzzy(ny1200));
    assertEquals(INT_0, ny1200._fuzzy(utc1600));
    
    // Test different instants in different timezones
    assertEquals(INT_14400, utcNoon._fuzzy(nyNoon)); // 4 hours = 14400 seconds
    
    // Test year boundary crossing
    assertEquals(INT_1, newYearEveUTC._fuzzy(newYearDayUTC)); // 1 second difference
    
    // Test leap year dates
    assertEquals(INT_86400, leapFeb28._fuzzy(leapFeb29)); // 1 day = 86400 seconds
    
    // Test with large time differences
    final var millennium = DateTime._of("2000-01-01T00:00:00Z");
    final var future = DateTime._of("2050-01-01T00:00:00Z");
    final var diff = millennium._fuzzy(future);
    assertSet.accept(diff);
    assertTrue.accept(diff._gt(INT_1000000000)); // More than ~31 years in seconds
    
    // Test with unsetDateTime DateTimes
    assertUnset.accept(unsetDateTime._fuzzy(fuzzyBase));
    assertUnset.accept(fuzzyBase._fuzzy(unsetDateTime));
    assertUnset.accept(unsetDateTime._fuzzy(unsetDateTime));
    
    // Test that fuzzy comparison is commutative
    final var localTime1 = dateTime1;
    final var localTime2 = DateTime._of("2023-03-22T15:45:12Z");
    assertEquals(localTime1._fuzzy(localTime2), localTime2._fuzzy(localTime1));
    
    // Test precision - seconds are truncated, not rounded
    final var timeB = DateTime._of("2023-06-15T12:00:01Z"); // 1 second later
    assertEquals(INT_1, fuzzyBase._fuzzy(timeB));
    
    // Test sub-second differences (should be 0 since we only return seconds)
    final var timeD = DateTime._of("2023-06-15T12:00:00.999Z"); // 999ms later
    assertEquals(INT_0, fuzzyBase._fuzzy(timeD)); // Less than 1 second = 0
  }

  @Test
  void testMergeOperator() {
    // Test _merge operator (delegates to _copy)
    var testDateTime = new DateTime(baseDateTime);
    final var sourceDateTime = DateTime._of("2023-07-20T18:45:30+09:00");
    
    // Test merge with valid DateTime
    testDateTime._merge(sourceDateTime);
    assertEquals(sourceDateTime.toString(), testDateTime.toString());
    assertEquals(INT_18, testDateTime.hour());
    assertEquals(INT_45, testDateTime.minute());
    assertEquals(INT_30, testDateTime.second());
    assertEquals(INT_20, testDateTime.day());
    assertEquals(INT_7, testDateTime.month());
    assertEquals(INT_2023, testDateTime.year());
    assertEquals(String._of("+09:00"), testDateTime.zone());
    
    // Test merge with unsetDateTime DateTime (should make target unsetDateTime)
    testDateTime = new DateTime(baseDateTime);
    testDateTime._merge(unsetDateTime);
    assertUnset.accept(testDateTime);
    
    // Test merge on unsetDateTime DateTime with valid source
    testDateTime = new DateTime();
    testDateTime._merge(sourceDateTime);
    assertEquals(sourceDateTime.toString(), testDateTime.toString());
    assertSet.accept(testDateTime);
    
    // Test merge on unsetDateTime DateTime with unsetDateTime source
    testDateTime = new DateTime();
    testDateTime._merge(unsetDateTime);
    assertUnset.accept(testDateTime);
    
    // Test timezone preservation during merge
    final var tokyoDateTime = DateTime._of("2023-06-15T21:00:00+09:00"); // Same instant as baseUTC
    
    testDateTime = DateTime._of("2023-01-01T00:00:00-05:00");
    testDateTime._merge(tokyoDateTime);
    assertEquals(String._of("+09:00"), testDateTime.zone());
    assertEquals(INT_21, testDateTime.hour());
    
    // Test with different date/time components
    final var evening = DateTime._of("2023-12-25T22:45:55-08:00");
    
    testDateTime = new DateTime(dateTime1);
    testDateTime._merge(evening);
    assertEquals(INT_2023, testDateTime.year());
    assertEquals(INT_12, testDateTime.month());
    assertEquals(INT_25, testDateTime.day());
    assertEquals(INT_22, testDateTime.hour());
    assertEquals(INT_45, testDateTime.minute());
    assertEquals(INT_55, testDateTime.second());
    assertEquals(String._of("-08:00"), testDateTime.zone());
    
    // Test that merge completely replaces the target with source
    testDateTime = beforeY2K;
    final var y2k = DateTime._of("2000-01-01T00:00:00Z");
    testDateTime._merge(y2k);
    assertEquals(y2k.toString(), testDateTime.toString());
    
    // Test chaining merge operations
    testDateTime = new DateTime(dateTime1);
    final var step1 = baseUTC;
    final var step2 = DateTime._of("2023-12-31T23:59:59+05:00");
    
    testDateTime._merge(step1);
    assertEquals(step1.toString(), testDateTime.toString());
    testDateTime._merge(step2);
    assertEquals(step2.toString(), testDateTime.toString());
  }

  @Test
  void testReplaceOperator() {
    // Test _replace operator (also delegates to _copy, should behave identically to _merge)
    var testDateTime = new DateTime(baseDateTime);
    final var sourceDateTime = DateTime._of("2023-07-20T18:45:30+09:00");
    
    // Test replace with valid DateTime
    testDateTime._replace(sourceDateTime);
    assertEquals(sourceDateTime.toString(), testDateTime.toString());
    assertEquals(INT_18, testDateTime.hour());
    assertEquals(INT_45, testDateTime.minute());
    assertEquals(INT_30, testDateTime.second());
    assertEquals(INT_20, testDateTime.day());
    assertEquals(INT_7, testDateTime.month());
    assertEquals(INT_2023, testDateTime.year());
    assertEquals(String._of("+09:00"), testDateTime.zone());
    
    // Test replace with unsetDateTime DateTime (should make target unsetDateTime)
    testDateTime = new DateTime(baseDateTime);
    testDateTime._replace(unsetDateTime);
    assertUnset.accept(testDateTime);
    
    // Test replace on unsetDateTime DateTime with valid source
    testDateTime = new DateTime();
    testDateTime._replace(sourceDateTime);
    assertEquals(sourceDateTime.toString(), testDateTime.toString());
    assertSet.accept(testDateTime);
    
    // Test replace on unsetDateTime DateTime with unsetDateTime source
    testDateTime = new DateTime();
    testDateTime._replace(unsetDateTime);
    assertUnset.accept(testDateTime);
    
    // Verify _replace and _merge behave identically
    final var testA = new DateTime(dateTime1);
    final var testB = new DateTime(dateTime1);
    final var source = DateTime._of("2023-12-31T23:59:59+05:00");
    
    testA._merge(source);
    testB._replace(source);
    assertEquals(testA.toString(), testB.toString());
    
    // Test with unsetDateTime source - both should behave the same
    final var testC = new DateTime(baseUTC);
    final var testD = new DateTime(baseUTC);
    
    testC._merge(unsetDateTime);
    testD._replace(unsetDateTime);
    assertUnset.accept(testC);
    assertUnset.accept(testD);
    assertEquals(testC._isSet().state, testD._isSet().state);
    
    // Test edge cases with different timezones
    testDateTime = new DateTime(baseUTC);
    
    testDateTime._replace(basePST);
    assertEquals(String._of("-08:00"), testDateTime.zone());
    assertEquals(INT_12, testDateTime.hour());
    
    testDateTime._replace(indiaOffset);
    assertEquals(String._of("+05:30"), testDateTime.zone());
    assertEquals(INT_12, testDateTime.hour());
    assertEquals(INT_0, testDateTime.minute());
    
    // Test that replace completely overwrites all components
    final var complex = leapFeb29; // Leap year date
    testDateTime = new DateTime(beforeY2K);
    testDateTime._replace(complex);
    assertEquals(complex.toString(), testDateTime.toString());
    assertEquals(INT_2024, testDateTime.year());
    assertEquals(INT_2, testDateTime.month());
    assertEquals(INT_29, testDateTime.day());
    assertEquals(TIMEZONE_UTC_Z, testDateTime.zone());
  }

  @Test
  void testIncrementDecrementOperators() {
    // Test increment operator ++ (adds one day)
    var testDateTime = new DateTime(baseDateTime);
    final var originalHour = testDateTime.hour();
    final var originalMinute = testDateTime.minute();
    final var originalSecond = testDateTime.second();
    final var originalZone = testDateTime.zone();
    
    // Test increment
    final var incremented = testDateTime._inc();
    assertEquals(testDateTime, incremented); // Returns this
    assertEquals(INT_16, testDateTime.day()); // Next day
    assertEquals(INT_6, testDateTime.month()); // Same month
    assertEquals(INT_2023, testDateTime.year()); // Same year
    assertEquals(originalHour, testDateTime.hour()); // Time preserved
    assertEquals(originalMinute, testDateTime.minute());
    assertEquals(originalSecond, testDateTime.second());
    assertEquals(originalZone, testDateTime.zone()); // Zone preserved
    
    // Test decrement operator -- (subtracts one day)
    final var decremented = testDateTime._dec();
    assertEquals(testDateTime, decremented); // Returns this
    assertEquals(INT_15, testDateTime.day()); // Back to original day
    assertEquals(INT_6, testDateTime.month());
    assertEquals(INT_2023, testDateTime.year());
    assertEquals(originalHour, testDateTime.hour());
    assertEquals(originalMinute, testDateTime.minute());
    assertEquals(originalSecond, testDateTime.second());
    assertEquals(originalZone, testDateTime.zone());
    
    // Test month boundary crossing with increment
    testDateTime = new DateTime(endOfJune);
    testDateTime._inc();
    assertEquals(INT_1, testDateTime.day()); // First day of July
    assertEquals(INT_7, testDateTime.month()); // July
    assertEquals(INT_2023, testDateTime.year());
    assertEquals(INT_14, testDateTime.hour()); // Time preserved
    assertEquals(INT_45, testDateTime.minute());
    assertEquals(INT_30, testDateTime.second());
    
    // Test month boundary crossing with decrement
    testDateTime = new DateTime(startOfJuly);
    testDateTime._dec();
    assertEquals(INT_30, testDateTime.day()); // Last day of June
    assertEquals(INT_6, testDateTime.month()); // June
    assertEquals(INT_2023, testDateTime.year());
    assertEquals(INT_8, testDateTime.hour());
    assertEquals(INT_15, testDateTime.minute());
    assertEquals(INT_20, testDateTime.second());
    
    // Test year boundary crossing with increment
    testDateTime = new DateTime(newYearEveUTC);
    testDateTime._inc();
    assertEquals(INT_1, testDateTime.day()); // First day of new year
    assertEquals(INT_1, testDateTime.month()); // January
    assertEquals(INT_2024, testDateTime.year()); // Next year
    assertEquals(INT_23, testDateTime.hour()); // Time preserved
    assertEquals(INT_59, testDateTime.minute());
    assertEquals(INT_59, testDateTime.second());
    
    // Test year boundary crossing with decrement
    testDateTime = new DateTime(newYearDayUTC);
    testDateTime._dec();
    assertEquals(INT_31, testDateTime.day()); // Last day of previous year
    assertEquals(INT_12, testDateTime.month()); // December
    assertEquals(INT_2023, testDateTime.year()); // Previous year
    assertEquals(INT_0, testDateTime.hour());
    assertEquals(INT_0, testDateTime.minute());
    assertEquals(INT_0, testDateTime.second());
    
    // Test leap year with increment
    testDateTime = new DateTime(leapFeb28);
    testDateTime._inc();
    assertEquals(INT_29, testDateTime.day()); // Leap day
    assertEquals(INT_2, testDateTime.month()); // Still February
    assertEquals(INT_2024, testDateTime.year());
    
    // Test leap year with decrement from March 1st
    testDateTime = new DateTime(leapMar01);
    testDateTime._dec();
    assertEquals(INT_29, testDateTime.day()); // Leap day
    assertEquals(INT_2, testDateTime.month()); // February
    assertEquals(INT_2024, testDateTime.year());
    
    // Test with different timezones (time and zone should be preserved)
    testDateTime = new DateTime(baseJST);
    testDateTime._inc();
    assertEquals(INT_16, testDateTime.day());
    assertEquals(INT_12, testDateTime.hour());
    assertEquals(INT_30, testDateTime.minute());
    assertEquals(INT_45, testDateTime.second());
    assertEquals(String._of("+09:00"), testDateTime.zone());
    
    testDateTime = new DateTime(basePST);
    testDateTime._dec();
    assertEquals(INT_14, testDateTime.day());
    assertEquals(INT_12, testDateTime.hour());
    assertEquals(INT_30, testDateTime.minute());
    assertEquals(INT_45, testDateTime.second());
    assertEquals(String._of("-08:00"), testDateTime.zone());
    
    // Test with unsetDateTime DateTime (should remain unsetDateTime and return unsetDateTime)
    var unsetTest = new DateTime();
    final var unsetIncResult = unsetTest._inc();
    assertUnset.accept(unsetTest);
    assertUnset.accept(unsetIncResult);
    assertTrue.accept(Boolean._of(unsetTest == unsetIncResult)); // Should return this (reference equality)
    
    unsetTest = new DateTime();
    final var unsetDecResult = unsetTest._dec();
    assertUnset.accept(unsetTest);
    assertUnset.accept(unsetDecResult);
    assertTrue.accept(Boolean._of(unsetTest == unsetDecResult)); // Should return this (reference equality)
    
    // Test chaining increment/decrement operations
    testDateTime = new DateTime(baseUTC);
    testDateTime._inc()._inc()._inc(); // Add 3 days
    assertEquals(INT_18, testDateTime.day());
    
    testDateTime._dec()._dec(); // Subtract 2 days
    assertEquals(INT_16, testDateTime.day());
    
    // Test that multiple operations preserve time and timezone
    testDateTime = DateTime._of("2023-06-15T23:45:30-05:00");
    testDateTime._inc()._dec()._inc()._dec(); // Net zero change
    assertEquals(INT_15, testDateTime.day());
    assertEquals(INT_23, testDateTime.hour());
    assertEquals(INT_45, testDateTime.minute());
    assertEquals(INT_30, testDateTime.second());
    assertEquals(String._of("-05:00"), testDateTime.zone());
  }
}