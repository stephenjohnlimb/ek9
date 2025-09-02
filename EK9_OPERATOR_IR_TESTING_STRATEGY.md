# EK9 Operator IR Testing Strategy

## Overview

This document provides a comprehensive strategy for systematically testing EK9 operator IR generation through granular, focused test files. This approach enables detailed analysis of the complete compilation pipeline from EK9 source → IR generation → IR optimization → target code generation (JVM/LLVM).

**Related Documentation:**
- **`EK9_IR_TEST_DRIVEN_DEVELOPMENT.md`** - `@IR` directive methodology and TDD patterns
- **`EK9_OPERATOR_SEMANTICS.md`** - EK9 operator behavior and semantics (mutating vs non-mutating)
- **`EK9_IR_AND_CODE_GENERATION.md`** - IR generation principles and literal encoding
- **`EK9_COALESCING_OPERATORS_IR_GENERATION.md`** - Complex operator IR generation patterns
- **`EK9_DEVELOPMENT_CONTEXT.md`** - EK9 type system architecture and testing patterns

## Strategic Purpose

### Systematic Compiler Pipeline Analysis

**Goal**: Create focused test files that enable systematic analysis of each stage in EK9's compilation pipeline:

1. **EK9 Source** → Simple, isolated operator usage
2. **IR Generation** → Method resolution and call patterns  
3. **IR Optimization** → Constant folding, dead code elimination, method inlining
4. **Code Generation** → JVM bytecode vs LLVM IR comparison

### Benefits of Granular Testing

**Precision**: Each operator gets its own IR footprint without noise from unrelated operations
**Optimization Visibility**: Clear before/after comparison of optimization passes
**Target Comparison**: Direct JVM vs LLVM code generation analysis
**Regression Prevention**: Changes to IR generation immediately visible
**Educational Value**: Living documentation of EK9 compilation behavior

## Operator Coverage Analysis

Based on analysis of `compiler-main/src/main/java/org/ek9lang/compiler/common/OperatorMap.java`:

### Complete Operator Inventory (88 Operators)

#### Comparison Operators (6)
- `<` → `_lt`, `<=` → `_lteq`, `>` → `_gt`, `>=` → `_gteq`
- `==` → `_eq`, `<>` → `_neq`

#### Arithmetic Operators (4)  
- `+` → `_add`, `-` → `_sub`, `*` → `_mul`, `/` → `_div`

#### Assignment Operators (4)
- `+=` → `_addAss`, `-=` → `_subAss`, `*=` → `_mulAss`, `/=` → `_divAss`

#### Bitwise Operators (5)
- `and` → `_and`, `or` → `_or`, `xor` → `_xor`  
- `<<` → `_shftl`, `>>` → `_shftr`

#### Special Operators (15)
- `<=>` → `_cmp`, `:=:` → `_copy`, `:^:` → `_replace`, `<~>` → `_fuzzy`, `:~:` → `_merge`
- `++` → `_inc`, `--` → `_dec`, `~` → `_negate`, `!` → `_fac`
- `?` → `_isSet`, `$` → `_string`, `$$` → `_json`, `#?` → `_hashcode`, `#^` → `_promote`
- `#<` → `_prefix`, `#>` → `_suffix`

#### Math Operators (6)
- `^` → `_pow`, `mod` → `_mod`, `rem` → `_rem`
- `sqrt` → `_sqrt`, `abs` → `_abs`

#### Utility Operators (8)
- `close` → `close`, `empty` → `_empty`, `length` → `_len`, `|` → `_pipe`

#### Collection Operators (8)
- `sort` → `_sort`, `filter` → `_filter`, `collect` → `_collect`, `map` → `_map`
- `group` → `_group`, `split` → `_split`, `head` → `_head`, `tail` → `_tail`

#### Text Operators (2)
- `contains` → `_contains`, `matches` → `_matches`

### Type Coverage Strategy

**Priority 1 - Core Coverage (covers 80% of operators):**
- **Integer** - Implements broadest range: arithmetic, comparison, bitwise, math, special operators
- **Boolean** - Logical operators, negation, basic comparisons
- **String** - Text operations, concatenation, pattern matching

