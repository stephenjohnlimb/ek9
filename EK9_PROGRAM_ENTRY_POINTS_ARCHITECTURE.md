# EK9 Program Entry Points Architecture

This document describes the comprehensive architecture for implementing EK9 program entry points, including type-safe command-line argument handling and multi-backend IR generation.

## Overview

EK9 programs provide a revolutionary type-safe command-line argument system that eliminates traditional `getopt` complexity through declarative parameter specifications. This system works seamlessly across both JVM and native (LLVM/C++) backends.

## Architecture Components

### 1. EK9 CLI Program Discovery and Selection

The `ek9` command-line tool serves as the universal entry point for all EK9 programs:

```bash
# Single program - automatic selection
./helloworld.ek9

# Multiple programs - explicit selection
./tcp.ek9 -r TCPServer2 4445 4446 SHUTDOWN
./tcp.ek9 -r TCPClient2 4445 4446
```

**EK9 CLI Responsibilities:**
- Read and parse `.ek9` files to discover all available programs
- Select target program (first program if no `-r` flag, specified program with `-r`)
- Launch appropriate runtime (JVM bytecode or native binary)
- Pass program selection and arguments to generated entry point

### 2. Argument Passing Convention

The EK9 CLI establishes a standardized argument passing convention:

```
argv[0] = fully-qualified-program-name  // "example.networking::TCPServer2"
argv[1..n] = user-supplied-arguments    // ["4445", "4446", "SHUTDOWN"]
```

This enables the generated program to:
1. Identify which specific program to execute via `argv[0]`
2. Access raw user arguments for type conversion via `argv[1..n]`

### 3. Type-Safe Parameter Declaration

EK9 programs declare parameters declaratively with strong typing:

```ek9
TCPServer2()
  -> processingPort as Integer      # "4445" → Integer(4445)
     controlPort as Integer         # "4446" → Integer(4446)
     shutdownCommand as String      # "SHUTDOWN" → String("SHUTDOWN")

DataCorrelation()
  -> argv as List of String         # Legacy manual parsing approach
```

**Allowed Parameter Types** (enforced by `ProgramArgumentPredicate`):
- **Basic**: String, Integer, Float, Boolean, Character, Bits
- **Temporal**: Date, Time, DateTime, Duration, Millisecond
- **Specialized**: Money, Colour, Resolution, Dimension, Regex
- **Collection**: List of String (only allowed parameterized type)

### 4. High-Level IR Design: PROGRAM_ENTRY_POINT_BLOCK

Following EK9's IR philosophy of rich semantic information with backend implementation flexibility, programs generate a high-level IR construct:

```
PROGRAM_ENTRY_POINT_BLOCK [
  available_programs: [
    program_definition: {
      qualified_name: "example.networking::TCPServer2"
      module_name: "example.networking"
      simple_name: "TCPServer2"
      parameter_signature: [
        { name: "processingPort", type: "org.ek9.lang::Integer", position: 0 },
        { name: "controlPort", type: "org.ek9.lang::Integer", position: 1 },
        { name: "shutdownCommand", type: "org.ek9.lang::String", position: 2 }
      ]
    },
    program_definition: {
      qualified_name: "example.networking::TCPClient2"
      module_name: "example.networking"
      simple_name: "TCPClient2"
      parameter_signature: [
        { name: "processingPort", type: "org.ek9.lang::Integer", position: 0 },
        { name: "controlPort", type: "org.ek9.lang::Integer", position: 1 }
      ]
    }
    // ... all other programs discovered in CompilableProgram
  ]
  default_program: "example.networking::TCPServer2"  # First program found
]
```

**Key Design Principles:**
- **Complete Program Discovery**: Search entire `CompilableProgram` across all modules at compile time
- **Self-Contained Semantics**: All information needed for execution without symbol references
- **Backend Agnostic**: High-level IR works identically for JVM and native targets
- **Type Safety**: Parameter types validated against allowed set during compilation

## Implementation Architecture

### Critical Architectural Insights

**Backend Neutrality Requirement**: The current IR generation in `simpleProgram.ek9` incorrectly includes Java-specific syntax (`[Ljava.lang.String;`) which violates EK9's backend-neutral IR principle. Our implementation must maintain pure EK9 semantics.

