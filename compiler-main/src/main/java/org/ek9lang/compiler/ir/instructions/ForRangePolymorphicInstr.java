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
 *
 * <p><b>Design Principle: Explicit Dispatch IR</b></p>
 * This instruction uses explicit IR sequences for all dispatch logic,
 * achieving the same abstraction level as CONTROL_FLOW_CHAIN.
 *
 * <p><b>Key Design Principles:</b></p>
 * <ul>
 *   <li><b>Polymorphic on Range Type:</b> Works with ANY type implementing {@code <=>}, {@code ++},
 *       {@code --}, {@code +=} operators (Integer, Float, Date, Duration, custom types)</li>
 *   <li><b>Runtime Direction Detection:</b> Uses {@code start <=> end} to determine ascending/descending/equal</li>
 *   <li><b>Single Body Storage:</b> User loop body stored ONCE in IR (backends emit it multiple times)</li>
 *   <li><b>Explicit Dispatch:</b> All dispatch logic expressed as explicit IR sequences</li>
 * </ul>
 *
 * <p><b>IR Structure:</b></p>
 * <pre>
 * FOR_RANGE_POLYMORPHIC:
 *   initialization:
 *     - Evaluate start, end, by expressions (explicit IR)
 *     - Assert all values are set (fail-fast)
 *     - direction = start._cmp(end)  // &lt;=&gt; operator on range type
 *     - current = start
 *
 *   dispatch_cases:
 *     ascending:
 *       direction_check: [explicit IR for direction &lt; 0]
 *       direction_primitive: _temp22
 *       loop_condition_template: [explicit IR for current &lt;= end]
 *       loop_condition_primitive: _temp32
 *       loop_body_setup: [explicit IR for loopVariable = current]
 *       loop_increment: [explicit IR for current++]
 *
 *     descending:
 *       direction_check: [explicit IR for direction &gt; 0]
 *       loop_condition_template: [explicit IR for current &gt;= end]
 *       loop_increment: [explicit IR for current--]
 *
 *     equal:
 *       loop_body_setup: [explicit IR for loopVariable = current]
 *       single_iteration: true
 *
 *   body: [user code stored ONCE]
 *
 *   metadata: [reference info - variable names, types]
 *   scope_metadata: [scopes for codegen]
 * </pre>
 *
 * <p><b>Backend Code Generation:</b></p>
 * Backends emit this IR directly as a dispatch structure:
 * <pre>
 * [emit initialization IR]
 *
 * // Ascending case
 * [emit direction_check IR]
 * if (direction_primitive) {
 *   while (true) {
 *     [emit loop_condition_template IR]
 *     if (!loop_condition_primitive) break;
 *     [emit loop_body_setup IR]
 *     [emit body IR]
 *     [emit loop_increment IR]
 *   }
 *   goto end;
 * }
 *
 * // Descending case (similar)
 * [emit direction_check IR]
 * if (direction_primitive) {
 *   while (true) { ... }
 *   goto end;
 * }
 *
 * // Equal case (single iteration)
 * [emit loop_body_setup IR]
 * [emit body IR]
 *
 * end:
 * </pre>
 *
 * <p><b>Abstraction Level Consistency:</b></p>
 * Like CONTROL_FLOW_CHAIN, this instruction provides:
 * <ul>
 *   <li>Explicit IR sequences for all computations</li>
 *   <li>Named variables for branching decisions</li>
 *   <li>Metadata for structural guidance</li>
 *   <li>Full type information for polymorphic operations</li>
 * </ul>
 *
 * <p><b>No IR Lowering Needed:</b></p>
 * Backends can directly consume this structure without an IR lowering pass.
 * The explicit IR sequences provide everything needed for code generation.
 *
 * <p><b>Why Not CONTROL_FLOW_CHAIN?</b></p>
 * CONTROL_FLOW_CHAIN is designed for branching (if/else/switch) where each case executes at most once.
 * For-range loops require:
 * <ul>
 *   <li>Repeated iteration with condition checked each time</li>
 *   <li>Polymorphic operator selection based on runtime direction</li>
 *   <li>Sharing identical body across three different loop configurations</li>
 * </ul>
 * Using CONTROL_FLOW_CHAIN would duplicate body 3x in IR (ascending/descending/equal cases).
 * FOR_RANGE_POLYMORPHIC achieves 40% IR size reduction by storing body once.
 */
