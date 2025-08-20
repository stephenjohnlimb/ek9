# EK9 Control Flow IR Design

This document provides the comprehensive design specification for EK9's unified control flow intermediate representation using the SWITCH_CHAIN_BLOCK structure. This design unifies if/else, if/else-if, and switch statements into a single, powerful IR construct that enables aggressive backend optimizations while preserving EK9's semantic richness.

**Related Documentation:**
- **`EK9_IR_AND_CODE_GENERATION.md`** - General IR generation principles and patterns
- **`EK9_Compiler_Architecture_and_Design.md`** - Complete architectural specification
- **`CLAUDE.md`** - Main project overview and development guidelines

## Architecture Overview

### Design Philosophy

Following EK9's **declarative IR approach** established by LOGICAL_AND_BLOCK and LOGICAL_OR_BLOCK, the SWITCH_CHAIN_BLOCK provides a unified structure for all sequential conditional evaluation constructs.

**Key Principles:**
- **Pre-computation of all execution paths** - All conditions and bodies are evaluated upfront
- **Backend-agnostic optimization hints** - Semantic information enables target-specific optimizations
- **Consistent memory management** - Same RETAIN/RELEASE/SCOPE patterns across all control flow
- **Unified processing** - Single generator class handles all control flow variants

### Architectural Benefits

1. **Implementation Simplicity**: One generator class, one IR opcode, unified parsing logic
2. **Backend Consistency**: Both JVM and LLVM receive structured optimization information
3. **Semantic Elegance**: Matches EK9's "switch as chained if/else-if" language design
4. **Performance Predictability**: Clear optimization boundaries for different construct types

## SWITCH_CHAIN_BLOCK Structure

### Complete IR Structure Definition

```ir
_temp1 = SWITCH_CHAIN_BLOCK  // ./source.ek9:line:col
[
// Scope management
switch_scope_id: _switch_scope_1
switch_scope_enter: [SCOPE_ENTER _switch_scope_1]

// Variable being evaluated (null for if/else-if)
evaluation_variable: temperature
evaluation_variable_type: "org.ek9.lang::Integer"
evaluation_variable_setup:
[
  _temp2 = CALL someFunction()
  RETAIN _temp2
  SCOPE_REGISTER _temp2, _switch_scope_1
  REFERENCE temperature, org.ek9.lang::Integer  // For declarations
  STORE temperature, _temp2
  RETAIN temperature
  SCOPE_REGISTER temperature, _switch_scope_1
]

// Return variable (for expression form)
return_variable: result
return_variable_type: "org.ek9.lang::String"
return_variable_setup:
[
  REFERENCE result, org.ek9.lang::String
  SCOPE_REGISTER result, _switch_scope_1
]

// Sequential condition evaluation
condition_chain:
[
  {
    case_scope_id: _case_scope_1_1
    case_type: "ENUM_CONSTANT" | "EXPRESSION" | "LITERAL"
    enum_constant: "LimitedEnum.A"    // For enum cases
    enum_ordinal: 0                   // For optimization
    condition_evaluation:
    [
      _temp3 = LOAD_LITERAL 12
      _temp4 = CALL (org.ek9.lang::Boolean)temperature._lt(_temp3)
      _temp5 = CALL (org.ek9.lang::Boolean)_temp4._true()
    ]
    condition_result: _temp4
    primitive_condition: _temp5
    body_evaluation:
    [
      SCOPE_ENTER _case_scope_1_1     // If case has local variables
      _temp6 = LOAD_LITERAL "Moderate"
      RETAIN _temp6
      SCOPE_REGISTER _temp6, _case_scope_1_1
      RELEASE result
      STORE result, _temp6
      RETAIN result
      SCOPE_EXIT _case_scope_1_1      // Clean up case locals
    ]
    body_result: result
  }
  // ... additional conditions
]

// Default/else clause
default_body_evaluation:
[
  _temp10 = LOAD_LITERAL "Default"
  RETAIN _temp10
  SCOPE_REGISTER _temp10, _switch_scope_1
  RELEASE result
  STORE result, _temp10
  RETAIN result
]
default_result: result

// Optimization and semantic hints
chain_type: "SWITCH_ENUM" | "SWITCH" | "IF_ELSE" | "IF_ELSE_IF"
enum_optimization_info:
{
  enum_type: "com.customer.just.switches::LimitedEnum",
  enum_values: ["A", "B", "C", "D"],
  enum_ordinals: [0, 1, 2, 3],
  is_exhaustive: true,
  is_dense: true
}

// Cleanup
switch_scope_exit: [SCOPE_EXIT _switch_scope_1]

// Result
expression_result: result
scope_id: _switch_scope_1
]
```

