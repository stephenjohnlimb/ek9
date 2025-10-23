# Switch Statement IR Generation - Implementation Notes

**Date**: 2025-10-23
**Session**: Initial implementation of statement-form switch with literal cases

---

## Completed (Session 2025-10-23)

✅ **simpleSwitchLiteral.ek9** - Statement form with integer literal cases

### Files Created/Modified

1. **Test File**: `compiler-main/src/test/resources/examples/irGeneration/controlFlow/simpleSwitchLiteral.ek9`
2. **Generator**: `compiler-main/src/main/java/org/ek9lang/compiler/phase7/generator/SwitchStatementGenerator.java`
3. **Registration**: Modified `GeneratorSet.java` and `IRConstructGenerators.java`
4. **Integration**: Modified `StmtInstrGenerator.java` to route switch statements to generator

---

## Architecture Decisions

### Evaluation Variable Placement

**Decision**: Evaluation variable created in CHAIN scope (not separate scope)

**Rationale**:
- Evaluation happens ONCE at switch start
- Variable must live for ENTIRE switch duration
- All case comparisons reference the same variable
- Released automatically when chain scope exits

**Pattern**:
```
SCOPE_ENTER chainScopeId
  // Evaluate switch expression here
  _temp1 = LOAD value
  RETAIN _temp1
  SCOPE_REGISTER _temp1, chainScopeId  // Lives for entire switch

  CONTROL_FLOW_CHAIN [
    evaluation_variable: _temp1  // Reference to existing variable
    ...
  ]
SCOPE_EXIT chainScopeId  // _temp1 released here
```

### Memory Management Pattern

**CRITICAL**: ALL EK9 object operations need RETAIN + SCOPE_REGISTER

✅ **Correct Pattern** (used in implementation):
```
// LOAD evaluation variable
_temp3 = LOAD _temp1
RETAIN _temp3
SCOPE_REGISTER _temp3, scopeId

// LOAD_LITERAL case value
_temp4 = LOAD_LITERAL 1, org.ek9.lang::Integer
RETAIN _temp4
SCOPE_REGISTER _temp4, scopeId

// CALL _eq operator - CALL RESULT NEEDS MEMORY MANAGEMENT!
_temp2 = CALL (org.ek9.lang::Boolean)_temp3._eq(_temp4)
RETAIN _temp2  // ← CRITICAL
SCOPE_REGISTER _temp2, scopeId  // ← CRITICAL

// Extract primitive boolean (no memory management needed)
_temp5 = CALL (java.lang.Boolean)_temp2._true()
```

### Scope Structure

1. **Chain scope** (entire switch)
2. **Tight condition scope per case** (exits immediately after primitive boolean extraction)
3. **Branch scope per case body**
4. **Default case branch scope**

**Example**:
```
SCOPE_ENTER _scope_1 (chain scope)
  CONTROL_FLOW_CHAIN [
    condition_chain: [
      // Case 1
      [
        condition_evaluation: [
          SCOPE_ENTER _scope_2 (tight condition scope)
            // Comparison logic
          SCOPE_EXIT _scope_2
        ]
        body_evaluation: [
          SCOPE_ENTER _scope_3 (branch scope)
            // Case body
          SCOPE_EXIT _scope_3
        ]
      ]
    ]
  ]
SCOPE_EXIT _scope_1
```

---

## Implementation Details

### Call Details Construction

**Uses**: `CallDetailsBuilder` with `CallContext.forBinaryOperation()`

**Pattern** (for _eq operator):
```java
// Get symbols
final var evalVarTypeSymbol = evalVariable.typeSymbol();  // From evaluation variable
final var caseValueSymbol = getRecordedSymbolOrException(caseExpr.expression());
final var returnType = exprSymbol.getType().orElseThrow();  // Boolean

// Get method name from operator map
final var operatorMap = new OperatorMap();
final var methodName = operatorMap.getForward("==");  // Returns "_eq"

// Create call context
final var callContext = CallContext.forBinaryOperation(
    evalVarTypeSymbol,
    caseValueSymbol,
    returnType,
    methodName,
    evalVarLoaded,      // Target variable
    caseValue,          // Argument variable
    conditionScopeId
);

// Build call details with promotion support
final var callDetailsResult = generators.callDetailsBuilder.apply(callContext);
conditionEvaluation.addAll(callDetailsResult.allInstructions());  // Add promotions

// Generate call
conditionEvaluation.add(CallInstr.operator(
    new VariableDetails(comparisonResult, debugInfo),
    callDetailsResult.callDetails()
));
```

### EvalVariable Record

**Purpose**: Store evaluation variable information across processing

```java
private record EvalVariable(String name, String type, ISymbol typeSymbol) {}
```

**Why include typeSymbol?**
- Needed for CallContext creation
- Avoids redundant symbol resolution
- Passed through from initial expression evaluation

---

## Test File Structure

