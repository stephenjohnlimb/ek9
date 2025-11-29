# Service Fuzz Test Implementation Workflow

**Purpose:** Step-by-step checklist for implementing 40 service validation fuzz tests
**Strategy:** Test each scenario individually in workarea.ek9 before adding to corpus
**Bonus:** Capture parse errors as additional test coverage

---

## 📋 Quick Reference

**Key Files:**
- **Scratch Pad:** `compiler-main/src/test/resources/examples/parseButFailCompile/workingarea/workarea.ek9`
- **Test Runner:** `compiler-main/src/test/java/org/ek9lang/compiler/main/WorkingAreaTest.java`
- **Target Corpus:** `compiler-main/src/test/resources/fuzzCorpus/serviceValidation/`
- **Parse Error Corpus:** `compiler-main/src/test/resources/fuzzCorpus/blockLevelSyntax/`

**Key Commands:**
```bash
# Run single test (fast feedback)
mvn test -Dtest=WorkingAreaTest -pl compiler-main

# Check for existing parse errors
grep -r "pattern" compiler-main/src/test/resources/fuzzCorpus/blockLevelSyntax/

# Check for existing semantic errors
grep -r "@Error.*SERVICE_" compiler-main/src/test/resources/examples/parseButFailCompile/
```

---

## 🔄 Iteration Workflow (Repeat 40 Times)

### **Step 1: Write Test in Workarea**

**Source:** Use detailed plan from `SERVICE_FUZZ_TEST_DETAILED_PLAN.md`

**Action:**
```bash
# Edit workarea.ek9 with SINGLE test scenario
vim compiler-main/src/test/resources/examples/parseButFailCompile/workingarea/workarea.ek9
```

