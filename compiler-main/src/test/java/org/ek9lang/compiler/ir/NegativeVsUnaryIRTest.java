package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Test IR generation for negative literals vs unary negation distinction.
 * <p>
 * This test verifies the grammar fix correctly distinguishes between:
 * 1. Negative literals (should be LOAD_LITERAL -X) - compile-time constants
 * 2. Unary negation on variables (should be LOAD var + CALL _negate()) - runtime operations
 * </p>
 * Tests all 5 types that support negative literals: Integer, Float, Millisecond, Dimension, Money.
 * <p>
 * Phase 10 (IR_GENERATION): Individual focused tests with comprehensive @IR directives
 * for systematic regression protection and educational documentation.
 * </p>
 */
class NegativeVsUnaryIRTest extends AbstractIRGenerationTest {

  public NegativeVsUnaryIRTest() {
    super("/examples/irGeneration/operatorUse/negativeLiterals",
        List.of(
            new SymbolCountCheck(1, "integer.negative.literal.test", 1),
            new SymbolCountCheck(1, "integer.unary.negation.test", 1),
            new SymbolCountCheck(1, "float.negative.literal.test", 1),
            new SymbolCountCheck(1, "float.unary.negation.test", 1),
            new SymbolCountCheck(1, "millisecond.negative.literal.test", 1),
            new SymbolCountCheck(1, "millisecond.unary.negation.test", 1),
            new SymbolCountCheck(1, "dimension.negative.literal.test", 1),
            new SymbolCountCheck(1, "dimension.unary.negation.test", 1),
            new SymbolCountCheck(1, "money.negative.literal.test", 1),
            new SymbolCountCheck(1, "money.unary.negation.test", 1)
        ), false, false, false); // showIR=false - @IR directives handle validation
  }
}