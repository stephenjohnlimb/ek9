# EK9 IR Optimization Strategy

## Overview

This document outlines the optimization strategy for EK9's Intermediate Representation (IR), focusing on redundancy elimination and code quality improvements before target code generation. The EK9 compiler includes dedicated optimization phases (Phase 9: IROptimisation) designed to handle these optimizations systematically.

## Development Approach

### Phase 1: Initial Implementation (Current)
- **Focus**: Get IR generation working correctly
- **Optimization**: None - generate naive but correct IR
- **Priority**: Correctness over efficiency
- **Status**: IR generation implemented, no optimization passes active

### Phase 2: Code Generation Implementation (Next)
- **Focus**: Implement JVM bytecode and LLVM code generation
- **Optimization**: Still minimal - ensure generated code runs correctly
- **Priority**: Working code generation for both targets

### Phase 3: Optimization Implementation (Future)
- **Focus**: Implement sophisticated IR optimization passes
- **Optimization**: Full optimization pipeline activated
- **Priority**: Performance and code quality improvements

## Identified Optimization Opportunities

### Common Redundancy Patterns

**1. Redundant Variable Loads**
```
// Current IR generates:
_temp1 = LOAD var1    // First expression
_temp7 = LOAD var1    // Second expression (redundant)
```

**2. Redundant Method Calls**
```
// Current IR generates:
_temp3 = CALL _temp1._isSet()    // First call
_temp9 = CALL _temp7._isSet()    // Second call (redundant)
```

**3. Duplicate Expression Evaluation**
Multiple coalescing operators on the same variables create redundant computation patterns.

### Optimization Difficulty Assessment

**Identification Difficulty**: ⭐⭐⭐⭐⭐ (Easy)
- Clear patterns in SSA-form IR
- Straightforward data flow analysis
- Well-established compiler optimization techniques

**Refactoring Difficulty**: ⭐⭐⭐⭐⚬ (Moderate)
- SSA form maintenance required
- Cross-basic-block dependencies
- Temporary variable renaming needed

## Optimization Strategies

### 1. Common Subexpression Elimination (CSE)

**Purpose**: Eliminate redundant computations within and across basic blocks.

**Implementation Approach**:
```java
public class CommonSubexpressionEliminator {
  private Map<String, IRInstruction> availableExpressions = new HashMap<>();
  
  public void eliminateCommonSubexpressions(BasicBlock block) {
    for (IRInstruction instruction : block.getInstructions()) {
      String expressionKey = generateExpressionKey(instruction);
      
      if (availableExpressions.containsKey(expressionKey)) {
        // Replace with existing result
        IRInstruction existing = availableExpressions.get(expressionKey);
        replaceAllUses(instruction.getResult(), existing.getResult());
        block.remove(instruction);
      } else {
        availableExpressions.put(expressionKey, instruction);
      }
    }
  }
}
```

**Example Transformation**:
```
// Before CSE:
_temp1 = LOAD var1
_temp3 = CALL _temp1._isSet()
_temp7 = LOAD var1              // Redundant
_temp9 = CALL _temp7._isSet()   // Redundant

// After CSE:
_temp1 = LOAD var1
_temp3 = CALL _temp1._isSet()
// _temp7 removed
// _temp9 replaced with _temp3
```

### 2. Load/Store Optimization

**Purpose**: Eliminate redundant memory operations.

**Key Concepts**:
- Track last load result for each variable
- Reuse load results when variable unchanged
- Invalidate cached loads when variable modified

**Implementation Pattern**:
```java
public class LoadStoreOptimizer {
  public void optimizeLoads(List<BasicBlock> blocks) {
    Map<String, String> lastLoadResult = new HashMap<>();
    
    for (BasicBlock block : blocks) {
      for (IRInstruction instruction : block.getInstructions()) {
        if (instruction.getOpcode() == LOAD) {
          String variable = instruction.getOperands().get(0);
          
          if (lastLoadResult.containsKey(variable)) {
            // Reuse previous load result
            String previousResult = lastLoadResult.get(variable);
            replaceAllUses(instruction.getResult(), previousResult);
            block.remove(instruction);
          } else {
            lastLoadResult.put(variable, instruction.getResult());
          }
        } else if (modifiesMemory(instruction)) {
          // Invalidate affected loads
          invalidateLoads(instruction, lastLoadResult);
        }
      }
    }
  }
}
```

