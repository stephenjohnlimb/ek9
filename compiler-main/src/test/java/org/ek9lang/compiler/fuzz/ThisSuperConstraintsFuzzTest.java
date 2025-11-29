package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for this/super constraint validation.
 * Tests INAPPROPRIATE_USE_OF_THIS, INAPPROPRIATE_USE_OF_SUPER,
 * SUPER_FOR_ANY_NOT_REQUIRED, THIS_AND_SUPER_CALLS_ONLY_IN_CONSTRUCTOR,
 * INCOMPATIBLE_GENUS.
 *
 * <p>Test corpus: fuzzCorpus/thisSuperConstraints (7 test files)
 * Validates this/super usage constraints across constructs.
 *
 * <p>Test scenarios:
 * 1. this_in_function.ek9 - this() in function context
 * - Pattern: Using this() call inside function body
 * - Tests INAPPROPRIATE_USE_OF_THIS (3 errors)
 * <br/>
 * 2. super_in_trait.ek9 - super in trait context
 * - Pattern: Using super. to access methods in traits
 * - Tests INAPPROPRIATE_USE_OF_SUPER (3 errors)
 * <br/>
 * 3. super_for_any.ek9 - super when only Any parent
 * - Pattern: Using super() or super. with implicit Any parent
 * - Tests SUPER_FOR_ANY_NOT_REQUIRED (3 errors)
 * <br/>
 * 4. this_super_outside_constructor.ek9 - outside constructor
 * - Pattern: Using this()/super() in regular methods
 * - Tests THIS_AND_SUPER_CALLS_ONLY_IN_CONSTRUCTOR (4 errors)
 * <br/>
 * 5. super_call_constructor.ek9 - super in constructor without parent
 * - Pattern: Calling super() when no explicit parent
 * - Tests SUPER_FOR_ANY_NOT_REQUIRED (3 errors)
 * <br/>
 * 6. super_in_function.ek9 - super() in function
 * - Pattern: Using super() call inside function body
 * - Tests INCOMPATIBLE_GENUS (2 errors)
 * <br/>
 * 7. valid_this_super_usage.ek9 - contrast valid vs invalid
 * - Pattern: Valid constructor delegation vs invalid method calls
 * - Tests THIS_AND_SUPER_CALLS_ONLY_IN_CONSTRUCTOR (2 errors)
 * </p>
 * <p>This/Super Semantics:
 * - this() and super() calls only valid in constructors
 * - super not appropriate in traits (use trait name directly)
 * - super not required when only implicit Any parent
 * - Error detected at FULL_RESOLUTION phase
 * </p>
 * <p>Gap addressed: This/super constraints had limited fuzz coverage:
 * - Tests focus on specific this/super violation patterns
 * - Each file targets distinct violation category
 * - 20 total error scenarios across 7 files
 * </p>
 */
class ThisSuperConstraintsFuzzTest extends FuzzTestBase {

  public ThisSuperConstraintsFuzzTest() {
    super("thisSuperConstraints", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testThisSuperConstraintsRobustness() {
    assertTrue(runTests() != 0);
  }
}
