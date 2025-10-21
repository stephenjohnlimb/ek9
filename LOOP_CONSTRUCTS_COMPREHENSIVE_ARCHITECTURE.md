# EK9 Loop Constructs: Comprehensive Architecture Analysis

**Date**: 2025-10-18 (Original), Updated: 2025-10-21
**Status**: ~~Architectural Planning~~ **PARTIALLY IMPLEMENTED** (FOR_RANGE complete, others pending)
**Author**: Claude Code + Steve

---

## Executive Summary

This document provides a comprehensive architectural analysis for implementing EK9's loop constructs (while, do-while, for-in, for-range).

**IMPORTANT ARCHITECTURAL UPDATE (2025-10-21):**

- **WHILE and DO-WHILE**: Use `CONTROL_FLOW_CHAIN` infrastructure (as originally planned)
- **FOR_RANGE**: Uses **separate `FOR_RANGE_POLYMORPHIC` instruction type** (NOT CONTROL_FLOW_CHAIN)
  - Specialized architecture for runtime direction detection
  - Three-way polymorphic dispatch (ascending/descending/equal)
  - Complete implementation: `ForRangePolymorphicInstr.java` (IR) + `ForRangePolymorphicAsmGenerator.java` (bytecode)
  - **Status: COMPLETE and WORKING** with tests passing

**Key Finding**: Loops are MORE valuable than switch statements for enabling real programs, should be implemented FIRST. WHILE/DO-WHILE fit naturally into CONTROL_FLOW_CHAIN architecture; FOR_RANGE requires specialized instruction for polymorphic behavior.

---

## Critical Architectural Pattern: Outer Scope Wrapper

**⚠️ MANDATORY for ALL loop constructs** (while, do-while, for-in, for-range):

### The Pattern

Every loop generator MUST create an outer scope that wraps the CONTROL_FLOW_CHAIN instruction, exactly like if/else does:

```java
// Generator creates outer scope
final var loopScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
stackContext.enterScope(loopScopeId, debugInfo, IRFrameType.BLOCK);

instructions.add(ScopeInstr.enter(loopScopeId, debugInfo));

// [Future: Guards process here - in outer scope]
// [Future: Return variable setup here - in outer scope]
// [Future: Iterator/range setup here - in outer scope]

// Create and emit CONTROL_FLOW_CHAIN
final var details = ControlFlowChainDetails.createWhileLoop(..., loopScopeId);
instructions.addAll(generators.controlFlowChainGenerator.apply(details));

// Exit outer scope
instructions.add(ScopeInstr.exit(loopScopeId, debugInfo));
stackContext.exitScope();
```

### Why This Architecture for Loops?

**Loop Outer Scope (_scope_1) holds:**
1. **Guards** (preFlowStatement) - Initialized once, persist across iterations
2. **Return variables** (expression forms) - Accumulate results across iterations
3. **Iterator state** (for-in) - Created once, used each iteration
4. **Range state** (for-range) - `_current`, `_end`, `_step` persist
5. **Condition temporaries** - Registered here, accumulate each iteration

**Iteration Inner Scope (_scope_2) holds:**
- Loop variable (for loops) - Fresh binding each iteration
- Body-local variables - Isolated per iteration
- Iteration-specific cleanup - Runs each time

### Loop Scope Lifecycle

```
SCOPE_ENTER _scope_1              ← Once at loop start
  [Guards executed once]
  [Iterator/range setup once]

  CONTROL_FLOW_CHAIN
    Loop:                         ← Backend loops back here
      [Condition evaluation]      ← Creates temps in _scope_1
      SCOPE_ENTER _scope_2        ← Each iteration
        [Loop variable binding]
        [Body execution]
      SCOPE_EXIT _scope_2         ← Each iteration
      [Backend jumps back]

SCOPE_EXIT _scope_1               ← Once when loop exits
```

**Key difference from if/else**: The condition evaluation and body execution happen MULTIPLE times, but the outer scope persists across all iterations.

### Reference Implementation

See `IfStatementGenerator.java:44-81` for the canonical outer scope wrapper pattern that loops MUST follow.

**⚠️ CRITICAL**: Even simple loops without guards or return values MUST use this architecture for future-proofing.

---

## Part 1: Grammar Analysis

### EK9 Loop Grammar (from EK9.g4)

```antlr4
// WHILE Loop
whileStatementExpression
    : WHILE (preFlowStatement (WITH|THEN))? control=expression
      NL+ INDENT (NL* returningParam)? NL* instructionBlock DEDENT
    | DO preFlowStatement?
      NL+ INDENT (NL* returningParam)? NL* instructionBlock DEDENT
      NL+ directive? WHILE control=expression
    ;

// FOR Loop (over collection/iterator)
forLoop
    : FOR (preFlowStatement (WITH|THEN))? identifier IN expression
    ;

// FOR Range (numeric iteration)
forRange
    : FOR (preFlowStatement (WITH|THEN))? identifier IN range (BY (literal | identifier))?
    ;

// Pre-flow statement (guards)
preFlowStatement
    : (variableDeclaration | assignmentStatement | guardExpression)
    ;

guardExpression
    : identifier op=GUARD expression  // The ?= operator
    ;

// Range construct
range
    : (expression) ELLIPSIS (expression)  // start ... end
    ;
```

### Key Grammar Observations

1. **preFlowStatement is optional** for all loops
2. **Keyword variations**: `WITH` or `THEN` after preFlowStatement
3. **returningParam** - loops can be expressions returning values
4. **DO-WHILE** has preFlowStatement at TOP of body (not before DO)
5. **FOR loops** have loop variable identifier explicitly in grammar
6. **BY clause** for range step (optional, defaults to 1)

---

## Part 2: EK9 Loop Patterns from Examples

### 2.1 While Loop Patterns

#### Simple While
```ek9
while itemIter.hasNext()
  item <- itemIter.next()
  stdout.println(item)
```

#### While with Guard (assignment)
```ek9
while itemIter ?= moreItems.iterator() then itemIter.hasNext()
  item <- itemIter.next()
  stdout.println(item)
```

#### While with Guard (declaration)
```ek9
while itemIterX <- moreItems.iterator() then itemIterX.hasNext()
  item <- itemIterX.next()
  stdout.println(item)
```

