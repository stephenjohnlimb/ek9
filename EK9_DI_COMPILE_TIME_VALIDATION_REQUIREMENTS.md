# EK9 Dependency Injection Compile-Time Validation Requirements

This document defines the essential requirements for EK9's revolutionary compile-time dependency injection validation system that guarantees zero runtime DI failures while maintaining architectural quality and explicit developer control.

## Overview

EK9's dependency injection system achieves unprecedented safety through compile-time validation based on four core requirements that together eliminate all possible dependency injection failure modes. This represents the first programming language with **mathematically guaranteed dependency injection safety**.

**Revolutionary Promise**: "Write valid EK9 DI code once, run anywhere without DI failures, forever."

## Foundation Requirement: Bottom-Up Dependency Analysis

### The Enabling Mechanism

All validation requirements depend on a foundational capability: **complete transitive dependency analysis** that propagates injection requirements from leaf constructs up through the call hierarchy to Programs.

```ek9
// Dependency flow: Direct → Indirect → Transitive → Program
DatabaseService()
  logger as Logger!           // DIRECT: DatabaseService needs Logger

BusinessLogic()
  db as DatabaseService!      // INDIRECT: BusinessLogic needs DatabaseService
                             // TRANSITIVE: BusinessLogic needs DatabaseService + Logger

OrderProgram() with application of MyApp
  logic as BusinessLogic!     // PROGRAM: Must validate {BusinessLogic, DatabaseService, Logger}
```

### Implementation Strategy: ANTLR Enter/Exit Pattern

**Bottom-Up Propagation Through AST Traversal**:

```java
class InjectionDependencyListener extends EK9BaseListener {
  // Transient analysis data
  private final Map<ParseTree, Set<TypeSymbol>> nodeRequirements = new HashMap<>();
  private final Map<Symbol, Set<TypeSymbol>> constructRequirements = new HashMap<>();

  @Override
  public void enterVariableOnlyDeclaration(EK9Parser.VariableOnlyDeclarationContext ctx) {
    if (hasInjectionSyntax(ctx)) {  // `as Type!`
      final var injectionType = extractInjectionType(ctx);
      addNodeRequirement(ctx, injectionType);  // Record at leaf level
    }
  }

  @Override
  public void exitMethodDeclaration(EK9Parser.MethodDeclarationContext ctx) {
    // Collect all DI requirements from child nodes
    final var childRequirements = collectChildRequirements(ctx);

    // Propagate to construct level
    final var constructSymbol = getConstructSymbol(ctx);
    addConstructRequirements(constructSymbol, childRequirements);

    // Propagate to parent node
    addNodeRequirements(ctx, childRequirements);
  }

  @Override
  public void enterMethodCall(EK9Parser.MethodCallContext ctx) {
    // When calling a method, inherit its construct's requirements
    final var calledMethod = resolveMethod(ctx);
    final var calleeRequirements = getConstructRequirements(calledMethod.getParentConstruct());

    // Propagate callee's requirements to current context
    addNodeRequirements(ctx, calleeRequirements);
  }
}
```

**Key Benefits**:
- **Natural ANTLR flow**: Works with AST traversal, not against it
- **Single pass analysis**: O(n) complexity where n = AST nodes
- **Transient data**: No permanent memory overhead
- **Immediate availability**: Results ready for validation

### Transitive Dependency Calculation

**Result of Bottom-Up Analysis**:
```java
// Complete transitive requirements at construct level
Map<Symbol, Set<TypeSymbol>> constructRequirements = {
  DatabaseService → {Logger},
  BusinessLogic → {DatabaseService, Logger},
  OrderController → {BusinessLogic, DatabaseService, Logger},
  OrderProgram → {OrderController, BusinessLogic, DatabaseService, Logger}
}
```

**Why Essential**: Every validation requirement depends on knowing the complete transitive dependency set. Without this foundation, the three core validations are impossible to implement.

## Core Requirement 1: Circular Dependency Prevention

### The Problem: Architectural Disasters

Circular dependencies create **unfixable runtime deadlocks** that require complex proxy mechanisms to resolve, leading to unpredictable behavior and performance overhead.

**Spring's Reality**:
```java
// Runtime proxy creation, lazy initialization, unpredictable failures
@Service
class UserService {
  @Autowired private OrderService orderService;  // Circular dependency
}

@Service
class OrderService {
  @Autowired private UserService userService;    // Creates proxy nightmare
}
```

### EK9's Solution: Hard Compile-Time Failure

```ek9
// COMPILE ERROR - No exceptions, no workarounds
BadApp
  register UserService() as UserService      // UserService needs OrderService
  register OrderService() as OrderService    // OrderService needs UserService

// Error: "Circular dependency detected: UserService → OrderService → UserService"
// Solution: Redesign architecture - extract shared concerns
```

