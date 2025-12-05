# EK9 Compiler Fuzzing Status

**Last Updated:** 2025-12-05
**Status:** ✅ Frontend Complete (100%) | ✅ Runtime Fuzz Testing Active | 🔨 Backend In Development

---

## Quick Reference

### Current Metrics

| Metric | Count |
|--------|-------|
| Compile-time fuzz tests | 544 EK9 files |
| Runtime fuzz tests | 32 EK9 files |
| Total fuzz corpus | 576 EK9 files |
| Test suites | 127 classes |
| Frontend error coverage | 205/205 (100%) |
| Bugs found via fuzzing | 9 |

### Key Directories

```
compiler-main/src/test/
├── java/org/ek9lang/compiler/fuzz/
│   ├── FuzzTestBase.java                    # Base class for compile-time tests
│   ├── *FuzzTest.java                       # 95 compile-time test suites
│   └── runtime/
│       └── *RuntimeFuzzTest.java            # 32 runtime test suites
│
└── resources/fuzzCorpus/
    ├── [errorCategory]/                     # Compile-time test corpus (544 files)
    │   └── *.ek9                            # Tests with @Error directives
    └── runtimeFuzz/
        └── [testName]/                      # Runtime test corpus (32 dirs)
            ├── [testName].ek9               # EK9 program
            └── expected_output.txt          # Expected stdout/stderr
```

---

## Methodology: AI-Assisted Intelligent Fuzzing

### Why This Approach

EK9 uses **reasoning-based fuzzing** rather than brute-force random generation:

| Aspect | Random Fuzzing | AI-Assisted (EK9) |
|--------|---------------|-------------------|
| Volume | 100k-1M+ tests | 555 targeted tests |
| Coverage | 70-85% typical | 100% frontend |
| Bug finding | Needle in haystack | Design test to expose weakness |
| Documentation | None | Every test has clear purpose |

### Bugs Found Through Reasoning

