# EK9 Development Context

> **Purpose**: Comprehensive reference for developing EK9 built-in types and unit tests
> 
> **Created**: From analysis of all EK9 components and OptionalTest development session
> 
> **Usage**: Reference this document when developing new EK9 components to follow established patterns

## Project Overview

### EK9 Language Implementation
- **Language**: New programming language with Java 23 backend
- **Architecture**: Multi-pass compiler (12 phases) transforming `.ek9` files to Java bytecode
- **Philosophy**: Honest handling of unset/unknown states, unset value propagation, type safety

### Key Modules
```
ek9/
├── ek9-lang/           # Core EK9 language runtime and built-in types
├── compiler-main/      # Core compiler implementation (CLI, LSP, compilation phases)
├── compiler-tooling/   # Tooling support for EK9 constructs
└── java-introspection/ # Java reflection utilities for bootstrap
```

## Build System & Commands

### Core Build Commands
```bash
# Build entire project
mvn clean install

# Run specific test class
mvn test -Dtest=OptionalTest -pl ek9-lang

# Run tests for specific module
mvn test -pl ek9-lang

# Clean build artifacts
mvn clean
```

### EK9 Compiler Usage (after build)
```bash
# Compile EK9 source
java -jar compiler-main/target/ek9c-jar-with-dependencies.jar -c file.ek9

# Language Server mode
java -jar compiler-main/target/ek9c-jar-with-dependencies.jar -ls

# Run tests
java -jar compiler-main/target/ek9c-jar-with-dependencies.jar -t file.ek9
```

## EK9 Type System Architecture

### Inheritance Hierarchy
```
Any (interface) - Universal base interface
  ↳ BuiltinType (abstract class) - Base class for all built-in types
    ↳ Boolean, Integer, Float, String - Primitive-like types
    ↳ List, Optional, Iterator - Collection types  
    ↳ Exception - Meta types
    ↳ Dimension, Date, Time, etc. - Domain-specific types
```

### Core Type System Principles

#### 1. **Three-Layer Architecture**
- **Any Interface**: Universal operations (`_isSet()`, `_eq()`, `_neq()`, `_string()`)
- **BuiltinType**: State management, validation framework, constraint checking
- **Concrete Types**: Specific implementations with domain logic

#### 2. **State Management**
- All types have `boolean isSet` field inherited from `BuiltinType`
- State transitions controlled by `set()` and `unSet()` methods
- All assignments go through `assign()` methods which validate constraints

#### 3. **"Unset Poison" Pattern**
- Operations with unset operands typically result in unset results
- Exception: `_merge()` and `_pipe()` operations ignore unset inputs
- `_isSet()` always returns a set Boolean indicating state
- Collection types (List, Dict) are always set, even when empty
- Error handling: Some operations throw exceptions rather than returning unset

## EK9 Annotation Formatting

### General Formatting Rules
EK9 uses specific formatting conventions for annotations that must be followed precisely since spacing is significant in the EK9 language:

#### Basic Patterns
- **Triple-quoted strings** (`"""`) for multi-line annotations
- **4-space indentation** for content from the opening quote
- **Arrow alignment**: `->` and `<-` align with content start
- **Parameter indentation**: Parameters indent 2 additional spaces from arrows
- **"as pure" positioning**: At the end of the first line when applicable

#### @Ek9Class Annotations
```java
@Ek9Class("""
    TypeName of type T as open""")  // Extensible types

@Ek9Class("""
    Iterator of type T as abstract""")  // Abstract base types

@Ek9Class  // Simple types without additional modifiers

@Ek9Class("""
    String as open""")  // Built-in types marked as open

@Ek9Class("""
    Result of type (O, E)""")  // Multi-type generics
```

#### @Ek9Constructor Annotations
```java
@Ek9Constructor("TypeName() as pure")  // Single line for simple constructors

@Ek9Constructor("""
    TypeName() as pure
      -> arg0 as String""")

@Ek9Constructor("""
    TypeName() as pure
      ->
        param1 as String
        param2 as Integer""")
```

#### @Ek9Method Annotations
```java
@Ek9Method("""
    methodName() as pure
      <- rtn as ReturnType?""")

@Ek9Method("""
    methodName() as pure
      -> param as ParamType
      <- rtn as ReturnType?""")

@Ek9Method("""
    methodName()
      -> acceptor as Acceptor of T""")  // Non-pure methods omit "as pure"
```

#### @Ek9Operator Annotations
```java
@Ek9Operator("""
    operator ? as pure
      <- rtn as Boolean?""")

@Ek9Operator("""
    operator + as pure
      -> arg as T
      <- rtn as T?""")

@Ek9Operator("""
    operator :=:
      -> arg as T""")  // Assignment operators typically not pure
```

## Modern Java Code Style Patterns

### Java 23 Language Features
EK9 implementations use modern Java syntax and conventions for cleaner, more maintainable code:

```java
// Modern variable declarations
final var result = _new();
final var scale = currency.getDefaultFractionDigits();
final var multiplier = BigDecimal.valueOf(arg.state);
final var product = this.state.multiply(multiplier).setScale(scale, RoundingMode.HALF_UP);

// Unnamed variables in catch blocks (Java 21+)
@SuppressWarnings("checkstyle:CatchParameterName")
private void parseMoneyString(java.lang.String input) {
    try {
        // parsing logic
    } catch (IllegalArgumentException _) {
        // Invalid currency code - ignore exception details
        unSet();
    }
}

// Pattern matching with instanceof
@Override
public Boolean _eq(Any arg) {
    if (arg instanceof Money asMoney) {
        return _eq(asMoney);
    }
    return new Boolean();
}
```

### Exception Handling Patterns
```java
// Granular exception handling with specific comments
try {
    final var amount = new BigDecimal(amountStr);
    final var curr = Currency.getInstance(currencyStr);
    assign(scaled, curr);
} catch (NumberFormatException _) {
    // Invalid number format
    unSet();
} catch (IllegalArgumentException _) {
    // Invalid currency code
    unSet();
}

// Checkstyle suppression for unnamed variables
@SuppressWarnings("checkstyle:CatchParameterName")
public Money _sqrt() {
    try {
        // calculation logic
    } catch (ArithmeticException _) {
        return _new();
    }
}
```

### Code Organization Patterns
```java
// Method-level suppression for specific checkstyle rules
@SuppressWarnings("checkstyle:CatchParameterName")
@Ek9Method("""
    convert() as pure
      ->
        multiplier as Float
        currencyCode as String
      <-
        rtn as Money?""")
public Money convert(Float multiplier, String currencyCode) {
    // implementation
}

// Consistent use of final for immutability
private Money convertInternal(BigDecimal exchangeRate, Currency targetCurrency) {
    final var result = _new();
    final var scale = targetCurrency.getDefaultFractionDigits();
    final var convertedAmount = this.state.multiply(exchangeRate).setScale(scale, RoundingMode.HALF_UP);
    result.assign(convertedAmount, targetCurrency);
    return result;
}
```

## Standard Implementation Patterns

### Polymorphic Operator Patterns

#### Dual Implementation Strategy
Many EK9 types implement operators in two forms: a type-specific version and a polymorphic Any version that dispatches to the specific implementation:

