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

## IR Generation Patterns

### Function-to-Class Transformation Pattern

EK9 functions are transformed into class-like constructs with synthetic `_call` methods, following the "Everything as Object" design principle:

**EK9 Source:**
```ek9
checkAssert()
  -> arg0 as Boolean
  assert arg0
```

**Generated IR Structure:**
```
ConstructDfn: justAssert::checkAssert
OperationDfn: justAssert::checkAssert._call()->org.ek9.lang::Void
BasicBlock: _entry_1
IRInstruction: REFERENCE arg0, org.ek9.lang::Boolean  // Parameter declaration
IRInstruction: SCOPE_ENTER _scope_1  // Function body scope
IRInstruction: _temp1 = LOAD arg0  // Load parameter value
IRInstruction: _temp2 = CALL (org.ek9.lang::Boolean)_temp1._true()  // Boolean conversion
IRInstruction: ASSERT _temp2  // Primitive boolean assertion
IRInstruction: SCOPE_EXIT _scope_1
IRInstruction: RETURN
```

**Key Transformation Components:**
- **FunctionDfnGenerator**: Creates function-as-class constructs with synthetic `_call` methods
- **OperationDfnGenerator**: Handles function body processing with proper scope management
- **Synthetic Method Pattern**: Functions become classes with `_call()` methods for uniform invocation

### Assert Statement Processing Pattern

EK9 assert statements convert Boolean expressions to primitive booleans before assertion:

**Processing Steps:**
1. **Expression Evaluation**: Generate IR to evaluate the assert expression to a temporary variable
2. **Boolean Conversion**: Call `_true()` method on the Boolean object to get primitive boolean value
3. **Assertion**: Use ASSERT IR instruction with the primitive boolean result

**AssertStmtGenerator Implementation:**
```java
// Evaluate the assert expression
final var rhsExprResult = context.generateTempName();
final var instructions = new ArrayList<>(expressionGenerator.apply(ctx.expression(), rhsExprResult, scopeId));

// Call the _true() method to get primitive boolean
final var rhsResult = context.generateTempName();
final var callDetails = new CallDetails(rhsExprResult, booleanTypeName, "_true", 
    List.of(), "boolean", List.of());
instructions.add(CallInstr.call(rhsResult, debugInfo, callDetails));

// Assert on the primitive boolean result
instructions.add(BranchInstr.assertValue(rhsResult, debugInfo));
```

### Memory Management and Scope Ownership Rules

EK9's IR generation includes sophisticated memory ownership tracking through scope registration:

**ShouldRegisterVariableInScope Logic:**
- **Parameters** (`_param_*` scopes): **FALSE** - caller-managed memory, no SCOPE_REGISTER
- **Return variables** (`_return_*` scopes): **FALSE** - ownership transferred to caller
- **Local variables** (`_scope_*` scopes): **TRUE** - function-managed memory, needs SCOPE_REGISTER

**Parameter Handling Pattern:**
```java
// Parameters get REFERENCE declaration only - no scope registration
instructions.add(MemoryInstr.reference(paramName, paramType, debugInfo));
// NO SCOPE_REGISTER for parameters - caller owns the memory

// Local variables get both REFERENCE and SCOPE_REGISTER
instructions.add(MemoryInstr.reference(localName, localType, debugInfo));
instructions.add(ScopeInstr.register(localName, scopeId, debugInfo));
```

**Critical Memory Ownership Rule:** Function parameters are caller-owned and should NOT be registered in function scope for cleanup. This prevents the function from attempting to manage memory it doesn't own.

### Variable Declaration Processing Pattern

**AbstractVariableDeclGenerator** provides common processing for variable declarations:

**Variable-Only Declarations** (parameters, uninitialized variables):
```
REFERENCE variableName, typeName
SCOPE_REGISTER variableName, scopeId  // Only if shouldRegisterVariableInScope returns true
```

**Variable Declarations with Initialization** (local variables with values):
```
REFERENCE variableName, typeName
SCOPE_REGISTER variableName, scopeId
_temp1 = [initialization expression IR]
RETAIN _temp1
SCOPE_REGISTER _temp1, scopeId
STORE variableName, _temp1
RETAIN variableName
```

### Scope Management Pattern

EK9 uses hierarchical scope management for memory cleanup:

**Function Scope Structure:**
1. **Parameter Scope** (`_param_*`): No automatic cleanup - caller-managed
2. **Return Scope** (`_return_*`): No automatic cleanup - transferred to caller
3. **Function Body Scope** (`_scope_*`): Automatic cleanup with SCOPE_EXIT

**Scope Lifecycle:**
```
SCOPE_ENTER _scope_1
// ... function body instructions
// ... SCOPE_REGISTER for local variables only
SCOPE_EXIT _scope_1  // Automatic RELEASE of all registered variables
```

## Implementation Patterns

### Common IR Transformation Patterns
- **Symbol-Driven Processing**: Always use `parsedModule.getRecordedSymbol(ctx)` instead of text parsing
- **Temporary Variable Generation**: Use `context.generateTempName()` for intermediate results
- **Debug Information Integration**: Apply consistent debug info from `DebugInfoCreator`
- **Type-Safe Operations**: All operations use fully qualified type names

### Error Handling in Code Generation
- **Null Validation**: All generators validate input parameters with `AssertValue.checkNotNull()`
- **Symbol Type Checking**: Verify expected symbol types before processing (e.g., `instanceof AggregateSymbol`)
- **Context Preservation**: Maintain parse context relationships for error reporting

### Resource Management Strategies
- **Scope-Based Cleanup**: Use SCOPE_ENTER/SCOPE_EXIT for automatic memory management
- **Ownership Tracking**: Distinguish between caller-owned (parameters) and function-owned (locals) memory
- **Reference Counting**: Apply RETAIN/RELEASE based on ownership and scope registration

### Testing and Validation Approaches
- **@IR Directive Pattern**: Embed expected IR in EK9 source files for test-driven IR development
- **AbstractIRGenerationTest**: Standard test infrastructure for IR generation validation
- **Debug Instrumentation**: Enable debug output with `-Dek9.instructionInstrumentation=true` for verification

---

**Note**: This is a placeholder file created to organize future IR and code generation knowledge. Content will be added as Steve and I work on compiler backend development, moving away from Java implementation details to focus on EK9's intermediate representation and multi-target code generation capabilities.