**Priority 2 - Specialized Coverage:**
- **Float** - Mixed arithmetic, mathematical functions, promotion
- **Bits** - Shift operations (`<<`, `>>`) and bitwise operations
- **RegEx** - Pattern matching with String (`matches`)

**Priority 3 - Complete Coverage:**
- **Collection types** (List, Dict) - Collection operators
- **JSON** - JSON conversion and piping
- **Special types** (Duration, Date, etc.) - Domain-specific operators

## File Organization Structure

### Directory Layout

Located in existing `compiler-main/src/test/resources/examples/irGeneration/operatorUse/`:

```
operatorUse/
├── arithmetic/
│   ├── addition_operator.ek9          // z <- 1 + 1  
│   ├── subtraction_operator.ek9       // z <- 5 - 2
│   ├── multiplication_operator.ek9    // z <- 3 * 4
│   ├── division_operator.ek9          // z <- 8 / 2
│   └── negate_operator.ek9            // z <- -5
├── comparison/
│   ├── less_than_operator.ek9         // b <- 1 < 2
│   ├── less_equal_operator.ek9        // b <- 1 <= 2  
│   ├── greater_than_operator.ek9      // b <- 2 > 1
│   ├── greater_equal_operator.ek9     // b <- 2 >= 1
│   ├── equals_operator.ek9            // b <- 1 == 1
│   ├── not_equals_operator.ek9        // b <- 1 <> 2
│   └── compare_operator.ek9           // i <- 1 <=> 2
├── assignment/
│   ├── add_assign_operator.ek9        // x += 5
│   ├── subtract_assign_operator.ek9   // x -= 2
│   ├── multiply_assign_operator.ek9   // x *= 3
│   ├── divide_assign_operator.ek9     // x /= 2
│   ├── copy_operator.ek9              // x :=: y
│   ├── replace_operator.ek9           // x :^: y
│   └── merge_operator.ek9             // x :~: y
├── logical/
│   ├── and_operator.ek9               // b <- true and false
│   ├── or_operator.ek9                // b <- true or false
│   ├── xor_operator.ek9               // b <- true xor false
│   └── not_operator.ek9               // b <- ~true
├── bitwise/
│   ├── bitwise_and_operator.ek9       // i <- 5 and 3
│   ├── bitwise_or_operator.ek9        // i <- 5 or 3
│   ├── bitwise_xor_operator.ek9       // i <- 5 xor 3
│   ├── left_shift_operator.ek9        // b <- bits << 2
│   └── right_shift_operator.ek9       // b <- bits >> 2
├── increment/
│   ├── increment_operator.ek9         // i <- x++
│   └── decrement_operator.ek9         // i <- x--
├── mathematical/
│   ├── power_operator.ek9             // f <- 2 ^ 3
│   ├── modulo_operator.ek9            // i <- 7 mod 3
│   ├── remainder_operator.ek9         // i <- 7 rem 3
│   ├── square_root_operator.ek9       // f <- sqrt 16
│   ├── absolute_operator.ek9          // i <- abs -5
│   └── factorial_operator.ek9         // i <- 5!
├── special/
│   ├── is_set_operator.ek9            // b <- value?
│   ├── string_operator.ek9            // s <- 42$
│   ├── json_operator.ek9              // j <- value$$
│   ├── hashcode_operator.ek9          // i <- value#?
│   ├── promote_operator.ek9           // f <- i#^
│   ├── prefix_operator.ek9            // c <- s#<
│   ├── suffix_operator.ek9            // c <- s#>
│   ├── fuzzy_operator.ek9             // i <- s1 <~> s2
│   └── pipe_operator.ek9              // x | y
├── utility/
│   ├── empty_operator.ek9             // b <- value empty
│   └── length_operator.ek9            // i <- value length
├── text/
│   ├── contains_operator.ek9          // b <- "hello" contains "ell"
│   └── matches_operator.ek9           // b <- "abc123" matches /\d+/
└── collection/
    ├── sort_operator.ek9              // sorted <- list sort
    ├── filter_operator.ek9            // filtered <- list filter
    ├── map_operator.ek9               // mapped <- list map
    ├── collect_operator.ek9           // collected <- list collect  
    └── head_tail_operators.ek9        // first <- list head, rest <- list tail
```

