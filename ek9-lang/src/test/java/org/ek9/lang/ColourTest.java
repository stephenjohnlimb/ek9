package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for Colour class.
 * Tests color parsing, HSL conversions, color arithmetic, and format conversions.
 */
class ColourTest extends Common {

  // Test data - key colors from JustColour.ek9
  private final Colour unset = new Colour();
  private final Colour testColour = Colour._of("FF186276");
  private final Colour modifiedColour = Colour._of("B7106236");
  private final Colour redRgb = Colour._of("FF0000");
  private final Colour greenRgb = Colour._of("00FF00");
  private final Colour blueRgb = Colour._of("0000FF");
  private final Colour redArgb = Colour._of("FFFF0000");
  @Test
  void testConstruction() {
    // Default constructor creates unset
    final var defaultConstructor = new Colour();
    assertUnset.accept(defaultConstructor);

    // Copy constructor
    final var copyConstructor = new Colour(testColour);
    assertSet.accept(copyConstructor);
    assertTrue.accept(copyConstructor._eq(testColour));

    // String constructor with valid hex
    final var stringConstructor = new Colour(String._of("#FF186276"));
    assertSet.accept(stringConstructor);
    assertEquals("#FF186276", stringConstructor._string().state);

    // String constructor with 3-digit hex
    final var shortHex = new Colour(String._of("#F00"));
    assertSet.accept(shortHex);
    assertEquals("#FF0000", shortHex._string().state);

    // String constructor with 6-digit hex (no alpha)
    final var rgbHex = new Colour(String._of("#FF0000"));
    assertSet.accept(rgbHex);
    assertEquals("#FF0000", rgbHex._string().state);

    // String constructor with invalid format
    final var invalidHex = new Colour(String._of("invalid"));
    assertUnset.accept(invalidHex);

    // Bits constructor with 24-bit RGB
    final var bitsRgb = new Colour(Bits._of("111111110000000000000000"));
    assertSet.accept(bitsRgb);
    assertEquals("#FF0000", bitsRgb._string().state);

    // Bits constructor with 32-bit ARGB
    final var bitsArgb = new Colour(Bits._of("11111111111111110000000000000000"));
    assertSet.accept(bitsArgb);
    assertEquals("#FFFF0000", bitsArgb._string().state);

    // Null handling
    final var nullConstructor = new Colour((Colour) null);
    assertUnset.accept(nullConstructor);

    final var nullStringConstructor = new Colour((String) null);
    assertUnset.accept(nullStringConstructor);
  }

  @Test
  void testFactoryMethods() {
    // Empty factory
    final var empty = Colour._of();
    assertUnset.accept(empty);

    // String factory
    final var fromString = Colour._of("#FF186276");
    assertSet.accept(fromString);
    assertEquals("#FF186276", fromString._string().state);

    // HSL factory (RGB)
    final var fromHsl = Colour._of(0, 100, 50);
    assertSet.accept(fromHsl);
    assertEquals("#FF0000", fromHsl._string().state);

    // HSL factory with alpha
    final var fromHslAlpha = Colour._of(128, 0, 100, 50);
    assertSet.accept(fromHslAlpha);
    assertEquals("#80FF0000", fromHslAlpha._string().state);
  }

  @Test
  void testStringParsing() {
    // Valid hex formats
    final var withHash = Colour._of("#FF186276");
    assertSet.accept(withHash);
    assertEquals("#FF186276", withHash._string().state);

    final var withoutHash = Colour._of("FF186276");
    assertSet.accept(withoutHash);
    assertEquals("#FF186276", withoutHash._string().state);

    // 3-digit expansion
    final var shortRed = Colour._of("#F00");
    assertSet.accept(shortRed);
    assertEquals("#FF0000", shortRed._string().state);

    // 6-digit RGB
    final var rgb = Colour._of("#FF0000");
    assertSet.accept(rgb);
    assertEquals("#FF0000", rgb._string().state);

    // 8-digit ARGB
    final var argb = Colour._of("#80FF0000");
    assertSet.accept(argb);
    assertEquals("#80FF0000", argb._string().state);

    // Invalid formats
    final var tooShort = Colour._of("#F0");
    assertUnset.accept(tooShort);

    final var tooLong = Colour._of("#FF0000000");
    assertUnset.accept(tooLong);

    final var invalidChars = Colour._of("#GGGGGG");
    assertUnset.accept(invalidChars);
  }