#### While as Expression
```ek9
result <- while complete <- false with not complete
  <- rtn <- 0
  rtn++
  complete: rtn == 10
assert result?
```

### 2.2 Do-While Loop Patterns

#### Simple Do-While
```ek9
do
  item <- itemIter.next()
  stdout.println(item)
while itemIter.hasNext()
```

#### Do-While with Guard (at top of body)
```ek9
do itemIter ?= moreItems.iterator()
  if itemIter?
    item <- itemIter.next()
    stdout.println(item)
while itemIter.hasNext()
```

#### Do-While as Expression
```ek9
result <- do complete <- false
  <- rtn <- 0
  rtn++
  complete: rtn == 10
while not complete

assert result?
```

### 2.3 For-In Loop Patterns

#### Simple For-In (collection)
```ek9
for item in ["Alpha", "Beta", "Charlie"]
  stdout.println(item)
```

#### For-In with Guard
```ek9
for toCheck <- provideUsableReturn() with i in [1, 5, 7]
  stdout.println(`Do expect this to compile ${toCheck.name}`)
```

#### For-In with Guarded Assignment
```ek9
toCheck <- UsableAggregate()
for toCheck ?= provideUsableReturn() with i in [1, 5, 7]
  stdout.println(`Maybe see ${toCheck.name}`)
```

#### For-In as Expression
```ek9
result <- for toCheck <- provideUsableReturn() with i in [1, 5, 7]
  <- rtn as String: toCheck.name
  stdout.println(`Value: ${toCheck.name}`)
assert result?
```

### 2.4 For-Range Loop Patterns

#### Simple For-Range
```ek9
for i in 1 ... 9
  stdout.println("Value [" + $i + "]")
```

#### For-Range with Step
```ek9
for i in 9 ... 3 by -3
  stdout.println("Value [" + $i + "]")
```

#### For-Range with Expressions
```ek9
for i in StartValue() ... EndValue()
  stdout.println("Value [" + $i + "]")
```

#### For-Range with Complex Step
```ek9
incrementer <- 6.3
for i in 8.2 ... 30.0 by incrementer
  stdout.println(`Value [${i}]`)
```

#### For-Range with Guard
```ek9
for toCheck <- provideUsableReturn() with i in 1 ... 10
  stdout.println(`Guard value: ${toCheck.name}`)
```

---

## Part 3: Scope Architecture Design

### 3.1 Comparison with If/Else Scope Structure

**Current If/Else (from IfStatementGenerator.java:44-81):**
```
Operation Scope (_call)                    ← Implicit
  └─ Chain Scope (_scope_1)                ← Guards + condition temps
      ├─ Branch Scope (_scope_2)           ← If body
      ├─ Branch Scope (_scope_3)           ← Else-if body
      └─ Branch Scope (_scope_4)           ← Else body
```

**Proven pattern:**
- Outer scope for guards and shared state
- Inner scopes for isolated branch bodies
- Three-tier architecture

### 3.2 Loop Scope Architecture

**Proposed Loop Scope Structure:**

```
Operation Scope (_call)                    ← Implicit, function-level
  └─ Loop Scope (_scope_1)                 ← OUTER: Guards, iterator, range state
      ├─ Guard variables                   ← preFlowStatement: iter <- collection.iterator()
      ├─ Iterator variable (for-in)        ← _iter_N created once
      ├─ Range state (for-range)           ← _current, _end, _step
      └─ Loop Iteration Scope (_scope_2)   ← INNER: Re-executed each iteration
          ├─ Condition evaluation temps    ← Condition temporaries
          ├─ Loop variable (for loops)     ← item in 'for item in ...'
          └─ Body instructions             ← Loop body
```

**Key Differences from If/Else:**
- Loop scope stays ACTIVE across iterations
- Iteration scope enters/exits EACH time through loop
- Loop variable lives in iteration scope (new binding each iteration)
- Guards and setup live in outer loop scope (persist)

### 3.3 Scope Lifecycle Examples

#### While Loop Scope Flow:
```
1. SCOPE_ENTER _scope_1 (loop scope)
2.   [Execute guard: iter <- collection.iterator()]
3.   CONTROL_FLOW_CHAIN chainType: WHILE_LOOP
4.     Loop:
5.       SCOPE_ENTER _scope_2 (iteration scope)
6.         [Evaluate condition: iter.hasNext()]
7.         If true:
8.           [Execute body]
9.       SCOPE_EXIT _scope_2
10.      [Backend jumps back to step 5]
11. SCOPE_EXIT _scope_1
```

#### For-In Loop Scope Flow:
```
1. SCOPE_ENTER _scope_1 (loop scope)
2.   [Execute guard if present]
3.   [Create iterator: _iter = collection.iterator()]
4.   CONTROL_FLOW_CHAIN chainType: FOR_IN_LOOP
5.     Loop:
6.       SCOPE_ENTER _scope_2 (iteration scope)
7.         [Condition: _iter.hasNext()]
8.         If true:
9.           [item = _iter.next()]  ← Loop variable binding
10.          [Execute body]
11.      SCOPE_EXIT _scope_2
12.      [Backend jumps back to step 6]
13. SCOPE_EXIT _scope_1
```

#### Do-While Special Case:
```
1. SCOPE_ENTER _scope_1 (loop scope)
2.   CONTROL_FLOW_CHAIN chainType: DO_WHILE_LOOP
3.     Loop:
4.       SCOPE_ENTER _scope_2 (iteration scope)
5.         [Execute guard if at top: toCheck <- getValue()]
6.         [Execute body]
7.         [Evaluate condition]
8.       SCOPE_EXIT _scope_2
9.       [Backend jumps back if condition true]
10. SCOPE_EXIT _scope_1
```

**Note**: Do-while guard is INSIDE iteration scope because it executes each time.

---

## Part 4: For Loop Decomposition Strategy

### 4.1 For-In Loop Decomposition

**EK9 Source:**
```ek9
for item in collection
  stdout.println(item)
```

**Conceptual Decomposition:**
```ek9
_iter = collection.iterator()
while _iter.hasNext()
  item = _iter.next()
  stdout.println(item)
```

