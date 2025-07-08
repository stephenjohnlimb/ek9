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
    TypeName of type T as open""")

@Ek9Class("""
    Iterator of type T as abstract""")
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

## Standard Implementation Patterns

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

### Collection Types (List, Optional, Iterator, Result)

#### Key Patterns
- **List**: Always set (empty list ≠ unset list), supports generics
- **Optional**: Represents potentially absent values, different from unset
- **Iterator**: Stateful, `_isSet()` indicates `hasNext()`
- **Result**: Dual-state error handling type with OK and ERROR values

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

### 5. **Fuzzy Matching Pattern**
```java
// Fuzzy comparison for approximate matching
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
- **StringTest.java** - Text processing patterns
- **ListTest.java** - Collection patterns
- **OptionalTest.java** - Wrapper type patterns
- **ResultTest.java** - Dual-state error handling patterns
- **ExceptionTest.java** - Error handling patterns

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
- [ ] Test cross-type operations
- [ ] Test constraint violations
- [ ] Verify exception handling and messages
- [ ] Test equality and hashCode consistency
- [ ] Follow established naming patterns
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

---

*This document provides comprehensive guidance based on analysis of all EK9 built-in types and their tests. Update as new patterns emerge.*