# EK9 JVM Bytecode Generation - Variable Mapping and Slot Allocation

**Author:** Claude Code (Anthropic AI) with Steve Limb
**Date:** 2025-11-03
**Purpose:** Definitive reference for IR → JVM bytecode variable mapping strategy

---

## Table of Contents

1. [Overview](#overview)
2. [IR Variable Model](#ir-variable-model)
3. [JVM Variable Model](#jvm-variable-model)
4. [Mapping Strategy](#mapping-strategy)
5. [Critical Bugs and Fixes](#critical-bugs-and-fixes)
6. [Implementation Reference](#implementation-reference)
7. [Best Practices](#best-practices)

---

## Overview

The EK9 compiler generates JVM bytecode from an intermediate representation (IR). The IR uses a **name-based variable model** where all variables are referenced by string names, while the JVM uses a **slot-based model** with distinct mechanisms for different variable types.

This document defines the **authoritative mapping strategy** from IR variables to JVM bytecode, documents critical bugs encountered, and provides implementation guidelines.

---

## IR Variable Model

The IR (Intermediate Representation) is **target-agnostic** and uses a simple name-based variable model:

### IR Variable Types

| IR Variable Pattern | Purpose | Example Names |
|---------------------|---------|---------------|
| `parameterName` | Method/constructor parameters | `"initialValue"`, `"count"`, `"name"` |
| `fieldName` | Field access (bare name) | `"value"`, `"name"`, `"count"` |
| `_tempN` | Temporary variables | `"_temp1"`, `"_temp2"`, `"_temp3"` |
| `localVarName` | Named local variables | `"result"`, `"index"`, `"item"` |
| `returnVarName` | Return value variables | `"rtn"`, `"_return"` |

### Key Characteristics

1. **Name-Only References** - IR uses variable names, no slot numbers
2. **No Type Distinction** - IR doesn't distinguish field vs local variable
3. **Target Agnostic** - Same IR works for JVM, LLVM, or other backends
4. **Field References** - Fields referenced as bare names (`"value"`), not `"this.value"`

---

## JVM Variable Model

The JVM uses **distinct mechanisms** for different variable categories:

### 1. Fields (Class Instance Variables)

**Bytecode Instructions:** `getfield`, `putfield`
**Storage:** Not in local variable slots
**Format:** `aload_0; getfield Owner.fieldName:Type`

```
GETFIELD Example:
  0: aload_0              // Load 'this'
  1: getfield #14         // Field value:Lorg/ek9/lang/Integer;
```

```
PUTFIELD Example:
  0: aload_0              // Load 'this'
  1: swap                 // Stack: [this, value]
  2: putfield #14         // Field value:Lorg/ek9/lang/Integer;
```

**Critical:** Fields are **never allocated local variable slots**.

### 2. Method Parameters

**Bytecode Instructions:** `aload_N`, `iload_N`, etc.
**Storage:** Local variable slots starting at index 1 (slot 0 = `this`)
**Pre-registration Required:** YES - Must allocate slots BEFORE processing method body

```
Constructor with Parameter:
  Slot 0: this
  Slot 1: initialValue (parameter)
  Slot 2: (first temp variable)
```

**Critical:** Parameters must be **pre-registered** in `variableMap` to prevent IR from allocating temps in their slots.

### 3. Local Variables (Temps)

**Bytecode Instructions:** `aload_N`, `astore_N`
**Storage:** Local variable slots after parameters
**Allocation:** Sequential, on-demand via `getVariableIndex()`

```
Method Local Variables:
  Slot 0: this
  Slot 1: (first local/temp)
  Slot 2: (second local/temp)
  ...
```

### 4. Return Values

**Bytecode Instructions:** `areturn`, `ireturn`, etc.
**Storage:** Operand stack (not local variables)
**Format:** Value left on stack, return instruction pops and returns it

```
Return Example:
  0: aconst_null
  1: astore_1            // Store to local
  2: aload_1             // Load back to stack
  3: areturn             // Return value from stack
```

---

## Mapping Strategy

### Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    IR Instructions                       │
│  (LOAD value, STORE _temp1, CALL <init>, etc.)         │
└───────────────────┬─────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────┐
│              OutputVisitor / Generators                  │
│  (CallInstrAsmGenerator, LoadInstrAsmGenerator, etc.)  │
└───────────────────┬─────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────┐
│            AbstractAsmGenerator                          │
│  • isFieldAccess() - Field detection                    │
│  • getVariableIndex() - Slot allocation                 │
│  • generateLoadVariable() - ALOAD vs GETFIELD           │
│  • generateStoreVariable() - ASTORE vs PUTFIELD         │
└───────────────────┬─────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────┐
│              MethodContext                               │
│  • variableMap: name → slot                             │
│  • fieldDescriptorMap: "this.fieldName" → descriptor    │
│  • nextVariableSlot: next available slot                │
└───────────────────┬─────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────┐
│              JVM Bytecode                                │
│  (aload_1, getfield #14, putfield #14, etc.)           │
└─────────────────────────────────────────────────────────┘
```

### Field Detection Algorithm

**Critical Decision:** How do we distinguish field references from local variables?

#### Problem

- IR generates field references as **bare field names** (`"value"`)
- Field metadata stores entries with **"this." prefix** (`"this.value"`)
- Original code checked: `variableName.startsWith("this.")` ← **BUG!**

#### Solution (AbstractAsmGenerator.java)

```java
protected boolean isFieldAccess(final String variableName) {
    if (variableName == null) {
        return false;
    }

    // Check explicit "this.fieldName" format
    if (variableName.startsWith("this.")) {
        return true;
    }

    // Check implicit "fieldName" format (IR generates these)
    // Normalize to "this.fieldName" and check metadata map
    final var withPrefix = "this." + variableName;
    return methodContext.fieldDescriptorMap.containsKey(withPrefix);
}
```

**Key Insight:** Check field metadata map for `"this." + variableName` to detect bare field names.

### Slot Allocation Algorithm

**Implemented in:** `AbstractAsmGenerator.getVariableIndex()`

```java
protected int getVariableIndex(final String variableName) {
    return methodContext.variableMap.computeIfAbsent(variableName,
        _ -> methodContext.nextVariableSlot++);
}
```

**Algorithm:**
1. Check if variable name already in map → return existing slot
2. If not found → allocate next available slot, increment counter
3. Sequential allocation ensures no gaps

**Critical:** Parameters must be **pre-registered** before processing method body!

### Parameter Pre-Registration

**Implemented in:** `AsmStructureCreator.processBasicBlock()`

```java
// Pre-register method parameters (prevents temp variables from stealing slots)
// Instance method parameters start at slot 1 (slot 0 is 'this')
int parameterSlot = 1;
for (String paramName : parameterNames) {
    methodContext.variableMap.put(paramName, parameterSlot++);
}
// Update nextVariableSlot to skip parameter slots
methodContext.nextVariableSlot = parameterSlot;
```

**Why Necessary:**
- Without pre-registration, `getVariableIndex("_temp1")` allocates slot 1
- Then `getVariableIndex("initialValue")` allocates slot 2
- But parameter is actually in slot 1 → **parameter overwritten!**

---

## Critical Bugs and Fixes

### Bug #1: Field Names Allocated Local Variable Slots

**Symptom:**
```
DEBUG: Allocating NEW slot 2 for variable 'value'
java.lang.VerifyError: Bad local variable type
  Location: bytecode/test/SimpleClass.<init>(...)@8: aload_2
  Reason: Type top (current frame, locals[2]) is not assignable to reference type
```

**Root Cause:**
- IR references field as bare name: `"value"`
- `isFieldAccess("value")` returned `false` (no "this." prefix)
- Field treated as local variable → allocated slot 2
- Bytecode tried `aload_2` for parameter but only 2 slots exist!

**Fix:**
Enhanced `isFieldAccess()` to check field metadata map:
```java
final var withPrefix = "this." + variableName;
return methodContext.fieldDescriptorMap.containsKey(withPrefix);
```

**Files Modified:**
- `AbstractAsmGenerator.java` - `isFieldAccess()`, `getFieldDescriptor()`, `extractFieldName()`

**Result:** Fields now use `getfield`/`putfield`, never allocated slots.

### Bug #2: Constructor Parameters Overwritten by Temps

**Symptom:**
```
Bytecode:
  8: aconst_null
  9: astore_1          ← Overwrites parameter in slot 1!
 10: aload_2           ← Tries to load parameter from wrong slot!
```

**Root Cause:**
- `processConstructorBasicBlock()` passed **empty parameter list**
- Parameters not pre-registered in `variableMap`
- First temp variable allocated slot 1
- Parameter in slot 1 overwritten with null!

**Fix:**
1. Created `extractParameterNames(OperationInstr)` method
2. Passed parameter names to `processConstructorBasicBlock()`
3. Pre-register parameters before processing body

**Files Modified:**
- `AsmStructureCreator.java` - `generateGeneralConstructorFromIR()`, `generateConstructorFromIR()`, `extractParameterNames()`

**Result:** Parameters correctly pre-registered, temps allocated after parameters.

### Bug #3: Missing Object super() Call for Any-Extending Classes

**Symptom:**
```
java.lang.VerifyError: Bad type on operand stack
  Reason: invokevirtual being called on uninitializedThis
```

**Root Cause:**
- EK9 semantics: Any is universal base type (IR skips super() call)
- JVM reality: Any is interface, classes must extend Object
- Missing Object.<init>() call → `this` never initialized

**Fix:**
1. Added `extendsAny(IRConstruct)` helper
2. Inject Object.<init>() in `generateGeneralConstructorFromIR()` when extending Any
3. JVM-specific implementation detail, IR stays target-agnostic

**Files Modified:**
- `AsmStructureCreator.java` - `generateGeneralConstructorFromIR()`, `extendsAny()`

**Result:** Constructors properly call Object.<init>() first, then i_init(), then body.

### Bug #4: Super Constructor Calls Generated Wrong Bytecode

**Symptom:**
```
Bytecode for super() call:
  0: new SuperClass        ← WRONG! Object already exists!
  1: dup
  2: invokespecial <init>
```

**Root Cause:**
- `CallInstrAsmGenerator` treated ALL `<init>` calls as object construction
- Super calls should be `ALOAD_0 + INVOKESPECIAL`, not `NEW + DUP + INVOKESPECIAL`

**Fix:**
1. Added `isSuperOrThisCall(CallInstr)` detection
2. Added `generateSuperConstructorCall()` method
3. Modified `accept()` to route super calls correctly

**Files Modified:**
- `CallInstrAsmGenerator.java` - `isSuperOrThisCall()`, `generateSuperConstructorCall()`, `accept()`

**Result:** Super calls generate correct `ALOAD_0 + INVOKESPECIAL` bytecode.

---

## Implementation Reference

### Key Classes and Methods

#### AsmStructureCreator.java

**Responsibilities:**
- Generate class structure and methods from IR
- Pre-register constructor parameters
- Inject Object super() for Any-extending classes

**Key Methods:**
- `generateGeneralConstructorFromIR()` - Generate constructor with parameter pre-registration
- `processBasicBlock()` - Set up MethodContext with parameters and field metadata
- `extractParameterNames()` - Extract parameter names from OperationInstr
- `populateFieldMetadata()` - Build field descriptor map
- `extendsAny()` - Detect if class extends Any (needs Object super())

#### AbstractAsmGenerator.java

**Responsibilities:**
- Variable mapping (names → slots or field access)
- Load/store bytecode generation
- Field vs local variable detection

**Key Methods:**
- `isFieldAccess()` - Detect field references (explicit or implicit)
- `getVariableIndex()` - Allocate or lookup local variable slot
- `generateLoadVariable()` - Generate ALOAD vs GETFIELD
- `generateStoreVariable()` - Generate ASTORE vs PUTFIELD
- `extractFieldName()` - Remove "this." prefix if present
- `getFieldDescriptor()` - Lookup field type descriptor

#### CallInstrAsmGenerator.java

**Responsibilities:**
- Generate method/constructor call bytecode
- Distinguish super calls from object construction

**Key Methods:**
- `accept()` - Route call types (super, instance, static, virtual, dispatcher)
- `isSuperOrThisCall()` - Detect super/this constructor calls
- `generateSuperConstructorCall()` - Generate ALOAD_0 + INVOKESPECIAL
- `generateInstanceCall()` - Generate NEW + DUP + INVOKESPECIAL for constructors

### MethodContext Structure

```java
class MethodContext {
    // Variable name → local slot mapping
    Map<String, Integer> variableMap = new HashMap<>();

    // Field metadata: "this.fieldName" → "Ltype/descriptor;"
    Map<String, String> fieldDescriptorMap = new HashMap<>();

    // Next available local variable slot
    int nextVariableSlot = 1;  // Slot 0 reserved for 'this'

    // Label mapping for branches
    Map<String, Label> labelMap = new HashMap<>();

    // Owner class name for field access
    String ownerClassName;
}
```

---

## Best Practices

### 1. Always Pre-Register Method Parameters

**DO:**
```java
// Extract parameter names from operation
final var parameterNames = extractParameterNames(operation);

// Pre-register before processing body
int parameterSlot = 1;
for (String paramName : parameterNames) {
    methodContext.variableMap.put(paramName, parameterSlot++);
}
methodContext.nextVariableSlot = parameterSlot;

// NOW process basic block
processBasicBlock(mv, basicBlock, parameterNames, isConstructor);
```

**DON'T:**
```java
// WRONG: Processing basic block without pre-registering parameters
processBasicBlock(mv, basicBlock, Collections.emptyList(), isConstructor);
// Result: Temp variables overwrite parameter slots!
```

### 2. Field Metadata Must Use "this." Prefix

**DO:**
```java
for (Field field : construct.getFields()) {
    final var fieldName = field.getSymbol().getName();
    final var descriptor = "L" + convertType(fieldType) + ";";
    methodContext.fieldDescriptorMap.put("this." + fieldName, descriptor);
}
```

**DON'T:**
```java
// WRONG: Storing bare field name
methodContext.fieldDescriptorMap.put(fieldName, descriptor);
// Result: isFieldAccess() won't find it!
```

### 3. Field Detection Must Handle Both Formats

**DO:**
```java
protected boolean isFieldAccess(final String variableName) {
    // Check explicit "this.fieldName"
    if (variableName.startsWith("this.")) {
        return true;
    }
    // Check implicit "fieldName"
    return methodContext.fieldDescriptorMap.containsKey("this." + variableName);
}
```

**DON'T:**
```java
// WRONG: Only checking explicit prefix
protected boolean isFieldAccess(final String variableName) {
    return variableName.startsWith("this.");
}
// Result: Bare field names treated as locals!
```

### 4. Constructor Sequence: super() → i_init() → body

**DO:**
```java
// 1. Call super constructor (Object or real superclass)
mv.visitVarInsn(ALOAD, 0);
mv.visitMethodInsn(INVOKESPECIAL, superClass, "<init>", "()V", false);

// 2. Call instance field initializer
mv.visitVarInsn(ALOAD, 0);
mv.visitMethodInsn(INVOKEVIRTUAL, thisClass, "i_init", "()V", false);

// 3. Process constructor body from IR
processConstructorBasicBlock(mv, basicBlock, parameterNames);
```

**DON'T:**
```java
// WRONG: Missing super() call
processConstructorBasicBlock(mv, basicBlock, parameterNames);
// Result: VerifyError - uninitializedThis!
```

### 5. Super Calls Use ALOAD_0, Not NEW

**DO:**
```java
// Super constructor call
mv.visitVarInsn(ALOAD, 0);  // Load 'this'
for (var arg : arguments) {
    generateLoadVariable(arg);
}
mv.visitMethodInsn(INVOKESPECIAL, superClass, "<init>", descriptor, false);
```

**DON'T:**
```java
// WRONG: Using NEW for super call
mv.visitTypeInsn(NEW, superClass);
mv.visitInsn(DUP);
mv.visitMethodInsn(INVOKESPECIAL, superClass, "<init>", descriptor, false);
// Result: Creates new object instead of calling super!
```

### 6. Field Access Always Through GETFIELD/PUTFIELD

**DO:**
```java
if (isFieldAccess(variableName)) {
    // GETFIELD: Load 'this', then getfield
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, ownerClass, fieldName, descriptor);
}
```

**DON'T:**
```java
// WRONG: Using local variable slot for field
mv.visitVarInsn(ALOAD, getVariableIndex(fieldName));
// Result: Field allocated local slot, verification error!
```

---

## Conclusion

The EK9 IR → JVM bytecode mapping requires careful handling of:

1. **Field Detection** - Recognize both explicit (`"this.value"`) and implicit (`"value"`) formats
2. **Parameter Pre-Registration** - Allocate slots BEFORE processing method body
3. **Slot Allocation** - Sequential allocation for locals, never for fields
4. **Constructor Sequences** - super() → i_init() → body (strict order)
5. **Any Handling** - Inject Object super() for classes extending Any (JVM-specific)

Following these patterns ensures correct, verifiable JVM bytecode generation from EK9's target-agnostic IR.

---

**End of Document**
