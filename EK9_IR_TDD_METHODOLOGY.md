# EK9 IR Test-Driven Development (TDD) Methodology

This document provides comprehensive guidance for using EK9's `@IR` directive for Test-Driven Development in compiler construction. This methodology enables surgical precision testing of IR generation, making compiler development faster, more reliable, and more maintainable.

**Related Documentation:**
- **`CLAUDE.md`** - Main project overview and daily development guidelines
- **`EK9_IR_AND_CODE_GENERATION.md`** - IR generation patterns and backend mapping
- **`EK9_Compiler_Architecture_and_Design.md`** - Complete architectural specification

## Overview: Compiler TDD Revolution

### The Problem with Traditional Compiler Testing

**Traditional Approach Challenges:**
- Large, complex programs make it hard to isolate specific compiler phase bugs
- Debugging requires wading through hundreds of lines of generated output
- Multiple language features interact, masking individual issues
- Regression testing is slow and imprecise
- New developers struggle to understand compiler behavior patterns

**Symptoms:**
- "Hope it works" development instead of "know it works"
- Hours spent debugging complex compilation output
- Difficult to verify incremental improvements
- Hard to build confidence in compiler correctness

### EK9's Solution: Multi-Phase Directive TDD

EK9 provides **embedded test specifications** for multiple compilation phases directly in EK9 source files using various directive types:

**Core Directive Types:**

1. **@IR: IR_GENERATION** - Test intermediate representation generation
2. **@Resolved: [PHASE_NAME]** - Test symbol resolution and type checking  
3. **@Error: [PHASE_NAME]** - Test expected compilation errors
4. **@Complexity: [PHASE_NAME]** - Test code complexity calculations

**Revolutionary Benefits:**
- **Multi-phase testing**: Validate behavior across all 20 compilation phases
- **Surgical precision**: Test exactly one language construct at a time
- **Immediate feedback**: See exact output for minimal code changes
- **Living documentation**: Specifications embedded directly in source
- **Regression protection**: Comprehensive micro-test suite
- **Progressive complexity**: Build from simple to complex patterns

## EK9 Compilation Phase Directives

### Complete Directive Reference

EK9's directive system enables **comprehensive Test-Driven Development** across all compilation phases. Each directive type validates specific compiler behavior and can be embedded directly in EK9 source files.

#### 1. @IR: IR_GENERATION Directives

**Purpose**: Test intermediate representation generation (Phase 7)

**Syntax**:
```ek9
@IR: IR_GENERATION: [CONSTRUCT_TYPE]: "[fully::qualified::name]": `
[Expected IR instructions]
`
```

**Construct Types**:
- **FUNCTION**: Tests function IR generation
- **TYPE**: Tests class/record/component IR generation
- **METHOD**: Tests individual method IR generation

**Examples**:

**Function IR Testing:**
```ek9
@IR: IR_GENERATION: FUNCTION: "test::SimpleFunction": `
ConstructDfn: test::SimpleFunction()->org.ek9.lang::Void
OperationDfn: test::SimpleFunction._call()->org.ek9.lang::Void
BasicBlock: _entry_1
SCOPE_ENTER _scope_1
_temp1 = LOAD_LITERAL true, org.ek9.lang::Boolean
RETAIN _temp1
SCOPE_REGISTER _temp1, _scope_1
_temp2 = CALL (org.ek9.lang::Boolean)_temp1._true()
ASSERT _temp2
SCOPE_EXIT _scope_1
RETURN
`
SimpleFunction()
  assert true
```

**Class IR Testing:**
```ek9
@IR: IR_GENERATION: TYPE: "test::SimpleClass": `
ConstructDfn: test::SimpleClass
Field: field1, org.ek9.lang::String
OperationDfn: test::SimpleClass.c_init()->org.ek9.lang::Void
OperationDfn: test::SimpleClass.i_init()->org.ek9.lang::Void
OperationDfn: test::SimpleClass.SimpleClass()->test::SimpleClass
`
SimpleClass as open
  field1 as String?
```

#### 2. @Resolved: [PHASE_NAME] Directives

**Purpose**: Test symbol resolution and type checking (Phases 1-6)

**Syntax**:
```ek9
@Resolved: [PHASE_NAME]: [CONSTRUCT_TYPE]: "[symbol_name]"
```

**Phase Names**:
- **SYMBOL_DEFINITION** (Phase 1): Basic symbol table creation
- **EXPLICIT_TYPE_SYMBOL_DEFINITION** (Phase 4): Generic type parameterization
- **FULL_RESOLUTION** (Phase 6): Template resolution and method matching

**Construct Types**:
- **TYPE**: Classes, records, components
- **FUNCTION**: Functions and methods  
- **TEMPLATE_TYPE**: Generic classes/records
- **TEMPLATE_FUNCTION**: Generic functions

**Examples**:

**Basic Symbol Definition:**
```ek9
@Resolved: SYMBOL_DEFINITION: TYPE: "SimpleClass"
SimpleClass as open
  field1 as String?
```

**Generic Type Resolution:**
```ek9
@Resolved: EXPLICIT_TYPE_SYMBOL_DEFINITION: TYPE: "List of (String)"
listOfStrings <- List() of String
```