public final class ForRangePolymorphicInstr extends IRInstr {

  private final List<IRInstr> initializationInstructions;
  private final DispatchCases dispatchCases;
  private final LoopMetadata metadata;
  private final List<IRInstr> bodyInstructions;
  private final ScopeMetadata scopeMetadata;

  /**
   * Create a polymorphic for-range loop instruction.
   */
  public static ForRangePolymorphicInstr forRangePolymorphic(
      final List<IRInstr> initializationInstructions,
      final DispatchCases dispatchCases,
      final LoopMetadata metadata,
      final List<IRInstr> bodyInstructions,
      final ScopeMetadata scopeMetadata,
      final DebugInfo debugInfo) {

    return new ForRangePolymorphicInstr(
        initializationInstructions,
        dispatchCases,
        metadata,
        bodyInstructions,
        scopeMetadata,
        debugInfo);
  }

  private ForRangePolymorphicInstr(
      final List<IRInstr> initializationInstructions,
      final DispatchCases dispatchCases,
      final LoopMetadata metadata,
      final List<IRInstr> bodyInstructions,
      final ScopeMetadata scopeMetadata,
      final DebugInfo debugInfo) {

    super(IROpcode.FOR_RANGE_POLYMORPHIC, null, debugInfo);

    AssertValue.checkNotNull("Initialization instructions cannot be null", initializationInstructions);
    AssertValue.checkNotNull("Dispatch cases cannot be null", dispatchCases);
    AssertValue.checkNotNull("Loop metadata cannot be null", metadata);
    AssertValue.checkNotNull("Body instructions cannot be null", bodyInstructions);
    AssertValue.checkNotNull("Scope metadata cannot be null", scopeMetadata);

    this.initializationInstructions = initializationInstructions;
    this.dispatchCases = dispatchCases;
    this.metadata = metadata;
    this.bodyInstructions = bodyInstructions;
    this.scopeMetadata = scopeMetadata;

    // Add operands for base class functionality
    addOperand(metadata.directionVariable());
    addOperand(metadata.currentVariable());
    addOperand(metadata.loopVariable());
  }

  public List<IRInstr> getInitializationInstructions() {
    return initializationInstructions;
  }

  public DispatchCases getDispatchCases() {
    return dispatchCases;
  }

