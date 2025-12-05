# JVM Bytecode Generator - Label Naming Convention

## Two-Tier Uniqueness Pattern

Label uniqueness in bytecode generation follows a **two-tier pattern** based on construct type:

| Construct Type | Uniqueness Source | Reason |
|----------------|-------------------|--------|
| **Statement-level** (if, while, for, switch, try) | Scope ID | Each statement creates a new scope |
| **Expression-level** (and, or, ?) | Result variable | Multiple expressions share containing scope |

## Pattern: createControlFlowLabel(prefix, uniqueId)

### Statement-Level Constructs (use Scope IDs)

Statement-level constructs create **new lexical scopes** via `stackContext.enterScope()`.
Each scope gets a unique ID, making `getScopeId()` the correct choice.

```java
// Loop generators - use scopeId from instruction
final var label = createControlFlowLabel("while_start", instr.getScopeId());
final var label = createControlFlowLabel("for_asc", scopeMetadata.loopScopeId());

// Conditional generators - use scopeId from instruction
final var label = createControlFlowLabel("if_end", instr.getScopeId());
final var label = createControlFlowLabel("if_next", conditionCase.caseScopeId());

// Try-catch generators - use scopeId for synthetic temp variables
final var uniqueTempVarName = "_temp_finally_exception_" + instr.getScopeId();
```

### Expression-Level Constructs (use Result Variables)

Expression-level constructs execute **within the containing scope** - they don't push new scopes.
Multiple expressions in the same scope share the same `currentScopeId()`.
Each expression has a unique result variable from `generateTempName()`.

```java
// Logical operation generators - use result variable (unique per expression)
final var shortCircuitLabel = createControlFlowLabel("and_short", instr.getResult());
final var endLabel = createControlFlowLabel("and_end", instr.getResult());

// Question operator - use result variable for main labels
final var endLabel = createControlFlowLabel("qop_end", instr.getResult());
// For internal cases, combine result + case scope ID
final var nextLabel = createControlFlowLabel("qop_next_" + instr.getResult(), caseScopeId);
```

### Incorrect Usage (DO NOT DO THIS)

```java
// WRONG - uses loop variable name (collides in nested loops with same variable)
final var label = createControlFlowLabel("for_asc", loopVariableName);

// WRONG - uses hardcoded counter (not thread-safe, not unique across methods)
static int counter = 0;
final var label = createControlFlowLabel("label", String.valueOf(counter++));

// WRONG - uses scopeId for expression-level constructs (would collide!)
// Multiple AND expressions in same scope share scopeId but have different results
final var label = createControlFlowLabel("and_short", instr.getScopeId());  // WRONG for expressions
```

## Why Two Patterns?

### Statement-Level: Why Scope IDs Work

Each statement creates a new scope:
```ek9
if condition1           // Creates _scope_2
  if condition2         // Creates _scope_4 (different!)
    action()
```
Labels: `if_end__scope_2` vs `if_end__scope_4` - unique!

### Expression-Level: Why Result Variables Are Required

Expressions share the containing scope:
```ek9
x <- a and b    // scopeId = "_scope_5", result = "_temp0"
y <- c and d    // scopeId = "_scope_5", result = "_temp3" (SAME scope!)
```

If we used scopeId:
- Labels for x: `and_short__scope_5`, `and_end__scope_5`
- Labels for y: `and_short__scope_5`, `and_end__scope_5` - **COLLISION!**

If we use result variable:
- Labels for x: `and_short__temp0`, `and_end__temp0`
- Labels for y: `and_short__temp3`, `and_end__temp3` - **Unique!**

## Nested Construct Examples

### Example 1: Nested Loops (Statement-Level)

**EK9 Code**:
```ek9
for i in 1 ... 10
  for i in 1 ... 5  // Same variable name "i"
    sum: sum + i
```

**IR Scope IDs** (automatically unique - each FOR creates a scope):
```
Outer loop: loopScopeId = "_scope_3"
Inner loop: loopScopeId = "_scope_6"
```

**Generated Labels** (no collision):
```
Outer: for_asc__scope_3, for_desc__scope_3, for_end__scope_3
Inner: for_asc__scope_6, for_desc__scope_6, for_end__scope_6  ✅
```

