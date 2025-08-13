# EK9 IR Test-Driven Development Guide

## Overview

This document describes EK9's comprehensive test-driven development approach for IR (Intermediate Representation) generation using embedded IR specifications directly in EK9 source files. This methodology ensures regression-free refactoring and provides living documentation of compiler behavior.

## The @IR Directive System

### Syntax and Structure

The `@IR` directive embeds expected IR output directly into EK9 source files using this format:

```ek9
@IR: <TEST_TYPE>: TYPE: "<fully_qualified_name>" : `
<expected_IR_output>
`
```

**Components:**
- `<TEST_TYPE>` - Descriptive test category (e.g., `IR_GENERATION`, `FOR_LOOP_GENERATION`)
- `<fully_qualified_name>` - Target construct's fully qualified name
- Backtick-delimited multi-line expected IR output

### Example: Basic Class IR Testing

```ek9
@IR: IR_GENERATION: TYPE: "introduction::Example" : `
ConstructDfn: introduction::Example
Field: aField, org.ek9.lang::String  // ./workarea.ek9:67:7
Field: bField, org.ek9.lang::String  // ./workarea.ek9:68:7
OperationDfn: introduction::Example.i_init()->org.ek9.lang::Void
BasicBlock: _entry_1
IRInstruction: REFERENCE this.aField, org.ek9.lang::String  // ./workarea.ek9:67:7
IRInstruction: REFERENCE this.bField, org.ek9.lang::String  // ./workarea.ek9:68:7
IRInstruction: _temp1 = CALL (org.ek9.lang::String)org.ek9.lang::String.<init>()
IRInstruction: RETAIN _temp1
IRInstruction: SCOPE_REGISTER _temp1, _i_init
IRInstruction: STORE this.bField, _temp1
IRInstruction: RETAIN this.bField
IRInstruction: RETURN
`

Example as open
  aField as String?
  bField as String := String()
```

## IR Testing Strategy

### 1. Focused Test Files

Create separate `.ek9` files for each language construct to enable isolated testing:

#### Control Flow Constructs

**forLoopTest.ek9** - For loop IR generation:
```ek9
@IR: FOR_LOOP_GENERATION: TYPE: "test::forExample" : `
OperationDfn: test::forExample.testMethod()->org.ek9.lang::Void
BasicBlock: _entry_1
IRInstruction: SCOPE_ENTER _scope_1
IRInstruction: REFERENCE items, org.ek9.lang::List  
IRInstruction: _temp1 = CALL (org.ek9.lang::Iterator)items.iterator()
IRInstruction: RETAIN _temp1
IRInstruction: SCOPE_REGISTER _temp1, _scope_1
BasicBlock: _loop_condition_2
IRInstruction: _temp2 = CALL (org.ek9.lang::Boolean)_temp1.hasNext()
IRInstruction: BRANCH_IF_FALSE _temp2, _loop_exit_4
BasicBlock: _loop_body_3
IRInstruction: REFERENCE item, org.ek9.lang::String
IRInstruction: _temp3 = CALL (org.ek9.lang::String)_temp1.next()
IRInstruction: STORE item, _temp3
IRInstruction: RETAIN item
IRInstruction: BRANCH _loop_condition_2
BasicBlock: _loop_exit_4
IRInstruction: SCOPE_EXIT _scope_1
IRInstruction: RETURN
`

defines function
  forExample()
    items <- ["a", "b", "c"]
    for item in items
      // simple loop body
```

**ifElseTest.ek9** - Conditional IR generation:
```ek9
@IR: IF_ELSE_GENERATION: TYPE: "test::conditionalExample" : `
OperationDfn: test::conditionalExample.testMethod()->org.ek9.lang::Void
BasicBlock: _entry_1
IRInstruction: SCOPE_ENTER _scope_1
IRInstruction: REFERENCE value, org.ek9.lang::Integer
IRInstruction: _temp1 = LOAD_LITERAL 5, org.ek9.lang::Integer
IRInstruction: STORE value, _temp1
IRInstruction: _temp2 = LOAD value
IRInstruction: _temp3 = LOAD_LITERAL 10, org.ek9.lang::Integer
IRInstruction: _temp4 = CALL (org.ek9.lang::Boolean)_temp2._gt(_temp3)
IRInstruction: BRANCH_IF_FALSE _temp4, _else_branch_3
BasicBlock: _if_branch_2
IRInstruction: _temp5 = LOAD_LITERAL "Greater", org.ek9.lang::String
IRInstruction: BRANCH _merge_point_4
BasicBlock: _else_branch_3
IRInstruction: _temp6 = LOAD_LITERAL "Not Greater", org.ek9.lang::String
BasicBlock: _merge_point_4
IRInstruction: SCOPE_EXIT _scope_1
IRInstruction: RETURN
`

defines function
  conditionalExample()
    value <- 5
    if value > 10
      result <- "Greater"
    else
      result <- "Not Greater"
```

