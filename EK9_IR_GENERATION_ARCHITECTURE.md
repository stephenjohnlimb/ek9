# EK9 IR Generation Architecture

This document details the architectural patterns, naming conventions, and implementation strategies for EK9's Intermediate Representation (IR) generation system in compiler phase 7.

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Naming Conventions](#naming-conventions) 
3. [IR vs Instructions vs Definitions](#ir-vs-instructions-vs-definitions)
4. [Implementation Patterns](#implementation-patterns)
5. [Context Management](#context-management)
6. [Type Information Enhancement](#type-information-enhancement)
7. [Class Hierarchy and Organization](#class-hierarchy-and-organization)
8. [Missing Components Analysis](#missing-components-analysis)
9. [Development Guidelines](#development-guidelines)
10. [Integration with CallInstr Enhancements](#integration-with-callinstr-enhancements)
11. [Property Initialization and Inheritance IR Patterns](#property-initialization-and-inheritance-ir-patterns)
12. [C++ Runtime Integration Architecture](#c-runtime-integration-architecture)

## Architecture Overview

EK9's IR generation follows a multi-layered architecture that transforms ANTLR AST nodes into target-agnostic intermediate representation:

### Phase 7 Pipeline
```
IRGenerator (entry point)
    ↓
IRDfnGenerator (per-file orchestration)  
    ↓
*DfnGenerator (structural definitions: Class, Function, etc.)
    ↓
*InstrGenerator (executable instruction sequences)
    ↓
IRInstr classes (individual IR instructions)
```

### Key Architectural Principles

1. **Symbol-Driven Generation**: Uses resolved symbols from phases 1-6 rather than raw AST text
2. **Target-Agnostic IR**: Clean intermediate representation that targets JVM and C++ LLVM backends
3. **Scope Isolation**: Each executable context gets its own `IRContext` for unique naming
4. **Type Information Preservation**: Complete type information captured for multi-target code generation

## Naming Conventions

### Generator vs Creator Distinction

**Generator** - Main IR generation orchestrators that traverse AST and coordinate IR generation:
- `IRGenerator` - Phase 7 entry point and workspace coordination
- `IRDfnGenerator` - Per-file definition generation orchestration
- `*DfnGenerator` - Structural definition generators (classes, functions, programs)
- `*InstrGenerator` - Executable instruction sequence generators

**Creator** - Specialized helper classes for ancillary/metadata creation:
- `DebugInfoCreator` - Creates debug metadata and source location information
- Future: `TypeInfoCreator`, `AnnotationCreator`

### Naming Pattern Benefits

1. **Immediate Role Identification**: Class name indicates scope and responsibility
2. **Architectural Consistency**: Uniform naming across the entire IR generation system  
3. **Template-Driven Development**: New classes follow established patterns
4. **Clear Hierarchy**: Generator = orchestrator, Creator = specialized helper

## IR vs Instructions vs Definitions

### Core Distinction

**Instructions (`*Instr`)** - Executable operations:
- `IRInstr` - Base class for all executable instructions
- `CallInstr` - Method calls, constructor invocations, operators
- `MemoryInstr` - Memory operations (LOAD, STORE, ALLOC, RETAIN, RELEASE)
- `BranchInstr` - Control flow (BRANCH, RETURN, conditional branches)
- `ScopeInstr` - Scope management (ENTER, EXIT, REGISTER for memory management)
- `LiteralInstr` - Literal value operations
- `BasicBlockInstr` - Sequence of instructions (control flow unit)

**Definitions/Constructs** - Structural/declarative elements:
- `IRConstruct` - Definitions of classes, traits, functions, programs
- `Operation` - Method/function definitions within constructs

### Architectural Soundness

This distinction follows established compiler architecture patterns:
- **LLVM**: Instructions vs GlobalValues/Functions
- **Traditional SSA**: Instructions vs Phi-nodes vs Basic Blocks
- **Industrial Practice**: Executable vs declarative IR element separation

Benefits:
- **Clear Separation of Concerns**: Instructions = "what to do", Constructs = "what things are"
- **Different Processing**: Instructions for optimization/codegen, Constructs for symbol management
- **Type Safety**: Instruction-specific operations maintain compile-time safety

## Implementation Patterns

### Standard Generator Structure

All `*InstrGenerator` classes follow this template:

```java
public final class [Name]InstrGenerator {
  private final IRContext context;
  private final DebugInfoCreator debugInfoCreator;
  // Optional: specialized sub-generators
  
  public [Name]InstrGenerator(final IRContext context) {
    AssertValue.checkNotNull("IRContext cannot be null", context);
    this.context = context;
    this.debugInfoCreator = new DebugInfoCreator(context);
  }
  
  public List<IRInstr> apply(final EK9Parser.[Context]Context ctx, 
                             final String scopeId) {
    // Implementation following symbol-driven approach
    final var instructions = new ArrayList<IRInstr>();
    
    // Get resolved symbols from parsed module
    final var symbol = context.getParsedModule().getRecordedSymbol(ctx);
    
    // Generate IR instructions using symbol information
    // Add debug information if enabled
    // Return instruction sequence
    
    return instructions;
  }
}
```

### Standard Definition Generator Structures

**Aggregate Definition Generators** (extending AbstractDfnGenerator):

```java
public final class [Name]DfnGenerator extends AbstractDfnGenerator
    implements Function<EK9Parser.[Name]DeclarationContext, IRConstruct> {
  
  private final ParsedModule parsedModule;
  private final CompilerFlags compilerFlags;
  
  [Name]DfnGenerator(final ParsedModule parsedModule, final CompilerFlags compilerFlags) {
    super(parsedModule, compilerFlags);  // Initialize base class
    this.parsedModule = parsedModule;
    this.compilerFlags = compilerFlags;
  }
  
  @Override
  public IRConstruct apply(final EK9Parser.[Name]DeclarationContext ctx) {
    final var symbol = parsedModule.getRecordedSymbol(ctx);
    
    if (symbol instanceof AggregateSymbol aggregateSymbol && 
        symbol.getGenus() == SymbolGenus.[GENUS]) {
      final var construct = new IRConstruct(symbol);
      
      // Process aggregateParts if present (methods, operators, properties)
      if (ctx.aggregateParts() != null) {
        createOperationsForAggregateParts(construct, aggregateSymbol, ctx.aggregateParts());
      }
      
      return construct;
    }
    throw new CompilerException("Cannot create [Name] - expect AggregateSymbol of [GENUS] Genus");
  }
  
  private void createOperationsForAggregateParts(final IRConstruct construct,
                                                 final AggregateSymbol aggregateSymbol,
                                                 final EK9Parser.AggregatePartsContext ctx) {
    // Process methods using inherited processAsMethodOrOperator
    for (final var methodCtx : ctx.methodDeclaration()) {
      final var symbol = parsedModule.getRecordedSymbol(methodCtx);
      processAsMethodOrOperator(construct, symbol, methodCtx.operationDetails());
    }
    
    // Process operators using inherited processAsMethodOrOperator  
    for (final var operatorCtx : ctx.operatorDeclaration()) {
      final var symbol = parsedModule.getRecordedSymbol(operatorCtx);
      processAsMethodOrOperator(construct, symbol, operatorCtx.operationDetails());
    }
  }
}
```

**Standalone Definition Generators** (Program/Function):

```java
public final class [Name]DfnGenerator implements Function<EK9Parser.[Context]Context, IRConstruct> {
  private final ParsedModule parsedModule;
  private final CompilerFlags compilerFlags;
  
  [Name]DfnGenerator(final ParsedModule parsedModule, final CompilerFlags compilerFlags) {
    this.parsedModule = parsedModule;
    this.compilerFlags = compilerFlags;
  }
  
  @Override
  public IRConstruct apply(final EK9Parser.[Context]Context ctx) {
    final var symbol = parsedModule.getRecordedSymbol(ctx);
    
    // Create IRConstruct and Operation directly for standalone construct
    final var construct = new IRConstruct(symbol);
    createOperation(construct, symbol, ctx);
    
    return construct;
  }
}
```

## Context Management

### IRContext Architecture

`IRContext` provides scope-isolated state management for IR generation:

**Key Features**:
- **Unique Naming**: Generates unique temp variables, scope IDs, block labels
- **Symbol Access**: Provides access to resolved symbols from ParsedModule
- **Debug Control**: Enables/disables debug instrumentation via CompilerFlags
- **Scope Isolation**: Each executable scope gets its own context instance

**Usage Pattern**:
```java
// Each method/function gets its own IRContext
final var context = new IRContext(parsedModule, compilerFlags);

// Generate unique names within this scope
final var tempVar = context.generateTempName();      // "_temp1"
final var scopeId = context.generateScopeId("main"); // "_main_1" 
final var blockLabel = context.generateBlockLabel("entry"); // "_entry_1"
```

### Context Lifecycle

1. **Per-File Level**: `IRDfnGenerator` coordinates file-level generation
2. **Per-Definition Level**: Each `*DfnGenerator` processes structural definitions
3. **Per-Method Level**: Each method/operator gets its own `IRContext` instance
4. **Instruction Level**: `*InstrGenerator` classes use the method-level context

This hierarchy prevents naming conflicts while enabling parallel processing.

## Type Information Enhancement

### CallInstr Type Information

Building on previous session's work, `CallInstr` now captures complete type information for multi-target code generation:

**Type Information Fields**:
```java
private final String targetTypeName;      // EK9 format: "org.ek9.lang::Stdout"
private final List<String> parameterTypes; // EK9 format: ["org.ek9.lang::String"]  
private final String returnTypeName;      // EK9 format: "org.ek9.lang::Void"
```

**Symbol-Driven Extraction**:
```java
// Extract type information from resolved MethodSymbol
final var targetTypeName = (parentScope instanceof ISymbol symbol) 
    ? symbol.getFullyQualifiedName() : parentScope.toString();
final var returnTypeName = toBeCalled.getType()
    .map(ISymbol::getFullyQualifiedName)
    .orElse("org.ek9.lang::Void");
final var parameterTypes = toBeCalled.getCallParameters().stream()
    .map(param -> param.getType().map(ISymbol::getFullyQualifiedName).orElse("org.ek9.lang::Any"))
    .toList();
```

**Benefits**:
- **Clean IR Output**: Human-readable IR format maintained: `_temp5 = CALL _temp6.println(toOutput)`
- **Complete Type Information**: All necessary data for target-specific code generation captured internally
- **No Backward Compatibility**: All CallInstr usage updated to include type information from resolved symbols

### IRConstruct Signature Qualification

`IRConstruct` provides two levels of naming qualification to support method overloading and target code generation:

#### **Fully Qualified Names** (`getFullyQualifiedName()`):
- **Purpose**: Module and construct identification
- **Format**: `"module::construct"`
- **Examples**: `"introduction::aSimpleFunction"`, `"myModule::MyClass"`
- **Usage**: Class/module resolution, namespace identification

#### **Signature Qualified Names** (`getSignatureQualifiedName()`):
- **Purpose**: Complete method signature identification for overload resolution
- **Format**: `"module::construct(org.ek9.lang::ParamType1,org.ek9.lang::ParamType2)->org.ek9.lang::ReturnType"`
- **Examples**:
  ```
  "introduction::aSimpleFunction(org.ek9.lang::String)->org.ek9.lang::String"
  "myModule::MyClass::method(org.ek9.lang::String,org.ek9.lang::Integer)->org.ek9.lang::Boolean"
  ```
- **Usage**: Method overloading resolution, target code generation

#### **Symbol-Driven Signature Extraction**:
```java
// Parameters from IFunctionSymbol/MethodSymbol
final var parameters = functionSymbol.getCallParameters();
final var parameterTypes = parameters.stream()
    .map(param -> param.getType().map(ISymbol::getFullyQualifiedName).orElse("org.ek9.lang::Any"))
    .collect(Collectors.toList());

// Return type from IMayReturnSymbol
final var returnType = mayReturnSymbol.getReturningSymbol().getType()
    .map(ISymbol::getFullyQualifiedName).orElse("org.ek9.lang::Void");
```

#### **Target Code Generation Benefits**:
```java
// EK9 Signature: "MyClass::method(org.ek9.lang::String,org.ek9.lang::Integer)->org.ek9.lang::Boolean" 
// JVM Conversion: "(Lorg/ek9/lang/String;Lorg/ek9/lang/Integer;)Lorg/ek9/lang/Boolean;"
// C++ LLVM Conversion: "@MyClass_method_String_Integer_Boolean"
```

## Class Hierarchy and Organization

### Existing Classes (Phase 7)

**Entry Point**:
- `IRGenerator` - Phase 7 coordinator, handles workspace and multi-threading

**Per-File Orchestration**:
- `IRDfnGenerator` - Processes compilation units, delegates to definition generators

**Definition Generator Hierarchy**:
- `AbstractDfnGenerator` - Base class providing shared method/operator processing logic
  - `ClassDfnGenerator` - Class definitions (extends AbstractDfnGenerator)
  - `RecordDfnGenerator` - Record definitions (extends AbstractDfnGenerator)
  - `TraitDfnGenerator` - Trait definitions (extends AbstractDfnGenerator)
  - `ComponentDfnGenerator` - Component definitions (extends AbstractDfnGenerator)
- `ProgramDfnGenerator` - EK9 program definitions (standalone implementation)
- `FunctionDfnGenerator` - Function definitions (standalone implementation)

**Operation Processing**:
- `OperationDfnGenerator` - Dedicated processor for OperationDetailsContext (BiConsumer interface)

**Instruction Generators**:
- `ExpressionInstrGenerator` - Expression instruction sequences
- `ObjectAccessInstrGenerator` - Object access and method call sequences
- `BasicBlockInstrGenerator` - Basic block instruction sequences
- `StatementInstrGenerator` - Statement instruction sequences
- `AssignmentExpressionInstrGenerator` - Assignment expression sequences
- `BlockStatementInstrGenerator` - Block statement sequences
- `VariableDeclarationInstrGenerator` - Variable declaration sequences

**Support Classes**:
- `IRContext` - Scope-isolated context management
- `DebugInfoCreator` - Debug metadata creation
- `ProgramWithIR` - Program representation with IR

### IR Classes (`compiler.ir` package)

**Instructions**:
- `IRInstr` - Base class for all instructions
- `CallInstr` - Method calls, constructors, operators
- `MemoryInstr` - Memory operations
- `BranchInstr` - Control flow
- `ScopeInstr` - Scope management  
- `LiteralInstr` - Literal values
- `BasicBlockInstr` - Instruction sequences

**Definitions**:
- `IRConstruct` - Structural definitions (classes, functions, etc.)
- `Operation` - Method/function definitions within constructs

**Support**:
- `INode` - Base interface for visitor pattern
- `IROpcode` - Instruction operation codes
- `DebugInfo` - Source location and debug metadata

## Refactoring Benefits and Architecture Improvements

### Code Elimination and Consistency

The introduction of `AbstractDfnGenerator` and `OperationDfnGenerator` provides significant architectural benefits:

**Duplicate Code Elimination**:
- **~40 lines removed**: Identical method/operator processing logic eliminated across 4 aggregate generators
- **Single source of truth**: `processAsMethodOrOperator` method centralized in base class
- **Uniform validation**: Consistent MethodSymbol type checking across all generators

**Template-Driven Development**:
```java
// Adding new aggregate types is now trivial:
final class ServiceDfnGenerator extends AbstractDfnGenerator
    implements Function<EK9Parser.ServiceDeclarationContext, IRConstruct> {
  // Only construct-specific validation needed
  // All method/operator processing inherited from base class
}
```

### Clean Separation of Concerns

**Three-Layer Architecture**:
1. **Construct Validation** - Each specific generator validates symbol types and creates IRConstruct
2. **Operation Processing** - Shared `OperationDfnGenerator` handles all executable content 
3. **Instruction Generation** - `BasicBlockInstrGenerator` and sub-generators create IR sequences

**BiConsumer Pattern**:
- `OperationDfnGenerator` implements `BiConsumer<Operation, EK9Parser.OperationDetailsContext>`
- Clean functional interface for processing operation details
- Consistent IRContext creation and BasicBlock generation

### Inheritance vs Composition Strategy

**Aggregate Types** (Class, Record, Trait, Component):
- **Extend `AbstractDfnGenerator`** - Share common aggregateParts processing patterns
- **Process aggregateParts uniformly** - Methods and operators handled identically
- **Leverage inheritance** - Reduces boilerplate and ensures consistency

**Standalone Types** (Program, Function):  
- **Direct implementation** - Unique processing patterns that don't benefit from shared base
- **Custom operation creation** - Programs need special static main entry point handling
- **Flexibility maintained** - No forced inheritance where it doesn't add value

### Integration with Instructions vs Definitions Architecture

This refactoring strengthens the core architectural distinction:

- **Definitions Layer**: `*DfnGenerator` classes create structural IRConstruct definitions
- **Operation Bridge**: `OperationDfnGenerator` bridges structural definitions to executable processing
- **Instructions Layer**: `BasicBlockInstrGenerator` and instruction generators handle executable IR

The clean separation enables:
- **Independent development** - Definition and instruction generators can be developed in parallel
- **Clear responsibilities** - Each layer has well-defined scope and purpose  
- **Extensibility** - New construct types or instruction types can be added without cross-layer impact

## Missing Components Analysis

### Control Flow Generators Needed

**Conditional Logic**:
- `IfStatementInstrGenerator` - if/else statement IR generation
- `TernaryOperatorInstrGenerator` - ternary conditional operator
- `GuardInstrGenerator` - EK9 guard statement IR generation

**Loop Constructs**:
- `ForLoopInstrGenerator` - for loop IR sequences
- `WhileLoopInstrGenerator` - while loop IR sequences  
- `DoWhileLoopInstrGenerator` - do-while loop IR sequences

**Switch/Case**:
- `SwitchInstrGenerator` - switch/case statement IR generation

### Exception Handling Generators

- `TryWithResourcesInstrGenerator` - try-with-resources IR generation
- `ThrowInstrGenerator` - throw statement IR generation
- `CatchInstrGenerator` - exception handling IR sequences

### EK9-Specific Constructs

**Literal Types**:
- `CollectionLiteralInstrGenerator` - List/Dict literal initialization
- `RangeInstrGenerator` - EK9 range expression IR generation

**Advanced Features**:
- `DynamicFunctionInstrGenerator` - Dynamic function capture IR
- `StreamInstrGenerator` - EK9 stream processing IR
- `PipelineInstrGenerator` - EK9 pipeline operator IR

### Additional Definition Generators

**Missing Definition Types**:
- `EnumerationDfnGenerator` - Enumeration type definitions
- `ServiceDfnGenerator` - Service construct definitions
- `ApplicationDfnGenerator` - Application construct definitions

### Advanced IR Instructions

**Memory Management**:
- Enhanced scope tracking for LLVM targets
- Reference counting instruction sequences
- Garbage collection coordination instructions

**Optimization Support**:
- Phi-node instructions for SSA form
- Dead code elimination markers
- Loop invariant markers

## Development Guidelines

### Implementation Strategy

1. **Symbol-First Approach**: Always use resolved symbols from ParsedModule rather than raw AST text
2. **Inheritance-Based Templates**: Use `AbstractDfnGenerator` for aggregate types, direct implementation for standalone types
3. **Context Isolation**: Ensure each executable scope gets its own IRContext
4. **Type Preservation**: Capture complete type information from resolved symbols

### New Generator Implementation Patterns

**For Aggregate Types** (Service, Application, Enumeration):
```java
final class ServiceDfnGenerator extends AbstractDfnGenerator
    implements Function<EK9Parser.ServiceDeclarationContext, IRConstruct> {

  private final ParsedModule parsedModule;
  private final CompilerFlags compilerFlags;

  ServiceDfnGenerator(final ParsedModule parsedModule, final CompilerFlags compilerFlags) {
    super(parsedModule, compilerFlags);  // Initialize base class
    this.parsedModule = parsedModule;
    this.compilerFlags = compilerFlags;
  }

  @Override
  public IRConstruct apply(final EK9Parser.ServiceDeclarationContext ctx) {
    final var symbol = parsedModule.getRecordedSymbol(ctx);
    
    if (symbol instanceof AggregateSymbol aggregateSymbol && 
        symbol.getGenus() == SymbolGenus.SERVICE) {
      final var construct = new IRConstruct(symbol);
      
      // Use inherited method/operator processing
      if (ctx.aggregateParts() != null) {
        for (final var methodCtx : ctx.aggregateParts().methodDeclaration()) {
          final var methodSymbol = parsedModule.getRecordedSymbol(methodCtx);
          processAsMethodOrOperator(construct, methodSymbol, methodCtx.operationDetails());
        }
        // Repeat for operatorDeclaration if needed
      }
      
      return construct;
    }
    throw new CompilerException("Cannot create Service - expect AggregateSymbol of SERVICE Genus");
  }
}
```

**Benefits of Inheritance Approach**:
- **3-5 lines of code** per method/operator processing (vs ~15 lines without inheritance)
- **Automatic consistency** - All aggregate types process methods/operators identically
- **Single point of maintenance** - Changes to operation processing logic affect all generators

### Testing Patterns

**Unit Test Structure**:
```java
@Test
void testGenerateIfStatement() {
    // Setup IRContext with mock ParsedModule
    final var context = new IRContext(parsedModule, compilerFlags);
    
    // Create generator instance
    final var generator = new IfStatementInstrGenerator(context);
    
    // Apply to ANTLR context
    final var instructions = generator.apply(ifStatementContext, "_main_1");
    
    // Verify instruction sequence
    assertThat(instructions).hasSize(expectedCount);
    assertThat(instructions.get(0)).isInstanceOf(BranchInstr.class);
}
```

### Parallel Development

**Classes can be implemented in parallel**:
- Each generator is independent with clear interfaces
- IRContext provides thread-safe unique naming within scope
- Symbol resolution is read-only after phases 1-6

### Debug Integration

**Always integrate debug information**:
```java
// Extract debug info if debugging instrumentation is enabled
final var debugInfo = debugInfoCreator.apply(symbol);

// Add to all generated instructions
instructions.add(SomeInstr.operation(params, debugInfo));
```

### Error Handling

**Consistent error handling pattern**:
```java
// Validate resolved symbols exist
final var symbol = context.getParsedModule().getRecordedSymbol(ctx);
AssertValue.checkNotNull("Symbol should be resolved by phases 1-6", symbol);

// Handle missing type information gracefully  
final var typeName = symbol.getType()
    .map(ISymbol::getFullyQualifiedName)
    .orElse("org.ek9.lang::Any");  // Fallback to Any type
```

## Integration with CallInstr Enhancements

### Type Information Flow

The enhanced `CallInstr` integrates seamlessly with the generator architecture:

1. **Symbol Resolution**: Generators extract resolved MethodSymbol from ParsedModule
2. **Type Extraction**: Complete type information extracted from symbol hierarchy
3. **Instruction Creation**: CallInstr factory methods require complete type information
4. **Clean IR Output**: Human-readable format preserved while capturing internal type data

### Multi-Target Preparation

**Current State**: IR captures all necessary type information for future target-specific code generation

**Multi-Target Integration**: Target-specific code generators access complete type information:
```java
// JVM bytecode generator
if (callInstr.isConstructorCall()) {
    String jvmTypeName = convertEk9ToJvm(callInstr.getTargetTypeName());
    // Generate JVM constructor bytecode
}

// C++ LLVM IR generator  
if (callInstr.isMethodCall()) {
    String cppFunctionName = mangleForCppLLVM(
        callInstr.getTargetTypeName(),
        callInstr.getMethodName(), 
        callInstr.getParameterTypes()
    );
    // Generate LLVM call to C++ runtime
}
```

## Conclusion

EK9's IR generation architecture provides a clean, extensible foundation for compiler phase 7 with significant improvements from recent refactoring:

### Core Architectural Strengths
- **Consistent Naming**: Generator/Creator distinction provides immediate architectural understanding
- **Symbol-Driven**: Leverages complete type resolution from previous phases
- **Target-Agnostic**: Clean IR suitable for multiple backend targets
- **Type-Preserving**: Complete type information captured for advanced code generation
- **Signature Qualification**: IRConstruct supports both basic and signature-qualified names for overload resolution
- **Scope-Isolated**: IRContext prevents naming conflicts while enabling parallelization

### Refactoring Achievements
- **Code Elimination**: ~40 lines of duplicate method/operator processing removed across generators
- **Inheritance Hierarchy**: `AbstractDfnGenerator` provides shared logic for all aggregate types
- **Clean Separation**: Three-layer architecture (Construct → Operation → Instruction) with clear responsibilities
- **Template Simplification**: New aggregate generators require only 3-5 lines for method/operator processing

### Development Benefits  
- **Rapid Development**: Adding new aggregate types (Service, Application) is now trivial via inheritance
- **Consistent Processing**: All aggregate types handle methods/operators identically through base class
- **Single Maintenance Point**: Operation processing changes affect all generators automatically
- **Independent Layers**: Definition and instruction generators can be developed in parallel

This architecture enables systematic completion of EK9's IR generation while maintaining clean separation of concerns, eliminating code duplication, and preparing for multi-target code generation with significantly reduced development overhead.