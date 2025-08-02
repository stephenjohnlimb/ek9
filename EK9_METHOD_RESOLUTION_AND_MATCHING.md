# EK9 Method Resolution and Matching Mechanism

## Overview

This document provides a comprehensive analysis of EK9's method resolution and matching system, explaining how the compiler resolves method calls, handles ambiguity, and manages the special role of the 'Any' type in method dispatch.

## Key Components

### 1. SymbolMatcher - Core Algorithm

**Location**: `compiler-main/src/main/java/org/ek9lang/compiler/support/SymbolMatcher.java`

The `SymbolMatcher` class implements EK9's cost-based method matching algorithm with these fundamental constants:

```java
public static final double ZERO_COST = 0.0;        // Perfect match
public static final double SUPER_COST = 0.05;      // Match via superclass
public static final double TRAIT_COST = 0.10;      // Match via trait
public static final double COERCION_COST = 0.5;    // Type coercion required
public static final double HIGH_COST = 20.0;       // Any match (last resort)
public static final double INVALID_COST = -1.0;    // No match possible
```

### 2. Method Resolution Process

**Core Algorithm**: `getPercentageMethodMatch()`

1. **Method Name Validation** - Exact match required
2. **Return Type Compatibility** - If specified, must be compatible
3. **Parameter Cost Calculation** - Via `getCostOfParameterMatch()`
4. **Percentage Calculation** - `100.0 - totalCost`

### 3. Parameter Matching Costs

**Cost Hierarchy** (lowest to highest):
- **Exact match**: `0.0` - Same type
- **Superclass match**: `0.05` per inheritance level
- **Trait match**: `0.10` per trait level
- **Type coercion**: `0.5` - Promotion/casting required
- **Any match**: `20.0` - Universal fallback (last resort)
- **Invalid**: `-1.0` - No match possible

### 4. Ambiguity Detection

**Location**: `compiler-main/src/main/java/org/ek9lang/compiler/search/MethodSymbolSearchResult.java`

```java
public boolean isAmbiguous() {
    if (results.size() > 1) {
        final var firstPercentage = results.get(0).getPercentageMatch();
        final var secondPercentage = results.get(1).getPercentageMatch();
        // Ambiguous if within 0.001 tolerance
        return Math.abs(firstPercentage - secondPercentage) < 0.001;
    }
    return false;
}
```

**Key Point**: Methods with percentage scores within **0.001** of each other are considered ambiguous.

## The 'Any' Type in Method Resolution

### 1. What is 'Any'?

**Location**: `ek9-lang/src/main/java/org/ek9/lang/Any.java`

- **Universal base interface** - Root of EK9's type hierarchy
- **Built-in dispatcher** - Provides fundamental operations (`?`, `==`, `$`, `#?`, `<=>`)
- **Interface-based** - Unlike Java's Object class, implemented as interface
- **Compiler integration** - Baked into compiler via `AnyTypeSymbol`

### 2. Any Type Cost Assignment

**Location**: `compiler-main/src/main/java/org/ek9lang/compiler/symbols/AggregateSymbol.java`

```java
// Lines 447-449
if (theSuperAggregate.getFullyQualifiedName().equals("org.ek9.lang::Any")) {
    return SymbolMatcher.HIGH_COST; // 20.0
}
```

**Critical Design Decision**: 'Any' matches get `HIGH_COST (20.0)`, making them the **least preferred** option.

### 3. Method Resolution Priority

1. **Exact type match** (cost 0.0) - Highest priority
2. **Direct superclass match** (cost 0.05)
3. **Trait match** (cost 0.10)
4. **Type coercion/promotion** (cost 0.5)
5. **'Any' match** (cost 20.0) - **Last resort, lowest priority**

### 4. Polymorphic Method Pattern

Many EK9 built-in types use this pattern:

```java
// Specific type method - preferred
@Ek9Operator("operator == as pure -> arg as List <- rtn as Boolean?")
public Boolean _eq(List arg) { /* optimized logic */ }

// Any fallback method - universal compatibility
@Ek9Operator("operator == as pure -> arg as Any <- rtn as Boolean?") 
public Boolean _eq(Any arg) { /* generic fallback */ }
```

**Result**: No ambiguity due to massive cost difference (0.0 vs 20.0).

## Real-World Examples

### Example 1: workarea.ek9 Ambiguity Case

**Class Hierarchy**: `C1` → `C2` → `C3`

**Methods**:
```ek9
methodA(C1, C2)  // Method A
methodA(C2, C1)  // Method B
```

**Ambiguous Call**: `methodA(c2, c3)` where `c2` is `C2`, `c3` is `C3`

**Cost Calculation**:
- **Method A**: `c2→C1` (0.05) + `c3→C2` (0.05) = **0.10 total**
- **Method B**: `c2→C2` (0.0) + `c3→C1` (0.10) = **0.10 total**

**Result**: Both methods have identical costs → **AMBIGUOUS**