**switchTest.ek9** - Switch statement IR generation:
```ek9
@IR: SWITCH_GENERATION: TYPE: "test::switchExample" : `
OperationDfn: test::switchExample.testMethod()->org.ek9.lang::Void
BasicBlock: _entry_1
IRInstruction: SCOPE_ENTER _scope_1
IRInstruction: REFERENCE value, org.ek9.lang::Integer
IRInstruction: _temp1 = LOAD value
BasicBlock: _switch_case_1_2
IRInstruction: _temp2 = LOAD_LITERAL 1, org.ek9.lang::Integer
IRInstruction: _temp3 = CALL (org.ek9.lang::Boolean)_temp1._eq(_temp2)
IRInstruction: BRANCH_IF_FALSE _temp3, _switch_case_2_3
IRInstruction: _temp4 = LOAD_LITERAL "One", org.ek9.lang::String
IRInstruction: BRANCH _switch_exit_5
BasicBlock: _switch_case_2_3
IRInstruction: _temp5 = LOAD_LITERAL 2, org.ek9.lang::Integer
IRInstruction: _temp6 = CALL (org.ek9.lang::Boolean)_temp1._eq(_temp5)
IRInstruction: BRANCH_IF_FALSE _temp6, _switch_default_4
IRInstruction: _temp7 = LOAD_LITERAL "Two", org.ek9.lang::String
IRInstruction: BRANCH _switch_exit_5
BasicBlock: _switch_default_4
IRInstruction: _temp8 = LOAD_LITERAL "Other", org.ek9.lang::String
BasicBlock: _switch_exit_5
IRInstruction: SCOPE_EXIT _scope_1
IRInstruction: RETURN
`

defines function
  switchExample()
    -> value as Integer
    switch value
      case 1
        result <- "One"
      case 2
        result <- "Two"
      default
        result <- "Other"
```

#### Expression and Operator Testing

**operatorTest.ek9** - Arithmetic and comparison operators:
```ek9
@IR: OPERATOR_GENERATION: TYPE: "test::operatorExample" : `
OperationDfn: test::operatorExample.testMethod()->org.ek9.lang::Void
BasicBlock: _entry_1
IRInstruction: SCOPE_ENTER _scope_1
IRInstruction: REFERENCE a, org.ek9.lang::Integer
IRInstruction: REFERENCE b, org.ek9.lang::Integer
IRInstruction: REFERENCE result, org.ek9.lang::Integer
IRInstruction: _temp1 = LOAD a
IRInstruction: _temp2 = LOAD b
IRInstruction: _temp3 = CALL (org.ek9.lang::Integer)_temp1._add(_temp2)
IRInstruction: STORE result, _temp3
IRInstruction: RETAIN result
IRInstruction: SCOPE_EXIT _scope_1
IRInstruction: RETURN
`

defines function
  operatorExample()
    -> a as Integer
    -> b as Integer
    result <- a + b
```

#### Method Call and Object Creation

**methodCallTest.ek9** - Method invocation patterns:
```ek9
@IR: METHOD_CALL_GENERATION: TYPE: "test::methodCallExample" : `
OperationDfn: test::methodCallExample.testMethod()->org.ek9.lang::Void
BasicBlock: _entry_1
IRInstruction: SCOPE_ENTER _scope_1
IRInstruction: REFERENCE obj, org.ek9.lang::String
IRInstruction: _temp1 = ALLOCATE org.ek9.lang::String
IRInstruction: _temp2 = CALL (org.ek9.lang::String)_temp1.<init>()
IRInstruction: STORE obj, _temp2
IRInstruction: RETAIN obj
IRInstruction: _temp3 = LOAD obj
IRInstruction: _temp4 = LOAD_LITERAL "test", org.ek9.lang::String
IRInstruction: _temp5 = CALL (org.ek9.lang::Boolean)_temp3.contains(_temp4)
IRInstruction: SCOPE_EXIT _scope_1
IRInstruction: RETURN
`

defines function
  methodCallExample()
    obj <- String()
    result <- obj.contains("test")
```

### 2. IR Pattern Predictions

When implementing new language constructs, we can predict IR patterns based on established conventions:

#### Basic Block Naming Conventions
- `_entry_1` - Method entry point
- `_loop_condition_N` - Loop condition checks  
- `_loop_body_N` - Loop body execution
- `_loop_exit_N` - Loop exit point
- `_if_branch_N` - If condition true branch
- `_else_branch_N` - Else condition branch
- `_merge_point_N` - Control flow merge point
- `_switch_case_N_M` - Switch case N, block M
- `_switch_default_N` - Switch default case
- `_switch_exit_N` - Switch statement exit

#### Temporary Variable Patterns
- `_temp1`, `_temp2`, etc. - Sequential temporary variables
- `_temp_literal_N` - Literal value temporaries
- `_temp_call_N` - Method call result temporaries
- `_temp_i_init`, `_temp_super_init` - Initialization temporaries

#### ARC (Automatic Reference Counting) Patterns
- **Object Creation**: `ALLOCATE → Constructor Call → RETAIN → SCOPE_REGISTER`
- **Assignment**: `RELEASE (LHS) → Store → RETAIN (LHS)`
- **Scope Management**: `SCOPE_ENTER → ... → SCOPE_EXIT`
- **Method Parameters**: `RETAIN (on entry) → SCOPE_REGISTER`

