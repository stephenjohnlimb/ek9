package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class DateTest extends Common {

  final Boolean true1 = Boolean._of("true");
  final Boolean false1 = Boolean._of("false");

  final Date unset = new Date();
  final Date date1 = Date._of("2023-01-01");
  final Date date2 = Date._of("2023-01-02");
  final Date date3 = Date._of("2023-12-31");
  final Date leapYear = Date._of("2024-02-29");
  final Date epochDate = new Date(Integer._of(0)); // 1970-01-01
  final Date futureDate = Date._of("2025-06-15");
  final Date before2000 = Date._of("1999-12-31");

  // Common test constants for all temporal arithmetic tests
  final Millisecond oneSecond = Millisecond._of(1000);
  final Millisecond oneDay = Millisecond._of(86400000L);
  final Duration oneDayDuration = Duration._of(86400L);
  final Duration twoDaysDuration = Duration._of(172800L);

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

    final var checkDate1 = new Date(String._of("2023-01-01"));
    assertSet.accept(checkDate1);
    assertEquals("2023-01-01", checkDate1.toString());

    // Copy constructor
    final var againDate1 = new Date(date1);
    assertSet.accept(againDate1);
    assertEquals(date1, againDate1);

    // Epoch days constructor
    final var epoch = new Date(Integer._of(0));
    assertSet.accept(epoch);
    assertEquals("1970-01-01", epoch.toString());

    final var unsetEpoch = new Date(new Integer());
    assertUnset.accept(unsetEpoch);

    // Component constructor (year, month, day)
    final var newYear2023 = new Date(Integer._of(2023), Integer._of(1), Integer._of(1));
    assertSet.accept(newYear2023);
    assertEquals("2023-01-01", newYear2023.toString());

    final var unsetFromInvalidDay = new Date(Integer._of(2023), Integer._of(2), Integer._of(30));
    assertUnset.accept(unsetFromInvalidDay);

    // Test _of(java.time.LocalDate) static method
    final var javaLocalDate = java.time.LocalDate.of(2023, 6, 15);
    final var fromJavaLocalDate = Date._of(javaLocalDate);
    assertSet.accept(fromJavaLocalDate);
    assertEquals("2023-06-15", fromJavaLocalDate.toString());
    assertEquals(Integer._of(2023), fromJavaLocalDate.year());
    assertEquals(Integer._of(6), fromJavaLocalDate.month());
    assertEquals(Integer._of(15), fromJavaLocalDate.day());
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

    final var invalidConstructor1 = new Date(Integer._of(2023), Integer._of(13), Integer._of(1));
    assertUnset.accept(invalidConstructor1);

    final var invalidConstructor2 = new Date(Integer._of(2023), Integer._of(2), Integer._of(30));
    assertUnset.accept(invalidConstructor2);
  }

  @Test
  void testOperators() {
    // Equality operators
    assertEquals(date1, date1);
    assertEquals(true1, date1._eq(date1));
    assertEquals(false1, date1._neq(date1));

    assertUnset.accept(date1._eq(unset));
    assertUnset.accept(unset._eq(unset));

    assertUnset.accept(date1._neq(unset));
    assertUnset.accept(unset._neq(unset));

    // Comparison operators
    assertTrue.accept(date1._lt(date2));
    assertFalse.accept(date2._lt(date1));
    assertUnset.accept(date1._lt(unset));
    assertUnset.accept(unset._lt(unset));
    assertTrue.accept(date2._gt(date1));
    assertUnset.accept(date2._gt(unset));
    assertUnset.accept(unset._gt(date1));

    assertTrue.accept(date1._lteq(date1));
    assertUnset.accept(date2._lteq(unset));
    assertUnset.accept(unset._lteq(date1));

    assertTrue.accept(date1._gteq(date1));
    assertUnset.accept(date2._gteq(unset));
    assertUnset.accept(unset._gteq(date1));

    // Comparison and fuzzy operators
    assertEquals(Integer._of(0), date1._cmp(date1));
    assertEquals(Integer._of(-1), date1._cmp(date2));
    assertEquals(Integer._of(1), date2._cmp(date1));
    assertEquals(Integer._of(0), date1._fuzzy(date1));

    assertUnset.accept(date1._cmp(new Any(){}));
    assertUnset.accept(unset._cmp(date1));
    assertUnset.accept(unset._fuzzy(date1));

    // Increment/decrement
    final var mutatedDate = Date._of("2023-01-01");
    mutatedDate._inc();
    assertEquals(date2, mutatedDate);
    mutatedDate._dec();
    assertEquals(date1, mutatedDate);
    assertUnset.accept(unset._inc());
    assertUnset.accept(unset._dec());
  }

  @Test
  void testCopyOperations() {
    // Replace/copy logic
    var mutatedValue = Date._of("2023-01-01");
    mutatedValue._replace(date2);
    assertEquals(date2, mutatedValue);
    mutatedValue._replace(unset);
    assertUnset.accept(mutatedValue);

    // Merge logic
    mutatedValue = new Date();
    mutatedValue._merge(date1);
    assertEquals(date1, mutatedValue);
    mutatedValue._merge(unset);
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
    assertUnset.accept(unset.year());
    assertUnset.accept(unset.month());
    assertUnset.accept(unset.day());
    assertUnset.accept(unset.dayOfWeek());
    assertUnset.accept(unset.dayOfMonth());
    assertUnset.accept(unset.dayOfYear());

    // Test various dates
    assertEquals(Integer._of(2023), date1.year());
    assertEquals(Integer._of(1), date1.month());
    assertEquals(Integer._of(1), date1.day());
    assertEquals(Integer._of(7), date1.dayOfWeek()); // Sunday
    assertEquals(Integer._of(1), date1.dayOfMonth());
    assertEquals(Integer._of(1), date1.dayOfYear());

    assertEquals(Integer._of(2023), date3.year());
    assertEquals(Integer._of(12), date3.month());
    assertEquals(Integer._of(31), date3.day());
    assertEquals(Integer._of(365), date3.dayOfYear()); // Dec 31st in non-leap year

    assertEquals(Integer._of(2024), leapYear.year());
    assertEquals(Integer._of(2), leapYear.month());
    assertEquals(Integer._of(29), leapYear.day());
    assertEquals(Integer._of(60), leapYear.dayOfYear()); // Feb 29th is day 60 in leap year

    // Today and clear operations
    final var todaysDate = new Date().today();
    assertSet.accept(todaysDate);

    final var clearedDate = Date._of("2023-01-01");
    clearedDate.clear();
    assertUnset.accept(clearedDate);
  }

  @Test
  void testDateRanges() {
    // Test chronological ordering: epochDate < before2000 < date1 < date2 < date3 < leapYear < futureDate
    assertTrue.accept(epochDate._lt(before2000)); // 1970 < 1999
    assertTrue.accept(before2000._lt(date1)); // 1999 < 2023
    assertTrue.accept(date1._lt(date2));
    assertTrue.accept(date2._lt(date3));
    assertTrue.accept(date3._lt(leapYear)); // 2023 < 2024
    assertTrue.accept(leapYear._lt(futureDate)); // 2024 < 2025

    // Test comparison operators across ranges
    assertEquals(Integer._of(1), before2000._cmp(epochDate)); // 1999 > 1970
    assertEquals(Integer._of(-1), before2000._cmp(date1)); // 1999 < 2023
    assertEquals(Integer._of(1), futureDate._cmp(date1)); // 2025 > 2023

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
    assertFalse.accept(unset._isSet());
    assertTrue.accept(date1._isSet());

    // String operations
    assertUnset.accept(unset._string());
    assertEquals(String._of("2023-01-01"), date1._string());
    assertEquals("2023-01-01", date1.toString());
    assertEquals("", unset.toString());

    // JSON operations
    final var date1Json = date1._json();
    assertSet.accept(date1Json);

    final var date2Json = date2._json();
    assertSet.accept(date2Json);

    assertUnset.accept(unset._json());

    // Hash code
    assertUnset.accept(unset._hashcode());
    assertEquals(date1._hashcode(), date1._hashcode());
    assertNotEquals(date1._hashcode(), date2._hashcode());

    // Test _getAsJavaTemporalAccessor
    final var javaDate1 = date1._getAsJavaTemporalAccessor();
    assertNotNull(javaDate1);
    assertEquals(java.time.LocalDate.of(2023, 1, 1), javaDate1);

    final var javaDate2 = leapYear._getAsJavaTemporalAccessor();
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
    assertEquals(date1, date1._add(oneSecond)); // Adding 1 second doesn't change date
    assertEquals(date2, date1._add(oneDay)); // Adding 1 day via milliseconds
    assertUnset.accept(unset._add(oneSecond));
    assertUnset.accept(date1._add(new Millisecond()));

    // Test Date + Duration  
    assertEquals(date2, date1._add(oneDayDuration));
    assertEquals(Date._of("2023-01-03"), date1._add(twoDaysDuration));
    assertUnset.accept(unset._add(oneDayDuration));
    assertUnset.accept(date1._add(new Duration()));

    // Test Date - Millisecond
    assertEquals(date2, date2._sub(oneSecond)); // Subtracting 1 second doesn't change date
    assertEquals(date1, date2._sub(oneDay));
    assertUnset.accept(unset._sub(oneSecond));
    assertUnset.accept(date1._sub(new Millisecond()));

    // Test Date - Duration
    assertEquals(date1, date2._sub(oneDayDuration));
    assertUnset.accept(unset._sub(oneDayDuration));
    assertUnset.accept(date1._sub(new Duration()));

    // Test Date - Date → Duration
    assertEquals(oneDayDuration, date2._sub(date1));
    assertEquals(Duration._of(0), date1._sub(date1));
    assertEquals(Duration._of(-86400L), date1._sub(date2));
    assertUnset.accept(unset._sub(date1));
    assertUnset.accept(date1._sub(unset));
  }

  @Test
  void testAssignmentOperators() {
    // Test Date += Millisecond
    var testDate = Date._of("2023-01-01");
    testDate._addAss(oneDay);
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
    testDate._subAss(oneDay);
    assertEquals(date1, testDate);
    
    // Test Date -= Duration
    testDate = Date._of("2023-01-02");
    testDate._subAss(oneDayDuration);
    assertEquals(date1, testDate);

    // Test unset object behavior
    var unsetDate = new Date();
    unsetDate._addAss(oneDay);
    assertUnset.accept(unsetDate);
  }

  @Test
  void testPipeOperators() {
    // Test Date | Millisecond (pipes behave like += assignment)
    var testDate = Date._of("2023-01-01");
    testDate._pipe(oneDay);
    assertEquals(date2, testDate);

    // Test Date | Duration
    testDate = Date._of("2023-01-01");
    testDate._pipe(oneDayDuration);
    assertEquals(date2, testDate);

    // Test unset behavior
    var unsetDate = new Date();
    unsetDate._pipe(oneDay);
    assertUnset.accept(unsetDate);
  }

  @Test
  void testSpecialOperators() {
    // Test Date #^ promotion to DateTime
    final var promotedDateTime = date1._promote();
    assertSet.accept(promotedDateTime);
    assertEquals(Integer._of(2023), promotedDateTime.year());
    assertEquals(Integer._of(1), promotedDateTime.month());
    assertEquals(Integer._of(1), promotedDateTime.day());
    assertEquals(Integer._of(0), promotedDateTime.hour());
    assertEquals(Integer._of(0), promotedDateTime.minute());
    assertEquals(Integer._of(0), promotedDateTime.second());
    assertEquals(String._of("Z"), promotedDateTime.zone());
    
    final var unsetPromoted = unset._promote();
    assertUnset.accept(unsetPromoted);

    // Test Date #< prefix operator (day)
    assertEquals(Integer._of(1), date1._prefix());
    assertEquals(Integer._of(2), date2._prefix());
    assertEquals(Integer._of(31), date3._prefix());
    assertEquals(Integer._of(29), leapYear._prefix());
    assertUnset.accept(unset._prefix());

    // Test Date #> suffix operator (year)
    assertEquals(Integer._of(2023), date1._suffix());
    assertEquals(Integer._of(2023), date2._suffix());
    assertEquals(Integer._of(2023), date3._suffix());
    assertEquals(Integer._of(2024), leapYear._suffix());
    assertEquals(Integer._of(1970), epochDate._suffix());
    assertEquals(Integer._of(2025), futureDate._suffix());
    assertUnset.accept(unset._suffix());
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
    assertEquals(Date._of("2024-02-29"), feb28._add(oneDay));
    assertEquals(Date._of("2024-03-01"), leapYear._add(oneDay));
  }
}