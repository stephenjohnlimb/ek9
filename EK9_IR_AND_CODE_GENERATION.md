# EK9 IR and Code Generation Guide

This document provides comprehensive guidance for working with EK9's Intermediate Representation (IR) and code generation to various targets. This is the specialized reference for compiler backend development, optimization passes, and target-specific code generation.

**Related Documentation:**
- **`CLAUDE.md`** - Main project overview and daily development guidelines
- **`EK9_COMPILER_PHASES.md`** - Detailed compiler phase implementation and pipeline
- **`EK9_Compiler_Architecture_and_Design.md`** - Complete architectural specification

## IR Generation Overview

EK9's IR generation transforms resolved symbols from compilation phases 1-6 into a target-agnostic intermediate representation that can be translated to multiple backends (JVM bytecode, LLVM IR, etc.).

### IR Design Principles
- **Target-agnostic**: IR must work equally well for JVM, LLVM-Go, and LLVM-C++ targets
- **Symbol-driven**: Uses resolved symbols from `ParsedModule.getRecordedSymbol()` instead of AST text parsing
- **Text-based representation**: Stores all values as strings for serialization and backend flexibility
- **Fully qualified type names**: All types use complete qualified names to avoid ambiguity

### Symbol Table to IR Transformation
The IR generation process in phase 7 transforms resolved symbols:
1. **Context Creation**: `IRGenerationContext` provides centralized state management
2. **Symbol Resolution**: Use `parsedModule.getRecordedSymbol(ctx)` for all AST nodes
3. **Type Information**: Extract fully qualified type names using `symbol.getType().getFullyQualifiedName()`
4. **Value Extraction**: Get literal values and identifiers from resolved symbols

## IR Literal Value Representation

### Design Decision: Text-Based IR with Careful Encoding

**Chosen Approach**: Pure text-based IR using Java Strings for all literal storage.

**Alternative Considered**: ISymbol-based IR storing rich symbol objects.
- **Rejected because**: Couples IR to compiler symbol table, not serializable, complex for code generation backends.

### Literal Value Encoding Rules

All literal values are stored as Java Strings that preserve their original semantic representation:

| EK9 Source | Internal Storage (Java String) | IR Output |
|------------|-------------------------------|-----------|
| `2` | `"2"` | `LOAD_LITERAL 2 (org.ek9lang.lang.Integer)` |
| `2024-12-25` | `"2024-12-25"` | `LOAD_LITERAL 2024-12-25 (org.ek9lang.lang.Date)` |
| `"Hello, World"` | `"\"Hello, World\""` | `LOAD_LITERAL "Hello, World" (org.ek9lang.lang.String)` |
| `true` | `"true"` | `LOAD_LITERAL true (org.ek9lang.lang.Boolean)` |

**Critical Rule**: String literals retain their quotes as part of the stored value to distinguish them from other literal types.

### Type Name Strategy

All types use fully qualified names to avoid ambiguity across different contexts:
- `String` → `org.ek9lang.lang.String`
- `Integer` → `org.ek9lang.lang.Integer`  
- `List of String` → `_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1`

### Code Generation Implications

This encoding strategy enables backends to correctly interpret literal values:

**JVM Bytecode Generation**:
- `2` → `bipush 2` or `ldc 2`
- `"Hello, World"` → `ldc "Hello, World"`
- Can distinguish numeric vs string constants

**LLVM IR Generation**:  
- `2` → `i32 2`
- `"Hello, World"` → `i8* getelementptr (...)`
- Type-aware constant generation

**Parsing Strategy**: Backends parse the stored literal values based on the fully qualified type information provided in the IR instruction.

## Code Generation Targets

### Java Bytecode Generation
*This section will contain:*
- Java bytecode generation patterns
- EK9 to Java type mapping strategies
- Optimization techniques for Java target
- Integration with Java ecosystem

### Future Target Support
*This section will contain:*
- LLVM IR generation planning
- Native compilation strategies
- Cross-platform considerations
- Performance optimization approaches

## Optimization Strategies

### EK9's Hybrid Optimization Philosophy

EK9 employs a **hybrid optimization strategy** that balances IR-level semantic clarity with backend-specific optimization capabilities. This approach leverages the strengths of both compile-time and runtime optimization while maintaining target portability.

### Null Check Optimization: A Case Study

**Problem**: EK9's null-safety features (question operator `?` and guarded assignment `:=?`) can generate multiple null checks on the same variable:

```ek9
someFunction()
  value as Integer?
  value :=? 42
  
  if value?        // First ? operator  
    stdout.println(value)
    
  result := value? // Second ? operator
  assert value?    // Third ? operator
```

**Current IR Generation** (explicit IS_NULL semantic clarity):
```
// Each operation generates explicit null checking
_temp1 = QUESTION_BLOCK [
  operand_evaluation: [
    _temp2 = LOAD value
    _temp3 = IS_NULL value  // Explicit null check
  ]
  // ... rest of question logic
]

// Repeated for each ? operator
_temp4 = QUESTION_BLOCK [
  operand_evaluation: [
    _temp5 = LOAD value  
    _temp6 = IS_NULL value  // Redundant null check
  ]
  // ... rest of question logic
]
```

### Backend Optimization Capabilities Analysis

#### LLVM Optimization Effectiveness ✅ **Excellent**
LLVM's optimization passes handle redundant null check elimination exceptionally well:

**LLVM Optimization Passes:**
- **EarlyCSE**: Common Subexpression Elimination removes duplicate IS_NULL checks
- **GVN**: Global Value Numbering identifies equivalent null check computations  
- **LICM**: Loop-Invariant Code Motion hoists null checks out of loops
- **DeadStoreElimination**: Removes redundant memory operations

**LLVM IR Example:**
```llvm
; Before optimization
%null1 = icmp eq ptr %value, null
%null2 = icmp eq ptr %value, null  ; Redundant check
%null3 = icmp eq ptr %value, null  ; Redundant check

; After LLVM optimization passes  
%null_check = icmp eq ptr %value, null  ; Single check
; Reused across all three locations
```