### Implementation Algorithm

```java
void detectCircularDependencies(ApplicationSymbol application) {
  final var dependencyGraph = new DirectedGraph<TypeSymbol>();

  // Build dependency edges for all registered types
  for (final var registration : application.getRegistrations()) {
    final var implType = registration.getImplementationType();
    final var requirements = getTransitiveRequirements(implType);

    requirements.forEach(dep -> dependencyGraph.addEdge(implType, dep));
  }

  // Find cycles using DFS
  final var cycles = dependencyGraph.findCycles();

  if (!cycles.isEmpty()) {
    for (final var cycle : cycles) {
      errorListener.semanticError(
        application.getSourceToken(),
        "Circular dependency in Application",
        "Circular dependency: " + cycle.stream()
            .map(TypeSymbol::getFriendlyName)
            .collect(joining(" → "))
      );
    }
  }
}
```

### Strategic Benefits

**Architectural Quality Enforcement**:
- **Forces layer separation**: Can't create artificial cycles between business layers
- **Promotes loose coupling**: Circular dependencies often indicate tight coupling
- **Eliminates proxy complexity**: No runtime proxy creation needed
- **Predictable performance**: Zero lazy initialization overhead

**Error Prevention**:
- **No runtime deadlocks**: Impossible to create circular initialization
- **No proxy failures**: Eliminates Spring's proxy creation edge cases
- **No hidden dependencies**: All dependencies must be explicit and acyclic

## Core Requirement 2: Completeness Validation

### The Problem: Runtime Null Pointer Exceptions

Traditional DI frameworks discover missing dependencies at runtime, leading to application failures in production.

**Spring's Reality**:
```java
@Service
class DatabaseService {
  @Autowired private Logger logger;  // NullPointerException if Logger not configured
}
```

### EK9's Solution: Compile-Time Completeness Guarantee

```ek9
// COMPILE ERROR - Missing dependency detected before runtime
IncompleteApp
  register DatabaseService() as Database  // DatabaseService needs Logger
  // Missing: Logger registration

MyProgram() with application of IncompleteApp
  db as Database!  // Would fail at runtime

// Error: "Program 'MyProgram' requires 'Logger' injection but Application 'IncompleteApp' doesn't provide it"
// Solution: "Add: register FileLogger() as Logger"
```

### Implementation Algorithm

```java
void validateApplicationCompleteness(ProgramSymbol program) {
  final var application = program.getAssociatedApplication();
  if (application == null) {
    // No Application required if no injection used
    return;
  }

  // 1. Get complete transitive requirements for Program
  final var programRequirements = getTransitiveRequirements(program);

  // 2. Get types provided by Application
  final var providedTypes = application.getRegisteredTypes();

  // 3. Find missing dependencies
  final var missingTypes = difference(programRequirements, providedTypes);

  if (!missingTypes.isEmpty()) {
    reportMissingDependencies(program, application, missingTypes);
  }
}

private void reportMissingDependencies(ProgramSymbol program,
                                     ApplicationSymbol application,
                                     Set<TypeSymbol> missingTypes) {
  for (final var missingType : missingTypes) {
    // Find exact injection sites for precise error reporting
    final var injectionSites = findInjectionSites(program, missingType);

    for (final var site : injectionSites) {
      errorListener.semanticError(
        site.getSourceToken(),
        "Missing dependency registration",
        String.format(
          "Program '%s' requires injection of type '%s' but Application '%s' does not register this type.\n" +
          "Solution: Add 'register SomeImplementation() as %s' to Application",
          program.getName(),
          missingType.getFriendlyName(),
          application.getName(),
          missingType.getFriendlyName()
        )
      );
    }
  }
}
```

### Application Self-Sufficiency Validation

**Extended Requirement**: Applications must satisfy their own internal dependencies.

```ek9
// Application types that use injection must be self-sufficient
SelfSufficientApp
  register ConfigService() as Config         // Leaf - no dependencies
  register Logger(config) as Logger          // Uses Config ✓
  register DatabaseService(logger) as Database // Uses Logger ✓

// vs.

IncompleteApp
  register DatabaseService() as Database     // Needs Logger but Logger not registered
  // Error: "Application registers DatabaseService but doesn't provide required Logger"
```

### Strategic Benefits

**Zero Runtime Failures**:
- **Impossible missing dependencies**: All requirements validated at compile time
- **No configuration errors**: Applications guaranteed to be self-sufficient
- **Predictable deployment**: No environment-specific DI failures

**Developer Experience**:
- **Precise error locations**: Shows exact injection sites that need dependencies
- **Actionable solutions**: Tells developer exactly what to add
- **Early feedback**: Catches configuration errors at development time

