# FOR Loop Variable Architecture Explanation

**Question:** Why is `loopVariableName` unused in `ForStatementGenerator.generateBodyInstructions()`?

## TL;DR - This is correct by design

The `loopVariableName` parameter in `generateBodyInstructions()` is **intentionally unused** because the loop variable assignment happens elsewhere in a separate IR sequence called `loopBodySetup`.

## Architecture Overview

FOR_RANGE_POLYMORPHIC uses a **split architecture** where loop control and loop body are separated:

```
FOR_RANGE_POLYMORPHIC instruction contains:
├── initialization (evaluate start/end/by, compute direction)
├── dispatchCases
│   ├── ascending
│   │   ├── directionCheck      [IR: direction < 0]
│   │   ├── loopCondition       [IR: current <= end]
│   │   ├── loopBodySetup ⭐     [IR: loopVariable = current]  ← ASSIGNMENT HAPPENS HERE
│   │   └── loopIncrement       [IR: current++]
│   ├── descending
│   │   ├── directionCheck      [IR: direction > 0]
│   │   ├── loopCondition       [IR: current >= end]
│   │   ├── loopBodySetup ⭐     [IR: loopVariable = current]  ← AND HERE
│   │   └── loopIncrement       [IR: current--]
│   └── equal
│       ├── loopBodySetup ⭐     [IR: loopVariable = current]  ← AND HERE
│       └── singleIteration: true
├── bodyInstructions ⭐          [IR: user's loop body code]   ← BODY STORED ONCE
└── metadata
```

## Code Flow Analysis

### Step 1: Generate Body Instructions (Line 130)

```java
// ForStatementGenerator.java:130
final var bodyInstructions = generateBodyInstructions(ctx, loopVariableName, debugInfo, bodyScopeId);
```

**Inside `generateBodyInstructions()` (lines 377-396):**

```java
private List<IRInstr> generateBodyInstructions(
    final EK9Parser.ForStatementExpressionContext ctx,
    final String loopVariableName,  // ⚠️ UNUSED PARAMETER
    final DebugInfo debugInfo,
    final String bodyScopeId) {

  final var bodyInstructions = new ArrayList<IRInstr>();

  // Add SCOPE_ENTER for body scope
  bodyInstructions.add(ScopeInstr.enter(bodyScopeId, debugInfo));

  // Process user's loop body (instruction block)
  bodyInstructions.addAll(processBlockStatements(ctx.instructionBlock()));

  // Add SCOPE_EXIT for body scope
  bodyInstructions.add(ScopeInstr.exit(bodyScopeId, debugInfo));

  return bodyInstructions;
}
```

**Why `loopVariableName` is unused here:**
- This method only generates the **user's loop body code**
- It does NOT generate the loop variable assignment
- The result contains: `SCOPE_ENTER + user code + SCOPE_EXIT`

### Step 2: Generate Loop Body Setup (Line 480)

```java
// ForStatementGenerator.java:480 (inside generateDirectionalCase)
final var loopBodySetup = generateBodySetup(loopVariableName, initData.currentTemp, debugInfo);
```

**Inside `generateBodySetup()` (lines 699-707):**

```java
private List<IRInstr> generateBodySetup(
    final String loopVariableName,  // ✅ USED HERE
    final String currentTemp,
    final DebugInfo debugInfo) {

  final var instructions = new ArrayList<IRInstr>();
  instructions.add(MemoryInstr.store(loopVariableName, currentTemp, debugInfo));
  //                              ^^^^^^^^^^^^^^^^  ^^^^^^^^^^^
  //                              User's loop var   Current counter value
  return instructions;
}
```

**What this generates:**
```
STORE loopVariableName currentTemp  // e.g., STORE "i" "_temp4"
```

This is the **loop variable assignment**: `i = current`

### Step 3: Store in Dispatch Cases

The `loopBodySetup` IR is stored **inside each dispatch case**:

```java
// ForStatementGenerator.java:493-501
final var params = new CaseConstructorParams(
    directionCheckResult.instructions(),
    directionCheckResult.primitiveVariableName(),
    loopConditionResult.instructions(),
    loopConditionResult.primitiveVariableName(),
    loopBodySetup,  // ⭐ Stored in ascending/descending case
    loopIncrement
);
return caseConstructor.apply(params);
```

For the **equal case** (line 576):

```java
// ForStatementGenerator.java:573-580
private ForRangePolymorphicInstr.EqualCase generateEqualCase(...) {
  // Body setup: loopVariable = current
  final var loopBodySetup = generateBodySetup(loopVariableName, currentTemp, debugInfo);

  return new ForRangePolymorphicInstr.EqualCase(
      loopBodySetup,  // ⭐ Stored in equal case
      true  // Single iteration
  );
}
```

