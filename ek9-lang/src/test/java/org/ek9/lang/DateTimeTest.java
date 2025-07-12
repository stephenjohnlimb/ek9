package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DateTimeTest extends Common {

  final Boolean true1 = Boolean._of("true");
  final Boolean false1 = Boolean._of("false");

  final DateTime unset = new DateTime();
  final DateTime epoch = DateTime._of("1970-01-01T00:00:00Z");
  final DateTime dateTime1 = DateTime._of("2023-01-01T12:00:00Z");
  final DateTime dateTime2 = DateTime._of("2023-01-02T14:30:15Z");
  final DateTime dateTime3 = DateTime._of("2023-12-31T23:59:59Z");
  final DateTime leapYear = DateTime._of("2024-02-29T18:45:30Z");
  final DateTime futureDateTime = DateTime._of("2025-06-15T09:15:45Z");
  final DateTime beforeY2K = DateTime._of("1999-12-31T23:59:59Z");

  // Time zone test data
  final DateTime utcDateTime = DateTime._of("2023-06-15T12:00:00Z");
  final DateTime newYearUTC = DateTime._of("2023-01-01T00:00:00Z");
  final DateTime newYearTokyo = DateTime._of("2023-01-01T00:00:00+09:00");

  final Date simpleDate = Date._of("2023-06-15");

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

    final var checkDateTime1 = new DateTime(String._of("2023-01-01T12:00:00Z"));
    assertSet.accept(checkDateTime1);
    assertEquals("2023-01-01T12:00:00Z", checkDateTime1.toString());

    // Copy constructor
    final var againDateTime1 = new DateTime(dateTime1);
    assertSet.accept(againDateTime1);
    assertEquals(dateTime1, againDateTime1);

    // Date constructor
    final var fromDate = new DateTime(simpleDate);
    assertSet.accept(fromDate);
    assertEquals(Integer._of(2023), fromDate.year());
    assertEquals(Integer._of(6), fromDate.month());
    assertEquals(Integer._of(15), fromDate.day());
    assertEquals(Integer._of(0), fromDate.hour());
    assertEquals(Integer._of(0), fromDate.minute());
    assertEquals(Integer._of(0), fromDate.second());
    assertEquals(String._of("Z"), fromDate.zone());

    // Component constructor (year, month, day)
    final var ymd = new DateTime(Integer._of(2023), Integer._of(1), Integer._of(1));
    assertSet.accept(ymd);
    assertEquals(Integer._of(2023), ymd.year());
    assertEquals(Integer._of(1), ymd.month());
    assertEquals(Integer._of(1), ymd.day());
    assertEquals(Integer._of(0), ymd.hour());
    assertEquals(String._of("Z"), ymd.zone());

    // Component constructor (year, month, day, hour)
    final var ymdh = new DateTime(Integer._of(2023), Integer._of(1), Integer._of(1), Integer._of(12));
    assertSet.accept(ymdh);
    assertEquals(Integer._of(12), ymdh.hour());
    assertEquals(Integer._of(0), ymdh.minute());

    // Component constructor (year, month, day, hour, minute)
    final var ymdhm = new DateTime(Integer._of(2023), Integer._of(1), Integer._of(1), Integer._of(12), Integer._of(30));
    assertSet.accept(ymdhm);
    assertEquals(Integer._of(30), ymdhm.minute());
    assertEquals(Integer._of(0), ymdhm.second());

    // Component constructor (year, month, day, hour, minute, second)
    final var ymdhms = new DateTime(Integer._of(2023), Integer._of(1), Integer._of(1), Integer._of(12), Integer._of(30),
        Integer._of(45));
    assertSet.accept(ymdhms);
    assertEquals(Integer._of(45), ymdhms.second());

    // Test invalid date components - these should not throw exceptions but return unset
    final var unsetDate = new DateTime(new Integer(), Integer._of(1), Integer._of(1));
    assertUnset.accept(unsetDate);
  }

  @Test
  void testTimeZoneOperations() {
    // Test withSameInstant - same moment in time, different zone
    final var utcToNY = utcDateTime.withSameInstant(String._of("America/New_York"));
    assertSet.accept(utcToNY);
    assertEquals(String._of("America/New_York"), utcToNY.zone());
    // UTC 12:00 = EDT 08:00 (UTC-4)
    assertEquals(Integer._of(8), utcToNY.hour());
    assertEquals(utcDateTime.minute(), utcToNY.minute());
    assertEquals(utcDateTime.second(), utcToNY.second());

    final var utcToLondon = utcDateTime.withSameInstant(String._of("Europe/London"));
    assertSet.accept(utcToLondon);
    assertEquals(String._of("Europe/London"), utcToLondon.zone());
    // UTC 12:00 = BST 13:00 (UTC+1)
    assertEquals(Integer._of(13), utcToLondon.hour());

    final var utcToTokyo = utcDateTime.withSameInstant(String._of("Asia/Tokyo"));
    assertSet.accept(utcToTokyo);
    assertEquals(String._of("Asia/Tokyo"), utcToTokyo.zone());
    // UTC 12:00 = JST 21:00 (UTC+9)
    assertEquals(Integer._of(21), utcToTokyo.hour());

    // Test withZone - same local time, different zone
    final var utcWithNYZone = utcDateTime.withZone(String._of("America/New_York"));
    assertSet.accept(utcWithNYZone);
    assertEquals(String._of("America/New_York"), utcWithNYZone.zone());
    // Same local time 12:00, but now in NY timezone
    assertEquals(Integer._of(12), utcWithNYZone.hour());
    assertEquals(utcDateTime.minute(), utcWithNYZone.minute());
    assertEquals(utcDateTime.second(), utcWithNYZone.second());

    // Test with invalid timezone - these may throw exceptions, so we need to handle them
    // The implementation might throw exceptions for invalid zones, so we'll test valid zones only

    // Test with unset
    assertUnset.accept(unset.withSameInstant(String._of("UTC")));
    assertUnset.accept(unset.withZone(String._of("UTC")));
  }

  @Test
  void testCrossTimezoneComparisons() {
    // Test basic timezone conversion without comparing across different timezone representations
    final var utc1200 = DateTime._of("2023-06-15T12:00:00Z");

    // Convert to different timezone and verify components
    final var ny0800 = utc1200.withSameInstant(String._of("America/New_York"));
    assertSet.accept(ny0800);
    // Just verify the conversion worked
    assertNotEquals(utc1200.hour(), ny0800.hour()); // Hours should be different

    // Test back to UTC
    final var backToUTC = ny0800.withSameInstant(String._of("UTC"));
    assertTrue.accept(utc1200._eq(backToUTC));

    // Test inequality - different instants
    final var ny0900 = DateTime._of("2023-06-15T09:00:00-04:00"); // 1 hour later than ny0800
    assertEquals(true1, ny0800._lt(ny0900));
    assertEquals(true1, ny0900._gt(ny0800));
    assertEquals(Integer._of(-1), ny0800._cmp(ny0900));
    assertEquals(Integer._of(1), ny0900._cmp(ny0800));

    // Test New Year across timezones
    // Tokyo New Year happens 9 hours before UTC New Year
    assertEquals(true1, newYearTokyo._lt(newYearUTC));
    assertEquals(Integer._of(-1), newYearTokyo._cmp(newYearUTC));

    // Test edge case: same local time, different zones
    final var localTime1200UTC = DateTime._of("2023-06-15T12:00:00Z");
    final var localTime1200NY = DateTime._of("2023-06-15T12:00:00-04:00");
    // These are different instants (4 hours apart)
    assertEquals(true1, localTime1200UTC._lt(localTime1200NY));
    assertEquals(Integer._of(-1), localTime1200UTC._cmp(localTime1200NY));

    //So twelve in UTC on this date, will be 8AM in new york. But the same instant in time.
    final var checkWhatItWouldBeInNY = localTime1200UTC.withSameInstant(String._of("America/New_York"));
    assertEquals("2023-06-15T08:00:00-04:00", checkWhatItWouldBeInNY.toString());

    //That's what date times mean in EK9 - are they the same instant.
    final var checkSame = localTime1200UTC._eq(checkWhatItWouldBeInNY);
    assertTrue.accept(checkSame);

  }

  @Test
  void testOperators() {
    final var dateTime1Again = DateTime._of("2023-01-01T12:00:00Z");

    // Equality operators
    assertEquals(dateTime1Again, dateTime1);
    assertEquals(true1, dateTime1._eq(dateTime1Again));
    assertEquals(false1, dateTime1._neq(dateTime1Again));

    assertUnset.accept(dateTime1._eq(unset));
    assertUnset.accept(unset._eq(unset));

    assertUnset.accept(dateTime1._neq(unset));
    assertUnset.accept(unset._neq(unset));

    // Comparison operators
    assertTrue.accept(dateTime1._lt(dateTime2));
    assertFalse.accept(dateTime2._lt(dateTime1));
    assertUnset.accept(dateTime1._lt(unset));
    assertUnset.accept(unset._lt(unset));

    assertTrue.accept(dateTime2._gt(dateTime1));
    assertUnset.accept(dateTime2._gt(unset));
    assertUnset.accept(unset._gt(dateTime1));

    assertTrue.accept(dateTime1._lteq(dateTime1Again));
    assertUnset.accept(dateTime2._lteq(unset));
    assertUnset.accept(unset._lteq(dateTime1));

    assertTrue.accept(dateTime1._gteq(dateTime1Again));
    assertUnset.accept(dateTime2._gteq(unset));
    assertUnset.accept(unset._gteq(dateTime1));

    // Comparison operator
    assertEquals(Integer._of(0), dateTime1._cmp(dateTime1Again));
    // dateTime1 is 2023-01-01T12:00:00Z, dateTime2 is 2023-01-02T14:30:15Z
    assertEquals(Integer._of(-1), dateTime1._cmp(dateTime2));
    assertEquals(Integer._of(1), dateTime2._cmp(dateTime1));

    assertUnset.accept(unset._cmp(dateTime1));
    assertUnset.accept(dateTime2._cmp(new Any(){}));

  }

  @Test
  void testCopyOperations() {
    // Copy logic
    var mutatedValue = DateTime._of("2023-01-01T12:00:00Z");
    mutatedValue._copy(dateTime2);
    assertTrue.accept(mutatedValue._eq(dateTime2));
    mutatedValue._copy(unset);
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
    // Test with unset
    assertUnset.accept(unset.year());
    assertUnset.accept(unset.month());
    assertUnset.accept(unset.day());
    assertUnset.accept(unset.hour());
    assertUnset.accept(unset.minute());
    assertUnset.accept(unset.second());
    assertUnset.accept(unset.zone());
    assertUnset.accept(unset.dayOfWeek());

    // Test various datetimes
    assertEquals(Integer._of(2023), dateTime1.year());
    assertEquals(Integer._of(1), dateTime1.month());
    assertEquals(Integer._of(1), dateTime1.day());
    assertEquals(Integer._of(12), dateTime1.hour());
    assertEquals(Integer._of(0), dateTime1.minute());
    assertEquals(Integer._of(0), dateTime1.second());
    assertEquals(String._of("Z"), dateTime1.zone());
    assertEquals(Integer._of(7), dateTime1.dayOfWeek()); // Sunday

    assertEquals(Integer._of(2023), dateTime2.year());
    assertEquals(Integer._of(1), dateTime2.month());
    assertEquals(Integer._of(2), dateTime2.day());
    assertEquals(Integer._of(14), dateTime2.hour());
    assertEquals(Integer._of(30), dateTime2.minute());
    assertEquals(Integer._of(15), dateTime2.second());

    assertEquals(Integer._of(2024), leapYear.year());
    assertEquals(Integer._of(2), leapYear.month());
    assertEquals(Integer._of(29), leapYear.day());
    assertEquals(Integer._of(18), leapYear.hour());
    assertEquals(Integer._of(45), leapYear.minute());
    assertEquals(Integer._of(30), leapYear.second());

    // Test epoch
    assertEquals(Integer._of(1970), epoch.year());
    assertEquals(Integer._of(1), epoch.month());
    assertEquals(Integer._of(1), epoch.day());
    assertEquals(Integer._of(0), epoch.hour());
    assertEquals(Integer._of(0), epoch.minute());
    assertEquals(Integer._of(0), epoch.second());
    assertEquals(Integer._of(4), epoch.dayOfWeek()); // Thursday

    // Test timezone components
    assertEquals(String._of("Z"), utcDateTime.zone());
    final var nyTime = utcDateTime.withSameInstant(String._of("America/New_York"));
    assertEquals(String._of("America/New_York"), nyTime.zone());
  }

  @Test
  void testStringConversion() {
    // Test _string() method (ISO format)
    assertUnset.accept(unset._string());
    assertEquals(String._of("2023-01-01T12:00:00Z"), dateTime1._string());
    assertEquals(String._of("2023-01-02T14:30:15Z"), dateTime2._string());
    assertEquals(String._of("1970-01-01T00:00:00Z"), epoch._string());

    // Test toString() method
    assertEquals("2023-01-01T12:00:00Z", dateTime1.toString());
    assertEquals("2023-01-02T14:30:15Z", dateTime2.toString());
    assertEquals("", unset.toString());

    // Test rfc7231() method (HTTP date format)
    assertUnset.accept(unset.rfc7231());

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
    final var newYorkDateTime = utcDateTime.withSameInstant(String._of("America/New_York"));
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
    final var clearedDateTime = DateTime._of("2023-01-01T12:00:00Z");
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
    assertEquals(Integer._of(2023), fromString.year());
    assertEquals(Integer._of(6), fromString.month());
    assertEquals(Integer._of(15), fromString.day());
    assertEquals(Integer._of(15), fromString.hour());
    assertEquals(Integer._of(30), fromString.minute());
    assertEquals(Integer._of(45), fromString.second());

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
    assertEquals(Integer._of(2023), fromHttp1.year());
    assertEquals(Integer._of(6), fromHttp1.month());
    assertEquals(Integer._of(15), fromHttp1.day());
    assertEquals(Integer._of(15), fromHttp1.hour());
    assertEquals(Integer._of(30), fromHttp1.minute());
    assertEquals(Integer._of(45), fromHttp1.second());


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
    assertEquals(Integer._of(1), beforeY2K._cmp(epoch));
    assertEquals(Integer._of(-1), beforeY2K._cmp(dateTime1));
    assertEquals(Integer._of(1), futureDateTime._cmp(dateTime1));

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
    final var nyDuringDST = utcDuringDST.withSameInstant(String._of("America/New_York"));
    assertEquals(Integer._of(12), nyDuringDST.hour()); // EDT is UTC-4

    // Test same instant in different zones during standard time
    final var utcDuringStandard = DateTime._of("2023-01-15T17:00:00Z");
    final var nyDuringStandard = utcDuringStandard.withSameInstant(String._of("America/New_York"));
    assertEquals(Integer._of(12), nyDuringStandard.hour()); // EST is UTC-5

    // Test round-trip timezone conversion
    final var original = DateTime._of("2023-06-15T14:30:00Z");
    final var toNY = original.withSameInstant(String._of("America/New_York"));
    final var backToUTC = toNY.withSameInstant(String._of("UTC"));
    assertEquals(true1, original._eq(backToUTC));

    // Test withZone vs withSameInstant difference
    final var baseUTC = DateTime._of("2023-06-15T12:00:00Z");
    final var sameInstantNY = baseUTC.withSameInstant(String._of("America/New_York"));
    final var sameLocalNY = baseUTC.withZone(String._of("America/New_York"));

    // Same instant should show 08:00 in NY (EDT = UTC-4)
    assertEquals(Integer._of(8), sameInstantNY.hour());
    // Same local time should show 12:00 in NY
    assertEquals(Integer._of(12), sameLocalNY.hour());

    // These represent different instants in time
    assertEquals(true1, sameLocalNY._gt(sameInstantNY));
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
    final var newYorkDateTime = DateTime._of("2023-06-15T12:00:00-04:00");
    final var javaNY = newYorkDateTime._getAsJavaTemporalAccessor();
    assertNotNull(javaNY);
    assertEquals(java.time.ZonedDateTime.of(2023, 6, 15, 12, 0, 0, 0, java.time.ZoneId.of("-04:00")), javaNY);
  }

  @Test
  void testLeapYearAndMonthEdgeCases() {
    // Test leap year February 29th
    assertSet.accept(leapYear);
    assertEquals(Integer._of(2024), leapYear.year());
    assertEquals(Integer._of(2), leapYear.month());
    assertEquals(Integer._of(29), leapYear.day());

    // Test end of months
    final var endOfJan = DateTime._of("2023-01-31T23:59:59Z");
    final var endOfFeb = DateTime._of("2023-02-28T23:59:59Z");
    final var endOfMar = DateTime._of("2023-03-31T23:59:59Z");
    final var endOfApr = DateTime._of("2023-04-30T23:59:59Z");

    assertEquals(Integer._of(31), endOfJan.day());
    assertEquals(Integer._of(28), endOfFeb.day());
    assertEquals(Integer._of(31), endOfMar.day());
    assertEquals(Integer._of(30), endOfApr.day());

    // Test chronological order
    assertTrue.accept(endOfJan._lt(endOfFeb));
    assertTrue.accept(endOfFeb._lt(endOfMar));
    assertTrue.accept(endOfMar._lt(endOfApr));

    // Test leap year February vs non-leap year
    final var nonLeapFeb28 = DateTime._of("2023-02-28T12:00:00Z");
    final var leapFeb29 = DateTime._of("2024-02-29T12:00:00Z");
    assertTrue.accept(nonLeapFeb28._lt(leapFeb29));
  }
}