```java
// Type-specific implementation
@Ek9Operator("""
    operator <=> as pure
      -> arg as Boolean
      <- rtn as Integer?""")
public Integer _cmp(Boolean arg) {
    // Direct implementation
}

// Polymorphic Any version
@Override
@Ek9Operator("""
    operator <=> as pure
      -> arg as Any
      <- rtn as Integer?""")
public Integer _cmp(Any arg) {
    if (arg instanceof Boolean asBoolean) {
        return _cmp(asBoolean);
    }
    return new Integer();
}
```

**Key Benefits:**
- Type safety with specific implementations
- Polymorphic flexibility through Any interface
- Pattern matching for type dispatch
- Consistent unset handling for unsupported types

## Complex Type Implementation Patterns

### Multi-Field Type Management
For types with multiple related fields (like Money with amount + currency), specific patterns ensure atomic state management:

```java
// Multiple private fields requiring coordinated updates
public class Money extends BuiltinType {
    BigDecimal state;      // Amount
    Currency currency;     // Currency code

    // Atomic assignment method for multi-field updates
    private void assign(BigDecimal amount, Currency curr) {
        this.state = amount;
        this.currency = curr;
        set();  // Mark as set only after both fields updated
    }

    // Domain-specific validation combining multiple fields
    private boolean canProcessSameCurrency(Money arg) {
        return canProcess(arg) && this.currency.equals(arg.currency);
    }
}
```

### Domain-Specific Validation Patterns
```java
// Layered validation for complex business rules
private void parseMoneyString(java.lang.String input) {
    try {
        // Format validation
        int hashIndex = input.indexOf('#');
        if (hashIndex == -1 || hashIndex == 0 || hashIndex == input.length() - 1) {
            return; // Invalid format
        }

        // Length validation
        final var currencyStr = input.substring(hashIndex + 1);
        if (currencyStr.length() != 3) {
            return; // Currency code must be 3 characters
        }

        // Type validation and conversion
        final var amount = new BigDecimal(amountStr);
        final var curr = Currency.getInstance(currencyStr);

        // Business rule application (automatic scaling)
        final int scale = curr.getDefaultFractionDigits();
        final var scaled = amount.setScale(scale, RoundingMode.HALF_UP);

        assign(scaled, curr);
    } catch (IllegalArgumentException _) {
        unSet();
    }
}
```

### Conditional Logic in Assignment Operations
```java
// Complex merge behavior based on domain rules
@Ek9Operator("""
    operator :~:
      -> arg as Money""")
public void _merge(Money arg) {
    if (isValid(arg)) {
        if (!isSet) {
            // If this is not set then just assign and take new value
            assign(arg.state, arg.currency);
        } else if (this.currency.equals(arg.currency)) {
            // Merge by addition if same currency
            this.state = this.state.add(arg.state);
        } else {
            // Different currencies cannot be merged - unset result
            unSet();
        }
    }
}
```

### Precision and Scaling Management
```java
// Automatic precision handling based on domain rules
@Ek9Operator("""
    operator * as pure
      -> arg as Float
      <- rtn as Money?""")
public Money _mul(Float arg) {
    if (canProcess(arg)) {
        Money result = _new();
        final var scale = currency.getDefaultFractionDigits();
        final var multiplier = BigDecimal.valueOf(arg.state);
        final var product = this.state.multiply(multiplier).setScale(scale, RoundingMode.HALF_UP);
        result.assign(product, this.currency);
        return result;
    }
    return _new();
}

// Zero detection using inherited utility methods
@Ek9Operator("""
    operator /=
      -> arg as Float""")
public void _divAss(Float arg) {
    if (canProcess(arg) && !nearEnoughToZero(arg.state)) {
        final var divisor = BigDecimal.valueOf(arg.state);
        final var scale = currency.getDefaultFractionDigits();
        this.state = this.state.divide(divisor, scale, RoundingMode.HALF_UP);
    } else {
        unSet();
    }
}
```

### Constructor Patterns

#### Standard Constructor Set
```java
// 1. Default Constructor - Always creates unset instance
public TypeName() {
    unSet();
}

// 2. Copy Constructor - Takes same type parameter
public TypeName(TypeName arg0) {
    unSet();
    if (isValid(arg0)) {
        assign(arg0.state);
    }
}

// 3. String Constructor - For parseable types
public TypeName(String arg0) {
    unSet();
    if (isValid(arg0)) {
        // Parse and assign if valid
    }
}

// 4. Cross-type Constructors - For related types
public Float(Integer arg0) {
    unSet();
    if (isValid(arg0)) {
        assign((double) arg0.state);
    }
}
```

#### Factory Method Pattern
```java
// Multiple overloaded _of() variants
public static TypeName _of() {
    return new TypeName();
}

public static TypeName _of(JavaType value) {
    return new TypeName(value);
}

public static TypeName _of(String value) {
    return new TypeName(value);
}
```

### Operator Implementation Patterns

#### Comparison Operators
```java
@Ek9Operator("""
    operator < as pure
      -> arg as T
      <- rtn as Boolean?""")
public Boolean _lt(TypeName arg) {
    if (canProcess(arg)) {
        return Boolean._of(this.state < arg.state);
    }
    return new Boolean(); // Return unset Boolean
}

@Ek9Operator("""
    operator <=> as pure
      -> arg as T
      <- rtn as Integer?""")
public Integer _cmp(TypeName arg) {
    if (canProcess(arg)) {
        return Integer._of(Long.compare(this.state, arg.state));
    }
    return new Integer(); // Return unset Integer
}
```

#### Arithmetic Operators
```java
@Ek9Operator("""
    operator + as pure
      -> arg as T
      <- rtn as T?""")
public TypeName _add(TypeName arg) {
    if (canProcess(arg)) {
        return TypeName._of(this.state + arg.state);
    }
    return new TypeName(); // Return unset result
}

// Cross-type arithmetic (Integer + Float → Float)
@Ek9Operator("""
    operator + as pure
      -> arg as Float
      <- rtn as Float?""")
public Float _add(Float arg) {
    if (canProcess(arg)) {
        return Float._of(this.state + arg.state);
    }
    return new Float();
}
```

#### State Management Operators
```java
@Override
@Ek9Operator("""
    operator ? as pure
      <- rtn as Boolean?""")
public Boolean _isSet() {
    return Boolean._of(isSet); // Always returns set Boolean
}

@Ek9Operator("""
    operator $ as pure
      <- rtn as String?""")
public String _string() {
    if (isSet) {
        return String._of(this.state.toString());
    }
    return new String(); // Return unset String
}

@Ek9Operator("""
    operator empty as pure
      <- rtn as Boolean?""")
public Boolean _empty() {
    if (isSet) {
        return Boolean._of(isEmpty()); // Type-specific empty check
    }
    return new Boolean();
}
```

#### Assignment and Mutation Operators
```java
@Ek9Operator("""
    operator :=:
      -> arg as T""")
public void _copy(TypeName arg) {
    if (arg != null && arg.isSet) {
        assign(arg.state);
    } else {
        unSet();
    }
}

@Ek9Operator("""
    operator :~:
      -> arg as T""")
public void _merge(TypeName arg) {
    if (arg != null && arg.isSet) {
        if (isSet) {
            // Merge logic - add/combine values
            assign(this.state + arg.state);
        } else {
            // If unset, copy the value
            assign(arg.state);
        }
    }
}

@Ek9Operator("""
    operator +=
      -> arg as T""")
public void _addAss(TypeName arg) {
    if (canProcess(arg)) {
        assign(this.state + arg.state);
    }
}
```

### Validation and Constraint Patterns

