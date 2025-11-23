# Guard Forms - Definitive Semantics Summary

## Based on guardedIf.ek9 Analysis

### 1. Assignment `:=` - NO Guard Check (Blind Assignment)

**Lines 43-46** (UnusableAggregate - no `?` operator):
```ek9
toCheck <- UnusableAggregate()  // Type has NO ? operator
if toCheck := provideUnusableReturn() with conditional
  stdout.println(`But this is just a blind assignment ${toCheck.name}`)
```

**Lines 52-56** (UsableAggregate - HAS `?` operator):
```ek9
toCheck <- UsableAggregate()  // Type HAS ? operator
if toCheck := provideUsableReturn() with conditional
  stdout.println(`But this is just a blind assignment ${toCheck.name}`)  // Still blind!
```

**Semantics**:
- `:=` is **pure assignment** - NO guard check
- Does NOT call `._isSet()` even if type has `?` operator
- Comment: "this does not use the 'guard' functionality"
- In control flow: Just assigns, then evaluates condition
- **NO NULL CHECK needed in IR!**

**IR Should Be**:
```
STORE toCheck, provideUnusableReturn()  // Just assignment
_temp = LOAD conditional               // Then condition
ifeq else_block                         // Branch if false
```

### 2. Declaration `<-` - WITH Guard Check

**Lines 63-69** (UsableAggregate - HAS `?` operator):
```ek9
if toCheck <- provideUsableReturn() with conditional
  stdout.println(`Do expect this to compile and maybe see ${toCheck.name}`)
else
  stdout.println("Either conditional is false/unset or toCheck is unset")
```

**Semantics**:
- `<-` is **declaration + guard check**
- DOES call `._isSet()` on declared value
- Comment: "declaration 'guard' functionality is triggers"
- Comment: "check that what was declared does actually report to have a meaningful value"
- **REQUIRES NULL CHECK in IR!**

**IR Should Be**:
```
STORE toCheck, provideUsableReturn()  // Assign
_temp1 = CONTROL_FLOW_CHAIN            // Guard check
[
  chain_type: "QUESTION_OPERATOR"
  NULL_CHECK on toCheck
  default: CALL toCheck._isSet()
]
_temp_result = LOGICAL_AND_BLOCK      // Combine with condition
[
  left: _temp1 (guard check)
  right: conditional (only if left true)
]
```

### 3. Guarded Assignment `?=` - WITH Guard Check (Always)

**Lines 76-81** (UsableAggregate - HAS `?` operator):
```ek9
toCheck <- UsableAggregate()
if toCheck ?= provideUsableReturn() with conditional
  stdout.println(`Do expect this to compile and maybe see ${toCheck.name}`)
else
  stdout.println("Either conditional is false/unset or toCheck is unset")
```

**Lines 92-96** (UnusableAggregate - NO `?` operator, but Any does):
```ek9
toCheck <- UnusableAggregate()  // No ? operator
if toCheck ?= provideUnusableReturn() with conditional
  stdout.println(`This will compile because Any has ? operator ${toCheck.name}`)
```

**Semantics**:
- `?=` is **guarded assignment** - ALWAYS checks
- Checks via type's `?` operator if available
- Falls back to `Any`'s `?` operator if type doesn't have it
- Comment: "This will compile because Any has ? operator"
- **REQUIRES NULL CHECK in IR!**

**IR Should Be**:
```
_temp_value = provideUnusableReturn()  // Evaluate first
_temp1 = CONTROL_FLOW_CHAIN            // Guard check on result
[
  chain_type: "QUESTION_OPERATOR"
  NULL_CHECK on _temp_value
  default: CALL _temp_value._isSet()  // Via Any if type doesn't have it
]
if_true:
  STORE toCheck, _temp_value           // Only assign if set
  _temp_result = LOGICAL_AND_BLOCK     // Combine with condition
  [
    left: _temp1 (guard check)
    right: conditional
  ]
```

### 4. Assignment If Unset `:=?` - WITH Guard Check on Result