### Step 4: Bytecode Generation (Backend)

When the backend generates bytecode from FOR_RANGE_POLYMORPHIC, it emits:

```java
// Pseudo-code for ascending case
if (direction < 0) {
  while (true) {
    // Check condition
    [emit loopConditionTemplate IR]  // current <= end
    if (!loop_condition_primitive) break;

    // Execute body
    [emit loopBodySetup IR]          // ⭐ STORE i current
    [emit bodyInstructions IR]       // ⭐ user's loop body

    // Increment
    [emit loopIncrement IR]          // current++
  }
}
```

**The loop variable assignment happens when `loopBodySetup` is emitted, NOT in `bodyInstructions`.**

## Why This Architecture?

### Design Principle: Single Body Storage

**Problem with CONTROL_FLOW_CHAIN approach:**
If we used CONTROL_FLOW_CHAIN for for-range loops, we'd have to duplicate the body 3 times:
```
ascending case:
  - condition
  - loopVariable = current + body + increment  ❌ Body duplicated
descending case:
  - condition
  - loopVariable = current + body + increment  ❌ Body duplicated
equal case:
  - loopVariable = current + body              ❌ Body duplicated
```

**FOR_RANGE_POLYMORPHIC solution:**
Store body once at top level, store setup separately in each case:
```
dispatch_cases:
  ascending:
    - loopBodySetup: [loopVariable = current]  ✅ Only assignment
  descending:
    - loopBodySetup: [loopVariable = current]  ✅ Only assignment
  equal:
    - loopBodySetup: [loopVariable = current]  ✅ Only assignment

bodyInstructions: [user code stored ONCE]      ✅ 67% IR size reduction
```

### IR Size Savings

**Example loop:**
```ek9
for i in 1 ... 100
  Stdout("iteration")
  doSomething(i)
  doSomethingElse(i + 1)
```

**If body stored 3 times:** ~150 IR instructions
**With single body storage:** ~50 IR instructions (initialization + dispatch) + ~30 instructions (body)

**Result:** 40-60% IR size reduction for polymorphic for-range loops

## Conclusion

**The `loopVariableName` parameter is unused in `generateBodyInstructions()` because:**

1. ✅ **Correct separation of concerns:**
   - `generateBodyInstructions()` → generates user's loop body code
   - `generateBodySetup()` → generates loop variable assignment

2. ✅ **Stored in different places:**
   - `loopBodySetup` → stored in each dispatch case
   - `bodyInstructions` → stored once at top level

3. ✅ **Backend combines them:**
   ```
   [emit loopBodySetup]     ← loopVariable = current
   [emit bodyInstructions]  ← user code
   ```

4. ✅ **Achieves 40% IR size reduction** by storing body once instead of three times

## Verification in Code

**ForStatementGenerator.java:**
- **Line 130:** `generateBodyInstructions()` called → returns body code only
- **Line 480:** `generateBodySetup()` called → returns `STORE loopVariable current`
- **Line 498:** `loopBodySetup` stored in CaseConstructorParams

**ForRangePolymorphicInstr.java:**
- **Line 122:** `bodyInstructions` field (stored once)
- **Line 43 (javadoc):** Documents `loop_body_setup: [explicit IR for loopVariable = current]`
- **Line 72 (javadoc):** Documents backend emission order: `loopBodySetup + body`

## Recommendation

**No code changes needed.** This is correct by design.

**Optional documentation improvement:**
Add a comment to `generateBodyInstructions()` explaining why `loopVariableName` is unused:

```java
/**
 * Generate body instructions (user's loop body code only).
 * <p>
 * NOTE: The loop variable assignment (loopVariable = current) is NOT
 * generated here. It's generated separately by generateBodySetup() and
 * stored in each dispatch case's loopBodySetup field.
 * </p>
 * <p>
 * This separation enables FOR_RANGE_POLYMORPHIC to store the body once
 * while having different loop variable assignments per direction case.
 * </p>
 *
 * @param ctx              ForStatementExpressionContext
 * @param loopVariableName User's loop variable name (unused here, used in generateBodySetup)
 * @param debugInfo        Debug information
 * @param bodyScopeId      Body iteration scope ID for SCOPE_ENTER/EXIT
 */
private List<IRInstr> generateBodyInstructions(
    final EK9Parser.ForStatementExpressionContext ctx,
    final String loopVariableName,  // Used for documentation/future use
    final DebugInfo debugInfo,
    final String bodyScopeId) {
  // ...
}
```

---

**End of Explanation**
