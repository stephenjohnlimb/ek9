# EK9 Mutation Testing - Complexity Limits Update

**Date:** 2025-11-27
**Issue:** Original nesting depth mutation plan conflicts with EK9's built-in complexity limits
**Status:** Plan Updated

---

## EK9 Complexity System Overview

### Hard Limits
```java
// From AcceptableConstructComplexityOrError.java
if (SymbolCategory.TYPE.equals(symbol.getCategory())) {
  errorIfTooComplex(symbol, complexityValue, 500);  // Classes: max 500
} else {
  errorIfTooComplex(symbol, complexityValue, 50);   // Functions: max 50
}
```

**Enforcement:**
- Functions/Methods/Operators: **Maximum complexity = 50**
- Types/Classes: **Maximum complexity = 500**
- Exceeding limit triggers: `EXCESSIVE_COMPLEXITY` error at PRE_IR_CHECKS phase

### Complexity Calculation Rules

**From ComplexityFuzzTest.java (lines 20-51):**

**Base Complexity:**
- Function/Method/Operator: +1
- Dynamic function: +2
- Class/Record/Trait: +1

**Incremental Complexity (partial list):**
- Each uninitialized variable (?): +1
- Each if statement: +1
- Each case in switch: +1
- Each loop (for/while/do-while): +1
- Each comparison operator (<, >, ==, etc.): +1
- Each boolean logic operator (and/or): +1
- Each guard expression: +1
- Each is-set check (?): +1
- Try block: +1
- Catch block: +1

---

## Impact on Nesting Depth Mutations

### Original Plan (INVALID - Would Exceed Limits)

**Session 3 originally proposed:**
```
nesting_control_010.ek9    # 10 levels → ~20 complexity (OK)
nesting_control_020.ek9    # 20 levels → ~40 complexity (OK)
nesting_control_050.ek9    # 50 levels → ~100 complexity (EXCEEDS 50 LIMIT ❌)
nesting_control_100.ek9    # 100 levels → ~200 complexity (EXCEEDS 50 LIMIT ❌)
```

**Problem:** 50+ nested if statements would trigger `EXCESSIVE_COMPLEXITY` error, making test fail.

### Updated Plan (VALID - Respects Limits)

**Session 3 (Valid Nesting Depth) - REVISED:**
```
nesting_control_005.ek9    # 5 levels → ~10 complexity (SAFE)
nesting_control_010.ek9    # 10 levels → ~20 complexity (SAFE)
nesting_control_015.ek9    # 15 levels → ~30 complexity (SAFE)
nesting_control_020.ek9    # 20 levels → ~40 complexity (SAFE, near boundary)
```

**Rationale:** Stay well under complexity limit of 50 for valid mutations.

---

## New Session: Excessive Complexity Testing (Invalid Mutations)

### Session 5B: Invalid Complexity Boundary Mutations - 2-3 hours

**Goal:** Validate EXCESSIVE_COMPLEXITY error detection at boundaries

**Test Matrix:**
- Boundary: 51 complexity (1 over limit)
- Moderate: 60 complexity (10 over limit)
- Extreme: 100 complexity (50 over limit)
- Class boundary: 501 complexity (1 over class limit)

**Corpus Structure:**
```
fuzzCorpus/mutations/invalid/excessiveComplexity/
├── function_complexity_051.ek9   # Exactly 51 complexity
├── function_complexity_060.ek9   # 60 complexity
├── function_complexity_100.ek9   # 100 complexity
├── operator_complexity_060.ek9   # Operator with 60 complexity
├── dynamic_function_complexity_070.ek9  # Dynamic function (base +2)
└── class_complexity_501.ek9      # Class with 501 complexity
```

**Example Test File (function_complexity_051.ek9):**
```ek9
#!ek9
defines module fuzztest.mutation.invalid.complexity.boundary051

  @Complexity: PRE_IR_CHECKS: FUNCTION: "BoundaryComplexity51": 51
  defines function
    BoundaryComplexity51()
      //Mutation: Create exactly 51 complexity (1 over limit)
      //Pattern: 50 if statements, each adds +1 complexity
      //Base function complexity: +1
      //50 if statements: +50
      //Total: 51 complexity

      @Error: PRE_IR_CHECKS: EXCESSIVE_COMPLEXITY
      value <- 0

      if value == 0  // +1 if, +1 comparison = +2
        value := 1
      if value == 1  // +2
        value := 2
      // ... repeat 48 more times
      if value == 48  // +2
        value := 49
```

**Test Class:**
```java
class InvalidExcessiveComplexityMutationTest extends FuzzTestBase {

  public InvalidExcessiveComplexityMutationTest() {
    super("mutations/invalid/excessiveComplexity", CompilationPhase.PRE_IR_CHECKS);
  }

  @Test
  void testExcessiveComplexityFailsConsistently() {
    assertTrue(runTests() != 0);
  }
}
```

**Success Criteria:**
- All complexity mutations fail with EXCESSIVE_COMPLEXITY error
- Error occurs at PRE_IR_CHECKS phase
- Boundary condition (51) correctly triggers error
- @Complexity directives match calculated values

---

## Updated Session Plan

### Original Session Order (8 sessions)
1. Valid: Identifier Length
2. Valid: Parameter Count
3. Valid: Nesting Depth ← **UPDATED (reduced max depth)**
4. Valid + Execution: Numeric Boundaries
5. Invalid: Type Resolution
6. Invalid: Parameter Mismatch
7. Invalid: Guards
8. Valid: Scale