#### JVM/HotSpot Optimization Effectiveness ✅ **Excellent**
HotSpot's C1/C2 compilers excel at null check elimination:

**HotSpot Optimizations:**
- **Null Check Elimination**: Removes provably redundant null checks
- **Range Check Elimination**: Similar pattern for array bounds
- **Method Inlining**: Can see across method boundaries for interprocedural optimization
- **Profile-Guided Optimization**: Uses runtime feedback to optimize frequent patterns

**Bytecode Optimization Example:**
```java
// Before HotSpot optimization
if (value == null) // First null check
if (value == null) // Second null check (eliminated)
if (value == null) // Third null check (eliminated)

// After HotSpot optimization
if (value == null) // Single null check, result reused
```

### Optimization Strategy Decision: Correctness-First Approach

**Core Philosophy**: **Generate semantically clear, correct IR first. Optimize in dedicated phases later.**

### IR Generation Strategy: Simplicity and Semantic Clarity

**Current Approach (Phase 7: IR_GENERATION)**:
- **Each operation loads variables independently** - even if the same variable is used multiple times
- **Explicit memory management** - every LOAD gets its own RETAIN/SCOPE_REGISTER sequence
- **Simple IR generation code** - each generator works independently without complex state tracking
- **Complete semantic information** - every operation shows its exact memory management needs

**Example of Correct "Duplicate" Operations**:
```java
// First operation: Check if variable is set
_temp3 = LOAD value                    // Load for primitive condition check
RETAIN _temp3                          // Reference count = 1
SCOPE_REGISTER _temp3, _scope_1        // Register for cleanup
_temp4 = IS_NULL _temp3                // Null check
_temp6 = CALL (org.ek9.lang::Boolean)_temp3._isSet()  // Method call

// Second operation: Same variable, different usage context
_temp10 = LOAD value                   // Load for different operation - CORRECT
RETAIN _temp10                         // Reference count = 1 - CORRECT  
SCOPE_REGISTER _temp10, _scope_1       // Register for cleanup - CORRECT
_temp11 = IS_NULL _temp10              // Different usage context
```

**Why This "Duplication" is Correct by Design**:
1. **Semantic Clarity**: Each operation explicitly shows its memory management requirements
2. **Simple Code Generation**: No complex state tracking between IR generators required
3. **Complete Context**: Phase 12 optimization gets full picture of variable usage patterns
4. **Backend Enablement**: Rich semantic information enables superior backend optimization

**Phase 12: IR_OPTIMISATION (Future Enhancement)**
With complete IR structure, optimization can perform:
- **Variable Load Coalescing**: Eliminate redundant LOAD/RETAIN/REGISTER sequences within scopes
- **Stack-Based Optimization**: Convert heap-based reference counting to stack operations where safe
- **Global Variable Analysis**: Make sophisticated decisions based on complete variable lifetime information
- **RETAIN/REGISTER Elimination**: Remove unnecessary memory management operations

**Benefits of Correctness-First Strategy**:
1. **Separation of Concerns**: IR generation focuses on correctness, optimization focuses on performance
2. **Maintainable Codebase**: Each IR generator is simple and independent
3. **Optimization Flexibility**: Complete semantic information enables global optimization decisions
4. **Correctness Guarantee**: Optimization never breaks semantic correctness

**Benefits of Explicit IS_NULL Approach:**
1. **Semantic Clarity**: Backends understand null-checking intent precisely
2. **Optimization Enablement**: Rich semantic information enables better backend optimization
3. **Debug Transparency**: Null checking logic is visible in IR inspection
4. **Correctness**: No ambiguity about null safety semantics

### General IR Optimization Principles

**Conservative IR-Level Optimizations (Future):**
- Same variable, same basic block, no intervening assignments
- Identical operands within small scope windows
- Clear dataflow analysis showing no mutations between operations

**Backend-Level Optimizations (Current):**
- Cross-function optimizations
- Complex control flow scenarios
- Target-specific optimizations
- Interprocedural analysis
- Profile-guided optimization

### Performance Measurement Strategy

**Optimization Validation Approach:**
1. **Baseline Measurement**: Profile current backend-optimized performance
2. **Hotspot Identification**: Identify actual performance bottlenecks in real applications
3. **Incremental Enhancement**: Add IR-level optimizations only where profiling shows benefit
4. **Regression Testing**: Ensure optimizations don't break correctness

**Key Insight**: Explicit semantic information (like IS_NULL) **enables** backend optimization rather than hindering it. Backends now have perfect information about null-checking intent, leading to superior optimization results.

## Target-Specific Considerations

### Java Target Specifics
*This section will contain:*
- Java interoperability requirements
- JVM limitations and workarounds
- Performance characteristics
- Debugging and profiling integration

### Native Target Planning
*This section will contain:*
- Native code generation requirements
- Memory management strategies
- System integration considerations
- Performance optimization opportunities

## IR Debugging and Analysis

### Debug Information Integration

EK9's IR system includes comprehensive debug information support for source mapping and debugging:

**DebugInfo Record Structure:**
```java
public record DebugInfo(
    String sourceFile,      // Relative .ek9 file path  
    int lineNumber,         // 1-based line number
    int columnNumber,       // 1-based column number
    String originalText     // Original EK9 source text (optional)
)
```

**Key Design Decisions:**
- **Relative Paths**: Uses `CompilableSource.getRelativeFileName()` for portability
- **Symbol-Driven**: Extracts location from `ISymbol.getSourceToken()`
- **Optional Integration**: Controlled by `CompilerFlags.isDebuggingInstrumentation()`
- **IR Comment Format**: `// workarea.ek9:12:15 'original text'`

### Debug Information Architecture