**Statement Usage** (excessiveClassComplexity.ek9):
```ek9
welcome :=? `${firstPart} ${secondPart}, volume of earth is ${volume}`
prop1 :=? arg1
```

**Control Flow Usage** (ifAssignmentIfUnset.ek9):
```ek9
existing <- Integer()  // Unset initially

// Check left side, lazy evaluate, assign, THEN check result
if existing :=? supplyInteger() with existing > 10
  result: "High"
else
  result: "Low"
```

**Semantics**:
- `:=?` checks **LEFT-HAND SIDE FIRST** (is target unset?)
- **LAZY EVALUATION**: Only evaluates expression if left is unset
- Assigns to left if left was unset
- **THEN checks if result is SET** (guard check on result)
- Used in BOTH statements AND control flow
- **TWO-PHASE pattern**: Check left, conditional assignment, guard right

**IR Pattern for Control Flow**:
```
// Phase 1: GUARDED_ASSIGNMENT chain - check left + conditional assign
CONTROL_FLOW_CHAIN
[
  chain_type: "GUARDED_ASSIGNMENT"
  condition: IS_NULL existing OR NOT _isSet (then NEGATE)
  body_if_unset: [
    evaluate expression (lazy!)
    STORE existing, expression
  ]
]

// Phase 2: IF_ELSE_WITH_GUARDS - check result + explicit condition
CONTROL_FLOW_CHAIN
[
  chain_type: "IF_ELSE_WITH_GUARDS"
  guard_check: QUESTION_OPERATOR (IS_NULL + _isSet on existing)
  explicit_condition: existing > 10
  LOGICAL_AND_BLOCK combines guard + condition
]
```

**IR Pattern for Statement Usage**:
```
CONTROL_FLOW_CHAIN
[
  chain_type: "GUARDED_ASSIGNMENT"
  NULL_CHECK on LEFT side (welcome)
  body_if_null: [STORE welcome, expression]
  default: [no-op]  // Do nothing if already set
]
```

## Summary Table

| Operator | Syntax | Guard Check? | Null Check? | When? | Use in Control Flow |
|----------|--------|--------------|-------------|-------|---------------------|
| `:=` | `var := expr` | ❌ NO | ❌ NO | Never | Blind assignment + condition |
| `<-` | `var <- expr` | ✅ YES | ✅ YES | If type has `?` | Declare + guard check |
| `?=` | `var ?= expr` | ✅ YES (right) | ✅ YES (right) | Always (via Any) | Assign + guard check |
| `:=?` | `var :=? expr` | ✅ YES (result) | ✅ YES (left + result) | If left unset | Lazy init + guard check (BOTH statements AND control flow) |

## IR Generation Requirements

### For `:=` Assignment (NO Guard)
```
STORE variable, expression  // No QUESTION_OPERATOR!
[condition evaluation if present]
```

### For `<-` Declaration (WITH Guard)
```
STORE variable, expression
CONTROL_FLOW_CHAIN
[
  chain_type: "QUESTION_OPERATOR"
  condition_chain: [[NULL_CHECK, IS_NULL variable, body: Boolean(false)]]
  default: CALL variable._isSet()
]
[LOGICAL_AND_BLOCK if condition present]
```

### For `?=` Guarded Assignment (WITH Guard)
```
_temp = expression  // Evaluate first
CONTROL_FLOW_CHAIN
[
  chain_type: "QUESTION_OPERATOR"
  condition_chain: [[NULL_CHECK, IS_NULL _temp, body: Boolean(false)]]
  default: CALL _temp._isSet()  // Via Any if needed
]
if_true_branch:
  STORE variable, _temp  // Only assign if guard succeeds
[LOGICAL_AND_BLOCK if condition present]
```

### For `:=?` Assignment If Unset (Two-Phase Pattern)

