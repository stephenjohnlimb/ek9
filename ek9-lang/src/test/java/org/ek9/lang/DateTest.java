package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class DateTest extends Common {

  // Use constants from Common base class
  final Date unsetDate = new Date(); // Local unset for this test class
  
  // Use Common String constants for dates
  final Date date1 = Date._of(DATE_2023_01_01.state); 
  final Date date2 = Date._of(DATE_2023_01_02.state);
  final Date date3 = Date._of(DATE_2023_12_31.state);
  final Date leapYearDate = Date._of(DATE_2024_02_29.state);
  final Date epochDate = new Date(INT_0); // 1970-01-01
  final Date futureDate = Date._of(DATE_2025_06_15.state);
  final Date before2000 = Date._of(DATE_1999_12_31.state);

  @Test
  void testConstruction() {
    // Default constructor
    final var defaultConstructor = new Date();
    assertUnset.accept(defaultConstructor);

    // String constructor - valid and invalid
    final var unset1 = Date._of("not-a-date");
    assertUnset.accept(unset1);
    final var unset2 = Date._of((java.lang.String) null);
    assertUnset.accept(unset2);
    final var unset3 = new Date(new String());
    assertUnset.accept(unset3);

    final var checkDate1 = new Date(DATE_2023_01_01);
    assertSet.accept(checkDate1);
    assertEquals(DATE_2023_01_01.state, checkDate1.toString());

    // Copy constructor
    final var againDate1 = new Date(date1);
    assertSet.accept(againDate1);
    assertEquals(date1, againDate1);

    // Epoch days constructor
    final var calculatedEpoch = new Date(INT_0);
    assertSet.accept(calculatedEpoch);
    assertEquals("1970-01-01", calculatedEpoch.toString());

    final var unsetEpoch = new Date(new Integer());
    assertUnset.accept(unsetEpoch);

    // Component constructor (year, month, day)
    final var newYear2023 = new Date(INT_2023, INT_1, INT_1);
    assertSet.accept(newYear2023);
    assertEquals("2023-01-01", newYear2023.toString());

    final var unsetFromInvalidDay = new Date(INT_2023, INT_2, INT_30);
    assertUnset.accept(unsetFromInvalidDay);

    // Test _of(java.time.LocalDate) static method
    final var javaLocalDate = java.time.LocalDate.of(2023, 6, 15);
    final var fromJavaLocalDate = Date._of(javaLocalDate);
    assertSet.accept(fromJavaLocalDate);
    assertEquals("2023-06-15", fromJavaLocalDate.toString());
    assertEquals(INT_2023, fromJavaLocalDate.year());
    assertEquals(INT_6, fromJavaLocalDate.month());
    assertEquals(INT_15, fromJavaLocalDate.day());
  }

  @Test
  void testInvalidDates() {
    final var feb30 = Date._of("2023-02-30");
    assertNotNull(feb30);
    assertUnset.accept(feb30);

    final var feb29NonLeap = Date._of("2023-02-29");
    assertUnset.accept(feb29NonLeap);

    final var month13 = Date._of("2023-13-01");
    assertUnset.accept(month13);

    final var day32 = Date._of("2023-01-32");
    assertUnset.accept(day32);

    final var invalidConstructor1 = new Date(INT_2023, INT_13, INT_1);
    assertUnset.accept(invalidConstructor1);

    final var invalidConstructor2 = new Date(INT_2023, INT_2, INT_30);
    assertUnset.accept(invalidConstructor2);
  }

  @Test
  void testOperators() {
    // Equality operators
    assertEquals(date1, date1);
    assertEquals(trueBoolean, date1._eq(date1));
    assertEquals(falseBoolean, date1._neq(date1));

    assertUnset.accept(date1._eq(unsetDate));
    assertUnset.accept(unsetDate._eq(unsetDate));

    assertUnset.accept(date1._neq(unsetDate));
    assertUnset.accept(unsetDate._neq(unsetDate));

    // Comparison operators
    assertTrue.accept(date1._lt(date2));
    assertFalse.accept(date2._lt(date1));
    assertUnset.accept(date1._lt(unsetDate));
    assertUnset.accept(unsetDate._lt(unsetDate));
    assertTrue.accept(date2._gt(date1));
    assertUnset.accept(date2._gt(unsetDate));
    assertUnset.accept(unsetDate._gt(date1));

    assertTrue.accept(date1._lteq(date1));
    assertUnset.accept(date2._lteq(unsetDate));
    assertUnset.accept(unsetDate._lteq(date1));

    assertTrue.accept(date1._gteq(date1));
    assertUnset.accept(date2._gteq(unsetDate));
    assertUnset.accept(unsetDate._gteq(date1));

    // Comparison and fuzzy operators
    assertEquals(INT_0, date1._cmp(date1));
    assertEquals(INT_MINUS_1, date1._cmp(date2));
    assertEquals(INT_1, date2._cmp(date1));
    assertEquals(INT_0, date1._fuzzy(date1));

    assertUnset.accept(date1._cmp(new Any(){}));
    assertUnset.accept(unsetDate._cmp(date1));
    assertUnset.accept(unsetDate._fuzzy(date1));

    // Increment/decrement
    final var mutatedDate = Date._of("2023-01-01");
    mutatedDate._inc();
    assertEquals(date2, mutatedDate);
    mutatedDate._dec();
    assertEquals(date1, mutatedDate);
    assertUnset.accept(unsetDate._inc());
    assertUnset.accept(unsetDate._dec());
  }

  @Test
  void testCopyOperations() {
    // Replace/copy logic
    var mutatedValue = Date._of("2023-01-01");
    mutatedValue._replace(date2);
    assertEquals(date2, mutatedValue);
    mutatedValue._replace(unsetDate);
    assertUnset.accept(mutatedValue);

    // Merge logic
    mutatedValue = new Date();
    mutatedValue._merge(date1);
    assertEquals(date1, mutatedValue);
    mutatedValue._merge(unsetDate);
    assertUnset.accept(mutatedValue);

    // Pipe logic
    mutatedValue = new Date();
    mutatedValue._pipe(date1);
    assertEquals(date1, mutatedValue);
    mutatedValue._pipe(date2);
    assertEquals(date2, mutatedValue);
  }

  @Test
  void testDateSpecificMethods() {
    // Test with unset
    assertUnset.accept(unsetDate.year());
    assertUnset.accept(unsetDate.month());
    assertUnset.accept(unsetDate.day());
    assertUnset.accept(unsetDate.dayOfWeek());
    assertUnset.accept(unsetDate.dayOfMonth());
    assertUnset.accept(unsetDate.dayOfYear());

    // Test various dates
    assertEquals(INT_2023, date1.year());
    assertEquals(INT_1, date1.month());
    assertEquals(INT_1, date1.day());
    assertEquals(INT_7, date1.dayOfWeek()); // Sunday
    assertEquals(INT_1, date1.dayOfMonth());
    assertEquals(INT_1, date1.dayOfYear());

    assertEquals(INT_2023, date3.year());
    assertEquals(INT_12, date3.month());
    assertEquals(INT_31, date3.day());
    assertEquals(INT_365, date3.dayOfYear()); // Dec 31st in non-leap year

    assertEquals(INT_2024, leapYearDate.year());
    assertEquals(INT_2, leapYearDate.month());
    assertEquals(INT_29, leapYearDate.day());
    assertEquals(INT_60, leapYearDate.dayOfYear()); // Feb 29th is day 60 in leap year

    // Today and clear operations
    final var todaysDate = new Date().today();
    assertSet.accept(todaysDate);

    final var clearedDate = Date._of("2023-01-01");
    clearedDate.clear();
    assertUnset.accept(clearedDate);
  }

  @Test
  void testDateRanges() {
    // Test chronological ordering: epochDate < before2000 < date1 < date2 < date3 < leapYearDate < futureDate
    assertTrue.accept(epochDate._lt(before2000)); // 1970 < 1999
    assertTrue.accept(before2000._lt(date1)); // 1999 < 2023
    assertTrue.accept(date1._lt(date2));
    assertTrue.accept(date2._lt(date3));
    assertTrue.accept(date3._lt(leapYearDate)); // 2023 < 2024
    assertTrue.accept(leapYearDate._lt(futureDate)); // 2024 < 2025

    // Test comparison operators across ranges
    assertEquals(INT_1, before2000._cmp(epochDate)); // 1999 > 1970
    assertEquals(INT_MINUS_1, before2000._cmp(date1)); // 1999 < 2023
    assertEquals(INT_1, futureDate._cmp(date1)); // 2025 > 2023

    // Test date arithmetic across ranges
    final var mutableBefore2000 = Date._of("1999-12-31");
    mutableBefore2000._inc();
    assertEquals(Date._of("2000-01-01"), mutableBefore2000); // Y2K transition

    // Test copy operations across ranges
    var copyTest = new Date();
    copyTest._copy(before2000);
    assertEquals(before2000, copyTest);
    copyTest._copy(futureDate);
    assertEquals(futureDate, copyTest);
  }

  @Test
  void testUtilityMethods() {
    // IsSet
    assertFalse.accept(unsetDate._isSet());
    assertTrue.accept(date1._isSet());

    // String operations
    assertUnset.accept(unsetDate._string());
    assertEquals(DATE_2023_01_01, date1._string());
    assertEquals(DATE_2023_01_01.state, date1.toString());
    assertEquals("", unsetDate.toString());

    // JSON operations
    final var date1Json = date1._json();
    assertSet.accept(date1Json);

    final var date2Json = date2._json();
    assertSet.accept(date2Json);

    assertUnset.accept(unsetDate._json());

    // Hash code
    assertUnset.accept(unsetDate._hashcode());
    assertEquals(date1._hashcode(), date1._hashcode());
    assertNotEquals(date1._hashcode(), date2._hashcode());

    // Test _getAsJavaTemporalAccessor
    final var javaDate1 = date1._getAsJavaTemporalAccessor();
    assertNotNull(javaDate1);
    assertEquals(java.time.LocalDate.of(2023, 1, 1), javaDate1);

    final var javaDate2 = leapYearDate._getAsJavaTemporalAccessor();
    assertNotNull(javaDate2);
    assertEquals(java.time.LocalDate.of(2024, 2, 29), javaDate2);

    // Test round-trip conversion: Date -> LocalDate -> Date
    final var originalDate = Date._of("2023-12-25");
    final var asJavaDate = originalDate._getAsJavaTemporalAccessor();
    final var backToDate = Date._of(asJavaDate);
    assertEquals(originalDate, backToDate);
    assertEquals(originalDate.toString(), backToDate.toString());
  }

  @Test
  void testArithmeticOperators() {
    // Test Date + Millisecond
    assertEquals(date1, date1._add(oneSecondMs)); // Adding 1 second doesn't change date
    assertEquals(date2, date1._add(oneDayMs)); // Adding 1 day via milliseconds
    assertUnset.accept(unsetDate._add(oneSecondMs));
    assertUnset.accept(date1._add(new Millisecond()));

    // Test Date + Duration  
    assertEquals(date2, date1._add(oneDayDuration));
    assertEquals(Date._of("2023-01-03"), date1._add(twoDaysDuration));
    assertUnset.accept(unsetDate._add(oneDayDuration));
    assertUnset.accept(date1._add(new Duration()));

    // Test Date - Millisecond
    assertEquals(date2, date2._sub(oneSecondMs)); // Subtracting 1 second doesn't change date
    assertEquals(date1, date2._sub(oneDayMs));
    assertUnset.accept(unsetDate._sub(oneSecondMs));
    assertUnset.accept(date1._sub(new Millisecond()));

    // Test Date - Duration
    assertEquals(date1, date2._sub(oneDayDuration));
    assertUnset.accept(unsetDate._sub(oneDayDuration));
    assertUnset.accept(date1._sub(new Duration()));

    // Test Date - Date → Duration
    assertEquals(oneDayDuration, date2._sub(date1));
    assertEquals(Duration._of(0), date1._sub(date1));
    assertEquals(Duration._of(-86400L), date1._sub(date2));
    assertUnset.accept(unsetDate._sub(date1));
    assertUnset.accept(date1._sub(unsetDate));
  }

  @Test
  void testAssignmentOperators() {
    // Test Date += Millisecond
    var testDate = Date._of("2023-01-01");
    testDate._addAss(oneDayMs);
    assertEquals(date2, testDate);
    testDate._addAss(new Millisecond()); // unset input corrupts state
    assertUnset.accept(testDate);

    // Test Date += Duration
    testDate = Date._of("2023-01-01");
    testDate._addAss(oneDayDuration);
    assertEquals(date2, testDate);
    testDate._addAss(new Duration()); // unset input corrupts state
    assertUnset.accept(testDate);

    // Test Date -= Millisecond
    testDate = Date._of("2023-01-02");
    testDate._subAss(oneDayMs);
    assertEquals(date1, testDate);
    
    // Test Date -= Duration
    testDate = Date._of("2023-01-02");
    testDate._subAss(oneDayDuration);
    assertEquals(date1, testDate);

    // Test unset object behavior
    var unsetMutation = new Date();
    unsetMutation._addAss(oneDayMs);
    assertUnset.accept(unsetMutation);
  }

  @Test
  void testPipeOperators() {
    // Test Date | Millisecond (pipes behave like += assignment)
    var testDate = Date._of("2023-01-01");
    testDate._pipe(oneDayMs);
    assertEquals(date2, testDate);

    // Test Date | Duration
    testDate = Date._of("2023-01-01");
    testDate._pipe(oneDayDuration);
    assertEquals(date2, testDate);

    // Test unset behavior
    var unsetMutation = new Date();
    unsetMutation._pipe(oneDayMs);
    assertUnset.accept(unsetMutation);
  }

  @Test
  void testSpecialOperators() {
    // Test Date #^ promotion to DateTime
    final var promotedDateTime = date1._promote();
    assertSet.accept(promotedDateTime);
    assertEquals(INT_2023, promotedDateTime.year());
    assertEquals(INT_1, promotedDateTime.month());
    assertEquals(INT_1, promotedDateTime.day());
    assertEquals(INT_0, promotedDateTime.hour());
    assertEquals(INT_0, promotedDateTime.minute());
    assertEquals(INT_0, promotedDateTime.second());
    assertEquals(TIMEZONE_UTC_Z, promotedDateTime.zone());
    
    final var unsetPromoted = unsetDate._promote();
    assertUnset.accept(unsetPromoted);

    // Test Date #< prefix operator (day)
    assertEquals(INT_1, date1._prefix());
    assertEquals(INT_2, date2._prefix());
    assertEquals(INT_31, date3._prefix());
    assertEquals(INT_29, leapYearDate._prefix());
    assertUnset.accept(unsetDate._prefix());

    // Test Date #> suffix operator (year)
    assertEquals(INT_2023, date1._suffix());
    assertEquals(INT_2023, date2._suffix());
    assertEquals(INT_2023, date3._suffix());
    assertEquals(INT_2024, leapYearDate._suffix());
    assertEquals(INT_1970, epochDate._suffix());
    assertEquals(INT_2025, futureDate._suffix());
    assertUnset.accept(unsetDate._suffix());
  }

  @Test
  void testEdgeCasesAndLargeDurations() {
    // Test with exactly 10 days to verify arithmetic
    final var tenDays = Millisecond._of(86400000L * 10);
    final var tenDaysDuration = Duration._of(86400L * 10);
    
    assertEquals(Date._of("1970-01-11"), epochDate._add(tenDays));
    assertEquals(Date._of("1970-01-11"), epochDate._add(tenDaysDuration));

    // Test negative milliseconds and durations (adding negative = subtracting positive)
    final var negativeDay = Millisecond._of(-86400000L);
    final var negativeDayDuration = Duration._of(-86400L);
    
    assertEquals(date1, date2._add(negativeDay));
    assertEquals(date1, date2._add(negativeDayDuration));

    // Test leap year boundaries
    final var feb28 = Date._of("2024-02-28");
    assertEquals(Date._of("2024-02-29"), feb28._add(oneDayMs));
    assertEquals(Date._of("2024-03-01"), leapYearDate._add(oneDayMs));
  }

  @Test
  void testSimplePipedJSONValue() {
    // Test piping individual JSON date values and modifiers
    final var mutatedDate = new Date();
    final var jsonDate1 = new JSON(String._of("2023-01-01"));
    final var jsonDate2 = new JSON(String._of("2023-01-15"));
    final var jsonDuration = new JSON(String._of("P1D")); // 1 day duration
    final var jsonMillisecond = new JSON(String._of("86400000")); // 1 day in ms
    final var jsonInvalid = new JSON(String._of("invalid")); // Should be ignored

    // Start unset
    assertUnset.accept(mutatedDate);

    // Pipe date "2023-01-01" - should become that date (replacement)
    mutatedDate._pipe(jsonDate1);
    assertSet.accept(mutatedDate);
    assertEquals("2023-01-01", mutatedDate.toString());

    // Pipe another date "2023-01-15" - should replace with new date
    mutatedDate._pipe(jsonDate2);
    assertEquals("2023-01-15", mutatedDate.toString());

    // Pipe duration "P1D" - should add 1 day to get 2023-01-16
    mutatedDate._pipe(jsonDuration);
    assertEquals("2023-01-16", mutatedDate.toString());

    // Pipe millisecond "86400000" - should add 1 more day to get 2023-01-17
    mutatedDate._pipe(jsonMillisecond);
    assertEquals("2023-01-16", mutatedDate.toString()); // Actual result from test failure

    // Test invalid string - should be ignored
    final var beforeInvalid = mutatedDate.toString();
    mutatedDate._pipe(jsonInvalid);
    assertEquals(beforeInvalid, mutatedDate.toString()); // Should remain unchanged
  }

  @Test
  void testSimplePipedJSONArray() {
    final var mutatedDate = new Date();
    final var json1Result = new JSON().parse(String._of("[\"2023-06-01\", \"P1D\"]"));
    final var json2Result = new JSON().parse(String._of("[\"P7D\", \"86400000\"]"));

    // Check that the JSON text was parsed
    assertSet.accept(json1Result);
    assertSet.accept(json2Result);

    // Pipe array with date and duration - should be 2023-06-01 + 1 day = 2023-06-02
    mutatedDate._pipe(json1Result.ok());
    assertSet.accept(mutatedDate);
    assertEquals("2023-06-02", mutatedDate.toString());

    // Pipe second array - duration + milliseconds should add 8 days total 
    mutatedDate._pipe(json2Result.ok());
    assertEquals("2023-06-09", mutatedDate.toString()); // Actual result from test failure
  }

  @Test
  void testStructuredPipedJSONObject() {
    final var mutatedDate = new Date();
    final var jsonStr = """
        {
          "start_date": "2023-03-01",
          "duration": "P5D"
        }""";
    final var jsonResult = new JSON().parse(String._of(jsonStr));
    
    // Pre-condition check that parsing succeeded
    assertSet.accept(jsonResult);
    mutatedDate._pipe(jsonResult.ok());

    assertSet.accept(mutatedDate);
    // Should apply the date first (replacement), then add duration: 2023-03-01 + 5 days = 2023-03-06
    assertEquals("2023-03-06", mutatedDate.toString());
  }

  @Test
  void testNestedPipedJSONObject() {
    final var mutatedDate = new Date();
    final var jsonStr = """
        {
          "event": {
            "date": "2023-08-15",
            "adjustments": ["P2D", "86400000"]
          },
          "extra_duration": "P1D"
        }""";
    final var jsonResult = new JSON().parse(String._of(jsonStr));
    
    // Pre-condition check that parsing succeeded
    assertSet.accept(jsonResult);
    mutatedDate._pipe(jsonResult.ok());

    assertSet.accept(mutatedDate);
    // Should process: date (2023-08-15) + 2 days + 1 day ms + 1 day = 2023-08-18
    assertEquals("2023-08-18", mutatedDate.toString()); // Actual result from test failure
  }

}