**simpleSwitchLiteral.ek9**:
```ek9
simpleSwitchLiteral()
  -> value as Integer
  <-
    result <- String()

    switch value
      case 1
        result := "One"
      case 2
        result := "Two"
      default
        result := "Other"
```

**Characteristics**:
- Statement form (no return value from switch itself)
- Integer type (no enums)
- Literal comparisons (case 1, case 2)
- One literal per case (not `case 1, 2, 3`)
- No guards (just `switch value`)
- Simple body statements (direct assignment)

---

## Expected IR Structure

```
SCOPE_ENTER _scope_1  // Chain scope
  // Evaluate switch expression
  _temp1 = LOAD value
  RETAIN _temp1
  SCOPE_REGISTER _temp1, _scope_1

  CONTROL_FLOW_CHAIN [
    chain_type: "SWITCH"
    evaluation_variable: _temp1
    evaluation_variable_type: "org.ek9.lang::Integer"

    condition_chain: [
      // Case 1
      [
        case_scope_id: _scope_3
        case_type: "LITERAL"
        condition_evaluation: [
          SCOPE_ENTER _scope_2
            _temp3 = LOAD _temp1
            RETAIN _temp3
            SCOPE_REGISTER _temp3, _scope_2
            _temp4 = LOAD_LITERAL 1, org.ek9.lang::Integer
            RETAIN _temp4
            SCOPE_REGISTER _temp4, _scope_2
            _temp2 = CALL (org.ek9.lang::Boolean)_temp3._eq(_temp4)
            RETAIN _temp2  // ← CRITICAL
            SCOPE_REGISTER _temp2, _scope_2  // ← CRITICAL
            _temp5 = CALL (java.lang.Boolean)_temp2._true()
          SCOPE_EXIT _scope_2
        ]
        condition_result: _temp2
        primitive_condition: _temp5
        body_evaluation: [
          SCOPE_ENTER _scope_3
            // result := "One" statements
          SCOPE_EXIT _scope_3
        ]
      ],
      // Case 2 - similar structure
    ]

    default_body_evaluation: [
      SCOPE_ENTER _scope_X
        // result := "Other" statements
      SCOPE_EXIT _scope_X
    ]

    scope_id: _scope_1
  ]
SCOPE_EXIT _scope_1
```

---

## Current Implementation Status

### ✅ Implemented

1. Statement-form switch (no return value)
2. Integer type evaluation variable
3. Literal case comparisons (case 1, case 2)
4. Default case support
5. Full memory management (RETAIN + SCOPE_REGISTER on all EK9 objects)
6. Proper scope structure (chain → condition → branch)
7. Integration with `StmtInstrGenerator`
8. Validation exceptions for unsupported features

### ❌ Not Yet Implemented

1. **Expression form** (switch returning value)
   - Requires `returningParam` processing
   - Return variable setup and initialization

2. **Guard variables**
   - `switch guard <- getGuard() ...`
   - Guard scope management

3. **Enum cases** (placeholder needed)
   - Enum type detection via `genus == CLASS_ENUMERATION`
   - EnumOptimizationDetails creation
   - Jump table potential for backends

4. **Multiple literals per case** (case 1, 2, 3)
   - OR chain generation: `(eval == 1) OR (eval == 2) OR (eval == 3)`
   - Requires `generateOrChain()` implementation

5. **Expression cases** (case < 12, case > 50)
   - Operator extraction from `caseExpr.op`
   - Different operator method calls (_lt, _gt, etc.)

---

## Next Steps (Priority Order)

### 1. Validate and Fill @IR Directive
```bash
# Run compiler to generate actual IR
mvn test -Dtest=<appropriate_test> -pl compiler-main

# Copy generated IR to @IR directive in simpleSwitchLiteral.ek9
```

### 2. Add Expression Cases (case < 12)
Create `simpleSwitchExpression.ek9`:
```ek9
switchWithExpressionCases()
  -> value as Integer
  <-
    result <- String()

    switch value
      case < 12
        result := "Low"
      case > 50
        result := "High"
      default
        result := "Medium"
```

**Implementation**:
- Check `caseExpr.op != null`
- Use `operatorMap.getForward(caseExpr.op.getText())`
- Same CallContext pattern, different operator

### 3. Add Enum Placeholder
Create `switchEnumPlaceholder.ek9`:
```ek9
switchEnumExample()
  -> check as SimpleEnum
  <-
    result <- String()

    switch check
      case SimpleEnum.Alpha
        result := "It's Alpha"
      case SimpleEnum.Beta
        result := "It's Beta"
      default
        result := "Something else"
```

**Implementation**:
```java
private boolean isEnumerationType(ISymbol symbol) {
  return symbol.getGenus() == ISymbol.SymbolGenus.CLASS_ENUMERATION;
}

private ConditionCaseDetails processCaseStatementEnum(...) {
  // Extract enum constant name
  // Generate sequential _eq comparison for now
  // Create ConditionCaseDetails.createEnumCase() with ordinal = -1
  // Populate EnumOptimizationDetails placeholder
}
```

