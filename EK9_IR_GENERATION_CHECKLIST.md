# EK9 IR Generation Quality Checklist

This document provides a mandatory checklist for all IR generation code in the EK9 compiler. These items must be verified for every IR generation feature to ensure correctness and maintainability.

## 🚨 **Mandatory Verification Items**

### **1. Debug Line Numbers**

**✅ ALWAYS CHECK**: Debug line numbers must point to the **correct source location**

**Common Issues**:
- Using symbol declaration line instead of actual operation line
- Using `lhsSymbol.getSourceToken()` when should use operation token
- Missing debug info entirely

**Correct Patterns**:
```java
// ❌ WRONG: Points to symbol declaration
final var debugInfo = debugInfoCreator.apply(lhsSymbol.getSourceToken());

// ✅ CORRECT: Points to actual operation
final var assignmentToken = new Ek9Token(ctx.op);
final var debugInfo = debugInfoCreator.apply(assignmentToken);
```

**Verification Method**:
- Run tests with `showIR = true`
- Check that debug comments point to the correct line numbers
- Example: `x += 5` should show `// ./file.ek9:17:??`, not `// ./file.ek9:16:??`

### **2. Memory Management**

**✅ ALWAYS CHECK**: All temporary variables must have proper memory management

**Required Pattern**:
```ir
_temp1 = LOAD something
RETAIN _temp1
SCOPE_REGISTER _temp1, _scope_id
```

**Common Issues**:
- Missing RETAIN instructions
- Missing SCOPE_REGISTER instructions  
- Using immutable lists with VariableMemoryManagement
- Not using VariableMemoryManagement wrapper

**Correct Implementation Pattern**:
```java
// Use VariableMemoryManagement wrapper
final var variableMemoryManagement = new VariableMemoryManagement();
final var instructions = variableMemoryManagement.apply(() -> {
    final var list = new ArrayList<IRInstr>();  // ← MUST be mutable
    list.add(someInstruction);
    return list;
}, variableDetails);
```

**Verification Method**:
- Run tests with `showIR = true`
- Verify every temporary variable has RETAIN and SCOPE_REGISTER
- Compare with working examples (e.g., existing binary operators)

### **3. Void Return Type Handling**

**✅ ALWAYS CHECK**: Methods returning Void must not generate result variables

**Pattern for Void methods**:
```java
if ("org.ek9.lang::Void".equals(returnType)) {
    // No result variable for Void methods
    instructions.add(CallInstr.operator(null, debugInfo, callDetails));
} else {
    // Generate result variable for non-Void methods
    final var resultVariable = context.generateTempName();
    instructions.add(CallInstr.operator(resultVariable, debugInfo, callDetails));
}
```

**Common Issues**:
- Creating result variables for Void-returning methods
- Attempting to store Void results
- Incorrect STORE instructions for mutating operations

### **4. Method Resolution**

**✅ ALWAYS CHECK**: Actual return types must be resolved from method signatures

**Correct Pattern**:
```java
// ❌ WRONG: Assume return type
final var returnType = leftType; 

// ✅ CORRECT: Resolve actual return type
final var returnType = resolveBinaryMethodReturnType(leftSymbol, rightSymbol, operatorName);
```

**Verification**: Use method resolution to determine if operators return the original type or Void.

## **🔍 Verification Workflow**

### **For Every IR Generation Feature**:

1. **Implement the feature**
2. **Run tests with `showIR = true`**
3. **Verify debug line numbers** point to correct source locations
4. **Verify memory management** - all temps have RETAIN/SCOPE_REGISTER
5. **Verify Void handling** - no result variables for Void methods  
6. **Verify method resolution** - return types are correctly resolved
7. **Compare with similar working features** for consistency
8. **Set `showIR = false` and run final tests**

### **Example Verification Commands**:
```bash
# Enable IR output
# Set showIR = true in test constructor

# Run specific test 
mvn test -Dtest=YourIRTest -pl compiler-main

# Check for patterns:
# - Debug line numbers: // ./file.ek9:LINE:COL
# - Memory management: RETAIN _temp, SCOPE_REGISTER _temp
# - Void methods: No result variables
# - Method calls: Correct operator names (_addAss, _subAss, etc.)
```