#### Input Validation
```java
// Helper methods for validation
protected boolean isValid(Object arg) {
    return arg != null && ((BuiltinType) arg).isSet;
}

protected boolean canProcess(Object arg) {
    return isValid(this) && isValid(arg);
}

// Assignment with validation
protected void assign(StateType value) {
    this.state = value;
    set(); // Mark as set
    validateConstraints(); // Check constraints
}

// Override for custom validation
@Override
protected void validateConstraints() {
    super.validateConstraints();
    // Custom validation logic
    if (violatesConstraint(this.state)) {
        unSet();
        throw new Exception(String._of("Constraint violation"));
    }
}
```

## Type-Specific Patterns

### Primitive-like Types (Boolean, Integer, Float, String)

#### Characteristics
- Have primitive Java state fields (`boolean state`, `long state`, `double state`, `String state`)
- Support full arithmetic and comparison operations
- Include parsing from String constructors
- Cross-type arithmetic promotion rules

#### Special Considerations

**Boolean**: Three-state logic (true/false/unset)
```java
// Logical operations with unset handling
@Ek9Operator("""
    operator and as pure
      -> arg as Boolean
      <- rtn as Boolean?""")
public Boolean _and(Boolean arg) {
    if (canProcess(arg)) {
        return Boolean._of(this.state && arg.state);
    }
    return new Boolean(); // Unset result for unset inputs
}
```

**Integer**: Factorial, modulus vs remainder, bitwise operations
```java
@Ek9Operator("""
    operator ! as pure
      <- rtn as Integer?""")
public Integer _fac() {
    if (isSet && state >= 0) {
        return Integer._of(factorial(state));
    }
    return new Integer();
}

@Ek9Operator("""
    operator mod as pure
      -> arg as Integer
      <- rtn as Integer?""")
public Integer _mod(Integer arg) {
    if (canProcess(arg) && arg.state != 0) {
        return Integer._of(Math.floorMod(this.state, arg.state));
    }
    return new Integer();
}
```

**Float**: Special handling for infinity/NaN, precision thresholds
```java
@Override
protected void validateConstraints() {
    super.validateConstraints();
    if (isSet && (Double.isNaN(state) || Double.isInfinite(state))) {
        unSet();
        throw new Exception(String._of("Invalid floating point value"));
    }
}
```

**String**: Levenshtein distance for fuzzy matching, padding operations
```java
@Ek9Operator("""
    operator <~> as pure
      -> arg as String
      <- rtn as Integer?""")
public Integer _fuzzy(String arg) {
    if (canProcess(arg)) {
        return Integer._of(levenshteinDistance(this.state, arg.state));
    }
    return new Integer();
}
```

### Collection Types (List, Optional, Iterator, Result, Money)

#### Key Patterns
- **List**: Always set (empty list ≠ unset list), supports generics, throws exceptions for invalid operations
- **Optional**: Represents potentially absent values, different from unset
- **Iterator**: Stateful, `_isSet()` indicates `hasNext()`
- **Result**: Dual-state error handling type with OK and ERROR values
- **Dict**: Key-value collections with integrated DictEntry iteration
- **PriorityQueue**: Ordered collections with comparator support and duplicate handling
- **Money**: Currency-safe arithmetic, automatic precision scaling, business rule enforcement
- **JSON**: Jackson-integrated JSON handling with nature-specific iterator behavior

#### Collection-Specific Operations
```java
// List operations
@Ek9Operator("""
    operator + as pure
      -> arg as List of T
      <- rtn as List of T?""")
public List _add(List arg) {
    if (canProcess(arg)) {
        final var result = new ArrayList<>(this.state);
        result.addAll(arg.state);
        return List._of(result);
    }
    return new List();
}

// Optional safe operations
@Ek9Method("""
    getOrDefault() as pure
      -> arg as T
      <- rtn as T?""")
public Any getOrDefault(Any arg) {
    if (isSet) {
        return state;
    }
    return arg; // Return provided default
}

// Result dual-state operations
@Ek9Method("""
    isOk() as pure
      <- rtn as Boolean?""")
public Boolean isOk() {
    return Boolean._of(okValue != null);
}

@Ek9Method("""
    okOrDefault() as pure
      -> arg0 as O
      <- rtn as O?""")
public Any okOrDefault(Any arg0) {
    if (okValue != null) {
        return okValue;
    }
    return arg0;
}

// Result conditional execution
@Ek9Method("""
    whenOk() as pure
      -> consumer as Consumer of O""")
public void whenOk(Consumer consumer) {
    if (okValue != null && canProcess(consumer)) {
        consumer._call(okValue);
    }
}

// JSON nature-specific iteration (Array, Object, Value)
@Ek9Method("""
    iterator() as pure
      <- rtn as Iterator of JSON?""")
public _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C iterator() {
    if (!hasValidJson()) {
        return _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C._of();
    }
    
    if (jsonNode.isArray()) {
        // Array: iterate over elements as JSON objects
        java.util.Spliterator<JsonNode> spliterator = 
            java.util.Spliterators.spliteratorUnknownSize(jsonNode.elements(), 
                java.util.Spliterator.ORDERED);
        return createIteratorFromStream(
            java.util.stream.StreamSupport.stream(spliterator, false)
                .map(JSON::_of));
    } else if (jsonNode.isObject()) {
        // Object: iterate over key-value pairs as named JSON objects
        ObjectNode objectNode = (ObjectNode) jsonNode;
        return createIteratorFromEntries(objectNode.properties().iterator());
    } else {
        // Value: single-element iterator containing this JSON
        return createSingleElementIterator(this);
    }
}
```

### Jackson-Integrated JSON Type

#### Key Characteristics
The JSON type provides comprehensive JSON manipulation using Jackson databind for high-performance JSON operations with nature-specific behavior patterns.

#### Core Design Principles
- **Jackson Integration**: Uses `JsonNode`, `ObjectNode`, `ArrayNode` for native JSON operations
- **Three JSON Natures**: Array, Object, and Value types with distinct behaviors
- **Type-Safe Operations**: All operations respect JSON nature and return appropriate types
- **Iterator Integration**: Nature-specific iteration patterns using Jackson's native iterators
- **Path Query Support**: JSONPath integration for complex data navigation

#### Jackson Iterator Conversion Pattern
```java
// Converting Jackson iterators to EK9 streams using Spliterators
private java.util.Iterator<Any> convertJacksonIterator() {
    java.util.Spliterator<JsonNode> spliterator = 
        java.util.Spliterators.spliteratorUnknownSize(jsonNode.elements(), 
            java.util.Spliterator.ORDERED);
    return java.util.stream.StreamSupport.stream(spliterator, false)
        .map(JSON::_of)
        .map(json -> (Any) json)
        .iterator();
}
```

#### Nature-Specific Iterator Behavior
```java
// Array nature: iterate over elements
if (jsonNode.isArray()) {
    // Elements returned as individual JSON objects
    javaIterator = convertArrayElements();
}

// Object nature: iterate over key-value pairs  
else if (jsonNode.isObject()) {
    // Properties returned as named JSON objects using JSON(String, JSON) constructor
    ObjectNode objectNode = (ObjectNode) jsonNode;
    javaIterator = convertObjectProperties(objectNode);
}

// Value nature: single element
else {
    // Single-element iterator containing the JSON value itself
    javaIterator = java.util.List.of((Any) this).iterator();
}
```

