package org.ek9lang.compiler.backend.jvm;

import java.util.List;
import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.instructions.ForRangePolymorphicInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;

/**
 * JVM bytecode generator for FOR_RANGE_POLYMORPHIC IR instruction.
 * <p>
 * Generates three-way dispatch based on runtime direction detection (start &lt;=&gt; end):
 * </p>
 * <ul>
 *   <li><b>Ascending:</b> direction &lt; 0 → loop with current &lt;= end, current++</li>
 *   <li><b>Descending:</b> direction &gt; 0 → loop with current &gt;= end, current--</li>
 *   <li><b>Equal:</b> direction == 0 → single iteration with loopVariable = current</li>
 * </ul>
 * <p>
 * <b>Bytecode Pattern:</b>
 * </p>
 * <pre>
 * ; Initialization (includes ASSERT for start._isSet() and end._isSet())
 * [evaluate start, end, compute direction = start &lt;=&gt; end, current = start]
 *
 * ; Three-way dispatch
 * [check direction &lt; 0]
 * ifne ascending_label
 * [check direction &gt; 0]
 * ifne descending_label
 * ; Fall through to equal
 *
 * equal_label:
 *   loopVariable = current
 *   [body once]
 *   goto end_label
 *
 * ascending_label:
 * asc_loop_start:
 *   [check current &lt;= end]
 *   ifeq end_label              ; Exit if false
 *   loopVariable = current
 *   [body]
 *   current = current._inc()
 *   goto asc_loop_start
 *
 * descending_label:
 * desc_loop_start:
 *   [check current &gt;= end]
 *   ifeq end_label
 *   loopVariable = current
 *   [body]
 *   current = current._dec()
 *   goto desc_loop_start
 *
 * end_label:
 * </pre>
 * <p>
 * <b>Stack Frame Invariant:</b> Stack is empty before loop, after initialization,
 * at loop start, after each iteration, and at loop end. All values stored in local variables.
 * </p>
 * <p>
 * <b>ASSERT Handling:</b> ASSERT instructions in initialization sequence (for start._isSet()
 * and end._isSet() validation) are handled automatically by recursive delegation to
 * BranchInstrAsmGenerator. No special handling needed.
 * </p>
 * <p>
 * <b>Label Naming:</b> Uses {@code loopScopeId} from ScopeMetadata to ensure uniqueness,
 * preventing collisions in nested loops with same variable names.
 * </p>
 */
final class ForRangePolymorphicAsmGenerator extends AbstractControlFlowAsmGenerator {

  ForRangePolymorphicAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                                  final OutputVisitor outputVisitor,
                                  final ClassWriter classWriter,
                                  final BytecodeGenerationContext context) {
    super(constructTargetTuple, outputVisitor, classWriter, context);
  }

  /**
   * Generate JVM bytecode for polymorphic FOR_RANGE loop.
   * <p>
   * Implementation Strategy:
   * </p>
   * <ol>
   *   <li>Emit initialization IR (evaluates start, end, direction, current)</li>
   *   <li>Create labels using loopScopeId (ensures uniqueness in nested loops)</li>
   *   <li>Generate three-way dispatch (ascending/descending/equal)</li>
   *   <li>Generate equal case (single iteration, fall through)</li>
   *   <li>Generate ascending case (loop with current++)</li>
   *   <li>Generate descending case (loop with current--)</li>
   *   <li>Place end label (all paths converge)</li>
   * </ol>
   * <p>
   * All IR instructions are processed via recursive delegation to OutputVisitor,
   * which routes them to specialized generators (Call, Literal, Memory, Branch, etc.).
   * </p>
   * <p>
   * Stack Frame Invariant maintained throughout:
   * Pre-condition: stack is empty
   * Post-condition: stack is empty
   * </p>
   *
   * @param instr FOR_RANGE_POLYMORPHIC instruction with all dispatch cases and metadata
   */
  public void generate(final ForRangePolymorphicInstr instr) {
    // 1. Emit initialization (ASSERT handled automatically by BranchInstrAsmGenerator)
    processInstructions(instr.getInitializationInstructions());
    // Stack: empty (all results in local variables: _temp3=start, _temp5=end, _temp11=direction, _temp12=current)

    // 2. Get scope metadata - use loopScopeId for all labels to prevent nested loop collisions
    final var scopeMeta = instr.getScopeMetadata();
    final var loopScopeId = scopeMeta.loopScopeId();  // e.g., "_scope_3" (globally unique)

    // 3. Create dispatch labels using loopScopeId (NOT loop variable name)
    final Label ascLabel = createControlFlowLabel("for_asc", loopScopeId);
    final Label descLabel = createControlFlowLabel("for_desc", loopScopeId);
    final Label equalLabel = createControlFlowLabel("for_equal", loopScopeId);
    final Label endLabel = createControlFlowLabel("for_end", loopScopeId);

    // 4. Generate three-way dispatch logic
    generateDispatch(instr.getDispatchCases(), ascLabel, descLabel);
    // Stack: empty (both paths - branch taken or fall through)

    // 5. Equal case (fall through from dispatch when direction == 0)
    placeLabel(equalLabel);
    // Stack: empty
    generateEqualCase(instr.getDispatchCases().equal(), instr.getBodyInstructions());
    // Stack: empty
    jumpTo(endLabel);
    // Stack: irrelevant (control transferred)

    // 6. Ascending case (direction &lt; 0, loop with current++  )
    placeLabel(ascLabel);
    // Stack: empty (from dispatch branch)
    final Label loopStartAsc = createControlFlowLabel("for_asc_loop", loopScopeId);
    generateLoopCase(new LoopCaseData(
        loopStartAsc,
        instr.getDispatchCases().ascending().loopConditionTemplate(),
        instr.getDispatchCases().ascending().loopConditionPrimitive(),
        instr.getDispatchCases().ascending().loopBodySetup(),
        instr.getBodyInstructions(),
        instr.getDispatchCases().ascending().loopIncrement(),
        endLabel,
        loopScopeId,
        BytecodeGenerationContext.DispatchCase.ASCENDING));
    // Stack: empty at end label
    jumpTo(endLabel);
    // Stack: irrelevant (control transferred)

    // 7. Descending case (direction &gt; 0, loop with current--)
    placeLabel(descLabel);
    // Stack: empty (from dispatch branch)
    final Label loopStartDesc = createControlFlowLabel("for_desc_loop", loopScopeId);
    generateLoopCase(new LoopCaseData(
        loopStartDesc,
        instr.getDispatchCases().descending().loopConditionTemplate(),
        instr.getDispatchCases().descending().loopConditionPrimitive(),
        instr.getDispatchCases().descending().loopBodySetup(),
        instr.getBodyInstructions(),
        instr.getDispatchCases().descending().loopIncrement(),
        endLabel,
        loopScopeId,
        BytecodeGenerationContext.DispatchCase.DESCENDING));
    // Stack: empty at end label

    // 8. End label - all paths converge here
    placeLabel(endLabel);
    // Stack: empty (guaranteed by all paths)
    // Continue with next instructions after FOR loop
  }

  /**
   * Generate three-way dispatch based on direction comparison result.
   * <p>
   * Logic:
   * </p>
   * <pre>
   * if (direction &lt; 0) goto ascLabel       // Ascending case
   * if (direction &gt; 0) goto descLabel      // Descending case
   * fall through (direction == 0)          // Equal case
   * </pre>
   * <p>
   * Stack: empty before, empty after (all branches)
   * </p>
   *
   * @param cases     Dispatch cases containing direction check IR for each case
   * @param ascLabel  Label for ascending case
   * @param descLabel Label for descending case
   */
  private void generateDispatch(final ForRangePolymorphicInstr.DispatchCases cases,
                                 final Label ascLabel,
                                 final Label descLabel) {

    processInstructions(cases.ascending().directionCheck());
    // Stack: empty (direction check result in primitive boolean variable)
    branchIfTrue(cases.ascending().directionPrimitive(), ascLabel);
    // Stack: empty (both paths - branch taken or continue)

    processInstructions(cases.descending().directionCheck());
    // Stack: empty
    branchIfTrue(cases.descending().directionPrimitive(), descLabel);
    // Stack: empty (both paths)

    // Fall through to equal case (direction == 0, no explicit check needed)
    // Stack: empty
  }

  /**
   * Generate ascending or descending loop case.
   * <p>
   * Pattern:
   * </p>
   * <pre>
   * loop_start:
   *   [condition template re-executed each iteration]
   *   iload primitive_condition
   *   ifeq end_label
   *   [body setup: loopVariable = current]
   *   [body instructions]
   *   [increment: current++ or current--]
   *   goto loop_start
   * </pre>
   * <p>
   * Stack: empty at loop_start, empty after condition, empty after body, empty after increment
   * </p>
   *
   * @param data All data needed for loop generation (label, IR instructions, scope ID, dispatch case)
   */
  private void generateLoopCase(final LoopCaseData data) {
    // Place loop start label
    placeLabel(data.loopStart());
    // Stack: empty

    // Set dispatch case BEFORE creating labels (ensures dispatch prefix is applied)
    context.enterDispatchCase(data.dispatchCase());

    // Create increment label - where nested control flow continues to next iteration
    // CRITICAL: Must be created AFTER enterDispatchCase() to get dispatch-specific prefix
    final Label incrementLabel = createControlFlowLabel("for_increment", data.loopScopeId());

    // PUSH loop context BEFORE processing body
    context.enterLoop(data.loopScopeId(), incrementLabel, data.endLabel());
    try {
      // Condition check (template re-executed each iteration: current &lt;=&gt; end, etc.)
      processInstructions(data.conditionTemplate());
      // Stack: empty (condition result in local variable)

      // Branch to end if condition is false
      branchIfFalse(data.conditionPrimitive(), data.endLabel());
      // Stack: empty (both paths - continue or branch)

      // Body setup: loopVariable = current (STORE instruction)
      processInstructions(data.bodySetup());
      // Stack: empty

      // Body execution (shared across all dispatch cases)
      // Nested generators can query context for loop continue/exit labels
      // Nested control flow gets dispatch-specific label prefixes (e.g., "ascending_")
      processInstructions(data.body());
      // Stack: empty

      // Place increment label (where nested control flow jumps)
      placeLabel(incrementLabel);

      // Increment: current = current._inc() or current._dec()
      processInstructions(data.increment());
      // Stack: empty

      // Jump back to loop start for next iteration
      jumpTo(data.loopStart());
      // Stack: irrelevant (control transferred)
    } finally {
      // POP loop context AFTER processing body (ensures cleanup even on exceptions)
      context.exitLoop();
      // Clear dispatch case AFTER processing body (ensures cleanup)
      context.exitDispatchCase();
    }
  }

  /**
   * Generate equal case (direction == 0, single iteration).
   * <p>
   * Pattern:
   * </p>
   * <pre>
   * [body setup: loopVariable = current]
   * [body instructions]
   * ; Fall through to end (no loop)
   * </pre>
   * <p>
   * Stack: empty before, empty after
   * </p>
   *
   * @param equalCase Equal case details from IR
   * @param body      IR instructions for loop body (user code, shared)
   */
  private void generateEqualCase(final ForRangePolymorphicInstr.EqualCase equalCase,
                                  final List<IRInstr> body) {
    // Set dispatch case BEFORE processing body (makes labels unique)
    context.enterDispatchCase(BytecodeGenerationContext.DispatchCase.EQUAL);
    try {
      // Body setup: loopVariable = current
      processInstructions(equalCase.loopBodySetup());
      // Stack: empty

      // Body execution once (nested control flow gets "equal_" label prefix)
      processInstructions(body);
      // Stack: empty

      // Fall through to end (no loop, single iteration only)
    } finally {
      // Clear dispatch case AFTER processing body (ensures cleanup)
      context.exitDispatchCase();
    }
  }

  /**
   * Process list of IR instructions via recursive delegation to OutputVisitor.
   * <p>
   * Each instruction knows how to generate its own bytecode via accept(visitor).
   * This enables:
   * </p>
   * <ul>
   *   <li>CALL instructions → CallInstrAsmGenerator</li>
   *   <li>LOAD/STORE/RETAIN/RELEASE → MemoryInstrAsmGenerator</li>
   *   <li>LOAD_LITERAL → LiteralInstrAsmGenerator</li>
   *   <li>ASSERT → BranchInstrAsmGenerator</li>
   *   <li>SCOPE_ENTER/EXIT → ScopeInstrAsmGenerator</li>
   * </ul>
   * <p>
   * All instructions maintain stack-empty invariant.
   * </p>
   *
   * @param instructions List of IR instructions to process
   */
  private void processInstructions(final List<IRInstr> instructions) {
    for (var instr : instructions) {
      instr.accept(outputVisitor);  // Recursive delegation - polymorphic dispatch to correct generator
    }
    // Post-condition: stack is empty (guaranteed by all IR instruction generators)
  }
}
