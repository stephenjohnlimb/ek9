# EK9 Operator Semantics

This document describes the unique operator semantics in the EK9 programming language, particularly where they differ from common languages like Java or C++.

## Increment and Decrement Operators

### EK9 `++` and `--` Operators
- **EK9 has only ONE form** of increment/decrement operators (unlike Java/C++ which have prefix and postfix)
- **Always mutates the object** and returns a reference to the same (now mutated) object
- **No distinction between prefix/postfix** - there is only one semantic

### Implementation in Java
```java
public Integer _inc() {
    if (isSet) {
        assign(state + 1);  // Mutates internal state
    }
    return this;  // Returns reference to same object
}

public Integer _dec() {
    if (isSet) {
        assign(state - 1);  // Mutates internal state  
    }
    return this;  // Returns reference to same object
}
```

### Key Differences from Java/C++

| Language | Prefix (`++x`) | Postfix (`x++`) |
|----------|----------------|-----------------|
| **Java/C++** | Increment first, return new value | Return old value, then increment |
| **EK9** | Always mutates and returns reference to same object | N/A - Only one form exists |

### Testing Implications
When testing EK9 increment/decrement operators:
- **DO NOT** call `++`/`--` on test constants/fixtures, as they will be permanently mutated
- **DO** create fresh objects when testing these operators:
  ```java
  // WRONG - mutates test constant
  assertEquals(i2, i1._inc()); // i1 is now 2, not 1!
  
  // CORRECT - use fresh object
  final var freshI1 = Integer._of(1);
  assertEquals(i2, freshI1._inc());
  ```

### Other Mutating Operators
Similar mutation semantics apply to other EK9 assignment operators:
- `_addAss()` (+=)
- `_subAss()` (-=) 
- `_mulAss()` (*=)
- `_divAss()` (/=)
- `_copy()` (:=:)
- `_replace()` (:^:)
- `_merge()` (:~:)
- `_pipe()` (|)

All these operators mutate the target object and return a reference to it.

### Non-Mutating Mathematical Operators
The following operators do NOT mutate and return new objects:
- `_add()` (+)
- `_sub()` (-)
- `_mul()` (*)
- `_div()` (/)
- `_negate()` (unary -)
- `_abs()` (absolute value)
- All comparison operators (`_eq`, `_lt`, etc.)

## Summary
EK9's approach simplifies the increment/decrement model by having only one semantic that always mutates and returns the same object reference. This eliminates the complexity and potential confusion of prefix vs postfix operators found in other languages.

## Coalescing Operators

EK9 introduces a family of coalescing operators that gracefully handle EK9's tri-state object model (null, unset, set). These operators simplify code by eliminating explicit null/unset checks.

### The Tri-State Model

EK9 objects can exist in three states:
1. **Null** - No memory allocated (Java `null`)
2. **Unset** - Memory allocated but no meaningful value (e.g., `new String()` without assignment)
3. **Set** - Memory allocated with a meaningful value (e.g., `new String("hello")`)

### Null Coalescing Operator (`??`)

**Semantics**: Returns LHS if not null, otherwise returns RHS.

```ek9
result <- x ?? y  // If x is not null, use x, else use y
```

**Safety**: Only checks for null, not for set/unset state.

**IR Structure**: Single NULL_CHECK case + default case.

### Elvis Coalescing Operator (`:?`)

**Semantics**: Returns LHS if both allocated AND set, otherwise returns RHS.

```ek9
result <- x ?: y  // If x is set (not null and not unset), use x, else use y
```

**Safety**: Two-stage check:
1. NULL_CHECK: Prevents null pointer access
2. IS_SET check: Detects unset values via `._isSet()._false()`

**IR Structure**: Two cases (NULL_CHECK + IS_SET) + default case.

### Comparison Coalescing Operators (`<?`, `>?`, `<=?`, `>=?`)

**Semantics**: Gracefully handle null/unset operands by favoring the valid value, otherwise perform comparison.

