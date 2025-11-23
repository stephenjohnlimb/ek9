# EK9 ARC Ownership Transfer Pattern

## Document Purpose

This document defines the **ownership transfer pattern** for EK9's Automatic Reference Counting (ARC) memory management system. Understanding this pattern is critical for correct IR generation when values cross scope boundaries through return statements, exception handling, and other mechanisms.

## Problem Statement

When an object reference crosses a scope boundary (function return, exception catch, callback parameter, etc.), we must ensure:

1. **No memory leaks** - Objects are eventually freed when no longer needed
2. **No premature deallocation** - Objects remain valid while still in use
3. **No double-free** - Objects are not freed multiple times

The naïve approach of treating all variables identically leads to incorrect refcount management at scope boundaries.

## Core Concepts

### ARC Refcount Operations

**RETAIN:** Increment reference count (claim ownership of a reference)
```
RETAIN variable  // refcount++
```

**SCOPE_REGISTER:** Register variable for automatic release on scope exit
```
SCOPE_REGISTER variable, scope_id  // Will RELEASE when scope exits
```

**SCOPE_EXIT:** Release all registered variables in this scope
```
SCOPE_EXIT scope_id  // Triggers RELEASE for all registered variables (refcount--)
```

### The Fundamental Invariant

**Every RETAIN must eventually have a corresponding RELEASE** (either explicit or via SCOPE_EXIT).

However, when references **transfer between scopes**, naive application of this rule causes problems.

## The Ownership Transfer Problem

### Incorrect Pattern (Memory Leak)

```ek9
getMessage() as String
  <- result as String: "Hello"

// INCORRECT IR:
REFERENCE result, String
_temp1 = LOAD_LITERAL "Hello", String
RETAIN _temp1                        // refcount = 1
SCOPE_REGISTER _temp1, _scope_1      // Will release on exit
STORE result, _temp1
RETAIN result                        // refcount = 2
SCOPE_REGISTER result, _scope_1      // Will release on exit ❌ WRONG!
RETURN result                        // Hand reference to caller
SCOPE_EXIT _scope_1                  // Releases both _temp1 and result → refcount = 0

// Caller receives:
msg = CALL getMessage()              // Gets reference... but refcount = 0!
// Object already freed! Dangling pointer! 💥
```

**Problem:** The function releases the return variable before handing it to the caller, leaving the caller with a freed object.

### Incorrect Pattern (Double Release)

If we try to fix this by having the caller SCOPE_REGISTER the received value:

```ek9
// In function:
RETAIN result
SCOPE_REGISTER result                // Function will release
RETURN result

// In caller:
msg = CALL getMessage()              // Receives reference
SCOPE_REGISTER msg                   // Caller will also release
SCOPE_EXIT                           // Releases msg (refcount = 0)
// But function already released it! Double-free! 💥
```

**Problem:** Both producer and consumer try to manage the same reference, leading to double-release.

## The Solution: Ownership Transfer Pattern

### The Pattern

**Ownership transfer** is implemented by:

1. **Producer (creating/returning value):** RETAIN but NO SCOPE_REGISTER
2. **Consumer (receiving value):** NO RETAIN but YES SCOPE_REGISTER

This ensures exactly one scope manages the reference's lifecycle.

### Why This Works

**Producer side:**
```
result <- "Hello"                    // Object created
RETAIN result                        // Producer owns reference (refcount = 1)
// NO SCOPE_REGISTER                 // Producer won't release it
RETURN result                        // Transfer ownership to caller
SCOPE_EXIT                           // Nothing to release for result
```

**Consumer side:**
```
msg = CALL getMessage()              // Receive reference (refcount = 1)
// NO RETAIN                         // Already have ownership
SCOPE_REGISTER msg, scope            // Accept ownership, will release on exit
... use msg ...
SCOPE_EXIT                           // RELEASE msg (refcount = 0) → freed ✓
```

**Refcount Timeline:**
1. Object created: refcount = 1 (producer owns)
2. RETURN: ownership transferred (still refcount = 1)
3. Consumer receives: refcount = 1 (consumer owns)
4. Consumer SCOPE_EXIT: refcount = 0 → freed

**No leak, no double-free, single ownership!** ✅

## Application to EK9 Constructs

### 1. Function Return Values

**Pattern:** Return variables are RETAIN'd but NOT SCOPE_REGISTER'd.

**Detection:** Use `symbol.isReturningParameter()` flag to identify return variables.

**Correct IR for function returning value:**

