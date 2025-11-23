# EK9 IR to JVM Bytecode Mapping Guide

## Overview

This document describes how EK9's intermediate representation (IR) maps to JVM bytecode, providing the complete bridge from EK9 source code → EK9 IR → JVM bytecode. The mapping leverages JVM's garbage collection, object model, and optimization capabilities while preserving EK9's type system and control flow semantics.

## Architecture Philosophy

### Design Principles

**EK9 IR to JVM Mapping Strategy:**
- **Leverage JVM garbage collection**: No manual memory management - let JVM handle object lifecycle
- **Direct variable stores**: Use JVM local variable slots for efficient variable access
- **Object-oriented mapping**: Map EK9 objects to JVM objects with method dispatch
- **Type-safe execution**: Maintain EK9's type safety through JVM's type system

**Key Architectural Decisions:**
- All EK9 objects represented as JVM objects (references)
- Memory management handled automatically by JVM garbage collector
- Control flow constructs lowered to JVM branches and direct stores
- EK9's scope-based semantics mapped to JVM local variable management

## Memory Management Mapping

### Simplified Memory Management

EK9's IR includes explicit memory management instructions that are simplified in JVM bytecode:

**EK9 IR Memory Operations:**
```ir
RETAIN object_variable        // Increment reference count
RELEASE object_variable       // Decrement reference count  
SCOPE_ENTER scope_id         // Enter memory management scope
SCOPE_REGISTER object, scope // Register object for scope cleanup
SCOPE_EXIT scope_id          // Exit scope and cleanup all registered objects
```

**JVM Implementation:**
```
// All memory management operations are NO-OPS in JVM
// JVM garbage collector handles object lifecycle automatically
// No explicit retain/release calls needed
// Scope management becomes local variable slot management
```

### Memory Management Patterns

#### Variable Declaration Pattern
**EK9 IR:**
```ir
REFERENCE variableName, typeName    // Variable declaration only - no object yet
```
**JVM Mapping:**
```
// Allocate local variable slot for object reference
// Slot number assigned by JVM local variable allocation
ACONST_NULL          ; Initialize to null
ASTORE 1             ; Store null in local variable slot 1 (variableName)
```

#### Object Creation and Assignment Pattern  
**EK9 IR:**
```ir
_temp1 = LOAD_LITERAL 1, org.ek9.lang::Integer  ; Create Integer(1) object
RETAIN _temp1                                   ; Reference count = 1 (NO-OP in JVM)
SCOPE_REGISTER _temp1, _scope_1                ; Register for cleanup (NO-OP in JVM)
RELEASE value                                   ; Release previous reference (NO-OP in JVM)
STORE value, _temp1                            ; Assign new reference
RETAIN value                                   ; Reference count = 2 (NO-OP in JVM)
SCOPE_REGISTER value, _scope_1                ; Register variable for cleanup (NO-OP in JVM)
```
**JVM Mapping:**
```
// Create Integer(1) object
LCONST_1                    ; Push long constant 1
INVOKESTATIC org/ek9/lang/Integer.newLiteral(J)Lorg/ek9/lang/Integer;

// Store in temporary variable (all memory management calls are NO-OPS)
ASTORE 2                    ; Store in slot 2 (_temp1)

// Assignment: value = _temp1
ALOAD 2                     ; Load _temp1 
ASTORE 1                    ; Store in slot 1 (value)

// No retain/release needed - JVM GC handles lifecycle
```

## Control Flow Mapping

### CONTROL_FLOW_CHAIN to JVM Control Flow

EK9's unified CONTROL_FLOW_CHAIN IR construct maps to JVM's branching instructions:

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

**JVM Mapping:**
```
// condition_evaluation: Load operand and null check (no memory management)
ALOAD 1                     ; Load value from slot 1
ASTORE 3                    ; Store in slot 3 (_temp3)

// IS_NULL check (primitive_condition)
ALOAD 3                     ; Load _temp3
IFNULL null_case            ; Branch if null

// default_body_evaluation: Call _isSet() directly to result variable
ALOAD 3                     ; Load _temp3 for method call
INVOKEVIRTUAL org/ek9/lang/Integer.isSet()Lorg/ek9/lang/Boolean;
ASTORE 2                    ; Store result directly in slot 2 (_temp2)
GOTO end                    ; Skip null case

null_case:
// body_evaluation: Create Boolean(false) directly to result variable
INVOKESTATIC org/ek9/lang/Boolean.ofFalse()Lorg/ek9/lang/Boolean;
ASTORE 2                    ; Store result directly in slot 2 (_temp2)

end:
// _temp2 now contains the result, no merge needed
```

### Control Flow Optimization Benefits

**JVM Optimization Benefits:**
- **Branch prediction**: JVM hotspot can optimize branch patterns
- **Inlining**: Static and instance method calls can be inlined
- **Escape analysis**: Objects that don't escape method can be stack allocated
- **Dead code elimination**: Unused branches can be eliminated by JIT
- **Direct stores**: No PHI-equivalent merging needed, direct variable stores

## Function Structure Mapping

### EK9 Function to JVM Method

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

**JVM Method Mapping:**
```
// EK9 function maps to JVM method
.method public static basicAssignment1_call()V
.limit stack 4
.limit locals 10

  // Scope management is NO-OP in JVM (handled by local variable scoping)
  
  // Function body instructions...
  
  // No explicit scope cleanup needed
  RETURN
.end method
```

## Type System Mapping

### EK9 Type to JVM Type Mapping

