package org.ek9.lang;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * EK9 Colour type implementation.
 * Represents colors as ARGB values with support for HSL color space operations.
 * Format: #RRGGBB or #AARRGGBB (e.g., #FF186276, #B7106236)
 * Supports color arithmetic, alpha blending, and precise HSL conversions.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    Colour as open""")
public class Colour extends SuffixedComponent {

  // Cached HSL values for performance
  private long hue;
  private double saturation;
  private double lightness;
  private boolean hslCached = false;

  @Ek9Constructor("""
      Colour() as pure""")
  public Colour() {
    super.unSet();
  }

  @Ek9Constructor("""
      Colour() as pure
        -> arg0 as Colour""")
  public Colour(Colour arg0) {
    this();
    if (isValid(arg0)) {
      assign(arg0);
    }
  }

  @Ek9Constructor("""
      Colour() as pure
        -> arg0 as String""")
  public Colour(String arg0) {
    this();
    if (isValid(arg0)) {
      parse(arg0.state);
    }
  }

  @Ek9Constructor("""
      Colour() as pure
        -> arg0 as Bits""")
  public Colour(Bits arg0) {
    this();
    if (isValid(arg0)) {
      java.lang.String hexValue = convertBitsToHex(arg0);
      if (hexValue != null) {
        assign(hexValue);
      }
    }
  }

  @Ek9Method("""
      hue() as pure
        <- rtn as Integer?""")
  public Integer hue() {
    if (isSet) {
      ensureHslCached();
      return Integer._of(hue);
    }
    return new Integer();
  }

  @Ek9Method("""
      saturation() as pure
        <- rtn as Float?""")
  public Float saturation() {
    if (isSet) {
      ensureHslCached();
      return Float._of(saturation);
    }
    return new Float();
  }

  @Ek9Method("""
      lightness() as pure
        <- rtn as Float?""")
  public Float lightness() {
    if (isSet) {
      ensureHslCached();
      return Float._of(lightness);
    }
    return new Float();
  }

  @Ek9Method("""
      bits() as pure
        <- rtn as Bits?""")
  public Bits bits() {
    if (isSet) {
      return Bits._of(convertHexToBits());
    }
    return new Bits();
  }

  @Ek9Method("""
      RGB() as pure
        <- rtn as String?""")
  public String RGB() {
    if (isSet) {
      return String._of(java.lang.String.format("#%06X", asLong() & 0x00FFFFFF));
    }
    return new String();
  }

  @Ek9Method("""
      RGBA() as pure
        <- rtn as String?""")
  public String RGBA() {
    if (isSet) {
      if (isAlphaNotPresent()) {
        return String._of(this._string().state + "FF");
      } else {
        // Get RGB part and append alpha
        java.lang.String rgb = java.lang.String.format("#%06X", asLong() & 0x00FFFFFF);
        java.lang.String alpha = java.lang.String.format("%02X", (asLong() >> 24) & 0xFF);
        return String._of(rgb + alpha);
      }
    }
    return new String();
  }

  @Ek9Method("""
      ARGB() as pure
        <- rtn as String?""")
  public String ARGB() {
    if (isSet) {
      if (isAlphaNotPresent()) {
        return String._of(java.lang.String.format("#FF%06X", asLong() & 0x00FFFFFF));
      } else {
        return this._string();
      }
    }
    return new String();
  }

  @Ek9Method("""
      withOpaque() as pure
        -> arg0 as Integer
        <- rtn as Colour?""")
  public Colour withOpaque(Integer arg0) {
    Colour result = _new();
    if (canProcess(arg0) && arg0.state >= 0 && arg0.state <= 100) {
      long amount = (long) ((256.0 * arg0.state) / 100.0);
      if (amount >= 256) {
        amount = 255; // Clamp to max
      }
      amount <<= 24;
      long newValue = (asLong() & 0x00FFFFFF) | amount;
      result.assign(java.lang.String.format("%08X", newValue));
    }
    return result;
  }

  @Ek9Method("""
      withHue() as pure
        -> arg0 as Integer
        <- rtn as Colour?""")
  public Colour withHue(Integer arg0) {
    Colour result = _new();
    if (canProcess(arg0) && arg0.state >= 0 && arg0.state <= 360) {
      ensureHslCached();
      long alpha = getAlpha();
      java.lang.String hexValue = createFromHSL(alpha, arg0.state, this.saturation, this.lightness);
      result.assign(hexValue);
    }
    return result;
  }

  @Ek9Method("""
      withLightness() as pure
        -> arg0 as Float
        <- rtn as Colour?""")
  public Colour withLightness(Float arg0) {
    Colour result = _new();
    if (canProcess(arg0) && arg0.state >= 0 && arg0.state <= 100) {
      ensureHslCached();
      long alpha = getAlpha();
      java.lang.String hexValue = createFromHSL(alpha, this.hue, this.saturation, arg0.state);
      result.assign(hexValue);
    }
    return result;
  }

  @Ek9Method("""
      withSaturation() as pure
        -> arg0 as Float
        <- rtn as Colour?""")
  public Colour withSaturation(Float arg0) {
    Colour result = _new();
    if (canProcess(arg0) && arg0.state >= 0 && arg0.state <= 100) {
      ensureHslCached();
      long alpha = getAlpha();
      java.lang.String hexValue = createFromHSL(alpha, this.hue, arg0.state, this.lightness);
      result.assign(hexValue);
    }
    return result;
  }

  @Ek9Operator("""
      operator < as pure
        -> arg as Colour
        <- rtn as Boolean?""")
  public Boolean _lt(Colour arg) {
    if (canProcess(arg)) {
      return Boolean._of(asLong() < arg.asLong());
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <= as pure
        -> arg as Colour
        <- rtn as Boolean?""")
  public Boolean _lteq(Colour arg) {
    if (canProcess(arg)) {
      return Boolean._of(asLong() <= arg.asLong());
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator > as pure
        -> arg as Colour
        <- rtn as Boolean?""")
  public Boolean _gt(Colour arg) {
    if (canProcess(arg)) {
      return Boolean._of(asLong() > arg.asLong());
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator >= as pure
        -> arg as Colour
        <- rtn as Boolean?""")
  public Boolean _gteq(Colour arg) {
    if (canProcess(arg)) {
      return Boolean._of(asLong() >= arg.asLong());
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as Colour
        <- rtn as Boolean?""")
  public Boolean _eq(Colour arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.suffix.equals(arg.suffix));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as Colour
        <- rtn as Boolean?""")
  public Boolean _neq(Colour arg) {
    if (canProcess(arg)) {
      return Boolean._of(!this.suffix.equals(arg.suffix));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as Colour
        <- rtn as Integer?""")
  public Integer _cmp(Colour arg) {
    if (canProcess(arg)) {
      long thisValue = asLong();
      long argValue = arg.asLong();
      if (thisValue < argValue) {
        return Integer._of(-1);
      } else if (thisValue > argValue) {
        return Integer._of(1);
      } else {
        return Integer._of(0);
      }
    }
    return new Integer();
  }

  @Override
  @Ek9Operator("""
      operator <=> as pure
        -> arg as Any
        <- rtn as Integer?""")
  public Integer _cmp(Any arg) {
    if (arg instanceof Colour asColour) {
      return _cmp(asColour);
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as Colour
        <- rtn as Colour?""")
  public Colour _add(Colour arg) {
    Colour result = _new();
    if (canProcess(arg)) {
      java.lang.String hexValue = performColourAddition(this, arg);
      result.assign(hexValue);
    }
    return result;
  }

  @Ek9Operator("""
      operator - as pure
        -> arg as Colour
        <- rtn as Colour?""")
  public Colour _sub(Colour arg) {
    Colour result = _new();
    if (canProcess(arg)) {
      java.lang.String hexValue = performColourSubtraction(this, arg);
      result.assign(hexValue);
    }
    return result;
  }

  @Ek9Operator("""
      operator ~ as pure
        <- rtn as Colour?""")
  public Colour _negate() {
    Colour result = _new();
    if (isSet) {
      long value = asLong();
      long alpha = value & 0xFF000000L;
      long rgb = value & 0x00FFFFFF;
      long complement = (~rgb) & 0x00FFFFFF;
      java.lang.String hexValue = java.lang.String.format("%08X", alpha | complement);
      result.assign(hexValue);
    }
    return result;
  }

  @Ek9Operator("""
      operator |
        -> arg0 as Colour""")
  public void _pipe(Colour arg0) {
    _merge(arg0);
  }

  @Ek9Operator("""
      operator :~:
        -> arg0 as Colour""")
  public void _merge(Colour arg0) {
    if (isValid(arg0)) {
      assign(arg0);
    }
  }

  @Ek9Operator("""
      operator :^:
        -> arg0 as Colour""")
  public void _replace(Colour arg0) {
    _copy(arg0);
  }

  @Ek9Operator("""
      operator :=:
        -> arg0 as Colour""")
  public void _copy(Colour arg0) {
    if (isValid(arg0)) {
      assign(arg0);
    } else {
      unSet();
    }
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(this.isSet);
  }

  @Override
  @Ek9Operator("""
      operator $$ as pure
        <- rtn as JSON?""")
  public JSON _json() {
    if (isSet) {
      return new JSON(this);
    }
    return new JSON();
  }

  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  public String _string() {
    if (isSet) {
      //Here suffix holds the actual argb value.
      return String._of("#" + suffix);
    }
    return new String();
  }

  @Override
  @Ek9Operator("""
      operator #? as pure
        <- rtn as Integer?""")
  public Integer _hashcode() {
    final var result = new Integer();
    if (isSet) {
      result.assign(suffix.hashCode());
    }
    return result;
  }

  // Start of Utility methods

  private long getAlpha() {
    return (this.suffix.length() == 8) ? (asLong() >> 24) & 0xFF : 0xFF;
  }


  @Override
  protected Colour _new() {
    return new Colour();
  }

  /**
   * Convert bits to hex string representation.
   */
  private java.lang.String convertBitsToHex(Bits bits) {
    if (!bits.isSet) {
      return null;
    }

    // Convert bits to hex by converting bit string to long
    java.lang.String bitString = bits._string().state;
    if (bitString.isEmpty()) {
      return null;
    }

    // Remove 0b prefix if present
    if (bitString.startsWith("0b")) {
      bitString = bitString.substring(2);
    }

    if (bitString.length() <= 24) {
      // RGB format
      long value = Long.parseLong(bitString, 2);
      return java.lang.String.format("%06X", value);
    } else if (bitString.length() <= 32) {
      // ARGB format
      long value = Long.parseLong(bitString, 2);
      return java.lang.String.format("%08X", value);
    }

    return null;
  }

  /**
   * Convert hex string to bits representation.
   */
  @SuppressWarnings("checkstyle:CatchParameterName")
  private java.lang.String convertHexToBits() {
    if (!isSet) {
      return null;
    }

    try {
      if (isAlphaNotPresent()) {
        // RGB format - add FF alpha
        return Long.toBinaryString(Long.parseLong("FF" + suffix, 16));
      }
      // ARGB format
      return Long.toBinaryString(Long.parseLong(suffix, 16));

    } catch (NumberFormatException _) {
      return null;
    }
  }

  @Override
  protected void parse(java.lang.String value) {
    // Support hex colors: #RGB, #RRGGBB, #AARRGGBB
    Pattern pattern = Pattern.compile("^#?([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$");
    Matcher matcher = pattern.matcher(value);

    if (matcher.find()) {
      java.lang.String hex = matcher.group(1);

      // Expand 3-digit hex to 6-digit
      if (hex.length() == 3) {
        StringBuilder expanded = new StringBuilder();
        for (char c : hex.toCharArray()) {
          expanded.append(c).append(c);
        }
        hex = expanded.toString();
      }

      assign(hex.toUpperCase());
    }
  }

  private void assign(Colour value) {
    if (isValid(value)) {
      assign(value.suffix);
    }
  }

  private void assign(java.lang.String hexValue) {
    boolean wasSet = isSet;
    java.lang.String previousSuffix = this.suffix;

    this.suffix = hexValue;
    this.hslCached = false;

    if (!validateConstraints().state) {
      this.suffix = previousSuffix;
      this.isSet = wasSet;
      throw Exception._of("Constraint violation can't change " + this + " to #" + hexValue);
    }

    set();
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  private long asLong() {
    if (isSet) {
      try {
        if (isAlphaNotPresent()) {
          // RGB format - add FF alpha
          return Long.parseLong("FF" + suffix, 16);
        } else {
          // ARGB format
          return Long.parseLong(suffix, 16);
        }
      } catch (NumberFormatException _) {
        return 0;
      }
    }
    return 0;
  }

  private boolean isAlphaNotPresent() {
    if (isSet) {
      return suffix.length() != 8;
    }
    return true;
  }

  private void ensureHslCached() {
    if (!hslCached) {
      updateHSL();
      hslCached = true;
    }
  }

  /**
   * A bit gnarly to convert argb to hsl.
   */
  private void updateHSL() {
    long argb = asLong();

    long red = (argb >> 16) & 0xFF;
    long green = (argb >> 8) & 0xFF;
    long blue = argb & 0xFF;

    double r = red / 255.0;
    double g = green / 255.0;
    double b = blue / 255.0;

    double min = Math.min(Math.min(r, g), b);
    double max = Math.max(Math.max(r, g), b);

    double h = 0.0;
    double s = 0.0;
    double l = (min + max) / 2.0;

    if (min != max) {
      double delta = max - min;

      // Saturation
      if (l <= 0.5) {
        s = delta / (max + min);
      } else {
        s = delta / (2.0 - max - min);
      }

      // Hue
      if (r == max) {
        h = (g - b) / delta;
        if (g < b) {
          h += 6;
        }
      } else if (g == max) {
        h = 2.0 + (b - r) / delta;
      } else {
        h = 4.0 + (r - g) / delta;
      }

      h *= 60;
    }

    this.hue = Math.round(h);
    this.saturation = s * 100.0;
    this.lightness = l * 100.0;
  }

  @SuppressWarnings("checkstyle:MultipleVariableDeclarations")
  private java.lang.String createFromHSL(long alpha, long hue, double saturation, double lightness) {

    double h = hue / 360.0;
    double s = saturation / 100.0;
    double l = lightness / 100.0;

    double r, g, b;

    if (s == 0) {
      r = g = b = l; // achromatic
    } else {
      double q = l < 0.5 ? l * (1 + s) : l + s - l * s;
      double p = 2 * l - q;
      r = hueToRgb(p, q, h + 1.0 / 3.0);
      g = hueToRgb(p, q, h);
      b = hueToRgb(p, q, h - 1.0 / 3.0);
    }

    long red = Math.round(r * 255);
    long green = Math.round(g * 255);
    long blue = Math.round(b * 255);

    long rgb = (red << 16) | (green << 8) | blue;
    if (alpha != 0xFF) {
      return java.lang.String.format("%08X", (alpha << 24) | rgb);
    } else {
      return java.lang.String.format("%06X", rgb);
    }
  }

  private double hueToRgb(double p, double q, double t) {
    if (t < 0) {
      t += 1;
    }
    if (t > 1) {
      t -= 1;
    }
    if (t < 1.0 / 6.0) {
      return p + (q - p) * 6 * t;
    }
    if (t < 1.0 / 2.0) {
      return q;
    }
    if (t < 2.0 / 3.0) {
      return p + (q - p) * (2.0 / 3.0 - t) * 6;
    }
    return p;
  }

  private java.lang.String performColourAddition(Colour left, Colour right) {

    //If there is no alpha component present in either then simple or.
    if (left.isAlphaNotPresent() && right.isAlphaNotPresent()) {
      long result = (left.asLong() | right.asLong()) & 0x00FFFFFF;
      return java.lang.String.format("%06X", result);
    }

    return performBlendedAlphaColourAddition(left, right);
  }

  private java.lang.String performBlendedAlphaColourAddition(Colour left, Colour right) {

    long leftValue = left.asLong();
    long rightValue = right.asLong();

    // Determine actual alpha values (not auto-filled ones from asLong())
    long leftAlpha = left.isAlphaNotPresent() ? 0xFF : (leftValue >> 24) & 0xFF;
    long rightAlpha = right.isAlphaNotPresent() ? 0xFF : (rightValue >> 24) & 0xFF;

    // Alpha blending logic for addition
    long resultAlpha;
    if (left.isAlphaNotPresent() && right.isAlphaNotPresent()) {
      // Both RGB - shouldn't reach here (handled by simple OR)
      resultAlpha = 0xFF;
    } else if (left.isAlphaNotPresent()) {
      // RGB + ARGB: preserve ARGB alpha
      resultAlpha = rightAlpha;
    } else if (right.isAlphaNotPresent()) {
      // ARGB + RGB: preserve ARGB alpha
      resultAlpha = leftAlpha;
    } else {
      // ARGB + ARGB: blend alphas using screen blend formula
      resultAlpha = leftAlpha + rightAlpha - (leftAlpha * rightAlpha / 255);
      if (resultAlpha > 255) {
        resultAlpha = 255; // Clamp to max
      }
    }

    // Perform bitwise OR on RGB channels
    long result = (leftValue | rightValue) & 0x00FFFFFF;
    return java.lang.String.format("%08X", (resultAlpha << 24) | result);

  }

  private java.lang.String performColourSubtraction(Colour left, Colour right) {

    //If there is no alpha component present in either then simple and with negated.
    if (left.isAlphaNotPresent() && right.isAlphaNotPresent()) {
      long result = left.asLong() & (~right.asLong()) & 0x00FFFFFF;
      return java.lang.String.format("%06X", result);
    }
    return performBlendedAlphaColourSubtraction(left, right);

  }

  private java.lang.String performBlendedAlphaColourSubtraction(Colour left, Colour right) {

    long leftValue = left.asLong();
    long rightValue = right.asLong();

    // Determine actual alpha values (not auto-filled ones from asLong())
    long leftAlpha = left.isAlphaNotPresent() ? 0xFF : (leftValue >> 24) & 0xFF;
    long rightAlpha = right.isAlphaNotPresent() ? 0xFF : (rightValue >> 24) & 0xFF;

    // Alpha blending logic for subtraction
    long resultAlpha;
    if (left.isAlphaNotPresent() && right.isAlphaNotPresent()) {
      // Both RGB - shouldn't reach here (handled by simple AND)
      resultAlpha = 0xFF;
    } else if (left.isAlphaNotPresent()) {
      // RGB + ARGB: preserve ARGB alpha
      resultAlpha = rightAlpha;
    } else if (right.isAlphaNotPresent()) {
      // ARGB + RGB: preserve ARGB alpha
      resultAlpha = leftAlpha;
    } else {
      // ARGB + ARGB: blend alphas using multiplicative blend for subtraction
      resultAlpha = (long) (leftAlpha * (1.0 - rightAlpha / 255.0));
      if (resultAlpha < 0) {
        resultAlpha = 0; // Clamp to minimum
      }
    }

    // Perform bitwise AND with negated value
    long result = leftValue & (~rightValue) & 0x00FFFFFF;
    return java.lang.String.format("%08X", (resultAlpha << 24) | result);
  }

  // Factory methods

  public static Colour _of() {
    return new Colour();
  }

  public static Colour _of(java.lang.String value) {
    Colour result = new Colour();
    result.parse(value);
    return result;
  }

  public static Colour _of(long hue, double saturation, double lightness) {
    Colour result = new Colour();
    java.lang.String hexValue = result.createFromHSL(0xFF, hue, saturation, lightness);
    result.assign(hexValue);
    return result;
  }

  public static Colour _of(long alpha, long hue, double saturation, double lightness) {
    Colour result = new Colour();
    java.lang.String hexValue = result.createFromHSL(alpha, hue, saturation, lightness);
    result.assign(hexValue);
    return result;
  }
}