**Template Function Resolution:**
```ek9
@Resolved: SYMBOL_DEFINITION: TEMPLATE_FUNCTION: "GenericFunction"
GenericFunction() of type T
  -> value as T
  <- rtn as T: value
```

**Parameterized Type Creation:**
```ek9
// First define the template
@Resolved: SYMBOL_DEFINITION: TEMPLATE_TYPE: "Optional"
Optional() of type T
  ...

// Then test its parameterization
@Resolved: EXPLICIT_TYPE_SYMBOL_DEFINITION: TYPE: "Optional of (String)"  
stringOpt <- Optional() of String
```

#### 3. @Error: [PHASE_NAME] Directives

**Purpose**: Test expected compilation errors and validation

**Syntax**:
```ek9
@Error: [PHASE_NAME]: [ERROR_TYPE]
// Code that should generate the error
```

**Common Phase Names**:
- **SYMBOL_DEFINITION**: Symbol table construction errors
- **PRE_IR_CHECKS** (Phase 8): Data flow analysis errors
- **FULL_RESOLUTION**: Type resolution and method matching errors

**Common Error Types**:
- **USED_BEFORE_INITIALISED**: Variable used before assignment
- **POSSIBLE_DUPLICATE_ENUMERATED_VALUE**: Duplicate enum values
- **UNSAFE_METHOD_ACCESS**: Accessing methods on unset objects
- **RETURN_NOT_ALWAYS_INITIALISED**: Return value not set in all paths

**Examples**:

**Data Flow Analysis Errors:**
```ek9
TestUninitialisedVariable()
  localVar as Integer?
  
  // Should error - variable never assigned
  @Error: PRE_IR_CHECKS: USED_BEFORE_INITIALISED
  assert localVar?
```

**Symbol Definition Errors:**
```ek9
BadEnumeration
  @Error: SYMBOL_DEFINITION: POSSIBLE_DUPLICATE_ENUMERATED_VALUE
  Value1,
  Value1  // Duplicate!
```

**Unsafe Access Errors:**
```ek9
TestUnsafeAccess()
  -> maybeResult as Result of String?
  
  // Should error - Result might not contain value
  @Error: PRE_IR_CHECKS: UNSAFE_METHOD_ACCESS
  value <- maybeResult.value()
```

#### 4. @Complexity: [PHASE_NAME] Directives

**Purpose**: Test code complexity calculations

**Syntax**:
```ek9
@Complexity: [PHASE_NAME]: [CONSTRUCT_TYPE]: "[function_name]": [expected_score]
```

**Examples**:

**Simple Function Complexity:**
```ek9
@Complexity: PRE_IR_CHECKS: FUNCTION: "simpleFunction": 2
simpleFunction()
  -> condition as Boolean
  <- rtn as Integer: condition <- 1 else 0  // 2 paths = complexity 2
```

**Switch Statement Complexity:**
```ek9
@Complexity: PRE_IR_CHECKS: FUNCTION: "switchFunction": 6
switchFunction()
  -> value as SimpleEnum
  result <- switch value  
    case Alpha
      rtn: "A"
    case Beta  
      rtn: "B"
    case Charlie
      rtn: "C"
    // Default case + 3 explicit cases + exit + unset check = 6 complexity
```

### Directive Integration Patterns

#### Multi-Phase Testing

Test the same construct across multiple phases:

```ek9
// Phase 1: Symbol definition
@Resolved: SYMBOL_DEFINITION: TYPE: "TestClass"

// Phase 7: IR generation  
@IR: IR_GENERATION: TYPE: "test::TestClass": `
ConstructDfn: test::TestClass
Field: value, org.ek9.lang::Integer
`

TestClass as open
  value as Integer?
```

#### Error-Driven Development

Use error directives to validate compiler diagnostics:

```ek9
// Test that the compiler correctly identifies the error
BadFunction()
  unsetVar as String?
  
  @Error: PRE_IR_CHECKS: USED_BEFORE_INITIALISED
  result <- unsetVar.length()  // Should error
```

#### Progressive Complexity Validation

Build complexity understanding incrementally:

```ek9
@Complexity: PRE_IR_CHECKS: FUNCTION: "linear": 1
linear() <- rtn as String: "simple"

@Complexity: PRE_IR_CHECKS: FUNCTION: "conditional": 2  
conditional()
  -> flag as Boolean
  <- rtn as String: flag <- "true" else "false"

@Complexity: PRE_IR_CHECKS: FUNCTION: "multiPath": 4
multiPath()
  -> a as Boolean
  -> b as Boolean
  <- rtn as String?
  
  if a and b
    rtn: "both"
  else if a
    rtn: "a only"  
  else if b
    rtn: "b only"
  // Complexity: 4 paths through the logic
```

## Core TDD Methodology

### 1. Surgical Precision Testing

**Principle**: Isolate individual language constructs in minimal functions.

**Example - Variable Declaration Testing:**
```ek9
// Test ONLY variable declaration with initialization
VariableWithInitialization()
  localVar <- true
  assert localVar

// Test ONLY variable declaration without initialization  
VariableOnlyDeclaration()
  localVar as Boolean?
  localVar: true
  assert localVar

// Test ONLY multiple variable declarations
MultipleVariables()
  var1 <- true
  var2 <- false
  assert var1 and var2
```

