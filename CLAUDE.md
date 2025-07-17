# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EK9 is a new programming language implementation with a comprehensive compiler written in Java 23. The project consists of a multi-pass compiler that transforms EK9 source code (`.ek9` files) into various target formats, primarily Java bytecode.

## Build System and Common Commands

This is a Maven-based project with a multi-module structure. All commands should be run from the root directory.

### Core Build Commands
- `mvn clean install` - Build the entire project and install to local repository
- `mvn clean compile` - Compile all modules
- `mvn test` - Run all unit tests (uses parallel execution with 8 threads)
- `mvn clean` - Clean all build artifacts

### EK9 Compiler Commands
After building, the EK9 compiler provides these commands:
- `java -jar compiler-main/target/ek9c-jar-with-dependencies.jar -c <file.ek9>` - Compile EK9 source
- `java -jar compiler-main/target/ek9c-jar-with-dependencies.jar -ls` - Start Language Server mode
- `java -jar compiler-main/target/ek9c-jar-with-dependencies.jar -t <file.ek9>` - Run tests
- `java -jar compiler-main/target/ek9c-jar-with-dependencies.jar -d <file.ek9>` - Debug mode

### Native Binary (GraalVM)
- `native-image --no-fallback -jar ek9c-jar-with-dependencies.jar` - Create native binary
- `./ek9` - Run native binary directly

### Testing
- `mvn test -Dtest=ExamplesBasicsTest` - Run specific test class
- `mvn test -pl compiler-main` - Run tests for specific module

## Module Structure

### compiler-main
The core compiler implementation containing:
- **CLI commands** (`org.ek9lang.cli`) - All command-line tools and entry points
- **Compiler core** (`org.ek9lang.compiler`) - Multi-pass compiler engine with 12 phases
- **LSP server** (`org.ek9lang.lsp`) - Language Server Protocol implementation

### compiler-tooling
Tooling support for EK9 constructs and language features.

### java-introspection
Java reflection and introspection utilities for bootstrap and external library integration.

### ek9-lang
Core EK9 language runtime and built-in types.

## Compiler Architecture

The EK9 compiler follows a **12-phase compilation pipeline**:

### Frontend (Phases 0-3)
1. **PARSING** - ANTLR4-based parsing
2. **SYMBOL_DEFINITION** - Symbol table creation
3. **DUPLICATION_CHECK** - Duplicate detection
4. **REFERENCE_CHECKS** - Reference validation

### Middle-end (Phases 4-8)
5. **EXPLICIT_TYPE_SYMBOL_DEFINITION** - Type resolution
6. **TYPE_HIERARCHY_CHECKS** - Inheritance validation
7. **FULL_RESOLUTION** - Template and generic resolution
8. **POST_RESOLUTION_CHECKS** - Symbol validation
9. **PRE_IR_CHECKS** - Code flow analysis

### Backend (Phases 9-11)
10. **IR_GENERATION** - Intermediate representation generation
11. **CODE_GENERATION** - Target code generation
12. **PACKAGING** - Application packaging

### Key Classes
- `Ek9Compiler` - Main compiler orchestrator
- `CompilerPhase` - Base class for all compilation phases
- `Workspace` - Manages compilation units and source files
- `CompilableProgram` - Central data structure for compilation

## Development Guidelines

### EK9 Built-in Type Development
For comprehensive guidance on developing EK9 built-in types and unit tests, see:
- **`EK9_DEVELOPMENT_CONTEXT.md`** - Complete reference with patterns, conventions, and best practices

This context file contains:
- EK9 type system architecture and inheritance patterns
- Standard implementation patterns for constructors, operators, and validation
- Comprehensive testing patterns and assertion helpers
- Type-specific insights (Boolean, Integer, Float, String, Collections, etc.)
- Common compilation issues and solutions
- Advanced patterns and best practices

### Code Style
- Java 23 with virtual threads support
- Follow existing naming conventions (CamelCase for classes, camelCase for methods)
- All new code must include comprehensive unit tests
- Use parallel processing where possible for performance

### Testing
- Test files are located in `src/test/resources/examples/`
- Bad examples (should fail compilation) are in `src/test/resources/badExamples/`
- Use `ExamplesBasicsTest` pattern for testing language features
- For built-in type testing, follow patterns in `EK9_DEVELOPMENT_CONTEXT.md`

### EK9 Built-in Type Testing Best Practices

Based on FileSystemPath testing patterns, follow these guidelines:

#### Resource Management
- **Always use try-with-resources** for streams and closeable resources:
  ```java
  try (var stream = Files.walk(path)) {
    stream.sorted(Comparator.reverseOrder())
        .forEach(p -> { /* process */ });
  }
  ```
- Use `Comparator.reverseOrder()` instead of custom lambdas like `(a, b) -> b.compareTo(a)`
- Proper cleanup in `@AfterEach` methods with comprehensive error handling