**Two-Phase Execution Discovery**: Analysis of EK9's Application pattern reveals programs require:
1. **No-args constructor** for dependency injection
2. **Application configuration** for service wiring
3. **Argument conversion** and `_call()` execution

### ProgramDfnGenerator Enhancement Strategy

```java
final class ProgramDfnGenerator extends AbstractDfnGenerator {

  private void createProgramEntryPoint(final IRConstruct construct,
                                      final CompilableProgram program) {
    // 1. Discover ALL programs across ALL modules
    final var allPrograms = discoverAllPrograms(program);

    // 2. Determine file-specific default program
    final var defaultProgram = determineFileDefaultProgram(construct, allPrograms);

    // 3. Build backend-neutral semantic information
    final var programEntryPoint = new ProgramEntryPointInstr(
        allPrograms,
        defaultProgram,
        debugInfo
    );

    // 4. Add to IR - backends handle target-specific implementation
    construct.add(programEntryPoint);
  }

  private List<ProgramDefinition> discoverAllPrograms(final CompilableProgram program) {
    return program.getAllModules()
        .stream()
        .flatMap(module -> module.getRecordedSymbols().stream())
        .filter(symbol -> symbol.getGenus() == SymbolGenus.PROGRAM)
        .map(this::buildProgramDefinition)
        .collect(Collectors.toList());
  }

  private String determineFileDefaultProgram(final IRConstruct construct,
                                           final List<ProgramDefinition> allPrograms) {
    // Find first program defined in this source file
    return allPrograms.stream()
        .filter(program -> program.isDefinedInSource(construct.getSource()))
        .findFirst()
        .map(ProgramDefinition::getQualifiedName)
        .orElse(allPrograms.get(0).getQualifiedName());
  }
}
```

### Backend Implementation Responsibilities

#### Java Backend (JVM Bytecode)

**Two-Phase Implementation** with Application integration:

```java
public static void main(String[] args) {
    // Extract program selection
    String programName = args[0]; // "example.networking::TCPServer2"
    String[] userArgs = Arrays.copyOfRange(args, 1, args.length);

    // Look up program in compiled registry (from IR)
    ProgramDefinition program = programRegistry.get(programName);
    if (program == null) {
        System.err.println("Program '" + programName + "' not found.");
        System.err.println("Available programs: " + String.join(", ", programRegistry.keySet()));
        System.exit(1);
    }

    // Validate argument count
    if (userArgs.length != program.parameterSignature.size()) {
        System.err.println("Expected " + program.parameterSignature.size() +
                          " arguments, got " + userArgs.length);
        printUsage(program);
        System.exit(1);
    }

    try {
        // PHASE 1: Instantiation & Application Configuration
        Object instance = createProgramInstance(program.qualified_name);  // No-args constructor

        if (program.hasApplication()) {
            Object application = createApplicationInstance(program.applicationName);
            configureApplicationDependencies(application, instance);  // Dependency injection
        }

        // PHASE 2: Argument Conversion & Execution
        Object[] convertedArgs = new Object[userArgs.length];
        for (int i = 0; i < userArgs.length; i++) {
            ParameterInfo param = program.parameterSignature.get(i);
            convertedArgs[i] = convertToEK9Type(userArgs[i], param.type);
        }

        // Call _call method with converted arguments
        invokeProgramCall(instance, convertedArgs);

    } catch (ConversionException e) {
        System.err.println("Argument conversion failed: " + e.getMessage());
        printUsage(program);
        System.exit(1);
    }
}

private static Object convertToEK9Type(String value, String typeName) {
    switch (typeName) {
        case "org.ek9.lang::Integer": return Integer._of(value);
        case "org.ek9.lang::String": return String._of(value);
        case "org.ek9.lang::Boolean": return Boolean._of(value);
        case "org.ek9.lang::Float": return Float._of(value);
        case "org.ek9.lang::List<String>": return convertToEK9List(value);
        // ... all 17 allowed types
        default: throw new ConversionException("Unsupported type: " + typeName);
    }
}

private static void configureApplicationDependencies(Object application, Object program) {
    // Use reflection or generated code to wire Application dependencies
    // Example: application.configure(program) calls register() methods
    Method configureMethod = application.getClass().getMethod("configure", program.getClass());
    configureMethod.invoke(application, program);
}
```

#### C++ Native Backend (LLVM)