**In Control Flow** (with guard check on result):
```
// Phase 1: Check left side + conditional assignment
CONTROL_FLOW_CHAIN
[
  chain_type: "GUARDED_ASSIGNMENT"
  condition_evaluation: [
    QUESTION_OPERATOR (IS_NULL + _isSet on variable)
    _negate() to check if UNSET
  ]
  body_evaluation: [
    evaluate expression (LAZY - only if left unset!)
    STORE variable, expression
  ]
]

// Phase 2: Guard check on result + explicit condition
CONTROL_FLOW_CHAIN
[
  chain_type: "IF_ELSE_WITH_GUARDS"
  guard_check: QUESTION_OPERATOR (IS_NULL + _isSet on variable - now potentially set)
  explicit_condition: user condition (e.g., variable > 10)
  LOGICAL_AND_BLOCK combines both
]
```

**In Statement** (no guard check needed):
```
CONTROL_FLOW_CHAIN
[
  chain_type: "GUARDED_ASSIGNMENT"
  NULL_CHECK on LEFT side (variable)
  body_if_null: [STORE variable, expression]
  default: [no-op]  // Do nothing if already set
]
```

## Working with Optional and Result Types

### Optional/Result Have the `?` Operator

Both `Optional<T>` and `Result<T,E>` implement the `?` operator (`isSet` check):
- `Optional?` returns true if it contains a value (not empty)
- `Result?` returns true if it is set (has ok or error value)

This means guards work directly with Optional and Result types!

---

### Optional Unwrapping Patterns

#### Pattern 1: Guard in Preflow (RECOMMENDED)

```ek9
// Guard automatically checks null AND isSet
if o <- getExplicitOptional()
  value <- o.get()  // SAFE - o guaranteed SET here
  assert value?
```

**How it works:**
1. `getExplicitOptional()` returns `Optional<String>`
2. Guard `<-` checks if `o` is SET (non-null AND has value)
3. If SET, `.get()` is SAFE inside the if body
4. Compiler enforces safety - cannot call `.get()` without guard

#### Pattern 2: Ternary (IDIOMATIC for pure contexts)

```ek9
o <- Optional("Steve")

// Get value or default
value <- o? <- o.get() else String()
assert value?
```

**How it works:**
1. `o?` checks if Optional has value
2. If true, call `o.get()` (safe)
3. If false, use `String()` (unset default)

#### Pattern 3: Explicit Check After Declaration

```ek9
o1 <- getExplicitOptional()

if o1?
  val <- o1.get()  // SAFE - checked with ?
  assert val?
```

#### Pattern 4: Guard with Additional Condition

```ek9
if o1 <- getExplicitOptional() with o1?
  val <- o1.get()  // SAFE - guard + explicit check
  assert val?
```

#### Pattern 5: Safe Methods (NO GUARD NEEDED)

```ek9
o <- Optional("Steve")

// These methods are ALWAYS safe:
assert o is not empty              // Check if empty
assert o contains "Steve"          // Check specific value
assuredValue <- o.getOrDefault("Stephen")  // Get or default
o.whenPresent(someConsumer)        // Execute only if present
emptyOpt <- o.asEmpty()           // Get empty version
```

---

### Optional in Control Flow Constructs

#### Switch with Optional Guard

```ek9
// Guard makes o.get() safe throughout switch
switch o <- getExplicitOptional() then o.get()
  case "Steve"
    someResult: "Just Steve"
  default
    someResult: o.get()  // Still safe
```

#### While with Optional Guard

```ek9
while o <- getExplicitOptional() then not triggerToEndLoop
  val <- o.get()  // SAFE - o scoped to while block
  assert val?
  triggerToEndLoop: true
```

#### For with Optional Guard

```ek9
for o <- getExplicitOptional() then item in getList()
  val <- o.get()  // SAFE - o checked once, available for whole loop
  assert val?
```

#### Try with Optional Guard

```ek9
try o <- getExplicitOptional()
  value <- o.get()  // SAFE in try block
  assert value?
catch
  -> ex as Exception
  value <- o.get()  // SAFE in catch block
finally
  value <- o.get()  // SAFE in finally block
```

---

### Result Unwrapping Patterns

#### Pattern 1: Guard in Preflow (RECOMMENDED)

```ek9
// Guard checks if Result is SET (has ok or error)
if r <- getExplicitResult()
  okValue <- r.ok()  // SAFE - r guaranteed SET
  assert okValue?
```

