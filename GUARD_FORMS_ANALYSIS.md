# EK9 Guard Forms - Complete Analysis

## Guard Operator Forms (from EK9LexerRules.g4)

### 1. Declaration Guard: `<-` (LEFT_ARROW)
**Token**: `LEFT_ARROW : '<-';` (line 545)
**Usage**: `identifier <- expression`
**Semantics**:
- Declare new variable
- Assign expression result
- Acts as guard in control flow

**Example**:
```ek9
if value <- getOptional()
  stdout.println(value)
```

### 2. Assignment Guard: `:=` (ASSIGN)
**Token**: `ASSIGN : ':=';` (line 550)
**Comment**: "ek9 assign - this copies the variable pointer over"
**Usage**: `identifier := expression`
**Semantics**:
- Assign to existing variable
- Pointer copy (not content copy)
- Acts as guard in control flow

**Example**:
```ek9
value <- String()  // Declare
if value := getOptional()  // Assign guard
  stdout.println(value)
```

### 3. Guarded Assignment (Check Null): `?=` (GUARD)
**Token**: `GUARD : '?=';` (line 508)
**Comment**: "a special type of assign that also checks if null on assignment used in controls"
**Usage**: `identifier ?= expression`
**Semantics**:
- Check if expression result is null before assignment
- Returns Boolean (success/failure)
- Used in control flow

**Example**:
```ek9
if value ?= getOptional()  // Only assigns if result not null
  stdout.println(value)
```

### 4. Assignment If Unset: `:=?` (ASSIGN_UNSET)
**Token**: `ASSIGN_UNSET : ':=?';` (line 513)
**Comment**: "Assign to the left hand side if left handside is null or unset"
**Usage**: `identifier :=? expression`
**Semantics**:
- Only assign if left-hand side is null or unset
- No-op if left-hand side already has value
- Conditional initialization pattern

**Example**:
```ek9
value :=? getDefault()  // Only assigns if value is unset
```

## Grammar Rules

### preFlowStatement (line 485)
```antlr
preFlowStatement
    : (variableDeclaration | assignmentStatement | guardExpression)
    ;
```

### variableDeclaration (lines 333-334)
```antlr
variableDeclaration
    : identifier AS? typeDef QUESTION? op=(ASSIGN | ASSIGN2 | COLON | MERGE) assignmentExpression
    | identifier op=LEFT_ARROW assignmentExpression
    ;
```

### assignmentStatement (line 379)
```antlr
assignmentStatement
    : (primaryReference | identifier | objectAccessExpression)
      op=(ASSIGN | ASSIGN2 | COLON | ASSIGN_UNSET | ADD_ASSIGN | SUB_ASSIGN | DIV_ASSIGN | MUL_ASSIGN | MERGE | REPLACE | COPY)
      assignmentExpression
    ;
```

### guardExpression (lines 488-490)
```antlr
guardExpression
    : identifier op=GUARD expression
    ;
```

## Control Flow Usage

All four forms can appear in `preFlowStatement`, which is used in control flow:

```antlr
preFlowAndControl
    : preFlowStatement (WITH|THEN) control=expression
    | control=expression
    ;
```

This allows patterns like:
```ek9
if value <- getOptional() with value.length() > 5
if value := getOptional() with value.isValid()
if value ?= getOptional() with value.ready()
```

## Critical IR Generation Requirements

For each guard form, the IR MUST include:

### 1. NULL Check (QUESTION_OPERATOR Pattern)
```
CONTROL_FLOW_CHAIN
[
  chain_type: "QUESTION_OPERATOR"
  condition_chain: [[
    case_type: "NULL_CHECK"
    condition_evaluation: [IS_NULL check]
    body_evaluation: [return Boolean(false) if null]
  ]]
  default_body_evaluation: [call ._isSet() if not null]
]
```

### 2. Short-Circuit AND (if guard + condition)
```
LOGICAL_AND_BLOCK
[
  left_evaluation: [guard check with NULL check]
  left_condition: primitive boolean
  right_evaluation: [explicit condition] // Only if left is true!
  result_computation: [combine results]
]
```

## Test Coverage Needed

### Declaration Guard `<-`
- [ ] **IR Test**: Declaration guard only
- [ ] **IR Test**: Declaration guard with condition
- [x] **Bytecode Test**: ifWithGuard (guard only) ✅
- [x] **Bytecode Test**: ifWithGuardAndCondition (guard + condition) ✅
- [x] **Bytecode Test**: ifElseIfWithGuards (multiple guards) ✅

### Assignment Guard `:=`
- [ ] **IR Test**: Assignment guard only
- [ ] **IR Test**: Assignment guard with condition
- [ ] **Bytecode Test**: Assignment guard only
- [ ] **Bytecode Test**: Assignment guard with condition

### Guarded Assignment `?=`
- [ ] **IR Test**: Guarded assignment in if
- [ ] **IR Test**: Guarded assignment with condition
- [ ] **Bytecode Test**: Guarded assignment in if
- [ ] **Bytecode Test**: Guarded assignment with condition

### Assignment If Unset `:=?`
- [ ] **IR Test**: Assignment if unset in if
- [ ] **IR Test**: Assignment if unset with condition
- [ ] **Bytecode Test**: Assignment if unset in if
- [ ] **Bytecode Test**: Assignment if unset with condition

## Search for Existing Examples

Need to find existing test files using these patterns:
- `?=` guard pattern
- `:=` assignment guard pattern
- `:=?` assignment if unset pattern

## IR Generation Fix Priority

1. **Fix Declaration Guard `<-` IR** (highest priority - we have bytecode tests)
   - Add IS_NULL check to IR
   - Use QUESTION_OPERATOR pattern
   - Use LOGICAL_AND_BLOCK for guard+condition

2. **Verify Assignment Guard `:=` semantics**
   - Should it also check null?
   - Should it use same QUESTION_OPERATOR pattern?

3. **Verify Guarded Assignment `?=` semantics**
   - "checks if null on assignment"
   - Should return Boolean
   - May need different IR pattern

4. **Verify Assignment If Unset `:=?` semantics**
   - Checks left-hand side, not right-hand side
   - Different pattern from other guards

## Questions for Clarification

1. **Assignment Guard `:=`**: Should this also do NULL check like `<-`?
   - Currently: Used in control flow like declaration guard
   - IR should be: Same as declaration guard?

2. **Guarded Assignment `?=`**: What's the difference from `<-`?
   - Comment says "checks if null on assignment"
   - Is this redundant with `<-` guard behavior?
   - Or is this checking the RESULT is not null before assigning?

3. **Assignment If Unset `:=?`**: When used in control flow?
   - Typically used as statement: `value :=? default`
   - Can it be used in if statement? `if value :=? getDefault()`
   - What should the guard check be?

## Next Steps

1. Search for examples of `:=`, `?=`, `:=?` in existing test files
2. Understand semantics of each guard form
3. Create IR test examples for each form
4. Fix IR generation to include IS_NULL checks
5. Create bytecode tests for missing guard forms
6. Verify LLVM generation will work correctly from fixed IR