#### EK9 Iterator Semantics
```java
// Critical: Empty structures return UNSET iterators (nothing to iterate over)
if (!hasValidJson()) {
    return _Iterator_8A55E2CE14B3B336B88069A9954BBCB8C58B64CA55768EECCED18381C1DA376C._of();
}

// Even empty arrays/objects return unset iterators if no content
// This follows EK9 principle: iterator unset when nothing to iterate over
```

#### Named JSON Object Pattern
```java
// Object properties preserve both key and value using named constructor
.map(entry -> {
    java.lang.String key = entry.getKey();
    JsonNode valueNode = entry.getValue();
    JSON valueJson = JSON._of(valueNode);
    return (Any) new JSON(String._of(key), valueJson); // Named JSON object
})
```

#### Required Jackson Imports
```java
import java.util.Spliterators;
import java.util.stream.StreamSupport;
import com.fasterxml.jackson.databind.node.ObjectNode;
```

#### Testing Pattern for JSON Iterator
```java
@Test
void testJSONIterator() {
    // Array iteration
    final var arrayJson = JSON._of("[1, 2, 3]");
    final var arrayIterator = arrayJson.iterator();
    assertTrue.accept(arrayIterator._isSet());
    
    // Object iteration - returns named JSON objects
    final var objectJson = JSON._of("{\"name\": \"test\", \"value\": 42}");
    final var objectIterator = objectJson.iterator();
    
    // Verify named JSON structure
    final var firstEntry = (JSON) objectIterator.next();
    assertTrue.accept(firstEntry.objectNature());
    
    // Value iteration
    final var valueJson = JSON._of("\"simple string\"");
    final var valueIterator = valueJson.iterator();
    assertEquals(valueJson, valueIterator.next());
    
    // Unset JSON returns unset iterator
    final var unsetJson = new JSON();
    assertUnset.accept(unsetJson.iterator());
}
```

### Meta Types (Exception, Any)

#### Exception Type
```java
// Exception always set when created
@Ek9Constructor("""
    Exception() as pure
      -> arg0 as String""")
public Exception(String arg0) {
    if (arg0 != null) {
        assign(arg0);
    } else {
        assign(String._of("Generic exception"));
    }
}
```

### Result Type - Dual-State Error Handling

#### Characteristics
The Result type is a sophisticated error handling construct that represents either success (OK) or failure (ERROR) states, providing explicit error handling without exceptions.

#### Key Design Principles
- **Dual-state management**: OK and ERROR are mutually exclusive states
- **Type safety**: Separate type parameters for OK (O) and ERROR (E) values
- **Explicit state checking**: `isOk()` and `isError()` methods for state validation
- **Safe value access**: `okOrDefault()` and error access patterns
- **Unset state**: Empty Result when neither OK nor ERROR values are present

#### State Management Pattern
```java
// Three distinct states: UNSET, OK, ERROR
private Any okValue;    // Contains success value when in OK state
private Any errorValue; // Contains error value when in ERROR state

// State checking methods
public Boolean isOk() {
    return Boolean._of(okValue != null);
}

public Boolean isError() {
    return Boolean._of(errorValue != null);
}

// _isSet() returns true only for OK state (following EK9 spec)
@Override
public Boolean _isSet() {
    return Boolean._of(okValue != null);
}

// _empty() returns true only when neither OK nor ERROR
public Boolean _empty() {
    return Boolean._of(okValue == null && errorValue == null);
}
```

#### Factory Method Patterns
```java
// Create unset Result
public static Result _of() {
    return new Result();
}

// Create OK Result using anonymous Any to bypass validation
public static Result _ofOk(Any okValue) {
    return new Result(okValue, new Any() {});
}

// Create ERROR Result using anonymous Any to bypass validation
public static Result _ofError(Any errorValue) {
    return new Result(new Any() {}, errorValue);
}

// Create mixed Result (both OK and ERROR values)
public static Result _of(Any okValue, Any errorValue) {
    return new Result(okValue, errorValue);
}
```

#### Safe Value Access Patterns
```java
// Safe OK value access with default
@Ek9Method("""
    okOrDefault() as pure
      -> arg0 as O
      <- rtn as O?""")
public Any okOrDefault(Any arg0) {
    if (okValue != null) {
        return okValue;
    }
    return arg0; // Return default when no OK value
}

// Throws exception if wrong state accessed
@Ek9Method("""
    ok() as pure
      <- rtn as O?""")
public Any ok() {
    if (okValue != null) {
        return okValue;
    }
    throw new Exception(String._of("No such element"));
}
```

#### Conditional Execution Patterns
```java
// Execute only if OK state
@Ek9Method("""
    whenOk() as pure
      -> consumer as Consumer of O""")
public void whenOk(Consumer consumer) {
    if (okValue != null && canProcess(consumer)) {
        consumer._call(okValue);
    }
}

// Execute only if ERROR state
@Ek9Method("""
    whenError() as pure
      -> consumer as Consumer of E""")
public void whenError(Consumer consumer) {
    if (errorValue != null && canProcess(consumer)) {
        consumer._call(errorValue);
    }
}
```

#### Iterator Integration Pattern
```java
// Iterator returns OK value or empty iterator
@Ek9Method("""
    iterator() as pure
      <- rtn as Iterator of O?""")
public Iterator iterator() {
    if (okValue != null) {
        return Iterator._of(okValue); // Single-item iterator
    }
    return Iterator._of(); // Empty iterator
}
```

#### Complex Equality Pattern
```java
// Result equality checks both OK and ERROR values
@Ek9Operator("""
    operator == as pure
      -> arg as Result of (O, E)
      <- rtn as Boolean?""")
public Boolean _eq(Result arg) {
    final var rtn = new Boolean();
    
    if (canProcess(arg)) {
        // Check for mismatched null states
        if ((okValue != null && arg.okValue == null) ||
            (okValue == null && arg.okValue != null) ||
            (errorValue != null && arg.errorValue == null) ||
            (errorValue == null && arg.errorValue != null)) {
            return Boolean._of(false);
        }
        
        // Compare actual values if present
        if (okValue != null) {
            rtn._pipe(this.okValue._eq(arg.okValue));
        }
        if (errorValue != null) {
            rtn._pipe(this.errorValue._eq(arg.errorValue));
        }
    }
    return rtn;
}
```

#### String Representation Pattern
```java
// Result string shows both OK and ERROR values
@Override
public String _string() {
    if (isSet) {
        StringBuilder builder = new StringBuilder("{");
        if (okValue != null) {
            builder.append(okValue._string());
        }
        if (errorValue != null) {
            if (builder.length() > 1) {
                builder.append(", ");
            }
            builder.append(errorValue._string());
        }
        builder.append("}");
        return String._of(builder.toString());
    }
    return new String();
}
```

#### Merge and Copy Patterns
```java
// Merge fills empty slots without overwriting
@Ek9Operator("""
    operator :~:
      -> arg as Result of (O, E)""")
public void _merge(Result arg) {
    if (arg != null) {
        if (this.okValue == null) {
            this.okValue = arg.okValue;
        }
        if (this.errorValue == null) {
            this.errorValue = arg.errorValue;
        }
        if (this.okValue != null || this.errorValue != null) {
            set();
        }
    }
}

// Copy replaces entire state
@Ek9Operator("""
    operator :=:
      -> arg as Result of (O, E)""")
public void _copy(Result arg) {
    if (arg != null) {
        this.okValue = arg.okValue;
        this.errorValue = arg.errorValue;
        if (arg.isSet) {
            set();
        } else {
            unSet();
        }
    } else {
        unSet();
        this.okValue = null;
        this.errorValue = null;
    }
}
```