**Benefits:**
- **Single concern per test**: Each function tests exactly one construct
- **Precise bug isolation**: Know exactly which feature has the problem
- **Clear failure analysis**: Obvious what broke when tests fail

### 2. Incremental Complexity Building

**Progression Strategy**: Simple → Intermediate → Complex

**Phase 1 - Foundations:**
```
basicExpressions/
├── literalValues.ek9           // true, false, 42, "hello"
├── variableLoad.ek9            // variable access
├── simpleArithmetic.ek9        // a + b
└── basicComparisons.ek9        // a == b
```

**Phase 2 - Combinations:**
```
intermediatePatterns/
├── multipleAssignments.ek9     // a <- b; c <- d
├── nestedExpressions.ek9       // (a + b) * c
├── simpleLogicalOps.ek9        // a and b
└── basicFunctionCalls.ek9      // method()
```

**Phase 3 - Advanced:**
```
complexConstructs/
├── nestedLogicalOps.ek9        // a and (b or c)
├── scopedVariables.ek9         // variables in different scopes
├── conditionalBlocks.ek9       // if/else statements
└── methodChaining.ek9          // obj.method1().method2()
```

**Confidence Building Process:**
1. ✅ **Master foundations**: Get simple cases working perfectly
2. ✅ **Compose patterns**: Combine working patterns into intermediate cases  
3. ✅ **Validate complexity**: Ensure complex cases don't break simple patterns
4. ✅ **Regression protection**: All levels tested continuously

### 3. Immediate Visual Feedback Loop

**Development Cycle:**
```
1. Write minimal EK9 function (3-5 lines)
2. Embed expected IR in @IR directive
3. Run test to see actual vs expected IR
4. Spot discrepancies immediately  
5. Fix IR generation code
6. Verify correction in seconds
```

**Example - Before/After Feedback:**

**Before Fix:**
```ir
REFERENCE localVar, org.ek9.lang::Boolean  // ./file.ek9:41:7
SCOPE_REGISTER localVar, _scope_1          // ./file.ek9:41:7  
REFERENCE localVar, org.ek9.lang::Boolean  // ./file.ek9:41:7  ← DUPLICATE!
RELEASE localVar  // ./file.ek9:41:7                          ← WRONG DEBUG LINE!
```

**After Fix:**
```ir
REFERENCE localVar, org.ek9.lang::Boolean  // ./file.ek9:41:7
SCOPE_REGISTER localVar, _scope_1          // ./file.ek9:41:7
STORE localVar, _temp1                     // ./file.ek9:44:7  ← CORRECT DEBUG LINE!
RETAIN localVar                            // ./file.ek9:44:7
```

**Feedback Speed:**
- **Seconds**: Spot the problem and verify the fix
- **Minutes**: Implement the solution  
- **Not hours/days**: Debugging through complex IR dumps

### 4. Living Documentation Pattern

**Self-Documenting Specifications:**

The `@IR` directive creates **executable documentation**:
- **Expected behavior** embedded directly in source code
- **Version control tracks** IR evolution alongside EK9 language changes
- **New developers** see exactly how constructs should translate
- **Behavioral contracts** for IR generation components

**Documentation Benefits:**
```ek9
// This file documents AND tests variable memory management
@IR: IR_GENERATION: FUNCTION: "test::VariableLifecycle": `
SCOPE_ENTER _scope_1
REFERENCE localVar, org.ek9.lang::Boolean    // Variable slot creation
SCOPE_REGISTER localVar, _scope_1            // Local scope registration  
_temp1 = LOAD_LITERAL true, org.ek9.lang::Boolean  // Literal evaluation
RETAIN _temp1                                 // Literal memory management
SCOPE_REGISTER _temp1, _scope_1              // Literal scope registration
STORE localVar, _temp1                       // Assignment operation
RETAIN localVar                              // Variable memory management
SCOPE_EXIT _scope_1                          // Automatic cleanup
`
VariableLifecycle()
  localVar <- true
```

**Knowledge Preservation:**
- **Architectural decisions** are documented with examples
- **Memory management patterns** are explicitly specified
- **Debug information format** is clearly demonstrated
- **Scope management rules** are validated and documented

## Testing Categories and Patterns

### Basic Language Constructs

**Literal Values:**
```ek9
LiteralBoolean() -> rtn as Boolean: true
LiteralInteger() -> rtn as Integer: 42  
LiteralString() -> rtn as String: "hello"
LiteralDate() -> rtn as Date: 2024-12-25
```

**Variable Operations:**
```ek9
VariableDeclaration() -> localVar <- 42; assert localVar
VariableAssignment() -> localVar as Integer?; localVar: 42; assert localVar  
VariableLoad() -> param as Integer -> assert param
```

**Arithmetic Operations:**
```ek9
Addition() -> a as Integer, b as Integer -> rtn as Integer: a + b
Subtraction() -> a as Integer, b as Integer -> rtn as Integer: a - b
Multiplication() -> a as Integer, b as Integer -> rtn as Integer: a * b
```

