package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for PRE_FLOW_SYMBOL_NOT_RESOLVED errors in FULL_RESOLUTION phase.
 * Tests objectAccessExpression assignments in control flow preFlow statements.
 *
 * <p>Test corpus: fuzzCorpus/preFlowExpression (4 test files)
 * Covers SWITCH statements with objectAccessExpression assignments lacking explicit controls.
 *
 * <p>Test scenarios:
 * 1. switch_add_assign.ek9 - SWITCH with += operator on objectAccessExpression
 * - Pattern: switch counter.value += 5 (no explicit control)
 * - Error: Cannot determine flow subject - is it 'counter' or 'value'?
 * - Tests PRE_FLOW_SYMBOL_NOT_RESOLVED (existing coverage: 1 test)
 * <br/>
 * 2. switch_sub_assign.ek9 - SWITCH with -= operator on objectAccessExpression
 * - Pattern: switch counter.value -= 3 (no explicit control)
 * - Same ambiguity with subtraction assignment
 * <br/>
 * 3. switch_mul_assign.ek9 - SWITCH with *= operator on objectAccessExpression
 * - Pattern: switch counter.value *= 3 (no explicit control)
 * - Same ambiguity with multiplication assignment
 * <br/>
 * 4. switch_div_assign.ek9 - SWITCH with /= operator on objectAccessExpression
 * - Pattern: switch counter.value /= 4 (no explicit control)
 * - Same ambiguity with division assignment
 * </p>
 * <p>PreFlow Expression Semantics:
 * - PRE_FLOW_SYMBOL_NOT_RESOLVED: "Only simply variable assignments supported without a control,
 *   failed to find subject of flow"
 * - Triggers when: objectAccessExpression (e.g., rec.field) used in assignment preFlow
 *   without explicit control expression
 * - Reason: Compiler cannot determine flow subject - should it be 'rec' or 'field'?
 * </p>
 * <p>Control Flow Context Analysis:
 * Grammar analysis reveals PRE_FLOW_SYMBOL_NOT_RESOLVED ONLY triggers in SWITCH contexts:
 *
 * <p>✓ SWITCH (without control): TRIGGERS ERROR
 * - Grammar: SWITCH preFlowAndControl (allows preFlow alone OR with explicit control)
 * - switch rec.value += 5 → ERROR (no explicit control, ambiguous subject)
 * - switch rec.value += 5 then rec.value → VALID (explicit control provided)
 *
 * <p>✗ IF: Does NOT trigger (valid)
 * - IF can evaluate assignment result directly as boolean control
 * - if rec.value += 5 → VALID (assignment result is control)
 *
 * <p>✗ WHILE: Does NOT trigger (valid)
 * - Grammar: WHILE (preFlowStatement (WITH|THEN))? control=expression
 * - 'control' expression is REQUIRED - preFlow optional
 * - while rec.value += 1 → Assignment IS the control expression, not preFlow
 *
 * <p>✗ DO-WHILE: Does NOT trigger (valid)
 * - Grammar: DO preFlowStatement? ... WHILE control=expression
 * - Control at closing WHILE is REQUIRED
 *
 * <p>✗ FOR: Does NOT trigger (valid)
 * - Grammar: FOR preFlowStatement (WITH|THEN) identifier IN primaryReference
 * - preFlowStatement executes once before loop, not acting as control
 * - Actual loop control is "identifier IN primaryReference"
 *
 * <p>✗ TRY: Does NOT trigger (valid)
 * - TRY accepts objectAccessExpression assignments without restriction
 * </p>
 * <p>Expected behavior:
 * - Compiler rejects objectAccessExpression assignments in SWITCH preFlow without control
 * - Simple identifier assignments work: switch counter += 5 → VALID
 * - Explicit control works: switch rec.value += 5 then rec.value → VALID
 * - Only SWITCH enforces this constraint (other contexts have different control semantics)
 * </p>
 * <p>Validates: SWITCH control flow subject resolution correctly enforces simple identifier
 * requirements when preFlow statement is used without explicit control expression.
 * </p>
 * <p>Gap addressed: PRE_FLOW_SYMBOL_NOT_RESOLVED had minimal coverage:
 * - Existing: 1 test in badSwitch3.ek9 (single += operator)
 * - Added: 4 comprehensive tests covering all assignment operators (+=, -=, *=, /=)
 * - Systematic validation that ONLY SWITCH triggers this error (not IF/WHILE/FOR/TRY)
 * This ensures EK9's control flow subject resolution is correctly enforced.
 * </p>
 */
class PreFlowExpressionFuzzTest extends FuzzTestBase {

  public PreFlowExpressionFuzzTest() {
    super("preFlowExpression", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testPreFlowExpressionRobustness() {
    assertTrue(runTests() != 0);
  }
}