#### EK9 Type System Testing
- **Test both SET and UNSET states** for all operations:
  ```java
  // Test with set values
  assertTrue(setPath.exists().state);
  
  // Test with unset values  
  assertUnset.accept(unsetPath.exists());
  ```
- **Test all parameter combinations** including unset/null inputs:
  ```java
  // Test normal operation
  assertTrue(path.startsWith(validPath).state);
  
  // Test with unset parameter
  assertUnset.accept(path.startsWith(new FileSystemPath()));
  ```
- **Test method overloads** separately (e.g., `createFile()` vs `createFile(Boolean)`)

#### Test Organization
- **Structure tests with clear sections** and explanatory comments
- **Use descriptive variable names** that indicate the test scenario
- **Group related assertions** logically within test methods
- **Test edge cases explicitly** with comments explaining the scenario:
  ```java
  //I know I just created it this is to check unset value being passed in
  assertUnset.accept(nestedFile.createFile(new Boolean()));
  ```

#### Comprehensive Coverage Patterns
- **Test operators with unset values**:
  ```java
  assertUnset.accept(pathA._lt(new FileSystemPath()));
  assertUnset.accept(pathA._lteq(new FileSystemPath()));
  assertUnset.accept(pathA._gt(new FileSystemPath()));
  assertUnset.accept(pathA._gteq(new FileSystemPath()));
  ```
- **Test assignment operations that can corrupt state**:
  ```java
  //Now corrupt it with unset
  mutablePath1._addAss(new FileSystemPath());
  assertUnset.accept(mutablePath1);
  ```
- **Test polymorphic operations** (e.g., `_cmp(Any)` vs `_cmp(SpecificType)`)

#### Thread Safety and Isolation
- Use `@Execution(SAME_THREAD)` for file system operations
- Use `@ResourceLock` for shared resource access
- Create isolated temporary directories per test
- Clean up all test artifacts reliably

#### Portable Testing
- Use temporary directories and files only
- Handle OS-specific behavior gracefully (e.g., file permissions)
- Use `File.separator` for cross-platform path operations
- Avoid hardcoded paths or OS-specific assumptions

### EK9 Source Files
- EK9 uses indentation-based syntax (similar to Python)
- Files must end with `.ek9` extension
- Use examples in `src/test/resources/examples/` as reference

## Language Server Integration

The compiler includes full LSP support:
- Start with `java -jar <jar> -ls`
- Integrates with VSCode extension for syntax highlighting
- Provides real-time compilation and error reporting

## Bootstrap Process

The compiler has a sophisticated bootstrap mechanism that loads built-in EK9 types and language constructs before compiling user code. This is handled by `Ek9LanguageBootStrap` and related classes.

## Target Architectures

Currently supports:
- Java bytecode generation (primary target)
- Planned: LLVM IR output for native compilation

## Common Development Tasks

### Adding New Language Features
1. Update ANTLR4 grammar in `compiler-main/src/main/antlr4/org/ek9lang/antlr/`
2. Add symbol definitions in appropriate compiler phase
3. Update IR generation and code generation phases
4. Add comprehensive tests in `examples/` directory

### Running Single Tests
Use the existing test infrastructure and examples directory structure for validation.

## Session Notes: EK9 Annotation Work (2025-01-16)

### Task Completed
Added `@Ek9Method` annotations to FileSystemPath component at `ek9-lang/src/main/java/org/ek9/lang/FileSystemPath.java`.

**15 Methods Annotated:**
1. `withCurrentWorkingDirectory()` - Factory method returning current working directory
2. `withTemporaryDirectory()` - Factory method returning temporary directory
3. `startsWith(FileSystemPath)` - Path testing method
4. `endsWith(String)` - Path testing method (String variant)
5. `endsWith(FileSystemPath)` - Path testing method (FileSystemPath variant)
6. `exists()` - File system query method
7. `isFile()` - File system query method
8. `isDirectory()` - File system query method
9. `isWritable()` - File system query method
10. `isReadable()` - File system query method
11. `isExecutable()` - File system query method
12. `isAbsolute()` - File system query method
13. `createFile()` - File creation method (no parameters)
14. `createFile(Boolean)` - File creation method (with directory creation option)
15. `createDirectory()` - Directory creation method (no parameters)
16. `createDirectory(Boolean)` - Directory creation method (with parent creation option)
17. `absolutePath()` - Path transformation method

### EK9 Annotation Patterns Used
```java
@Ek9Method("""
    methodName() as pure
      -> param as ParamType
      <- rtn as ReturnType?""")
```

### Key Validation Test
**`Ek9IntrospectedBootStrapTest`** (`compiler-main/src/test/java/org/ek9lang/compiler/bootstrap/Ek9IntrospectedBootStrapTest.java`)
- Introspects Java classes with EK9 annotations
- Generates EK9 source code from annotations
- Parses generated code with EK9 parser
- Detects syntax errors with precise location (line/position)
- Example error: `EK9Comp : Syntax : 'Unknown' on line 965 position 24: extraneous input 'pureish' expecting {'pure', 'dispatcher', 'abstract', NL}`