#### Result Testing Patterns
```java
// Test all three states
@Test
void testResultStates() {
    final var unsetResult = new Result();
    final var okResult = Result._ofOk(testValue);
    final var errorResult = Result._ofError(testError);
    
    // UNSET state
    assertTrue.accept(unsetResult._empty());
    assertFalse.accept(unsetResult.isOk());
    assertFalse.accept(unsetResult.isError());
    assertFalse.accept(unsetResult._isSet());
    
    // OK state
    assertFalse.accept(okResult._empty());
    assertTrue.accept(okResult.isOk());
    assertFalse.accept(okResult.isError());
    assertTrue.accept(okResult._isSet()); // _isSet() returns isOk()
    
    // ERROR state
    assertFalse.accept(errorResult._empty());
    assertFalse.accept(errorResult.isOk());
    assertTrue.accept(errorResult.isError());
    assertFalse.accept(errorResult._isSet()); // _isSet() returns isOk()
}

// Test conditional execution
@Test
void testConditionalExecution() {
    final var okResult = Result._ofOk(testValue);
    final var errorResult = Result._ofError(testError);
    
    final var mockConsumer = new MockConsumer();
    
    // OK result calls consumer
    okResult.whenOk(mockConsumer);
    assertTrue(mockConsumer.verifyCalledWith(testValue));
    
    // ERROR result doesn't call OK consumer
    final var mockConsumer2 = new MockConsumer();
    errorResult.whenOk(mockConsumer2);
    assertTrue(mockConsumer2.verifyNotCalled());
}
```

## Comprehensive Testing Patterns

### Test Structure and Organization

#### Base Test Class Pattern
```java
class YourTypeTest extends Common {
    // Test data setup - consistent naming
    private final YourType unset = new YourType();
    private final YourType value1 = YourType._of("test1");
    private final YourType value2 = YourType._of("test2");
    
    // Common assertion helpers from base class:
    // assertSet.accept(value)     - Validates object is set
    // assertUnset.accept(value)   - Validates object is unset
    // assertTrue.accept(bool)     - Validates Boolean is true and set
    // assertFalse.accept(bool)    - Validates Boolean is false and set
}
```

#### Standard Test Method Categories

**1. Construction Testing** (`testConstruction`)
```java
@Test
void testConstruction() {
    // Default constructor creates unset
    final var defaultConstructor = new YourType();
    assertUnset.accept(defaultConstructor);
    
    // Value constructor creates set
    final var valueConstructor = new YourType(testValue);
    assertSet.accept(valueConstructor);
    
    // Factory methods
    final var factoryEmpty = YourType._of();
    final var factoryWithValue = YourType._of("test");
    
    // String constructor (if applicable)
    final var stringConstructor = new YourType(String._of("test"));
    
    // Cross-type constructors
    final var crossType = new Float(Integer._of(42));
    
    // Null handling
    final var nullConstructor = new YourType(null);
    assertUnset.accept(nullConstructor);
}
```

**2. State Management Testing** (`testStateManagement`, `testIsSet`)
```java
@Test
void testStateManagement() {
    // Test _isSet operator
    assertFalse.accept(unset._isSet());
    assertTrue.accept(setValue._isSet());
    
    // Test _empty operator
    assertTrue.accept(unset._empty());
    assertFalse.accept(setValue._empty());
    
    // Test state transitions
    final var mutable = new YourType();
    assertUnset.accept(mutable);
    mutable.assign(testValue);
    assertSet.accept(mutable);
}
```

**3. Operator Testing** (`testEquality`, `testComparison`, `testArithmetic`)
```java
@Test
void testEquality() {
    // Set values equality
    assertTrue.accept(value1._eq(value1));
    assertFalse.accept(value1._eq(value2));
    
    // Unset propagation
    assertUnset.accept(unset._eq(value1));
    assertUnset.accept(value1._eq(unset));
    assertUnset.accept(unset._eq(unset));
}

@Test
void testArithmetic() {
    // Normal operations
    final var result = value1._add(value2);
    assertSet.accept(result);
    
    // Unset propagation
    assertUnset.accept(unset._add(value1));
    assertUnset.accept(value1._add(unset));
    
    // Cross-type operations
    final var crossResult = intValue._add(floatValue);
    assertTrue(crossResult instanceof Float);
}
```

**4. Assignment Operations Testing** (`testAssignmentOperators`, `testMutationOperators`)
```java
@Test
void testAssignmentOperators() {
    // _copy operator
    final var target = new YourType();
    target._copy(value1);
    assertSet.accept(target);
    assertEquals(value1, target);
    
    // _merge operator
    final var mergeTarget = new YourType();
    mergeTarget._merge(value1);
    assertSet.accept(mergeTarget);
    
    // Null handling
    target._copy(null);
    assertUnset.accept(target);
}

@Test
void testMutationOperators() {
    // += operator
    final var mutable = YourType._of(initialValue);
    mutable._addAss(increment);
    assertEquals(expectedResult, mutable);
    
    // Unset operand handling
    mutable._addAss(unset);
    assertSet.accept(mutable); // Should remain unchanged
}
```

**5. Utility Methods Testing** (`testUtilityMethods`, `testAsString`)
```java
@Test
void testUtilityMethods() {
    // String conversion
    final var str = setValue._string();
    assertSet.accept(str);
    assertEquals(expectedString, str);
    
    // Unset string conversion
    final var unsetStr = unset._string();
    assertUnset.accept(unsetStr);
    
    // Hash code
    final var hash = setValue._hashcode();
    assertSet.accept(hash);
    
    // Type-specific utilities
    final var length = stringValue._len();
    assertSet.accept(length);
}
```

**6. Edge Cases and Error Handling** (`testEdgeCases`, `testConstraintViolations`)
```java
@Test
void testEdgeCases() {
    // Boundary conditions
    final var maxValue = YourType._of(MAX_VALUE);
    final var overflow = maxValue._add(YourType._of(1));
    // Behavior depends on type - might be unset or throw exception
    
    // Division by zero
    assertThrows(Exception.class, () -> value._div(YourType._of(0)));
    
    // Invalid string parsing
    final var invalid = new YourType(String._of("invalid"));
    assertUnset.accept(invalid);
}
```

## Enhanced Testing Patterns

### JUnit 5 Integration Patterns
```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Modern JUnit 5 test patterns for EK9 types.
 */
class MoneyTest extends Common {

    @Test
    void testDomainSpecificBehavior() {
        // Enhanced null safety checking
        final var anotherTenPounds = Money._of("10.00#GBP");
        assertNotNull(anotherTenPounds);
        assertTrue.accept(tenPounds._eq(anotherTenPounds));
        
        // Business logic validation with explanatory comments
        // Merge with different currency - you cannot do this it is meaningless.
        // If you want to replace then use replace/copy.
        final var mergeTarget3 = new Money(tenPounds);
        mergeTarget3._merge(thirtyDollars);
        assertUnset.accept(mergeTarget3);
    }
}
```

