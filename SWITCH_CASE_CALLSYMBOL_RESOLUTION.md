# Switch Case CallSymbol Resolution Architecture

## Architectural Principle: Separation of Concerns Across Phases

### The Established EK9 Compiler Pattern

**CRITICAL**: The EK9 compiler follows a strict separation of concerns across compilation phases. This is not arbitrary - it's fundamental to maintainability and correctness.

```
Phases 1-2:  Create symbol STRUCTURES (empty shells)
Phases 3-6:  FULLY RESOLVE all symbols (types, methods, promotion requirements)
Phase 7:     MECHANICAL IR generation from RESOLVED symbols
```

### Pattern Used for Binary Operations (e.g., `a + b`, `x == y`)

**Phase 1 - SYMBOL_DEFINITION (DefinitionListener.java:1070-1076):**
```java
@Override
public void enterOperationCall(final EK9Parser.OperationCallContext ctx) {
  // Create CallSymbol structure (empty shell)
  symbolsAndScopes.recordSymbol(
      symbolFactory.newOperationCall(ctx, symbolsAndScopes.getTopScope()), ctx);
  super.enterOperationCall(ctx);
}
```
- Creates CallSymbol
- Records on operationCall context
- **No resolution** - just structure

**Phase 3 - FULL_RESOLUTION (CallOrError.java):**
```java
// Get the CallSymbol created in Phase 1
final var callSymbol = (CallSymbol) symbolsAndScopes.getRecordedSymbol(ctx);

// Resolve the actual method to call (e.g., Integer._add(Integer))
final var resolvedMethod = /* ... method resolution with fuzzy matching ... */;

// Set the resolved method - THIS IS THE KEY STEP
callSymbol.setResolvedSymbolToCall(resolvedMethod);
```
- Gets CallSymbol (created in Phase 1)
- Performs method resolution (exact match or fuzzy with promotion)
- **Sets `resolvedSymbolToCall`** - CallSymbol now fully resolved
- No IR generation - just resolution

**Phase 7 - IR_GENERATION (BinaryOperationGenerator.java:68-101):**
```java
// Get operand symbols
final var leftSymbol = getRecordedSymbolOrException(leftExpr);
final var rightSymbol = getRecordedSymbolOrException(rightExpr);

// Get the RESOLVED CallSymbol
final var exprSymbol = getRecordedSymbolOrException(ctx);
if (exprSymbol instanceof CallSymbol cs) {
  final var resolvedMethod = cs.getResolvedSymbolToCall();  // Already resolved in Phase 3!
  // Extract return type from resolved method
  // ...
}

// Create simple CallContext - just plumbing
final var callContext = CallContext.forBinaryOperation(
    leftSymbol, rightSymbol, returnType, methodName,
    leftVariable, rightVariable, scopeId
);

// CallDetailsBuilder generates IR MECHANICALLY
// It uses the resolved method, detects promotion needs, generates promotion IR
final var callDetailsResult = generators.callDetailsBuilder.apply(callContext);

// Add promotion instructions (if any)
instructions.addAll(callDetailsResult.allInstructions());
```
- Gets **already-resolved** CallSymbol from Phase 3
- Extracts resolved method information
- Mechanical IR generation
- **No complex resolution logic** - that's done in Phase 3!

### Current BROKEN Pattern for Switch Cases

**Phase 1 - SYMBOL_DEFINITION:**
- ✓ Creates CallSymbol for case operator (correct!)

**Phase 3 - FULL_RESOLUTION:**
- ✗ CaseExpressionOrError **OVERWRITES** CallSymbol with value symbol
- ✗ **No resolution happens** - `resolvedSymbolToCall` stays NULL
- ✗ Violates phase separation principle

**Phase 7 - IR_GENERATION:**
- ✗ CallSymbol doesn't exist (ConstantSymbol overwrote it)
- ✗ **Tries to resolve method from scratch during IR generation**
- ✗ **Doing Phase 3's job in Phase 7!**
- ✗ Violates architectural principle

