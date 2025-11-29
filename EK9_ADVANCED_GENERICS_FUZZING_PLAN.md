# EK9 Advanced Generics Fuzzing: Implementation Plan

**Date**: 2025-11-17
**Status**: READY TO IMPLEMENT
**Estimated Effort**: 2-3 hours
**Priority**: MEDIUM (completes frontend fuzzing to 100%)

---

## Executive Summary

This plan implements the **final 15 frontend fuzzing tests** to achieve 100% frontend fuzzing completion. These tests cover advanced generic type and function constraints that are currently untested.

### Current Generic Coverage

**Already Covered (11 tests):**
- ✅ OPERATOR_NOT_DEFINED - 8 tests (GenericOperatorConstraintsFuzzTest)
- ✅ GENERIC_FUNCTION_IMPLEMENTATION_REQUIRED - 3 tests (GenericFunctionValidationFuzzTest)
- ✅ GENERIC_TYPE_OR_FUNCTION_PARAMETERS_NEEDED - Covered in phase3 tests
- ✅ GENERIC_WITH_NAMED_DYNAMIC_CLASS - Covered in dynamic class tests

**Missing Coverage (17 error types, 15 tests planned):**
- 🔴 TYPE_CANNOT_BE_CONSTRAINED
- 🔴 CONSTRUCTOR_NOT_RESOLVED_IN_GENERIC_CONTEXT
- 🔴 GENERIC_TYPE_OR_FUNCTION_PARAMETERS_INVALID
- 🔴 GENERIC_TYPE_OR_FUNCTION_PARAMETERS_INCORRECT
- 🔴 GENERIC_TYPE_CONSTRUCTOR_INAPPROPRIATE
- 🔴 GENERIC_TYPE_REQUIRES_TWO_CONSTRUCTORS
- 🔴 GENERIC_TYPE_REQUIRES_CORRECT_CONSTRUCTOR_ARGUMENT_TYPES
- 🔴 GENERIC_CONSTRUCTORS_MUST_BE_PUBLIC
- 🔴 GENERIC_TYPE_OR_FUNCTION_PARAMETER_MISMATCH
- 🔴 GENERIC_TYPE_OR_FUNCTION_NOT_RESOLVED
- 🔴 CONSTRAINED_FUNCTIONS_NOT_SUPPORTED
- 🔴 GENERIC_TYPE_OR_FUNCTION_NOT_APPLICABLE
- 🔴 GENERIC_TYPE_DEFINITION_CANNOT_EXTEND
- 🔴 CONSTRUCTOR_WITH_FUNCTION_IN_GENERIC
- 🔴 FUNCTION_USED_IN_GENERIC
- 🔴 CONSTRAINED_TYPE_CONSTRUCTOR_MISSING
- 🔴 IMPLIED_OPERATOR_NOT_DEFINED_FROM_GENERIC

---

## Test Suite Design

### Suite 1: Generic Type Parameterization Constraints (5 tests)
**Test Class**: `GenericParameterizationConstraintsFuzzTest`
**Corpus Directory**: `fuzzCorpus/genericParameterizationConstraints/`
**Target Phase**: `CompilationPhase.FULL_RESOLUTION`

#### Test Files:

**1. generic_parameters_invalid_type.ek9**
```ek9
#!ek9
defines module generic.parameters.invalid

  defines function
    NonGenericFunction()
      <- result as String: "test"

  defines program
    TestInvalidParameterType()
      // Try to parameterize with function type (not allowed)
      @Error: FULL_RESOLUTION: GENERIC_TYPE_OR_FUNCTION_PARAMETERS_INVALID
      list <- List() of NonGenericFunction
```
**Error**: GENERIC_TYPE_OR_FUNCTION_PARAMETERS_INVALID
**Scenario**: Parameterizing generic with invalid type (function)

---

