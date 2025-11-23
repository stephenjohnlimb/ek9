# EK9 IR to LLVM Mapping Guide

**Related Documentation:**
- **`EK9_DUAL_BACKEND_IR_ARCHITECTURE.md`** - How IR supports both JVM and LLVM backends (SSA, ARC)
- **`EK9_CONTROL_FLOW_IR_DESIGN.md`** - Control flow chain architecture and guard integration
- **`EK9_IR_AND_CODE_GENERATION.md`** - General IR generation principles and patterns

## Overview

This document describes how EK9's intermediate representation (IR) maps to LLVM IR, providing the complete bridge from EK9 source code → EK9 IR → LLVM IR. The mapping preserves EK9's memory management semantics, type system, and control flow patterns while leveraging LLVM's optimization capabilities.

## Architecture Philosophy

### Design Principles

**EK9 IR to LLVM Mapping Strategy:**
- **Preserve EK9 semantics**: All EK9 language semantics are maintained in the LLVM translation
- **Leverage LLVM optimizations**: Generate LLVM IR that enables maximum optimization opportunities
- **Manual memory management**: Implement EK9's reference counting system using LLVM runtime calls
- **Type-safe object system**: Map EK9's type hierarchy to LLVM's type system with runtime type information

**Key Architectural Decisions:**
- All EK9 objects represented as `i8*` pointers in LLVM
- Reference counting implemented through runtime library calls
- Control flow constructs lowered to LLVM branches and PHI nodes
- EK9's scope-based memory management mapped to structured cleanup calls

## Memory Management Mapping

### Reference Counting System

EK9's IR includes explicit memory management instructions that map directly to LLVM runtime calls:

**EK9 IR Memory Operations:**
```ir
RETAIN object_variable        // Increment reference count
RELEASE object_variable       // Decrement reference count  
SCOPE_ENTER scope_id         // Enter memory management scope
SCOPE_REGISTER object, scope // Register object for scope cleanup
SCOPE_EXIT scope_id          // Exit scope and cleanup all registered objects
```

**LLVM Implementation:**
```llvm
; Runtime library functions for memory management
declare void @ek9_object_retain(i8*)
declare void @ek9_object_release(i8*)  
declare i8* @ek9_scope_create()
declare void @ek9_scope_register(i8*, i8*)
declare void @ek9_scope_exit(i8*)
```

### Memory Management Patterns

#### Variable Declaration Pattern
**EK9 IR:**
```ir
REFERENCE variableName, typeName    // Variable declaration only - no object yet
```
**LLVM Mapping:**
```llvm
%variableName = alloca i8*          ; Declare pointer to EK9 object
store i8* null, i8** %variableName ; Initialize to null
```

#### Object Creation and Assignment Pattern  
**EK9 IR:**
```ir
_temp1 = LOAD_LITERAL 1, org.ek9.lang::Integer  ; Create Integer(1) object
RETAIN _temp1                                   ; Reference count = 1
SCOPE_REGISTER _temp1, _scope_1                ; Register for cleanup
RELEASE value                                   ; Release previous reference
STORE value, _temp1                            ; Assign new reference
RETAIN value                                   ; Reference count = 2  
SCOPE_REGISTER value, _scope_1                ; Register variable for cleanup
```
**LLVM Mapping:**
```llvm
; Create Integer(1) object
%_temp1 = call i8* @org_ek9_lang_Integer_new_literal(i64 1)

; Increment reference count
call void @ek9_object_retain(i8* %_temp1)

; Register with scope for cleanup
call void @ek9_scope_register(i8* %scope_1, i8* %_temp1)

; Release previous reference
%old_value = load i8*, i8** %value
call void @ek9_object_release(i8* %old_value)

; Store new reference
store i8* %_temp1, i8** %value

; Load and retain new reference  
%value_obj = load i8*, i8** %value
call void @ek9_object_retain(i8* %value_obj)

; Register with scope
call void @ek9_scope_register(i8* %scope_1, i8* %value_obj)
```

## Control Flow Mapping

### CONTROL_FLOW_CHAIN to LLVM Control Flow

EK9's unified CONTROL_FLOW_CHAIN IR construct maps to LLVM's structured control flow:

**EK9 IR CONTROL_FLOW_CHAIN:**
```ir
_temp2 = CONTROL_FLOW_CHAIN  // ./source.ek9:75:7
[
chain_type: "QUESTION_OPERATOR"
condition_chain:
[
  {
    case_scope_id: _scope_1
    case_type: "NULL_CHECK"
    condition_evaluation: [
      _temp3 = LOAD value
      RETAIN _temp3
      SCOPE_REGISTER _temp3, _scope_1
      _temp4 = IS_NULL _temp3
    ]
    primitive_condition: _temp4
    body_evaluation: [
      _temp5 = CALL_STATIC Boolean._ofFalse()
      RETAIN _temp5
      SCOPE_REGISTER _temp5, _scope_1
    ]
    body_result: _temp5
  }
]
default_body_evaluation: [
  _temp6 = CALL Integer._isSet()
  RETAIN _temp6
  SCOPE_REGISTER _temp6, _scope_1
]
default_result: _temp6
]
```