### New Session Order (9 sessions)
1. Valid: Identifier Length
2. Valid: Parameter Count
3. Valid: Nesting Depth ← **REVISED (max 20 levels, not 100)**
4. Valid + Execution: Numeric Boundaries
5. Invalid: Type Resolution
6. Invalid: Parameter Mismatch
7. Invalid: Guards
8. **Invalid: Excessive Complexity** ← **NEW SESSION**
9. Valid: Scale

---

## Complexity Calculation Examples

### Example 1: 10 Nested If Statements
```ek9
defines function
  deeplyNested10()
    <- rtn as Boolean: true

    if depth > 0      // +1 if, +1 comparison
      if depth > 1    // +1 if, +1 comparison
        if depth > 2  // +1 if, +1 comparison
          // ... 10 levels
```

**Complexity Calculation:**
- Base function: +1
- 10 if statements: +10
- 10 comparison operators: +10
- **Total: ~21 complexity (SAFE)**

### Example 2: 25 Nested If Statements
```ek9
defines function
  deeplyNested25()
    <- rtn as Boolean: true

    if depth > 0      // +1 if, +1 comparison
      if depth > 1    // +1 if, +1 comparison
        // ... 25 levels
```

**Complexity Calculation:**
- Base function: +1
- 25 if statements: +25
- 25 comparison operators: +25
- **Total: ~51 complexity (EXCEEDS LIMIT ❌)**

---

## Revised Session 3: Valid Nesting Depth Mutations

### Updated Test Matrix

**Control Flow Nesting (if/while/for):**
- 5 levels: ~10 complexity (SAFE)
- 10 levels: ~20 complexity (SAFE)
- 15 levels: ~30 complexity (SAFE)
- 20 levels: ~40 complexity (SAFE, boundary)

**Generic Type Nesting (List of List of...):**
- 5 levels: minimal complexity impact (SAFE)
- 10 levels: minimal complexity impact (SAFE)
- 15 levels: minimal complexity impact (SAFE)
- 20 levels: minimal complexity impact (SAFE)

**Note:** Generic type nesting does NOT add to cyclomatic complexity (no control flow), so can test deeper nesting for type system stress.

**Expression Nesting:**
- 10 levels: arithmetic/logic nesting (SAFE)
- 20 levels: arithmetic/logic nesting (SAFE)

### Updated Corpus Structure

```
fuzzCorpus/mutations/valid/nestingDepth/
├── nesting_control_005.ek9       # 5-level if/while nesting
├── nesting_control_010.ek9       # 10-level if/while nesting
├── nesting_control_015.ek9       # 15-level if/while nesting
├── nesting_control_020.ek9       # 20-level if/while nesting (max)
├── nesting_generics_010.ek9      # 10-level generic nesting (List of List...)
├── nesting_generics_020.ek9      # 20-level generic nesting
├── nesting_generics_050.ek9      # 50-level generic nesting (no complexity impact)
└── nesting_expressions_020.ek9   # 20-level expression nesting
```

### Example Test File (nesting_control_020.ek9)

```ek9
#!ek9
defines module fuzztest.mutation.valid.nesting.depth.control20

  @Complexity: PRE_IR_CHECKS: FUNCTION: "deeplyNestedControl20": 41
  defines function
    deeplyNestedControl20()
      -> depth as Integer
      <- rtn as Boolean: true

      //20 nested if statements
      //Base: +1, each if: +1, each comparison: +1
      //Total: 1 + 20 + 20 = 41 complexity (UNDER 50 limit)

      if depth > 0
        if depth > 1
          if depth > 2
            if depth > 3
              if depth > 4
                if depth > 5
                  if depth > 6
                    if depth > 7
                      if depth > 8
                        if depth > 9
                          if depth > 10
                            if depth > 11
                              if depth > 12
                                if depth > 13
                                  if depth > 14
                                    if depth > 15
                                      if depth > 16
                                        if depth > 17
                                          if depth > 18
                                            if depth > 19
                                              rtn: true
```

---

## Summary of Changes

### What Changed:
1. **Session 3 (Valid Nesting Depth):**
   - Reduced max control flow nesting from 100 levels → **20 levels**
   - Keep deep generic type nesting (50+ levels) since no complexity impact
   - Cap control flow nesting at ~40 complexity (10 under limit for safety)

2. **New Session 5B (Invalid Excessive Complexity):**
   - Test EXCESSIVE_COMPLEXITY error detection
   - Validate boundary conditions (51, 60, 100+ complexity)
   - Test both functions and operators
   - Test class complexity (500 limit)

### What Stayed the Same:
- All other mutation categories unchanged
- Still ~50-60 mutation test files total
- Still ~8-9 JUnit test classes
- Still multi-session implementation approach

---

## Next Steps

**Immediate Actions:**
1. Update EK9_MUTATION_TESTING_MASTER_PLAN.md with complexity-aware nesting limits
2. Review updated Session 3 specification
3. Review new Session 5B specification
4. Proceed with Session 1 (Identifier Length) when ready

**Documentation Updates Required:**
- Update Session 3 in master plan (reduce max nesting depth)
- Add Session 5B to master plan (excessive complexity testing)
- Update session count from 8 → 9
- Update total effort estimate (+2-3 hours for Session 5B)

---

**Status:** ✅ **Complexity Analysis Complete - Plan Updated to Respect EK9 Limits**

**Key Lesson:** Always review language-specific constraints before planning mutation testing strategies.