### Why This Matters

**Steve's Rationale**: "This has left us too much to do in phase 7. We should pull as much of that processing and structuring as early as reasonably as possible. This spreads the work correctly across the phases, leaving the IR (phase 7) to do much less work and just focus on the IR generation (which is hard enough as it is)."

**Consequences of Broken Pattern**:
1. **Architectural Violation**: Mixing resolution logic with IR generation
2. **Code Duplication**: Method resolution logic appears in both Phase 3 (for normal operators) and Phase 7 (for switch)
3. **Maintenance Burden**: Changes to resolution must be synchronized across phases
4. **Testing Complexity**: Can't validate resolution separately from IR generation
5. **Performance**: Doing expensive resolution during IR generation instead of earlier
6. **Debugging Difficulty**: Resolution errors appear during IR generation, not where they belong

**The Fix**: Align switch processing with the established pattern used everywhere else in the compiler.

## Problem Statement

### Grammar Structure
```antlr
switchStatementExpression
    : (SWITCH|GIVEN) preFlowAndControl NL+ INDENT (NL* returningParam)?
      caseStatement+ (directive? DEFAULT block NL+)?
      DEDENT
    ;

caseStatement
    : directive? (CASE|WHEN) caseExpression (COMMA caseExpression)* block NL+
    ;

caseExpression
    : call
    | objectAccessExpression
    | op=(EQUAL | LE | GE | GT | LT | NOTEQUAL | NOTEQUAL2 | MATCHES | CONTAINS) expression
    | primary
    ;
```

### Semantic Meaning

Each `caseExpression` represents an implicit operator call:

**Source Code:**
```ek9
switch text
  case 'D'              // Implicit: text._eq('D') → String._eq(Character)
    result := "Found D"
  case == "Hello"       // Explicit: text._eq("Hello") → String._eq(String)
    result := "Found Hello"
  case < 10             // For numeric: value._lt(10) → Integer._lt(Integer)
    result := "Less than 10"
```

**Desugared Semantics:**
```ek9
case 'D'           →  text._eq('D')       →  String._eq(Character) with _promote()
case == "Hello"    →  text._eq("Hello")   →  String._eq(String)
case < 10          →  value._lt(10)       →  Integer._lt(Integer)
```

### Multiple Cases (OR Logic)

```ek9
case 'D', 'A', 'Z'
  result := "Vowel or D"
```

Maps to: `(text._eq('D')) OR (text._eq('A')) OR (text._eq('Z'))`

Each caseExpression gets its own resolved CallSymbol.

## Root Cause Analysis

### Issue 1: CaseExpressionOrError Overwrites CallSymbol

**Phase 1 (DefinitionListener.java:1078-1087):**
```java
@Override
public void enterCaseExpression(final EK9Parser.CaseExpressionContext ctx) {
  // Creates CallSymbol and records on caseExpression context ✓
  symbolsAndScopes.recordSymbol(
      symbolFactory.newCaseExpressionCall(ctx, symbolsAndScopes.getTopScope()), ctx);
  super.enterCaseExpression(ctx);
}
```

**Phase 3 (CaseExpressionOrError.java:37-48):**
```java
@Override
public void accept(final EK9Parser.CaseExpressionContext ctx) {
  // Gets value symbol from child context
  if (ctx.primary() != null) {
    // Overwrites the CallSymbol created in Phase 1!
    recordAgainstContext(getRecordedAndTypedSymbol(ctx.primary()), ctx);
  }
  // ... similar for call, objectAccessExpression, expression
}
```

**Problem**: `recordAgainstContext()` overwrites the CallSymbol with the value symbol.

**Why This Happened**: The value symbols are **already recorded on their child contexts** (ctx.primary(), ctx.expression(), etc.) by earlier processing. CaseExpressionOrError redundantly copies them to the parent caseExpression context "for convenience" - but this destroys the CallSymbol!

