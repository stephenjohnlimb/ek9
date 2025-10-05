# EK9 JVM Bytecode Generation and Testing Strategy

This document provides comprehensive guidance for EK9's JVM bytecode generation backend, testing methodologies, and optimization strategies. It also covers the parallel LLVM backend testing strategy to ensure multi-backend parity.

**Related Documentation:**
- **`CLAUDE.md`** - Main project overview and development guidelines
- **`EK9_IR_AND_CODE_GENERATION.md`** - IR generation philosophy and patterns
- **`EK9_Compiler_Architecture_and_Design.md`** - Complete architectural specification

## Table of Contents
1. [Current State Assessment](#current-state-assessment)
2. [JVM Backend Architecture](#jvm-backend-architecture)
3. [Bytecode Testing Strategy](#bytecode-testing-strategy)
4. [Multi-Backend Testing Architecture](#multi-backend-testing-architecture)
5. [Industry Bytecode Testing Practices](#industry-bytecode-testing-practices)
6. [Optimization Strategy](#optimization-strategy)
7. [Implementation Roadmap](#implementation-roadmap)

---

## Current State Assessment

### IR Generation: Production-Quality (Phase 7)

**Status**: ~76 IR generation files implementing sophisticated medium/high-level IR constructs

**Key Achievements**:
- ✅ Medium-level IR (LOGICAL_AND_BLOCK, LOGICAL_OR_BLOCK, CONTROL_FLOW_CHAIN)
- ✅ Complete memory management IR (RETAIN/RELEASE/SCOPE_REGISTER for future LLVM/ARC)
- ✅ Two-tier scope architecture (implicit `_call` + explicit `_scope_N`)
- ✅ 81+ `@IR` directive tests with comprehensive coverage
- ✅ Correctness-first philosophy: verbose IR enables optimization in later phases

**IR is backend-agnostic**: Tests in `examples/irGeneration/` validate semantic correctness regardless of target architecture.

### JVM Bytecode Generation: Refactored Architecture (Phase 15)

**Status**: Architecture refactoring complete, ~40-45% implementation complete

**Implemented**:
- ✅ Consumer-based visitor pattern (aligned with frontend Phase 1-6 patterns)
- ✅ OutputVisitor coordinator with pattern matching dispatcher
- ✅ Five specialized generators: `CallInstrAsmGenerator`, `LiteralInstrAsmGenerator`, `MemoryInstrAsmGenerator`, `BranchInstrAsmGenerator`, `LabelInstrAsmGenerator`
- ✅ Complete control flow support (RETURN, BRANCH, BRANCH_TRUE, BRANCH_FALSE, ASSERT)
- ✅ Label caching in MethodContext (prevents duplicate label creation)
- ✅ Proper variable map access (fixed hashcode bug)
- ✅ Single generator instances (created once, reused across methods)
- ✅ Clean separation: AsmStructureCreator (class structure) vs generators (instruction processing)

**Critical Gaps**:
- ❌ No medium/high-level IR lowering (LOGICAL_AND_BLOCK, CONTROL_FLOW_CHAIN)
- ❌ No bytecode validation testing (@BYTE_CODE directives not yet implemented)
- ❌ Incomplete instruction coverage (still need arithmetic, comparison operations)

**Architecture**: Clean, extensible, follows frontend patterns consistently

---

## JVM Backend Architecture

### Implemented Architecture (Consumer-Based Visitor Pattern)

The JVM backend follows the same Consumer-based pattern used in frontend phases 1-6, ensuring architectural consistency throughout the compiler.

**Architecture Diagram**:
```
OutputVisitor (coordinator)
  ├─ Pattern matching dispatcher: visit(IRInstr)
  │    ├─ CallInstr → visit(CallInstr) → callInstrGenerator.accept()
  │    ├─ LiteralInstr → visit(LiteralInstr) → literalInstrGenerator.accept()
  │    ├─ MemoryInstr → visit(MemoryInstr) → memoryInstrGenerator.accept()
  │    ├─ BranchInstr → visit(BranchInstr) → branchInstrGenerator.accept()
  │    ├─ LabelInstr → visit(LabelInstr) → labelInstrGenerator.accept()
  │    └─ ScopeInstr → visit(ScopeInstr) → no-op
  │
  ├─ Five specialized generators (Consumer<InstructionType>):
  │    ├─ CallInstrAsmGenerator implements Consumer<CallInstr>
  │    ├─ LiteralInstrAsmGenerator implements Consumer<LiteralInstr>
  │    ├─ MemoryInstrAsmGenerator implements Consumer<MemoryInstr>
  │    ├─ BranchInstrAsmGenerator implements Consumer<BranchInstr>
  │    └─ LabelInstrAsmGenerator implements Consumer<LabelInstr>
  │
  └─ AsmStructureCreator (class/method structure ONLY)
       └─ Delegates instruction processing via visitor.accept()
```

**Key Components**:

**1. OutputVisitor (Coordinator)**
```java
public final class OutputVisitor implements INodeVisitor {
  // Specialized generators created once, reused across all methods
  private final CallInstrAsmGenerator callInstrGenerator;
  private final LiteralInstrAsmGenerator literalInstrGenerator;
  private final MemoryInstrAsmGenerator memoryInstrGenerator;
  private final BranchInstrAsmGenerator branchInstrGenerator;
  private final LabelInstrAsmGenerator labelInstrGenerator;

  // Pattern matching dispatcher routes to typed visit methods
  @Override
  public void visit(final IRInstr irInstr) {
    switch (irInstr) {
      case CallInstr i -> visit(i);
      case LiteralInstr i -> visit(i);
      case MemoryInstr i -> visit(i);
      case BranchInstr i -> visit(i);
      case LabelInstr i -> visit(i);
      case ScopeInstr i -> visit(i);
      default -> throw new CompilerException("Unhandled: " + irInstr);
    }
  }

  // Typed visit methods delegate to Consumer generators
  public void visit(final CallInstr callInstr) {
    callInstrGenerator.accept(callInstr);
  }

  public void visit(final BranchInstr branchInstr) {
    branchInstrGenerator.accept(branchInstr);
  }
  // ... (similar for other types)

  // Share method context with all generators before processing
  public void setMethodContext(final AbstractAsmGenerator.MethodContext methodContext,
                               final MethodVisitor mv,
                               final boolean isConstructor) {
    callInstrGenerator.setSharedMethodContext(methodContext);
    literalInstrGenerator.setSharedMethodContext(methodContext);
    memoryInstrGenerator.setSharedMethodContext(methodContext);
    branchInstrGenerator.setSharedMethodContext(methodContext);
    labelInstrGenerator.setSharedMethodContext(methodContext);

    callInstrGenerator.setCurrentMethodVisitor(mv);
    literalInstrGenerator.setCurrentMethodVisitor(mv);
    memoryInstrGenerator.setCurrentMethodVisitor(mv);
    branchInstrGenerator.setCurrentMethodVisitor(mv);
    labelInstrGenerator.setCurrentMethodVisitor(mv);

    branchInstrGenerator.setConstructorMode(isConstructor);
  }
}
```

**2. Specialized Generators (Consumer Pattern)**
```java
// Example: BranchInstrAsmGenerator
public final class BranchInstrAsmGenerator extends AbstractAsmGenerator
    implements Consumer<BranchInstr> {

  private boolean isConstructor = false;

  @Override
  public void accept(final BranchInstr branchInstr) {
    AssertValue.checkNotNull("BranchInstr cannot be null", branchInstr);
    branchInstr.getDebugInfo().ifPresent(this::generateDebugInfo);

    switch (branchInstr.getOpcode()) {
      case RETURN -> generateReturn(branchInstr);
      case BRANCH -> generateBranch(branchInstr);
      case BRANCH_TRUE -> generateBranchTrue(branchInstr);
      case BRANCH_FALSE -> generateBranchFalse(branchInstr);
      case ASSERT -> generateAssert(branchInstr);
      default -> throw new CompilerException("Unhandled: " + branchInstr.getOpcode());
    }
  }

  private void generateReturn(final BranchInstr branchInstr) {
    final var mv = getCurrentMethodVisitor();

    if (isConstructor) {
      mv.visitInsn(Opcodes.RETURN);
      return;
    }

    final var returnValue = branchInstr.getReturnValue();
    if (returnValue != null && !returnValue.isEmpty()) {
      // FIXED: Use proper variable map (not hashcode hack)
      final Integer varIndex = getVariableIndex(returnValue);
      mv.visitVarInsn(Opcodes.ALOAD, varIndex);
      mv.visitInsn(Opcodes.ARETURN);
    } else {
      mv.visitInsn(Opcodes.RETURN);
    }
  }
}
```

**3. MethodContext (Shared State Per Method)**
```java
public static class MethodContext {
  final Map<String, Integer> variableMap = new HashMap<>();
  final Map<String, TempVariableSource> tempSourceMap = new HashMap<>();
  final Map<String, org.objectweb.asm.Label> labelMap = new HashMap<>();  // Label cache
  int nextVariableSlot = 1;
}

// Label caching prevents duplicate label creation
protected org.objectweb.asm.Label getOrCreateLabel(final String labelName) {
  return methodContext.labelMap.computeIfAbsent(labelName,
                                                name -> new org.objectweb.asm.Label());
}
```

**4. AsmStructureCreator (Class Structure Only)**
```java
private void processBasicBlockWithTypedInstructions(final MethodVisitor mv,
                                                    final BasicBlockInstr basicBlock,
                                                    final List<String> parameterNames,
                                                    final boolean isConstructor) {
  final OutputVisitor outputVisitor = (OutputVisitor) visitor;

  // Create fresh MethodContext for this method
  final var methodContext = new AbstractAsmGenerator.MethodContext();

  // Pre-register method parameters (prevents null overwriting)
  int parameterSlot = 1;
  for (String paramName : parameterNames) {
    methodContext.variableMap.put(paramName, parameterSlot++);
  }
  methodContext.nextVariableSlot = parameterSlot;

  // Share context with all generators
  outputVisitor.setMethodContext(methodContext, mv, isConstructor);

  // Process each IR instruction using visitor pattern
  for (var instruction : basicBlock.getInstructions()) {
    instruction.accept(visitor);  // Double dispatch via visitor pattern
  }
}
```

**Benefits of This Architecture**:
- ✅ Single generator instances (created once per construct, reused across methods)
- ✅ Proper variable map access (fixes hashcode bug)
- ✅ Clear separation: structure (AsmStructureCreator) vs instruction processing (generators)
- ✅ Consistent with frontend Consumer/BiConsumer pattern (Phase 1-6)
- ✅ Standardized `accept()` method across all generators (not custom names like `generateCall()`)
- ✅ Label caching prevents duplicate JVM Label creation
- ✅ Extensible for complex IR lowering (future LOGICAL_AND_BLOCK, CONTROL_FLOW_CHAIN)
- ✅ Better testability (each generator has single responsibility)

### Frontend Pattern Alignment

The backend architecture deliberately mirrors the helper pattern used throughout frontend phases 1-6, ensuring consistency and predictability across the entire compiler codebase.

**Frontend Phase Pattern (Phase 1-6)**:
```java
// Example from Phase 1: SYMBOL_DEFINITION
public class DefinitionListener extends Ek9BaseListener {
  // Helper classes implementing Consumer/BiConsumer
  private final Consumer<EK9Parser.FunctionDeclarationContext> functionDefinition;
  private final Consumer<EK9Parser.ClassDeclarationContext> classDefinition;
  private final BiConsumer<EK9Parser.MethodDeclarationContext, IScope> methodDefinition;

  @Override
  public void enterFunctionDeclaration(EK9Parser.FunctionDeclarationContext ctx) {
    functionDefinition.accept(ctx);  // Standardized accept() call
  }

  @Override
  public void enterClassDeclaration(EK9Parser.ClassDeclarationContext ctx) {
    classDefinition.accept(ctx);  // Standardized accept() call
  }
}
```

**Backend Generator Pattern (Phase 15)**:
```java
// OutputVisitor (backend coordinator, like DefinitionListener)
public final class OutputVisitor implements INodeVisitor {
  // Helper classes implementing Consumer
  private final Consumer<CallInstr> callInstrGenerator;
  private final Consumer<LiteralInstr> literalInstrGenerator;
  private final Consumer<BranchInstr> branchInstrGenerator;

  @Override
  public void visit(final IRInstr irInstr) {
    switch (irInstr) {
      case CallInstr i -> visit(i);
      case BranchInstr i -> visit(i);
      // ... pattern matching dispatcher
    }
  }

  public void visit(final CallInstr callInstr) {
    callInstrGenerator.accept(callInstr);  // Standardized accept() call
  }

  public void visit(final BranchInstr branchInstr) {
    branchInstrGenerator.accept(branchInstr);  // Standardized accept() call
  }
}
```

**Pattern Consistency Benefits**:

1. **Predictable Method Names**: All helpers use `accept()` (Consumer) or `apply()` (Function), never custom names
   - ❌ Before: `generateCall()`, `generateLiteral()`, `generateMemoryOperation()`
   - ✅ After: `accept()` for all generators (standard Java Consumer interface)

2. **Familiar Architecture**: Developers working on frontend phases immediately understand backend structure
   - OutputVisitor = DefinitionListener (coordinator)
   - Generators = Helper classes (single responsibility)
   - `accept()` calls = Standard delegation pattern

3. **Consistent Testing Patterns**: Same testing approach for frontend helpers and backend generators
   - Mock coordinator, inject test helper
   - Verify helper receives correct inputs
   - Test helper behavior in isolation

4. **Clear Separation of Concerns**: Coordinator handles dispatching, helpers handle specific logic
   - Frontend: DefinitionListener routes ANTLR contexts → helpers define symbols
   - Backend: OutputVisitor routes IR instructions → generators produce bytecode

**Example Parallel**:

| Aspect | Frontend (Phase 1-6) | Backend (Phase 15) |
|--------|---------------------|-------------------|
| Coordinator | `DefinitionListener` | `OutputVisitor` |
| Helper Interface | `Consumer<ContextType>` | `Consumer<InstrType>` |
| Helper Examples | `FunctionDefinition`, `ClassDefinition` | `CallInstrAsmGenerator`, `BranchInstrAsmGenerator` |
| Method Name | `accept(ctx)` | `accept(instruction)` |
| Initialization | Created once in constructor | Created once in constructor |
| Shared State | `SymbolTable`, `ErrorListener` | `MethodContext`, `ClassWriter` |
| Dispatch Pattern | `enterX()` → `helper.accept()` | `visit(IRInstr)` → `generator.accept()` |

This architectural consistency makes the EK9 compiler codebase highly maintainable and easy to navigate, with the same patterns repeated throughout all phases.

---

## Bytecode Testing Strategy

### The Testing Gap

**Current IR testing**: 81+ tests with `@IR` directives - excellent coverage
**Current bytecode testing**: None - generate .class files but zero validation

### Strategic Value of Backend Testing

**Key insight**: Unlike industry (testing mature compilers for behavioral correctness), EK9 is testing NEW backends for structural correctness.

**What backend testing enables**:
1. ✅ **Visibility into lowering correctness** - See exact bytecode/LLVM-IR generated from IR
2. ✅ **Optimization progress tracking** - Measure instruction count reduction over time
3. ✅ **Regression detection** - Tests fail if optimization breaks correctness
4. ✅ **Debug visibility** - `showBytecode()` / `showLlvm()` parallel to `showIR()`
5. ✅ **Backend parity validation** - Ensure JVM and LLVM backends both work

### Example: Optimization Visibility

**EK9 Source**:
```ek9
basicAssignment()
  value as Integer?
  value: 1
```

**IR** (verbose by design - enables Phase 12 optimization):
```
_temp1 = LOAD_LITERAL 1, org.ek9.lang::Integer
RETAIN _temp1
SCOPE_REGISTER _temp1, _scope_1
RELEASE value
STORE value, _temp1
RETAIN value
SCOPE_REGISTER value, _scope_1
```

**Current Naive JVM Bytecode** (no optimization):
```
ICONST_1
INVOKESTATIC Integer.toString (I)Ljava/lang/String;
INVOKESTATIC Integer._of (Ljava/lang/String;)LInteger;
ASTORE 2        // _temp1 (unnecessary temporary)
ALOAD 2
ASTORE 1        // value (redundant load/store)
```

**Future Optimized JVM Bytecode**:
```
ICONST_1
INVOKESTATIC Integer.toString (I)Ljava/lang/String;
INVOKESTATIC Integer._of (Ljava/lang/String;)LInteger;
ASTORE 1        // value (direct assignment, no temp)
```

**LLVM IR** (has explicit ARC - RETAIN/RELEASE lowered):
```llvm
define void @assignments.basicAssignment._main() {
entry:
  %value = alloca %Integer*
  store %Integer* null, %Integer** %value
  %1 = call %Integer* @Integer._of(i32 1)
  call void @Integer._retain(%Integer* %1)
  %2 = load %Integer*, %Integer** %value
  call void @Integer._release(%Integer* %2)
  store %Integer* %1, %Integer** %value
  ret void
}
```

**Metrics visible via backend directives**:
- JVM Naive: 6 instructions → Optimized: 4 instructions (33% reduction)
- LLVM: Explicit ARC calls visible (JVM doesn't have these)
- Backend differences clearly documented

### Backend Directive Pattern

Mirrors proven `@IR` directive pattern, extended for multiple backends:

**JVM Bytecode Test** (`bytecodeGeneration/assignmentStatements/basicAssignment.ek9`):
```ek9
#!ek9
defines module assignments

  defines function

    @BYTE_CODE: FUNCTION: "assignments::basicAssignment": `
    method: _main()V
      ACONST_NULL
      ASTORE 1
      ICONST_1
      INVOKESTATIC java/lang/Integer.toString (I)Ljava/lang/String;
      INVOKESTATIC org/ek9/lang/Integer._of (Ljava/lang/String;)Lorg/ek9/lang/Integer;
      ASTORE 1
      RETURN
    `

    basicAssignment()
      value as Integer?
      value: 1
```

**LLVM IR Test** (`llvmGeneration/assignmentStatements/basicAssignment.ek9`):
```ek9
#!ek9
defines module assignments

  defines function

    @LLVM: FUNCTION: "assignments::basicAssignment": `
    define void @assignments.basicAssignment._main() {
    entry:
      %value = alloca %Integer*
      store %Integer* null, %Integer** %value
      %1 = call %Integer* @Integer._of(i32 1)
      call void @Integer._retain(%Integer* %1)
      %2 = load %Integer*, %Integer** %value
      call void @Integer._release(%Integer* %2)
      store %Integer* %1, %Integer** %value
      ret void
    }
    `

    basicAssignment()
      value as Integer?
      value: 1
```

**Key features**:
- Standard format (javap for JVM, LLVM IR for LLVM)
- Normalized output (constant pool indices removed for JVM)
- Human-readable for debugging
- Version-controlled with source code
- **Separate files** - no mixing @BYTE_CODE and @LLVM in same file

### Bytecode Normalization (JVM)

**Challenge**: Raw javap output is fragile (constant pool indices change)

**Solution**: Normalize output to remove fragile elements

```java
public class BytecodeNormalizer {

  public static String normalize(byte[] classBytes) {
    String javapOutput = executeJavap(classBytes, "-c", "-p");

    return javapOutput
        .replaceAll("#\\d+", "#CP")                    // #7 → #CP
        .replaceAll("line \\d+:.*\\n", "")             // Remove line numbers
        .replaceAll("LocalVariableTable:.*?(?=\\n\\n)", "") // Remove var table
        .replaceAll("StackMapTable:.*?(?=\\n\\n)", "")      // Remove stack maps
        .trim();
  }

  private static String executeJavap(byte[] classBytes, String... options) {
    // Write to temp file, run javap, capture output
    Path tempFile = Files.createTempFile("ek9-bytecode", ".class");
    Files.write(tempFile, classBytes);

    ProcessBuilder pb = new ProcessBuilder("javap");
    pb.command().addAll(Arrays.asList(options));
    pb.command().add(tempFile.toString());

    Process process = pb.start();
    String output = new String(process.getInputStream().readAllBytes());
    Files.delete(tempFile);

    return output;
  }
}
```

**Normalization rules**:
1. Replace constant pool indices: `#7` → `#CP`
2. Remove LineNumberTable (implementation detail)
3. Remove LocalVariableTable (implementation detail)
4. Remove StackMapTable (JVM internal)
5. Remove class header/footer boilerplate
6. Keep only essential instruction sequence

**Stability**:
- ❌ Adding/removing fields → Changes constant pool → **BUT normalized away**
- ❌ Reordering methods → Changes constant pool → **BUT normalized away**
- ✅ Changing instruction sequence → **Caught by test** ✅
- ✅ Wrong opcodes → **Caught by test** ✅
- ✅ Wrong method calls → **Caught by test** ✅

### Debugging Workflow (Parallel to @IR)

**Similar to `showIR()` in `AbstractIRGenerationTest`**:

```java
public abstract class AbstractBytecodeGenerationTest extends PhasesTest {

  // Flag to show bytecode (like showIR flag)
  private final boolean showGeneratedBytecode;

  public AbstractBytecodeGenerationTest(String fromResourcesDirectory,
                                        List<SymbolCountCheck> expectedSymbols,
                                        boolean verbose,
                                        boolean muteReportedErrors,
                                        boolean showBytecode) {
    super(fromResourcesDirectory, verbose, muteReportedErrors);
    this.expectedSymbols = expectedSymbols;
    this.showGeneratedBytecode = showBytecode;
    if (showBytecode) {
      System.err.println("Warning Show Bytecode is enabled for " + fromResourcesDirectory);
    }
  }

  protected void showBytecode(CompilableProgram program) {
    // Display generated bytecode for all classes
    program.getGeneratedClassFiles().forEach(classFile -> {
      System.out.println("=== " + classFile.getClassName() + " ===");
      System.out.println(BytecodeNormalizer.normalize(classFile.getBytes()));
      System.out.println();
    });
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors,
                                    CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    expectedSymbols.forEach(check -> check.test(program));

    if (showGeneratedBytecode) {
      showBytecode(program);
    }

    // Validate bytecode matches @BYTE_CODE directives
    validateBytecodeDirectives(program);
  }
}
```

**Developer workflow when test fails**:
```bash
# 1. Test fails
mvn test -Dtest=BytecodeAssignmentStatementTest
# ❌ FAILED: Bytecode mismatch

# 2. Enable bytecode display (set showBytecode=true in test constructor)
# Or add flag similar to -Dek9.instructionInstrumentation=true
```

**Console output** (immediately see the issue):
```
=== assignments::basicAssignment ===
method: _main()V
  ACONST_NULL
  ASTORE 1
  ICONST_1
  ...
  ALOAD 7            // ← BUG! Expected ALOAD 1 (hashcode hack)
  ARETURN

Expected:
  ALOAD 1
  ARETURN

Actual:
  ALOAD 7            // ← Clearly wrong
  ARETURN
```

---

## Multi-Backend Testing Architecture

EK9 will support multiple backend targets (JVM and LLVM). The testing architecture maintains separate but parallel test suites for each backend.

### Resource Directory Structure

**Separate directories with parallel structure**:

```
compiler-main/src/test/resources/examples/
│
├── irGeneration/              # @IR directives (backend-agnostic)
│   ├── assignmentStatements/
│   │   ├── basicAssignment.ek9
│   │   └── guardedAssignment.ek9
│   ├── booleanExpressions/
│   │   ├── andExpression.ek9
│   │   ├── orExpression.ek9
│   │   └── mixedExpression.ek9
│   ├── calls/
│   │   ├── functionCall.ek9
│   │   ├── constructorCall.ek9
│   │   └── ...
│   └── operatorUse/
│       ├── arithmetic/
│       ├── logical/
│       └── ...
│
├── bytecodeGeneration/        # @BYTE_CODE directives (JVM-specific)
│   ├── assignmentStatements/
│   │   ├── basicAssignment.ek9        # Same EK9 code, JVM expectations
│   │   └── guardedAssignment.ek9
│   ├── booleanExpressions/
│   │   ├── andExpression.ek9
│   │   └── orExpression.ek9
│   ├── calls/
│   │   ├── functionCall.ek9
│   │   └── constructorCall.ek9
│   └── operatorUse/
│       └── arithmetic/
│
└── llvmGeneration/            # @LLVM directives (LLVM-specific)
    ├── assignmentStatements/
    │   ├── basicAssignment.ek9        # Same EK9 code, LLVM expectations
    │   └── guardedAssignment.ek9
    ├── booleanExpressions/
    │   ├── andExpression.ek9
    │   └── orExpression.ek9
    ├── calls/
    │   ├── functionCall.ek9
    │   └── constructorCall.ek9
    └── operatorUse/
        └── arithmetic/
```

### Java Test Class Structure

**Parallel test class hierarchies**:

```
compiler-main/src/test/java/org/ek9lang/compiler/
│
├── ir/                        # IR generation tests (Phase 7)
│   ├── AbstractIRGenerationTest.java
│   ├── AssignmentStatementTest.java
│   ├── BooleanExpressionsTest.java
│   ├── CallsTest.java
│   └── ... (24 test classes)
│
├── bytecode/                  # JVM bytecode tests (Phase 15)
│   ├── AbstractBytecodeGenerationTest.java
│   ├── AssignmentStatementTest.java
│   ├── BooleanExpressionsTest.java
│   ├── CallsTest.java
│   └── ...
│
└── llvm/                      # LLVM IR tests (Phase 15 - future)
    ├── AbstractLlvmGenerationTest.java
    ├── AssignmentStatementTest.java
    ├── BooleanExpressionsTest.java
    ├── CallsTest.java
    └── ...
```

### Test Class Pattern

**IR Test** (existing pattern):
```java
package org.ek9lang.compiler.ir;

class AssignmentStatementTest extends AbstractIRGenerationTest {
  public AssignmentStatementTest() {
    super("/examples/irGeneration/assignmentStatements",
        List.of(new SymbolCountCheck(2, "assignments", 4)),
        false, false, false);  // verbose, muteErrors, showIR
  }
}
```

**JVM Bytecode Test** (new, mirrors IR pattern):
```java
package org.ek9lang.compiler.bytecode;

class AssignmentStatementTest extends AbstractBytecodeGenerationTest {
  public AssignmentStatementTest() {
    super("/examples/bytecodeGeneration/assignmentStatements",
        List.of(new SymbolCountCheck(2, "assignments", 4)),
        false, false, false);  // verbose, muteErrors, showBytecode
  }
}
```

**LLVM IR Test** (future, mirrors same pattern):
```java
package org.ek9lang.compiler.llvm;

class AssignmentStatementTest extends AbstractLlvmGenerationTest {
  public AssignmentStatementTest() {
    super("/examples/llvmGeneration/assignmentStatements",
        List.of(new SymbolCountCheck(2, "assignments", 4)),
        false, false, false);  // verbose, muteErrors, showLlvm
  }
}
```

### Benefits of Separate Directories

**1. Clear Visibility of Coverage**
```bash
# Quick diff to see what's missing
diff -r bytecodeGeneration/ llvmGeneration/
# Shows files only in one backend
```

**2. Easy to Spot Gaps**
```bash
ls bytecodeGeneration/assignmentStatements/
# basicAssignment.ek9
# guardedAssignment.ek9

ls llvmGeneration/assignmentStatements/
# basicAssignment.ek9
# (missing guardedAssignment.ek9!) ← Immediately visible
```

**3. No Directive Confusion**
- Each file has ONLY the directive for its backend
- No mixing `@BYTE_CODE` and `@LLVM` in same file
- Clearer to read and maintain

**4. Backend-Specific Features Easy to Identify**
```
bytecodeGeneration/jvmSpecific/
└── stackOptimization.ek9      # JVM-specific optimization tests

llvmGeneration/llvmSpecific/
└── arcMemoryManagement.ek9    # LLVM-specific ARC tests
```

**5. Side-by-Side Comparison**
```bash
# Compare JVM vs LLVM output for same EK9 code
diff bytecodeGeneration/assignmentStatements/basicAssignment.ek9 \
     llvmGeneration/assignmentStatements/basicAssignment.ek9
```

Shows differences in backend expectations while keeping EK9 code identical.

### Backend Parity Enforcement

**Automated test to ensure parallel coverage**:

```java
package org.ek9lang.compiler.backend;

class BackendParityTest {

  @Test
  void testBackendDirectoryParity() {
    Path jvmDir = Paths.get("src/test/resources/examples/bytecodeGeneration");
    Path llvmDir = Paths.get("src/test/resources/examples/llvmGeneration");

    Set<String> jvmFiles = findAllEk9Files(jvmDir);
    Set<String> llvmFiles = findAllEk9Files(llvmDir);

    // Report missing files
    Set<String> onlyInJvm = new HashSet<>(jvmFiles);
    onlyInJvm.removeAll(llvmFiles);

    Set<String> onlyInLlvm = new HashSet<>(llvmFiles);
    onlyInLlvm.removeAll(jvmFiles);

    if (!onlyInJvm.isEmpty()) {
      System.err.println("Files missing in LLVM backend tests: " + onlyInJvm);
    }
    if (!onlyInLlvm.isEmpty()) {
      System.err.println("Files missing in JVM backend tests: " + onlyInLlvm);
    }

    assertTrue(onlyInJvm.isEmpty() && onlyInLlvm.isEmpty(),
               "Backend test coverage should be parallel");
  }

  private Set<String> findAllEk9Files(Path baseDir) {
    try (Stream<Path> paths = Files.walk(baseDir)) {
      return paths
          .filter(p -> p.toString().endsWith(".ek9"))
          .map(baseDir::relativize)
          .map(Path::toString)
          .collect(Collectors.toSet());
    }
  }
}
```

**This test fails if directories aren't in sync**, forcing maintenance of backend parity.

### Example: Backend-Specific Differences

**JVM Bytecode** (no memory management):
```ek9
@BYTE_CODE: FUNCTION: "assignments::basicAssignment": `
method: _main()V
  ACONST_NULL
  ASTORE 1
  ICONST_1
  INVOKESTATIC Integer._of (I)LInteger;
  ASTORE 1        # Direct store, no RETAIN/RELEASE
  RETURN
`
```

**LLVM IR** (explicit ARC):
```ek9
@LLVM: FUNCTION: "assignments::basicAssignment": `
define void @assignments.basicAssignment._main() {
entry:
  %value = alloca %Integer*
  store %Integer* null, %Integer** %value
  %1 = call %Integer* @Integer._of(i32 1)
  call void @Integer._retain(%Integer* %1)      # Explicit ARC
  %2 = load %Integer*, %Integer** %value
  call void @Integer._release(%Integer* %2)     # Explicit ARC
  store %Integer* %1, %Integer** %value
  ret void
}
`
```

**Key differences visible**:
- JVM: GC handles memory automatically
- LLVM: Explicit `_retain` and `_release` calls
- Same EK9 semantics, different backend lowering

---

## Industry Bytecode Testing Practices

Research into how major JVM language projects test bytecode generation reveals different approaches for different maturity levels.

### OpenJDK javac (Gold Standard - Mature Compiler)

**Primary approach**: "Golden file" tests using jtreg framework

```java
/**
 * @test
 * @bug 8043467
 * @summary Test that compiler behavior matches expected output
 * @compile/fail/ref=ExpectedOutput.out -XDrawDiagnostics TestFile.java
 */
```

**Key insights**:
- Uses `-XDrawDiagnostics` for stable, text-based output
- Reference files contain compiler diagnostics, not bytecode details
- Focuses on compile success/failure, not bytecode inspection
- **For mature compiler**: Behavioral correctness over structural validation

**However**: javac DOES use javap for specific bytecode verification:
- Constant pool correctness
- Specific instruction sequences for edge cases
- Class file format validation

### Kotlin Compiler

**Primary approach**: kotlin-compile-testing library

```kotlin
@Test
fun `test bytecode generation`() {
    val result = KotlinCompilation().apply {
        sources = listOf(SourceFile.kotlin("Test.kt", """
            fun test() = 42
        """))
    }.compile()

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

    // Load and verify class via reflection
    val clazz = result.classLoader.loadClass("TestKt")
    assertEquals(42, clazz.getMethod("test").invoke(null))
}
```

**Key insights**:
- **Execute the code** and verify behavior (semantic testing)
- Load compiled classes via reflection
- IntelliJ "Show Kotlin Bytecode" for manual inspection
- Can decompile bytecode to Java for verification
- **No text-based bytecode assertion** - focuses on correctness

### Scala Compiler

**Primary approach**: Functional/behavioral testing

```scala
@Test
def testCodeGeneration(): Unit = {
  val code = """def add(a: Int, b: Int): Int = a + b"""
  val compiled = compile(code)
  assert(compiled.exitCode == Success)

  // Execute and verify
  val result = runMethod(compiled, "add", 2, 3)
  assert(result == 5)
}
```

**Key insights**:
- Uses scalap/javap for manual debugging, not automated tests
- Functional testing: Does the code execute correctly?
- Can inspect compiler phases and intermediate representations
- Scala 2.10+ uses ASM, but doesn't test ASM output directly

### Industry Consensus Pattern

**What they DON'T do**:
- ❌ Text-based javap comparison (too fragile with constant pool indices)
- ❌ Bytecode instruction-by-instruction text validation
- ❌ Manual bytecode inspection in automated tests

**What they DO**:
1. ✅ **Semantic/functional testing** - Execute and verify behavior
2. ✅ **Compile success/failure** - Does it compile correctly?
3. ✅ **Reflection-based validation** - Load class, inspect via Java reflection
4. ✅ **Manual javap inspection** - For debugging, not automated tests
5. ✅ **JVM bytecode verification** - Let the JVM verify structural validity

### Why EK9 Is Different

**Industry approach works because they're testing MATURE compilers:**
- Industry: "Does this feature compile correctly?" (add features to mature compiler)
- EK9: "Is the IR being lowered to correct bytecode?" (new backend, verify lowering)

**EK9's stage requires different approach**:
- ✅ Building backends (need to verify IR → bytecode/LLVM lowering)
- ✅ Not testing javac (testing YOUR bytecode generation)
- ✅ Small, focused examples (industry uses this for specific bytecode concerns)
- ✅ Parallel to @IR testing (proven pattern in your project)
- ✅ Multi-backend validation (ensure JVM and LLVM parity)

---

## Optimization Strategy

### Current State: Naive Lowering

**Philosophy**: Correctness first, optimization later (mirrors IR generation philosophy)

**Current bytecode characteristics**:
- Verbose temporary variable usage
- Redundant LOAD/STORE instructions
- No stack-oriented optimization
- Each IR instruction lowered independently

**This is acceptable** because:
1. IR is semantically correct (verified by @IR tests)
2. Naive lowering is straightforward to verify
3. JVM JIT will optimize some inefficiencies
4. Provides baseline for measuring future optimizations

### Single-Version Approach: Optimization Replaces Naive

**Once optimization is implemented, it always applies** - no "naive mode" to maintain.

**Why not maintain both naive and optimized versions?**

**Arguments against dual-mode**:
- ❌ Maintenance burden (two code paths to maintain)
- ❌ Testing complexity (test both modes?)
- ❌ User confusion (which mode are they getting?)
- ❌ No real-world benefit (users always want optimal code)

**Arguments for optimization-only**:
- ✅ Single code path (simpler maintenance)
- ✅ Clear testing (one expected output per backend)
- ✅ Always-optimal (users always get best bytecode)
- ✅ Git history preserves optimization journey
- ✅ @BYTE_CODE directives updated when optimization lands

### Optimization Workflow

**Today**: Write naive @BYTE_CODE tests
```ek9
@BYTE_CODE: FUNCTION: "assignments::basicAssignment": `
method: _main()V
  ACONST_NULL
  ASTORE 1
  ICONST_1
  INVOKESTATIC Integer._of ...
  ASTORE 2        // _temp1 (naive lowering)
  ALOAD 2
  ASTORE 1        // value (redundant)
  RETURN
`
```

**Later**: Implement optimization, update @BYTE_CODE directive
```ek9
@BYTE_CODE: FUNCTION: "assignments::basicAssignment": `
method: _main()V
  ACONST_NULL
  ASTORE 1
  ICONST_1
  INVOKESTATIC Integer._of ...
  ASTORE 1        // value (optimized - direct)
  RETURN
`
```

**Git history shows improvement**:
```bash
git diff HEAD~1 basicAssignment.ek9
# Shows -2 instructions (eliminated temp variable)
```

### Optimization Techniques

**Level 1: Temporary Variable Elimination**

**Before**:
```
ICONST_1
INVOKESTATIC Integer._of ...
ASTORE 2        // _temp1
ALOAD 2
ASTORE 1        // value
```

**After**:
```
ICONST_1
INVOKESTATIC Integer._of ...
ASTORE 1        // value (direct)
```

**Technique**: Track temp variable sources, skip intermediate storage for single-use temps

**Level 2: Stack-Oriented Code Generation**

**Before**:
```
ALOAD 1         // Load arg0
ASTORE 2        // Store to temp
ALOAD 2         // Load from temp
INVOKEVIRTUAL Boolean._true ...
```

**After**:
```
ALOAD 1         // Load arg0
DUP             // Duplicate on stack
INVOKEVIRTUAL Boolean._true ...
```

**Technique**: Use JVM stack operations (DUP, SWAP) instead of temporary variables

**Level 3: Dead Code Elimination**

**Before**:
```
ALOAD 1
ASTORE 3        // Never used again
RETURN
```

**After**:
```
RETURN
```

**Technique**: Dataflow analysis to eliminate unused variable assignments

### Optimization Measurement

**Metrics to track**:
1. **Instruction count** - Fewer instructions = faster execution
2. **Bytecode size** - Smaller .class files
3. **Local variable slots** - Fewer slots = less stack frame overhead
4. **Execution time** - Benchmark critical paths

**Git history provides automatic tracking**:
```bash
# Before optimization
git show HEAD~5:bytecodeGeneration/assignmentStatements/basicAssignment.ek9
# Shows @BYTE_CODE with 8 instructions

# After optimization
cat bytecodeGeneration/assignmentStatements/basicAssignment.ek9
# Shows @BYTE_CODE with 5 instructions

# Measure improvement
git diff HEAD~5 bytecodeGeneration/assignmentStatements/basicAssignment.ek9
# Shows -3 instructions (37% reduction)
```

---

## Implementation Roadmap

### Phase 1: Architecture Refactoring ✅ COMPLETED

**Goal**: Fix critical bugs and establish clean architecture

**Completed Tasks**:
1. ✅ Implemented Consumer-based visitor pattern (aligned with frontend Phase 1-6)
   - Created five specialized generators implementing `Consumer<InstructionType>`
   - OutputVisitor coordinator with pattern matching dispatcher
   - Single generator instances (created once in constructor, reused across all methods)
   - Proper `MethodContext` management per method with shared state

2. ✅ Fixed RETURN instruction handling (BranchInstrAsmGenerator.java:62-87)
   - Removed hashcode hack: `Math.abs(returnType.hashCode() % 10) + 1`
   - Use proper variable map: `getVariableIndex(returnValue)`
   - Correctly handle constructor vs regular method returns
   - Handle 'this' return value specially (slot 0)

3. ✅ Implemented complete BranchInstr support (BranchInstrAsmGenerator.java)
   - RETURN → Opcodes.RETURN / Opcodes.ARETURN
   - BRANCH → Opcodes.GOTO label
   - BRANCH_TRUE → Opcodes.IFNE label (branch if non-zero)
   - BRANCH_FALSE → Opcodes.IFEQ label (branch if zero)
   - ASSERT → AssertionError throw with source location

4. ✅ Implemented LabelInstr support (LabelInstrAsmGenerator.java)
   - Places JVM labels for branch targets
   - Label caching in MethodContext prevents duplicate label creation
   - Uses `getOrCreateLabel()` for consistent label references

5. ✅ Updated AsmStructureCreator for visitor pattern
   - Removed `InstructionVisitor` inner class
   - Delegates all instruction processing via `instruction.accept(visitor)`
   - Proper method context initialization with parameter pre-registration
   - Uses OutputVisitor.setMethodContext() to share state with all generators

**Deliverables**:
- ✅ Clean Consumer-based architecture matching frontend patterns
- ✅ Critical RETURN bug fixed
- ✅ All control flow instructions implemented
- ✅ Foundation for complex IR lowering
- ✅ Integration tests passing (introduction/HelloWorld, etc.)

**Testing**: ✅ All integration tests passing, no regressions

### Phase 2: Bytecode Testing Infrastructure (Next)

**Goal**: Implement @BYTE_CODE testing framework parallel to @IR

**Tasks**:
1. ✅ Create `BytecodeNormalizer` utility
   - Execute javap programmatically
   - Normalize output (remove constant pool indices, debug tables)
   - Return clean, diff-friendly text format
   - Location: `compiler-main/src/main/java/org/ek9lang/compiler/support/BytecodeNormalizer.java`

2. ✅ Create `AbstractBytecodeGenerationTest` base class
   - Mirror `AbstractIRGenerationTest` pattern exactly
   - `showBytecode()` method (parallel to `showIR()`)
   - Constructor params: `(fromResourcesDirectory, expectedSymbols, verbose, muteErrors, showBytecode)`
   - `extractBytecodeDirective()` for @BYTE_CODE parsing
   - Test to phase: `CompilationPhase.CODE_GENERATION_AGGREGATES`
   - Location: `compiler-main/src/test/java/org/ek9lang/compiler/bytecode/AbstractBytecodeGenerationTest.java`

3. ✅ Create resource directory structure
   - `compiler-main/src/test/resources/examples/bytecodeGeneration/`
   - Mirror `irGeneration/` directory structure exactly
   - Subdirectories: `assignmentStatements/`, `calls/`, `operatorUse/`, etc.

4. ✅ Implement 5-10 foundational @BYTE_CODE tests
   - `AssignmentStatementTest.java` → `bytecodeGeneration/assignmentStatements/`
   - `CallsTest.java` → `bytecodeGeneration/calls/`
   - Each test class extends `AbstractBytecodeGenerationTest`
   - Each .ek9 file has @BYTE_CODE directive with expected output
   - Location: `compiler-main/src/test/java/org/ek9lang/compiler/bytecode/`

5. ✅ Create `BytecodeDiffer` utility
   - Show side-by-side expected vs actual
   - Highlight differences clearly
   - Integrate with test failure output

**Deliverables**:
- Complete @BYTE_CODE testing framework
- 5-10 passing bytecode tests proving naive lowering correctness
- Debugging tools (`showBytecode()`) for development
- Parallel structure to proven @IR testing pattern

**Testing**: All @BYTE_CODE tests pass with current naive lowering

### Phase 3: Medium-Level IR Lowering (After Phase 2)

**Goal**: Implement lowering for LOGICAL_AND_BLOCK and CONTROL_FLOW_CHAIN

**Tasks**:
1. ✅ Implement LOGICAL_AND_BLOCK lowering in `IRInstructionProcessor`
   - Short-circuit evaluation (branch on left condition)
   - Generate labels for short-circuit and end
   - Process nested instruction sequences recursively
   - Add @BYTE_CODE test: `bytecodeGeneration/booleanExpressions/andExpression.ek9`

2. ✅ Implement LOGICAL_OR_BLOCK lowering
   - Short-circuit evaluation (branch on left condition)
   - Mirror AND lowering pattern
   - Add @BYTE_CODE test: `bytecodeGeneration/booleanExpressions/orExpression.ek9`

3. ✅ Implement CONTROL_FLOW_CHAIN lowering (question operator, guarded assignment)
   - Generate conditional branches
   - Handle null checks (IS_NULL)
   - Process case chains
   - Add @BYTE_CODE tests for `?` and `:=?` operators

4. ✅ Add comprehensive @BYTE_CODE tests
   - Complex Boolean expressions: `arg0 and (arg1 or arg2)`
   - Nested control flow chains
   - Mixed operators

**Deliverables**:
- Complete medium-level IR lowering for JVM
- All Boolean expression IR tests now generate bytecode
- 15+ @BYTE_CODE tests covering complex constructs

**Testing**: Run full IR test suite, verify bytecode generation for all constructs

### Phase 4: LLVM Backend Foundation (Parallel Development)

**Goal**: Establish LLVM backend testing infrastructure parallel to JVM

**Tasks**:
1. ✅ Create `AbstractLlvmGenerationTest` base class
   - Mirror `AbstractBytecodeGenerationTest` pattern
   - `showLlvm()` method (parallel to `showBytecode()`)
   - Target architecture: `TargetArchitecture.LLVM`
   - Location: `compiler-main/src/test/java/org/ek9lang/compiler/llvm/AbstractLlvmGenerationTest.java`

2. ✅ Create resource directory structure
   - `compiler-main/src/test/resources/examples/llvmGeneration/`
   - Mirror `bytecodeGeneration/` directory structure exactly
   - Subdirectories: `assignmentStatements/`, `calls/`, `operatorUse/`, etc.

3. ✅ Implement basic LLVM IR generation
   - Memory management lowering (RETAIN → `@_retain`, RELEASE → `@_release`)
   - Basic instruction lowering (LOAD, STORE, CALL)
   - LLVM IR structure generation

4. ✅ Implement 5-10 foundational @LLVM tests
   - Same EK9 code as JVM tests
   - Different expected output (LLVM IR vs JVM bytecode)
   - Shows explicit ARC memory management

5. ✅ Create `BackendParityTest`
   - Automated test ensuring `bytecodeGeneration/` and `llvmGeneration/` are in sync
   - Fails if files exist in one backend but not the other
   - Enforces parallel coverage

**Deliverables**:
- LLVM backend testing infrastructure
- 5-10 passing LLVM IR tests
- Parity enforcement between JVM and LLVM backends
- Clear visibility into backend-specific differences

**Testing**: All @LLVM tests pass, parity test ensures coverage alignment

### Phase 5: Optimization (Future)

**Goal**: Implement bytecode-level optimizations to reduce instruction count

**Strategy**: Once optimization is implemented, it replaces naive lowering entirely (no dual-mode)

**Tasks**:
1. ✅ Implement temporary variable elimination
   - Track single-use temps
   - Skip intermediate storage
   - Update @BYTE_CODE tests to reflect optimized output

2. ✅ Implement stack-oriented code generation
   - Use DUP/SWAP instead of temp variables where possible
   - Prefer stack operations over local variable slots
   - Update @BYTE_CODE tests

3. ✅ Implement dead code elimination
   - Dataflow analysis to detect unused assignments
   - Remove unreachable code
   - Update @BYTE_CODE tests

4. ✅ Measure and document improvements
   - Instruction count reduction
   - Bytecode size reduction
   - Execution time benchmarks
   - Update documentation with metrics

**Migration workflow**:
```bash
# Before optimization (current state)
git log --oneline bytecodeGeneration/assignmentStatements/basicAssignment.ek9
# Shows @BYTE_CODE with 8 instructions (naive lowering)

# Implement optimization
# Update @BYTE_CODE directive in .ek9 file to reflect new output (5 instructions)

# Commit shows improvement
git diff HEAD~1 bytecodeGeneration/assignmentStatements/basicAssignment.ek9
# Shows -3 instructions (37% reduction)
```

**Deliverables**:
- Optimized bytecode generation
- Updated @BYTE_CODE tests showing improvements
- Performance metrics documenting gains
- All tests still pass (semantic correctness preserved)

**Testing**:
- All @BYTE_CODE tests pass with new expected output
- Semantic execution tests pass (behavior unchanged)
- Benchmarks show measurable performance improvement

### Phase 6: Complete Language Features (Long-term)

**Goal**: Complete backend support for all EK9 language features

**Tasks**:
1. If statements, switch expressions
2. While/for loops
3. Try/catch/finally
4. Streams and pipelines
5. Component/service injection
6. Aspect-oriented features

Each feature follows the pattern:
1. Verify IR generation works (@IR tests exist)
2. Implement JVM bytecode lowering in `IRInstructionProcessor`
3. Implement LLVM IR lowering (parallel)
4. Add @BYTE_CODE test for structure validation
5. Add @LLVM test for structure validation
6. Verify parity test passes
7. Optimize if needed
8. Document and measure

---

## Summary: Key Principles

### Architecture
- ✅ **Clean separation**: Structure (AsmStructureCreator) vs Instructions (IRInstructionProcessor)
- ✅ **Single generator instances**: Created once, reused across all methods
- ✅ **Proper variable management**: MethodContext per method, shared across generators
- ✅ **Extensible design**: Natural home for complex IR lowering

### Testing
- ✅ **Multi-backend validation**: Separate directories for JVM and LLVM with parallel structure
- ✅ **Parallel to @IR pattern**: Proven approach, familiar workflow
- ✅ **Debugging visibility**: `showBytecode()` / `showLlvm()` like `showIR()`
- ✅ **Parity enforcement**: Automated tests ensure backend coverage alignment
- ✅ **Single-version approach**: @BYTE_CODE/@LLVM updated when optimization lands

### Optimization
- ✅ **Correctness first**: Naive lowering initially, optimize later
- ✅ **Single mode**: Once optimized, always optimized (no dual-mode maintenance)
- ✅ **Measurable progress**: Git history + directives document improvements
- ✅ **Semantic preservation**: Optimization never breaks correctness

### Development Workflow
```bash
# Fix a bug or add a feature
vim IRInstructionProcessor.java

# Compile and test (JVM backend)
mvn test -Dtest=BytecodeAssignmentStatementTest

# See actual bytecode output (set showBytecode=true in test constructor)
# Compare to expected @BYTE_CODE directive
# Debug and fix

# Test LLVM backend (parallel)
mvn test -Dtest=LlvmAssignmentStatementTest

# Ensure parity
mvn test -Dtest=BackendParityTest

# When working, commit
git commit -m "Implement LOGICAL_AND_BLOCK lowering for JVM and LLVM"
# @BYTE_CODE and @LLVM directives are version-controlled, document the change
```

This multi-backend architecture provides visibility, validation, and measurable progress throughout the backend development journey while maintaining clear separation and parity between JVM and LLVM targets.
