# EK9 Tri-State Semantics

**Date**: 2025-11-15
**Purpose**: Comprehensive explanation of EK9's tri-state object model

---

## Overview

**CRITICAL**: EK9 implements a sophisticated tri-state object model that distinguishes between different states of object validity. Understanding this is essential for correct EK9 development.

## The Three States

EK9 objects can exist in three distinct states:

1. **Object Absent** - Object doesn't exist (e.g., missing key in Dict)
2. **Object Present but Unset** - Object exists in memory but has no meaningful value
3. **Object Present and Set** - Object exists with a valid, usable value

This tri-state model enables EK9 to handle optional data, partial initialization, and complex data states while maintaining type safety and avoiding null pointer exceptions.

## Type-Specific Semantics

### Primitive/Basic Types (String, Integer, Boolean, etc.)

- Can be unset: `new String()` creates an unset but defined object
- `_isSet()` returns false for unset, true for set
- **Never accept Java `null`** - always reject null to maintain type safety

**Example:**
```ek9
name <- String()        // Object exists but is UNSET
assert not name?        // _isSet() returns false

name: "Steve"           // Now SET with value
assert name?            // _isSet() returns true
```

### Collections (List, Dict, etc.)

- **Always set when created**, even if empty: `new List()` → set (0 items)
- Empty ≠ unset: an empty collection is a valid, usable state
- Only become unset via explicit `unSet()` call or invalid construction

**Example:**
```ek9
items <- List()         // ALWAYS SET, even though empty (0 items)
assert items?           // _isSet() returns true
assert items.length() == 0  // But it's empty

items.unSet()           // Explicit unset call
assert not items?       // Now UNSET
```

**CRITICAL**: Collection types (Dict, List, etc.) are **always set/valid** even when empty:
- `new Dict()` → **set** (empty dict with 0 items)
- `new List()` → **set** (empty list with 0 items)
- Only explicit `unSet()` calls or invalid constructor arguments make collections unset
- Empty collections ≠ unset collections

### Container Types (DictEntry, Result, Optional)

- Complex tri-state logic based on contained values
- DictEntry: set when key is valid, regardless of value state
- Result/Optional: specific semantics for contained value states

**Example:**
```ek9
// DictEntry is SET when key is valid, even if value is UNSET
unsetValue <- String()  // Unset value
entry <- DictEntry("key", unsetValue)
assert entry?           // Entry itself is SET
assert not entry.value()?  // But value is UNSET
```

## Implementation Guidelines

### Constructor Patterns

**Accept unset EK9 objects (tri-state semantics):**
```java
// Accept unset EK9 objects (tri-state semantics)
public DictEntry(Any k, Any v) {
  if (isValid(k) && v != null) {  // Reject null, accept unset
    this.keyValue = k;
    this.entryValue = v;  // v can be unset EK9 object
    set();
  }
}
```

**Key principle:** Reject Java `null` but accept unset EK9 objects. This maintains type safety while supporting tri-state semantics.

### Testing Patterns

**Test with unset EK9 objects, not null:**
```java
// Test with unset EK9 objects, not null
final var unsetValue = new String();  // Unset but defined
final var entry = new DictEntry(key, unsetValue);
assertSet.accept(entry);  // Container is set
assertFalse.accept(((BuiltinType) entry.value())._isSet());  // Value is unset
```

**Never test with null:**
```java
// ❌ WRONG - don't use null
final var entry = new DictEntry(key, null);  // Should be rejected

// ✅ CORRECT - use unset EK9 object
final var unsetValue = new String();
final var entry = new DictEntry(key, unsetValue);
```

## JSON Integration

Unset EK9 values map to JSON null for proper serialization:

```ek9
// EK9 code
data <- Dict()
data.put("name", "Steve")
data.put("age", Integer())  // Unset Integer

// JSON output
{
  "name": "Steve",
  "age": null
}
```

This enables proper JSON serialization of tri-state data while maintaining EK9's type safety internally.

## Logical Consistency

The `_isSet()` method defines "meaningful, normal, usable value":

- **Primitives**: Has actual data (not default/empty state)
- **Collections**: Always meaningful when created (empty is valid)
- **Containers**: Based on container-specific logic (e.g., DictEntry set when key valid)
- **Aggregates (records/classes)**: **ANY field set** semantics (see below)

**Key insight:** The definition of "set" is type-dependent and context-aware, allowing EK9 to model real-world semantics accurately.

### Aggregate Types: "ANY Field Set" Semantics

For records and classes using `default operator ?`, the isSet semantics follow the **"ANY field set"** rule:

```
object? = true   if ANY field is set
object? = false  only if ALL fields are unset
```

**Rationale:** An object with at least one meaningful field value has some valid state, even if other fields are unset. This supports partial initialization patterns common in real-world applications.

**Example:**
```ek9
defines class
  Person
    name <- String()
    age <- Integer()
    default operator ?

// Usage
p <- Person()
assert not p?        // All fields unset → false

p.name: "Steve"
assert p?            // ANY field set → true (even though age is unset)

p.age: 42
assert p?            // ALL fields set → still true
```

**Inheritance:** The "ANY field set" semantics propagate through inheritance. If either parent OR child fields are set, the object is considered set:

```ek9
defines class
  Base as open
    id <- Integer()
    default operator ?

  Derived extends Base
    name <- String()
    default operator ?

// If parent field set, child unset → true
d <- Derived()
d.id: 1
assert d?            // Parent field set → true

// If parent unset, child field set → true
d2 <- Derived()
d2.name: "Test"
assert d2?           // Child field set → true
```

**Shallow Copy Semantics:** When using `:=:` (copy), field references are copied, not deep cloned:

```ek9
dst :=: src  // Copies field references, not values
// Mutation through src.field affects dst.field (same reference)
```

## Comparison with Other Languages

### Java/C#/Python (Null Model)
- Two states: object exists OR null
- Null pointer exceptions are runtime errors
- No distinction between "never initialized" and "explicitly set to null"

### EK9 (Tri-State Model)
- Three states: absent, unset, set
- Null pointer exceptions are **impossible** (no null in language)
- Clear distinction between "never had value" (unset) and "no value available" (absent)

## Benefits of Tri-State Model

1. **Type Safety**: No null pointer exceptions - `null` doesn't exist in EK9
2. **Semantic Clarity**: Distinguish between "no data yet" vs "data doesn't exist"
3. **Compiler Enforcement**: Guards (`<-`, `:=?`) enforce checks at compile time
4. **JSON Compatibility**: Maps cleanly to JSON null when needed
5. **Partial Initialization**: Model complex data states naturally

## See Also

- **`EK9_ASSIGNMENT_OPERATORS_AND_GUARDS.md`** - Guard system built on tri-state semantics
- **`EK9_DEVELOPMENT_CONTEXT.md`** - Built-in type development patterns using tri-state
- **`EK9_GUARDS_AND_TRI_STATE_UNIFIED_SYSTEM.md`** - Complete authoritative guide

---

**Last Updated**: 2025-12-06