### Issue 2: No Resolution in Phase 3

**Phase 3 (SwitchStatementExpressionOrError.java):**
- Current `operatorExistsOrError()` only **validates** operator exists
- Does **NOT** resolve the actual method to call
- Does **NOT** set `callSymbol.setResolvedSymbolToCall()`
- Violates phase separation principle

### Issue 3: Resolution in Wrong Phase

**Phase 7 (SwitchStatementGenerator.java:208-237):**
```java
// WRONG: Trying to resolve method during IR generation
final var callContext = CallContext.forBinaryOperation(
    evalVarSymbol,      // Created during IR generation
    caseValueSymbol,    // Created during IR generation
    ...
);

// WRONG: Method resolution during IR generation
final var callDetailsResult = generators.callDetailsBuilder.apply(callContext);
```

**Problem**: Method resolution belongs in Phase 3, not Phase 7!

## Correct Architecture: Follow Established Pattern

### Phase 1: SYMBOL_DEFINITION - Create CallSymbol Structure

**File**: `compiler-main/src/main/java/org/ek9lang/compiler/phase1/DefinitionListener.java`

**Status**: ✓ Already implemented correctly (lines 1078-1087)

**Purpose**: Create empty CallSymbol shell for each caseExpression

**What Gets Created**:
```java
@Override
public void enterCaseExpression(final EK9Parser.CaseExpressionContext ctx) {
  // Determine operator: explicit or inferred equality
  // CallSymbol created via: symbolFactory.newCaseExpressionCall(ctx, scope)
  //   - Name: "==" (inferred) or ctx.op.getText() (explicit)
  //   - Operator: true
  //   - Scope: current scope
  //   - resolvedSymbolToCall: NULL (will be set in Phase 3)

  symbolsAndScopes.recordSymbol(
      symbolFactory.newCaseExpressionCall(ctx, symbolsAndScopes.getTopScope()), ctx);
  super.enterCaseExpression(ctx);
}
```

### Phase 3: FULL_RESOLUTION - Resolve CallSymbol Method

**CRITICAL**: This phase must **completely resolve** the CallSymbol, following the same pattern as CallOrError for normal operators.

#### Step 1: Prevent CaseExpressionOrError from Overwriting CallSymbol

**File**: `compiler-main/src/main/java/org/ek9lang/compiler/phase3/ExpressionsListener.java`

**Line**: ~309-312

**Current Code (WRONG)**:
```java
@Override
public void exitCaseExpression(final EK9Parser.CaseExpressionContext ctx) {
  caseExpressionOrError.accept(ctx);  // ← OVERWRITES CallSymbol!
  super.exitCaseExpression(ctx);
}
```

**Correct Fix (Option A - PREFERRED)**:
```java
@Override
public void exitCaseExpression(final EK9Parser.CaseExpressionContext ctx) {
  // Value symbols are ALREADY on child contexts (ctx.primary(), ctx.expression(), etc.)
  // Recording them on parent context is REDUNDANT and OVERWRITES the CallSymbol
  // The CallSymbol created in Phase 1 MUST remain on caseExpression context
  // It will be resolved in SwitchStatementExpressionOrError
  super.exitCaseExpression(ctx);
}
```

**Alternative Fix (Option B)**:
```java
// Modify CaseExpressionOrError.java to preserve CallSymbol
@Override
public void accept(final EK9Parser.CaseExpressionContext ctx) {
  final var existingSymbol = symbolsAndScopes.getRecordedSymbol(ctx);

  // If CallSymbol exists from Phase 1, preserve it
  if (existingSymbol instanceof CallSymbol) {
    return;  // Don't overwrite - CallSymbol must stay for resolution
  }

  // Fallback for other scenarios (if any exist)
  // ... existing recording logic ...
}
```

**Recommendation**: Use Option A - removes redundant processing entirely.

#### Step 2: Resolve CallSymbol in SwitchStatementExpressionOrError

