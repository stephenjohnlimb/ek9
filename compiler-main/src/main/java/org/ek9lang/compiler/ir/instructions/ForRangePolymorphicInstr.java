package org.ek9lang.compiler.ir.instructions;

import java.util.List;
import org.ek9lang.compiler.ir.IROpcode;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.core.AssertValue;

/**
 * Specialized IR instruction for EK9 polymorphic for-range loops.
 * <p>
 * Handles runtime direction detection and polymorphic operator dispatch for loops like:
 * {@code for i in start ... end [by step]}
 * </p>
 * <p>
 * <b>Key Design Principles:</b>
 * <ul>
 *   <li><b>Polymorphic on Range Type:</b> Works with ANY type implementing {@code <=>}, {@code ++},
 *       {@code --}, {@code +=} operators (Integer, Float, Date, Duration, custom types)</li>
 *   <li><b>Runtime Direction Detection:</b> Uses {@code start <=> end} to determine ascending/descending/equal</li>
 *   <li><b>Single Body Storage:</b> User loop body stored ONCE in IR (backends emit it multiple times)</li>
 *   <li><b>Operator Dispatch:</b> Metadata specifies which operators to use per direction</li>
 * </ul>
 * </p>
 * <p>
 * <b>IR Structure:</b>
 * <pre>
 * FOR_RANGE_POLYMORPHIC:
 *   initialization_instructions:
 *     - Evaluate start, end, by expressions
 *     - Assert all values are set (fail-fast)
 *     - direction = start._cmp(end)  // <=> operator on range type
 *     - current = start
 *
 *   dispatch_metadata:
 *     direction_variable: "_temp12"      // Result of start <=> end (Integer)
 *     current_variable: "_temp13"        // Mutable loop counter (range type)
 *     loop_variable: "i"                 // User's loop variable
 *     range_type: "org.ek9.lang::Integer"
 *
 *     ascending:  { condition_op: "_lteq", increment_op: "_inc" }
 *     descending: { condition_op: "_gteq", increment_op: "_dec" }
 *     equal: { single_iteration: true }
 *
 *   body_instructions: [user code stored ONCE]
 *   scope_metadata: { scopes for codegen }
 * </pre>
 * </p>
 * <p>
 * <b>Backend Code Generation:</b>
 * Backends read this IR and generate:
 * <pre>
 * [initialization instructions]
 * int direction = direction_variable.intValue();
 * if (direction < 0) {
 *   // Ascending loop
 *   while (current._cmp(end)._lteq(0)._true()) {
 *     i = current;
 *     [body instructions - emitted from IR]
 *     current = current._inc();
 *   }
 * } else if (direction > 0) {
 *   // Descending loop
 *   while (current._cmp(end)._gteq(0)._true()) {
 *     i = current;
 *     [body instructions - emitted from IR]
 *     current = current._dec();
 *   }
 * } else {
 *   // Equal - single iteration
 *   i = current;
 *   [body instructions - emitted from IR]
 * }
 * </pre>
 * </p>
 * <p>
 * <b>Why Not CONTROL_FLOW_CHAIN?</b>
 * CONTROL_FLOW_CHAIN is designed for branching (if/else/switch) where each case executes at most once.
 * For-range loops require:
 * - Repeated iteration with condition checked each time
 * - Polymorphic operator selection based on runtime direction
 * - Sharing identical body across three different loop configurations
 * Using CONTROL_FLOW_CHAIN would duplicate body 3x in IR (ascending/descending/equal cases).
 * </p>
 */
public final class ForRangePolymorphicInstr extends IRInstr {

  private final List<IRInstr> initializationInstructions;
  private final DispatchMetadata dispatchMetadata;
  private final List<IRInstr> bodyInstructions;
  private final ScopeMetadata scopeMetadata;

  /**
   * Create a polymorphic for-range loop instruction.
   */
  public static ForRangePolymorphicInstr forRangePolymorphic(
      final List<IRInstr> initializationInstructions,
      final DispatchMetadata dispatchMetadata,
      final List<IRInstr> bodyInstructions,
      final ScopeMetadata scopeMetadata,
      final DebugInfo debugInfo) {

    return new ForRangePolymorphicInstr(
        initializationInstructions,
        dispatchMetadata,
        bodyInstructions,
        scopeMetadata,
        debugInfo);
  }