#### Memory Management Lifecycle
1. **Creation**: Object starts with reference count = 0
2. **Assignment**: RETAIN increments count, STORE assigns pointer
3. **Scope Registration**: SCOPE_REGISTER tracks for cleanup
4. **Scope Exit**: Automatic RELEASE of all registered objects
5. **Method Return**: SCOPE_EXIT releases all temporaries

### 3. Testing Strategy Benefits

#### Regression Prevention
- **Exact IR Matching**: Any deviation from expected IR immediately detected
- **Refactoring Safety**: Major architectural changes (ANTLR → symbol-based) protected
- **ARC Correctness**: Memory management operations validated mathematically

#### Living Documentation
- **Transparent Behavior**: IR generation visible alongside EK9 source
- **Implementation Guide**: New developers can see exact expected output
- **Pattern Reference**: Consistent IR patterns documented through examples

#### Incremental Development
- **Isolated Testing**: Each construct tested independently
- **Focused Debugging**: IR issues isolated to specific language features
- **Gradual Complexity**: Simple cases first, complex interactions later

### 4. Implementation Workflow

#### Step 1: Create Test File
1. Create focused `.ek9` test file (e.g., `whileLoopTest.ek9`)
2. Write minimal EK9 source for the construct
3. Add empty `@IR` directive as placeholder

#### Step 2: Generate Expected IR
1. Run IR generation on test file
2. Capture actual IR output from compiler
3. Review and validate IR correctness
4. Embed validated IR in `@IR` directive

#### Step 3: Implement IR Validation
1. Compiler parses `@IR` directive during compilation
2. Compare actual IR generation against embedded expectation
3. Report any deviations as compilation errors
4. Ensure bit-for-bit IR matching

#### Step 4: Refactor with Confidence
1. Modify IR generation code (generators, instruction creation, etc.)
2. Run test suite to verify no regressions
3. Update `@IR` specifications only when intentionally changing behavior
4. Maintain comprehensive test coverage

### 5. Advanced Testing Patterns

#### Composite Constructs
For complex interactions, create targeted combination tests:

```ek9
@IR: NESTED_LOOP_GENERATION: TYPE: "test::nestedExample" : `
// Expected IR for nested for loops with break/continue
`

defines function
  nestedExample()
    for i in 1..3
      for j in 1..3
        if i == j
          continue
        if i > 2
          break
```

#### Error Path Testing
Include error handling and exception paths:

```ek9
@IR: EXCEPTION_GENERATION: TYPE: "test::exceptionExample" : `
// Expected IR for try/catch blocks and exception handling
`

defines function
  exceptionExample()
    try
      riskyOperation()
    catch ex as Exception
      handleError(ex)
```

#### Generic Type Instantiation
Test parameterized type IR generation:

```ek9
@IR: GENERIC_INSTANTIATION: TYPE: "test::genericExample" : `
// Expected IR for generic type usage and method calls
`

defines function
  genericExample()
    list <- List() of String
    list.add("item")
```

### 6. File Organization

#### Test Directory Structure
```
src/test/resources/examples/irGeneration/
├── basic/
│   ├── classTest.ek9
│   ├── functionTest.ek9
│   └── programTest.ek9
├── controlFlow/
│   ├── forLoopTest.ek9
│   ├── whileLoopTest.ek9
│   ├── ifElseTest.ek9
│   └── switchTest.ek9
├── expressions/
│   ├── operatorTest.ek9
│   ├── methodCallTest.ek9
│   └── objectCreationTest.ek9
├── advanced/
│   ├── nestedConstructsTest.ek9
│   ├── exceptionHandlingTest.ek9
│   └── genericTypesTest.ek9
└── regression/
    ├── arcCorrectnessTest.ek9
    ├── inheritanceChainTest.ek9
    └── memoryManagementTest.ek9
```

#### Test Categories
- **basic/** - Core language constructs (classes, functions, programs)
- **controlFlow/** - Control flow statements (loops, conditionals, switches)
- **expressions/** - Expression evaluation and operator calls
- **advanced/** - Complex language features (generics, exceptions, composition)
- **regression/** - Specific regression tests for previously fixed issues

### 7. Integration with Development Workflow

#### Before Implementing New Features
1. Create IR specification test file with expected behavior
2. Implement IR generation logic to match specification
3. Validate against embedded `@IR` directive
4. Expand test coverage for edge cases

#### During Refactoring
1. Ensure all existing `@IR` tests pass before changes
2. Make architectural modifications
3. Run full IR test suite to detect regressions
4. Update only intentionally changed specifications

#### Code Review Process
1. Review both EK9 source and embedded IR specifications
2. Verify IR patterns follow established conventions
3. Check ARC operations for mathematical correctness
4. Ensure comprehensive test coverage for new constructs

## Conclusion

The `@IR` directive system provides a comprehensive, regression-safe approach to IR generation development. By embedding expected IR output directly in EK9 source files, we create self-validating, living documentation that ensures compiler behavior remains consistent through major architectural refactoring.

This approach will be critical for the upcoming month of IR generation implementation and refinement, enabling confident development of complex language constructs while maintaining behavioral compatibility.