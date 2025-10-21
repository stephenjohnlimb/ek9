# EK9 ARC Memory Management

**Automatic Reference Counting (ARC) in EK9 IR Generation**

This document provides comprehensive documentation of EK9's ARC-based memory management model, used in the Intermediate Representation (IR) to support both JVM and LLVM native backends.

## Table of Contents

1. [Overview](#overview)
2. [Core Principles](#core-principles)
3. [IR Instructions](#ir-instructions)
4. [Variable Ownership Model](#variable-ownership-model)
5. [Declaration Patterns](#declaration-patterns)
6. [Reassignment Patterns](#reassignment-patterns)
7. [Temporary Variables](#temporary-variables)
8. [Special Cases](#special-cases)
9. [Complete Examples](#complete-examples)
10. [Comparison with Swift](#comparison-with-swift)
11. [Backend Implementation](#backend-implementation)
12. [Future Optimizations](#future-optimizations)

---

## Overview

EK9 uses Automatic Reference Counting (ARC) for memory management in its Intermediate Representation. This provides:

- **Predictable memory management** without garbage collection pauses
- **Explicit lifetime control** suitable for both JVM and native (LLVM) backends
- **Zero-cost abstraction** on JVM (hints for future optimization)
- **Direct C++ ARC implementation** on LLVM backend (Swift-inspired)

### Key Design Decision

**Variables are registered to scopes at declaration time ONLY, not at reassignment.**

This ensures:
- Each variable has exactly ONE owning scope
- No double-release bugs
- No use-after-free errors
- Clear ownership semantics

---

## 🚨 CRITICAL: Object Fields vs Local Variables

### The Fundamental Ownership Distinction

**This is the most important concept in EK9's ARC memory management:**

```
┌─────────────────────────────────────────────────────────────────┐
│                    OWNERSHIP MODEL                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  LOCAL VARIABLES:                                                │
│    Owned by: SCOPE that declares them                           │
│    Lifetime: Declaration → Scope exit                           │
│    IR Pattern: RETAIN + SCOPE_REGISTER to declaring scope       │
│    Released by: SCOPE_EXIT of declaring scope                   │
│                                                                  │
│  OBJECT FIELDS:                                                  │
│    Owned by: THE OBJECT ITSELF                                  │
│    Lifetime: Object construction → Object destruction           │
│    IR Pattern: RETAIN only (NO SCOPE_REGISTER)                  │
│    Released by: Object destructor                               │
│                                                                  │
│  🚨 CRITICAL: Fields initialized in i_init() must NOT be        │
│     SCOPE_REGISTER'd to i_init scope. The object outlives       │
│     i_init() and needs its fields to remain alive!              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Why This Distinction Matters

**Field initialization happens in the special `i_init()` instance initializer method.** This method executes during object construction but **exits before the object is used**. If fields were registered to the `i_init` scope, they would be prematurely released when `i_init` exits, causing use-after-free bugs.

### Wrong vs Correct Field Initialization

**❌ WRONG IR (Field SCOPE_REGISTER'd to i_init):**
```
OperationDfn: Example.i_init()->Void
BasicBlock: _entry_1
REFERENCE this.aField, String
_temp1 = LOAD_LITERAL "Steve", String
RETAIN _temp1
SCOPE_REGISTER _temp1, i_init
STORE this.aField, _temp1
RETAIN this.aField
SCOPE_REGISTER this.aField, i_init  ← BUG! Field registered to i_init
RETURN

// When i_init exits:
SCOPE_EXIT i_init
  → Release _temp1
  → Release this.aField  ← PREMATURE RELEASE!

// Later, when object uses field:
_temp2 = LOAD this.aField  ← USE-AFTER-FREE! 💥
```

**✅ CORRECT IR (Field NOT SCOPE_REGISTER'd):**
```
OperationDfn: Example.i_init()->Void
BasicBlock: _entry_1
REFERENCE this.aField, String
_temp1 = LOAD_LITERAL "Steve", String
RETAIN _temp1
SCOPE_REGISTER _temp1, i_init
STORE this.aField, _temp1
RETAIN this.aField              ← Field retained
// NO SCOPE_REGISTER            ← Owned by object, not scope
RETURN

// When i_init exits:
SCOPE_EXIT i_init
  → Release _temp1 only
  → this.aField remains alive ✅

// Later, when object uses field:
_temp2 = LOAD this.aField  ← SAFE! Field still alive ✅

// When object is destroyed:
Object destructor:
  → Release this.aField  ← Released by object, not scope
```

### Implementation: isPropertyField() Check

The compiler distinguishes fields from local variables using the `isPropertyField()` method:

**File**: `compiler-main/src/main/java/org/ek9lang/compiler/phase7/generator/AssignExpressionToSymbol.java`

```java
// ARC FIX: Distinguish fields from local variables
if (!release) {  // Declaration path
  if (lhsSymbol.isPropertyField()) {
    // Field: RETAIN only, owned by object
    instructions.add(MemoryInstr.retain(lhsVariableName, rhsExprDebugInfo));
  } else {
    // Local variable: RETAIN + SCOPE_REGISTER, owned by scope
    final var lhsVariableDetails = new VariableDetails(lhsVariableName, rhsExprDebugInfo);
    variableMemoryManagement.apply(() -> instructions, lhsVariableDetails);
  }
} else {  // Reassignment path
  // Just RETAIN, variable already registered at declaration
  instructions.add(MemoryInstr.retain(lhsVariableName, rhsExprDebugInfo));
}
```

### The i_init() Instance Initializer

**What is i_init()?**
- Synthetic method generated by the compiler for field initialization
- Executes during object construction, before constructor body
- Initializes all fields declared with default values
- **Exits before object is returned to caller**

**Construction Sequence:**
```
1. Object allocated (malloc/new)
2. c_init() - static class initialization (if needed)
3. i_init() - instance field initialization ← Fields initialized here
4. Constructor body - user code
5. Object returned to caller
```

**Key Insight**: Fields must outlive `i_init()`, so they cannot be registered to the `i_init` scope.

---

## Core Principles

### 1. Object Reference Counting

Every heap-allocated object maintains a reference count (refcount):
- **Created with refcount=0** (not yet owned)
- **RETAIN increments** refcount (new owner)
- **RELEASE decrements** refcount (owner relinquishes)
- **Freed when refcount reaches 0** (no owners left)

### 2. Variable Ownership

Variables "own" the objects they point to:
- **Variable declaration**: RETAIN object, register variable to declaring scope
- **Variable reassignment**: RELEASE old object, RETAIN new object (NO re-registration)
- **Scope exit**: RELEASE all variables registered to that scope

### 3. SCOPE_REGISTER Semantics

**SCOPE_REGISTER tracks variable NAMES, not object addresses.**

```
SCOPE_REGISTER counter, _scope_1
```

Means: "_scope_1 owns the variable 'counter' and will release whatever it points to at scope exit"

**Critical**: When a scope exits, it releases the object that the variable **currently points to**, not what it pointed to at registration time.

### 4. Single Registration Rule

**A variable is registered to EXACTLY ONE scope** - its declaring scope.

```
// Declaration in _scope_1
counter <- 0
// Generates: RETAIN, SCOPE_REGISTER counter, _scope_1

// Reassignment in _scope_4 (inside loop)
counter: counter + 1
// Generates: RELEASE, RETAIN
// NO SCOPE_REGISTER - already registered to _scope_1
```

---

## IR Instructions

### RETAIN

```
RETAIN variableName
```

Increments the reference count of the object that `variableName` points to.

**When emitted:**
- After variable receives a value (declaration or reassignment)
- For temporary expression results
- For property fields and return parameters

### RELEASE

```
RELEASE variableName
```

Decrements the reference count of the object that `variableName` points to.

**When emitted:**
- Before variable reassignment (release old value)
- At scope exit (for all registered variables)
- Manually when ownership transfer occurs

### SCOPE_REGISTER

```
SCOPE_REGISTER variableName, scopeId
```

Registers a variable name to a scope for automatic cleanup.

**When emitted:**
- At variable declaration (first assignment)
- For temporary variables (in their declaring scope)
- **NEVER at reassignment**

**Semantics:** At `SCOPE_EXIT scopeId`, the scope will emit `RELEASE variableName` for each registered variable.

### STORE

```
STORE variableName, sourceVariable
```

Assigns the value from `sourceVariable` to `variableName`.

**Memory semantics:**
- Does NOT affect reference counts
- Just updates the pointer
- Always surrounded by RELEASE/RETAIN for proper ARC

---

## Variable Ownership Model

### Ownership Lifecycle

```
1. Variable Declaration
   ┌─────────────────────────────────┐
   │ REFERENCE counter, Integer      │
   │ _temp1 = LOAD_LITERAL 0         │
   │ RETAIN _temp1                   │
   │ SCOPE_REGISTER _temp1, _scope_1 │ ← temp owned by _scope_1
   │ STORE counter, _temp1           │
   │ RETAIN counter                  │
   │ SCOPE_REGISTER counter, _scope_1│ ← counter owned by _scope_1
   └─────────────────────────────────┘

2. Variable Reassignment (in inner scope)
   ┌─────────────────────────────────┐
   │ _temp6 = CALL _add(...)         │
   │ RETAIN _temp6                   │
   │ SCOPE_REGISTER _temp6, _scope_4 │ ← temp owned by _scope_4
   │ RELEASE counter                 │ ← Release old value
   │ STORE counter, _temp6           │
   │ RETAIN counter                  │ ← Retain new value
   │ // NO SCOPE_REGISTER            │ ← Still owned by _scope_1
   └─────────────────────────────────┘

3. Scope Exit
   ┌─────────────────────────────────┐
   │ SCOPE_EXIT _scope_4             │
   │   → Release _temp6              │ ← _scope_4 cleanup
   │   → Does NOT release counter    │ ← counter owned by _scope_1
   │                                 │
   │ SCOPE_EXIT _scope_1             │
   │   → Release _temp1              │ ← _scope_1 cleanup
   │   → Release counter             │ ← Releases CURRENT value
   └─────────────────────────────────┘
```

### Key Insight: Scope Exit Releases CURRENT Value

When `_scope_1` exits, it doesn't remember which object `counter` pointed to at registration time. It releases **whatever counter currently points to**.

This is why reassignment works correctly:
- counter points to Integer(0) at registration
- counter reassigned to Integer(1), Integer(2), ..., Integer(10)
- _scope_1 exit releases Integer(10) (current value) ✅

---

## Declaration Patterns

### Local Variable Declaration with Initialization

**EK9 Source:**
```ek9
counter <- 0
```

**Generated IR:**
```
REFERENCE counter, org.ek9.lang::Integer
_temp1 = LOAD_LITERAL 0, org.ek9.lang::Integer
RETAIN _temp1
SCOPE_REGISTER _temp1, _scope_1
STORE counter, _temp1
RETAIN counter
SCOPE_REGISTER counter, _scope_1
```

**Memory trace:**
```
Integer(0) created: refcount=0
RETAIN _temp1: refcount=1
SCOPE_REGISTER _temp1 to _scope_1
STORE counter from _temp1
RETAIN counter: refcount=2
SCOPE_REGISTER counter to _scope_1

_scope_1.registered = {_temp1, counter}
Integer(0) refcount=2
```

### Local Variable Declaration Without Initialization

**EK9 Source:**
```ek9
var result
```

**Generated IR:**
```
REFERENCE result, org.ek9.lang::String
```

**Memory trace:**
```
No object created yet
Variable declared but uninitialized
No RETAIN, no SCOPE_REGISTER
Will be registered when first assigned
```

### Return Parameter Declaration

**EK9 Source:**
```ek9
someFunction()
  <- result as String
  result <- String()
```

**Generated IR:**
```
REFERENCE result, org.ek9.lang::String
_temp1 = CALL String.<init>()
RETAIN _temp1
SCOPE_REGISTER _temp1, _call
STORE result, _temp1
RETAIN result
// NO SCOPE_REGISTER for result (ownership transfers to caller)
```

**Memory trace:**
```
String() created: refcount=0
RETAIN _temp1: refcount=1
SCOPE_REGISTER _temp1 to _call
STORE result from _temp1
RETAIN result: refcount=2

_call.registered = {_temp1}  // result NOT registered
```

---

## Reassignment Patterns

### Simple Reassignment

**EK9 Source:**
```ek9
counter <- 0
counter: 5
```

**Generated IR:**
```
// Declaration
SCOPE_ENTER _scope_1
REFERENCE counter, Integer
_temp1 = LOAD_LITERAL 0
RETAIN _temp1
SCOPE_REGISTER _temp1, _scope_1
STORE counter, _temp1
RETAIN counter
SCOPE_REGISTER counter, _scope_1

// Reassignment
_temp2 = LOAD_LITERAL 5
RETAIN _temp2
SCOPE_REGISTER _temp2, _scope_1
RELEASE counter           // Release Integer(0)
STORE counter, _temp2
RETAIN counter            // Retain Integer(5)
// NO SCOPE_REGISTER      // Already registered to _scope_1

SCOPE_EXIT _scope_1
```

**Memory trace:**
```
Integer(0): refcount=2 (_temp1 + counter)
Integer(5): refcount=1 (_temp2)

After reassignment:
  RELEASE counter → Integer(0) refcount=1
  STORE counter, _temp2
  RETAIN counter → Integer(5) refcount=2

Scope exit:
  Release _temp1 → Integer(0) refcount=0, freed
  Release _temp2 → Integer(5) refcount=1
  Release counter → Integer(5) refcount=0, freed
```

### Reassignment in Nested Scope

**EK9 Source:**
```ek9
x <- 0

if condition
  x: 1
```

**Generated IR:**
```
// Declaration in _scope_1
SCOPE_ENTER _scope_1
REFERENCE x, Integer
_temp1 = LOAD_LITERAL 0
RETAIN _temp1
SCOPE_REGISTER _temp1, _scope_1
STORE x, _temp1
RETAIN x
SCOPE_REGISTER x, _scope_1

// Reassignment in _scope_2 (if body)
SCOPE_ENTER _scope_2
_temp2 = LOAD_LITERAL 1
RETAIN _temp2
SCOPE_REGISTER _temp2, _scope_2
RELEASE x             // Release Integer(0)
STORE x, _temp2
RETAIN x              // Retain Integer(1)
// NO SCOPE_REGISTER  // x still owned by _scope_1

SCOPE_EXIT _scope_2   // Releases _temp2 only
SCOPE_EXIT _scope_1   // Releases _temp1 and x
```

**Memory trace:**
```
Before if:
  Integer(0) refcount=2 (_temp1 + x)

In if body:
  Integer(1) created: refcount=0
  RETAIN _temp2 → refcount=1
  SCOPE_REGISTER _temp2 to _scope_2
  RELEASE x → Integer(0) refcount=1
  STORE x, _temp2
  RETAIN x → Integer(1) refcount=2

After SCOPE_EXIT _scope_2:
  Release _temp2 → Integer(1) refcount=1
  x still points to Integer(1) ✅

After SCOPE_EXIT _scope_1:
  Release _temp1 → Integer(0) refcount=0, freed
  Release x → Integer(1) refcount=0, freed
```

### Reassignment in Loop

**EK9 Source:**
```ek9
counter <- 0
while counter < 10
  counter: counter + 1
```

**Generated IR:**
```
// Declaration
SCOPE_ENTER _scope_1
REFERENCE counter, Integer
_temp1 = LOAD_LITERAL 0
RETAIN _temp1
SCOPE_REGISTER _temp1, _scope_1
STORE counter, _temp1
RETAIN counter
SCOPE_REGISTER counter, _scope_1

// Loop condition scope
SCOPE_ENTER _scope_2
SCOPE_ENTER _scope_3
CONTROL_FLOW_CHAIN
[
  // Loop body scope
  SCOPE_ENTER _scope_4
  _temp7 = LOAD counter
  RETAIN _temp7
  SCOPE_REGISTER _temp7, _scope_4
  _temp8 = LOAD_LITERAL 1
  RETAIN _temp8
  SCOPE_REGISTER _temp8, _scope_4
  _temp6 = CALL _temp7._add(_temp8)
  RETAIN _temp6
  SCOPE_REGISTER _temp6, _scope_4

  RELEASE counter         // Release old value (Integer(N))
  STORE counter, _temp6
  RETAIN counter          // Retain new value (Integer(N+1))
  // NO SCOPE_REGISTER    // counter owned by _scope_1

  SCOPE_EXIT _scope_4     // Releases temps, NOT counter
]
SCOPE_EXIT _scope_3
SCOPE_EXIT _scope_2
SCOPE_EXIT _scope_1       // Releases counter's CURRENT value
```

**Memory trace (iteration 1):**
```
Before loop:
  counter → Integer(0), refcount=2

Iteration 1 body:
  _temp7 = LOAD counter → points to Integer(0), refcount=3
  _temp8 = LOAD_LITERAL 1 → Integer(1_literal), refcount=1
  _temp6 = _add(...) → creates Integer(1_result), refcount=1

  RELEASE counter → Integer(0) refcount=2
  STORE counter, _temp6 → counter now → Integer(1_result)
  RETAIN counter → Integer(1_result) refcount=2

Iteration 1 scope exit:
  Release _temp7 → Integer(0) refcount=1
  Release _temp8 → Integer(1_literal) refcount=0, freed
  Release _temp6 → Integer(1_result) refcount=1

Counter survives with refcount=1 ✅

After 10 iterations:
  counter → Integer(10), refcount=1

SCOPE_EXIT _scope_1:
  Release _temp1 → Integer(0) refcount=0, freed
  Release counter → Integer(10) refcount=0, freed ✅
```

---

## Temporary Variables

Temporary variables (like `_temp1`, `_temp2`) follow the same rules as declared variables:
- Created in expression evaluation scope
- RETAIN + SCOPE_REGISTER in that scope
- Released when that scope exits

### Example: Expression Evaluation

**EK9 Source:**
```ek9
result: a + b
```

**Generated IR:**
```
SCOPE_ENTER _scope_1
// ... a and b declared earlier in _scope_1 ...

_temp1 = LOAD a
RETAIN _temp1
SCOPE_REGISTER _temp1, _scope_1

_temp2 = LOAD b
RETAIN _temp2
SCOPE_REGISTER _temp2, _scope_1

_temp3 = CALL _temp1._add(_temp2)
RETAIN _temp3
SCOPE_REGISTER _temp3, _scope_1

RELEASE result
STORE result, _temp3
RETAIN result
// NO SCOPE_REGISTER (result already registered at declaration)

SCOPE_EXIT _scope_1
// Releases _temp1, _temp2, _temp3, result
```

### Temporary Variable Lifetime

Temporaries are short-lived:
- Created during expression evaluation
- Registered to the scope where the expression executes
- Released when that scope exits
- This is correct: temps are local to the expression

**Future optimization:** Last-use analysis could release temps earlier, but current approach is correct and simple.

---

## Special Cases

### Property Fields

**See the comprehensive section [Object Fields vs Local Variables](#-critical-object-fields-vs-local-variables) for complete documentation.**

Property fields are object members, not local variables. They are owned by the object and must NOT be SCOPE_REGISTER'd.

**EK9 Source:**
```ek9
class Person
  name <- "Alice"
```

**Generated IR:**
```
// In i_init() instance initializer
OperationDfn: Person.i_init()->Void
BasicBlock: _entry_1
REFERENCE this.name, String
_temp1 = LOAD_LITERAL "Alice"
RETAIN _temp1
SCOPE_REGISTER _temp1, i_init      ← Temporary registered to i_init
STORE this.name, _temp1
RETAIN this.name
// NO SCOPE_REGISTER for this.name ← Field NOT registered (owned by object)
RETURN
```

**Memory management:**
- Fields are RETAINED when assigned
- Temporaries used to initialize fields ARE registered to `i_init` scope
- Fields themselves are NOT registered to any scope (owned by object)
- Object destructor releases all fields when object is destroyed

**Critical Distinction:**
- `_temp1` is a temporary variable → registered to `i_init` → released when `i_init` exits
- `this.name` is a field → NOT registered → released when object is destroyed

### Return Parameters

Return parameters transfer ownership to caller.

**EK9 Source:**
```ek9
getValue()
  <- rtn as String
  rtn: "value"
```

**Generated IR:**
```
REFERENCE rtn, String
_temp1 = LOAD_LITERAL "value"
RETAIN _temp1
SCOPE_REGISTER _temp1, _call
RELEASE rtn
STORE rtn, _temp1
RETAIN rtn
// NO SCOPE_REGISTER for rtn
RETURN rtn
```

**Memory management:**
- Return parameter NOT registered to scope
- Caller receives object with refcount=1
- Caller responsible for eventual release

### Method-Based Assignments (+=, -=, etc.)

Assignment operators like `+=` mutate in place, not reassign.

**EK9 Source:**
```ek9
counter += 1
```

**Generated IR:**
```
_temp1 = LOAD counter
RETAIN _temp1
SCOPE_REGISTER _temp1, _scope_1

_temp2 = LOAD_LITERAL 1
RETAIN _temp2
SCOPE_REGISTER _temp2, _scope_1

CALL _temp1._addAss(_temp2)  // Returns Void, mutates _temp1 in place
// NO RELEASE, NO STORE, NO RETAIN
// counter still points to same object (now mutated)
```

**Memory management:**
- Object mutates in place
- Variable still points to same object
- No refcount changes needed
- Object refcount unchanged

---

## Complete Examples

### Example 1: Variable in If/Else

**EK9 Source:**
```ek9
simpleIfElse(value as Integer)
  <- result as String

  x <- 0

  if value > 10
    x: 1
  else
    x: 2

  result: x.string()
```

**Generated IR with Memory Trace:**
```
OperationDfn: simpleIfElse._call(Integer)->String
BasicBlock: _entry_1
REFERENCE value, Integer
REFERENCE result, String

// x declaration in _scope_1
SCOPE_ENTER _scope_1
REFERENCE x, Integer
_temp1 = LOAD_LITERAL 0               // Integer(0) refcount=0
RETAIN _temp1                         // refcount=1
SCOPE_REGISTER _temp1, _scope_1       // _scope_1.registered += _temp1
STORE x, _temp1
RETAIN x                              // refcount=2
SCOPE_REGISTER x, _scope_1            // _scope_1.registered += x

// If body (_scope_2)
SCOPE_ENTER _scope_2
_temp2 = LOAD_LITERAL 1               // Integer(1) refcount=0
RETAIN _temp2                         // refcount=1
SCOPE_REGISTER _temp2, _scope_2       // _scope_2.registered += _temp2
RELEASE x                             // Integer(0) refcount=1
STORE x, _temp2
RETAIN x                              // Integer(1) refcount=2
// NO SCOPE_REGISTER                  // x still owned by _scope_1
SCOPE_EXIT _scope_2                   // Release _temp2: Integer(1) refcount=1

// Else body (_scope_3)
SCOPE_ENTER _scope_3
_temp3 = LOAD_LITERAL 2               // Integer(2) refcount=0
RETAIN _temp3                         // refcount=1
SCOPE_REGISTER _temp3, _scope_3       // _scope_3.registered += _temp3
RELEASE x                             // Integer(1) refcount=0, freed
STORE x, _temp3
RETAIN x                              // Integer(2) refcount=2
// NO SCOPE_REGISTER                  // x still owned by _scope_1
SCOPE_EXIT _scope_3                   // Release _temp3: Integer(2) refcount=1

// Convert x to string
_temp4 = LOAD x                       // Integer(2) refcount=3
RETAIN _temp4
SCOPE_REGISTER _temp4, _scope_1
_temp5 = CALL _temp4._string()        // String("2") refcount=0
RETAIN _temp5                         // refcount=1
SCOPE_REGISTER _temp5, _scope_1
RELEASE result
STORE result, _temp5
RETAIN result                         // refcount=2
// NO SCOPE_REGISTER                  // result is return param

SCOPE_EXIT _scope_1
// Release _temp1: Integer(0) refcount=0, freed
// Release x: Integer(2) refcount=0, freed
// Release _temp4: (already freed with x)
// Release _temp5: String("2") refcount=1

RETURN result                         // Caller gets String("2") refcount=1
```

### Example 2: While Loop (from test)

**EK9 Source:**
```ek9
simpleWhileLoop()
  counter <- 0

  while counter < 10
    counter: counter + 1
```

**Generated IR with Memory Trace:**
```
OperationDfn: simpleWhileLoop._call()->Void
BasicBlock: _entry_1

// Variable declaration scope
SCOPE_ENTER _scope_1
REFERENCE counter, Integer
_temp1 = LOAD_LITERAL 0               // Integer(0) refcount=0
RETAIN _temp1                         // refcount=1
SCOPE_REGISTER _temp1, _scope_1
STORE counter, _temp1
RETAIN counter                        // refcount=2
SCOPE_REGISTER counter, _scope_1      // counter owned by _scope_1

// While loop scopes
SCOPE_ENTER _scope_2                  // Outer wrapper (for guards)
SCOPE_ENTER _scope_3                  // Condition scope
CONTROL_FLOW_CHAIN
[
  chain_type: "WHILE_LOOP"
  condition_chain:
  [
    [
      case_scope_id: _scope_4
      condition_evaluation:
      [
        _temp3 = LOAD counter         // Current value, refcount+=1
        RETAIN _temp3
        SCOPE_REGISTER _temp3, _scope_3
        _temp4 = LOAD_LITERAL 10      // Integer(10) refcount=1
        RETAIN _temp4
        SCOPE_REGISTER _temp4, _scope_3
        _temp2 = CALL _temp3._lt(_temp4)  // Boolean
        _temp5 = CALL _temp2._true()
      ]
      body_evaluation:
      [
        SCOPE_ENTER _scope_4

        // Load current counter value
        _temp7 = LOAD counter         // Integer(N) refcount+=1
        RETAIN _temp7
        SCOPE_REGISTER _temp7, _scope_4

        // Load literal 1
        _temp8 = LOAD_LITERAL 1       // Integer(1) refcount=1
        RETAIN _temp8
        SCOPE_REGISTER _temp8, _scope_4

        // Add operation
        _temp6 = CALL _temp7._add(_temp8)  // Integer(N+1) refcount=0
        RETAIN _temp6                 // refcount=1
        SCOPE_REGISTER _temp6, _scope_4

        // Reassignment (NOT re-registration)
        RELEASE counter               // Integer(N) refcount-=1
        STORE counter, _temp6         // counter → Integer(N+1)
        RETAIN counter                // Integer(N+1) refcount=2
        SCOPE_REGISTER counter, _scope_4  // ← WRONG! This is the bug we're fixing

        SCOPE_EXIT _scope_4
        // Release _temp7: Integer(N) refcount-=1
        // Release _temp8: Integer(1) freed
        // Release _temp6: Integer(N+1) refcount=1
        // Release counter: Integer(N+1) refcount=0, FREED ← BUG!
      ]
    ]
  ]
]
SCOPE_EXIT _scope_3
SCOPE_EXIT _scope_2
SCOPE_EXIT _scope_1
// Release _temp1: Integer(0)
// Release counter: ??? (already freed by _scope_4!)
RETURN
```

**After our fix (removing SCOPE_REGISTER in reassignment):**
```
        // Reassignment (corrected)
        RELEASE counter               // Integer(N) refcount-=1
        STORE counter, _temp6         // counter → Integer(N+1)
        RETAIN counter                // Integer(N+1) refcount=2
        // NO SCOPE_REGISTER          // ← FIXED! counter owned by _scope_1

        SCOPE_EXIT _scope_4
        // Release _temp7: Integer(N) refcount-=1
        // Release _temp8: Integer(1) freed
        // Release _temp6: Integer(N+1) refcount=1
        // Does NOT release counter ✅

After 10 iterations:
  counter → Integer(10), refcount=1

SCOPE_EXIT _scope_1:
  Release _temp1 → Integer(0) freed
  Release counter → Integer(10) freed ✅ CORRECT!
```

---

## Comparison with Swift

### Similarities

1. **ARC-based**: Both use automatic reference counting
2. **Retain/Release pairs**: Both emit paired operations
3. **Ownership semantics**: Both have clear ownership rules

### Differences

| Aspect | EK9 (Current) | Swift (SIL) |
|--------|---------------|-------------|
| **Variable lifetime** | Scope-based (declaration to scope exit) | Use-based (declaration to last use) |
| **Scope registration** | Explicit SCOPE_REGISTER instruction | No equivalent (implicit) |
| **Optimization** | Conservative (longer lifetimes) | Aggressive (minimal lifetimes) |
| **Release placement** | At scope exit only | At last use or scope exit |
| **Debug experience** | Variables visible until scope exit | May disappear before scope exit (optimized builds) |

### Swift's Approach

**Swift SIL uses data flow analysis** to determine last use:

```swift
var counter = 0        // retain(Integer(0))
while counter < 10 {   // use of counter
  counter += 1         // last use of old value, release here
}
// Last use of counter is in loop condition
// Swift inserts release right after last use
// counter may be freed before scope exit
```

**EK9's Approach**

**EK9 uses scope-based lifetime**:

```ek9
counter <- 0           // retain, SCOPE_REGISTER to _scope_1
while counter < 10     // use of counter
  counter: counter + 1 // release old, retain new
// counter still alive (refcount >= 1)
// SCOPE_EXIT _scope_1 releases counter
```

### Trade-offs

**EK9 (Scope-Based):**
- ✅ Simpler IR generation (no data flow analysis)
- ✅ Predictable lifetimes (until scope exit)
- ✅ Better debug experience (variables always visible)
- ❌ Longer lifetimes (more memory usage)
- ❌ Later releases (holds memory longer)

**Swift (Use-Based):**
- ✅ Minimal lifetimes (optimal memory usage)
- ✅ Earlier releases (frees memory sooner)
- ✅ Better performance (less memory pressure)
- ❌ Complex IR generation (requires data flow analysis)
- ❌ Worse debug experience (variables may disappear)

---

## Backend Implementation

### JVM Backend

On the JVM, RETAIN/RELEASE/SCOPE_REGISTER are **hints** for future optimization:

- **Current**: JVM garbage collector handles memory, ARC instructions are no-ops
- **Future**: Could guide escape analysis and stack allocation
- **Benefit**: Documents intended lifetimes for optimization passes

### LLVM Native Backend

On LLVM with C++ runtime, ARC instructions are **direct operations**:

```cpp
// RETAIN counter
counter->retainCount++;

// RELEASE counter
if (--counter->retainCount == 0) {
  delete counter;
}

// SCOPE_REGISTER counter, _scope_1
scope_1.registerVariable(&counter);

// SCOPE_EXIT _scope_1
for (auto* var : scope_1.registeredVariables) {
  if (--(*var)->retainCount == 0) {
    delete *var;
  }
}
```

**Implementation details:**
- Every heap object has `retainCount` field
- Scopes maintain sets of registered variable pointers
- Scope destructors iterate and release all registered variables
- Cycle detection (future) via `gc_color` and `gc_next` fields

---

## Future Optimizations

### 1. Last-Use Analysis (Swift-Style)

**Current:**
```
counter <- 0
print(counter)
// counter still alive until scope exit
```

**Optimized:**
```
counter <- 0
print(counter)        // Last use
RELEASE counter       // Release immediately after last use
// counter freed earlier ✅
```

**Requirements:**
- Data flow analysis pass
- SSA form conversion
- Liveness analysis
- More complex IR generation

**Benefits:**
- Shorter lifetimes (less memory usage)
- Earlier releases (better cache utilization)
- More optimization opportunities

### 2. Copy Elision

**Current:**
```
_temp1 = CALL String.<init>("value")
RETAIN _temp1
SCOPE_REGISTER _temp1, _scope_1
STORE result, _temp1
RETAIN result         // Unnecessary copy
```

**Optimized:**
```
result = CALL String.<init>("value")
RETAIN result
SCOPE_REGISTER result, _scope_1
// No temp, no double retain
```

### 3. Move Semantics

**Current:** Always retain/release, even for last use

**Optimized:** Move ownership instead of copy for last use
```
// Last use of x
MOVE y, x   // Transfer ownership, no retain/release
```

### 4. Escape Analysis

Determine which variables never escape their scope:
- Allocate on stack instead of heap
- No retain/release needed (automatic cleanup)
- Significant performance improvement

---

## Summary

### Key Points

1. **SCOPE_REGISTER is one-time** - At variable declaration only
2. **Reassignments just RELEASE + RETAIN** - Never SCOPE_REGISTER
3. **Scopes own variables** - Release at scope exit
4. **Temporaries are variables too** - Follow same rules
5. **Scope exit releases CURRENT value** - Not registration-time value

### Implementation Checklist

When generating IR for variables:

**Local Variables:**
- [ ] Declaration: RETAIN + SCOPE_REGISTER to declaring scope
- [ ] Reassignment: RELEASE + RETAIN (NO SCOPE_REGISTER)
- [ ] Temporary: RETAIN + SCOPE_REGISTER (in declaring scope)

**Special Cases:**
- [ ] Property field: RETAIN (NO SCOPE_REGISTER) - owned by object
- [ ] Return param: RETAIN (NO SCOPE_REGISTER) - ownership transfers to caller

**Field Initialization (in i_init()):**
- [ ] Temporary used for initialization: RETAIN + SCOPE_REGISTER to i_init
- [ ] Field itself: RETAIN (NO SCOPE_REGISTER) - owned by object

### Common Mistakes

❌ **WRONG**: Registering variable at reassignment
```
RELEASE counter
STORE counter, _temp6
RETAIN counter
SCOPE_REGISTER counter, _scope_4  // ← BUG!
```

✅ **CORRECT**: Just release and retain
```
RELEASE counter
STORE counter, _temp6
RETAIN counter
// counter already registered to declaring scope
```

---

## Document Version

- **Created**: 2025-10-19
- **Last Updated**: 2025-10-19
- **Status**: Active (post-fix documentation)
- **Related**: EK9_IR_AND_CODE_GENERATION.md, LOOP_CONSTRUCTS_COMPREHENSIVE_ARCHITECTURE.md