**IR Generation Strategy:**
```
SCOPE_ENTER _scope_1
  // Iterator setup
  _temp1 = LOAD collection
  _iter = CALL _temp1.iterator()

  CONTROL_FLOW_CHAIN chainType: FOR_IN_LOOP
  [
    condition_chain:
    [
      {
        condition_evaluation:
        [
          _temp2 = CALL _iter.hasNext()  // Returns Boolean
          _temp3 = CALL _temp2._true()    // Convert to primitive
        ]
        primitive_condition: _temp3
        body_evaluation:
        [
          SCOPE_ENTER _scope_2
            // Loop variable binding
            item = CALL _iter.next()
            RETAIN item
            SCOPE_REGISTER item, _scope_2

            // Body
            [stdout.println instructions]
          SCOPE_EXIT _scope_2
        ]
      }
    ]
  ]
SCOPE_EXIT _scope_1
```

### 4.2 For-Range Loop Architecture (**IMPLEMENTED**)

**⚠️ ARCHITECTURAL DECISION**: FOR_RANGE uses **separate `FOR_RANGE_POLYMORPHIC` instruction**, NOT `CONTROL_FLOW_CHAIN`.

**Why Separate Instruction:**
1. **Runtime direction detection** - `start <=> end` determines ascending/descending/equal at runtime
2. **Three-way polymorphic dispatch** - Single IR generates three distinct code paths
3. **Type polymorphism** - Works with ANY type implementing `<=>`, `++`, `--`, `<=`, `>=` operators
4. **Unique metadata** - Requires `ScopeMetadata` with `loopScopeId` for label generation

**EK9 Source:**
```ek9
for i in 1 ... 10
  sum := sum + i
```

**FOR_RANGE_POLYMORPHIC IR Structure:**
```
FOR_RANGE_POLYMORPHIC result: <none>
  initialization:
  [
    // Evaluate start expression
    start = LOAD_LITERAL 1
    ASSERT start._isSet()  // Fail-fast validation

    // Evaluate end expression
    end = LOAD_LITERAL 10
    ASSERT end._isSet()    // Fail-fast validation

    // Compute direction: start <=> end
    direction = CALL start._cmp(end)  // Returns -1, 0, or 1

    // Initialize current to start
    current = LOAD start
  ]

  dispatch_cases:
  {
    ascending: {  // When direction < 0
      direction_check: [IR for direction._lt(0)]
      direction_primitive: _temp14
      loop_condition_template: [IR for current._lteq(end)]
      loop_condition_primitive: _temp18
      loop_body_setup: [loopVar = current]
      loop_increment: [current = current._inc()]
    }

    descending: {  // When direction > 0
      direction_check: [IR for direction._gt(0)]
      direction_primitive: _temp22
      loop_condition_template: [IR for current._gteq(end)]
      loop_condition_primitive: _temp26
      loop_body_setup: [loopVar = current]
      loop_increment: [current = current._dec()]
    }

    equal: {  // When direction == 0
      // No direction check (fall through)
      loop_body_setup: [loopVar = current]
      single_iteration: true
    }
  }

  body: [user code - stored ONCE, emitted THREE times by backend]

  scope_metadata: {
    loopScopeId: "_scope_3"           // Unique per loop
    loopVariableName: "i"
    rangeTypeName: "Integer"
  }
```

**JVM Bytecode Pattern (ForRangePolymorphicAsmGenerator):**
```
; Initialization
[evaluate start, end, direction, current with ASSERT validation]

; Three-way dispatch
iload direction_primitive
iflt ascending_label        ; if direction < 0
iload direction_primitive
ifgt descending_label       ; if direction > 0
; fall through to equal

equal_label:
  loopVariable = current
  [body once]
  goto end_label

ascending_label:
asc_loop_start:
  [evaluate current._lteq(end)]
  iload condition_primitive
  ifeq end_label             ; Exit if false
  loopVariable = current
  [body]
  current = current._inc()
  goto asc_loop_start

descending_label:
desc_loop_start:
  [evaluate current._gteq(end)]
  iload condition_primitive
  ifeq end_label
  loopVariable = current
  [body]
  current = current._dec()
  goto desc_loop_start

end_label:
```

**Implementation Files:**
- **IR:** `ForRangePolymorphicInstr.java` (488 lines) - Complete instruction definition
- **Bytecode:** `ForRangePolymorphicAsmGenerator.java` (325 lines) - Three-way dispatch generator
- **Test:** `SimpleForRangeLoopTest.java` + `simpleForRangeLoop.ek9` - **PASSING**

**Status:** ✅ **COMPLETE and WORKING** (as of 2025-10-21)

---

## Part 5: CONTROL_FLOW_CHAIN Adaptation

### 5.1 Current CONTROL_FLOW_CHAIN Design

**From ControlFlowChainDetails.java:**
```java
record ControlFlowChainDetails(
  String resultVariable,              // null for statements
  String chainType,                   // "IF_ELSE", "QUESTION_OPERATOR", etc.
  GuardVariableDetails guardVariableDetails,
  EvaluationVariableDetails evaluationVariableDetails,
  ReturnVariableDetails returnVariableDetails,
  List<ConditionCaseDetails> conditionChain,
  DefaultCaseDetails defaultCaseDetails,
  EnumOptimizationDetails enumOptimizationDetails,
  DebugInfo debugInfo,
  String scopeId
)
```

**Designed for branching** - execute ONE case, then exit.

### 5.2 Loop Adaptation Strategy

**Key insight**: Loops need DIFFERENT backend interpretation, not different IR structure.

**New chainType values:**
- `"WHILE_LOOP"` - Condition at top, loop back if true
- `"DO_WHILE_LOOP"` - Condition at bottom, loop back if true
- `"FOR_IN_LOOP"` - Iterator-based, loop back if hasNext()
- `"FOR_RANGE_LOOP"` - Range-based, loop back if in range

**Same IR structure, different backend semantics:**

```java
// FOR WHILE LOOPS:
ControlFlowChainDetails.createWhileLoop(
  null,                    // No result for statement form
  conditionChain,          // Single ConditionCaseDetails with condition + body
  debugInfo,
  scopeId
);

// Backend sees chainType: "WHILE_LOOP" and generates:
loop_start:
  [evaluate condition]
  ifeq loop_end
  [execute body]
  goto loop_start
loop_end:
```

### 5.3 Backend Dispatch Pattern

