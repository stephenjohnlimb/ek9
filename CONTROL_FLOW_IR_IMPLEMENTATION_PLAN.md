# Control Flow IR Generation Implementation Plan

**Date**: 2025-10-14
**Status**: In Progress
**Author**: Claude Code + Steve

## Executive Summary

This document outlines the comprehensive implementation plan for adding control flow structures (if/else, switch, while, for) to the EK9 IR generation system. All control flow constructs will use the unified `CONTROL_FLOW_CHAIN` IR instruction, enabling consistent optimization across both JVM and LLVM backends.

---

## Architecture Overview

### Core Design Principles

1. **Unified CONTROL_FLOW_CHAIN**: All control flow maps to a single IR instruction type
2. **GeneratorSet Pattern**: All new generators use dependency injection via GeneratorSet
3. **Pre-computed Paths**: Complete evaluation paths computed at IR generation time
4. **Backend Agnostic**: IR works equally well for stack-based JVM and SSA-based LLVM

### Key Components

- **CONTROL_FLOW_CHAIN**: Unified IR instruction defined in `IROpcode.java`
- **ControlFlowChainGenerator**: Existing generator that creates CONTROL_FLOW_CHAIN instructions
- **ControlFlowChainDetails**: Data structure containing all control flow information
- **GeneratorSet**: Dependency injection container for all generators

### Critical Architectural Pattern: Outer Scope Wrapper

**⚠️ MANDATORY for ALL control flow constructs** (if/else, switch, while, for, try/catch):

#### The Pattern

Every control flow construct generator MUST create an outer scope that wraps the CONTROL_FLOW_CHAIN instruction:

```java
// Generator creates outer scope
final var outerScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
stackContext.enterScope(outerScopeId, debugInfo, IRFrameType.BLOCK);

instructions.add(ScopeInstr.enter(outerScopeId, debugInfo));

// [Future: Guards process here - in outer scope]
// [Future: Return variable setup here - in outer scope]
// [Future: Evaluation variable setup here - in outer scope]

// Create and emit CONTROL_FLOW_CHAIN
final var details = ControlFlowChainDetails.create*(..., outerScopeId);
instructions.addAll(generators.controlFlowChainGenerator.apply(details));

// Exit outer scope
instructions.add(ScopeInstr.exit(outerScopeId, debugInfo));
stackContext.exitScope();
```

#### Why This Architecture?

**Outer scope holds:**
1. **Guards** (preFlowStatement variables) - Available to all branches/iterations
2. **Return variables** (expression forms) - Accumulate results across branches/iterations
3. **Evaluation variables** (switch) - Evaluated once, used by all cases
4. **Condition temporaries** - Registered here for cleanup

**Inner scopes hold:**
- Branch/iteration-specific locals
- Isolated execution context
- Per-branch/per-iteration cleanup

#### Three-Tier Scope Architecture

```
Operation Scope (_call)           ← Implicit, function-level
  └─ Outer Scope (_scope_1)       ← Generator creates via SCOPE_ENTER/EXIT
      ├─ Guards                    ← Persistent across construct
      ├─ Return variables          ← Accumulate results
      ├─ Evaluation variables      ← Evaluated once
      ├─ Condition temporaries     ← Registered for cleanup
      └─ CONTROL_FLOW_CHAIN
          └─ Inner Scopes          ← Branch/iteration scopes
              (_scope_2, _scope_3, ...)
```

#### Consistency Across Constructs

**If/Else:**
```
SCOPE_ENTER _scope_1                ← Outer: guards, condition temps
  CONTROL_FLOW_CHAIN IF_ELSE_IF
    Branch scopes: _scope_2, _scope_3, ...
SCOPE_EXIT _scope_1
```

**While/For:**
```
SCOPE_ENTER _scope_1                ← Outer: guards, return var, iterator
  CONTROL_FLOW_CHAIN WHILE_LOOP
    Iteration scope: _scope_2       ← Re-entered each iteration
SCOPE_EXIT _scope_1
```

