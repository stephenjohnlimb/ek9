package org.ek9.lang;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Utility class for handling dimension unit conversions.
 * Provides conversion factors between compatible measurement units.
 *
 * <p>Supported conversion categories:</p>
 * <ul>
 *   <li><strong>Physical Length:</strong> km, m, cm, mm, mile, in</li>
 *   <li><strong>Typography:</strong> pc, pt (and conversion to physical units)</li>
 *   <li><strong>Context-dependent:</strong> px, em, ex, ch, rem, vw, vh, vmin, vmax, % (no conversions)</li>
 * </ul>
 *
 * <p>Conversions between context-dependent units or from physical to context-dependent
 * units are not supported and will return empty Optional results.</p>
 */
public final class DimensionConversion {

  private static final Set<java.lang.String> PHYSICAL_UNITS
      = Set.of("km", "m", "cm", "mm", "mile", "in");
  private static final Set<java.lang.String> TYPOGRAPHY_UNITS
      = Set.of("pc", "pt");

  private static final Map<java.lang.String, Double> TO_METERS = Map.of(
      "km", 1000.0,
      "m", 1.0,
      "cm", 0.01,
      "mm", 0.001,
      "mile", 1609.34,
      "in", 0.0254,
      "pc", 0.00423333,  // 1 pica = 1/6 inch = 0.00423333 meters
      "pt", 0.000352778  // 1 point = 1/72 inch = 0.000352778 meters
  );

  private DimensionConversion() {
    // Utility class - prevent instantiation
  }

  /**
   * Checks if a conversion is possible between two dimension suffixes.
   *
   * @param fromSuffix source dimension suffix.
   * @param toSuffix   target dimension suffix.
   * @return true if conversion is mathematically deterministic, false otherwise
   */
  public static boolean isConvertible(java.lang.String fromSuffix, java.lang.String toSuffix) {
    if (fromSuffix.equals(toSuffix)) {
      return true;
    }

    // Both units must be convertible (physical or typography)
    boolean fromConvertible = PHYSICAL_UNITS.contains(fromSuffix) || TYPOGRAPHY_UNITS.contains(fromSuffix);
    boolean toConvertible = PHYSICAL_UNITS.contains(toSuffix) || TYPOGRAPHY_UNITS.contains(toSuffix);

    return fromConvertible && toConvertible;
  }

  /**
   * Gets the conversion factor to convert from one dimension suffix to another.
   *
   * @param fromSuffix source dimension suffix
   * @param toSuffix   target dimension suffix
   * @return conversion factor if conversion is possible, empty Optional otherwise
   */
  public static Optional<java.lang.Double> getConversionFactor(java.lang.String fromSuffix, java.lang.String toSuffix) {
    if (!isConvertible(fromSuffix, toSuffix)) {
      return Optional.empty();
    }

    if (fromSuffix.equals(toSuffix)) {
      return Optional.of(1.0);
    }

    Double fromToMeters = TO_METERS.get(fromSuffix);
    Double toToMeters = TO_METERS.get(toSuffix);

    if (fromToMeters == null || toToMeters == null) {
      return Optional.empty();
    }

    // Convert via meters: from -> meters -> to
    double conversionFactor = fromToMeters / toToMeters;
    return Optional.of(conversionFactor);
  }
}