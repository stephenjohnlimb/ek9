# EK9 Control Flow Scope Architecture

**Date**: 2025-10-18
**Status**: MANDATORY Architectural Pattern
**Author**: Claude Code + Steve

---

## Executive Summary

This document defines the **mandatory architectural pattern** for ALL control flow constructs in EK9. Every control flow generator (if/else, switch, while, for, try/catch) MUST create an outer scope wrapper around the CONTROL_FLOW_CHAIN instruction.

**This is not optional** - it is fundamental infrastructure that enables guards, expression forms, and consistent memory management across all control flow constructs.

---

## The Mandatory Pattern

### Code Template

Every control flow generator follows this exact pattern:

```java
public List<IRInstr> apply(final EK9Parser.SomeControlFlowContext ctx) {
  final var instructions = new ArrayList<IRInstr>();
  final var debugInfo = stackContext.createDebugInfo(ctx);

  // 1. CREATE OUTER SCOPE
  final var outerScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
  stackContext.enterScope(outerScopeId, debugInfo, IRFrameType.BLOCK);

  // 2. EMIT SCOPE_ENTER
  instructions.add(ScopeInstr.enter(outerScopeId, debugInfo));

  // 3. PROCESS GUARDS (if applicable)
  if (ctx.preFlowStatement() != null) {
    // Process guards in outer scope
    instructions.addAll(processGuards(ctx.preFlowStatement()));
  }

  // 4. SETUP SPECIAL VARIABLES (if applicable)
  // - Return variables (expression forms)
  // - Evaluation variables (switch)
  // - Iterator/range state (loops)

  // 5. CREATE CONTROL_FLOW_CHAIN
  final var details = ControlFlowChainDetails.create*(..., outerScopeId);
  instructions.addAll(generators.controlFlowChainGenerator.apply(details));

  // 6. EMIT SCOPE_EXIT
  instructions.add(ScopeInstr.exit(outerScopeId, debugInfo));

  // 7. EXIT SCOPE CONTEXT
  stackContext.exitScope();

  return instructions;
}
```

### Three-Tier Scope Architecture

```
┌─────────────────────────────────────────────────────────────┐
│ Operation Scope (_call)           ← Implicit, function-level│
│                                                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Outer Scope (_scope_1)         ← Generator creates   │   │
│  │                                                        │   │
│  │  ┌─ Guards                      ← preFlowStatement   │   │
│  │  ├─ Return variables            ← Expression forms   │   │
│  │  ├─ Evaluation variables        ← Switch             │   │
│  │  ├─ Iterator/Range state        ← Loops              │   │
│  │  ├─ Condition temporaries       ← Accumulate here    │   │
│  │  │                                                    │   │
│  │  └─ CONTROL_FLOW_CHAIN                               │   │
│  │      │                                                │   │
│  │      ├─ Inner Scope 1 (_scope_2) ← Branch/Iteration  │   │
│  │      ├─ Inner Scope 2 (_scope_3) ← Branch/Iteration  │   │
│  │      └─ Inner Scope N (_scope_N) ← Branch/Iteration  │   │
│  │                                                        │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

---

## Why This Architecture?

### 1. Guards Slot In Naturally

When adding guard support to ANY control flow construct:

```java
// Before (simple form)
SCOPE_ENTER _scope_1
  CONTROL_FLOW_CHAIN
SCOPE_EXIT _scope_1

// After (with guards)
SCOPE_ENTER _scope_1
  // Guards process here - already in outer scope!
  iter <- collection.iterator()
  CONTROL_FLOW_CHAIN
SCOPE_EXIT _scope_1
```

**No architectural changes needed** - guards just add instructions in the outer scope.

### 2. Expression Forms Slot In Naturally

When adding expression form (returns a value):

```java
// Before (statement form)
SCOPE_ENTER _scope_1
  CONTROL_FLOW_CHAIN
SCOPE_EXIT _scope_1

// After (expression form)
SCOPE_ENTER _scope_1
  REFERENCE returnVar, Type    // Return variable setup here
  CONTROL_FLOW_CHAIN
  STORE result, returnVar       // Final assignment here