**2. generic_parameter_count_mismatch.ek9**
```ek9
#!ek9
defines module generic.parameter.mismatch

  defines program
    TestParameterCountMismatch()
      // Dict requires 2 type parameters, providing only 1
      @Error: FULL_RESOLUTION: GENERIC_TYPE_OR_FUNCTION_PARAMETERS_INCORRECT
      dict <- Dict() of String

      // List requires 1 type parameter, providing 2
      @Error: FULL_RESOLUTION: GENERIC_TYPE_OR_FUNCTION_PARAMETER_MISMATCH
      list <- List() of String and Integer
```
**Errors**: GENERIC_TYPE_OR_FUNCTION_PARAMETERS_INCORRECT, GENERIC_TYPE_OR_FUNCTION_PARAMETER_MISMATCH
**Scenario**: Wrong number of type parameters

---

**3. generic_non_generic_parameterization.ek9**
```ek9
#!ek9
defines module generic.non.generic.param

  defines class
    RegularClass
      value <- "test"

  defines program
    TestNonGenericParameterization()
      // Try to parameterize non-generic class
      @Error: FULL_RESOLUTION: GENERIC_TYPE_OR_FUNCTION_NOT_APPLICABLE
      obj <- RegularClass() of String
```
**Error**: GENERIC_TYPE_OR_FUNCTION_NOT_APPLICABLE
**Scenario**: Parameterizing non-generic type

---

**4. generic_type_not_resolved.ek9**
```ek9
#!ek9
defines module generic.type.not.resolved

  defines program
    TestGenericNotResolved()
      // Use generic type that doesn't exist
      @Error: FULL_RESOLUTION: GENERIC_TYPE_OR_FUNCTION_NOT_RESOLVED
      container <- UnknownGeneric() of String
```
**Error**: GENERIC_TYPE_OR_FUNCTION_NOT_RESOLVED
**Scenario**: Generic type doesn't exist

---

**5. generic_function_parameter_type_mismatch.ek9**
```ek9
#!ek9
defines module generic.function.param.mismatch

  defines function
    GenericProcessor() of type T
      -> input as T
      <- result as T: input

  defines program
    TestFunctionParameterMismatch()
      processor <- GenericProcessor() of String

      // Call with wrong type (Integer instead of String)
      @Error: FULL_RESOLUTION: GENERIC_TYPE_OR_FUNCTION_PARAMETER_MISMATCH
      result <- processor(42)
```
**Error**: GENERIC_TYPE_OR_FUNCTION_PARAMETER_MISMATCH
**Scenario**: Function parameterized type vs actual argument mismatch

---

### Suite 2: Generic Type Definition Constraints (4 tests)
**Test Class**: `GenericTypeDefinitionConstraintsFuzzTest`
**Corpus Directory**: `fuzzCorpus/genericTypeDefinitionConstraints/`
**Target Phase**: `CompilationPhase.FULL_RESOLUTION`

#### Test Files:

**6. generic_class_extends_class.ek9**
```ek9
#!ek9
defines module generic.class.extends

  defines class
    BaseClass
      value <- "base"

  defines class
    @Error: FULL_RESOLUTION: GENERIC_TYPE_DEFINITION_CANNOT_EXTEND
    GenericContainer of type T extends BaseClass
      item <- T()
```
**Error**: GENERIC_TYPE_DEFINITION_CANNOT_EXTEND
**Scenario**: Generic class cannot extend another class

---

**7. generic_class_extends_generic.ek9**
```ek9
#!ek9
defines module generic.class.extends.generic

  defines class
    GenericBase of type T
      value as T?

  defines class
    @Error: FULL_RESOLUTION: GENERIC_TYPE_DEFINITION_CANNOT_EXTEND
    GenericDerived of type T extends GenericBase of T
      extra as T?
```
**Error**: GENERIC_TYPE_DEFINITION_CANNOT_EXTEND
**Scenario**: Generic class cannot extend generic class

---

**8. generic_requires_two_constructors.ek9**
```ek9
#!ek9
defines module generic.requires.two.constructors

  defines class
    InvalidGeneric of type T
      value as T?

      // Only one constructor - missing default and inferred type constructors
      @Error: FULL_RESOLUTION: GENERIC_TYPE_REQUIRES_TWO_CONSTRUCTORS
      InvalidGeneric()
        value: T()
```
**Error**: GENERIC_TYPE_REQUIRES_TWO_CONSTRUCTORS
**Scenario**: Generic type needs default and type-inferred constructors

