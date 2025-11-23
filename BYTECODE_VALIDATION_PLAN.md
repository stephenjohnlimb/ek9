# EK9 Bytecode Generation Validation Plan

**Date:** 2025-11-03 (Updated: 2025-11-17)
**Purpose:** Comprehensive validation of all bytecode generation examples after bug fixes
**Total Programs:** 44 (41 original + 3 guard tests)

---

## Setup

```bash
export EK9_HOME=/Users/stevelimb/IdeaProjects/ek9/compiler-main/target
cd /Users/stevelimb/IdeaProjects/ek9/compiler-main/src/test/resources/examples/bytecodeGeneration
```

---

## Test Categories

### Category 1: Simple Programs (No Arguments) - 28 programs

| # | Program | Expected Behavior |
|---|---------|-------------------|
| 1 | `./helloWorld/helloWorld.ek9` | Print "Hello World!" |
| 2 | `./simpleClass/simpleClass.ek9` | Test constructor, getter, increment |
| 3 | `./simpleIfStatement/simpleIfStatement.ek9` | Basic if statement test |
| 4 | `./ifElseStatement/ifElseStatement.ek9` | If-else branching test |
| 5 | `./ifElseIfChain/ifElseIfChain.ek9` | If-else-if chain test |
| 6 | `./simpleWhileLoop/simpleWhileLoop.ek9` | While loop iteration test |
| 7 | `./simpleDoWhileLoop/simpleDoWhileLoop.ek9` | Do-while loop test |
| 8 | `./simpleForRangeLoop/simpleForRangeLoop.ek9` | For-range loop test |
| 9 | `./simpleForInLoop/simpleForInLoop.ek9` | For-in loop test |
| 10 | `./nestedIfInForRange/nestedIfInForRange.ek9` | Nested if in for loop |
| 11 | `./simpleListLiteral/simpleListLiteral.ek9` | List literal creation test |
| 12 | `./simpleTryCatch/simpleTryCatch.ek9` | Try-catch exception handling |
| 13 | `./simpleTryFinally/simpleTryFinally.ek9` | Try-finally resource cleanup |
| 14 | `./tryCatchFinally/tryCatchFinally.ek9` | Try-catch-finally combo |
| 15 | `./simpleThrow/simpleThrow.ek9` | Throw exception test |
| 16 | `./throwCatchExceptionSubtypes/throwCatchExceptionSubtypes.ek9` | Exception subtype handling |
| 17 | `./arithmeticOperators/arithmeticOperators.ek9` | +, -, *, /, % operators |
| 18 | `./comparisonOperators/comparisonOperators.ek9` | <, >, <=, >=, ==, <> operators |
| 19 | `./mathematicalOperators/mathematicalOperators.ek9` | Math functions |
| 20 | `./textOperators/textOperators.ek9` | String operations |
| 21 | `./bitwiseOperators/bitwiseOperators.ek9` | Bitwise AND, OR, XOR, shift |
| 22 | `./andOperator/andOperator.ek9` | Logical AND test |
| 23 | `./orOperator/orOperator.ek9` | Logical OR test |
| 24 | `./xorOperator/xorOperator.ek9` | Logical XOR test |
| 25 | `./notOperator/notOperator.ek9` | Logical NOT test |
| 26 | `./unaryOperators/unaryOperators.ek9` | Unary +, -, ++, -- |
| 27 | `./isSetOperator/isSetOperator.ek9` | isSet (?) operator test |
| 28 | `./conversionOperators/conversionOperators.ek9` | Type conversion operators |
| 29 | `./stringCompareAndFuzzy/stringCompareAndFuzzy.ek9` | String comparison and fuzzy match |
| 30 | `./forRangeAssertions/forRangeUnsetStart/forRangeUnsetStart.ek9` | For range with unset start |
| 31 | `./forRangeAssertions/forRangeUnsetEnd/forRangeUnsetEnd.ek9` | For range with unset end |
| 32 | `./forRangeAssertions/forRangeUnsetBy/forRangeUnsetBy.ek9` | For range with unset by |

### Category 2: Guard Tests (Null Safety) - 3 programs

**Status:** ✅ PASSING (Updated 2025-11-17)
**IR/Bytecode:** Uses new CONTROL_FLOW_CHAIN pattern with explicit IS_NULL instructions

