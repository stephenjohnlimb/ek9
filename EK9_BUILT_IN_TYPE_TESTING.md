# EK9 Built-in Type Testing Best Practices

**Date**: 2025-11-15
**Purpose**: Comprehensive testing patterns for EK9 built-in types
**Based on**: FileSystemPath testing patterns and established EK9 conventions

---

## Overview

This guide provides comprehensive testing patterns for EK9 built-in types. These patterns ensure thorough coverage of tri-state semantics, operator behavior, and edge cases.

## Resource Management

### Always Use Try-With-Resources

For streams and closeable resources, always use try-with-resources:

```java
try (var stream = Files.walk(path)) {
  stream.sorted(Comparator.reverseOrder())
      .forEach(p -> { /* process */ });
}
```

**Why:** Ensures proper cleanup even if exceptions occur. Critical for file system operations and other resources.

### Use Standard Comparators

```java
// ✅ CORRECT - use standard comparator
stream.sorted(Comparator.reverseOrder())

// ❌ WRONG - custom lambda is unnecessary
stream.sorted((a, b) -> b.compareTo(a))
```

**Why:** Standard comparators are clearer and more maintainable.

### Proper Cleanup in @AfterEach

```java
@AfterEach
void tearDown() {
  if (tempDir != null && Files.exists(tempDir)) {
    try (var stream = Files.walk(tempDir)) {
      stream.sorted(Comparator.reverseOrder())
          .forEach(p -> {
            try {
              Files.delete(p);
            } catch (IOException e) {
              // Log but don't fail cleanup
            }
          });
    } catch (IOException e) {
      // Log but don't fail cleanup
    }
  }
}
```

**Why:** Comprehensive error handling ensures cleanup always happens, preventing test pollution.

## EK9 Type System Testing

### Test Both SET and UNSET States

**Always test operations with both set and unset values:**

```java
// Test with set values
assertTrue(setPath.exists().state);

// Test with unset values
assertUnset.accept(unsetPath.exists());
```

**Why:** EK9's tri-state model means operations must handle unset inputs correctly.

### Test All Parameter Combinations

**Test normal operation and unset/null parameter handling:**

```java
// Test normal operation
assertTrue(path.startsWith(validPath).state);

// Test with unset parameter
assertUnset.accept(path.startsWith(new FileSystemPath()));

// Test with null parameter (should reject)
assertThrows(NullPointerException.class,
    () -> path.startsWith(null));
```

**Why:** Built-in types must reject null but accept unset EK9 objects.

### Test Method Overloads Separately

```java
// Test createFile() - no parameters
final var file1 = path.createFile();
assertSet.accept(file1);

// Test createFile(Boolean) - with parameter
final var file2 = path.createFile(new Boolean(true));
assertSet.accept(file2);

// Test with unset parameter
assertUnset.accept(path.createFile(new Boolean()));
```

**Why:** Each overload may have different behavior with unset parameters.

## Test Organization

### Structure Tests with Clear Sections

```java
@Test
void testFileOperations() {
  // Setup
  final var tempFile = tempDir.resolve("test.txt");

  // Create file
  final var path = new FileSystemPath(tempFile.toString());
  final var created = path.createFile();
  assertSet.accept(created);

  // Verify existence
  assertTrue(path.exists().state);

  // Test operations on unset values
  final var unsetPath = new FileSystemPath();
  assertUnset.accept(unsetPath.exists());
}
```

**Why:** Clear sections make tests easier to read and maintain.

### Use Descriptive Variable Names

```java
// ✅ GOOD - indicates test scenario
final var unsetValue = new String();
final var emptyList = new List();
final var invalidPath = new FileSystemPath();

// ❌ BAD - unclear what's being tested
final var x = new String();
final var list = new List();
final var p = new FileSystemPath();
```

**Why:** Descriptive names document the test scenario.

### Group Related Assertions

```java
// Group all comparison operator tests together
assertUnset.accept(pathA._lt(new FileSystemPath()));
assertUnset.accept(pathA._lteq(new FileSystemPath()));
assertUnset.accept(pathA._gt(new FileSystemPath()));
assertUnset.accept(pathA._gteq(new FileSystemPath()));
```

**Why:** Logical grouping makes it easier to verify complete coverage.

### Test Edge Cases Explicitly

```java
//I know I just created it this is to check unset value being passed in
assertUnset.accept(nestedFile.createFile(new Boolean()));
```

**Why:** Comments explain non-obvious test scenarios.

## Comprehensive Coverage Patterns

### Test Operators with Unset Values

```java
// Comparison operators
assertUnset.accept(pathA._lt(new FileSystemPath()));
assertUnset.accept(pathA._lteq(new FileSystemPath()));
assertUnset.accept(pathA._gt(new FileSystemPath()));
assertUnset.accept(pathA._gteq(new FileSystemPath()));

// Equality operators
assertUnset.accept(pathA._eq(new FileSystemPath()));
assertUnset.accept(pathA._neq(new FileSystemPath()));

// Other operators
assertUnset.accept(pathA._add(new FileSystemPath()));
assertUnset.accept(pathA._cmp(new FileSystemPath()));
```

