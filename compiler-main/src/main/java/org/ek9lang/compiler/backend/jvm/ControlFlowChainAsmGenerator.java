package org.ek9lang.compiler.backend.jvm;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.instructions.ControlFlowChainInstr;
import org.ek9lang.core.CompilerException;
import org.objectweb.asm.ClassWriter;

/**
 * Main dispatcher for CONTROL_FLOW_CHAIN bytecode generation.
 * Delegates to specialized generators based on chain_type.
 * Follows strategy pattern with lazy instantiation.
 * <p>
 * Supported Control Flow Types:
 * </p>
 * <ul>
 *   <li>QUESTION_OPERATOR - EK9 question operator (?) for null/set checking</li>
 *   <li>NULL_COALESCING_OPERATOR - EK9 null coalescing (??) for null-safe defaults</li>
 *   <li>ELVIS_COALESCING_OPERATOR - EK9 elvis coalescing (:?) for null/unset-safe defaults</li>
 *   <li>LESS_THAN_COALESCING_OPERATOR</li>
 *   <li>GREATER_THAN_COALESCING_OPERATOR</li>
 *   <li>LESS_EQUAL_COALESCING_OPERATOR</li>
 *   <li>GREATER_EQUAL_COALESCING_OPERATOR</li>
 *   <li>GUARDED_ASSIGNMENT - EK9 guarded assignment (:=?) for conditional initialization</li>
 *   <li>IF_ELSE_IF - If/else and if/else-if/else statements</li>
 *   <li>IF_ELSE_WITH_GUARDS - If/else with guard variables (if x &lt;- expr with condition)</li>
 *   <li>WHILE_LOOP - While loop statements</li>
 *   <li>DO_WHILE_LOOP - Do-while loop statements</li>
 *   <li>SWITCH, SWITCH_ENUM - Switch statements (general and enum-optimized)</li>
 *   <li>TRY_CATCH_FINALLY - Try/catch/finally exception handling</li>
 * </ul>
 * <p>
 * NOTE: FOR_RANGE loops use FOR_RANGE_POLYMORPHIC instruction, not CONTROL_FLOW_CHAIN.
 * </p>
 * <p>
 * Architecture Benefits:
 * </p>
 * <ul>
 *   <li>Single responsibility: Each control flow type has dedicated generator</li>
 *   <li>Lazy initialization: Generators created only when needed</li>
 *   <li>Shared logic: Common patterns in AbstractControlFlowAsmGenerator</li>
 *   <li>Extensibility: Easy to add new control flow types</li>
 * </ul>
 */
final class ControlFlowChainAsmGenerator extends AbstractAsmGenerator {

  // Lazily instantiated specialized generators
  private QuestionOperatorAsmGenerator questionOperatorGenerator;
  private NullCoalescingOperatorAsmGenerator nullCoalescingOperatorGenerator;
  private ElvisCoalescingOperatorAsmGenerator elvisCoalescingOperatorGenerator;
  private ComparisonCoalescingOperatorAsmGenerator comparisonCoalescingOperatorGenerator;
  private GuardedAssignmentAsmGenerator guardedAssignmentGenerator;
  private IfElseAsmGenerator ifElseGenerator;
  private WhileLoopAsmGenerator whileLoopGenerator;
  private DoWhileLoopAsmGenerator doWhileLoopGenerator;
  private SwitchAsmGenerator switchGenerator;
  private TryCatchAsmGenerator tryCatchGenerator;
  // Future generators:
  // private ForLoopAsmGenerator forLoopGenerator;

  private final BytecodeGenerationContext context;

  ControlFlowChainAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                               final OutputVisitor outputVisitor,
                               final ClassWriter classWriter,
                               final BytecodeGenerationContext context) {
    super(constructTargetTuple, outputVisitor, classWriter);
    this.context = context;
  }

  /**
   * Dispatch to appropriate generator based on chain type.
   * This is the main entry point called by OutputVisitor.
   *
   * @param instr CONTROL_FLOW_CHAIN instruction to generate bytecode for
   * @throws CompilerException if chain type is not supported
   */
  public void generate(final ControlFlowChainInstr instr) {
    final var chainType = instr.getChainType();

    switch (chainType) {
      case "QUESTION_OPERATOR" -> generateQuestionOperator(instr);
      case "NULL_COALESCING_OPERATOR" -> generateNullCoalescingOperator(instr);
      case "ELVIS_COALESCING_OPERATOR" -> generateElvisCoalescingOperator(instr);
      case "LESS_THAN_COALESCING_OPERATOR", "GREATER_THAN_COALESCING_OPERATOR",
          "LESS_EQUAL_COALESCING_OPERATOR", "GREATER_EQUAL_COALESCING_OPERATOR" ->
          generateComparisonCoalescingOperator(instr);
      case "GUARDED_ASSIGNMENT" -> generateGuardedAssignment(instr);
      case "IF_ELSE_IF", "IF_ELSE_WITH_GUARDS" -> generateIfElse(instr);
      case "WHILE_LOOP" -> generateWhileLoop(instr);
      case "DO_WHILE_LOOP" -> generateDoWhileLoop(instr);
      case "SWITCH", "SWITCH_ENUM" -> generateSwitch(instr);
      case "TRY_CATCH_FINALLY" -> generateTryCatch(instr);
      // Future cases will be added here as generators are implemented:
      // case "FOR" -> generateForLoop(instr);
      default -> throw new CompilerException(
          "Unsupported control flow chain type: " + chainType);
    }
  }

  /**
   * Generate bytecode for question operator (?).
   * Lazily instantiates QuestionOperatorAsmGenerator on first use.
   *
   * @param instr CONTROL_FLOW_CHAIN instruction with QUESTION_OPERATOR type
   */
  private void generateQuestionOperator(final ControlFlowChainInstr instr) {
    if (questionOperatorGenerator == null) {
      questionOperatorGenerator = new QuestionOperatorAsmGenerator(
          constructTargetTuple, outputVisitor, classWriter, context);
    }
    // Always update context in case method changed
    questionOperatorGenerator.setSharedMethodContext(getMethodContext());
    questionOperatorGenerator.setCurrentMethodVisitor(getCurrentMethodVisitor());
    questionOperatorGenerator.generate(instr);
  }

  /**
   * Generate bytecode for null coalescing operator (??).
   * Lazily instantiates NullCoalescingOperatorAsmGenerator on first use.
   *
   * @param instr CONTROL_FLOW_CHAIN instruction with NULL_COALESCING_OPERATOR type
   */
  private void generateNullCoalescingOperator(final ControlFlowChainInstr instr) {
    if (nullCoalescingOperatorGenerator == null) {
      nullCoalescingOperatorGenerator = new NullCoalescingOperatorAsmGenerator(
          constructTargetTuple, outputVisitor, classWriter, context);
    }
    // Always update context in case method changed
    nullCoalescingOperatorGenerator.setSharedMethodContext(getMethodContext());
    nullCoalescingOperatorGenerator.setCurrentMethodVisitor(getCurrentMethodVisitor());
    nullCoalescingOperatorGenerator.generate(instr);
  }

  /**
   * Generate bytecode for Elvis coalescing operator (:?).
   * Lazily instantiates ElvisCoalescingOperatorAsmGenerator on first use.
   *
   * @param instr CONTROL_FLOW_CHAIN instruction with ELVIS_COALESCING_OPERATOR type
   */
  private void generateElvisCoalescingOperator(final ControlFlowChainInstr instr) {
    if (elvisCoalescingOperatorGenerator == null) {
      elvisCoalescingOperatorGenerator = new ElvisCoalescingOperatorAsmGenerator(
          constructTargetTuple, outputVisitor, classWriter, context);
    }
    // Always update context in case method changed
    elvisCoalescingOperatorGenerator.setSharedMethodContext(getMethodContext());
    elvisCoalescingOperatorGenerator.setCurrentMethodVisitor(getCurrentMethodVisitor());
    elvisCoalescingOperatorGenerator.generate(instr);
  }

  /**
   * Generate bytecode for comparison coalescing operators ({@code <?, >?, <=?, >=?}).
   * Lazily instantiates ComparisonCoalescingOperatorAsmGenerator on first use.
   *
   * @param instr CONTROL_FLOW_CHAIN instruction with comparison coalescing type
   */
  private void generateComparisonCoalescingOperator(final ControlFlowChainInstr instr) {
    if (comparisonCoalescingOperatorGenerator == null) {
      comparisonCoalescingOperatorGenerator = new ComparisonCoalescingOperatorAsmGenerator(
          constructTargetTuple, outputVisitor, classWriter, context);
    }
    // Always update context in case method changed
    comparisonCoalescingOperatorGenerator.setSharedMethodContext(getMethodContext());
    comparisonCoalescingOperatorGenerator.setCurrentMethodVisitor(getCurrentMethodVisitor());
    comparisonCoalescingOperatorGenerator.generate(instr);
  }

  /**
   * Generate bytecode for guarded assignment (:=?).
   * Lazily instantiates GuardedAssignmentAsmGenerator on first use.
   *
   * @param instr CONTROL_FLOW_CHAIN instruction with GUARDED_ASSIGNMENT type
   */
  private void generateGuardedAssignment(final ControlFlowChainInstr instr) {
    if (guardedAssignmentGenerator == null) {
      guardedAssignmentGenerator = new GuardedAssignmentAsmGenerator(
          constructTargetTuple, outputVisitor, classWriter, context);
    }
    // Always update context in case method changed
    guardedAssignmentGenerator.setSharedMethodContext(getMethodContext());
    guardedAssignmentGenerator.setCurrentMethodVisitor(getCurrentMethodVisitor());
    guardedAssignmentGenerator.generate(instr);
  }

  /**
   * Generate bytecode for if/else statements.
   * Lazily instantiates IfElseAsmGenerator on first use.
   *
   * @param instr CONTROL_FLOW_CHAIN instruction with IF_ELSE type
   */
  private void generateIfElse(final ControlFlowChainInstr instr) {
    if (ifElseGenerator == null) {
      ifElseGenerator = new IfElseAsmGenerator(
          constructTargetTuple, outputVisitor, classWriter, context);
    }
    // Always update context in case method changed
    ifElseGenerator.setSharedMethodContext(getMethodContext());
    ifElseGenerator.setCurrentMethodVisitor(getCurrentMethodVisitor());
    ifElseGenerator.generate(instr);
  }

  /**
   * Generate bytecode for while loops.
   * Lazily instantiates WhileLoopAsmGenerator on first use.
   *
   * @param instr CONTROL_FLOW_CHAIN instruction with WHILE_LOOP type
   */
  private void generateWhileLoop(final ControlFlowChainInstr instr) {
    if (whileLoopGenerator == null) {
      whileLoopGenerator = new WhileLoopAsmGenerator(
          constructTargetTuple, outputVisitor, classWriter, context);
    }
    // Always update context in case method changed
    whileLoopGenerator.setSharedMethodContext(getMethodContext());
    whileLoopGenerator.setCurrentMethodVisitor(getCurrentMethodVisitor());
    whileLoopGenerator.generate(instr);
  }

  /**
   * Generate bytecode for do-while loops.
   * Lazily instantiates DoWhileLoopAsmGenerator on first use.
   *
   * @param instr CONTROL_FLOW_CHAIN instruction with DO_WHILE_LOOP type
   */
  private void generateDoWhileLoop(final ControlFlowChainInstr instr) {
    if (doWhileLoopGenerator == null) {
      doWhileLoopGenerator = new DoWhileLoopAsmGenerator(
          constructTargetTuple, outputVisitor, classWriter, context);
    }
    // Always update context in case method changed
    doWhileLoopGenerator.setSharedMethodContext(getMethodContext());
    doWhileLoopGenerator.setCurrentMethodVisitor(getCurrentMethodVisitor());
    doWhileLoopGenerator.generate(instr);
  }

  /**
   * Generate bytecode for switch statements.
   * Lazily instantiates SwitchAsmGenerator on first use.
   *
   * @param instr CONTROL_FLOW_CHAIN instruction with SWITCH or SWITCH_ENUM type
   */
  private void generateSwitch(final ControlFlowChainInstr instr) {
    if (switchGenerator == null) {
      switchGenerator = new SwitchAsmGenerator(
          constructTargetTuple, outputVisitor, classWriter, context);
    }
    // Always update context in case method changed
    switchGenerator.setSharedMethodContext(getMethodContext());
    switchGenerator.setCurrentMethodVisitor(getCurrentMethodVisitor());
    switchGenerator.generate(instr);
  }

  /**
   * Generate bytecode for try/catch/finally statements.
   * Lazily instantiates TryCatchAsmGenerator on first use.
   *
   * @param instr CONTROL_FLOW_CHAIN instruction with TRY_CATCH_FINALLY type
   */
  private void generateTryCatch(final ControlFlowChainInstr instr) {
    if (tryCatchGenerator == null) {
      tryCatchGenerator = new TryCatchAsmGenerator(
          constructTargetTuple, outputVisitor, classWriter, context);
    }
    // Always update context in case method changed
    tryCatchGenerator.setSharedMethodContext(getMethodContext());
    tryCatchGenerator.setCurrentMethodVisitor(getCurrentMethodVisitor());
    tryCatchGenerator.generate(instr);
  }

  /**
   * Update method context for all generators.
   * Called when OutputVisitor.setMethodContext() is invoked.
   * Propagates context to already-instantiated generators.
   *
   * @param context Shared method context with variable/label maps
   */
  @Override
  public void setSharedMethodContext(MethodContext context) {
    super.setSharedMethodContext(context);

    // Propagate to already-instantiated generators
    if (questionOperatorGenerator != null) {
      questionOperatorGenerator.setSharedMethodContext(context);
    }
    if (nullCoalescingOperatorGenerator != null) {
      nullCoalescingOperatorGenerator.setSharedMethodContext(context);
    }
    if (elvisCoalescingOperatorGenerator != null) {
      elvisCoalescingOperatorGenerator.setSharedMethodContext(context);
    }
    if (guardedAssignmentGenerator != null) {
      guardedAssignmentGenerator.setSharedMethodContext(context);
    }
    if (ifElseGenerator != null) {
      ifElseGenerator.setSharedMethodContext(context);
    }
    if (whileLoopGenerator != null) {
      whileLoopGenerator.setSharedMethodContext(context);
    }
    if (doWhileLoopGenerator != null) {
      doWhileLoopGenerator.setSharedMethodContext(context);
    }
    if (switchGenerator != null) {
      switchGenerator.setSharedMethodContext(context);
    }
    if (tryCatchGenerator != null) {
      tryCatchGenerator.setSharedMethodContext(context);
    }
    // Future generators will be added here
  }

  /**
   * Update method visitor for all generators.
   * Called when OutputVisitor.setMethodContext() is invoked.
   * Propagates method visitor to already-instantiated generators.
   *
   * @param mv ASM MethodVisitor for current method being generated
   */
  @Override
  public void setCurrentMethodVisitor(org.objectweb.asm.MethodVisitor mv) {
    super.setCurrentMethodVisitor(mv);

    // Propagate to already-instantiated generators
    if (questionOperatorGenerator != null) {
      questionOperatorGenerator.setCurrentMethodVisitor(mv);
    }
    if (nullCoalescingOperatorGenerator != null) {
      nullCoalescingOperatorGenerator.setCurrentMethodVisitor(mv);
    }
    if (elvisCoalescingOperatorGenerator != null) {
      elvisCoalescingOperatorGenerator.setCurrentMethodVisitor(mv);
    }
    if (guardedAssignmentGenerator != null) {
      guardedAssignmentGenerator.setCurrentMethodVisitor(mv);
    }
    if (ifElseGenerator != null) {
      ifElseGenerator.setCurrentMethodVisitor(mv);
    }
    if (whileLoopGenerator != null) {
      whileLoopGenerator.setCurrentMethodVisitor(mv);
    }
    if (doWhileLoopGenerator != null) {
      doWhileLoopGenerator.setCurrentMethodVisitor(mv);
    }
    if (switchGenerator != null) {
      switchGenerator.setCurrentMethodVisitor(mv);
    }
    if (tryCatchGenerator != null) {
      tryCatchGenerator.setCurrentMethodVisitor(mv);
    }
    // Future generators will be added here
  }
}