**Example (Test #1 - service_uri_multiple_variables.ek9):**
```ek9
#!ek9
defines module fuzztest.service.uri.multiple

  defines service
    <?- Service URI with multiple path variables -?>
    @Error: SYMBOL_DEFINITION: SERVICE_URI_WITH_VARS_NOT_SUPPORTED
    ApiService :/api/{version}/{tenant}

      index() :/
        <- response as HTTPResponse: () with trait of HTTPResponse
          override content()
            <- rtn as String: "OK"
```

---

### **Step 2: Run WorkingAreaTest**

```bash
mvn test -Dtest=WorkingAreaTest -pl compiler-main
```

**Wait for result...**

---

### **Step 3A: IF PARSE ERROR (Phase 0)**

**Indicators:**
- Error message says "Syntax" or "parse"
- Line like: `EK9Comp : Syntax : 'workarea.ek9' on line X position Y:`

**Decision Tree:**

```
PARSE ERROR DETECTED
        ↓
Is this a NEW parse error?
        ↓
  ┌─────┴─────┐
  ▼           ▼
 YES         NO
  ↓           ↓
SAVE IT    FIX SYNTAX
  ↓           ↓
  └─────┬─────┘
        ↓
    GO TO STEP 2
```

**Check if NEW:**
```bash
# Search for similar parse errors
grep -r "service.*missing" compiler-main/src/test/resources/fuzzCorpus/blockLevelSyntax/
grep -r "similar keyword" compiler-main/src/test/resources/examples/parseButFailCompile/
```

**If NEW → Save to Parse Error Corpus:**
```bash
# Create descriptive filename based on error
FILE="compiler-main/src/test/resources/fuzzCorpus/blockLevelSyntax/service_[describe_error].ek9"

# Copy workarea.ek9 to parse error corpus
cp compiler-main/src/test/resources/examples/parseButFailCompile/workingarea/workarea.ek9 $FILE

# Add comment documenting the accidental discovery
vim $FILE  # Add header comment explaining parse error
```

**Example Parse Error Save:**
```ek9
#!ek9
<?-
  Accidental discovery during service fuzz testing (2025-11-12).
  Tests service definition with malformed syntax.
  Parse error: [describe specific syntax error]
-?>
defines module fuzztest.parse.service.error

  [THE BROKEN CODE THAT CAUSED PARSE ERROR]
```

**Then Fix Syntax in workarea.ek9:**
```bash
vim compiler-main/src/test/resources/examples/parseButFailCompile/workingarea/workarea.ek9
# Fix the syntax error
```

**Re-run from Step 2**

---

### **Step 3B: IF SEMANTIC ERROR (Phase 1-6)**

**Indicators:**
- Error message says phase name: `SYMBOL_DEFINITION`, `EXPLICIT_TYPE_SYMBOL_DEFINITION`, etc.
- Error message says error type: `SERVICE_URI_WITH_VARS_NOT_SUPPORTED`, etc.

**Decision Tree:**

```
SEMANTIC ERROR DETECTED
        ↓
Is this the EXPECTED error?
        ↓
  ┌─────┴─────┐
  ▼           ▼
 YES         NO
  ↓           ↓
CORRECT    WRONG PHASE?
ERROR      WRONG ERROR TYPE?
  ↓       WRONG PLACEMENT?
  ↓           ↓
  ↓      ADJUST @Error
  ↓      DIRECTIVE
  ↓           ↓
  └─────┬─────┘
        ↓
   COPY TO CORPUS
```

**Verify Expected Error:**
- Check error type matches `@Error` directive
- Check phase matches expected phase
- Check line number is correct construct

**If WRONG → Adjust @Error Directive:**
```bash
vim compiler-main/src/test/resources/examples/parseButFailCompile/workingarea/workarea.ek9

# Common fixes:
# - Move @Error to correct line (must be BEFORE error-triggering construct)
# - Change phase: SYMBOL_DEFINITION vs EXPLICIT_TYPE_SYMBOL_DEFINITION
# - Change error type to actual error reported
```

**Re-run from Step 2 until CORRECT**

**If CORRECT → Copy to Service Validation Corpus:**
```bash
# Use descriptive filename from plan
TARGET="compiler-main/src/test/resources/fuzzCorpus/serviceValidation/[test_filename].ek9"

# Copy validated test
cp compiler-main/src/test/resources/examples/parseButFailCompile/workingarea/workarea.ek9 $TARGET

echo "✅ Test copied to corpus: $TARGET"
```

---

### **Step 4: Track Progress**

**Mark test complete in tracking list:**
```bash
# Quick check - how many tests completed?
ls compiler-main/src/test/resources/fuzzCorpus/serviceValidation/*.ek9 | wc -l

# Should count up: 1, 2, 3... → 40
```

**Update TodoWrite (optional but helpful):**
- Mark current test as completed
- Mark next test as in_progress

---

### **Step 5: Move to Next Test**

**Clear workarea.ek9 or leave as template:**
```bash
# Option A: Clear it
echo '#!ek9\n' > compiler-main/src/test/resources/examples/parseButFailCompile/workingarea/workarea.ek9

# Option B: Keep last test as reference (my preference)
# Just edit in place for next test
```

**Go to Step 1 with next test from plan**

---

## 📊 Test Implementation Order

**Follow order from SERVICE_FUZZ_TEST_DETAILED_PLAN.md:**

### **Priority 1: ZERO Coverage Tests (17 files)**

**Phase 1 (SYMBOL_DEFINITION):**
1. service_uri_multiple_variables.ek9
2. service_uri_nested_variables.ek9
3. service_http_access_in_constructor.ek9
4. service_http_access_in_regular_method.ek9
5. service_operator_invalid_bitwise.ek9
6. service_operator_invalid_logical.ek9
7. service_operator_invalid_comparison.ek9
8. service_caching_in_definition.ek9 ⚠️ (may require grammar check)
9. service_caching_in_operation.ek9 ⚠️ (may require grammar check)
10. service_access_name_mapping.ek9 ⚠️ (may require grammar check)
11. service_access_name_custom.ek9 ⚠️ (may require grammar check)
12. service_operator_with_get_verb.ek9
13. service_operator_with_post_verb.ek9
14. service_operator_with_delete_verb.ek9

**Phase 2 (EXPLICIT_TYPE_SYMBOL_DEFINITION):**
15. service_header_missing_name.ek9
16. service_header_empty_name.ek9
17. service_header_invalid_characters.ek9
18. service_header_reserved_name.ek9 ⚠️ (may not be enforced)
19. service_header_numeric_name.ek9
20. service_body_with_qualifier.ek9
21. service_content_with_qualifier.ek9
22. service_path_assumed_no_variables.ek9
23. service_path_assumed_wrong_name.ek9

### **Priority 2: Edge Case Tests (23 files)**

**Continue with remaining tests from detailed plan...**

---

## 🎯 Final Validation (After All 40 Tests)

### **Step 1: Create Test Suite**

**File:** `compiler-main/src/test/java/org/ek9lang/compiler/fuzz/ServiceValidationFuzzTest.java`

```java
package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzz tests for EK9 service validation.
 * Tests 23 SERVICE error types across 40 comprehensive scenarios.
 *
 * Coverage:
 * - Phase 1 (SYMBOL_DEFINITION): Service definition, URI, operators
 * - Phase 2 (EXPLICIT_TYPE_SYMBOL_DEFINITION): Parameters, headers, returns
 */
class ServiceValidationFuzzTest extends FuzzTestBase {
  public ServiceValidationFuzzTest() {
    super("serviceValidation", CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION, false);
  }

  @Test
  void testServiceValidationRobustness() {
    assertTrue(runTests() != 0);
  }
}
```

### **Step 2: Run Full Suite**

```bash
mvn test -Dtest=ServiceValidationFuzzTest -pl compiler-main
```

**Expected:** All 40 tests pass (since each was pre-validated in workarea)

### **Step 3: Document Completion**

**Update FUZZING_MASTER_STATUS.md:**
- Add Service Validation Fuzzing section
- Update metrics (256 → 296 tests, 40 → 41 suites)
- Update error coverage (115+ → 122+ error types)
- Mark Gap 2 as COMPLETE

**Create STATUS_REPORT:**
- Similar to STREAM_PROCESSING_FUZZING_STATUS.md
- Document all 40 tests, error types, coverage achievements

---

## ⚠️ Common Issues & Solutions

### **Issue: "Had errors before reaching phase"**

**Problem:** @Error directive expects Phase 2 but error occurs in Phase 1

**Solution:**
- Check actual error phase in output
- Adjust `@Error: PHASE: ERROR_TYPE` to match actual phase
- Service errors can be SYMBOL_DEFINITION or EXPLICIT_TYPE_SYMBOL_DEFINITION

### **Issue: "Expecting @Error directive"**

**Problem:** Compiler found error but no @Error directive on that line

**Solution:**
- Move @Error directive to line BEFORE the error-triggering code
- Ensure @Error is on the construct (class, method, parameter) not in comment

### **Issue: Parse error on valid EK9 syntax**

**Problem:** I might misunderstand EK9 service syntax

**Solution:**
- Check existing service examples: AddressService.ek9
- Look at badServiceMethodArgumentType.ek9 patterns
- Ask Steve if unsure about syntax

### **Issue: Error type doesn't exist**

**Problem:** @Error directive references non-existent error type

**Solution:**
- Double-check error type name from exploration results
- Verify error is actually implemented (7 errors may have no implementation)
- May need to skip tests for unimplemented errors

---

## 📝 Progress Tracking Template

**Copy this for each session:**

```
SERVICE FUZZ TEST PROGRESS
--------------------------
Date: 2025-11-12
Session: 1

Completed: 0/40
├─ Priority 1 (ZERO Coverage): 0/23
└─ Priority 2 (Edge Cases): 0/17

Current Test: service_uri_multiple_variables.ek9
Status: [in_progress|completed|blocked]

Bonus Parse Errors Found: 0
├─ [List any new parse errors added to blockLevelSyntax]

Notes:
- [Any learnings, patterns discovered, issues encountered]
```

---

## ✅ Success Criteria

**Per-Test:**
- ✅ Passes WorkingAreaTest with correct error
- ✅ @Error directive validated
- ✅ Copied to serviceValidation corpus
- ✅ No duplicates created

**Overall:**
- ✅ All 40 tests in serviceValidation corpus
- ✅ ServiceValidationFuzzTest passes (40/40)
- ✅ Zero test failures
- ✅ Documentation updated
- ✅ Bonus: N parse errors added to blockLevelSyntax

---

**This workflow ensures consistent, high-quality test implementation across all 40 service validation scenarios.**
