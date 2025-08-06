# EK9 IR and Code Generation Guide

This document provides comprehensive guidance for working with EK9's Intermediate Representation (IR) and code generation to various targets. This is the specialized reference for compiler backend development, optimization passes, and target-specific code generation.

**Related Documentation:**
- **`CLAUDE.md`** - Main project overview and daily development guidelines
- **`EK9_COMPILER_PHASES.md`** - Detailed compiler phase implementation and pipeline
- **`EK9_Compiler_Architecture_and_Design.md`** - Complete architectural specification

## IR Generation Overview

EK9's IR generation transforms resolved symbols from compilation phases 1-6 into a target-agnostic intermediate representation that can be translated to multiple backends (JVM bytecode, LLVM IR, etc.).

### IR Design Principles
- **Target-agnostic**: IR must work equally well for JVM, LLVM-Go, and LLVM-C++ targets
- **Symbol-driven**: Uses resolved symbols from `ParsedModule.getRecordedSymbol()` instead of AST text parsing
- **Text-based representation**: Stores all values as strings for serialization and backend flexibility
- **Fully qualified type names**: All types use complete qualified names to avoid ambiguity

### Symbol Table to IR Transformation
The IR generation process in phase 7 transforms resolved symbols:
1. **Context Creation**: `IRGenerationContext` provides centralized state management
2. **Symbol Resolution**: Use `parsedModule.getRecordedSymbol(ctx)` for all AST nodes
3. **Type Information**: Extract fully qualified type names using `symbol.getType().getFullyQualifiedName()`
4. **Value Extraction**: Get literal values and identifiers from resolved symbols

## IR Literal Value Representation

### Design Decision: Text-Based IR with Careful Encoding

**Chosen Approach**: Pure text-based IR using Java Strings for all literal storage.

**Alternative Considered**: ISymbol-based IR storing rich symbol objects.
- **Rejected because**: Couples IR to compiler symbol table, not serializable, complex for code generation backends.

### Literal Value Encoding Rules

All literal values are stored as Java Strings that preserve their original semantic representation:

| EK9 Source | Internal Storage (Java String) | IR Output |
|------------|-------------------------------|-----------|
| `2` | `"2"` | `LOAD_LITERAL 2 (org.ek9lang.lang.Integer)` |
| `2024-12-25` | `"2024-12-25"` | `LOAD_LITERAL 2024-12-25 (org.ek9lang.lang.Date)` |
| `"Hello, World"` | `"\"Hello, World\""` | `LOAD_LITERAL "Hello, World" (org.ek9lang.lang.String)` |
| `true` | `"true"` | `LOAD_LITERAL true (org.ek9lang.lang.Boolean)` |

**Critical Rule**: String literals retain their quotes as part of the stored value to distinguish them from other literal types.

### Type Name Strategy

All types use fully qualified names to avoid ambiguity across different contexts:
- `String` → `org.ek9lang.lang.String`
- `Integer` → `org.ek9lang.lang.Integer`  
- `List of String` → `_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1`

### Code Generation Implications

This encoding strategy enables backends to correctly interpret literal values:

**JVM Bytecode Generation**:
- `2` → `bipush 2` or `ldc 2`
- `"Hello, World"` → `ldc "Hello, World"`
- Can distinguish numeric vs string constants

**LLVM IR Generation**:  
- `2` → `i32 2`
- `"Hello, World"` → `i8* getelementptr (...)`
- Type-aware constant generation

**Parsing Strategy**: Backends parse the stored literal values based on the fully qualified type information provided in the IR instruction.

## Code Generation Targets

### Java Bytecode Generation
*This section will contain:*
- Java bytecode generation patterns
- EK9 to Java type mapping strategies
- Optimization techniques for Java target
- Integration with Java ecosystem

### Future Target Support
*This section will contain:*
- LLVM IR generation planning
- Native compilation strategies
- Cross-platform considerations
- Performance optimization approaches

## Optimization Passes

*This section will contain:*
- IR optimization strategies
- Dead code elimination
- Constant folding and propagation
- Loop optimization techniques
- Inlining strategies

## Target-Specific Considerations

### Java Target Specifics
*This section will contain:*
- Java interoperability requirements
- JVM limitations and workarounds
- Performance characteristics
- Debugging and profiling integration

### Native Target Planning
*This section will contain:*
- Native code generation requirements
- Memory management strategies
- System integration considerations
- Performance optimization opportunities

## IR Debugging and Analysis

### Debug Information Integration

EK9's IR system includes comprehensive debug information support for source mapping and debugging:

**DebugInfo Record Structure:**
```java
public record DebugInfo(
    String sourceFile,      // Relative .ek9 file path  
    int lineNumber,         // 1-based line number
    int columnNumber,       // 1-based column number
    String originalText     // Original EK9 source text (optional)
)
```

**Key Design Decisions:**
- **Relative Paths**: Uses `CompilableSource.getRelativeFileName()` for portability
- **Symbol-Driven**: Extracts location from `ISymbol.getSourceToken()`
- **Optional Integration**: Controlled by `CompilerFlags.isDebuggingInstrumentation()`
- **IR Comment Format**: `// workarea.ek9:12:15 'original text'`

### Debug Information Architecture

**Encapsulation Pattern:**
```java
// DebugInfoCreator encapsulates debug information generation logic
final class DebugInfoCreator implements Function<ISymbol, DebugInfo> {
  public DebugInfo apply(final ISymbol symbol) {
    return context.getCompilerFlags().isDebuggingInstrumentation()
        ? DebugInfo.from(context.getParsedModule().getSource(), symbol) : null;
  }
}

// Used consistently across all IR instruction creators
private final DebugInfoCreator debugInfoCreator;
final var debugInfo = debugInfoCreator.apply(symbol);
```

**IR Integration:**
- All `IRInstruction` subclasses support optional `DebugInfo`
- Debug information appears as IR comments: `CALL method() // ./file.ek9:10:5`
- Enables source mapping for target code generation (JVM, LLVM, etc.)

### Testing and Verification

**Debug Output Verification:**
- WorkingAreaTest demonstrates debug instrumentation with `-Dek9.instructionInstrumentation=true`
- Relative path formatting: `./workarea.ek9:8:17` (not absolute paths)
- Method call tracking: `CALL _temp6.println(toOutput) // ./workarea.ek9:13:14`
- Constructor call tracking: `CALL org.ek9.lang::Stdout.<init>() // ./workarea.ek9:8:17`

## Implementation Patterns

*This section will contain:*
- Common IR transformation patterns
- Error handling in code generation
- Resource management strategies
- Testing and validation approaches

---

**Note**: This is a placeholder file created to organize future IR and code generation knowledge. Content will be added as Steve and I work on compiler backend development, moving away from Java implementation details to focus on EK9's intermediate representation and multi-target code generation capabilities.