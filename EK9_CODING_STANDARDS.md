# EK9 Coding Standards

This document defines the coding standards for the EK9 project, ensuring consistent, readable, and maintainable code across all components. These standards are based on the EK9 code style configuration and integrate with EK9-specific language constructs.

## Table of Contents
- [Formatting and Indentation](#formatting-and-indentation)
- [Import Organization](#import-organization)
- [Variable and Parameter Guidelines](#variable-and-parameter-guidelines)
- [Method and Class Structure](#method-and-class-structure)
- [Control Flow Statements](#control-flow-statements)
- [EK9-Specific Standards](#ek9-specific-standards)
- [Code Examples](#code-examples)
- [IDE Configuration](#ide-configuration)

## Formatting and Indentation

### Basic Indentation Rules
- **Indent Size**: 2 spaces (never tabs)
- **Continuation Indent**: 4 spaces for wrapped lines
- **Line Length**: 120 characters for Java code, 160 for other files
- **Line Separators**: Unix LF (Line Feed) only

```java
// ✅ CORRECT - 2 space indentation
public class ExampleClass {
  private final String value;
  
  public ExampleClass(final String value) {
    this.value = value;
  }
}

// ❌ INCORRECT - Using tabs or 4 spaces
public class ExampleClass {
    private final String value;  // Too much indentation
}
```

### Line Wrapping
Aggressive wrapping is preferred to maintain readability:

```java
// ✅ CORRECT - Method parameters wrapped
public void methodWithManyParameters(
    final String parameter1,
    final String parameter2,
    final String parameter3) {
  // Implementation
}

// ✅ CORRECT - Method calls wrapped
final var result = someObject
    .methodCall()
    .anotherMethod()
    .finalMethod();

// ✅ CORRECT - Binary operations wrapped with operator on next line
final var complexCondition = someValue
    && anotherValue
    || yetAnotherValue;
```

## Import Organization

### Import Rules
- **No wildcard imports**: Each class imported individually
- **Import order**: 
  1. Module imports
  2. Static imports  
  3. Blank line
  4. Regular imports (alphabetically)

```java
// ✅ CORRECT - Explicit imports, proper order
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.function.Consumer;
import org.ek9.lang.Integer;
import org.ek9tooling.Ek9Class;

// ❌ INCORRECT - Wildcard imports
import java.util.*;
import org.ek9.lang.*;
```

### Package Declaration
Always include one blank line before the package declaration:

```java
// ✅ CORRECT

package org.ek9.lang;

import java.util.List;
```

## Variable and Parameter Guidelines

### Final Variables and Parameters
- **All local variables must be final** when possible
- **All method parameters must be final**
- **Package-local visibility** by default unless specifically needed otherwise

```java
// ✅ CORRECT - Final parameters and local variables
public String processValue(final String input, final Integer count) {
  final var processed = input.toLowerCase();
  final var result = processed.repeat(count.state);
  return result;
}

// ❌ INCORRECT - Missing final keywords
public String processValue(String input, Integer count) {
  var processed = input.toLowerCase();  // Should be final
  var result = processed.repeat(count.state);
  return result;
}
```

### Variable Declaration
- Use `var` with `final` for local variables when type is obvious
- Explicit types when clarity is needed

```java
// ✅ CORRECT - Clear intent with final var
final var stringValue = String._of("example");
final var integerValue = Integer._of(42);

// ✅ CORRECT - Explicit type when needed
final List<Consumer<String>> processors = new ArrayList<>();
```

## Method and Class Structure

### Method Organization
- Package-local methods by default
- Public methods only when explicitly needed for API
- Proper spacing and parameter formatting

```java
// ✅ CORRECT - Package-local method with proper formatting
Integer calculateSum(final Integer first, final Integer second) {
  if (!first._isSet().state || !second._isSet().state) {
    return new Integer();  // Return unset for invalid inputs
  }
  return Integer._of(first.state + second.state);
}
```

### Array Initialization
Space before array initializer braces:

```java
// ✅ CORRECT - Space before brace
final int[] values = new int[] {1, 2, 3, 4, 5};

// ❌ INCORRECT - No space before brace  
final int[] values = new int[]{1, 2, 3, 4, 5};
```

## Control Flow Statements

### Mandatory Braces
**All control flow statements MUST use braces**, even for single statements:

```java
// ✅ CORRECT - Always use braces
if (condition) {
  doSomething();
}

while (iterator.hasNext()) {
  processItem(iterator.next());
}

for (final var item : collection) {
  processItem(item);
}

// ❌ INCORRECT - Missing braces
if (condition)
  doSomething();  // Never acceptable
```

### Exception Handling
- Use meaningful variable names for exceptions
- Use `_` for ignored exceptions with `@SuppressWarnings` when appropriate

```java
// ✅ CORRECT - Meaningful exception handling
try {
  final var result = parseValue(input);
  return result;
} catch (final NumberFormatException parseError) {
  logger.warn("Failed to parse input: {}", input, parseError);
  return new Integer();  // Return unset for EK9 semantics
}

// ✅ CORRECT - Ignored exception with suppression
@SuppressWarnings("checkstyle:CatchParameterName")
try {
  state = UUID.fromString(arg.state);
} catch (final IllegalArgumentException _) {
  state = UUID.randomUUID();  // Fallback behavior
}
```

## EK9-Specific Standards

### EK9 Annotations
Proper formatting for EK9 annotations using triple-quoted strings:

```java
// ✅ CORRECT - Multi-line EK9 annotation with proper indentation
@Ek9Method("""
    methodName() as pure
      -> param as ParamType
      <- rtn as ReturnType?""")
public ReturnType methodName(final ParamType param) {
  // Implementation
}

// ✅ CORRECT - Single line annotation
@Ek9Constructor("Integer() as pure")
public Integer() {
  super.unSet();
}
```

### EK9 Operator Method Names
Follow the definitive mapping in `OperatorMap.java`:

```java
// ✅ CORRECT - Proper operator method names
public Boolean _eq(final Integer other) { }      // == operator
public Boolean _neq(final Integer other) { }     // <> operator  
public Integer _hashcode() { }                   // #? operator
public Boolean _isSet() { }                      // ? operator
public String _string() { }                      // $ operator
```

### EK9 Set/Unset Semantics
Consistent handling of EK9's set/unset pattern:

```java
// ✅ CORRECT - EK9 set/unset pattern
public Integer _add(final Integer other) {
  if (!isValid(this) || !isValid(other)) {
    return new Integer();  // Return unset, not null
  }
  return Integer._of(this.state + other.state);
}

// ✅ CORRECT - Helper method for validation
private boolean isValid(final Integer value) {
  return value != null && value._isSet().state;
}
```

### Factory Method Patterns
Consistent `_of()` factory method implementations:

```java
// ✅ CORRECT - Consistent factory method pattern
public static Integer _of() {
  return new Integer();  // Unset instance
}

public static Integer _of(final String value) {
  final var result = new Integer();
  if (value != null && value._isSet().state) {
    result.assign(parseValue(value.state));
  }
  return result;
}
```

## Code Examples

### Complete Class Example

```java

package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.function.Consumer;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Method;

/**
 * Example class demonstrating EK9 coding standards.
 */
@Ek9Class("""
    ExampleType as open""")
public class ExampleType extends BuiltinType {
  
  private final String state;
  
  // Constructor with proper annotation formatting
  @Ek9Constructor("ExampleType() as pure")
  public ExampleType() {
    super.unSet();
    this.state = "";
  }
  
  @Ek9Constructor("""
      ExampleType() as pure
        -> value as String""")
  public ExampleType(final String value) {
    unSet();
    if (isValid(value)) {
      this.state = value.state;
      set();
    } else {
      this.state = "";
    }
  }
  
  // Method with proper parameter and return type formatting
  @Ek9Method("""
      combine() as pure
        -> other as ExampleType
        <- rtn as ExampleType?""")
  public ExampleType combine(final ExampleType other) {
    if (!isValid(this) || !isValid(other)) {
      return new ExampleType();  // Return unset
    }
    return ExampleType._of(this.state + other.state);
  }
  
  // Factory methods following EK9 patterns
  public static ExampleType _of() {
    return new ExampleType();
  }
  
  public static ExampleType _of(final String value) {
    return new ExampleType(value);
  }
  
  // Operator implementation with proper naming
  @Ek9Operator("""
      operator == as pure
        -> arg as ExampleType
        <- rtn as Boolean?""")
  public Boolean _eq(final ExampleType other) {
    if (!isValid(this) || !isValid(other)) {
      return new Boolean();  // Return unset
    }
    return Boolean._of(this.state.equals(other.state));
  }
  
  // Helper method with package-local visibility
  private boolean isValid(final ExampleType value) {
    return value != null && value._isSet().state;
  }
}
```

### Test Class Example

```java

package org.ek9.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Test class demonstrating EK9 testing standards.
 */
class ExampleTypeTest extends Common {
  
  // Test data with final variables
  private final ExampleType unset = new ExampleType();
  private final String testValue = String._of("TestValue");
  private final ExampleType setExample = new ExampleType(testValue);
  
  @Test
  void testConstruction() {
    // Test default constructor creates unset instance
    final var defaultConstructor = new ExampleType();
    assertNotNull(defaultConstructor);
    assertUnset.accept(defaultConstructor);
    
    // Test parameterized constructor
    final var paramConstructor = new ExampleType(testValue);
    assertSet.accept(paramConstructor);
    assertTrue.accept(paramConstructor._isSet());
  }
  
  @Test
  void testCombineMethod() {
    // Test normal operation
    final var first = ExampleType._of("Hello");
    final var second = ExampleType._of(" World");
    final var result = first.combine(second);
    
    assertSet.accept(result);
    assertEquals("Hello World", result.state);
    
    // Test unset propagation
    assertUnset.accept(unset.combine(setExample));
    assertUnset.accept(setExample.combine(unset));
    assertUnset.accept(unset.combine(unset));
  }
}
```

## IDE Configuration

### IntelliJ IDEA Setup
The provided `Code standard EK9.xml` can be imported directly into IntelliJ IDEA:

1. Go to **File → Settings → Editor → Code Style → Java**
2. Click the gear icon and select **Import Scheme → IntelliJ IDEA code style XML**
3. Select the `Code standard EK9.xml` file
4. Apply the "EK9" scheme to your project

### Key IDE Settings Applied
- 2-space indentation with no tabs
- 120-character line limit for Java
- Aggressive wrapping for readability
- Final variable generation
- Package-local visibility by default
- Proper import organization

### Code Formatting Shortcuts
- **Ctrl+Alt+L** (Windows/Linux) or **Cmd+Alt+L** (macOS): Format code
- **Ctrl+Alt+O** (Windows/Linux) or **Cmd+Alt+O** (macOS): Optimize imports

## Validation and Code Review

### Pre-commit Checklist
- [ ] All indentation uses 2 spaces (no tabs)
- [ ] Line length under 120 characters
- [ ] All local variables and parameters are final
- [ ] No wildcard imports
- [ ] All control flow statements use braces
- [ ] EK9 annotations use proper multi-line formatting
- [ ] Set/unset semantics properly handled
- [ ] Consistent factory method patterns used

### Automated Validation
The coding standards are enforced through:
- IDE code style configuration
- Checkstyle rules (if configured)
- Code review process
- Maven build integration (where applicable)

## References

- **OperatorMap.java**: Definitive mapping of EK9 operators to Java method names
- **EK9_OPERATOR_SEMANTICS.md**: Understanding EK9's unique operator behavior
- **EK9_DEVELOPMENT_CONTEXT.md**: Comprehensive EK9 development patterns and testing approaches

## Updates and Evolution

This document should be updated when:
- New EK9 language features are added
- Code style requirements change
- IDE configuration is modified
- New coding patterns are established

All updates should maintain backward compatibility and be communicated to the development team.