**File**: `compiler-main/src/main/java/org/ek9lang/compiler/phase3/SwitchStatementExpressionOrError.java`

**Method**: `operatorExistsOrError(caseExpressionContext, controlSymbol)`

**Current Implementation (INCOMPLETE)**:
```java
private void operatorExistsOrError(final EK9Parser.CaseExpressionContext ctx, final ISymbol controlSymbol) {
  final var caseVariable = getRecordedAndTypedSymbol(ctx);  // Gets ConstantSymbol - WRONG!

  controlSymbol.getType().ifPresent(controlType -> {
    if (controlType instanceof IAggregateSymbol aggregate) {
      final var search = new MethodSymbolSearch(operator.getText())...;
      if (aggregate.resolve(search).isEmpty()) {
        emitMethodNotResolvedError(...);  // Only validates - doesn't resolve!
      }
    }
  });
}
```

**Correct Implementation (COMPLETE RESOLUTION)**:
```java
private void operatorExistsOrError(final EK9Parser.CaseExpressionContext ctx, final ISymbol controlSymbol) {

  // STEP 1: Get the CallSymbol created in Phase 1
  final var symbol = symbolsAndScopes.getRecordedSymbol(ctx);

  if (!(symbol instanceof CallSymbol callSymbol)) {
    // This should not happen if Phase 1 worked correctly
    throw new CompilerException("Expected CallSymbol for case expression, but got: "
        + (symbol != null ? symbol.getClass().getSimpleName() : "null"));
  }

  // STEP 2: Get case VALUE type from CHILD context
  // The value symbol is on ctx.primary(), ctx.expression(), etc. - NOT on ctx itself!
  final var valueSymbol = getValueSymbolFromChildContext(ctx);

  // STEP 3: Ensure we have typed symbols
  if (valueSymbol == null || valueSymbol.getType().isEmpty()) {
    // Error already emitted by earlier processing
    return;
  }

  // STEP 4: Get switch variable type and resolve method
  controlSymbol.getType().ifPresent(controlType -> {
    if (controlType instanceof IAggregateSymbol aggregate) {

      // Ensure parameterized types are fully resolved
      if (aggregate.isParameterisedType()
          && aggregate instanceof org.ek9lang.compiler.symbols.PossibleGenericSymbol possibleGeneric) {
        symbolsAndScopes.resolveOrDefine(possibleGeneric, errorListener);
      }

      // STEP 5: Resolve method with FUZZY MATCHING (includes promotion)
      // MethodSymbolSearch with operator name and parameter type
      final var search = new MethodSymbolSearch(callSymbol.getName())
          .addTypeParameter(valueSymbol.getType());

      final var result = new MethodSymbolSearchResult();

      // resolveMatchingMethods uses SymbolMatcher for cost-based fuzzy matching
      // This automatically handles promotion (Character → String)
      aggregate.resolveMatchingMethods(search, result);
      final var bestMatch = result.getSingleBestMatchSymbol();

      if (bestMatch.isPresent()) {
        // STEP 6: Set resolved method on CallSymbol
        // THIS IS THE CRITICAL STEP - makes CallSymbol fully resolved
        callSymbol.setResolvedSymbolToCall(bestMatch.get());

        // bestMatch might be String._eq(String) even though valueSymbol is Character
        // SymbolMatcher detected promotion needed and selected String._eq(String) with cost 0.5
        // Phase 7 will use ParameterPromotionProcessor to generate the _promote() call

      } else {
        // No matching method found (even with fuzzy matching)
        emitMethodNotResolvedError(ctx, controlSymbol, search);
      }
    }
  });
}

// Helper method to get value symbol from child context
private ISymbol getValueSymbolFromChildContext(final EK9Parser.CaseExpressionContext ctx) {
  if (ctx.call() != null) {
    return getRecordedAndTypedSymbol(ctx.call());
  } else if (ctx.objectAccessExpression() != null) {
    return getRecordedAndTypedSymbol(ctx.objectAccessExpression());
  } else if (ctx.expression() != null) {
    return getRecordedAndTypedSymbol(ctx.expression());
  } else if (ctx.primary() != null) {
    return getRecordedAndTypedSymbol(ctx.primary());
  }
  return null;
}
```