**Encapsulation Pattern:**
```java
// DebugInfoCreator encapsulates debug information generation logic
final class DebugInfoCreator implements Function<ISymbol, DebugInfo> {
  public DebugInfo apply(final ISymbol symbol) {
    return context.getCompilerFlags().isDebuggingInstrumentation()
        ? DebugInfo.from(context.getParsedModule().getSource(), symbol) : null;
  }
}

// Used consistently across all IR instruction creators
private final DebugInfoCreator debugInfoCreator;
final var debugInfo = debugInfoCreator.apply(symbol);
```

**IR Integration:**
- All `IRInstruction` subclasses support optional `DebugInfo`
- Debug information appears as IR comments: `CALL method() // ./file.ek9:10:5`
- Enables source mapping for target code generation (JVM, LLVM, etc.)

### Testing and Verification

**Debug Output Verification:**
- WorkingAreaTest demonstrates debug instrumentation with `-Dek9.instructionInstrumentation=true`
- Relative path formatting: `./workarea.ek9:8:17` (not absolute paths)
- Method call tracking: `CALL _temp6.println(toOutput) // ./workarea.ek9:13:14`
- Constructor call tracking: `CALL org.ek9.lang::Stdout.<init>() // ./workarea.ek9:8:17`

## IR Generation Patterns

### Function-to-Class Transformation Pattern

EK9 functions are transformed into class-like constructs with synthetic `_call` methods, following the "Everything as Object" design principle:

**EK9 Source:**
```ek9
checkAssert()
  -> arg0 as Boolean
  assert arg0
```

**Generated IR Structure:**
```
ConstructDfn: justAssert::checkAssert
OperationDfn: justAssert::checkAssert._call()->org.ek9.lang::Void
BasicBlock: _entry_1
IRInstruction: REFERENCE arg0, org.ek9.lang::Boolean  // Parameter declaration
IRInstruction: SCOPE_ENTER _scope_1  // Function body scope
IRInstruction: _temp1 = LOAD arg0  // Load parameter value
IRInstruction: _temp2 = CALL (org.ek9.lang::Boolean)_temp1._true()  // Boolean conversion
IRInstruction: ASSERT _temp2  // Primitive boolean assertion
IRInstruction: SCOPE_EXIT _scope_1
IRInstruction: RETURN
```

**Key Transformation Components:**
- **FunctionDfnGenerator**: Creates function-as-class constructs with synthetic `_call` methods
- **OperationDfnGenerator**: Handles function body processing with proper scope management
- **Synthetic Method Pattern**: Functions become classes with `_call()` methods for uniform invocation

### Assert Statement Processing Pattern

EK9 assert statements convert Boolean expressions to primitive booleans before assertion:

**Processing Steps:**
1. **Expression Evaluation**: Generate IR to evaluate the assert expression to a temporary variable
2. **Boolean Conversion**: Call `_true()` method on the Boolean object to get primitive boolean value
3. **Assertion**: Use ASSERT IR instruction with the primitive boolean result

**AssertStmtGenerator Implementation:**
```java
// Evaluate the assert expression
final var rhsExprResult = context.generateTempName();
final var instructions = new ArrayList<>(expressionGenerator.apply(ctx.expression(), rhsExprResult, scopeId));

// Call the _true() method to get primitive boolean
final var rhsResult = context.generateTempName();
final var callDetails = new CallDetails(rhsExprResult, booleanTypeName, "_true", 
    List.of(), "boolean", List.of());
instructions.add(CallInstr.call(rhsResult, debugInfo, callDetails));

// Assert on the primitive boolean result
instructions.add(BranchInstr.assertValue(rhsResult, debugInfo));
```

### Memory Management and Scope Ownership Rules

EK9's IR generation includes sophisticated memory ownership tracking through scope registration based on **reference counting** for objects:

**ShouldRegisterVariableInScope Logic:**
- **Parameters** (`_param_*` scopes): **FALSE** - caller-managed memory, no SCOPE_REGISTER
- **Return variables** (`_return_*` scopes): **FALSE** - ownership transferred to caller
- **Local variables** (`_scope_*` scopes): **TRUE** - function-managed memory, needs SCOPE_REGISTER

**CRITICAL UNDERSTANDING: Variable Declaration vs Object Reference**

**Variable Declaration (REFERENCE):**
```java
// REFERENCE only declares that a variable CAN hold a reference - NO object exists yet
REFERENCE localVar, org.ek9.lang::Boolean  // Variable declaration only
// NO SCOPE_REGISTER at this point - localVar doesn't reference any object yet
```

**Object Creation and Reference Counting:**
```java
// Step 1: Create object and establish first reference
_temp1 = LOAD_LITERAL true, org.ek9.lang::Boolean  // Create Boolean object
RETAIN _temp1                                      // Reference count = 1
SCOPE_REGISTER _temp1, _scope_1                   // Register _temp1 for cleanup

// Step 2: Store object reference in localVar
STORE localVar, _temp1                            // localVar now references same object  
RETAIN localVar                                   // Reference count = 2
SCOPE_REGISTER localVar, _scope_1                // Register localVar for cleanup
```

**Key Memory Management Principles:**
1. **REFERENCE declares variables** - NO object, NO SCOPE_REGISTER needed
2. **SCOPE_REGISTER happens when variable references an object** - after STORE operations
3. **Same object can have multiple variable references** - each gets RETAIN/SCOPE_REGISTER
4. **Reference counting tracks object lifetime** - SCOPE_EXIT auto-releases all registered variables
5. **Primitive return values** (like `_true()` method) need NO memory management

**Parameter Handling Pattern:**
```java
// Parameters get REFERENCE declaration only - no scope registration
instructions.add(MemoryInstr.reference(paramName, paramType, debugInfo));
// NO SCOPE_REGISTER for parameters - caller owns the memory
```

**Critical Memory Ownership Rule:** Function parameters are caller-owned and should NOT be registered in function scope for cleanup. This prevents the function from attempting to manage memory it doesn't own.

### Variable Declaration Processing Pattern

