# EK9 Assignment Operators and Guard System

**Date**: 2025-11-15
**Purpose**: Comprehensive explanation of EK9's three assignment operators and universal guard system

---

## Overview

**CRITICAL**: EK9's three assignment operators enable a revolutionary unified guard system that works identically across ALL control flow constructs.

## Assignment Operator Semantics

EK9 provides three distinct assignment operators with precise semantics:

| Operator | Name | Syntax | Semantics | Use Case |
|----------|------|--------|-----------|----------|
| `<-` | Declaration | `var <- value` | Create NEW variable + first assignment | First-time assignment |
| `:=` | Assignment | `var := value` | Assign to EXISTING variable | Reassignment |
| `:=?` | Guarded Assignment | `var :=? value` | Only assign if variable is UNSET | Conditional initialization |

### Declaration (`<-`)

Creates a new variable and performs the first assignment. Type is inferred from the value.

**Examples:**
```ek9
name <- "Steve"           // Declare new variable (type: String)
count <- 42               // Type inferred as Integer
items <- List()           // Type inferred as List
price <- 19.99            // Type inferred as Float
```

**Key principle:** Declaration is the ONLY way to introduce a new variable into scope.

### Assignment (`:=`)

Assigns a value to an EXISTING variable. The variable must have been previously declared.

**Examples:**
```ek9
name <- "Steve"           // Declaration
name := "John"            // Assignment - updates existing variable
count := count + 1        // Increment existing variable
items := List()           // Replace entire collection
```

**Compiler enforcement:** Using `:=` on an undeclared variable is a compile error.

### Guarded Assignment (`:=?`)

Only assigns if the variable is currently UNSET. If already SET, the assignment is skipped.

**Examples:**
```ek9
result <- String()        // Create unset variable
result :=? "First"        // Assigns "First" (was unset)
result :=? "Second"       // Does NOT assign (already set)
assert result == "First"  // Still has first value

// Useful for default values
config <- getConfig()     // Might return unset
config :=? "default"      // Only set if getConfig() returned unset
```

**Use case:** Implement "set if not already set" semantics, useful for default values and optional initialization.

## Guard Variables in Control Flow

Guards combine assignment with null/isSet checking, eliminating boilerplate across ALL control flow constructs. The same syntax works identically in IF, SWITCH, FOR, WHILE, DO-WHILE, and TRY statements.

### IF with Declaration Guard

Only execute body if value is SET:

```ek9
if name <- getName()
  stdout.println(name)  // name guaranteed SET here

// Equivalent to (in other languages):
// String name = getName();
// if (name != null) {
//   System.out.println(name);
// }
```

**Key advantage:** Single line replaces two-line pattern, and the compiler enforces that `name` is SET within the if block.

### SWITCH with Guard

Eliminate null checks entirely:

```ek9
switch record <- database.getRecord(id)
  case .type == "USER"
    processUser(record)  // record guaranteed safe
  case .type == "ORDER"
    processOrder(record)
  default
    logUnknown(record)
```

**Key advantage:** No need for separate null check before switch. If `getRecord()` returns unset/absent, switch body doesn't execute.

### FOR with Guard

Loop only over SET values:

```ek9
for item <- iterator.next()
  process(item)  // item guaranteed SET each iteration
```

**Key advantage:** Loop automatically stops when `iterator.next()` returns unset (end of iteration). No explicit `hasNext()` check needed.

### WHILE with Guard

Continue while getting values:

```ek9
while conn <- getActiveConnection()
  transferData(conn)  // conn guaranteed active
```

**Key advantage:** Loop continues as long as `getActiveConnection()` returns SET values. Natural termination when function returns unset.

### TRY with Guard

Resource management with safety:

```ek9
try resource <- acquireResource()
  processResource(resource)  // resource guaranteed valid
catch
  -> ex as Exception
  handleError(ex)
```

**Key advantage:** Try block only executes if `acquireResource()` returns SET. Combines guard check with exception handling.

## The Universal Pattern

**The Power:** Same syntax, same safety guarantees, across ALL control flow. Learn once, apply everywhere.

### Unified Syntax Pattern

```ek9
<control-flow-keyword> <variable> <- <expression>
  // body - variable guaranteed SET here
```

This works for:
- `if variable <- expression`
- `switch variable <- expression`
- `for variable <- expression`
- `while variable <- expression`
- `try variable <- expression`

### Revolutionary Impact

- **90-95% elimination** of null pointer exceptions through compile-time enforcement
- **Universal pattern** - one syntax works across if/switch/for/while/try constructs
- **Perfect AI collaboration** - systematic patterns vs framework chaos
- **Cannot bypass** - compiler enforces safety, no escape hatches

## Advanced Guard Patterns

### Guard with Condition (if statement)

```ek9
if validData <- validate() with validData.isReady()
  result: process(validData)
```

**Explanation:** Execute body only if `validate()` returns SET AND `validData.isReady()` is true.

### Multiple Guards (nested)

```ek9
if user <- getUser(id)
  if profile <- user.getProfile()
    display(profile)
```

**Explanation:** Each guard provides safety for the next level. No null checks needed.

### Guards in switch cases

```ek9
switch record <- database.getRecord(id)
  case record.type == "USER"
    processUser(record)
  case record.type == "ORDER" with record.isValid()
    processOrder(record)
  default
    logUnknown(record)
```

**Explanation:** Cases can have additional conditions with `with` clause.

## Comparison with Other Languages

### Java/C# (Traditional Null Checks)

```java
String name = getName();
if (name != null) {
  System.out.println(name);
}
```

**Problems:**
- Two lines instead of one
- Easy to forget null check
- No compiler enforcement
- Null pointer exceptions at runtime

### EK9 (Guard Expression)

```ek9
if name <- getName()
  stdout.println(name)
```

**Benefits:**
- One line, clear intent
- Impossible to forget check (part of syntax)
- Compiler enforces safety
- No runtime null pointer exceptions

## See Also

- **`EK9_TRI_STATE_SEMANTICS.md`** - Tri-state model that guards are built on
- **`EK9_GUARDS_AND_TRI_STATE_UNIFIED_SYSTEM.md`** - Complete authoritative guide
- **`flowControl.html`** - Comprehensive guard examples with all constructs
- **`EK9_ACADEMIC_LANGUAGE_INNOVATIONS.md`** - Revolutionary competitive positioning

---

**Last Updated**: 2025-11-15
