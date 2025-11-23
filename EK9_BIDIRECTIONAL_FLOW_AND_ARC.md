# EK9 Bidirectional Flow and ARC Memory Management

**Document Status**: Authoritative architectural reference
**Last Updated**: 2025-10-31
**Related Documents**:
- `EK9_ARC_MEMORY_MANAGEMENT.md` - Comprehensive ARC documentation
- `EK9_IR_MEMORY_MANAGEMENT.md` - Memory architecture
- `EK9_ARC_OWNERSHIP_TRANSFER_PATTERN.md` - Ownership transfer patterns
- `EK9_TRY_CATCH_FINALLY_IR_DESIGN.md` - Exception handling IR design

---

## Executive Summary

EK9's memory management architecture is fundamentally based on **bidirectional object flow**:

1. **Normal Downward Flow**: Objects created in outer scopes flow DOWN to inner scopes (caller → callee, parent → child)
2. **Exception Upward Flow**: Exceptions created in inner scopes flow UP through stack unwinding (callee → caller, child → parent)

**Critical Insight**: The direction of flow determines the ARC (Automatic Reference Counting) pattern required for safe memory management.

### Why Exception ARC Differs

**The Problem**: If exceptions used the normal RETAIN + SCOPE_REGISTER pattern, the exception object would be **deallocated during stack unwinding** because the scope that created it would exit, causing a **use-after-free** error when the catch handler tries to access it.

**The Solution**: Exceptions receive an **extra RETAIN before THROW** to ensure the object survives stack unwinding, and the catch handler takes ownership without an additional RETAIN.

### Key Pattern Difference

| Flow Type | Direction | ARC Pattern | Lifetime |
|-----------|-----------|-------------|----------|
| **Normal** | ⬇️ DOWN (outer → inner) | RETAIN + SCOPE_REGISTER | Object dies when creator scope exits |
| **Exception** | ⬆️ UP (inner → outer) | RETAIN + SCOPE_REGISTER + **extra RETAIN** before THROW | Object survives creator scope, dies when catcher scope exits |

---

## Table of Contents

