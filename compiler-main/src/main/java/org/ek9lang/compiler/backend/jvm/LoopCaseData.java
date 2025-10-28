package org.ek9lang.compiler.backend.jvm;

import java.util.List;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.objectweb.asm.Label;

/**
 * Encapsulates all data needed for generating a FOR_RANGE loop case (ascending or descending).
 * <p>
 * This record groups the 9 parameters needed for loop generation into a single cohesive unit.
 * </p>
 *
 * @param loopStart          Label marking the start of the loop (for goto jumps back)
 * @param conditionTemplate  IR instructions to evaluate loop condition (re-executed each iteration)
 * @param conditionPrimitive Variable name holding primitive boolean condition result
 * @param bodySetup          IR instructions for body setup (assigns loopVariable = current)
 * @param body               IR instructions for loop body (user code, shared across all cases)
 * @param increment          IR instructions for increment (current++ or current--)
 * @param endLabel           Label to jump to when loop exits (shared across all cases)
 * @param loopScopeId        Unique scope ID for this loop (used for label generation)
 * @param dispatchCase       Which FOR_RANGE dispatch case (ASCENDING or DESCENDING)
 */
record LoopCaseData(Label loopStart,
                    List<IRInstr> conditionTemplate,
                    String conditionPrimitive,
                    List<IRInstr> bodySetup,
                    List<IRInstr> body,
                    List<IRInstr> increment,
                    Label endLabel,
                    String loopScopeId,
                    BytecodeGenerationContext.DispatchCase dispatchCase) {
}
