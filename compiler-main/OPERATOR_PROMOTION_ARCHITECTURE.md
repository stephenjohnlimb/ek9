# EK9 Operator Promotion Architecture - Implementation Plan

## Problem Statement

**Current Issue**: Character-to-String promotion works for switch case operators but NOT for general binary/unary operators in expressions.

**Example**:
```ek9
// Switch case - promotion WORKS ✅
switch text
  case == 'A'  // Character 'A' promoted to String

// Binary expression - promotion BROKEN ❌
result := text == 'A'  // Character 'A' NOT promoted to String
```

**Root Cause**: Architectural inconsistency in how operators are handled across compilation phases.

## Current Architecture Analysis

### What WORKS (CallSymbol Pattern):

| Construct | Phase 1 | Phase 3 | Phase 7 | Promotion |
|---|---|---|---|---|
| **Function calls** | `enterCall` → CallSymbol | `CallOrError` resolves → `setResolvedSymbolToCall` | `FunctionCallProcessor` uses `CallDetailsBuilder` | ✅ Works |
| **Method calls** | `enterOperationCall` → CallSymbol | `ObjectAccessExpressionOrError` resolves → `setResolvedSymbolToCall` | `CallInstrGenerator` uses `CallDetailsBuilder` | ✅ Works |
| **Switch operators** | `enterCaseExpression` → CallSymbol | `SwitchStatementExpressionOrError` resolves → `setResolvedSymbolToCall` | `SwitchStatementGenerator` uses `CallDetailsBuilder` | ✅ Works |

### What DOESN'T WORK (ExpressionSymbol Pattern):

| Construct | Phase 1 | Phase 3 | Phase 7 | Promotion |
|---|---|---|---|---|
| **Binary operators** | ❌ No symbol | `ExpressionOrError` resolves → creates ExpressionSymbol (discards method) | `BinaryOperationGenerator` re-resolves (unreliable) | ❌ Broken |
| **Unary operators** | ❌ No symbol | `ExpressionOrError` resolves → creates ExpressionSymbol (discards method) | `UnaryOperationGenerator` re-resolves (unreliable) | ❌ Broken |

## Why Promotion Fails for Binary Operators

### Current Flow:

**Phase 1**: No symbol created for `text == 'A'`

**Phase 3** (`ExpressionOrError.processOperationOrError`):
1. Search for `String._eq` with parameter type `Character`
2. Find `String._eq(String)` with cost calculation including promotion
3. Get return type: `Boolean`
4. Create `ExpressionSymbol` with return type
5. **DISCARD the resolved method symbol** ❌

**Phase 7** (`BinaryOperationGenerator`):
1. Get `ExpressionSymbol` (no method information)
2. Try to re-resolve method to check promotion
3. Different resolution path → promotion check fails

## The Solution: Unified CallSymbol Architecture

### Core Principle

**Operators ARE method calls** → They should be CallSymbols from Phase 1, just like function/method calls.

### Grammar Analysis (EK9.g4 lines 399-434)

**Operators that create method calls** (`ctx.op`, `ctx.coalescing`, `ctx.coalescing_equality`):
- **Unary postfix**: `expr++`, `expr--`, `expr?`, `expr!`
- **Unary prefix**: `$expr`, `#^expr`, `-expr`, `!expr`, `~expr`, `#?expr`, `||expr||`, etc.
- **Binary arithmetic**: `+`, `-`, `*`, `/`, `%`, `rem`, `^`
- **Binary comparison**: `<`, `<=`, `>`, `>=`, `<=>`, `~`
- **Binary equality**: `==`, `<>`, `!=`
- **Binary logical**: `and`, `or`, `xor`
- **Binary bitwise**: `<<`, `>>`
- **Special binary**: `matches`, `contains`, `in`
- **Coalescing**: `??`, `?:`
- **Coalescing equality**: `?<`, `?<=`, `?>`, `?>=`

**Non-operators** (NOT method calls, keep as ExpressionSymbols):
- **Primary**: `ctx.primary()` - literals, identifiers
- **Call**: `ctx.call()` - already creates CallSymbol
- **Object access**: `ctx.objectAccessExpression()` - field access
- **Collections**: `ctx.list()`, `ctx.dict()` - literals
- **Control**: `ctx.control` - ternary operator
- **Range**: `ctx.IN()` with `ctx.range()` - range checks

### Recursive Expression Handling

**Example**: `result := (a + b) * c - d`