## Core Requirement 3: Ordering Validation

### The Problem: Initialization Order Dependencies

Traditional DI frameworks use complex algorithms to determine initialization order, or require developers to manually manage lifecycle with annotations.

**Spring's Complexity**:
```java
@Service
class DatabaseService {
  @Autowired private Logger logger;      // Needs Logger during construction

  @PostConstruct                          // Manual lifecycle management
  public void init() {
    logger.info("Database initialized");   // Hope Logger is ready
  }
}
```

### EK9's Solution: Explicit Sequential Ordering

```ek9
// COMPILE ERROR - Wrong initialization order
WrongOrderApp
  register DatabaseService() as Database  // Error: DatabaseService needs Logger
  register FileLogger() as Logger        // But Logger not available yet!

// Error: "Cannot register DatabaseService: requires Logger but Logger not registered yet"
// Solution: "Move Logger registration before DatabaseService"

// COMPILE SUCCESS - Correct order
CorrectOrderApp
  register FileLogger() as Logger        // Leaf first (no dependencies)
  register DatabaseService() as Database // Can safely use Logger (already registered)
```

### Implementation Algorithm

```java
void validateApplicationOrdering(ApplicationSymbol application) {
  final var registrations = application.getRegistrations(); // In declaration order
  final var availableTypes = new HashSet<TypeSymbol>();

  for (final var registration : registrations) {
    final var implType = registration.getImplementationType();
    final var requirements = getTransitiveRequirements(implType);

    // Check if all requirements are already available
    final var missingDeps = difference(requirements, availableTypes);

    if (!missingDeps.isEmpty()) {
      reportOrderingError(registration, missingDeps, availableTypes);
      return; // Stop on first error for clarity
    }

    // Add this type to available set for subsequent registrations
    availableTypes.add(registration.getInterfaceType());
  }
}

private void reportOrderingError(Registration registration,
                               Set<TypeSymbol> missingDeps,
                               Set<TypeSymbol> availableTypes) {
  errorListener.semanticError(
    registration.getSourceToken(),
    "Dependency ordering error",
    String.format(
      "Cannot register %s as %s: requires %s but only %s are available.\n" +
      "Solution: Move the registration of %s before this line.",
      registration.getImplementationType().getFriendlyName(),
      registration.getInterfaceType().getFriendlyName(),
      missingDeps.stream().map(TypeSymbol::getFriendlyName).collect(joining(", ")),
      availableTypes.isEmpty() ? "no types" :
        availableTypes.stream().map(TypeSymbol::getFriendlyName).collect(joining(", ")),
      missingDeps.stream().map(TypeSymbol::getFriendlyName).collect(joining(", "))
    )
  );
}
```

### Strategic Benefits

**Self-Documenting Architecture**:
```ek9
// Registration order tells the complete dependency story
EnterpriseApp
  register DatabaseConfig("prod.yaml") as Config     // Foundation layer
  register ConnectionPool(config) as Database        // Infrastructure layer
  register AuditLogger(config) as Logger            // Cross-cutting layer
  register UserService(database, logger) as UserService // Business layer
  register OrderService(database, logger, userService) as OrderService // Business layer
  register WebController(orderService, userService) as Controller // Presentation layer
```

**Simplified Backend Implementation**:
```java
// No complex ordering algorithms needed!
public static void initializeApplication(ApplicationDefinition app) {
  for (Registration reg : app.getRegistrations()) {
    // All dependencies guaranteed available already
    Object instance = createInstance(reg.getImplementationType());
    GlobalApplication.register(reg.getInterfaceType(), instance);
  }
}
```

**Performance Benefits**:
- **O(n) initialization**: No topological sorting algorithms
- **Zero lazy initialization**: Everything available when needed
- **Predictable timing**: No deferred construction complexity

## Complete Validation Framework

### The Three Requirements Form a Minimal Complete Set

**Coverage Analysis - All Possible DI Failure Modes**:
1. ✅ **Missing dependencies** → Requirement 2 (Completeness)
2. ✅ **Circular dependencies** → Requirement 1 (Circularity)
3. ✅ **Initialization ordering** → Requirement 3 (Ordering)
4. ✅ **Transitive dependencies** → Foundation (Bottom-up analysis)

**No other failure modes exist** for a compile-time validated DI system.

### Irreducible Requirements

Each requirement addresses distinct failure modes and cannot be merged or eliminated:

**Requirement 1 (Circularity)**:
- **Unique concern**: Architectural cycle detection
- **Cannot merge with others**: Different graph algorithms and error types
- **Cannot eliminate**: Would allow runtime deadlocks

