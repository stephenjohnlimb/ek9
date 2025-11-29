# EK9 Directive and Test File Work: Mandatory Process

**Date**: 2025-11-15
**Purpose**: Step-by-step process for creating/modifying EK9 test files with directives

---

## Overview

**CRITICAL WORKFLOW**: When creating or modifying ANY EK9 test file with directives (`@IR`, `@BYTECODE`, `@Resolved`, `@Error`, `@Complexity`), you MUST follow this exact process. This is NON-NEGOTIABLE for compiler work where hundreds of test files must follow identical patterns.

**🛑 STOP - DO NOT IMPLEMENT IMMEDIATELY 🛑**

## The 5-Step Mandatory Process

### Step 1: Check Documentation First

```bash
# Search for the directive name in documentation
grep -r "@IR:\|@BYTECODE:\|@Resolved:\|@Error:" *.md
# Read the relevant section completely
# EK9_IR_TDD_METHODOLOGY.md - @IR directive format and examples
# EK9_IR_AND_CODE_GENERATION.md - @BYTECODE directive format (lines 2274-2336)
```

### Step 2: Find Working Examples (MANDATORY - not optional)

```bash
# Find 2-3 existing test files with the same directive type
grep -r "@BYTECODE: CODE_GENERATION_AGGREGATES" compiler-main/src/test/resources/examples/bytecodeGeneration/
grep -r "@IR: IR_GENERATION: FUNCTION" compiler-main/src/test/resources/examples/irGeneration/
# Read complete examples - understand the EXACT format
```

### Step 3: Copy Structure from Working Example

- Open a working example file
- Copy the ENTIRE directive structure including backticks
- Note placement (BEFORE the construct it describes)
- Note formatting (content starts on SAME line as opening backtick)

### Step 4: Modify Content Only - NEVER Change Format

- Replace type name: `"module::OldName"` → `"module::NewName"`
- Replace directive content within backticks
- Keep ALL formatting identical (backticks, placement, spacing)
- Use `#CP` for constant pool references in @BYTECODE (not actual numbers)

### Step 5: Validate Against Pattern

- Compare your new file side-by-side with the example
- Verify backtick placement matches exactly
- Verify indentation matches exactly
- Verify directive is BEFORE the construct (not after, not in comments)

## Why This Process is Mandatory

Steve has created **hundreds of test files** following identical patterns. The goal is **"boring consistency"** - easy to reproduce, easy to spot errors. Random format variations:

- ❌ Break test infrastructure
- ❌ Require debugging time (30+ min per file)
- ❌ Scale terribly (hundreds more files needed)

Following the pattern:

- ✅ Works immediately (5 min per file)
- ✅ Scales perfectly
- ✅ Self-documenting through consistency

## Common Anti-Patterns to AVOID

### Anti-Pattern 1: Using Comment Syntax

❌ **Never use comment syntax for directives:**
```ek9
//@BYTECODE              ← WRONG - this is a comment, not a directive
//public void _main();  ← WRONG - directives are NOT comments
```

✅ **Always use actual directive syntax:**
```ek9
@BYTECODE: CODE_GENERATION_AGGREGATES: TYPE: "module::Name": `content here
with multiple lines
within backticks`
```

### Anti-Pattern 2: Directive Placement After Code

❌ **Never place directives after the code:**
```ek9
MyFunction()
  stdout.println("test")

@IR: IR_GENERATION: ...  ← WRONG - too late, directive must be BEFORE
```

✅ **Always place directives before the construct:**
```ek9
@IR: IR_GENERATION: FUNCTION: "module::MyFunction": `...`
MyFunction()
  stdout.println("test")
```

### Anti-Pattern 3: Content on New Line After Backtick

❌ **Never start content on new line after backtick:**
```ek9
@BYTECODE: CODE_GENERATION_AGGREGATES: TYPE: "module::Name": `
public class module.Name {  ← WRONG - must start on same line as backtick
```

✅ **Always start content on same line as opening backtick:**
```ek9
@BYTECODE: CODE_GENERATION_AGGREGATES: TYPE: "module::Name": `public class module.Name {
  static {};
  ...
}`
```

## Documentation References

- **EK9_IR_TDD_METHODOLOGY.md** - Complete @IR directive guide with examples
- **EK9_IR_AND_CODE_GENERATION.md** (lines 2274-2336) - @BYTECODE format requirements
- **Existing test files** - 30+ bytecode tests, 100+ IR tests following identical patterns

## Golden Rule

**When in doubt**: Copy from a working example. Never invent new formats.

---

**Last Updated**: 2025-11-15
