package org.ek9.lang;

public class Common {

  final Boolean unsetBoolean = new Boolean();
  final Boolean trueBoolean = Boolean._of("true");
  final Boolean falseBoolean = Boolean._of("false");

  //Common Character constants
  final Character unsetCharacter = new Character();
  final Character cA = Character._of('A');
  final Character cB = Character._of('B');
  final Character cC = Character._of('C');
  final Character cLowerA = Character._of('a');
  final Character cSpace = Character._of(' ');
  final Character cZ = Character._of('Z');
  final Character cZero = Character._of('0');
  final Character cOne = Character._of('1');

  // ============ COMMON STRING CONSTANTS ============
  // Most frequently used String values across test files
  final String unsetString = new String();
  final String EMPTY_STRING = String._of("");
  final String STR_HELLO = String._of("hello");
  final String STR_LOCALHOST = String._of("localhost");
  final String STR_TEST = String._of("test");
  final String STR_TEST_MESSAGE = String._of("Test message");
  final String STR_NAME = String._of("name");
  final String STR_DEFAULT = String._of("default");
  final String STR_LETTER_A = String._of("a");
  final String STR_ABC = String._of("abc");

  // ============ TIMEZONE CONSTANTS ============
  final String TIMEZONE_UTC = String._of("UTC");
  final String TIMEZONE_UTC_Z = String._of("Z");
  final String TIMEZONE_NEW_YORK = String._of("America/New_York");
  final String TIMEZONE_LONDON = String._of("Europe/London");
  final String TIMEZONE_TOKYO = String._of("Asia/Tokyo");

  // ============ COMMON DATE CONSTANTS ============
  // Date strings used across multiple test classes
  final String DATE_1999_12_31 = String._of("1999-12-31");
  final String DATE_2023_01_01 = String._of("2023-01-01");
  final String DATE_2023_01_02 = String._of("2023-01-02");
  final String DATE_2023_12_31 = String._of("2023-12-31");
  final String DATE_2024_02_29 = String._of("2024-02-29");
  final String DATE_2025_06_15 = String._of("2025-06-15");

  // ============ COMMON DATETIME CONSTANTS ============
  // Most frequently used DateTime strings (6+ occurrences)
  final String DATETIME_BASE_UTC = String._of("2023-01-01T12:00:00Z");
  final String DATETIME_NEW_YEAR_EVE = String._of("2023-12-31T23:59:59Z");
  final String DATETIME_LEAP_FEB_29_NOON = String._of("2024-02-29T12:00:00Z");
  final String DATETIME_JUNE_15_AFTERNOON = String._of("2023-06-15T14:30:45Z");
  final String DATETIME_JUNE_15_NOON_UTC = String._of("2023-06-15T12:00:00Z");
  final String DATETIME_JUNE_15_NOON_NY = String._of("2023-06-15T12:00:00-04:00");

  final DateTime unsetDateTime = new DateTime();
  final DateTime epoch = DateTime._of("1970-01-01T00:00:00Z");
  final DateTime dateTime1 = DateTime._of(DATETIME_BASE_UTC.state);
  final DateTime dateTime2 = DateTime._of("2023-01-02T14:30:15Z");
  final DateTime dateTime3 = DateTime._of(DATETIME_NEW_YEAR_EVE.state);
  final DateTime leapYear = DateTime._of("2024-02-29T18:45:30Z");
  final DateTime futureDateTime = DateTime._of("2025-06-15T09:15:45Z");
  final DateTime beforeY2K = DateTime._of("1999-12-31T23:59:59Z");

  // Time zone test data
  final DateTime newYearUTC = DateTime._of("2023-01-01T00:00:00Z");
  final DateTime newYearTokyo = DateTime._of("2023-01-01T00:00:00+09:00");

