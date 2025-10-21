# EK9 Control Flow Canonical Scope Architecture

**Date:** 2025-10-20
**Status:** CANONICAL REFERENCE - Based on actual IR generation from 1004 passing tests
**Purpose:** Define consistent scope architecture for ALL control flow constructs

---

## 🎯 Critical Principle: Scope Consistency Across All Control Flow

**ALL control flow constructs (if/else, while, do-while, for-range, for-in, switch, try/catch) MUST follow the same fundamental scope architecture pattern for:**
1. **Maintainability** - Developers understand one pattern
2. **Correctness** - Memory management semantics are consistent
3. **Backend Optimization** - JVM and LLVM can optimize uniformly
4. **Future Features** - Guards, expression forms work across all constructs

---

## 📊 Scope Architecture: Two Patterns Based on Iteration

### **Pattern A: NON-ITERATIVE Control Flow** (if/else, switch)
Condition evaluated **ONCE**, multiple execution paths

### **Pattern B: ITERATIVE Control Flow** (while, do-while, for-range, for-in)
Condition evaluated **EVERY ITERATION**, single path repeats

---

## 🔍 PATTERN A: Non-Iterative (If/Else, Switch)

### **Scope Hierarchy** (from actual IR: `controlFlow::simpleIf`)

```
_call (operation scope - implicit)
  └─ _scope_1: GUARD SCOPE
      └─ _scope_2: CHAIN/CONDITION SCOPE ⭐
          ├─ Condition temps live here
          ├─ _scope_3: BRANCH 1 BODY
          ├─ _scope_4: BRANCH 2 BODY
          └─ _scope_N: BRANCH N BODY
```

### **IR Evidence:**
```
SCOPE_ENTER _scope_1                    // Guard scope (empty - for future guards)
SCOPE_ENTER _scope_2                    // Chain scope
  condition_evaluation:
    _temp3 = LOAD value
    SCOPE_REGISTER _temp3, _scope_2     // ⭐ Condition temps in chain scope
    _temp4 = LOAD_LITERAL 10
    SCOPE_REGISTER _temp4, _scope_2     // ⭐ Condition temps in chain scope
  body_evaluation:
    SCOPE_ENTER _scope_3                // Branch body scope
      _temp6 = LOAD_LITERAL "High"
      SCOPE_REGISTER _temp6, _scope_3   // Body temps in body scope
    SCOPE_EXIT _scope_3
SCOPE_EXIT _scope_2                     // Chain scope exit
SCOPE_EXIT _scope_1                     // Guard scope exit
```

### **Key Characteristics:**
1. **_scope_1 (guard scope):** Currently empty, reserved for guard variables (future)
2. **_scope_2 (chain scope):** Contains condition evaluation temps
   - ❗ **NOT an iteration scope** - entered/exited ONCE
   - Condition temps live until chain scope exits
3. **_scope_3+ (branch body scopes):** Each branch has its own scope
   - Body temps released when branch exits

### **Why This Pattern:**
- Condition evaluated **ONCE**
- Different branches are **mutually exclusive**
- No need for iteration scopes
- Condition temps can persist in chain scope (not regenerated)

---

## 🔄 PATTERN B: Iterative (While, Do-While, For-Range, For-In)

### **Scope Hierarchy** (from actual IR: `loops::simpleWhileLoop`)

```
_call (operation scope - implicit)
  └─ _scope_1: USER VARIABLES (stdout, counter, etc.)
      └─ _scope_2: GUARD SCOPE
          └─ _scope_3: LOOP CONTROL SCOPE
              ├─ _scope_4: CONDITION ITERATION SCOPE ⭐ (enters/exits EVERY iteration)
              │   └─ Condition temps released each iteration
              └─ _scope_5: BODY ITERATION SCOPE ⭐ (enters/exits EVERY iteration)
                  └─ Body temps released each iteration
```