### Memory Management Patterns

#### **🎯 Critical Memory Lifetime Distinctions**

| Memory Type | REFERENCE | SCOPE_REGISTER | RELEASE on Reassignment | Lifetime | Cleanup Responsibility |
|-------------|-----------|----------------|-------------------------|----------|------------------------|
| **🔥 Object Properties** | ✅ Required | ❌ **Never** | ✅ Required | Object lifetime | Object GC |
| **Local Variables** | ✅ Required | ✅ **Always** | ✅ Required | Method scope | Method scope |
| **Function Parameters** | ✅ Required | ❌ **Never** | ❌ N/A | Caller scope | Caller |
| **Return Values** | ✅ Required | ❌ **Never** | ❌ N/A | Caller scope | Caller |
| **Temporaries** | ❌ N/A | ✅ Required | ❌ N/A | Method scope | Method scope |

**Local Variables (method-scoped, always scope-registered):**
```ir
REFERENCE localVar, org.ek9.lang::Integer
SCOPE_REGISTER localVar, _scope_1    // ← Local variables get scope registration
```

**Parameters (caller-managed, never scope-registered):**
```ir  
REFERENCE param, org.ek9.lang::Integer
// NO SCOPE_REGISTER - parameters are caller-managed
```

**🎯 Object Properties (object-lifetime, never scope-registered):**
```ir
REFERENCE this.aField, org.ek9.lang::String
_temp1 = LOAD_LITERAL "value", org.ek9.lang::String
RETAIN _temp1
SCOPE_REGISTER _temp1, _scope_1      // ← Temporary is scope-registered
RELEASE this.aField                   // ← Release old property value
STORE this.aField, _temp1
RETAIN this.aField                    // ← Property retained but NOT scope-registered
// NO SCOPE_REGISTER for this.aField - must survive method exit
```

**Return Values (caller-managed, never scope-registered):**
```ir
REFERENCE rtn, org.ek9.lang::Integer  
// NO SCOPE_REGISTER - return values outlive current scope
```

#### **🔥 Property Memory Management Patterns**

**Property Declaration with Initialization:**
```ir
// i_init method - property initialization
REFERENCE this.property, org.ek9.lang::String
_temp1 = CALL (org.ek9.lang::String)org.ek9.lang::String.<init>()
RETAIN _temp1                         // Temporary management
SCOPE_REGISTER _temp1, _i_init_1      // Temporary cleaned up at method exit
STORE this.property, _temp1           // Assign to property
RETAIN this.property                  // Property survives method
// NO SCOPE_REGISTER for this.property - object lifetime, not method lifetime
```

**Property Reassignment in Methods:**
```ir
// Constructor or method - property reassignment
_temp1 = LOAD_LITERAL "new value", org.ek9.lang::String
RETAIN _temp1                         // Secure new value
SCOPE_REGISTER _temp1, _scope_1       // Temporary will be cleaned up
RELEASE this.property                 // 🔥 Critical: Release old property value
STORE this.property, _temp1           // Assign new value
RETAIN this.property                  // Property survives method
// NO SCOPE_REGISTER for this.property - must outlive method scope
```

**Multi-Property Class Pattern:**
```ir
// Perfect isolation - each property independently managed
REFERENCE this.aField, org.ek9.lang::String  // Field 1
REFERENCE this.bField, org.ek9.lang::String  // Field 2  
REFERENCE this.cField, org.ek9.lang::String  // Field 3
REFERENCE this.dField, org.ek9.lang::String  // Field 4
// Each field uses separate temporaries - no cross-contamination
_temp1 = ... // for bField
_temp2 = ... // for cField  
_temp3 = ... // for dField
// All properties retained but never scope-registered
```

### Logical Operations (Medium-Level IR)

**Simple AND/OR:**
```ek9
SimpleAnd() -> a as Boolean, b as Boolean -> rtn as Boolean: a and b
SimpleOr() -> a as Boolean, b as Boolean -> rtn as Boolean: a or b
```

**Expected LOGICAL_AND_BLOCK pattern:**
```ir
_temp_result = LOGICAL_AND_BLOCK
{
  left_operand: _temp_left
  left_condition: _temp_left_primitive
  right_evaluation: { /* right operand instructions */ }
  right_operand: _temp_right
  result_computation: { /* EK9 Boolean._and() call */ }
  logical_result: _temp_and_result
  scope_id: _scope_1
}
```

**Nested Logical Operations:**
```ek9  
NestedLogical() -> a as Boolean, b as Boolean, c as Boolean -> rtn as Boolean: a and (b or c)
```

### Function Call Patterns

**Simple Method Calls:**
```ek9
MethodCall() -> obj as SomeType -> obj.method()
```

**Method Chaining:**
```ek9
MethodChaining() -> obj as SomeType -> obj.method1().method2()
```

**Constructor Calls:**
```ek9
ConstructorCall() -> rtn as SomeType: SomeType()
```

## Debug Information Validation

### Correct Debug Information Patterns

**Variable Declaration:**
```ir
REFERENCE localVar, org.ek9.lang::Boolean  // ./file.ek9:41:7  ← Declaration line
SCOPE_REGISTER localVar, _scope_1          // ./file.ek9:41:7  ← Declaration line
```