**Switch:**
```
SCOPE_ENTER _scope_1                ← Outer: guards, eval var, return var
  CONTROL_FLOW_CHAIN SWITCH
    Case scopes: _scope_2, _scope_3, ...
SCOPE_EXIT _scope_1
```

**Try/Catch:**
```
SCOPE_ENTER _scope_1                ← Outer: resource guards, return var
  CONTROL_FLOW_CHAIN TRY_CATCH
    Try scope: _scope_2
    Catch scopes: _scope_3, _scope_4, ...
SCOPE_EXIT _scope_1
```

#### Implementation Checklist

When creating ANY control flow generator:

- [ ] Create outer scope with `generateScopeId()`
- [ ] Enter scope context with `enterScope()`
- [ ] Add `SCOPE_ENTER` instruction to IR
- [ ] Process guards/setup in outer scope (if applicable)
- [ ] Create CONTROL_FLOW_CHAIN with `outerScopeId`
- [ ] Add `SCOPE_EXIT` instruction to IR
- [ ] Exit scope context with `exitScope()`

#### Why This Matters

1. **Future-proof**: Even simple forms (no guards) use correct architecture
2. **Consistency**: All control flow follows same pattern
3. **Guards slot in**: When adding guard support, just process preFlowStatement in outer scope
4. **Expression forms slot in**: When adding return values, setup goes in outer scope
5. **Memory management**: Clear ownership of temporaries and variables

#### Reference Implementation

See `IfStatementGenerator.java:44-81` for the canonical pattern.

**⚠️ NEVER bypass this pattern** - even for "simple" cases without guards or return values.
The outer scope wrapper is MANDATORY architectural infrastructure.

---

## Phase 1: If/Else Statement Implementation (Immediate Work)

### 1.1 Test File Structure

Create directory: `compiler-main/src/test/resources/examples/irGeneration/controlFlow/`

#### File: `simpleIf.ek9`
```ek9
#!ek9
defines module controlFlow

  defines function

    @IR: IR_GENERATION: FUNCTION: "controlFlow::simpleIf": `
    ConstructDfn: controlFlow::simpleIf(org.ek9.lang::Integer)->org.ek9.lang::String
    OperationDfn: controlFlow::simpleIf.c_init()->org.ek9.lang::Void
    BasicBlock: _entry_1
    RETURN
    OperationDfn: controlFlow::simpleIf.i_init()->org.ek9.lang::Void
    BasicBlock: _entry_1
    RETURN
    OperationDfn: controlFlow::simpleIf.simpleIf()->controlFlow::simpleIf
    BasicBlock: _entry_1
    CALL (controlFlow::simpleIf)this.i_init() [pure=false, complexity=0]
    RETURN this
    OperationDfn: controlFlow::simpleIf._call(org.ek9.lang::Integer)->org.ek9.lang::String
    BasicBlock: _entry_1
    REFERENCE value, org.ek9.lang::Integer
    REFERENCE result, org.ek9.lang::String
    SCOPE_ENTER _scope_1
    _temp1 = CONTROL_FLOW_CHAIN
    [
      chain_type: IF_ELSE
      condition_chain:
      [
        {
          condition_evaluation:
          [
            _temp2 = LOAD value
            RETAIN _temp2
            SCOPE_REGISTER _temp2, _scope_1
            _temp3 = LOAD_LITERAL 10, org.ek9.lang::Integer
            RETAIN _temp3
            SCOPE_REGISTER _temp3, _scope_1
            _temp4 = CALL (org.ek9.lang::Boolean)_temp2._gt(_temp3)
            RETAIN _temp4
            SCOPE_REGISTER _temp4, _scope_1
            _temp5 = CALL (org.ek9.lang::Boolean)_temp4._true()
          ]
          condition_result: _temp4
          primitive_condition: _temp5
          body_evaluation:
          [
            _temp6 = LOAD_LITERAL "High", org.ek9.lang::String
            RETAIN _temp6
            SCOPE_REGISTER _temp6, _scope_1
            RELEASE result
            STORE result, _temp6
            RETAIN result
            SCOPE_REGISTER result, _scope_1
          ]
          body_result: null
          scope_id: _scope_1
        }
      ]
      default_case: none
      scope_id: _scope_1
    ]
    SCOPE_EXIT _scope_1
    RETURN result`
    simpleIf()
      -> value as Integer
      <- result <- String()

      if value > 10
        result: "High"

//EOF
```

