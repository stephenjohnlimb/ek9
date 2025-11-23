# EK9 Try/Catch/Finally IR Structure Design - Comprehensive Analysis
**Date**: 2025-10-28 (Design) | **Updated**: 2025-11-16 (Implementation Status)
**Author**: Claude Code Analysis
**Status**: Design Document for IR Generation (Phase 10) | **Implementation: Steps 1-6 COMPLETE ✅**

---

## Table of Contents
1. [Executive Summary](#executive-summary)
2. [EK9 Grammar Analysis](#ek9-grammar-analysis)
3. [Unique EK9 Semantics](#unique-ek9-semantics)
4. [Control Flow Model](#control-flow-model)
5. [IR Structure Design](#ir-structure-design)
6. [Implementation Strategy](#implementation-strategy)
7. [Bytecode Generation Strategy](#bytecode-generation-strategy)

---

## **IMPLEMENTATION STATUS** (Updated 2025-11-16)

### **Phase Overview**
- **Current Phase:** Steps 1-6 COMPLETE ✅ | Steps 7-8 OPTIONAL (TBD for v1.0)
- **Completion Date:** 2025-11-16
- **Overall Progress:** Basic + resource management complete (Steps 1-6) ✅ | Advanced features optional (Steps 7-8) ⏭️

### **Completed Features** ✅

#### **Basic Exception Handling (Steps 1-4)** ✅ PRODUCTION READY
- ✅ **Step 1:** Simple Try (no catch, no finally, no resources, no return)
- ✅ **Step 2:** Try-Catch (no finally, no resources, no return)
- ✅ **Step 3:** Try-Finally (no catch, no resources, no return)
- ✅ **Step 4:** Try-Catch-Finally (no resources, no return)

#### **Resource Management (Steps 5-6)** ✅ PRODUCTION READY
- ✅ **Step 5:** Try with Single Resource - **COMPLETE**
- ✅ **Step 6:** Try with Multiple Resources - **COMPLETE**

**IR Tests Passing (7 tests):**
- `irGeneration/exceptionHandling/simpleTryCatch.ek9` ✅
- `irGeneration/exceptionHandling/simpleTryFinally.ek9` ✅
- `irGeneration/exceptionHandling/tryCatchFinally.ek9` ✅
- `irGeneration/exceptionHandling/tryHandleFinally.ek9` ✅ (keyword synonym test)
- `irGeneration/exceptionHandling/throwInTryCatch.ek9` ✅
- `irGeneration/exceptionHandling/tryWithSingleResource.ek9` ✅
- `irGeneration/exceptionHandling/tryWithMultipleResources.ek9` ✅

**Bytecode Tests Passing (11 tests):**
- `bytecodeGeneration/simpleTryCatch/` ✅ **Executable - runtime validated**
- `bytecodeGeneration/simpleTryFinally/` ✅ **Executable - runtime validated**
- `bytecodeGeneration/tryCatchFinally/` ✅ **Executable - runtime validated**
- `bytecodeGeneration/throwCatchExceptionSubtypes/` ✅ **Executable - polymorphic exception catching**
- `bytecodeGeneration/tryWithSingleResource/` ✅ **Executable - resource cleanup validated**
- `bytecodeGeneration/tryWithMultipleResources/` ✅ **Executable - LIFO cleanup validated**
- `bytecodeGeneration/tryWithMultipleResourcesAndFinally/` ✅ **Executable**
- `bytecodeGeneration/tryWithResourceFinally/` ✅ **Executable**
- `bytecodeGeneration/tryWithResourceNoCatch/` ✅ **Executable**
- `bytecodeGeneration/tryWithResourceExceptionPaths/` ✅ **Executable**
- `bytecodeGeneration/tryComprehensiveExceptionPaths/` ✅ **Executable - all 6 cases passing**

**Bug Fixes:**
- ✅ **Exception handler registration order corrected** - Finally blocks now execute during exception propagation

**Architecture Complete:**
- ✅ `TryCatchStatementGenerator.java` - IR generation for Steps 1-6
- ✅ `TryCatchAsmGenerator.java` - JVM bytecode generation for Steps 1-6
- ✅ `ThrowInstrAsmGenerator.java` - THROW → ATHROW bytecode generation
- ✅ Exception table generation working (try/catch/finally handlers)
- ✅ Finally block duplication (normal path + exception path)
- ✅ Resource cleanup generation (LIFO order)

### **Optional Advanced Features** ⏭️

#### **Advanced Features (Steps 7-8)** - OPTIONAL FOR v1.0
**Decision Needed:** Are these required for v1.0 release?

| Step | Feature | Status | Test File | Notes |
|------|---------|--------|-----------|-------|
| Step 7 | Try Expression with Return Variable | ⏭️ Optional | Not created | May be syntactic sugar |
| Step 8 | Try with Pre-flow Declaration | ⏭️ Optional | Parse tests exist | Grammar supports, needs IR/bytecode |

---

### **CRITICAL: Resource Management Design** (Updated 2025-11-11)

#### **Resource Variable Scope Registration Rules**

**MANDATORY PRINCIPLE:** Resource variables MUST be registered to the OUTERMOST resource scope (`_scope_2`), NOT to any inner scopes.

**Why This is Critical:**

1. **Resource Lifetime**: Resource must remain alive through the entire try-catch-finally structure, including the finally block where `close()` is called
2. **ARC Semantics**: SCOPE_REGISTER determines when RELEASE happens. If registered to an inner scope, resource would be freed BEFORE finally block executes
3. **Backend Independence**: Both JVM and LLVM backends rely on scope registration for memory management
4. **Semantic Correctness**: Calling `close()` on a freed resource is undefined behavior

#### **Correct IR Structure for Try-With-Resources**

```
SCOPE_ENTER _scope_1  // Outer wrapper scope
SCOPE_ENTER _scope_2  // Resource scope (OUTERMOST for resource variables)

  // Resource initialization
  REFERENCE resource, SimpleResource
  _temp1 = CALL SimpleResource.<init>(...)
  STORE resource, _temp1

  // ✅ CRITICAL: Register ACTUAL variable to OUTERMOST scope
  SCOPE_REGISTER resource, _scope_2  // NOT temp, NOT inner scope!
  RETAIN resource                     // ARC manage actual variable

  SCOPE_ENTER _scope_3  // Control flow scope
    CONTROL_FLOW_CHAIN [
      try_block: SCOPE_ENTER _scope_4 ... SCOPE_EXIT _scope_4
      catch_block: SCOPE_ENTER _scope_5 ... SCOPE_EXIT _scope_5
      finally_block_evaluation: [
        SCOPE_ENTER _scope_6
          CALL resource.close()  // ✅ Resource still alive!
        SCOPE_EXIT _scope_6
      ]
    ]
  SCOPE_EXIT _scope_3

SCOPE_EXIT _scope_2  // ✅ Resource RELEASED here, AFTER finally
SCOPE_EXIT _scope_1
```

#### **What NOT To Do** ❌

**WRONG Pattern 1:** Registering temp variables instead of actual variables
```
REFERENCE resource, SimpleResource
_temp1 = CALL SimpleResource.<init>(...)
SCOPE_REGISTER _temp1, _scope_2  // ❌ WRONG: temp variable!
STORE resource, _temp1             // ❌ Actual variable not registered!
```
**Problem:** Actual variable `resource` has no scope association, breaks LocalVariableTable and LLVM backend.

**WRONG Pattern 2:** Registering to inner scope
```
SCOPE_ENTER _scope_3  // Inner scope
  REFERENCE resource, SimpleResource
  SCOPE_REGISTER resource, _scope_3  // ❌ WRONG: inner scope!
SCOPE_EXIT _scope_3  // ❌ Resource RELEASED here!
CALL resource.close()  // ❌ Use-after-free!
```
**Problem:** Resource freed before finally block, `close()` operates on freed memory.

**WRONG Pattern 3:** Creating unnecessary inner scopes
```
SCOPE_ENTER _scope_2  // Resource scope
SCOPE_ENTER _scope_3  // ❌ WRONG: extra scope!
  // Initialize resource
SCOPE_EXIT _scope_3
SCOPE_ENTER _scope_4  // Control flow scope
```
**Problem:** Adds complexity, no semantic benefit, increases chance of registering to wrong scope.

#### **Implementation Checklist**

- [ ] Remove extra scope creation for resource initialization
- [ ] Register actual variable name (e.g., `resource`), NOT temp variables (e.g., `_temp2`)
- [ ] Register to outermost resource scope (`_scope_2`), NOT control flow scope (`_scope_3`)
- [ ] Apply RETAIN to actual variable after SCOPE_REGISTER
- [ ] Update IR test directives to match correct pattern
- [ ] Verify LocalVariableTable shows correct scope for resource variables
- [ ] Validate LLVM backend can use same IR without modifications

---

**Implementation Timeline** (from design document):
- **Week 1-2:** Resources and return variables (IR generation)
- **Week 3:** Pre-flow and integration (IR generation)
- **Week 4:** Bytecode generation for all advanced features

### **Pending Implementation** ⏭️

#### **Advanced Features - Bytecode Generation**
After IR generation complete (Steps 5-8), implement bytecode generation:
- ⏭️ Resource cleanup bytecode (LIFO order, after finally block)
- ⏭️ Expression form return value handling
- ⏭️ Pre-flow guard semantics bytecode
- ⏭️ Integration tests combining all advanced features

#### **Comprehensive Integration (Steps 9-10)**
- ⏭️ **Step 9:** Try with Pre-flow Guarded Assignment (combines Steps 5, 6, 8)
- ⏭️ **Step 10:** Comprehensive Integration Test (all features combined)

### **Key Technical Achievements** ✅

1. **Exception Table Generation** ✅
   - Correct JVM exception handler generation
   - Proper try/catch/finally scoping
   - Exception subtype matching (polymorphic catching)

2. **Finally Block Handling** ✅
   - Duplication for normal and exception paths
   - Only 2 copies needed (vs Java's N copies per return)
   - Benefit of EK9's no-early-return design

3. **THROW Statement** ✅
   - IR generation complete
   - ATHROW bytecode generation working
   - Integration with exception handling complete

4. **Class Bytecode Generation** ✅
   - General class support (not just programs)
   - Field access (getfield/putfield) working
   - Enables custom exception classes

### **Architecture Status**

**IR Instruction Structure:**
- ✅ Basic `EXCEPTION_HANDLER_BLOCK` structure working
- 🔨 Extending for resources section (Step 5-6)
- ⏭️ Extension for return variable section (Step 7)
- ⏭️ Extension for pre-flow section (Step 8)

**Code Generation:**
- ✅ `TryCatchStatementGenerator` handles basic forms
- 🔨 Extending for resource declarations
- ⏭️ Extending for expression form
- ⏭️ Extending for pre-flow

### **Test Coverage Status**

**IR Tests:** 5 passing (basic forms) | 4 to create (advanced features)
**Bytecode Tests:** 4 passing (basic forms + throw) | 4+ to create (advanced features)
**E2E Tests:** Runtime execution validated for basic forms ✅

### **Next Steps** (Immediate)

1. **Create Step 5 test file:** `tryWithSingleResource.ek9` with @IR directive
2. **Extend IR structure:** Add resources section to EXCEPTION_HANDLER_BLOCK
3. **Implement IR generation:** Modify TryCatchStatementGenerator for resource declarations
4. **Verify IR output:** Validate against expected structure from design (lines 960-1061)
5. **Move to Step 6:** Multiple resources after Step 5 validation

### **Reference Sections in This Document**

- **Steps 5-8 Implementation Plans:** Lines 750-1440 (detailed IR structure for each step)
- **Bytecode Generation Strategy:** Lines 1440-1740 (resource cleanup, LIFO order, exception paths)
- **IR Structure Design:** Lines 500-710 (EXCEPTION_HANDLER_BLOCK specification)

---

## Executive Summary

### Critical Design Principle: No Early Returns

**EK9 has NO early return statements.** All blocks (try, catch, finally) execute sequentially to completion or until an exception is thrown. This dramatically simplifies exception handling compared to Java/C++.

**Key Implications**:
- Control flow is linear and predictable
- Resource cleanup happens at exactly ONE location
- Only 4 control flow paths (not 10+)
- Finally block needs only 2 copies in bytecode (normal + exception)
- Return variable is just a regular mutable variable

**Complexity Comparison**:
| Language | Early Returns | Finally Copies | Resource Cleanup Locations |
|----------|---------------|----------------|----------------------------|
| Java     | Yes           | N (per return) | N exit points             |
| C++      | Yes           | 0 (RAII)      | Destructors               |
| EK9      | **No**        | **2 (max)**   | **1 location**            |

---

## EK9 Grammar Analysis

### Try Statement Expression Grammar
```antlr
tryStatementExpression
    : (TRY|FUNCTION) preFlowStatement? NL+ INDENT NL*
      declareArgumentParam?           // -> resource <- ...
      returningParam?                  // <- rtn as Type: initialValue
      instructionBlock?                // try body
      DEDENT
      catchStatementExpression?
      finallyStatementExpression?
    ;
```

### Catch Statement Expression Grammar
```antlr
catchStatementExpression
    : NL+ directive? (CATCH|HANDLE) NL+ INDENT NL*
      argumentParam                    // -> ex as Exception
      instructionBlock
      DEDENT
    ;
```

### Finally Statement Expression Grammar
```antlr
finallyStatementExpression
    : NL+ directive? FINALLY block
    ;
```

### Grammar Component Analysis

#### 1. Keywords: TRY/FUNCTION, CATCH/HANDLE
```ek9
try          // Standard keyword
  ...

function     // Synonym for try (emphasizes inline function pattern)
  ...

catch        // Standard keyword
  ...

handle       // Synonym for catch (emphasizes exception handling)
  ...
```

**Both pairs are fully interchangeable** in the grammar.

#### 2. Pre-Flow Statement (Optional)
```ek9
try someVar <- getValue()    // BEFORE INDENT - declaration with optional guard
  ...
```

**Location**: Before INDENT (not inside indented section)
**Forms**:
- `try someVar <- getValue()` - Declaration (guard if type has `?` operator)
- `try someVar := getValue()` - Assignment (blind)
- `try someVar ?= getValue()` - Guarded assignment (only if unset)

**Scope**: `someVar` accessible in try, catch, and finally blocks

#### 3. Declare Argument Param (Optional) - Resources
```ek9
try
  -> resource <- SomeResource().open()    // AFTER INDENT, BEFORE body
  ...
```

**Location**: After INDENT, in header section
**Syntax**: Uses `->` prefix (like function parameters, but with initialization)

**Single Resource**:
```ek9
try
  -> resource <- SomeResource("name").open()
  <body instructions>
```

**Multiple Resources**:
```ek9
try
  ->
    resource1 <- SomeResource("res1").open()
    resource2 <- SomeResource("res2").open()
  <body instructions>
```

**Semantics**:
- Resources must have `operator close` defined
- Automatically closed when scope exits (normal or exception)
- Closed in REVERSE order (LIFO)
- Accessible in try, catch, and finally blocks

#### 4. Returning Param (Optional) - Return Variable
```ek9
function
  <- rtn as String: "initial value"    // AFTER INDENT, AFTER resources
  <body instructions>
```

**Location**: After INDENT, after resources, before body
**Syntax**: `<- rtn as Type: initialValue`

**Forms**:

**Simple Initialization**:
```ek9
function
  <- rtn as String: "default"
  rtn: "new value"  // Can reassign in body
```

**Complex Initialization**:
```ek9
function
  -> resource <- SomeResource().open()
  <- rtn as String: resource.getValue()  // Can use resources in initialization
  rtn: "override"  // Can still reassign
```

**Semantics**:
- `rtn` is a **mutable variable** in try/catch/finally scope
- Initialized in the header (not at the end!)
- Can be reassigned with `:` in try, catch, finally
- Can be guard-assigned with `:=?` in catch, finally
- Final value is returned to outer scope when expression completes

#### 5. Instruction Block (Optional) - Try Body
```ek9
try
  value <- getValue()
  assert value?
  someOperation()
```

**Standard instruction block** - executes sequentially to completion or exception

#### 6. Catch Block (Optional)
```ek9
catch
  -> ex as Exception    // Single parameter only
  handleError(ex)
  rtn: "error"         // Can assign to rtn if present
```

**Key Constraints** (validated in Phase 3):
- **Single parameter only** - not multiple like Java
- Parameter must extend `org.ek9.lang::Exception`
- Can access pre-flow variables, resources, and rtn

#### 7. Finally Block (Optional)
```ek9
finally
  cleanup()
  rtn :=? "fallback"  // Can guard-assign to rtn
```

**Semantics**:
- Executes after try (if no exception) or after catch (if exception caught)
- Executes even if exception propagates
- Can access pre-flow variables, resources, and rtn
- Can reassign or guard-assign rtn

---

## Unique EK9 Semantics

### 1. Try/Function as Expression (Return Value)

**EK9**:
```ek9
result <- function
  <- rtn as String: String()  // Declare rtn in header

  if condition
    rtn: "value1"  // Assignment

  // THIS CODE STILL EXECUTES (no early return!)
  someOtherOperation()
  rtn: "value2"  // Another assignment

  // Block runs to completion, rtn = "value2"

handle
  -> ex as Exception
  rtn: "error"  // Can assign in catch too

finally
  rtn :=? "default"  // Can guard-assign in finally

// NOW result = final value of rtn
```

**Key Points**:
- `rtn` declared in header: `<- rtn as Type: initialValue`
- `rtn` is a regular mutable variable
- No early return - all statements execute sequentially
- Can reassign `rtn` anywhere in try/catch/finally
- Final value of `rtn` is returned when expression completes

**Java Comparison** (for contrast):
```java
String result = try {
    String rtn = "initial";
    if (condition) {
        return "value1";  // EARLY EXIT - skips rest!
    }
    someOtherOperation();  // NOT executed if condition true
    return "value2";
} catch (Exception ex) {
    return "error";  // Another early exit
} finally {
    // Cannot modify return value in Java!
}
```

### 2. Automatic Resource Management

**EK9**:
```ek9
result <- function
  ->
    resource1 <- SomeResource("res1").open()
    resource2 <- SomeResource("res2").open()
  <- rtn as String: resource1.getValue()

  rtn: rtn + resource2.getValue()

handle
  -> ex as Exception
  // Resources STILL accessible here!
  assert resource1?
  rtn: "error"

finally
  // Resources STILL accessible here!
  assert resource1? and resource2?

// Resources closed HERE in reverse order:
// 1. resource2.close()
// 2. resource1.close()
```

**Semantics**:
- Resources declared with `-> resource <- init` in header
- Resources accessible in try, catch, AND finally blocks
- Automatically closed when scope exits (normal or exception)
- Closed in REVERSE order (LIFO)
- Close happens AFTER finally block completes
- Must have `operator close` defined (validated in Phase 3)

**Java Comparison** (try-with-resources):
```java
String result;
try (Resource1 resource1 = new Resource1();
     Resource2 resource2 = new Resource2()) {
    result = resource1.getValue();
    result += resource2.getValue();
} catch (Exception ex) {
    // Resources NOT accessible here (already closed!)
    result = "error";
} finally {
    // Resources NOT accessible here (already closed!)
}
// Resources closed BEFORE catch/finally in Java
```

**EK9's design is superior**: Resources available throughout exception handling.

### 3. Pre-Flow Variable with Guard Semantics

**Declaration Form**:
```ek9
try someVar <- getValue()
  -> resource <- SomeResource(someVar).open()  // Can use in resources
  <- rtn as String: someVar                     // Can use in return init
  assert someVar?                               // Can use in body
catch
  -> ex as Exception
  handleError(someVar, ex)  // Accessible in catch
finally
  cleanup(someVar)          // Accessible in finally
```

**Guarded Assignment Form**:
```ek9
existingVar <- String()
try existingVar ?= getValue()
  // Only executes if getValue() returned a SET value
  useValue(existingVar)
```

**Semantics**:
- Pre-flow is declared BEFORE INDENT
- Three forms: declaration (`<-`), assignment (`:=`), guarded assignment (`:=?`)
- If declaration and type has `?` operator, acts as guard
- Variable accessible in resources, rtn initialization, try, catch, finally

### 4. Single Catch Block (Dispatcher Pattern)

**EK9 Philosophy**:
```ek9
catch
  -> ex as Exception  // Single parameter
  handleException(ex)  // Dispatcher method handles type-specific logic
```

**Why Single Catch**:
- EK9 uses **dispatcher pattern** for type-specific exception handling
- Dispatcher methods provide clean type-specific logic
- Avoids Java's multiple-catch complexity
- Simpler IR structure
- More functional/composable design

**Example Dispatcher Pattern**:
```ek9
private handleException() as dispatcher
  -> ex as Exception
  <- rtn as String: $ex

private handleException()
  -> ex as AnException
  <- rtn as String: $ex
  tidyUpForAnException()

private handleException()
  -> ex as OtherException
  <- rtn as String: $ex
  this.retryAfter: ex.retryAfter()
```

**Phase 3 Validation**:
- Must be single parameter (error: `SINGLE_EXCEPTION_ONLY`)
- Parameter must extend `Exception` (error: `TYPE_MUST_EXTEND_EXCEPTION`)

### 5. Finally Block Guarantees

**EK9 Guarantees**:
1. Finally executes after try (if no exception)
2. Finally executes after catch (if exception caught)
3. Finally executes before exception propagation (if not caught or thrown in catch)
4. Finally can access pre-flow variables, resources, and rtn
5. Finally can modify rtn with assignment or guarded assignment

**Example**:
```ek9
result <- function
  <- rtn as String: "default"
  throw Exception("Error!")
catch
  -> ex as Exception
  rtn: "caught"
finally
  // rtn currently = "caught"
  rtn :=? "fallback"  // No effect (rtn is already set)
  // rtn still = "caught"
// result = "caught"
```

---

## Control Flow Model

### Only 4 Control Flow Paths

**Path 1: Normal Execution (No Exception)**
```
1. Evaluate pre-flow (if present)
2. Initialize resources (if present)
3. Initialize rtn (if present)
4. Execute try body to completion
5. Execute finally block (if present) to completion
6. Close resources in reverse order
7. Return rtn value (if expression form) or exit normally
```

**Path 2: Exception in Try, Caught**
```
1. Evaluate pre-flow (if present)
2. Initialize resources (if present)
3. Initialize rtn (if present)
4. Execute try body until exception thrown
5. Match exception type to catch parameter
6. Execute catch block to completion
7. Execute finally block (if present) to completion
8. Close resources in reverse order
9. Return rtn value (if expression form) or exit normally
```

**Path 3: Exception in Catch**
```
1. Evaluate pre-flow (if present)
2. Initialize resources (if present)
3. Initialize rtn (if present)
4. Execute try body until exception thrown
5. Execute catch block until exception thrown
6. Execute finally block (if present) to completion
7. Close resources in reverse order
8. Propagate exception from catch
```

**Path 4: Exception Not Caught (No Catch or Wrong Type)**
```
1. Evaluate pre-flow (if present)
2. Initialize resources (if present)
3. Initialize rtn (if present)
4. Execute try body until exception thrown
5. (No catch or type doesn't match)
6. Execute finally block (if present) to completion
7. Close resources in reverse order
8. Propagate exception from try
```

### Sequential Execution Guarantee

**Critical Insight**: All blocks execute sequentially to completion (or exception).

**No early exits means**:
- No need to track multiple return points
- No need to save/restore return values around finally
- No need to duplicate finally code at each return
- Resource cleanup happens at exactly one location

**Example Demonstrating Sequential Execution**:
```ek9
result <- function
  <- rtn as String: "A"
  stdout.println("Try 1")
  rtn: "B"
  stdout.println("Try 2")
  rtn: "C"
  stdout.println("Try 3")
  // Block runs to completion, rtn = "C"
catch
  -> ex as Exception
  stdout.println("Catch 1")
  rtn: "D"
  stdout.println("Catch 2")
  // Not executed in this example
finally
  stdout.println("Finally 1")
  rtn: "E"
  stdout.println("Finally 2")
  // Block runs to completion, rtn = "E"

// Output:
// Try 1
// Try 2
// Try 3
// Finally 1
// Finally 2
// result = "E"
```

---

## IR Structure Design

### EXCEPTION_HANDLER_BLOCK Instruction

Based on our established `CONTROL_FLOW_CHAIN` pattern, but adapted for exception semantics:

```
EXCEPTION_HANDLER_BLOCK
[
  handler_type: "TRY_CATCH_FINALLY" | "TRY_CATCH" | "TRY_FINALLY" | "TRY"

  // ========== PRE-FLOW SECTION ==========
  // Optional: pre-flow variable (declared before try)
  preflow_variable:
  [
    preflow_type: "DECLARATION" | "ASSIGNMENT" | "GUARDED_ASSIGNMENT"
    preflow_variable_name: "someVar"
    preflow_variable_type: "org.ek9.lang::SomeType"

    preflow_evaluation:
    [
      SCOPE_ENTER _scope_preflow
      _temp_preflow = <call getValue() or other expression>
      RETAIN _temp_preflow
      SCOPE_REGISTER _temp_preflow, _scope_preflow
      STORE someVar, _temp_preflow
      RETAIN someVar
      SCOPE_REGISTER someVar, _scope_handler  // Register in handler scope
      SCOPE_EXIT _scope_preflow
    ]

    // Only for DECLARATION with ? operator (guard semantics)
    has_guard: true | false
    guard_check:
    [
      SCOPE_ENTER _scope_guard_check
      _temp_guard = CALL (SomeType)someVar._isSet() [pure=true, ...]
      RETAIN _temp_guard
      SCOPE_REGISTER _temp_guard, _scope_guard_check
      _temp_guard_bool = CALL (org.ek9.lang::Boolean)_temp_guard._true() [pure=true, ...]
      SCOPE_EXIT _scope_guard_check
    ]
    guard_result: _temp_guard
    guard_primitive: _temp_guard_bool
  ]

  // ========== RESOURCE SECTION ==========
  // Optional: resources declared with -> in header
  resources:
  [
    [
      resource_variable: "resource1"
      resource_type: "com.example::SomeResource"
      resource_has_close: true  // Validated in Phase 3

      resource_initialization:
      [
        SCOPE_ENTER _scope_res1_init
        REFERENCE resource1, com.example::SomeResource
        _temp_res1 = CALL (com.example::SomeResource)SomeResource.<init>("name") [...]
        RETAIN _temp_res1
        SCOPE_REGISTER _temp_res1, _scope_res1_init
        _temp_res1_opened = CALL (com.example::SomeResource)_temp_res1.open() [...]
        RETAIN _temp_res1_opened
        SCOPE_REGISTER _temp_res1_opened, _scope_res1_init
        STORE resource1, _temp_res1_opened
        RETAIN resource1
        SCOPE_REGISTER resource1, _scope_resources  // Register in resources scope
        SCOPE_EXIT _scope_res1_init
      ]
    ],
    [
      resource_variable: "resource2"
      resource_type: "com.example::AnotherResource"
      resource_has_close: true

      resource_initialization:
      [
        SCOPE_ENTER _scope_res2_init
        REFERENCE resource2, com.example::AnotherResource
        _temp_res2 = CALL (com.example::AnotherResource)AnotherResource.<init>("name") [...]
        RETAIN _temp_res2
        SCOPE_REGISTER _temp_res2, _scope_res2_init
        _temp_res2_opened = CALL (com.example::AnotherResource)_temp_res2.open() [...]
        RETAIN _temp_res2_opened
        SCOPE_REGISTER _temp_res2_opened, _scope_res2_init
        STORE resource2, _temp_res2_opened
        RETAIN resource2
        SCOPE_REGISTER resource2, _scope_resources
        SCOPE_EXIT _scope_res2_init
      ]
    ]
  ]

  // ========== RETURN VARIABLE SECTION ==========
  // Optional: return variable (expression form only)
  return_variable:
  [
    return_variable_name: "rtn"
    return_variable_type: "org.ek9.lang::String"

    return_initialization:
    [
      SCOPE_ENTER _scope_rtn_init
      REFERENCE rtn, org.ek9.lang::String
      _temp_rtn = <initialization expression - can use resources, preflow>
      RETAIN _temp_rtn
      SCOPE_REGISTER _temp_rtn, _scope_rtn_init
      STORE rtn, _temp_rtn
      RETAIN rtn
      SCOPE_REGISTER rtn, _scope_handler  // Register in handler scope
      SCOPE_EXIT _scope_rtn_init
    ]
  ]

  // ========== TRY BLOCK ==========
  try_block:
  [
    try_scope_id: _scope_try_body

    try_body_evaluation:
    [
      SCOPE_ENTER _scope_try_body
      <try body instructions>
      // Can access: preflow variables, resources, rtn
      // Can assign to rtn: STORE rtn, new_value
      SCOPE_EXIT _scope_try_body
    ]
  ]

  // ========== CATCH BLOCK (Optional) ==========
  catch_block:
  [
    catch_scope_id: _scope_catch
    exception_parameter: "ex"
    exception_type: "org.ek9.lang::Exception"

    catch_evaluation:
    [
      SCOPE_ENTER _scope_catch
      REFERENCE ex, org.ek9.lang::Exception
      // Exception object populated by runtime/backend
      RETAIN ex
      SCOPE_REGISTER ex, _scope_catch

      <catch body instructions>
      // Can access: preflow variables, resources, rtn, ex
      // Can assign to rtn

      SCOPE_EXIT _scope_catch
    ]
  ]

  // ========== FINALLY BLOCK (Optional) ==========
  finally_block:
  [
    finally_scope_id: _scope_finally

    finally_evaluation:
    [
      SCOPE_ENTER _scope_finally
      <finally body instructions>
      // Can access: preflow variables, resources, rtn
      // Can assign to rtn
      // Can guard-assign to rtn
      SCOPE_EXIT _scope_finally
    ]
  ]

  // ========== RESOURCE CLEANUP ==========
  // ALWAYS executes (normal or exception path)
  // AFTER finally block completes
  // REVERSE order (LIFO)
  resource_cleanup:
  [
    // resource2 closed first (declared last)
    CALL (com.example::AnotherResource)resource2.close() [pure=true, ...]
    RELEASE resource2

    // resource1 closed second (declared first)
    CALL (com.example::SomeResource)resource1.close() [pure=true, ...]
    RELEASE resource1
  ]

  // ========== SCOPE HIERARCHY ==========
  scope_metadata:
  [
    outer_scope: _scope_outer              // Enclosing scope
    handler_scope: _scope_handler          // Overall try/catch/finally scope
    resources_scope: _scope_resources      // Scope for resources (accessible in all blocks)
    try_body_scope: _scope_try_body        // Try block body
    catch_scope: _scope_catch              // Catch block (optional)
    finally_scope: _scope_finally          // Finally block (optional)
  ]

  // ========== EXCEPTION METADATA ==========
  // Used by backend for exception table generation
  exception_metadata:
  [
    has_catch: true | false
    catch_exception_type: "org.ek9.lang::Exception"  // If has_catch
    has_finally: true | false
    has_resources: true | false
    resource_count: 2  // Number of resources to clean up
  ]
]
```

### Key IR Design Decisions

#### 1. Sequential Block Structure

**Each block (try, catch, finally) is a sequential instruction list**:
- No need to model exception branching in IR
- Backend handles exception control flow via exception tables
- IR represents structure, not control mechanism

#### 2. Explicit Resource Cleanup Sequence

**Resource cleanup is explicit in IR**:
- Appears as a separate section
- LIFO order (reverse of declaration)
- Backend can optimize (inline, eliminate redundant calls)
- Makes IR self-documenting

#### 3. Pre-flow, Resources, and rtn in Shared Scope

**All three accessible in try, catch, finally**:
- Pre-flow variables registered in `_scope_handler`
- Resources registered in `_scope_resources` (child of `_scope_handler`)
- rtn registered in `_scope_handler`
- All three accessible by reference in nested scopes

#### 4. Exception Parameter is Local to Catch

**ex parameter only exists in catch scope**:
- Not accessible in finally (unlike resources/rtn)
- Follows standard parameter scoping rules

#### 5. Return Variable is Mutable

**rtn is a regular variable**:
- Can be reassigned with `STORE rtn, new_value` in any block
- No special "return value" handling needed
- Final value is whatever remains after all blocks complete

---

## Implementation Strategy

### Phase 7 (IR_GENERATION) Implementation Plan

#### Step 1: Simple Try (No Catch, No Finally, No Resources, No Return)

**Test File**: `irGeneration/exceptions/simpleTry.ek9`
```ek9
defines module exceptions.test

  defines function

    @IR: IR_GENERATION: FUNCTION: "exceptions.test::simpleTry"
    simpleTry()
      value <- String()

      try
        value: "success"
        assert value?

      assert value?
```

**Expected IR**:
```
EXCEPTION_HANDLER_BLOCK
[
  handler_type: "TRY"

  try_block:
  [
    try_scope_id: _scope_try_body
    try_body_evaluation:
    [
      SCOPE_ENTER _scope_try_body
      _temp1 = LOAD_LITERAL "success", org.ek9.lang::String
      RETAIN _temp1
      SCOPE_REGISTER _temp1, _scope_try_body
      STORE value, _temp1
      // assert value? IR
      SCOPE_EXIT _scope_try_body
    ]
  ]

  scope_metadata: [...]
  exception_metadata: [has_catch: false, has_finally: false, has_resources: false]
]
```

#### Step 2: Try-Catch (No Finally, No Resources, No Return)

**Test File**: `irGeneration/exceptions/simpleTryCatch.ek9`
```ek9
defines module exceptions.test

  defines function

    throwsException()
      throw Exception("Test error")

    @IR: IR_GENERATION: FUNCTION: "exceptions.test::simpleTryCatch"
    simpleTryCatch()
      result <- String()

      try
        throwsException()
        result: "success"
      catch
        -> ex as Exception
        result: "caught"

      assert result?
```

**Expected IR**:
```
EXCEPTION_HANDLER_BLOCK
[
  handler_type: "TRY_CATCH"

  try_block:
  [
    try_body_evaluation:
    [
      SCOPE_ENTER _scope_try_body
      CALL throwsException() [...]
      _temp1 = LOAD_LITERAL "success", org.ek9.lang::String
      STORE result, _temp1
      SCOPE_EXIT _scope_try_body
    ]
  ]

  catch_block:
  [
    catch_scope_id: _scope_catch
    exception_parameter: "ex"
    exception_type: "org.ek9.lang::Exception"
    catch_evaluation:
    [
      SCOPE_ENTER _scope_catch
      REFERENCE ex, org.ek9.lang::Exception
      RETAIN ex
      SCOPE_REGISTER ex, _scope_catch
      _temp2 = LOAD_LITERAL "caught", org.ek9.lang::String
      STORE result, _temp2
      SCOPE_EXIT _scope_catch
    ]
  ]

  exception_metadata: [has_catch: true, catch_exception_type: "org.ek9.lang::Exception", ...]
]
```

#### Step 3: Try-Finally (No Catch, No Resources, No Return)

**Test File**: `irGeneration/exceptions/simpleTryFinally.ek9`
```ek9
defines module exceptions.test

  defines function

    @IR: IR_GENERATION: FUNCTION: "exceptions.test::simpleTryFinally"
    simpleTryFinally()
      stdout <- Stdout()

      try
        stdout.println("Try block")
      finally
        stdout.println("Finally block")
```

**Expected IR**:
```
EXCEPTION_HANDLER_BLOCK
[
  handler_type: "TRY_FINALLY"

  try_block: [...]

  finally_block:
  [
    finally_scope_id: _scope_finally
    finally_evaluation:
    [
      SCOPE_ENTER _scope_finally
      _temp1 = LOAD stdout
      _temp2 = LOAD_LITERAL "Finally block", org.ek9.lang::String
      CALL (org.ek9.lang::StringOutput)_temp1.println(_temp2) [...]
      SCOPE_EXIT _scope_finally
    ]
  ]

  exception_metadata: [has_catch: false, has_finally: true, ...]
]
```

#### Step 4: Try-Catch-Finally (No Resources, No Return)

**Test File**: `irGeneration/exceptions/simpleTryCatchFinally.ek9`
```ek9
defines module exceptions.test

  defines function

    throwsException()
      throw Exception("Test error")

    @IR: IR_GENERATION: FUNCTION: "exceptions.test::simpleTryCatchFinally"
    simpleTryCatchFinally()
      stdout <- Stdout()
      result <- String()

      try
        throwsException()
        result: "success"
      catch
        -> ex as Exception
        result: "caught"
      finally
        stdout.println("Finally: " + result)

      assert result == "caught"
```

**Expected IR**:
```
EXCEPTION_HANDLER_BLOCK
[
  handler_type: "TRY_CATCH_FINALLY"

  try_block: [...]
  catch_block: [...]
  finally_block:
  [
    finally_evaluation:
    [
      SCOPE_ENTER _scope_finally
      // Can access result variable here
      _temp1 = LOAD stdout
      _temp2 = LOAD result
      _temp3 = LOAD_LITERAL "Finally: ", org.ek9.lang::String
      _temp4 = CALL (org.ek9.lang::String)_temp3.+(result) [...]
      CALL (org.ek9.lang::StringOutput)_temp1.println(_temp4) [...]
      SCOPE_EXIT _scope_finally
    ]
  ]

  exception_metadata: [has_catch: true, has_finally: true, ...]
]
```

#### Step 5: Try with Single Resource

**Test File**: `irGeneration/exceptions/tryWithSingleResource.ek9`
```ek9
defines module exceptions.test

  defines class

    TestResource
      name <- String()
      opened <- Boolean()
      closed <- Boolean()

      default private TestResource()

      TestResource()
        -> name as String
        this.name: name

      open() as pure
        <- rtn as TestResource: this
        opened: true

      getValue()
        <- rtn as String: "value from " + name

      operator close as pure
        closed: true

      override operator ?
        <- rtn as Boolean: opened? and opened and not (closed? and closed)

  defines function

    @IR: IR_GENERATION: FUNCTION: "exceptions.test::tryWithSingleResource"
    tryWithSingleResource()
      result <- String()

      try
        -> resource <- TestResource("res1").open()
        result: resource.getValue()

      assert result == "value from res1"
```

**Expected IR**:
```
EXCEPTION_HANDLER_BLOCK
[
  handler_type: "TRY"

  resources:
  [
    [
      resource_variable: "resource"
      resource_type: "exceptions.test::TestResource"
      resource_has_close: true

      resource_initialization:
      [
        SCOPE_ENTER _scope_res_init
        REFERENCE resource, exceptions.test::TestResource
        _temp1 = LOAD_LITERAL "res1", org.ek9.lang::String
        _temp2 = CALL (exceptions.test::TestResource)TestResource.<init>(_temp1) [...]
        RETAIN _temp2
        SCOPE_REGISTER _temp2, _scope_res_init
        _temp3 = CALL (exceptions.test::TestResource)_temp2.open() [...]
        RETAIN _temp3
        SCOPE_REGISTER _temp3, _scope_res_init
        STORE resource, _temp3
        RETAIN resource
        SCOPE_REGISTER resource, _scope_resources
        SCOPE_EXIT _scope_res_init
      ]
    ]
  ]

  try_block:
  [
    try_body_evaluation:
    [
      SCOPE_ENTER _scope_try_body
      _temp4 = LOAD resource
      RETAIN _temp4
      SCOPE_REGISTER _temp4, _scope_try_body
      _temp5 = CALL (exceptions.test::TestResource)_temp4.getValue() [...]
      RETAIN _temp5
      SCOPE_REGISTER _temp5, _scope_try_body
      STORE result, _temp5
      SCOPE_EXIT _scope_try_body
    ]
  ]

  resource_cleanup:
  [
    CALL (exceptions.test::TestResource)resource.close() [pure=true, ...]
    RELEASE resource
  ]

  exception_metadata: [has_resources: true, resource_count: 1, ...]
]
```

#### Step 6: Try with Multiple Resources

**Test File**: `irGeneration/exceptions/tryWithMultipleResources.ek9`
```ek9
defines module exceptions.test

  defines function

    @IR: IR_GENERATION: FUNCTION: "exceptions.test::tryWithMultipleResources"
    tryWithMultipleResources()
      result <- String()

      try
        ->
          resource1 <- TestResource("res1").open()
          resource2 <- TestResource("res2").open()
        result: resource1.getValue() + " " + resource2.getValue()

      assert result == "value from res1 value from res2"
```

**Expected IR**:
```
EXCEPTION_HANDLER_BLOCK
[
  handler_type: "TRY"

  resources:
  [
    [resource1 initialization],
    [resource2 initialization]
  ]

  try_block: [uses both resources]

  resource_cleanup:
  [
    // LIFO order: resource2 closed first
    CALL (exceptions.test::TestResource)resource2.close() [...]
    RELEASE resource2

    // resource1 closed second
    CALL (exceptions.test::TestResource)resource1.close() [...]
    RELEASE resource1
  ]

  exception_metadata: [has_resources: true, resource_count: 2, ...]
]
```

#### Step 7: Try Expression with Return Variable

**Test File**: `irGeneration/exceptions/tryExpressionWithReturn.ek9`
```ek9
defines module exceptions.test

  defines function

    throwsException()
      throw Exception("Test error")

    @IR: IR_GENERATION: FUNCTION: "exceptions.test::tryExpressionWithReturn"
    tryExpressionWithReturn()

      result <- function
        <- rtn as String: "default"
        rtn: "success"
      catch
        -> ex as Exception
        rtn: "error"
      finally
        rtn :=? "fallback"

      assert result == "success"
```

**Expected IR**:
```
EXCEPTION_HANDLER_BLOCK
[
  handler_type: "TRY_CATCH_FINALLY"

  return_variable:
  [
    return_variable_name: "rtn"
    return_variable_type: "org.ek9.lang::String"

    return_initialization:
    [
      SCOPE_ENTER _scope_rtn_init
      REFERENCE rtn, org.ek9.lang::String
      _temp1 = LOAD_LITERAL "default", org.ek9.lang::String
      RETAIN _temp1
      SCOPE_REGISTER _temp1, _scope_rtn_init
      STORE rtn, _temp1
      RETAIN rtn
      SCOPE_REGISTER rtn, _scope_handler
      SCOPE_EXIT _scope_rtn_init
    ]
  ]

  try_block:
  [
    try_body_evaluation:
    [
      SCOPE_ENTER _scope_try_body
      _temp2 = LOAD_LITERAL "success", org.ek9.lang::String
      RETAIN _temp2
      SCOPE_REGISTER _temp2, _scope_try_body
      STORE rtn, _temp2  // Assignment to rtn
      SCOPE_EXIT _scope_try_body
    ]
  ]

  catch_block:
  [
    catch_evaluation:
    [
      SCOPE_ENTER _scope_catch
      REFERENCE ex, org.ek9.lang::Exception
      RETAIN ex
      SCOPE_REGISTER ex, _scope_catch
      _temp3 = LOAD_LITERAL "error", org.ek9.lang::String
      RETAIN _temp3
      SCOPE_REGISTER _temp3, _scope_catch
      STORE rtn, _temp3  // Assignment to rtn in catch
      SCOPE_EXIT _scope_catch
    ]
  ]

  finally_block:
  [
    finally_evaluation:
    [
      SCOPE_ENTER _scope_finally
      // Guarded assignment: rtn :=? "fallback"
      _temp4 = LOAD rtn
      _temp5 = CALL (org.ek9.lang::String)_temp4._isSet() [...]
      _temp6 = CALL (org.ek9.lang::Boolean)_temp5._false() [...]  // Check if UNSET
      // If unset, assign "fallback"
      // (This would be a GuardedAssignmentInstr or similar)
      SCOPE_EXIT _scope_finally
    ]
  ]

  // rtn's final value is returned to result
]
```

#### Step 8: Try with Pre-flow Declaration

**Test File**: `irGeneration/exceptions/tryWithPreflowDeclaration.ek9`
```ek9
defines module exceptions.test

  defines function

    getValue()
      <- rtn as String: "preflow value"

    @IR: IR_GENERATION: FUNCTION: "exceptions.test::tryWithPreflowDeclaration"
    tryWithPreflowDeclaration()

      try someVar <- getValue()
        -> resource <- TestResource(someVar).open()
        <- rtn as String: someVar + " " + resource.getValue()
      catch
        -> ex as Exception
        <- rtn: someVar + " error"
      finally
        stdout <- Stdout()
        stdout.println(someVar)

      assert rtn == "preflow value value from preflow value"
```

**Expected IR**:
```
EXCEPTION_HANDLER_BLOCK
[
  handler_type: "TRY_CATCH_FINALLY"

  preflow_variable:
  [
    preflow_type: "DECLARATION"
    preflow_variable_name: "someVar"
    preflow_variable_type: "org.ek9.lang::String"

    preflow_evaluation:
    [
      SCOPE_ENTER _scope_preflow
      _temp_preflow = CALL getValue() [...]
      RETAIN _temp_preflow
      SCOPE_REGISTER _temp_preflow, _scope_preflow
      STORE someVar, _temp_preflow
      RETAIN someVar
      SCOPE_REGISTER someVar, _scope_handler  // Accessible in all blocks
      SCOPE_EXIT _scope_preflow
    ]

    // String has ? operator, so guard check generated
    has_guard: true
    guard_check:
    [
      SCOPE_ENTER _scope_guard_check
      _temp_guard = CALL (org.ek9.lang::String)someVar._isSet() [...]
      RETAIN _temp_guard
      SCOPE_REGISTER _temp_guard, _scope_guard_check
      _temp_guard_bool = CALL (org.ek9.lang::Boolean)_temp_guard._true() [...]
      SCOPE_EXIT _scope_guard_check
    ]
    guard_result: _temp_guard
    guard_primitive: _temp_guard_bool
  ]

  resources:
  [
    [
      // resource initialization uses someVar
      resource_initialization:
      [
        _temp1 = LOAD someVar
        _temp2 = CALL TestResource.<init>(_temp1) [...]
        ...
      ]
    ]
  ]

  return_variable:
  [
    // return initialization uses someVar and resource
    return_initialization:
    [
      _temp3 = LOAD someVar
      _temp4 = LOAD resource
      _temp5 = CALL resource.getValue() [...]
      _temp6 = CALL _temp3.+(" ") [...]
      _temp7 = CALL _temp6.+(_temp5) [...]
      STORE rtn, _temp7
      ...
    ]
  ]

  catch_block:
  [
    catch_evaluation:
    [
      // Can access someVar in catch
      _temp8 = LOAD someVar
      ...
    ]
  ]

  finally_block:
  [
    finally_evaluation:
    [
      // Can access someVar in finally
      _temp9 = LOAD someVar
      ...
    ]
  ]
]
```

#### Step 9: Try with Pre-flow Guarded Assignment

**Test File**: `irGeneration/exceptions/tryWithPreflowGuard.ek9`
```ek9
defines module exceptions.test

  defines function

    maybeGetValue()
      <- rtn as String: String()  // Returns unset value

    @IR: IR_GENERATION: FUNCTION: "exceptions.test::tryWithPreflowGuard"
    tryWithPreflowGuard()
      existingVar <- String()

      try existingVar ?= maybeGetValue()
        // Only executes if maybeGetValue() returned a SET value
        assert existingVar?

      // If guard failed, existingVar still unset
      assert not existingVar?
```

**Expected IR**:
```
EXCEPTION_HANDLER_BLOCK
[
  handler_type: "TRY"

  preflow_variable:
  [
    preflow_type: "GUARDED_ASSIGNMENT"
    preflow_variable_name: "existingVar"
    preflow_variable_type: "org.ek9.lang::String"

    preflow_evaluation:
    [
      SCOPE_ENTER _scope_preflow
      _temp_result = CALL maybeGetValue() [...]
      // Guarded assignment: only assign if existingVar currently unset
      // (GuardedAssignmentInstr or similar)
      SCOPE_EXIT _scope_preflow
    ]

    has_guard: true
    guard_check:
    [
      // Check if assignment succeeded and result is set
      SCOPE_ENTER _scope_guard_check
      _temp_guard = CALL (org.ek9.lang::String)existingVar._isSet() [...]
      _temp_guard_bool = CALL (org.ek9.lang::Boolean)_temp_guard._true() [...]
      SCOPE_EXIT _scope_guard_check
    ]
    guard_result: _temp_guard
    guard_primitive: _temp_guard_bool
  ]

  try_block:
  [
    // Only executes if guard passed
    try_body_evaluation:
    [
      // Assert existingVar is set
      ...
    ]
  ]
]
```

#### Step 10: Comprehensive Integration Test

**Test File**: `irGeneration/exceptions/comprehensiveTryCatchFinally.ek9`
```ek9
defines module exceptions.test

  defines function

    getPrefix()
      <- rtn as String: "prefix"

    @IR: IR_GENERATION: FUNCTION: "exceptions.test::comprehensiveTryCatchFinally"
    comprehensiveTryCatchFinally()

      result <- function prefix <- getPrefix()
        ->
          resource1 <- TestResource(prefix + "_res1").open()
          resource2 <- TestResource(prefix + "_res2").open()
        <-
          rtn as String: resource1.getValue() + " " + resource2.getValue()

        // Additional try body (could reassign rtn)
        assert prefix?
        assert resource1? and resource2?
        assert rtn?

      catch
        -> ex as Exception
        // Can access prefix, resources, rtn
        assert prefix?
        assert resource1?
        rtn: prefix + " error: " + $ex

      finally
        // Can access prefix, resources, rtn
        assert prefix?
        assert resource1? and resource2?
        rtn :=? prefix + " fallback"

      // Resources closed here (resource2, then resource1)
      // result = final value of rtn
      assert result?
```

**Expected IR**: Combination of all previous features.

---

## Bytecode Generation Strategy

### Phase 14 (CODE_GENERATION) Implementation Plan

#### JVM Exception Table Structure

**Key Insight**: With no early returns, only 2 copies of finally/cleanup code needed:
1. Normal completion path
2. Exception propagation path

**Bytecode Structure**:
```
METHOD_START:
  // Pre-flow initialization (if present)
  <preflow bytecode>

  // Resource initialization (if present)
  <resource1 init>
  astore resource1_slot
  <resource2 init>
  astore resource2_slot

  // Return variable initialization (if present)
  <rtn init>
  astore rtn_slot

TRY_START:
  // Try block body
  <try body bytecode>
  // Try completes normally
  goto FINALLY_NORMAL

TRY_END:

CATCH_START:
  // Exception caught, stored in ex
  astore ex_slot

  // Catch block body
  <catch body bytecode>
  // Catch completes normally
  goto FINALLY_NORMAL

CATCH_END:

FINALLY_NORMAL:
  // Finally block (normal path)
  <finally body bytecode>

  // Resource cleanup (normal path)
  aload resource2_slot
  invokevirtual close
  aload resource1_slot
  invokevirtual close

  // Return rtn (if expression form)
  aload rtn_slot
  areturn
  // Or just return (if statement form)
  return

FINALLY_EXCEPTION:
  // Finally block (exception path - same bytecode)
  <finally body bytecode>

  // Resource cleanup (exception path - same bytecode)
  aload resource2_slot
  invokevirtual close
  aload resource1_slot
  invokevirtual close

  // Propagate exception
  athrow

EXCEPTION_TABLE:
  TRY_START → TRY_END : CATCH_START (org/ek9/lang/Exception)
  TRY_START → CATCH_END : FINALLY_EXCEPTION (any exception)
```

**Note**: Finally and resource cleanup bytecode duplicated only ONCE (2 copies total).

#### Bytecode Generator Class Structure

**Location**: `compiler-main/src/main/java/org/ek9lang/compiler/backend/jvm/TryCatchFinallyAsmGenerator.java`

**Key Methods**:
```java
class TryCatchFinallyAsmGenerator extends AbstractAsmGenerator {

  void generate(ExceptionHandlerBlockInstr instr) {
    // 1. Generate preflow (if present)
    generatePreflow(instr);

    // 2. Generate resources (if present)
    generateResources(instr);

    // 3. Generate rtn initialization (if present)
    generateReturnVariable(instr);

    // 4. Mark try start
    Label tryStart = new Label();
    Label tryEnd = new Label();
    Label catchStart = new Label();
    Label catchEnd = new Label();
    Label finallyNormal = new Label();
    Label finallyException = new Label();

    mv.visitLabel(tryStart);

    // 5. Generate try body
    generateTryBody(instr);

    // 6. Normal exit from try
    mv.visitLabel(tryEnd);
    mv.visitJumpInsn(GOTO, finallyNormal);

    // 7. Generate catch (if present)
    if (instr.hasCatch()) {
      mv.visitLabel(catchStart);
      generateCatchBody(instr);
      mv.visitLabel(catchEnd);
      mv.visitJumpInsn(GOTO, finallyNormal);
    }

    // 8. Finally - normal path
    mv.visitLabel(finallyNormal);
    generateFinallyBody(instr);
    generateResourceCleanup(instr);
    generateNormalReturn(instr);

    // 9. Finally - exception path
    mv.visitLabel(finallyException);
    generateFinallyBody(instr);  // Same bytecode!
    generateResourceCleanup(instr);  // Same bytecode!
    mv.visitInsn(ATHROW);

    // 10. Exception table
    String catchType = instr.hasCatch()
        ? instr.getCatchExceptionType()
        : null;
    mv.visitTryCatchBlock(tryStart, tryEnd,
        instr.hasCatch() ? catchStart : finallyException,
        catchType);

    if (instr.hasCatch()) {
      mv.visitTryCatchBlock(tryStart, catchEnd, finallyException, null);
    }
  }
}
```

#### Resource Cleanup Bytecode Pattern

**LIFO Order** (reverse of declaration):
```java
void generateResourceCleanup(ExceptionHandlerBlockInstr instr) {
  List<ResourceInfo> resources = instr.getResources();

  // Reverse order (LIFO)
  for (int i = resources.size() - 1; i >= 0; i--) {
    ResourceInfo resource = resources.get(i);

    // Load resource
    mv.visitVarInsn(ALOAD, resource.getLocalVarSlot());

    // Call close()
    String descriptor = resource.getCloseDescriptor();
    mv.visitMethodInsn(INVOKEVIRTUAL,
        resource.getInternalName(),
        "close",
        descriptor,
        false);
  }
}
```

#### Guard Semantics Bytecode

**Pre-flow with Guard**:
```java
void generatePreflowWithGuard(PreflowInfo preflow) {
  // Evaluate and assign
  generateExpression(preflow.getInitExpression());
  mv.visitVarInsn(ASTORE, preflow.getLocalVarSlot());

  // Check if set (if has guard)
  if (preflow.hasGuard()) {
    mv.visitVarInsn(ALOAD, preflow.getLocalVarSlot());

    // Call _isSet()
    mv.visitMethodInsn(INVOKEVIRTUAL,
        preflow.getInternalTypeName(),
        "_isSet",
        "()Lorg/ek9/lang/Boolean;",
        false);

    // Call _true()
    mv.visitMethodInsn(INVOKEVIRTUAL,
        "org/ek9/lang/Boolean",
        "_true",
        "()Z",
        false);

    // If false, skip try block
    Label skipTry = new Label();
    mv.visitJumpInsn(IFEQ, skipTry);

    // Try block executes here if guard passed
    // ...

    mv.visitLabel(skipTry);
  }
}
```

---

## Summary and Next Steps

### Key Design Achievements

**1. No Early Returns = Massive Simplification**
- Linear control flow
- Single resource cleanup location
- Only 2 copies of finally/cleanup code
- No complex return value save/restore

**2. IR Structure Matches EK9 Semantics**
- Pre-flow variables in header
- Resources in header with auto-close
- Return variable in header (mutable throughout)
- Sequential block execution
- Explicit resource cleanup

**3. Backend Implementation is Straightforward**
- Standard JVM exception tables
- Minimal finally duplication
- Clear control flow paths
- No special return handling needed

### Implementation Complexity: LOW ✅

**Rationale**:
- Grammar is clear and stable
- Phase validation already implemented
- IR structure is natural extension of CONTROL_FLOW_CHAIN
- No early returns eliminates major complexity
- Resource management is explicit and simple
- Sequential execution model is easy to reason about

### Implementation Order (Recommended)

**Week 1: Basic Try/Catch/Finally (No Resources, No Return)**
- Day 1-2: IR instruction classes
- Day 3-4: Phase 7 generator (simple forms)
- Day 5: Tests for simple try, try-catch, try-finally, try-catch-finally

**Week 2: Resources and Return Variables**
- Day 1-2: Resource initialization and cleanup in IR
- Day 3: Return variable handling
- Day 4-5: Tests for resources and return variables

**Week 3: Pre-flow and Integration**
- Day 1-2: Pre-flow variable handling
- Day 3: Guard semantics
- Day 4-5: Comprehensive integration tests

**Week 4: Bytecode Generation**
- Day 1-3: JVM bytecode generator
- Day 4-5: Executable tests, validation, debugging

### Critical Success Factors

**1. Test-Driven Development**
- Start with simplest case
- Progressive complexity
- Validate IR before bytecode

**2. Leverage Existing Patterns**
- CONTROL_FLOW_CHAIN architecture
- SCOPE_ENTER/EXIT pattern
- RETAIN/RELEASE memory management
- Temporary variable generation

**3. Resource Cleanup Guarantee**
- LIFO order (reverse of declaration)
- Happens after finally
- Single location in IR and bytecode

**4. Scope Management**
- Pre-flow, resources, rtn in shared scope
- Exception parameter local to catch
- Proper SCOPE_REGISTER for all variables

---

## Appendix: Example IR Scenarios

### Scenario A: Simple Try-Catch with Variable Assignment

**EK9 Code**:
```ek9
value <- String()

try
  value: "success"
catch
  -> ex as Exception
  value: "error"
```

**IR**:
```
EXCEPTION_HANDLER_BLOCK
[
  handler_type: "TRY_CATCH"

  try_block:
  [
    SCOPE_ENTER _scope_try
    _temp1 = LOAD_LITERAL "success"
    STORE value, _temp1
    SCOPE_EXIT _scope_try
  ]

  catch_block:
  [
    SCOPE_ENTER _scope_catch
    REFERENCE ex, org.ek9.lang::Exception
    RETAIN ex
    SCOPE_REGISTER ex, _scope_catch
    _temp2 = LOAD_LITERAL "error"
    STORE value, _temp2
    SCOPE_EXIT _scope_catch
  ]
]
```

### Scenario B: Expression Form with Resource

**EK9 Code**:
```ek9
result <- function
  -> resource <- TestResource("test").open()
  <- rtn as String: resource.getValue()
finally
  rtn :=? "fallback"
```

**IR**:
```
EXCEPTION_HANDLER_BLOCK
[
  handler_type: "TRY_FINALLY"

  resources:
  [
    [resource initialization]
  ]

  return_variable:
  [
    return_initialization:
    [
      _temp_res = LOAD resource
      _temp_val = CALL _temp_res.getValue()
      STORE rtn, _temp_val
    ]
  ]

  finally_block:
  [
    finally_evaluation:
    [
      // rtn :=? "fallback"
      _temp1 = LOAD rtn
      _temp2 = CALL _temp1._isSet()
      _temp3 = CALL _temp2._false()
      // If false (unset), assign fallback
      GUARDED_ASSIGNMENT rtn, "fallback", _temp3
    ]
  ]

  resource_cleanup:
  [
    CALL resource.close()
    RELEASE resource
  ]
]
```

### Scenario C: Pre-flow with Resources and Catch

**EK9 Code**:
```ek9
result <- function prefix <- getPrefix()
  ->
    res1 <- TestResource(prefix + "_1").open()
    res2 <- TestResource(prefix + "_2").open()
  <- rtn as String: res1.getValue()
  rtn: rtn + " " + res2.getValue()
catch
  -> ex as Exception
  rtn: prefix + " error"
```

**IR**:
```
EXCEPTION_HANDLER_BLOCK
[
  handler_type: "TRY_CATCH"

  preflow_variable:
  [
    preflow_type: "DECLARATION"
    preflow_variable_name: "prefix"
    preflow_evaluation: [CALL getPrefix(), STORE prefix]
  ]

  resources:
  [
    [res1 initialization using prefix],
    [res2 initialization using prefix]
  ]

  return_variable:
  [
    return_initialization: [res1.getValue(), STORE rtn]
  ]

  try_block:
  [
    // rtn: rtn + " " + res2.getValue()
    _temp1 = LOAD rtn
    _temp2 = LOAD res2
    _temp3 = CALL _temp2.getValue()
    _temp4 = CALL _temp1.+(" ")
    _temp5 = CALL _temp4.+(_temp3)
    STORE rtn, _temp5
  ]

  catch_block:
  [
    // rtn: prefix + " error"
    _temp6 = LOAD prefix
    _temp7 = CALL _temp6.+(" error")
    STORE rtn, _temp7
  ]

  resource_cleanup:
  [
    CALL res2.close()
    RELEASE res2
    CALL res1.close()
    RELEASE res1
  ]
]
```

---

*Document Complete - Ready for Implementation*

**Confidence Level**: 9.5/10

**Remaining 0.5 Uncertainty**:
- Edge cases in guard semantics (will discover during implementation)
- Interaction between finally and resource cleanup timing (should be straightforward)
- Nested try/catch/finally scenarios (should follow naturally from scope hierarchy)

**Ready to Proceed**: YES ✅