| # | Program | Guard Pattern | Key Bytecode Feature |
|---|---------|---------------|---------------------|
| 1 | `./ifWithGuard/ifWithGuard.ek9` | `if value <- optional` | Single guard with IS_NULL check at line 99 (`ifnull 106`) |
| 2 | `./ifWithGuardAndCondition/ifWithGuardAndCondition.ek9` | `if value <- optional with condition` | Guard + condition AND logic, IS_NULL at line 171 (`ifnull 178`) |
| 3 | `./ifElseIfWithGuards/ifElseIfWithGuards.ek9` | Multiple guards in if/else-if chain | TWO guards: IS_NULL at lines 184 and 280 (`ifnull 191`, `ifnull 287`) |

**Critical Improvement (2025-11-17):**
- **Before:** Guards used `?()` method call hiding null checks (vulnerable to NPE if method called on null)
- **After:** Explicit `ifnull` bytecode instruction BEFORE `_isSet()` call (100% null-safe)

**IR Pattern:**
```
CONTROL_FLOW_CHAIN
[
  chain_type: "QUESTION_OPERATOR"
  condition_chain:
  [
    case_type: "NULL_CHECK"
    condition_evaluation:
    [
      _temp = IS_NULL value    ← Explicit IS_NULL instruction
    ]
    primitive_condition: _temp
    body_evaluation:
    [
      _tempResult = CALL_STATIC Boolean._ofFalse()  ← Returns false if null
    ]
  ]
  default_body_evaluation:
  [
    _temp2 = LOAD value
    _tempResult = CALL _temp2._isSet()  ← Only called if NOT null
  ]
]
```

**Bytecode Pattern:**
```
97: aload         12        # Load guard variable
99: ifnull        106       # ← EXPLICIT NULL CHECK!
102: iconst_0                # Not null - continue to _isSet()
103: goto          107
106: iconst_1                # Null - return false immediately
107: istore        14
...
126: aload         12
128: astore        17
130: aload         17
132: invokevirtual _isSet   # Only called if NOT null (safe!)
```

**Test Execution:**
```bash
export EK9_HOME=/Users/stevelimb/IdeaProjects/ek9/compiler-main/target
cd /Users/stevelimb/IdeaProjects/ek9/compiler-main/src/test/resources/examples/bytecodeGeneration

# Guard-only pattern
cd ifWithGuard && ./ifWithGuard.ek9 1    # Set value
cd ifWithGuard && ./ifWithGuard.ek9 0    # Unset value

# Guard + condition pattern
cd ifWithGuardAndCondition && ./ifWithGuardAndCondition.ek9 1  # Condition fails
cd ifWithGuardAndCondition && ./ifWithGuardAndCondition.ek9 2  # Condition succeeds

# Multiple guards pattern
cd ifElseIfWithGuards && ./ifElseIfWithGuards.ek9 1  # First guard succeeds
cd ifElseIfWithGuards && ./ifElseIfWithGuards.ek9 2  # Second guard succeeds
cd ifElseIfWithGuards && ./ifElseIfWithGuards.ek9 0  # Both guards fail
```

**Maven Test Results:**
```
[INFO] Tests run: 1, Failures: 0, Errors: 0 -- IfWithGuardTest
[INFO] Tests run: 2, Failures: 0, Errors: 0 -- IfWithGuardAndConditionTest
[INFO] BUILD SUCCESS
```

---

### Category 3: Programs Requiring Arguments - 9 programs

#### comprehensiveNestedControlFlow (2 Integer args)
```bash
cd comprehensiveNestedControlFlow
./comprehensiveNestedControlFlow.ek9 10 5    # Normal case
./comprehensiveNestedControlFlow.ek9 5 3     # Edge case
./comprehensiveNestedControlFlow.ek9 0 0     # Zero inputs
./comprehensiveNestedControlFlow.ek9 20 7    # Larger values
```

#### simpleSwitchLiteral (Integer arg)
```bash
cd simpleSwitchLiteral
./simpleSwitchLiteral.ek9 1      # Case 1
./simpleSwitchLiteral.ek9 2      # Case 2
./simpleSwitchLiteral.ek9 3      # Case 3
./simpleSwitchLiteral.ek9 99     # Default case
```