**ANTLR Parse Tree**:
```
expression (SUB) → enterExpression → CREATE CallSymbol("-")
├─ left: expression (MUL) → enterExpression → CREATE CallSymbol("*")
│  ├─ left: expression (ADD) → enterExpression → CREATE CallSymbol("+")
│  │  ├─ left: expression (primary: a) → enterExpression → NO symbol (ctx.op is null)
│  │  └─ right: expression (primary: b) → enterExpression → NO symbol
│  └─ right: expression (primary: c) → enterExpression → NO symbol
└─ right: expression (primary: d) → enterExpression → NO symbol
```

**Result**: 7 `enterExpression` calls, 3 CallSymbols created (for operators only)

## Implementation Plan

### Phase 1: Create CallSymbols for Operators

**File**: `compiler-main/src/main/java/org/ek9lang/compiler/phase1/DefinitionListener.java`

**Add new method**:
```java
@Override
public void enterExpression(final EK9Parser.ExpressionContext ctx) {
  String operator = null;

  // Check all operator patterns in grammar
  if (ctx.op != null) {
    operator = ctx.op.getText();  // Most operators
  } else if (ctx.coalescing != null) {
    operator = ctx.coalescing.getText();  // ??, ?:
  } else if (ctx.coalescing_equality != null) {
    operator = ctx.coalescing_equality.getText();  // ?<, ?<=, ?>, ?>=
  }

  if (operator != null) {
    // Create CallSymbol for operator (same pattern as case expressions)
    symbolsAndScopes.recordSymbol(
      symbolFactory.newOperatorCall(operator, ctx, symbolsAndScopes.getTopScope()), ctx);
  }
  // If no operator: primary, call, objectAccess, etc. - no symbol created here

  super.enterExpression(ctx);
}
```

**Rationale**: Only create CallSymbols when `ctx.op != null` OR `ctx.coalescing != null` OR `ctx.coalescing_equality != null`. This correctly handles recursive expressions - primaries don't get CallSymbols, only operators do.

### Phase 1: Add Factory Method

**Files**:
- `compiler-main/src/main/java/org/ek9lang/compiler/support/SymbolFactory.java`
- `compiler-main/src/main/java/org/ek9lang/compiler/support/OperationFactory.java`

**SymbolFactory.java**:
```java
public CallSymbol newOperatorCall(final String operator,
                                  final EK9Parser.ExpressionContext ctx,
                                  final IScope scope) {
  return operationFactory.newOperatorCall(operator, ctx, scope);
}
```

**OperationFactory.java** (pattern from `newCaseExpressionCall`):
```java
public CallSymbol newOperatorCall(final String operator,
                                  final EK9Parser.ExpressionContext ctx,
                                  final IScope scope) {
  final var callSymbol = new CallSymbol(operator, scope);
  final var startToken = new Ek9Token(ctx.start);

  configureSymbol(callSymbol, startToken);
  callSymbol.setOperator(true);  // Mark as operator
  callSymbol.setInitialisedBy(startToken);

  return callSymbol;
}
```

### Phase 3: Resolve Operators and Set resolvedSymbolToCall

**File**: `compiler-main/src/main/java/org/ek9lang/compiler/phase3/ExpressionOrError.java`

**Modify `accept` method** (lines 64-82) to avoid re-recording CallSymbols:
```java
@Override
public void accept(final EK9Parser.ExpressionContext ctx) {
  final var symbol = processExpressionOrError(ctx);
  if (symbol != null) {
    // Only record if NOT a CallSymbol (CallSymbols already recorded in Phase 1)
    if (!(symbol instanceof CallSymbol)) {
      symbolsAndScopes.recordSymbol(symbol, ctx);
    }

    final var errorLocation = new Ek9Token(ctx.start);
    if (symbol.getType().isEmpty()) {
      emitTypeNotResolvedError(errorLocation, symbol);
    } else {
      // String interpolation check
      if (ctx.getParent() instanceof EK9Parser.StringPartContext) {
        isConvertableToStringOrError.test(errorLocation, symbol.getType().get());
      }
    }
  }
}
```

**Modify `processExpressionOrError` method** (lines 90-113):
```java
private ISymbol processExpressionOrError(final EK9Parser.ExpressionContext ctx) {
  // Check if this is an operator expression
  if (ctx.op != null || ctx.coalescing != null || ctx.coalescing_equality != null) {
    // Get CallSymbol created in Phase 1
    final var callSymbol = symbolsAndScopes.getRecordedSymbol(ctx);
    if (!(callSymbol instanceof CallSymbol)) {
      throw new CompilerException("Expected CallSymbol for operator expression");
    }

    // Resolve operator method and set resolvedSymbolToCall
    return resolveOperatorCallSymbol((CallSymbol) callSymbol, ctx);
  } else if (ctx.primary() != null) {
    return processPrimary(ctx);
  } else if (ctx.call() != null) {
    return symbolFromContextOrError.apply(ctx.call());
  } else if (ctx.objectAccessExpression() != null) {
    return processObjectAccessExpression(ctx);
  }

  return processControlsOrStructures(ctx);
}
```

