# EK9 Backend Implementation Architecture

This document defines the comprehensive architecture for implementing EK9 program entry points across both JVM and LLVM backends, establishing a unified approach to program execution, type conversion, and runtime infrastructure.

## Executive Summary

EK9 employs a **hybrid architecture** that combines minimal generated code with robust runtime utilities to create universal program entry points. This approach enables:

- **Single entry point per compilation unit** (`ek9.Main` for JVM, `main()` for LLVM)
- **Incremental build optimization** (check single artifact vs all source files)
- **Consistent user experience** across all backends
- **Type-safe command-line argument conversion** with comprehensive error handling
- **JAR packaging simplification** (`Main-Class: ek9.Main`)

## Core Architecture Principles

### 1. Universal Entry Point Strategy

**Problem Solved**: Instead of generating multiple entry points per program, create one universal dispatcher that:
- Contains a complete program registry from `PROGRAM_ENTRY_POINT_BLOCK` IR
- Handles program selection via `-r programName` command-line parsing
- Validates argument count and performs type-safe conversion
- Provides user-friendly error messages and help

**Command-Line Format**:
```bash
# JVM Backend
java ek9.Main -r "introduction1::HelloWorld"
java ek9.Main -r "introduction1::HelloMessage" "Hello there"

# LLVM Backend (future)
./main -r "introduction1::HelloWorld"
./main -r "introduction1::HelloMessage" "Hello there"
```

### 2. Hybrid Implementation Strategy

**Generated Code** (via ASM/LLVM):
- Minimal entry point class/function
- Program registry from IR metadata
- Individual program method implementations
- Program dispatch logic

**Pre-built Runtime Utilities** (hand-written, unit-tested):
- Command-line argument parsing
- Type conversion with set/unset validation
- Error handling and usage messages
- Program lookup and execution coordination

## JVM Backend Implementation

### Module Organization

**Location**: `ek9-lang` module
- **Rationale**: Runtime code that executes with user programs, not compiler infrastructure
- **Dependencies**: Needs EK9 built-in types (`org.ek9.lang.*`)
- **Packaging**: Already required in final executable JARs

**Package Structure**:
```
ek9-lang/
  src/main/java/
    org/ek9/lang/           # Core EK9 built-in types (existing)
      String.java, Integer.java, Boolean.java, etc.
    ek9/                    # New: Runtime infrastructure
      ProgramLauncher.java    # Command-line parsing and program execution
      TypeConverter.java      # String to EK9 type conversion with validation
      ProgramMetadata.java    # Program signature and metadata holder
      ArgumentParser.java     # -r flag and argument handling
```

### Generated ek9.Main Class

**ASM Generation Target**:
```java
package ek9;

import java.util.Map;
import java.util.HashMap;
import ek9.ProgramLauncher;
import ek9.ProgramMetadata;

public final class Main {
    // Generated from PROGRAM_ENTRY_POINT_BLOCK IR
    private static final Map<String, ProgramMetadata> REGISTRY = createRegistry();

    public static void main(String[] args) {
        // Delegate to pre-built runtime utilities
        ProgramLauncher.launch(REGISTRY, args);
    }

    // Generated program methods from OperationDfn IR
    public static void executeHelloWorld() {
        // IR translation: LOAD_LITERAL, CALL, STORE, RETURN
        org.ek9.lang.Stdout stdout = new org.ek9.lang.Stdout();
        stdout.println("Hello, World");
    }

    public static void executeHelloMessage(org.ek9.lang.String message) {
        // IR translation with parameter handling
        org.ek9.lang.Stdout stdout = new org.ek9.lang.Stdout();
        stdout.println(message);
    }

    // Generated registry builder from PROGRAM_ENTRY_POINT_BLOCK
    private static Map<String, ProgramMetadata> createRegistry() {
        Map<String, ProgramMetadata> registry = new HashMap<>();
        registry.put("introduction1::HelloWorld", new ProgramMetadata(
            "introduction1::HelloWorld",
            new String[0],  // No parameters
            "Main::executeHelloWorld"
        ));
        registry.put("introduction1::HelloMessage", new ProgramMetadata(
            "introduction1::HelloMessage",
            new String[]{"org.ek9.lang::String"},  // One String parameter
            "Main::executeHelloMessage"
        ));
        return registry;
    }
}
```

### Runtime Utilities Implementation