### 3. Pure Method Call Memoization

**Purpose**: Cache results of pure method calls (methods with no side effects).

**EK9 Pure Methods**:
- `_isSet()` - Returns object's set state
- `_lt()`, `_gt()`, `_eq()` - Comparison methods
- `_hashcode()` - Hash code calculation
- Many others marked with `as pure` in EK9

**Implementation Approach**:
```java
public class PureMethodOptimizer {
  private Map<String, String> memoizedResults = new HashMap<>();
  
  public void optimizePureMethods(BasicBlock block) {
    for (IRInstruction instruction : block.getInstructions()) {
      if (instruction.getOpcode() == CALL && isPureMethod(instruction)) {
        String callSignature = generateCallSignature(instruction);
        
        if (memoizedResults.containsKey(callSignature)) {
          // Reuse memoized result
          String cachedResult = memoizedResults.get(callSignature);
          replaceAllUses(instruction.getResult(), cachedResult);
          block.remove(instruction);
        } else {
          memoizedResults.put(callSignature, instruction.getResult());
        }
      }
    }
  }
}
```

### 4. Dead Code Elimination

**Purpose**: Remove unused instructions and unreachable code.

**Implementation**: Standard dead code elimination algorithms using def-use analysis.

### 5. Basic Block Optimization

**Purpose**: Merge basic blocks and simplify control flow.

**Opportunities**:
- Merge sequential basic blocks
- Eliminate empty basic blocks
- Simplify unconditional branches

## Optimization Pipeline Architecture

### Integration with Existing Phases

The EK9 compiler already includes optimization phases:

**Phase 9: IRAnalysis**
- Currently: Basic IR structure analysis
- Future: Static analysis for optimization opportunities

**Phase 10: IROptimisation** 
- Currently: Placeholder/minimal optimization
- Future: Full optimization pipeline implementation

### Proposed Optimization Pipeline

```java
public class IROptimizationPipeline {
  public void optimize(IRModule irModule) {
    // Pass 1: Local optimizations within basic blocks
    new LocalOptimizer().optimize(irModule);
    
    // Pass 2: Common Subexpression Elimination
    new CommonSubexpressionEliminator().optimize(irModule);
    
    // Pass 3: Load/Store Optimization
    new LoadStoreOptimizer().optimize(irModule);
    
    // Pass 4: Pure Method Call Optimization
    new PureMethodOptimizer().optimize(irModule);
    
    // Pass 5: Dead Code Elimination
    new DeadCodeEliminator().optimize(irModule);
    
    // Pass 6: Control Flow Simplification
    new ControlFlowSimplifier().optimize(irModule);
    
    // Pass 7: Basic Block Merging
    new BasicBlockMerger().optimize(irModule);
    
    // Pass 8: Final cleanup pass
    new FinalCleanupPass().optimize(irModule);
  }
}
```

## Example Optimization Result

### Before Optimization
```
// Expression 1: lesserThan <- var1 <? var2
BasicBlock: _coalescing_block_1
  _temp1 = LOAD var1
  _temp2 = LOAD var2
  _temp3 = CALL _temp1._isSet()
  BRANCH_FALSE _temp3, _var1_unset_1
  _temp4 = CALL _temp2._isSet()
  // ... rest of expression 1

// Expression 2: greaterThan <- var1 >? var2  
BasicBlock: _coalescing_block_2
  _temp7 = LOAD var1              // REDUNDANT
  _temp8 = LOAD var2              // REDUNDANT  
  _temp9 = CALL _temp7._isSet()   // REDUNDANT
  BRANCH_FALSE _temp9, _var1_unset_2
  _temp10 = CALL _temp8._isSet()  // REDUNDANT
  // ... rest of expression 2
```