**Key Points**:
1. Get CallSymbol from caseExpression context (created in Phase 1)
2. Get value type from **child context**, not parent
3. Use `resolveMatchingMethods()` with SymbolMatcher (fuzzy matching with promotion)
4. **Set `callSymbol.setResolvedSymbolToCall(bestMatch)`** ← THE CRITICAL STEP
5. Do NOT record value symbol on caseExpression context (keep CallSymbol there)

### Phase 7: IR_GENERATION - Mechanical Translation

**File**: `compiler-main/src/main/java/org/ek9lang/compiler/phase7/generator/SwitchStatementGenerator.java`

**Method**: `generateCaseCondition(caseExpr, evalVariable, conditionScopeId)`

**Current Implementation (WRONG - Doing Phase 3 work)**:
```java
// WRONG: Creating symbols and resolving during IR generation
final var evalVarSymbol = evalVariable.typeSymbol();
final var caseValueSymbol = getCaseExpressionSymbol(caseExpr);

final var callContext = CallContext.forBinaryOperation(
    evalVarSymbol,      // Creating context during IR - WRONG!
    caseValueSymbol,
    returnType,
    methodName,
    ...
);

// WRONG: Method resolution during IR generation
final var callDetailsResult = generators.callDetailsBuilder.apply(callContext);
```