SCOPE_EXIT _scope_1
```

**No architectural changes needed** - return variable setup happens in outer scope.

### 3. Consistent Memory Management

All condition temporaries register to outer scope:

```java
// In condition evaluation (executed multiple times for loops)
_temp1 = LOAD counter
RETAIN _temp1
SCOPE_REGISTER _temp1, _scope_1    // ← Outer scope

_temp2 = LOAD_LITERAL 10
RETAIN _temp2
SCOPE_REGISTER _temp2, _scope_1    // ← Outer scope

// All temps cleaned up when outer scope exits
```

For loops, hundreds of temps can accumulate (10 iterations × 4 temps = 40 temps), all registered to _scope_1, all cleaned up when loop exits.

### 4. Future-Proofing

Even "simple" forms without guards or return values use correct architecture:

- Simple if/else → outer scope ready for guards later
- Simple while → outer scope ready for guards + expression form later
- Simple switch → outer scope ready for guards + evaluation variable later

**Build it right the first time** - no refactoring needed when adding features.

---

## Construct-Specific Patterns

### If/Else

```
SCOPE_ENTER _scope_1                ← Outer: guards, condition temps
  [Guards if present]
  CONTROL_FLOW_CHAIN IF_ELSE_IF
    [Condition evaluation]          ← Temps registered to _scope_1
    Branch scope _scope_2:
      SCOPE_ENTER _scope_2
        [if body]
      SCOPE_EXIT _scope_2
    Branch scope _scope_3:
      SCOPE_ENTER _scope_3
        [else if body]
      SCOPE_EXIT _scope_3
    Branch scope _scope_4:
      [else body - no extra scope if simple]
SCOPE_EXIT _scope_1
```

**Reference**: `IfStatementGenerator.java:44-81`

### While Loop

```
SCOPE_ENTER _scope_1                ← Outer: guards, return var, condition temps
  [Guards if present]
  [Return variable setup if expression form]
  CONTROL_FLOW_CHAIN WHILE_LOOP
    Loop:                           ← Backend loops back here
      [Condition evaluation]        ← Temps registered to _scope_1 (accumulate!)
      Iteration scope _scope_2:
        SCOPE_ENTER _scope_2
          [body]
        SCOPE_EXIT _scope_2
        [Backend jumps back to condition]
SCOPE_EXIT _scope_1
```

**Key difference**: Condition evaluation happens MULTIPLE times, accumulating temps in _scope_1.

### Do-While Loop

```
SCOPE_ENTER _scope_1                ← Outer: return var (guards at top of body!)
  [Return variable setup if expression form]
  CONTROL_FLOW_CHAIN DO_WHILE_LOOP
    Loop:                           ← Backend loops back here
      Iteration scope _scope_2:
        SCOPE_ENTER _scope_2
          [Guards if present - execute each iteration!]
          [body]
          [Condition evaluation]    ← At end
        SCOPE_EXIT _scope_2
        [Backend jumps back if condition true]
SCOPE_EXIT _scope_1
```

**Special case**: Do-while guards execute EACH iteration (inside _scope_2).

### For-In Loop

```
SCOPE_ENTER _scope_1                ← Outer: guards, iterator, return var
  [Guards if present]
  [Iterator setup: _iter = collection.iterator()]
  [Return variable setup if expression form]
  CONTROL_FLOW_CHAIN FOR_IN_LOOP
    Loop:
      [Condition: _iter.hasNext()]  ← Temps registered to _scope_1
      Iteration scope _scope_2:
        SCOPE_ENTER _scope_2
          [Loop variable: item = _iter.next()]
          [body]
        SCOPE_EXIT _scope_2
        [Backend jumps back]