**In ControlFlowChainAsmGenerator.java:**
```java
public void generate(final ControlFlowChainInstr instr) {
  switch (instr.getChainType()) {
    case "IF_ELSE" -> generateIfElse(instr);
    case "QUESTION_OPERATOR" -> generateQuestionOperator(instr);
    case "GUARDED_ASSIGNMENT" -> generateGuardedAssignment(instr);
    case "WHILE_LOOP" -> generateWhileLoop(instr);           // NEW
    case "DO_WHILE_LOOP" -> generateDoWhileLoop(instr);     // NEW
    case "FOR_IN_LOOP" -> generateForInLoop(instr);         // NEW
    case "FOR_RANGE_LOOP" -> generateForRangeLoop(instr);   // NEW
    default -> throw new CompilerException("Unknown chain type");
  }
}
```

**Each generator implements loop-specific bytecode:**
```java
private void generateWhileLoop(final ControlFlowChainInstr instr) {
  final var loopStart = new Label();
  final var loopEnd = new Label();

  placeLabel(loopStart);

  // Evaluate condition from single ConditionCaseDetails
  processConditionEvaluation(instr.getConditionChain().get(0));

  // Branch to end if false
  methodVisitor.visitJumpInsn(IFEQ, loopEnd);

  // Execute body
  processBodyEvaluation(instr.getConditionChain().get(0).bodyEvaluation());

  // Jump back to start
  methodVisitor.visitJumpInsn(GOTO, loopStart);

  placeLabel(loopEnd);
}
```

---

## Part 6: Guard Implementation Strategy

### 6.1 Guard Types (from preFlowStatement)

```antlr4
preFlowStatement
    : (variableDeclaration | assignmentStatement | guardExpression)
    ;
```

**Three guard forms:**

1. **Variable Declaration**: `while iter <- collection.iterator() then ...`
2. **Assignment**: `while iter := getValue() with ...`
3. **Guarded Assignment**: `while iter ?= getValue() with ...`

### 6.2 Guard Semantics by Loop Type

**WHILE loops:**
- Guard executes ONCE before first iteration
- Guard variable accessible in condition and body
- Guard lives in loop outer scope

**DO-WHILE loops:**
- Guard at TOP: `do guard <- value() ...` - executes EACH iteration
- Guard at BOTTOM: Not supported by grammar
- Guard lives in iteration scope (re-initialized each time)

**FOR loops:**
- Guard executes ONCE before first iteration
- Guard variable accessible in body
- Guard lives in loop outer scope
- Separate from loop variable

### 6.3 Guard IR Generation Pattern

**From grammar examples:**
```ek9
while toCheck ?= provideUsableReturn() with conditional
  stdout.println(`Value: ${toCheck.name}`)
```

**Generated IR:**
```
SCOPE_ENTER _scope_1                        // Loop scope
  // Guard evaluation
  _temp1 = CALL provideUsableReturn()
  GUARDED_ASSIGNMENT_BLOCK                  // Reuse existing generator!
  [
    condition: toCheck? (null or !_isSet())
    assignment: toCheck := _temp1
  ]

  CONTROL_FLOW_CHAIN chainType: WHILE_LOOP
  [
    condition_chain:
    [
      {
        condition_evaluation: [conditional]
        body_evaluation: [stdout.println...]
      }
    ]
  ]
SCOPE_EXIT _scope_1
```

**Key insight**: Guard handling reuses existing generators:
- `VariableDeclInstrGenerator` for declarations
- `AssignmentStmtGenerator` for assignments
- `GuardedAssignmentBlockGenerator` for guarded assignments

### 6.4 Guard Implementation Complexity

**Complexity: MEDIUM (not high)**

**Why simpler than expected:**
- Reuse existing generators (VariableDecl, Assignment, GuardedAssignment)
- Guards are just preFlowStatement - process FIRST
- Guard scope management already proven (if/else uses same pattern)

**Implementation steps:**
1. Detect preFlowStatement in grammar context
2. Call appropriate generator based on statement type
3. Add instructions to guard scope setup
4. Rest of loop sees guard variables in scope

**Estimate:** 1-2 hours per loop type (already have the infrastructure)

---

## Part 7: Loop as Expression (Returning Values)

### 7.1 Expression Form Grammar

```antlr4
whileStatementExpression
    : WHILE ... NL+ INDENT (NL* returningParam)? NL* instructionBlock DEDENT
    //                       ^^^^^^^^^^^^^^^^^ Optional return parameter
```

### 7.2 Expression Form Examples

```ek9
result <- while complete <- false with not complete
  <- rtn <- 0        // Return parameter declaration
  rtn++
  complete: rtn == 10
assert result?
```

**Semantics:**
- Return variable (`rtn`) lives in iteration scope
- Each iteration can update it
- Last value persists after loop exits
- Result is the final value of return variable

### 7.3 Expression Form IR

```
REFERENCE result, org.ek9.lang::Integer

SCOPE_ENTER _scope_1
  // Guard: complete <- false
  REFERENCE complete, org.ek9.lang::Boolean
  complete = LOAD_LITERAL false

  // Return variable declaration (lives in iteration scope)
  REFERENCE rtn, org.ek9.lang::Integer

  CONTROL_FLOW_CHAIN chainType: WHILE_LOOP_EXPRESSION
  [
    result_variable: result
    return_variable: rtn
    condition_chain:
    [
      {
        condition_evaluation: [not complete]
        body_evaluation:
        [
          SCOPE_ENTER _scope_2
            // Initialize return variable on first iteration
            rtn = LOAD_LITERAL 0

            // Body
            rtn = ADD rtn, 1
            complete = CALL_EQUALS rtn, 10
          SCOPE_EXIT _scope_2
        ]
      }
    ]
  ]

  // Store final return value to result
  STORE result, rtn
SCOPE_EXIT _scope_1
```

### 7.4 Expression Form Complexity

**Complexity: MEDIUM-HIGH**

**Additional requirements:**
- Return variable management
- Initialize on first iteration
- Persist across iterations
- Final value assignment

**Recommendation**: Implement AFTER statement forms working.

---

## Part 8: Implementation Phases

### Phase 1: Simple While Loop (2-3 hours)

**Goal**: Prove loop architecture with simplest case.