  private ForRangePolymorphicInstr(
      final List<IRInstr> initializationInstructions,
      final DispatchMetadata dispatchMetadata,
      final List<IRInstr> bodyInstructions,
      final ScopeMetadata scopeMetadata,
      final DebugInfo debugInfo) {

    super(IROpcode.FOR_RANGE_POLYMORPHIC, null, debugInfo);

    AssertValue.checkNotNull("Initialization instructions cannot be null", initializationInstructions);
    AssertValue.checkNotNull("Dispatch metadata cannot be null", dispatchMetadata);
    AssertValue.checkNotNull("Body instructions cannot be null", bodyInstructions);
    AssertValue.checkNotNull("Scope metadata cannot be null", scopeMetadata);

    this.initializationInstructions = initializationInstructions;
    this.dispatchMetadata = dispatchMetadata;
    this.bodyInstructions = bodyInstructions;
    this.scopeMetadata = scopeMetadata;

    // Add operands for base class functionality
    addOperand(dispatchMetadata.directionVariable());
    addOperand(dispatchMetadata.currentVariable());
    addOperand(dispatchMetadata.loopVariable());
  }

  public List<IRInstr> getInitializationInstructions() {
    return initializationInstructions;
  }

  public DispatchMetadata getDispatchMetadata() {
    return dispatchMetadata;
  }

  public List<IRInstr> getBodyInstructions() {
    return bodyInstructions;
  }

  public ScopeMetadata getScopeMetadata() {
    return scopeMetadata;
  }

  @Override
  public String toString() {
    var builder = new StringBuilder();

    appendInstructionHeader(builder);
    builder.append("\n[\n");

    appendInitializationSection(builder);
    appendDispatchMetadataSection(builder);
    appendBodySection(builder);
    appendScopeMetadataSection(builder);

    builder.append("]");

    return builder.toString();
  }

  private void appendInstructionHeader(StringBuilder builder) {
    builder.append(getOpcode().name());
    if (getDebugInfo().isPresent() && getDebugInfo().get().isValidLocation()) {
      builder.append("  ").append(getDebugInfo().get());
    }
  }

  private void appendInitializationSection(StringBuilder builder) {
    if (!initializationInstructions.isEmpty()) {
      builder.append("initialization:\n");
      builder.append("[\n");
      for (var instr : initializationInstructions) {
        builder.append(instr.toString()).append("\n");
      }
      builder.append("]\n");
    }
  }

  private void appendDispatchMetadataSection(StringBuilder builder) {
    builder.append("dispatch_metadata:\n");
    builder.append("[\n");
    builder.append("direction_variable: \"").append(dispatchMetadata.directionVariable()).append("\"\n");
    builder.append("current_variable: \"").append(dispatchMetadata.currentVariable()).append("\"\n");
    builder.append("loop_variable: \"").append(dispatchMetadata.loopVariable()).append("\"\n");
    builder.append("end_variable: \"").append(dispatchMetadata.endVariable()).append("\"\n");
    builder.append("range_type: \"").append(dispatchMetadata.rangeType()).append("\"\n");

    if (dispatchMetadata.byVariable() != null) {
      builder.append("by_variable: \"").append(dispatchMetadata.byVariable()).append("\"\n");
      builder.append("by_type: \"").append(dispatchMetadata.byType()).append("\"\n");
    }

    builder.append("ascending: { condition_op: \"").append(dispatchMetadata.ascending().conditionOperator())
        .append("\", increment_op: \"").append(dispatchMetadata.ascending().incrementOperator()).append("\" }\n");
    builder.append("descending: { condition_op: \"").append(dispatchMetadata.descending().conditionOperator())
        .append("\", increment_op: \"").append(dispatchMetadata.descending().incrementOperator()).append("\" }\n");
    builder.append("equal_single_iteration: ").append(dispatchMetadata.equalSingleIteration()).append("\n");
    builder.append("]\n");
  }

  private void appendBodySection(StringBuilder builder) {
    if (!bodyInstructions.isEmpty()) {
      builder.append("body:\n");
      builder.append("[\n");
      for (var instr : bodyInstructions) {
        builder.append(instr.toString()).append("\n");
      }
      builder.append("]\n");
    }
  }

