package org.ek9lang.compiler.backend.jvm;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.data.ConditionCaseDetails;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

/**
 * Abstract base class for control flow bytecode generators.
 * Provides common patterns for:
 * - Label creation with consistent naming (using scopeId for uniqueness)
 * - Condition evaluation processing
 * - Result variable copying
 * - Stack frame validation helpers
 * - Branch instruction helpers (branchIfTrue, branchIfFalse)
 * <p>
 * All control flow generators extend this class to reuse common logic and ensure
 * consistent stack frame handling:
 * </p>
 * <ul>
 *   <li>Operators: QuestionOperatorAsmGenerator, NullCoalescingOperatorAsmGenerator,
 *       ElvisCoalescingOperatorAsmGenerator, GuardedAssignmentAsmGenerator</li>
 *   <li>Statements: IfElseAsmGenerator, SwitchAsmGenerator</li>
 *   <li>Loops: WhileLoopAsmGenerator, DoWhileLoopAsmGenerator, ForRangePolymorphicAsmGenerator</li>
 *   <li>Exception Handling: TryCatchAsmGenerator</li>
 *   <li>Logical Operations: LogicalOperationAsmGenerator (AND/OR short-circuit)</li>
 * </ul>
 * <p>
 * Stack Frame Invariant: All helper methods maintain the invariant that the JVM
 * operand stack is empty before and after their execution. Results are always
 * stored in local variables, never left on the stack. This ensures correct
 * stack frame balancing at control flow merge points.
 * </p>
 * <h2>Label Naming Convention (MANDATORY)</h2>
 * <p>
 * ALL control flow generators MUST use {@code scopeId} from IR instructions for label uniqueness.
 * <b>NEVER</b> use result variables, loop variable names, or any other identifiers for labels.
 * </p>
 * <pre>
 * // CORRECT - uses scopeId from IR instruction
 * final var label = createControlFlowLabel("while_start", instr.getScopeId());
 * final var label = createControlFlowLabel("for_asc", scopeMetadata.loopScopeId());
 * final var label = createControlFlowLabel("if_next", conditionCase.caseScopeId());
 *
 * // WRONG - uses result variable (fragile, inconsistent)
 * final var label = createControlFlowLabel("prefix", instr.getResult());
 *
 * // WRONG - uses variable name (collides in nested loops)
 * final var label = createControlFlowLabel("for_asc", loopVariableName);
 * </pre>
 * <p><b>Rationale:</b></p>
 * <ul>
 *   <li><b>Guaranteed Uniqueness:</b> Scope IDs are globally unique per method by IR generator contract</li>
 *   <li><b>Semantic Clarity:</b> Scope IDs represent lexical structure, not operational details</li>
 *   <li><b>Consistency:</b> All loop and conditional generators use this pattern</li>
 *   <li><b>Robustness:</b> Immune to IR optimizations that might reuse temp variables</li>
 *   <li><b>Nested Safety:</b> Each nested scope gets unique ID, preventing label collisions</li>
 * </ul>
 * <p><b>For Nested Constructs:</b></p>
 * <ul>
 *   <li>Nested loops with same variable name (e.g., "for i" inside "for i"): Each has unique loopScopeId</li>
 *   <li>Nested conditions: Each branch has unique caseScopeId</li>
 *   <li>No collisions possible, even with identical variable names</li>
 * </ul>
 * <p>
 * See {@code LABEL_NAMING_CONVENTION.md} for complete documentation and examples.
 * </p>
 */
abstract class AbstractControlFlowAsmGenerator extends AbstractAsmGenerator {

  /**
   * Context stack for nested control flow.
   * Allows nested generators to query enclosing control flow context.
   */
  protected final BytecodeGenerationContext context;

