# EK9 JUnit Parameterized Testing Patterns

This document captures best practices and lessons learned for JUnit 5 parameterized testing in the EK9 project, based on real refactoring experiences during JSON test suite development.

## Table of Contents
- [Core Principles](#core-principles)
- [Common Anti-Patterns and Solutions](#common-anti-patterns-and-solutions)
- [Best Practices](#best-practices)
- [Real-World Examples](#real-world-examples)
- [Guidelines for Data Providers](#guidelines-for-data-providers)
- [When NOT to Use Parameterized Tests](#when-not-to-use-parameterized-tests)

## Core Principles

### 1. Simplicity Over Abstraction
**Golden Rule**: The best abstraction is often no abstraction at all.

- Use direct parameter provision instead of wrapper objects when possible
- Avoid creating records or classes just to pass test data
- Let JUnit's parameterization handle the complexity

### 2. Direct Data Provision
Provide actual test objects directly rather than metadata that requires runtime interpretation.

```java
// ✅ GOOD: Direct provision
static Stream<JSON> getTestCases() {
    return Stream.of(
        jsonFromString("test"),
        createJsonArray()._add(jsonFromString("item")),
        createNamedJson("key", jsonFromString("value"))
    );
}

// ❌ BAD: Metadata approach
static Stream<TestCase> getTestCases() {
    return Stream.of(
        new TestCase("Value", () -> jsonFromString("test")),
        new TestCase("Array", () -> { /* complex setup */ }),
        new TestCase("Object", () -> createNamedJson("key", jsonFromString("value")))
    );
}
```

### 3. Compile-Time Safety
Prefer compile-time safety over runtime string matching and conditionals.

## Common Anti-Patterns and Solutions

### Anti-Pattern: Over-Engineered Record Wrappers

**❌ Over-Engineered Approach:**
```java
record ClearMethodTestCase(java.lang.String typeName, Supplier<JSON> jsonCreator) {}

static Stream<ClearMethodTestCase> getClearMethodTestCases() {
    return Stream.of(
        new ClearMethodTestCase("Value", null), // Forced to use null due to static issues
        new ClearMethodTestCase("Array", null), 
        new ClearMethodTestCase("Object", null)
    );
}

@ParameterizedTest
@MethodSource("getClearMethodTestCases")
void testClearMethodMakesJsonUnset(ClearMethodTestCase testCase) {
    // Complex switch statement based on type name
    final JSON json;
    switch (testCase.typeName()) {
        case "Value" -> json = jsonFromString("test");
        case "Array" -> {
            json = createJsonArray();
            addStringElement(json, "item");
        }
        case "Object" -> json = createNamedJson("key", jsonFromString("value"));
        default -> throw new IllegalArgumentException("Unknown type: " + testCase.typeName());
    }
    
    // Test logic...
}
```

**Problems:**
- Unnecessary abstraction complexity
- Runtime type checking with potential for errors
- Static/instance method access conflicts
- Verbose setup logic in test method
- More lines of code for same functionality

**✅ Elegant Solution:**
```java
static Stream<JSON> getClearMethodTestCases() {
    return Stream.of(
        jsonFromString("test"),                                    // Value JSON
        createJsonArray()._add(jsonFromString("item")),           // Array JSON  
        createNamedJson("key", jsonFromString("value"))           // Object JSON
    );
}

@ParameterizedTest
@MethodSource("getClearMethodTestCases") 
void testClearMethodMakesJsonUnset(JSON json) {
    assertNotNull(json);
    assertSet.accept(json); // Verify JSON is initially set

    final var result = json.clear();
    assertUnset.accept(result);
    assertUnset.accept(json); // Original should also be unset
}
```

**Benefits:**
- 7 lines vs ~20 lines of code
- Compile-time safety
- Self-documenting test data
- No runtime switching logic
- Direct parameter passing

### Anti-Pattern: Complex Static Method Workarounds

When you encounter static method access issues, the solution is usually to simplify the approach, not create complex workarounds.

**❌ Complex Workaround:**
```java
// Trying to work around static access by using null suppliers
// and complex conditional logic in test methods
```

**✅ Simple Solution:**
```java
// Use fluent APIs and direct method calls in data provider
createJsonArray()._add(jsonFromString("item"))
```

## Best Practices

### 1. Use Fluent APIs for Complex Setup

Instead of multi-step setup, leverage fluent method chaining:

```java
// ✅ GOOD: One-line fluent creation
createJsonArray()._add(jsonFromString("item"))

// ❌ BAD: Multi-step setup
final var array = createJsonArray();
addStringElement(array, "item");
return array;
```

### 2. Self-Documenting Data Providers

Make your test data immediately understandable:

```java
static Stream<JSON> getJsonTypes() {
    return Stream.of(
        jsonFromString("test"),        // Tests value JSON
        createJsonArray(),             // Tests empty array
        createJsonObject(),            // Tests empty object
        createComplexNestedJson()      // Tests complex structures
    );
}
```

### 3. Keep Test Methods Focused

Test methods should focus only on the behavior being tested, not on setup logic:

```java
@ParameterizedTest
@MethodSource("getJsonTypes")
void testJsonBehavior(JSON json) {
    // Test logic only - no setup complexity
    assertTrue.accept(json.someMethod());
}
```

### 4. Prefer Direct Types Over Wrappers

```java
// ✅ GOOD: Direct parameter
void testMethod(JSON json)

// ❌ BAD: Wrapper parameter
void testMethod(JsonTestCase testCase)
```

## Real-World Examples

### Example 1: Testing Constructor Behaviors

```java
static Stream<UnsetConstructorTestCase> getUnsetConstructorTestCases() {
    return Stream.of(
        new UnsetConstructorTestCase("String", () -> new JSON(new String())),
        new UnsetConstructorTestCase("Integer", () -> new JSON(new Integer())),
        new UnsetConstructorTestCase("Boolean", () -> new JSON(new Boolean())),
        // ... more constructor types
    );
}

@ParameterizedTest
@MethodSource("getUnsetConstructorTestCases")
void testUnsetConstructorCreatesUnsetJson(UnsetConstructorTestCase testCase) {
    final var unsetJson = testCase.constructor().get();
    assertUnset.accept(unsetJson);
}
```

**Note**: This example uses wrapper objects because the constructors need different parameter types. The wrapper is justified here because it solves a real problem (type variance), unlike the clear method example where it was unnecessary.

### Example 2: Simple Behavior Testing

```java
static Stream<JSON> getJsonTypesForClearing() {
    return Stream.of(
        jsonFromString("test"),
        createJsonArray()._add(jsonFromString("item")),
        createNamedJson("key", jsonFromString("value"))
    );
}

@ParameterizedTest
@MethodSource("getJsonTypesForClearing")
void testClearMakesJsonUnset(JSON json) {
    final var result = json.clear();
    assertUnset.accept(result);
    assertUnset.accept(json);
}
```

## Guidelines for Data Providers

### Static Method Requirements
- Data provider methods must be `static` when referenced from `@MethodSource`
- They should return `Stream<T>` where T is your parameter type
- Keep them simple and focused

### When to Use Wrapper Objects
Use wrapper objects only when you have a genuine need:
- **✅ Good reason**: Different constructor parameter types (String vs Integer vs Boolean)
- **✅ Good reason**: Multiple related parameters that belong together
- **❌ Bad reason**: Trying to pass metadata that requires runtime interpretation
- **❌ Bad reason**: Working around static method access issues

### Data Provider Location
```java
// ✅ GOOD: In the nested test class near the test
@Nested
class SomeTestGroup {
    static Stream<JSON> getTestData() { /* ... */ }
    
    @ParameterizedTest
    @MethodSource("getTestData")
    void testSomething(JSON json) { /* ... */ }
}
```

## When NOT to Use Parameterized Tests

### 1. Single Test Case
If you only have one scenario, a regular `@Test` is simpler:

```java
// ✅ GOOD: Simple single test
@Test
void testSpecificBehavior() {
    final var json = jsonFromString("test");
    // test logic
}
```

### 2. Highly Different Test Logic
When test cases require completely different assertion logic, separate tests are clearer:

```java
// ✅ GOOD: Separate tests for different behaviors
@Test
void testArraySpecificBehavior() { /* array-specific assertions */ }

@Test  
void testObjectSpecificBehavior() { /* object-specific assertions */ }
```

### 3. Complex Setup with No Common Pattern
When setup logic is completely different and has no common pattern, parameterization adds complexity without benefit.

## Key Lessons Learned

### 1. Question Every Abstraction
Before creating wrapper objects or complex data structures, ask:
- "What problem does this solve?"
- "Can I provide the data directly?"
- "Am I creating complexity to solve a problem that doesn't exist?"

### 2. Leverage Language and Framework Features
- Use fluent APIs for concise object creation
- Let JUnit handle parameterization mechanics
- Prefer compile-time features over runtime logic

### 3. Optimize for Readability
The most elegant code is often the most readable code. If someone else (or future you) can't immediately understand what a test does, it needs simplification.

### 4. Static vs Instance Method Issues
If you're fighting static method access issues in data providers, you're probably over-complicating the solution. Step back and look for a simpler approach.

## Conclusion

The best parameterized tests are those where the parameterization feels natural and obvious. If you find yourself creating complex infrastructure just to parameterize a test, that's usually a sign that a simpler approach exists.

Remember: **The goal of parameterized tests is to reduce duplication and improve clarity, not to showcase clever abstractions.**