### Field Definitions

| Field | Purpose | Optional | Usage |
|-------|---------|----------|-------|
| `evaluation_variable` | Variable being switched on | Yes | null for if/else-if |
| `evaluation_variable_setup` | How to compute/initialize the variable | Yes | For `switch var := expr` |
| `return_variable` | Explicit return variable | Yes | For expression form with `<- rtn as Type?` |
| `condition_chain` | Sequential conditions to evaluate | No | Core evaluation logic |
| `default_body_evaluation` | Else/default clause | Yes | Final fallback |
| `chain_type` | Semantic hint for optimization | No | Guides backend code generation |
| `enum_optimization_info` | Enum-specific optimization data | Yes | Enables jump table optimization |

## EK9 Language Mapping

### Grammar Analysis

EK9's grammar naturally maps to SWITCH_CHAIN_BLOCK:

```antlr
// If statement grammar
ifStatement: ifControlBlock (NL+ directive? ELSE ifControlBlock)* elseOnlyBlock?;

// Switch statement grammar  
switchStatementExpression: (SWITCH|GIVEN) preFlowAndControl NL+ INDENT 
  (NL* returningParam)? caseStatement+ (directive? DEFAULT block NL+)? DEDENT;
```

**Mapping:**
- `ifControlBlock` → condition_chain entry
- `(NL+ directive? ELSE ifControlBlock)*` → Multiple condition_chain entries  
- `caseStatement+` → condition_chain entries with enum optimization hints
- `elseOnlyBlock?` / `DEFAULT` → Optional default_body_evaluation

### Control Flow Variants

#### 1. Simple If Statement
```ek9
if condition
  doSomething()
```

```ir
_temp1 = SWITCH_CHAIN_BLOCK
[
evaluation_variable: null
condition_chain: [
  {
    condition_evaluation: [/* boolean expression */],
    body_evaluation: [/* doSomething() */]
  }
]
default_body_evaluation: null
chain_type: "IF_ELSE"
]
```

#### 2. If/Else-If Chain
```ek9  
if value > 10
  result: "High"
else if value > 5
  result: "Medium"
else
  result: "Low"
```

```ir
_temp1 = SWITCH_CHAIN_BLOCK
[
evaluation_variable: null
condition_chain: [
  {
    condition_evaluation: [value._gt(10)],
    body_evaluation: [result = "High"]
  },
  {
    condition_evaluation: [value._gt(5)], 
    body_evaluation: [result = "Medium"]
  }
]
default_body_evaluation: [result = "Low"]
chain_type: "IF_ELSE_IF"
]
```

#### 3. Enum Switch
```ek9
result <- switch enumVal
  <- rtn as String?
  case LimitedEnum.A
    rtn: "Just A"
  case LimitedEnum.B
    rtn: "Just B"
  default
    rtn: "Unknown"
```