  private void appendScopeMetadataSection(StringBuilder builder) {
    builder.append("scope_metadata:\n");
    builder.append("[\n");
    builder.append("outer_scope: \"").append(scopeMetadata.outerScopeId()).append("\"\n");
    builder.append("loop_scope: \"").append(scopeMetadata.loopScopeId()).append("\"\n");
    builder.append("body_scope: \"").append(scopeMetadata.bodyScopeId()).append("\"\n");
    builder.append("]\n");
  }

  /**
   * Metadata describing polymorphic operator dispatch for different loop directions.
   * <p>
   * This captures which operators to use based on runtime direction detection.
   * The direction variable contains the result of {@code start <=> end} (Integer).
   * </p>
   *
   * @param directionVariable Variable holding direction result (Integer from {@code <=>})
   * @param currentVariable   Mutable loop counter variable (range type)
   * @param loopVariable      User's loop variable name (e.g., "i")
   * @param endVariable       Loop end value variable (range type)
   * @param rangeType         Fully qualified type name (e.g., "org.ek9.lang::Integer")
   * @param byVariable        Optional BY step variable (null if no BY clause)
   * @param byType            Optional BY step type (null if no BY clause)
   * @param ascending         Dispatch case for ascending direction (direction < 0)
   * @param descending        Dispatch case for descending direction (direction > 0)
   * @param equalSingleIteration Whether equal case (direction == 0) does single iteration
   */
  public record DispatchMetadata(
      String directionVariable,
      String currentVariable,
      String loopVariable,
      String endVariable,
      String rangeType,
      String byVariable,
      String byType,
      DispatchCase ascending,
      DispatchCase descending,
      boolean equalSingleIteration
  ) {
    public DispatchMetadata {
      AssertValue.checkNotEmpty("Direction variable cannot be empty", directionVariable);
      AssertValue.checkNotEmpty("Current variable cannot be empty", currentVariable);
      AssertValue.checkNotEmpty("Loop variable cannot be empty", loopVariable);
      AssertValue.checkNotEmpty("End variable cannot be empty", endVariable);
      AssertValue.checkNotEmpty("Range type cannot be empty", rangeType);
      AssertValue.checkNotNull("Ascending case cannot be null", ascending);
      AssertValue.checkNotNull("Descending case cannot be null", descending);
    }
  }

  /**
   * Describes which polymorphic operators to use for a specific loop direction.
   * <p>
   * For a loop with no BY clause:
   * - Ascending: condition="_lteq" (<=), increment="_inc" (++)
   * - Descending: condition="_gteq" (>=), increment="_dec" (--)
   * </p>
   * <p>
   * For a loop with BY clause:
   * - Both directions: condition="_lteq" or "_gteq", increment="_addAss" (+=)
   * - Sign of BY value determines actual direction
   * </p>
   *
   * @param conditionOperator EK9 operator name for loop condition (e.g., "_lteq", "_gteq")
   * @param incrementOperator EK9 operator name for increment (e.g., "_inc", "_dec", "_addAss")
   */
  public record DispatchCase(
      String conditionOperator,
      String incrementOperator
  ) {
    public DispatchCase {
      AssertValue.checkNotEmpty("Condition operator cannot be empty", conditionOperator);
      AssertValue.checkNotEmpty("Increment operator cannot be empty", incrementOperator);
    }
  }

  /**
   * Scope identifiers for different parts of the loop structure.
   * <p>
   * Scope hierarchy:
   * - Outer scope: Guards and loop setup
   * - Loop scope: Whole loop control structure
   * - Body scope: Per-iteration scope for body execution
   * </p>
   *
   * @param outerScopeId Outer scope for guards (future use)
   * @param loopScopeId  Loop control structure scope
   * @param bodyScopeId  Per-iteration body execution scope
   */
  public record ScopeMetadata(
      String outerScopeId,
      String loopScopeId,
      String bodyScopeId
  ) {
    public ScopeMetadata {
      AssertValue.checkNotEmpty("Outer scope ID cannot be empty", outerScopeId);
      AssertValue.checkNotEmpty("Loop scope ID cannot be empty", loopScopeId);
      AssertValue.checkNotEmpty("Body scope ID cannot be empty", bodyScopeId);
    }
  }
}
