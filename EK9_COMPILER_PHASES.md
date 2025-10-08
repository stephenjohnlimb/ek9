# EK9 Compiler Phases Implementation Guide

This document provides detailed guidance for implementing and understanding the EK9 compiler's 20-phase compilation pipeline. This is the specialized reference for compiler phase development, symbol table management, and phase interdependencies.

**Related Documentation:**
- **`CLAUDE.md`** - Main project overview and daily development guidelines  
- **`EK9_IR_AND_CODE_GENERATION.md`** - IR generation and code generation specifics
- **`EK9_Compiler_Architecture_and_Design.md`** - Complete architectural specification and phase overview

## Compilation Pipeline Overview

The EK9 compiler follows a **20-phase compilation pipeline** organized into three main stages:

### Frontend Phases (0-9)
- **Phase 0: PARSING** - ANTLR4-based parsing
- **Phase 1: SYMBOL_DEFINITION** - Symbol table creation  
- **Phase 2: DUPLICATION_CHECK** - Duplicate detection
- **Phase 3: REFERENCE_CHECKS** - Reference validation
- **Phase 4: EXPLICIT_TYPE_SYMBOL_DEFINITION** - Type resolution
- **Phase 5: TYPE_HIERARCHY_CHECKS** - Inheritance validation
- **Phase 6: FULL_RESOLUTION** - Template and generic resolution
- **Phase 7: POST_RESOLUTION_CHECKS** - Symbol validation
- **Phase 8: PRE_IR_CHECKS** - Code flow analysis
- **Phase 9: PLUGIN_RESOLUTION** - Plugin resolution

### Middle-end Phases (10-12)
- **Phase 10: IR_GENERATION** - Intermediate representation generation
- **Phase 11: IR_ANALYSIS** - IR analysis and validation
- **Phase 12: IR_OPTIMISATION** - IR-level optimizations

### Backend Phases (13-19)
- **Phase 13: CODE_GENERATION_PREPARATION** - Code generation preparation
- **Phase 14: CODE_GENERATION_AGGREGATES** - Generate code for aggregates
- **Phase 15: CODE_GENERATION_CONSTANTS** - Generate code for constants
- **Phase 16: CODE_OPTIMISATION** - Target code optimizations
- **Phase 17: PLUGIN_LINKAGE** - Link external plugins
- **Phase 18: APPLICATION_PACKAGING** - Application packaging
- **Phase 19: PACKAGING_POST_PROCESSING** - Completing post processing

## Phase Implementation Patterns

*This section will contain:*
- Common phase implementation patterns
- Error handling and recovery strategies
- Performance considerations for each phase
- Testing approaches for phase correctness

## Symbol Table Management

### Symbol Definition and Resolution
*This section will contain:*
- Symbol table structure and organization
- Symbol resolution algorithms across phases
- Scoping rules and symbol visibility
- Generic type symbol handling

### Cross-Phase Data Flow
*This section will contain:*
- Data structures passed between phases
- State management across compilation
- Symbol table evolution through phases
- Error accumulation and reporting

## Phase-Specific Implementation Details

### Parsing Phase (0)
*This section will contain:*
- ANTLR4 grammar integration
- Parse tree construction
- Error recovery strategies
- Source location tracking

### Symbol Definition Phase (1)
*This section will contain:*
- Initial symbol table creation
- Type definition processing
- Forward reference handling
- Module and package symbol creation

### Type Resolution Phases (4-8)
*This section will contain:*
- Type hierarchy construction
- Generic type instantiation
- Constraint resolution
- Inheritance and composition handling

### Code Analysis Phases (9-18)
*This section will contain:*
- Control flow analysis
- Data flow analysis
- Dead code detection
- Optimization opportunity identification

### IR Generation Phase (10)

**Phase 10: IR_GENERATION** is the **CURRENT DEVELOPMENT PRIORITY** that transforms resolved symbols from frontend phases into target-agnostic intermediate representation.

**Status**: 🔨 Active development - extending IR generation to cover ALL EK9 language features

**Priority**: Completeness and correctness for all control flow, operators, and language constructs

**Rationale**: IR is the common foundation for both JVM and LLVM backends. Complete IR generation is required before either backend can achieve feature completeness.

**Goal**: Every EK9 construct must have correct IR representation:
- All control flow statements (if/else, while, for, switch, etc.)
- All EK9 operators and expressions
- Function/method calls and closures
- Generic type instantiation
- Exception handling
- Pattern matching and guards
- All other EK9 language features

**Development Approach**: Dual-backend parallel development
- **`main` branch**: JVM bytecode generation (depends on complete IR)
- **`ek9llvm` branch**: LLVM IR generation (depends on complete IR)
- Two Claude Code instances working in parallel, both requiring complete IR foundation

#### Function-to-Class Transformation

EK9 functions are transformed into class-like constructs using the "Everything as Object" design principle:

**Function Processing Flow:**
1. **FunctionDfnGenerator** creates function-as-class construct with synthetic `_call` method
2. **OperationDfnGenerator** processes function body with proper scope management
3. **Parameter handling** with caller-owned memory semantics (no SCOPE_REGISTER)
4. **Local variable handling** with function-owned memory semantics (includes SCOPE_REGISTER)

**Example Function IR Pattern:**
```ek9
// EK9 Source:
checkAssert()
  -> arg0 as Boolean
  assert arg0

// Generated IR:
ConstructDfn: justAssert::checkAssert
OperationDfn: justAssert::checkAssert._call()->org.ek9.lang::Void
BasicBlock: _entry_1
IRInstruction: REFERENCE arg0, org.ek9.lang::Boolean  // Parameter declaration only
IRInstruction: SCOPE_ENTER _scope_1  // Function body scope
IRInstruction: _temp1 = LOAD arg0  // Load parameter value
IRInstruction: _temp2 = CALL (org.ek9.lang::Boolean)_temp1._true()  // Boolean conversion
IRInstruction: ASSERT _temp2  // Primitive boolean assertion
IRInstruction: SCOPE_EXIT _scope_1
IRInstruction: RETURN
```