### Comprehensive Edge Case Testing
```java
@Test
void testCrossCurrencyOperationFailures() {
    // Systematic testing of all cross-currency operations
    assertUnset.accept(tenPounds._add(thirtyDollars));
    assertUnset.accept(tenPounds._sub(thirtyDollars));
    assertUnset.accept(tenPounds._eq(thirtyDollars));
    assertUnset.accept(tenPounds._lt(thirtyDollars));
    assertUnset.accept(tenPounds._gt(thirtyDollars));
    assertUnset.accept(tenPounds._cmp(thirtyDollars));
    assertUnset.accept(tenPounds._fuzzy(thirtyDollars));
    assertUnset.accept(tenPounds._div(thirtyDollars));
    assertUnset.accept(tenPounds._rem(thirtyDollars));

    // Assignment operations with different currencies should unset
    final var mutable = new Money(tenPounds);
    assertNotNull(mutable);
    mutable._addAss(thirtyDollars);
    assertUnset.accept(mutable);
}
```

### Domain-Specific Testing Patterns
```java
@Test
void testCurrencyDecimalPlaces() {
    // Test automatic scaling to currency-specific decimal places
    final var usd = Money._of("10.123#USD");
    assertSet.accept(usd);
    assertEquals("10.12#USD", usd._string().state); // Should round to 2 places

    // JPY has 0 decimal places
    final var jpy = Money._of("1000.5#JPY");
    assertSet.accept(jpy);
    assertEquals("1001#JPY", jpy._string().state); // Should round to 0 places

    // Handle optional currencies gracefully
    try {
        final var clf = Money._of("45.99999#CLF");
        if (clf._isSet().state) {
            assertEquals("46.0000#CLF", clf._string().state);
        }
    } catch (Exception _) {
        // CLF might not be available in all Java installations
    }
}
```

### Business Logic Testing
```java
@Test
void testRoundingBehavior() {
    // Test real-world scenarios from documentation
    final var amount = Money._of("99.51#GBP");
    final var halved = amount._div(Float._of(2.0));
    assertSet.accept(halved);
    assertEquals("49.76#GBP", halved._string().state); // Should round up

    // Test complex calculation from documentation
    final var fortyEightSeventySix = Money._of("49.76#GBP");
    final var result = fortyEightSeventySix._mul(Float._of(-8.754));
    assertSet.accept(result);
    assertEquals("-435.60#GBP", result._string().state);
}
```

### Polymorphic Operation Testing
```java
@Test
void testPolymorphicOperations() {
    // Test both specific and Any versions of operators
    final var cmpResult = tenPounds._cmp(fivePounds);
    assertSet.accept(cmpResult);
    assertTrue.accept(Boolean._of(cmpResult.state > 0));

    // Polymorphic comparison
    assertSet.accept(tenPounds._cmp((Any) fivePounds));
    assertUnset.accept(tenPounds._cmp((Any) String._of("not money")));

    // Polymorphic equality with Any
    assertTrue.accept(tenPounds._eq((Any) anotherTenPounds));
    assertUnset.accept(tenPounds._eq((Any) String._of("not money")));
}
```

### Mock Integration Patterns

#### Lightweight Mock Usage
```java
// Test functional interfaces
@Test
void testFunctionalInterfaces() {
    final var acceptor = new MockAcceptor();
    setValue.whenPresent(acceptor);
    assertTrue(acceptor.verifyCalledWith(expectedValue));
    
    final var consumer = new MockConsumer();
    setValue.whenPresent(consumer);
    assertTrue(consumer.verifyCalledWith(expectedValue));
    
    // Unset handling
    final var unsetAcceptor = new MockAcceptor();
    unset.whenPresent(unsetAcceptor);
    assertTrue(unsetAcceptor.verifyNotCalled());
}
```

### Systematic Unset Testing Patterns

#### Unset Propagation Testing
```java
@Test
void testUnsetPropagation() {
    // Test all operators with unset inputs
    final var operations = List.of(
        () -> unset._add(value1),
        () -> value1._add(unset),
        () -> unset._sub(value1),
        () -> unset._mul(value1),
        // ... all operations
    );
    
    operations.forEach(op -> {
        final var result = op.get();
        assertUnset.accept(result);
    });
}
```

## Common Compilation Issues & Solutions

### 1. Boolean Type Mismatches
```java
// ❌ Error: Cannot convert EK9 Boolean to Java boolean
if (ek9Boolean) { }

// ✅ Solution: Use .state field
if (ek9Boolean.state) { }
```

### 2. String Type Mismatches  
```java
// ❌ Error: Cannot convert Java String to EK9 String
String testValue = "text";

// ✅ Solution: Use factory method
String testValue = String._of("text");
```

### 3. Assertion Method Issues
```java
// ❌ Error: Method not found
assertTrue(ek9Boolean);

// ✅ Solution: Use Common helper or static import
assertTrue.accept(ek9Boolean); // Common helper
// OR
import static org.junit.jupiter.api.Assertions.assertTrue;
assertTrue(ek9Boolean.state); // Convert to Java boolean
```

### 4. Unary Operator Issues
```java
// ❌ Error: Bad operand type for unary operator !
!ek9Boolean

// ✅ Solution: Use .state or EK9 negation
!ek9Boolean.state           // Java boolean negation
ek9Boolean._empty()         // EK9 semantic negation
```

### 5. State Assignment Issues
```java
// ❌ Error: Direct state assignment bypasses validation
myType.state = newValue;

// ✅ Solution: Use assign method
myType.assign(newValue);
```

## Advanced Patterns and Best Practices

### 1. **Constraint Validation Pattern**
```java
@Override
protected void validateConstraints() {
    super.validateConstraints();
    if (isSet && violatesConstraint(state)) {
        final var oldState = state;
        unSet();
        throw new Exception(String._of("Constraint violation: " + oldState));
    }
}
```

### 2. **Cross-Type Operation Pattern**
```java
// Promotion rules: Integer + Float → Float
@Ek9Operator("""
    operator + as pure
      -> arg as Float
      <- rtn as Float?""")
public Float _add(Float arg) {
    if (canProcess(arg)) {
        return Float._of(this.state + arg.state);
    }
    return new Float();
}
```

### 3. **Pipeline Operation Pattern**
```java
// _pipe ignores unset inputs for pipeline operations
@Ek9Operator("""
    operator |
      -> arg as T""")
public void _pipe(YourType arg) {
    if (arg != null && arg.isSet) {
        if (isSet) {
            // Accumulate/merge
            assign(combineValues(this.state, arg.state));
        } else {
            // First value
            assign(arg.state);
        }
    }
    // Ignore unset inputs - don't change state
}
```

### 4. **Immutable Operation Pattern**
```java
// Pure operations return new instances
@Ek9Operator("""
    operator + as pure
      -> arg as T
      <- rtn as T?""")
public YourType _add(YourType arg) {
    if (canProcess(arg)) {
        return YourType._of(this.state + arg.state);
    }
    return new YourType(); // Return unset for invalid operations
}
```

## Advanced Operator Implementation Patterns

### Remainder vs Modulus Operator Distinction
```java
// Use 'rem' for remainder operations instead of 'mod'
@Ek9Operator("""
    operator rem as pure
      -> arg as Money
      <- rtn as Money?""")
public Money _rem(Money arg) {
    if (canProcessSameCurrency(arg) && arg.state.compareTo(BigDecimal.ZERO) != 0) {
        Money result = _new();
        BigDecimal remainder = this.state.remainder(arg.state);
        result.assign(remainder, this.currency);
        return result;
    }
    return _new();
}
```

