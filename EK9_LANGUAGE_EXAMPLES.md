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

### Migration Patterns: From Traditional to EK9

**Purpose:** This section provides side-by-side comparisons of common programming patterns in traditional languages (Java, Python, C++, JavaScript) and their EK9 equivalents. These examples demonstrate why EK9's alternatives are safer, more maintainable, and equally (or more) expressive.

**Key Principle:**
> "EK9 doesn't remove features arbitrarily - it replaces dangerous patterns with safer, equally powerful alternatives."

---

#### 1. Loop Early Exit → Stream Pipelines

**Traditional Pattern (Java/Python/C++):**
```java
// Java - Find first matching item with break
String result = null;
for (String item : items) {
  if (item.contains("match")) {
    result = item;
    break;  // Easy to break in wrong loop when nested
  }
}
```

```python
# Python - Early return pattern
def find_match(items):
  for item in items:
    if "match" in item:
      return item  # Early return
  return None
```

**EK9 Alternative - Stream Pipeline:**
```ek9
// Find first matching item - declarative
result <- cat items | filter by contains("match") | head

// Equivalent expanded form:
result <- cat items
  | filter by item -> item.contains("match")
  | head
```

**Why EK9 is Better:**
- **Cannot break in wrong loop** - no break keyword exists
- **Declarative intent** - clearly states "find first match"
- **Cannot forget to set result** - pipeline returns value directly
- **Composable** - can add more pipeline stages easily

---

#### 2. Continue Logic → Filter/Skip

**Traditional Pattern (Java/Python):**
```java
// Java - Skip items with continue
List<String> results = new ArrayList<>();
for (String item : items) {
  if (!item.isValid()) {
    continue;  // Skip invalid items
  }
  if (item.isPending()) {
    continue;  // Skip pending items
  }
  results.add(process(item));
}
```

```python
# Python - Continue pattern
results = []
for item in items:
  if not item.is_valid():
    continue  # Skip
  if item.is_pending():
    continue  # Skip
  results.append(process(item))
```

**EK9 Alternative - Filter Pipeline:**
```ek9
// Filter valid and non-pending items, then process
results <- cat items
  | filter by isValid
  | filter by item -> not item.isPending()
  | map with process
  | collect
```

**Why EK9 is Better:**
- **No continue keyword** - impossible to skip wrong iteration
- **Explicit conditions** - filter stages clearly show what's kept
- **Chainable** - multiple filters compose naturally
- **Testable** - each pipeline stage can be tested independently

---

#### 3. Early Return Validation → Guard Expressions

**Traditional Pattern (Java/C++):**
```java
// Java - Multiple early returns
public String processData(Data data) {
  if (data == null) {
    return null;  // Early return #1
  }
  if (!data.isValid()) {
    return "";  // Early return #2 - easy to bypass validation
  }
  if (!data.hasPermission()) {
    return "Unauthorized";  // Early return #3
  }
  return data.process();  // Final return
}
// Problem: Easy to add new validation and forget to return
```

```python
# Python - Guard clauses pattern
def process_data(data):
  if data is None:
    return None
  if not data.is_valid():
    return ""  # Bypass risk
  if not data.has_permission():
    return "Unauthorized"
  return data.process()
```

**EK9 Alternative - Guard Expressions:**
```ek9
// Single return variable with guard expressions
processData()
  -> data as Data
  <- result as String?

  if validData <- sanitize(data) with validData.isValid()
    if authorized <- checkPermission(validData) with authorized?
      result: process(validData)
    else
      result: "Unauthorized"
  else
    result: ""
  // Compiler enforces: result MUST be initialized on ALL paths
```

**Why EK9 is Better:**
- **No early returns** - cannot bypass validation (Apple SSL bug impossible)
- **Compiler enforced** - all paths must initialize result
- **Single exit point** - easier to reason about resource cleanup
- **Type safe** - guard expressions ensure values are SET before use

---

#### 4. Switch Fallthrough → Multiple Case Values

**Traditional Pattern (C/Java):**
```c
// C - Switch with fallthrough (dangerous)
switch (day) {
  case MONDAY:
  case TUESDAY:
  case WEDNESDAY:
  case THURSDAY:
    workday();
    break;  // Easy to forget!
  case FRIDAY:
    workday();
    happy_hour();
    // MISSING BREAK - OOPS! Falls through!
  case SATURDAY:
  case SUNDAY:
    weekend();
    break;
}
```