#### Memory Ownership and Scope Management

**Critical Memory Management Rules:**
- **Parameters**: Caller-owned memory, receive REFERENCE declaration only
- **Return Variables**: Ownership transferred to caller, no SCOPE_REGISTER
- **Local Variables**: Function-owned memory, require both REFERENCE and SCOPE_REGISTER

**ShouldRegisterVariableInScope Logic:**
```java
// Parameters (_param_*): FALSE - caller manages memory
// Return variables (_return_*): FALSE - transferred to caller
// Local variables (_scope_*): TRUE - function manages memory
```

#### Assert Statement Processing

Assert statements demonstrate EK9's "Everything as Object" philosophy:

**Processing Steps:**
1. **Expression Evaluation**: Convert assert expression to temporary variable
2. **Boolean Conversion**: Call `_true()` method on Boolean object for primitive boolean
3. **Assertion**: Use ASSERT IR instruction with primitive boolean result

**IR Pattern:**
```
_temp1 = LOAD parameter
_temp2 = CALL (org.ek9.lang::Boolean)_temp1._true()
ASSERT _temp2
```

#### Key IR Generation Components

- **IRGenerator**: Main orchestration class processing CompilableSource modules
- **IRDfnGenerator**: Processes different EK9 constructs (classes, functions, programs)
- **AbstractDfnGenerator**: Base class providing common processing patterns
- **OperationDfnGenerator**: Handles method/function body processing
- **AssertStmtGenerator**: Specific processing for assert statements
- **ExprInstrGenerator**: Expression evaluation and temporary variable generation

#### Scope-Based Resource Management

**Scope Lifecycle Pattern:**
```
SCOPE_ENTER _scope_1
// ... function body instructions with SCOPE_REGISTER for locals
SCOPE_EXIT _scope_1  // Automatic RELEASE of all registered variables
```

**Scope Types:**
- `_param_*`: Parameter scope (no automatic cleanup)
- `_return_*`: Return scope (ownership transfer)
- `_scope_*`: General scope (automatic cleanup)

### IR Optimisation Phase (12)

**Phase 12: IR_OPTIMISATION** is **DEFERRED** until after both JVM and LLVM backends achieve feature completeness.

**Status**: ⏸️ Stub implementation only (returns true without performing optimization)

**Current Implementation**: The `IROptimisation` phase exists in the pipeline but performs no actual optimization passes.

**Rationale for Deferral**:
1. **Foundation First**: Incomplete IR generation blocks both backends
2. **Correctness Over Performance**: Working features are more valuable than optimized incomplete features
3. **Parallel Development**: Both backend Claudes can work simultaneously once IR is complete
4. **Testing Confidence**: Correct behavior must be validated before optimization complicates testing

**Timeline**: After both backends achieve feature completeness, optimization work merges:
- Stack allocation optimization (opcodes already defined)
- Escape analysis (data structures in place)
- Memory optimization passes (infrastructure ready)
- Dead code elimination
- Constant folding and propagation

**Infrastructure Ready**:
- ✅ IR opcodes defined for stack allocation (STACK_ALLOC, STACK_ALLOC_LITERAL, etc.)
- ✅ Escape analysis data structures exist
- ✅ Optimization architecture designed
- 📋 Optimization passes not yet implemented

**When Optimization Becomes Priority**:
- All EK9 language constructs have correct IR generation
- Both JVM and LLVM backends can compile complete EK9 programs
- Cross-backend validation confirms identical behavior
- Performance benchmarking framework is in place

### Code Generation Phases (14-19)
*This section will contain:*
- Target-specific code generation strategies
- Backend optimizations
- Resource management
- Output file generation

## Error Handling Across Phases

### Error Collection and Reporting
*This section will contain:*
- Error accumulation strategies
- Error severity classification
- Cross-phase error correlation
- User-friendly error reporting

### Recovery Strategies
*This section will contain:*
- Error recovery techniques per phase
- Graceful degradation approaches
- Partial compilation support
- IDE integration considerations

## Phase Dependencies and Ordering

### Required Phase Ordering
*This section will contain:*
- Phase dependency analysis
- Critical path through compilation
- Parallel execution opportunities
- Phase skipping conditions

### Data Dependencies
*This section will contain:*
- Inter-phase data requirements
- Symbol table state requirements
- Minimal phase completion criteria
- Incremental compilation support

## Testing and Validation

### Phase-Specific Testing
*This section will contain:*
- Unit testing strategies for individual phases
- Integration testing across phase boundaries
- Regression testing approaches
- Performance benchmarking

### End-to-End Validation
*This section will contain:*
- Full pipeline testing strategies
- Complex source code validation
- Error handling testing
- Performance regression detection

## Performance Considerations

### Optimization Strategies
*This section will contain:*
- Phase-specific optimizations
- Memory usage optimization
- Parallel processing opportunities
- Caching and memoization strategies

### Profiling and Analysis
*This section will contain:*
- Phase timing analysis
- Memory usage profiling
- Bottleneck identification
- Scalability considerations

---

**Note**: This is a placeholder file created to organize future compiler phase implementation knowledge. Content will be added as Steve and I work on detailed compiler phase development, focusing on the 20-phase pipeline implementation, symbol table management, and phase interdependencies that make up the core of the EK9 compilation process.