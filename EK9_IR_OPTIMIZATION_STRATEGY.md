# EK9 IR Optimization Strategy

## Overview

This document outlines the optimization strategy for EK9's Intermediate Representation (IR), focusing on redundancy elimination and code quality improvements before target code generation. The EK9 compiler includes dedicated optimization phases (Phase 9: IROptimisation) designed to handle these optimizations systematically.

**Related Documentation:**
- **`EK9_IR_AND_CODE_GENERATION.md`** - IR structure, code generation, and Phase 12 status
- **`EK9_Compiler_Architecture_and_Design.md`** - Complete compiler architecture specification
- **`EK9_COMPILER_PHASES.md`** - Detailed compiler phase implementation

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

## IR Structure Readiness Assessment

### Current IR Architecture (2025-10-23)

The EK9 IR structure is **optimization-ready without requiring a separate "optimized IR" representation**. Analysis of the typed object architecture confirms the current implementation supports all standard optimization passes without structural modifications.

#### IR Structure Strengths for Optimization

**1. Control Flow Graph is Native**
```java
// BasicBlockInstr has built-in CFG edges
List<BasicBlockInstr> predecessors;  // Incoming control flow
List<BasicBlockInstr> successors;    // Outgoing control flow
```
- CFG is a first-class IR structure, not computed separately
- Enables efficient data flow analysis (reaching definitions, liveness)
- Graph traversal algorithms work directly on IR objects

**2. Hybrid Mutability Model**
```java
// IRInstr: Instructions are immutable
private final IROpcode opcode;        // Cannot change opcode
private final String result;          // Cannot change result variable
private final List<String> operands;  // Defensive copy on access

// BasicBlockInstr: Containers are mutable
private final List<IRInstr> instructions = new ArrayList<>();  // Can add/remove
```
**Optimization pattern**: Create new instructions, replace in blocks (not modify in-place)