```ek9
getMessage() as String
  <- result as String: "Hello"
```

**Generated IR:**
```
OperationDfn: getMessage()->org.ek9.lang::String
BasicBlock: _entry_1
SCOPE_ENTER _scope_1
REFERENCE result, org.ek9.lang::String
_temp1 = LOAD_LITERAL "Hello", org.ek9.lang::String
RETAIN _temp1
SCOPE_REGISTER _temp1, _scope_1      ← Temp is managed by function scope
STORE result, _temp1
RETAIN result                        ← Function owns return variable
                                     ← NO SCOPE_REGISTER (ownership transfer!)
RETURN result                        ← Hand ownership to caller
SCOPE_EXIT _scope_1                  ← Releases _temp1, NOT result
```

**Caller side:**
```ek9
msg <- getMessage()
```

**Generated IR:**
```
REFERENCE msg, org.ek9.lang::String
_temp2 = CALL getMessage()           ← Receive ownership (refcount = 1)
STORE msg, _temp2
                                     ← NO RETAIN (already have ownership)
SCOPE_REGISTER msg, _scope_caller    ← Take ownership, will release on exit
... use msg ...
SCOPE_EXIT _scope_caller             ← RELEASE msg (refcount = 0) → freed
```

### 2. Exception Variables in Catch Blocks

**Pattern:** Exception variables are NOT RETAIN'd but ARE SCOPE_REGISTER'd.

**Detection:** Exception variables are declared in catch handler parameter (`-> ex as Exception`).

**Rationale:** The exception mechanism creates the exception object and "throws" it. The catch block **receives** this object with ownership already transferred.

**Correct IR for catch block:**

```ek9
try
  // Code that throws
catch
  -> ex as Exception
  stdout.println(ex)
```

**Generated IR:**
```
try_block_details: [...]
condition_chain:
[
[
case_scope_id: _scope_5
case_type: "EXCEPTION_HANDLER"
body_evaluation:
[
SCOPE_ENTER _scope_5
REFERENCE ex, org.ek9.lang::Exception
                                     ← NO RETAIN (receiving ownership from exception mechanism)
SCOPE_REGISTER ex, _scope_5          ← Accept ownership, will release on exit
_temp10 = LOAD ex
RETAIN _temp10
SCOPE_REGISTER _temp10, _scope_5
stdout.println(_temp10)
SCOPE_EXIT _scope_5                  ← Releases both ex and _temp10
]
exception_type: "org.ek9.lang::Exception"
exception_variable: ex
]
]
```

**Backend Responsibility:** When backend generates exception handling code:
1. JVM: `catch (Exception ex)` - JVM gives us a reference with refcount = 1
2. LLVM: `landingpad` - Landing pad extracts exception with refcount = 1
3. Store into `ex` variable slot with existing refcount
4. IR SCOPE_REGISTER ensures it's released when catch scope exits

### 3. Future Applications

The ownership transfer pattern applies to any boundary crossing:

**Callback Parameters:**
```ek9
forEach(callback: (item: String) -> Void)
```
- Iterator owns collection items, transfers ownership temporarily to callback
- Callback should NOT RETAIN parameter (receiving ownership)
- Callback SHOULD SCOPE_REGISTER parameter (will release on callback exit)

**Moved Values (future feature):**
```ek9
newOwner := move(oldOwner)
```
- oldOwner transfers ownership to newOwner
- oldOwner should NOT SCOPE_REGISTER (no longer owns)
- newOwner should SCOPE_REGISTER (new owner)

**Async/Await Captures:**
```ek9
async { captured }
```
- Parent scope transfers ownership to async context
- Parent should NOT SCOPE_REGISTER captured value
- Async context SHOULD SCOPE_REGISTER

## Implementation Checklist

### For Return Variables

**Location:** `AssignExpressionToSymbol.java:63-72`

**Current Code:**
```java
if (!release) {
  if (lhsSymbol.isPropertyField()) {
    // Field: RETAIN only
    instructions.add(MemoryInstr.retain(lhsVariableName, rhsExprDebugInfo));
  } else {
    // Local variable: RETAIN + SCOPE_REGISTER
    final var lhsVariableDetails = new VariableDetails(lhsVariableName, rhsExprDebugInfo);
    variableMemoryManagement.apply(() -> instructions, lhsVariableDetails);
  }
}
```