**Scope:**
- Simple while (no guards, statement form only)
- IR generation
- Bytecode generation
- Tests

**Files to create:**
- `WhileStatementGenerator.java`
- `WhileLoopAsmGenerator.java`
- Test files

**Validation:**
```ek9
while counter < 10
  stdout.println(counter)
  counter++
```

### Phase 2: Simple Do-While Loop (1-2 hours)

**Goal**: Complete while family.

**Scope:**
- Simple do-while (no guards, statement form)
- IR generation
- Bytecode generation
- Tests

**Files:**
- Update `WhileStatementGenerator` for do-while
- `DoWhileLoopAsmGenerator.java`
- Test files

**Validation:**
```ek9
do
  stdout.println(counter)
  counter++
while counter < 10
```

### Phase 3: Simple For-In Loop (3-4 hours)

**Goal**: Enable collection iteration.

**Scope:**
- For-in over collections/iterators
- Iterator protocol handling
- IR generation with iterator setup
- Bytecode generation
- Tests

**Files:**
- `ForStatementGenerator.java`
- `ForInLoopAsmGenerator.java`
- Test files

**Validation:**
```ek9
for item in ["Alpha", "Beta", "Charlie"]
  stdout.println(item)
```

### Phase 4: Simple For-Range Loop ~~(2-3 hours)~~ ✅ **COMPLETE**

**Goal**: ~~Enable numeric iteration~~ **ACHIEVED**

**Status**: ✅ **IMPLEMENTED and WORKING** (as of 2025-10-21)

**Actual Implementation:**
- **Separate instruction type**: `FOR_RANGE_POLYMORPHIC` (not CONTROL_FLOW_CHAIN)
- Runtime direction detection via `start <=> end`
- Three-way polymorphic dispatch (ascending/descending/equal)
- Type-agnostic (works with ANY type implementing comparison operators)

**Files Created:**
- ✅ `ForRangePolymorphicInstr.java` (488 lines) - IR instruction definition
- ✅ `ForRangePolymorphicAsmGenerator.java` (325 lines) - JVM bytecode generator
- ✅ `SimpleForRangeLoopTest.java` - JUnit test **PASSING**
- ✅ `simpleForRangeLoop.ek9` - Test program with @BYTECODE validation

**Validation:**
```ek9
for i in 1 ... 10
  sum := sum + i
stdout.println(sum)  // Prints 55
```

**Test Status:** All tests passing with correct bytecode generation.

**Architectural Notes:**
- Uses `ScopeMetadata.loopScopeId` for unique label generation
- Handles ASSERT validation for start/end isSet checks
- Maintains stack-empty invariant throughout
- Single body stored in IR, emitted three times in bytecode

### Phase 5: Guards for All Loops (3-4 hours)

**Goal**: Cross-cutting guard support.

**Scope:**
- While guard processing
- Do-while guard processing (top of body)
- For-in guard processing
- For-range guard processing
- Tests for all combinations

**Files:**
- Update all loop generators
- Comprehensive guard tests

**Validation:**
```ek9
while iter <- collection.iterator() then iter.hasNext()
  item <- iter.next()

for guard <- getValue() with i in 1 ... 10
  stdout.println(guard)
```

### Phase 6: Expression Forms (4-6 hours, OPTIONAL)

**Goal**: Loops as expressions.

**Scope:**
- Return parameter handling
- Result variable management
- All loop types as expressions
- Tests

**Validation:**
```ek9
result <- while counter <- 0 with counter < 10
  <- rtn <- ""
  rtn += "."
  counter++
assert result == ".........."
```

---

## Part 9: Complexity Comparison

### 9.1 Loop Implementation Estimates

| Component | Complexity | Time | Notes |
|-----------|------------|------|-------|
| **Simple While** | LOW | 2-3h | Proves architecture |
| **Simple Do-While** | LOW | 1-2h | Reuses while patterns |
| **Simple For-In** | MEDIUM | 3-4h | Iterator protocol |
| **Simple For-Range** | MEDIUM | 2-3h | Range arithmetic |
| **Guards (All)** | MEDIUM | 3-4h | Cross-cutting, reuse generators |
| **Expression Forms** | MEDIUM-HIGH | 4-6h | Return variable complexity |
| **TOTAL (without expressions)** | | **11-16h** | |
| **TOTAL (with expressions)** | | **15-22h** | |

### 9.2 Switch Statement Estimate

| Component | Complexity | Time | Notes |
|-----------|------------|------|-------|
| **Simple Switch** | MEDIUM | 4-5h | Multi-case logic |
| **Expression Cases** | MEDIUM | 2-3h | >, <, matches, contains |
| **Multiple Case Values** | MEDIUM | 2-3h | OR logic |
| **Enum Optimization** | MEDIUM-HIGH | 3-4h | Exhaustiveness, hints |
| **Switch as Expression** | MEDIUM | 2-3h | Result management |
| **TOTAL** | | **13-18h** | |

### 9.3 Value Comparison

**Loops:**
- ✅ **ESSENTIAL** - Cannot write real programs without iteration
- ✅ **Foundation** - Enables all iterative algorithms
- ✅ **Turing complete** - if/else + while = complete language
- ✅ **Validates architecture** - Proves CONTROL_FLOW_CHAIN handles loops

**Switch:**
- ✅ **CONVENIENT** - Nice to have for multi-way branches
- ❌ **Not essential** - Can use if/else-if chains instead
- ⚠️ **Defers iteration** - Every day without loops = limited programs

---

## Part 10: Strategic Recommendation

### 10.1 Implementation Priority

**RECOMMENDED ORDER:**

1. **Simple While Loop** (2-3h)
   - Proves loop architecture
   - Validates CONTROL_FLOW_CHAIN for loops
   - Enables basic iteration

2. **Simple Do-While Loop** (1-2h)
   - Completes while family
   - Different control flow pattern

3. **Simple For-In Loop** (3-4h)
   - Essential for collections
   - Iterator protocol
   - Most common loop type

4. **Simple For-Range Loop** (2-3h)
   - Numeric iteration
   - Range arithmetic

5. **Guards for All Loops** (3-4h)
   - Cross-cutting benefit
   - Real-world loop patterns
   - Implement once, works everywhere

6. **Switch Statement** (13-18h)
   - After loops complete
   - Convenience feature
   - Not blocking real programs