**LLVM Mapping:**
```llvm
; condition_evaluation: Load operand and null check
%_temp3 = load i8*, i8** %value
call void @ek9_object_retain(i8* %_temp3)  
call void @ek9_scope_register(i8* %scope_1, i8* %_temp3)

; IS_NULL check (primitive_condition)
%_temp4 = icmp eq i8* %_temp3, null

; Branch based on null check
br i1 %_temp4, label %null_case, label %set_case

null_case:
; body_evaluation: Create Boolean(false)
%_temp5 = call i8* @org_ek9_lang_Boolean_ofFalse()
call void @ek9_object_retain(i8* %_temp5)
call void @ek9_scope_register(i8* %scope_1, i8* %_temp5)
br label %merge

set_case:
; default_body_evaluation: Call _isSet()
%_temp6 = call i8* @org_ek9_lang_Integer_isSet(i8* %_temp3)
call void @ek9_object_retain(i8* %_temp6)
call void @ek9_scope_register(i8* %scope_1, i8* %_temp6)
br label %merge

merge:
; PHI node to merge results from both paths
%_temp2 = phi i8* [%_temp5, %null_case], [%_temp6, %set_case]
call void @ek9_object_retain(i8* %_temp2)
call void @ek9_scope_register(i8* %scope_1, i8* %_temp2)
```

### Control Flow Optimization Opportunities

**LLVM Optimization Benefits:**
- **Branch prediction**: LLVM can optimize branch patterns based on usage
- **Dead code elimination**: Unused paths in CONTROL_FLOW_CHAIN can be eliminated
- **Constant propagation**: Known constant conditions can collapse entire branches
- **PHI node optimization**: LLVM's SSA form enables advanced optimizations

## Function Structure Mapping

### EK9 Function to LLVM Function

**EK9 IR Function Structure:**
```ir
ConstructDfn: assignments::basicAssignment1()->org.ek9.lang::Void
OperationDfn: assignments::basicAssignment1._call()->org.ek9.lang::Void
BasicBlock: _entry_1
SCOPE_ENTER _scope_1
; ... function body
SCOPE_EXIT _scope_1
RETURN
```

**LLVM Function Mapping:**
```llvm
; EK9 function maps to LLVM function
define void @assignments_basicAssignment1_call() {
entry:
  ; Create scope for memory management
  %scope_1 = call i8* @ek9_scope_create()
  
  ; Function body instructions...
  
  ; Cleanup scope before return
  call void @ek9_scope_exit(i8* %scope_1)
  ret void
}
```

## Type System Mapping

### EK9 Type to LLVM Type Mapping

**Basic Type Mappings:**
- **EK9 Objects**: `i8*` (opaque pointer with runtime type information)
- **Primitive booleans**: `i1` (LLVM boolean)
- **Integer literals**: `i64` for construction, then converted to `i8*` EK9 Integer
- **Scope IDs**: `i8*` (opaque scope management structure)

**Runtime Type Information:**
```llvm
; Each EK9 object has runtime type information
%obj = call i8* @org_ek9_lang_Integer_new_literal(i64 42)
; %obj is i8* but runtime knows it's org.ek9.lang::Integer
```

## Method Call Mapping

### EK9 Method Calls to LLVM Function Calls

**EK9 IR Method Calls:**
```ir
CALL_STATIC (org.ek9.lang::Boolean)org.ek9.lang::Boolean._ofFalse()
CALL (org.ek9.lang::Boolean)object._isSet()
```

**LLVM Mapping:**
```llvm
; Static method call
%result1 = call i8* @org_ek9_lang_Boolean_ofFalse()

; Instance method call  
%result2 = call i8* @org_ek9_lang_Integer_isSet(i8* %object)
```

## Debug Information Mapping

### Source Location Preservation

**EK9 IR Debug Information:**
```ir
_temp1 = LOAD_LITERAL 1, org.ek9.lang::Integer  // ./basicAssignment.ek9:74:14
```

**LLVM Debug Mapping:**
```llvm
%_temp1 = call i8* @org_ek9_lang_Integer_new_literal(i64 1), !dbg !42

; Debug metadata
!42 = !DILocation(line: 74, column: 14, scope: !43)
!43 = !DIFile(filename: "basicAssignment.ek9", directory: "./")
```

## Complete Example: EK9 → IR → LLVM

### EK9 Source Code
```ek9
basicAssignment1()
  value as Integer?
  value: 1
  assert value?
```

### EK9 IR (Generated)
```ir
OperationDfn: assignments::basicAssignment1._call()->org.ek9.lang::Void
BasicBlock: _entry_1
SCOPE_ENTER _scope_1
REFERENCE value, org.ek9.lang::Integer
_temp1 = LOAD_LITERAL 1, org.ek9.lang::Integer
RETAIN _temp1
SCOPE_REGISTER _temp1, _scope_1
RELEASE value
STORE value, _temp1
RETAIN value
SCOPE_REGISTER value, _scope_1
_temp2 = CONTROL_FLOW_CHAIN [...]  ; Question operator value?
RETAIN _temp2
SCOPE_REGISTER _temp2, _scope_1
_temp7 = CALL (org.ek9.lang::Boolean)_temp2._true()
ASSERT _temp7
SCOPE_EXIT _scope_1
RETURN
```

