# EK9 IR Evolution Plan: From Current to Language-Agnostic Design

## Current State Assessment

### **Alignment Analysis: 60% Foundation Ready**

The existing IR in `src/main/java/org/ek9lang/compiler/ir` provides a solid foundation but requires significant enhancement to achieve our language-agnostic, instruction-level design goals.

#### **What Works Well** ✅
- **INode interface**: Excellent visitor pattern foundation
- **Symbol integration**: Proper use of `ISymbol` linking to frontend resolution
- **Construct/Operation structure**: Good mapping to EK9 language constructs
- **Block concept**: Foundation for basic block structure
- **Marker**: Perfect for labels and jump targets
- **Call**: Properly captures resolved method information

#### **What Needs Enhancement** ⚠️
```java
// Current: Too high-level
public final class Statement implements INode {
  private final String statementText;  // ❌ String-based representation
}

// Current: Generic assignment
public final class Assignment implements INode {
  private final INode lhs;
  private final INode rhs;  // ❌ Needs instruction decomposition
}

// Current: Empty expression
public final class Expression implements INode {
  // ❌ No decomposition into basic operations
}
```

### **Major Gaps Identified**

1. **No Basic Instruction Set**: Missing `LOAD`, `STORE`, `CALL`, `BRANCH` instructions
2. **No Control Flow Decomposition**: EK9 constructs not broken into basic blocks
3. **No Exception Handling Primitives**: Missing `SETUP_HANDLER`, `THROW`, `CLEANUP`
4. **No Variable Management**: Missing allocation, scoping, lifetime tracking

## Evolution Strategy: HelloWorld.ek9 First Approach

### **Phase 1: HelloWorld IR Foundation**

Start with minimal viable IR components needed for the simplest EK9 program:

```ek9
#!ek9
defines module hello.world

defines program
  HelloWorld()
    Stdout().println("Hello, World!")
```

#### **Required IR Components**

```java
// Core instruction hierarchy
public abstract class IRInstruction implements INode {
  protected final IROpcode opcode;
  protected final ISymbol sourceSymbol;  // Link to EK9 symbol
}

public enum IROpcode {
  // Memory operations
  LOAD,           // Load from variable/field/constant
  STORE,          // Store to variable/field
  ALLOCA,         // Allocate local variable
  
  // Method calls
  CALL,           // Method call with resolved signature
  CALL_STATIC,    // Static method call
  
  // Control flow
  RETURN,         // Return from method
  
  // Object operations
  NEW,            // Create object
  FIELD_GET,      // Get field value
}

// Specific instructions for HelloWorld
public class LoadInstruction extends IRInstruction {
  private final String destination;   // Variable to load into
  private final String source;        // Variable/constant to load from
  private final String sourceType;    // Type for code generation
}

public class CallInstruction extends IRInstruction {
  private final String result;              // Variable to store result (optional)
  private final String targetObject;        // Object to call method on
  private final String methodName;          // Resolved method name
  private final String fullyQualifiedSig;   // Complete method signature
  private final List<String> arguments;     // Argument variables
  private final String jvmDescriptor;       // For ASM generation
}

public class ReturnInstruction extends IRInstruction {
  private final Optional<String> value;     // Variable to return (optional)
}

// Enhanced basic block
public class BasicBlock extends Block {
  private final String label;                    // Block label
  private final List<IRInstruction> instructions; // Typed instructions
  private final TerminatorInstruction terminator; // Explicit terminator
}
```

#### **HelloWorld IR Example**

```java
// Expected IR for HelloWorld program
BasicBlock main_entry:
  CALL stdout_obj = Stdout._new()
  LOAD hello_str = "Hello, World!"
  CALL stdout_obj.println(hello_str)
  RETURN
```

### **Phase 2: JVM Backend Assessment**

#### **Current JVM Backend Analysis**

Based on `src/main/java/org/ek9lang/compiler/backend/jvm`:

**Current Structure:**
```
backend/jvm/
├── AsmStructureCreator.java      // ASM bytecode generation
├── FullyQualifiedJvmName.java    // Name mangling
├── FullyQualifiedFileName.java   // File naming
├── OutputFileAccess.java         // File I/O
├── OutputVisitor.java            // Visitor for output
└── JvmTarget.java               // Target specification
```

#### **Compatibility Assessment** ✅

The existing JVM backend structure **aligns well** with our approach:

**Strengths:**
- **AsmStructureCreator**: Already handles ASM bytecode generation
- **Visitor pattern**: Integrates with IR visitor architecture
- **Target abstraction**: Good separation of concerns
- **File management**: Proper output handling

