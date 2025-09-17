# EK9 Dependency Injection Implementation Proposals

This document outlines specific implementation proposals for EK9's revolutionary compile-time dependency injection validation system, building on the architectural framework defined in `EK9_DI_COMPILE_TIME_VALIDATION_REQUIREMENTS.md`.

## Overview

Based on our analysis of EK9's requirements for mathematically guaranteed dependency injection safety, this document proposes concrete implementation strategies for the three core validation requirements and supporting infrastructure.

## Foundation Implementation: Bottom-Up Dependency Analysis

### Proposal 1: InjectionDependencyListener Integration

**Implementation Strategy**: Extend the existing ANTLR listener pattern used throughout the EK9 compiler.

**Key Classes to Create**:
```java
// Phase 6 integration - during FULL_RESOLUTION
public class InjectionDependencyListener extends EK9BaseListener {
  private final Map<ParseTree, Set<TypeSymbol>> nodeRequirements = new HashMap<>();
  private final Map<Symbol, Set<TypeSymbol>> constructRequirements = new HashMap<>();
  private final CompilableProgram compilableProgram;
  private final ErrorListener errorListener;
}
```

**Integration Point**: Add to `FullResolvePhase.java` alongside existing listeners:
```java
// In FullResolvePhase.java - after existing listeners
final var injectionListener = new InjectionDependencyListener(compilableProgram, errorListener);
walker.walk(injectionListener, moduleCtx);
```

**Rationale**:
- Leverages existing compiler infrastructure
- Phase 6 is optimal - symbols resolved but before IR generation
- Natural integration with current listener pattern

### Proposal 2: Injection Syntax Detection

**Implementation Strategy**: Detect `as Type!` syntax during AST traversal.

**Key Detection Logic**:
```java
@Override
public void enterVariableOnlyDeclaration(EK9Parser.VariableOnlyDeclarationContext ctx) {
  if (hasInjectionSyntax(ctx)) {
    final var injectionType = extractInjectionType(ctx);
    addNodeRequirement(ctx, injectionType);

    // Store for construct-level aggregation
    final var parentConstruct = findParentConstruct(ctx);
    addConstructRequirement(parentConstruct, injectionType);
  }
}

private boolean hasInjectionSyntax(EK9Parser.VariableOnlyDeclarationContext ctx) {
  return ctx.typeDef() != null &&
         ctx.typeDef().getText().endsWith("!"); // DI syntax marker
}
```

**Benefits**:
- Precise syntax recognition
- Immediate requirement capture
- Clear separation of concerns

## Core Requirement 1: Circular Dependency Prevention

### Proposal 3: Graph-Based Cycle Detection

**Implementation Strategy**: Use directed graph with DFS-based cycle detection during Application validation.

**Key Implementation**:
```java
public class CircularDependencyValidator {

  public void validateApplication(ApplicationSymbol application) {
    final var dependencyGraph = buildDependencyGraph(application);
    final var cycles = detectCycles(dependencyGraph);

    if (!cycles.isEmpty()) {
      reportCircularDependencies(application, cycles);
    }
  }

  private DirectedGraph<TypeSymbol> buildDependencyGraph(ApplicationSymbol application) {
    final var graph = new DirectedGraph<TypeSymbol>();

    for (final var registration : application.getRegistrations()) {
      final var implType = registration.getImplementationType();
      final var requirements = getTransitiveRequirements(implType);

      requirements.forEach(dep -> graph.addEdge(implType, dep));
    }

    return graph;
  }

  private List<List<TypeSymbol>> detectCycles(DirectedGraph<TypeSymbol> graph) {
    return graph.findCycles(); // Standard DFS-based algorithm
  }
}
```

**Integration Point**: Add to `PostResolutionChecksPhase.java`:
```java
// In PostResolutionChecksPhase.java
final var circularValidator = new CircularDependencyValidator(errorListener);
circularValidator.validateApplication(applicationSymbol);
```

