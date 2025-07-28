package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Colour class.
 * Tests color parsing, HSL conversions, color arithmetic, and format conversions.
 */
class ColourTest extends Common {

  // Helper methods to reduce duplication
  private void assertColorEquals(Colour actual, Colour expected) {
    assertTrue.accept(actual._eq(expected));
  }

  private void assertHexEquals(java.lang.String expected, Colour actual) {
    assertEquals(expected, actual._string().state);
  }

  @Nested
  class ConstructionAndFactories {

    @Test
    void testConstruction() {
      // Default constructor creates unset
      final var defaultConstructor = new Colour();
      assertUnset.accept(defaultConstructor);

      // Copy constructor
      final var copyConstructor = new Colour(testColour);
      assertColorEquals(copyConstructor, testColour);

      // String constructor with valid hex
      final var stringConstructor = new Colour(String._of("#FF186276"));
      assertHexEquals("#FF186276", stringConstructor);

      // String constructor with 3-digit hex
      final var shortHex = new Colour(String._of("#F00"));
      assertHexEquals("#FF0000", shortHex);

      // String constructor with 6-digit hex (no alpha)
      final var rgbHex = new Colour(String._of("#FF0000"));
      assertColorEquals(rgbHex, redRgb);

      // String constructor with invalid format
      final var invalidHex = new Colour(String._of("invalid"));
      assertUnset.accept(invalidHex);

      // Bits constructor with 24-bit RGB
      final var bitsRgb = new Colour(Bits._of(BITS_RGB_RED));
      assertColorEquals(bitsRgb, redRgb);

      // Bits constructor with 32-bit ARGB
      final var bitsArgb = new Colour(Bits._of(BITS_ARGB_RED));
      assertColorEquals(bitsArgb, redArgb);

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
      assertHexEquals("#FF186276", fromString);

      // HSL factory (RGB)
      final var fromHsl = Colour._of(0, 100, 50);
      assertHexEquals("#FF0000", fromHsl);

      // HSL factory with alpha
      final var fromHslAlpha = Colour._of(128, 0, 100, 50);
      assertHexEquals("#80FF0000", fromHslAlpha);
    }

    @Test
    void testStringParsing() {
      // Valid hex formats
      final var withHash = Colour._of("#FF186276");
      assertHexEquals("#FF186276", withHash);

      final var withoutHash = Colour._of("FF186276");
      assertHexEquals("#FF186276", withoutHash);

      // 3-digit expansion
      final var shortRed = Colour._of("#F00");
      assertHexEquals("#FF0000", shortRed);

      // 6-digit RGB
      final var rgb = Colour._of("#FF0000");
      assertHexEquals("#FF0000", rgb);

      // 8-digit ARGB
      final var argb = Colour._of("#80FF0000");
      assertHexEquals("#80FF0000", argb);

      // Invalid formats
      final var tooShort = Colour._of(HEX_TOO_SHORT);
      assertUnset.accept(tooShort);

      final var tooLong = Colour._of(HEX_TOO_LONG);
      assertUnset.accept(tooLong);

      final var invalidChars = Colour._of(HEX_INVALID_CHARS);
      assertUnset.accept(invalidChars);
    }
  }

  @Nested
  class OperatorTests {

    @Nested
    class ComparisonOperators {

      @Test
      void testEquality() {
        // Same color equality
        final var red1 = Colour._of("#FF0000");
        final var red2 = Colour._of("FF0000");
        assertTrue.accept(red1._eq(red2));
        assertFalse.accept(red1._neq(red2));

        // Different colors
        assertFalse.accept(redRgb._eq(blueRgb));
        assertTrue.accept(redRgb._neq(blueRgb));

        // Unset propagation
        assertUnset.accept(unsetColour._eq(redRgb));
        assertUnset.accept(redRgb._eq(unsetColour));
        assertUnset.accept(unsetColour._neq(redRgb));
        assertUnset.accept(redRgb._neq(unsetColour));
      }

