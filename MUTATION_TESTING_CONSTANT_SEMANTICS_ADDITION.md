# Mutation Testing: Constant Semantics Addition

**Date:** 2025-11-27
**Sessions Added:** 10A (Valid Constant Copy Semantics) + 10B (Invalid Constant Mutability)
**Status:** ✅ Plan Complete, Ready for Implementation

---

## Executive Summary

Added comprehensive testing for EK9's constant semantics, covering:
1. **Automatic copy-on-reference** for all 17 EK9 constant types
2. **Immutability enforcement** for constants, loop variables, and enumeration values
3. **Critical gap addressed:** Loop variable mutability (not previously tested)

---

## Key EK9 Constant Semantics

### 1. Copy-on-Reference (Automatic)

**What Developer Writes:**
```ek9
defines constant
  FIXED_INT <- 42

defines function
  example()
    x <- FIXED_INT  // Developer writes simple assignment
    x++             // Mutate the copy
```

**What IR Generates:**
```ek9
x <- FIXED_INT._copy()  // Compiler automatically inserts _copy() call
x++                      // Mutation works because x is a copy, not a reference
```

**Why This Works:**
- Constants can ONLY be EK9 built-in types
- All built-in types have guaranteed `_copy()` operator
- Copy is automatic and invisible to developer
- Preserves immutability while allowing practical use

### 2. Immutability Rules

| Type | Mutable? | Reason |
|------|----------|--------|
| Constants (`defines constant FIXED <- 42`) | ❌ No | Explicitly defined as constant |
| Loop control variables (`for i in 0...10`) | ❌ No | Managed by loop control mechanism |
| Enumeration values (`CardSuit.Hearts`) | ❌ No | Part of type definition |
| Guard variables (`if x <- getValue()`) | ✅ Yes | Just normal variable declarations |
| Function parameters | ✅ Yes | Normal variables (unless marked otherwise) |
| Copies of constants (`x <- FIXED`) | ✅ Yes | New variables created via `_copy()` |

---

## EK9 Constant Types Catalog

Complete list of types that can be constants (from grammar and SimpleConstants.ek9):

1. **Boolean**: `true`, `false`
2. **Character**: `':'`
3. **String**: `"text"`
4. **Integer**: `42`, `-2`
5. **Float**: `3.142`
6. **Binary**: `0b01110001`
7. **Time**: `12:00`, `12:00:01`
8. **Duration**: `P2Y`, `P2M`, `P-2D`, `PT2H`, `PT-2H`, `PT2M`, etc.
9. **Millisecond**: `250ms`
10. **Date**: `2000-01-01`
11. **DateTime**: `2018-01-31T01:30:00-05:00`
12. **Money**: `300000#USD`
13. **Color**: `#AB6F2B`
14. **Dimension**: `2m`
15. **RegEx**: `/pattern/`
16. **Version**: version literals
17. **Path**: path literals

**Total: 17 constant types** - one test file per type in Session 10A

---

## Session 10A: Valid Constant Copy Semantics

### Goal
Test EK9's automatic copy-on-reference behavior for ALL 17 constant types

### Test Strategy
For each constant type:
1. Define constant
2. Assign to variable (triggers automatic copy)
3. Mutate the copy
4. Verify copy was mutated
5. Verify original constant unchanged

### Corpus Structure
```
fuzzCorpus/mutations/valid/constantCopySemantics/
├── copy_boolean.ek9
├── copy_character.ek9
├── copy_string.ek9
├── copy_integer.ek9
├── copy_float.ek9
├── copy_binary.ek9
├── copy_time.ek9
├── copy_duration.ek9
├── copy_millisecond.ek9
├── copy_date.ek9
├── copy_datetime.ek9
├── copy_money.ek9
├── copy_color.ek9
├── copy_dimension.ek9
├── copy_regex.ek9
├── copy_version.ek9
└── copy_path.ek9
```

**Total: 17 test files**

### Test Class
```java
class ValidConstantCopySemanticsMutationTest extends FuzzTestBase {
  public ValidConstantCopySemanticsMutationTest() {
    super("mutations/valid/constantCopySemantics", CompilationPhase.PRE_IR_CHECKS, false);
  }

  @Test
  void testConstantCopySemanticsForAllTypes() {
    assertEquals(17, runTests());  // All 17 files should compile successfully
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult,
                                     final int numberOfErrors,
                                     final CompilableProgram program) {
    assertTrue(compilationResult, "Valid constant copy mutations should compile successfully");
    assertEquals(0, numberOfErrors, "Valid constant copy mutations should have zero errors");
  }
}
```