**Correct Implementation (MECHANICAL - Using Phase 3 resolution)**:
```java
private List<IRInstr> generateCaseCondition(
    final EK9Parser.CaseExpressionContext caseExpr,
    final Variable evalVariable,
    final String conditionScopeId) {

  final var conditionEvaluation = new ArrayList<IRInstr>();
  final var debugInfo = generateDebugInfo(caseExpr);

  // STEP 1: Get the RESOLVED CallSymbol from Phase 3
  final var symbol = stackContext.getParsedModule().getRecordedSymbol(caseExpr);

  if (!(symbol instanceof CallSymbol callSymbol)) {
    throw new CompilerException("Expected resolved CallSymbol for case expression, but got: "
        + (symbol != null ? symbol.getClass().getSimpleName() : "null"));
  }

  // Verify CallSymbol was resolved in Phase 3
  if (callSymbol.getResolvedSymbolToCall() == null) {
    throw new CompilerException("CallSymbol not resolved in Phase 3: " + callSymbol.getName());
  }

  // STEP 2: Get case value symbol from child context
  final var caseValueSymbol = getValueSymbolFromChildContext(caseExpr);

  // STEP 3: Load switch variable (already done by caller)
  final var evalVarLoaded = evalVariable.variableName();

  // STEP 4: Load/create case value
  final var caseValueDetails = generators.exprGenerator.apply(
      new ExprProcessingDetails(getCaseValueContext(caseExpr), new VariableDetails(debugInfo))
  );
  conditionEvaluation.addAll(caseValueDetails.instructions());
  final var caseValue = caseValueDetails.resultVariable();

  // STEP 5: Prepare CALL result variable
  final var comparisonResult = stackContext.generateTempName();

  // STEP 6: Get operator method name (for IR comment/debugging)
  final var methodName = operatorMap.getForward(callSymbol.getName());

  // STEP 7: Extract return type from resolved method
  final var resolvedMethod = callSymbol.getResolvedSymbolToCall();
  final ISymbol returnType;
  switch (resolvedMethod) {
    case MethodSymbol ms -> returnType = ms.getReturningSymbol().getType()
        .orElse(stackContext.getParsedModule().getEk9Types().ek9Boolean());
    case FunctionSymbol fs -> returnType = fs.getReturningSymbol().getType()
        .orElse(stackContext.getParsedModule().getEk9Types().ek9Boolean());
    default -> returnType = stackContext.getParsedModule().getEk9Types().ek9Boolean();
  }

  // STEP 8: Get symbols for CallContext (just plumbing)
  final var evalVarSymbol = evalVariable.typeSymbol();

  // STEP 9: Create CallContext WITH parseContext
  // This tells CallDetailsBuilder to use the resolved method from Phase 3
  final var callContext = CallContext.forBinaryOperationWithContext(
      evalVarSymbol,              // Switch variable symbol
      caseValueSymbol,            // Case value symbol
      returnType,                 // Return type from resolved method
      methodName,                 // Method name for IR
      evalVarLoaded,              // Switch variable IR name
      caseValue,                  // Case value IR name
      conditionScopeId,           // Current scope
      caseExpr                    // Parse context with resolved CallSymbol
  );

  // STEP 10: CallDetailsBuilder generates IR MECHANICALLY
  // It extracts the resolved method from CallSymbol
  // It detects promotion needs via ParameterPromotionProcessor
  // It generates promotion IR if needed
  final var callDetailsResult = generators.callDetailsBuilder.apply(callContext);

  // STEP 11: Add any promotion instructions (e.g., Character._promote())
  conditionEvaluation.addAll(callDetailsResult.allInstructions());

  // STEP 12: Generate the CALL instruction
  conditionEvaluation.add(CallInstr.operator(
      new VariableDetails(comparisonResult, debugInfo),
      callDetailsResult.callDetails()
  ));

  // STEP 13: Memory management for CALL result
  conditionEvaluation.add(MemoryInstr.retain(comparisonResult, debugInfo));
  conditionEvaluation.add(ScopeInstr.register(comparisonResult, conditionScopeId, debugInfo));

  // STEP 14: Extract primitive boolean for condition
  final var primitiveBoolean = stackContext.generateTempName();
  conditionEvaluation.add(LoadInstr.primitiveValue(
      new VariableDetails(primitiveBoolean, debugInfo),
      new VariableDetails(comparisonResult, debugInfo)
  ));

  return conditionEvaluation;
}

// Helper methods
private ISymbol getValueSymbolFromChildContext(EK9Parser.CaseExpressionContext ctx) {
  if (ctx.call() != null) return getRecordedSymbolOrException(ctx.call());
  if (ctx.objectAccessExpression() != null) return getRecordedSymbolOrException(ctx.objectAccessExpression());
  if (ctx.expression() != null) return getRecordedSymbolOrException(ctx.expression());
  return getRecordedSymbolOrException(ctx.primary());
}

private ParserRuleContext getCaseValueContext(EK9Parser.CaseExpressionContext ctx) {
  if (ctx.call() != null) return ctx.call();
  if (ctx.objectAccessExpression() != null) return ctx.objectAccessExpression();
  if (ctx.expression() != null) return ctx.expression();
  return ctx.primary();
}
```

**Key Differences from Current Implementation**:
1. Gets **already-resolved** CallSymbol from Phase 3
2. Verifies `resolvedSymbolToCall` is set
3. Extracts return type from resolved method
4. Creates CallContext with parseContext (tells CallDetailsBuilder to use resolved method)
5. **No method resolution logic** - that's done in Phase 3!
6. Mechanical IR generation only

## Type Promotion Flow (Character → String)

### Example

**Source**:
```ek9
switch text as String
  case 'D'  // Character literal
    result := "Found D"
```

### Phase-by-Phase Flow

**Phase 1 - SYMBOL_DEFINITION**:
```
Create CallSymbol("==", scope)
  - name: "=="
  - operator: true
  - resolvedSymbolToCall: NULL
Record on caseExpression context
```

