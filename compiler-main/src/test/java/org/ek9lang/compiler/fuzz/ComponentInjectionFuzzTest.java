package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for component injection constraint errors in FULL_RESOLUTION phase.
 * Tests COMPONENT_INJECTION_OF_NON_ABSTRACT and COMPONENT_INJECTION_NOT_POSSIBLE.
 *
 * <p>Test corpus: fuzzCorpus/componentInjection (3 test files)
 * Validates that only abstract components can be used for dependency injection.
 *
 * <p>Test scenarios:
 * 1. inject_concrete_component.ek9 - Inject non-abstract component
 * - Pattern: ConcreteService extends BaseService (not marked as abstract)
 * - Error: Dependency injection requires abstract components
 * - Tests COMPONENT_INJECTION_OF_NON_ABSTRACT (existing coverage: 1 test)
 * <br/>
 * 2. inject_regular_class.ek9 - Inject regular class instead of component
 * - Pattern: DataProcessor (class, not component) with injection marker
 * - Error: Only components can be injected, not regular classes
 * - Tests COMPONENT_INJECTION_NOT_POSSIBLE
 * <br/>
 * 3. inject_final_component.ek9 - Inject sealed/concrete component
 * - Pattern: SealedComponent extends BaseComponent (concrete implementation)
 * - Error: Sealed components cannot be injected, must be abstract
 * - Tests COMPONENT_INJECTION_OF_NON_ABSTRACT
 * </p>
 * <p>Injection Constraint Semantics:
 * - COMPONENT_INJECTION_OF_NON_ABSTRACT: Only abstract components can be injected;
 *   concrete implementations violate dependency injection principles
 * - COMPONENT_INJECTION_NOT_POSSIBLE: Only component types support injection;
 *   classes, records, and other types cannot use the injection marker (!)
 * - Error detected at FULL_RESOLUTION phase during component validation
 * </p>
 * <p>Expected behavior:
 * - Compiler should detect invalid component injections at FULL_RESOLUTION phase
 * - Only abstract components with unimplemented methods can be dependency injection targets
 * - Non-component types (classes, records) cannot use injection syntax
 * </p>
 * <p>Validates: Component injection enforcement ensures type safety and follows
 * dependency injection patterns correctly, preventing runtime injection failures.
 * </p>
 * <p>Gap addressed: Critical component injection errors had minimal coverage:
 * - COMPONENT_INJECTION_OF_NON_ABSTRACT: 1 existing test → 3 total tests
 * Covers concrete components, regular classes, and sealed implementations.
 * This ensures EK9's dependency injection system correctly enforces abstraction requirements.
 * </p>
 */
class ComponentInjectionFuzzTest extends FuzzTestBase {

  public ComponentInjectionFuzzTest() {
    super("componentInjection", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testComponentInjectionRobustness() {
    assertTrue(runTests() != 0);
  }
}