SCOPE_EXIT _scope_1
```

**Key**: Iterator lives in _scope_1, loop variable in _scope_2 (fresh each iteration).

### For-Range Loop

```
SCOPE_ENTER _scope_1                ← Outer: guards, range state, return var
  [Guards if present]
  [Range setup: _current, _end, _step]
  [Return variable setup if expression form]
  CONTROL_FLOW_CHAIN FOR_RANGE_LOOP
    Loop:
      [Condition: range check]      ← Temps registered to _scope_1
      Iteration scope _scope_2:
        SCOPE_ENTER _scope_2
          [Loop variable: i = _current]
          [body]
          [Increment: _current += _step]
        SCOPE_EXIT _scope_2
        [Backend jumps back]
SCOPE_EXIT _scope_1
```

**Key**: Range state (_current, _end, _step) persists in _scope_1 across iterations.

### Switch

```
SCOPE_ENTER _scope_1                ← Outer: guards, eval var, return var
  [Guards if present]
  [Evaluation variable: _eval = expression]
  [Return variable setup if expression form]
  CONTROL_FLOW_CHAIN SWITCH
    [Case conditions]               ← Compare against _eval
    Case scope _scope_2:
      SCOPE_ENTER _scope_2
        [case body]
      SCOPE_EXIT _scope_2
    Case scope _scope_3:
      SCOPE_ENTER _scope_3
        [case body]
      SCOPE_EXIT _scope_3
    Default scope _scope_4:
      [default body]
SCOPE_EXIT _scope_1
```

**Key**: Evaluation variable evaluated ONCE in _scope_1, used by all cases.

### Try/Catch

```
SCOPE_ENTER _scope_1                ← Outer: resource guards, return var
  [Resource guards: stream <- openFile()]
  [Return variable setup if expression form]
  CONTROL_FLOW_CHAIN TRY_CATCH
    Try scope _scope_2:
      SCOPE_ENTER _scope_2
        [try body]
      SCOPE_EXIT _scope_2
    Catch scope _scope_3:
      SCOPE_ENTER _scope_3
        [Exception variable]
        [catch body]
      SCOPE_EXIT _scope_3
    Finally scope _scope_4:
      [finally body - cleanup resources]
SCOPE_EXIT _scope_1
```

**Key**: Resources opened in _scope_1 available to all try/catch/finally blocks.

---

## Implementation Checklist

When creating ANY control flow generator:

### Required Steps

- [ ] **Create outer scope** with `generateScopeId(IRConstants.GENERAL_SCOPE)`
- [ ] **Enter scope context** with `enterScope(outerScopeId, debugInfo, IRFrameType.BLOCK)`
- [ ] **Emit SCOPE_ENTER** instruction: `ScopeInstr.enter(outerScopeId, debugInfo)`
- [ ] **Process guards** in outer scope (if `preFlowStatement` present)
- [ ] **Setup special variables** in outer scope (return vars, eval vars, iterators)
- [ ] **Create CONTROL_FLOW_CHAIN** with `outerScopeId` parameter
- [ ] **Emit CONTROL_FLOW_CHAIN** via `generators.controlFlowChainGenerator.apply(details)`
- [ ] **Emit SCOPE_EXIT** instruction: `ScopeInstr.exit(outerScopeId, debugInfo)`
- [ ] **Exit scope context** with `exitScope()`

### Common Mistakes to Avoid

- ❌ **Skipping outer scope** for "simple" cases
- ❌ **Putting guards in CONTROL_FLOW_CHAIN** instead of outer scope
- ❌ **Forgetting to register temps** to outer scope
- ❌ **Creating outer scope inside CONTROL_FLOW_CHAIN** instead of wrapping it
- ❌ **Inconsistent scope IDs** between context and details

### Verification

After implementation, verify:

1. ✅ IR shows `SCOPE_ENTER _scope_1` BEFORE `CONTROL_FLOW_CHAIN`
2. ✅ IR shows `SCOPE_EXIT _scope_1` AFTER `CONTROL_FLOW_CHAIN`
3. ✅ All condition temps registered to _scope_1
4. ✅ Inner scopes (_scope_2, _scope_3) for branches/iterations
5. ✅ Pattern matches `IfStatementGenerator.java:44-81`

---

## Why This Pattern is Mandatory

### Technical Reasons

1. **Guards require outer scope** - They execute before condition evaluation
2. **Expression forms require outer scope** - Return variable accumulates across branches/iterations
3. **Memory management requires outer scope** - Condition temps must have cleanup location
4. **Consistency simplifies backends** - All constructs follow same pattern

### Architectural Reasons

1. **Future-proof** - Adding features doesn't require refactoring
2. **Predictable** - Every generator follows identical structure
3. **Maintainable** - Clear separation of concerns
4. **Testable** - Scope structure validates correctly

### Practical Reasons

1. **No special cases** - Same pattern for all constructs
2. **Copy-paste friendly** - Template works for everything
3. **Review-friendly** - Deviations are obvious
4. **Documentation-friendly** - Single pattern to learn

---

## Reference Implementation

**File**: `IfStatementGenerator.java:44-81`

This is the canonical example. All control flow generators should structurally match this pattern:

```java
// Enter NEW scope for entire if/else chain
final var chainScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
stackContext.enterScope(chainScopeId, debugInfo, IRFrameType.BLOCK);