**Variable Assignment:**  
```ir
STORE localVar, _temp1                     // ./file.ek9:44:7  ← Assignment line
RETAIN localVar                            // ./file.ek9:44:7  ← Assignment line
```

**Expression Evaluation:**
```ir
_temp1 = CALL method()                     // ./file.ek9:15:12 ← Method call position  
_temp2 = LOAD variable                     // ./file.ek9:16:8  ← Variable access position
```

### Debug Information Accuracy Patterns

#### **🎯 Precise Source Location Mapping**

**Variable Declaration vs Usage:**
```ir
// Declaration operations point to declaration line
REFERENCE localVar, org.ek9.lang::Boolean  // ./file.ek9:38:7  ← Declaration position

// Usage operations point to usage line  
_temp2 = LOAD localVar                     // ./file.ek9:42:7  ← Usage position
RETAIN _temp2                              // ./file.ek9:42:7  ← Usage position
_temp3 = CALL (...)_temp2._true()          // ./file.ek9:42:7  ← Usage position
ASSERT _temp3                              // ./file.ek9:42:7  ← Usage position
```

**Property Operations with Precise Mapping:**
```ir
// Property declaration
REFERENCE this.aField, org.ek9.lang::String     // ./file.ek9:78:7   ← Property name

// Property initialization
_temp1 = LOAD_LITERAL "Steve", org.ek9.lang::String  // ./file.ek9:80:27  ← Literal position
STORE this.aField, _temp1                       // ./file.ek9:80:27  ← Initialization expression

// Property reassignment in constructor
_temp1 = LOAD_LITERAL "New Value", org.ek9.lang::String  // ./file.ek9:84:17  ← New literal
RELEASE this.aField                             // ./file.ek9:84:17  ← Assignment expression
STORE this.aField, _temp1                       // ./file.ek9:84:17  ← Assignment expression
RETAIN this.aField                              // ./file.ek9:84:17  ← Assignment expression
```

**Parameter vs Local Variable Debug Mapping:**
```ir
// Parameter operations point to parameter declaration
REFERENCE arg0, org.ek9.lang::Boolean      // ./file.ek9:33:10  ← Parameter position

// Local variable operations point to local usage
REFERENCE local1, org.ek9.lang::Integer    // ./file.ek9:86:9   ← Local declaration
_temp2 = LOAD_LITERAL 1, org.ek9.lang::Integer  // ./file.ek9:86:19  ← Literal position
STORE local1, _temp2                       // ./file.ek9:86:19  ← Assignment position

// Local reassignment points to reassignment line
_temp3 = LOAD_LITERAL 2, org.ek9.lang::Integer  // ./file.ek9:87:17  ← New literal
RELEASE local1                             // ./file.ek9:87:17  ← Reassignment line
STORE local1, _temp3                       // ./file.ek9:87:17  ← Reassignment line
```

#### **Common Debug Information Issues**

**❌ Wrong Line Numbers:**
- Assignment operations pointing to declaration lines instead of assignment lines
- Method calls pointing to wrong expression positions  
- Memory management operations with incorrect source mapping
- Variable usage operations pointing to declaration instead of usage

**✅ Correct Line Numbers:**
- **Declaration operations** point to declaration syntax position
- **Assignment operations** point to assignment expression, not declaration
- **Usage operations** point to usage location, not declaration
- **Method calls** point to exact call position within expressions
- Each IR instruction maps to precise source location that generated it
- Debug information enables accurate source mapping for backend targets

## File Organization Strategy

### Directory Structure

**Multi-Phase Test Organization:**

