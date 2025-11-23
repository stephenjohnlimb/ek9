# EK9 Dual-Backend IR Architecture

This document describes how EK9's Intermediate Representation (IR) is designed to support both JVM (stack-based, garbage collected) and LLVM (SSA-based, ARC memory management) backends from identical IR code.

**Related Documentation:**
- **`EK9_IR_AND_CODE_GENERATION.md`** - General IR generation and code generation guide
- **`EK9_IR_TO_LLVM_MAPPING.md`** - Specific LLVM IR mapping patterns
- **`EK9_CONTROL_FLOW_IR_DESIGN.md`** - Control flow chain architecture and guard integration
- **`EK9_BACKEND_IMPLEMENTATION_ARCHITECTURE.md`** - Program entry point and execution architecture

## Executive Summary

The EK9 IR is **deliberately designed as a dual-backend bridge** with:
- **SSA-ready structure** for LLVM without being pure SSA (allowing simpler JVM codegen)
- **Target-agnostic ARC instructions** that work as no-ops for JVM, real calls for LLVM
- **Unified control flow representation** that maps to both stack-based and SSA-based targets

**Key Design Principle**: Same IR, different code generation. No IR changes needed for either backend.

## 1. SSA Compatibility Analysis

### 1.1 SSA-Ready but Not Pure SSA

The EK9 IR is designed to be **SSA-convertible** without being pure SSA:

| Aspect | EK9 IR Approach | SSA Impact |
|--------|-----------------|------------|
| **Temporary naming** | `_temp1`, `_temp2`, etc. (fresh per operation) | Naturally SSA-compliant |
| **Variable reassignment** | Allowed via STORE instruction | LLVM's mem2reg handles |
| **Merge points** | CONTROL_FLOW_CHAIN identifies them | PHI placement hints available |
| **PHI instruction** | IROpcode.PHI exists | Ready for insertion |

**Why This Design?**
- **JVM backend**: Can ignore SSA complexity (stack-based model)
- **LLVM backend**: Simple conversion - unique temp names + PHI at merge points

### 1.2 Temporary Variable Generation

The `IRContext` class ensures unique temporary naming per scope:

```java
// From IRContext.java
public String generateTempName() {
  return "_temp" + getNextCounterFor("temp");
}
```

**Properties:**
- **Unique within method**: Each method gets fresh counter via copy constructor
- **No reuse**: Never reassigns the same temp variable name
- **Thread-safe**: Uses `ConcurrentHashMap<String, AtomicInteger>`
- **SSA-friendly**: Each assignment creates new name (implicit SSA)

### 1.3 SSA Conversion Path for LLVM

```
EK9 IR (high-level CONTROL_FLOW_CHAIN)
         |
         v
Lower to basic blocks with BRANCH instructions
         |
         v
Insert PHI nodes at merge points (using GuardVariableDetails hints)
         |
         v
Pure SSA form for LLVM IR generation
```

### 1.4 GuardVariableDetails for PHI Placement

The IR provides SSA conversion hints via `GuardVariableDetails`:

```java
/**
 * List of guard variable names that will be reassigned during condition evaluation.
 * Provides SSA conversion hints to LLVM backend for phi node placement.
 */
List<String> guardVariables,
```

This tells the LLVM backend exactly where to insert PHI nodes for guard variables.

## 2. ARC Memory Management Architecture

### 2.1 Unified IR Instructions

The IR uses target-agnostic memory management instructions:

```ir
_temp1 = CALL List<String>.<init>()
RETAIN _temp1                        ; JVM: no-op, LLVM: ek9_object_retain()
SCOPE_REGISTER _temp1, _scope_1     ; JVM: debug metadata, LLVM: track for cleanup
...
SCOPE_EXIT _scope_1                 ; JVM: label only, LLVM: cleanup all registered
```

### 2.2 Backend-Specific Implementation