---

### Example 2: Nested Conditionals (Statement-Level)

**EK9 Code**:
```ek9
if condition1
  if condition2
    action()
```

**IR Scope IDs** (automatically unique - each IF creates a scope):
```
Outer: scopeId = "_scope_2", caseScopeId = "_scope_3"
Inner: scopeId = "_scope_4", caseScopeId = "_scope_5"
```

**Generated Labels** (no collision):
```
Outer: if_end__scope_2, if_next__scope_3
Inner: if_end__scope_4, if_next__scope_5  ✅
```

---

### Example 3: Multiple Logical Operations (Expression-Level)

**EK9 Code**:
```ek9
x <- a and b
y <- c and d
z <- (a and b) or (c and d)
```

**IR Result Variables** (unique per expression via generateTempName()):
```
x assignment: result = "_temp0"
y assignment: result = "_temp3"
z inner AND 1: result = "_temp6"
z inner AND 2: result = "_temp9"
z outer OR: result = "_temp12"
```

**Generated Labels** (no collision - each expression has unique result):
```
x: and_short__temp0, and_end__temp0
y: and_short__temp3, and_end__temp3
z AND 1: and_short__temp6, and_end__temp6
z AND 2: and_short__temp9, and_end__temp9
z OR: or_short__temp12, or_end__temp12  ✅
```

**Note**: All these expressions may share the same `scopeId` (the containing method/block),
but each has a unique result variable.

## Uniqueness Source by IR Instruction Type

| IR Instruction Type | Uniqueness Source | Accessor | Reason |
|---------------------|-------------------|----------|--------|
| `ControlFlowChainInstr` (if/switch) | Scope ID | `instr.getScopeId()` | Statement creates scope |
| `ConditionCaseDetails` | Case Scope ID | `conditionCase.caseScopeId()` | Each case has scope |
| `ForRangePolymorphicInstr` | Loop Scope ID | `scopeMetadata.loopScopeId()` | Loop creates scope |
| `WhileLoopInstr` | Scope ID | `instr.getScopeId()` | Loop creates scope |
| `TryCatchInstr` | Scope ID | `instr.getScopeId()` | Try creates scope |
| `LogicalOperationInstr` | **Result Variable** | `instr.getResult()` | Expression in containing scope |
| `QuestionOperatorInstr` | **Result Variable** | `instr.getResult()` | Expression in containing scope |

## Verification Checklist

When implementing a new control flow generator:

- [ ] **Identify construct type** - Is it statement-level (creates scope) or expression-level (uses containing scope)?
- [ ] **Statement-level**: Use `getScopeId()` or appropriate scope accessor
- [ ] **Expression-level**: Use `getResult()` for uniqueness
- [ ] **For internal cases**: Use case-specific scope IDs (`caseScopeId()`)
- [ ] **Synthetic temp vars**: Always append scope ID (e.g., `_temp_finally_exception_` + scopeId)
- [ ] **Test with multiples**: Create test with multiple same-type constructs in same scope
- [ ] **Verify labels in javap** - Manually inspect bytecode to confirm unique labels

## Current Status (as of 2025-12-05)

### ✅ Statement-Level Generators (using scopeId)
- `WhileLoopAsmGenerator` - uses `instr.getScopeId()`
- `DoWhileLoopAsmGenerator` - uses `instr.getScopeId()`
- `IfElseAsmGenerator` - uses `instr.getScopeId()` and `conditionCase.caseScopeId()`
- `SwitchAsmGenerator` - uses `instr.getScopeId()` and `conditionCase.caseScopeId()`
- `SwitchExpressionAsmGenerator` - uses `instr.getScopeId()` and `conditionCase.caseScopeId()`
- `TernaryOperatorAsmGenerator` - uses `instr.getScopeId()` and `conditionCase.caseScopeId()`
- `ForRangePolymorphicAsmGenerator` - uses `scopeMetadata.loopScopeId()`
- `TryCatchAsmGenerator` - uses `instr.getScopeId()` for temp variable uniqueness
- `NullCoalescingOperatorAsmGenerator` - uses `instr.getScopeId()`
- `ElvisCoalescingOperatorAsmGenerator` - uses `instr.getScopeId()`
- `ComparisonCoalescingOperatorAsmGenerator` - uses `instr.getScopeId()`
- `GuardedAssignmentAsmGenerator` - uses `instr.getScopeId()`