**Fixed Code:**
```java
if (!release) {
  if (lhsSymbol.isPropertyField()) {
    // Field: RETAIN only (object owns, not scope)
    instructions.add(MemoryInstr.retain(lhsVariableName, rhsExprDebugInfo));
  } else if (lhsSymbol.isReturningParameter()) {
    // Return variable: RETAIN only (ownership transfer to caller)
    instructions.add(MemoryInstr.retain(lhsVariableName, rhsExprDebugInfo));
  } else {
    // Local variable: RETAIN + SCOPE_REGISTER (scope owns)
    final var lhsVariableDetails = new VariableDetails(lhsVariableName, rhsExprDebugInfo);
    variableMemoryManagement.apply(() -> instructions, lhsVariableDetails);
  }
}
```

### For Exception Variables

**Location:** `TryCatchStatementGenerator.java:207-216`

**Current Code:**
```java
// Process catch block body
final var catchBodyEvaluation = new ArrayList<IRInstr>();
catchBodyEvaluation.add(ScopeInstr.enter(catchScopeId, debugInfo));

// Process instruction block
catchBodyEvaluation.addAll(processBlockStatements(catchCtx.instructionBlock()));

catchBodyEvaluation.add(ScopeInstr.exit(catchScopeId, debugInfo));
```

**Fixed Code:**
```java
// Process catch block body
final var catchBodyEvaluation = new ArrayList<IRInstr>();
catchBodyEvaluation.add(ScopeInstr.enter(catchScopeId, debugInfo));

// Register exception variable with ownership transfer semantics
// Backend stores caught exception into variable slot (refcount = 1)
// We receive ownership, so NO RETAIN, but YES SCOPE_REGISTER
final var exceptionVarRef = ReferenceInstr.reference(exceptionVariable, exceptionType, debugInfo);
catchBodyEvaluation.add(exceptionVarRef);

final var scopeRegisterEx = ScopeRegisterInstr.scopeRegister(
    exceptionVariable, catchScopeId, debugInfo);
catchBodyEvaluation.add(scopeRegisterEx);

// Process instruction block
catchBodyEvaluation.addAll(processBlockStatements(catchCtx.instructionBlock()));

catchBodyEvaluation.add(ScopeInstr.exit(catchScopeId, debugInfo));
```

## Testing Strategy

### Test 1: Return Variable Pattern

**Create:** `returnValueOwnership.ek9`

```ek9
defines module testReturn

  defines function

    getMessage() as String
      <- result as String: "Hello World"
```

**Expected IR (key section):**
```
REFERENCE result, org.ek9.lang::String
_temp1 = LOAD_LITERAL "Hello World", org.ek9.lang::String
RETAIN _temp1
SCOPE_REGISTER _temp1, _scope_1
STORE result, _temp1
RETAIN result                        ← Present
                                     ← SCOPE_REGISTER absent! (ownership transfer)
RETURN result
```

**Verify:** Return variable has RETAIN but no SCOPE_REGISTER.

### Test 2: Exception Variable Pattern

**Modify:** `simpleTryCatch.ek9`

```ek9
catch
  -> ex as Exception
  stdout.println("Exception: ")
  stdout.println(ex)  ← Actually use the exception variable!
```

**Expected IR (key section):**
```
SCOPE_ENTER _scope_5
REFERENCE ex, org.ek9.lang::Exception
                                     ← RETAIN absent! (receiving ownership)
SCOPE_REGISTER ex, _scope_5          ← Present (accepting ownership)
_temp10 = LOAD ex
RETAIN _temp10
SCOPE_REGISTER _temp10, _scope_5
```

**Verify:** Exception variable has SCOPE_REGISTER but no RETAIN.

### Test 3: Call Site Pattern

**Verify caller receives and manages return value correctly:**

```ek9
testCaller()
  msg <- getMessage()
  stdout.println(msg)
```

**Expected IR:**
```
_temp1 = CALL getMessage()           ← Receive ownership
STORE msg, _temp1
                                     ← NO RETAIN (already have ownership)
SCOPE_REGISTER msg, _scope_caller    ← Take ownership
```

## Common Pitfalls

### Pitfall 1: Treating All Variables Identically

**Wrong Assumption:** "All variables should be RETAIN'd and SCOPE_REGISTER'd."

**Reality:** Variables have different ownership semantics:
- **Local variables:** RETAIN + SCOPE_REGISTER (scope owns)
- **Return variables:** RETAIN only (transferring out)
- **Received values:** SCOPE_REGISTER only (transferring in)
- **Property fields:** RETAIN only (object owns, not scope)

### Pitfall 2: Forgetting Backend Initialization