## IR Testing Templates

### Standard File Template

Each operator test file follows this structure:

```ek9
#!ek9
<?-
  Test IR generation for [OPERATOR_NAME] operator ([OPERATOR_SYMBOL])
  
  Purpose: Analyze IR generation patterns for method resolution, 
  temporary variable allocation, and optimization opportunities.
  
  Expected IR: Method call to [TYPE]._[METHOD_NAME]([PARAM_TYPE])
-?>
defines module [operator_name].test

  defines function
  
    @IR: IR_GENERATION: FUNCTION: "[operator_name].test::testOperator": `
    [Expected IR output following established patterns]
    `
    testOperator()
      [Minimal test case - typically 1-3 lines]

//EOF
```

### Example: Addition Operator Test

```ek9
#!ek9
<?-
  Test IR generation for addition operator (+)
  
  Purpose: Analyze IR generation for Integer._add(Integer) method resolution,
  literal loading, temporary variable management, and optimization potential.
  
  Expected IR: Method call to Integer._add(Integer) with literal operands
-?>
defines module addition.test

  defines function
  
    @IR: IR_GENERATION: FUNCTION: "addition.test::testAddition": `
    ConstructDfn: addition.test::testAddition()->org.ek9.lang::Void
    OperationDfn: addition.test::testAddition.c_init()->org.ek9.lang::Void
    BasicBlock: _entry_1
    RETURN
    OperationDfn: addition.test::testAddition.i_init()->org.ek9.lang::Void  
    BasicBlock: _entry_1
    RETURN
    OperationDfn: addition.test::testAddition.testAddition()->addition.test::testAddition
    BasicBlock: _entry_1
    _temp_i_init = CALL (addition.test::testAddition)this.i_init()
    RETURN this
    OperationDfn: addition.test::testAddition._call()->org.ek9.lang::Void
    BasicBlock: _entry_1
    SCOPE_ENTER _scope_1
    REFERENCE z, org.ek9.lang::Integer
    _temp1 = LOAD_LITERAL 1, org.ek9.lang::Integer
    RETAIN _temp1
    SCOPE_REGISTER _temp1, _scope_1
    _temp2 = LOAD_LITERAL 1, org.ek9.lang::Integer
    RETAIN _temp2
    SCOPE_REGISTER _temp2, _scope_1
    _temp3 = CALL (org.ek9.lang::Integer)_temp1._add(_temp2)
    RETAIN _temp3
    SCOPE_REGISTER _temp3, _scope_1
    RELEASE z
    STORE z, _temp3
    RETAIN z
    SCOPE_REGISTER z, _scope_1
    SCOPE_EXIT _scope_1
    RETURN
    `
    testAddition()
      z <- 1 + 1

//EOF
```

## IR Generation Patterns

### Key IR Instructions for Operators

**Literal Loading:**
```
_temp1 = LOAD_LITERAL 1, org.ek9.lang::Integer
_temp2 = LOAD_LITERAL 2, org.ek9.lang::Integer
```

**Method Resolution and Call:**
```  
_temp3 = CALL (org.ek9.lang::Integer)_temp1._add(_temp2)
```

**Memory Management:**
```
RETAIN _temp1
SCOPE_REGISTER _temp1, _scope_1  
RELEASE z
STORE z, _temp3
```

**Assignment Operators (Mutating):**
```
_temp1 = LOAD x
_temp2 = LOAD_LITERAL 5, org.ek9.lang::Integer
CALL (void)_temp1._addAss(_temp2)  // Mutates _temp1 directly
```

### Optimization Opportunities

**Constant Folding:**
- **Before**: `_temp3 = CALL _temp1._add(_temp2)` where both temps are constants
- **After**: `_temp3 = LOAD_LITERAL 2, org.ek9.lang::Integer` (for 1 + 1)

**Method Inlining:**
- **Before**: Full method call with parameter passing
- **After**: Direct arithmetic operation (for primitive-like operations)

**Dead Code Elimination:**
- Remove unused temporary variables and operations

## Integration with EK9 Compilation Pipeline

### Phase Integration