| IR Instruction | JVM Backend | LLVM Backend |
|---------------|-------------|--------------|
| `RETAIN object` | No-op (GC handles) | `call @ek9_object_retain(i8* %obj)` |
| `RELEASE object` | No-op (GC handles) | `call @ek9_object_release(i8* %obj)` |
| `SCOPE_ENTER scope_id` | Label for LocalVariableTable | `call @ek9_scope_create()` |
| `SCOPE_REGISTER obj, scope` | Variable metadata update | `call @ek9_scope_register()` |
| `SCOPE_EXIT scope_id` | Label placement | Emit cleanup for all registered objects |

### 2.3 Memory Management Pattern in IR

Every object creation/assignment follows this sequence:

```ir
; Step 1: Create/evaluate expression
_temp1 = CALL List<String>.<init>()

; Step 2: Increment reference count
RETAIN _temp1

; Step 3: Register for scope-based cleanup
SCOPE_REGISTER _temp1, _scope_1
```

**Why This Pattern?**
- **JVM**: Steps 2-3 are no-ops; GC handles cleanup automatically
- **LLVM**: Steps 2-3 become actual runtime calls; scope exit triggers cleanup
- **Exception safety**: Scope-based cleanup works for both (GC + ARC)

### 2.4 LLVM Runtime Functions

The LLVM backend maps IR instructions to these runtime functions:

```llvm
; Memory Management
declare void @ek9_object_retain(i8*)      ; RETAIN
declare void @ek9_object_release(i8*)     ; RELEASE
declare i8* @ek9_scope_create()           ; SCOPE_ENTER
declare void @ek9_scope_register(i8*, i8*); SCOPE_REGISTER
declare void @ek9_scope_exit(i8*)         ; SCOPE_EXIT
```

### 2.5 Optimization Markers

The IR includes markers for optimization phases:

```java
NO_RETAIN,           // Skip RETAIN for this object
NO_RELEASE,          // Skip RELEASE for this object
NO_SCOPE_REGISTER,   // Skip SCOPE_REGISTER for this object
TRANSFER_OWNERSHIP,  // Move without ref counting overhead
STACK_ALLOC,         // Allocate on stack instead of heap
```

These enable Phase 12 (IR_OPTIMISATION) to eliminate unnecessary reference counting when escape analysis proves it safe.

## 3. Control Flow Bytecode Generation

### 3.1 JVM Control Flow Patterns

The JVM backend maintains these invariants:

**Stack Empty Invariant:**
- All control flow paths maintain empty JVM stack before/after
- Prevents stack frame imbalance across branches

**Label Uniqueness:**
- Uses `scopeId` (not variable names) for label names
- Pattern: `while_start_scope_5`, `if_next_scope_3`
- Avoids conflicts with nested constructs

**Dispatch Case Tracking:**
- FOR_RANGE labels include direction: `_ascending_`, `_descending_`, `_equal_`
- Pattern: `for_increment_ascending_scope_3`

### 3.2 Control Flow Chain Types

| Chain Type | JVM Implementation | LLVM Mapping |
|------------|-------------------|--------------|
| `QUESTION_OPERATOR` | `ifeq`/`ifne` branches | `icmp eq i8* %obj, null` + `br` |
| `IF_ELSE_IF` | Sequential `ifeq` with labels | Basic blocks + PHI at merge |
| `IF_ELSE_WITH_GUARDS` | Same + guard scope | Same + PHI for guard scope |
| `SWITCH` | Sequential comparison | Same (no tableswitch) |
| `SWITCH_WITH_GUARDS` | IF_ELSE_IF wrapper around switch | Guard entry block + switch blocks |
| `WHILE_LOOP` | `goto` back to start | `br` back to condition block |
| `WHILE_LOOP_WITH_GUARDS` | Same + guard wrapper | Same + guard entry block |
| `DO_WHILE_LOOP` | Body first, then `ifne` | Body block + conditional `br` |
| `FOR_RANGE_POLYMORPHIC` | Three-way dispatch | Three branches at direction check |
| `TRY_CATCH_FINALLY` | Exception table entries | `invoke` + `landingpad` |

### 3.3 Branch Instruction Mapping

For every JVM branch pattern, there's a direct LLVM equivalent:

```java
// JVM Backend
branchIfFalse(condition, nextLabel);  // ILOAD + IFEQ

// Generates:
// iload <condition_index>
// ifeq nextLabel
```

