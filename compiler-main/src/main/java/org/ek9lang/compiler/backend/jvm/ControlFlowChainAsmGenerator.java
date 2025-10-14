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
 *   <li>GUARDED_ASSIGNMENT - EK9 guarded assignment (:=?) for conditional initialization</li>
 *   <li>TODO: IF_ELSE, SWITCH, WHILE, FOR, TRY_CATCH</li>
 * </ul>
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
  private GuardedAssignmentAsmGenerator guardedAssignmentGenerator;
  private IfElseAsmGenerator ifElseGenerator;
  // Future generators:
  // private SwitchAsmGenerator switchGenerator;
  // private WhileLoopAsmGenerator whileLoopGenerator;
  // private ForLoopAsmGenerator forLoopGenerator;
  // private TryCatchAsmGenerator tryCatchGenerator;

  ControlFlowChainAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                               final OutputVisitor outputVisitor,
                               final ClassWriter classWriter) {
    super(constructTargetTuple, outputVisitor, classWriter);
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
      case "GUARDED_ASSIGNMENT" -> generateGuardedAssignment(instr);
      case "IF_ELSE" -> generateIfElse(instr);
      // Future cases will be added here as generators are implemented:
      // case "SWITCH", "SWITCH_ENUM" -> generateSwitch(instr);
      // case "WHILE" -> generateWhileLoop(instr);
      // case "FOR" -> generateForLoop(instr);
      // case "TRY_CATCH" -> generateTryCatch(instr);
      default -> throw new CompilerException(
          "Unsupported control flow chain type: " + chainType
              + ". Expected one of: QUESTION_OPERATOR, GUARDED_ASSIGNMENT, IF_ELSE");
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
          constructTargetTuple, outputVisitor, classWriter);
    }
    // Always update context in case method changed
    questionOperatorGenerator.setSharedMethodContext(getMethodContext());
    questionOperatorGenerator.setCurrentMethodVisitor(getCurrentMethodVisitor());
    questionOperatorGenerator.generate(instr);
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
          constructTargetTuple, outputVisitor, classWriter);
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
          constructTargetTuple, outputVisitor, classWriter);
    }
    // Always update context in case method changed
    ifElseGenerator.setSharedMethodContext(getMethodContext());
    ifElseGenerator.setCurrentMethodVisitor(getCurrentMethodVisitor());
    ifElseGenerator.generate(instr);
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
    if (guardedAssignmentGenerator != null) {
      guardedAssignmentGenerator.setSharedMethodContext(context);
    }
    if (ifElseGenerator != null) {
      ifElseGenerator.setSharedMethodContext(context);
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
    if (guardedAssignmentGenerator != null) {
      guardedAssignmentGenerator.setCurrentMethodVisitor(mv);
    }
    if (ifElseGenerator != null) {
      ifElseGenerator.setCurrentMethodVisitor(mv);
    }
    // Future generators will be added here
  }
}
