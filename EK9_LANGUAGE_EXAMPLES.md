# EK9 Language Examples and Patterns

This document contains idiomatic EK9 source code examples, language feature demonstrations, and best practices for writing effective EK9 code. This is the reference for understanding EK9 syntax, semantics, and recommended coding patterns.

**Related Documentation:**
- **`CLAUDE.md`** - Main project overview and daily development guidelines
- **`EK9_DEVELOPMENT_CONTEXT.md`** - Built-in type development and Java implementation patterns
- **`EK9_Compiler_Architecture_and_Design.md`** - Complete language specification and architecture

## Basic Language Features

### Assignment Operators

EK9 provides three distinct assignment operators, each with precise semantics:

#### Declaration (`<-`)

Creates a new variable with initial value:

```ek9
// Basic declarations
name <- "Steve"                    // String type inferred
age <- 42                          // Integer type inferred
isActive <- true                   // Boolean type inferred

// Collection declarations
items <- List()                    // Empty list
scores <- Dict()                   // Empty dictionary
uniqueIds <- Set()                 // Empty set

// Type explicitly specified (optional)
count as Integer <- 0
message as String <- "Hello"
```

#### Assignment (`:=`)

Updates an existing variable:

```ek9
// Simple reassignment
counter <- 0
counter := counter + 1             // Increment
counter := counter * 2             // Multiply

// String reassignment
greeting <- "Hello"
greeting := greeting + " World"    // Concatenation

// Collection updates
names <- List()
names := names + "Alice"           // Add element
```

#### Guarded Assignment (`:=?`)

Only assigns if variable is currently UNSET:

```ek9
// Default value pattern
config <- String()                 // Create unset
config :=? "default.cfg"           // Sets to "default.cfg"
config :=? "other.cfg"             // No-op (already set)

// Lazy initialization
cache <- String()
getValue()
  cache :=? computeExpensive()     // Only compute first time
  <- cache

// Multiple defaults (first-wins)
setting <- String()
setting :=? getFromEnv()           // Try environment first
setting :=? getFromConfig()        // Then config file
setting :=? "hardcoded-default"    // Finally fallback
```

**When to use each:**
- `<-` for first declaration
- `:=` for updates
- `:=?` for conditional initialization

### Guard Patterns in Control Flow

EK9's revolutionary guard system works identically across ALL control flow constructs.

#### IF Statement Guards

```ek9
// Declaration guard - only execute if value is SET
if name <- getName()
  stdout.println(name)             // name guaranteed SET

// Assignment guard - update existing variable
result <- String()
if result := fetchData()
  process(result)                  // result guaranteed SET

// Conditional guard - additional check after guard
if value <- getValue() then value > 100
  processHighValue(value)          // value is SET AND > 100

// Guarded assignment guard
cache <- String()
if cache :=? loadFromDisk()
  useCache(cache)                  // Executed only if assignment happened
```

#### SWITCH Statement Guards

```ek9
// Guard on switch expression
switch record <- database.getRecord(id)
  case .type == "USER"
    processUser(record)            // record guaranteed safe
  case .type == "ORDER"
    processOrder(record)
  default
    logUnknown(record)

// Chain of responsibility pattern
switch
  case result := tryMethod1() -> handleResult(result)
  case result := tryMethod2() -> handleResult(result)
  case result := tryMethod3() -> handleResult(result)
  default -> handleError()
```

#### FOR Loop Guards

```ek9
// Loop with declaration guard
for item <- iterator.next()
  process(item)                    // item guaranteed SET each iteration

// Traditional range with guard in body
for i in 1 ... 10
  if value <- lookup(i)
    process(value)
```

#### WHILE Loop Guards

```ek9
// While with declaration guard
while conn <- getActiveConnection()
  transferData(conn)               // conn guaranteed active

// Continue while values available
while item <- fetchNext()
  processItem(item)
  saveResult(item)
```

#### TRY-CATCH Guards

```ek9
// Try with resource guard
try resource <- acquireResource()
  processResource(resource)        // resource guaranteed valid
catch
  -> ex as Exception
  handleError(ex)
finally
  // resource automatically cleaned up
```

**The Power:** Same syntax across IF/SWITCH/FOR/WHILE/TRY - learn once, apply everywhere.

**See also:**
- **`EK9_GUARDS_AND_TRI_STATE_UNIFIED_SYSTEM.md`** - Complete guide to guard system
- **`CLAUDE.md`** - Tri-state semantics and operator reference
- **`flowControl.html`** - Comprehensive examples

### Hello World and Program Structure

*This section will contain:*
- Basic program structure
- Module definitions
- Entry points

## EK9 Type System Examples

### Built-in Types
*This section will contain:*
- String, Integer, Float, Boolean usage examples
- Character, Date, DateTime, Money examples
- Collection types: List, Dict, Set examples
- Optional and Result type patterns

### Custom Types
*This section will contain:*
- Record definition and usage
- Class definition and inheritance examples
- Trait definition and composition
- Enumeration examples

## Advanced Language Features

### Generic Types
*This section will contain:*
- Generic function definitions
- Generic class and record examples
- Constraint-based generics
- Type inference with generics

### Functional Programming
*This section will contain:*
- Higher-order functions
- Lambda expressions and closures
- Function composition patterns
- Pipeline and stream processing

### Object-Oriented Programming
*This section will contain:*
- Class hierarchies and inheritance
- Polymorphism and method dispatch
- Composition over inheritance patterns
- Encapsulation best practices

## EK9 Idioms and Best Practices

### Naming Conventions
*This section will contain:*
- Variable and function naming
- Type naming conventions
- Package and module organization
- Constant and enumeration naming

### Error Handling
*This section will contain:*
- Result type usage patterns
- Exception handling strategies
- Validation and error propagation
- Defensive programming techniques

### Performance Patterns
*This section will contain:*
- Efficient collection usage
- Memory management considerations
- Lazy evaluation patterns
- Optimization techniques

## Code Organization Patterns

### Module Structure
*This section will contain:*
- Package organization strategies
- Module dependency management
- Public API design
- Internal implementation patterns

### Design Patterns in EK9
*This section will contain:*
- Common design patterns adapted for EK9
- EK9-specific patterns and idioms
- Anti-patterns to avoid
- Refactoring strategies

## Testing Examples

*This section will contain:*
- Unit testing patterns in EK9
- Integration testing strategies
- Mock and stub patterns
- Property-based testing examples

## Real-World Examples

### Small Applications
*This section will contain:*
- Command-line utilities
- Data processing scripts
- Simple web services
- File manipulation tools

### Larger Examples
*This section will contain:*
- Multi-module applications
- Web applications and services
- Data analysis pipelines
- System integration examples

## Migration and Interop

### Java Interoperability
*This section will contain:*
- Calling Java from EK9
- Using Java libraries
- Data type mapping
- Performance considerations

### Best Practices for Conversion
*This section will contain:*
- Converting Java patterns to EK9
- Taking advantage of EK9 features
- Common migration pitfalls
- Gradual migration strategies

---

**Note**: This is a placeholder file created to organize future EK9 language examples and patterns. Content will be added as Steve and I shift from Java implementation work to generating idiomatic EK9 source code examples, demonstrating language features, and establishing coding best practices for the EK9 community.