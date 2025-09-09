# EK9 Phase 7 Functional Refactoring Guide

## Overview

This document outlines the refactoring of Phase 7 (IR Generation) to apply the functional decomposition pattern used in earlier compiler phases, while handling the unique complexity of IR generation including state management, instruction ordering, and memory management.

## Problem Analysis

### Current Issues in Phase 7

1. **Duplicate Function Call Handling:**
   - `CallInstrGenerator` - handles function calls from statement contexts
   - `ExprInstrGenerator.processCall()` - handles function calls from expression contexts
   - **Neither uses our unified CallDetailsBuilder architecture**

2. **Broken Function Call Processing:**
   ```java
   // ExprInstrGenerator.processCall() only handles constructors
   if (toBeCalled instanceof MethodSymbol methodSymbol && methodSymbol.isConstructor()) {
     // Handle constructor...
   } else {
     AssertValue.fail("Expecting method to have been resolved"); // FAILS for function calls!
   }
   ```

3. **Manual CallDetails Construction:**
   Both generators manually create `CallDetails` with no promotion logic:
   ```java
   final var callDetails = new CallDetails(functionInstanceVar, fullyQualifiedFunctionName, "_call", 
                                          argumentDetails.parameterTypes(), returnType, 
                                          argumentDetails.argumentVariables(), metaData);
   ```

### Contrast with Earlier Phases (Proper Pattern)

```java
// Phase 3 - Proper functional routing pattern
@Override
public void exitCall(final EK9Parser.CallContext ctx) {
  callOrError.accept(ctx);  // Delegates to functional component
  super.exitCall(ctx);
}

// ExpressionsListener constructor - configured functional components
protected ExpressionsListener(ParsedModule parsedModule) {
  this.callOrError = new CallOrError(symbolsAndScopes, symbolFactory, errorListener);
  this.expressionOrError = new ExpressionOrError(symbolsAndScopes, symbolFactory, errorListener);
  // All components share state through constructor injection
}
```

## Architecture Design

### Two-Level Architecture

**Router Level (Simple Delegation):**
- Generators become simple routers like earlier phases
- Pure delegation without complex logic

**Component Level (Configured Stateful Functions):**
- Functional interfaces with state injection
- Complex IR generation logic
- Proper state threading and memory management

### Pattern Example

```java
// Router Level - Simple delegation
private List<IRInstr> processCall(final ExprProcessingDetails details) {
  return functionCallProcessor.apply(details);  // Pure routing like earlier phases
}

// Component Level - Configured stateful functions
class FunctionCallProcessor implements Function<CallProcessingDetails, List<IRInstr>> {
  private final CallDetailsBuilder callDetailsBuilder;     // Configured with IRContext
  private final VariableMemoryManagement memoryManagement; // Memory handling
  private final IRContext context;                         // State threading
  
  FunctionCallProcessor(IRContext context) { 
    this.callDetailsBuilder = new CallDetailsBuilder(context);
    this.memoryManagement = new VariableMemoryManagement();
    this.context = context;
  }
  
  public List<IRInstr> apply(CallProcessingDetails details) {
    // Unified method resolution with promotion
    var callDetailsResult = callDetailsBuilder.apply(createCallContext(details));
    
    // Context-appropriate memory management
    if (details.isStatementContext()) {
      return applyStatementMemoryManagement(callDetailsResult, details);
    } else {
      return applyExpressionMemoryManagement(callDetailsResult, details);
    }
  }
}
```

## Critical IR Generation Considerations

### 1. Memory Management Complexity
Unlike earlier phases, IR generation has intricate memory patterns:
```java
RETAIN _temp2
SCOPE_REGISTER _temp2, _scope_1
```
**Requirements:**
- Functional components must preserve exact memory patterns
- Context-sensitive behavior (statement vs expression contexts)
- No disruption to existing memory model

### 2. Instruction Ordering
IR instruction sequence is critical:
```java
_temp1 = LOAD_LITERAL 42        // Must come first
_temp2 = PROMOTE _temp1         // Must come second  
_temp3 = FUNCTION_INSTANCE...   // Must come third
CALL ...(_temp2)               // Uses promoted result
```
**Requirements:**
- Components must maintain proper instruction sequencing
- No reordering during refactoring

### 3. State Threading
IR generation maintains complex shared state:
- `IRContext` with temp variable generation
- Scope tracking
- Debug info creation
- All functional components must thread state properly

### 4. Context-Sensitive Behavior
Same call needs different handling:
- **Statement context**: Full memory management with RETAIN/SCOPE_REGISTER
- **Expression context**: Minimal memory management
- Components need context-aware memory strategy

## Implementation Plan

