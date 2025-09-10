# EK9 Phase 7 IR Generation: Architectural Analysis & Refactoring Strategy

## Executive Summary

**Core Problem**: EK9's Phase 7 (IR Generation) violates the proven architectural patterns that make phases 1-6 successful, maintainable, and scalable.

**Key Findings**:
- **67 Java files, 6,019 lines** of IR generation code with extensive duplication
- **35 generator classes** with overlapping responsibilities and no clear coordination
- **Parameter threading hell**: Complex state passed through every method call
- **Missing abstractions**: No centralized state management or clear architectural pattern

**The Solution**: Apply EK9's own proven **Coordinator + Focused Helpers** pattern from successful phases 1-6.

---

## The Proven EK9 Pattern (Phases 1-6)

### ✅ What Works: Clear Architectural Success

EK9's early phases demonstrate a **consistently successful pattern**:

#### **1. Main Coordinator Pattern**
```java
// Phase 1: DefinitionListener orchestrates ~40 focused helpers
public class DefinitionListener extends AbstractEK9PhaseListener {
    // Clean delegation to single-responsibility helpers
    private final ApplicationBodyOrError applicationBodyOrError;
    private final BlockScopeName blockScopeName;
    private final EmitConstructAndReferenceConflictError conflictError;
    // ... 40+ focused helpers
}

// Phase 3: ExpressionsListener orchestrates ~100 focused helpers  
public class ExpressionsListener extends AbstractEK9PhaseListener {
    private final CallOrError callOrError;
    private final IdentifierOrError identifierOrError;
    private final AssignmentOrError assignmentOrError;
    // ... 100+ focused helpers
}
```

#### **2. Shared State Management**
```java
public class SymbolsAndScopes {
    private final ParsedModule parsedModule;
    private final ScopeStack scopeStack;  // ⭐ Proven transient stack pattern
    
    // Clean state management
    public void enterNewScope(final IScope scope, final ParseTree node);
    public void exitScope();
    public IScope getTopScope();
}
```

#### **3. Single Responsibility Helpers**
Each helper does **exactly one thing**:
- `CallOrError` - validates function/method calls
- `IdentifierOrError` - resolves identifier references  
- `ApplicationBodyOrError` - validates application definitions
- `BlockScopeName` - generates scope names for blocks

#### **4. Transient Stack Success**
The `ScopeStack` pattern has proven **extremely effective**:
```java
// Phase 1 pattern that works beautifully:
@Override
public void enterBlock(final EK9Parser.BlockContext ctx) {
    final var scopeName = blockScopeName.apply(new Ek9Token(ctx.start));
    final LocalScope instructionBlock = new LocalScope(scopeName, symbolsAndScopes.getTopScope());
    symbolsAndScopes.enterNewScope(instructionBlock, ctx);  // Push to stack
}

@Override  
public void exitBlock(final EK9Parser.BlockContext ctx) {
    symbolsAndScopes.exitScope();  // Pop from stack
}
```

**Why This Works**:
- **Clean state management** - no parameter threading
- **Automatic cleanup** - stack pops maintain consistency  
- **Context awareness** - always know current scope
- **Thread safety** - each thread has its own stack
- **Scalable** - handles arbitrary nesting depth

---

## Phase 7 IR Generation Problems

### ❌ Current State Analysis

#### **Scale & Complexity Issues**
- **67 Java files** for IR generation (vs ~50 for all other phases combined)
- **6,019 lines of code** with extensive duplication
- **35 Generator classes** with overlapping responsibilities
- **30 Support classes** with unclear separation of concerns

#### **Parameter Threading Hell**  
Every IR generator method requires:
```java
// Repeated across 35+ generator classes:
public List<IRInstr> someMethod(
    IRContext context,           // Thread through everything
    String scopeId,             // Thread through everything  
    DebugInfo debugInfo,        // Thread through everything
    VariableDetails resultDetails, // Thread through everything
    EK9Parser.SomeContext parseContext // Thread through everything
) {
    // Complex state management in every method
    final var tempResult = context.generateTempName();
    final var variableDetails = new VariableDetails(tempResult, new BasicDetails(scopeId, debugInfo));
    // ... repeat this pattern 100+ times across codebase
}
```