**Error Reporting**:
```java
private void reportCircularDependencies(ApplicationSymbol application,
                                       List<List<TypeSymbol>> cycles) {
  for (final var cycle : cycles) {
    final var cycleDescription = cycle.stream()
        .map(TypeSymbol::getFriendlyName)
        .collect(joining(" → "));

    errorListener.semanticError(
      application.getSourceToken(),
      "Circular dependency in Application",
      String.format("Circular dependency detected: %s → %s",
                   cycleDescription, cycle.get(0).getFriendlyName())
    );
  }
}
```

## Core Requirement 2: Completeness Validation

### Proposal 4: Set-Based Completeness Checking

**Implementation Strategy**: Compare Program requirements against Application provisions using set operations.

**Key Implementation**:
```java
public class CompletenessValidator {

  public void validateProgramCompleteness(ProgramSymbol program) {
    final var application = program.getAssociatedApplication();
    if (application == null) return; // No DI required

    final var programRequirements = getTransitiveRequirements(program);
    final var providedTypes = getProvidedTypes(application);
    final var missingTypes = Sets.difference(programRequirements, providedTypes);

    if (!missingTypes.isEmpty()) {
      reportMissingDependencies(program, application, missingTypes);
    }
  }

  private Set<TypeSymbol> getProvidedTypes(ApplicationSymbol application) {
    return application.getRegistrations().stream()
        .map(Registration::getInterfaceType)
        .collect(toSet());
  }

  private void reportMissingDependencies(ProgramSymbol program,
                                        ApplicationSymbol application,
                                        Set<TypeSymbol> missingTypes) {
    for (final var missingType : missingTypes) {
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
}
```

**Advanced Feature - Application Self-Sufficiency**:
```java
public void validateApplicationSelfSufficiency(ApplicationSymbol application) {
  for (final var registration : application.getRegistrations()) {
    final var implType = registration.getImplementationType();
    final var requirements = getTransitiveRequirements(implType);
    final var availableTypes = getProvidedTypes(application);

    final var missingDeps = Sets.difference(requirements, availableTypes);
    if (!missingDeps.isEmpty()) {
      reportApplicationIncomplete(registration, missingDeps);
    }
  }
}
```

## Core Requirement 3: Ordering Validation

### Proposal 5: Sequential Order Validation

**Implementation Strategy**: Validate registration order during Application parsing using available-types tracking.

**Key Implementation**:
```java
public class OrderingValidator {

  public void validateRegistrationOrder(ApplicationSymbol application) {
    final var registrations = application.getRegistrations(); // In declaration order
    final var availableTypes = new HashSet<TypeSymbol>();

    for (final var registration : registrations) {
      final var implType = registration.getImplementationType();
      final var requirements = getTransitiveRequirements(implType);

      final var missingDeps = Sets.difference(requirements, availableTypes);

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
}
```

## Integration Proposal: Unified Validation Framework

### Proposal 6: DependencyInjectionValidator Coordinator

**Implementation Strategy**: Create a unified validator that orchestrates all three validation requirements.

**Key Coordinator Class**:
```java
public class DependencyInjectionValidator {
  private final CircularDependencyValidator circularValidator;
  private final CompletenessValidator completenessValidator;
  private final OrderingValidator orderingValidator;
  private final InjectionDependencyListener dependencyListener;

  public DependencyInjectionValidator(ErrorListener errorListener) {
    this.circularValidator = new CircularDependencyValidator(errorListener);
    this.completenessValidator = new CompletenessValidator(errorListener);
    this.orderingValidator = new OrderingValidator(errorListener);
    this.dependencyListener = new InjectionDependencyListener(errorListener);
  }

  public void validateCompilationUnit(CompilableProgram compilableProgram) {
    // Phase 1: Collect dependency requirements (during AST traversal)
    collectDependencyRequirements(compilableProgram);

    // Phase 2: Validate all Applications
    final var applications = compilableProgram.getAllApplications();
    for (final var application : applications) {
      validateApplication(application);
    }

    // Phase 3: Validate all Programs with Applications
    final var programs = compilableProgram.getAllPrograms();
    for (final var program : programs) {
      if (program.hasAssociatedApplication()) {
        validateProgramCompleteness(program);
      }
    }
  }

  private void validateApplication(ApplicationSymbol application) {
    // All three validations for each Application
    orderingValidator.validateRegistrationOrder(application);
    circularValidator.validateApplication(application);
    completenessValidator.validateApplicationSelfSufficiency(application);
  }
}
```

