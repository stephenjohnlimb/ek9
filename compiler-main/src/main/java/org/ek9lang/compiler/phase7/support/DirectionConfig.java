package org.ek9lang.compiler.phase7.support;

/**
 * Configuration for for-range loop direction (ascending vs descending).
 * <p>
 * CONCERN: Explicit operator triplet configuration for symmetric for-range cases.
 * RESPONSIBILITY: Encapsulate the ONLY differences between ascending/descending loops.
 * REUSABILITY: FOR_RANGE_POLYMORPHIC case generation.
 * </p>
 * <p>
 * The ascending and descending cases in for-range loops differ ONLY in 3 operators:
 * </p>
 * <ul>
 *   <li>Direction check: "&lt;" (ascending) vs "&gt;" (descending)</li>
 *   <li>Loop condition: "&lt;=" (ascending) vs "&gt;=" (descending)</li>
 *   <li>Increment: "++" (ascending) vs "--" (descending)</li>
 * </ul>
 * <p>
 * This record makes the symmetric relationship <b>explicit in the type system</b>
 * instead of implicit in duplicated code.
 * </p>
 * <p>
 * <b>Example:</b>
 * </p>
 * <pre>
 * for i in 1 ... 10:    // Ascending: 1 &lt; 10, use &lt;=, ++
 * for i in 10 ... 1:    // Descending: 10 &gt; 1, use &gt;=, --
 * </pre>
 *
 * @param directionOperator Operator for runtime direction detection ("&lt;" or "&gt;")
 * @param conditionOperator Operator for loop continuation check ("&lt;=" or "&gt;=")
 * @param incrementOperator Operator for loop counter update ("++" or "--")
 */
public record DirectionConfig(
    String directionOperator,
    String conditionOperator,
    String incrementOperator
) {

  /**
   * Configuration for ascending for-range loops.
   * <p>
   * Example: {@code for i in 1 ... 10}
   * </p>
   * <ul>
   *   <li>Direction: start &lt; end → direction &lt; 0</li>
   *   <li>Condition: current &lt;= end</li>
   *   <li>Increment: current++</li>
   * </ul>
   */
  public static DirectionConfig ascending() {
    return new DirectionConfig("<", "<=", "++");
  }

  /**
   * Configuration for descending for-range loops.
   * <p>
   * Example: {@code for i in 10 ... 1}
   * </p>
   * <ul>
   *   <li>Direction: start &gt; end → direction &gt; 0</li>
   *   <li>Condition: current &gt;= end</li>
   *   <li>Increment: current--</li>
   * </ul>
   */
  public static DirectionConfig descending() {
    return new DirectionConfig(">", ">=", "--");
  }
}