1. [Normal Downward Flow](#normal-downward-flow)
2. [Exception Upward Flow](#exception-upward-flow)
3. [Why Patterns Must Differ](#why-patterns-must-differ)
4. [Try-With-Resources in Both Scenarios](#try-with-resources-in-both-scenarios)
5. [Complete Memory Traces](#complete-memory-traces)
6. [Pattern Comparison Tables](#pattern-comparison-tables)
7. [Comparison with Other Languages](#comparison-with-other-languages)
8. [Backend Implementation Notes](#backend-implementation-notes)
9. [Common Misconceptions](#common-misconceptions)
10. [Debugging and Verification](#debugging-and-verification)
11. [Appendices](#appendices)

---

## 1. Normal Downward Flow

### Conceptual Model

In normal execution, objects flow **downward** through the call stack:

```
┌─────────────────────────────────┐
│  Outer Scope (e.g., main)      │  Higher stack frame
│  - Creates objects              │
│  - Passes to inner scopes       │
└────────────┬────────────────────┘
             │ ⬇️ Downward flow
             │
┌────────────▼────────────────────┐
│  Inner Scope (e.g., function)  │  Lower stack frame
│  - Receives objects             │
│  - Uses objects                 │
│  - Returns control              │
└─────────────────────────────────┘
```

### Key Characteristics

1. **Creator outlives consumer**: The outer scope that creates the object remains alive while inner scopes use it
2. **Ownership remains with creator**: The creator holds the primary reference
3. **Automatic cleanup**: When creator scope exits, object is deallocated
4. **Stack discipline**: LIFO (Last In, First Out) - inner scopes exit before outer scopes

### EK9 Source Example

```ek9
defines function
  outerFunction()
    value <- String("Hello")      // Created in outer scope
    result <- innerFunction(value) // Passed DOWN to inner scope
    // value still alive here

defines function
  innerFunction()
    -> input as String             // Receives from outer scope
    <- rtn as String: input        // Uses and returns
```

### Generated IR - Normal Downward Flow

```
OperationDfn: outerFunction._call()->org.ek9.lang::Void
BasicBlock: _entry_1
SCOPE_ENTER _scope_1

// Create object in OUTER scope
REFERENCE value, org.ek9.lang::String
_temp1 = LOAD_LITERAL Hello, org.ek9.lang::String
_temp2 = CALL String.<init>(_temp1)
RETAIN _temp2                          // ref_count = 1 (creator claims ownership)
SCOPE_REGISTER _temp2, _scope_1        // Register to outer scope
STORE value, _temp2

// Pass to INNER scope (downward)
_temp3 = LOAD value                    // ref_count still = 1
_temp4 = CALL innerFunction(_temp3)    // Pass to inner function
                                       // Inner function doesn't RETAIN (temporary use)

// Object still alive in outer scope
SCOPE_EXIT _scope_1                    // Implicit: RELEASE _temp2 → ref_count = 0
                                       // Object deallocated when outer scope exits
```

### Reference Count Trace - Downward Flow

```
Event                              | ref_count | Object State       | Stack Frame
-----------------------------------|-----------|--------------------|--------------
CALL String.<init>()               | 0         | Created            | outer
RETAIN _temp2                      | 1         | Owned by outer     | outer
SCOPE_REGISTER _temp2, _scope_1    | 1         | Registered         | outer
CALL innerFunction(_temp3)         | 1         | In use by inner    | inner
  (inner function returns)         | 1         | Back to outer      | outer
SCOPE_EXIT _scope_1                | 0         | Deallocated        | (none)
```

**Key Observation**: Object lifetime is controlled by the **creator (outer) scope**. When outer scope exits, object dies.

---

## 2. Exception Upward Flow

### Conceptual Model

When exceptions are thrown, objects flow **upward** through the call stack during unwinding:

```
┌─────────────────────────────────┐
│  Outer Scope (catch handler)   │  Higher stack frame
│  - RECEIVES exception           │  ⬆️ Destination
│  - Takes ownership              │
└────────────▲────────────────────┘
             │ ⬆️ Upward flow (unwinding)
             │
┌────────────┴────────────────────┐
│  Inner Scope (throw site)       │  Lower stack frame
│  - CREATES exception            │  ⬆️ Source
│  - THROWS exception             │
│  - Scope EXITS during unwind    │  ❌ Creator dies!
└─────────────────────────────────┘
```

### Key Characteristics

1. **Consumer outlives creator**: The inner scope that creates the exception **exits during unwinding** before the outer scope catches it
2. **Ownership transfer during unwinding**: Exception object must **survive** the destruction of its creator scope
3. **Reverse cleanup order**: Inner scopes exit during unwinding, but exception must remain alive
4. **Upward propagation**: Exception travels UP through stack frames until caught

### EK9 Source Example

```ek9
defines function
  outerFunction()
    try
      innerFunction()              // Call inner scope
    catch
      -> ex as Exception           // RECEIVES exception from inner scope
      handleError(ex)

defines function
  innerFunction()
    ex <- Exception("Error")       // CREATE in inner scope
    throw ex                       // THROW to outer scope
                                   // ⚠️ Inner scope EXITS during unwind!
```

### Generated IR - Exception Upward Flow

```
OperationDfn: innerFunction._call()->org.ek9.lang::Void
BasicBlock: _entry_1
SCOPE_ENTER _scope_1

// Create exception in INNER scope
REFERENCE ex, org.ek9.lang::Exception
_temp1 = LOAD_LITERAL Error, org.ek9.lang::String
_temp2 = CALL Exception.<init>(_temp1)
RETAIN _temp2                          // ref_count = 1 (initial ownership)
SCOPE_REGISTER _temp2, _scope_1        // Register to inner scope
STORE ex, _temp2
RETAIN ex                              // ref_count = 2 (variable ownership)
SCOPE_REGISTER ex, _scope_1            // Register variable

// ⚠️ CRITICAL: Extra RETAIN before THROW
RETAIN ex                              // ref_count = 3 (for upward transfer!)
THROW ex                               // Begin stack unwinding

// ⚠️ CRITICAL: Scope exits during unwinding
SCOPE_EXIT _scope_1                    // Implicit: RELEASE _temp2, RELEASE ex
                                       // ref_count = 3 - 2 = 1 ✅
                                       // Object SURVIVES creator scope exit!

// Exception propagates UP to catch handler...

OperationDfn: outerFunction._call()->org.ek9.lang::Void
catch_handler:
SCOPE_ENTER _scope_2

// ⚠️ CRITICAL: Catch receives exception WITHOUT RETAIN
REFERENCE ex, org.ek9.lang::Exception  // ref_count still = 1 (transferred ownership)
SCOPE_REGISTER ex, _scope_2            // Take ownership in catcher scope

// Use exception
_temp5 = CALL handleError(ex)

// Catcher scope exits
SCOPE_EXIT _scope_2                    // Implicit: RELEASE ex
                                       // ref_count = 1 - 1 = 0
                                       // Object DEALLOCATED in catcher scope
```

### Reference Count Trace - Upward Flow

```
Event                              | ref_count | Object State           | Stack Frame
-----------------------------------|-----------|------------------------|---------------
CALL Exception.<init>()            | 0         | Created                | inner (throw site)
RETAIN _temp2                      | 1         | Owned by inner         | inner
SCOPE_REGISTER _temp2, _scope_1    | 1         | Registered             | inner
RETAIN ex (variable)               | 2         | Variable ownership     | inner
SCOPE_REGISTER ex, _scope_1        | 2         | Variable registered    | inner
RETAIN ex (before THROW)           | 3         | Transfer ownership UP  | inner
THROW ex                           | 3         | Unwinding begins       | (unwinding)
SCOPE_EXIT _scope_1 (inner exits!) | 1         | Survived creator exit! | (unwinding) ✅
  (exception propagates upward)    | 1         | In flight              | (unwinding)
SCOPE_ENTER _scope_2 (catch)       | 1         | Caught by outer        | outer (catch)
SCOPE_REGISTER ex, _scope_2        | 1         | Owned by catcher       | outer
  (use exception)                  | 1         | In use                 | outer
SCOPE_EXIT _scope_2                | 0         | Deallocated            | (none)
```

**Key Observation**: Exception object lifetime is controlled by the **catcher (outer) scope**, but the object must **survive** the exit of the **creator (inner) scope** during unwinding. This requires the extra RETAIN before THROW.

---

## 3. Why Patterns Must Differ

### The Use-After-Free Problem

**Without the extra RETAIN before THROW**, this catastrophic scenario would occur:

```
// ❌ INCORRECT: Using normal downward flow pattern for exceptions

SCOPE_ENTER _scope_1 (inner scope)

// Create exception (normal pattern)
_temp2 = CALL Exception.<init>()
RETAIN _temp2                      // ref_count = 1
SCOPE_REGISTER _temp2, _scope_1
STORE ex, _temp2
RETAIN ex                          // ref_count = 2
SCOPE_REGISTER ex, _scope_1

// Throw without extra RETAIN
THROW ex                           // ref_count still = 2

// Stack unwinding causes scope exit
SCOPE_EXIT _scope_1
  RELEASE _temp2                   // ref_count = 1
  RELEASE ex                       // ref_count = 0 ❌
  // Object DEALLOCATED!

// Exception propagates to catch handler
catch_handler:
  REFERENCE ex                     // ❌ DANGLING POINTER!
  // Accessing FREED MEMORY → use-after-free bug!
```

**Result**: Catch handler receives a **dangling pointer** to freed memory. This is a critical memory safety violation.

### The Solution: Extra RETAIN for Ownership Transfer

```
// ✅ CORRECT: Exception upward flow pattern

SCOPE_ENTER _scope_1 (inner scope)

// Create exception
_temp2 = CALL Exception.<init>()
RETAIN _temp2                      // ref_count = 1
SCOPE_REGISTER _temp2, _scope_1
STORE ex, _temp2
RETAIN ex                          // ref_count = 2
SCOPE_REGISTER ex, _scope_1

// ✅ Extra RETAIN for ownership transfer
RETAIN ex                          // ref_count = 3

// Throw exception
THROW ex

// Stack unwinding causes scope exit
SCOPE_EXIT _scope_1
  RELEASE _temp2                   // ref_count = 2
  RELEASE ex                       // ref_count = 1 ✅
  // Object SURVIVES!

// Exception propagates to catch handler
catch_handler:
  REFERENCE ex                     // ✅ Valid pointer, ref_count = 1
  SCOPE_REGISTER ex, _scope_2      // Take ownership
  // Safe access!

SCOPE_EXIT _scope_2
  RELEASE ex                       // ref_count = 0
  // Object deallocated safely in catcher scope
```

**Result**: Exception object survives creator scope exit and is safely transferred to catch handler.

### Asymmetry Explained

| Aspect | Normal Downward | Exception Upward |
|--------|-----------------|------------------|
| **Creator lifetime** | Creator outlives consumers | Creator **exits before** consumer receives |
| **Ownership transfer** | Temporary borrowing | **Permanent transfer** during unwinding |
| **RETAIN pattern** | RETAIN + SCOPE_REGISTER | RETAIN + SCOPE_REGISTER + **extra RETAIN** |
| **Consumer RETAIN** | No (temporary use) | **No** (receives with transferred ownership) |
| **Deallocation site** | Creator scope exit | **Consumer scope exit** |

### Why Catch Handler Doesn't RETAIN

The catch handler receives the exception with `ref_count = 1` (from the extra RETAIN before THROW). This represents **transferred ownership**, not borrowed access:

```
catch_handler:
  REFERENCE ex                     // ref_count = 1 (from throw site)
  SCOPE_REGISTER ex, _scope_2      // Take ownership (no RETAIN needed)
  // ex is now owned by catch scope

SCOPE_EXIT _scope_2
  RELEASE ex                       // ref_count = 0, deallocate
```

**If catch did RETAIN**, this would happen:

```
catch_handler:
  REFERENCE ex                     // ref_count = 1
  RETAIN ex                        // ref_count = 2 ❌
  SCOPE_REGISTER ex, _scope_2

SCOPE_EXIT _scope_2
  RELEASE ex                       // ref_count = 1 ❌
  // Object NOT deallocated! Memory leak!
```

The asymmetry (RETAIN before THROW, no RETAIN in catch) is essential for correct ownership transfer.

---

## 4. Try-With-Resources in Both Scenarios

Try-with-resources combines both flow patterns:

1. **Resources**: Normal downward flow (declared in try header, used in try body)
2. **Exceptions**: Upward flow (thrown in try body, caught in catch handler)

### EK9 Source Example

```ek9
defines function
  testResource()
    <- rtn as String: String()

    try
      -> resource <- SimpleResource("test")  // Resource: downward flow
      rtn: performOperation(resource)        // May throw exception: upward flow
    catch
      -> ex as Exception                     // Exception: upward flow
      rtn: "ERROR"
```

### Generated IR - Dual Flow Pattern

```
SCOPE_ENTER _scope_1  // Outer wrapper

SCOPE_ENTER _scope_2  // Try wrapper

// DOWNWARD FLOW: Resource declaration
SCOPE_ENTER _scope_3  // Resource scope
REFERENCE resource, SimpleResource
_temp2 = CALL SimpleResource.<init>()
RETAIN _temp2                          // ref_count = 1 (downward flow pattern)
SCOPE_REGISTER _temp2, _scope_3        // Register to resource scope
STORE resource, _temp2

SCOPE_ENTER _scope_4  // Control flow scope

// TRY-CATCH-FINALLY structure
CONTROL_FLOW_CHAIN
  try_block_details:
    SCOPE_ENTER _scope_5  // Try body scope

    // Normal downward flow: passing resource
    _temp3 = LOAD resource
    _temp4 = CALL performOperation(_temp3)  // May throw exception

    // If exception thrown here:
    //   RETAIN exception              // ref_count = 3 (upward flow pattern)
    //   THROW exception
    //   SCOPE_EXIT _scope_5           // Try body scope exits
    //   SCOPE_EXIT _scope_4           // Control flow scope exits
    //   (Jump to catch handler)

    SCOPE_EXIT _scope_5

  condition_chain:
    case_scope_id: _scope_6
    case_type: "EXCEPTION_HANDLER"
    body_evaluation:
      SCOPE_ENTER _scope_6  // Catch scope

      // UPWARD FLOW: Exception received
      REFERENCE ex, Exception         // ref_count = 1 (transferred ownership)
      SCOPE_REGISTER ex, _scope_6     // No RETAIN! (upward flow pattern)

      // Handle exception
      _temp5 = LOAD_LITERAL ERROR, String
      STORE rtn, _temp5

      SCOPE_EXIT _scope_6             // Release exception

  finally_block_evaluation:
    SCOPE_ENTER _scope_7  // Finally scope

    // DOWNWARD FLOW: Resource cleanup
    CALL resource._close()            // Resource still valid (downward flow)

    SCOPE_EXIT _scope_7

SCOPE_EXIT _scope_4  // Control flow scope
SCOPE_EXIT _scope_3  // Resource scope - RELEASE resource (downward flow)
SCOPE_EXIT _scope_2  // Try wrapper scope
SCOPE_EXIT _scope_1  // Outer wrapper scope
```

### Memory Flow Visualization

```
Stack Growth (Downward) →

┌─────────────────────────────────────────────────┐
│ _scope_1 (outer wrapper)                        │
│ ┌─────────────────────────────────────────────┐ │
│ │ _scope_2 (try wrapper)                      │ │
│ │ ┌─────────────────────────────────────────┐ │ │
│ │ │ _scope_3 (resource scope)               │ │ │
│ │ │   resource ← SimpleResource()           │ │ │  ⬇️ DOWNWARD: Resource flows down
│ │ │   ref_count = 1                          │ │ │
│ │ │ ┌───────────────────────────────────┐   │ │ │
│ │ │ │ _scope_4 (control flow)           │   │ │ │
│ │ │ │ ┌─────────────────────────────┐   │   │ │ │
│ │ │ │ │ _scope_5 (try body)         │   │   │ │ │
│ │ │ │ │   performOperation(resource)│   │   │ │ │  ⬇️ Resource passed down
│ │ │ │ │   💥 Exception thrown!      │   │   │ │ │  ⬆️ UPWARD: Exception flows up
│ │ │ │ │   RETAIN ex (ref_count = 3) │   │   │ │ │
│ │ │ │ │   THROW ex                  │   │   │ │ │
│ │ │ │ └─────────────────────────────┘   │   │ │ │  (scope exits during unwind)
│ │ │ │ (unwinding...)                    │   │ │ │
│ │ │ └───────────────────────────────────┘   │ │ │  (scope exits during unwind)
│ │ │ ┌─────────────────────────────────┐     │ │ │
│ │ │ │ _scope_6 (catch handler)        │     │ │ │
│ │ │ │   ex (ref_count = 1) ✅         │     │ │ │  ⬆️ Exception received
│ │ │ │   SCOPE_REGISTER ex (no RETAIN) │     │ │ │
│ │ │ └─────────────────────────────────┘     │ │ │
│ │ │ ┌─────────────────────────────────┐     │ │ │
│ │ │ │ _scope_7 (finally)              │     │ │ │
│ │ │ │   resource._close()             │     │ │ │  ⬇️ Resource still valid
│ │ │ └─────────────────────────────────┘     │ │ │
│ │ └─────────────────────────────────────────┘ │ │
│ │   (RELEASE resource, ref_count = 0)         │ │  ⬇️ Resource cleanup
│ └─────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

### Key Insights

1. **Resources use downward flow pattern**: Created in outer scope, used in inner scopes, cleaned up when outer scope exits
2. **Exceptions use upward flow pattern**: Created in inner scope, propagate UP through unwinding, caught in outer scope
3. **Finally block accesses resources (downward)**: Resource cleanup happens in finally, which can still access resource because it's registered to an outer scope
4. **Exception survives resource scope**: Extra RETAIN ensures exception isn't deallocated when inner scopes exit during unwinding

---

## 5. Complete Memory Traces

### Trace 1: Normal Function Call (Downward Flow)

**EK9 Source**:
```ek9
defines function
  caller()
    data <- String("test")
    result <- callee(data)

defines function
  callee()
    -> input as String
    <- rtn as String: input
```

**Complete IR with ref_count annotations**:

```
// caller() scope
SCOPE_ENTER _scope_1

REFERENCE data, String
_temp1 = LOAD_LITERAL test, String
_temp2 = CALL String.<init>(_temp1)
RETAIN _temp2                          // ref_count: 0 → 1 (caller owns)
SCOPE_REGISTER _temp2, _scope_1
STORE data, _temp2

// Call callee
_temp3 = LOAD data                     // ref_count: still 1
_temp4 = CALL callee(_temp3)           // Pass to callee (borrowed, no RETAIN)
  // Inside callee:
  // REFERENCE input, String           // ref_count: still 1 (borrowed)
  // ... use input ...
  // RETURN input
  // (callee doesn't RETAIN, just borrows)

// Back in caller
SCOPE_EXIT _scope_1
  // Implicit: RELEASE _temp2         // ref_count: 1 → 0
  // data deallocated
```

**Memory Timeline**:
```
Time | Event                      | ref_count | Owner    | Location
-----|----------------------------|-----------|----------|----------
T0   | String.<init>()            | 0         | None     | caller
T1   | RETAIN _temp2              | 1         | caller   | caller
T2   | CALL callee(data)          | 1         | caller   | callee (borrowed)
T3   | Return from callee         | 1         | caller   | caller
T4   | SCOPE_EXIT _scope_1        | 0         | None     | (freed)
```

### Trace 2: Exception Throw and Catch (Upward Flow)

**EK9 Source**:
```ek9
defines function
  outer()
    try
      inner()
    catch
      -> ex as Exception
      handleError(ex)

defines function
  inner()
    error <- Exception("fail")
    throw error
```

**Complete IR with ref_count annotations**:

```
// inner() scope
SCOPE_ENTER _scope_1

REFERENCE error, Exception
_temp1 = LOAD_LITERAL fail, String
_temp2 = CALL Exception.<init>(_temp1)
RETAIN _temp2                          // ref_count: 0 → 1 (initial)
SCOPE_REGISTER _temp2, _scope_1
STORE error, _temp2
RETAIN error                           // ref_count: 1 → 2 (variable)
SCOPE_REGISTER error, _scope_1

// ⚠️ CRITICAL: Extra RETAIN for upward transfer
RETAIN error                           // ref_count: 2 → 3 (for throw)
THROW error

// Stack unwinding begins
SCOPE_EXIT _scope_1
  // Implicit: RELEASE _temp2         // ref_count: 3 → 2
  // Implicit: RELEASE error           // ref_count: 2 → 1 ✅
  // Exception SURVIVES!

// Exception propagates up...

// outer() catch handler
catch_handler:
SCOPE_ENTER _scope_2

REFERENCE ex, Exception                // ref_count: still 1 (transferred)
SCOPE_REGISTER ex, _scope_2            // No RETAIN! (ownership transfer)

// Use exception
_temp5 = LOAD ex
_temp6 = CALL handleError(_temp5)

SCOPE_EXIT _scope_2
  // Implicit: RELEASE ex              // ref_count: 1 → 0
  // Exception deallocated
```

**Memory Timeline**:
```
Time | Event                      | ref_count | Owner      | Location
-----|----------------------------|-----------|------------|-------------
T0   | Exception.<init>()         | 0         | None       | inner
T1   | RETAIN _temp2              | 1         | inner      | inner
T2   | RETAIN error (variable)    | 2         | inner      | inner
T3   | RETAIN error (for throw)   | 3         | transferring | inner
T4   | THROW error                | 3         | (unwinding)| unwinding
T5   | SCOPE_EXIT _scope_1 (inner)| 1         | (in flight)| unwinding ✅
T6   | Catch in outer             | 1         | outer      | outer
T7   | SCOPE_EXIT _scope_2 (catch)| 0         | None       | (freed)
```

**Critical Observation**: At T5, when inner scope exits during unwinding, ref_count drops from 3 to 1, not to 0. This is the key to preventing use-after-free.

### Trace 3: Try-With-Resources with Exception

**EK9 Source**:
```ek9
defines function
  test()
    <- rtn as String: String()

    try
      -> resource <- SimpleResource("db")
      rtn: dangerousOperation(resource)  // Throws exception
    catch
      -> ex as Exception
      rtn: "ERROR"
```

**Complete IR with ref_count annotations**:

```
SCOPE_ENTER _scope_1  // Outer

SCOPE_ENTER _scope_2  // Try wrapper

// RESOURCE CREATION (downward flow)
SCOPE_ENTER _scope_3  // Resource scope
REFERENCE resource, SimpleResource
_temp2 = CALL SimpleResource.<init>()
RETAIN _temp2                          // ref_count(resource): 0 → 1
SCOPE_REGISTER _temp2, _scope_3
STORE resource, _temp2

SCOPE_ENTER _scope_4  // Control flow

CONTROL_FLOW_CHAIN
  try_block:
    SCOPE_ENTER _scope_5  // Try body

    // Pass resource (downward, borrowed)
    _temp3 = LOAD resource             // ref_count(resource): still 1
    _temp4 = CALL dangerousOperation(_temp3)

    // 💥 Exception thrown inside dangerousOperation:
    //   _temp_ex = CALL Exception.<init>()
    //   RETAIN _temp_ex                // ref_count(ex): 0 → 1
    //   SCOPE_REGISTER _temp_ex, (inner scope)
    //   RETAIN _temp_ex                // ref_count(ex): 1 → 2
    //   SCOPE_REGISTER _temp_ex, (inner scope)
    //   RETAIN _temp_ex                // ref_count(ex): 2 → 3 (for throw)
    //   THROW _temp_ex

    // Stack unwinding:
    SCOPE_EXIT _scope_5                // Try body exits
    SCOPE_EXIT _scope_4                // Control flow exits
    // ref_count(resource): still 1 (registered to _scope_3, not exited yet)
    // ref_count(ex): 3 → 1 (survived inner scope exits)

  catch_handler:
    SCOPE_ENTER _scope_6  // Catch scope

    // EXCEPTION RECEIVED (upward flow)
    REFERENCE ex, Exception            // ref_count(ex): 1 (transferred)
    SCOPE_REGISTER ex, _scope_6        // No RETAIN!

    _temp5 = LOAD_LITERAL ERROR, String
    STORE rtn, _temp5

    SCOPE_EXIT _scope_6                // ref_count(ex): 1 → 0 (ex freed)

  finally_block:
    SCOPE_ENTER _scope_7  // Finally scope

    // RESOURCE CLEANUP (downward flow)
    CALL resource._close()             // ref_count(resource): still 1

    SCOPE_EXIT _scope_7

SCOPE_EXIT _scope_4  // Control flow
SCOPE_EXIT _scope_3  // Resource scope
  // Implicit: RELEASE _temp2         // ref_count(resource): 1 → 0 (resource freed)
SCOPE_EXIT _scope_2  // Try wrapper
SCOPE_EXIT _scope_1  // Outer
```

**Dual Memory Timeline**:

```
Time | Event                      | ref_count(resource) | ref_count(ex) | Notes
-----|----------------------------|---------------------|---------------|------------------
T0   | Resource.<init>()          | 1                   | -             | Downward flow
T1   | dangerousOperation() call  | 1 (borrowed)        | -             |
T2   | Exception.<init>()         | 1                   | 1             | Upward flow starts
T3   | RETAIN ex (variable)       | 1                   | 2             |
T4   | RETAIN ex (for throw)      | 1                   | 3             | Critical!
T5   | THROW ex                   | 1                   | 3             | Unwinding starts
T6   | SCOPE_EXIT _scope_5 (try)  | 1                   | 3             | Try body exits
T7   | SCOPE_EXIT _scope_4 (ctrl) | 1                   | 3             | Control flow exits
T8   | Jump to catch              | 1                   | 1             | Ex survives! ✅
T9   | Catch handler              | 1                   | 1             | Ex transferred
T10  | SCOPE_EXIT _scope_6 (catch)| 1                   | 0             | Ex freed
T11  | Finally block              | 1                   | -             | Resource cleanup
T12  | SCOPE_EXIT _scope_3 (res)  | 0                   | -             | Resource freed
```

**Key Insight**: Resource and exception have **independent lifecycles** because they use different flow patterns:
- **Resource** (downward): Lives from T0 to T12 (owned by resource scope)
- **Exception** (upward): Lives from T2 to T10 (transferred to catch scope)

---

## 6. Pattern Comparison Tables

### Table 1: Flow Direction Impact

| Aspect | Downward Flow (Normal) | Upward Flow (Exception) |
|--------|------------------------|-------------------------|
| **Direction** | Outer → Inner (⬇️) | Inner → Outer (⬆️) |
| **Creator lifetime** | Outlives consumers | **Exits before consumer** |
| **Consumer lifetime** | Exits before creator | **Outlives creator** |
| **Ownership** | Remains with creator | **Transferred to consumer** |
| **Deallocation** | Creator scope exit | **Consumer scope exit** |
| **Primary use case** | Function calls, resources | Exceptions, error propagation |

### Table 2: ARC Pattern Comparison

| Operation | Downward Flow IR | Upward Flow IR | Difference |
|-----------|------------------|----------------|------------|
| **Creation** | `CALL Type.<init>()` | `CALL Exception.<init>()` | Same |
| **Initial ownership** | `RETAIN _temp` (ref_count = 1) | `RETAIN _temp` (ref_count = 1) | Same |
| **Registration** | `SCOPE_REGISTER _temp, _scope` | `SCOPE_REGISTER _temp, _scope` | Same |
| **Variable storage** | `STORE var, _temp`<br>`RETAIN var` (ref_count = 2) | `STORE var, _temp`<br>`RETAIN var` (ref_count = 2) | Same |
| **Transfer prep** | None | **`RETAIN var` (ref_count = 3)** | **Extra RETAIN!** |
| **Transfer** | `CALL func(var)` (borrowed) | **`THROW var`** | **Different mechanism** |
| **Consumer receive** | Function param (no RETAIN) | **Catch param (no RETAIN)** | Same (no RETAIN) |
| **Consumer ownership** | Borrowed (temporary) | **Transferred (permanent)** | **Different semantics** |
| **Cleanup** | Creator scope exit | **Consumer scope exit** | **Different location** |

### Table 3: Reference Count Evolution

| Stage | Downward Flow | Upward Flow (Exception) | Explanation |
|-------|---------------|-------------------------|-------------|
| **After creation** | 1 | 1 | Both start at 1 |
| **After variable storage** | 2 | 2 | Both increment for variable |
| **Before transfer** | 2 | **3** | **Exception gets extra RETAIN** |
| **During transfer** | 2 (borrowed) | **3** (unwinding) | Exception in flight |
| **After creator exit** | N/A (creator still alive) | **1** (creator exited) | **Exception survives** |
| **After consumer exit** | 0 (creator exit) | **0** (consumer exit) | Different deallocation site |

### Table 4: Scope Exit Behavior

| Scenario | Downward Flow | Upward Flow | Why Different |
|----------|---------------|-------------|---------------|
| **Creator scope exits normally** | RELEASE object → ref_count = 0 | N/A (exception thrown) | Normal vs exceptional exit |
| **Creator scope exits during unwinding** | N/A (no unwinding) | RELEASE object → **ref_count = 1** | **Object survives due to extra RETAIN** |
| **Consumer scope exits** | No ownership (borrowed) | RELEASE object → ref_count = 0 | Consumer owns in exception case |

---

## 7. Comparison with Other Languages

### Swift (Similar ARC Model)

Swift's exception handling uses a similar pattern with ARC:

**Swift Code**:
```swift
func inner() throws {
    let error = NSError(domain: "test", code: 1)  // ref_count = 1
    throw error  // Swift runtime: +1 retain before throw (ref_count = 2)
    // Scope exits during unwinding: -1 release (ref_count = 1)
}

func outer() {
    do {
        try inner()
    } catch let ex {  // ref_count = 1 (transferred ownership)
        handleError(ex)
    }  // -1 release (ref_count = 0)
}
```

**Similarities to EK9**:
- Extra retain before throw
- No retain in catch (transferred ownership)
- Ref count = 1 when caught
- Deallocation in catch scope

**EK9 Advantage**: Explicit IR makes the pattern visible and verifiable

### Rust (Move Semantics)

Rust uses move semantics with Result/panic:

**Rust Code**:
```rust
fn inner() -> Result<(), String> {
    let error = String::from("fail");  // Owned by inner
    Err(error)  // Move ownership to Result
    // error dropped, but moved into Result first
}

fn outer() {
    match inner() {
        Ok(_) => {},
        Err(ex) => {  // ex owns the String
            handle_error(&ex);
        }  // ex dropped here
    }
}
```

**Differences from EK9**:
- Rust: Compile-time move checking (zero-cost abstraction)
- EK9: Runtime reference counting (overhead, but simpler)
- Rust: Explicit Result type (exceptions are values)
- EK9: Implicit exception propagation (similar to Java/Swift)

**EK9 Advantage**: No need for explicit Result types, more ergonomic

### Java (Garbage Collection)

Java uses GC, no explicit reference counting:

**Java Code**:
```java
void inner() throws Exception {
    Exception error = new Exception("fail");  // GC root
    throw error;  // Stored in exception table
    // GC keeps error alive during unwinding
}

void outer() {
    try {
        inner();
    } catch (Exception ex) {  // GC root in catch scope
        handleError(ex);
    }  // ex becomes unreachable, GC will collect
}
```

**Differences from EK9**:
- Java: GC scans all reachable objects (no ref counting)
- EK9: Explicit ARC (deterministic deallocation)
- Java: Non-deterministic cleanup (GC pauses)
- EK9: Predictable performance (ref count overhead)

**EK9 Advantage**: Deterministic deallocation, no GC pauses, suitable for real-time systems

### C++ (Exception Tables + RAII)

C++ uses exception tables and RAII:

**C++ Code**:
```cpp
void inner() {
    std::exception error("fail");  // Stack allocated
    throw error;  // Copy constructed into exception storage
    // Stack unwinds, original error destroyed
}

void outer() {
    try {
        inner();
    } catch (const std::exception& ex) {  // Reference to exception storage
        handleError(ex);
    }  // Exception storage destroyed
}
```

**Differences from EK9**:
- C++: Copy semantics (exception is copied during throw)
- EK9: Reference counted (same object, multiple owners)
- C++: RAII for cleanup (destructors called during unwinding)
- EK9: ARC for cleanup (RELEASE during unwinding)

**EK9 Advantage**: No copy overhead for exceptions, single object identity preserved

### Comparison Summary Table

| Language | Memory Model | Exception Lifetime | Transfer Mechanism | Deallocation |
|----------|--------------|--------------------|--------------------|--------------|
| **EK9** | ARC | Extra RETAIN before throw | Reference with ref_count = 1 | Consumer scope exit (deterministic) |
| **Swift** | ARC | Extra retain before throw | Reference with ref_count = 1 | Consumer scope exit (deterministic) |
| **Rust** | Ownership/Move | Moved into Result | Ownership transfer (compile-time) | Consumer scope exit (deterministic) |
| **Java** | GC | GC keeps alive | GC roots during unwinding | GC cycle (non-deterministic) |
| **C++** | RAII/Manual | Copy during throw | Copy into exception storage | Exception storage destruction (deterministic) |

**EK9's Position**: Closest to Swift's ARC model, with explicit IR making patterns verifiable.

---

## 8. Backend Implementation Notes

### JVM Backend

The JVM uses exception tables for control flow during unwinding. EK9's IR must map to JVM bytecode correctly:

#### Exception Throw Bytecode

**EK9 IR**:
```
RETAIN ex                    // ref_count = 3
THROW ex
```

**JVM Bytecode** (conceptual):
```
aload ex                     // Load exception reference
invokeinterface _retain      // Call ARC retain (ref_count = 3)
athrow                       // Throw exception (JVM unwinding begins)

// Exception table entry:
// from: try_start, to: try_end, target: catch_handler, type: Exception
```

**JVM Stack Unwinding**:
1. `athrow` triggers JVM exception mechanism
2. JVM scans exception table for matching handler
3. JVM unwinds stack frames, calling finally blocks
4. **CRITICAL**: EK9 finally blocks must call `_release` for scope-registered temps
5. When catch handler is reached, exception reference is on JVM stack

#### Catch Handler Bytecode

**EK9 IR**:
```
REFERENCE ex, Exception      // ref_count = 1 (transferred)
SCOPE_REGISTER ex, _scope_6  // No RETAIN
```

**JVM Bytecode** (conceptual):
```
catch_handler:
astore ex                    // Store exception reference (no _retain call!)
// Use exception
aload ex
invokeinterface getMessage
// ...

// Scope exit
aload ex
invokeinterface _release     // ref_count = 0, deallocate
return
```

**Key Implementation Detail**: JVM backend must **NOT** emit `_retain` call in catch handler, only in throw site.

#### SCOPE_EXIT Implementation

**EK9 IR**:
```
SCOPE_REGISTER _temp1, _scope_1
SCOPE_REGISTER _temp2, _scope_1
// ...
SCOPE_EXIT _scope_1  // Implicit: RELEASE _temp1, _temp2
```

**JVM Bytecode** (conceptual):
```
// At scope exit point (or in finally block)
aload _temp1
invokeinterface _release     // RELEASE _temp1

aload _temp2
invokeinterface _release     // RELEASE _temp2

// Repeat for all SCOPE_REGISTER'd temps in reverse order
```

**Exception Safety**: JVM finally blocks ensure SCOPE_EXIT runs even during unwinding.

### LLVM Backend

LLVM uses `invoke`/`landingpad` for exception handling. EK9's IR maps to LLVM IR:

#### Exception Throw LLVM IR

**EK9 IR**:
```
RETAIN ex                    // ref_count = 3
THROW ex
```

**LLVM IR** (conceptual):
```llvm
; Load exception object
%ex = load %Exception*, %Exception** %ex_ptr

; Extra retain for upward transfer
call void @ek9_retain(%Exception* %ex)  ; ref_count = 3

; Throw exception (LLVM unwinding)
call void @__cxa_throw(
    i8* bitcast(%Exception* %ex to i8*),
    i8* bitcast(@_ZTI9Exception to i8*),  ; Type info
    i8* null                              ; Destructor (none, ARC handles it)
)
unreachable
```

**LLVM Unwinding**:
1. `__cxa_throw` invokes Itanium C++ ABI exception mechanism
2. LLVM unwinds stack frames using `landingpad` instructions
3. **CRITICAL**: Cleanup landingpads must call `ek9_release` for scope-registered objects
4. When catch landingpad is reached, exception pointer is extracted

#### Catch Handler LLVM IR

**EK9 IR**:
```
REFERENCE ex, Exception      // ref_count = 1 (transferred)
SCOPE_REGISTER ex, _scope_6  // No RETAIN
```

**LLVM IR** (conceptual):
```llvm
catch_handler:
; Landingpad extracts exception
%landingpad = landingpad { i8*, i32 }
    catch i8* bitcast (@_ZTI9Exception to i8*)

%ex_ptr = extractvalue { i8*, i32 } %landingpad, 0
%ex = bitcast i8* %ex_ptr to %Exception*

; NO call to @ek9_retain here! (ref_count already = 1)

; Store exception
store %Exception* %ex, %Exception** %ex_local

; Use exception
%msg = call %String* @Exception_getMessage(%Exception* %ex)
; ...

; Scope exit
%ex_final = load %Exception*, %Exception** %ex_local
call void @ek9_release(%Exception* %ex_final)  ; ref_count = 0
ret void
```

**Key Implementation Detail**: LLVM backend must **NOT** emit `ek9_retain` in catch landingpad, only in throw site.

#### SCOPE_EXIT Implementation

**EK9 IR**:
```
SCOPE_REGISTER _temp1, _scope_1
SCOPE_REGISTER _temp2, _scope_1
// ...
SCOPE_EXIT _scope_1  // Implicit: RELEASE _temp1, _temp2
```

**LLVM IR** (conceptual):
```llvm
; Normal scope exit
scope_exit:
%temp1 = load %Type1*, %Type1** %temp1_ptr
call void @ek9_release(%Type1* %temp1)

%temp2 = load %Type2*, %Type2** %temp2_ptr
call void @ek9_release(%Type2* %temp2)

br label %next_block

; Cleanup landingpad (during unwinding)
cleanup_landingpad:
%lp = landingpad { i8*, i32 }
    cleanup

; Same releases as normal scope exit
%temp1_cleanup = load %Type1*, %Type1** %temp1_ptr
call void @ek9_release(%Type1* %temp1_cleanup)

%temp2_cleanup = load %Type2*, %Type2** %temp2_ptr
call void @ek9_release(%Type2* %temp2_cleanup)

resume { i8*, i32 } %lp  ; Continue unwinding
```

**Exception Safety**: LLVM cleanup landingpads ensure SCOPE_EXIT runs during unwinding.

### Backend Comparison Table

| Aspect | JVM Backend | LLVM Backend |
|--------|-------------|--------------|
| **Mechanism** | Exception tables + athrow | invoke/landingpad + __cxa_throw |
| **Unwinding** | JVM runtime | Itanium C++ ABI |
| **RETAIN implementation** | invokeinterface _retain | call @ek9_retain |
| **RELEASE implementation** | invokeinterface _release | call @ek9_release |
| **SCOPE_EXIT cleanup** | Finally blocks | Cleanup landingpads |
| **Type information** | Class metadata | RTTI (TypeInfo) |
| **Performance** | JVM overhead (polymorphic calls) | Native direct calls |

---

## 9. Common Misconceptions

### Misconception 1: "Extra RETAIN before THROW is redundant"

**Misconception**: Since the exception variable already has `ref_count = 2` (from temp + variable), why is a third RETAIN needed?

**Reality**: Without the extra RETAIN, the exception would be deallocated when the creator scope exits during unwinding:

```
// WITHOUT extra RETAIN:
RETAIN _temp                   // ref_count = 1
SCOPE_REGISTER _temp, _scope
RETAIN ex                      // ref_count = 2
SCOPE_REGISTER ex, _scope
THROW ex                       // ref_count still = 2
SCOPE_EXIT _scope
  RELEASE _temp                // ref_count = 1
  RELEASE ex                   // ref_count = 0 ❌ FREED!
// Catch handler receives dangling pointer!

// WITH extra RETAIN:
RETAIN ex                      // ref_count = 3
THROW ex
SCOPE_EXIT _scope
  RELEASE _temp                // ref_count = 2
  RELEASE ex                   // ref_count = 1 ✅ SURVIVES!
```

**Lesson**: The extra RETAIN is specifically to survive the SCOPE_EXIT during unwinding.

### Misconception 2: "Catch handler should RETAIN the exception"

**Misconception**: Since all other consumers RETAIN objects when receiving them, why doesn't the catch handler?

**Reality**: Catch handler receives **transferred ownership**, not borrowed access:

```
// If catch did RETAIN:
catch_handler:
  REFERENCE ex                 // ref_count = 1 (from throw site)
  RETAIN ex                    // ref_count = 2 ❌
  SCOPE_REGISTER ex, _scope
  // Use exception...
  SCOPE_EXIT _scope
    RELEASE ex                 // ref_count = 1 ❌
    // Memory leak! Object never deallocated!

// Correct (no RETAIN):
catch_handler:
  REFERENCE ex                 // ref_count = 1 (transferred ownership)
  SCOPE_REGISTER ex, _scope    // Take ownership
  // Use exception...
  SCOPE_EXIT _scope
    RELEASE ex                 // ref_count = 0 ✅ Deallocated
```

**Lesson**: Ownership transfer (throw→catch) is different from borrowing (function call).

### Misconception 3: "Exception pattern should match normal pattern for consistency"

**Misconception**: All object flows should use the same ARC pattern for consistency.

**Reality**: The direction of flow fundamentally determines the pattern:

| Flow Direction | Creator Lifetime | Pattern Required |
|----------------|------------------|------------------|
| **Downward** (normal) | Creator outlives consumer | RETAIN + SCOPE_REGISTER (ref_count = 1 in consumer) |
| **Upward** (exception) | **Creator exits before consumer** | RETAIN + SCOPE_REGISTER + **extra RETAIN** (ref_count = 1 in consumer) |

**Lesson**: Consistency comes from **correctness for the flow direction**, not from using identical patterns everywhere.

### Misconception 4: "Double registration (temp + variable) is a bug"

**Misconception**: Registering both `_temp` and `ex` to the same scope seems redundant.

**Reality**: They represent different lifetimes:

```
REFERENCE ex, Exception
_temp2 = CALL Exception.<init>()
RETAIN _temp2                   // Temporary lifetime
SCOPE_REGISTER _temp2, _scope_1 // Temp cleanup
STORE ex, _temp2
RETAIN ex                       // Variable lifetime
SCOPE_REGISTER ex, _scope_1     // Variable cleanup

// At scope exit:
SCOPE_EXIT _scope_1
  RELEASE _temp2                // Temp lifetime ends
  RELEASE ex                    // Variable lifetime ends
```

**Purpose**: `_temp` and `ex` are separate storage locations, each needs cleanup.

**Lesson**: Double registration handles both temp and variable lifetimes correctly.

### Misconception 5: "SCOPE_EXIT should have explicit RELEASE instructions"

**Misconception**: IR should show explicit `RELEASE` instructions for each `SCOPE_REGISTER`.

**Reality**: `SCOPE_EXIT` **implicitly** generates `RELEASE` for all registered temps:

```
// IR (as shown):
SCOPE_REGISTER _temp1, _scope_1
SCOPE_REGISTER _temp2, _scope_1
SCOPE_EXIT _scope_1  // Implicit: RELEASE _temp1, _temp2

// Backend generates (conceptual):
RELEASE _temp2  // Reverse order
RELEASE _temp1
```

**Reason**: Declarative pattern (SCOPE_REGISTER) is more robust than imperative (explicit RELEASE):
- Exception-safe (cleanup always happens)
- Can't forget cleanup
- Single exit point
- Backend can optimize

**Lesson**: SCOPE_REGISTER is a **consumer pattern** that declares "this scope owns this temp and will clean it up."

---

## 10. Debugging and Verification

### Verifying Correct ARC Patterns

#### Pattern 1: Normal Downward Flow

**Check**: Object created in outer scope, used in inner scope, deallocated when outer scope exits.

```
SCOPE_ENTER _scope_outer
REFERENCE obj, Type
_temp = CALL Type.<init>()
RETAIN _temp                          // ✅ ref_count = 1
SCOPE_REGISTER _temp, _scope_outer    // ✅ Registered to outer
STORE obj, _temp

SCOPE_ENTER _scope_inner
_temp2 = LOAD obj
CALL someFunction(_temp2)             // ✅ No RETAIN (borrowed)
SCOPE_EXIT _scope_inner               // ✅ Inner exits first

SCOPE_EXIT _scope_outer               // ✅ Outer exits last
                                      // ✅ RELEASE _temp → ref_count = 0
```

**Verification checklist**:
- [ ] Object registered to **outer** scope, not inner
- [ ] Inner scope does **NOT** RETAIN (borrowed access)
- [ ] Inner scope exits **before** outer scope
- [ ] Object deallocated when **outer** scope exits

#### Pattern 2: Exception Upward Flow

**Check**: Exception created in inner scope, thrown, caught in outer scope.

```
SCOPE_ENTER _scope_inner
REFERENCE ex, Exception
_temp = CALL Exception.<init>()
RETAIN _temp                          // ✅ ref_count = 1
SCOPE_REGISTER _temp, _scope_inner
STORE ex, _temp
RETAIN ex                             // ✅ ref_count = 2
SCOPE_REGISTER ex, _scope_inner
RETAIN ex                             // ✅ ref_count = 3 (CRITICAL!)
THROW ex

SCOPE_EXIT _scope_inner               // ✅ Inner exits during unwinding
                                      // ✅ RELEASE _temp, RELEASE ex
                                      // ✅ ref_count = 3 - 2 = 1 (survives!)

catch_handler:
SCOPE_ENTER _scope_outer
REFERENCE ex, Exception               // ✅ ref_count = 1 (transferred)
SCOPE_REGISTER ex, _scope_outer       // ✅ NO RETAIN! (ownership transfer)
// Use exception
SCOPE_EXIT _scope_outer               // ✅ RELEASE ex → ref_count = 0
```

**Verification checklist**:
- [ ] **Extra RETAIN** before THROW (ref_count = 3)
- [ ] Inner scope exits during unwinding (**before** catch)
- [ ] After inner exit, ref_count = 1 (not 0!)
- [ ] Catch handler does **NOT** RETAIN
- [ ] Exception deallocated when **catch scope** exits

#### Pattern 3: Try-With-Resources

**Check**: Resource uses downward flow, exception uses upward flow.

```
// Resource scope (outer)
SCOPE_ENTER _scope_resource
REFERENCE resource, Type
_temp1 = CALL Type.<init>()
RETAIN _temp1                         // ✅ ref_count(resource) = 1
SCOPE_REGISTER _temp1, _scope_resource // ✅ Registered to outer scope

// Try body (inner)
SCOPE_ENTER _scope_try
_temp2 = LOAD resource                // ✅ ref_count(resource) still = 1
CALL operation(_temp2)                // May throw exception

// If exception thrown:
//   RETAIN ex                        // ✅ ref_count(ex) = 3
//   THROW ex
//   SCOPE_EXIT _scope_try             // ✅ Try body exits
//   (resource still alive: registered to outer scope)

catch_handler:
SCOPE_ENTER _scope_catch
REFERENCE ex, Exception               // ✅ ref_count(ex) = 1
SCOPE_REGISTER ex, _scope_catch       // ✅ No RETAIN
SCOPE_EXIT _scope_catch               // ✅ RELEASE ex

finally_block:
SCOPE_ENTER _scope_finally
CALL resource._close()                // ✅ resource still valid
SCOPE_EXIT _scope_finally

SCOPE_EXIT _scope_resource            // ✅ RELEASE resource
```

**Verification checklist**:
- [ ] Resource registered to **outer** scope (survives try body exit)
- [ ] Exception uses **extra RETAIN** before THROW
- [ ] Catch handler does **NOT** RETAIN exception
- [ ] Finally block can access resource (still valid)
- [ ] Resource deallocated when **outer scope** exits
- [ ] Exception deallocated when **catch scope** exits

### Common Debugging Scenarios

#### Scenario 1: Use-After-Free in Catch Handler

**Symptom**: Crash or garbage data when accessing exception in catch handler.

**Likely Cause**: Missing extra RETAIN before THROW.

**Debug Steps**:
1. Find THROW instruction in IR
2. Check immediately before THROW:
   ```
   RETAIN ex  // This should be present!
   THROW ex
   ```
3. Trace ref_count through scope exit:
   ```
   ref_count before THROW:  should be 3
   ref_count after scope exit: should be 1
   ```
4. If ref_count = 0 after scope exit, **extra RETAIN is missing**.

#### Scenario 2: Memory Leak After Catch

**Symptom**: Exception object never deallocated, memory usage grows.

**Likely Cause**: Catch handler incorrectly RETAINs exception.

**Debug Steps**:
1. Find catch handler in IR
2. Check for RETAIN in catch:
   ```
   catch_handler:
   REFERENCE ex
   RETAIN ex  // ❌ This should NOT be present!
   ```
3. Trace ref_count through catch scope exit:
   ```
   ref_count entering catch: should be 1
   ref_count after SCOPE_EXIT: should be 0
   ```
4. If ref_count > 0 after catch exit, **remove spurious RETAIN**.

#### Scenario 3: Resource Deallocated Too Early

**Symptom**: Crash in finally block when calling resource._close().

**Likely Cause**: Resource registered to wrong scope (try body instead of resource scope).

**Debug Steps**:
1. Find resource declaration in IR
2. Check SCOPE_REGISTER:
   ```
   SCOPE_ENTER _scope_resource  // Outer
   SCOPE_ENTER _scope_try       // Inner

   REFERENCE resource, Type
   _temp = CALL Type.<init>()
   RETAIN _temp
   SCOPE_REGISTER _temp, _scope_resource  // ✅ Should be resource scope!
   ```
3. If registered to `_scope_try`, resource will be deallocated when try body exits.
4. **Fix**: Register to outer resource scope, not try body scope.

### IR Analysis Tools

#### Tool 1: Reference Count Tracer

Trace ref_count through IR execution:

```python
# Pseudocode for ref_count analysis tool
def trace_ref_count(ir_instructions, variable):
    ref_count = 0
    for instr in ir_instructions:
        if instr.opcode == "CALL" and instr.is_constructor():
            ref_count = 0
            print(f"{instr}: ref_count = 0 (created)")
        elif instr.opcode == "RETAIN" and instr.operand == variable:
            ref_count += 1
            print(f"{instr}: ref_count = {ref_count} (retain)")
        elif instr.opcode == "SCOPE_EXIT":
            # Count registered temps
            releases = count_registered_temps(instr.scope)
            ref_count -= releases
            print(f"{instr}: ref_count = {ref_count} (scope exit, {releases} releases)")
        elif instr.opcode == "THROW" and instr.operand == variable:
            print(f"{instr}: ref_count = {ref_count} (throw)")
    return ref_count

# Example usage:
trace_ref_count(ir, "ex")
# Output:
# CALL Exception.<init>(): ref_count = 0 (created)
# RETAIN _temp: ref_count = 1 (retain)
# RETAIN ex: ref_count = 2 (retain)
# RETAIN ex: ref_count = 3 (retain)
# THROW ex: ref_count = 3 (throw)
# SCOPE_EXIT _scope_1: ref_count = 1 (scope exit, 2 releases)
# SCOPE_EXIT _scope_2: ref_count = 0 (scope exit, 1 release)
```

#### Tool 2: Scope Registration Validator

Verify SCOPE_REGISTER matches scope hierarchy:

```python
# Pseudocode for scope validation tool
def validate_scope_registration(ir_instructions):
    scope_stack = []
    registrations = {}

    for instr in ir_instructions:
        if instr.opcode == "SCOPE_ENTER":
            scope_stack.append(instr.scope_id)
        elif instr.opcode == "SCOPE_REGISTER":
            variable = instr.operand
            scope = instr.scope_id
            registrations[variable] = scope

            # Validate: scope must be currently active
            if scope not in scope_stack:
                print(f"❌ ERROR: {variable} registered to inactive scope {scope}")
            else:
                print(f"✅ OK: {variable} registered to active scope {scope}")

        elif instr.opcode == "SCOPE_EXIT":
            scope = instr.scope_id
            scope_stack.remove(scope)

            # Check registered temps
            for var, reg_scope in registrations.items():
                if reg_scope == scope:
                    print(f"✅ RELEASE: {var} (scope {scope} exits)")

# Example usage:
validate_scope_registration(ir)
# Output:
# ✅ OK: _temp registered to active scope _scope_1
# ✅ OK: ex registered to active scope _scope_1
# ✅ RELEASE: _temp (scope _scope_1 exits)
# ✅ RELEASE: ex (scope _scope_1 exits)
```

---

## 11. Appendices

### Appendix A: Complete IR Examples

#### A.1: Normal Function Call

```
OperationDfn: caller._call()->org.ek9.lang::Void
BasicBlock: _entry_1
SCOPE_ENTER _scope_1

REFERENCE data, org.ek9.lang::String
_temp1 = LOAD_LITERAL test, org.ek9.lang::String
_temp2 = CALL (org.ek9.lang::String)org.ek9.lang::String.<init>(_temp1)
RETAIN _temp2
SCOPE_REGISTER _temp2, _scope_1
STORE data, _temp2

_temp3 = LOAD data
_temp4 = CALL callee(_temp3)

SCOPE_EXIT _scope_1
RETURN

OperationDfn: callee._call(org.ek9.lang::String)->org.ek9.lang::String
BasicBlock: _entry_1
SCOPE_ENTER _scope_1

REFERENCE input, org.ek9.lang::String
REFERENCE rtn, org.ek9.lang::String
_temp1 = LOAD input
STORE rtn, _temp1
RETAIN rtn

SCOPE_EXIT _scope_1
RETURN rtn
```

#### A.2: Exception Throw and Catch

```
OperationDfn: outer._call()->org.ek9.lang::Void
BasicBlock: _entry_1
SCOPE_ENTER _scope_1

SCOPE_ENTER _scope_2

CONTROL_FLOW_CHAIN
  chain_type: "TRY_CATCH_FINALLY"
  try_block_details:
    try_scope_id: _scope_3
    try_body_evaluation:
      SCOPE_ENTER _scope_3
      CALL inner()
      SCOPE_EXIT _scope_3

  condition_chain:
    case_scope_id: _scope_4
    case_type: "EXCEPTION_HANDLER"
    body_evaluation:
      SCOPE_ENTER _scope_4
      REFERENCE ex, org.ek9.lang::Exception
      SCOPE_REGISTER ex, _scope_4
      _temp1 = CALL handleError(ex)
      SCOPE_EXIT _scope_4
    exception_type: "org.ek9.lang::Exception"
    exception_variable: ex

  scope_id: _scope_2

SCOPE_EXIT _scope_2
SCOPE_EXIT _scope_1
RETURN

OperationDfn: inner._call()->org.ek9.lang::Void
BasicBlock: _entry_1
SCOPE_ENTER _scope_1

REFERENCE error, org.ek9.lang::Exception
_temp1 = LOAD_LITERAL fail, org.ek9.lang::String
_temp2 = CALL (org.ek9.lang::Exception)org.ek9.lang::Exception.<init>(_temp1)
RETAIN _temp2
SCOPE_REGISTER _temp2, _scope_1
STORE error, _temp2
RETAIN error
SCOPE_REGISTER error, _scope_1
RETAIN error
THROW error

SCOPE_EXIT _scope_1
RETURN
```

#### A.3: Try-With-Resources

```
OperationDfn: test._call()->org.ek9.lang::String
BasicBlock: _entry_1
SCOPE_ENTER _scope_1

REFERENCE rtn, org.ek9.lang::String
_temp1 = CALL (org.ek9.lang::String)org.ek9.lang::String.<init>()
RETAIN _temp1
SCOPE_REGISTER _temp1, _scope_1
STORE rtn, _temp1
RETAIN rtn

SCOPE_ENTER _scope_2

SCOPE_ENTER _scope_3
REFERENCE resource, SimpleResource
_temp3 = LOAD_LITERAL test, org.ek9.lang::String
_temp2 = CALL (SimpleResource)SimpleResource.<init>(_temp3)
RETAIN _temp2
SCOPE_REGISTER _temp2, _scope_3
STORE resource, _temp2

SCOPE_ENTER _scope_4

CONTROL_FLOW_CHAIN
  chain_type: "TRY_CATCH_FINALLY"
  try_block_details:
    try_scope_id: _scope_5
    try_body_evaluation:
      SCOPE_ENTER _scope_5
      _temp4 = LOAD resource
      _temp5 = CALL performOperation(_temp4)
      SCOPE_EXIT _scope_5

  condition_chain:
    case_scope_id: _scope_6
    case_type: "EXCEPTION_HANDLER"
    body_evaluation:
      SCOPE_ENTER _scope_6
      REFERENCE ex, org.ek9.lang::Exception
      SCOPE_REGISTER ex, _scope_6
      _temp6 = LOAD_LITERAL ERROR, org.ek9.lang::String
      RETAIN _temp6
      SCOPE_REGISTER _temp6, _scope_6
      RELEASE rtn
      STORE rtn, _temp6
      RETAIN rtn
      SCOPE_EXIT _scope_6
    exception_type: "org.ek9.lang::Exception"
    exception_variable: ex

  finally_block_evaluation:
    SCOPE_ENTER _scope_7
    CALL (SimpleResource)resource._close()
    SCOPE_EXIT _scope_7

  scope_id: _scope_4

SCOPE_EXIT _scope_4
SCOPE_EXIT _scope_3
SCOPE_EXIT _scope_2
SCOPE_EXIT _scope_1
RETURN rtn
```

### Appendix B: Stack Frame Diagrams

#### B.1: Downward Flow Stack Diagram

```
Time: T0 (before call)
┌──────────────────────┐
│ caller() stack frame │
│   data: "test"       │  ref_count(data) = 1
│   ref_count = 1      │
└──────────────────────┘

Time: T1 (during call)
┌──────────────────────┐
│ caller() stack frame │
│   data: "test"       │  ref_count(data) = 1
│   ref_count = 1      │
└──────────┬───────────┘
           │ Pass reference (borrowed)
           ▼
┌──────────────────────┐
│ callee() stack frame │
│   input: "test"      │  ref_count(data) still = 1
│   (borrowed)         │  (no RETAIN)
└──────────────────────┘

Time: T2 (after return)
┌──────────────────────┐
│ caller() stack frame │
│   data: "test"       │  ref_count(data) = 1
│   ref_count = 1      │
└──────────────────────┘
(callee frame destroyed)

Time: T3 (caller scope exit)
(all frames destroyed)
data deallocated, ref_count = 0
```

#### B.2: Upward Flow Stack Diagram

```
Time: T0 (exception created)
┌──────────────────────┐
│ outer() stack frame  │
│   try block active   │
└──────────┬───────────┘
           │ Call inner()
           ▼
┌──────────────────────┐
│ inner() stack frame  │
│   ex: Exception      │  ref_count(ex) = 3
│   ref_count = 3      │  (extra RETAIN before throw)
└──────────────────────┘

Time: T1 (exception thrown, unwinding starts)
┌──────────────────────┐
│ outer() stack frame  │
│   try block active   │
└──────────▲───────────┘
           │ Unwinding (exception propagates UP)
           │
┌──────────┴───────────┐
│ inner() stack frame  │  EXITING (during unwind)
│   ex: Exception      │  SCOPE_EXIT releases 2 refs
│   ref_count = 3      │  ref_count: 3 → 1 ✅
└──────────────────────┘  (exception SURVIVES!)

Time: T2 (exception caught)
┌──────────────────────┐
│ outer() stack frame  │
│   catch handler      │
│   ex: Exception      │  ref_count(ex) = 1
│   ref_count = 1      │  (transferred ownership)
└──────────────────────┘
(inner frame destroyed)

Time: T3 (catch scope exit)
(all frames destroyed)
ex deallocated, ref_count = 0
```

### Appendix C: Glossary

**ARC (Automatic Reference Counting)**: Memory management technique where each object maintains a reference count; object is deallocated when count reaches zero.

**Bidirectional Flow**: EK9's architectural principle distinguishing downward object flow (normal execution) from upward object flow (exception propagation).

**Downward Flow**: Normal execution pattern where objects created in outer scopes are passed to inner scopes (caller → callee, parent → child).

**Upward Flow**: Exception propagation pattern where exceptions created in inner scopes propagate to outer scopes (callee → caller, child → parent) during stack unwinding.

**RETAIN**: IR operation that increments an object's reference count, claiming ownership.

**RELEASE**: IR operation that decrements an object's reference count, releasing ownership. Implicitly generated by SCOPE_EXIT.

**SCOPE_REGISTER**: IR operation that marks a variable for automatic cleanup when a scope exits. This is the **consumer pattern** for ARC.

**SCOPE_EXIT**: IR operation that marks the end of a scope. Implicitly generates RELEASE for all SCOPE_REGISTER'd temps in reverse order.

**Ownership Transfer**: Pattern where ownership of an object is permanently transferred from one scope to another (e.g., throw → catch).

**Borrowed Access**: Pattern where an object is temporarily used without claiming ownership (e.g., function parameter).

**Stack Unwinding**: Process of exiting stack frames during exception propagation, destroying local variables and calling cleanup code.

**Use-After-Free**: Memory safety bug where code accesses deallocated memory, resulting in crashes or data corruption.

**Extra RETAIN (before THROW)**: Critical ARC pattern where exception receives an additional RETAIN before THROW to ensure it survives creator scope exit during unwinding.

**Transferred Ownership**: Exception catch pattern where the catcher receives ownership without an additional RETAIN (ref_count = 1).

---

## Summary

EK9's bidirectional flow architecture provides a **principled foundation** for memory management:

1. **Downward flow** (normal execution): Objects flow from creator to consumer, creator outlives consumer, standard RETAIN + SCOPE_REGISTER pattern
2. **Upward flow** (exception propagation): Exceptions flow from creator to catcher, **creator exits before catcher**, requires **extra RETAIN** before THROW to survive unwinding
3. **Dual flow** (try-with-resources): Resources use downward flow, exceptions use upward flow, both patterns coexist safely

**Key Principle**: The **direction of object flow** fundamentally determines the ARC pattern required for memory safety.

This architecture ensures:
- ✅ No use-after-free bugs (exceptions survive creator scope exit)
- ✅ No memory leaks (catch handler doesn't double-retain)
- ✅ Predictable performance (deterministic deallocation)
- ✅ Exception safety (SCOPE_EXIT cleanup always runs)
- ✅ Backend flexibility (JVM and LLVM implementations)

**EK9's innovation**: Making bidirectional flow **explicit in the IR**, enabling verification, optimization, and correct code generation across multiple backend targets.

---

**End of Document**
