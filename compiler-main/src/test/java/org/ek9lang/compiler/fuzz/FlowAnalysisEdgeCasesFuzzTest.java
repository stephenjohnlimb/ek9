package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for flow analysis edge cases (PRE_IR_CHECKS phase).
 * Tests USED_BEFORE_INITIALISED error detection in complex control flow scenarios.
 *
 * <p>Test corpus: fuzzCorpus/flowAnalysisEdgeCases (4 test files)
 * Validates that flow analysis correctly tracks initialization across complex
 * control flow structures including switch defaults, multiple paths, and nested exceptions.
 *
 * <p>Test scenarios:
 * 1. switch_default_missing_init.ek9 - Switch with default case that forgets to initialize
 * - Pattern: Switch with cases that initialize, default forgets
 * - Error: Variable used after switch may be uninitialized
 * - Expected: 1 USED_BEFORE_INITIALISED error
 * <br/>
 * 2. multiple_control_flow_paths_incomplete_init.ek9 - If/else with missing initialization
 * - Pattern: If branch initializes, else branch forgets
 * - Error: Variable used after if/else may be uninitialized
 * - Expected: 1 USED_BEFORE_INITIALISED error
 * <br/>
 * 3. nested_try_catch_complex_init.ek9 - Nested try/catch initialization dependencies
 * - Pattern: Inner catch forgets to initialize, outer try uses variable
 * - Error: Variable may not be initialized through exception path
 * - Expected: 1 USED_BEFORE_INITIALISED error
 * <br/>
 * 4. sequential_try_blocks_dependencies.ek9 - Sequential try blocks with dependencies
 * - Pattern: First catch forgets to initialize, second try depends on it
 * - Error: Variable used in second try may be uninitialized
 * - Expected: 2 USED_BEFORE_INITIALISED errors
 *
 * <p>Why These Are Genuine Edge Cases:
 * Existing tests cover basic flow analysis scenarios:
 * - badTryStatements.ek9: Basic try/catch initialization
 * - badIfStatements.ek9: Simple if/else paths
 * - badSwitchStatements.ek9: Switch without default (caught earlier in FULL_RESOLUTION)
 * <br/>
 * These tests add:
 * - Switch WITH default that forgets to initialize (PRE_IR_CHECKS analysis)
 * - Nested try/catch with complex initialization dependencies
 * - Sequential exception blocks with cross-block dependencies
 * - Multiple control flow paths with partial initialization
 *
 * <p>Expected behavior:
 * - Flow analysis tracks initialization state through all paths
 * - Variables used without guaranteed initialization trigger errors
 * - Error messages clearly identify the problematic usage
 *
 * <p>Validates: EK9's flow analysis correctly enforces initialization safety
 * even in complex control flow scenarios, preventing runtime null access bugs.
 *
 * <p>Total: 5 USED_BEFORE_INITIALISED errors across 4 test files
 */
class FlowAnalysisEdgeCasesFuzzTest extends FuzzTestBase {

  public FlowAnalysisEdgeCasesFuzzTest() {
    super("flowAnalysisEdgeCases", CompilationPhase.PRE_IR_CHECKS);
  }

  @Test
  void testFlowAnalysisEdgeCasesRobustness() {
    assertTrue(runTests() != 0);
  }
}
