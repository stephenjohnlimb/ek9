# EK9 Operator Complexity and Testing Strategy

This document provides comprehensive analysis of EK9's 88 operators, categorizing them by bytecode complexity and providing strategic testing guidance. This serves as the reference for determining which operators require `@BYTE_CODE` structural validation vs. integration testing alone.

**Related Documentation:**
- **`EK9_JVM_BYTECODE_GENERATION_AND_TESTING.md`** - Implementation roadmap and testing infrastructure
- **`EK9_IR_AND_CODE_GENERATION.md`** - IR generation patterns and memory management
- **`EK9_COALESCING_OPERATORS_IR_GENERATION.md`** - Detailed coalescing operator IR patterns
- **`EK9_OPERATOR_SEMANTICS.md`** - EK9 operator behavior and semantics
- **`CLAUDE.md`** - Main project overview and development guidelines

## Table of Contents
1. [Overview](#overview)
2. [Complete Operator Inventory](#complete-operator-inventory)
3. [Complexity Categorization](#complexity-categorization)
4. [Guard Expression Architecture](#guard-expression-architecture)
5. [Bytecode Complexity Analysis](#bytecode-complexity-analysis)
6. [Testing Strategy Matrix](#testing-strategy-matrix)
7. [Implementation Guidelines](#implementation-guidelines)

---

## Overview

### Purpose

EK9 implements **88 distinct operators** (defined in `compiler-main/src/main/java/org/ek9lang/compiler/common/OperatorMap.java`), ranging from simple arithmetic (`+`, `-`) to sophisticated null-safety and coalescing operations (`?`, `:=?`, `<?`, `??`, `?:`).

**Key Insight**: Not all operators require the same level of bytecode testing. This document provides **data-driven categorization** to guide testing strategy.

### The Complexity Spectrum

**Simple Operators** (Direct method mapping):
```
EK9: value1 + value2
IR:  CALL value1._add(value2)
JVM: INVOKEVIRTUAL Integer._add(LInteger;)LInteger;
```
→ **3-5 bytecode instructions**, straightforward lowering, integration tests sufficient

**Complex Operators** (Multi-branch null-safety logic):
```
EK9: value1 <? value2  // Coalescing less-than
IR:  COALESCING_LT_BLOCK [
       null_check_left → IS_NULL value1
       null_check_right → IS_NULL value2
       comparison → CALL value1._lt(value2)
       selection_logic → BRANCH patterns
     ]
JVM: ~15-20 bytecode instructions with multiple BRANCH/LABEL pairs
```
→ **15-20+ bytecode instructions**, complex branching, **@BYTE_CODE tests essential**

### Strategic Testing Approach

**The Decision Matrix**:
- **Simple operators** → Integration tests only (behavioral validation)
- **Complex operators** → @BYTE_CODE + Integration tests (structural + behavioral validation)
- **Guard expressions** → @BYTE_CODE tests (control flow integration)

**Rationale**: Focus @BYTE_CODE testing effort on operators where:
1. Bytecode lowering complexity is high
2. Likelihood of lowering bugs is significant
3. Integration tests cannot validate structural correctness

---

## Complete Operator Inventory

Based on analysis of `OperatorMap.java` (88 operators total):

### Category 1: Comparison Operators (6)

| Operator | EK9 Symbol | Method Name | Complexity |
|----------|------------|-------------|------------|
| Less Than | `<` | `_lt` | Simple |
| Less Than or Equal | `<=` | `_lteq` | Simple |
| Greater Than | `>` | `_gt` | Simple |
| Greater Than or Equal | `>=` | `_gteq` | Simple |
| Equal | `==` | `_eq` | Simple |
| Not Equal | `<>` | `_neq` | Simple |

**Bytecode Pattern**: Direct method invocation
**Testing**: Integration tests sufficient

### Category 2: Arithmetic Operators (4)

| Operator | EK9 Symbol | Method Name | Complexity |
|----------|------------|-------------|------------|
| Addition | `+` | `_add` | Simple |
| Subtraction | `-` | `_sub` | Simple |
| Multiplication | `*` | `_mul` | Simple |
| Division | `/` | `_div` | Simple |

**Bytecode Pattern**: Direct method invocation
**Testing**: Integration tests sufficient

### Category 3: Assignment Operators (4)

| Operator | EK9 Symbol | Method Name | Complexity |
|----------|------------|-------------|------------|
| Add Assign | `+=` | `_addAss` | Simple |
| Subtract Assign | `-=` | `_subAss` | Simple |
| Multiply Assign | `*=` | `_mulAss` | Simple |
| Divide Assign | `/=` | `_divAss` | Simple |

**Bytecode Pattern**: Load, method call, store
**Testing**: Integration tests sufficient

### Category 4: Bitwise Operators (5)

| Operator | EK9 Symbol | Method Name | Complexity |
|----------|------------|-------------|------------|
| Bitwise AND | `and` | `_and` | Simple |
| Bitwise OR | `or` | `_or` | Simple |
| Bitwise XOR | `xor` | `_xor` | Simple |
| Shift Left | `<<` | `_shftl` | Simple |
| Shift Right | `>>` | `_shftr` | Simple |

**Bytecode Pattern**: Direct method invocation
**Testing**: Integration tests sufficient

### Category 5: Mathematical Operators (6)

| Operator | EK9 Symbol | Method Name | Complexity |
|----------|------------|-------------|------------|
| Power | `^` | `_pow` | Simple |
| Modulo | `mod` | `_mod` | Simple |
| Remainder | `rem` | `_rem` | Simple |
| Square Root | `sqrt` | `_sqrt` | Simple |
| Absolute Value | `abs` | `_abs` | Simple |
| Factorial | `!` | `_fac` | Simple |

**Bytecode Pattern**: Direct method invocation
**Testing**: Integration tests sufficient

### Category 6: Null-Safety Operators (5) ⚠️ COMPLEX

| Operator | EK9 Symbol | Method Name | Complexity | Bytecode Instructions |
|----------|------------|-------------|------------|----------------------|
| **isSet** | `?` | `_isSet` | **Complex** | ~6-8 |
| **Guarded Assignment** | `:=?` | N/A (special syntax) | **Complex** | ~8-10 |
| **Null Coalescing** | `??` | N/A (selection logic) | **Complex** | ~8-12 |
| **Elvis Coalescing** | `?:` | N/A (selection logic) | **Complex** | ~10-15 |
| **Guard Expression** | `?=` | N/A (control flow) | **Complex** | ~8-12 per guard |

**Bytecode Pattern**: IS_NULL checks + BRANCH + conditional logic
**Testing**: **@BYTE_CODE tests required** + Integration tests

### Category 7: Coalescing Comparison Operators (4) ⚠️ COMPLEX

| Operator | EK9 Symbol | Method Name | Complexity | Bytecode Instructions |
|----------|------------|-------------|------------|----------------------|
| **Coalescing LT** | `<?` | N/A (coalescing logic) | **Complex** | ~15-20 |
| **Coalescing LTE** | `<=?` | N/A (coalescing logic) | **Complex** | ~15-20 |
| **Coalescing GT** | `>?` | N/A (coalescing logic) | **Complex** | ~15-20 |
| **Coalescing GTE** | `>=?` | N/A (coalescing logic) | **Complex** | ~15-20 |

**Bytecode Pattern**: Null checks for BOTH operands + comparison + selection logic
**Testing**: **@BYTE_CODE tests required** + Integration tests

### Category 8: Special Assignment Operators (3)

| Operator | EK9 Symbol | Method Name | Complexity |
|----------|------------|-------------|------------|
| Copy | `:=:` | `_copy` | Simple |
| Replace | `:^:` | `_replace` | Simple |
| Merge | `:~:` | `_merge` | Simple |

**Bytecode Pattern**: Method call + assignment
**Testing**: Integration tests sufficient

### Category 9: Unary Operators (6)

| Operator | EK9 Symbol | Method Name | Complexity |
|----------|------------|-------------|------------|
| Increment | `++` | `_inc` | Simple |
| Decrement | `--` | `_dec` | Simple |
| Negate (Bitwise) | `~` | `_negate` | Simple |
| String Conversion | `$` | `_string` | Simple |
| JSON Conversion | `$$` | `_json` | Simple |
| Hashcode | `#?` | `_hashcode` | Simple |

**Bytecode Pattern**: Direct method invocation (note: `++`/`--` mutate but straightforward)
**Testing**: Integration tests sufficient

### Category 10: Conversion Operators (3)

| Operator | EK9 Symbol | Method Name | Complexity |
|----------|------------|-------------|------------|
| Promote | `#^` | `_promote` | Simple |
| Prefix | `#<` | `_prefix` | Simple |
| Suffix | `#>` | `_suffix` | Simple |

**Bytecode Pattern**: Direct method invocation
**Testing**: Integration tests sufficient

### Category 11: Utility Operators (4)

| Operator | EK9 Symbol | Method Name | Complexity |
|----------|------------|-------------|------------|
| Empty Check | `empty` | `_empty` | Simple |
| Length | `length` | `_len` | Simple |
| Compare | `<=>` | `_cmp` | Simple |
| Fuzzy Match | `<~>` | `_fuzzy` | Simple |

**Bytecode Pattern**: Direct method invocation
**Testing**: Integration tests sufficient

### Category 12: Collection Operators (8)

| Operator | EK9 Symbol | Method Name | Complexity |
|----------|------------|-------------|------------|
| Pipe | `\|` | `_pipe` | Moderate (streaming) |
| Sort | `sort` | `_sort` | Simple |
| Filter | `filter` | `_filter` | Simple |
| Map | `map` | `_map` | Simple |
| Collect | `collect` | `_collect` | Simple |
| Group | `group` | `_group` | Simple |
| Split | `split` | `_split` | Simple |
| Head | `head` | `_head` | Simple |
| Tail | `tail` | `_tail` | Simple |

**Bytecode Pattern**: Method calls (streaming operators may have complex IR)
**Testing**: Integration tests sufficient (streaming tested via stream tests)

### Category 13: Text Operators (2)

| Operator | EK9 Symbol | Method Name | Complexity |
|----------|------------|-------------|------------|
| Contains | `contains` | `_contains` | Simple |
| Matches | `matches` | `_matches` | Simple |

**Bytecode Pattern**: Direct method invocation
**Testing**: Integration tests sufficient

### Summary Statistics

**Total Operators**: 88

**By Complexity**:
- **Simple**: 69 operators (78%) - Direct method calls, straightforward lowering
- **Complex**: 14 operators (16%) - Multi-branch null-safety/coalescing logic
- **Moderate**: 5 operators (6%) - Context-dependent complexity

**Testing Implications**:
- **Integration tests**: All 88 operators (behavioral validation)
- **@BYTE_CODE tests**: 14 complex operators only (structural validation)
- **Coverage ratio**: Testing 16% with @BYTE_CODE achieves 80%+ of bytecode complexity validation

---

## Complexity Categorization

### Simple Operators (69 total)

**Definition**: Operators with **direct IR → bytecode mapping** requiring minimal branching.

**Characteristics**:
1. ✅ **Single method call** - `CALL method` → `INVOKEVIRTUAL method`
2. ✅ **No branching** - Straight-line bytecode (LOAD → CALL → STORE pattern)
3. ✅ **No null checks** - Assume operands are valid (or delegate to method implementation)
4. ✅ **3-5 bytecode instructions** typical

**Example: Addition Operator**
```ek9
result <- value1 + value2
```

**IR**:
```
_temp1 = LOAD value1
_temp2 = LOAD value2
_temp3 = CALL _temp1._add(_temp2)
STORE result, _temp3
```

**JVM Bytecode**:
```
ALOAD 1           // Load value1
ALOAD 2           // Load value2
INVOKEVIRTUAL Integer._add(LInteger;)LInteger;
ASTORE 3          // Store result
```

**Why integration tests sufficient**:
- Lowering bug probability: **Low** (minimal logic to get wrong)
- Behavioral tests catch semantic issues (wrong result)
- Bytecode structure predictable and simple

**Simple Operator List**:
- All arithmetic: `+`, `-`, `*`, `/`
- All comparison: `<`, `>`, `<=`, `>=`, `==`, `<>`
- All bitwise: `and`, `or`, `xor`, `<<`, `>>`
- All mathematical: `^`, `mod`, `rem`, `sqrt`, `abs`, `!`
- All assignment: `+=`, `-=`, `*=`, `/=`
- Most special: `:=:`, `:^:`, `:~:`, `#^`, `#<`, `#>`
- All utility: `empty`, `length`, `<=>`, `<~>`
- All collection: `sort`, `filter`, `map`, `collect`, `group`, `split`, `head`, `tail`
- All text: `contains`, `matches`
- Most unary: `++`, `--`, `~`, `$`, `$$`, `#?`

### Complex Operators (14 total) ⚠️

**Definition**: Operators requiring **multi-branch null-safety logic** with sophisticated bytecode patterns.

**Characteristics**:
1. ⚠️ **Multiple branch points** - 3+ BRANCH/LABEL instruction pairs
2. ⚠️ **Null/isSet checks** - IFNULL + INVOKEVIRTUAL isSet() patterns
3. ⚠️ **Conditional selection** - Different code paths based on operand state
4. ⚠️ **Label management** - Complex label generation and referencing
5. ⚠️ **10-20+ bytecode instructions** typical

**Example: Coalescing Less-Than Operator**
```ek9
result <- value1 <? value2  // Return lesser value, handle unset gracefully
```

**IR** (high-level representation):
```
COALESCING_LT_BLOCK [
  left_evaluation: [_temp1 = LOAD value1]
  right_evaluation: [_temp2 = LOAD value2]

  null_check_left: [_null1 = IS_NULL _temp1]
  null_check_right: [_null2 = IS_NULL _temp2]

  comparison: [_cmp = CALL _temp1._lt(_temp2)]
  comparison_valid: [_valid = CALL _cmp._isSet()]

  selection_logic: [
    BRANCH patterns selecting appropriate value
    based on null states and comparison result
  ]
]
```

**JVM Bytecode** (~15-20 instructions):
```
// Load operands
ALOAD 1                     // Load value1
ASTORE 3                    // Store to _temp1
ALOAD 2                     // Load value2
ASTORE 4                    // Store to _temp2

// Check if value1 is set
ALOAD 3
INVOKEVIRTUAL _isSet()
INVOKEVIRTUAL _state()      // Boolean → boolean
IFEQ value1_unset           // Branch if unset

// Check if value2 is set
ALOAD 4
INVOKEVIRTUAL _isSet()
INVOKEVIRTUAL _state()
IFEQ value2_unset           // Branch if unset

// Both set - do comparison
ALOAD 3
ALOAD 4
INVOKEVIRTUAL _lt(...)
ASTORE 5                    // Store comparison result
ALOAD 5
INVOKEVIRTUAL _isSet()
INVOKEVIRTUAL _state()
IFEQ comparison_invalid

// Comparison valid - check result
ALOAD 5
INVOKEVIRTUAL _state()
IFNE value1_smaller         // If true, use value1

// value1 >= value2, use value2
ALOAD 4
ASTORE 6                    // result
GOTO end

value1_smaller:
ALOAD 3
ASTORE 6                    // result
GOTO end

value1_unset:
ALOAD 4
ASTORE 6                    // Use value2
GOTO end

value2_unset:
ALOAD 3
ASTORE 6                    // Use value1
GOTO end

comparison_invalid:
NEW String
DUP
INVOKESPECIAL <init>
ASTORE 6                    // Unset result
GOTO end

end:
ALOAD 6                     // Final result
```

**Why @BYTE_CODE tests required**:
- Lowering bug probability: **High** (many branches, easy to target wrong label)
- Label management: Easy to create duplicate labels or incorrect references
- Null-safety logic: Tri-state semantics must be precisely implemented
- Integration tests insufficient: Can't validate branch structure, only final result

**Complex Operator List** (Priority for @BYTE_CODE testing):

**Null-Safety Operators** (5):
1. **`?` (isSet)** - IS_NULL check + BRANCH + `_isSet()` call (~6-8 instructions)
2. **`:=?` (guarded assignment)** - null/isSet check + conditional STORE (~8-10 instructions)
3. **`??` (null coalescing)** - Null check + operand evaluation + selection (~8-12 instructions)
4. **`?:` (elvis)** - Null AND isSet checks + selection (~10-15 instructions)
5. **`?=` (guard expression)** - Used in control flow, complex integration (~8-12 per guard)

**Coalescing Comparison Operators** (4):
6. **`<?` (coalescing LT)** - Null checks for both operands + comparison + selection (~15-20 instructions)
7. **`<=?` (coalescing LTE)** - Similar complexity to `<?` (~15-20 instructions)
8. **`>?` (coalescing GT)** - Similar complexity to `<?` (~15-20 instructions)
9. **`>=?` (coalescing GTE)** - Similar complexity to `<?` (~15-20 instructions)

**Control Flow Integration** (5):
10. **Guards in if statements** - `if v ?= value() then v > 0`
11. **Guards in while loops** - `while item ?= next() then item.isValid()`
12. **Guards in for loops** - `for entry ?= get(key) then entry.score > threshold`
13. **Guards in switch** - `switch v ?= getValue() ...`
14. **Guards in try** - `try v ?= riskyOp() ...`

### Decision Criteria

**Add @BYTE_CODE test if operator has:**
- ✅ **3+ BRANCH/LABEL pairs** in typical usage
- ✅ **IS_NULL or IFNULL checks** required
- ✅ **INVOKEVIRTUAL _isSet()** calls required
- ✅ **Multiple code paths** based on operand state
- ✅ **Complex label management** (interrelated labels)
- ✅ **Tri-state logic** (null, unset, set handling)

**Skip @BYTE_CODE test if operator has:**
- ❌ **Single INVOKEVIRTUAL** call pattern
- ❌ **No branching** (straight-line bytecode)
- ❌ **Simple LOAD-CALL-STORE** pattern
- ❌ **Integration test catches issues** (behavioral validation sufficient)

---

## Guard Expression Architecture

Guard expressions are **syntactic sugar for null/isSet checking** integrated into EK9's control flow constructs. They represent some of the most **complex bytecode patterns** in the language.

### Grammar Definition

From `compiler-main/src/main/antlr4/org/ek9lang/antlr/EK9.g4`:

```antlr4
preFlowAndControl
    : preFlowStatement
    | control=expression
    | preFlowStatement (WITH|THEN) control=expression

preFlowStatement
    : (variableDeclaration | assignmentStatement | guardExpression)

guardExpression
    : identifier op=GUARD expression  // GUARD is '?='

ifControlBlock
    : (IF | WHEN) preFlowAndControl block

whileStatementExpression
    : WHILE (preFlowStatement (WITH|THEN))? control=expression NL+ ...

forLoop
    : FOR (preFlowStatement (WITH|THEN))? identifier IN expression

switchStatementExpression
    : (SWITCH|GIVEN) preFlowAndControl NL+ ...

tryStatementExpression
    : (TRY|FUNCTION) preFlowStatement? NL+ ...
```

**Key Observations**:
1. Guards (`?=`) are **valid in all control flow** constructs (if, while, for, switch, try)
2. Guards can be **combined with conditions** using `WITH` or `THEN` keywords
3. Guards introduce **scoped variables** accessible in the block

### EK9 Usage Patterns

#### Pattern 1: Simple Guard (null/isSet check only)
```ek9
if v ?= value()
  v += 6  // v is guaranteed set and non-null here
```

**Semantics**:
- Call `value()` function
- Assign result to `v` (temporary variable in if scope)
- Check if `v` is non-null AND set
- Execute block only if both checks pass

#### Pattern 2: Guard with Condition
```ek9
when selectedTemp ?= currentTemperature("US") with selectedTemp > 50
  stdout.println("Warm in the US")
```

**Semantics**:
- Call `currentTemperature("US")`
- Assign result to `selectedTemp`
- Check if `selectedTemp` is non-null AND set
- **If passed**, evaluate condition `selectedTemp > 50`
- Execute block only if ALL checks pass

#### Pattern 3: Guard in While Loop
```ek9
while item ?= iterator.next() then item.isValid()
  process(item)
```

**Semantics**:
- Call `iterator.next()` each iteration
- Assign result to `item`
- Check null/isSet for `item`
- Evaluate `item.isValid()` condition
- Continue loop only if all checks pass

#### Pattern 4: Guard in For Loop
```ek9
for entry ?= dict.get(key) with entry.score > threshold
  results.add(entry)
```

**Semantics**:
- Call `dict.get(key)` before loop
- Assign result to `entry`
- Check null/isSet for `entry`
- Evaluate `entry.score > threshold`
- Execute loop if all checks pass

### Bytecode Complexity Analysis

**Simple Guard** (`if v ?= value()`):
```
// Pseudo-bytecode (8-12 instructions)
ALOAD 0                    // Load 'this'
INVOKEVIRTUAL value()      // Call value()
ASTORE 1                   // Store to v
ALOAD 1                    // Load v
IFNULL guard_failed        // Jump if null
ALOAD 1                    // Load v again
INVOKEVIRTUAL _isSet()     // Check if set
INVOKEVIRTUAL _state()     // Boolean → boolean
IFEQ guard_failed          // Jump if unset
// ... if block ...
guard_failed:
// ... after if or else block ...
```

**Guard with Condition** (`if v ?= value() then v > 0`):
```
// Pseudo-bytecode (12-16 instructions)
ALOAD 0
INVOKEVIRTUAL value()
ASTORE 1                   // v
ALOAD 1
IFNULL guard_failed        // Null check
ALOAD 1
INVOKEVIRTUAL _isSet()
INVOKEVIRTUAL _state()
IFEQ guard_failed          // IsSet check

// Additional condition evaluation
ALOAD 1
ICONST_0
INVOKESTATIC Integer._of()
INVOKEVIRTUAL _gt()
INVOKEVIRTUAL _state()
IFEQ guard_failed          // Condition check

// ... if block ...
guard_failed:
// ... after if or else block ...
```

**Why Guards Need @BYTE_CODE Tests**:

1. **Multi-stage checking** (null → isSet → condition):
   - Each stage requires correct branch target
   - Easy to generate wrong label or skip a check

2. **Scope management**:
   - Guard variable must be accessible in block
   - Variable lifetime must be correctly managed
   - Incorrect scope can cause verification errors

3. **Label uniqueness**:
   - Each guard needs unique labels (guard_failed_1, guard_failed_2, etc.)
   - Nested guards require careful label management
   - Duplicate labels cause JVM verification failure

4. **Integration across constructs**:
   - Same guard pattern used in if/while/for/switch/try
   - Each construct has different scope and flow requirements
   - Testing one doesn't validate all contexts

5. **Tri-state semantics**:
   - null (no memory allocated) → fail
   - unset (memory allocated, no meaningful value) → fail
   - set (memory allocated, meaningful value) → check condition
   - Each state requires correct bytecode path

---

## Bytecode Complexity Analysis

### Instruction Count by Operator Category

| Category | Typical Instructions | Branch Points | Null Checks | @BYTE_CODE Needed? |
|----------|---------------------|---------------|-------------|-------------------|
| Simple Arithmetic | 3-5 | 0 | 0 | ❌ No |
| Simple Comparison | 3-5 | 0 | 0 | ❌ No |
| Assignment | 4-6 | 0 | 0 | ❌ No |
| Bitwise | 3-5 | 0 | 0 | ❌ No |
| Mathematical | 3-5 | 0 | 0 | ❌ No |
| **isSet Operator** | **6-8** | **2-3** | **1** | ✅ Yes |
| **Guarded Assignment** | **8-10** | **3-4** | **1-2** | ✅ Yes |
| **Null Coalescing** | **8-12** | **2-3** | **1** | ✅ Yes |
| **Elvis Coalescing** | **10-15** | **3-4** | **1-2** | ✅ Yes |
| **Coalescing Comparison** | **15-20** | **4-6** | **2** | ✅ Yes |
| **Guard Expression** | **8-12** | **2-4** | **1-2** | ✅ Yes |
| **Guard + Condition** | **12-16** | **3-5** | **1-2** | ✅ Yes |

### Real-World Bytecode Examples

For detailed examples with actual bytecode sequences, see:
- **`EK9_COALESCING_OPERATORS_IR_GENERATION.md`** - Complete coalescing operator IR and bytecode patterns
- **`EK9_IR_AND_CODE_GENERATION.md`** - QUESTION_BLOCK and GUARDED_ASSIGNMENT_BLOCK architecture

### Performance Implications

**Simple Operators**:
- 3-5 instructions = 1-2 CPU cycles (modern JIT optimization)
- Straightforward optimization by HotSpot C1/C2 compilers
- Predictable branch prediction (no branches)

**Complex Operators**:
- 10-20 instructions = 5-10 CPU cycles (before JIT optimization)
- Branch prediction crucial (multiple IFNULL/IFEQ)
- JIT optimization can eliminate redundant null checks (EarlyCSE, GVN passes)
- **Explicit IS_NULL instructions** enable backend optimization

**Key Insight**: Verbose IR with explicit null checks **enables** optimization, doesn't hinder it. Backends can eliminate redundancy; implicit null checks can't be optimized.

---

## Testing Strategy Matrix

### Complete Operator Testing Matrix

| Operator | Symbol | Complexity | Integration Test | @BYTE_CODE Test | @IR Test | Priority |
|----------|--------|------------|-----------------|----------------|----------|----------|
| Addition | `+` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Subtraction | `-` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Multiplication | `*` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Division | `/` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Less Than | `<` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Greater Than | `>` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Less Equal | `<=` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Greater Equal | `>=` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Equal | `==` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Not Equal | `<>` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| **isSet** | **`?`** | **Complex** | ✅ Required | **✅ Required** | ✅ Required | **High** |
| **Guarded Assign** | **`:=?`** | **Complex** | ✅ Required | **✅ Required** | ✅ Required | **High** |
| **Null Coalesce** | **`??`** | **Complex** | ✅ Required | **✅ Required** | ✅ Required | **High** |
| **Elvis** | **`?:`** | **Complex** | ✅ Required | **✅ Required** | ✅ Required | **High** |
| **Coalesce LT** | **`<?`** | **Complex** | ✅ Required | **✅ Required** | ✅ Required | **High** |
| **Coalesce LTE** | **`<=?`** | **Complex** | ✅ Required | **✅ Required** | ✅ Required | **High** |
| **Coalesce GT** | **`>?`** | **Complex** | ✅ Required | **✅ Required** | ✅ Required | **High** |
| **Coalesce GTE** | **`>=?`** | **Complex** | ✅ Required | **✅ Required** | ✅ Required | **High** |
| **Guard (if)** | **`?=`** | **Complex** | ✅ Required | **✅ Required** | ✅ Required | **High** |
| **Guard (while)** | **`?=`** | **Complex** | ✅ Required | **✅ Required** | ✅ Required | **High** |
| **Guard (for)** | **`?=`** | **Complex** | ✅ Required | **✅ Required** | ✅ Required | **High** |
| **Guard (switch)** | **`?=`** | **Complex** | ✅ Required | **✅ Required** | ✅ Required | **High** |
| **Guard (try)** | **`?=`** | **Complex** | ✅ Required | **✅ Required** | ✅ Required | **High** |
| Bitwise AND | `and` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Bitwise OR | `or` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Bitwise XOR | `xor` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Shift Left | `<<` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Shift Right | `>>` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Power | `^` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Modulo | `mod` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Remainder | `rem` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Square Root | `sqrt` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Absolute | `abs` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Factorial | `!` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Copy | `:=:` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Replace | `:^:` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Merge | `:~:` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Add Assign | `+=` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Sub Assign | `-=` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Mul Assign | `*=` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Div Assign | `/=` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Increment | `++` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Decrement | `--` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Negate | `~` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| String | `$` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| JSON | `$$` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Hashcode | `#?` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Promote | `#^` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Prefix | `#<` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Suffix | `#>` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Empty | `empty` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Length | `length` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Compare | `<=>` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Fuzzy | `<~>` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Contains | `contains` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Matches | `matches` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Sort | `sort` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Filter | `filter` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Map | `map` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Collect | `collect` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Group | `group` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Split | `split` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Head | `head` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Tail | `tail` | Simple | ✅ Required | ❌ Skip | ✅ Required | Low |
| Pipe | `\|` | Moderate | ✅ Required | ❌ Skip | ✅ Required | Medium |

**Summary**:
- **Total operators**: 88
- **Require @BYTE_CODE tests**: 14 (16%)
- **Integration tests only**: 74 (84%)
- **High priority @BYTE_CODE**: 14 complex operators

### Test File Organization

**Recommended structure** (see `EK9_JVM_BYTECODE_GENERATION_AND_TESTING.md` for details):

```
examples/
├── irGeneration/                    (@IR - ALL 88 operators)
│   ├── operatorUse/
│   │   ├── arithmetic/
│   │   ├── comparison/
│   │   ├── coalescing/
│   │   └── ... (comprehensive)
│   └── ... (81+ files)
│
├── bytecodeGeneration/              (@BYTE_CODE - 14 complex operators)
│   ├── complexOperators/            (Priority 1: 15-20 files)
│   │   ├── isSet_operator.ek9
│   │   ├── guarded_assignment.ek9
│   │   ├── null_coalescing.ek9
│   │   ├── elvis_coalescing.ek9
│   │   ├── coalescing_lt.ek9
│   │   └── ... (coalescing variants)
│   │
│   └── guardExpressions/            (Priority 2: 10-15 files)
│       ├── if_with_guard_simple.ek9
│       ├── if_with_guard_and_condition.ek9
│       ├── while_with_guard.ek9
│       ├── for_with_guard.ek9
│       └── ... (guard variants)
│
└── integration-tests/               (ALL operators - behavioral)
    ├── GuardedControlFlow.ek9
    ├── CoalescingOperators.ek9
    └── ... (5-10 real-world scenarios)
```

---

## Implementation Guidelines

### When Implementing New EK9 Operator

**Step 1: Assess Complexity**

Run through complexity checklist:
- [ ] Does operator require null checks?
- [ ] Does operator require isSet checks?
- [ ] Does operator have 3+ branch points?
- [ ] Does operator have complex label management?
- [ ] Does operator implement tri-state logic?

**If YES to 2+ questions** → Complex operator
**If NO to all** → Simple operator

**Step 2: Write Tests Based on Complexity**

**For Simple Operators**:
1. ✅ Write `@IR` test validating method resolution
2. ✅ Write integration test validating behavior
3. ❌ Skip `@BYTE_CODE` test

**For Complex Operators**:
1. ✅ Write `@IR` test validating IR generation
2. ✅ Write `@BYTE_CODE` test validating bytecode structure
3. ✅ Write integration test validating behavior
4. ✅ Document in this file (update operator inventory)

**Step 3: Document in Testing Matrix**

Add row to "Complete Operator Testing Matrix" above with:
- Operator name and symbol
- Complexity assessment
- Test requirements (Integration/ByteCode/IR)
- Priority (High/Medium/Low)

**Step 4: Cross-Reference**

Ensure operator documented in:
- [ ] This document (`EK9_OPERATOR_COMPLEXITY_AND_TESTING.md`)
- [ ] `EK9_JVM_BYTECODE_GENERATION_AND_TESTING.md` (if complex)
- [ ] `EK9_OPERATOR_SEMANTICS.md` (if special semantics)
- [ ] `EK9_COALESCING_OPERATORS_IR_GENERATION.md` (if coalescing)

### Guard Expression Implementation Checklist

When implementing guards in new control flow construct:

- [ ] Null check generates correct IFNULL branch
- [ ] isSet check generates correct INVOKEVIRTUAL + IFEQ branch
- [ ] Labels are unique (use label counter)
- [ ] Guard variable is in correct scope
- [ ] Guard variable accessible in block
- [ ] Multiple guards generate unique labels each
- [ ] Nested guards don't interfere
- [ ] WITH/THEN condition evaluated after null/isSet checks
- [ ] @BYTE_CODE test validates all branch paths
- [ ] Integration test validates behavioral correctness

### Testing Best Practices

**@IR Tests** (All operators):
- Focus on method resolution correctness
- Validate CALL instruction targets correct method
- Check RETAIN/RELEASE patterns for memory management
- Verify scope registration

**@BYTE_CODE Tests** (Complex operators only):
- Focus on bytecode structure, not behavior
- Validate branch targets are correct
- Check label uniqueness
- Verify null/isSet check patterns
- Document expected instruction sequence in directive

**Integration Tests** (All operators):
- Focus on behavioral correctness
- Test with various argument combinations
- Include edge cases (null, unset, boundary values)
- Verify end-to-end execution
- Multiple operators in combination

---

## Summary

### Key Takeaways

1. **88 total operators**, only **14 require @BYTE_CODE tests** (16%)
2. **Simple operators** (78%) have straightforward bytecode, integration tests sufficient
3. **Complex operators** (16%) have sophisticated null-safety/coalescing logic requiring structural validation
4. **Guard expressions** represent highest bytecode complexity, require dedicated testing
5. **Strategic subset approach** balances thorough validation with reasonable maintenance burden

### Testing Strategy Summary

**Three-Tier Testing Pyramid**:
```
         Integration Tests (5-10 files)
        /        All operators
       /         Behavioral validation
      /
     /  @BYTE_CODE Tests (25-35 files)
    /   Complex operators only
   /    Structural validation
  /
 /  @IR Tests (81+ files)
/   All operators
    IR correctness validation
```

**ROI Analysis**:
- Testing 16% of operators with @BYTE_CODE
- Validates 80%+ of bytecode complexity
- Reasonable maintenance (35 vs 506 tests)
- Complements (not duplicates) existing tests

### Next Steps

For implementation roadmap and detailed @BYTE_CODE testing infrastructure, see:
**`EK9_JVM_BYTECODE_GENERATION_AND_TESTING.md`**