**Phase 1-6**: Symbol resolution and method matching
**Phase 7 (IR_GENERATION)**: Transform to IR using these test patterns  
**Phase 8-9 (IR_ANALYSIS/OPTIMIZATION)**: Apply optimization passes
**Phase 10+ (CODE_GENERATION)**: Generate JVM/LLVM code

### Testing Workflow

1. **Create operator test file** following templates above
2. **Run IR generation**: `mvn test -Dtest=ExamplesBasicsTest -pl compiler-main`
3. **Capture unoptimized IR** from test output
4. **Run IR optimization passes** (future implementation)
5. **Compare optimized IR** for transformation analysis
6. **Generate target code** for JVM/LLVM comparison

## Type-Specific Considerations

### Integer Type Testing

**Advantages:**
- Implements broadest operator range (arithmetic, comparison, bitwise, mathematical)
- Clear constant folding opportunities (`1 + 1` → `2`)
- Mixed-type operations with Float provide promotion testing

**Key Operators**: `+`, `-`, `*`, `/`, `<`, `==`, `++`, `--`, `^`, `mod`, `rem`, `abs`, `!`

### Boolean Type Testing  

**Advantages:**
- Logical operators (`and`, `or`, `xor`, `~`)
- Simple true/false constant folding
- Clear mutating vs non-mutating distinction

**Key Operators**: `and`, `or`, `xor`, `~`, `==`, `<>`

### String Type Testing

**Advantages:**
- Text operations (`contains`, `matches`)  
- Concatenation patterns (`+`)
- Prefix/suffix operations (`#<`, `#>`)

**Key Operators**: `+`, `contains`, `matches`, `#<`, `#>`, `empty`, `length`

### Mutating vs Non-Mutating Operators

**Mutating Operators** (modify the object, return reference):
- Assignment: `+=`, `-=`, `*=`, `/=`  
- Increment/Decrement: `++`, `--`
- Copy/Replace/Merge: `:=:`, `:^:`, `:~:`
- Pipe: `|`

**Non-Mutating Operators** (return new objects):
- Arithmetic: `+`, `-`, `*`, `/`
- Comparison: `<`, `<=`, `>`, `>=`, `==`, `<>`
- Mathematical: `^`, `sqrt`, `abs`, `!`
- Utility: `$`, `$$`, `#?`, `empty`, `length`

## Development Workflow

### Step-by-Step Implementation

1. **Start with Priority 1 operators** (arithmetic, comparison)
2. **Use Integer type** for maximum operator coverage
3. **Create minimal test cases** (1-3 lines of EK9 code)
4. **Follow @IR directive patterns** from existing IR test files
5. **Verify IR generation** through compiler testing
6. **Document optimization opportunities** discovered
7. **Expand to specialized operators** as needed

### Quality Assurance

**File Naming**: Descriptive operator names (`addition_operator.ek9`)
**Documentation**: Clear purpose and expected IR in each file  
**Minimal Scope**: One operator per file for precision
**IR Accuracy**: Verify @IR directives match actual compiler output
**Cross-References**: Link to related documentation as needed

## Future Extensions

### Advanced Operator Scenarios

**Mixed-Type Operations:**
- `Integer + Float` → promotion testing
- `String + Character` → type conversion

**Generic Type Operators:**
- `List<String>` operators
- `Optional<Integer>` operators  

**Coalescing Operators:**
- Leverage patterns from `EK9_COALESCING_OPERATORS_IR_GENERATION.md`
- Complex control flow IR analysis

### Multi-Target Analysis

**JVM vs LLVM Comparison:**
- Same IR → different target code generation
- Performance analysis between targets
- Optimization effectiveness comparison

## Conclusion

This systematic operator IR testing strategy provides comprehensive coverage of EK9's 88 operators through focused, granular test files. By isolating each operator, we can precisely analyze IR generation patterns, optimization opportunities, and target code generation quality. This approach supports EK9's evolution from a single-target (JVM) to multi-target (JVM/LLVM) compiler while maintaining regression-free development through living documentation of compiler behavior.

The strategy integrates seamlessly with existing EK9 documentation and testing infrastructure, leveraging established `@IR` directive patterns and TDD methodologies while providing the granular analysis needed for advanced compiler development.