**Current Hardcoding Issues:**
```java
// From AsmStructureCreator.java (estimated based on typical patterns)
// ❌ Hardcoded assumptions about program structure
// ❌ Limited to specific EK9 constructs
// ❌ No instruction-level processing
```

#### **Enhancement Path for JVM Backend**

```java
// Enhanced AsmStructureCreator
public class AsmInstructionGenerator {
  
  public void generateLoadInstruction(LoadInstruction load) {
    // LOAD var = constant  →  LDC "constant"; ASTORE var_index
    // LOAD var = field     →  ALOAD obj_index; GETFIELD field_desc; ASTORE var_index  
  }
  
  public void generateCallInstruction(CallInstruction call) {
    // Uses resolved method signature from compile-time
    // No runtime method resolution needed
    methodVisitor.visitMethodInsn(
      INVOKEVIRTUAL,
      call.getOwnerClass(),
      call.getMethodName(), 
      call.getJvmDescriptor(),
      false
    );
  }
  
  public void generateBasicBlock(BasicBlock block) {
    // Generate label
    Label blockLabel = new Label();
    methodVisitor.visitLabel(blockLabel);
    
    // Generate instructions
    for (IRInstruction instruction : block.getInstructions()) {
      instruction.accept(this);  // Visitor pattern
    }
    
    // Generate terminator
    block.getTerminator().accept(this);
  }
}
```

### **Phase 3: Implementation Plan**

#### **Step 1: Minimal IR Extension** (Week 1-2)
```java
// Add to existing IR package
public abstract class IRInstruction implements INode { }
public enum IROpcode { LOAD, CALL, RETURN }
public class LoadInstruction extends IRInstruction { }
public class CallInstruction extends IRInstruction { }
public class ReturnInstruction extends IRInstruction { }
```

#### **Step 2: HelloWorld IR Generation** (Week 2-3)
```java
// Extend IRDefinitionVisitor to generate instruction-level IR
public class InstructionLevelIRGenerator {
  public List<BasicBlock> generateHelloWorldIR(EK9Parser.ProgramContext ctx) {
    // Generate instruction sequence for HelloWorld
  }
}
```

#### **Step 3: ASM Integration** (Week 3-4)
```java
// Enhance AsmStructureCreator
public void processInstructionLevelIR(List<BasicBlock> blocks) {
  for (BasicBlock block : blocks) {
    generateBasicBlock(block);
  }
}
```

#### **Step 4: Validation** (Week 4)
- Generate HelloWorld.class from IR
- Verify bytecode correctness
- Test execution: `java HelloWorld`
- Performance comparison with existing approach

### **Phase 4: Incremental Expansion**

Once HelloWorld works, incrementally add support for:
1. **Variables and assignments**
2. **Method calls with parameters**
3. **Control flow (if/while)**
4. **Exception handling**
5. **EK9-specific constructs (guards, etc.)**

## Detailed Component Requirements

### **IR Instruction Hierarchy**

```java
// Base instruction
public abstract class IRInstruction implements INode {
  protected final IROpcode opcode;
  protected final ISymbol sourceSymbol;    // Debug info
  protected final int lineNumber;          // Source line
  
  public abstract void accept(IRInstructionVisitor visitor);
}

// Memory operations
public class LoadInstruction extends IRInstruction {
  private final String destination;        // %temp1, %stdout_obj
  private final String source;             // "constant", field_name, variable_name
  private final LoadType loadType;         // CONSTANT, VARIABLE, FIELD
  private final String jvmType;           // Ljava/lang/String;
}

public class StoreInstruction extends IRInstruction {
  private final String destination;        // variable_name, field_name
  private final String source;             // %temp1
  private final StoreType storeType;       // VARIABLE, FIELD
}

// Method calls
public class CallInstruction extends IRInstruction {
  private final Optional<String> result;  // Variable to store result
  private final String targetObject;      // null for static calls
  private final String methodName;        // println, _new
  private final String ownerClass;        // org/ek9/lang/Stdout
  private final String jvmDescriptor;     // (Ljava/lang/String;)V
  private final List<String> arguments;   // [%hello_str]
  private final CallType callType;        // VIRTUAL, STATIC, CONSTRUCTOR
}

// Control flow
public class ReturnInstruction extends IRInstruction {
  private final Optional<String> value;   // Return value variable
}
```

### **Enhanced Basic Block**