**AbstractVariableDeclGenerator** provides common processing for variable declarations:

**CORRECTED PATTERNS - Variable Declaration vs Object Reference:**

**Variable-Only Declarations** (uninitialized variables):
```
REFERENCE variableName, typeName  // Variable declaration only - NO object yet
// NO SCOPE_REGISTER - variable doesn't reference any object yet
```

**Variable Declarations with Initialization** (local variables with values):
```
REFERENCE variableName, typeName              // Variable declaration only
_temp1 = [initialization expression IR]      // Create/load object
RETAIN _temp1                                 // Reference count = 1 for object
SCOPE_REGISTER _temp1, scopeId               // Register _temp1 for cleanup
STORE variableName, _temp1                   // variableName now references object
RETAIN variableName                          // Reference count = 2 for same object
SCOPE_REGISTER variableName, scopeId        // Register variableName for cleanup
```

**Variable Assignment** (separate from declaration):
```
REFERENCE variableName, typeName              // Variable was declared previously
_temp1 = LOAD_LITERAL value, typeName        // Create/load object  
RETAIN _temp1                                 // Reference count = 1
SCOPE_REGISTER _temp1, scopeId               // Register temp for cleanup
RELEASE variableName                         // Release previous reference (if any)
STORE variableName, _temp1                   // variableName now references object
RETAIN variableName                          // Reference count = 2
SCOPE_REGISTER variableName, scopeId        // Register variableName for cleanup
```

**Memory Management Rule:** SCOPE_REGISTER only happens when a variable actually references an object, not when the variable is declared.

### CRITICAL: Understanding "Duplicate" Memory Operations

**Important Design Principle**: The IR generation intentionally creates what appears to be "duplicate" LOAD/RETAIN/SCOPE_REGISTER operations. This is **correct by design** and essential for the optimization strategy.

**Example of Correct Pattern**:
```java
// Function using same variable in multiple operations
guardedAssignment()
  value as Integer?
  value :=? 1        // First operation: guarded assignment
  assert value?      // Second operation: question operator on same variable
```

**Generated IR (Correct)**:
```
// First operation: Guarded assignment using value
_temp3 = LOAD value                    // Load #1
RETAIN _temp3                          // Retain #1  
SCOPE_REGISTER _temp3, _scope_1        // Register #1
_temp4 = IS_NULL _temp3                // Null check for guarded assignment

// Second operation: Question operator on same variable  
_temp10 = LOAD value                   // Load #2 - CORRECT, NOT redundant
RETAIN _temp10                         // Retain #2 - CORRECT, NOT redundant
SCOPE_REGISTER _temp10, _scope_1       // Register #2 - CORRECT, NOT redundant  
_temp11 = IS_NULL _temp10              // Null check for question operator
```

**Why Multiple Loads Are Necessary**:
1. **Semantic Independence**: Each operation must show its complete memory management requirements
2. **Phase 12 Context**: Optimization phase needs to see all variable usage patterns to make global decisions
3. **Backend Flexibility**: Backends can optimize based on complete semantic information
4. **Simple Generators**: Each IR generator works independently without complex state management

**Safe RELEASE on Uninitialized Variables**: The `RELEASE` operation before first assignment is safe and avoids explicit null checks:
```java
REFERENCE value, org.ek9.lang::Integer        // Declaration only - no object yet
_temp1 = LOAD_LITERAL 1, org.ek9.lang::Integer
RETAIN _temp1
SCOPE_REGISTER _temp1, _scope_1
RELEASE value                                 // SAFE on uninitialized - backend handles null check
STORE value, _temp1                           // First assignment
RETAIN value
SCOPE_REGISTER value, _scope_1
```

### Scope Management Pattern

EK9 uses hierarchical scope management for memory cleanup:

**Function Scope Structure:**
1. **Parameter Scope** (`_param_*`): No automatic cleanup - caller-managed
2. **Return Scope** (`_return_*`): No automatic cleanup - transferred to caller
3. **Function Body Scope** (`_scope_*`): Automatic cleanup with SCOPE_EXIT

**Scope Lifecycle:**
```
SCOPE_ENTER _scope_1
// ... function body instructions
// ... SCOPE_REGISTER for local variables only
SCOPE_EXIT _scope_1  // Automatic RELEASE of all registered variables
```

## Medium-Level IR: LOGICAL_AND_BLOCK and LOGICAL_OR_BLOCK

### Overview: Declarative Control Flow IR

EK9 introduces a novel medium-level IR approach for Boolean logical operations that moves beyond traditional low-level branching and PHI nodes to **declarative conditional blocks**. This approach pre-computes all execution paths while maintaining backend flexibility for optimization strategies.

**Key Innovation**: Complete path pre-computation with dual semantic preservation (primitive boolean conditions + EK9 Boolean object results).

### Architecture Philosophy

**Problem with Traditional Approaches:**
- **Low-level PHI nodes**: LLVM PHI predecessor relationship issues, complex maintenance
- **Basic block IRs**: Backend-specific, poor cross-target portability  
- **Pure functional IRs**: Loss of imperative execution semantics
- **Runtime branching**: Limited optimization opportunities

**EK9's Solution: Medium-Level Declarative Blocks**
```
LOGICAL_AND_BLOCK result_var condition: primitive_boolean_condition
{
  left_operand: ek9_boolean_left
  left_condition: primitive_boolean_left  
  right_evaluation: { /* complete instruction sequence */ }
  right_operand: ek9_boolean_right
  result_computation: { /* EK9 Boolean._and() call sequence */ }
  logical_result: ek9_boolean_result
  scope_id: memory_scope
}
```

### IR Structure Definition