instructions.add(ScopeInstr.enter(chainScopeId, debugInfo));

// TODO Phase 2: Process guard variable if exists
// For now, throwing exception in processIfControlBlockWithBranchScope

// Process all if/else if conditions with UNIQUE branch scopes
final var conditionChain = new ArrayList<ConditionCaseDetails>();
for (var ifControlBlock : ctx.ifControlBlock()) {
  conditionChain.add(processIfControlBlockWithBranchScope(ifControlBlock));
}

// Process else block with its OWN scope
List<IRInstr> defaultBodyEvaluation = List.of();
if (ctx.elseOnlyBlock() != null) {
  defaultBodyEvaluation = processElseBlockWithBranchScope(ctx.elseOnlyBlock());
}

// Create CONTROL_FLOW_CHAIN details (chainScopeId is the outer scope)
final var details = ControlFlowChainDetails.createIfElse(
    null, // No result for statement form
    conditionChain,
    defaultBodyEvaluation,
    null, // No default result for statement form
    debugInfo,
    chainScopeId
);

// Use ControlFlowChainGenerator to generate IR
instructions.addAll(generators.controlFlowChainGenerator.apply(details));

// Exit chain scope
instructions.add(ScopeInstr.exit(chainScopeId, debugInfo));
stackContext.exitScope();
```

---

## Consequences of Violating This Pattern

### If You Skip Outer Scope

**Problem**: Where do guards go?
- ❌ Can't add guards later without refactoring
- ❌ Can't add expression forms without refactoring
- ❌ No clear ownership for condition temps

**Result**: Technical debt and architectural inconsistency.

### If You Put Guards Inside CONTROL_FLOW_CHAIN

**Problem**: Guards execute too late.
- ❌ Guards should initialize BEFORE condition evaluation
- ❌ Can't reference guard variables in condition
- ❌ Scope lifetime doesn't match semantic requirements

**Result**: Incorrect semantics and potential runtime errors.

### If You Use Different Patterns for Different Constructs

**Problem**: Inconsistency makes codebase harder to understand.
- ❌ Each generator looks different
- ❌ Patterns don't transfer between constructs
- ❌ Code review requires understanding multiple approaches

**Result**: Maintenance burden and increased cognitive load.

---

## Summary

**The Outer Scope Wrapper is MANDATORY for ALL control flow constructs.**

**Pattern**:
1. Create outer scope
2. Enter scope context
3. Emit SCOPE_ENTER
4. Process guards/setup
5. Create CONTROL_FLOW_CHAIN
6. Emit SCOPE_EXIT
7. Exit scope context

**Rationale**:
- Guards need persistent scope
- Expression forms need return variable location
- Condition temps need cleanup location
- Consistency across all constructs

**Reference**: `IfStatementGenerator.java:44-81`

**⚠️ DO NOT bypass this pattern** - even for "simple" cases without guards or return values.

This is fundamental architectural infrastructure, not optional convenience.

---

**Document Status**: MANDATORY Reference
**Last Updated**: 2025-10-18
**Review Required**: Before implementing ANY new control flow generator