**Wrong Assumption:** "Exception variables start with refcount = 0, need RETAIN."

**Reality:** Backend creates/catches exception with refcount = 1 and stores into variable slot. We receive ownership at refcount = 1.

### Pitfall 3: Double Management

**Wrong Pattern:**
```
// Producer
RETAIN result
SCOPE_REGISTER result  ❌
RETURN result

// Consumer
msg = CALL
SCOPE_REGISTER msg
```

**Result:** Both scopes try to release → double-free.

**Correct Pattern:** Only ONE scope manages via SCOPE_REGISTER.

## Architectural Rationale

### Why Not Reference Counting on Every Operation?

EK9 uses **scope-based ARC** rather than operation-based reference counting:

**Scope-Based (EK9):**
- References managed at scope entry/exit boundaries
- SCOPE_REGISTER + SCOPE_EXIT handle bulk releases
- Lower overhead (fewer refcount operations)
- Ownership clearly tied to scope lifetime

**Operation-Based (alternative):**
- Every LOAD increments, every last-use decrements
- Much higher overhead (refcount on every operation)
- More complex analysis (need liveness analysis)
- Ownership diffuse across operations

### Why Ownership Transfer is Essential

Without ownership transfer, we'd need:

1. **Explicit reference counting at call boundaries:**
   ```
   result = CALL()
   RETAIN result  ← Every call site must remember this
   ```

2. **Complex scope exit analysis:**
   ```
   if (returning) {
     // Don't release
   } else {
     RELEASE
   }
   ```

3. **Backend-specific handling:**
   - JVM: Different rules for return vs local
   - LLVM: Different patterns for exception handling

**Ownership transfer** provides:
- Uniform pattern across all boundary crossings
- Clear producer/consumer semantics
- Backend-agnostic memory management
- Compile-time ownership verification

## Backend Implementation Notes

### JVM Backend

**Return Values:**
- ALOAD return_variable → loads reference onto stack
- ARETURN → returns reference to caller
- No JVM refcount operations (handled by garbage collector)
- EK9's RETAIN/RELEASE map to metadata for future GC optimization

**Exception Variables:**
- `catch (ExceptionType ex)` → JVM stores exception in local variable slot
- EK9 sees this as "receiving ownership"
- SCOPE_EXIT in catch block ensures proper cleanup metadata

### LLVM Backend

**Return Values:**
- Load return variable pointer
- Potentially increment refcount if needed by ABI
- Return value in register or stack
- Caller receives pointer with ownership

**Exception Variables:**
- `landingpad` extracts exception pointer
- Store into exception variable alloca
- Exception already has refcount = 1 from throwing mechanism
- Catch block SCOPE_EXIT decrements refcount

## Critical Instruction Ordering: SCOPE_EXIT Before RETURN

### The Fundamental Rule

**SCOPE_EXIT must ALWAYS come BEFORE RETURN in the IR instruction sequence.**

This ordering is not arbitrary—it aligns with how all major languages with deterministic resource management handle scope cleanup on function exit.

### Industry Standard Semantics

#### LLVM ARC (Swift, Objective-C)
From Clang ARC documentation:
> "When returning from such a function or method, ARC **retains the value** at the point of evaluation of the return statement, then **leaves all local scopes**, and then **balances out the retain** while ensuring that the value lives across the call boundary."

**Order:** Retain return value → Leave scopes (cleanup) → Return

#### C++ RAII
From C++ standard behavior:
> "When a return statement is executed, **the return value is evaluated first** before the return takes place, then what happens is equivalent to **all nested scopes of the function being exited** in order from innermost to outermost, with **destructors being called appropriately** at each exit."

**Order:** Evaluate return expression → Exit scopes (destructors) → Return

#### Swift defer Statement
From Swift semantics:
> "The defer block executes as the last action before the function exits its current scope... **execution happening after the method's other code but before returning**."

**Order:** Function body → defer blocks (cleanup) → Return

### Why SCOPE_EXIT Before RETURN

1. **Return Value Safety:** The return value is prepared/retained FIRST (at return statement evaluation)
2. **Cleanup Before Exit:** All local objects are released BEFORE control leaves the function
3. **Stack Frame Integrity:** Cleanup happens while the stack frame still exists
4. **Ownership Transfer:** Return value survives cleanup because it's retained before SCOPE_EXIT

### Correct IR Pattern