#### LOGICAL_AND_BLOCK Structure
```
_temp_result = LOGICAL_AND_BLOCK  // debug_info
{
  left_operand: _temp_left_ek9_boolean      // EK9 Boolean left operand
  left_condition: _temp_left_primitive      // primitive boolean (from _true())
  right_evaluation:                         // Instructions to evaluate right operand
  {
    _temp_right_load = LOAD right_source
    RETAIN _temp_right_load  
    SCOPE_REGISTER _temp_right_load, scope_id
    // ... additional nested logical operations
  }
  right_operand: _temp_right_ek9_boolean    // EK9 Boolean right operand
  result_computation:                       // Instructions for full evaluation  
  {
    _temp_and_result = CALL (org.ek9.lang::Boolean)_temp_left._and(_temp_right)
    RETAIN _temp_and_result
    SCOPE_REGISTER _temp_and_result, scope_id
  }
  logical_result: _temp_and_result          // EK9 Boolean final result
  scope_id: _scope_1                        // Memory management scope
}
```

#### LOGICAL_OR_BLOCK Structure
```
_temp_result = LOGICAL_OR_BLOCK  // debug_info  
{
  left_operand: _temp_left_ek9_boolean      // EK9 Boolean left operand
  left_condition: _temp_left_primitive      // primitive boolean (from _true())
  right_evaluation:                         // Instructions to evaluate right operand
  {
    _temp_right_load = LOAD right_source
    RETAIN _temp_right_load
    SCOPE_REGISTER _temp_right_load, scope_id
    // ... additional nested logical operations
  }
  right_operand: _temp_right_ek9_boolean    // EK9 Boolean right operand  
  result_computation:                       // Instructions for full evaluation
  {
    _temp_or_result = CALL (org.ek9.lang::Boolean)_temp_left._or(_temp_right)
    RETAIN _temp_or_result
    SCOPE_REGISTER _temp_or_result, scope_id
  }
  logical_result: _temp_or_result           // EK9 Boolean final result
  scope_id: _scope_1                        // Memory management scope
}
```

### Complex Expression Example

**EK9 Source:**
```ek9
rtn: arg0 and (arg1 or arg2)
```

**Generated IR:**
```
_temp2 = LOGICAL_AND_BLOCK  // ./workarea.ek9:35:17
{
  left_operand: _temp3                      // arg0
  left_condition: _temp4                    // arg0._true()
  right_evaluation:
  {
    _temp6 = LOAD arg1
    RETAIN _temp6
    SCOPE_REGISTER _temp6, _scope_1
    _temp7 = CALL (org.ek9.lang::Boolean)_temp6._true()
    _temp5 = LOGICAL_OR_BLOCK  // ./workarea.ek9:35:27
    {
      left_operand: _temp6                  // arg1
      left_condition: _temp7                // arg1._true()  
      right_evaluation:
      {
        _temp8 = LOAD arg2
        RETAIN _temp8
        SCOPE_REGISTER _temp8, _scope_1
      }
      right_operand: _temp8                 // arg2
      result_computation:
      {
        _temp9 = CALL (org.ek9.lang::Boolean)_temp6._or(_temp8)
        RETAIN _temp9
        SCOPE_REGISTER _temp9, _scope_1
      }
      logical_result: _temp9                // arg1._or(arg2)
      scope_id: _scope_1
    }
    RETAIN _temp5
    SCOPE_REGISTER _temp5, _scope_1
  }
  right_operand: _temp5                     // (arg1 or arg2)
  result_computation:
  {
    _temp10 = CALL (org.ek9.lang::Boolean)_temp3._and(_temp5)
    RETAIN _temp10
    SCOPE_REGISTER _temp10, _scope_1
  }
  logical_result: _temp10                   // arg0._and(arg1._or(arg2))
  scope_id: _scope_1
}
```

### Key Design Principles

#### 1. Complete Path Pre-computation
- **All execution paths** are evaluated during IR generation
- **Right operand evaluation** instructions fully computed
- **Result computation** instructions (EK9 method calls) fully specified
- **No runtime path construction** required

#### 2. Dual Semantic Preservation  
- **Primitive boolean conditions** (`left_condition`) for backend control flow optimization
- **EK9 Boolean objects** (`left_operand`, `right_operand`, `logical_result`) for language semantics
- **Both representations available** simultaneously for backend choice

#### 3. Single Condition Model
- Only **left condition needed** for short-circuit decisions
- **AND operation**: if `left_condition` is false → short-circuit to `left_operand`
- **OR operation**: if `left_condition` is true → short-circuit to `left_operand` 
- **Right condition irrelevant** in short-circuit evaluation

#### 4. Context-Aware Memory Management
**Fundamental Rule**: The location where a variable is first created/accessed owns the memory management responsibility.

- **First creation point** determines `RETAIN`/`SCOPE_REGISTER` responsibility
- **Context awareness**: Different variable origins (parameter, local, return) have different scope rules
- **No duplication**: Only one location handles memory management per variable
- **Lifecycle correctness**: Variables managed according to their actual usage context

**Example Applications**:
- **Return parameters**: Should NOT be scope-registered (outlive current scope)
- **Local variables**: Should be scope-registered for cleanup  
- **Parameters**: Context-dependent scope registration
- **Intermediate results**: Appropriate scope management based on role

### Backend Mapping Strategies

#### LLVM Backend Lowering

**Short-Circuit Branch Strategy:**
```llvm
; Left operand evaluation and condition check
%left_operand = call %EK9Boolean @evaluate_left_operand()
%left_condition = call i1 @EK9Boolean.true(%left_operand)

; Short-circuit decision branch
br i1 %left_condition, label %evaluate_right, label %and_short_circuit

evaluate_right:
  ; Execute pre-computed right_evaluation instructions
  %right_operand = call %EK9Boolean @evaluate_right_operand()
  
  ; Execute pre-computed result_computation instructions  
  %logical_result = call %EK9Boolean @EK9Boolean.and(%left_operand, %right_operand)
  br label %merge

and_short_circuit:
  br label %merge
  
merge:
  ; PHI node with correct predecessor relationships
  %final_result = phi %EK9Boolean [%left_operand, %and_short_circuit], 
                                  [%logical_result, %evaluate_right]
```