```llvm
; LLVM Backend
%cond_int = load i32, i32* %condition
%cond = icmp eq i32 %cond_int, 0
br i1 %cond, label %next, label %current
```

### 3.4 PHI Node Generation at Merge Points

The IR provides all information needed for PHI insertion:

```ir
CONTROL_FLOW_CHAIN _temp2
[
  chain_type: "IF_ELSE_IF"
  condition_chain: [
    { body_result: _temp5 },  ; Path 1 result
    { body_result: _temp6 }   ; Path 2 result
  ]
  default_result: _temp7      ; Default path result
]
```

LLVM conversion:

```llvm
merge:
  %_temp2 = phi i8* [%_temp5, %path1], [%_temp6, %path2], [%_temp7, %default]
```

### 3.5 Example: Question Operator

**IR:**
```ir
_temp2 = CONTROL_FLOW_CHAIN
[
  chain_type: "QUESTION_OPERATOR"
  condition_chain: [
    {
      case_type: "NULL_CHECK"
      condition_evaluation: [ _temp4 = IS_NULL _temp3 ]
      primitive_condition: _temp4
      body_evaluation: [ _temp5 = CALL Boolean._ofFalse() ]
      body_result: _temp5
    }
  ]
  default_body_evaluation: [ _temp6 = CALL Integer._isSet() ]
  default_result: _temp6
]
```

**JVM Bytecode:**
```
aload <value_index>
ifnull null_case
; not null: call _isSet()
aload <value_index>
invokevirtual org/ek9/lang/Integer._isSet()
goto merge
null_case:
invokestatic org/ek9/lang/Boolean._ofFalse()
merge:
astore <result_index>
```

**LLVM IR:**
```llvm
%_temp3 = load i8*, i8** %value
%_temp4 = icmp eq i8* %_temp3, null
br i1 %_temp4, label %null_case, label %set_case

null_case:
  %_temp5 = call i8* @org_ek9_lang_Boolean_ofFalse()
  br label %merge

set_case:
  %_temp6 = call i8* @org_ek9_lang_Integer_isSet(i8* %_temp3)
  br label %merge

merge:
  %_temp2 = phi i8* [%_temp5, %null_case], [%_temp6, %set_case]
```

## 4. Key Design Decisions Enabling Dual-Backend

### 4.1 All Operations Are Method Calls

```java
/**
 * Key Design Principle: All EK9 operators become method calls (CALL instructions).
 * No primitive arithmetic operations (ADD, SUB, etc.) are included - everything
 * goes through EK9's object method dispatch system.
 */
```

**Impact:**
- **JVM**: `invokevirtual` for all operators
- **LLVM**: VTable dispatch or direct function calls
- **Unified semantics**: Both backends handle polymorphism identically

**Example:**
```ir
; x + y in EK9 becomes:
_temp3 = LOAD x
_temp4 = LOAD y
_temp2 = CALL _temp3._plus(_temp4)  ; Not primitive ADD
```

### 4.2 Explicit Memory Operations

```ir
_temp1 = LOAD value      ; Not implicit stack manipulation
STORE result, _temp1     ; Explicit assignment
```

**Impact:**
- **JVM**: Converts to `aload`/`astore` with slot management
- **LLVM**: Converts to `load`/`store`; mem2reg promotes to registers

### 4.3 Scope-Based Cleanup

```ir
SCOPE_ENTER _scope_1
  ; ... all allocations registered with scope ...
SCOPE_EXIT _scope_1
```

**Impact:**
- **JVM**: Labels for LocalVariableTable (debug info)
- **LLVM**: Cleanup code generation at scope boundaries

### 4.4 Target-Agnostic IR

The IR contains **no backend-specific opcodes**:
- No JVM bytecode references (`aload`, `invokevirtual`)
- No LLVM constructs (`phi`, `br`, `icmp`)
- Both backends see identical IR

## 5. Architecture Diagram