#### simpleSwitchFloat (Float arg)
```bash
cd simpleSwitchFloat
./simpleSwitchFloat.ek9 1.5      # Case 1.5
./simpleSwitchFloat.ek9 2.5      # Case 2.5
./simpleSwitchFloat.ek9 3.5      # Case 3.5
./simpleSwitchFloat.ek9 99.9     # Default case
```

#### simpleSwitchString (String arg)
```bash
cd simpleSwitchString
./simpleSwitchString.ek9 "hello"    # Case hello
./simpleSwitchString.ek9 "world"    # Case world
./simpleSwitchString.ek9 "test"     # Case test
./simpleSwitchString.ek9 "other"    # Default case
```

#### simpleSwitchBoolean (Boolean arg)
```bash
cd simpleSwitchBoolean
./simpleSwitchBoolean.ek9 true      # True case
./simpleSwitchBoolean.ek9 false     # False case
./simpleSwitchBoolean.ek9 invalid   # Error handling
```

#### simpleSwitchCharacterPromotion (Character arg)
```bash
cd simpleSwitchCharacterPromotion
./simpleSwitchCharacterPromotion.ek9 A      # Case A
./simpleSwitchCharacterPromotion.ek9 B      # Case B
./simpleSwitchCharacterPromotion.ek9 Z      # Default case
```

#### simpleSwitchExplicitEquality (Integer arg)
```bash
cd simpleSwitchExplicitEquality
./simpleSwitchExplicitEquality.ek9 10      # Case 10
./simpleSwitchExplicitEquality.ek9 20      # Case 20
./simpleSwitchExplicitEquality.ek9 30      # Case 30
./simpleSwitchExplicitEquality.ek9 99      # Default case
```

#### multipleCaseLiterals (Integer arg)
```bash
cd multipleCaseLiterals
./multipleCaseLiterals.ek9 1      # Case 1,2,3
./multipleCaseLiterals.ek9 4      # Case 4,5,6
./multipleCaseLiterals.ek9 5      # Case 4,5,6
./multipleCaseLiterals.ek9 99     # Default case
```

#### multipleCaseCharacterPromotion (Character arg)
```bash
cd multipleCaseCharacterPromotion
./multipleCaseCharacterPromotion.ek9 A      # Case A,B,C
./multipleCaseCharacterPromotion.ek9 D      # Case D,E,F
./multipleCaseCharacterPromotion.ek9 X      # Default case
```

---

## Execution Strategy

### Phase 1: Quick Smoke Test (5 programs)
Run a representative sample to verify basic functionality:
```bash
export EK9_HOME=/Users/stevelimb/IdeaProjects/ek9/compiler-main/target
cd /Users/stevelimb/IdeaProjects/ek9/compiler-main/src/test/resources/examples/bytecodeGeneration

# 1. Hello World
cd helloWorld && ./helloWorld.ek9

# 2. Simple Class (our recent fix)
cd ../simpleClass && ./simpleClass.ek9

# 3. Control Flow
cd ../simpleIfStatement && ./simpleIfStatement.ek9

# 4. Exception Handling
cd ../simpleTryCatch && ./simpleTryCatch.ek9

# 5. Operators
cd ../arithmeticOperators && ./arithmeticOperators.ek9
```

### Phase 2: No-Argument Programs (28 programs)
Run all programs that don't require arguments:
```bash
cd /Users/stevelimb/IdeaProjects/ek9/compiler-main/src/test/resources/examples/bytecodeGeneration

for dir in helloWorld simpleClass simpleIfStatement ifElseStatement ifElseIfChain \
           simpleWhileLoop simpleDoWhileLoop simpleForRangeLoop simpleForInLoop \
           nestedIfInForRange simpleListLiteral simpleTryCatch simpleTryFinally \
           tryCatchFinally simpleThrow throwCatchExceptionSubtypes \
           arithmeticOperators comparisonOperators mathematicalOperators \
           textOperators bitwiseOperators andOperator orOperator xorOperator \
           notOperator unaryOperators isSetOperator conversionOperators \
           stringCompareAndFuzzy; do
    echo "=== Testing $dir ==="
    cd $dir && ./*.ek9
    cd ..
done
```