      @Test
      void testComparison() {
        // Color comparison based on numeric value (Black < Blue < Red < White)
        assertTrue.accept(blackRgb._lt(blueRgb));
        assertTrue.accept(blueRgb._lt(redRgb));
        assertTrue.accept(redRgb._lt(whiteRgb));
        assertFalse.accept(whiteRgb._lt(blackRgb));

        assertTrue.accept(blackRgb._lteq(blueRgb));
        assertTrue.accept(redRgb._lteq(redRgb));
        assertFalse.accept(whiteRgb._lteq(blackRgb));

        assertTrue.accept(whiteRgb._gt(redRgb));
        assertTrue.accept(redRgb._gt(blackRgb));
        assertFalse.accept(blackRgb._gt(whiteRgb));

        assertTrue.accept(whiteRgb._gteq(redRgb));
        assertTrue.accept(redRgb._gteq(redRgb));
        assertFalse.accept(blackRgb._gteq(whiteRgb));

        // Comparison operator
        assertTrue(blackRgb._cmp(redRgb).state < 0);
        assertTrue(whiteRgb._cmp(redRgb).state > 0);
        assertEquals(0, redRgb._cmp(redRgb).state);

        //Should have unset result.
        assertUnset.accept(whiteRgb._cmp(Any._new()));
        //This will go via Any and back up to Colour.
        assertUnset.accept(whiteRgb._lt(Any._new()));

        // Unset propagation
        assertUnset.accept(unsetColour._lt(redRgb));
        assertUnset.accept(redRgb._lt(unsetColour));
        assertUnset.accept(unsetColour._cmp(redRgb));
        assertUnset.accept(redRgb._cmp(unsetColour));
      }

      @Test
      void testFuzzyOperator() {
        // Basic fuzzy comparison - same colors should return 0
        assertEquals(0, redRgb._fuzzy(redRgb).state);

        // Different colors - should match _cmp behavior since _fuzzy delegates to _cmp
        // Black < Blue < Red < White (numerically by ARGB value)
        // Black: 0xFF000000, Blue: 0xFF0000FF, Red: 0xFFFF0000, White: 0xFFFFFFFF
        assertTrue(blackRgb._fuzzy(blueRgb).state < 0);
        assertTrue(blueRgb._fuzzy(redRgb).state < 0);
        assertTrue(redRgb._fuzzy(whiteRgb).state < 0);

        // Reverse comparisons
        assertTrue(blueRgb._fuzzy(blackRgb).state > 0);
        assertTrue(redRgb._fuzzy(blueRgb).state > 0);
        assertTrue(whiteRgb._fuzzy(redRgb).state > 0);

        // Test RGB vs ARGB versions of same color
        // Both should be equivalent (RGB gets FF alpha prepended, ARGB already has FF alpha)
        assertEquals(0, redArgb._fuzzy(redRgb).state);
        assertEquals(0, redRgb._fuzzy(redArgb).state);

        // Alpha channel effects on comparison
        assertTrue(redHalfAlpha._fuzzy(redArgb).state < 0);
        assertTrue(redArgb._fuzzy(redHalfAlpha).state > 0);

        // Similar but different colors
        final var red2 = Colour._of("#FF0001");
        assertTrue(redRgb._fuzzy(red2).state < 0);
        assertTrue(red2._fuzzy(redRgb).state > 0);

        // Test with predefined test colors from Common
        assertTrue(testColour._fuzzy(modifiedColour).state > 0);
        assertTrue(modifiedColour._fuzzy(testColour).state < 0);
        assertEquals(0, testColour._fuzzy(testColour).state);

        // Verify fuzzy results match _cmp results (since _fuzzy delegates to _cmp)
        assertEquals(redRgb._cmp(blueRgb).state, redRgb._fuzzy(blueRgb).state);
        assertEquals(blueRgb._cmp(redRgb).state, blueRgb._fuzzy(redRgb).state);
        assertEquals(redRgb._cmp(redRgb).state, redRgb._fuzzy(redRgb).state);

        // Unset propagation - all should return unset Integer
        assertUnset.accept(unsetColour._fuzzy(redRgb));
        assertUnset.accept(redRgb._fuzzy(unsetColour));
        assertUnset.accept(unsetColour._fuzzy(unsetColour));

        // Test null handling (should return unset)
        assertUnset.accept(redRgb._fuzzy(null));

        // Extreme values comparison using Common constants
        assertTrue(transparentBlack._fuzzy(transparentWhite).state < 0);
        assertTrue(transparentWhite._fuzzy(transparentBlack).state > 0);
        assertEquals(0, transparentBlack._fuzzy(transparentBlack).state);
        assertEquals(0, transparentWhite._fuzzy(transparentWhite).state);
      }
    }

    @Nested
    class ArithmeticOperators {

      @Test
      void testArithmeticOperations() {
        // Addition (bitwise OR) - verify expected composite colors
        final var purple = redRgb._add(blueRgb);
        assertColorEquals(purple, purpleRgb);

        final var yellow = redRgb._add(greenRgb);
        assertColorEquals(yellow, yellowRgb);

        final var cyan = greenRgb._add(blueRgb);
        assertColorEquals(cyan, cyanRgb);

        // Subtraction (bitwise AND with negation)
        final var backToRed = purpleRgb._sub(blueRgb);
        assertColorEquals(backToRed, redRgb);

        final var backToGreen = yellowRgb._sub(redRgb);
        assertColorEquals(backToGreen, greenRgb);

        // Complement
        final var complementRed = redRgb._negate();
        assertHexEquals("#FF00FFFF", complementRed);

        // Unset handling
        assertUnset.accept(unsetColour._add(redRgb));
        assertUnset.accept(redRgb._add(unsetColour));
        assertUnset.accept(unsetColour._sub(redRgb));
        assertUnset.accept(redRgb._sub(unsetColour));
        assertUnset.accept(unsetColour._negate());
      }