**Requirement 2 (Completeness)**:
- **Unique concern**: Availability validation
- **Cannot merge with others**: Set difference operations vs graph algorithms
- **Cannot eliminate**: Would allow runtime null pointer exceptions

**Requirement 3 (Ordering)**:
- **Unique concern**: Sequential initialization validation
- **Cannot merge with others**: Different from availability (temporal vs spatial)
- **Cannot eliminate**: Would require complex initialization algorithms

### Validation Success Criteria

**For any Program + Application pair, guarantee**:
1. ✅ **No circular dependencies** in Application-registered types
2. ✅ **All Program requirements satisfied** by Application registrations
3. ✅ **Application registration order** satisfies dependency initialization requirements

**Result**: **Mathematical guarantee of zero runtime dependency injection failures.**

## Implementation Roadmap

### Phase 1: Foundation Infrastructure (Months 1-2)
- [ ] Implement `as Type!` syntax parsing and AST integration
- [ ] Create InjectionDependencyListener with enter/exit pattern
- [ ] Build transitive dependency propagation mechanism
- [ ] Add construct-level requirement storage system

### Phase 2: Core Validation Algorithms (Months 2-3)
- [ ] Implement circular dependency detection with DFS cycle finding
- [ ] Create completeness validation with set difference operations
- [ ] Build sequential ordering validation algorithm
- [ ] Integrate all three validations into unified framework

### Phase 3: Error Reporting and Developer Experience (Months 3-4)
- [ ] Design precise error messages with exact source locations
- [ ] Create actionable solution suggestions for each error type
- [ ] Implement error recovery and multi-error reporting
- [ ] Add IDE integration support for validation feedback

### Phase 4: Backend Integration (Months 4-5)
- [ ] Generate IR metadata with validation results
- [ ] Implement simplified backend initialization (no ordering needed)
- [ ] Create GlobalApplication registry with guaranteed safety
- [ ] Add performance optimizations for validated systems

### Phase 5: Advanced Features and Testing (Months 5-6)
- [ ] Support generic type injection validation
- [ ] Implement interface hierarchy matching
- [ ] Create comprehensive test suite covering all edge cases
- [ ] Add performance benchmarks vs traditional DI frameworks

## Strategic Impact and Competitive Advantage

### Revolutionary Capabilities Achieved

**EK9 becomes the first language with**:
- **Mathematical DI safety guarantees**: Zero runtime failures possible
- **Architectural quality enforcement**: No circular dependencies allowed
- **Self-documenting Applications**: Registration order shows architecture
- **Zero-overhead DI**: No proxies, lazy initialization, or complex algorithms

### Competitive Positioning

**vs. Spring Framework**:
- ❌ Spring: Runtime configuration errors, circular dependency proxies, complex lifecycle
- ✅ EK9: Compile-time validation, explicit architecture, simple initialization

**vs. Google Guice**:
- ❌ Guice: Binding configuration errors, runtime injection failures
- ✅ EK9: Guaranteed completeness, type-safe registration

**vs. Java CDI**:
- ❌ CDI: Complex scoping, runtime wiring failures, annotation complexity
- ✅ EK9: Simple explicit registration, compile-time safety

**vs. Other Languages**:
- ❌ Most languages: No built-in DI or framework-based solutions
- ✅ EK9: Language-integrated DI with mathematical safety guarantees

### Market Differentiation

**EK9's Unique Value Proposition**:
> "The only programming language where dependency injection is mathematically guaranteed to work correctly, with enterprise-grade capabilities and zero runtime overhead."

**Enterprise Benefits**:
- **Eliminated production DI failures**: Impossible with compile-time validation
- **Reduced development time**: No debugging circular dependencies or missing configurations
- **Architectural clarity**: Applications self-document their dependency structure
- **Performance advantages**: No reflection, proxies, or lazy initialization overhead

## Conclusion

The three core requirements (Circularity Prevention, Completeness Validation, Ordering Validation) built on the foundation of Bottom-Up Dependency Analysis create a **mathematically complete validation framework** for dependency injection.

This framework eliminates **every possible dependency injection failure mode** while maintaining:
- **Developer simplicity**: Explicit, readable Application definitions
- **Architectural quality**: Forces good design practices through hard constraints
- **Performance excellence**: Zero runtime overhead compared to framework-based solutions
- **Enterprise reliability**: Mathematical guarantee of correct behavior

EK9's dependency injection system represents a **fundamental advancement** in programming language design, providing enterprise-grade capabilities with unprecedented safety guarantees and developer experience benefits.

The explicit nature of the three requirements—combined with their irreducible and complete coverage of all failure modes—positions EK9 as offering **"Dependency Injection Done Right"** for the first time in programming language history.