**LLVM Advantages:**
- **Proper PHI predecessors**: Each PHI has correct basic block relationships  
- **Pre-computed instruction sequences**: LLVM optimization passes can analyze complete paths
- **Speculative execution support**: Both paths available for parallel execution

#### JVM Backend Lowering

**Flexible Strategy Selection:**

**Option 1: Short-Circuit Bytecode**
```java
// Load left operand and evaluate condition
ALOAD left_operand_var           // EK9 Boolean
DUP
INVOKEVIRTUAL EK9Boolean.true()Z // primitive boolean condition  
IFEQ short_circuit_label         // Jump if false for AND

// Execute pre-computed right_evaluation bytecode sequence
[right_evaluation bytecode sequence]

// Execute pre-computed result_computation bytecode sequence  
[result_computation bytecode sequence]
GOTO merge_label

short_circuit_label:
// Left operand already on stack

merge_label:
// Final result on stack
```

**Option 2: Full Method Call (Non-Short-Circuit)**
```java
// Simply execute all EK9 method calls - no branching
ALOAD left_operand_var
ALOAD right_operand_var  
INVOKEVIRTUAL EK9Boolean.and(EK9Boolean;)EK9Boolean;
```

**JVM Advantages:**
- **Context-driven optimization**: Can choose short-circuit vs full evaluation per usage
- **Stack-based efficiency**: Natural fit for JVM execution model
- **JIT optimization opportunities**: HotSpot can optimize frequent patterns

### Performance Characteristics and CPU Architecture Benefits

#### Modern CPU Optimization Opportunities

**Branch Prediction Enhancement:**
- **Pre-computed paths enable unprecedented branch prediction optimization**
- **Static analysis hints**: Backends can provide branch probability information
- **Dual path prefetching**: CPUs can prefetch both short-circuit and full evaluation paths
- **Pipeline preparation**: Method calls and instruction sequences can be pre-pipelined

**Speculative Execution Benefits:**
- **Parallel path execution**: Modern CPUs can execute both paths speculatively
- **Result selection**: Choose correct path when condition resolves  
- **Cache optimization**: Pre-computed instruction sequences enable superior cache layout

**CPU Architecture Specific Advantages:**

**Intel Ice Lake/Sapphire Rapids:**
- **µop Cache optimization**: Pre-computed paths fit perfectly in micro-op cache patterns
- **Enhanced branch prediction**: Static hints improve branch predictor accuracy
- **Better execution port utilization**: Known instruction sequences improve scheduling

**AMD Zen 4:**
- **Op Cache optimization**: Predictable instruction patterns improve op cache usage
- **Branch Target Buffer**: Better BTB utilization with pre-known targets  
- **Execution unit scheduling**: Pre-computed dependency chains improve parallel execution

**ARM Neoverse V1/V2:**
- **Branch Target Identification**: Enhanced security with compile-time known targets
- **Instruction fetch patterns**: Improved instruction fetch with predictable sequences
- **SVE integration**: Vector operations can be pre-planned and optimized

#### Competitive Analysis vs Industry IRs

| Feature | EK9 LOGICAL_BLOCK | MLIR | XLA HLO | Swift SIL | V8 Turbofan |
|---------|-------------------|------|---------|-----------|-------------|
| **Abstraction Level** | **Medium-High** | Medium | High | Medium-Low | Medium-Low |
| **Path Pre-computation** | **✅ Complete** | ❌ On-demand | ❌ Functional | ❌ Basic blocks | ❌ Speculative |
| **Dual Semantics** | **✅ Primitive + Object** | ❌ Single | ❌ Functional | ❌ Basic blocks | ❌ Speculative |  
| **Backend Agnostic** | **✅ Clean mapping** | Requires dialects | Tensor-focused | Swift-specific | JS-specific |
| **Branch Prediction** | **✅ Advanced hints** | Standard | N/A | Standard | Profile-guided |
| **CPU Architecture** | **✅ Future-optimized** | General | ML-optimized | Mobile-focused | JS-optimized |

**Strategic Positioning:**
- **More structured** than low-level basic block IRs
- **More explicit** than pure functional IRs  
- **More self-contained** than dialect-based systems
- **Better backend flexibility** than runtime-specific IRs
- **Future-proof** for modern CPU architectures

### Implementation Classes and Patterns

#### Core IR Instructions
- **`LogicalOperationInstr`**: Base class for LOGICAL_AND_BLOCK and LOGICAL_OR_BLOCK
- **`IROpcode.LOGICAL_AND_BLOCK`**: Enum value for AND operations
- **`IROpcode.LOGICAL_OR_BLOCK`**: Enum value for OR operations

#### Generator Classes
- **`ShortCircuitAndGenerator`**: Generates LOGICAL_AND_BLOCK IR structures
- **`ShortCircuitOrGenerator`**: Generates LOGICAL_OR_BLOCK IR structures  
- **`CallDetailsForTrue`**: Helper for consistent Boolean-to-primitive conversion

#### Integration Points
- **`ExprInstrGenerator`**: Uses logical generators for AND/OR expressions
- **`RecordExprProcessing`**: Handles nested expression evaluation with proper memory management
- **Memory Management**: Context-aware RETAIN/SCOPE_REGISTER following first-creation ownership rules

### Testing and Validation

#### Test-Driven Development Pattern
- **`WorkingAreaTest`**: Live testing environment for IR generation validation
- **Complex expression testing**: Mixed AND/OR expressions like `arg0 and (arg1 or arg2)`
- **Memory management validation**: Ensures no duplicate RETAIN/SCOPE_REGISTER calls
- **Formatting validation**: Clean, readable IR output with proper nested indentation

#### Debug Output Analysis
```
// Enable debug instrumentation for detailed IR analysis
mvn test -Dtest=WorkingAreaTest -Dek9.instructionInstrumentation=true
```