      @Test
      void testColorArithmeticWithAlpha() {
        // Test mixed alpha operations (ARGB + RGB)
        final var mixedAddition1 = redHalfAlpha._add(blueRgb);
        assertHexEquals("#80FF00FF", mixedAddition1);

        final var mixedAddition2 = blueRgb._add(redHalfAlpha);
        assertHexEquals("#80FF00FF", mixedAddition2);

        final var mixedSubtraction1 = redHalfAlpha._sub(blueRgb);
        assertHexEquals("#80FF0000", mixedSubtraction1);

        final var mixedSubtraction2 = blueRgb._sub(redHalfAlpha);
        assertHexEquals("#800000FF", mixedSubtraction2);

        // Alpha on alpha operations
        final var mixedAddition3 = redHalfAlpha._add(greenWithAlpha);
        assertHexEquals("#90FFFF00", mixedAddition3);

        final var mixedSubtraction3 = greenWithAlpha._sub(redHalfAlpha);
        assertHexEquals("#0F00FF00", mixedSubtraction3);
      }
    }

    @Nested
    class AssignmentOperators {

      @Test
      void testAssignmentOperators() {
        // Copy operation
        final var target = new Colour();
        target._copy(redRgb);
        assertColorEquals(target, redRgb);

        // Merge operation
        final var mergeTarget = new Colour();
        mergeTarget._merge(blueRgb);
        assertColorEquals(mergeTarget, blueRgb);

        // Replace operation
        final var replaceTarget = new Colour(redRgb);
        replaceTarget._replace(greenRgb);
        assertColorEquals(replaceTarget, greenRgb);

        // Pipe operation
        final var pipeTarget = new Colour();
        pipeTarget._pipe(redRgb);
        assertColorEquals(pipeTarget, redRgb);

        // Null handling
        target._copy(null);
        assertUnset.accept(target);

        final var unsetTarget = new Colour();
        unsetTarget._copy(unsetColour);
        assertUnset.accept(unsetTarget);
      }
    }
  }

  @Nested
  class PropertyAndConversion {

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
      assertUnset.accept(unsetColour.hue());
      assertUnset.accept(unsetColour.saturation());
      assertUnset.accept(unsetColour.lightness());
    }

    @Test
    void testFormatConversion() {
      // RGB format conversion
      assertEquals("#FF0000", redRgb.RGB().state);
      assertEquals("#FF0000FF", redRgb.RGBA().state);
      assertEquals("#FFFF0000", redRgb.ARGB().state);

      // ARGB format conversion
      assertEquals("#FF0000", redArgb.RGB().state);
      assertEquals("#FF0000FF", redArgb.RGBA().state);
      assertEquals("#FFFF0000", redArgb.ARGB().state);

      // Bits conversion
      final var redBits = redRgb.bits();
      assertTrue(redBits._string().state.contains("1111")); // Should contain binary representation

      // Unset handling
      assertUnset.accept(unsetColour.RGB());
      assertUnset.accept(unsetColour.RGBA());
      assertUnset.accept(unsetColour.ARGB());
      assertUnset.accept(unsetColour.bits());
    }

    @Test
    void testBitIntegration() {
      // Roundtrip conversion
      final var originalBits = testColour.bits();
      final var reconstructed = new Colour(originalBits);
      assertColorEquals(testColour, reconstructed);

      // RGB to bits
      final var redBits = redRgb.bits();
      assertTrue(redBits._string().state.contains("1111")); // Should contain binary representation

      // ARGB to bits
      final var argbBits = redArgb.bits();
      assertTrue(argbBits._string().state.contains("1111")); // Should contain binary representation

      // Bits to color using test data constants
      final var fromBits = new Colour(Bits._of(BITS_MIXED_COLOR));
      assertHexEquals("#FF00FF00", fromBits);
    }

    @Test
    void testStringRepresentation() {
      // RGB format
      assertHexEquals("#FF0000", redRgb);

      // ARGB format
      assertHexEquals("#FFFF0000", redArgb);

      // Unset representation
      assertUnset.accept(unsetColour._string());
    }

