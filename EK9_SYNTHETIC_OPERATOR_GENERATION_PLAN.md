# EK9 Synthetic Operator Generation Plan

**Author:** Steve Limb & Claude Code
**Date:** 2025-11-28 (Updated: 2025-12-06)
**Status:** Active Implementation (Phases 1-6 complete, `_json` operator added, runtime fuzz testing complete, enumeration work pending)

## Executive Summary

This document defines the architecture and implementation plan for generating synthetic operators at the IR level for EK9 constructs that use `default` operator declarations, including records, classes, and enumerations.

**Key Architectural Decision:** All synthetic operator generation happens at **IR Generation (Phase 10)**, not at the backend level. This ensures:
- Single implementation serves both JVM and LLVM backends
- Testable via `@IR` directives
- IR optimization (Phase 12) benefits both backends
- Backends remain "pretty dumb" - just translate IR instructions

---

## Table of Contents

1. [Background and Motivation](#background-and-motivation)
2. [Architecture Overview](#architecture-overview)
3. [Aggregate Type Mapping](#aggregate-type-mapping)
4. [Synthetic Operator Catalog](#synthetic-operator-catalog)
5. [Implementation Phases](#implementation-phases)
6. [IR Patterns by Operator](#ir-patterns-by-operator)
7. [Testing Strategy](#testing-strategy)
8. [Enumeration Special Handling](#enumeration-special-handling)
9. [Open Questions and Decisions](#open-questions-and-decisions)

---

## Background and Motivation

### The `default` Keyword

EK9 allows developers to request compiler-generated operator implementations:

```ek9
defines record
  Point
    x as Float
    y as Float

    default operator ==
    default operator <>
    default operator #?
    default operator $
```

The frontend phases (1-5) create MethodSymbol entries for these operators but leave the **body empty**. By Phase 10 (IR Generation), we must generate the actual implementation.

### Why IR-Level Generation?

| Approach | Pros | Cons |
|----------|------|------|
| **Backend Generation** | Backend can use target-specific optimizations | Duplicated logic for JVM/LLVM; can't test via @IR; backends must understand EK9 semantics |
| **IR-Level Generation** | Single implementation; testable via @IR; backend stays simple; optimization benefits both backends | Verbose IR output |

**Decision:** IR-level generation is correct. The verbose IR is a feature, not a bug - it enables analysis, optimization, and testing.

---

## Architecture Overview

### Detection Flow

```
Frontend Phases (1-5)
    │
    │ Creates MethodSymbol with:
    │   - name = "_eq"
    │   - synthetic = true
    │   - body = EMPTY
    │
    ↓
Phase 10: IR Generation
    │
    │ Detects: isSynthetic() && !hasBody()
    │
    ↓
SyntheticOperatorGenerator
    │
    │ Dispatches to appropriate generator
    │
    ↓
List<BasicBlockInstr>
    │
    │ Complete IR instruction sequence
    │
    ↓
Backend (Phase 14+)
    │
    │ Just translates IR → bytecode/LLVM IR
    │ (knows nothing about synthesis)
    ↓
```

### Package Structure

```
org.ek9lang.compiler.ir.synthesis/
├── SyntheticOperatorGenerator.java       // Main coordinator
├── SyntheticMethodDetector.java          // Identifies synthetic methods
│
├── generators/
│   ├── AbstractSyntheticGenerator.java   // Base class with common patterns
│   ├── EqualsGenerator.java              // _eq for records/classes
│   ├── NotEqualsGenerator.java           // _neq (delegates to _eq)
│   ├── HashCodeGenerator.java            // _hashcode
│   ├── ToStringGenerator.java            // _string ($ operator)
│   ├── CopyGenerator.java                // _copy (:=: operator)
│   ├── CompareGenerator.java             // _cmp (<=> operator) for enums
│   ├── OrdinalComparisonGenerator.java   // _lt, _lte, _gt, _gte for enums
│   └── IncrementDecrementGenerator.java  // _inc, _dec for enums
│
└── patterns/
    ├── IsSetGuardPattern.java            // Generate isSet check + branch
    ├── FieldIterationPattern.java        // Iterate fields, apply operation
    ├── HashCombinePattern.java           // 31 * hash + fieldHash
    ├── StringConcatPattern.java          // String building
    └── UnsetReturnPattern.java           // Return unset result
```

---

## Field Set Status Optimization

### The `_fieldSetStatus()` Helper Method

Synthetic operators that need to check or compare field set status use a shared helper method generated for each aggregate type.

**Signature:** `_fieldSetStatus() -> Bits`

**Purpose:** Returns a `Bits` value where each bit represents whether the corresponding field is set:
- Bit 0 = first field's set status
- Bit 1 = second field's set status
- etc.

**Why Bits (not Integer):**
- **Unlimited fields** - Integer limits to 32 fields; Bits has no limit
- **Cleaner API** - `_empty()` checks if all fields unset in single call
- **Rich operators** - `_eq()`, `_xor()`, `_and()` for status comparison

**IR Pattern:**
```
_fieldSetStatus() -> org.ek9.lang::Bits
  result = NEW Bits("")          // Empty-but-set Bits (not unset!)
  for each field:
    isSet = CALL field._isSet()
    CALL result._addAss(isSet)   // Append: result += isSet (mutates in place)
  return result
```

**Critical Implementation Detail:** Uses `Bits("")` (empty string) not `Bits()` (no-args). The no-args constructor creates an *unset* Bits, but `Bits("")` creates a *set* Bits with 0 bits. This is required because `_addAss(Boolean)` requires `this.isSet == true` to work correctly.

**Used By:**

| Generator | Usage |
|-----------|-------|
| `EqualsGenerator` | `thisStatus._eq(otherStatus)` - compare field set patterns before field comparison |
| `CompareGenerator` | `thisStatus._eq(otherStatus)` - compare field set patterns before field comparison |
| `HashCodeGenerator` | `status._hashcode()` - incorporate set status into hash as base value |
| `ToJsonGenerator` | `status._empty()` - check if any fields set (optimization: skip empty objects) |
| `ToStringGenerator` | Does NOT use `_fieldSetStatus()` - iterates fields directly |

**Optimization Impact:**
- For `_json` and `_string`: Reduces N `_isSet()` calls to 2 method calls (`_fieldSetStatus()._empty()`)
- For `_eq` and `_cmp`: Early detection of set/unset mismatches before expensive field comparisons

---

## Aggregate Type Mapping

| EK9 Construct | Java Bytecode | Synthetic Operators |
|---------------|---------------|---------------------|
| **Record** | Class (final fields) | `_eq`, `_neq`, `_hashcode`, `_string`, `_copy` |
| **Class** | Class | Same as record (when `default` used) |
| **Trait** | Interface | N/A (traits don't have default operators) |
| **Enumeration** | Class + static instances | `_eq`, `_neq`, `_lt`, `_lte`, `_gt`, `_gte`, `_cmp`, `_hashcode`, `_string`, `_inc`, `_dec` |
| **Component** | Class | Same as class (when `default` used) |

---

## Synthetic Operator Catalog

### Operators for Records and Classes

| Operator | Method Name | Signature | Behavior |
|----------|-------------|-----------|----------|
| `==` | `_eq` | `(T other) -> Boolean` | True if all fields equal |
| `<>` | `_neq` | `(T other) -> Boolean` | Logical NOT of `_eq` |
| `#?` | `_hashcode` | `() -> Integer` | Combined hash of all fields |
| `$` | `_string` | `() -> String` | "TypeName(field1=v1, field2=v2)" |
| `$$` | `_json` | `() -> JSON` | JSON representation of object |
| `:=:` | `_copy` | `(T source)` | Copy all fields from source |

### Additional Operators for Enumerations

| Operator | Method Name | Signature | Behavior |
|----------|-------------|-----------|----------|
| `<` | `_lt` | `(T other) -> Boolean` | Ordinal less than |
| `<=` | `_lte` | `(T other) -> Boolean` | Ordinal less than or equal |
| `>` | `_gt` | `(T other) -> Boolean` | Ordinal greater than |
| `>=` | `_gte` | `(T other) -> Boolean` | Ordinal greater than or equal |
| `<=>` | `_cmp` | `(T other) -> Integer` | Compare ordinals (-1, 0, 1) |
| `++` | `_inc` | `() -> T` | Next enum value (unset at end) |
| `--` | `_dec` | `() -> T` | Previous enum value (unset at start) |

---

## Implementation Phases

### Phase 1: Infrastructure (Foundation)

**Goal:** Create the synthesis framework and detection logic.

**Deliverables:**
- [ ] `SyntheticMethodDetector` - Identify synthetic methods needing generation
- [ ] `AbstractSyntheticGenerator` - Base class with common patterns
- [ ] `IsSetGuardPattern` - Reusable isSet check generation
- [ ] `UnsetReturnPattern` - Reusable unset return block generation
- [ ] Integration point in IR generation visitor

**Test:** Verify detection correctly identifies synthetic methods.

---

### Phase 2: Record Equality (`_eq`)

**Goal:** Implement `_eq` for records - the foundational operator.

**Deliverables:**
- [ ] `EqualsGenerator` - Generate field-by-field comparison
- [ ] `FieldIterationPattern` - Reusable field iteration

**IR Pattern:**
```
OPERATION: "module::Record._eq" [pure=true, synthetic=true]
  PARAMETERS: [other: module::Record]
  RETURN: rtn: org.ek9.lang::Boolean

  BASIC_BLOCK: entry
    // Guard: this isSet
    _t1 = CALL this._isSet()
    BRANCH_IF_FALSE _t1 -> return_unset

    // Guard: other isSet
    _t2 = CALL other._isSet()
    BRANCH_IF_FALSE _t2 -> return_unset

    // For each field:
    _f1_this = LOAD this.field1
    _f1_other = LOAD other.field1
    _cmp1 = CALL _f1_this._eq(_f1_other)
    _cmp1_set = CALL _cmp1._isSet()
    BRANCH_IF_FALSE _cmp1_set -> return_unset
    _cmp1_val = UNBOX _cmp1
    BRANCH_IF_FALSE _cmp1_val -> return_false

    // ... repeat for each field ...

    BRANCH -> return_true

  BASIC_BLOCK: return_true
    _result = CALL Boolean._of(true)
    STORE rtn = _result
    RETURN

  BASIC_BLOCK: return_false
    _result = CALL Boolean._of(false)
    STORE rtn = _result
    RETURN

  BASIC_BLOCK: return_unset
    _result = CALL Boolean._new()
    STORE rtn = _result
    RETURN
```

**Tests:**
- [ ] IR test: Verify structure with @IR directive
- [ ] Bytecode test: Two equal records → true
- [ ] Bytecode test: Two unequal records → false
- [ ] Bytecode test: One unset record → unset result
- [ ] Bytecode test: Record with unset field → unset result

---

### Phase 3: Record Not-Equals (`_neq`)

**Goal:** Implement `_neq` - delegates to `_eq`.

**Deliverables:**
- [ ] `NotEqualsGenerator` - Call _eq, negate result

**IR Pattern:**
```
OPERATION: "module::Record._neq" [pure=true, synthetic=true]
  PARAMETERS: [other: module::Record]
  RETURN: rtn: org.ek9.lang::Boolean

  BASIC_BLOCK: entry
    _eq_result = CALL this._eq(other)
    _eq_set = CALL _eq_result._isSet()
    BRANCH_IF_FALSE _eq_set -> return_unset

    _negated = CALL _eq_result._not()  // or Boolean.negate()
    STORE rtn = _negated
    RETURN

  BASIC_BLOCK: return_unset
    _result = CALL Boolean._new()
    STORE rtn = _result
    RETURN
```

**Tests:**
- [ ] IR test: Verify delegation to _eq
- [ ] Bytecode test: Verify negation works correctly

---

### Phase 4: Record HashCode (`_hashcode`)

**Goal:** Implement `_hashcode` - combine field hashes.

**Deliverables:**
- [ ] `HashCodeGenerator` - Generate hash combination logic
- [ ] `HashCombinePattern` - 31 * hash + fieldHash pattern

**IR Pattern:**
```
OPERATION: "module::Record._hashcode" [pure=true, synthetic=true]
  RETURN: rtn: org.ek9.lang::Integer

  BASIC_BLOCK: entry
    // Guard: this isSet
    _t1 = CALL this._isSet()
    BRANCH_IF_FALSE _t1 -> return_unset

    // Initialize hash
    _hash = CALL Integer._of(17)

    // For each field:
    _f1 = LOAD this.field1
    _f1_hash = CALL _f1._hashcode()
    _f1_set = CALL _f1_hash._isSet()
    BRANCH_IF_FALSE _f1_set -> return_unset

    // hash = 31 * hash + fieldHash
    _mult = CALL Integer._of(31)
    _scaled = CALL _hash._mult(_mult)
    _hash = CALL _scaled._add(_f1_hash)

    // ... repeat for each field ...

    STORE rtn = _hash
    RETURN

  BASIC_BLOCK: return_unset
    _result = CALL Integer._new()
    STORE rtn = _result
    RETURN
```

**Tests:**
- [ ] IR test: Verify hash combination pattern
- [ ] Bytecode test: Same values → same hash
- [ ] Bytecode test: Different values → different hash (usually)
- [ ] Bytecode test: Unset record → unset hash

---

### Phase 5: Record ToString (`_string`)

**Goal:** Implement `_string` - build readable representation.

**Deliverables:**
- [ ] `ToStringGenerator` - Generate string building logic
- [ ] `StringConcatPattern` - String concatenation pattern

**IR Pattern:**
```
OPERATION: "module::Point._string" [pure=true, synthetic=true]
  RETURN: rtn: org.ek9.lang::String

  BASIC_BLOCK: entry
    _t1 = CALL this._isSet()
    BRANCH_IF_FALSE _t1 -> return_unset

    // Build "Point(x=..., y=...)"
    _s1 = CALL String._of("Point(")

    _s2 = CALL String._of("x=")
    _f1 = LOAD this.x
    _f1_str = CALL _f1._string()
    _s3 = CALL _s1._add(_s2)
    _s4 = CALL _s3._add(_f1_str)

    _s5 = CALL String._of(", y=")
    _f2 = LOAD this.y
    _f2_str = CALL _f2._string()
    _s6 = CALL _s4._add(_s5)
    _s7 = CALL _s6._add(_f2_str)

    _s8 = CALL String._of(")")
    _result = CALL _s7._add(_s8)

    STORE rtn = _result
    RETURN

  BASIC_BLOCK: return_unset
    _result = CALL String._new()
    STORE rtn = _result
    RETURN
```

**Tests:**
- [ ] IR test: Verify string building pattern
- [ ] Bytecode test: Verify output format
- [ ] Bytecode test: Unset record → unset string

---

### Phase 6: Record Copy (`_copy`)

**Goal:** Implement `_copy` - copy all fields from source.

**Deliverables:**
- [ ] `CopyGenerator` - Generate field copy logic

**IR Pattern:**
```
OPERATION: "module::Record._copy" [pure=false, synthetic=true]
  PARAMETERS: [source: module::Record]
  RETURN: (void - mutates this)

  BASIC_BLOCK: entry
    // Copy each field
    _f1 = LOAD source.field1
    STORE this.field1 = _f1

    _f2 = LOAD source.field2
    STORE this.field2 = _f2

    // ... repeat for each field ...

    // Copy isSet state
    _isSet = CALL source._isSet()
    // Store to this._isSet (internal)

    RETURN
```

**Tests:**
- [ ] IR test: Verify field copy pattern
- [ ] Bytecode test: Copy preserves all values
- [ ] Bytecode test: Copy preserves unset state

---

### Phase 7: Enumeration Infrastructure

**Goal:** Set up enumeration-specific synthesis.

**Deliverables:**
- [ ] Static instance generation in IR
- [ ] `_ordinal` and `_name` synthetic field handling
- [ ] `_values` array generation
- [ ] Private constructor with ordinal/name parameters

**IR Pattern for Static Initializer:**
```
STATIC_INIT: "module::Color.<clinit>"
  // Create instances
  _inst0 = CALL Color._new(0, "Red")
  STORE Color.Red = _inst0

  _inst1 = CALL Color._new(1, "Green")
  STORE Color.Green = _inst1

  _inst2 = CALL Color._new(2, "Blue")
  STORE Color.Blue = _inst2

  // Create values array
  _array = NEW_ARRAY Color[3]
  STORE _array[0] = _inst0
  STORE _array[1] = _inst1
  STORE _array[2] = _inst2
  STORE Color._values = _array

  RETURN
```

---

### Phase 8: Enumeration Comparison Operators

**Goal:** Implement ordinal-based comparisons for enumerations.

**Deliverables:**
- [ ] `OrdinalComparisonGenerator` - _lt, _lte, _gt, _gte
- [ ] `CompareGenerator` - _cmp (three-way comparison)

**IR Pattern for `_lt`:**
```
OPERATION: "module::Color._lt" [pure=true, synthetic=true]
  PARAMETERS: [other: module::Color]
  RETURN: rtn: org.ek9.lang::Boolean

  BASIC_BLOCK: entry
    _t1 = CALL this._isSet()
    BRANCH_IF_FALSE _t1 -> return_unset

    _t2 = CALL other._isSet()
    BRANCH_IF_FALSE _t2 -> return_unset

    _ord1 = LOAD this._ordinal
    _ord2 = LOAD other._ordinal
    _result = CALL _ord1._lt(_ord2)

    STORE rtn = _result
    RETURN

  BASIC_BLOCK: return_unset
    _result = CALL Boolean._new()
    STORE rtn = _result
    RETURN
```

**Tests:**
- [ ] IR test: Verify ordinal comparison pattern
- [ ] Bytecode test: Red < Green < Blue
- [ ] Bytecode test: Blue > Green > Red
- [ ] Bytecode test: Red == Red, Red <> Green

---

### Phase 9: Enumeration Navigation (`_inc`, `_dec`)

**Goal:** Implement increment/decrement for enumeration navigation.

**Deliverables:**
- [ ] `IncrementDecrementGenerator` - _inc, _dec

**IR Pattern for `_inc`:**
```
OPERATION: "module::Color._inc" [pure=true, synthetic=true]
  RETURN: rtn: module::Color

  BASIC_BLOCK: entry
    _t1 = CALL this._isSet()
    BRANCH_IF_FALSE _t1 -> return_unset

    _ord = LOAD this._ordinal
    _one = CALL Integer._of(1)
    _nextOrd = CALL _ord._add(_one)

    _values = LOAD Color._values
    _len = LOAD _values.length

    _inBounds = CALL _nextOrd._lt(_len)
    _inBounds_val = UNBOX _inBounds
    BRANCH_IF_FALSE _inBounds_val -> return_unset

    _result = LOAD _values[_nextOrd]
    STORE rtn = _result
    RETURN

  BASIC_BLOCK: return_unset
    _result = CALL Color._new()  // unset Color
    STORE rtn = _result
    RETURN
```

**Tests:**
- [ ] IR test: Verify array lookup pattern
- [ ] Bytecode test: Red++ → Green
- [ ] Bytecode test: Blue++ → unset
- [ ] Bytecode test: Green-- → Red
- [ ] Bytecode test: Red-- → unset

---

### Phase 10: Trait Support

**Goal:** Generate Java interfaces for EK9 traits.

**Deliverables:**
- [ ] Interface bytecode generation (ACC_INTERFACE flag)
- [ ] Abstract method generation
- [ ] Default method generation (methods with bodies)
- [ ] Trait delegation in classes

**Note:** Traits don't have `default` operators - they define contracts, not implementations.

---

### Phase 11: Component Support

**Goal:** Support component constructs with DI patterns.

**Deliverables:**
- [ ] Injection point detection
- [ ] Constructor parameter injection
- [ ] Lifecycle method recognition

---

## Testing Strategy

### Test Organization

```
compiler-main/src/test/resources/examples/
├── irGeneration/
│   └── synthetic/
│       ├── recordEquals/
│       │   └── recordEquals.ek9          # @IR test for _eq
│       ├── recordNotEquals/
│       │   └── recordNotEquals.ek9       # @IR test for _neq
│       ├── recordHashCode/
│       │   └── recordHashCode.ek9        # @IR test for _hashcode
│       ├── recordToString/
│       │   └── recordToString.ek9        # @IR test for _string
│       ├── recordCopy/
│       │   └── recordCopy.ek9            # @IR test for _copy
│       ├── enumComparison/
│       │   └── enumComparison.ek9        # @IR test for enum operators
│       └── enumNavigation/
│           └── enumNavigation.ek9        # @IR test for _inc/_dec
│
└── bytecodeGeneration/
    ├── synthetic/
    │   ├── syntheticRecordEquals/
    │   │   ├── syntheticRecordEquals.ek9
    │   │   ├── test.sh
    │   │   └── expected_output.txt
    │   ├── syntheticRecordHashCode/
    │   │   └── ...
    │   ├── syntheticEnumeration/
    │   │   └── ...
    │   └── comprehensiveSynthetic/       # All operators together
    │       └── ...
    │
    └── json/                             # JSON operator tests (NEW)
        ├── jsonSingleString/             # Single String field
        ├── jsonSingleInteger/            # Single Integer field
        ├── jsonMultipleFields/           # Multiple mixed types
        ├── jsonBooleanFloat/             # Boolean and Float fields
        ├── jsonNestedObject/             # Nested class field
        ├── jsonListField/                # List of Strings field
        ├── jsonOneFieldUnset/            # One field unset (excluded from output)
        ├── jsonAllFieldsUnset/           # All fields unset (empty JSON {})
        └── jsonInheritance/              # Parent + child fields
```

### Test Levels

1. **IR Structure Tests** - Verify generated IR matches expected patterns
2. **Individual Operator Tests** - One operator per test, verify behavior
3. **Comprehensive Tests** - Full record/enum with all default operators
4. **Edge Case Tests** - Unset values, boundary conditions, nested types

### @IR Directive Testing

Each synthetic operator should have an @IR test that verifies:
- Correct guard checks (isSet)
- Correct field iteration
- Correct return paths (true/false/unset)

Example:
```ek9
#!ek9
defines module test.synthetic.equals

@IR: IR_GENERATION: OPERATION: "test.synthetic.equals::Point._eq": `
// Verify isSet guards
CALL this._isSet
BRANCH_IF_FALSE
CALL other._isSet
BRANCH_IF_FALSE
// Verify field comparisons
LOAD this.x
LOAD other.x
CALL .*\._eq
LOAD this.y
LOAD other.y
CALL .*\._eq
// Verify return blocks exist
return_true:
return_false:
return_unset:
`

defines record
  Point
    x as Float
    y as Float

    default operator ==

defines program
  TestEquals()
    p1 <- Point(1.0, 2.0)
    p2 <- Point(1.0, 2.0)
    assert p1 == p2
```

---

## Enumeration Special Handling

### Implicit Default Operators

Enumerations **implicitly** get all applicable default operators - the developer doesn't need to specify `default operator ==`. The frontend should automatically add synthetic method symbols for:

- `_eq`, `_neq` - Equality comparison
- `_lt`, `_lte`, `_gt`, `_gte` - Ordinal comparison
- `_cmp` - Three-way comparison
- `_hashcode` - Based on ordinal
- `_string` - Returns name
- `_inc`, `_dec` - Navigation
- `_isSet` - Tri-state support

### Synthetic Fields

Enumerations need synthetic fields added by the frontend:
- `_ordinal: Integer` - Position in declaration order (0, 1, 2, ...)
- `_name: String` - String name of the value
- `_values: T[]` - Static array of all values

### Static Factory Methods

Generate:
- `values() -> T[]` - Returns copy of _values array
- `valueOf(String name) -> T` - Lookup by name (unset if not found)

---

## Inheritance Handling Rules

### Phase 3 Validation Rules (from `DefaultOperatorsOrError.java`)

Before IR generation, Phase 3 validates that `default operator` usage is legal:

1. **`Any` is explicitly ignored** - The universal base type `Any` is skipped for all operator checks
2. **Super must have the operator** - If super is NOT `Any`, it must have the same operator
3. **All properties must have the operator** - Each field's type must support the operator

### IR Generation with Inheritance

When generating a synthetic operator for a class that extends another class:

```
// Pseudocode for _eq with inheritance
function generate_eq(thisClass, otherParam):

    // 1. Check isSet guards
    if not this._isSet(): return unset
    if not other._isSet(): return unset

    // 2. Call super._eq() if super is NOT Any and has _eq
    superAggregate = thisClass.getSuperAggregate()
    if superAggregate.isPresent():
        superType = superAggregate.get()
        if superType != Any AND superType.has("_eq"):
            superResult = super._eq(other)
            if superResult.isUnset: return unset
            if superResult == false: return false

    // 3. Compare THIS class's direct fields only (not inherited)
    for field in thisClass.getProperties():  // Direct properties only
        thisField = this.field
        otherField = other.field
        fieldResult = thisField._eq(otherField)
        if fieldResult.isUnset: return unset
        if fieldResult == false: return false

    // 4. All comparisons passed
    return true
```

### Key Points

1. **Use `getProperties()` not `getAllProperties()`** - Only compare fields declared in THIS class
2. **Super handles its own fields** - The `super._eq()` call compares inherited fields
3. **Short-circuit evaluation** - Return early on unset or false
4. **Any is always skipped** - Never call `Any._eq()` even if it theoretically exists

### Example: Class Hierarchy

```ek9
defines class
  Base
    id as Integer
    default operator ==

  Derived extends Base
    name as String
    default operator ==
```

**Generated IR for `Derived._eq`:**
```
// 1. isSet guards for this and other
// 2. CALL super._eq(other) -> check Base.id
// 3. Compare Derived.name only (not Base.id again)
// 4. Return combined result
```

---

## Open Questions and Decisions

### Q1: Recursive Types in _eq

**Question:** How should `_eq` handle recursive types?

```ek9
defines record
  Node
    value as Integer
    next as Node?
```

**Options:**
1. Generate recursive call to `next._eq(other.next)` - could stack overflow on cycles
2. Use reference equality for recursive fields
3. Detect cycles at runtime

**Decision:** TBD - likely option 1 (recursive call) as cycles are a programming error.

### Q2: Field Ordering

**Question:** What order should fields be processed in `_hashcode` and `_string`?

**Decision:** Declaration order from `aggregate.getFields()`.

### Q3: Unset Field vs Unset Object

**Question:** For `_eq`, should "object with unset field" be equal to "another object with same unset field"?

```ek9
p1 <- Point()  // unset x and y
p2 <- Point()  // unset x and y
p1 == p2       // true or unset?
```

**Decision:** If comparing two unset fields, the comparison result is **unset** (propagates uncertainty). Only fully-set objects can be definitively equal.

### Q4: Enumeration at Boundaries

**Question:** What should `Red--` and `Blue++` return?

**Options:**
1. Return unset (current plan)
2. Wrap around (Red-- → Blue)
3. Return same value (Red-- → Red)

**Decision:** Return **unset** - wrapping hides bugs, same-value is confusing.

### Q5: Component Injection Timing

**Question:** When are component dependencies injected?

**Decision:** TBD - this is Phase 11 scope.

---

## Success Criteria

### Phase Completion Checklist

- [x] **Phase 1 Complete:** Infrastructure in place, synthetic detection working
- [x] **Phase 2 Complete:** `_eq` generates correct IR, all tests pass
- [x] **Phase 3 Complete:** `_neq` generates correct IR, all tests pass
- [x] **Phase 4 Complete:** `_hashcode` generates correct IR, all tests pass
- [x] **Phase 5 Complete:** `_string` generates correct IR, all tests pass
- [x] **Phase 6 Complete:** `_copy` generates correct IR, all tests pass
- [x] **Phase 6.5 Complete:** `_json` generates correct IR, comprehensive JSON tests pass (NEW - 2025-12-05)
- [ ] **Phase 7 Complete:** Enumeration infrastructure in place
- [ ] **Phase 8 Complete:** Enum comparisons working
- [ ] **Phase 9 Complete:** Enum navigation working
- [ ] **Phase 10 Complete:** Traits generate correct interfaces
- [ ] **Phase 11 Complete:** Components with DI working

### Quality Gates

1. All synthetic operators have @IR tests verifying structure
2. All synthetic operators have bytecode execution tests
3. Edge cases (unset, boundary) are tested
4. Both positive and negative test cases exist
5. Memory management (RETAIN/SCOPE_REGISTER) is correct in generated IR

---

## Runtime Fuzz Testing Results (2025-12-06)

### `?` (isSet) - "ANY Field Set" Semantics

The `default operator ?` for aggregates implements **"ANY field set"** semantics:

```
object? = true   if ANY field is set
object? = false  only if ALL fields are unset
```

**Rationale:** An object with at least one meaningful field value has some valid state.

**7 runtime fuzz tests verify:**
- All fields unset → false
- First/last/any field set → true
- Inheritance: parent OR child field set → true

### `:=:` (copy) - Shallow Copy Semantics

The `default operator :=:` implements **shallow copy** (references copied, not values):

```ek9
dst :=: src  // Copies field references
// Mutation through src.nested affects dst.nested (same reference)
```

**10 runtime fuzz tests verify:**
- Value copying (fully set, partial set, from/to empty, overwrite)
- Inheritance (super fields also copied)
- **Shallow copy proof**: mutation through original reference visible in both
- Collection fields: shallow (List mutation visible in both)
- Multi-level nesting: shallow propagates through all levels

### Bugs Found Through Fuzz Testing

| Bug | Status | Description |
|-----|--------|-------------|
| `++` (_inc) bytecode | ❌ OPEN | Generates invalid bytecode causing VerifyError at runtime |
| Empty class `_fieldSetStatus()` | ❌ OPEN | Method not generated for classes with no fields |

### Correct Behavior Verified

| Behavior | Test Result |
|----------|-------------|
| Self-copy (`p :=: p`) | ✅ Correctly rejected at compile time (E08080) |
| "ANY field set" semantics | ✅ All 7 variations pass |
| Shallow copy semantics | ✅ Mutation propagation proven |

---

## Appendix: Quick Reference

### Operator to Method Name Mapping

| Operator | Method | Notes |
|----------|--------|-------|
| `==` | `_eq` | Equality |
| `<>` | `_neq` | Not equal |
| `<` | `_lt` | Less than |
| `<=` | `_lte` | Less than or equal |
| `>` | `_gt` | Greater than |
| `>=` | `_gte` | Greater than or equal |
| `<=>` | `_cmp` | Three-way compare |
| `#?` | `_hashcode` | Hash code |
| `$` | `_string` | To string |
| `:=:` | `_copy` | Copy |
| `++` | `_inc` | Increment |
| `--` | `_dec` | Decrement |

### Memory Management Pattern

Every temporary in synthetic IR needs:
```
_temp = CALL/LOAD ...
RETAIN _temp
SCOPE_REGISTER _temp, scope_id
```

### Standard Return Blocks

```
return_true:
  _result = CALL Boolean._of(true)
  STORE rtn = _result
  RETURN

return_false:
  _result = CALL Boolean._of(false)
  STORE rtn = _result
  RETURN

return_unset:
  _result = CALL Boolean._new()
  STORE rtn = _result
  RETURN
```

---

**End of Document**