### **IR Evidence:**
```
SCOPE_ENTER _scope_1                    // User variables
  stdout, counter registered to _scope_1
  SCOPE_ENTER _scope_2                  // Guard scope (empty)
    SCOPE_ENTER _scope_3                // Loop control scope (empty)
      CONTROL_FLOW_CHAIN [scope_id: _scope_3]
        condition_evaluation:
          SCOPE_ENTER _scope_4          // ⭐ Iteration scope!
            _temp4 = LOAD counter
            SCOPE_REGISTER _temp4, _scope_4  // Temp dies at _scope_4 exit
          SCOPE_EXIT _scope_4           // ⭐ Releases condition temps!
        body_evaluation:
          SCOPE_ENTER _scope_5          // ⭐ Iteration scope!
            _temp8 = LOAD counter
            SCOPE_REGISTER _temp8, _scope_5  // Temp dies at _scope_5 exit
          SCOPE_EXIT _scope_5           // ⭐ Releases body temps!
    SCOPE_EXIT _scope_3
  SCOPE_EXIT _scope_2
SCOPE_EXIT _scope_1
```

### **Key Characteristics:**
1. **_scope_1 (user variables):** Loop-external variables (declared before loop)
2. **_scope_2 (guard scope):** Currently empty, reserved for guard variables (future)
3. **_scope_3 (loop control scope):** Empty wrapper, passed as `scope_id` to CONTROL_FLOW_CHAIN
4. **_scope_4 (condition iteration):** ⭐ **ENTERS/EXITS EVERY ITERATION**
   - Condition temps released after each evaluation
   - Tight memory management
5. **_scope_5 (body iteration):** ⭐ **ENTERS/EXITS EVERY ITERATION**
   - Body temps released after each iteration
   - Loop variables re-created each iteration

### **Why This Pattern:**
- Condition evaluated **EVERY ITERATION**
- Temps must be **released each iteration** (not accumulated)
- Iteration scopes provide tight memory management
- Backend can optimize per-iteration scope management

---

## 🆕 FOR-RANGE EXTENDED PATTERN (New Addition)

### **Challenge:** Range expressions create many temps that must be released

Example:
```ek9
for i in config.getSettings().getMin() ... config.getSettings().getMax() by getStep()
// Creates ~12 temps for expression chains!
```

### **Solution:** Add **EVALUATION SCOPE** for one-time setup

```
_call (operation scope - implicit)
  └─ _scope_1: USER VARIABLES
      └─ _scope_2: GUARD SCOPE
          ├─ _scope_3: RANGE EVALUATION SCOPE ⭐ NEW!
          │   ├─ Start expression temps (released immediately)
          │   ├─ End expression temps (released immediately)
          │   └─ Step expression temps (released immediately)
          ├─ _start, _end, _step, _current (persist in _scope_2)
          └─ _scope_4: LOOP CONTROL SCOPE
              ├─ _scope_5: CONDITION ITERATION SCOPE
              └─ _scope_6: BODY ITERATION SCOPE
```

### **IR Structure:**
```
SCOPE_ENTER _scope_1                    // User variables
  SCOPE_ENTER _scope_2                  // Guard scope
    SCOPE_ENTER _scope_3                // ⭐ Range evaluation scope
      // Evaluate start expression
      _temp1 = LOAD config
      SCOPE_REGISTER _temp1, _scope_3   // Dies at _scope_3 exit
      _temp2 = CALL _temp1.getSettings()
      SCOPE_REGISTER _temp2, _scope_3   // Dies at _scope_3 exit
      _temp3 = CALL _temp2.getMin()
      SCOPE_REGISTER _temp3, _scope_3   // Dies at _scope_3 exit
      STORE _tempStart, _temp3
      SCOPE_REGISTER _tempStart, _scope_3  // Dies at _scope_3 exit

      // Evaluate end expression (similar)
      // Evaluate step expression (similar)
    SCOPE_EXIT _scope_3                 // ⭐ Releases ALL evaluation temps!

    // Store to persistent variables in guard scope
    _temp10 = LOAD _tempStart
    SCOPE_REGISTER _temp10, _scope_2    // Persists entire loop
    STORE _start, _temp10
    SCOPE_REGISTER _start, _scope_2     // Persists entire loop

    // Similar for _end, _step, _current

    SCOPE_ENTER _scope_4                // Loop control scope
      CONTROL_FLOW_CHAIN [scope_id: _scope_4]
        condition_evaluation:
          SCOPE_ENTER _scope_5          // Condition iteration scope
            // _current <= _end condition
          SCOPE_EXIT _scope_5
        body_evaluation:
          SCOPE_ENTER _scope_6          // Body iteration scope
            // Loop variable binding + user body + increment
          SCOPE_EXIT _scope_6
    SCOPE_EXIT _scope_4
  SCOPE_EXIT _scope_2
SCOPE_EXIT _scope_1
```