```ek9
result <- x <? y   // Return lesser value, handling null/unset gracefully
result <- x >? y   // Return greater value, handling null/unset gracefully
result <- x <=? y  // Return x if x <= y (when both valid), handling null/unset
result <- x >=? y  // Return x if x >= y (when both valid), handling null/unset
```

**Priority Cascade Logic**:
1. If LHS is (null OR unset) → return RHS
2. Else if RHS is (null OR unset) → return LHS
3. Else both valid → perform comparison and return appropriate value
4. Default → return RHS

#### Truth Table for `x <? y` (Lesser Value)

| x state | y state | Rule Applied | Result | Rationale |
|---------|---------|--------------|--------|-----------|
| null    | null    | x invalid    | **y** | Favor RHS when both invalid |
| null    | unset   | x invalid    | **y** | Favor RHS when LHS invalid |
| null    | set     | x invalid    | **y** | Use valid RHS |
| unset   | null    | x invalid    | **y** | Favor RHS when both invalid |
| unset   | unset   | x invalid    | **y** | Favor RHS when both invalid |
| unset   | set     | x invalid    | **y** | Use valid RHS |
| set     | null    | y invalid    | **x** | Use valid LHS |
| set     | unset   | y invalid    | **x** | Use valid LHS |
| set     | set     | compare      | **x if x<y, else y** | Normal comparison logic |

#### Truth Table for `x >? y` (Greater Value)

| x state | y state | Rule Applied | Result | Rationale |
|---------|---------|--------------|--------|-----------|
| null    | null    | x invalid    | **y** | Favor RHS when both invalid |
| null    | unset   | x invalid    | **y** | Favor RHS when LHS invalid |
| null    | set     | x invalid    | **y** | Use valid RHS |
| unset   | null    | x invalid    | **y** | Favor RHS when both invalid |
| unset   | unset   | x invalid    | **y** | Favor RHS when both invalid |
| unset   | set     | x invalid    | **y** | Use valid RHS |
| set     | null    | y invalid    | **x** | Use valid LHS |
| set     | unset   | y invalid    | **x** | Use valid LHS |
| set     | set     | compare      | **x if x>y, else y** | Normal comparison logic |

#### Truth Table for `x <=? y` and `x >=? y`

The `<=?` and `>=?` operators follow identical validity checking logic, only the comparison differs when both operands are valid.

**IR Structure for Comparison Coalescing**:
```
CONTROL_FLOW_CHAIN with 6 cases:
  Case 1: NULL_CHECK on LHS → if null, return RHS
  Case 2: IS_SET check on LHS → if not set, return RHS
  Case 3: NULL_CHECK on RHS → if null, return LHS
  Case 4: IS_SET check on RHS → if not set, return LHS
  Case 5: COMPARISON (e.g., LHS < RHS) → if true, return LHS
  Default: return RHS
```

### Benefits of Coalescing Operators

1. **Eliminates boilerplate**: No manual null/unset checking
2. **Short-circuit evaluation**: RHS only evaluated if needed
3. **Type safety**: Compiler enforces compatible types
4. **Cleaner code**: `x <? y` vs multi-line if/else blocks
5. **Function call efficiency**: Avoids storing intermediate results

### Examples

```ek9
// Null coalescing - simple fallback
name <- userName ?? "Guest"

// Elvis coalescing - requires set value
displayName <- user.name ?: "Unknown User"

// Comparison coalescing - min/max with null safety
minPrice <- price1 <? price2  // Returns lesser, handling null/unset
maxScore <- score1 >? score2  // Returns greater, handling null/unset

// Comparison coalescing with function calls (no temp variables needed!)
bestDeal <- findDealA() <? findDealB()  // Calls both, returns lesser valid value
```

### Contrast with Traditional Code

**Without coalescing operators**:
```ek9
birdA <- String()
if not bird1?
  birdA := bird2
else if not bird2?
  birdA := bird1
else
  birdA := bird1 < bird2 <- bird1 else bird2
```

**With comparison coalescing**:
```ek9
birdA <- bird1 <? bird2
```

The coalescing operator version is:
- **90% less code**
- **More readable** - intent is immediately clear
- **More efficient** - single expression evaluation
- **Safer** - compiler-enforced null/unset handling