**Phase 3 - FULL_RESOLUTION**:
```
1. Get CallSymbol from caseExpression context
2. Get controlSymbol type = String
3. Get valueSymbol from ctx.primary() = ConstantSymbol('D') with type Character
4. Create MethodSymbolSearch("==").addTypeParameter(Character)
5. Call String.resolveMatchingMethods(search, result)
6. SymbolMatcher.getCostOfParameterMatch():
   - fromType: Character
   - toType: String (from String._eq(String) parameter)
   - Call: Character.getAssignableCostTo(String)
   - Character has _promote() method → return COERCION_COST (0.5)
7. bestMatch = String._eq(String) with total cost 0.5
8. callSymbol.setResolvedSymbolToCall(String._eq(String))  ← RESOLVED!
```

**Phase 7 - IR_GENERATION**:
```
1. Get CallSymbol from caseExpression context
2. Verify callSymbol.getResolvedSymbolToCall() = String._eq(String)
3. Create CallContext with parseContext = caseExpr
4. CallDetailsBuilder.resolveFromParsedSymbol(context):
   a. Gets CallSymbol from parseContext (caseExpr)
   b. Gets resolvedSymbolToCall = String._eq(String)
   c. Calls ParameterPromotionProcessor.apply():
      - Compares argumentTypes [Character] with method params [String]
      - Detects mismatch
      - Checks Character has _promote() → YES
      - Generates: _temp_promoted = CALL Character._promote() on _temp_character
   d. Returns promoted arguments and instructions
5. Add promotion instruction to IR
6. Generate CALL String._eq(_temp_promoted) on text
```

**Generated IR**:
```
_temp_character = LOAD_LITERAL 'D'
_temp_promoted = CALL (org.ek9.lang::String)Character._promote() on _temp_character
_temp_result = CALL (org.ek9.lang::Boolean)String._eq(_temp_promoted) on text
```

## Key Components

### SymbolMatcher (Cost-Based Fuzzy Matching)

**File**: `compiler-main/src/main/java/org/ek9lang/compiler/support/SymbolMatcher.java`

**Used in Phase 3** to find best method match with promotion

**Method**: `getCostOfParameterMatch(fromSymbols, toSymbols)`
- Compares parameter types
- Calls `fromSymbol.getAssignableCostTo(toSymbol)`
- Detects promotion via `_promote()` method
- Returns cost (0.0 = exact, 0.5 = promotion, -1.0 = incompatible)

### ParameterPromotionProcessor (IR Promotion Generation)

**File**: `compiler-main/src/main/java/org/ek9lang/compiler/phase7/support/ParameterPromotionProcessor.java`

**Used in Phase 7** to generate promotion IR

**Method**: `apply(CallContext, MethodResolutionResult)`
- Compares actual argument types with resolved method parameters
- For mismatches, generates `CALL _promote()` instructions
- Returns promoted argument variables

### CallDetailsBuilder (Resolution Coordinator)

**File**: `compiler-main/src/main/java/org/ek9lang/compiler/phase7/calls/CallDetailsBuilder.java`

**Used in Phase 7** to orchestrate IR generation

**Method**: `resolveFromParsedSymbol(CallContext)`
- Gets CallSymbol from parseContext
- Extracts `resolvedSymbolToCall` (set in Phase 3)
- Calls ParameterPromotionProcessor
- Returns CallDetails + promotion instructions

## Files to Modify

### 1. Phase 3: Prevent CallSymbol Overwrite
**File**: `compiler-main/src/main/java/org/ek9lang/compiler/phase3/ExpressionsListener.java`
**Line**: ~309-312
**Change**: Remove `caseExpressionOrError.accept(ctx);` call
**Reason**: Value symbols are on child contexts - don't overwrite CallSymbol

### 2. Phase 3: Complete CallSymbol Resolution
**File**: `compiler-main/src/main/java/org/ek9lang/compiler/phase3/SwitchStatementExpressionOrError.java`
**Method**: `operatorExistsOrError()`
**Changes**:
- Get CallSymbol from caseExpression context
- Get value type from child context (ctx.primary(), etc.)
- Resolve method using MethodSymbolSearch with fuzzy matching
- **Set `callSymbol.setResolvedSymbolToCall(bestMatch)`** ← CRITICAL
**Reason**: Follow established pattern - full resolution in Phase 3