7. **Loop Expression Forms** (4-6h, OPTIONAL)
   - Functional style
   - After statement forms solid

### 10.2 Why Loops Before Switch

**Value Proposition:**
- Loops enable ITERATIVE ALGORITHMS - essential for real programs
- Switch enables MULTI-WAY BRANCHING - can use if/else-if instead
- Every day without loops = developers can't write real programs
- Switch is polish, loops are necessity

**Risk Mitigation:**
- Loops proven architecture pattern (while → do-while → for)
- Incremental validation at each step
- Guards implemented once for all
- Lower complexity per component

**Time Investment:**
- Loops (without expression): 11-16 hours
- Switch: 13-18 hours
- Similar effort, MUCH higher value for loops

### 10.3 Alternative: Guards First (If Staying with If/Else)

If Steve prefers to stay with branching constructs:

**Implement Guards for If/Else FIRST** (1-2h)
- Low risk
- Completes if/else family
- Foundation for switch guards
- Then do switch

**But this defers essential iteration capability.**

---

## Part 11: Key Architectural Decisions

### Decision 1: Use CONTROL_FLOW_CHAIN for Loops

**DECISION: YES**

**Rationale:**
- ✅ Unified IR across all control flow
- ✅ Consistent patterns
- ✅ Backend dispatch based on chainType
- ✅ Proven with if/else/question/guarded assignment

**Implementation:**
- Add new chainType values: WHILE_LOOP, DO_WHILE_LOOP, FOR_IN_LOOP, FOR_RANGE_LOOP
- Backend generates loop-specific bytecode based on type
- Same IR structure, different backend interpretation

### Decision 2: Three-Tier Scope Architecture

**DECISION: Loop Outer Scope + Iteration Inner Scope + Body Scope**

**Rationale:**
- ✅ Matches proven if/else pattern
- ✅ Guards live in outer scope (persist)
- ✅ Iteration scope re-executes (condition + loop var)
- ✅ Clean memory management

**Structure:**
```
_scope_1: Loop outer scope (guards, iterator, range state)
  _scope_2: Iteration scope (condition temps, loop variable, body)
```

### Decision 3: For Loop Decomposition in IR

**DECISION: Decompose to iterator/range in IR, not backend**

**Rationale:**
- ✅ Backend-agnostic
- ✅ Optimization opportunities in IR
- ✅ Clear semantics
- ✅ LLVM and JVM both see explicit iteration

**Implementation:**
- For-in: Generate iterator.hasNext() loop in IR
- For-range: Generate range boundary check in IR
- Backend just generates loop bytecode

### Decision 4: Guard Implementation After Basic Loops

**DECISION: Implement guards AFTER all basic loop forms working**

**Rationale:**
- ✅ Lower risk - basic loops proven first
- ✅ Cross-cutting benefit - implement once for all
- ✅ Reuse existing generators (VariableDecl, Assignment, GuardedAssignment)
- ✅ Clean separation of concerns

**Timeline:**
1. Basic loops (8-12h)
2. Guards for all (3-4h)
3. Total: 11-16h

---

## Part 12: Success Criteria

### Deliverables

**Phase 1-4 (Basic Loops):**
- ✅ While loops compile and execute
- ✅ Do-while loops compile and execute
- ✅ For-in loops compile and execute
- ✅ For-range loops compile and execute
- ✅ Correct bytecode generated (labels, jumps)
- ✅ All scope management correct
- ✅ Memory management correct (no leaks)

**Phase 5 (Guards):**
- ✅ Variable declaration guards work
- ✅ Assignment guards work
- ✅ Guarded assignment (?=) works
- ✅ Guards work with all loop types
- ✅ Guard scope management correct

**Phase 6 (Expression Forms, OPTIONAL):**
- ✅ Loops as expressions return values
- ✅ Return variable management correct
- ✅ Last value correctly assigned to result

### Testing Strategy

**For Each Loop Type:**
1. Simple case (no guards, literal values)
2. Complex condition (function calls, operators)
3. Nested loops
4. Guard combinations (all three types)
5. Edge cases (empty collections, zero iterations)

**Bytecode Validation:**
1. Correct loop labels
2. Correct condition evaluation
3. Correct backward jumps
4. Correct exit paths
5. Stack frame management

---

## Part 13: FINAL RECOMMENDATION

**IMPLEMENT LOOPS BEFORE SWITCH**

**Implementation Order:**
1. Simple While Loop (2-3h)
2. Simple Do-While Loop (1-2h)
3. Simple For-In Loop (3-4h)
4. Simple For-Range Loop (2-3h)
5. Guards for All Loops (3-4h)
6. Switch Statement (AFTER loops complete)

**Total Time:** 11-16 hours for complete loop support (statement forms)

**Rationale:**
- **ESSENTIAL vs CONVENIENT** - Loops enable real programs, switch is syntactic sugar
- **INCREMENTAL VALIDATION** - Each loop type builds on previous
- **ARCHITECTURE PROOF** - Validates CONTROL_FLOW_CHAIN handles loops
- **FOUNDATION FIRST** - Core primitives before polish features
- **SIMILAR EFFORT** - Loops 11-16h, Switch 13-18h, but loops have 10x value

**Next Step:** Start with simple while loop, prove architecture, then incrementally add remaining loop types.

---

## Appendix A: Grammar Quick Reference

```antlr4
// While
while [guard (with|then)?] condition
  body

// Do-While
do [guard?]
  body
while condition

// For-In
for [guard (with|then)?] variable in expression
  body

// For-Range
for [guard (with|then)?] variable in start...end [by step]
  body

// Guards
guard: variable <- expression        // Declaration
guard: variable := expression        // Assignment
guard: variable ?= expression        // Guarded assignment
```

## Appendix B: Scope Management Cheat Sheet

**Loop Outer Scope (_scope_1):**
- Guards (persist across iterations)
- Iterator variable (for-in)
- Range state (for-range)
- SCOPE_ENTER once, SCOPE_EXIT once

**Loop Iteration Scope (_scope_2):**
- Condition evaluation temporaries
- Loop variable (for loops)
- Body instructions
- SCOPE_ENTER each iteration, SCOPE_EXIT each iteration