#### **Architectural Inconsistencies**
```java
// Different generators handle same concepts inconsistently:
new BasicDetails("temp_scope", debugInfo)     // ❌ Hardcoded scope
new BasicDetails(scopeId, null)              // ❌ Null debug info  
new BasicDetails(actualScope, correctDebug)  // ✅ What we want (rare)
```

#### **Generator Explosion**
- `BinaryOperationGenerator` + `BinaryOperationGeneratorWithProcessor`  
- `UnaryOperationGenerator` + `UnaryOperationGeneratorWithProcessor`
- `AssignmentStmtGenerator` + `AssignExpressionToSymbol` + `AssignmentExprInstrGenerator`

**Root Cause**: No clear architectural pattern or coordination strategy.

### ❌ Functional Approach Fighting Stateful Requirements

**The Mismatch**: IR generation is **inherently stateful** (scope management, memory management, debug tracking), but implemented with **functional patterns** (parameter threading, immutable contexts).

**Evidence**: Our recent fixes to parameter promotion required:
- Passing `CallContext` through multiple layers
- Manual scope ID management  
- Debug info threading through helper methods
- Memory management state coordination

This pattern **does not scale** to EK9's full synthesis requirements.

---

## EK9's Unique IR Synthesis Requirements

### Beyond Traditional Compilation

EK9 doesn't just **translate** AST nodes to IR - it **synthesizes** code that doesn't exist in the source:

#### **1. Synthetic Method Generation**
```ek9
// Developer writes:
class Person(name String, age Integer)

// EK9 must synthesize IR for:
_string() <- rtn as String {
    // Call name._string() + age._string() + super._string()
}
_cmp(other Person) <- rtn as Integer {
    // Compare name, then age, call super._cmp()  
}
// Derive <, <=, >=, > operators from _cmp()
```

**Already Identified**: Earlier phases mark synthetic methods with `method.setSynthetic(true)`

#### **2. Dependency Injection Wiring** 
```ek9
class ConfigHandler
    config as BaseConfiguration!  // ⭐ Injection point marked with !

// EK9 must synthesize:
// - Component lookup from application registry
// - Injection wiring at construction time
// - Lifecycle management code
```

#### **3. AOP Aspect Weaving**
```ek9
register Solution1() as BaseConfiguration with aspect of TimerAspect(), LoggingAspect("DEBUG")

// EK9 must synthesize:
class Solution1_AspectWoven extends Solution1 {
    override someMethod() {
        var prepared = timerAspect.beforeAdvice(joinPoint);
        loggingAspect.beforeAdvice(joinPoint);
        try {
            var result = super.someMethod();
            loggingAspect.afterAdvice(prepared);  
            timerAspect.afterAdvice(prepared);
            return result;
        } catch (ex) {
            // Exception handling aspect code
        }
    }
}
```

#### **4. Application-Program Binding**
```ek9
defines application firstApp
    register FileLogger() as ILogger
    register Solution1() as BaseConfiguration

Program1 with application of firstApp

// EK9 must synthesize:
// - Component factory registry
// - Dependency resolution graph
// - Injection context setup
```

#### **5. Generic Type Instantiation**
```ek9
var list = List(String)()

// EK9 must synthesize:
// - List<String> concrete implementation  
// - All templated methods with String substitution
// - Memory management specific to String references
```

#### **6. Trait Delegation & Weaving**
```ek9
class MyClass with SomeTrait, AnotherTrait

// EK9 must synthesize:
// - Delegate methods to trait implementations
// - Resolve method conflicts between traits  
// - Weave trait state into class state
```

### The Synthesis Challenge

**Traditional Compiler**: AST node → IR instructions  
**EK9 Compiler**: Language semantics + existing symbols → Generated IR for non-existent code

This requires:
- **Rich context management** (current class, traits, generics, application scope)
- **Template-based generation** (reusable patterns for synthesis)
- **Dependency orchestration** (traits before classes, super before derived)
- **Multi-phase synthesis** (injection points, then aspects, then bindings)

---

## Evidence from EK9 Source Examples

### Complex Real-World Requirements