### Cross-Type Return Type Patterns
```java
// Money ÷ Money returns Float (ratio), not Money
@Ek9Operator("""
    operator / as pure
      -> arg as Money
      <- rtn as Float?""")
public Float _div(Money arg) {
    if (canProcessSameCurrency(arg) && arg.state.compareTo(BigDecimal.ZERO) != 0) {
        BigDecimal ratio = this.state.divide(arg.state, 10, RoundingMode.HALF_UP);
        return Float._of(ratio.doubleValue());
    }
    return new Float();
}

// Money × Integer/Float returns Money with proper scaling
@Ek9Operator("""
    operator * as pure
      -> arg as Float
      <- rtn as Money?""")
public Money _mul(Float arg) {
    if (canProcess(arg)) {
        Money result = _new();
        final var scale = currency.getDefaultFractionDigits();
        final var multiplier = BigDecimal.valueOf(arg.state);
        final var product = this.state.multiply(multiplier).setScale(scale, RoundingMode.HALF_UP);
        result.assign(product, this.currency);
        return result;
    }
    return _new();
}
```

### Advanced Mathematical Algorithms
```java
// Newton's method for BigDecimal square root
private BigDecimal sqrt(BigDecimal value) {
    BigDecimal x = value;
    BigDecimal previous;
    BigDecimal two = BigDecimal.valueOf(2);
    int scale = currency.getDefaultFractionDigits() + 10; // Extra precision for calculation

    do {
        previous = x;
        x = x.add(value.divide(x, scale, RoundingMode.HALF_UP))
             .divide(two, scale, RoundingMode.HALF_UP);
    } while (x.subtract(previous).abs().compareTo(BigDecimal.valueOf(0.0001)) > 0);

    return x.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
}

@Ek9Operator("""
    operator sqrt as pure
      <- rtn as Money?""")
public Money _sqrt() {
    if (isSet && state.compareTo(BigDecimal.ZERO) >= 0) {
        try {
            BigDecimal sqrtValue = sqrt(state);
            Money result = _new();
            result.assign(sqrtValue, currency);
            return result;
        } catch (ArithmeticException _) {
            return _new();
        }
    }
    return _new();
}
```

### Domain-Specific Validation in Operators
```java
// Currency-safe operations with business rule enforcement
@Ek9Operator("""
    operator + as pure
      -> arg as Money
      <- rtn as Money?""")
public Money _add(Money arg) {
    if (canProcessSameCurrency(arg)) {  // Business rule: same currency only
        Money result = _new();
        result.assign(this.state.add(arg.state), this.currency);
        return result;
    }
    return _new();  // Different currencies return unset
}

// Assignment operators that unset on business rule violations
@Ek9Operator("""
    operator +=
      -> arg as Money""")
public void _addAss(Money arg) {
    if (canProcessSameCurrency(arg)) {
        this.state = this.state.add(arg.state);
    } else {
        unSet();  // Violates currency compatibility rule
    }
}
```

### Utility Method Integration
```java
// Using inherited utility methods for precision
@Ek9Operator("""
    operator /=
      -> arg as Float""")
public void _divAss(Float arg) {
    if (canProcess(arg) && !nearEnoughToZero(arg.state)) {  // Use inherited zero check
        final var divisor = BigDecimal.valueOf(arg.state);
        final var scale = currency.getDefaultFractionDigits();
        this.state = this.state.divide(divisor, scale, RoundingMode.HALF_UP);
    } else {
        unSet();
    }
}
```

### 5. **Fuzzy Matching Pattern**
```java
// String: Levenshtein distance implementation
@Ek9Operator("""
    operator <~> as pure
      -> arg as String
      <- rtn as Integer?""")
public Integer _fuzzy(String arg) {
    if (!this.canProcess(arg)) {
        return new Integer();
    }
    Levenshtein fuzzy = new Levenshtein();
    return Integer._of(fuzzy.costOfMatch(this.state, arg.state));
}

// Boolean: Fuzzy match delegates to comparison
@Ek9Operator("""
    operator <~> as pure
      -> arg as Boolean
      <- rtn as Integer?""")
public Integer _fuzzy(Boolean arg) {
    //For boolean fuzzy match is just compare.
    return _cmp(arg);
}

// General pattern for distance-based fuzzy matching
@Ek9Operator("""
    operator <~> as pure
      -> arg as T
      <- rtn as Integer?""")
public Integer _fuzzy(YourType arg) {
    if (canProcess(arg)) {
        return Integer._of(calculateDistance(this.state, arg.state));
    }
    return new Integer();
}
```

## Business Logic Integration Patterns

### Format Parsing with Business Rules
```java
// Multi-layer validation implementing business format requirements
private void parseMoneyString(java.lang.String input) {
    try {
        // Format: amount#CurrencyCode (e.g., "10.50#USD", "99.99#GBP")
        int hashIndex = input.indexOf('#');
        if (hashIndex == -1 || hashIndex == 0 || hashIndex == input.length() - 1) {
            return; // Invalid format
        }

        final var amountStr = input.substring(0, hashIndex);
        final var currencyStr = input.substring(hashIndex + 1);

        if (currencyStr.length() != 3) {
            return; // Currency code must be 3 characters
        }

        final var amount = new BigDecimal(amountStr);
        final var curr = Currency.getInstance(currencyStr);

        // Automatic business rule: set scale to currency's default fraction digits
        final int scale = curr.getDefaultFractionDigits();
        final var scaled = amount.setScale(scale, RoundingMode.HALF_UP);

        assign(scaled, curr);
    } catch (IllegalArgumentException _) {
        unSet();
    }
}
```

### Currency Conversion with Exchange Rates
```java
// Business method for currency conversion
@Ek9Method("""
    convert() as pure
      ->
        multiplier as Float
        currencyCode as String
      <-
        rtn as Money?""")
public Money convert(Float multiplier, String currencyCode) {
    if (canProcess(multiplier) && canProcess(currencyCode)) {
        try {
            Currency targetCurrency = Currency.getInstance(currencyCode.state);
            return convertInternal(BigDecimal.valueOf(multiplier.state), targetCurrency);
        } catch (IllegalArgumentException _) {
            return _new();
        }
    }
    return _new();
}

// Internal conversion with automatic scaling
private Money convertInternal(BigDecimal exchangeRate, Currency targetCurrency) {
    final var result = _new();
    final var scale = targetCurrency.getDefaultFractionDigits();
    final var convertedAmount = this.state.multiply(exchangeRate).setScale(scale, RoundingMode.HALF_UP);
    result.assign(convertedAmount, targetCurrency);
    return result;
}
```

### Domain-Specific String Representation
```java
// Business-aware string formatting
@Override
@Ek9Operator("""
    operator $ as pure
      <- rtn as String?""")
public String _string() {
    if (isSet) {
        // Format: "amount#currencyCode" for business clarity
        return String._of(state.toPlainString() + "#" + currency.getCurrencyCode());
    }
    return new String();
}
```

### Business Rule Enforcement in Operations
```java
// Merge operation with explicit business logic
@Ek9Operator("""
    operator :~:
      -> arg as Money""")
public void _merge(Money arg) {
    if (isValid(arg)) {
        if (!isSet) {
            // Business rule: unset target accepts any valid currency
            assign(arg.state, arg.currency);
        } else if (this.currency.equals(arg.currency)) {
            // Business rule: same currency values can be added
            this.state = this.state.add(arg.state);
        } else {
            // Business rule: different currencies cannot be merged
            unSet();
        }
    }
}
```