```
examples/
├── irGeneration/              # @IR: IR_GENERATION directive tests
│   ├── basicExpressions/      # Foundation IR patterns
│   │   ├── literals/          # true, false, 42, "hello"
│   │   ├── variables/         # load, store, scope management  
│   │   └── arithmetic/        # +, -, *, /, %
│   ├── memoryManagement/      # Memory and scope patterns
│   │   ├── localVariables/    # scope registration rules
│   │   ├── parameters/        # caller-managed memory
│   │   └── returnValues/      # transferred ownership
│   ├── logicalOperations/     # Medium-level IR blocks
│   │   ├── simpleAndOr/      # basic and/or operations
│   │   ├── nestedLogical/    # complex expressions
│   │   └── shortCircuit/     # short-circuit behavior
│   ├── functionCalls/        # Call conventions
│   │   ├── simpleCalls/      # basic method invocation
│   │   ├── constructors/     # object creation
│   │   └── methodChaining/   # fluent interface calls
│   ├── controlFlow/          # if/else, loops, switch
│   │   ├── conditionals/     # CONDITIONAL_BLOCK patterns
│   │   ├── loops/            # WHILE_BLOCK, ITERATION_BLOCK
│   │   └── switches/         # SWITCH_CHAIN_BLOCK  
│   └── integration/          # Complex multi-construct tests
│       ├── realWorldPatterns/  # common programming patterns
│       └── regressionTests/    # bug prevention cases
├── parseAndCompile/           # @Resolved: directive tests
│   ├── basicTypes/           # Simple type resolution
│   │   ├── primitives/       # String, Integer, Boolean
│   │   └── collections/      # List, Dict, Optional
│   ├── genericsUse/          # Generic type parameterization  
│   │   ├── simpleGenerics/   # List of String, Optional of Integer
│   │   └── complexGenerics/  # Multi-parameter generics
│   ├── justResolution/       # Method resolution testing
│   │   ├── functionCalls/    # Function resolution patterns
│   │   ├── methodCalls/      # Class method resolution
│   │   └── operatorCalls/    # Operator resolution
│   └── constructs/           # Language construct resolution
│       ├── classes/          # Class inheritance resolution
│       ├── traits/           # Trait composition resolution
│       └── services/         # Service definition resolution
├── parseButFailCompile/       # @Error: directive tests
│   ├── phase1/               # Symbol definition errors
│   │   ├── duplicateSymbols/ # Duplicate type/function names
│   │   ├── badEnumerations/  # Invalid enum definitions
│   │   └── badDirectives/    # Malformed directive syntax
│   ├── phase2/               # Type hierarchy errors
│   │   ├── circularHierarchy/ # Circular inheritance
│   │   ├── badInheritance/    # Invalid inheritance patterns
│   │   └── badOperatorUse/    # Incorrect operator usage
│   ├── phase3/               # Access control errors
│   │   ├── badAccessChecks/   # Private/protected violations
│   │   └── badOverriding/     # Invalid method overrides
│   ├── phase5/               # Pre-IR analysis errors
│   │   ├── usedBeforeInit/    # USED_BEFORE_INITIALISED
│   │   ├── unsafeAccess/      # UNSAFE_METHOD_ACCESS
│   │   └── loopStatements/    # Control flow errors
│   └── workingarea/          # Development sandbox
└── complexity/               # @Complexity: directive tests
    ├── simpleFunctions/      # Linear complexity validation
    ├── conditionals/         # if/else complexity testing
    ├── loops/                # Loop complexity analysis
    └── switches/             # Switch statement complexity
```

### Naming Conventions

**File Names:**
- **Descriptive**: `variableDeclarationWithInitialization.ek9`
- **Action-focused**: `assignmentToExistingVariable.ek9`  
- **Pattern-specific**: `nestedLogicalAndOrExpression.ek9`

**Function Names:**
- **Clear intent**: `LocalVariableWithInitialization()`
- **Single purpose**: `ParameterLoadAndReturn()`
- **Testable behavior**: `AssertBooleanExpression()`

## Development Workflow

### Creating New IR Tests

**Step 1 - Identify the Construct:**
```
What specific EK9 language feature am I testing?
- Variable declaration? Assignment? Expression evaluation?
- Memory management pattern? Scope handling? Debug information?
```

**Step 2 - Write Minimal EK9 Code:**
```ek9
TestFunction()
  // Absolute minimum code to test the construct
  // Usually 1-4 lines of EK9 code
  // Include assert to force compilation to IR stage
```

**Step 3 - Run and Capture Actual IR:**
```bash
mvn test -Dtest=ExamplesBasicsTest
# Look at generated IR output
```

**Step 4 - Embed Expected IR:**
```ek9
@IR: IR_GENERATION: FUNCTION: "module::TestFunction": `
[Paste and clean up the actual IR]
`
TestFunction()
  // EK9 code
```

**Step 5 - Validate and Refine:**
- Check debug information accuracy
- Verify memory management consistency  
- Ensure pattern matches documented best practices
- Test edge cases and error conditions

### Debugging IR Generation Issues

**Common Issue Categories:**

1. **Memory Management Problems:**
   - Duplicate RETAIN/SCOPE_REGISTER calls
   - Missing memory management for variables
   - Incorrect scope registration patterns

2. **Debug Information Issues:**
   - Wrong line numbers in generated IR
   - Operations pointing to incorrect source locations
   - Missing debug information for instructions

3. **Pattern Inconsistencies:**
   - Different IR patterns for similar constructs
   - Unexpected instruction sequences
   - Incorrect type information or naming

**Debugging Process:**
1. **Isolate**: Create minimal test case showing the issue
2. **Compare**: Look at similar constructs that work correctly  
3. **Analyze**: Identify the specific IR generation component responsible
4. **Fix**: Modify the generator and verify with @IR test
5. **Validate**: Ensure fix doesn't break other patterns

## Advanced Memory Management Patterns

### 🎯 Property Memory Management Mastery

#### **Critical Property Lifetime Understanding**

Properties are **object-scoped, not method-scoped**. This fundamental distinction drives all property memory management patterns:

```ek9
// Property lifetime extends beyond method that creates/modifies it
Example as open
  property as String?         // Object lifetime - survives all methods
  
  Example()                   // Method scope - temporary
    property: "value"         // Assignment survives constructor exit
```

#### **Property Initialization Patterns**

**1. Uninitialized Property (Declaration Only):**
```ir
// Only creates property slot - no initialization
REFERENCE this.property, org.ek9.lang::String  // ./file.ek9:line:col
// No STORE, RETAIN, or SCOPE_REGISTER - property remains uninitialized
```