#### Pattern 2: Explicit isOk() Check

```ek9
r <- getExplicitResult()

if r.isOk()
  val <- r.ok()  // SAFE - checked with isOk()
  assert val?
```

#### Pattern 3: Explicit isError() Check

```ek9
r <- Result(String(), -1)

if r.isError()
  errorValue <- r.error()  // SAFE - checked with isError()
  assert errorValue?
```

#### Pattern 4: Ternary for Ok Value

```ek9
r <- Result("Steve", Integer())

// Get ok value or default
okValue <- r.isOk() <- r.ok() else String()
assert okValue?
```

#### Pattern 5: Ternary for Error Value

```ek9
r <- Result(String(), -1)

// Get error value or default
errorValue <- r.isError() <- r.error() else Integer()
assert errorValue?
```

#### Pattern 6: Safe Methods (NO GUARD NEEDED)

```ek9
r <- Result("Steve", Integer())

// These methods are ALWAYS safe:
assert r.isOk() or r.isError()        // Check status
hasOkSteve <- r contains "Steve"      // Check value
assuredValue <- r.okOrDefault("Stephen")  // Get ok or default
assuredValue2 <- r.getOrDefault("Stephen")  // Get or default
r.whenOk(someConsumer)                // Execute only if ok
r.whenError(someErrorConsumer)        // Execute only if error
someResult <- r.asOk("value")         // Create Result with just ok
anotherResult <- r.asError(21)        // Create Result with just error
```

---

### Result in Control Flow Constructs

#### Switch with Result Guard

```ek9
switch r <- getExplicitResult() then r.ok()
  case "Steve"
    someResult: "Just Steve"
  default
    someResult: r.ok()  // SAFE
```

#### Switch with Conditional

```ek9
switch r <- getExplicitResult() then conditional
  case 'A'
    value <- r.ok()  // SAFE - r checked by guard
  default
    value <- r.ok() + conditional  // SAFE
```

---

### Compiler Safety Enforcement

#### INVALID: Direct .get() without guard

```ek9
o <- Optional("Steve")

// ERROR: PRE_IR_CHECKS: UNSAFE_METHOD_ACCESS
value <- o.get()  // Must check o? first in if/ternary
```

#### INVALID: Direct .ok()/.error() without guard

```ek9
r <- Result(String(), Integer())

// ERROR: PRE_IR_CHECKS: UNSAFE_METHOD_ACCESS
okValue <- r.ok()  // Must check r.isOk() first

// ERROR: PRE_IR_CHECKS: UNSAFE_METHOD_ACCESS
errorValue <- r.error()  // Must check r.isError() first
```

#### INVALID: Check outside of safe block

```ek9
o <- Optional("Steve")
hasValue <- o?
assert hasValue

if hasValue
  // ERROR: PRE_IR_CHECKS: UNSAFE_METHOD_ACCESS
  value <- o.get()  // Check not in same block - unsafe
```

#### INVALID: Reassignment within safe block

```ek9
o <- Optional("Steve")

if o?
  val <- o.get()  // OK

  // ERROR: PRE_IR_CHECKS: NO_REASSIGNMENT_WITHIN_SAFE_ACCESS
  o: Optional("Stephen")  // Cannot mutate in safe block
  val: o.get()
```

---

### Key Principles Summary

**1. Both Optional and Result have `?` operator:**
- Use guards directly on Optional/Result
- No manual null checking needed

**2. Guards make unwrapping SAFE:**
- `if o <- getOptional()` → `.get()` is safe in body
- `if r <- getResult()` → `.ok()`/`.error()` safe in body

**3. Ternary for pure contexts:**
- `value <- o? <- o.get() else default`
- `okValue <- r.isOk() <- r.ok() else default`

**4. Safe methods don't require guards:**
- `.whenPresent()`, `.whenOk()`, `.whenError()`
- `.getOrDefault()`, `.okOrDefault()`
- `.contains()`, `.isOk()`, `.isError()`, `.is empty`