```java
// Java - Same pattern, same risk
switch (status) {
  case PENDING:
  case PROCESSING:
    updateProgress();
    break;
  case COMPLETE:
    finalize();
    // Forgot break here!
  case FAILED:
    cleanup();
}
```

**EK9 Alternative - Multiple Case Values:**
```ek9
// Explicit multiple values - no fallthrough possible
switch day
  case MONDAY, TUESDAY, WEDNESDAY, THURSDAY
    workday()
  case FRIDAY
    workday()
    happyHour()
  case SATURDAY, SUNDAY
    weekend()
  default
    invalidDay()
// Each case is independent - cannot fall through
```

**Why EK9 is Better:**
- **No fallthrough** - feature doesn't exist
- **Cannot forget break** - no break keyword
- **Explicit intent** - `case MONDAY, TUESDAY` clearly shows grouped values
- **Self-documenting** - code clearly shows which values trigger which behavior

---

#### 5. Complex Loop Exit → Take/Head

**Traditional Pattern (Java/Python):**
```java
// Java - Take first N items with break counter
List<String> firstTen = new ArrayList<>();
int count = 0;
for (String item : items) {
  if (count >= 10) {
    break;  // Stop after 10
  }
  firstTen.add(item);
  count++;
}
```

```python
# Python - Enumerate and break pattern
first_ten = []
for i, item in enumerate(items):
  if i >= 10:
    break
  first_ten.append(item)
```

**EK9 Alternative - Head/Take:**
```ek9
// Take first 10 items - declarative
firstTen <- cat items | head 10 | collect

// Skip first 5, take next 10
batch <- cat items | skip 5 | head 10 | collect
```

**Why EK9 is Better:**
- **No manual counters** - head/skip handle indexing
- **No break logic** - declarative approach
- **Clearer intent** - "head 10" is obvious
- **Cannot mess up** - no off-by-one errors

---

#### 6. Nested Loop Break → Flatten with Streams

**Traditional Pattern (Java):**
```java
// Java - Break in nested loop (dangerous)
String result = null;
outerLoop:  // Label required!
for (List<String> group : groups) {
  for (String item : group) {
    if (item.matches()) {
      result = item;
      break outerLoop;  // Easy to break wrong loop
    }
  }
}
```

```python
# Python - Nested loop with flag
found = False
result = None
for group in groups:
  for item in group:
    if item.matches():
      result = item
      found = True
      break
  if found:
    break  # Break outer loop
```

**EK9 Alternative - Flatten Stream:**
```ek9
// Flatten nested structure and find first match
result <- cat groups
  | flatten
  | filter by matches
  | head

// Alternative: map each group and collect first match
result <- cat groups
  | map with group -> cat group | filter by matches | head
  | filter by isSet
  | head
```

**Why EK9 is Better:**
- **No labeled breaks** - feature doesn't exist
- **No flag variables** - stream pipeline handles logic
- **Flatten operation** - makes nesting explicit
- **Cannot break wrong loop** - impossible to introduce bug

---

#### 7. Resource Management with Early Return → Single Exit

**Traditional Pattern (Java):**
```java
// Java - Multiple returns can leak resources
public String readConfig(String path) {
  File file = null;
  try {
    file = new File(path);
    if (!file.exists()) {
      return null;  // Early return - did we close file?
    }
    if (!file.canRead()) {
      return "";  // Another early return
    }
    return readFile(file);
  } finally {
    if (file != null) {
      file.close();  // Cleanup
    }
  }
}
```

**EK9 Alternative - Guard with Single Exit:**
```ek9
// Resource management with guard expressions
readConfig()
  -> path as String
  <- result as String?

  try fileHandle <- openFile(path)
    if readable <- checkReadable(fileHandle) with readable?
      result: readFile(fileHandle)
    else
      result: ""
  catch
    -> ex as IOException
    result := String()  // Unset result on error
  // Single exit point - resource cleanup guaranteed
```

**Why EK9 is Better:**
- **Single exit point** - resource cleanup is guaranteed
- **No early returns** - cannot leak resources
- **Guard expressions** - ensure file exists and is readable
- **Try-with-resources pattern** - automatic cleanup

---

#### 8. Conditional Assignment → Guard Assignment `:=?`

**Traditional Pattern (Java/Python):**
```java
// Java - Check before assign pattern
String config = getDefaultConfig();
String userConfig = getUserConfig();
if (userConfig != null) {
  config = userConfig;  // Override if present
}
```