Generates `main(int argc, char* argv[])` function with:

```cpp
int main(int argc, char* argv[]) {
    // Initialize EK9 runtime
    ek9_runtime_init();

    try {
        // Extract program selection
        std::string programName = argv[1]; // "example.networking::TCPServer2"

        // Look up program in compiled registry
        auto program = programRegistry.find(programName);
        if (program == programRegistry.end()) {
            std::cerr << "Program '" << programName << "' not found." << std::endl;
            printAvailablePrograms();
            return 1;
        }

        // Validate argument count
        int expectedArgs = program->second.parameterSignature.size();
        int providedArgs = argc - 2; // Skip program name and our program name

        if (providedArgs != expectedArgs) {
            std::cerr << "Expected " << expectedArgs << " arguments, got " << providedArgs << std::endl;
            printUsage(program->second);
            return 1;
        }

        // Type-safe conversion with ARC management
        std::vector<ek9_ptr<BuiltinType>> convertedArgs;
        for (int i = 0; i < expectedArgs; i++) {
            auto param = program->second.parameterSignature[i];
            auto converted = convertToEK9Type(argv[i + 2], param.type);
            convertedArgs.push_back(converted);
        }

        // Create and call EK9 program
        auto instance = createProgramInstance(programName);
        invokeProgramCall(instance, convertedArgs);

    } catch (const ConversionException& e) {
        std::cerr << "Argument conversion failed: " << e.what() << std::endl;
        return 1;
    }

    // Cleanup EK9 runtime
    ek9_runtime_cleanup();
    return 0;
}

ek9_ptr<BuiltinType> convertToEK9Type(const char* value, const std::string& typeName) {
    if (typeName == "org.ek9.lang::Integer") return Integer::_of(value);
    if (typeName == "org.ek9.lang::String") return String::_of(value);
    if (typeName == "org.ek9.lang::Boolean") return Boolean::_of(value);
    if (typeName == "org.ek9.lang::Float") return Float::_of(value);
    // ... all 17 allowed types
    throw ConversionException("Unsupported type: " + typeName);
}
```

## Error Handling and User Experience

### Comprehensive Error Messages

The system provides clear, actionable error messages:

```
# Program not found
Program 'TCPServer3' not found.
Available programs: TCPServer1, TCPServer2, TCPClient1, TCPClient2

# Wrong argument count
TCPServer2 expects 3 arguments (processingPort:Integer, controlPort:Integer, shutdownCommand:String), got 2
Usage: ./tcp.ek9 -r TCPServer2 <processingPort> <controlPort> <shutdownCommand>

# Type conversion failure
Cannot convert '999999999999999999999' to Integer: value out of range
Usage: ./tcp.ek9 -r TCPServer2 <processingPort> <controlPort> <shutdownCommand>

# Application configuration failure
Failed to configure Application 'DemoApp' for program 'Demonstration': missing dependency HRSystem
Check Application definition in source file
```

### Auto-Generated Usage Help

The IR information enables automatic usage help generation:

```java
private static void printUsage(ProgramDefinition program) {
    System.err.println("Usage: " + program.simple_name +
                      program.parameterSignature.stream()
                          .map(p -> " <" + p.name + ":" + simplifyTypeName(p.type) + ">")
                          .collect(Collectors.joining()));
}
```

## Benefits Achieved

### 1. Type-Safe getOpt Replacement

Traditional C/Java argument parsing:
```c
// Traditional approach - error-prone boilerplate
int processingPort = atoi(argv[1]);  // No validation
int controlPort = atoi(argv[2]);     // No error handling
char* shutdownCmd = argv[3];         // Manual type juggling
```

EK9 declarative approach:
```ek9
TCPServer2()
  -> processingPort as Integer    # Automatic conversion + validation
     controlPort as Integer       # Type safety guaranteed
     shutdownCommand as String    # Zero boilerplate
```

### 2. Compile-Time Safety

- **Parameter types validated** against 17 allowed types during compilation
- **Program signatures captured** in IR for runtime dispatch
- **Impossible states eliminated** through type system constraints

### 3. Developer Experience

- **Zero argument parsing code** - developers focus on business logic
- **Declarative parameter specification** - intent clearly expressed
- **Automatic error handling** - consistent user experience across all programs
- **IDE support** - parameter types provide completion and validation

### 4. Multi-Backend Consistency

