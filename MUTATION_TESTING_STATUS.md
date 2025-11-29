# EK9 Mutation Testing - Status and Plan

**Date:** 2025-11-27
**Status:** ✅ Planning Complete - Ready for Implementation

---

## Summary

Completed comprehensive mutation testing plan based on:
1. **Infrastructure Analysis**: Reviewed PhasesTest.java, FuzzTestBase.java, existing Bad*Test.java patterns
2. **Phase-Specific Testing**: Understood @Error directive validation at specific compilation phases
3. **JUnit Multi-threaded Pattern**: Will use existing FuzzTestBase infrastructure (not test.sh)

---

## Key Deliverable

**EK9_MUTATION_TESTING_MASTER_PLAN.md** - Complete 8-session implementation plan including:

### Two-Track Testing Approach

**1. Valid Mutations** (should compile after mutation):
- Identifier length variations (1 char → 500 chars, Unicode)
- Parameter count variations (0 → 200 parameters)
- Nesting depth variations (5 → 100 levels)
- Numeric boundaries WITH EXECUTION (Integer.MIN/MAX, Float boundaries, EK9 'unset')
- Scale testing (1000+ functions, 500+ term expressions)

**2. Invalid Mutations** (should fail with SAME error after mutation):
- Type resolution errors (Integer → Integr typo)
- Parameter mismatch errors (too few/many, wrong type)
- Guard validation errors (wrong operator, wrong context)

### Infrastructure Pattern Understood

**For Invalid Mutations with @Error Directives:**
```java
class InvalidTypeResolutionMutationTest extends FuzzTestBase {
  public InvalidTypeResolutionMutationTest() {
    super("mutations/invalid/typeResolution", CompilationPhase.SYMBOL_DEFINITION);
  }

  @Test
  void testInvalidMutationsFailConsistently() {
    assertTrue(runTests() != 0);  // Ensures errors detected
    // FuzzTestBase automatically validates @Error directives
  }
}
```

**For Valid Mutations (should compile):**
```java
class ValidIdentifierLengthMutationTest extends FuzzTestBase {
  public ValidIdentifierLengthMutationTest() {
    super("mutations/valid/identifierLength", CompilationPhase.PRE_IR_CHECKS);
  }

  @Test
  void testValidMutationsStillCompile() {
    assertEquals(0, runTests());  // Ensures NO errors
  }
}
```

**@Error Directive Format in EK9 Files:**
```ek9
//Mutation: Integer → Integr (typo)
@Error: SYMBOL_DEFINITION: TYPE_NOT_RESOLVED
value <- Integr()
```

### Implementation Roadmap

**Week 1: Valid Mutations (Foundation)**
- Session 1: Identifier length (2-3 hours) → 8 test files
- Session 2: Parameter count (2-3 hours) → 8 test files
- Session 3: Nesting depth (2-3 hours) → 8 test files

**Week 2: Execution + Invalid Mutations**
- Session 4: Numeric boundaries WITH EXECUTION (3-4 hours) → 6 test files
- Session 5: Invalid type resolution (2-3 hours) → 5 test files
- Session 6: Invalid parameter mismatch (2-3 hours) → 5 test files

**Week 3: Advanced Mutations**
- Session 7: Invalid guard mutations (2-3 hours) → 5 test files
- Session 8: Invalid excessive complexity (2-3 hours) → 6 test files
- Session 9: Scale mutations (3-4 hours) → 8 test files

**Total:** ~22-27 hours across 9 sessions, ~55-65 mutation test files, ~9 new JUnit test classes

**⚠️ CRITICAL UPDATE (2025-11-27):** Plan revised to respect EK9's complexity limits:
- Functions: max complexity = 50 (enforced at PRE_IR_CHECKS phase)
- Session 3 (Nesting Depth): Control flow nesting limited to 20 levels (~41 complexity)
- Session 8 (NEW): Tests EXCESSIVE_COMPLEXITY error detection at boundaries

---

## Key Corrections Made

During plan development, several corrections were made based on your feedback:

1. ✅ **NaN → unset**: EK9 uses 'unset' tri-state semantics, not Java NaN
2. ✅ **test.sh → JUnit**: Use existing multi-threaded JUnit infrastructure with stdout capture
3. ✅ **Phase-specific testing**: Invalid mutations must run to correct phase and validate @Error directives
4. ✅ **Execution validation**: Boundary tests MUST execute bytecode to prove correctness (not just compile)

---

## Directory Structure

```
fuzzCorpus/mutations/
├── valid/
│   ├── identifierLength/      # Session 1: 8 files
│   ├── parameterCount/         # Session 2: 8 files
│   ├── nestingDepth/           # Session 3: 8 files
│   ├── numericBoundaries/      # Session 4: 6 files (WITH EXECUTION)
│   └── scale/                  # Session 8: 8 files
└── invalid/
    ├── typeResolution/         # Session 5: 5 files
    ├── parameterMismatch/      # Session 6: 5 files
    └── guards/                 # Session 7: 5 files

compiler-main/src/test/java/org/ek9lang/compiler/fuzz/
├── ValidIdentifierLengthMutationTest.java
├── ValidParameterCountMutationTest.java
├── ValidNestingDepthMutationTest.java
├── ValidNumericBoundariesMutationTest.java    # Includes bytecode execution
├── ValidScaleMutationTest.java
├── InvalidTypeResolutionMutationTest.java
├── InvalidParameterMismatchMutationTest.java
└── InvalidGuardMutationTest.java
```

---

## Success Metrics

**Per-Session:**
- Valid mutations: 100% compile successfully (error count = 0)
- Invalid mutations: 100% fail with expected error (@Error directives match)
- No crashes, no hangs, clear error messages

**Overall:**
- ~50-60 mutation test files created
- ~8 new JUnit test classes
- ~366 → ~500 total fuzz tests
- Zero regression (existing tests continue passing)

---

## Expected Discoveries

**Robustness Findings:**
1. Identifier length limits (may find undocumented limits)
2. Parameter count limits (stack depth issues at 200+)
3. Nesting depth limits (stack overflow at 100+)
4. Numeric boundary edge cases
5. Error message consistency across contexts
6. Performance cliffs with large files

**Compiler Hardening:**
- Add explicit limits with clear error messages
- Improve error recovery
- Add timeout mechanisms
- Optimize parser/symbol table for scale

---

## Next Steps

**Immediate Action:**
1. Review EK9_MUTATION_TESTING_MASTER_PLAN.md for complete details
2. Approve or request modifications
3. Begin Session 1: Identifier Length Mutations

**Validation Command:**
```bash
# After each session
mvn test -Dtest=Valid*MutationTest -pl compiler-main

# Verify no regressions
mvn test -pl compiler-main
```

---

**Status:** ✅ **Planning Complete - Awaiting Approval to Begin Implementation**

**Recommendation:** Review EK9_MUTATION_TESTING_MASTER_PLAN.md and approve Session 1 start.

---

## Documentation References

**Master Plan:** EK9_MUTATION_TESTING_MASTER_PLAN.md (complete 8-session implementation plan)
**Infrastructure:** FuzzTestBase.java, PhasesTest.java (existing patterns followed)
**Related:** EK9_ADVANCED_FUZZING_STRATEGIES.md (strategic overview)