**ek9.ProgramLauncher**:
```java
package ek9;

import java.util.Map;

public final class ProgramLauncher {
    public static void launch(Map<String, ProgramMetadata> registry, String[] args) {
        try {
            // Parse -r programName
            if (args.length < 2 || !"-r".equals(args[0])) {
                printUsageAndExit(registry);
            }

            String programName = args[1];
            String[] userArgs = Arrays.copyOfRange(args, 2, args.length);

            // Look up program
            ProgramMetadata program = registry.get(programName);
            if (program == null) {
                printProgramNotFound(programName, registry);
                System.exit(1);
            }

            // Validate and convert arguments
            Object[] convertedArgs = convertArguments(userArgs, program);

            // Execute program via reflection or direct method call
            executeProgram(program, convertedArgs);

        } catch (Exception e) {
            System.err.println("Execution failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printUsageAndExit(Map<String, ProgramMetadata> registry) {
        System.err.println("Usage: java ek9.Main -r <program-name> [arguments...]");
        System.err.println("\nAvailable programs:");
        registry.values().forEach(program ->
            System.err.println("  " + program.getSignature())
        );
        System.exit(2);
    }
}
```

**ek9.TypeConverter**:
```java
package ek9;

public final class TypeConverter {
    public static Object convertToEK9Type(String userValue, String ek9TypeName) {
        switch (ek9TypeName) {
            case "org.ek9.lang::String":
                return org.ek9.lang.String._of(userValue);

            case "org.ek9.lang::Integer":
                org.ek9.lang.Integer result = org.ek9.lang.Integer._of(userValue);
                if (!result._isSet()) {
                    throw new TypeConversionException("Invalid integer: '" + userValue + "'");
                }
                return result;

            case "org.ek9.lang::Dimension":
                org.ek9.lang.Dimension dim = org.ek9.lang.Dimension._of(userValue);
                if (!dim._isSet()) {
                    throw new TypeConversionException(
                        "Invalid dimension: '" + userValue + "'. Expected format like '61m', '10ft', etc."
                    );
                }
                return dim;

            // Additional type conversions...
            default:
                throw new TypeConversionException("Unsupported type: " + ek9TypeName);
        }
    }
}
```

## LLVM Backend Implementation (Future)

### Module Organization

**Location**: EK9 C++ Runtime Library (libek9.a/libek9.so)
- **Rationale**: Parallel to Java ek9-lang module
- **Dependencies**: EK9 C++ built-in types implementation
- **Packaging**: Statically linked with generated executable

**Directory Structure**:
```
ek9-lang-cpp/
  include/
    org/ek9/lang/           # Core EK9 built-in types
      String.h, Integer.h, Boolean.h, etc.
    ek9/                    # Runtime infrastructure headers
      ProgramLauncher.h
      TypeConverter.h
      ProgramMetadata.h
  src/
    ek9/                    # Runtime implementation
      ProgramLauncher.cpp
      TypeConverter.cpp
      ProgramMetadata.cpp
```

### Generated main.cpp

**LLVM IR to C++ Generation Target**:
```cpp
#include "ek9/ProgramLauncher.h"
#include "ek9/ProgramMetadata.h"
#include "org/ek9/lang/String.h"
#include "org/ek9/lang/Stdout.h"

// Generated program registry from PROGRAM_ENTRY_POINT_BLOCK
static ek9::ProgramRegistry createRegistry() {
    ek9::ProgramRegistry registry;
    registry.add("introduction1::HelloWorld",
                 ek9::ProgramMetadata("introduction1::HelloWorld", {}, executeHelloWorld));
    registry.add("introduction1::HelloMessage",
                 ek9::ProgramMetadata("introduction1::HelloMessage", {"org.ek9.lang::String"},
                                      executeHelloMessage));
    return registry;
}

// Generated program functions from OperationDfn IR
void executeHelloWorld() {
    // IR translation to C++ with ARC
    auto stdout = ek9::make_shared<org::ek9::lang::Stdout>();
    auto message = ek9::make_shared<org::ek9::lang::String>("Hello, World");
    stdout->println(message);
}

void executeHelloMessage(ek9::shared_ptr<org::ek9::lang::String> message) {
    // IR translation with parameter handling
    auto stdout = ek9::make_shared<org::ek9::lang::Stdout>();
    stdout->println(message);
}

int main(int argc, char** argv) {
    // Initialize EK9 runtime system
    ek9::runtime_init();

    // Create program registry
    auto registry = createRegistry();

    // Delegate to C++ runtime utilities
    return ek9::ProgramLauncher::launch(registry, argc, argv);
}
```

### C++ Runtime Utilities

**ek9::ProgramLauncher**:
```cpp
namespace ek9 {
    class ProgramLauncher {
    public:
        static int launch(const ProgramRegistry& registry, int argc, char** argv) {
            try {
                // Parse -r programName
                if (argc < 3 || std::string(argv[1]) != "-r") {
                    printUsageAndExit(registry);
                }

                std::string programName = argv[2];
                std::vector<std::string> userArgs(argv + 3, argv + argc);

                // Look up program
                auto program = registry.find(programName);
                if (!program) {
                    printProgramNotFound(programName, registry);
                    return 1;
                }

                // Convert arguments and execute
                auto convertedArgs = convertArguments(userArgs, program->getSignature());
                program->execute(convertedArgs);

                return 0;
            } catch (const std::exception& e) {
                std::cerr << "Execution failed: " << e.what() << std::endl;
                return 1;
            }
        }
    };
}
```