**Why:** All operators must handle unset inputs consistently.

### Test Assignment Operations That Can Corrupt State

```java
// Start with set value
final var mutablePath = new FileSystemPath("/valid/path");
assertSet.accept(mutablePath);

// Corrupt it with unset
mutablePath._addAss(new FileSystemPath());
assertUnset.accept(mutablePath);
```

**Why:** Mutating operators can corrupt object state if not implemented correctly.

### Test Polymorphic Operations

```java
// Specific type overload
assertTrue(path1._eq(path2).state);

// Any type overload (polymorphic)
assertTrue(path1._eq((Any) path2).state);

// Comparison with specific type
final var cmpResult = path1._cmp(path2);

// Comparison with Any type
final var cmpResultAny = path1._cmp((Any) path2);
```

**Why:** Both specific and polymorphic overloads must work correctly.

## Thread Safety and Isolation

### Use @Execution(SAME_THREAD) for File System Operations

```java
@Execution(ExecutionMode.SAME_THREAD)
class FileSystemPathTest {
  // Tests that modify file system
}
```

**Why:** Prevents parallel tests from interfering with each other's file operations.

### Use @ResourceLock for Shared Resources

```java
@ResourceLock(value = "FILE_SYSTEM", mode = READ_WRITE)
@Test
void testFileCreation() {
  // Test that modifies file system
}
```

**Why:** Ensures exclusive access to shared resources.

### Create Isolated Temporary Directories

```java
@BeforeEach
void setUp() throws IOException {
  tempDir = Files.createTempDirectory("ek9-test-");
}

@AfterEach
void tearDown() {
  // Clean up tempDir
}
```

**Why:** Each test gets its own isolated workspace, preventing interference.

### Clean Up All Test Artifacts

```java
@AfterEach
void tearDown() {
  if (tempDir != null && Files.exists(tempDir)) {
    try (var stream = Files.walk(tempDir)) {
      stream.sorted(Comparator.reverseOrder())
          .forEach(p -> {
            try {
              Files.delete(p);
            } catch (IOException e) {
              // Log but don't fail cleanup
            }
          });
    } catch (IOException e) {
      // Log but don't fail cleanup
    }
  }
}
```

**Why:** Prevents test pollution and resource leaks.

## Portable Testing

### Use Temporary Directories Only

```java
// ✅ GOOD - uses temp directory
final var tempDir = Files.createTempDirectory("ek9-test-");
final var path = new FileSystemPath(tempDir.toString());

// ❌ BAD - hardcoded path
final var path = new FileSystemPath("/tmp/test");
```

**Why:** Temporary directories work across all operating systems.

### Handle OS-Specific Behavior

```java
// File permissions may differ by OS
if (System.getProperty("os.name").toLowerCase().contains("windows")) {
  // Windows-specific test
} else {
  // Unix-specific test
}
```

**Why:** Some file system features are OS-dependent.

### Use File.separator for Cross-Platform Paths

```java
// ✅ GOOD - cross-platform
final var path = "dir" + File.separator + "file.txt";

// ❌ BAD - Unix-specific
final var path = "dir/file.txt";
```

**Why:** Path separators differ between Windows and Unix.

### Avoid Hardcoded Paths

```java
// ✅ GOOD - relative to temp directory
final var path = tempDir.resolve("test.txt");

// ❌ BAD - absolute hardcoded path
final var path = Paths.get("/home/user/test.txt");
```

**Why:** Hardcoded paths break on different machines and operating systems.

## Common Testing Assertions

### Standard Assertion Helpers

```java
// Assert object is SET
assertSet.accept(myObject);

// Assert object is UNSET
assertUnset.accept(myObject);

// Assert boolean state
assertTrue(result.state);
assertFalse(result.state);

// Assert equality
assertEquals(expected, actual);

// Assert exception thrown
assertThrows(SomeException.class, () -> {
  // Code that should throw
});
```

### Custom Assertions for EK9 Types

```java
// Assert tri-state semantics
final Consumer<BuiltinType> assertSet = obj ->
    assertTrue(obj._isSet(), "Expected object to be SET");

final Consumer<BuiltinType> assertUnset = obj ->
    assertFalse(obj._isSet(), "Expected object to be UNSET");
```

## See Also

- **`EK9_DEVELOPMENT_CONTEXT.md`** - Built-in type development patterns
- **`EK9_TRI_STATE_SEMANTICS.md`** - Tri-state model understanding
- **`EK9_OPERATOR_SEMANTICS.md`** - Operator behavior and semantics
- **`EK9_JUNIT_PARAMETERIZED_TESTING_PATTERNS.md`** - Parameterized testing patterns

---

**Last Updated**: 2025-11-15
