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