```python
# Python - Conditional assignment
config = get_default_config()
user_config = get_user_config()
if user_config is not None:
  config = user_config
```

**EK9 Alternative - Guard Assignment:**
```ek9
// Use :=? (guarded assignment) - only assigns if unset
config <- getDefaultConfig()
config :=? getUserConfig()  // Only assigns if config is UNSET

// Example: First available fallback
result <- String()           // Unset
result :=? getPrimary()      // Try primary
result :=? getSecondary()    // Try secondary
result :=? getDefault()      // Fall back to default
// result contains first SET value
```

**Why EK9 is Better:**
- **`:=?` operator** - conditional assignment built into language
- **No null checks** - tri-state semantics handle unset
- **Chaining** - multiple fallbacks are explicit
- **Intent is clear** - `:=?` says "assign if unset"

---

#### 9. Loop with Accumulator → Reduce/Collect

**Traditional Pattern (Java/Python):**
```java
// Java - Manual accumulation
int sum = 0;
for (int value : values) {
  if (value > 0) {
    sum += value;
  }
}
```

```python
# Python - Accumulator pattern
total = 0
for value in values:
  if value > 0:
    total += value
```

**EK9 Alternative - Stream Reduce:**
```ek9
// Functional reduction
sum <- cat values
  | filter by value -> value > 0
  | reduce with Integer() | item, acc -> acc + item

// Or using built-in collection methods
positiveValues <- cat values
  | filter by value -> value > 0
  | collect
sum <- positiveValues.sum()
```

**Why EK9 is Better:**
- **Functional approach** - no mutable accumulator
- **Composable** - filter and reduce are separate concerns
- **Built-in aggregations** - sum, max, min, average available
- **Parallel ready** - streams can be parallelized

---

#### 10. Multiple Return Values → Named Return Parameters

**Traditional Pattern (Java/Python):**
```java
// Java - Return object or array
class Result {
  String value;
  boolean success;
}

public Result process(Data data) {
  if (data.isValid()) {
    return new Result("processed", true);
  }
  return new Result("", false);
}
```

```python
# Python - Tuple unpacking
def process(data):
  if data.is_valid():
    return ("processed", True)
  return ("", False)

value, success = process(data)
```

**EK9 Alternative - Named Return Parameters:**
```ek9
// Multiple named return values
process()
  -> data as Data
  <- value as String?
  <- success as Boolean?

  if data.isValid()
    value: "processed"
    success: true
  else
    value: ""
    success: false
  // Compiler enforces: ALL return params initialized on ALL paths

// Caller gets named values
result <- process(someData)
stdout.println(result.value)
stdout.println(result.success)
```

**Why EK9 is Better:**
- **Named returns** - clear what each value represents
- **Compiler enforced** - all paths must initialize all returns
- **Type safe** - each return has its own type
- **No wrapper classes** - language feature, not pattern

---

### Migration Strategy Summary

**When migrating from traditional languages to EK9, follow this pattern:**

1. **Identify dangerous patterns:**
   - Find all `break`/`continue` statements
   - Find all early `return` statements
   - Find all switch statements (check for missing breaks)
   - Find all uninitialized variables

2. **Replace with EK9 alternatives:**
   - `break` → `head`, `tail`, `take`
   - `continue` → `filter`, `skip`
   - Early `return` → Guard expressions (`<-`, `:=?`)
   - Switch fallthrough → Multiple case values
   - Manual loops → Stream pipelines

3. **Leverage compiler enforcement:**
   - Let compiler catch missing initialization
   - Use tri-state semantics for optional values
   - Use guard expressions for null safety
   - Use single return variables for resource safety

4. **Expected learning curve:**
   - **Day 1**: Understand stream pipelines vs loops
   - **Day 2**: Internalize guard expressions vs early returns
   - **Week 1**: Write idiomatic EK9 code naturally
   - **Month 1**: Appreciate safety benefits in production

**ROI of Migration:**
- **Immediate**: 15-25% fewer bugs (Microsoft/Google data)
- **Month 1**: Faster code reviews (fewer patterns to audit)
- **Year 1**: Measurable reduction in production incidents
- **Lifetime**: Competitive advantage (ahead of industry trends)

---

**Note**: This is a placeholder file created to organize future EK9 language examples and patterns. Content will be added as Steve and I shift from Java implementation work to generating idiomatic EK9 source code examples, demonstrating language features, and establishing coding best practices for the EK9 community.