**Do-While Special Case:**
- Guard at top of body → lives in iteration scope
- Re-executed each iteration

---

## Appendix C: Memory Management in Loops (ARC Pattern)

**For comprehensive ARC documentation, see: [EK9_ARC_MEMORY_MANAGEMENT.md](EK9_ARC_MEMORY_MANAGEMENT.md)**

### Critical Rule: Variable Registration Happens ONCE at Declaration

**Variables are registered to scopes at declaration ONLY, not at reassignment.**

### Correct Pattern for Loop Variables

**Loop Variable Declaration (Before Loop):**
```
SCOPE_ENTER _scope_1
REFERENCE counter, Integer
_temp1 = LOAD_LITERAL 0
RETAIN _temp1
SCOPE_REGISTER _temp1, _scope_1      // temp registered to _scope_1
STORE counter, _temp1
RETAIN counter
SCOPE_REGISTER counter, _scope_1     // counter registered to _scope_1 ✅
```

**Loop Variable Reassignment (Inside Loop Body):**
```
SCOPE_ENTER _scope_4                 // Loop iteration scope
_temp6 = CALL _add(...)
RETAIN _temp6
SCOPE_REGISTER _temp6, _scope_4      // temp registered to _scope_4

RELEASE counter                      // Release old value
STORE counter, _temp6                // Store new value
RETAIN counter                       // Retain new value
// NO SCOPE_REGISTER                 // ✅ counter already registered to _scope_1!

SCOPE_EXIT _scope_4                  // Releases _temp6, NOT counter
```

**Scope Exit (After Loop):**
```
SCOPE_EXIT _scope_1
// Releases _temp1 (original value)
// Releases counter (CURRENT value - e.g., Integer(10) after 10 iterations) ✅
```

### Why This Matters

**WRONG (Old Pattern - Use-After-Free Bug):**
```
// In loop body
RELEASE counter
STORE counter, _temp6
RETAIN counter
SCOPE_REGISTER counter, _scope_4     // ❌ DOUBLE REGISTRATION!

SCOPE_EXIT _scope_4
// Releases counter → frees Integer(N+1)
// Next iteration: LOAD counter → USE AFTER FREE! ❌
```

**CORRECT (Fixed Pattern):**
```
// In loop body
RELEASE counter
STORE counter, _temp6
RETAIN counter
// NO SCOPE_REGISTER                 // ✅ Registered to _scope_1 already

SCOPE_EXIT _scope_4
// Does NOT release counter → Integer(N+1) survives ✅
// Next iteration: LOAD counter → valid access ✅
```

### Memory Management Semantics

**SCOPE_REGISTER tracks variable NAMES, not object addresses:**
- When a scope exits, it releases whatever the variable **currently** points to
- Not what it pointed to at registration time
- This is why reassignment works: _scope_1 releases Integer(10), not Integer(0)

**Object Lifecycle Example (10 iterations):**
```
Iteration 0:
  counter → Integer(0), refcount=2

Iteration 1:
  Create Integer(1), refcount=0
  RETAIN _temp6 → Integer(1) refcount=1
  RELEASE counter → Integer(0) refcount=1
  STORE counter → counter now points to Integer(1)
  RETAIN counter → Integer(1) refcount=2
  SCOPE_EXIT _scope_4 → Release _temp6 → Integer(1) refcount=1

Iteration 2-9: Similar pattern

Iteration 10:
  counter → Integer(10), refcount=1

SCOPE_EXIT _scope_1:
  Release _temp1 → Integer(0) refcount=0, freed
  Release counter → Integer(10) refcount=0, freed ✅
```

### For-Loop Iteration Variables

**For-in loops:** Iterator variable is NOT reassigned, it's redeclared each iteration
```
for item in collection
  // item is a NEW declaration in each iteration scope
  // Gets SCOPE_REGISTER in iteration scope ✅
```

**For-range loops:** Index variable follows reassignment pattern
```
for idx in 0...10
  // idx declared before loop in _scope_1
  // Reassigned each iteration (NO SCOPE_REGISTER in iteration scope)
```

### Summary

**Declaration (First Assignment):**
- RETAIN + SCOPE_REGISTER ✅

**Reassignment (Subsequent Assignments):**
- RELEASE + RETAIN (NO SCOPE_REGISTER) ✅

**Temporary Variables:**
- RETAIN + SCOPE_REGISTER (in their declaring scope) ✅

**Scope Exit:**
- Releases all registered variables ✅
- Releases CURRENT value, not registration-time value ✅

---

## Appendix D: While vs Do-While - Critical Implementation Differences

### Comparative Architecture Analysis

Based on successful implementation of both while and do-while loops, this appendix documents the critical semantic and implementation differences.

| Aspect | While Loop | Do-While Loop |
|--------|-----------|---------------|
| **Execution Order** | Condition FIRST, body SECOND | Body FIRST, condition SECOND |
| **JVM Branch Instruction** | `IFEQ` (if equal/false) | `IFNE` (if not equal/true) |
| **Branch Direction** | Forward jump to loop_end if FALSE | Backward jump to loop_start if TRUE |
| **Minimum Executions** | 0 (may skip body entirely) | 1 (always executes once) |
| **Label Strategy** | Needs both loop_start AND loop_end labels | Only needs loop_start label (fall through on false) |
| **IR Body Processing** | After condition evaluation | Before condition evaluation |
| **Guard Initialization** | Can execute before any iteration | Already inside first iteration |

### JVM Bytecode Pattern Comparison

**While Loop Bytecode**:
```
loop_start:
  [evaluate condition]        // Check first
  iload <condition_var>
  ifeq loop_end              // Jump FORWARD if FALSE (zero)
  [execute body]             // Body may never execute
  goto loop_start            // Explicit backward jump
loop_end:                    // Explicit end label needed
```

**Do-While Loop Bytecode**:
```
loop_start:
  [execute body]             // Body always executes first
  [evaluate condition]        // Check after body
  iload <condition_var>
  ifne loop_start            // Jump BACKWARD if TRUE (non-zero)
                             // Fall through if false (no explicit end label)
```

### Key Implementation Insight: Opposite Branch Logic

The opposite branch logic (`ifne` vs `ifeq`) is **NOT arbitrary**—it's fundamental to the semantics:

**While Loop**: "If condition is FALSE, exit loop"
- Evaluation: Check condition → decide whether to enter/continue
- Branch: `ifeq loop_end` → Jump FORWARD when false
- Natural reading: "exit if not true"

**Do-While Loop**: "If condition is TRUE, continue loop"
- Evaluation: Execute body → check if should repeat
- Branch: `ifne loop_start` → Jump BACKWARD when true
- Natural reading: "continue while true"

This mirrors natural language: **"do X while Y"** means **"keep doing if Y is true"**, not "stop if Y is false".

### IR Generation Differences

**While Loop IR** (`WhileStatementGenerator.java:87-133`):
```java
// 1. Condition evaluation scope
final var conditionScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
stackContext.enterScope(conditionScopeId, debugInfo, IRFrameType.BLOCK);
// ... generate condition evaluation ...
stackContext.exitScope();

// 2. Body iteration scope
final var bodyScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
stackContext.enterScope(bodyScopeId, debugInfo, IRFrameType.BLOCK);
// ... generate body ...
stackContext.exitScope();

// 3. Create control flow chain: condition BEFORE body
final var conditionDetails = new ConditionCaseDetails(
    conditionScopeId,
    conditionEvaluation,
    primitiveCondition,
    bodyScopeId,
    bodyEvaluation);
```

**Do-While Loop IR** (`WhileStatementGenerator.java:164-259`):
```java
// 1. Body iteration scope FIRST
final var bodyScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
stackContext.enterScope(bodyScopeId, debugInfo, IRFrameType.BLOCK);
// ... generate body ...
stackContext.exitScope();

// 2. Condition evaluation scope SECOND
final var conditionScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
stackContext.enterScope(conditionScopeId, debugInfo, IRFrameType.BLOCK);
// ... generate condition evaluation ...
stackContext.exitScope();

// 3. Create control flow chain: body BEFORE condition
final var conditionDetails = new ConditionCaseDetails(
    conditionScopeId,
    conditionEvaluation,
    primitiveCondition,
    bodyScopeId,
    bodyEvaluation);
```

**Critical Observation**: The IR scope processing order directly determines bytecode execution order.

### Bytecode Generation Differences

**While Loop Bytecode Generator** (`WhileLoopAsmGenerator.java:64-92`):
```java
public void generate(final ControlFlowChainInstr instr) {
    final var loopStartLabel = createControlFlowLabel("while_start", instr.getScopeId());
    final var loopEndLabel = createControlFlowLabel("while_end", instr.getScopeId());
    final var conditionCase = instr.getConditionChain().getFirst();

    placeLabel(loopStartLabel);
    // Stack: empty

    processConditionEvaluation(conditionCase.conditionEvaluation());
    // Stack: empty (condition result in local variable)

    branchIfFalse(conditionCase.primitiveCondition(), loopEndLabel);  // Forward if FALSE
    // Stack: empty (both paths - fall through or branch forward)

    processBodyEvaluation(conditionCase.bodyEvaluation());
    // Stack: empty

    jumpTo(loopStartLabel);  // Explicit backward jump
    // Stack: empty

    placeLabel(loopEndLabel);  // Explicit end label
    // Stack: empty
}
```

**Do-While Loop Bytecode Generator** (`DoWhileLoopAsmGenerator.java:64-92`):
```java
public void generate(final ControlFlowChainInstr instr) {
    final var loopStartLabel = createControlFlowLabel("do_while_start", instr.getScopeId());
    final var conditionCase = instr.getConditionChain().getFirst();

    placeLabel(loopStartLabel);
    // Stack: empty

    processBodyEvaluation(conditionCase.bodyEvaluation());  // Body FIRST
    // Stack: empty

    processConditionEvaluation(conditionCase.conditionEvaluation());  // Condition AFTER
    // Stack: empty (condition result in local variable)

    branchIfTrue(conditionCase.primitiveCondition(), loopStartLabel);  // Backward if TRUE
    // Stack: empty (both paths - fall through or branch back)

    // Fall through to loop end (no explicit label needed)
    // Stack: empty
}
```

### Helper Method Semantic Encoding

The `AbstractControlFlowAsmGenerator` base class provides semantic helper methods:

```java
// While loop: exit if condition is false
protected void branchIfFalse(String primitiveCondition, String targetLabel) {
    final var condition = loadLocalVariable(primitiveCondition);
    mv.visitJumpInsn(IFEQ, labelMap.get(targetLabel));  // Jump if zero (false)
}

// Do-while loop: continue if condition is true
protected void branchIfTrue(String primitiveCondition, String targetLabel) {
    final var condition = loadLocalVariable(primitiveCondition);
    mv.visitJumpInsn(IFNE, labelMap.get(targetLabel));  // Jump if non-zero (true)
}
```

**Design Principle**: Never use raw ASM instructions (`mv.visitJumpInsn(IFEQ, ...)`) directly in control flow generators. Always use semantic helpers that make intent clear.

### Testing Validation

Both loop types validated with:
- **IR Tests**: Verify correct IR structure with appropriate chain_type
- **Bytecode Tests**: Verify correct JVM branch instructions (`ifeq` vs `ifne`)
- **Runtime Tests**: Verify correct execution semantics (0+ vs 1+ iterations)

**While Loop Validation**:
```ek9
counter <- 0
while counter < 10  // May execute 0 times if counter starts >= 10
  counter: counter + 1
stdout.println(counter)  // Outputs: 10
```

**Do-While Loop Validation**:
```ek9
counter <- 0
do
  counter: counter + 1  // Executes at least once
while counter < 5
stdout.println(counter)  // Outputs: 5
```

### Implementation Checklist

When implementing a new loop type:

- [ ] Determine if condition is checked BEFORE or AFTER body
- [ ] Choose appropriate branch instruction (`ifeq` for forward-if-false, `ifne` for backward-if-true)
- [ ] Use semantic helper methods (`branchIfFalse()` vs `branchIfTrue()`)
- [ ] Process IR scopes in correct order (condition-then-body vs body-then-condition)
- [ ] Create appropriate labels (both start+end vs start-only)
- [ ] Verify bytecode with `@BYTECODE` directive showing correct branch instruction
- [ ] Test runtime behavior (0+ executions vs 1+ executions)

---

**Document Complete: Ready for Implementation**