### Success Criteria
- ✅ All 17 constant type test files compile successfully
- ✅ Copy-on-reference works for all EK9 literal types
- ✅ Mutations on copies succeed (verifies copy was created)
- ✅ Original constants remain unchanged

---

## Session 10B: Invalid Constant Mutability

### Goal
Test immutability enforcement for constants, loop control variables, and enumeration values

### Test Matrix

**1. Direct Constant Mutation (5 files)**
- direct_integer_increment.ek9: `FIXED_INT++`
- direct_string_concat.ek9: `FIXED_STR += "x"`
- direct_date_add.ek9: `FIXED_DATE += P1D`
- direct_boolean_assign.ek9: `FIXED_BOOL := false`
- direct_float_multiply.ek9: `FIXED_FLT *= 2.0`

**2. Loop Control Variables (4 files) - CRITICAL GAP**
- loop_var_for_range_increment.ek9: `for i in 0...10, then i++`
- loop_var_for_range_assign.ek9: `for i in 0...10, then i := 5`
- loop_var_for_in_increment.ek9: `for v in list, then v++`
- loop_var_for_in_assign.ek9: `for v in list, then v := newVal`

**3. Enumeration Values (1 file)**
- enum_value_assignment.ek9: `CardSuit.Hearts := CardSuit.Diamonds`

**Total: 10 test files**

### Corpus Structure
```
fuzzCorpus/mutations/invalid/constantMutability/
├── direct_integer_increment.ek9
├── direct_string_concat.ek9
├── direct_date_add.ek9
├── direct_boolean_assign.ek9
├── direct_float_multiply.ek9
├── loop_var_for_range_increment.ek9
├── loop_var_for_range_assign.ek9
├── loop_var_for_in_increment.ek9
├── loop_var_for_in_assign.ek9
└── enum_value_assignment.ek9
```

### Test Class
```java
class InvalidConstantMutabilityMutationTest extends FuzzTestBase {
  public InvalidConstantMutabilityMutationTest() {
    super("mutations/invalid/constantMutability", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testConstantImmutabilityEnforced() {
    assertTrue(runTests() != 0, "All constant mutation attempts should fail");
  }
}
```

### Success Criteria
- ✅ All 10 mutation files fail with NOT_MUTABLE error
- ✅ Errors occur at FULL_RESOLUTION phase (consistent with badMutation.ek9)
- ✅ Loop variable mutation consistently rejected across for-range and for-in
- ✅ Error messages clearly indicate what cannot be mutated

---

## Critical Gap Addressed

### Loop Variable Immutability

**Previously:** No tests existed for loop variable mutation attempts

**Now Testing:**
```ek9
for i in 0...10
  i++  // Should fail with NOT_MUTABLE

for value in [1, 2, 3]
  value++  // Should fail with NOT_MUTABLE
```

**Why Critical:**
- In C++/Java, loop variables ARE mutable (leads to bugs and confusion)
- EK9's immutable loop variables prevent entire class of bugs
- Loop control mechanism manages the variable - developer cannot interfere
- Common mistake for developers coming from C++/Java backgrounds

---

## Comparison with Existing Tests

### badMutation.ek9 (existing)
- **Location:** `examples/parseButFailCompile/phase3/badConstantUse/`
- **Coverage:** Comprehensive constant mutation testing
- **Operators tested:** `++`, `--`, `+=`, `:=`, `:=?`, `:=:`, `:~:`, `:^:`
- **Types tested:** Integer, String, Date, Enumeration values
- **Loop variables:** ❌ Not tested

### Session 10B (new)
- **Location:** `fuzzCorpus/mutations/invalid/constantMutability/`
- **Coverage:** Focused mutation testing corpus
- **Operators tested:** `++`, `+=`, `:=`, `*=`
- **Types tested:** Integer, String, Date, Boolean, Float, Enumeration values
- **Loop variables:** ✅ Tested (4 files - critical gap addressed)

**Relationship:** Session 10B complements badMutation.ek9 by adding loop variable testing and organizing tests in mutation corpus format

---

## Expected Errors