  @Test
  void testStateManagement() {
    // Test _isSet operator
    assertFalse.accept(unset._isSet());
    assertTrue.accept(testColour._isSet());
    assertTrue.accept(redRgb._isSet());
  }

  @Test
  void testEquality() {
    // Same color equality
    final var red1 = Colour._of("#FF0000");
    assertNotNull(red1);
    final var red2 = Colour._of("FF0000");
    assertTrue.accept(red1._eq(red2));
    assertFalse.accept(red1._neq(red2));

    // Different colors
    assertFalse.accept(redRgb._eq(blueRgb));
    assertTrue.accept(redRgb._neq(blueRgb));

    // Unset propagation
    assertUnset.accept(unset._eq(redRgb));
    assertUnset.accept(redRgb._eq(unset));
    assertUnset.accept(unset._neq(redRgb));
    assertUnset.accept(redRgb._neq(unset));
  }

  @Test
  void testComparison() {
    // Color comparison based on numeric value
    final var black = Colour._of("#000000");
    final var white = Colour._of("#FFFFFF");
    final var red = Colour._of("#FF0000");

    assertTrue.accept(black._lt(red));
    assertTrue.accept(red._lt(white));
    assertFalse.accept(white._lt(black));

    assertTrue.accept(black._lteq(red));
    assertTrue.accept(red._lteq(red));
    assertFalse.accept(white._lteq(black));

    assertTrue.accept(white._gt(red));
    assertTrue.accept(red._gt(black));
    assertFalse.accept(black._gt(white));

    assertTrue.accept(white._gteq(red));
    assertTrue.accept(red._gteq(red));
    assertFalse.accept(black._gteq(white));

    // Comparison operator
    assertTrue(black._cmp(red).state < 0);
    assertTrue(white._cmp(red).state > 0);
    assertEquals(0, red._cmp(red).state);

    //Should have unset result.
    assertUnset.accept(white._cmp(Any._new()));
    //This will go via Any and back up to Colour.
    assertUnset.accept(white._lt(Any._new()));

    // Unset propagation
    assertUnset.accept(unset._lt(red));
    assertUnset.accept(red._lt(unset));
    assertUnset.accept(unset._cmp(red));
    assertUnset.accept(red._cmp(unset));
  }

  @Test
  void testPropertyAccess() {
    // HSL properties of test color from JustColour.ek9
    assertEquals(148, modifiedColour.hue().state);
    assertEquals(71.9298245614035, modifiedColour.saturation().state, 0.0001);
    assertEquals(22.35294117647059, modifiedColour.lightness().state, 0.0001);

    // Primary colors
    assertEquals(0, redRgb.hue().state);
    assertEquals(100.0, redRgb.saturation().state, 0.0001);
    assertEquals(50.0, redRgb.lightness().state, 0.0001);

    assertEquals(120, greenRgb.hue().state);
    assertEquals(100.0, greenRgb.saturation().state, 0.0001);
    assertEquals(50.0, greenRgb.lightness().state, 0.0001);

    assertEquals(240, blueRgb.hue().state);
    assertEquals(100.0, blueRgb.saturation().state, 0.0001);
    assertEquals(50.0, blueRgb.lightness().state, 0.0001);

    // Unset handling
    assertUnset.accept(unset.hue());
    assertUnset.accept(unset.saturation());
    assertUnset.accept(unset.lightness());
  }