## **🔒 ValidOperatorOrError Semantic Rules**

**CRITICAL**: EK9 enforces strict operator semantics through ValidOperatorOrError to prevent C++-style operator abuse.

### **Assignment Operator Hard Rules**
Located in `compiler-main/src/main/java/org/ek9lang/compiler/phase2/ValidOperatorOrError.java`:

```java
// mutatorChecks Map (lines 144-155)
final Map<String, Consumer<MethodSymbol>> mutatorChecks = Map.of(
    "+=", addNonPureCheck(this::oneArgumentNoReturnOrError),
    "-=", addNonPureCheck(this::oneArgumentNoReturnOrError), 
    "*=", addNonPureCheck(this::oneArgumentNoReturnOrError),
    "/=", addNonPureCheck(this::oneArgumentNoReturnOrError),
    // ... other mutating operators
);
```

**Enforced Rules**:
1. **Must NOT be pure** (`addNonPureCheck` - prevents "as pure" qualifier)
2. **Must take exactly one argument** (`oneArgumentOrError`) 
3. **Must return Void or nothing** (`noReturnOrError` - validates Void return type)

### **Defensive IR Generation Pattern**
In IR generation, add defensive assertions to catch ValidOperatorOrError violations:

```java
// Assignment operators MUST return Void - enforced by ValidOperatorOrError semantic rules
AssertValue.checkTrue("Assignment operator " + ctx.op.getText() + " must return Void, got: " + returnType,
    "org.ek9.lang::Void".equals(returnType));
```

### **Adding New Strict Operator Rules**
When adding new operators that require strict semantics:

1. **Add to appropriate check map** in ValidOperatorOrError.populateOperatorChecks()
2. **Use helper methods**:
   - `addPureCheck()` - for operators that MUST be pure
   - `addNonPureCheck()` - for operators that CANNOT be pure
   - `oneArgumentReturnTypeBooleanOrError()` - for comparison operators
   - `noArgumentsReturnTypeIntegerOrError()` - for specific return type requirements
3. **Add defensive assertions** in corresponding IR generation code
4. **Document the semantic rationale** - why this operator needs strict rules

### **Existing Strict Rule Categories**

**Logical Operators**: `<`, `<=`, `>`, `>=`, `==`, `<>` - MUST be pure, MUST return Boolean
**Mutating Operators**: `+=`, `-=`, `*=`, `/=` - CANNOT be pure, MUST return Void  
**Type Conversion**: `$` (String), `$$` (JSON), `#?` (hashcode) - MUST be pure, specific return types
**Comparison**: `<=>` (cmp), `<~>` (fuzzy) - MUST be pure, MUST return Integer

**Purpose**: Prevent operator overloading abuse and maintain consistent, predictable semantics for both AI code generation and human developers.

## **📋 Common Anti-Patterns to Avoid**

### **❌ Debug Info Anti-Patterns**:
- Using symbol tokens instead of operation tokens
- Missing debug info entirely
- Inconsistent debug info across related instructions

### **❌ Memory Management Anti-Patterns**:  
- Missing RETAIN/SCOPE_REGISTER for temporary variables
- Using immutable lists with VariableMemoryManagement
- Manual memory management instead of using wrappers

### **❌ Type System Anti-Patterns**:
- Assuming return types without method resolution  
- Incorrect handling of Void-returning methods
- Missing type validation

### **❌ Testing Anti-Patterns**:
- Not using `showIR = true` during development
- Not comparing generated IR with working examples
- Skipping verification of memory management patterns

## **✅ Success Criteria**

**An IR generation feature is correct when**:

1. **All debug line numbers** point to the correct source code locations
2. **All temporary variables** have proper RETAIN and SCOPE_REGISTER instructions  
3. **Void-returning methods** generate calls without result variables
4. **Non-Void methods** properly generate and manage result variables
5. **Method resolution** correctly determines return types
6. **Generated IR** follows established patterns from similar features
7. **Tests pass** with both `showIR = true` and `showIR = false`

---

**💡 Remember**: IR generation correctness is critical for EK9's memory management and debugging capabilities. Always verify these items before considering any IR generation feature complete.