  protected AbstractControlFlowAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                                            final OutputVisitor outputVisitor,
                                            final ClassWriter classWriter,
                                            final BytecodeGenerationContext context) {
    super(constructTargetTuple, outputVisitor, classWriter);
    this.context = context;
  }

  /**
   * Generate unique label for control flow construct.
   * Pattern: prefix_[dispatchCase_]scopeId
   * <p>
   * <b>CRITICAL:</b> Always use {@code scopeId} as the uniqueId parameter.
   * </p>
   * <p>
   * <b>FOR_RANGE Dispatch:</b> When inside FOR_RANGE dispatch case, labels include
   * dispatch case suffix to ensure uniqueness when same body IR is processed three times:
   * </p>
   * <pre>
   * // Outside FOR_RANGE: if_next_scope_5
   * // Equal case:       if_next_equal_scope_5
   * // Ascending case:   if_next_ascending_scope_5
   * // Descending case:  if_next_descending_scope_5
   * </pre>
   * <pre>
   * // CORRECT - main control flow structure
   * createControlFlowLabel("while_start", instr.getScopeId());
   * createControlFlowLabel("for_asc", scopeMetadata.loopScopeId());
   *
   * // CORRECT - condition cases
   * createControlFlowLabel("if_next", conditionCase.caseScopeId());
   *
   * // WRONG - uses result variable (fragile)
   * createControlFlowLabel("prefix", instr.getResult());
   *
   * // WRONG - uses variable name (collides in nested loops)
   * createControlFlowLabel("for_asc", loopVariableName);
   * </pre>
   * <p><b>Why scopeId?</b></p>
   * <ul>
   *   <li>Globally unique per method (guaranteed by IR generator)</li>
   *   <li>Semantically appropriate (represents lexical scope)</li>
   *   <li>Consistent across all control flow generators</li>
   *   <li>Robust to IR refactoring (e.g., temp variable reuse)</li>
   *   <li>Safe for nested constructs with same variable names</li>
   * </ul>
   *
   * @param prefix   Label prefix identifying the construct type (e.g., "while_start", "if_end", "for_asc")
   * @param uniqueId MUST be scopeId from IR instruction (never use result variable or variable name)
   * @return JVM Label for bytecode generation
   */
  protected Label createControlFlowLabel(final String prefix, final String uniqueId) {
    final StringBuilder labelName = new StringBuilder(prefix).append("_");

    // Include dispatch case if inside FOR_RANGE dispatch (makes labels unique per case)
    context.getCurrentDispatchCase()
        .ifPresent(dispatchCase -> labelName.append(dispatchCase.toString().toLowerCase()).append("_"));

    labelName.append(uniqueId);
    return getOrCreateLabel(labelName.toString());
  }

  /**
   * Process list of IR instructions via recursive delegation to OutputVisitor.
   * <p>
   * Each instruction knows how to generate its own bytecode via accept(visitor).
   * This enables polymorphic dispatch to the correct generator:
   * </p>
   * <ul>
   *   <li>CALL instructions → CallInstrAsmGenerator</li>
   *   <li>LOAD/STORE/RETAIN/RELEASE → MemoryInstrAsmGenerator</li>
   *   <li>LOAD_LITERAL → LiteralInstrAsmGenerator</li>
   *   <li>ASSERT → BranchInstrAsmGenerator</li>
   *   <li>SCOPE_ENTER/EXIT → ScopeInstrAsmGenerator</li>
   *   <li>CONTROL_FLOW_CHAIN → ControlFlowChainAsmGenerator</li>
   * </ul>
   * <p>
   * All instructions maintain stack-empty invariant.
   * </p>
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty (all results in local variables)
   * </p>
   *
   * @param instructions List of IR instructions to process
   */
  protected void processInstructions(final java.util.List<IRInstr> instructions) {
    for (var instr : instructions) {
      instr.accept(outputVisitor);  // Recursive delegation - polymorphic dispatch to correct generator
      // Each instruction maintains stack-empty invariant
    }
    // Post-condition: stack is empty (guaranteed by all IR instruction generators)
  }

  /**
   * Process condition evaluation instructions via recursive visiting.
   * Semantic wrapper around {@link #processInstructions(java.util.List)} for clarity.
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty (all results in local variables)
   * </p>
   *
   * @param conditionEvaluation List of IR instructions to evaluate condition
   */
  protected void processConditionEvaluation(final java.util.List<IRInstr> conditionEvaluation) {
    processInstructions(conditionEvaluation);
  }

  /**
   * Process body evaluation instructions via recursive visiting.
   * Semantic wrapper around {@link #processInstructions(java.util.List)} for clarity.
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty (all results in local variables)
   * </p>
   *
   * @param bodyEvaluation List of IR instructions for case/branch body
   */
  protected void processBodyEvaluation(final java.util.List<IRInstr> bodyEvaluation) {
    processInstructions(bodyEvaluation);
  }

  /**
   * Load primitive condition (int) and branch if false (0).
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty (IFEQ consumes the int)
   * </p>
   * <p>
   * Control Flow: If condition is 0 (false), jumps to falseLabel.
   * Otherwise, execution continues to next instruction.
   * </p>
   *
   * @param primitiveCondition Variable name holding primitive int (0 or 1)
   * @param falseLabel         Label to jump to if condition is false (0)
   */
  protected void branchIfFalse(final String primitiveCondition, final Label falseLabel) {
    final var conditionIndex = getVariableIndex(primitiveCondition);
    getCurrentMethodVisitor().visitVarInsn(Opcodes.ILOAD, conditionIndex);  // stack: [int]
    getCurrentMethodVisitor().visitJumpInsn(Opcodes.IFEQ, falseLabel);  // stack: [] (consumed by IFEQ)
    // Post-condition: stack is empty
  }

  /**
   * Load primitive condition (int) and branch if true (non-zero).
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty (IFNE consumes the int)
   * </p>
   * <p>
   * Control Flow: If condition is non-zero (true), jumps to trueLabel.
   * Otherwise, execution continues to next instruction.
   * </p>
   * <p>
   * Used by logical OR operator for short-circuit optimization:
   * If left operand is true, skip right evaluation and return left.
   * </p>
   *
   * @param primitiveCondition Variable name holding primitive int (0 or 1)
   * @param trueLabel          Label to jump to if condition is true (non-zero)
   */
  protected void branchIfTrue(final String primitiveCondition, final Label trueLabel) {
    final var conditionIndex = getVariableIndex(primitiveCondition);
    getCurrentMethodVisitor().visitVarInsn(Opcodes.ILOAD, conditionIndex);  // stack: [int]
    getCurrentMethodVisitor().visitJumpInsn(Opcodes.IFNE, trueLabel);  // stack: [] (consumed by IFNE)
    // Post-condition: stack is empty
  }

  /**
   * Copy result from source variable to destination variable (object reference).
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty
   * </p>
   * <p>
   * Bytecode: ALOAD source_index, ASTORE dest_index
   * </p>
   *
   * @param sourceVar Variable holding the result to copy
   * @param destVar   Variable to store the result into
   */
  protected void copyResultVariable(final String sourceVar, final String destVar) {
    final var sourceIndex = getVariableIndex(sourceVar);
    final var destIndex = getVariableIndex(destVar);
    getCurrentMethodVisitor().visitVarInsn(Opcodes.ALOAD, sourceIndex);  // stack: [obj]
    getCurrentMethodVisitor().visitVarInsn(Opcodes.ASTORE, destIndex);  // stack: []
    // Post-condition: stack is empty
  }

  /**
   * Generate unconditional jump to label.
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: control transferred (stack irrelevant after GOTO)
   * </p>
   * <p>
   * Control Flow: Execution continues at the target label.
   * Stack at target must match stack at GOTO (empty in our convention).
   * </p>
   *
   * @param label Target label for unconditional jump
   */
  protected void jumpTo(final Label label) {
    getCurrentMethodVisitor().visitJumpInsn(Opcodes.GOTO, label);
    // Post-condition: control transferred, stack must be empty at target
  }

  /**
   * Place label at current bytecode position.
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack state depends on incoming control flow paths
   * Post-condition: stack state unchanged (label is just a position marker)
   * </p>
   * <p>
   * CRITICAL: All control flow paths reaching this label MUST have the same
   * stack depth. In our convention, all paths arrive with empty stack.
   * </p>
   *
   * @param label Label to place at current position
   */
  protected void placeLabel(final Label label) {
    getCurrentMethodVisitor().visitLabel(label);
    // Stack state: must be consistent across all incoming paths
  }

  /**
   * Process a condition case: evaluate condition, branch, execute body.
   * Common pattern used by question operator, if/else, switch.
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty at both nextCaseLabel and after GOTO
   * </p>
   * <p>
   * Control Flow:
   * 1. Evaluate condition → result in variable, stack empty
   * 2. Branch to nextCaseLabel if false → stack empty at label
   * 3. Execute body → result in variable, stack empty
   * 4. Copy result to overall result → stack empty
   * 5. Jump to endLabel → stack empty at label
   * 6. Place nextCaseLabel → stack empty (from branch in step 2)
   * </p>
   *
   * @param conditionCase IR details for this case
   * @param nextCaseLabel Label to jump if condition is false
   * @param endLabel      Label to jump after body execution
   * @param resultVar     Overall result variable (null if no result)
   */
  protected void processConditionCase(final ConditionCaseDetails conditionCase,
                                      final Label nextCaseLabel,
                                      final Label endLabel,
                                      final String resultVar) {
    processConditionCaseWithoutLabelPlacement(conditionCase, nextCaseLabel, endLabel, resultVar);

    // 6. Place next case label (reached via branch from step 2 with empty stack)
    placeLabel(nextCaseLabel);
    // Stack: empty (from incoming branch)
  }

  /**
   * Process a condition case WITHOUT placing the nextCaseLabel.
   * Use this when the caller needs to control label placement (e.g., if/else-if chains).
   * <p>
   * Stack Frame Invariant:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty after GOTO
   * </p>
   *
   * @param conditionCase IR details for this case
   * @param nextCaseLabel Label to jump if condition is false
   * @param endLabel      Label to jump after body execution
   * @param resultVar     Overall result variable (null if no result)
   */
  protected void processConditionCaseWithoutLabelPlacement(final ConditionCaseDetails conditionCase,
                                                           final Label nextCaseLabel,
                                                           final Label endLabel,
                                                           final String resultVar) {
    // 1. Evaluate condition (leaves stack empty)
    processConditionEvaluation(conditionCase.conditionEvaluation());
    // Stack: empty

    // 2. Branch if condition is false
    branchIfFalse(conditionCase.primitiveCondition(), nextCaseLabel);
    // Stack: empty (both paths - continue or branch)

    // 3. Execute body (leaves stack empty)
    processBodyEvaluation(conditionCase.bodyEvaluation());
    // Stack: empty

    // 4. Copy body result to overall result (if exists)
    if (resultVar != null && conditionCase.bodyResult() != null) {
      copyResultVariable(conditionCase.bodyResult(), resultVar);
      // Stack: empty
    }

    // 5. Jump to end
    jumpTo(endLabel);
    // Stack: empty at endLabel
    // NOTE: nextCaseLabel is NOT placed by this method - caller must place it
  }
}