```ir
_temp1 = SWITCH_CHAIN_BLOCK
[
evaluation_variable: enumVal
return_variable: rtn
condition_chain: [
  {
    case_type: "ENUM_CONSTANT",
    enum_constant: "LimitedEnum.A",
    enum_ordinal: 0,
    condition_evaluation: [enumVal._eq(LimitedEnum.A)],
    body_evaluation: [rtn = "Just A"]
  },
  {
    case_type: "ENUM_CONSTANT", 
    enum_constant: "LimitedEnum.B",
    enum_ordinal: 1,
    condition_evaluation: [enumVal._eq(LimitedEnum.B)],
    body_evaluation: [rtn = "Just B"]
  }
]
default_body_evaluation: [rtn = "Unknown"]
chain_type: "SWITCH_ENUM"
enum_optimization_info: {
  enum_type: "LimitedEnum",
  enum_values: ["A", "B"], 
  enum_ordinals: [0, 1],
  is_exhaustive: false
}
]
```

#### 4. Complex Switch Expression
```ek9
result <- switch stringVar
  case 'D'
    "Inappropriate"  
  case matches /[nN]ame/
    "Perfect"
  case > "Gandalf"
    "Moderate"
  default
    "Suitable"
```

```ir
_temp1 = SWITCH_CHAIN_BLOCK
[
evaluation_variable: stringVar
condition_chain: [
  {
    case_type: "LITERAL",
    condition_evaluation: [stringVar._eq('D')],
    body_evaluation: ["Inappropriate"]
  },
  {
    case_type: "EXPRESSION", 
    condition_evaluation: [stringVar.matches(/[nN]ame/)],
    body_evaluation: ["Perfect"]
  },
  {
    case_type: "EXPRESSION",
    condition_evaluation: [stringVar._gt("Gandalf")],
    body_evaluation: ["Moderate"]
  }
]
default_body_evaluation: ["Suitable"]
chain_type: "SWITCH"
]
```

## Backend Implementation Strategy

### JVM Backend Optimization

#### Enum Switch → tableswitch
```java
// SWITCH_ENUM with dense ordinals
switch enumVal.ordinal() {
  case 0: return "Just A";    // O(1) jump table lookup
  case 1: return "Just B";
  case 2: return "Just C";
  default: return "Unknown";
}
```

**JVM Bytecode:**
```bytecode
aload_1              // load enumVal
invokevirtual ordinal()I
tableswitch {        // Optimal jump table
  0: case_A
  1: case_B
  2: case_C
  default: default_case
}
```

#### Regular Switch → Sequential Evaluation
```java
// SWITCH with complex expressions
if (stringVar.equals("D")) return "Inappropriate";
if (stringVar.matches("[nN]ame")) return "Perfect";  
if (stringVar.compareTo("Gandalf") > 0) return "Moderate";
return "Suitable";
```

#### If/Else → Conditional Branches
```java
// IF_ELSE_IF chain
if (value > 10) return "High";
if (value > 5) return "Medium";
return "Low";
```

### LLVM Backend Optimization  

#### Enum Switch → LLVM switch Instruction
```llvm
%ordinal = call i32 @enum_ordinal(%enum_val)
switch i32 %ordinal, label %default [
  i32 0, label %case_A
  i32 1, label %case_B
  i32 2, label %case_C
]

case_A:
  ret ptr @string_A
case_B:
  ret ptr @string_B
case_C:
  ret ptr @string_C
default:
  ret ptr @string_default
```

**LLVM Optimizations:**
- Jump tables for dense enum ranges
- Binary search for sparse cases
- Branch prediction hints based on case ordering
- Dead code elimination for unreachable cases

#### Complex Expressions → Optimized Branches
```llvm
; String comparison chain with optimizations
%cmp1 = call i32 @strcmp(%str, %literal_D)
%is_D = icmp eq i32 %cmp1, 0
br i1 %is_D, label %case_D, label %check_regex

check_regex:
%match = call i1 @regex_match(%str, %pattern)
br i1 %match, label %case_regex, label %check_greater
```

### Scope Management Translation

#### JVM: Local Variable Table
```ir
SCOPE_ENTER _switch_scope_1     →  // New local variable scope
REFERENCE temperature, Integer  →  // Local variable slot allocation  
SCOPE_EXIT _switch_scope_1      →  // Mark variables out of scope
```