### Step 1: Create Functional Components

#### Primary Components to Create:

1. **`FunctionCallProcessor`**
   - **Purpose**: Unified function call handling using CallDetailsBuilder
   - **Replaces**: Manual function call logic in both CallInstrGenerator and ExprInstrGenerator
   - **Integration**: Uses CallDetailsBuilder for method resolution and automatic promotion
   - **Context Handling**: Statement vs expression memory management

2. **`OperatorCallProcessor`**
   - **Purpose**: Unified operator handling (binary, unary, assignment)
   - **Status**: Partially implemented via CallDetailsBuilder integration in existing generators
   - **Next Steps**: Complete unification and routing pattern

3. **Enhanced `ConstructorCallProcessor`**
   - **Status**: Already exists
   - **Enhancement**: Add CallDetailsBuilder integration for promotion support
   - **Goal**: Consistent method resolution across all call types

#### Component Architecture:

```java
// Base functional component pattern
interface CallProcessor<T> extends Function<T, List<IRInstr>> {
  // Marker interface for IR processing components
}

// Function call processor
class FunctionCallProcessor implements CallProcessor<CallProcessingDetails> {
  private final CallDetailsBuilder callDetailsBuilder;
  private final VariableMemoryManagement memoryManagement;
  private final IRContext context;
  
  public FunctionCallProcessor(IRContext context) {
    this.callDetailsBuilder = new CallDetailsBuilder(context);
    this.memoryManagement = new VariableMemoryManagement();
    this.context = context;
  }
  
  @Override
  public List<IRInstr> apply(CallProcessingDetails details) {
    // Implementation with proper state management
  }
}
```

### Step 2: Convert Generators to Routers

#### Transform Existing Generators:

1. **`ExprInstrGenerator`**
   ```java
   // Before - complex inline logic
   private List<IRInstr> processCall(final ExprProcessingDetails details) {
     // 50+ lines of complex logic
     if (toBeCalled instanceof MethodSymbol methodSymbol && methodSymbol.isConstructor()) {
       // constructor handling...
     } else {
       AssertValue.fail("Function calls not supported");
     }
   }
   
   // After - simple routing
   private List<IRInstr> processCall(final ExprProcessingDetails details) {
     return functionCallProcessor.apply(details);
   }
   ```

2. **`CallInstrGenerator`**
   - Remove duplicate function call logic
   - Delegate to same components as expression context
   - Maintain context differences through component configuration

#### Router Configuration Pattern:

```java
// Generator constructor - configure functional components
ExprInstrGenerator(IRContext context) {
  super(context);
  this.functionCallProcessor = new FunctionCallProcessor(context);
  this.operatorCallProcessor = new OperatorCallProcessor(context);
  // ... other components
}
```

### Step 3: Gradual Migration with Validation

#### Migration Strategy:

1. **Phase 1**: Create components alongside existing code
2. **Phase 2**: Replace broken `ExprInstrGenerator.processCall()` first (lowest risk)
3. **Phase 3**: Migrate CallInstrGenerator function call logic
4. **Phase 4**: Unify operator processing
5. **Phase 5**: Cleanup and consolidation

#### Validation at Each Step:

```bash
# Run full IR test suite after each migration
mvn test -pl compiler-main -Dtest="*IR*Test"

# Specific IR generation tests
mvn test -Dtest=CallsTest -pl compiler-main
mvn test -Dtest=ConversionOperatorTest -pl compiler-main
mvn test -Dtest=BinaryOperatorTest -pl compiler-main
```

#### Validation Criteria:

- **Exact IR Match**: Before/after IR output must be identical
- **Memory Management**: RETAIN/SCOPE_REGISTER patterns preserved
- **Instruction Ordering**: Sequence maintained
- **Performance**: No degradation in compilation speed
- **Test Coverage**: All existing IR tests pass

### Step 4: Integration and Testing

#### Final Integration Steps:

1. **Remove Duplicate Code**: ~200 lines of repeated method resolution logic
2. **Consolidate Memory Management**: Unified patterns across components
3. **Update Promotion Test**: Expect correct promotion behavior
4. **Documentation Update**: Update Phase 7 architecture documentation

#### Success Criteria:

- **Unified Promotion**: All function calls automatically get Integer→Float promotion
- **Reduced Duplication**: Eliminated manual CallDetails construction
- **Architectural Consistency**: Phase 7 follows same pattern as earlier phases
- **Maintained Functionality**: All existing IR tests pass
- **Enhanced Testability**: Components can be tested in isolation

## Testing Strategy

### Existing Test Integration