### **Memory Lifecycle:**
- **Evaluation temps** (_temp1-3): Live only during _scope_3 → **Released immediately**
- **Range variables** (_start, _end, _step, _current): Live in _scope_2 → **Entire loop**
- **Condition temps**: Live only during _scope_5 iteration → **Per iteration**
- **Body temps**: Live only during _scope_6 iteration → **Per iteration**

---

## 📋 CANONICAL PATTERNS SUMMARY

### **Pattern A: Non-Iterative (If/Else, Switch)**
```
Guard Scope (for future guards)
  └─ Chain/Condition Scope (condition temps persist)
      └─ Branch Body Scopes (one per branch)
```

**Scope Purposes:**
1. **Guard scope**: Future guard variables
2. **Chain scope**: Condition temps (evaluated once)
3. **Branch scopes**: Branch-specific temps

### **Pattern B-Simple: Iterative (While, Do-While)**
```
User Variables Scope
  └─ Guard Scope (for future guards)
      └─ Loop Control Scope (empty wrapper)
          ├─ Condition Iteration Scope (per-iteration release)
          └─ Body Iteration Scope (per-iteration release)
```

**Scope Purposes:**
1. **User variables**: Pre-loop declarations
2. **Guard scope**: Future guard variables
3. **Loop control**: CONTROL_FLOW_CHAIN scope_id
4. **Condition iteration**: Condition temps (released each iteration)
5. **Body iteration**: Body temps + loop variables (released each iteration)

### **Pattern B-Extended: Iterative with Setup (For-Range, For-In)**
```
User Variables Scope
  └─ Guard Scope (for future guards)
      ├─ Evaluation Scope (expression temps, released ONCE)
      ├─ Loop state variables (_start, _end, _step, _current in guard scope)
      └─ Loop Control Scope (empty wrapper)
          ├─ Condition Iteration Scope (per-iteration release)
          └─ Body Iteration Scope (per-iteration release)
```

**Scope Purposes:**
1. **User variables**: Pre-loop declarations
2. **Guard scope**: Future guard variables + loop state variables
3. **Evaluation scope**: ⭐ **NEW** - Range/iterator expression temps (released immediately)
4. **Loop control**: CONTROL_FLOW_CHAIN scope_id
5. **Condition iteration**: Condition temps (released each iteration)
6. **Body iteration**: Body temps + loop variables (released each iteration)

---

## 🔮 FUTURE: Guards Implementation

### **What Guards Are:**
```ek9
// Guard with declaration
for prefix <- getPrefix() then i in 1 ... 10
  stdout.println(`${prefix}${i}`)

// Guard with guarded assignment
while result ?= tryCompute() then result.isValid()
  processResult(result)
```

### **Where Guards Go:**
**Guard variables are declared in the GUARD SCOPE (_scope_2)**

### **Modified Pattern B-Extended with Guards:**
```
User Variables Scope
  └─ Guard Scope
      ├─ ⭐ GUARD VARIABLES (prefix, result, etc.)
      ├─ Evaluation Scope (range/iterator setup)
      ├─ Loop state variables (_start, _end, etc.)
      └─ Loop Control Scope
          ├─ Condition Iteration Scope
          └─ Body Iteration Scope
```

### **IR Structure with Guards:**
```
SCOPE_ENTER _scope_2                    // Guard scope
  // Process guard variable (preFlowStatement)
  prefix <- getPrefix()
  SCOPE_REGISTER prefix, _scope_2       // Lives entire loop

  // Then process evaluation scope / loop setup
  SCOPE_ENTER _scope_3                  // Evaluation scope
    // Range evaluation
  SCOPE_EXIT _scope_3

  // Loop state setup
  _start, _end, _step, _current in _scope_2

  // Loop execution
  SCOPE_ENTER _scope_4                  // Loop control
    // Condition and body iterations
  SCOPE_EXIT _scope_4
SCOPE_EXIT _scope_2                     // Releases guard variables + loop state
```