#### File: `ifElse.ek9`
```ek9
#!ek9
defines module controlFlow

  defines function

    @IR: IR_GENERATION: FUNCTION: "controlFlow::ifElse": `
    [Similar structure to simpleIf but with default_case populated]
    ...
    default_case:
    {
      body_evaluation:
      [
        _temp7 = LOAD_LITERAL "Low", org.ek9.lang::String
        RETAIN _temp7
        SCOPE_REGISTER _temp7, _scope_1
        RELEASE result
        STORE result, _temp7
        RETAIN result
        SCOPE_REGISTER result, _scope_1
      ]
      default_result: null
    }
    ...`
    ifElse()
      -> value as Integer
      <- result <- String()

      if value > 10
        result: "High"
      else
        result: "Low"

//EOF
```

#### File: `ifElseIfChain.ek9`
```ek9
#!ek9
defines module controlFlow

  defines function

    @IR: IR_GENERATION: FUNCTION: "controlFlow::ifElseIfChain": `
    [Multiple ConditionCaseDetails in condition_chain]
    ...`
    ifElseIfChain()
      -> value as Integer
      <- result <- String()

      if value > 100
        result: "Very High"
      else if value > 50
        result: "High"
      else if value > 10
        result: "Medium"
      else
        result: "Low"

//EOF
```

#### File: `ifWithComplexCondition.ek9`
```ek9
#!ek9
defines module controlFlow

  defines function

    @IR: IR_GENERATION: FUNCTION: "controlFlow::ifWithComplexCondition": `
    [Shows LOGICAL_AND_BLOCK nested in condition evaluation]
    ...`
    ifWithComplexCondition()
      -> arg0 as Boolean
      -> arg1 as Boolean
      <- result <- String()

      if arg0 and arg1
        result: "Both true"
      else
        result: "Not both true"

//EOF
```

### 1.2 IfStatementGenerator Implementation

**File**: `compiler-main/src/main/java/org/ek9lang/compiler/phase7/generator/IfStatementGenerator.java`