**Phase Integration**:
```java
// In PostResolutionChecksPhase.java
@Override
public boolean doApply(final CompilableProgram compilableProgram,
                      final Workspace workspace) {

  final var diValidator = new DependencyInjectionValidator(errorListener);
  diValidator.validateCompilationUnit(compilableProgram);

  return !errorListener.hasErrors();
}
```

## Implementation Roadmap

### Phase 1: Foundation Infrastructure (Sprint 1-2)
- [ ] **Week 1**: Create `InjectionDependencyListener` with basic AST traversal
- [ ] **Week 1**: Implement injection syntax detection (`as Type!`)
- [ ] **Week 2**: Build transitive dependency propagation mechanism
- [ ] **Week 2**: Add construct-level requirement storage system
- [ ] **Week 2**: Integration with Phase 6 (FULL_RESOLUTION)

**Deliverable**: Bottom-up dependency analysis working for simple cases

**Success Criteria**: Can detect and propagate injection requirements from leaf nodes to Program level

### Phase 2: Core Validation Implementation (Sprint 3-4)
- [ ] **Week 3**: Implement `CircularDependencyValidator` with graph-based cycle detection
- [ ] **Week 3**: Create `OrderingValidator` with sequential availability checking
- [ ] **Week 4**: Build `CompletenessValidator` with set-based missing dependency detection
- [ ] **Week 4**: Implement precise error reporting with source locations and solutions

**Deliverable**: All three core validations functional independently

**Success Criteria**: Each validator catches its specific error types with actionable error messages