---

## 🎯 IMPLEMENTATION CHECKLIST

### **For Each New Control Flow Construct:**

✅ **1. Determine Pattern:**
- [ ] Non-iterative (A) or Iterative (B)?
- [ ] Simple (B-Simple) or with Setup (B-Extended)?

✅ **2. Implement Guard Scope:**
- [ ] Create guard scope (_scope_2)
- [ ] Leave empty for now (throw CompilerException if guards detected)
- [ ] Add comment: `// TODO: Process guard variables here when implemented`

✅ **3. For Iterative: Implement Iteration Scopes:**
- [ ] Condition iteration scope (SCOPE_ENTER/EXIT in condition_evaluation)
- [ ] Body iteration scope (SCOPE_ENTER/EXIT in body_evaluation)
- [ ] Verify temps registered to iteration scopes
- [ ] Verify scopes exit at end of evaluation arrays

✅ **4. For B-Extended: Implement Evaluation Scope:**
- [ ] Create evaluation scope for expression temps
- [ ] Evaluate all setup expressions (start, end, step, iterator, etc.)
- [ ] Register ALL temps to evaluation scope
- [ ] EXIT evaluation scope immediately
- [ ] Store results to persistent variables in guard scope

✅ **5. Backend Integration:**
- [ ] Create specialized ASM generator
- [ ] Wire into ControlFlowChainAsmGenerator dispatch
- [ ] Verify bytecode generation handles scope management

✅ **6. Testing:**
- [ ] IR test with @IR directive
- [ ] Bytecode test with @BYTECODE directive
- [ ] Verify scope structure matches canonical pattern
- [ ] Verify temps released at correct scope exits

---

## 📚 APPLIES TO FUTURE CONSTRUCTS

### **Switch Statements:** Pattern A (Non-Iterative)
```
Guard Scope
  └─ Switch Evaluation Scope (evaluation variable + condition temps)
      ├─ Case 1 Body Scope
      ├─ Case 2 Body Scope
      └─ Default Body Scope
```

### **Try/Catch/Finally:** Special Pattern
```
Guard Scope
  ├─ Try Body Scope
  ├─ Catch 1 Body Scope (with exception variable)
  ├─ Catch N Body Scope
  └─ Finally Scope (always executes)
```

### **For-In Loops:** Pattern B-Extended
```
Guard Scope
  ├─ Evaluation Scope (collection expression temps)
  ├─ _iterator variable (persist entire loop)
  └─ Loop Control Scope
      ├─ Condition Iteration Scope (hasNext() condition)
      └─ Body Iteration Scope (next() call + loop variable + user body)
```

---

## ✅ CONSISTENCY VALIDATION

### **Run This Check for Every Control Flow Implementation:**

1. **Count scopes:** Does it match the canonical pattern?
2. **Check guard scope:** Is _scope_2 reserved for guards?
3. **For iterative:** Do condition/body have iteration scopes?
4. **For B-Extended:** Is there an evaluation scope?
5. **Check temp registration:** Are temps registered to correct scopes?
6. **Check scope exits:** Do scopes exit at proper boundaries?

### **Red Flags:**
- ❌ Temps registered to wrong scope level
- ❌ Missing iteration scopes for loops
- ❌ Evaluation temps persisting entire loop
- ❌ Condition temps in non-iteration scope for loops
- ❌ Inconsistent pattern compared to while/if examples

---

## 🎓 KEY INSIGHTS

1. **Pattern consistency enables comprehension:** One mental model for all control flow
2. **Iteration scopes enable tight memory management:** Release temps every iteration
3. **Evaluation scopes prevent temp accumulation:** Complex expressions don't leak temps
4. **Guard scopes enable future enhancement:** Guards work uniformly across all constructs
5. **Backend optimization opportunity:** Uniform patterns enable uniform optimization

---

**STATUS:** CANONICAL REFERENCE - Use this document for ALL control flow implementation
**LAST UPDATED:** 2025-10-20
**VALIDATED BY:** 1004 passing tests (if/else, while, do-while implementations)