### 3. Phase 7: Mechanical IR Generation
**File**: `compiler-main/src/main/java/org/ek9lang/compiler/phase7/generator/SwitchStatementGenerator.java`
**Method**: `generateCaseCondition()`
**Changes**:
- Get resolved CallSymbol from caseExpression context
- Verify `resolvedSymbolToCall` is set
- Create CallContext with parseContext
- Let CallDetailsBuilder handle everything mechanically
**Reason**: Phase 7 should be mechanical - no resolution logic

## Testing Strategy

### Phase 3 Validation
After Phase 3 completion:
- [ ] All caseExpression contexts have CallSymbols (not ConstantSymbols)
- [ ] All CallSymbols have `resolvedSymbolToCall` set (not NULL)
- [ ] Character → String cases have `resolvedSymbolToCall = String._eq(String)`
- [ ] Exact match cases have correct resolved methods

### Phase 7 Validation
After IR generation:
- [ ] Character promotion generates `_promote()` CALL before comparison
- [ ] Explicit `==` generates identical IR to implicit equality
- [ ] Multiple cases generate correct OR logic
- [ ] Different operators generate correct method calls

### Test Cases

1. **Character Promotion**:
   ```ek9
   switch text as String
     case 'D'
   ```
   **Expected Phase 3**: CallSymbol.resolvedSymbolToCall = String._eq(String)
   **Expected IR**: Character._promote() call before String._eq()

2. **Explicit Equality**:
   ```ek9
   switch value as Integer
     case == 10
     case 20
   ```
   **Expected Phase 3**: Both have resolvedSymbolToCall = Integer._eq(Integer)
   **Expected IR**: Identical CALL instructions

3. **Multiple Cases**:
   ```ek9
   case 'D', 'A', 'Z'
   ```
   **Expected Phase 3**: Three CallSymbols, all resolved
   **Expected IR**: Three comparisons with OR logic

4. **Different Operators**:
   ```ek9
   case < -10.0
   case <= 5.0
   ```
   **Expected Phase 3**: Resolved to Float._lt() and Float._lteq()
   **Expected IR**: Correct operator calls

## Implementation Checklist

- [ ] **Phase 3 - Prevent Overwrite**: Modify ExpressionsListener.exitCaseExpression()
- [ ] **Phase 3 - Resolution**: Update SwitchStatementExpressionOrError.operatorExistsOrError()
  - [ ] Get CallSymbol from caseExpression context
  - [ ] Get value from child context
  - [ ] Resolve method with fuzzy matching
  - [ ] Set resolvedSymbolToCall
- [ ] **Phase 7 - Mechanical**: Update SwitchStatementGenerator.generateCaseCondition()
  - [ ] Get resolved CallSymbol
  - [ ] Verify resolvedSymbolToCall is set
  - [ ] Remove resolution logic
  - [ ] Use CallDetailsBuilder with parseContext
- [ ] **Testing**: Run SwitchIRTest
- [ ] **Validation**: Verify Character._promote() in IR
- [ ] **Validation**: Verify no resolution in Phase 7
- [ ] **Regression**: Run all IR tests

## Summary

**The Architectural Principle**:
- Phases 1-2: Create structures
- Phases 3-6: **Fully resolve** symbols
- Phase 7: **Mechanical** IR generation

**The Problem**: Switch processing violated this principle by leaving resolution work in Phase 7.

**The Solution**: Follow the established pattern used for all other operators:
1. Phase 1: Create CallSymbol (✓ already done)
2. Phase 3: **Fully resolve** CallSymbol with `setResolvedSymbolToCall()`
3. Phase 7: **Mechanical** IR generation from resolved CallSymbol

**The Benefit**: Correct separation of concerns, maintainability, testability, and architectural consistency.
