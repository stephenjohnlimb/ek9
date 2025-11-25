package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.core.AssertValue;

/**
 * Generates ARC-safe assignment instructions for expression form control flow branches.
 * <p>
 * Expression forms like {@code result &lt;- switch expr} require special ARC memory
 * management when assigning branch results to the return variable. This helper
 * generates the correct instruction sequence following the ternary operator pattern.
 * </p>
 * The ARC ownership transfer pattern inside branches is:
 * <pre>
 * RELEASE result        // Release old value (decrement ARC count)
 * STORE result, _temp   // Store new value from branch computation
 * RETAIN result         // Increment ARC count (ownership transfer)
 * </pre>
 * <p>
 * <b>IMPORTANT:</b> NO SCOPE_REGISTER is emitted inside branches. The receiving scope
 * (outer scope where the return variable is declared) handles scope registration.
 * This separation ensures proper ARC semantics:
 * </p>
 * <ul>
 *   <li>Branch: RETAIN without SCOPE_REGISTER (ownership transfer to outer scope)</li>
 *   <li>Outer scope: SCOPE_REGISTER during return variable setup (ensures cleanup)</li>
 * </ul>
 *
 * @see ReturningParamProcessor
 */
public final class ExpressionResultAssigner {

  /**
   * Generate ARC-safe assignment instructions for a branch result.
   * <p>
   * This follows the ternary operator pattern from ControlFlowChainGenerator.generateTernary():
   * </p>
   * <pre>
   * // Store then result to output variable
   * thenBodyEvaluation.add(MemoryInstr.release(resultVariable, debugInfo));
   * thenBodyEvaluation.add(MemoryInstr.store(resultVariable, thenResultTemp, debugInfo));
   * thenBodyEvaluation.add(MemoryInstr.retain(resultVariable, debugInfo));
   * </pre>
   *
   * @param returnVariable  The return variable name (e.g., the "result" in "result &lt;- switch")
   * @param branchResult    The temporary variable holding the branch's computed result
   * @param debugInfo       Debug information for the instructions
   * @return List of IR instructions for the ARC-safe assignment
   */
  public List<IRInstr> assign(final String returnVariable,
                              final String branchResult,
                              final DebugInfo debugInfo) {
    AssertValue.checkNotNull("returnVariable cannot be null", returnVariable);
    AssertValue.checkNotNull("branchResult cannot be null", branchResult);
    AssertValue.checkNotNull("debugInfo cannot be null", debugInfo);

    final var instructions = new ArrayList<IRInstr>();

    // ARC ownership transfer pattern:
    // 1. RELEASE old value (decrement ARC, may deallocate if count reaches 0)
    instructions.add(MemoryInstr.release(returnVariable, debugInfo));

    // 2. STORE new value from branch computation
    instructions.add(MemoryInstr.store(returnVariable, branchResult, debugInfo));

    // 3. RETAIN new value (increment ARC, ownership transfer to outer scope)
    // NOTE: NO SCOPE_REGISTER here - that's handled by the outer scope
    instructions.add(MemoryInstr.retain(returnVariable, debugInfo));

    return instructions;
  }

  /**
   * Check if assignment should be generated for this context.
   * <p>
   * Assignment is only generated when:
   * </p>
   * <ul>
   *   <li>This is an expression form (returnVariable is not null)</li>
   *   <li>The branch has a result to assign (branchResult is not null)</li>
   * </ul>
   *
   * @param returnVariable The return variable name (may be null for statement forms)
   * @param branchResult   The branch result variable (may be null if branch has no value)
   * @return true if assignment instructions should be generated
   */
  public boolean shouldAssign(final String returnVariable, final String branchResult) {
    return returnVariable != null && branchResult != null;
  }

  /**
   * Conditionally generate assignment instructions.
   * Returns empty list if this is a statement form (no return variable) or
   * if the branch has no result to assign.
   *
   * @param returnVariable  The return variable name (may be null for statement forms)
   * @param branchResult    The branch result variable (may be null)
   * @param debugInfo       Debug information for the instructions
   * @return List of IR instructions, or empty list if no assignment needed
   */
  public List<IRInstr> assignIfNeeded(final String returnVariable,
                                      final String branchResult,
                                      final DebugInfo debugInfo) {
    if (shouldAssign(returnVariable, branchResult)) {
      return assign(returnVariable, branchResult, debugInfo);
    }
    return List.of();
  }
}