From `ComponentsAndAspects.ek9`:

#### **Dependency Injection Pattern**
```ek9
class ConfigHandler
    config as BaseConfiguration!  // ⭐ Will be injected marker

class LoggingAspect extends Aspect  
    logger as ILogger!            // ⭐ Will be injected marker
```

#### **AOP Aspect Definition**
```ek9
register Solution1() as BaseConfiguration with aspect of TimerAspect(SystemClock()), LoggingAspect("DEBUG")

// Must weave aspect methods around all Solution1 methods:
override beforeAdvice() -> joinPoint as JoinPoint
override afterAdvice() -> preparedMetaData as PreparedMetaData  
```

#### **Application Composition**
```ek9
defines application firstApp
    register FileLogger() as ILogger  
    register Solution1() as BaseConfiguration with aspect of TimerAspect(), LoggingAspect("DEBUG")

Program1 with application of firstApp
```

#### **Complex Multi-Aspect Scenarios**
```ek9
// Multiple aspects on single component:
register Solution1() as BaseConfiguration with aspect of TimerAspect(SystemClock()), LoggingAspect("DEBUG")

// Different aspect combinations across applications:
firstApp: TimerAspect + LoggingAspect("DEBUG")  
secondApp: LoggingAspect("WARN")
thirdApp: TimerAspect only
```

### Synthesis Complexity Scale

This is **orders of magnitude more complex** than synthetic operators:

1. **Synthetic Operators**: Generate ~5-10 methods per class
2. **Full EK9 Synthesis**: Generate component registries + injection wiring + aspect weaving + application binding + generic instantiation + trait delegation

**Current Approach Cannot Scale** to this level of complexity.

---

## Proposed Solution: Apply EK9's Proven Pattern

### Main Coordinator Pattern (Like ExpressionsListener)

```java
/**
 * Single coordinator for all IR generation, following proven EK9 pattern.
 * Orchestrates focused helpers like ExpressionsListener does for phase 3.
 */
public class IRGenerationCoordinator extends AbstractEK9PhaseListener {
    
    // Focused helpers - each does ONE thing well
    private final SyntheticMethodIRHelper syntheticMethodHelper;
    private final InjectionPointIRHelper injectionHelper;  
    private final AspectWeavingIRHelper aspectHelper;
    private final ApplicationBindingIRHelper appBindingHelper;
    private final GenericInstantiationIRHelper genericHelper;
    private final TraitDelegationIRHelper traitHelper;
    
    // Shared state management (like SymbolsAndScopes)
    private final IRGenerationContext irContext;
    
    @Override
    public void exitClassDeclaration(EK9Parser.ClassDeclarationContext ctx) {
        var classSymbol = (ClassSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
        
        // Clean orchestration - each helper handles its domain
        if (hasSyntheticMethods(classSymbol)) {
            syntheticMethodHelper.generateFor(classSymbol, irContext);
        }
        
        if (hasInjectionPoints(classSymbol)) {
            injectionHelper.generateFor(classSymbol, irContext);
        }
        
        if (hasAspects(classSymbol)) {
            aspectHelper.generateFor(classSymbol, irContext);
        }
        
        // etc. - clean, obvious, scalable
    }
    
    @Override  
    public void exitApplicationDeclaration(EK9Parser.ApplicationDeclarationContext ctx) {
        var appSymbol = (ApplicationSymbol) symbolsAndScopes.getRecordedSymbol(ctx);
        appBindingHelper.generateFor(appSymbol, irContext);
    }
}
```

### Shared IR State Management (Like SymbolsAndScopes)

```java
/**
 * Centralized state management for IR generation.
 * Replaces parameter threading with clean context management.
 */
public class IRGenerationContext {
    private final IRScopeStack irScopeStack;  // ⭐ Reuse proven transient stack pattern!
    private final IRInstructionBuilder instructionBuilder;
    private final MemoryManagementStrategy memoryStrategy;
    private final DebugInfoManager debugManager;
    
    // Clean context management (no parameter threading)
    public void enterIRScope(String scopeId, DebugInfo debugInfo) {
        irScopeStack.push(new IRScopeFrame(scopeId, debugInfo));
    }
    
    public void exitIRScope() {
        irScopeStack.pop();
    }
    
    // Context-aware decisions
    public String currentScopeId() {
        return irScopeStack.peek().scopeId;
    }
    
    public DebugInfo currentDebugInfo() {
        return irScopeStack.peek().debugInfo;
    }
    
    public boolean needsResultVariable(String returnType) {
        return !"org.ek9.lang::Void".equals(returnType) && irScopeStack.peek().hasLeftHandSide;
    }
}
```