```
                    EK9 Source Code
                          |
                          v
            +---------------------------+
            |  Frontend Phases (0-9)    |
            |  - Parsing, Symbols       |
            |  - Type Resolution        |
            +---------------------------+
                          |
                          v
            +---------------------------+
            |  Phase 10: IR Generation  |
            |  - Target-agnostic IR     |
            |  - CONTROL_FLOW_CHAIN     |
            |  - RETAIN/RELEASE/SCOPE_* |
            +---------------------------+
                          |
          +---------------+---------------+
          |                               |
          v                               v
+-------------------+           +-------------------+
|   JVM Backend     |           |   LLVM Backend    |
|   (Phases 14-16)  |           |   (Future)        |
+-------------------+           +-------------------+
| - Stack-based     |           | - SSA conversion  |
| - No PHI nodes    |           | - PHI insertion   |
| - GC handles mem  |           | - ARC calls       |
| - RETAIN = no-op  |           | - RETAIN = retain |
+-------------------+           +-------------------+
          |                               |
          v                               v
    Java Bytecode                    LLVM IR
    (.class files)               (native binary)
```

## 6. Validation: IR Requirements Checklist

| Requirement | Status | Evidence |
|-------------|--------|----------|
| **SSA-convertible** | YES | Unique temp names, GuardVariableDetails, PHI opcode |
| **ARC for LLVM** | YES | RETAIN/RELEASE/SCOPE_* map directly to runtime calls |
| **Control flow** | YES | CONTROL_FLOW_CHAIN identifies all merge points |
| **Memory safety** | YES | Scope-based cleanup is exception-safe |
| **Target-agnostic** | YES | No backend-specific opcodes in IR |
| **Optimization-ready** | YES | NO_RETAIN/NO_RELEASE/STACK_ALLOC markers exist |

## 7. Implementation Notes for Backend Developers

### 7.1 JVM Backend Implementer Notes

1. **Ignore RETAIN/RELEASE**: Generate no code for these instructions
2. **SCOPE_* for debug info**: Use for LocalVariableTable generation only
3. **Stack balance**: Ensure empty stack before/after CONTROL_FLOW_CHAIN
4. **Label naming**: Always use scopeId for uniqueness, never variable names

### 7.2 LLVM Backend Implementer Notes

1. **Lower CONTROL_FLOW_CHAIN**: Convert to basic blocks with explicit branches
2. **Insert PHI nodes**: At merge points identified by condition_chain/default_result
3. **Use GuardVariableDetails**: For phi placement in guarded constructs
4. **Implement scope cleanup**: Generate cleanup code at SCOPE_EXIT
5. **Runtime library**: Implement `ek9_object_retain`, `ek9_object_release`, etc.

### 7.3 Adding New IR Instructions

When adding new IR instructions, ensure:
1. **No backend assumptions**: Don't assume stack-based or register-based
2. **Memory management**: Include RETAIN/RELEASE where ownership transfers
3. **Scope tracking**: Use SCOPE_REGISTER for allocated objects
4. **SSA hints**: Provide GuardVariableDetails if variables are reassigned

## 8. Future Considerations

### 8.1 Optimization Phase (Phase 12)

When implementing IR optimization:
- **Retain/Release elimination**: Remove matched pairs
- **Escape analysis**: Convert heap allocations to STACK_ALLOC
- **Dead object elimination**: Remove unused allocations entirely
- Both backends benefit from identical optimized IR

### 8.2 Cycle Detection

EK9's compile-time cycle detection for reference counting:
- Analyze object graphs at compile time
- Insert cycle-breaking cleanup code
- LLVM backend benefits; JVM backend ignores

### 8.3 Cross-Backend Testing

The dual-backend architecture enables:
- Same EK9 code compiled to both JVM and LLVM
- Same test cases validate identical behavior
- Confidence that IR is semantically correct

## Conclusion

The EK9 IR is **deliberately architected for dual-backend support**:

1. **SSA-ready without being pure SSA**: Enables simple JVM codegen while supporting LLVM conversion
2. **Target-agnostic ARC**: Same instructions, different interpretations per backend
3. **Unified control flow**: CONTROL_FLOW_CHAIN works for both stack-based and SSA-based targets
4. **Optimization-ready**: Markers enable future optimization passes that benefit both backends

This design allows the JVM backend to proceed with full confidence that the same IR will work for LLVM when that backend is implemented. No IR redesign will be needed.
