package org.ek9lang.compiler.ir;

import java.util.List;

/**
 * Record containing optimization information for enum-based switch statements.
 * <p>
 * Provides backends with comprehensive metadata to enable aggressive optimizations:
 * - Jump table generation for dense enum ranges
 * - Binary search for sparse enum cases
 * - Bounds checking optimization for exhaustive switches
 * - Dead code elimination for unreachable cases
 * </p>
 * <p>
 * This information enables backends to choose optimal code generation strategies
 * while preserving the sequential evaluation semantics of EK9 switch statements.
 * </p>
 */
public record EnumOptimizationInfo(
    /*
     * Fully qualified type name of the enum being switched on.
     * Example: "com.example.project::Color"
     */
    String enumType,

    /*
     * List of enum constant names being handled in this switch.
     * Example: ["RED", "GREEN", "BLUE"]
     * Ordered by appearance in the switch statement.
     */
    List<String> enumValues,

    /*
     * List of enum ordinal values corresponding to enumValues.
     * Example: [0, 1, 2]
     * Used for jump table generation and bounds checking.
     */
    List<Integer> enumOrdinals,

    /*
     * Whether this switch covers all possible enum values.
     * true: No default case needed, all enum values handled
     * false: Default case present or some enum values missing
     */
    boolean isExhaustive,

    /*
     * Whether the enum ordinals form a dense range suitable for jump tables.
     * true: Ordinals are consecutive (or nearly so), jump table beneficial
     * false: Sparse ordinals, sequential evaluation or binary search better
     */
    boolean isDense
) {

  /**
   * Create enum optimization info for a dense, exhaustive switch.
   * Optimal case for jump table generation.
   */
  public static EnumOptimizationInfo createDenseExhaustive(
      String enumType,
      List<String> enumValues,
      List<Integer> enumOrdinals) {

    return new EnumOptimizationInfo(
        enumType,
        enumValues,
        enumOrdinals,
        true,  // exhaustive
        true   // dense
    );
  }

  /**
   * Create enum optimization info for a sparse switch with default case.
   * Better suited for sequential evaluation or binary search.
   */
  public static EnumOptimizationInfo createSparseWithDefault(
      String enumType,
      List<String> enumValues,
      List<Integer> enumOrdinals) {

    return new EnumOptimizationInfo(
        enumType,
        enumValues,
        enumOrdinals,
        false, // not exhaustive (has default)
        false  // sparse
    );
  }

  /**
   * Create enum optimization info for a dense switch with default case.
   * Could benefit from jump table with bounds check for default.
   */
  public static EnumOptimizationInfo createDenseWithDefault(
      String enumType,
      List<String> enumValues,
      List<Integer> enumOrdinals) {

    return new EnumOptimizationInfo(
        enumType,
        enumValues,
        enumOrdinals,
        false, // not exhaustive (has default)
        true   // dense - still beneficial for jump table
    );
  }

  /**
   * Determine if this enum switch is suitable for jump table optimization.
   * Considers both density and the number of cases.
   */
  public boolean isJumpTableCandidate() {
    // Jump tables beneficial for dense ranges with multiple cases
    return isDense && enumOrdinals.size() >= 3;
  }

  /**
   * Calculate the density ratio of this enum switch.
   * Returns percentage of ordinal range that is covered by cases.
   */
  public double getDensityRatio() {
    if (enumOrdinals.isEmpty()) {
      return 0.0;
    }

    int min = enumOrdinals.stream().min(Integer::compare).orElse(0);
    int max = enumOrdinals.stream().max(Integer::compare).orElse(0);
    int range = max - min + 1;

    return (double) enumOrdinals.size() / range;
  }

  /**
   * Get the minimum ordinal value in this switch.
   */
  public int getMinOrdinal() {
    return enumOrdinals.stream().min(Integer::compare).orElse(0);
  }

  /**
   * Get the maximum ordinal value in this switch.
   */
  public int getMaxOrdinal() {
    return enumOrdinals.stream().max(Integer::compare).orElse(0);
  }

  /**
   * Get the ordinal range span (max - min + 1).
   */
  public int getOrdinalRange() {
    if (enumOrdinals.isEmpty()) {
      return 0;
    }
    return getMaxOrdinal() - getMinOrdinal() + 1;
  }

  /**
   * IR-optimized toString following EK9's bracket-only, no-indentation format.
   */
  @Override
  public String toString() {
    var builder = new StringBuilder("[");
    builder.append("enum_type: \"").append(enumType).append("\"");
    builder.append("enum_values: ").append(enumValues);
    builder.append("enum_ordinals: ").append(enumOrdinals);
    builder.append("is_exhaustive: ").append(isExhaustive);
    builder.append("is_dense: ").append(isDense);
    return builder.append("]").toString();
  }
}