### Single-Responsibility IR Helpers (Like CallOrError)

```java
/**
 * Handles ONLY synthetic method IR generation.
 * Uses context for all state - no parameter threading.
 */
public class SyntheticMethodIRHelper {
    
    public void generateFor(ClassSymbol classSymbol, IRGenerationContext context) {
        // Use existing markers - don't duplicate logic!
        for (var method : classSymbol.getAllMethods()) {
            if (method.isSynthetic()) {  // ⭐ Already marked in earlier phases!
                generateSyntheticMethodIR(method, context);
            }
        }
    }
    
    private void generateSyntheticMethodIR(MethodSymbol method, IRGenerationContext context) {
        context.enterIRScope(method.getScopeName(), method.getDebugInfo());
        
        switch (method.getName()) {
            case "_string" -> generateStringOperatorIR(method, context);
            case "_cmp" -> generateComparisonOperatorIR(method, context);
            case "<", "<=", ">=", ">" -> generateDerivedComparisonIR(method, context);
            // etc.
        }
        
        context.exitIRScope();
    }
}

/**
 * Handles ONLY dependency injection IR generation.
 */
public class InjectionPointIRHelper {
    
    public void generateFor(ClassSymbol classSymbol, IRGenerationContext context) {
        for (var field : classSymbol.getProperties()) {
            if (isInjectionPoint(field)) {  // ⭐ Check for ! suffix
                generateInjectionWiringIR(field, context);
            }
        }
    }
    
    private boolean isInjectionPoint(ISymbol field) {
        // Check for ! suffix marking injection point
        return field.getName().endsWith("!");
    }
}

/**
 * Handles ONLY AOP aspect weaving IR generation.
 */
public class AspectWeavingIRHelper {
    
    public void generateFor(ClassSymbol classSymbol, IRGenerationContext context) {
        var aspects = getAspectsForClass(classSymbol);  // From application registry
        if (!aspects.isEmpty()) {
            generateAspectWeavingIR(classSymbol, aspects, context);
        }
    }
}
```

---

## Implementation Strategy

### Phase 1: Create Coordinator & Shared Context

**1.1 Implement IRGenerationCoordinator**
- Create main coordinator following `ExpressionsListener` pattern
- Implement basic orchestration for existing functionality
- Maintain backward compatibility with current generators

**1.2 Implement IRGenerationContext**  
- Create shared state management with transient stack pattern
- Replace parameter threading with context-aware methods
- Implement `enterIRScope`/`exitIRScope` stack management

**1.3 Create IRInstructionBuilder**
- Stateful builder that uses context automatically
- Handles variable creation, memory management, debug info consistently
- Replaces manual `VariableDetails`/`BasicDetails` construction

### Phase 2: Extract Focused Helpers

**2.1 Create Single-Responsibility Helpers**
- `SyntheticMethodIRHelper` - use existing `isSynthetic()` markers
- `InjectionPointIRHelper` - handle `!` suffix injection points
- `AspectWeavingIRHelper` - implement AOP weaving patterns
- `ApplicationBindingIRHelper` - component registry and wiring

**2.2 Implement Helper Integration**
- Each helper uses shared `IRGenerationContext`
- Clear interfaces and single responsibilities
- Testable in isolation

### Phase 3: Migrate Existing Generators

**3.1 Consolidate Similar Generators**
- Merge `BinaryOperationGenerator` + `BinaryOperationGeneratorWithProcessor`
- Merge `UnaryOperationGenerator` + `UnaryOperationGeneratorWithProcessor`  
- Combine assignment-related generators

**3.2 Apply New Pattern to Existing Logic**
- Convert existing generators to use `IRGenerationContext`
- Eliminate parameter threading
- Use consistent state management