  @Test
  void testFormatConversion() {
    // RGB format conversion
    assertEquals("#FF0000", redRgb.RGB()._string().state);
    assertEquals("#FF0000FF", redRgb.RGBA()._string().state);
    assertEquals("#FFFF0000", redRgb.ARGB()._string().state);

    // ARGB format conversion
    assertEquals("#FF0000", redArgb.RGB()._string().state);
    assertEquals("#FF0000FF", redArgb.RGBA()._string().state);
    assertEquals("#FFFF0000", redArgb.ARGB()._string().state);

    // Bits conversion
    final var redBits = redRgb.bits();
    assertSet.accept(redBits);
    assertTrue(redBits._string().state.contains("1111")); // Should contain binary representation

    // Unset handling
    assertUnset.accept(unset.RGB());
    assertUnset.accept(unset.RGBA());
    assertUnset.accept(unset.ARGB());
    assertUnset.accept(unset.bits());
  }

  @Test
  void testColorManipulation() {
    // Test withOpaque
    final var halfOpaque = redRgb.withOpaque(Integer._of(50));
    assertSet.accept(halfOpaque);
    assertEquals("#80FF0000", halfOpaque._string().state);

    // Test withHue
    final var green = redRgb.withHue(Integer._of(120));
    assertSet.accept(green);
    assertEquals("#00FF00", green._string().state);

    // Test withLightness
    final var darker = redRgb.withLightness(Float._of(25));
    assertSet.accept(darker);
    assertEquals("#800000", darker._string().state);

    // Test withSaturation
    final var desaturated = redRgb.withSaturation(Float._of(0));
    assertSet.accept(desaturated);
    assertEquals("#808080", desaturated._string().state);

    // Invalid ranges
    final var invalidOpaque = redRgb.withOpaque(Integer._of(200));
    assertUnset.accept(invalidOpaque);

    final var invalidHue = redRgb.withHue(Integer._of(400));
    assertUnset.accept(invalidHue);

    // Unset handling
    assertUnset.accept(unset.withOpaque(Integer._of(50)));
    assertUnset.accept(unset.withHue(Integer._of(120)));
    assertUnset.accept(unset.withLightness(Float._of(50)));
    assertUnset.accept(unset.withSaturation(Float._of(50)));
  }

  @Test
  void testArithmeticOperations() {
    // Addition (bitwise OR)
    final var purple = redRgb._add(blueRgb);
    assertSet.accept(purple);
    assertEquals("#FF00FF", purple._string().state);

    final var yellow = redRgb._add(greenRgb);
    assertSet.accept(yellow);
    assertEquals("#FFFF00", yellow._string().state);

    final var cyan = greenRgb._add(blueRgb);
    assertSet.accept(cyan);
    assertEquals("#00FFFF", cyan._string().state);

    // Subtraction (bitwise AND with negation)
    final var backToRed = purple._sub(blueRgb);
    assertSet.accept(backToRed);
    assertEquals("#FF0000", backToRed._string().state);

    final var backToGreen = yellow._sub(redRgb);
    assertSet.accept(backToGreen);
    assertEquals("#00FF00", backToGreen._string().state);

    // Complement
    final var complementRed = redRgb._negate();
    assertSet.accept(complementRed);
    assertEquals("#FF00FFFF", complementRed._string().state);

    // Unset handling
    assertUnset.accept(unset._add(redRgb));
    assertUnset.accept(redRgb._add(unset));
    assertUnset.accept(unset._sub(redRgb));
    assertUnset.accept(redRgb._sub(unset));
    assertUnset.accept(unset._negate());
  }

  @Test
  void testBitIntegration() {
    // Roundtrip conversion
    final var originalBits = testColour.bits();
    final var reconstructed = new Colour(originalBits);
    assertSet.accept(reconstructed);
    assertTrue.accept(testColour._eq(reconstructed));

    // RGB to bits
    final var redBits = redRgb.bits();
    assertTrue(redBits._string().state.contains("1111")); // Should contain binary representation

    // ARGB to bits
    final var argbBits = redArgb.bits();
    assertTrue(argbBits._string().state.contains("1111")); // Should contain binary representation

    // Bits to color
    final var fromBits = new Colour(Bits._of("11111111000000001111111100000000"));
    assertEquals("#FF00FF00", fromBits._string().state);
  }