**2. Constructor Call Initialization:**
```ir
REFERENCE this.property, org.ek9.lang::String
_temp1 = CALL (org.ek9.lang::String)org.ek9.lang::String.<init>()
RETAIN _temp1                    // Secure constructor result
SCOPE_REGISTER _temp1, _i_init_1 // Temporary cleanup in i_init scope
STORE this.property, _temp1      // Assign to property
RETAIN this.property             // 🔥 Property survives i_init method
// NO SCOPE_REGISTER for property - object lifetime, not method lifetime
```

**3. Literal Initialization:**
```ir
REFERENCE this.property, org.ek9.lang::String
_temp1 = LOAD_LITERAL "value", org.ek9.lang::String
RETAIN _temp1                    // Secure literal
SCOPE_REGISTER _temp1, _i_init_1 // Temporary cleanup
STORE this.property, _temp1      // Assign literal to property
RETAIN this.property             // 🔥 Property survives method
```

**4. Direct Assignment Pattern:**
```ir
REFERENCE this.property, org.ek9.lang::String
_temp1 = LOAD_LITERAL "direct", org.ek9.lang::String
RETAIN _temp1                    // Secure value
SCOPE_REGISTER _temp1, _i_init_1 // Temporary cleanup
STORE this.property, _temp1      // Direct assignment
RETAIN this.property             // 🔥 Property survives method
```

#### **Property Reassignment Sophistication**

**Critical RELEASE Pattern:**
```ir
// Property reassignment requires releasing old value
_temp1 = LOAD_LITERAL "new value", org.ek9.lang::String
RETAIN _temp1                    // Secure new value
SCOPE_REGISTER _temp1, _scope_1  // Temporary will be cleaned up
RELEASE this.property            // 🔥 CRITICAL: Release old property value
STORE this.property, _temp1      // Assign new value
RETAIN this.property             // Property survives method
```

**Why RELEASE is Essential:**
- **Memory Safety**: Prevents memory leaks by releasing old property values
- **Reference Counting**: Properly manages object reference counts
- **GC Integration**: Allows garbage collector to reclaim old values
- **Defensive Programming**: Handles both first assignment and reassignment

#### **Multi-Property Isolation Validation**

**Perfect Field Independence:**
```ir
// Four properties - each completely isolated
Field: aField, org.ek9.lang::String    // Property 1
Field: bField, org.ek9.lang::String    // Property 2
Field: cField, org.ek9.lang::String    // Property 3
Field: dField, org.ek9.lang::String    // Property 4

// i_init method - separate temporaries for each property
REFERENCE this.aField, org.ek9.lang::String
REFERENCE this.bField, org.ek9.lang::String
_temp1 = CALL String.<init>()          // bField initialization
_temp2 = LOAD_LITERAL "Steve"          // cField initialization  
_temp3 = LOAD_LITERAL "Stephen"        // dField initialization

// Each property independently managed
STORE this.bField, _temp1; RETAIN this.bField  // bField
STORE this.cField, _temp2; RETAIN this.cField  // cField
STORE this.dField, _temp3; RETAIN this.dField  // dField

// Constructor - property reassignment + local variables
_temp1 = LOAD_LITERAL "Constructor"    // aField reassignment
_temp2 = LOAD_LITERAL 1               // local1 initialization
_temp3 = LOAD_LITERAL 2               // local1 reassignment

// No cross-contamination between properties or with locals
```

#### **Property vs Local Variable Integration**

**Complex Class Constructor Pattern:**
```ek9
Example()
  aField: "Property Value"       // Property reassignment
  local1 <- 1                   // Local variable declaration
  local1: 2                     // Local variable reassignment
  assert aField? and local1?    // Mixed property/local usage
```

**Expected IR Memory Management:**
```ir
// Property reassignment
RELEASE this.aField            // Release old property value
RETAIN this.aField            // Property survives constructor
// NO SCOPE_REGISTER for property

// Local variable management  
RETAIN local1                 // Retain local value
SCOPE_REGISTER local1, _scope_1  // Local cleaned up at method exit

// Both patterns coexist perfectly
```

### 🔥 Advanced Debugging Patterns

#### **Temporary Variable Tracing**

**Method Scope Temporary Numbering:**
```ir
// Constructor method temporaries
_temp1 → aField assignment literal
_temp2 → local1 initialization literal  
_temp3 → local1 reassignment literal
_temp4 → local1._isSet() result
_temp5 → local1 load for assertion
_temp6 → _temp4._true() result
```

**i_init Method Temporary Numbering:**
```ir
// i_init method temporaries
_temp1 → bField constructor call
_temp2 → cField literal value
_temp3 → dField literal value
```

**Pattern Validation:**
- ✅ **Sequential Numbering**: Temporaries numbered sequentially within method scope
- ✅ **Scope Isolation**: Each method scope starts temporary numbering fresh
- ✅ **No Reuse**: No temporary variable reuse conflicts between operations

## Advanced Patterns

### Testing Complex Interactions

**Multi-Property Class Testing:**
```ek9
ComplexPropertyTest()
  Example as open
    aField as String?              // Uninitialized property
    bField as String := String()   // Constructor-initialized
    cField as String := "Literal"  // Literal-initialized
    dField <- "Direct"             // Direct assignment
    
    Example()
      aField: "Constructor"        // Property reassignment
      local1 <- 1                  // Local variable
      local1: 2                    // Local reassignment
      assert aField? and local1?
```