**3. SSA-Like Variable Naming**
```
// Example IR showing single-assignment temporaries
_temp1 = CALL Stdout.<init>()         // Single assignment to _temp1
_temp2 = CALL List.<init>()           // Single assignment to _temp2
_temp3 = LOAD items                   // Single assignment to _temp3
```
- **Temporaries** (`_temp*`) follow strict single-assignment form
- **Named variables** (`stdout`, `items`) use LOAD/STORE pattern (similar to LLVM's alloca/load/store)
- Enables straightforward def-use chain construction

**4. Extensible Metadata System**
```java
// IRInstr.java line 36
private EscapeMetaDataDetails escapeMetaData;  // Mutable field

// Line 111: Setter for optimization passes
public void setEscapeMetaData(final EscapeMetaDataDetails escapeMetaData) {
    this.escapeMetaData = escapeMetaData;
}
```
- Annotations don't require IR structure changes
- Classic "annotate-then-transform" optimization approach
- Analysis passes attach metadata, transformation passes consume it

#### Required Enhancements for Full Optimization Support

**Add to `BasicBlockInstr.java`** (3 methods, ~30 lines of code):

```java
/**
 * Replace all instructions in this block (for optimization passes).
 * Used by transformation passes that rebuild instruction sequences.
 */
public BasicBlockInstr setInstructions(final List<IRInstr> newInstructions) {
    instructions.clear();
    instructions.addAll(newInstructions);
    return this;
}

/**
 * Replace instruction at given index (for peephole optimization).
 * Used by local optimization passes that replace single instructions.
 */
public BasicBlockInstr replaceInstruction(final int index, final IRInstr newInstr) {
    AssertValue.checkRange("Index", index, 0, instructions.size());
    instructions.set(index, newInstr);
    return this;
}

/**
 * Remove instruction at given index (for dead code elimination).
 * Used by optimization passes that eliminate redundant instructions.
 */
public BasicBlockInstr removeInstruction(final int index) {
    AssertValue.checkRange("Index", index, 0, instructions.size());
    instructions.remove(index);
    return this;
}
```

**Impact**: These 3 methods unlock full optimization capability without changing IR architecture.

#### Optimization Architecture: Single IR, Multiple Passes

```
Current IR (in-memory typed objects)
       ↓
┌──────────────────────────────────────┐
│  Analysis Phase (Read-Only)         │
│  ─────────────────────────────────  │
│  • Build def-use chains             │
│  • Compute liveness analysis        │
│  • Detect loops and headers         │
│  • Build dominator tree (optional)  │
│  • Identify pure functions          │
└──────────────────────────────────────┘
       ↓ (Metadata attached to existing IR)
┌──────────────────────────────────────┐
│  Transformation Phase (Mutating)    │
│  ─────────────────────────────────  │
│  • Common Subexpression Elimination │
│  • Dead Temp Elimination            │
│  • Loop-Invariant Code Motion       │
│  • Copy Propagation                 │
│  • Dead Code Elimination            │
│  • Constant Folding                 │
└──────────────────────────────────────┘
       ↓
Optimized IR (same structures, better instruction sequences)
       ↓
Phase 10: Code Generation (JVM/LLVM)
```

**Key Insight**: Auxiliary analysis structures (def-use chains, liveness info, dominator trees) **reference** the IR but don't **replace** it. This follows LLVM's proven architecture.

### Comparison with Industry Standard Architectures

| Compiler | IR Levels | Optimization Approach | EK9 Alignment |
|----------|-----------|----------------------|---------------|
| **LLVM** | 1 (LLVM IR) | In-place transformation + passes | ✅ **IDENTICAL APPROACH** |
| **GCC** | 3 (GIMPLE→RTL→ASM) | Multiple lowering stages | ❌ More complex, unnecessary |
| **Swift** | 2 (SIL→LLVM IR) | High-level + low-level IRs | ⚠️ Could do this, but single IR is simpler |
| **Rust** | 2 (MIR→LLVM IR) | Mid-level + LLVM backend | ⚠️ Similar to Swift |
| **V8/SpiderMonkey** | 1 (Bytecode) | Single IR + JIT optimization | ✅ Similar single-IR model |

**EK9's single-IR approach matches LLVM**: One canonical representation, multiple optimization passes operating on the same structures.

### Why No Separate "Optimized IR" is Needed

**❌ ANTI-PATTERN: Create separate IR classes for optimized form**
- Doubles maintenance burden (two IR implementations)
- Requires complex IR→IR' transformation logic
- Complicates code generation (which IR version to use?)
- Loses information during transformation
- Adds architectural complexity with minimal benefit

**✅ CORRECT PATTERN: Transform existing IR in-place**
- Single source of truth (one IR implementation)
- Simpler mental model (all passes work on same structures)
- Works seamlessly for both JVM and LLVM backends
- Optimization passes compose naturally (CSE → DCE → LICM)
- Information preserved throughout pipeline

**Historical Precedent**:
- **LLVM**: Single LLVM IR + 100+ transformation passes
- **V8**: Single bytecode IR + TurboFan optimization passes
- **SpiderMonkey**: Single MIR + IonMonkey optimization passes

All major optimizing compilers use single-IR transformation architectures for their core optimization work.

### Practical Example: CSE Optimization on Current IR

**Current unoptimized IR** (from for-in loop analysis):
```
_temp3 = LOAD items                    // First load
RETAIN _temp3
SCOPE_REGISTER _temp3, _scope_1
CALL _temp3._addAss(_temp4)

_temp5 = LOAD items                    // Redundant load ← ELIMINATE
RETAIN _temp5                          // Redundant retain ← ELIMINATE
SCOPE_REGISTER _temp5, _scope_1        // Redundant register ← ELIMINATE
CALL _temp5._addAss(_temp6)            // Replace _temp5 with _temp3
```

**After CSE optimization** (same IR structures, fewer instructions):
```
_temp3 = LOAD items                    // Keep first load
RETAIN _temp3
SCOPE_REGISTER _temp3, _scope_1
CALL _temp3._addAss(_temp4)
// _temp5 load eliminated - not in IR anymore
CALL _temp3._addAss(_temp6)            // Uses _temp3 directly
```

**Implementation pseudocode**:
```java
// Phase 9: IROptimisation.java
Map<String, String> loadedVars = new HashMap<>();  // variable → temp holding it

for (IRInstr instr : block.getInstructions()) {
    if (instr.getOpcode() == LOAD) {
        String var = instr.getOperands().get(0);  // e.g., "items"

        if (loadedVars.containsKey(var)) {
            // Variable already loaded - eliminate this load
            String existingTemp = loadedVars.get(var);
            replaceAllUses(instr.getResult(), existingTemp);  // _temp5 → _temp3
            block.removeInstruction(instr);  // Remove redundant LOAD
        } else {
            loadedVars.put(var, instr.getResult());  // Track: items → _temp3
        }
    }
}
```

**Result**: 3 instructions eliminated (LOAD, RETAIN, SCOPE_REGISTER), 30% reduction in this code sequence.

### Benefits of Current IR Architecture

1. **✅ Optimization-Ready Today**: No architectural changes needed, just add 3 helper methods
2. **✅ Language-Agnostic**: Works identically for JVM and LLVM backends
3. **✅ Proven Architecture**: Follows LLVM's successful single-IR model
4. **✅ Composable Passes**: Optimizations stack naturally (CSE enables DCE, DCE enables LICM)
5. **✅ Maintainable**: Single IR codebase, clear transformation semantics

### Implementation Roadmap

**Phase 9 Optimization Pipeline** (when implemented):

1. **Build Analysis Structures** (read-only passes)
   - Def-use chain builder
   - Liveness analyzer
   - Loop detector
   - Dominator tree builder (if needed for advanced optimizations)

2. **Apply Transformations** (mutating passes)
   - Common Subexpression Elimination (CSE)
   - Dead Temp Elimination (DTE)
   - Loop-Invariant Code Motion (LICM)
   - Copy Propagation
   - Dead Code Elimination (DCE)

3. **Validate Transformations**
   - CFG integrity check
   - Use-before-def validation
   - Type consistency verification

**Estimated complexity**: Medium (well-established algorithms, clean IR structure)

**Timeline**: After JVM and LLVM backends achieve feature completeness (per current development strategy)

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

**Key Architectural Decision (2025-10-23)**: Analysis confirms the current IR structure is **optimization-ready without requiring a separate "optimized IR" representation**. The typed object architecture with hybrid mutability (immutable instructions, mutable containers) matches LLVM's proven single-IR optimization model. Implementation requires only 3 helper methods in `BasicBlockInstr` to enable full optimization capability. See **IR Structure Readiness Assessment** section above for detailed architectural analysis.