---

**9. generic_private_constructor.ek9**
```ek9
#!ek9
defines module generic.private.constructor

  defines class
    InvalidGeneric of type T
      value as T?

      @Error: FULL_RESOLUTION: GENERIC_CONSTRUCTORS_MUST_BE_PUBLIC
      private InvalidGeneric()
        value: T()

      @Error: FULL_RESOLUTION: GENERIC_CONSTRUCTORS_MUST_BE_PUBLIC
      private InvalidGeneric()
        -> arg as T
        value: arg
```
**Error**: GENERIC_CONSTRUCTORS_MUST_BE_PUBLIC
**Scenario**: Generic constructors cannot be private

---

### Suite 3: Generic Constraint Validation (3 tests)
**Test Class**: `GenericConstraintValidationFuzzTest`
**Corpus Directory**: `fuzzCorpus/genericConstraintValidation/`
**Target Phase**: `CompilationPhase.FULL_RESOLUTION`

#### Test Files:

**10. generic_constrain_by_function.ek9**
```ek9
#!ek9
defines module generic.constrain.by.function

  defines function
    ConstraintFunction()
      <- result as String: "constraint"

  defines class
    @Error: FULL_RESOLUTION: CONSTRAINED_FUNCTIONS_NOT_SUPPORTED
    Container of type T constrain by ConstraintFunction
      item as T?
```
**Error**: CONSTRAINED_FUNCTIONS_NOT_SUPPORTED
**Scenario**: Cannot constrain generic by function type

---

**11. generic_type_cannot_be_constrained.ek9**
```ek9
#!ek9
defines module generic.type.cannot.constrain

  defines function
    SomeFunction()
      <- result as String: "test"

  defines class
    @Error: FULL_RESOLUTION: TYPE_CANNOT_BE_CONSTRAINED
    Container of type T constrain by SomeFunction
      item as T?
```
**Error**: TYPE_CANNOT_BE_CONSTRAINED
**Scenario**: Type is not valid constraint candidate

---

**12. generic_constrained_type_constructor_missing.ek9**
```ek9
#!ek9
defines module generic.constrained.constructor.missing

  defines class
    Constraint
      value <- "constraint"

      // No default constructor
      Constraint()
        -> arg as String
        value: arg

  defines class
    Container of type T constrain by Constraint
      item as T?

      Container()
        // T must have constructors that Constraint has
        @Error: FULL_RESOLUTION: CONSTRAINED_TYPE_CONSTRUCTOR_MISSING
        item: T()

  defines program
    TestConstrainedConstructor()
      @Error: FULL_RESOLUTION: CONSTRUCTOR_NOT_RESOLVED_IN_GENERIC_CONTEXT
      container <- Container() of String
```
**Errors**: CONSTRAINED_TYPE_CONSTRUCTOR_MISSING, CONSTRUCTOR_NOT_RESOLVED_IN_GENERIC_CONTEXT
**Scenario**: Constrained type missing required constructors

---

### Suite 4: Generic Function Usage in Generics (3 tests)
**Test Class**: `GenericFunctionUsageConstraintsFuzzTest`
**Corpus Directory**: `fuzzCorpus/genericFunctionUsageConstraints/`
**Target Phase**: `CompilationPhase.FULL_RESOLUTION`

#### Test Files:

**13. generic_with_function_constructor.ek9**
```ek9
#!ek9
defines module generic.with.function.constructor

  defines function
    Processor()
      <- result as String: "process"

  defines class
    Container of type T
      item as T?

      @Error: FULL_RESOLUTION: CONSTRUCTOR_WITH_FUNCTION_IN_GENERIC
      Container()
        -> func as Processor
        // Constructor cannot have function parameter in generic
```
**Error**: CONSTRUCTOR_WITH_FUNCTION_IN_GENERIC
**Scenario**: Generic constructor cannot have function parameters

