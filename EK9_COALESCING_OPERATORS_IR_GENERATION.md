# EK9 Coalescing Operators IR Generation

## Overview

This document describes how EK9 coalescing operators (`<?`, `<=?`, `>?`, `>=?`, `??`, `?:`) are transformed into intermediate representation (IR) during compilation. Coalescing operators handle potentially unset values gracefully, providing fallback behavior instead of propagating "unset" states.

## EK9 Coalescing Operators

### Operator Types

1. **`<?`** - Less Than Coalescing: Returns the lesser value if both are set, otherwise returns the set value
2. **`<=?`** - Less Than or Equal Coalescing: Returns the lesser/equal value if both are set
3. **`>?`** - Greater Than Coalescing: Returns the greater value if both are set
4. **`>=?`** - Greater Than or Equal Coalescing: Returns the greater/equal value if both are set
5. **`??`** - Null Coalescing: Returns left if allocated, otherwise right
6. **`?:`** - Elvis Coalescing: Returns left if set, otherwise right

### Coalescing Logic

For comparison coalescing operators (`<?`, `<=?`, `>?`, `>=?`):

1. **If left operand is unset**: Return right operand (regardless of right's state)
2. **If right operand is unset**: Return left operand 
3. **If both operands are set**: Perform comparison and return appropriate value
4. **If both operands are unset**: Return unset value
5. **If comparison fails**: Return unset value

This differs from regular comparison operators which return unset if either operand is unset.

## IR Generation Architecture

### Naming Convention

To avoid conflicts with EK9 user-defined identifiers, all IR-generated names use underscore prefixes:

- **Temporary variables**: `_temp1`, `_temp2`, `_temp3`
- **Scope identifiers**: `_scope_1`, `_property_init_1`
- **Block labels**: `_block_1`, `_coalescing_block_1`
- **Control flow labels**: `_var1_unset_1`, `_end_1`

This is safe because EK9 identifiers cannot start with underscore.

### Unique Name Generation

The `IRGenerationContext` class provides thread-safe unique name generation:

```java
public String generateTempName()           // "_temp1", "_temp2", ...
public String generateScopeId(String prefix)  // "_scope_1", "_property_init_1", ...
public String generateBlockLabel(String prefix) // "_block_1", "_if_then_1", ...
public String generateLabelName(String prefix)  // "_var1_unset_1", "_end_1", ...
```

## Detailed IR Generation Examples

### Example 1: Less Than Coalescing (`<?`)

**EK9 Source:**
```ek9
var1 <- "Goose"  // Set to "Goose"
var2 <- "Duck"   // Set to "Duck"
lesserThan <- var1 <? var2    // Should be "Duck"
```

**Generated IR:**
```
BasicBlock: _coalescing_block_1
  // Load operands
  _temp1 = LOAD var1          // _temp1 = "Goose"
  _temp2 = LOAD var2          // _temp2 = "Duck"
  
  // Check if var1 is set
  _temp3 = CALL _temp1._isSet()    // _temp3 = true
  BRANCH_FALSE _temp3, _var1_unset_1
  
  // var1 is set - check if var2 is set  
  _temp4 = CALL _temp2._isSet()    // _temp4 = true
  BRANCH_FALSE _temp4, _var2_unset_1
  
  // Both are set - do comparison var1 < var2
  _temp5 = CALL _temp1._lt(_temp2)     // _temp5 = false (Goose > Duck)
  _temp6 = CALL _temp5._isSet()        // _temp6 = true
  BRANCH_FALSE _temp6, _comparison_invalid_1
  
  // Comparison is valid - use ternary logic
  BRANCH_TRUE _temp5, _var1_smaller_1  // Skip (var1 not smaller)
  
  // var1 >= var2, so use var2 (the smaller one)
  _temp_result = STORE _temp2          // _temp_result = "Duck"
  BRANCH _end_1
  
_var1_smaller_1:
  // var1 < var2, so use var1
  _temp_result = STORE _temp1
  BRANCH _end_1

_var1_unset_1:
  // var1 is unset - use var2 regardless
  _temp_result = STORE _temp2
  BRANCH _end_1
  
_var2_unset_1:
  // var2 is unset but var1 is set - use var1
  _temp_result = STORE _temp1
  BRANCH _end_1
  
_comparison_invalid_1:
  // Comparison failed - create unset result
  _temp_result = CALL String()  // unset String
  BRANCH _end_1

_end_1:
  // Store final result
  STORE lesserThan, _temp_result       // lesserThan = "Duck"
```

### Example 2: Greater Than Coalescing (`>?`)

**EK9 Source:**
```ek9
greaterThan <- var1 >? var2   // Should be "Goose"
```

**Generated IR:**
```
BasicBlock: _coalescing_block_2
  // Load operands
  _temp7 = LOAD var1          // _temp7 = "Goose"  
  _temp8 = LOAD var2          // _temp8 = "Duck"
  
  // Check if var1 is set
  _temp9 = CALL _temp7._isSet()    // _temp9 = true
  BRANCH_FALSE _temp9, _var1_unset_2
  
  // var1 is set - check if var2 is set  
  _temp10 = CALL _temp8._isSet()   // _temp10 = true
  BRANCH_FALSE _temp10, _var2_unset_2
  
  // Both are set - do comparison var1 > var2
  _temp11 = CALL _temp7._gt(_temp8)    // _temp11 = true (Goose > Duck)
  _temp12 = CALL _temp11._isSet()      // _temp12 = true
  BRANCH_FALSE _temp12, _comparison_invalid_2
  
  // Comparison is valid - use ternary logic
  BRANCH_TRUE _temp11, _var1_greater_2 // Take this branch
  
  // var1 <= var2, so use var2 (the greater one for >? operator)
  _temp_result2 = STORE _temp8
  BRANCH _end_2
  
_var1_greater_2:
  // var1 > var2, so use var1 (the greater one)
  _temp_result2 = STORE _temp7         // _temp_result2 = "Goose"
  BRANCH _end_2

_var1_unset_2:
  // var1 is unset - use var2 regardless
  _temp_result2 = STORE _temp8
  BRANCH _end_2
  
_var2_unset_2:
  // var2 is unset but var1 is set - use var1
  _temp_result2 = STORE _temp7
  BRANCH _end_2
  
_comparison_invalid_2:
  // Comparison failed - create unset result
  _temp_result2 = CALL String()  // unset String
  BRANCH _end_2

_end_2:
  // Store final result
  STORE greaterThan, _temp_result2     // greaterThan = "Goose"
```

## Control Flow Patterns

### Label Uniqueness

Each coalescing expression generates unique labels to avoid conflicts:

- **First expression**: `_var1_unset_1`, `_var2_unset_1`, `_end_1`
- **Second expression**: `_var1_unset_2`, `_var2_unset_2`, `_end_2`
- **Nth expression**: `_var1_unset_N`, `_var2_unset_N`, `_end_N`

This ensures multiple coalescing operators in sequence don't have label conflicts.

### Branch Logic

The IR follows a consistent pattern for all coalescing operators:

1. **Load operands** into temporary variables
2. **Check left operand** using `_isSet()` method
3. **Branch to unset handler** if left is unset
4. **Check right operand** using `_isSet()` method  
5. **Branch to unset handler** if right is unset
6. **Perform comparison** using appropriate method (`_lt`, `_gt`, etc.)
7. **Check comparison validity** using `_isSet()` on result
8. **Branch based on comparison result**
9. **Store appropriate value** and branch to end
10. **Handle unset cases** with fallback logic

## Target Platform Compatibility

### JVM/ASM Compatibility

- **JVM identifiers** allow `_`, letters, digits, and `$`
- Our `_temp1`, `_scope_1` names are **fully valid JVM identifiers**
- No conflicts with bytecode generation using ASM library

**Example JVM bytecode generation:**
```java
// IR: _temp1 = LOAD var1
methodVisitor.visitVarInsn(ALOAD, getLocalVarIndex("var1"));
methodVisitor.visitVarInsn(ASTORE, getLocalVarIndex("_temp1"));
```

### LLVM Compatibility

- **LLVM local identifiers** use `%` prefix: `%_temp1`, `%_scope1`
- **LLVM labels** end with `:`: `%_end_1:`
- Our underscore-prefixed names map perfectly to LLVM naming

**Example LLVM IR generation:**
```llvm
; IR: _temp1 = LOAD var1  →  LLVM:
%_temp1 = load i64, i64* %var1

; IR: BRANCH _end_1  →  LLVM:
br label %_end_1

_end_1:  →  
%_end_1:
```

## Key Design Principles

### 1. Target Agnostic
The IR is designed to be independent of final code generation target (JVM, LLVM, etc.).

### 2. Method-Call Based
All operations, including comparisons, are method calls to support EK9's object-oriented nature.

### 3. Tri-State Handling
The IR properly handles EK9's tri-state semantics (absent, present-unset, present-set).

### 4. SSA Form
Each temporary variable is assigned exactly once (Single Static Assignment).

### 5. Explicit Memory Management
RETAIN, RELEASE, and SCOPE_REGISTER instructions support LLVM targets requiring explicit memory management.

## Testing Verification

The IR generation can be verified by ensuring:

1. **Correct branch targets** - All labels are properly defined and unique
2. **Proper unset handling** - Unset values are handled gracefully without propagation
3. **Method call accuracy** - Appropriate EK9 methods are called (`_isSet`, `_lt`, `_gt`, etc.)
4. **Result correctness** - Final values match expected EK9 coalescing semantics

## Future Extensions

### Additional Coalescing Operators

The same IR generation pattern can be extended for:
- **`<=?`** - Less than or equal coalescing
- **`>=?`** - Greater than or equal coalescing  
- **`??`** - Null coalescing (memory allocation check)
- **`?:`** - Elvis coalescing (set value check)

### Optimization Opportunities

Future optimizations could include:
- **Constant folding** when operands are compile-time constants
- **Branch elimination** when operand set/unset state is known at compile time
- **Temporary variable reduction** through live variable analysis

## Related Documentation

- **EK9_Compiler_Architecture_and_Design.md** - Overall compiler architecture
- **EK9_IR_AND_CODE_GENERATION.md** - IR structure and code generation
- **EK9_LANGUAGE_EXAMPLES.md** - EK9 language syntax and semantics