**JVM Debug Info:**
```bytecode
LocalVariableTable:
  0: temperature, I, start=10, end=50, slot=1  // Switch-scoped
  1: var1, Ljava/lang/String;, start=15, end=20, slot=2  // Case-scoped
```

#### LLVM: Lifetime Intrinsics
```ir
SCOPE_ENTER _switch_scope_1     →  call void @llvm.lifetime.start.p0i8
SCOPE_REGISTER temp, scope      →  // Lifetime tracking
SCOPE_EXIT _switch_scope_1      →  call void @llvm.lifetime.end.p0i8
```

```llvm
%temperature = alloca i32
call void @llvm.lifetime.start.p0i8(i64 4, i8* %temperature)
; ... switch logic ...
call void @llvm.lifetime.end.p0i8(i64 4, i8* %temperature)
```

## Performance Analysis

### Optimization Impact by Switch Type

| Switch Type | Cases | JVM Performance | LLVM Performance | Optimization Strategy |
|------------|-------|----------------|------------------|-------------------|
| **SWITCH_ENUM** (dense) | 4 | O(1) | O(1) | tableswitch/jump table |
| **SWITCH_ENUM** (sparse) | 10 | O(1) | O(log n) | lookupswitch/binary search |
| **SWITCH** (literals) | 4 | O(n) | O(n) | Sequential with short-circuit |
| **SWITCH** (expressions) | 4 | O(n) | O(n) | Sequential evaluation |
| **IF_ELSE_IF** | 4 | O(n) | O(n) | Branch chain with prediction |

### Memory Management Performance

**Benefits:**
- Explicit scope boundaries enable stack allocation optimizations
- Variable lifetime information guides register allocation
- RETAIN/RELEASE patterns provide memory usage hints

**JVM Benefits:**
- Local variable reuse within scopes
- Escape analysis optimizations
- GC pressure reduction through explicit lifetimes

**LLVM Benefits:**
- Stack allocation for scoped variables
- Precise lifetime intrinsics enable optimization
- Better register allocation through liveness analysis

## Real-World Examples Analysis

### From JustSwitch.ek9

#### Exhaustive Enum Switch
```ek9
// Lines 18-33: SimpleExhaustiveEnumerationExpressionSwitch
result <- switch val
  <- rtn as String?
  case LimitedEnum.A
    rtn: "Just A"
  case LimitedEnum.B  
    rtn: "Just B"
  case LimitedEnum.C
    rtn: "Just C" 
  case LimitedEnum.D
    rtn: "Just D"
  default
    rtn: "Val is not set"
```

**Optimization Opportunities:**
- `is_exhaustive: false` (has default for unset case)
- `is_dense: true` (ordinals 0,1,2,3)
- JVM: tableswitch with bounds check for unset
- LLVM: jump table with explicit unset handling

#### Complex Expression Switch
```ek9
// Lines 105-117: ASimpleSwitchStatement
switch conditionVariable
  case < 12
    resultText1 :=? "Moderate"
  case > 10*multiplier
    resultText1 :=? "Very High"
  case 25, 26, 27
    resultText1 :=? "Slightly High"  
  case currentTemperature("GB"), 21, 22, 23, 24
    resultText1 :=? "Perfect"
  default
    resultText1 :=? "Not Suitable"
```

**IR Characteristics:**
- Multiple complex expressions per case
- Function calls in case expressions
- Guarded assignments (`:=?`) in case bodies
- No enum optimization possible

#### Variable Declaration in Switch
```ek9  
// Lines 187-200: ASwitchWithDeclaration
resultText <- switch temperature <- currentTemperature("GB") with temperature
  // temperature only visible within switch
```

**Scope Management:**
```ir
switch_scope_id: _switch_scope_1
evaluation_variable_setup:
[
  SCOPE_ENTER _switch_scope_1
  _temp2 = CALL currentTemperature("GB")
  REFERENCE temperature, org.ek9.lang::Integer
  STORE temperature, _temp2
  SCOPE_REGISTER temperature, _switch_scope_1
]
// ... cases using temperature ...
switch_scope_exit: [SCOPE_EXIT _switch_scope_1]
```