```java
package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.data.ConditionCaseDetails;
import org.ek9lang.compiler.ir.data.ControlFlowChainDetails;
import org.ek9lang.compiler.ir.data.DefaultCaseDetails;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.calls.CallDetailsForIsTrue;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Generates IR for if/else statements using CONTROL_FLOW_CHAIN.
 * Uses GeneratorSet pattern for dependency injection.
 */
public final class IfStatementGenerator extends AbstractGenerator
    implements Function<EK9Parser.IfStatementContext, List<IRInstr>> {

    private final GeneratorSet generators;
    private final CallDetailsForIsTrue callDetailsForIsTrue = new CallDetailsForIsTrue();

    public IfStatementGenerator(IRGenerationContext stackContext, GeneratorSet generators) {
        super(stackContext);
        AssertValue.checkNotNull("GeneratorSet cannot be null", generators);
        this.generators = generators;
    }

    @Override
    public List<IRInstr> apply(EK9Parser.IfStatementContext ctx) {
        AssertValue.checkNotNull("IfStatementContext cannot be null", ctx);

        final var debugInfo = stackContext.createDebugInfo(ctx);
        final var scopeId = stackContext.currentScopeId();

        // Process all if/else if conditions into ConditionCaseDetails
        final var conditionChain = new ArrayList<ConditionCaseDetails>();

        for (var ifControlBlock : ctx.ifControlBlock()) {
            conditionChain.add(processIfControlBlock(ifControlBlock));
        }

        // Process else block if present
        List<IRInstr> defaultBodyEvaluation = List.of();
        if (ctx.elseOnlyBlock() != null) {
            defaultBodyEvaluation = processElseOnlyBlock(ctx.elseOnlyBlock());
        }

        // Create CONTROL_FLOW_CHAIN details
        final var details = ControlFlowChainDetails.createIfElse(
            null, // No result for statement form
            conditionChain,
            defaultBodyEvaluation,
            null, // No default result for statement form
            debugInfo,
            scopeId
        );

        // Use ControlFlowChainGenerator to generate IR
        return generators.controlFlowChainGenerator().apply(details);
    }

    private ConditionCaseDetails processIfControlBlock(EK9Parser.IfControlBlockContext ctx) {
        final var debugInfo = stackContext.createDebugInfo(ctx);
        final var scopeId = stackContext.currentScopeId();

        // Check for preFlowAndControl (guard variables)
        if (ctx.preFlowAndControl() != null && ctx.preFlowAndControl().preFlowStatement() != null) {
            // TODO: Handle guard variables - future enhancement
            throw new CompilerException("Guard variables in if not yet implemented");
        }

        // Process condition expression
        final var conditionResult = stackContext.generateTempName();
        final var conditionDetails = new VariableDetails(conditionResult, debugInfo);
        final var conditionEvaluation = new ArrayList<>(
            generators.exprGenerator().apply(
                new ExprProcessingDetails(ctx.preFlowAndControl().expression(), conditionDetails)
            )
        );

        // Add primitive boolean conversion for backend optimization
        final var primitiveCondition = stackContext.generateTempName();
        conditionEvaluation.add(CallInstr.operator(
            new VariableDetails(primitiveCondition, debugInfo),
            callDetailsForIsTrue.apply(conditionResult)
        ));

        // Process body block
        final var bodyEvaluation = generators.blockStmtGenerator().apply(ctx.block());

        return ConditionCaseDetails.createExpression(
            scopeId,
            conditionEvaluation,
            conditionResult,    // EK9 Boolean result
            primitiveCondition, // primitive boolean for backend
            bodyEvaluation,
            null // No result for statement form
        );
    }

    private List<IRInstr> processElseOnlyBlock(EK9Parser.ElseOnlyBlockContext ctx) {
        return generators.blockStmtGenerator().apply(ctx.block());
    }
}
```

### 1.3 GeneratorSet Integration

**File**: `compiler-main/src/main/java/org/ek9lang/compiler/phase7/generator/GeneratorSet.java`

Add to existing class:
```java
// Add field declaration
private IfStatementGenerator ifStatementGenerator;

// Add to initialization method (call after controlFlowChainGenerator is created)
void initializeControlFlowGenerators() {
    this.ifStatementGenerator = new IfStatementGenerator(stackContext, this);
    // Future: switchStatementGenerator, whileStatementGenerator, forStatementGenerator
}

// Add accessor method
public IfStatementGenerator ifStatementGenerator() {
    return ifStatementGenerator;
}
```

### 1.4 StmtInstrGenerator Integration

**File**: `compiler-main/src/main/java/org/ek9lang/compiler/phase7/generator/StmtInstrGenerator.java`

Line 61-63, replace:
```java
// Change from:
if (ctx.ifStatement() != null) {
    throw new CompilerException("If not implemented");
}

// To:
if (ctx.ifStatement() != null) {
    instructions.addAll(generators.ifStatementGenerator().apply(ctx.ifStatement()));
}
```

### 1.5 JUnit Test

**File**: `compiler-main/src/test/java/org/ek9lang/compiler/ir/ControlFlowIRTest.java`

```java
package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Focused on testing IR generation for control flow statements.
 * Each test file contains a single function to keep IR output manageable.
 */
class ControlFlowIRTest extends AbstractIRGenerationTest {

  public ControlFlowIRTest() {
    super("/examples/irGeneration/controlFlow",
        List.of(new SymbolCountCheck(4, "controlFlow", 4)),
        false, false, false);
  }
}
```