  @Test
  void testAssignmentOperators() {
    // Copy operation
    final var target = new Colour();
    assertNotNull(target);
    target._copy(redRgb);
    assertSet.accept(target);
    assertTrue.accept(target._eq(redRgb));

    // Merge operation
    final var mergeTarget = new Colour();
    mergeTarget._merge(blueRgb);
    assertSet.accept(mergeTarget);
    assertTrue.accept(mergeTarget._eq(blueRgb));

    // Replace operation
    final var replaceTarget = new Colour(redRgb);
    replaceTarget._replace(greenRgb);
    assertSet.accept(replaceTarget);
    assertTrue.accept(replaceTarget._eq(greenRgb));

    // Pipe operation
    final var pipeTarget = new Colour();
    pipeTarget._pipe(redRgb);
    assertSet.accept(pipeTarget);
    assertTrue.accept(pipeTarget._eq(redRgb));

    // Null handling
    target._copy(null);
    assertUnset.accept(target);

    final var unsetTarget = new Colour();
    unsetTarget._copy(unset);
    assertUnset.accept(unsetTarget);
  }

  @Test
  void testEdgeCases() {
    // Black color
    final var black = Colour._of("#000000");
    assertSet.accept(black);
    assertEquals(0, black.hue().state);
    assertEquals(0.0, black.saturation().state, 0.0001);
    assertEquals(0.0, black.lightness().state, 0.0001);

    // White color
    final var white = Colour._of("#FFFFFF");
    assertSet.accept(white);
    assertEquals(0, white.hue().state);
    assertEquals(0.0, white.saturation().state, 0.0001);
    assertEquals(100.0, white.lightness().state, 0.0001);

    // Gray color
    final var gray = Colour._of("#808080");
    assertSet.accept(gray);
    assertEquals(0, gray.hue().state);
    assertEquals(0.0, gray.saturation().state, 0.0001);
    assertEquals(50.196078431372548, gray.lightness().state, 0.0001);

    // Transparent color
    final var transparent = Colour._of("#00000000");
    assertSet.accept(transparent);
    assertEquals("#00000000", transparent._string().state);
  }

  @Test
  void testUnsetPropagation() {
    // All operations with unset should return unset
    assertUnset.accept(unset._add(redRgb));
    assertUnset.accept(unset._sub(redRgb));
    assertUnset.accept(unset._negate());
    assertUnset.accept(unset.withOpaque(Integer._of(50)));
    assertUnset.accept(unset.withHue(Integer._of(120)));
    assertUnset.accept(unset.withLightness(Float._of(50)));
    assertUnset.accept(unset.withSaturation(Float._of(50)));
    assertUnset.accept(unset.hue());
    assertUnset.accept(unset.saturation());
    assertUnset.accept(unset.lightness());
    assertUnset.accept(unset.bits());
    assertUnset.accept(unset.RGB());
    assertUnset.accept(unset.RGBA());
    assertUnset.accept(unset.ARGB());
    assertUnset.accept(unset._string());
    assertUnset.accept(unset._hashcode());
  }

  @Test
  void testPreciseHSLConversion() {
    //This is R= 24, G= 98, B=118 which maps to #186276 and #FF186276 if Alpha added (opaque).
    final var testColor = Colour._of("#FF186276");
    assertEquals(193, testColor.hue().state);
    assertEquals(66.19718309859155, testColor.saturation().state, 0.0001);
    assertEquals(27.84313725490196, testColor.lightness().state, 0.0001);

    //Now just flip it back(ish).
    final var fromHsl = Colour._of(193, 67, 28);
    assertSet.accept(fromHsl);
    //Close to the original, but not exact dues to rounding of the precision.
    assertEquals("#186377", fromHsl._string().state);

    final var modifiedColor = Colour._of("#B7106236");
    assertEquals(148, modifiedColor.hue().state);
    assertEquals(71.9298245614035, modifiedColor.saturation().state, 0.0001);
    assertEquals(22.35294117647059, modifiedColor.lightness().state, 0.0001);

  }