### 4. Add Multiple Literals Per Case
```ek9
case 1, 2, 3
  result := "One, Two, or Three"
```

**Implementation**:
```java
private String generateCaseExpressionChain(
    List<CaseExpressionContext> caseExpressions, ...) {

  if (caseExpressions.size() == 1) {
    return generateSingleCaseComparison(...);
  }

  // Generate OR chain: comp1 OR comp2 OR comp3
  var comparisons = new ArrayList<String>();
  for (var caseExpr : caseExpressions) {
    comparisons.add(generateSingleCaseComparison(...));
  }

  return generateOrChain(comparisons, instructions);
}
```

### 5. Expression Form (switch as expression)
```ek9
resultText <- switch conditionVariable
  <- result <- String()
  case 1
    result :=? "One"
  default
    result :=? "Other"
```

**Implementation**:
- Process `returningParam` context
- Setup return variable with initialization
- Pass return details to `ControlFlowChainDetails.createSwitch()`

### 6. Guard Variables
```ek9
switch guard <- getGuard()
  case guardValue < 10
    ...
```

**Implementation**: Follow `IfStatementGenerator` guard pattern

---

## Testing Commands

```bash
# Compile
mvn clean compile -pl compiler-main

# Run all tests
mvn test -pl compiler-main

# Run specific test (TBD - need to find/create appropriate test)
mvn test -Dtest=<TestName> -pl compiler-main

# Check specific EK9 file compilation
java -jar compiler-main/target/ek9c-jar-with-dependencies.jar -C <file.ek9>
```

---

## Lessons Learned

### 1. Memory Management is Non-Negotiable
- Every LOAD needs RETAIN + SCOPE_REGISTER
- Every CALL returning EK9 object needs RETAIN + SCOPE_REGISTER
- CALL returning primitive needs NO memory management
- This pattern must be followed religiously

### 2. Symbol Resolution via getRecordedSymbolOrException
- Use for AST nodes that have been resolved in phases 1-6
- Returns ISymbol with guaranteed type information
- For type names, use separate symbol table resolution

### 3. OperatorMap Usage
- Create instance: `new OperatorMap()`
- Get method name: `operatorMap.getForward("==")`  → `"_eq"`
- Instance method, not static

### 4. CallContext Pattern for Operators
- Use `CallContext.forBinaryOperation()` for all binary operators
- Provides cost-based method resolution
- Handles promotions automatically
- Returns `CallDetailsResult` with promotion instructions

### 5. Scope Management
- Evaluation variable: Chain scope (lives for entire switch)
- Condition evaluation: Tight scope (exits immediately)
- Branch body: Unique scope per case
- Stack-based: Use `stackContext.currentScopeId()`

### 6. EvalVariable Record Design
- Store symbol WITH type information
- Avoids redundant resolution
- Simplifies CallContext creation

---

## Key Design Patterns

### Pattern 1: Evaluation Variable Inline
```java
// DON'T create separate evaluation scope
// DO evaluate inline in chain scope
instructions.add(ScopeInstr.enter(chainScopeId, debugInfo));

// Evaluate immediately - variable registered in chain scope
final var evalVariable = evaluateSwitchExpressionInline(ctx, instructions);

// Now available for all case comparisons
```

### Pattern 2: Memory Management Wrapper
```java
// ALWAYS use variableMemoryManagement for EK9 object operations
instructions.addAll(
    generators.variableMemoryManagement.apply(
        () -> generators.exprGenerator.apply(details),
        variableDetails
    )
);
```

### Pattern 3: Tight Condition Scope
```java
// Enter tight scope
SCOPE_ENTER conditionScopeId

  // All condition temps registered here
  _temp = LOAD ...
  RETAIN _temp
  SCOPE_REGISTER _temp, conditionScopeId

  // Extract primitive boolean
  _primitive = CALL boolean._true()

SCOPE_EXIT conditionScopeId  // All temps released

// Primitive boolean persists (no memory management)
```

---

## Compilation Status

✅ **Compiles successfully** (2025-10-23)
✅ **Integration complete** (registered in all generators)
⏳ **Pending IR validation** (need to run actual test and validate output)

---

## Notes for Future Sessions

1. **Test Infrastructure**: Need to identify or create appropriate test class that validates `@IR` directives
2. **IR Output Validation**: Once IR is generated, copy to `@IR` directive and verify structure
3. **Enum Support**: Requires completion of enum/enumeration implementation in EK9 first
4. **OR Chain**: Straightforward - just need to implement the iteration pattern
5. **Expression Cases**: Similar to literal cases, just different operator method call

---

## References

- **Similar Implementation**: `IfStatementGenerator.java`
- **Memory Management**: `VariableMemoryManagement.java`
- **Call Details**: `CallDetailsBuilder.java`
- **Operator Mapping**: `OperatorMap.java`
- **CONTROL_FLOW_CHAIN**: `ControlFlowChainInstr.java`, `ControlFlowChainDetails.java`