    @Test
    void testAsJson() {
      // Test JSON conversion with set values (no need to check specific JSON content)
      final var redColour = new Colour(String._of("#FF0000"));
      assertTrue.accept(redColour._json()._isSet());

      final var blueColour = new Colour(String._of("#0000FF"));
      assertTrue.accept(blueColour._json()._isSet());

      // Test JSON conversion with unset value
      assertUnset.accept(unsetColour._json());
    }
  }

  @Nested
  class ColorManipulation {

    @Test
    void testColorManipulation() {
      // Test withOpaque
      final var halfOpaque = redRgb.withOpaque(INT_50);
      assertHexEquals("#80FF0000", halfOpaque);

      // Test withHue
      final var green = redRgb.withHue(INT_120);
      assertColorEquals(green, greenRgb);

      // Test withLightness
      final var darker = redRgb.withLightness(FLOAT_25);
      assertHexEquals("#800000", darker);

      // Test withSaturation
      final var desaturated = redRgb.withSaturation(FLOAT_0);
      assertColorEquals(desaturated, grayRgb);

      // Invalid ranges
      final var invalidOpaque = redRgb.withOpaque(Integer._of(200));
      assertUnset.accept(invalidOpaque);

      final var invalidHue = redRgb.withHue(Integer._of(400));
      assertUnset.accept(invalidHue);

      // Unset handling
      assertUnset.accept(unsetColour.withOpaque(INT_50));
      assertUnset.accept(unsetColour.withHue(INT_120));
      assertUnset.accept(unsetColour.withLightness(FLOAT_50));
      assertUnset.accept(unsetColour.withSaturation(FLOAT_50));
    }

    @Test
    void testComplexColorOperations() {
      // Test chained operations
      final var result = redRgb._add(blueRgb)._sub(greenRgb);
      assertColorEquals(result, purpleRgb);

      // Test with opacity changes
      final var opaqueResult = result.withOpaque(INT_50);
      assertHexEquals("#80FF00FF", opaqueResult);

      // Test HSL manipulations
      final var hslResult = opaqueResult.withHue(INT_60).withSaturation(FLOAT_75);
      assertTrue.accept(hslResult._isSet());
      assertHexEquals("#80DFDF20", hslResult);
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
      //Close to the original, but not exact dues to rounding of the precision.
      assertHexEquals("#186377", fromHsl);

      final var modifiedColor = Colour._of("#B7106236");
      assertEquals(148, modifiedColor.hue().state);
      assertEquals(71.9298245614035, modifiedColor.saturation().state, 0.0001);
      assertEquals(22.35294117647059, modifiedColor.lightness().state, 0.0001);
    }
  }

  @Nested
  class EdgeCasesAndValidation {

    @Test
    void testStateManagement() {
      // Test _isSet operator
      assertFalse.accept(unsetColour._isSet());
      assertTrue.accept(testColour._isSet());
      assertTrue.accept(redRgb._isSet());
    }

    @Test
    void testEdgeCases() {
      // Black color (already set, no need for assertSet)
      assertEquals(0, blackRgb.hue().state);
      assertEquals(0.0, blackRgb.saturation().state, 0.0001);
      assertEquals(0.0, blackRgb.lightness().state, 0.0001);

      // White color
      assertEquals(0, whiteRgb.hue().state);
      assertEquals(0.0, whiteRgb.saturation().state, 0.0001);
      assertEquals(100.0, whiteRgb.lightness().state, 0.0001);

      // Gray color
      assertEquals(0, grayRgb.hue().state);
      assertEquals(0.0, grayRgb.saturation().state, 0.0001);
      assertEquals(50.196078431372548, grayRgb.lightness().state, 0.0001);

      // Transparent color
      assertHexEquals("#00000000", transparentBlack);
    }

    @Test
    void testHashCode() {
      // Same colors should have same hash
      final var red2 = Colour._of("FF0000");
      assertEquals(redRgb._hashcode().state, red2._hashcode().state);

      // Different colors should have different hash
      assertNotEquals(redRgb._hashcode(), blueRgb._hashcode());

      // Unset colors should have unset hash
      assertUnset.accept(unsetColour._hashcode());
    }

    @Test
    void testUnsetPropagation() {
      // All operations with unset should return unset (selective sampling - not all operations)
      assertUnset.accept(unsetColour._add(redRgb));
      assertUnset.accept(unsetColour._sub(redRgb));
      assertUnset.accept(unsetColour._negate());
      assertUnset.accept(unsetColour.withOpaque(INT_50));
      assertUnset.accept(unsetColour.hue());
      assertUnset.accept(unsetColour.saturation());
      assertUnset.accept(unsetColour.lightness());
      assertUnset.accept(unsetColour.bits());
      assertUnset.accept(unsetColour.RGB());
      assertUnset.accept(unsetColour._string());
      assertUnset.accept(unsetColour._hashcode());
    }
  }
}