### After Optimization
```
// Combined and optimized expressions
BasicBlock: _coalescing_optimized_1
  _temp1 = LOAD var1          // Load once
  _temp2 = LOAD var2          // Load once
  _temp3 = CALL _temp1._isSet()    // Call once
  _temp4 = CALL _temp2._isSet()    // Call once
  
  // Expression 1: lesserThan <- var1 <? var2
  BRANCH_FALSE _temp3, _var1_unset_1
  BRANCH_FALSE _temp4, _var2_unset_1
  // ... optimized expression 1 logic
  STORE lesserThan, _result1
  
  // Expression 2: greaterThan <- var1 >? var2
  // Reuse _temp3 and _temp4 - no redundant operations
  BRANCH_FALSE _temp3, _var1_unset_2
  BRANCH_FALSE _temp4, _var2_unset_2
  // ... optimized expression 2 logic
  STORE greaterThan, _result2
```

**Optimization Savings**:
- Eliminated 2 redundant LOAD instructions
- Eliminated 2 redundant method calls
- Merged basic blocks for better cache locality
- Reduced temporary variable count

## Implementation Timeline

### Phase 1: Foundation (Current - Completed)
- ✅ IR generation working correctly
- ✅ Basic IR instruction set implemented
- ✅ SSA form generation
- ✅ Target-agnostic design

### Phase 2: Code Generation (Next Priority)
- 🔄 Implement JVM bytecode generation with ASM
- 🔄 Implement LLVM IR generation  
- 🔄 Validate generated code correctness
- 🔄 Basic integration testing

### Phase 3: Optimization Implementation (Future)
- ⏳ Implement optimization pass infrastructure
- ⏳ Common subexpression elimination
- ⏳ Load/store optimization
- ⏳ Pure method call optimization
- ⏳ Dead code elimination
- ⏳ Control flow simplification

### Phase 4: Advanced Optimizations (Long-term)
- ⏳ Loop optimizations
- ⏳ Inlining optimizations  
- ⏳ Constant propagation and folding
- ⏳ Register allocation optimization
- ⏳ Profile-guided optimization

## Testing Strategy

### Optimization Correctness
- **Regression Testing**: Ensure optimized code produces same results
- **Performance Testing**: Measure optimization effectiveness
- **Edge Case Testing**: Handle complex control flow and expression patterns

### Validation Approach
- Compare unoptimized vs optimized IR output
- Validate against EK9 language semantics
- Cross-reference with target platform behavior

## Future Considerations

### Target-Specific Optimizations
While IR optimization is target-agnostic, some optimizations may be more beneficial for specific targets:
- **JVM**: Method call optimization, object allocation patterns
- **LLVM**: Register allocation hints, memory access patterns

### Profile-Guided Optimization
Future enhancement could include:
- Runtime profiling integration
- Hot path identification
- Adaptive optimization strategies

## Related Documentation

- **EK9_COALESCING_OPERATORS_IR_GENERATION.md** - Detailed IR generation patterns
- **EK9_IR_AND_CODE_GENERATION.md** - IR structure and code generation
- **EK9_Compiler_Architecture_and_Design.md** - Overall compiler architecture
- **Phase 9 & 10 Implementation** - IRAnalysis and IROptimisation phases

## Summary

The EK9 compiler's optimization strategy follows a phased approach:
1. **Correctness First**: Ensure IR generation works properly
2. **Code Generation**: Implement target code generation  
3. **Optimization**: Add sophisticated optimization passes

The identified redundancy patterns in coalescing operator IR generation represent common optimization opportunities that will be addressed systematically in Phase 3. The existing compiler architecture already includes the necessary phases (IRAnalysis, IROptimisation) to implement these optimizations effectively.