**Add new method `resolveOperatorCallSymbol`**:
```java
/**
 * Resolve operator method and set resolvedSymbolToCall on CallSymbol.
 * This follows the same pattern as switch case operator resolution.
 */
private ISymbol resolveOperatorCallSymbol(final CallSymbol callSymbol,
                                          final EK9Parser.ExpressionContext ctx) {
  // Determine which type of operator
  if (ctx.op != null && ctx.op.getText().equals("?")) {
    // Special case for isSet operator
    return isSetOrError(ctx);
  }

  // Get left operand for method resolution
  final var leftExpr = ctx.expression(0);  // Unary or binary, always has at least one
  final var leftSymbol = symbolFromContextOrError.apply(leftExpr);

  if (leftSymbol != null && leftSymbol.getType().isPresent()) {
    // Build method search
    final var opToken = ctx.op != null ? new Ek9Token(ctx.op)
                     : ctx.coalescing != null ? new Ek9Token(ctx.coalescing)
                     : new Ek9Token(ctx.coalescing_equality);

    final var search = methodSymbolSearchForExpression.apply(ctx);

    // Resolve operator method
    final var operatorData = new CheckOperatorData(leftSymbol, opToken, search);
    final var returnType = resolveOperatorAndSetMethod(operatorData, callSymbol);

    if (returnType.isPresent()) {
      // Set return type on CallSymbol
      callSymbol.setType(returnType);
      return negationIfRequiredOrError(ctx, callSymbol);
    }
  }

  return null;
}
```

### Phase 3: Modify RequiredOperatorPresentOrError

**File**: `compiler-main/src/main/java/org/ek9lang/compiler/phase3/RequiredOperatorPresentOrError.java`

**Change signature** to accept CallSymbol:
```java
Optional<ISymbol> apply(CheckOperatorData checkOperatorData, CallSymbol callSymbol)
```

**Modify resolution** (lines 64-75) to set `resolvedSymbolToCall`:
```java
final var search = checkOperatorData.search();
final var results = aggregate.resolveMatchingMethods(search, new MethodSymbolSearchResult());
final var bestMatch = results.getSingleBestMatchSymbol();

if (bestMatch.isPresent()) {
  final var operator = bestMatch.get();

  // Set resolved method on CallSymbol (KEY CHANGE!)
  callSymbol.setResolvedSymbolToCall(operator);

  noteOperatorAccessedIfConceptualType(aggregate, operator);
  checkPureAccess(checkOperatorData.operatorUseToken(), operator);
  return operator.getReturningSymbol().getType();
}
```

### Phase 7: Use CallSymbol in BinaryOperationGenerator

**File**: `compiler-main/src/main/java/org/ek9lang/compiler/phase7/generator/BinaryOperationGenerator.java`

**Revert to use CallSymbol pattern** (like SwitchStatementGenerator):

```java
// Get the resolved CallSymbol from the parse context (resolved in Phase 3)
final var exprSymbol = getRecordedSymbolOrException(ctx);

if (!(exprSymbol instanceof CallSymbol callSymbol)) {
  throw new CompilerException("Expected CallSymbol for binary operation, but got: "
      + (exprSymbol != null ? exprSymbol.getClass().getSimpleName() : "null"));
}

// Get the actual resolved method symbol (set by Phase 3)
final var resolvedMethod = callSymbol.getResolvedSymbolToCall();
if (resolvedMethod == null) {
  throw new CompilerException("CallSymbol has no resolved method: " + methodName);
}

// Get return type from resolved method
final ISymbol returnType;
switch (resolvedMethod) {
  case MethodSymbol ms -> returnType = ms.getReturningSymbol().getType().orElse(leftType);
  case FunctionSymbol fs -> returnType = fs.getReturningSymbol().getType().orElse(leftType);
  default -> returnType = leftType;
}

// Create call context WITH parseContext (enables promotion checking)
final var callContext = CallContext.forBinaryOperationWithContext(
    leftType,
    rightType,
    returnType,
    methodName,
    leftDetails.resultVariable(),
    rightDetails.resultVariable(),
    stackContext.currentScopeId(),
    ctx  // Parse context with resolved CallSymbol
);

// Use CallDetailsBuilder - promotion checking now works!
final var callDetailsResult = generators.callDetailsBuilder.apply(callContext);
```