**Semantic Correctness:**
After switch completion, `temperature` is out of scope and can be redeclared:
```ek9
temperature <- "Some other value"  // Legal - different variable
```

## Implementation Guidelines

### Generator Class Architecture

```java
/**
 * Unified generator for all EK9 control flow constructs.
 * Handles if/else, if/else-if, switch statements and their expression forms.
 */
public final class SwitchChainBlockGenerator 
    implements Function<SwitchChainBlockGenerator.ControlFlowDetails, List<IRInstr>> {
    
  public List<IRInstr> generateIfStatement(IfStatementContext ctx) {
    return generateSwitchChainBlock(analyzeIfStatement(ctx));
  }
  
  public List<IRInstr> generateSwitchStatement(SwitchStatementExpressionContext ctx) {
    return generateSwitchChainBlock(analyzeSwitchStatement(ctx));  
  }
  
  private ControlFlowAnalysis analyzeIfStatement(IfStatementContext ctx) {
    return ControlFlowAnalysis.builder()
        .chainType("IF_ELSE_IF")
        .evaluationVariable(null)
        .conditionChain(extractIfConditions(ctx))
        .defaultBody(ctx.elseOnlyBlock())
        .build();
  }
  
  private ControlFlowAnalysis analyzeSwitchStatement(SwitchStatementExpressionContext ctx) {
    final var evalVarSymbol = getEvaluationVariableSymbol(ctx);
    final var chainType = evalVarSymbol.getType().isEnumeration() ? 
        "SWITCH_ENUM" : "SWITCH";
        
    return ControlFlowAnalysis.builder()
        .chainType(chainType)
        .evaluationVariable(evalVarSymbol)
        .enumOptimizationInfo(analyzeEnumInfo(evalVarSymbol, ctx))
        .conditionChain(extractSwitchCases(ctx))
        .defaultBody(ctx.defaultBlock())
        .build();
  }
}
```

### Integration with EK9 IR Pipeline

```java
// In ExprInstrGenerator or similar
public List<IRInstr> visitSwitchStatementExpression(SwitchStatementExpressionContext ctx) {
  final var generator = new SwitchChainBlockGenerator(context, /* dependencies */);
  return generator.generateSwitchStatement(ctx);
}

public List<IRInstr> visitIfStatement(IfStatementContext ctx) {
  final var generator = new SwitchChainBlockGenerator(context, /* dependencies */);
  return generator.generateIfStatement(ctx);
}
```

### Testing Strategy

#### Test File Organization
```
examples/irGeneration/controlFlow/
├── ifStatements/
│   ├── simpleIf.ek9
│   ├── ifElse.ek9
│   ├── ifElseIfChain.ek9
│   └── complexConditions.ek9
├── switchStatements/
│   ├── enumSwitch.ek9
│   ├── stringSwitch.ek9
│   ├── expressionSwitch.ek9
│   └── switchWithDeclaration.ek9
└── expressionForms/
    ├── ifExpression.ek9
    ├── switchExpression.ek9
    └── nestedExpressions.ek9
```