---

## Phase 2: Switch Statement Implementation

### 2.1 Test Files Structure

Create in `controlFlow/` directory:

#### `simpleSwitch.ek9`
- Basic switch with literal cases
- Shows evaluation variable setup
- Multiple case values

#### `switchWithExpressions.ek9`
- Complex case expressions (>, <, matches, contains)
- Shows expression evaluation in cases

#### `exhaustiveEnumSwitch.ek9`
- Enum type switching
- EnumOptimizationDetails for backend hints

#### `switchExpression.ek9`
- Switch as expression with return value
- Return variable management

### 2.2 SwitchStatementGenerator Design

```java
public class SwitchStatementGenerator extends AbstractGenerator {
    private final GeneratorSet generators;

    public SwitchStatementGenerator(IRGenerationContext stackContext, GeneratorSet generators) {
        super(stackContext);
        this.generators = generators;
    }

    // Handle statement form
    public List<IRInstr> generateStatement(SwitchStatementExpressionContext ctx) {
        // 1. Process preFlowAndControl for evaluation variable
        // 2. Create evaluation variable setup instructions
        // 3. Process each case statement:
        //    - For literals: direct equality check
        //    - For expressions: evaluate full expression
        //    - Multiple values: OR them together
        // 4. Process default case if present
        // 5. Use ControlFlowChainDetails.createSwitch()
    }

    // Handle expression form
    public List<IRInstr> generateExpression(SwitchExpressionContext ctx,
                                           VariableDetails result) {
        // Similar but with:
        // - Return variable setup
        // - Result assignment in each case
        // - Use ControlFlowChainDetails.createSwitch() with result
    }
}
```

### 2.3 Special Considerations

- **Enum Exhaustiveness**: Detect enum types, use `EnumOptimizationDetails`
- **Multiple Case Values**: `case 1, 2, 3` becomes OR condition
- **Case Expressions**: Support `>, <, matches, contains` operators
- **Guard Variables**: Handle preFlowStatement in switch

---

## Phase 3: While Loop Implementation

### 3.1 Loop Representation Challenge

Loops don't naturally fit the CONTROL_FLOW_CHAIN model designed for branching.

**Solution**: Use special chain types with backend interpretation:
- `chainType: "WHILE_LOOP"`
- `chainType: "DO_WHILE_LOOP"`

### 3.2 WhileStatementGenerator Design

```java
public class WhileStatementGenerator extends AbstractGenerator {
    private final GeneratorSet generators;

    public List<IRInstr> generateWhile(WhileStatementExpressionContext ctx) {
        // Create single ConditionCaseDetails for loop condition
        // Body goes in case body (executed when true)
        // No default case for while loops
        // Backend handles loop semantics:
        //   - Loop labels
        //   - Condition re-evaluation
        //   - Branch back to start
    }

    public List<IRInstr> generateDoWhile(DoWhileContext ctx) {
        // Similar but:
        // - Body executes first (in default case)
        // - Condition evaluated after
        // - chainType: "DO_WHILE_LOOP"
    }
}
```

### 3.3 IR Structure

```
CONTROL_FLOW_CHAIN
[
  chain_type: WHILE_LOOP
  condition_chain:
  [
    {
      condition_evaluation: [evaluate loop condition]
      primitive_condition: _temp_bool
      body_evaluation: [loop body instructions]
    }
  ]
  default_case: none
]
```

Backend generates appropriate loop structure:
- JVM: Loop labels and conditional branches
- LLVM: Basic blocks with phi nodes

---

## Phase 4: For Loop Implementation

### 4.1 Complex Decomposition

For loops decompose into multiple components:

```ek9
for item in collection
  // body

// Decomposes to:
iterator = collection.iterator()
while iterator.hasNext()
  item = iterator.next()
  // body
```

### 4.2 ForStatementGenerator Design