  // ============ COMMON INTEGER CONSTANTS ============
  final Integer unsetInteger = Integer._of();
  final Integer INT_MINUS_2 = Integer._of(-2);
  final Integer INT_MINUS_1 = Integer._of(-1);
  final Integer INT_0 = Integer._of(0);
  final Integer INT_1 = Integer._of(1);
  final Integer INT_2 = Integer._of(2);
  final Integer INT_3 = Integer._of(3);
  final Integer INT_4 = Integer._of(4);
  final Integer INT_5 = Integer._of(5);
  final Integer INT_6 = Integer._of(6);
  final Integer INT_7 = Integer._of(7);
  final Integer INT_8 = Integer._of(8);
  final Integer INT_9 = Integer._of(9);
  final Integer INT_11 = Integer._of(11);
  final Integer INT_12 = Integer._of(12);
  final Integer INT_13 = Integer._of(13);
  final Integer INT_14 = Integer._of(14);
  final Integer INT_15 = Integer._of(15);
  final Integer INT_16 = Integer._of(16);
  final Integer INT_17 = Integer._of(17);
  final Integer INT_18 = Integer._of(18);
  final Integer INT_19 = Integer._of(19);
  final Integer INT_20 = Integer._of(20);
  final Integer INT_21 = Integer._of(21);
  final Integer INT_22 = Integer._of(22);
  final Integer INT_23 = Integer._of(23);
  final Integer INT_24 = Integer._of(24);
  final Integer INT_28 = Integer._of(28);
  final Integer INT_29 = Integer._of(29);
  final Integer INT_30 = Integer._of(30);
  final Integer INT_31 = Integer._of(31);
  final Integer INT_32 = Integer._of(32);
  final Integer INT_34 = Integer._of(34);
  final Integer INT_44 = Integer._of(44);
  final Integer INT_45 = Integer._of(45);
  final Integer INT_46 = Integer._of(46);
  final Integer INT_50 = Integer._of(50);
  final Integer INT_56 = Integer._of(56);
  final Integer INT_59 = Integer._of(59);
  final Integer INT_120 = Integer._of(120);
  final Integer INT_167 = Integer._of(167);
  final Integer INT_1970 = Integer._of(1970);
  final Integer INT_2023 = Integer._of(2023);
  final Integer INT_2024 = Integer._of(2024);
  final Integer INT_2025 = Integer._of(2025);
  final Integer INT_3600 = Integer._of(3600);
  final Integer INT_7200 = Integer._of(7200);
  final Integer INT_14400 = Integer._of(14400);
  final Integer INT_86399 = Integer._of(86399);
  final Integer INT_42 = Integer._of(42);
  final Integer INT_86400 = Integer._of(86400);
  final Integer INT_100 = Integer._of(100);
  final Integer INT_604800 = Integer._of(604800);
  final Integer INT_1000000000 = Integer._of(1000000000);
  final Integer INT_8080 = Integer._of(8080);
  final Integer INT_12345 = Integer._of(12345);

  final Integer INT_25 = Integer._of(25);
  final Integer INT_26 = Integer._of(26);
  final Integer INT_55 = Integer._of(55);
  final Integer INT_60 = Integer._of(60);
  final Integer INT_61 = Integer._of(61);
  final Integer INT_90 = Integer._of(90);
  final Integer INT_166 = Integer._of(166);
  final Integer INT_365 = Integer._of(365);
  final Integer INT_366 = Integer._of(366);


  // ============ COMMON FLOAT CONSTANTS ============
  final Float unsetFloat = new Float();
  final Float FLOAT_MINUS_2 = Float._of(-2);
  final Float FLOAT_MINUS_1 = Float._of(-1);
  final Float FLOAT_0 = Float._of(0);
  final Float FLOAT_0_5 = Float._of(0.5);
  final Float FLOAT_1 = Float._of(1);
  final Float FLOAT_2 = Float._of(2);
  final Float FLOAT_2_0 = Float._of(2.0);
  final Float FLOAT_3 = Float._of(3);
  final Float FLOAT_4 = Float._of(4);
  final Float FLOAT_4_5 = Float._of(4.5);
  final Float FLOAT_25 = Float._of(25.0);
  final Float FLOAT_50 = Float._of(50.0);
  final Float FLOAT_75 = Float._of(75.0);
  // ============ TIME/DURATION CONSTANTS ============
  // Millisecond Constants
  final Millisecond oneSecondMs = Millisecond._of(1000);
  final Millisecond oneMinuteMs = Millisecond._of(60000);
  final Millisecond oneHourMs = Millisecond._of(3600000);
  final Millisecond oneDayMs = Millisecond._of(86400000L);