- **Same declarative syntax** works for JVM and native compilation
- **Identical runtime behavior** across platforms
- **Shared error handling** and usage help generation
- **Performance optimization** opportunities in each backend

## Testing and Validation

### IR Generation Test Pattern

**Corrected Backend-Neutral IR** (removing Java-specific artifacts):

```ek9
@IR: IR_GENERATION: PROGRAM_ENTRY_POINT: `
PROGRAM_ENTRY_POINT_BLOCK [
  available_programs: [
    {
      qualified_name: "test::SimpleProgram",
      module_name: "test",
      simple_name: "SimpleProgram",
      parameter_signature: [
        { name: "port", type: "org.ek9.lang::Integer", position: 0 }
      ],
      has_application: false
    }
  ],
  default_program: "test::SimpleProgram"
]`

@IR: IR_GENERATION: FUNCTION: "test::SimpleProgram": `
ConstructDfn: test::SimpleProgram()->org.ek9.lang::Void  // No-args constructor
OperationDfn: test::SimpleProgram._call(org.ek9.lang::Integer)->org.ek9.lang::Void
BasicBlock: _entry_1
REFERENCE port, org.ek9.lang::Integer
// ... program implementation logic
RETURN`
```

**Program with Application Example:**

```ek9
@IR: IR_GENERATION: PROGRAM_ENTRY_POINT: `
PROGRAM_ENTRY_POINT_BLOCK [
  available_programs: [
    {
      qualified_name: "com.customer.just.employees2::Demonstration",
      module_name: "com.customer.just.employees2",
      simple_name: "Demonstration",
      parameter_signature: [],  // No command-line parameters
      has_application: true,
      application_name: "com.customer.just.employees2::DemoApp"
    }
  ],
  default_program: "com.customer.just.employees2::Demonstration"
]`
```

### End-to-End Testing

1. **Compilation Testing**: Validate IR generation for various program signatures
2. **Runtime Testing**: Test argument conversion and error handling
3. **Cross-Platform Testing**: Ensure consistent behavior across JVM and native
4. **Performance Testing**: Validate startup time and conversion performance

## Strategic Impact

This architecture establishes EK9 as providing **dependency injection for command-line arguments**, eliminating one of the most error-prone and tedious aspects of system programming while maintaining complete type safety and performance.

The approach scales from simple single-program utilities to complex multi-program applications (like the networking examples with 4 different programs), all with zero boilerplate and maximum developer productivity.

## Future Extensions

### Enhanced Validation

- **Range validation**: `port as Integer[1024..65535]`
- **Pattern validation**: `email as String[/.*@.*/]`
- **File existence**: `config as FileSystemPath[exists]`

### Documentation Generation

- **Auto-generated man pages** from program signatures
- **Shell completion scripts** based on parameter types
- **API documentation** for program interfaces

### Configuration Integration

- **Environment variable binding**: Parameters sourced from env vars
- **Configuration file integration**: YAML/JSON parameter binding
- **Default value specification**: Fallback values for optional parameters

## Key Architectural Corrections and Insights

### Backend Neutrality Enforcement
The implementation **must avoid** Java-specific syntax in IR generation. Current examples showing `[Ljava.lang.String;` violate EK9's backend-neutral principle and prevent LLVM/native compilation.

### Application Integration Architecture
EK9's dependency injection pattern via Applications requires:
- **No-args program constructors** for clean instantiation
- **Application configuration phase** before argument processing
- **Two-phase execution**: Construction → DI Configuration → Argument Processing → Execution

### File-Tied IR Benefits
Each `.ek9` file containing programs generates its own `PROGRAM_ENTRY_POINT_BLOCK` with:
- **Complete program registry** (cross-CompilableProgram visibility)
- **File-specific default program** (natural execution model)
- **Backend deployment flexibility** (single-jar vs per-program executables)

### Enhanced Type Safety
The architecture provides **compile-time validation** of program parameters against 17 allowed types, **automatic type conversion** using EK9 factory methods, and **rich error reporting** with usage help generation.

This architecture positions EK9's command-line interface capabilities as significantly more advanced than traditional approaches while maintaining the language's core principles of safety, performance, and developer productivity. The integration with EK9's Application pattern provides **dependency injection for command-line programs**, a capability that doesn't exist in other systems programming languages.