### Phase 3: Integration and Error Experience (Sprint 5)
- [ ] **Week 5**: Create `DependencyInjectionValidator` coordinator
- [ ] **Week 5**: Integration with `PostResolutionChecksPhase`
- [ ] **Week 5**: Comprehensive error message testing and refinement
- [ ] **Week 5**: Multi-error reporting (don't stop on first error)

**Deliverable**: Unified validation framework integrated into compiler pipeline

**Success Criteria**: Compilation fails predictably for all DI error scenarios with helpful messages

### Phase 4: Backend Integration Preparation (Sprint 6)
- [ ] **Week 6**: Add IR metadata generation with validation results
- [ ] **Week 6**: Create simplified backend initialization (no ordering algorithms needed)
- [ ] **Week 6**: Design `GlobalApplication` registry interface
- [ ] **Week 6**: Performance optimization for validated systems

**Deliverable**: Backend-ready DI metadata and simplified runtime architecture

**Success Criteria**: Validation results enable zero-overhead backend DI implementation

### Phase 5: Advanced Features and Testing (Sprint 7-8)
- [ ] **Week 7**: Support generic type injection validation (`List<String>!`)
- [ ] **Week 7**: Implement interface hierarchy matching for DI
- [ ] **Week 8**: Create comprehensive test suite covering all edge cases
- [ ] **Week 8**: Add performance benchmarks vs traditional DI frameworks

**Deliverable**: Production-ready DI system with advanced type support

**Success Criteria**: Handles complex generic injection scenarios and shows measurable performance advantages

## Testing Strategy

### Unit Testing Approach
**Test Structure**: Follow EK9's existing pattern with dedicated test classes for each validator.

```java
class CircularDependencyValidatorTest {
  @ParameterizedTest
  @MethodSource("circularDependencyScenarios")
  void testCircularDependencyDetection(String ek9Code, List<String> expectedCycles) {
    // Test each validation requirement in isolation
  }
}

class CompletenessValidatorTest {
  @ParameterizedTest
  @MethodSource("missingDependencyScenarios")
  void testMissingDependencyDetection(String ek9Code, List<String> expectedMissing) {
    // Test completeness validation scenarios
  }
}
```

### Integration Testing
**EK9 Source Testing**: Create comprehensive `.ek9` files in `src/test/resources/badExamples/dependencyInjection/`

```ek9
// circularDependency.ek9 - should fail compilation
BadCircularApp
  register UserService() as UserService      // UserService needs OrderService
  register OrderService() as OrderService    // OrderService needs UserService

TestProgram() with application of BadCircularApp
  service as UserService!
```

### Error Message Testing
**Validate Error Quality**: Ensure error messages are actionable and precise.

```java
@Test
void testCircularDependencyErrorMessage() {
  final var result = compileEk9File("circularDependency.ek9");

  assertThat(result.hasErrors()).isTrue();
  assertThat(result.getErrors())
      .anyMatch(error -> error.getMessage().contains("Circular dependency: UserService → OrderService → UserService"));
}
```

## Performance Considerations

### Complexity Analysis
- **Bottom-up analysis**: O(n) where n = AST nodes
- **Circular detection**: O(V + E) where V = types, E = dependencies
- **Completeness checking**: O(k) where k = registered types
- **Ordering validation**: O(m) where m = registrations

**Total**: O(n + V + E + k + m) - linear complexity for realistic dependency graphs

### Memory Optimization
- **Transient data structures**: Clear analysis maps after validation
- **Lazy evaluation**: Only analyze Applications/Programs that use injection
- **Incremental compilation**: Cache validation results for unchanged modules

### Caching Strategy
```java
// Cache validation results for incremental compilation
public class DependencyValidationCache {
  private final Map<ApplicationSymbol, ValidationResult> applicationResults = new ConcurrentHashMap<>();
  private final Map<ProgramSymbol, ValidationResult> programResults = new ConcurrentHashMap<>();

  public boolean isValidationCurrent(ApplicationSymbol app) {
    return applicationResults.containsKey(app) &&
           !app.hasChangedSince(applicationResults.get(app).timestamp);
  }
}
```

## Risk Assessment and Mitigation

### Implementation Risks

**Risk 1: Performance Impact on Compilation**
- **Mitigation**: Implement lazy evaluation - only analyze when DI is actually used
- **Fallback**: Make validation optional with compiler flag during development

**Risk 2: Complex Error Message Complexity**
- **Mitigation**: Extensive user testing of error messages with iterative refinement
- **Fallback**: Simple error messages first, enhance incrementally

**Risk 3: Integration Complexity with Existing Phases**
- **Mitigation**: Minimal changes to existing phases, self-contained validation
- **Fallback**: Separate validation pass if integration proves problematic

### Technical Debt Prevention

**Code Quality Measures**:
- Comprehensive unit tests for each validator (>95% coverage)
- Integration tests with realistic EK9 DI scenarios
- Performance benchmarks to prevent regression
- Clear separation of concerns between validators

**Documentation Requirements**:
- JavaDoc for all public APIs
- Architecture decision records for major design choices
- Performance benchmarks and optimization guides
- Migration guide for existing EK9 code

## Success Metrics

### Functional Success
- [ ] **Zero false positives**: Valid DI code compiles successfully
- [ ] **Zero false negatives**: All DI errors caught at compile time
- [ ] **Actionable errors**: All error messages include specific solutions
- [ ] **Complete coverage**: All three validation requirements implemented

### Performance Success
- [ ] **<5% compilation overhead**: DI validation adds minimal compilation time
- [ ] **Linear complexity**: Validation scales linearly with code size
- [ ] **Memory efficient**: Transient analysis with minimal permanent overhead

### Developer Experience Success
- [ ] **Clear error messages**: Developers can fix DI errors without documentation
- [ ] **Precise error locations**: Error messages point to exact source locations
- [ ] **Solution suggestions**: Error messages suggest specific fixes
- [ ] **Fast feedback**: Validation errors shown immediately during development

## Conclusion

This implementation proposal provides a concrete roadmap for delivering EK9's revolutionary compile-time dependency injection validation system. The phased approach ensures incremental delivery of value while maintaining quality and performance standards.

The proposed architecture leverages EK9's existing compiler infrastructure while adding minimal complexity. The three-tier validation framework provides mathematical guarantees of DI safety while maintaining developer productivity through clear error messages and actionable solutions.

**Next Steps**:
1. Review and approve implementation approach
2. Begin Phase 1 implementation with foundation infrastructure
3. Establish testing framework and success metrics
4. Iterate on developer experience based on early feedback

This proposal positions EK9 to deliver on its promise of "Mathematical Dependency Injection Safety" while maintaining the language's core principles of simplicity, safety, and performance.