Our created test demonstrates the current problem:
- **File**: `/examples/irGeneration/calls/functionParameterPromotion.ek9`
- **Current Behavior**: No promotion (Integer passed directly to Float parameter)
- **Expected After Refactoring**: Automatic `_promote()` call generation

### Validation Commands

```bash
# Test specific promotion functionality
mvn test -Dtest=CallsTest -pl compiler-main

# Full IR generation test suite
mvn test -pl compiler-main -Dtest="*IR*Test,*Operator*Test,*Call*Test"

# Specific test categories
mvn test -Dtest=ConversionOperatorTest -pl compiler-main  # Promotion tests
mvn test -Dtest=ArithmeticOperatorTest -pl compiler-main  # Operator tests
mvn test -Dtest=AssignmentOperatorTest -pl compiler-main  # Assignment tests
```

### Test-Driven Migration

1. **Baseline**: Ensure all IR tests pass before refactoring
2. **Component Creation**: Test components in isolation with mock contexts
3. **Router Migration**: Validate exact IR output match after each router conversion
4. **Integration**: Full test suite regression testing
5. **Enhancement**: Update promotion test to validate new behavior

## Implementation Notes

### Key Files to Modify

1. **Primary Generators**:
   - `/compiler-main/src/main/java/org/ek9lang/compiler/phase7/ExprInstrGenerator.java`
   - `/compiler-main/src/main/java/org/ek9lang/compiler/phase7/CallInstrGenerator.java`

2. **New Components** (to create):
   - `/compiler-main/src/main/java/org/ek9lang/compiler/phase7/support/FunctionCallProcessor.java`
   - `/compiler-main/src/main/java/org/ek9lang/compiler/phase7/support/OperatorCallProcessor.java`

3. **Enhanced Components** (to modify):
   - `/compiler-main/src/main/java/org/ek9lang/compiler/phase7/support/ConstructorCallProcessor.java`

4. **Test Files**:
   - `/test/resources/examples/irGeneration/calls/functionParameterPromotion.ek9`
   - `/test/java/org/ek9lang/compiler/ir/CallsTest.java`

### State Management Pattern

```java
// Consistent state injection pattern
class IRComponent {
  private final IRContext context;              // Core context
  private final CallDetailsBuilder builder;     // Method resolution
  private final VariableMemoryManagement memory; // Memory handling
  
  public IRComponent(IRContext context) {
    this.context = context;
    this.builder = new CallDetailsBuilder(context);
    this.memory = new VariableMemoryManagement();
  }
  
  // Use context throughout processing
  public List<IRInstr> process(ProcessingDetails details) {
    var tempVar = context.generateTempName();
    var debugInfo = context.createDebugInfo(details.token());
    // ... proper state threading
  }
}
```

### Memory Management Context Handling

```java
// Context-sensitive memory management
private List<IRInstr> applyMemoryManagement(CallDetailsResult callResult, 
                                           ProcessingDetails details) {
  var instructions = new ArrayList<IRInstr>();
  instructions.addAll(callResult.allInstructions());
  
  if (details.isStatementContext()) {
    // Full memory management for statements
    instructions.add(MemoryInstr.retain(details.resultVariable()));
    instructions.add(MemoryInstr.scopeRegister(details.resultVariable(), details.scopeId()));
  }
  // Expression context uses minimal memory management
  
  return instructions;
}
```

## Benefits of This Refactoring

### Architectural Benefits

1. **Consistency**: Phase 7 follows same functional decomposition pattern as earlier phases
2. **Composability**: Components can be easily combined and tested
3. **Separation of Concerns**: Routing logic separate from IR generation logic
4. **Maintainability**: Easier to modify and extend individual components

### Functional Benefits

1. **Unified Promotion**: All function calls automatically support parameter promotion
2. **Reduced Duplication**: ~200 lines of repeated method resolution logic eliminated
3. **Consistent Behavior**: Same method resolution logic across operators, functions, constructors
4. **Enhanced Error Handling**: Consistent error patterns across all call types

### Development Benefits

1. **Testability**: Components can be unit tested in isolation
2. **Debugging**: Easier to trace issues through functional components
3. **Performance**: Potential optimization opportunities through component reuse
4. **Documentation**: Clear component responsibilities and interfaces

## Conclusion

This refactoring transforms Phase 7 from a monolithic approach to a functional decomposition pattern consistent with earlier compiler phases, while carefully preserving the complex requirements of IR generation including memory management, instruction ordering, and state threading.

The key insight is using **configured stateful functional components** rather than pure functions, allowing us to maintain the benefits of functional decomposition while handling Phase 7's unique complexity requirements.

**Next Steps**: Begin with Step 1 - creating the `FunctionCallProcessor` component to handle unified function call processing with CallDetailsBuilder integration.