---

**14. generic_function_used_beyond_isset.ek9**
```ek9
#!ek9
defines module generic.function.used.beyond.isset

  defines function
    Transformer()
      <- result as String: "transform"

  defines class
    Container of type T
      item as T?

      process()
        @Error: FULL_RESOLUTION: FUNCTION_USED_IN_GENERIC
        if item.someMethod()
          // Functions in generics can only use '?' operator
```
**Error**: FUNCTION_USED_IN_GENERIC
**Scenario**: Functions in generics limited to '?' operator

---

**15. generic_implied_operator_from_constraint.ek9**
```ek9
#!ek9
defines module generic.implied.operator

  defines class
    Comparable
      value as Integer: 0

      operator <=> as pure
        -> other as Comparable
        <- result as Integer: value <=> other.value

  defines class
    Container of type T constrain by Comparable
      item as T?

      compare()
        -> other as Container of T
        // Using implied operator from constraint
        @Error: FULL_RESOLUTION: IMPLIED_OPERATOR_NOT_DEFINED_FROM_GENERIC
        <- result as Integer: item.unknownOp(other.item)

  defines program
    TestImpliedOperator()
      c1 <- Container() of Comparable
```
**Error**: IMPLIED_OPERATOR_NOT_DEFINED_FROM_GENERIC
**Scenario**: Implied operator not defined in generic constraint

---

## Implementation Steps

### Step 1: Create Test Suites (30 minutes)

**Create 4 test suite classes:**

1. `GenericParameterizationConstraintsFuzzTest.java`
2. `GenericTypeDefinitionConstraintsFuzzTest.java`
3. `GenericConstraintValidationFuzzTest.java`
4. `GenericFunctionUsageConstraintsFuzzTest.java`

**Pattern:**
```java
package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for [description] in FULL_RESOLUTION phase.
 * Tests [error types] constraints.
 *
 * <p>Test corpus: fuzzCorpus/[directory]
 * [Additional documentation...]
 */
class [TestClassName] extends FuzzTestBase {

  public [TestClassName]() {
    super("[directoryName]", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void test[Name]Robustness() {
    assertTrue(runTests() != 0);
  }
}
```

### Step 2: Create Corpus Directories (5 minutes)

```bash
cd compiler-main/src/test/resources/fuzzCorpus
mkdir -p genericParameterizationConstraints
mkdir -p genericTypeDefinitionConstraints
mkdir -p genericConstraintValidation
mkdir -p genericFunctionUsageConstraints
```

### Step 3: Create Test Files (60 minutes)

Create 15 `.ek9` test files following the specifications above.

**Critical Requirements:**
1. ✅ Use `@Error: FULL_RESOLUTION: [ERROR_TYPE]` directives
2. ✅ Place directive immediately before error-triggering line
3. ✅ Include module header and program/class/function as needed
4. ✅ Add comments explaining test scenario
5. ✅ Use consistent naming: `generic_[scenario]_[variant].ek9`

### Step 4: Validate Tests (15 minutes)

```bash
# Compile to ensure infrastructure is correct
mvn clean compile -pl compiler-main

# Run each test suite
mvn test -Dtest=GenericParameterizationConstraintsFuzzTest -pl compiler-main
mvn test -Dtest=GenericTypeDefinitionConstraintsFuzzTest -pl compiler-main
mvn test -Dtest=GenericConstraintValidationFuzzTest -pl compiler-main
mvn test -Dtest=GenericFunctionUsageConstraintsFuzzTest -pl compiler-main

# Verify all pass
mvn test -Dtest="Generic*FuzzTest" -pl compiler-main
```

### Step 5: Update Documentation (10 minutes)

**Update files:**
1. `FUZZING_MASTER_STATUS.md` - Add Phase 2C completion
2. `PRE_IR_CHECKS_IMPLEMENTATION_STATUS.md` - Update if needed
3. `EK9_ADVANCED_GENERICS_FUZZING_PLAN.md` - Mark complete

---

## Error Type Coverage Summary