**Example debug output shows:**
- **Complete path pre-computation**: All right_evaluation and result_computation instructions visible
- **Proper nesting**: Nested LOGICAL_OR_BLOCK inside LOGICAL_AND_BLOCK correctly formatted
- **Memory management**: Context-aware, non-duplicated memory handling
- **Debug location tracking**: Each instruction annotated with source location

### Future Extensions

The LOGICAL_AND_BLOCK/LOGICAL_OR_BLOCK pattern establishes a foundation for **comprehensive medium-level IR constructs**:

#### Planned Control Flow Extensions
- **`CONDITIONAL_BLOCK`**: if/else statements with similar pre-computation approach
- **`SWITCH_CHAIN_BLOCK`**: EK9 switch statements with sequential case evaluation
- **`WHILE_BLOCK`**: Loop constructs with condition and body pre-computation  
- **`EXCEPTION_BLOCK`**: try/catch with exception handling path pre-computation
- **`ITERATION_BLOCK`**: for loops with iterator and body pre-computation

#### Consistency Benefits
- **Unified IR vocabulary**: All control flow uses similar declarative block structures
- **Common optimization patterns**: Backends can apply similar optimization strategies across all constructs
- **Consistent memory management**: Same context-aware ownership rules apply to all blocks
- **Predictable performance**: Similar CPU architecture optimization opportunities across all control flow

## High-Level IR: QUESTION_BLOCK and GUARDED_ASSIGNMENT_BLOCK

### Overview: Null-Safe Declarative Operations

Following the success of medium-level LOGICAL_BLOCK constructs, EK9 introduces high-level **null-safety operations** that combine EK9's tri-state semantics with explicit backend optimization information.

**Key Innovation**: Explicit `IS_NULL` semantic clarity with complete null/non-null path pre-computation.

### QUESTION_BLOCK Architecture

**Purpose**: Implements EK9's question operator (`?`) for null-safe `_isSet()` checks with explicit null-checking semantics.

**Structure Pattern**:
```
_result = QUESTION_BLOCK [
  operand_evaluation: [
    _operand = LOAD variable
    RETAIN _operand 
    SCOPE_REGISTER _operand, scope
    _null_check = IS_NULL _operand  // Explicit null check
  ]
  operand: _operand
  null_check_condition: _null_check
  null_case_evaluation: [
    _null_result = CALL Boolean._ofFalse()  // Return false for null
    RETAIN _null_result
    SCOPE_REGISTER _null_result, scope
  ]
  null_result: _null_result
  set_case_evaluation: [
    _set_result = CALL _operand._isSet()    // Check if meaningful value
    RETAIN _set_result
    SCOPE_REGISTER _set_result, scope
  ]
  set_result: _set_result
  scope_id: scope
]
```

### GUARDED_ASSIGNMENT_BLOCK Architecture

**Purpose**: Implements EK9's guarded assignment operator (`:=?`) using QUESTION_BLOCK composition for consistent null-safety semantics.

**Semantic Logic**: Assign only if `(LHS == null) OR (!LHS._isSet())`

**Structure Pattern**:
```
_result = GUARDED_ASSIGNMENT_BLOCK [
  condition_evaluation: [
    _lhs_temp = LOAD lhs_variable
    _condition = QUESTION_BLOCK [...]  // Reuse question operator logic
    _assign_condition = CALL _condition._not()  // Invert: assign when unset
    RETAIN _assign_condition
    SCOPE_REGISTER _assign_condition, scope
  ]
  condition_result: _assign_condition
  assignment_evaluation: [
    _value = LOAD_LITERAL assignment_value
    RETAIN _value
    SCOPE_REGISTER _value, scope
    RELEASE lhs_variable              // Standard RELEASE-RETAIN pattern
    STORE lhs_variable, _value
    RETAIN lhs_variable
    SCOPE_REGISTER lhs_variable, scope
  ]
  assignment_result: _result
  scope_id: scope
]
```

### Backend Optimization Benefits

#### Explicit IS_NULL Semantic Advantages

**LLVM Optimization**:
```llvm
; Direct null check mapping
%null_check = icmp eq ptr %operand, null
br i1 %null_check, label %null_case, label %set_case

null_case:
  ; Return Boolean(false) 
  %false_result = call %Boolean* @Boolean_ofFalse()
  br label %merge

set_case:
  ; Call _isSet() method
  %set_result = call i1 @operand_isSet(ptr %operand)  
  br label %merge

merge:
  %final_result = phi %Boolean* [%false_result, %null_case], [%set_result, %set_case]
```

**JVM Optimization**:
```java
// Clean bytecode generation
aload_1          // load operand
ifnull null_case // Direct IS_NULL mapping
invokevirtual isSet  // _isSet() call
goto merge

null_case:
invokestatic Boolean.ofFalse  // Boolean(false)

merge:
// Result on stack
```

#### Composition Benefits

**Code Reuse**: GUARDED_ASSIGNMENT_BLOCK reuses QUESTION_BLOCK logic, ensuring:
- **Consistent null-safety semantics** across operators
- **Single source of truth** for null checking logic  
- **Reduced maintenance burden** with shared implementation
- **Optimization consistency** across both constructs

### Performance Characteristics

#### Redundant Null Check Optimization

**Problem**: Multiple operations on same variable generate repeated null checks:
```ek9
value :=? 42    // First null check in guarded assignment
if value?       // Second null check in question operator  
assert value?   // Third null check in assertion
```

**Solution**: Backend optimization eliminates redundancy:

**LLVM**: EarlyCSE, GVN, and LICM passes eliminate duplicate `IS_NULL` instructions
**JVM**: HotSpot null check elimination removes redundant null testing

**Key Insight**: Explicit `IS_NULL` **enables** backend optimization by providing perfect semantic information.

### Implementation Classes