### Phase 4: Validation & Optimization

**4.1 Comprehensive Testing**
- Ensure all existing IR tests pass
- Add tests for new synthesis capabilities
- Validate debug info and memory management consistency

**4.2 Performance Validation**
- Measure compilation performance impact
- Optimize hot paths in new architecture
- Validate memory usage improvements

---

## Expected Benefits

### ✅ Architectural Consistency

**Phase 7 will feel like phases 1-6**:
- Clear coordinator with focused helpers
- Shared state management with transient stack
- Single responsibility principle applied consistently
- Obvious, scalable patterns

### ✅ Maintainability & Correctness

**Clean Code**:
- No more parameter threading hell
- Consistent scope and debug info management  
- No hardcoded values like `"temp_scope"`
- Clear separation of concerns

**Correct Behavior**:
- Proper memory management patterns applied uniformly
- Debug info points to correct source locations
- Scope consistency across all generated IR

### ✅ Scalability for EK9's Future

**Easy Extension**:
- Add new synthesis types by creating focused helpers
- Extend existing helpers without affecting others
- Clear integration points for new language features

**Complex Feature Support**:
- Ready for full AOP implementation
- Supports complex dependency injection patterns
- Handles generic instantiation complexity
- Manages trait delegation and conflict resolution

---

## Technical Deep Dives

### Memory Management Consistency

#### Current Problems
```java
// Inconsistent patterns across generators:
instructions.add(MemoryInstr.retain(tempVar, debugInfo));           // Manual
instructions.add(ScopeInstr.register(tempVar, new BasicDetails(...))); // Manual + wrong scope

// vs

variableMemoryManagement.apply(() -> instructions, variableDetails); // Correct pattern
```

#### Proposed Solution  
```java
public class IRInstructionBuilder {
    private final IRGenerationContext context;
    
    public String createTempVariable() {
        var temp = context.generateTempName();
        // Automatic memory management using context
        addMemoryManagement(temp, context.currentScopeId(), context.currentDebugInfo());
        return temp;
    }
    
    private void addMemoryManagement(String variable, String scopeId, DebugInfo debugInfo) {
        instructions.add(MemoryInstr.retain(variable, debugInfo));
        instructions.add(ScopeInstr.register(variable, new BasicDetails(scopeId, debugInfo)));
    }
}
```

### Debug Information Handling

#### Fixed Issues (From Parameter Promotion Work)
- ✅ Promotion calls now use call site debug info (line 49) instead of method definition (line 18)
- ✅ Memory management instructions use consistent debug positions  
- ✅ Scope naming uses actual scope ID instead of hardcoded `"temp_scope"`

#### Generalized Solution
```java
public class DebugInfoManager {
    private final IRGenerationContext context;
    
    public DebugInfo getCallSiteDebugInfo(EK9Parser.CallContext callCtx) {
        // Always use call site, never method definition
        return debugInfoCreator.apply(new Ek9Token(callCtx.start));
    }
    
    public DebugInfo getCurrentDebugInfo() {
        // Use context stack for current debug position
        return context.irScopeStack.peek().debugInfo;
    }
}
```

### Scope Management Hierarchy

#### IRScopeStack Implementation
```java
public class IRScopeStack {
    private final Deque<IRScopeFrame> scopeStack = new ArrayDeque<>();
    
    public void push(IRScopeFrame frame) {
        scopeStack.push(frame);
    }
    
    public IRScopeFrame pop() {
        return scopeStack.pop();
    }
    
    public IRScopeFrame peek() {
        return scopeStack.peek();  
    }
    
    // Navigate up scope hierarchy (like successful ScopeStack)
    public Optional<IRScopeFrame> findScopeOfType(IRScopeType type) {
        return scopeStack.stream()
            .filter(frame -> frame.scopeType == type)
            .findFirst();
    }
}

public record IRScopeFrame(
    String scopeId,
    DebugInfo debugInfo, 
    IRScopeType scopeType,
    boolean hasLeftHandSide  // For result variable decisions
) {}
```

### Synthesis Template Patterns