All Session 10B tests should fail with:
- **Phase:** FULL_RESOLUTION
- **Error Type:** NOT_MUTABLE
- **Example Message:** `'FIXED_INT as Integer' cannot be changed: not mutable`

Consistent with existing error behavior in `badMutation.ek9`

---

## workarea.ek9 Analysis

**File:** `examples/parseButFailCompile/workingarea/workarea.ek9`

**Content:**
```ek9
defines constant
  FIXED <- 20

defines function
  modifyCopyOfFixed()
    var <- FIXED  // Copy created automatically
    var++         // Mutate copy - VALID
    assert var?

  modifyFixedDirectly()
    FIXED++       // Direct mutation - INVALID
    assert FIXED?
```

**Current Status:** Test is failing (expected - demonstrates NOT_MUTABLE error)

**Relationship to Session 10:**
- Demonstrates copy semantics (Session 10A concept)
- Shows direct mutation error (Session 10B concept)
- Can remain as-is or be updated with proper @Error directive

---

## Implementation Timeline

### Session 10A: Valid Constant Copy Semantics (2-3 hours)
1. Create directory: `fuzzCorpus/mutations/valid/constantCopySemantics/`
2. Create 17 EK9 test files (one per constant type)
3. Create JUnit test class: `ValidConstantCopySemanticsMutationTest.java`
4. Run tests: `mvn test -Dtest=ValidConstantCopySemanticsMutationTest -pl compiler-main`
5. Verify all 17 files compile successfully

### Session 10B: Invalid Constant Mutability (2-3 hours)
1. Create directory: `fuzzCorpus/mutations/invalid/constantMutability/`
2. Create 10 EK9 test files with @Error directives
3. Create JUnit test class: `InvalidConstantMutabilityMutationTest.java`
4. Run tests: `mvn test -Dtest=InvalidConstantMutabilityMutationTest -pl compiler-main`
5. Verify all 10 files fail with NOT_MUTABLE error

---

## Strategic Value

### For EK9 Language
- Validates unique constant copy-on-reference semantics
- Tests all 17 constant types comprehensively
- Ensures immutability enforcement consistency

### For Compiler Robustness
- Addresses critical gap (loop variable immutability)
- Validates IR generation of automatic `_copy()` calls
- Tests error detection for all immutability violations

### For Developer Safety
- Loop variable immutability prevents common C++/Java bug patterns
- Clear error messages guide developers to correct usage
- Copy semantics enable practical use of constants without sacrificing immutability

---

## Updated Master Plan Summary

**Sessions:** 9 → 11 (added 10A + 10B)
**Test Files:** ~55-65 → ~82-92
**JUnit Classes:** ~9 → ~11
**Total Hours:** ~22-27 → ~26-33

**Directory Structure:**
```
fuzzCorpus/mutations/
├── valid/
│   ├── identifierLength/         # Session 1
│   ├── parameterCount/            # Session 2
│   ├── nestingDepth/              # Session 3
│   ├── numericBoundaries/         # Session 4
│   ├── scale/                     # Session 9
│   └── constantCopySemantics/     # Session 10A ⭐ NEW
└── invalid/
    ├── typeResolution/            # Session 5
    ├── parameterMismatch/         # Session 6
    ├── guards/                    # Session 7
    ├── excessiveComplexity/       # Session 8
    └── constantMutability/        # Session 10B ⭐ NEW
```

---

## Key Insights from Planning Session

1. **Guard variables are NOT constants** - they're just normal variable declarations in guard position
2. **Copy-on-reference is automatic** - developer writes `x <- FIXED`, IR generates `x <- FIXED._copy()`
3. **17 constant types exist** - all must be tested for comprehensive coverage
4. **Loop variable immutability is critical** - common bug source in other languages, not previously tested
5. **Constants only work for built-in types** - guarantees `_copy()` operator exists

---

**Status:** ✅ **Plan Complete, Ready for Implementation**

**Next Steps:**
1. Implement Session 10A (17 constant copy semantics tests)
2. Implement Session 10B (10 constant mutability tests)
3. Update MUTATION_TESTING_FINAL_STATUS.md with new totals

**Documentation References:**
- **EK9_MUTATION_TESTING_MASTER_PLAN.md** - Complete 11-session plan
- **MUTATION_TESTING_FINAL_STATUS.md** - Will need updating after implementation
- **SimpleConstants.ek9** - Reference for all 17 constant types
- **badMutation.ek9** - Existing comprehensive constant mutation tests