| Error Type | Tests | Priority | Status |
|------------|-------|----------|--------|
| TYPE_CANNOT_BE_CONSTRAINED | 1 | HIGH | 🔴 Planned |
| CONSTRUCTOR_NOT_RESOLVED_IN_GENERIC_CONTEXT | 1 | HIGH | 🔴 Planned |
| GENERIC_TYPE_OR_FUNCTION_PARAMETERS_INVALID | 1 | HIGH | 🔴 Planned |
| GENERIC_TYPE_OR_FUNCTION_PARAMETERS_INCORRECT | 1 | HIGH | 🔴 Planned |
| GENERIC_TYPE_OR_FUNCTION_PARAMETER_MISMATCH | 2 | HIGH | 🔴 Planned |
| GENERIC_TYPE_OR_FUNCTION_NOT_RESOLVED | 1 | MEDIUM | 🔴 Planned |
| GENERIC_TYPE_OR_FUNCTION_NOT_APPLICABLE | 1 | MEDIUM | 🔴 Planned |
| GENERIC_TYPE_DEFINITION_CANNOT_EXTEND | 2 | HIGH | 🔴 Planned |
| GENERIC_TYPE_REQUIRES_TWO_CONSTRUCTORS | 1 | HIGH | 🔴 Planned |
| GENERIC_CONSTRUCTORS_MUST_BE_PUBLIC | 1 | MEDIUM | 🔴 Planned |
| CONSTRAINED_FUNCTIONS_NOT_SUPPORTED | 1 | MEDIUM | 🔴 Planned |
| CONSTRAINED_TYPE_CONSTRUCTOR_MISSING | 1 | HIGH | 🔴 Planned |
| CONSTRUCTOR_WITH_FUNCTION_IN_GENERIC | 1 | MEDIUM | 🔴 Planned |
| FUNCTION_USED_IN_GENERIC | 1 | MEDIUM | 🔴 Planned |
| IMPLIED_OPERATOR_NOT_DEFINED_FROM_GENERIC | 1 | MEDIUM | 🔴 Planned |
| **TOTAL** | **15** | - | **🔴 PENDING** |

---

## Success Metrics

### Before Implementation
- Frontend tests: 366
- Frontend suites: 53
- Frontend error coverage: ~75%
- Frontend completion: 96% (15 tests short of 100%)

### After Implementation
- Frontend tests: **381** (+15)
- Frontend suites: **57** (+4)
- Frontend error coverage: **~78%** (+3%)
- Frontend completion: **100%** ✅

### Quality Targets
- ✅ Zero compiler crashes on any test input
- ✅ All 15 error types systematically validated
- ✅ 100% frontend fuzzing completion
- ✅ All tests pass with proper @Error directives

---

## ROI Analysis

**Investment**: 2-3 hours
**Return**:
- Complete frontend fuzzing coverage (psychological milestone)
- Validate 15 previously untested error types
- Prevent 5-10 potential production bugs
- Each bug avoided: 2-5 days debugging + customer impact

**Expected ROI**: 10-20x

---

## Risk Assessment

**Risks**: LOW

**Potential Issues:**
1. **Generic syntax complexity** - Mitigated by following existing patterns
2. **Error directive placement** - Mitigated by FuzzTestBase framework
3. **Constraint semantics unclear** - Mitigated by ErrorListener documentation

**Mitigation Strategy:**
- Study existing generic tests before implementation
- Test incrementally (one suite at a time)
- Verify each test file compiles with expected errors

---

## Next Steps After Completion

After completing these 15 tests:

1. ✅ Frontend fuzzing: **100% COMPLETE**
2. 🔄 Shift focus to **backend testing** (100-129 tests needed)
3. 🎯 Prioritize backend generic type IR/bytecode tests (25 tests, HIGH priority)

---

**Document Status**: READY FOR IMPLEMENTATION
**Estimated Completion**: 2-3 hours
**Priority**: MEDIUM (completes frontend)
**Confidence**: HIGH - Clear plan with proven patterns

**Last Updated**: 2025-11-17