**ek9::TypeConverter**:
```cpp
namespace ek9 {
    class TypeConverter {
    public:
        static ek9::shared_ptr<org::ek9::lang::String> convertToString(const char* cstr) {
            return org::ek9::lang::String::_of(std::string(cstr));
        }

        static ek9::shared_ptr<org::ek9::lang::Integer> convertToInteger(const char* cstr) {
            auto result = org::ek9::lang::Integer::_of(std::string(cstr));
            if (!result->_isSet()) {
                throw TypeConversionException("Invalid integer: '" + std::string(cstr) + "'");
            }
            return result;
        }

        static ek9::shared_ptr<org::ek9::lang::Dimension> convertToDimension(const char* cstr) {
            auto result = org::ek9::lang::Dimension::_of(std::string(cstr));
            if (!result->_isSet()) {
                throw TypeConversionException(
                    "Invalid dimension: '" + std::string(cstr) +
                    "'. Expected format like '61m', '10ft', etc."
                );
            }
            return result;
        }
    };
}
```

## Implementation Strategy

### Phase 1: JVM Backend Foundation
1. **Create runtime utilities in ek9-lang**:
   - `ek9.ProgramLauncher` with command-line parsing
   - `ek9.TypeConverter` with comprehensive type conversion
   - `ek9.ProgramMetadata` for program signature storage
   - Complete unit test suite

2. **Implement ASM generation in compiler-main**:
   - Modify `AsmStructureCreator.processProgram()`
   - Generate `ek9.Main` class with program registry
   - Process `PROGRAM_ENTRY_POINT_BLOCK` deduplication
   - Translate `OperationDfn` IR to program methods

### Phase 2: Testing and Validation
3. **Test with hello world examples**:
   - `java ek9.Main -r "introduction1::HelloWorld"` → "Hello, World"
   - `java ek9.Main -r "introduction1::HelloMessage" "Test"` → "Test"
   - Error cases: wrong program, argument count, type conversion failures

### Phase 3: LLVM Backend Foundation (Future)
4. **Implement C++ runtime utilities**:
   - Parallel implementation of ProgramLauncher, TypeConverter
   - Integration with EK9 C++ standard library
   - ARC memory management integration

## Benefits of This Architecture

### 1. Incremental Build Optimization
- **Single artifact check**: Compare `ek9.Main.class` timestamp vs all `.ek9` source files
- **Skip compilation**: If `Main.class` newer, directly execute `java ek9.Main -r ...`
- **Build system integration**: Simplified dependency tracking

### 2. Deployment Simplification
- **Consistent JAR packaging**: Always `Main-Class: ek9.Main`
- **Docker containers**: Predictable entry point
- **CI/CD pipelines**: Standard execution format
- **Enterprise deployment**: Single artifact model

### 3. Cross-Backend Consistency
- **Identical user experience**: Same command-line format, error messages
- **Shared testing**: Same test cases validate both backends
- **Documentation**: Single set of user documentation
- **Training**: Developers learn one execution model

### 4. Maintainability
- **Unit testable**: Complex logic in standard Java/C++ classes
- **Debuggable**: Runtime utilities debuggable with standard tools
- **Minimal generated code**: Only program-specific logic generated
- **Clear separation**: Generated vs hand-written code boundaries

## Future Enhancements

### Application Integration
The architecture supports future Application/DI features through:
- **Extended ProgramMetadata**: Include Application references
- **Two-phase execution**: Construction → DI configuration → execution
- **Enhanced TypeConverter**: Factory method integration with DI container

### Multi-Module Projects
- **Cross-module program discovery**: Registry spans all modules in compilation
- **Namespace management**: Fully qualified names prevent conflicts
- **Dependency tracking**: Enhanced build optimization across modules

### Native Packaging
- **GraalVM integration**: Direct native compilation of ek9.Main
- **Static linking**: Embed runtime utilities in native executable
- **Platform-specific optimization**: Leverage native compilation benefits

## Conclusion

This hybrid architecture provides a robust foundation for EK9 program execution across multiple backends while maintaining simplicity, testability, and consistent user experience. The separation of generated program-specific code from reusable runtime utilities enables comprehensive testing and maintainable implementation.

The single entry point model (`ek9.Main`) with universal program registry enables powerful build optimization and deployment simplification that scales from simple hello world programs to complex enterprise applications.