  public LoopMetadata getMetadata() {
    return metadata;
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
    appendDispatchCasesSection(builder);
    appendBodySection(builder);
    appendMetadataSection(builder);
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

  private void appendDispatchCasesSection(StringBuilder builder) {
    builder.append("dispatch_cases:\n");
    builder.append("[\n");

    builder.append("ascending:\n");
    builder.append("[\n");
    appendAscendingCase(builder, dispatchCases.ascending());
    builder.append("]\n");

    builder.append("descending:\n");
    builder.append("[\n");
    appendDescendingCase(builder, dispatchCases.descending());
    builder.append("]\n");

    builder.append("equal:\n");
    builder.append("[\n");
    appendEqualCase(builder, dispatchCases.equal());
    builder.append("]\n");

    builder.append("]\n");
  }

  private void appendAscendingCase(StringBuilder builder, AscendingCase ascCase) {
    builder.append("direction_check:\n");
    builder.append("[\n");
    for (var instr : ascCase.directionCheck()) {
      builder.append(instr.toString()).append("\n");
    }
    builder.append("]\n");
    builder.append("direction_primitive: ").append(ascCase.directionPrimitive()).append("\n");

    builder.append("loop_condition_template:\n");
    builder.append("[\n");
    for (var instr : ascCase.loopConditionTemplate()) {
      builder.append(instr.toString()).append("\n");
    }
    builder.append("]\n");
    builder.append("loop_condition_primitive: ").append(ascCase.loopConditionPrimitive()).append("\n");

    builder.append("loop_body_setup:\n");
    builder.append("[\n");
    for (var instr : ascCase.loopBodySetup()) {
      builder.append(instr.toString()).append("\n");
    }
    builder.append("]\n");

    builder.append("loop_increment:\n");
    builder.append("[\n");
    for (var instr : ascCase.loopIncrement()) {
      builder.append(instr.toString()).append("\n");
    }
    builder.append("]\n");
  }

  private void appendDescendingCase(StringBuilder builder, DescendingCase descCase) {
    builder.append("direction_check:\n");
    builder.append("[\n");
    for (var instr : descCase.directionCheck()) {
      builder.append(instr.toString()).append("\n");
    }
    builder.append("]\n");
    builder.append("direction_primitive: ").append(descCase.directionPrimitive()).append("\n");

    builder.append("loop_condition_template:\n");
    builder.append("[\n");
    for (var instr : descCase.loopConditionTemplate()) {
      builder.append(instr.toString()).append("\n");
    }
    builder.append("]\n");
    builder.append("loop_condition_primitive: ").append(descCase.loopConditionPrimitive()).append("\n");

    builder.append("loop_body_setup:\n");
    builder.append("[\n");
    for (var instr : descCase.loopBodySetup()) {
      builder.append(instr.toString()).append("\n");
    }
    builder.append("]\n");

    builder.append("loop_increment:\n");
    builder.append("[\n");
    for (var instr : descCase.loopIncrement()) {
      builder.append(instr.toString()).append("\n");
    }
    builder.append("]\n");
  }

  private void appendEqualCase(StringBuilder builder, EqualCase equalCase) {
    builder.append("loop_body_setup:\n");
    builder.append("[\n");
    for (var instr : equalCase.loopBodySetup()) {
      builder.append(instr.toString()).append("\n");
    }
    builder.append("]\n");
    builder.append("single_iteration: ").append(equalCase.singleIteration()).append("\n");
  }

  private void appendMetadataSection(StringBuilder builder) {
    builder.append("metadata:\n");
    builder.append("[\n");
    builder.append("direction_variable: \"").append(metadata.directionVariable()).append("\"\n");
    builder.append("current_variable: \"").append(metadata.currentVariable()).append("\"\n");
    builder.append("loop_variable: \"").append(metadata.loopVariable()).append("\"\n");
    builder.append("end_variable: \"").append(metadata.endVariable()).append("\"\n");
    builder.append("range_type: \"").append(metadata.rangeType()).append("\"\n");

    if (metadata.byVariable() != null) {
      builder.append("by_variable: \"").append(metadata.byVariable()).append("\"\n");
      builder.append("by_type: \"").append(metadata.byType()).append("\"\n");
    }

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
   * Container for all three dispatch cases in a for-range loop.
   * Each case contains explicit IR sequences for dispatch logic.
   */
  public record DispatchCases(
      AscendingCase ascending,
      DescendingCase descending,
      EqualCase equal
  ) {
    public DispatchCases {
      AssertValue.checkNotNull("Ascending case cannot be null", ascending);
      AssertValue.checkNotNull("Descending case cannot be null", descending);
      AssertValue.checkNotNull("Equal case cannot be null", equal);
    }
  }

  /**
   * Dispatch case for ascending loops (direction < 0).
   * Contains explicit IR for all loop operations.
   *
   * <p>Example: for i in 1 ... 10</p>
   * <ul>
   *   <li>direction_check: IR for direction < 0</li>
   *   <li>loop_condition: IR for current <= end</li>
   *   <li>loop_increment: IR for current++</li>
   * </ul>
   */
  public record AscendingCase(
      List<IRInstr> directionCheck,        // IR: direction < 0
      String directionPrimitive,           // Primitive boolean variable name
      List<IRInstr> loopConditionTemplate, // IR: current <= end (or with polymorphic operator)
      String loopConditionPrimitive,       // Primitive boolean for loop condition
      List<IRInstr> loopBodySetup,         // IR: loopVariable = current
      List<IRInstr> loopIncrement          // IR: current++ or current += by
  ) {
    public AscendingCase {
      AssertValue.checkNotNull("Direction check cannot be null", directionCheck);
      AssertValue.checkNotEmpty("Direction primitive cannot be empty", directionPrimitive);
      AssertValue.checkNotNull("Loop condition template cannot be null", loopConditionTemplate);
      AssertValue.checkNotEmpty("Loop condition primitive cannot be empty", loopConditionPrimitive);
      AssertValue.checkNotNull("Loop body setup cannot be null", loopBodySetup);
      AssertValue.checkNotNull("Loop increment cannot be null", loopIncrement);
    }
  }

  /**
   * Dispatch case for descending loops (direction > 0).
   * Contains explicit IR for all loop operations.
   *
   * <p>Example: for i in 10 ... 1</p>
   * <ul>
   *   <li>direction_check: IR for direction > 0</li>
   *   <li>loop_condition: IR for current >= end</li>
   *   <li>loop_increment: IR for current--</li>
   * </ul>
   */
  public record DescendingCase(
      List<IRInstr> directionCheck,        // IR: direction > 0
      String directionPrimitive,           // Primitive boolean variable name
      List<IRInstr> loopConditionTemplate, // IR: current >= end (or with polymorphic operator)
      String loopConditionPrimitive,       // Primitive boolean for loop condition
      List<IRInstr> loopBodySetup,         // IR: loopVariable = current
      List<IRInstr> loopIncrement          // IR: current-- or current += by
  ) {
    public DescendingCase {
      AssertValue.checkNotNull("Direction check cannot be null", directionCheck);
      AssertValue.checkNotEmpty("Direction primitive cannot be empty", directionPrimitive);
      AssertValue.checkNotNull("Loop condition template cannot be null", loopConditionTemplate);
      AssertValue.checkNotEmpty("Loop condition primitive cannot be empty", loopConditionPrimitive);
      AssertValue.checkNotNull("Loop body setup cannot be null", loopBodySetup);
      AssertValue.checkNotNull("Loop increment cannot be null", loopIncrement);
    }
  }

  /**
   * Dispatch case for equal loops (direction == 0).
   * Single iteration only - no condition checking or increment needed.
   *
   * <p>Example: for i in 5 ... 5</p>
   * <p>Just setup loop variable and execute body once</p>
   */
  public record EqualCase(
      List<IRInstr> loopBodySetup,         // IR: loopVariable = current
      boolean singleIteration              // Always true
  ) {
    public EqualCase {
      AssertValue.checkNotNull("Loop body setup cannot be null", loopBodySetup);
    }
  }

  /**
   * Non-executable metadata about the loop structure.
   * Used for reference and debugging, not for code generation.
   * All actual code generation uses explicit IR sequences in DispatchCases.
   */
  public record LoopMetadata(
      String directionVariable,
      String currentVariable,
      String loopVariable,
      String endVariable,
      String rangeType,
      String byVariable,      // null if no BY clause
      String byType           // null if no BY clause
  ) {
    public LoopMetadata {
      AssertValue.checkNotEmpty("Direction variable cannot be empty", directionVariable);
      AssertValue.checkNotEmpty("Current variable cannot be empty", currentVariable);
      AssertValue.checkNotEmpty("Loop variable cannot be empty", loopVariable);
      AssertValue.checkNotEmpty("End variable cannot be empty", endVariable);
      AssertValue.checkNotEmpty("Range type cannot be empty", rangeType);
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