#### Test Pattern Example
```ek9
// simpleEnumSwitch.ek9
@IR: SWITCH_CHAIN_BLOCK_GENERATION: FUNCTION: "test::enumSwitchExample": `
OperationDfn: test::enumSwitchExample._call()->org.ek9.lang::Void
BasicBlock: _entry_1
SCOPE_ENTER _scope_1
_temp1 = SWITCH_CHAIN_BLOCK  // ./simpleEnumSwitch.ek9:10:15
[
evaluation_variable: enumVal
evaluation_variable_type: "test::Color"
condition_chain:
[
  {
    case_type: "ENUM_CONSTANT"
    enum_constant: "Color.RED"
    enum_ordinal: 0
    condition_evaluation:
    [
      _temp2 = LOAD_LITERAL Color.RED
      _temp3 = CALL (org.ek9.lang::Boolean)enumVal._eq(_temp2)
      _temp4 = CALL (org.ek9.lang::Boolean)_temp3._true()
    ]
    condition_result: _temp3
    primitive_condition: _temp4
    body_evaluation:
    [
      _temp5 = LOAD_LITERAL "Red Color"
      RETAIN _temp5
      SCOPE_REGISTER _temp5, _scope_1
    ]
    body_result: _temp5
  }
]
chain_type: "SWITCH_ENUM"
enum_optimization_info:
{
  enum_type: "test::Color",
  enum_values: ["RED", "GREEN", "BLUE"],
  enum_ordinals: [0, 1, 2],
  is_exhaustive: false
}
]
SCOPE_EXIT _scope_1
RETURN`

defines function
  enumSwitchExample()
    -> enumVal as Color
    
    result <- switch enumVal
      case Color.RED
        "Red Color"
      default
        "Other Color"
        
    assert result?
```

### Memory Management Patterns

#### Variable Lifetime Management
```ir
// Switch scope creates isolated lifetime
SCOPE_ENTER _switch_scope_1

// Variables declared in switch setup
REFERENCE switchVar, org.ek9.lang::String
SCOPE_REGISTER switchVar, _switch_scope_1

// Case body scope for complex cases
SCOPE_ENTER _case_scope_1_1
// ... case local variables ...
SCOPE_EXIT _case_scope_1_1

// Switch scope cleanup
SCOPE_EXIT _switch_scope_1  // Cleans up switchVar and any switch-level temps
```

#### Expression Result Handling
```ir
// For expression form, result must escape switch scope
return_variable: result
return_variable_setup:
[
  REFERENCE result, org.ek9.lang::String
  SCOPE_REGISTER result, _parent_scope  // Register in parent scope!
]

// Case assignments to result
body_evaluation:
[
  _temp6 = LOAD_LITERAL "Case Value"
  RELEASE result          // Release old value
  STORE result, _temp6    // Assign new value
  RETAIN result           // Retain for parent scope
]
```

## Future Extensions

### Advanced Optimization Opportunities

1. **Pattern Matching Integration**
   - Extend case_type to support pattern matching
   - Destructuring assignments in case bodies
   - Exhaustiveness checking for pattern coverage

2. **Compile-Time Evaluation**
   - Constant propagation through switch conditions
   - Dead case elimination for impossible conditions
   - Switch statement simplification

3. **Profile-Guided Optimization**  
   - Case reordering based on execution frequency
   - Speculative inlining of hot cases
   - Branch prediction hints from profiling data

### Language Feature Extensions

1. **Guard Expressions**
   ```ek9
   switch value
     case x when x > 0 and x < 10
       "Small positive"
     case x when x.isPrime()
       "Prime number"
   ```

2. **Multiple Switch Variables**
   ```ek9
   switch (x, y)
     case (1, 2)
       "One and Two"
     case (a, b) when a == b
       "Equal values"
   ```

3. **Switch Expressions with Complex Return Types**
   ```ek9
   result <- switch input
     <- rtn as Result of String
     case validInput
       rtn.setOk("Valid")
     default
       rtn.setError("Invalid")
   ```

## Summary

The SWITCH_CHAIN_BLOCK design provides a unified, powerful IR construct that:

- **Consolidates all EK9 control flow** into a single, well-understood structure
- **Enables aggressive backend optimizations** through semantic hints and optimization metadata
- **Preserves EK9's rich semantics** including tri-state logic, variable scoping, and expression forms
- **Maintains implementation simplicity** with a single generator class and unified processing pipeline
- **Provides clear performance characteristics** with predictable optimization boundaries
- **Supports future language evolution** through extensible metadata and structured representation

This design represents the optimal balance between semantic richness, implementation simplicity, and backend optimization potential for EK9's control flow constructs.