### Phase 3: Argument-Based Programs (9 programs)
Run programs with various argument combinations:
```bash
# Switch statements and complex control flow
cd comprehensiveNestedControlFlow
./comprehensiveNestedControlFlow.ek9 10 5
./comprehensiveNestedControlFlow.ek9 0 0
cd ..

cd simpleSwitchLiteral
./simpleSwitchLiteral.ek9 1
./simpleSwitchLiteral.ek9 99
cd ..

cd simpleSwitchFloat
./simpleSwitchFloat.ek9 1.5
./simpleSwitchFloat.ek9 99.9
cd ..

cd simpleSwitchString
./simpleSwitchString.ek9 "hello"
./simpleSwitchString.ek9 "other"
cd ..

cd simpleSwitchBoolean
./simpleSwitchBoolean.ek9 true
./simpleSwitchBoolean.ek9 false
cd ..

# ... (continue for other arg-based programs)
```

### Phase 4: Edge Cases and Assertions
Test for-range assertion programs:
```bash
cd forRangeAssertions/forRangeUnsetStart && ./forRangeUnsetStart.ek9
cd ../forRangeUnsetEnd && ./forRangeUnsetEnd.ek9
cd ../forRangeUnsetBy && ./forRangeUnsetBy.ek9
```

---

## Success Criteria

Each program must:
1. ✅ **Compile successfully** (bytecode generation completes)
2. ✅ **Execute without VerifyError** (JVM accepts bytecode)
3. ✅ **Produce expected output** (program logic correct)
4. ✅ **Exit with code 0** (no runtime exceptions)

## Failure Handling

If a program fails:
1. **Record** the exact error message
2. **Capture** stack trace if available
3. **Use javap** to inspect bytecode: `javap -c -v ProgramClass.class`
4. **Analyze** which bug category it falls into:
   - Field vs local variable issue
   - Parameter slot allocation issue
   - Super constructor call issue
   - Other bytecode generation issue

---

## Expected Validation Results

After all 3 bug fixes:
- **Field Access**: All programs with field access should work (simpleClass, etc.)
- **Parameters**: All programs with constructor/method parameters should work
- **Super Calls**: All programs with class inheritance should work
- **Control Flow**: All if/while/for/switch programs should work
- **Operators**: All arithmetic/logical/comparison operator programs should work
- **Exception Handling**: All try/catch/finally/throw programs should work

**Target: 100% pass rate (44/44 programs)**

**Guard Tests (3 programs):** ✅ PASSING
- All guard tests validate explicit IS_NULL bytecode generation
- Null safety guaranteed at JVM level (not just EK9 compiler level)

---

## Automated Test Script

```bash
#!/bin/bash
# run_all_bytecode_tests.sh

export EK9_HOME=/Users/stevelimb/IdeaProjects/ek9/compiler-main/target
BASE_DIR=/Users/stevelimb/IdeaProjects/ek9/compiler-main/src/test/resources/examples/bytecodeGeneration

cd "$BASE_DIR"

PASSED=0
FAILED=0

# Test programs without arguments
NO_ARG_PROGRAMS=(
    "helloWorld/helloWorld.ek9"
    "simpleClass/simpleClass.ek9"
    "simpleIfStatement/simpleIfStatement.ek9"
    # ... (full list)
)

for prog in "${NO_ARG_PROGRAMS[@]}"; do
    echo "Testing: $prog"
    if ./"$prog" > /dev/null 2>&1; then
        echo "  ✅ PASSED"
        ((PASSED++))
    else
        echo "  ❌ FAILED"
        ((FAILED++))
    fi
done

# Test programs with arguments
echo "Testing: comprehensiveNestedControlFlow.ek9 10 5"
if ./comprehensiveNestedControlFlow/comprehensiveNestedControlFlow.ek9 10 5 > /dev/null 2>&1; then
    echo "  ✅ PASSED"
    ((PASSED++))
else
    echo "  ❌ FAILED"
    ((FAILED++))
fi

# ... (more argument tests)

echo ""
echo "========================================="
echo "Total: $((PASSED + FAILED)) programs"
echo "Passed: $PASSED"
echo "Failed: $FAILED"
echo "========================================="
```

---

## Next Steps After Validation

1. **Document Failures** - Record any programs that fail
2. **Categorize Issues** - Group failures by root cause
3. **Fix Remaining Bugs** - Address any new issues found
4. **Re-validate** - Run full suite again
5. **Update Documentation** - Add findings to EK9_JVM_BYTECODE_GENERATION.md

**End of Plan**