```
OperationDfn: <function>._call(...)-><ReturnType>
BasicBlock: _entry_X
SCOPE_ENTER _scope_1              // Enter function body scope

// Function body: create locals, perform operations
REFERENCE result, ReturnType
_temp1 = CALL Constructor()
RETAIN _temp1                     // refcount = 1
STORE result, _temp1
RETAIN result                     // Retain return value (refcount = 2)
// Note: result NOT SCOPE_REGISTERED (ownership transfer pattern)

SCOPE_EXIT _scope_1               // ← MUST come FIRST
                                  // Releases all SCOPE_REGISTERED locals
                                  // BUT NOT result (not registered)
                                  // refcount for result = 1 (stays alive)

RETURN result                     // ← MUST come SECOND
                                  // Transfers ownership to caller
```

### What Would Go Wrong If Order Was Reversed

**INCORRECT (RETURN before SCOPE_EXIT):**
```
RETURN result     // Transfer ownership to caller
SCOPE_EXIT _scope_1   // ← TOO LATE! Already returned!
```

**Problems:**
1. **Unreachable Cleanup:** SCOPE_EXIT is never executed (control already left function)
2. **Memory Leaks:** All SCOPE_REGISTERED objects never get RELEASE
3. **Violates ARC Semantics:** Doesn't match LLVM/C++/Swift behavior
4. **Debugger Confusion:** Debug line numbers don't reflect actual execution order

### Bytecode Line Number Implications

The SCOPE_EXIT and RETURN instruction order affects bytecode line number tables and SMAP (Source Map) entries:

**Expected SMAP pattern:**
```
89#1:89  ← SCOPE_EXIT (points to instruction block start for symmetry with SCOPE_ENTER)
88#1:88  ← RETURN (points back to method signature)
```

This pattern creates proper debugging experience:
- Debugger shows you "at instruction block" during cleanup
- Then shows "at method signature" during return
- Matches mental model of "cleaning up before leaving"

### Implementation in OperationDfnGenerator.java

```java
// 3. Process instruction block statements
if (hasInstructionBlock) {
  instructionBuilder.addInstructions(processInstructionBlockStatements(ctx.instructionBlock()));
}

// 4. Exit function body scope BEFORE return (scope cleanup must happen before function exit)
if (needsFunctionBodyScope) {
  // Add SCOPE_EXIT instruction (uses same DebugInfo as SCOPE_ENTER for symmetry)
  instructionBuilder.addInstruction(ScopeInstr.exit(bodyScopeId, bodyScopeDebugInfo));

  // Pop scope from stack
  stackContext.exitScope();
}

// 5. Add return statement based on function signature (happens after scope cleanup)
instructionBuilder.addInstructions(generateReturnStatement(operation));
```

**Key Point:** Steps 4 and 5 execute in this ORDER. SCOPE_EXIT (step 4) must come before RETURN (step 5).

### Verification

All @IR test directives must reflect this pattern:
```
// OLD (missing SCOPE_EXIT - phantom _call scope bug):
RETURN

// WRONG (reversed order):
RETURN
SCOPE_EXIT _scope_1

// CORRECT:
SCOPE_EXIT _scope_1
RETURN
```

When updating test expectations, **always** verify the SCOPE_EXIT → RETURN ordering.

## Summary

The **ownership transfer pattern** is fundamental to EK9's ARC memory management:

1. **Producer:** RETAIN (claim) but NO SCOPE_REGISTER (transfer out)
2. **Consumer:** NO RETAIN (receive) but YES SCOPE_REGISTER (accept ownership)
3. **Instruction Order:** SCOPE_EXIT BEFORE RETURN (always)

This pattern ensures:
- ✅ No memory leaks
- ✅ No premature deallocation
- ✅ No double-free
- ✅ Clear ownership semantics
- ✅ Backend-agnostic implementation
- ✅ Correct cleanup ordering matching LLVM ARC / C++ RAII / Swift defer

**Key Insights:**
- Scope boundaries represent **ownership transfer points**, not just execution flow boundaries
- SCOPE_EXIT before RETURN is **mandatory** for correct ARC semantics
- This ordering matches **all major languages** with deterministic resource management

**Implementation:**
- Check `isReturningParameter()` flag and exception variable context to apply correct pattern
- Always generate SCOPE_EXIT before RETURN in OperationDfnGenerator

---

**Document Version:** 2.0
**Date:** 2025-10-30
**Status:** Authoritative specification for ARC ownership transfer in EK9 IR generation
**Update:** Added critical instruction ordering section (SCOPE_EXIT before RETURN)