**Basic Type Mappings:**
- **EK9 Objects**: JVM object references (`Lorg/ek9/lang/Type;`)
- **Primitive booleans**: JVM boolean (`Z`)
- **Integer literals**: JVM long (`J`) for construction, then object reference
- **Scope IDs**: NO-OP in JVM (local variable scoping handles this)

**Type Information:**
```
// Each EK9 object is a JVM object with full type information
LCONST_1
INVOKESTATIC org/ek9/lang/Integer.newLiteral(J)Lorg/ek9/lang/Integer;
// Result is Lorg/ek9/lang/Integer; with full JVM type safety
```

## Method Call Mapping

### EK9 Method Calls to JVM Method Calls

**EK9 IR Method Calls:**
```ir
CALL_STATIC (org.ek9.lang::Boolean)org.ek9.lang::Boolean._ofFalse()
CALL (org.ek9.lang::Boolean)object._isSet()
```

**JVM Mapping:**
```
// Static method call
INVOKESTATIC org/ek9/lang/Boolean.ofFalse()Lorg/ek9/lang/Boolean;

// Instance method call  
ALOAD 1                     ; Load object reference
INVOKEVIRTUAL org/ek9/lang/Integer.isSet()Lorg/ek9/lang/Boolean;
```

## Debug Information Mapping

### Source Location Preservation

**EK9 IR Debug Information:**
```ir
_temp1 = LOAD_LITERAL 1, org.ek9.lang::Integer  // ./basicAssignment.ek9:74:14
```

**JVM Debug Mapping:**
```
.line 74
LCONST_1
INVOKESTATIC org/ek9/lang/Integer.newLiteral(J)Lorg/ek9/lang/Integer;
ASTORE 2

// Debug information in class file
LocalVariableTable:
  Start  Length  Slot  Name   Signature
      0      20     1  value   Lorg/ek9/lang/Integer;
      5      15     2  _temp1  Lorg/ek9/lang/Integer;

LineNumberTable:
  line 74: 0
  line 75: 8
```

## Complete Example: EK9 → IR → JVM

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

### JVM Bytecode (Generated)
```
.method public static basicAssignment1_call()V
.limit stack 4
.limit locals 5

  // Variable declaration
  ACONST_NULL
  ASTORE 1                  ; value = null
  
  // Create Integer(1) (no memory management calls)
  LCONST_1
  INVOKESTATIC org/ek9/lang/Integer.newLiteral(J)Lorg/ek9/lang/Integer;
  ASTORE 2                  ; _temp1 = Integer(1)
  
  // Assignment
  ALOAD 2                   ; Load _temp1
  ASTORE 1                  ; value = _temp1
  
  // Question operator (value?)
  ALOAD 1                   ; Load value
  ASTORE 3                  ; _temp3 = value
  
  ALOAD 3                   ; Load _temp3
  IFNULL null_case          ; if (_temp3 == null) goto null_case
  
  // set_case: Call _isSet()
  ALOAD 3                   ; Load _temp3 for method call
  INVOKEVIRTUAL org/ek9/lang/Integer.isSet()Lorg/ek9/lang/Boolean;
  ASTORE 4                  ; _temp2 = result
  GOTO merge
  
null_case:
  // Create Boolean(false)
  INVOKESTATIC org/ek9/lang/Boolean.ofFalse()Lorg/ek9/lang/Boolean;
  ASTORE 4                  ; _temp2 = Boolean(false)
  
merge:
  // Assert
  ALOAD 4                   ; Load _temp2
  INVOKEVIRTUAL org/ek9/lang/Boolean.state()Z
  INVOKESTATIC org/ek9/runtime/Assert.assertTrue(Z)V
  
  // Return
  RETURN
.end method
```

## Runtime Library Requirements

### Required JVM Runtime Classes

**EK9 Object Classes:**
```java
// Standard JVM class structure
package org.ek9.lang;

public class Integer {
  public static Integer newLiteral(long value) { /* ... */ }
  public Boolean isSet() { /* ... */ }
  // ... other methods
}

public class Boolean {
  public static Boolean ofFalse() { /* ... */ }
  public boolean state() { /* ... */ }  // Returns primitive boolean
  // ... other methods
}
```

**Assertion Support:**
```java
package org.ek9.runtime;

public class Assert {
  public static void assertTrue(boolean condition) {
    if (!condition) {
      throw new AssertionError("Assertion failed");
    }
  }
}
```

## Optimization Considerations

### JVM-Specific Optimizations

**Garbage Collection Benefits:**
- **Automatic Memory Management**: No manual retain/release overhead
- **Generational GC**: Young objects (temporaries) collected efficiently
- **Reference Tracking**: JVM handles all object lifecycle automatically

**JIT Compilation Optimizations:**
- **Method Inlining**: Frequent method calls can be inlined
- **Branch Prediction**: Hotspot optimizes conditional branches
- **Escape Analysis**: Objects that don't escape can be stack allocated
- **Dead Code Elimination**: Unused variables and branches removed

**Local Variable Optimizations:**
- **Direct Variable Access**: No indirection through scope management
- **Register Allocation**: JVM maps local variables to CPU registers
- **Variable Lifetime Analysis**: Optimal variable slot reuse

**Control Flow Optimizations:**
- **Branch Coalescing**: Multiple conditions can be combined
- **Loop Optimization**: Control flow patterns optimized by hotspot
- **Exception Handling**: JVM's native exception mechanism for assertions

This mapping strategy provides efficient JVM bytecode generation from EK9's intermediate representation while leveraging JVM's automatic memory management, optimization capabilities, and robust object model. The simplified approach eliminates manual memory management overhead while preserving all EK9 language semantics.