### LLVM IR (Generated)
```llvm
define void @assignments_basicAssignment1_call() {
entry:
  ; Scope management
  %scope_1 = call i8* @ek9_scope_create()
  
  ; Variable declaration
  %value = alloca i8*
  store i8* null, i8** %value
  
  ; Create Integer(1)
  %_temp1 = call i8* @org_ek9_lang_Integer_new_literal(i64 1)
  call void @ek9_object_retain(i8* %_temp1)
  call void @ek9_scope_register(i8* %scope_1, i8* %_temp1)
  
  ; Assignment
  %old_value = load i8*, i8** %value
  call void @ek9_object_release(i8* %old_value)
  store i8* %_temp1, i8** %value
  %value_obj = load i8*, i8** %value
  call void @ek9_object_retain(i8* %value_obj)
  call void @ek9_scope_register(i8* %scope_1, i8* %value_obj)
  
  ; Question operator (value?)
  %_temp3 = load i8*, i8** %value
  call void @ek9_object_retain(i8* %_temp3)
  call void @ek9_scope_register(i8* %scope_1, i8* %_temp3)
  
  %_temp4 = icmp eq i8* %_temp3, null
  br i1 %_temp4, label %null_case, label %set_case

null_case:
  %_temp5 = call i8* @org_ek9_lang_Boolean_ofFalse()
  call void @ek9_object_retain(i8* %_temp5)
  call void @ek9_scope_register(i8* %scope_1, i8* %_temp5)
  br label %merge

set_case:
  %_temp6 = call i8* @org_ek9_lang_Integer_isSet(i8* %_temp3)
  call void @ek9_object_retain(i8* %_temp6)
  call void @ek9_scope_register(i8* %scope_1, i8* %_temp6)
  br label %merge

merge:
  %_temp2 = phi i8* [%_temp5, %null_case], [%_temp6, %set_case]
  call void @ek9_object_retain(i8* %_temp2)
  call void @ek9_scope_register(i8* %scope_1, i8* %_temp2)
  
  ; Assert
  %_temp7 = call i1 @org_ek9_lang_Boolean_true(i8* %_temp2)
  call void @ek9_assert(i1 %_temp7)
  
  ; Cleanup and return
  call void @ek9_scope_exit(i8* %scope_1)
  ret void
}
```

## Runtime Library Requirements

### Required LLVM Runtime Functions

**Memory Management:**
```llvm
declare void @ek9_object_retain(i8*)
declare void @ek9_object_release(i8*)
declare i8* @ek9_scope_create()
declare void @ek9_scope_register(i8*, i8*)
declare void @ek9_scope_exit(i8*)
```

**Assertion Support:**
```llvm  
declare void @ek9_assert(i1)
```

**EK9 Object Constructors:**
```llvm
declare i8* @org_ek9_lang_Integer_new_literal(i64)
declare i8* @org_ek9_lang_Boolean_ofFalse()
```

**EK9 Object Methods:**
```llvm
declare i8* @org_ek9_lang_Integer_isSet(i8*)
declare i1 @org_ek9_lang_Boolean_true(i8*)
```

## Optimization Considerations

### LLVM-Specific Optimizations

**Reference Counting Optimizations:**
- **Retain/Release Pairing**: LLVM can eliminate matched retain/release pairs
- **Scope-based Optimization**: Objects with single-scope lifetime can use stack allocation
- **Dead Object Elimination**: Unused objects can be eliminated entirely

**Control Flow Optimizations:**
- **Branch Prediction**: Profile-guided optimization for CONTROL_FLOW_CHAIN patterns
- **Constant Folding**: Known constant conditions can eliminate entire code paths
- **PHI Node Optimization**: LLVM's SSA form enables advanced data flow analysis

**Memory Layout Optimizations:**
- **Object Pooling**: Frequent allocations can be pooled
- **Escape Analysis**: Stack allocation for objects that don't escape scope  
- **Cache Optimization**: Object layout can be optimized for cache locality
- **Cycle Detection**: Compile-time detection and cleanup of reference cycles across method boundaries

**EK9's Advanced Memory Management**: The combination of reference counting with **compile-time cycle detection** makes EK9's memory management superior to traditional approaches:

- **vs. Traditional Reference Counting**: Eliminates memory leaks from reference cycles
- **vs. Garbage Collection**: Deterministic cleanup with no pause times
- **vs. Manual Memory Management**: Automatic and safe without developer burden

This mapping strategy provides a robust foundation for generating efficient LLVM IR from EK9's intermediate representation while preserving all language semantics, enabling LLVM's powerful optimization capabilities, and delivering **zero-leak memory management** through advanced compile-time analysis.