```java
public class ForStatementGenerator extends AbstractGenerator {
    private final GeneratorSet generators;

    public List<IRInstr> generateForIn(ForLoopContext ctx) {
        // 1. Create iterator from collection
        // 2. Store in guard scope
        // 3. Create condition: iterator.hasNext()
        // 4. Body includes:
        //    - item = iterator.next()
        //    - User body
        // 5. Use chainType: "FOR_IN_LOOP"
    }

    public List<IRInstr> generateForRange(ForRangeContext ctx) {
        // 1. Initialize range with start, end, step
        // 2. Create condition: current <= end
        // 3. Body includes:
        //    - User body
        //    - current += step
        // 4. Use chainType: "FOR_RANGE_LOOP"
    }
}
```

---

## Implementation Checklist

### Phase 1: If/Else (Immediate)
- [ ] Create `controlFlow/` test directory
- [ ] Create `simpleIf.ek9` with @IR directive
- [ ] Create `ifElse.ek9` with @IR directive
- [ ] Create `ifElseIfChain.ek9` with @IR directive
- [ ] Create `ifWithComplexCondition.ek9` with @IR directive
- [ ] Implement `IfStatementGenerator.java`
- [ ] Update `GeneratorSet` with `ifStatementGenerator`
- [ ] Wire into `StmtInstrGenerator`
- [ ] Create `ControlFlowIRTest.java`
- [ ] Run tests and validate IR output

### Phase 2: Switch
- [ ] Create switch test files
- [ ] Implement `SwitchStatementGenerator.java`
- [ ] Handle enum optimization
- [ ] Test complex case expressions

### Phase 3: While Loops
- [ ] Create while/do-while test files
- [ ] Implement `WhileStatementGenerator.java`
- [ ] Define loop IR semantics

### Phase 4: For Loops
- [ ] Create for-in/for-range test files
- [ ] Implement `ForStatementGenerator.java`
- [ ] Handle iterator management

---

## Key Implementation Patterns

### GeneratorSet Pattern
All new generators follow:
```java
public MyGenerator(IRGenerationContext stackContext, GeneratorSet generators) {
    super(stackContext);
    this.generators = generators;
}
```

### CONTROL_FLOW_CHAIN Structure
```java
ControlFlowChainDetails.createIfElse(
    result,              // null for statements
    conditionChain,      // List<ConditionCaseDetails>
    defaultBodyEval,     // List<IRInstr> for else
    defaultResult,       // null for statements
    debugInfo,
    scopeId
);
```

### Memory Management
- RETAIN all loaded objects
- SCOPE_REGISTER for cleanup
- RELEASE before STORE

### Scope Management
- Use `stackContext.currentScopeId()`
- No manual scope tracking

### Primitive Conversion
- Boolean conditions need `_true()` call
- Store as both EK9 Boolean and primitive

---

## Testing Strategy

### Test File Organization
- One function per file
- @IR directive with complete expected output
- Focus on single pattern per test

### Incremental Development
1. Start with simplest case (simpleIf)
2. Add complexity gradually
3. Validate IR at each step
4. Fix issues before proceeding

### IR Validation
- Exact match with @IR directive
- Check memory management patterns
- Verify scope handling
- Validate primitive conversions

---

## Future Enhancements

### Guard Variables
- Support preFlowStatement in all control flow
- Dedicated guard scope management
- Variable visibility rules

### Loop Control
- Break/continue statements
- Label support for nested loops
- Early returns from loops

### Pattern Matching
- Switch on types
- Destructuring in cases
- Guard expressions in patterns

---

## References

- `EK9_IR_AND_CODE_GENERATION.md` - IR generation principles
- `PHASE7_REFACTORING_FINAL_SUMMARY.md` - Generator architecture
- `IROpcode.java` - CONTROL_FLOW_CHAIN definition
- `ControlFlowChainGenerator.java` - Existing implementation

---

## Session Notes

This document should be sufficient to continue the implementation in a new session. The immediate focus is Phase 1 (if/else), with clear patterns established for subsequent phases.