| Bug | How Found |
|-----|-----------|
| FOR_RANGE direction/BY mismatch | Reasoned: "What if BY sign doesn't match direction?" |
| Constant mutability (methods not checked) | Reasoned about all mutation paths |
| Method chaining `(a.+(b)).*(c)` invalid | Explored dual-form edge cases |
| `not` has no method form | Systematically tested operator forms |
| ABS/SQRT grammar issues | Tested unary operators systematically |
| E11011 EXCESSIVE_NESTING | Designed tests to stress nesting limits |
| Default/abstract method defect | Targeted fuzzing of new feature |
| `final` is reserved keyword | Discovered during test creation |
| Exception propagates after catch (Bug #3) | Try/catch/finally nesting variation testing |

---

## Compile-Time Fuzz Testing

### Pattern: FuzzTestBase

```java
class MyErrorTypeFuzzTest extends FuzzTestBase {
  public MyErrorTypeFuzzTest() {
    super("myErrorType", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testMyErrorTypeRobustness() {
    assertTrue(runTests() != 0);
  }
}
```

### @Error Directive Requirement

**Critical:** Semantic phase tests MUST use `@Error` directives to validate correct error detection.

```ek9
defines class BadProcessor
  @Error: SYMBOL_DEFINITION: ABSTRACT_BUT_BODY_PROVIDED
  process() as abstract
    <- result as Boolean: true
```

Without directives, a typo could cause parse failure - test "passes" but never exercises intended code path.

### Running Compile-Time Tests

```bash
mvn test -Dtest="*FuzzTest" -pl compiler-main        # All fuzz tests
mvn test -Dtest=AbstractBodyConflictsFuzzTest -pl compiler-main  # Specific suite
```

---

## Runtime Fuzz Testing

### Pattern: AbstractExecutableBytecodeTest

```java
class ForRangeUnsetStartRuntimeFuzzTest extends AbstractExecutableBytecodeTest {
  public ForRangeUnsetStartRuntimeFuzzTest() {
    super("/fuzzCorpus/runtimeFuzz/forRangeUnsetStart",
        "fuzz.runtime.forrange.unsetstart",
        "ForRangeUnsetStart",
        List.of(new SymbolCountCheck("fuzz.runtime.forrange.unsetstart", 1)));
  }
}
```

### Expected Output Validation

For normal output:
```
1
3
5
7
9
Done
```

For expected exceptions:
```
Exception in thread "main" java.lang.AssertionError: For-range 'start' value must be set
	at fuzz.runtime.forrange.unsetstart.ForRangeUnsetStart._main(forRangeUnsetStart.ek9)
	at ek9.Main.main(Unknown Source)
```

### Current Runtime Coverage

**FOR_RANGE Tests (11 tests):**

| Test | Purpose |
|------|---------|
| ForRangeDescendingRuntimeFuzzTest | Descending range (10 → 1) |
| ForRangeWithByValueRuntimeFuzzTest | BY clause (1, 3, 5, 7, 9) |
| ForRangeWrongDirectionByRuntimeFuzzTest | Direction/BY mismatch |
| ForRangeDescendingWrongByRuntimeFuzzTest | Descending + positive BY |
| ForRangeEmptyRangeRuntimeFuzzTest | Single-value range |
| ForRangeSingleIterationRuntimeFuzzTest | Single iteration |
| ForRangeNegativeRangeRuntimeFuzzTest | Negative to positive |
| GuardAssignmentUnsetRuntimeFuzzTest | Guard with unset variable |
| ForRangeUnsetStartRuntimeFuzzTest | AssertionError: start unset |
| ForRangeUnsetEndRuntimeFuzzTest | AssertionError: end unset |
| ForRangeUnsetByRuntimeFuzzTest | AssertionError: by unset |

**Guard/Control Flow Tests (4 tests):**

| Test | Purpose |
|------|---------|
| IfGuardUnsetRuntimeFuzzTest | IF body skipped when guard unset |
| WhileGuardUnsetRuntimeFuzzTest | WHILE never enters when guard unset |
| SwitchGuardUnsetRuntimeFuzzTest | SWITCH skipped when guard unset |
| ForInEmptyListRuntimeFuzzTest | FOR-IN skips empty collection |

**Try/Catch/Finally Tests (17 tests):**

| Test | Purpose |
|------|---------|
| **Nesting Variations** | |
| TryNestedInnerCaughtRuntimeFuzzTest | Inner catch handles, outer not triggered |
| TryNestedPropagatesRuntimeFuzzTest | Exception propagates through inner finally |
| TryNestedBothFinallyRuntimeFuzzTest | Both finally blocks execute in order |
| TryNestedCatchThrowsRuntimeFuzzTest | Catch throws new exception |
| TryNestedFinallyThrowsRuntimeFuzzTest | Finally throws exception |
| TryNested3LevelFinallyRuntimeFuzzTest | 2-level with finally propagation |
| **Multiple Catch Ordering** | |
| TryMultipleCatchFirstRuntimeFuzzTest | First exception type matched |
| TryMultipleCatchSecondRuntimeFuzzTest | Second exception type matched |
| TryMultipleCatchFallbackRuntimeFuzzTest | Base Exception catches as fallback |
| TryMultipleCatchNoneMatchRuntimeFuzzTest | None match, exception propagates |
| **Finally Guarantee** | |
| TryFinallyCatchThrowsRuntimeFuzzTest | Finally runs when catch throws |
| TryFinallyOverridesRuntimeFuzzTest | Finally exception overrides try exception |
| TryFinallyNormalThrowsRuntimeFuzzTest | Finally throws when try succeeds |
| **Expression Form** | |
| TryExpressionCatchReturnsRuntimeFuzzTest | Try expression returns value from catch |
| TryExpressionBothPathsRuntimeFuzzTest | Both success and exception paths |
| **Guard Integration** | |
| TryGuardUnsetSkipsAllRuntimeFuzzTest | Guard unset skips entire try block |
| TryGuardSetExecutesRuntimeFuzzTest | Guard set executes try body |

### Running Runtime Tests

```bash
mvn test -Dtest="*RuntimeFuzzTest" -pl compiler-main
```

### EK9 Exception Handling Syntax Notes

**Single catch per try:** EK9 does NOT support multiple catch blocks like Java. To catch multiple exception types, use nested tries:

```ek9
// WRONG - EK9 doesn't support this
try
  throw ex
catch
  -> e as ExceptionA
  handle(e)
catch                    // ERROR: No viable alternative
  -> e as ExceptionB
  handle(e)

// CORRECT - Use nested tries
try
  try
    throw ex
  catch
    -> e as ExceptionA
    handle(e)
catch
  -> e as ExceptionB
  handle(e)
```

---

## Runtime vs Bytecode Test Design

### Separation of Concerns

| Test Type | Location | Purpose | Verification | Directives |
|-----------|----------|---------|--------------|------------|
| Bytecode | `bytecodeGeneration/` | Verify bytecode structure | `@BYTECODE` | Required |
| Runtime Fuzz | `runtimeFuzz/` | Verify execution behavior | `expected_output.txt` | None |

**Key insight:** Bytecode tests verify the compiler generates correct instructions. Runtime tests verify the program behaves correctly when executed. These are complementary - the FOR_RANGE bug proved that correct bytecode doesn't guarantee correct behavior.

### No @BYTECODE in Runtime Fuzz Tests

Runtime fuzz tests should be **simple programs** without `@BYTECODE` directives:
- Bytecode structure is already verified in `bytecodeGeneration/` tests
- `expected_output.txt` IS the specification for correct behavior
- Simple tests are easier to read, understand, and maintain
- Focus is on **what the program does**, not how bytecode is structured

### Variation Factors Checklist

When designing runtime fuzz tests, systematically vary these factors:

| Factor | Variations to Test |
|--------|-------------------|
| **Guard state** | SET with value, UNSET (tri-state), edge values (0, empty) |
| **Collection size** | Empty (0), single (1), many (N) |
| **Range direction** | Ascending, descending, equal (start == end) |
| **BY clause** | Positive, negative, sign mismatches direction |
| **Exception paths** | No exception, caught by type, caught by parent, propagated, finally |
| **Operand state** | Both SET, left UNSET, right UNSET, both UNSET |

### Why EK9 Needs Runtime Testing

EK9 has unique semantics that are **runtime behaviors**:
1. **Tri-state semantics** - `_isSet()` checked at runtime, not compile-time
2. **Guard expressions** - Body execution depends on runtime `_isSet()` check
3. **Polymorphic dispatch** - Correct method selected at runtime based on type
4. **Exception handling** - Handler selection is inherently runtime

---

## Future Opportunities

### Runtime Fuzzing Expansion

**Completed:**

| Category | Status | Tests |
|----------|--------|-------|
| Guard unset in IF | ✅ Complete | 1 |
| Guard unset in WHILE | ✅ Complete | 1 |
| Guard unset in SWITCH | ✅ Complete | 1 |
| Empty collection FOR-IN | ✅ Complete | 1 |
| Exception paths | ✅ Complete | 17 |

**Pending - High Priority:**

| Category | Description | Priority |
|----------|-------------|----------|
| Try-with-resources | Resource acquisition/release with exceptions | HIGH |
| Collection boundaries | Dict missing key, List out of bounds | HIGH |
| Operator boundaries | MAX_INT + 1, division by zero, overflow | MEDIUM |
| Stream execution | cat → filter → map → collect end-to-end | MEDIUM |
| Dynamic constructs | Dynamic class/function closure capture | MEDIUM |

**Known Bugs to Create Failing Tests For:**

| Bug | Description | Location |
|-----|-------------|----------|
| Bug #3 | Exception propagates after catch when outer try has both catch AND finally | `EXCEPTION_HANDLING_BUGS.md` |
| Bug #2 | Resource close() exceptions are uncaught | `EXCEPTION_HANDLING_BUGS.md` |

**The ratio opportunity:** 544 compile-time tests vs 32 runtime tests. As backend matures, each compile-time error has a "valid twin" that should execute correctly.

### Systematic Coverage

Every control flow construct should have runtime tests for:
1. Normal execution path
2. Edge cases (empty, single, boundary)
3. Unset/tri-state variations
4. Error conditions

---

## Pre-Fuzz Checklist

Before creating new fuzz tests:

1. **Search existing coverage:**
   ```bash
   grep -r "ERROR_CODE" compiler-main/src/test/resources/examples/parseButFailCompile/
   grep -r "ERROR_CODE" compiler-main/src/test/resources/fuzzCorpus/
   ```

2. **Check test locations:**
   - `parseButFailCompile/phase1/` - SYMBOL_DEFINITION errors
   - `parseButFailCompile/phase2/` - EXPLICIT_TYPE_SYMBOL_DEFINITION errors
   - `parseButFailCompile/phase3/` - FULL_RESOLUTION errors
   - `fuzzCorpus/*/` - Existing fuzz tests

3. **Verify gap is real** - Read existing tests before claiming a gap

4. **Document the gap** - Only proceed if tests add genuinely NEW patterns

---

## Compilation Phases Reference

| Phase | Name | Tests For |
|-------|------|-----------|
| 0 | PARSING | Syntax errors, malformed code |
| 1 | SYMBOL_DEFINITION | Duplicate symbols, abstract/body conflicts |
| 2 | DUPLICATION_CHECK | Duplicate definitions |
| 3 | REFERENCE_CHECKS | Missing symbols, circular references |
| 4 | EXPLICIT_TYPE_SYMBOL_DEFINITION | Invalid type names, type resolution |
| 5 | TYPE_HIERARCHY_CHECKS | Circular inheritance, trait conflicts |
| 6 | FULL_RESOLUTION | Generics, dispatcher completeness |
| 7 | POST_RESOLUTION_CHECKS | Generic operator constraints |
| 8 | PRE_IR_CHECKS | Flow analysis, complexity limits |

---

## Coverage Summary

### Frontend (Phases 0-8): ✅ 100% Complete

| Phase | Status |
|-------|--------|
| Phase 0 (PARSING) | 119 syntax fuzz tests |
| Phase 1-6 (Semantic) | 370+ tests including all constraint types |
| Phase 7 (POST_RESOLUTION) | 10 generic operator tests |
| Phase 8 (PRE_IR) | 24 flow analysis tests |
| Mutation testing | 48 valid pattern tests |
| Complex expressions | 16 dual-form/nesting tests |

### Backend (Phases 10-14): 🔨 Active Development

| Category | Count |
|----------|-------|
| IR Generation tests | 154 with @IR directives |
| Bytecode tests | 96 with @BYTECODE directives |
| E2E execution tests | 91 test.sh scripts |
| Runtime fuzz tests | 32 (FOR_RANGE + guards + try/catch/finally) |

---

**Remember:** Fuzzing without @Error directives (compile-time) or expected_output.txt (runtime) is like unit tests without assertions - it runs but doesn't verify anything meaningful.
