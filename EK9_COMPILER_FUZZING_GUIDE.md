# EK9 Compiler Fuzzing Guide

**Purpose:** Comprehensive guide to implementing and maintaining EK9 compiler fuzz tests
**Last Updated:** 2025-12-02

---

## Why We Fuzz

### "One Bite at the Cherry"

Programming language adoption is unforgiving:
- Developers will try EK9 **once**
- A buggy compiler experience is **fatal** to adoption
- No second chances to make a first impression

**Therefore:** Compiler must be **rock-solid** before public launch.

### What Fuzzing Validates

| Aspect | Goal | Example |
|--------|------|---------|
| **Correctness** | Accept valid EK9 code | Valid syntax parses, semantics type-check |
| **Robustness** | Reject invalid code gracefully | Clear errors, no cascading failures |
| **Resilience** | Handle adversarial input | No crashes, no infinite loops, no OOM |

---

## Architecture

### Directory Structure

```
compiler-main/src/test/
├── java/org/ek9lang/compiler/fuzz/
│   ├── FuzzTestBase.java              # Base class for all fuzz tests
│   ├── AbstractBodyConflictsFuzzTest.java
│   ├── DispatcherFuzzTest.java
│   └── ...
│
└── resources/fuzzCorpus/
    ├── abstractBodyConflicts/         # ABSTRACT_BUT_BODY_PROVIDED errors
    ├── dispatcher/                    # Dispatcher completeness errors
    ├── duplicateVariables/            # Duplicate symbol errors
    └── ...
```

**Key Principle:** Directories organized by **ERROR TYPE**, not compilation phase.
- Directory name = What you're testing (e.g., `duplicateVariables/`)
- Compilation phase = Specified in test class constructor
- Easy to find: "duplicate variable issue" → `duplicateVariables/`

### Cached Compiler Bootstrap (Performance)

**Traditional approach:** 3 seconds bootstrap × 1000 tests = 50 minutes
**EK9 approach:** 3 seconds bootstrap ONCE + 10ms deserialize × 1000 = 13 seconds

The `CompilableProgramSupplier` caches the bootstrap:
```java
// First test: Full bootstrap (3 seconds)
// Subsequent tests: Deserialize from cache (10ms)
```

---

## Implementing Fuzz Tests

### Base Class Pattern

```java
package org.ek9lang.compiler.fuzz;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MyErrorTypeFuzzTest extends FuzzTestBase {

  public MyErrorTypeFuzzTest() {
    // Directory under fuzzCorpus/, target phase
    super("myErrorType", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testMyErrorTypeRobustness() {
    assertTrue(runTests() != 0);
  }
}
```

### @Error Directive Meta-Validation

**Critical:** @Error directives validate that tests exercise intended code paths.

**Without directives (DANGEROUS):**
```ek9
defines claass BadProcessor  // TYPO: "claass"
  process() as abstract
    <- result as Boolean: true
```
- Test passes (no crash)
- FALSE CONFIDENCE: Parser rejected at "claass", abstract validation never ran

**With directives (CORRECT):**
```ek9
defines class BadProcessor
  @Error: SYMBOL_DEFINITION: ABSTRACT_BUT_BODY_PROVIDED
  process() as abstract
    <- result as Boolean: true
```
- Test validates exact error at exact phase
- If syntax error exists, directive mismatch fails the test

### Phase Requirements

| Phase | @Error Directives |
|-------|-------------------|
| PARSING | Not required (syntax errors unpredictable) |
| SYMBOL_DEFINITION+ | **Required** (semantic errors predictable) |

FuzzTestBase automatically enables directive checking for semantic phases.

---

## Pre-Fuzz Checklist

**CRITICAL:** Before creating any new fuzz test suite:

### 1. Search Existing Coverage
```bash
grep -r "ERROR_CODE" compiler-main/src/test/resources/examples/parseButFailCompile/
grep -r "ERROR_CODE" compiler-main/src/test/resources/fuzzCorpus/
```

### 2. Check Test Locations
- `parseButFailCompile/phase1/` - SYMBOL_DEFINITION errors
- `parseButFailCompile/phase2/` - EXPLICIT_TYPE_SYMBOL_DEFINITION errors
- `parseButFailCompile/phase3/` - FULL_RESOLUTION errors
- `fuzzCorpus/*/` - Existing fuzz tests

### 3. Verify Gap is Real
Read existing test files - understand what's actually tested before claiming a gap.

### 4. Document the Gap
Only proceed if tests add genuinely **NEW** mutation patterns.

**Session 2025-11-29 Example:**
Access Control (NOT_ACCESSIBLE), Reference Constraints (NOT_RESOLVED), and Abstract/Body (NOT_ABSTRACT_AND_NO_BODY_PROVIDED) tests were created and then **deleted** after discovering they duplicated existing `parseButFailCompile/` tests. Always check first!

---

## Test File Template

```ek9
#!ek9
<?-
  [Error Category] Mutation Test: [Specific Scenario]
  [Description of what this tests]

  Mutation: [What invalid pattern is being tested]
  Expected: [ERROR_CODE] at [PHASE]
-?>
defines module fuzz.category.scenario

  defines class

    TestClass

      @Error: PHASE: ERROR_CODE
      methodCausingError()
        <- rtn <- invalid_expression

//EOF
```

---

## When to Add vs Skip Fuzz Tests

### DO Add When:
- Zero existing tests for an error type
- Specific construct type missing from error category
- Edge cases not covered (e.g., 5+ level deep hierarchies)
- Error has HIGH crash risk (stack overflow, infinite loops)

### DO NOT Add When:
- Existing tests cover the error extensively (>5 instances)
- Error is well-covered in `parseButFailCompile/`
- Would duplicate existing coverage
- Error is variation of already-tested pattern

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

## Running Fuzz Tests

```bash
# Run all fuzz tests
mvn test -Dtest="*FuzzTest" -pl compiler-main

# Run specific suite
mvn test -Dtest=AbstractBodyConflictsFuzzTest -pl compiler-main

# Clean build (required if corpus changed)
mvn clean test -Dtest="*FuzzTest" -pl compiler-main
```

---

## Key Documents

- **FUZZING_MASTER_STATUS.md** - Current status, metrics, progress tracking
- **EK9_FUZZING_ROADMAP.md** - Future work, detailed guidelines, roadmap

---

**Remember:** Fuzzing without @Error directives is like unit tests without assertions - it runs, but doesn't verify anything meaningful.