Key files to modify:
1. Create test files in `controlFlow/` directory
2. Create `IfStatementGenerator.java`
3. Update `GeneratorSet.java`
4. Update `StmtInstrGenerator.java` (line 62)
5. Create `ControlFlowIRTest.java`

Run tests with: `mvn test -Dtest=ControlFlowIRTest`

---

## Critical Implementation Insights (2025-10-14)

### 1. IR Notation Understanding
- **CRITICAL**: In IR output like `(Integer)_temp2._gt()`, the `(Integer)` shows the **type of the object calling the method**, NOT a cast or return type
- This notation is for debugging/visualization only - the actual IR uses typed objects
- Example: `CALL (org.ek9.lang::Integer)_temp3._gt(_temp4)` means _temp3 is an Integer

### 2. Memory Management for Primitives
- **Primitive booleans** from `_true()` don't need RETAIN/SCOPE_REGISTER
- Only **EK9 objects** (like Boolean from `_gt()`) need memory management
- The pattern is:
  1. Expression generates EK9 Boolean (needs memory management)
  2. Call `_true()` to get primitive boolean (no memory management needed)
  3. Use primitive for backend branching, EK9 Boolean for cleanup

### 3. GeneratorSet Pattern Specifics
- **ALWAYS use public fields**, not methods: `generators.controlFlowChainGenerator` NOT `generators.controlFlowChainGenerator()`
- This is a struct-style pattern for circular dependency resolution
- Fields are populated during construction phase, then logically immutable

### 4. ANTLR Grammar Navigation
- Block structure: `BlockContext` → `instructionBlock()` → `blockStatement()`
- Always check the grammar for correct accessor methods
- Example fix: `ctx.block().instructionBlock().blockStatement()` not `ctx.block().blockStatement()`

### 5. Test File Organization
- **Each test directory can only have ONE module**
- Multiple functions in same module = same file
- Different modules = different directories
- Comments in function section can cause parse errors - use blank lines for padding

### 6. Line Number Alignment
- @IR directives must have exact line numbers matching actual code
- Use blank lines (not comments) to align function definitions
- Clean build (`mvn clean`) may be needed after multiple test runs

### 7. Import Corrections
- `CompilerException` is in `org.ek9lang.core` not `org.ek9lang.compiler`
- `AssertValue` is in `org.ek9lang.core`
- Always check existing generators for correct imports

### 8. Actual vs Expected IR Differences
- Temp variable numbering may differ - this is OK if functionality is correct
- Missing `default_case: none` in output - backends handle this gracefully
- String literals may not have quotes in actual output
- Focus on functional correctness over exact formatting

### 9. ControlFlowChainDetails Usage
- The existing `ControlFlowChainGenerator` handles all the heavy lifting
- Just need to build proper `ConditionCaseDetails` and `DefaultCaseDetails`
- Don't try to generate CONTROL_FLOW_CHAIN instruction directly

### 10. Expression Generation Pattern
- Let `exprGenerator` handle all temp variable management for expressions
- Don't pre-allocate temp names for expression internals
- Only allocate temps for the final results you need to reference

---

## Completed Work (Phase 1 - If/Else)

✅ **Simple If Statement**
- Created `simpleIf.ek9` with working @IR directive
- Implemented `IfStatementGenerator` with GeneratorSet pattern
- Integrated into `GeneratorSet` and `StmtInstrGenerator`
- Created `ControlFlowIRTest` JUnit test
- Successfully generates CONTROL_FLOW_CHAIN IR
- Handles primitive boolean conversion via `_true()`
- Proper memory management for EK9 objects

### Next Steps for Other Control Flow

When implementing switch/while/for:
1. Create test files in SEPARATE directories if different modules needed
2. Use exact same GeneratorSet pattern (2 params: context, generators)
3. Let existing generators handle expression evaluation
4. Focus on building proper Details objects for ControlFlowChainGenerator
5. Test incrementally - get simplest case working first
6. Check line number alignment if IR comparison fails