```java
public class BasicBlock extends Block {
  private final String label;                    // "main_entry", "loop_start"
  private final List<IRInstruction> instructions; // Ordered instruction sequence
  private final TerminatorInstruction terminator; // RETURN, BRANCH, etc.
  private final Set<BasicBlock> predecessors;    // For optimization
  private final Set<BasicBlock> successors;      // For optimization
  
  public void addInstruction(IRInstruction instruction) {
    instructions.add(instruction);
  }
  
  public void setTerminator(TerminatorInstruction terminator) {
    this.terminator = terminator;
  }
}
```

### **ASM Generation Strategy**

```java
public class IRToAsmGenerator implements IRInstructionVisitor {
  private final MethodVisitor methodVisitor;
  private final Map<String, Integer> variableSlots;  // Variable name → local slot
  private final Map<String, Label> labelMap;         // Block name → ASM label
  
  @Override
  public void visitLoadInstruction(LoadInstruction load) {
    switch (load.getLoadType()) {
      case CONSTANT:
        methodVisitor.visitLdcInsn(load.getSource());
        methodVisitor.visitVarInsn(ASTORE, getVariableSlot(load.getDestination()));
        break;
      case VARIABLE:
        methodVisitor.visitVarInsn(ALOAD, getVariableSlot(load.getSource()));
        methodVisitor.visitVarInsn(ASTORE, getVariableSlot(load.getDestination()));
        break;
      case FIELD:
        // GETFIELD generation
        break;
    }
  }
  
  @Override
  public void visitCallInstruction(CallInstruction call) {
    // Load target object (if virtual call)
    if (call.getTargetObject() != null) {
      methodVisitor.visitVarInsn(ALOAD, getVariableSlot(call.getTargetObject()));
    }
    
    // Load arguments
    for (String arg : call.getArguments()) {
      methodVisitor.visitVarInsn(ALOAD, getVariableSlot(arg));
    }
    
    // Generate method call
    int opcode = switch (call.getCallType()) {
      case STATIC -> INVOKESTATIC;
      case VIRTUAL -> INVOKEVIRTUAL;
      case CONSTRUCTOR -> INVOKESPECIAL;
    };
    
    methodVisitor.visitMethodInsn(
      opcode,
      call.getOwnerClass(),
      call.getMethodName(),
      call.getJvmDescriptor(),
      false
    );
    
    // Store result (if any)
    if (call.getResult().isPresent()) {
      methodVisitor.visitVarInsn(ASTORE, getVariableSlot(call.getResult().get()));
    }
  }
}
```

## Success Criteria

### **Phase 1 Success: HelloWorld Execution**
- [ ] HelloWorld.ek9 compiles to instruction-level IR
- [ ] IR generates correct JVM bytecode via ASM
- [ ] Generated HelloWorld.class executes correctly
- [ ] Output: "Hello, World!" to console

### **Validation Steps**
1. **IR Generation**: Verify instruction sequence is logical
2. **Bytecode Verification**: Use `javap -c HelloWorld` to inspect
3. **Execution Test**: `java HelloWorld` produces expected output
4. **Performance Test**: Compare compilation time with existing approach

### **Quality Gates**
- No hardcoded assumptions in IR generation
- Clean separation between IR and ASM generation
- Extensible architecture for additional constructs
- Proper error handling and diagnostics

## Risk Mitigation

### **Technical Risks**
- **Symbol Resolution Integration**: Ensure IR properly uses compile-time resolution
- **ASM Complexity**: Start simple, add complexity incrementally
- **Performance Regression**: Monitor compilation performance

### **Mitigation Strategies**
- **Incremental Development**: Start with minimal HelloWorld support
- **Parallel Development**: Keep existing IR functional during transition
- **Comprehensive Testing**: Validate each component independently
- **Rollback Plan**: Maintain existing code generation as fallback

## Future Expansion Path

### **Post-HelloWorld Priorities**
1. **Variable Operations**: Local variables, assignments
2. **Method Parameters**: Function/method arguments and returns
3. **Object Creation**: Constructor calls, field initialization
4. **Control Flow**: If statements, loops
5. **Exception Handling**: Try/catch/finally blocks
6. **EK9 Constructs**: Guards, switches, advanced features

### **Architecture Evolution**
- **Optimization Passes**: Add IR-level optimizations
- **Multiple Targets**: Prepare for LLVM backend
- **Debug Information**: Maintain source-to-IR-to-bytecode mapping
- **Performance Tuning**: Profile and optimize hot paths

This plan provides a concrete, incremental approach to evolving the current IR toward our language-agnostic design goals, starting with the simplest possible case and building complexity systematically.