**5. Compiler enforces safety:**
- Cannot call `.get()`, `.ok()`, `.error()` without guard
- `UNSAFE_METHOD_ACCESS` error at compile time
- Cannot reassign within safe access block

**6. Guard scope is control flow block:**
- Safe access valid for entire if/switch/while/for/try body
- Includes catch and finally blocks for try guards
- Cannot escape guard scope

---

## Quick Reference: Choosing Guard Operators

### Decision Tree (Use-Case Based)

```
┌─ First time using this variable?
│  └─ YES → Use `<-` (declaration guard)
│     • Declares NEW variable
│     • Checks if value is SET
│     • Example: if name <- getName()
│     • Example: while conn <- getConnection()
│
│  └─ NO → Variable already exists...
│     ┌─ Is variable already set with value you want to keep?
│     │  (e.g., lazy cache, don't re-compute if already has value)
│     │  └─ YES → Use `:=?` (assignment if unset)
│     │     • Checks LEFT side first (is target unset?)
│     │     • LAZY EVALUATION - only calls expression if left unset
│     │     • Assigns to left if left was unset
│     │     • Then checks result is SET (guard on result)
│     │     • Perfect for caching/lazy init patterns
│     │     • Example: if cache :=? computeExpensive() with cache > 100
│     │     • Example: config.timeout :=? 30  // Set default only if unset
│     │
│     └─ NO → Need to update/replace value...
│        ┌─ Need to verify new value is valid before using?
│        │  (e.g., fetching data that might fail, need to check if SET)
│        │  └─ YES → Use `?=` (guarded assignment)
│        │     • Evaluates expression ALWAYS
│        │     • Checks if result is SET
│        │     • Then assigns
│        │     • Example: if data ?= fetchData() with data.isValid()
│        │     • Example: switch case result ?= trySource1() -> use(result)
│        │
│        └─ NO → Use `:=` (blind assignment)
│           • No safety checks
│           • Assignment happens unconditionally
│           • Use when value is known safe
│           • Or when explicit condition validates it
│           • Example: if count := getCount() with count > 0
│           • Example: existing := calculate()  // Known to return valid value
```

### Quick Comparison Table

| Scenario | Operator | Rationale |
|----------|----------|-----------|
| **First use** | `<-` | Declaration + guard check |
| **Lazy cache** | `:=?` | Don't re-evaluate if already set + guard check |
| **Update with check** | `?=` | Always evaluate + guard check result |
| **Update no check** | `:=` | Blind assignment (known safe or explicit condition) |

### Examples by Use Case

#### Use Case: Lazy Initialization (Cache Pattern)

```ek9
// BEFORE: Traditional pattern (verbose)
cache <- String()

if not cache?
  temp <- computeExpensive()
  if temp?
    cache: temp
    if cache.length() > 100
      process(cache)

// AFTER: EK9 with :=? (concise, safe)
cache <- String()

if cache :=? computeExpensive() with cache.length() > 100
  process(cache)
```

**Use `:=?`** - Checks left (is cache unset?), lazy evaluates, assigns, checks result

---

#### Use Case: Fetch Data with Validation

```ek9
// Existing variable, always fetch fresh data, validate before use
existing <- String()

if existing ?= fetchFreshData() with existing.isValid()
  process(existing)
```

**Use `?=`** - Always fetches, checks result is SET, then assigns

---

#### Use Case: Try Multiple Sources

```ek9
// Try source1, if fails try source2, if fails try source3
switch
  case result ?= trySource1() -> use(result)
  case result ?= trySource2() -> use(result)
  case result ?= trySource3() -> use(result)
  default -> handleFailure()
```

**Use `?=`** - Each source tried in order, guard checks result

---

#### Use Case: Declaration with Validation

```ek9
// First time using 'user' variable
if user <- fetchUser(id) with user.isActive()
  process(user)
```

**Use `<-`** - Declares new variable + guard check

---

#### Use Case: Known Safe Update

```ek9
// Updating counter (known to be valid Integer)
count <- 0

if count := increment() with count > threshold
  processHighCount(count)
```