#### Reusable Generation Templates
```java
public abstract class SynthesisTemplate {
    public abstract List<IRInstr> synthesize(ClassSymbol classSymbol, IRGenerationContext context);
}

public class StringOperatorTemplate extends SynthesisTemplate {
    @Override
    public List<IRInstr> synthesize(ClassSymbol classSymbol, IRGenerationContext context) {
        var instructions = new ArrayList<IRInstr>();
        
        // Template: call _string() on all properties + super._string()
        for (var property : classSymbol.getProperties()) {
            // Generate: property._string() call
            // Use context for scope, debug info, memory management
        }
        
        // Generate: super._string() call if has superclass
        if (classSymbol.getSuperClass().isPresent()) {
            // Generate super call
        }
        
        return instructions;
    }
}
```

---

## Migration Checklist

### ✅ Preparation Phase
- [ ] Create `IRGenerationContext` with transient scope stack
- [ ] Implement `IRInstructionBuilder` with automatic state management  
- [ ] Create base `IRHelper` interface for focused helpers
- [ ] Implement `IRGenerationCoordinator` skeleton

### ✅ Helper Extraction Phase  
- [ ] Extract `SyntheticMethodIRHelper` using existing `isSynthetic()` markers
- [ ] Extract `InjectionPointIRHelper` for `!` suffix handling
- [ ] Extract `AspectWeavingIRHelper` for AOP patterns
- [ ] Extract `ApplicationBindingIRHelper` for component wiring

### ✅ Generator Migration Phase
- [ ] Migrate `CallInstrGenerator` to use `IRGenerationContext`
- [ ] Consolidate binary/unary operation generators
- [ ] Merge assignment-related generators  
- [ ] Convert all generators to coordinator pattern

### ✅ Validation Phase
- [ ] All existing IR tests pass with new architecture
- [ ] Debug positioning is consistent across all generated IR
- [ ] Memory management patterns are applied uniformly
- [ ] No hardcoded scope names or debug positions remain

### ✅ Optimization Phase
- [ ] Measure compilation performance impact
- [ ] Optimize context stack operations
- [ ] Validate memory usage improvements
- [ ] Clean up deprecated generator classes

---

## Reference Materials

### Key Files Demonstrating Problems
- `CallDetailsBuilder.java` - Parameter threading, hardcoded scopes
- `ParameterPromotionProcessor.java` - Debug info and scope issues (now fixed)
- `StmtInstrGenerator.java` - Unnecessary result variable creation
- `functionParameterPromotion.ek9` - IR correctness validation

### Successful Architectural Examples
- `DefinitionListener.java` - Main coordinator pattern
- `ExpressionsListener.java` - Helper orchestration  
- `SymbolsAndScopes.java` - Shared state management
- `ScopeStack.java` - Transient stack pattern
- `AbstractEK9PhaseListener.java` - Base listener structure

### Complex EK9 Features Requiring Synthesis
- `ComponentsAndAspects.ek9` - AOP, injection, application binding
- `JustTraits.ek9` - Trait delegation patterns
- `ExtendsClass.ek9` - Inheritance and synthetic methods
- `UseOfBuiltInGenerics*.ek9` - Generic instantiation requirements

### Architectural Diagrams

#### Current Architecture (Problem)
```
IRGenerator
├── 67 generators with unclear relationships
├── Parameter threading through all methods  
├── Inconsistent state management
├── Duplicated patterns across generators
└── No clear coordination or architectural pattern
```

#### Proposed Architecture (Solution)
```  
IRGenerationCoordinator (like ExpressionsListener)
├── IRGenerationContext (like SymbolsAndScopes)
│   └── IRScopeStack (like ScopeStack)
├── SyntheticMethodIRHelper (focused responsibility)
├── InjectionPointIRHelper (focused responsibility)  
├── AspectWeavingIRHelper (focused responsibility)
├── ApplicationBindingIRHelper (focused responsibility)
└── IRInstructionBuilder (stateful, context-aware)
```

---

## EK9 Default Operators Specification

### Complete List of Supported Default Operators

Based on analysis of `OperatorFactory.java` (lines 76-88 and 98-108), EK9 supports exactly **11 default operators**. These are the ONLY operators that can use the `default` keyword:

#### **Comparison Operators (Derived from `<=>`)**
All comparison operators require the aggregate to have a working `<=>` operator implementation:

- **`<=>`** - Primary comparison operator (returns Integer: -1, 0, 1)
- **`==`** - Equality (derived from `<=> == 0`, returns Boolean)
- **`<>`** - Inequality (derived from `<=> != 0`, returns Boolean)  
- **`<`** - Less than (derived from `<=> < 0`, returns Boolean)
- **`<=`** - Less or equal (derived from `<=> <= 0`, returns Boolean)
- **`>`** - Greater than (derived from `<=> > 0`, returns Boolean)
- **`>=`** - Greater or equal (derived from `<=> >= 0`, returns Boolean)

#### **Unary Operators (No Parameters)**
These operators work independently and don't require other operators:

- **`?`** - IsSet check (returns Boolean)
- **`$`** - String representation (returns String)
- **`$$`** - JSON representation (returns Json)
- **`#?`** - Hashcode (returns Integer)

### Implementation Requirements

Each default operator creates a synthetic `MethodSymbol` with specific characteristics:

```java
// From OperatorFactory.getDefaultOperator()
if (rtn != null) {
    rtn.putSquirrelledData(DEFAULTED, "TRUE");  // Marked as defaulted
    rtn.setSourceToken(aggregate.getSourceToken()); // Uses aggregate's token
    rtn.setSynthetic(true);                     // Marked as synthetic
}
```

### Map-Based Synthetic Generator Pattern

This specification enables a clean map-based approach for synthetic IR generation:

```java
private Map<String, BiFunction<MethodSymbol, AggregateSymbol, List<IRInstr>>> initializeDefaultOperators() {
    return Map.of(
        // Foundation comparison operator
        "<=>", this::generateComparisonOperator,
        
        // Derived comparison operators
        "==",  this::generateEqualityOperator,        // Uses <=> internally
        "<>",  this::generateInequalityOperator,      // Uses <=> internally
        "<",   this::generateLessThanOperator,        // Uses <=> internally  
        "<=",  this::generateLessEqualOperator,       // Uses <=> internally
        ">",   this::generateGreaterThanOperator,     // Uses <=> internally
        ">=",  this::generateGreaterEqualOperator,    // Uses <=> internally
        
        // Independent unary operators
        "?",   this::generateIsSetOperator,           // Property/field isSet checks
        "$",   this::generateStringOperator,          // Calls _string on all properties
        "$$",  this::generateToJsonOperator,          // JSON serialization
        "#?",  this::generateHashcodeOperator         // Hash of all properties
    );
}
```

### Stack-Based Context for Generation

Each synthetic operator generator method receives:
- **MethodSymbol**: The synthetic method symbol (marked with `isSynthetic()`)
- **AggregateSymbol**: The containing record/class with properties to process
- **IRGenerationStack**: Current scope, debug info, and instruction context

### Validation Integration

The specification integrates with existing validation in:
- **`CheckAndPopulateOperator.java`**: Validates operator can be defaulted
- **`DefaultOperatorsOrError.java`**: Ensures required dependencies exist
- **`RetrieveDefaultedOperators.java`**: Extracts defaulted operators from aggregates

This provides the complete foundation for implementing map-based synthetic operator generation in the stack-based IR generation architecture.

---

## Conclusion

EK9's Phase 7 IR generation currently **violates the architectural patterns** that make the rest of the compiler successful. By applying EK9's own **proven coordinator + focused helpers pattern** with **transient stack state management**, we can:

1. **Achieve architectural consistency** with phases 1-6
2. **Eliminate complexity** from parameter threading and duplicated patterns  
3. **Enable scalable synthesis** for EK9's unique language features
4. **Maintain correctness** with proper scope, debug, and memory management

The refactoring will transform Phase 7 from a **chaotic collection of generators** into a **clean, maintainable architecture** that feels natural to EK9 developers and can scale to support the language's full synthesis requirements.

**Next Steps**: Begin implementation with the coordinator and shared context, then incrementally migrate existing functionality to the new pattern while maintaining full backward compatibility.