  @Test
  void testHashCode() {
    // Same colors should have same hash
    final var red1 = Colour._of("#FF0000");
    final var red2 = Colour._of("FF0000");
    assertEquals(red1._hashcode().state, red2._hashcode().state);

    // Different colors should have different hash
    assertNotEquals(redRgb._hashcode(), blueRgb._hashcode());

    // Unset colors should have unset hash
    assertUnset.accept(unset._hashcode());
  }

  @Test
  void testStringRepresentation() {
    // RGB format
    assertEquals("#FF0000", redRgb._string().state);

    // ARGB format
    assertEquals("#FFFF0000", redArgb._string().state);

    // Unset representation
    assertUnset.accept(unset._string());
  }

  @Test
  void testColorArithmeticWithAlpha() {
    // Test mixed alpha operations
    final var redWithAlpha = Colour._of("#80FF0000");
    final var greenWithAlpha = Colour._of("#2000FF00");
    final var blueNoAlpha = Colour._of("#0000FF");

    final var mixedAddition1 = redWithAlpha._add(blueNoAlpha);
    assertSet.accept(mixedAddition1);
    assertEquals("#80FF00FF", mixedAddition1._string().state);

    final var mixedAddition2 = blueNoAlpha._add(redWithAlpha);
    assertSet.accept(mixedAddition2);
    assertEquals("#80FF00FF", mixedAddition2._string().state);
    final var mixedSubtraction1 = redWithAlpha._sub(blueNoAlpha);
    assertSet.accept(mixedSubtraction1);
    assertEquals("#80FF0000", mixedSubtraction1._string().state);

    final var mixedSubtraction2 = blueNoAlpha._sub(redWithAlpha);
    assertSet.accept(mixedSubtraction2);
    assertEquals("#800000FF", mixedSubtraction2._string().state);

    //Now alpha on alpha.
    final var mixedAddition3 = redWithAlpha._add(greenWithAlpha);
    assertSet.accept(mixedAddition3);
    assertEquals("#90FFFF00", mixedAddition3._string().state);

    final var mixedSubtraction3 = greenWithAlpha._sub(redWithAlpha);
    assertSet.accept(mixedSubtraction3);
    assertEquals("#0F00FF00", mixedSubtraction3._string().state);
  }

  @Test
  void testComplexColorOperations() {
    // Test chained operations
    final var result = redRgb._add(blueRgb)._sub(greenRgb);
    assertSet.accept(result);
    assertEquals("#FF00FF", result._string().state);

    // Test with opacity changes
    final var opaqueResult = result.withOpaque(Integer._of(50));
    assertSet.accept(opaqueResult);
    assertEquals("#80FF00FF", opaqueResult._string().state);

    // Test HSL manipulations
    final var hslResult = opaqueResult.withHue(Integer._of(60)).withSaturation(Float._of(75));
    assertSet.accept(hslResult);
    assertTrue.accept(hslResult._isSet());
    assertEquals("#80DFDF20", hslResult._string().state);

  }

  @Test
  void testAsJson() {
    // Test JSON conversion with set values
    final var redColour = new Colour(String._of("#FF0000"));
    assertNotNull(redColour);
    final var redJson = redColour._json();
    assertSet.accept(redJson);

    final var blueColour = new Colour(String._of("#0000FF"));
    final var blueJson = blueColour._json();
    assertSet.accept(blueJson);

    // Test JSON conversion with unset value
    assertUnset.accept(unset._json());
  }
}