### Example 2: Any Type Resolution

**Methods**:
```ek9
methodCall(String arg)  // Method A
methodCall(Any arg)     // Method B
```

**Call**: `methodCall("hello")`

**Cost Calculation**:
- **Method A**: String→String (0.0) = **100.0% match**
- **Method B**: String→Any (20.0) = **80.0% match**

**Result**: Method A wins decisively → **No ambiguity**

### Example 3: Type Hierarchy with Any

**Class Hierarchy**: `C1` → `C2`

**Methods**:
```ek9
method(C1 arg)   // Method A
method(Any arg)  // Method B
```

**Call**: `method(c2Instance)`

**Cost Calculation**:
- **Method A**: C2→C1 (0.05) = **99.95% match**
- **Method B**: C2→Any (20.0) = **80.0% match**

**Result**: Method A wins → **No ambiguity**

## Key Implementation Files

### Core Resolution Engine
- **`SymbolMatcher.java`** - Cost-based matching algorithm
- **`MethodSymbolSearchResult.java`** - Ambiguity detection logic
- **`AggregateSymbol.java`** - Type hierarchy cost calculation

### Compilation Phase Integration
- **`SymbolResolution.java`** - FULL_RESOLUTION phase coordinator
- **`ResolveMethodOrError.java`** - Method resolution with error handling
- **`CheckConflictingMethods.java`** - Static conflict detection

### Symbol System
- **`AnyTypeSymbol.java`** - 'Any' type representation
- **`MethodSymbol.java`** - Method metadata and signatures
- **`ISymbol.java`** - Base symbol interface hierarchy

## Design Principles

### 1. Predictable Resolution
Cost-based matching ensures consistent, deterministic method resolution across complex inheritance scenarios.

### 2. Developer Clarity
When ambiguity occurs, developers receive specific error messages showing conflicting methods and exact costs.

### 3. Performance Optimization
Specific type matches are strongly preferred, avoiding unnecessary boxing/unboxing and maintaining performance.

### 4. Flexible Typing
The 'Any' type enables gradual typing - developers can write specific methods for performance and 'Any' methods for flexibility.

### 5. Ambiguity Prevention
The 20.0 cost penalty for 'Any' matches prevents false ambiguities while maintaining universal compatibility.

## Integration with Compilation Pipeline

### Phase 7: FULL_RESOLUTION
Method resolution occurs during the `FULL_RESOLUTION` phase of EK9's 22-phase compilation pipeline:

1. **Symbol Collection** - Gather all candidate methods
2. **Cost Calculation** - Calculate percentage match for each candidate
3. **Ambiguity Detection** - Check for methods within 0.001 tolerance
4. **Error Reporting** - Emit `METHOD_AMBIGUOUS` errors when needed
5. **Resolution** - Select best match or report ambiguity

### Multi-threading Support
The resolution system supports parallel processing across multiple source files while maintaining thread safety.

### Language Server Integration
Method resolution integrates with EK9's Language Server Protocol implementation, providing real-time method resolution feedback in IDEs.

## Advanced Scenarios

### 1. Generic Type Resolution
The system handles parameterized generic types through dynamic instantiation during the resolution phase.

### 2. Trait Diamond Inheritance
Special handling for complex trait hierarchies where multiple inheritance paths exist.

### 3. Promotion and Coercion
Support for EK9's `#^` promotion operator and automatic type coercions within the cost framework.

### 4. Service and Component Resolution
Extended method resolution for EK9's service and component architecture patterns.

## Testing and Validation

### Test Location
`compiler-main/src/test/resources/examples/parseButFailCompile/workingarea/workarea.ek9`

### Key Test Cases
- Method overloading with type hierarchies
- Ambiguity detection and error reporting
- Cost calculation verification
- Resolution strategy validation

### Expected Behaviors
- `@Error: FULL_RESOLUTION: METHOD_AMBIGUOUS` for truly ambiguous calls
- Clear resolution for cases with cost differences
- Proper error messages with method signatures and line numbers

## Future Considerations

### 1. LLVM Backend Support
Method resolution will extend to LLVM IR generation while maintaining the same cost-based algorithm.

### 2. Performance Optimizations
Potential caching of method resolution results for frequently called methods.

### 3. Enhanced Diagnostics
Possible expansion of error messages to show exact cost calculations for better developer understanding.

### 4. IDE Integration Enhancements
Real-time method resolution hints and suggestions based on the cost algorithm.

## Summary

EK9's method resolution system represents a sophisticated balance between flexibility and predictability. The cost-based algorithm with special handling for the 'Any' type enables:

- **Universal compatibility** without ambiguity
- **Performance optimization** through specific type preferences  
- **Developer control** over method selection
- **Predictable behavior** in complex inheritance scenarios
- **Clear error reporting** when true ambiguity exists

This design allows EK9 to support both precise type-safe programming and flexible dynamic dispatch while maintaining clear, understandable resolution rules for developers.