  // Duration Constants
  final Duration oneHourDuration = Duration._of(3600L);
  final Duration oneDayDuration = Duration._of(86400L);
  final Duration twoDaysDuration = Duration._of(172800L);
  final Duration oneWeekDuration = Duration._of(604800L);

  // ============ COMMON JSON CONSTANTS ============
  final JSON JSON_42 = JSON._of("42");
  final JSON JSON_TRUE = JSON._of("true");
  final JSON JSON_NULL = JSON._of("null");

  // ============ COMMON Colour CONSTANTS ============
  final Colour unsetColour = new Colour();
  final Colour testColour = Colour._of("FF186276");
  final Colour modifiedColour = Colour._of("B7106236");
  
  // Primary RGB colors
  final Colour redRgb = Colour._of("FF0000");
  final Colour greenRgb = Colour._of("00FF00");
  final Colour blueRgb = Colour._of("0000FF");
  final Colour blackRgb = Colour._of("000000");
  final Colour whiteRgb = Colour._of("FFFFFF");
  final Colour grayRgb = Colour._of("808080");
  
  // ARGB colors with alpha channel
  final Colour redArgb = Colour._of("FFFF0000");
  final Colour redHalfAlpha = Colour._of("80FF0000");
  final Colour greenWithAlpha = Colour._of("2000FF00");
  final Colour transparentBlack = Colour._of("00000000");
  final Colour transparentWhite = Colour._of("FFFFFFFF");
  
  // Composite colors (results of arithmetic operations)
  final Colour purpleRgb = Colour._of("FF00FF");    // red + blue
  final Colour yellowRgb = Colour._of("FFFF00");    // red + green  
  final Colour cyanRgb = Colour._of("00FFFF");      // green + blue
  
  // ============ COMMON DIMENSION CONSTANTS ============
  final Dimension unsetDimension = new Dimension();

  // Basic pixel dimensions (most commonly used)
  final Dimension DIMENSION_0PX = Dimension._of("0px");
  final Dimension DIMENSION_1PX = Dimension._of("1px");
  final Dimension DIMENSION_2PX = Dimension._of("2px");
  final Dimension DIMENSION_3PX = Dimension._of("3px");
  final Dimension DIMENSION_4PX = Dimension._of("4px");
  final Dimension DIMENSION_8PX = Dimension._of("8px");
  final Dimension DIMENSION_50PX = Dimension._of("50px");
  final Dimension DIMENSION_100PX = Dimension._of("100px");
  final Dimension DIMENSION_150PX = Dimension._of("150px");
  final Dimension DIMENSION_200PX = Dimension._of("200px");
  final Dimension DIMENSION_1000PX = Dimension._of("1000px");

  // Different units for testing unit compatibility
  final Dimension DIMENSION_50M = Dimension._of("50m");
  final Dimension DIMENSION_100M = Dimension._of("100m");

  // Negative dimensions for testing
  final Dimension DIMENSION_NEG_2PX = Dimension._of("-2px");
  final Dimension DIMENSION_NEG_100PX = Dimension._of("-100px");
  final Dimension DIMENSION_NEG_1000PX = Dimension._of("-1000px");

  // ============ TEST DATA CONSTANTS ============
  // Bit patterns for color testing (Java strings for Bits._of)
  final java.lang.String BITS_RGB_RED = "111111110000000000000000";
  final java.lang.String BITS_ARGB_RED = "11111111111111110000000000000000";
  final java.lang.String BITS_MIXED_COLOR = "11111111000000001111111100000000";
  
  // Invalid hex test strings (Java strings for Colour._of)
  final java.lang.String HEX_INVALID_CHARS = "#GGGGGG";
  final java.lang.String HEX_TOO_SHORT = "#F0";
  final java.lang.String HEX_TOO_LONG = "#FF0000000";

  protected final AssertUnset assertUnset = new AssertUnset();
  protected final AssertSet assertSet = new AssertSet();
  protected final AssertTrue assertTrue = new AssertTrue();
  protected final AssertFalse assertFalse = new AssertFalse();
}