### ✅ Expression-Level Generators (using result variable)
- `LogicalOperationAsmGenerator` - uses `instr.getResult()` (correct for expressions)
- `QuestionOperatorAsmGenerator` - uses `instr.getResult()` + `caseScopeId()` for internal cases

### ✅ Special Cases (delegate to IR)
- `BranchInstrAsmGenerator` - uses IR-provided label names (correct delegation)
- `LabelInstrAsmGenerator` - uses IR-provided label names (correct delegation)

## Implementation Pattern Templates

### Statement-Level Generator Template

```java
/**
 * Specialized generator for [STATEMENT CONSTRUCT].
 * Creates new scope - uses scopeId for label uniqueness.
 */
final class [Construct]AsmGenerator extends AbstractControlFlowAsmGenerator {

  public void generate(final [InstrType] instr) {
    // Use scope ID - this statement creates its own scope
    final var scopeId = instr.getScopeId();

    final var startLabel = createControlFlowLabel("prefix_start", scopeId);
    final var endLabel = createControlFlowLabel("prefix_end", scopeId);

    // For internal cases, use case-specific scope IDs
    for (var conditionCase : instr.getConditionChain()) {
      final var caseLabel = createControlFlowLabel(
          "prefix_case",
          conditionCase.caseScopeId());
      // ...
    }

    placeLabel(startLabel);
    // ... bytecode generation ...
    placeLabel(endLabel);
  }
}
```

### Expression-Level Generator Template

```java
/**
 * Specialized generator for [EXPRESSION CONSTRUCT].
 * Executes in containing scope - uses result variable for label uniqueness.
 */
final class [Construct]AsmGenerator extends AbstractControlFlowAsmGenerator {

  public void generate(final [InstrType] instr) {
    // Use result variable - multiple expressions share the same containing scope
    // but each has a unique result from generateTempName()
    final var shortCircuitLabel = createControlFlowLabel("prefix_short", instr.getResult());
    final var endLabel = createControlFlowLabel("prefix_end", instr.getResult());

    // Process evaluation...
    branchIfFalse(condition, shortCircuitLabel);
    // ...
    jumpTo(endLabel);

    placeLabel(shortCircuitLabel);
    // Short-circuit path...

    placeLabel(endLabel);
  }
}
```

## Common Pitfalls

### Pitfall 1: Using Scope ID for Expression-Level Constructs
**Wrong pattern**: `createControlFlowLabel("and_short", instr.getScopeId())`
**Problem**: Multiple expressions in same scope would have same scopeId - collision!
**Fix**: Use `instr.getResult()` for expression-level constructs

### Pitfall 2: Using Loop Variable Names
**Wrong pattern**: `createControlFlowLabel("for_asc", loopVariableName)`
**Problem**: Nested loops with same variable name would collide
**Fix**: Use scope ID - each loop creates its own scope

### Pitfall 3: Hardcoded Synthetic Variable Names
**Wrong pattern**: `getVariableIndex("_temp_exception")`
**Problem**: Multiple try-catch blocks share the same temp variable - bug!
**Fix**: Append scope ID: `"_temp_exception_" + instr.getScopeId()`

### Pitfall 4: Not Testing Multiple Same-Type Constructs
**Wrong assumption**: "One AND works, multiple ANDs will be fine"
**Problem**: Label collisions only appear with multiple constructs in same scope
**Fix**: Always test with `x <- a and b; y <- c and d` pattern

## Summary

**Two-Tier Rule**:
1. **Statement-level constructs** (if, while, for, switch, try) - use **scope IDs**
2. **Expression-level constructs** (and, or, ?) - use **result variables**

This ensures:
- ✅ Guaranteed uniqueness across all scenarios
- ✅ Correct handling of multiple expressions in same scope
- ✅ Correct handling of nested statements
- ✅ Synthetic temp variables are unique per construct