**Use `:=`** - No guard needed, explicit condition validates

---

### Common Mistakes and Fixes

#### Mistake 1: Using `:=` when guard needed

```ek9
// ❌ WRONG - value might be unset!
existing <- String()

if existing := mayFail()  // No guard check!
  value <- existing  // Might be unset - unsafe!
```

```ek9
// ✅ CORRECT - use ?= for guard check
if existing ?= mayFail()
  value <- existing  // Safe - ?= checked it's SET
```

---

#### Mistake 2: Using `?=` for lazy cache (wasteful)

```ek9
// ❌ INEFFICIENT - always evaluates computeExpensive()!
cache <- String()

if cache ?= computeExpensive()
  use(cache)  // But computeExpensive() called even if cache was set!
```

```ek9
// ✅ EFFICIENT - use :=? for lazy evaluation
cache <- String()

if cache :=? computeExpensive()
  use(cache)  // computeExpensive() only called if cache was unset
```

---

#### Mistake 3: Confusing `<-` and `?=`

```ek9
// ❌ WRONG - redeclaration error if 'data' exists
data <- String()  // First declaration

if data <- fetchData()  // ❌ ERROR - data already declared!
```

```ek9
// ✅ CORRECT - use ?= to update existing variable
data <- String()

if data ?= fetchData()  // ✅ Updates existing variable
```

---

### Key Decision Points

1. **New variable?** → Use `<-`
2. **Don't re-compute if set?** → Use `:=?`
3. **Always fetch + validate?** → Use `?=`
4. **Known safe value?** → Use `:=`

**When in doubt:** If you need to check if the value is SET, use a guard (`<-`, `?=`, or `:=?`). Only use `:=` when you're absolutely certain the value is safe or have an explicit condition to validate it.

---

## Test Files to Create

### IR Generation Tests

1. **`ifAssignmentGuard.ek9`** - Test `:=` (NO guard check)
   - Verify NO QUESTION_OPERATOR in IR
   - Just STORE + condition evaluation

2. **`ifDeclarationGuard.ek9`** - Test `<-` (WITH guard check)
   - Verify QUESTION_OPERATOR with IS_NULL in IR
   - Verify LOGICAL_AND_BLOCK for condition

3. **`ifGuardedAssignment.ek9`** - Test `?=` (WITH guard check)
   - Verify QUESTION_OPERATOR with IS_NULL in IR
   - Verify assignment only happens in true branch

4. **`assignmentIfUnset.ek9`** - Test `:=?` standalone
   - Verify LEFT side check pattern
   - Not used in control flow typically

### Bytecode Generation Tests

After IR is fixed, create bytecode tests for:

1. `:=` assignment guard (should NOT have null check bytecode)
2. `<-` declaration guard (should have null check + _isSet bytecode) - **Already have!**
3. `?=` guarded assignment (should have null check + _isSet bytecode)
4. `:=?` assignment if unset (different bytecode pattern)

## Critical Fix: IR Generation

**Current Problem**: IR generator treats ALL guards the same way
- Calls `.?()` for ALL guard forms
- No distinction between `:=`, `<-`, `?=`

**Required Fix**: IR generator must differentiate:
- `:=` → NO QUESTION_OPERATOR
- `<-` → YES QUESTION_OPERATOR
- `?=` → YES QUESTION_OPERATOR
- `:=?` → Different pattern (checks left side)

**Location to Fix**:
- `ControlFlowChainGenerator.java` or similar
- IR generation phase for guard expressions
- Detect operator type and generate appropriate IR

## LLVM Backend Benefits

Once IR is fixed:
- JVM backend: IS_NULL → `ifnull` instruction
- LLVM backend: IS_NULL → `icmp eq null` instruction
- Both work automatically from correct IR!
- No special cases needed in either backend!

## Next Steps

1. ✅ Document all guard forms and semantics (this file)
2. Create IR test examples for each guard form
3. Fix IR generator to handle each form correctly
4. Verify existing bytecode tests still work
5. Create bytecode tests for missing forms
6. LLVM backend will work automatically!