### Multi-Module Build Challenge
**Issue**: Changes in `ek9-lang` module not reflected in `compiler-main` test
**Required Workflow**: 
1. `mvn package -pl ek9-lang` (rebuild and package the dependency)
2. `mvn clean compile -pl compiler-main` (clean and rebuild dependent module)
3. `mvn test -Dtest=Ek9IntrospectedBootStrapTest -pl compiler-main` (run validation test)

### Status
- All annotations added successfully
- Validation test demonstrates proper error detection
- Need to resolve Maven build dependency issue to complete verification

## Session Notes: EK9 GUID Implementation (2025-01-17)

### Task Completed
Implemented complete EK9 GUID component with Java UUID backing at `ek9-lang/src/main/java/org/ek9/lang/GUID.java`.

### Key GUID Implementation Insights

#### **GUID Interface Requirements** (from `Ek9BuiltinLangSupplier.java`)
```
GUID
  GUID() as pure                              // Default constructor
  GUID() as pure -> arg as GUID              // Copy constructor  
  GUID() as pure -> arg as String            // String constructor
  operator == as pure -> arg as GUID <- rtn as Boolean?
  operator <> as pure -> arg as GUID <- rtn as Boolean?
  operator <=> as pure -> arg as GUID <- rtn as Integer?
  operator <=> as pure -> arg as Any <- rtn as Integer?
  operator :^: -> arg as GUID                // Replace operator
  operator :=: -> arg as GUID                // Copy operator
  operator #^ as pure <- rtn as String?      // Promotion operator
  operator $ as pure <- rtn as String?       // String operator
  operator ? as pure <- rtn as Boolean?      // Set/unset check
```

#### **Critical EK9 Patterns Learned**

1. **Exception Handling**: Use `@SuppressWarnings("checkstyle:CatchParameterName")` and `_` for ignored exceptions:
   ```java
   try {
     state = UUID.fromString(arg.state);
   } catch (IllegalArgumentException _) {
     state = UUID.randomUUID();
   }
   ```

2. **EK9 Null Semantics**: Invalid arguments return unset values, not false:
   ```java
   public Boolean _eq(GUID arg) {
     if (isValid(arg)) {
       return Boolean._of(this.state.equals(arg.state));
     }
     return new Boolean(); // Unset, not Boolean._of(false)
   }
   ```

3. **Set/Unset Behavior**: Even "always-set" types can be unset for invalid operations:
   ```java
   public Boolean _isSet() {
     return Boolean._of(isSet); // Not always true
   }
   ```

4. **Factory Method Pattern**: Consistent `_of()` overloads:
   ```java
   public static GUID _of()                    // Generate new
   public static GUID _of(java.lang.String)   // From string
   public static GUID _of(UUID)               // From Java UUID
   ```

5. **Assignment Operators**: `:^:` (replace) and `:=:` (copy) follow String patterns:
   ```java
   public void _replace(GUID arg) {
     _copy(arg); // Replace delegates to copy
   }
   
   public void _copy(GUID value) {
     if (isValid(value)) {
       assign(value.state);
     } else {
       state = UUID.randomUUID(); // Fallback behavior
       set();
     }
   }
   ```

6. **Dual String Operators**: Both `#^` and `$` required for different compiler contexts:
   ```java
   public String _promote() { return String._of(state.toString()); }
   public String _string()  { return String._of(state.toString()); }
   ```

#### **Comprehensive Testing Patterns**

- **Null Safety**: Test all constructors and methods with null inputs
- **Edge Cases**: Invalid UUID strings, unset arguments, type mismatches
- **Round-trip Testing**: String conversion and reconstruction
- **Operator Consistency**: All operators behave correctly with set/unset states
- **assertDoesNotThrow**: Prefer over direct exception-throwing calls

#### **Key Implementation Files**
- **GUID.java**: Main implementation with 189 lines
- **GUIDTest.java**: Comprehensive test suite with 28 tests covering all scenarios
- **Integration**: Verified with `Ek9IntrospectedBootStrapTest`

#### **Lessons for Future EK9 Type Development**
1. **Always handle null inputs gracefully** - EK9 types must be robust
2. **Follow EK9 semantics** - Invalid operations return unset, not false
3. **Consistent factory patterns** - Use `_of()` overloads
4. **Proper exception handling** - Use `_` for ignored exceptions
5. **Comprehensive testing** - Cover all edge cases and null scenarios
6. **Integration validation** - Always run bootstrap tests

### Status
- GUID implementation complete and fully tested
- All 28 tests passing
- Bootstrap integration successful  
- Ready for production use in EK9 language

## Personal Preferences
- **Always refer to Steve by name** (not "user" or "the user")
- Steve prefers direct, concise communication
- Focus on practical implementation details and patterns