### Phase 7: Remove Temporary Hack

**File**: `compiler-main/src/main/java/org/ek9lang/compiler/phase7/calls/CallDetailsBuilder.java`

**Remove**:
- `resolveOperatorMethodAndCheckPromotion` method (lines 198-238)
- Debug output in that method
- Call to that method from `resolveAsOperator` (line 179)

**Restore** original `resolveAsOperator` to return empty promotion result (line 176-177):
```java
// For operators without Phase 3 resolution, no promotion checking is possible
final PromotionResult promotionResult = new PromotionResult(context.argumentVariables(), List.of());
```

## Benefits of This Architecture

1. ✅ **Uniform call handling**: Functions, methods, and operators all use CallSymbols
2. ✅ **Single resolution point**: Phase 3 resolves once, Phase 7 uses result
3. ✅ **No duplicate logic**: Phase 7 doesn't re-resolve methods
4. ✅ **Promotion works everywhere**: All operators get automatic promotion support
5. ✅ **Scalable**: New operators automatically inherit promotion capability
6. ✅ **Consistent with existing patterns**: Uses proven switch case approach

## Testing Strategy

### Initial Test (Already in place):

**File**: `compiler-main/src/test/resources/examples/parseButFailCompile/workingarea/workarea.ek9`
```ek9
defines module promotion

  defines function

    checkPromotion()
      -> text as String
      <- result <- Boolean()

      result: text == 'A'  // Should generate Character._promote() call
```

**Expected IR Output** (after fix):
```
_temp4 = LOAD_LITERAL 'A', org.ek9.lang::Character
_temp5 = CALL (org.ek9.lang::String)_temp4._promote() [pure=true, ...]  // ← PROMOTION CALL
_temp2 = CALL (org.ek9.lang::String)_temp3._eq(_temp5) [pure=true, ...]
```

### Comprehensive Tests (to add after fix):

**Location**: `compiler-main/src/test/resources/examples/irGeneration/operatorUse/`

**Test cases**:
1. Binary comparison operators requiring promotion (`==`, `<>`, `<`, `>`, `<=`, `>=`)
2. Binary string operators requiring promotion (`contains`, `matches`)
3. Nested expressions with multiple promotions: `(a + 'X') == (b + 'Y')`
4. Unary operators (if applicable)
5. Mixed types in complex expressions

## Files Modified Summary

1. **Phase 1**:
   - `DefinitionListener.java` - Add `enterExpression`
   - `SymbolFactory.java` - Add `newOperatorCall` delegation
   - `OperationFactory.java` - Add `newOperatorCall` implementation

2. **Phase 3**:
   - `ExpressionOrError.java` - Modify operator handling, skip re-recording CallSymbols
   - `RequiredOperatorPresentOrError.java` - Add `setResolvedSymbolToCall` on CallSymbol

3. **Phase 7**:
   - `BinaryOperationGenerator.java` - Use CallSymbol pattern instead of re-resolving
   - `CallDetailsBuilder.java` - Remove temporary promotion hack

## Risk Assessment

**Scope**: Medium-High (touches fundamental expression handling)
**Complexity**: Moderate (following established pattern from switch cases)
**Testing**: Critical (extensive testing required across all operator types)

## Implementation Sequence

1. Document current state (this file) ✅
2. Unwind temporary changes to clean state
3. Implement Phase 1 changes (create CallSymbols for operators)
4. Implement Phase 3 changes (resolve and set resolvedSymbolToCall)
5. Implement Phase 7 changes (use CallSymbol pattern)
6. Test with workarea.ek9 and verify IR output
7. Add comprehensive operator promotion tests
8. Run full test suite to verify no regressions
9. Commit changes

## Session Recovery

To resume this work in a fresh session:
1. Read this document
2. Check current implementation state
3. Review test results from `WorkingAreaTest`
4. Continue with next step in implementation sequence

## References

- **Grammar**: `compiler-main/src/main/antlr4/org/ek9lang/antlr/EK9.g4` (lines 399-448)
- **Switch case pattern**: `SwitchStatementExpressionOrError.java` (lines 174-218)
- **Existing promotion**: `ParameterPromotionProcessor.java` (lines 48-113)
- **Method resolution**: `RequiredOperatorPresentOrError.java` (lines 64-83)