#### Core IR Instructions
- **`QuestionOperatorInstr`**: QUESTION_BLOCK instruction with explicit null check condition
- **`GuardedAssignmentBlockInstr`**: GUARDED_ASSIGNMENT_BLOCK instruction with composition
- **`IROpcode.QUESTION_BLOCK`**: Enum value for question operator blocks
- **`IROpcode.GUARDED_ASSIGNMENT_BLOCK`**: Enum value for guarded assignment blocks

#### Generator Classes  
- **`QuestionBlockGenerator`**: Generates QUESTION_BLOCK IR with explicit IS_NULL
- **`GuardedAssignmentBlockGenerator`**: Generates GUARDED_ASSIGNMENT_BLOCK using composition
- **`GuardedAssignmentGenerator`**: Simplified wrapper using block generator delegation

#### Integration and Composition
- **Composition Pattern**: GuardedAssignmentBlockGenerator reuses QuestionBlockGenerator logic
- **Semantic Consistency**: Both constructs use identical null-safety evaluation logic
- **Memory Management**: Standard RETAIN/SCOPE_REGISTER patterns with proper cleanup

### Testing and Real-World Usage

**Example Test Case**: `guardedAssignment.ek9`
```ek9
guardedAssignment()
  value as Integer?     // Nullable Integer declaration
  value :=? 1          // Guarded assignment - assign only if null/unset
  assert value?        // Assert value is now set
```

**Generated IR Highlights**:
- **Explicit null checking**: `IS_NULL` instructions provide clear semantics
- **Composition reuse**: Guarded assignment uses QUESTION_BLOCK internally  
- **Backend optimization ready**: Clear control flow for optimization passes
- **Memory safety**: Proper RETAIN/RELEASE/SCOPE_REGISTER patterns

### Strategic Benefits

**Developer Benefits**:
- **Predictable null-safety**: Clear semantics for null handling
- **Composable operations**: Question operator and guarded assignment work together cleanly
- **Debug transparency**: IR inspection shows exact null-checking logic

**Backend Benefits**:
- **Optimization enablement**: Explicit semantics enable superior optimization
- **Target flexibility**: High-level constructs allow target-specific lowering strategies
- **Performance predictability**: Clear performance characteristics across targets

**Compiler Benefits**:
- **Code reuse**: Composition reduces implementation complexity from 76 to ~15 lines
- **Consistency**: Single source of truth for null-safety logic
- **Maintainability**: Shared implementation reduces bug surface area

## Implementation Patterns

### Common IR Transformation Patterns
- **Symbol-Driven Processing**: Always use `parsedModule.getRecordedSymbol(ctx)` instead of text parsing
- **Temporary Variable Generation**: Use `context.generateTempName()` for intermediate results
- **Debug Information Integration**: Apply consistent debug info from `DebugInfoCreator`
- **Type-Safe Operations**: All operations use fully qualified type names

### Error Handling in Code Generation
- **Null Validation**: All generators validate input parameters with `AssertValue.checkNotNull()`
- **Symbol Type Checking**: Verify expected symbol types before processing (e.g., `instanceof AggregateSymbol`)
- **Context Preservation**: Maintain parse context relationships for error reporting

### Resource Management Strategies
- **Scope-Based Cleanup**: Use SCOPE_ENTER/SCOPE_EXIT for automatic memory management
- **Ownership Tracking**: Distinguish between caller-owned (parameters) and function-owned (locals) memory
- **Reference Counting**: Apply RETAIN/RELEASE based on ownership and scope registration

### Testing and Validation Approaches
- **@IR Directive Pattern**: Embed expected IR in EK9 source files for test-driven IR development
- **AbstractIRGenerationTest**: Standard test infrastructure for IR generation validation
- **Debug Instrumentation**: Enable debug output with `-Dek9.instructionInstrumentation=true` for verification

## IR Review and Validation Guidelines

### What Constitutes Correct IR

When reviewing IR generation examples, focus on **structural correctness** rather than apparent "inefficiencies":

#### ✅ **Correct Patterns to Expect**

1. **Multiple LOAD Operations on Same Variable**:
   - Each IR generator loads variables independently
   - Creates complete semantic context for optimization phases
   - Enables simple, maintainable IR generation code

2. **Explicit Memory Management**:
   - Every LOAD gets RETAIN/SCOPE_REGISTER sequence
   - RELEASE operations are safe on uninitialized variables
   - Parameters get REFERENCE only (caller-managed)
   - Return variables use separate return scopes

3. **Medium-Level IR Structures**:
   - LOGICAL_AND_BLOCK/LOGICAL_OR_BLOCK with complete path pre-computation
   - SWITCH_CHAIN_BLOCK with explicit condition evaluation
   - Nested structures maintain proper scope management

#### ❌ **What NOT to Flag as Issues**

1. **"Redundant" Variable Loading**: Multiple LOADs of same variable are correct by design
2. **"Excessive" RETAIN/REGISTER**: Each operation shows complete memory requirements explicitly  
3. **RELEASE on Uninitialized**: Safe operations that avoid explicit null checks
4. **"Verbose" IR Structure**: Semantic clarity is prioritized over conciseness

#### 🔍 **What TO Review For**

1. **Debug Information**: Accurate line:column mapping to source
2. **Scope Hierarchy**: Proper SCOPE_ENTER/SCOPE_EXIT pairing
3. **Memory Ownership**: Parameters, locals, and returns follow documented patterns
4. **IR Structure**: Medium-level constructs follow documented formats

### IR Optimization Strategy Reminder

- **Phase 7 (IR_GENERATION)**: Generate correct, semantically clear IR
- **Phase 12 (IR_OPTIMISATION)**: Eliminate redundancy with global context
- **Backend Phases**: Target-specific optimization with full semantic information

**Key Principle**: Apparent "inefficiencies" in generated IR are features, not bugs. They provide the semantic richness needed for sophisticated optimization in later phases.

---

This document captures the complete IR generation strategy and design philosophy for EK9. All IR generation should follow these patterns to ensure correctness, maintainability, and optimization opportunity preservation.