### Automatic Precision Management
```java
// Automatic scaling based on currency business rules
@Ek9Operator("""
    operator * as pure
      -> arg as Float
      <- rtn as Money?""")
public Money _mul(Float arg) {
    if (canProcess(arg)) {
        Money result = _new();
        // Business rule: automatically apply currency-specific decimal places
        final var scale = currency.getDefaultFractionDigits();
        final var multiplier = BigDecimal.valueOf(arg.state);
        final var product = this.state.multiply(multiplier).setScale(scale, RoundingMode.HALF_UP);
        result.assign(product, this.currency);
        return result;
    }
    return _new();
}
```

## Testing Commands Reference

```bash
# Run specific test method (if supported)
mvn test -Dtest=YourTypeTest#testConstruction -pl ek9-lang

# Run with verbose output
mvn test -Dtest=YourTypeTest -pl ek9-lang -X

# Run all tests in module
mvn test -pl ek9-lang

# Run specific test category
mvn test -Dtest="*Test" -pl ek9-lang

# Clean and test
mvn clean test -pl ek9-lang
```

## File Locations Reference

### Implementation Files
- **Location**: `ek9-lang/src/main/java/org/ek9/lang/`
- **Pattern**: `{TypeName}.java`
- **Base classes**: `BuiltinType.java`, `Any.java` (interface)

### Test Files
- **Location**: `ek9-lang/src/test/java/org/ek9/lang/`
- **Pattern**: `{TypeName}Test.java`
- **Common helpers**: `Common.java` (base class)
- **Mock objects**: `MockAcceptor.java`, `MockConsumer.java`

### Key Files to Reference
- **BuiltinType.java** - Base class for all types
- **Any.java** - Universal interface
- **Common.java** - Test base class with assertion helpers
- **BooleanTest.java** - Three-state logic patterns
- **IntegerTest.java** - Comprehensive numeric testing
- **StringTest.java** - Text processing patterns, Levenshtein distance
- **ListTest.java** - Collection patterns, exception handling
- **OptionalTest.java** - Wrapper type patterns
- **ResultTest.java** - Dual-state error handling patterns
- **ExceptionTest.java** - Error handling patterns
- **DictTest.java** - Dictionary patterns, key-value operations
- **DictEntryTest.java** - Key-value pair patterns
- **PriorityQueueTest.java** - Ordered collection patterns
- **MoneyTest.java** - Currency-safe operations, business logic testing, precision handling
- **Money.java** - Multi-field type implementation, business rule enforcement, domain-specific validation
- **MockAcceptor.java**, **MockConsumer.java**, **MockFunction.java** - Lightweight mock patterns

## Quick Development Checklist

When developing a new EK9 built-in type:

**Implementation:**
- [ ] Extend `BuiltinType` class
- [ ] Implement all required constructors (default, copy, string, cross-type)
- [ ] Add factory methods (`_of()` variants)
- [ ] Implement core operators (`_isSet()`, `_string()`, `_eq()`, etc.)
- [ ] Add comparison operators if applicable
- [ ] Add arithmetic operators if applicable
- [ ] Add assignment operators (`_copy()`, `_merge()`, etc.)
- [ ] Override `validateConstraints()` for custom validation
- [ ] Use `canProcess()` for unset handling
- [ ] Handle cross-type operations with promotion rules

**Testing:**
- [ ] Extend `Common` class
- [ ] Use EK9 type factory methods (`Type._of()`)
- [ ] Test all constructors and factory methods
- [ ] Test all operators with set/unset combinations
- [ ] Use `.state` for Boolean conditionals
- [ ] Use Common assertion helpers for EK9 types
- [ ] Test unset value propagation scenarios
- [ ] Include null handling tests
- [ ] Test cross-type operations and type promotion
- [ ] Test constraint violations
- [ ] Verify exception handling and messages (List.first(), List.get())
- [ ] Test equality and hashCode consistency
- [ ] Test polymorphic operator patterns (specific type + Any versions)
- [ ] Test fuzzy matching operators where applicable
- [ ] Test pipeline operations (_pipe)
- [ ] Follow established naming patterns
- [ ] Test merge vs copy vs replace operator behaviors
- [ ] Test business rule enforcement in domain-specific operations
- [ ] Test automatic precision and scaling where applicable
- [ ] Use JUnit 5 static imports and assertNotNull for robustness
- [ ] For iterator implementations: test that empty structures return unset iterators (EK9 semantics)
- [ ] For Jackson integration: use Spliterators.spliteratorUnknownSize() and StreamSupport for iterator conversion
- [ ] Run `mvn test -Dtest={TestName} -pl ek9-lang` to verify

**Code Quality:**
- [ ] Follow consistent naming conventions
- [ ] Use `@Ek9Operator` and `@Ek9Method` annotations
- [ ] Include comprehensive JavaDoc
- [ ] Handle edge cases gracefully
- [ ] Ensure immutable operations return new instances
- [ ] Validate all inputs and handle null safely
- [ ] Follow unset propagation principles
- [ ] Test with representative data sets
- [ ] Use modern Java syntax (final var, unnamed catch variables, pattern matching)
- [ ] Apply @SuppressWarnings appropriately for checkstyle
- [ ] Implement domain-specific validation methods for complex types
- [ ] Use rem instead of mod for remainder operations

## State Initialization Patterns

### Collection Types (Always Set)
```java
// Collections are always set, even when empty
@Ek9Constructor("""
    List() as pure""")
public List() {
    set();  // Mark as set immediately
    // state is initialized to new ArrayList<>()
}
```

### Value Types (Initially Unset)
```java
// Most value types start unset
@Ek9Constructor("""
    Boolean() as pure""")
public Boolean() {
    super.unSet();  // Start unset
}
```

### String Type (Special Case)
```java
// String initializes state but starts unset
public class String extends BuiltinType {
    java.lang.String state = "";  // Initialize to empty
    
    public String() {
        super.unSet();  // But mark as unset
    }
}
```

## Exception vs Unset Patterns

### When to Throw Exceptions
- **List operations on empty collections**: `first()`, `last()`, `get(invalid_index)`
- **Result access with wrong state**: `ok()` when in ERROR state, `error()` when in OK state
- **Invalid constraint violations**: Overflow, invalid state transitions

### When to Return Unset
- **Arithmetic with unset operands**: All mathematical operations
- **Comparison with unset operands**: All comparison operations
- **Cross-type operations with incompatible types**: Any polymorphic operations

```java
// Exception pattern
public Any first() {
    if (state.isEmpty()) {
        throw new Exception(String._of("List is empty"));
    }
    return state.getFirst();
}

// Unset pattern
public Boolean _eq(Any arg) {
    if (!(arg instanceof TypeName)) {
        return new Boolean();  // Return unset for wrong type
    }
    // ... specific implementation
}
```

---

*This document provides comprehensive guidance based on analysis of all EK9 built-in types and their tests. Update as new patterns emerge.*

**Last Updated**: Based on analysis of Boolean, String, List, Result, Dict, PriorityQueue, Money, and mock classes (July 2025)

**Major Additions in this Update:**
- Modern Java code style patterns (final var, unnamed catch variables, pattern matching)
- Complex type implementation for multi-field types (Money with amount + currency)
- Enhanced testing patterns with JUnit 5 and business logic validation
- Advanced operator implementation (remainder vs modulus, cross-type returns, mathematical algorithms)
- Business logic integration patterns (currency conversion, format parsing, automatic precision)
- Currency-safe operations and domain-specific validation methods