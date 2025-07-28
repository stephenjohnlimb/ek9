package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LocaleTest extends Common {

  // Test data setup matching documentation examples
  final Locale unsetLocale = new Locale();

  // Test data from builtInTypes.html documentation
  final Integer i1 = Integer._of(-92208);
  final Integer i2 = Integer._of(675807);
  final Float f1 = Float._of(-4.9E-22);
  final Float f2 = Float._of(-1.797693134862395E12);
  final Money tenPounds = Money._of("10#GBP");
  final Money thirtyDollars = Money._of("30.89#USD");
  final Money chileanCurrency = Money._of("6798.9288#CLF");


  @Test
  void testConstruction() {
    // Default constructor creates unset
    final var defaultConstructor = new Locale();
    assertUnset.accept(defaultConstructor);

    // String constructor with various formats
    final var fromString1 = new Locale(String._of("en_GB"));
    assertSet.accept(fromString1);
    assertEquals("en", fromString1._prefix()._string().state);
    assertEquals("GB", fromString1._suffix()._string().state);

    final var fromString2 = new Locale(String._of("en-US"));
    assertSet.accept(fromString2);
    assertEquals("en", fromString2._prefix()._string().state);
    assertEquals("US", fromString2._suffix()._string().state);

    // Two-parameter constructor
    final var fromTwoParams = new Locale(String._of("de"), String._of("DE"));
    assertSet.accept(fromTwoParams);
    assertEquals("de", fromTwoParams._prefix()._string().state);
    assertEquals("DE", fromTwoParams._suffix()._string().state);

    // Copy constructor
    final var testEnGB = new Locale(String._of("en_GB"));
    final var fromCopy = new Locale(testEnGB);
    assertSet.accept(fromCopy);
    assertEquals(testEnGB, fromCopy);

    // Null handling
    final var nullConstructor1 = new Locale(new String());
    assertUnset.accept(nullConstructor1);

    final var nullConstructor2 = new Locale(new Locale());
    assertUnset.accept(nullConstructor2);

    final var nullConstructor3 = new Locale(new String(), String._of("GB"));
    assertUnset.accept(nullConstructor3);

    final var nullConstructor4 = new Locale(String._of("en"), new String());
    assertUnset.accept(nullConstructor4);
  }

  @Test
  void testEquality() {
    final var enGB = new Locale(String._of("en_GB"));
    assertNotNull(enGB);
    final var enUS = new Locale(String._of("en-US"));
    assertNotNull(enUS);
    final var anotherEnGB = Locale._of("en_GB");
    assertNotNull(anotherEnGB);

    // Set values equality
    assertTrue.accept(enGB._eq(anotherEnGB));
    assertFalse.accept(enGB._eq(enUS));

    // Unset propagation
    assertUnset.accept(unsetLocale._eq(enGB));
    assertUnset.accept(enGB._eq(unsetLocale));
    assertUnset.accept(unsetLocale._eq(unsetLocale));

    // Not equal
    assertFalse.accept(enGB._neq(anotherEnGB));
    assertTrue.accept(enGB._neq(enUS));

    // Unset propagation for neq
    assertUnset.accept(unsetLocale._neq(enGB));
    assertUnset.accept(enGB._neq(unsetLocale));
    assertUnset.accept(unsetLocale._neq(unsetLocale));
  }

  @Test
  void testComparison() {
    final var enGB = new Locale(String._of("en_GB"));
    final var enUS = new Locale(String._of("en-US"));
    final var en = new Locale(String._of("en"));
    final var de = new Locale(String._of("de"));

    assertEquals(Integer._of(0), en._cmp(en));
    assertTrue(de._cmp(en).state < 0);
    assertTrue(en._cmp(de).state > 0);

    assertEquals(Integer._of(0), en._fuzzy(en));
    assertTrue(de._fuzzy(en).state < 0);
    assertTrue(en._fuzzy(de).state > 0);


    // Normal comparison
    assertEquals(Integer._of(0), enGB._cmp(enGB));
    assertTrue(enGB._cmp(enUS).state < 0); // GB < US lexicographically (difference between G and U)
    assertTrue(enUS._cmp(enGB).state > 0);

    //Now for fuzzy match, both are 'en' so that's a fuzzy match!
    assertEquals(Integer._of(0), enGB._fuzzy(enGB));
    assertEquals(0, enGB._fuzzy(enUS).state);
    assertEquals(0, enUS._fuzzy(enGB).state);

    // Unset propagation
    assertUnset.accept(unsetLocale._cmp(enGB));
    assertUnset.accept(enGB._cmp(unsetLocale));
    assertUnset.accept(unsetLocale._cmp(unsetLocale));

    assertUnset.accept(unsetLocale._fuzzy(enGB));
    assertUnset.accept(enGB._fuzzy(unsetLocale));
    assertUnset.accept(unsetLocale._fuzzy(unsetLocale));

    // Less than
    assertTrue.accept(enGB._lt(enUS));
    assertFalse.accept(enUS._lt(enGB));
    assertFalse.accept(enGB._lt(enGB));

    // Less than or equal
    assertTrue.accept(enGB._lteq(enUS));
    assertTrue.accept(enGB._lteq(enGB));
    assertFalse.accept(enUS._lteq(enGB));

    // Greater than
    assertTrue.accept(enUS._gt(enGB));
    assertFalse.accept(enGB._gt(enUS));
    assertFalse.accept(enGB._gt(enGB));

    // Greater than or equal
    assertTrue.accept(enUS._gteq(enGB));
    assertTrue.accept(enGB._gteq(enGB));
    assertFalse.accept(enGB._gteq(enUS));

    // Unset propagation for comparison operators
    assertUnset.accept(unsetLocale._lt(enGB));
    assertUnset.accept(enGB._lt(unsetLocale));
    assertUnset.accept(unsetLocale._lteq(enGB));
    assertUnset.accept(enGB._lteq(unsetLocale));
    assertUnset.accept(unsetLocale._gt(enGB));
    assertUnset.accept(enGB._gt(unsetLocale));
    assertUnset.accept(unsetLocale._gteq(enGB));
    assertUnset.accept(enGB._gteq(unsetLocale));
  }

  @Test
  void testIsSet() {
    assertNotNull(unsetLocale);
    assertFalse.accept(unsetLocale._isSet());

    final var enGB = new Locale(String._of("en_GB"));
    assertNotNull(enGB);
    assertTrue.accept(enGB._isSet());

    final var enUS = new Locale(String._of("en-US"));
    assertNotNull(enUS);
    assertTrue.accept(enUS._isSet());
  }

  @Test
  void testPrefixSuffixOperators() {
    final var enGB = new Locale(String._of("en_GB"));
    final var enUS = new Locale(String._of("en-US"));
    final var deutsch = new Locale(String._of("de_DE"));
    final var skSK = new Locale(String._of("sk"), String._of("SK"));

    // Test prefix operator (#<) - should return language code
    assertEquals("en", enGB._prefix()._string().state);
    assertEquals("en", enUS._prefix()._string().state);
    assertEquals("de", deutsch._prefix()._string().state);
    assertEquals("sk", skSK._prefix()._string().state);

    // Test suffix operator (#>) - should return country code
    assertEquals("GB", enGB._suffix()._string().state);
    assertEquals("US", enUS._suffix()._string().state);
    assertEquals("DE", deutsch._suffix()._string().state);
    assertEquals("SK", skSK._suffix()._string().state);

    // Test with unset locale
    assertUnset.accept(unsetLocale._prefix());
    assertUnset.accept(unsetLocale._suffix());

    // Test that language() and country() methods delegate to operators
    assertEquals(enGB._prefix()._string().state, enGB.language()._string().state);
    assertEquals(enGB._suffix()._string().state, enGB.country()._string().state);
  }

  @Test
  void testUtilityMethods() {
    final var enGB = new Locale(String._of("en_GB"));
    final var deutsch = new Locale(String._of("de_DE"));

    // Test language() method
    assertEquals("en", enGB.language()._string().state);
    assertEquals("de", deutsch.language()._string().state);
    assertUnset.accept(unsetLocale.language());

    // Test country() method
    assertEquals("GB", enGB.country()._string().state);
    assertEquals("DE", deutsch.country()._string().state);
    assertUnset.accept(unsetLocale.country());
  }

  @Test
  void testIntegerFormatting() {
    final var enGB = new Locale(String._of("en_GB"));
    final var deutsch = new Locale(String._of("de_DE"));
    final var skSK = new Locale(String._of("sk"), String._of("SK"));

    // Test with exact values from documentation

    // English GB formatting
    assertEquals("-92,208", enGB.format(i1)._string().state);
    assertEquals("675,807", enGB.format(i2)._string().state);

    // German formatting
    assertEquals("-92.208", deutsch.format(i1)._string().state);
    assertEquals("675.807", deutsch.format(i2)._string().state);

    // Slovak formatting (uses non-breaking space Unicode 160)
    assertEquals("-92\u00A0208", skSK.format(i1)._string().state);
    assertEquals("675\u00A0807", skSK.format(i2)._string().state);

    // Unset handling
    assertUnset.accept(unsetLocale.format(i1));
    assertUnset.accept(enGB.format(new Integer()));
  }

  @Test
  void testFloatFormatting() {
    final var enGB = new Locale(String._of("en_GB"));
    final var deutsch = new Locale(String._of("de_DE"));
    final var skSK = new Locale(String._of("sk"), String._of("SK"));

    // Test basic float formatting
    assertEquals("-0.00000000000000000000049", enGB.format(f1)._string().state);
    assertEquals("-1,797,693,134,862.395", enGB.format(f2)._string().state);

    // Test with precision control
    assertEquals("-0.0000000000000000000005", enGB.format(f1, Integer._of(22))._string().state);
    assertEquals("-1,797,693,134,862.4", enGB.format(f2, Integer._of(1))._string().state);

    // German formatting
    assertEquals("-0,00000000000000000000049", deutsch.format(f1)._string().state);
    assertEquals("-1.797.693.134.862,395", deutsch.format(f2)._string().state);

    // Slovak formatting (uses non-breaking space Unicode 160)
    assertEquals("-0,00000000000000000000049", skSK.format(f1)._string().state);
    assertEquals("-1\u00A0797\u00A0693\u00A0134\u00A0862,395", skSK.format(f2)._string().state);

    // Unset handling
    assertUnset.accept(unsetLocale.format(f1));
    assertUnset.accept(enGB.format(new Float()));
    assertUnset.accept(enGB.format(f1, new Integer()));
  }

  @Test
  void testMoneyFormatting() {
    final var enGB = new Locale(String._of("en_GB"));
    final var deutsch = new Locale(String._of("de_DE"));
    final var skSK = new Locale(String._of("sk"), String._of("SK"));

    // Full format (default)
    assertEquals("£10.00", enGB.format(tenPounds)._string().state);
    assertEquals("US$30.89", enGB.format(thirtyDollars)._string().state);
    assertEquals("CLF6,798.9288", enGB.format(chileanCurrency)._string().state);

    // German formatting (uses non-breaking space Unicode 160)
    assertEquals("10,00\u00A0£", deutsch.format(tenPounds)._string().state);
    assertEquals("30,89\u00A0$", deutsch.format(thirtyDollars)._string().state);
    assertEquals("6.798,9288\u00A0CLF", deutsch.format(chileanCurrency)._string().state);

    // Slovak formatting (uses non-breaking space Unicode 160)
    assertEquals("10,00\u00A0GBP", skSK.format(tenPounds)._string().state);
    assertEquals("30,89\u00A0USD", skSK.format(thirtyDollars)._string().state);
    assertEquals("6\u00A0798,9288\u00A0CLF", skSK.format(chileanCurrency)._string().state);

    // Custom format with boolean flags
    assertEquals("CLF6,799", enGB.format(chileanCurrency, Boolean._of(true), Boolean._of(false))._string().state);
    assertEquals("6,799", enGB.format(chileanCurrency, Boolean._of(false), Boolean._of(false))._string().state);

    // Unset handling
    assertUnset.accept(unsetLocale.format(tenPounds));
    assertUnset.accept(enGB.format(new Money()));
    assertUnset.accept(enGB.format(tenPounds, new Boolean(), Boolean._of(true)));
    assertUnset.accept(enGB.format(tenPounds, Boolean._of(true), new Boolean()));
    assertUnset.accept(enGB.format(new Money(), Boolean._of(true), Boolean._of(false)));
  }

  @Test
  void testShortMediumLongFullMoneyFormats() {
    final var enGB = new Locale(String._of("en_GB"));

    // Short format (no symbol, no fractional part)
    assertEquals("10", enGB.shortFormat(tenPounds)._string().state);
    assertEquals("31", enGB.shortFormat(thirtyDollars)._string().state);

    // Medium format (symbol, no fractional part)
    assertEquals("£10", enGB.mediumFormat(tenPounds)._string().state);
    assertEquals("US$31", enGB.mediumFormat(thirtyDollars)._string().state);

    // Long format (no symbol, with fractional part)
    assertEquals("10.00", enGB.longFormat(tenPounds)._string().state);
    assertEquals("30.89", enGB.longFormat(thirtyDollars)._string().state);
    assertEquals("6,798.9288", enGB.longFormat(chileanCurrency)._string().state);

    // Full format (symbol, with fractional part) - same as default format
    assertEquals("£10.00", enGB.fullFormat(tenPounds)._string().state);
    assertEquals("US$30.89", enGB.fullFormat(thirtyDollars)._string().state);
    assertEquals("CLF6,798.9288", enGB.fullFormat(chileanCurrency)._string().state);

    // Unset handling
    assertUnset.accept(unsetLocale.shortFormat(tenPounds));
    assertUnset.accept(unsetLocale.mediumFormat(tenPounds));
    assertUnset.accept(unsetLocale.longFormat(tenPounds));
    assertUnset.accept(unsetLocale.fullFormat(tenPounds));
  }

  @Test
  void testBooleanFormatting() {
    final var enGB = new Locale(String._of("en_GB"));

    assertEquals("true", enGB.format(trueBoolean)._string().state);
    assertEquals("false", enGB.format(falseBoolean)._string().state);

    // Unset handling
    assertUnset.accept(unsetLocale.format(trueBoolean));
    assertUnset.accept(enGB.format(new Boolean()));
  }

  @Test
  void testAsString() {
    final var enGB = new Locale(String._of("en_GB"));
    final var enUS = new Locale(String._of("en-US"));
    final var deutsch = new Locale(String._of("de_DE"));
    final var skSK = new Locale(String._of("sk"), String._of("SK"));

    assertEquals("en_GB", enGB._string().state);
    assertEquals("en_US", enUS._string().state);
    assertEquals("de_DE", deutsch._string().state);
    assertEquals("sk_SK", skSK._string().state);

    assertUnset.accept(unsetLocale._string());

    // JSON operations
    final var enGBJson = enGB._json();
    assertSet.accept(enGBJson);

    final var deutschJson = deutsch._json();
    assertSet.accept(deutschJson);

    assertUnset.accept(unsetLocale._json());
  }

  @Test
  void testHashCode() {
    final var enGB = new Locale(String._of("en_GB"));
    final var enUS = new Locale(String._of("en-US"));
    final var deutsch = new Locale(String._of("de_DE"));

    assertUnset.accept(unsetLocale._hashcode());
    assertEquals(enGB._hashcode(), enGB._hashcode());
    assertNotEquals(enGB._hashcode(), enUS._hashcode());
    assertNotEquals(enGB._hashcode(), deutsch._hashcode());
  }

  @Test
  void testAssignmentOperators() {
    final var enGB = new Locale(String._of("en_GB"));
    final var enUS = new Locale(String._of("en-US"));
    final var deutsch = new Locale(String._of("de_DE"));
    final var skSK = new Locale(String._of("sk"), String._of("SK"));

    // Test _copy operator
    final var target = new Locale();
    target._copy(enGB);
    assertSet.accept(target);
    assertEquals(enGB, target);

    // Test _merge operator
    final var mergeTarget = new Locale();
    mergeTarget._merge(enUS);
    assertSet.accept(mergeTarget);
    assertEquals(enUS, mergeTarget);

    // Test _replace operator
    final var replaceTarget = new Locale(enGB);
    replaceTarget._replace(deutsch);
    assertSet.accept(replaceTarget);
    assertEquals(deutsch, replaceTarget);

    // Test _pipe operator
    final var pipeTarget = new Locale();
    pipeTarget._pipe(skSK);
    assertSet.accept(pipeTarget);
    assertEquals(skSK, pipeTarget);

    // Null handling
    target._copy(null);
    assertUnset.accept(target);

    final var unsetTarget = new Locale();
    unsetTarget._copy(unsetLocale);
    assertUnset.accept(unsetTarget);
  }

  @Test
  void testFactoryMethods() {
    // Test _of() variants
    final var empty = Locale._of();
    assertUnset.accept(empty);

    final var fromString = Locale._of("fr_FR");
    assertSet.accept(fromString);
    assertEquals("fr", fromString._prefix()._string().state);
    assertEquals("FR", fromString._suffix()._string().state);

    final var fromTwoStrings = Locale._of("es", "ES");
    assertSet.accept(fromTwoStrings);
    assertEquals("es", fromTwoStrings._prefix()._string().state);
    assertEquals("ES", fromTwoStrings._suffix()._string().state);
  }

  @Test
  void testEdgeCases() {
    // Test with malformed locale strings
    final var invalid1 = new Locale(String._of("invalid"));
    assertUnset.accept(invalid1);

    final var invalid2 = new Locale(String._of("en_"));
    assertUnset.accept(invalid2);

    final var invalid3 = new Locale(String._of("_GB"));
    assertUnset.accept(invalid3);

    // Test with null inputs
    final var nullInput = new Locale((String) null);
    assertUnset.accept(nullInput);

    // Test boundary conditions
    final var justLanguage = new Locale(String._of("en"));
    assertSet.accept(justLanguage);
    assertEquals("en", justLanguage._prefix()._string().state);
    assertEquals("", justLanguage._suffix()._string().state);
  }

  @Test
  void testTemporalFormatting() {
    final var enGB = new Locale(String._of("en_GB"));
    final var deutsch = new Locale(String._of("de_DE"));

    // Create test temporal items
    final var testDate = Date._of("2020-10-03");
    final var testTime = Time._of("12:00:01");
    final var testDateTime = DateTime._of("2020-10-03T12:00:00Z");

    // Test date formatting - UK formats
    final var ukDateFull = enGB.format(testDate);
    assertSet.accept(ukDateFull);
    assertEquals("Saturday 3 October 2020", ukDateFull._string().state);

    final var ukDateShort = enGB.shortFormat(testDate);
    assertSet.accept(ukDateShort);
    assertEquals("03/10/2020", ukDateShort._string().state);

    final var ukDateMedium = enGB.mediumFormat(testDate);
    assertSet.accept(ukDateMedium);
    assertEquals("3 Oct 2020", ukDateMedium._string().state);

    final var ukDateLong = enGB.longFormat(testDate);
    assertSet.accept(ukDateLong);
    assertEquals("3 October 2020", ukDateLong._string().state);

    assertEquals("12:00:01", enGB.longFormat(testTime)._string().state);

    final var ukDateFull2 = enGB.fullFormat(testDate);
    assertSet.accept(ukDateFull2);
    assertEquals("Saturday 3 October 2020", ukDateFull2._string().state);

    assertEquals("Saturday 3 October 2020, 12:00:00 Z", enGB.fullFormat(testDateTime)._string().state);
    assertEquals("12:00:01", enGB.fullFormat(testTime)._string().state);

    // Test date formatting - German formats
    final var germanDateFull = deutsch.format(testDate);
    assertSet.accept(germanDateFull);
    assertEquals("Samstag, 3. Oktober 2020", germanDateFull._string().state);

    final var germanDateShort = deutsch.shortFormat(testDate);
    assertSet.accept(germanDateShort);
    assertEquals("03.10.20", germanDateShort._string().state);

    // Test time formatting - UK formats
    final var ukTimeFull = enGB.format(testTime);
    assertSet.accept(ukTimeFull);
    assertEquals("12:00:01", ukTimeFull._string().state);

    final var ukTimeShort = enGB.shortFormat(testTime);
    assertSet.accept(ukTimeShort);
    assertEquals("12:00", ukTimeShort._string().state);

    final var ukTimeMedium = enGB.mediumFormat(testTime);
    assertSet.accept(ukTimeMedium);
    assertEquals("12:00:01", ukTimeMedium._string().state);

    // Test time formatting - German formats
    final var germanTimeFull = deutsch.format(testTime);
    assertSet.accept(germanTimeFull);
    assertEquals("12:00:01", germanTimeFull._string().state);

    final var germanTimeShort = deutsch.shortFormat(testTime);
    assertSet.accept(germanTimeShort);
    assertEquals("12:00", germanTimeShort._string().state);

    // Test datetime formatting - UK formats
    final var ukDateTimeFull = enGB.format(testDateTime);
    assertSet.accept(ukDateTimeFull);
    assertEquals("Saturday 3 October 2020, 12:00:00 Z", ukDateTimeFull._string().state);

    final var ukDateTimeShort = enGB.shortFormat(testDateTime);
    assertSet.accept(ukDateTimeShort);
    assertEquals("03/10/2020, 12:00", ukDateTimeShort._string().state);

    final var ukDateTimeMedium = enGB.mediumFormat(testDateTime);
    assertSet.accept(ukDateTimeMedium);
    assertEquals("3 Oct 2020, 12:00:00", ukDateTimeMedium._string().state);

    final var ukDateTimeLong = enGB.longFormat(testDateTime);
    assertSet.accept(ukDateTimeLong);
    assertEquals("3 October 2020, 12:00:00 Z", ukDateTimeLong._string().state);

    // Test datetime formatting - German formats
    final var germanDateTimeFull = deutsch.format(testDateTime);
    assertSet.accept(germanDateTimeFull);
    assertEquals("Samstag, 3. Oktober 2020, 12:00:00 Z", germanDateTimeFull._string().state);

    final var germanDateTimeShort = deutsch.shortFormat(testDateTime);
    assertSet.accept(germanDateTimeShort);
    assertEquals("03.10.20, 12:00", germanDateTimeShort._string().state);

    // Test unset handling
    assertUnset.accept(unsetLocale.format(testDate));
    assertUnset.accept(enGB.format(new Date()));
    assertUnset.accept(unsetLocale.format(testTime));
    assertUnset.accept(enGB.format(new Time()));
    assertUnset.accept(unsetLocale.format(testDateTime));
    assertUnset.accept(enGB.format(new DateTime()));
  }

  @Test
  void testDimensionFormatting() {
    final var enGB = new Locale(String._of("en_GB"));
    final var testDimension = Dimension._of("100m");

    // Test basic dimension formatting

    final var simpleFormat = enGB.format(testDimension);
    assertSet.accept(simpleFormat);
    assertEquals(String._of("100"), simpleFormat);

    // Test dimension formatting with precision
    final var precisionFormat = enGB.format(testDimension, Integer._of(2));
    assertSet.accept(precisionFormat);
    assertEquals(String._of("100.00"), precisionFormat);

    // Test unset handling
    assertUnset.accept(unsetLocale.format(testDimension));
    assertUnset.accept(enGB.format(new Dimension()));
    assertUnset.accept(enGB.format(testDimension, new Integer()));
  }

  @Test
  void testDayOfWeekFormatting() {
    final var enGB = new Locale(String._of("en_GB"));
    final var deutsch = new Locale(String._of("de_DE"));
    final var testDate = Date._of("2020-10-03"); // This is a Saturday

    // Test dayOfWeek method
    final var ukDayOfWeek = enGB.dayOfWeek(testDate);
    assertSet.accept(ukDayOfWeek);
    assertEquals(String._of("Saturday"), ukDayOfWeek);

    final var deutschDayOfWeek = deutsch.dayOfWeek(testDate);
    assertSet.accept(deutschDayOfWeek);
    assertEquals(String._of("Samstag"), deutschDayOfWeek);

    // Test unset handling
    assertUnset.accept(unsetLocale.dayOfWeek(testDate));
    assertUnset.accept(enGB.dayOfWeek(new Date()));
  }
}