**Expected IR should show:**
- **Property Isolation**: Each property uses separate temporaries
- **Memory Lifetime Distinction**: Properties never scope-registered, locals always scope-registered
- **RELEASE Patterns**: Properties released before reassignment
- **Clean Temporary Management**: All temporaries properly scope-registered and cleaned up

**Multi-Variable Scope Testing:**
```ek9
ComplexScopeTest()
  outerVar <- 42
  if outerVar > 0
    innerVar <- true  
    assert innerVar
  assert outerVar  
```

**Expected IR should show:**
- Proper scope nesting (`_scope_1`, `_scope_2`)
- Correct variable lifetime management
- Appropriate memory cleanup at scope boundaries

**Memory Ownership Testing:**
```ek9
MemoryOwnershipTest() 
  -> param as Boolean        // Caller-managed, no scope registration
  <- rtn as Boolean          // Transferred to caller, no scope registration
  
  localVar <- param          // Local variable, should be scope registered
  rtn: localVar
```

**Property vs Local Variable Distinction Testing:**
```ek9
PropertyLocalTest()
  TestClass as open
    property as String?        // Object-lifetime property
    
    TestClass()
      property: "Property"     // Property assignment - no scope registration
      localVar <- "Local"      // Local variable - scope registration required
      assert property? and localVar?
```

**Expected IR Pattern Validation:**
- **Property**: `RETAIN this.property` but no `SCOPE_REGISTER`
- **Local**: Both `RETAIN localVar` and `SCOPE_REGISTER localVar, _scope_1`
- **Temporaries**: All temporaries scope-registered for cleanup
- **RELEASE**: Both property and local variable released before reassignment

### Regression Prevention

**Known Bug Patterns:**
- Duplicate REFERENCE instructions for variable-only declarations
- Incorrect debug line numbers for assignment operations  
- Memory management duplication in expression processing
- Inconsistent scope registration rules

**Prevention Strategy:**
- Create specific @IR tests for each discovered bug
- Add tests to regression suite to prevent recurrence
- Document the bug pattern and correct solution
- Use tests as specifications for future development

### Performance Validation

**Efficient IR Patterns:**
- Minimal temporary variable generation
- Proper memory management without duplication
- Clean scope handling with appropriate cleanup
- Correct debug information without overhead

**Inefficient Patterns to Avoid:**
- Redundant memory operations
- Unnecessary temporary variables  
- Duplicate instruction generation
- Excessive debug information verbosity

## Integration with EK9 Development

### Continuous Validation

**During Development:**
- Run @IR tests after any IR generation changes
- Validate patterns remain consistent across modifications
- Check debug information accuracy with each change
- Ensure memory management rules are preserved

**Before Commits:**
```bash
# Run all IR generation tests
mvn test -Dtest=ExamplesBasicsTest

# Check specific IR patterns  
mvn test -Dtest=IRGenerationTest

# Validate debug instrumentation
mvn test -Dek9.instructionInstrumentation=true
```

### Knowledge Transfer

**For New Developers:**
1. **Start with basic @IR examples** to understand IR generation patterns  
2. **Study memory management examples** to learn ownership rules
3. **Examine logical operation patterns** to understand medium-level IR design
4. **Create simple test cases** to validate understanding
5. **Graduate to complex constructs** as confidence builds

**For Experienced Developers:**
- Use @IR tests to validate refactoring changes
- Create new patterns for advanced language features
- Document architectural decisions with concrete examples
- Build comprehensive test suites for new constructs

### Future Extensions

**Planned IR Testing Areas:**
- **Control Flow**: if/else, switch, while loops with CONDITIONAL_BLOCK patterns
- **Exception Handling**: try/catch with EXCEPTION_BLOCK patterns  
- **Object-Oriented**: class methods, inheritance, polymorphism
- **Generics**: parameterized types and method resolution
- **Concurrency**: async/await, parallel execution patterns

**Testing Infrastructure Evolution:**
- Automated IR comparison tools
- Visual diff displays for IR changes
- Performance benchmarking for IR generation
- Cross-backend validation (JVM vs LLVM IR compatibility)

## Conclusion

The `@IR` directive TDD methodology represents a **paradigm shift** in compiler development:

**From:**
- "Hope it works" development
- Complex debugging through large IR dumps  
- Slow feedback cycles
- Difficult regression detection

**To:**  
- "Know it works" confidence
- Surgical precision testing
- Immediate feedback and validation
- Comprehensive regression protection

This approach transforms EK9 compiler development into a **fast, reliable, and maintainable process** that builds confidence through systematic validation of every language construct and IR generation pattern.

**Key Success Factors:**
1. **Start small**: Master simple patterns before complex ones
2. **Build incrementally**: Combine working patterns into more advanced constructs  
3. **Validate continuously**: Test every change against established patterns
4. **Document everything**: Use @IR tests as living specifications
5. **Think systematically**: Every language construct needs its @IR test suite

The result is a **comprehensive, self-validating compiler development process** that accelerates development while ensuring correctness at every level.