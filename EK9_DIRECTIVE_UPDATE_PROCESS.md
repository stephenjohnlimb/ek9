# EK9 Directive Update Process

This document describes the automated process for updating `@IR` and `@BYTECODE` directives in EK9 test files when intentional changes are made to IR generation or bytecode output.

## When to Use This Process

**Use this process when:**
- Adding new synthetic methods/operators to generated code
- Changing operation ordering (e.g., alphabetical sorting)
- Adding new fields to IR constructs (e.g., `traceableFields`, `implementsTraits`)
- Refactoring code generation that intentionally changes output format
- Any planned change that will cause directive tests to fail

**Do NOT use this process when:**
- Tests fail unexpectedly after refactoring (investigate the bug first!)
- You haven't intentionally changed IR/bytecode generation
- Investigating a bug (the failing test output helps diagnose issues)

## Overview

The EK9 compiler has two types of directive tests:

| Directive | Purpose | Listener Class | Test Pattern |
|-----------|---------|----------------|--------------|
| `@IR` | Validates intermediate representation | `IRDirectiveListener` | `*IRTest`, `*IRGenerationTest` |
| `@BYTECODE` | Validates JVM bytecode output | `ByteCodeDirectiveListener` | `**/bytecode/*Test` |

Both listeners output structured blocks when tests fail, enabling automated parsing and file updates.

## Structured Output Format

When a directive test fails, the listener outputs a structured block to stderr:

**For IR directives:**
```
===IR_UPDATE_START===
FILE: /path/to/target/test-classes/example.ek9
SYMBOL: module.name::SymbolName
CATEGORY: FUNCTION
---IR_CONTENT_START---
[actual IR content here]
---IR_CONTENT_END---
===IR_UPDATE_END===
```

**For BYTECODE directives:**
```
===BYTECODE_UPDATE_START===
FILE: /path/to/target/test-classes/example.ek9
SYMBOL: module.name::SymbolName
CATEGORY: TYPE
---BYTECODE_CONTENT_START---
[actual bytecode content here]
---BYTECODE_CONTENT_END===
===BYTECODE_UPDATE_END===
```

## Step-by-Step Workflow

### Step 1: Run the Failing Tests and Capture Output

```bash
# For IR tests
mvn test -Dtest="*IRTest,*IRGenerationTest" -pl compiler-main 2>&1 | tee /tmp/ir_test_output.txt

# For BYTECODE tests
mvn test -Dtest="**/bytecode/*Test" -pl compiler-main 2>&1 | tee /tmp/bytecode_test_output.txt
```

### Step 2: Verify Structured Blocks Were Captured

```bash
# Count IR update blocks
grep -c "===IR_UPDATE_START===" /tmp/ir_test_output.txt

# Count BYTECODE update blocks
grep -c "===BYTECODE_UPDATE_START===" /tmp/bytecode_test_output.txt
```

The count should approximately match the number of test errors.

### Step 3: Create and Run the Update Script

Save the appropriate Python script (see below), then run:

```bash
# For IR directives
python3 update_ir_directives.py /tmp/ir_test_output.txt

# For BYTECODE directives
python3 update_bytecode_directives.py /tmp/bytecode_test_output.txt
```

### Step 4: Iterate Until All Tests Pass

Repeat steps 1-3 until all tests pass. Usually 2-3 iterations are sufficient.

### Step 5: Run Full Build

```bash
mvn clean test -pl compiler-main
```

---

## Python Update Scripts

### IR Directive Update Script

Save this as `update_ir_directives.py`:

```python
#!/usr/bin/env python3
"""
Script to update @IR directives in EK9 test files based on test output.
Parses structured output from IRDirectiveListener and updates source files.

Usage:
    python3 update_ir_directives.py <test_output_file>
    cat test_output.txt | python3 update_ir_directives.py -
"""

import re
import sys
import os

def parse_ir_blocks(input_text):
    """Parse structured IR update blocks from test output."""
    blocks = []
    pattern = r'===IR_UPDATE_START===\s*\n' \
              r'FILE:\s*(.+?)\s*\n' \
              r'SYMBOL:\s*(.+?)\s*\n' \
              r'CATEGORY:\s*(.+?)\s*\n' \
              r'---IR_CONTENT_START---\s*\n' \
              r'(.*?)' \
              r'---IR_CONTENT_END---\s*\n' \
              r'===IR_UPDATE_END==='

    matches = re.findall(pattern, input_text, re.DOTALL)
    for match in matches:
        file_path, symbol, category, ir_content = match
        blocks.append({
            'file': file_path.strip(),
            'symbol': symbol.strip(),
            'category': category.strip(),
            'ir': ir_content.rstrip('\n')
        })
    return blocks

def convert_target_to_source_path(target_path):
    """Convert target/test-classes path to src/test/resources path."""
    if '/target/test-classes/' in target_path:
        return target_path.replace('/target/test-classes/', '/src/test/resources/')
    return target_path

def update_ir_directive(file_path, symbol, ir_content):
    """Update the @IR directive in the source file."""
    if not os.path.exists(file_path):
        print(f"WARNING: File not found: {file_path}")
        return False

    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Pattern to match @IR directive for this symbol
    # Format: @IR: PHASE: CATEGORY: "symbol": `content`
    escaped_symbol = re.escape(symbol)
    pattern = r'(@IR:\s*\w+:\s*\w+:\s*"' + escaped_symbol + r'":\s*`)([^`]*?)(`)'

    def replacement(match):
        prefix = match.group(1)
        suffix = match.group(3)
        return prefix + ir_content + suffix

    new_content, count = re.subn(pattern, replacement, content, flags=re.DOTALL)

    if count == 0:
        print(f"WARNING: Could not find @IR directive for symbol '{symbol}' in {file_path}")
        return False

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)

    print(f"Updated: {file_path} (symbol: {symbol})")
    return True

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 update_ir_directives.py <test_output_file>")
        print("       cat test_output.txt | python3 update_ir_directives.py -")
        sys.exit(1)

    input_file = sys.argv[1]
    if input_file == '-':
        input_text = sys.stdin.read()
    else:
        with open(input_file, 'r', encoding='utf-8') as f:
            input_text = f.read()

    blocks = parse_ir_blocks(input_text)
    print(f"Found {len(blocks)} IR update blocks")

    updated, failed = 0, 0
    seen = set()

    for block in blocks:
        key = (block['file'], block['symbol'])
        if key in seen:
            continue
        seen.add(key)

        source_path = convert_target_to_source_path(block['file'])
        if update_ir_directive(source_path, block['symbol'], block['ir']):
            updated += 1
        else:
            failed += 1

    print(f"\nSummary: {updated} files updated, {failed} failed")

if __name__ == '__main__':
    main()
```

### BYTECODE Directive Update Script

Save this as `update_bytecode_directives.py`:

```python
#!/usr/bin/env python3
"""
Script to update @BYTECODE directives in EK9 test files based on test output.
Parses structured output from ByteCodeDirectiveListener and updates source files.

Usage:
    python3 update_bytecode_directives.py <test_output_file>
    cat test_output.txt | python3 update_bytecode_directives.py -
"""

import re
import sys
import os

def parse_bytecode_blocks(input_text):
    """Parse structured BYTECODE update blocks from test output."""
    blocks = []
    pattern = r'===BYTECODE_UPDATE_START===\s*\n' \
              r'FILE:\s*(.+?)\s*\n' \
              r'SYMBOL:\s*(.+?)\s*\n' \
              r'CATEGORY:\s*(.+?)\s*\n' \
              r'---BYTECODE_CONTENT_START---\s*\n' \
              r'(.*?)' \
              r'---BYTECODE_CONTENT_END---\s*\n' \
              r'===BYTECODE_UPDATE_END==='

    matches = re.findall(pattern, input_text, re.DOTALL)
    for match in matches:
        file_path, symbol, category, bytecode_content = match
        blocks.append({
            'file': file_path.strip(),
            'symbol': symbol.strip(),
            'category': category.strip(),
            'bytecode': bytecode_content.rstrip('\n')
        })
    return blocks

def convert_target_to_source_path(target_path):
    """Convert target/test-classes path to src/test/resources path."""
    if '/target/test-classes/' in target_path:
        return target_path.replace('/target/test-classes/', '/src/test/resources/')
    return target_path

def update_bytecode_directive(file_path, symbol, bytecode_content):
    """Update the @BYTECODE directive in the source file."""
    if not os.path.exists(file_path):
        print(f"WARNING: File not found: {file_path}")
        return False

    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Pattern to match @BYTECODE directive for this symbol
    # Format: @BYTECODE: PHASE: CATEGORY: "symbol": `content`
    escaped_symbol = re.escape(symbol)
    pattern = r'(@BYTECODE:\s*\w+:\s*\w+:\s*"' + escaped_symbol + r'":\s*`)([^`]*?)(`)'

    def replacement(match):
        prefix = match.group(1)
        suffix = match.group(3)
        return prefix + bytecode_content + suffix

    new_content, count = re.subn(pattern, replacement, content, flags=re.DOTALL)

    if count == 0:
        print(f"WARNING: Could not find @BYTECODE directive for symbol '{symbol}' in {file_path}")
        return False

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)

    print(f"Updated: {file_path} (symbol: {symbol})")
    return True

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 update_bytecode_directives.py <test_output_file>")
        print("       cat test_output.txt | python3 update_bytecode_directives.py -")
        sys.exit(1)

    input_file = sys.argv[1]
    if input_file == '-':
        input_text = sys.stdin.read()
    else:
        with open(input_file, 'r', encoding='utf-8') as f:
            input_text = f.read()

    blocks = parse_bytecode_blocks(input_text)
    print(f"Found {len(blocks)} BYTECODE update blocks")

    updated, failed = 0, 0
    seen = set()

    for block in blocks:
        key = (block['file'], block['symbol'])
        if key in seen:
            continue
        seen.add(key)

        source_path = convert_target_to_source_path(block['file'])
        if update_bytecode_directive(source_path, block['symbol'], block['bytecode']):
            updated += 1
        else:
            failed += 1

    print(f"\nSummary: {updated} files updated, {failed} failed")

if __name__ == '__main__':
    main()
```

---

## Common Issues and Solutions

### Issue: Python script reports "Could not find directive"

**Cause:** Symbol name mismatch between actual function/class name and directive.

**Solution:** Check the source file - ensure the function/class name matches what the directive expects.

**Example from actual fix:**
```
# File had function: testAddAssign()
# Directive expected: add_assign.test::testAddAssignment
# Fix: Rename function to testAddAssignment() or update directive symbol
```

### Issue: Duplicate files with same module name

**Cause:** Two files define the same module, causing symbol resolution confusion.

**Solution:** Delete the duplicate file or rename one of the modules.

### Issue: Files getting corrupted

**Cause:** Script matched wrong content or interleaved output.

**Solution:**
1. Restore from git: `git checkout -- path/to/file.ek9`
2. Re-run the tests and script
3. The `OUTPUT_LOCK` synchronization should prevent interleaving

### Issue: Count mismatch between errors and blocks

**Cause:** Some tests may fail before reaching the directive check (e.g., compilation errors).

**Solution:** Fix compilation errors first, then re-run the update process.

---

## Quick Reference Commands

```bash
# === COMPLETE IR UPDATE WORKFLOW ===
mvn test -Dtest="*IRTest,*IRGenerationTest" -pl compiler-main 2>&1 | tee /tmp/ir_output.txt
python3 update_ir_directives.py /tmp/ir_output.txt
# Repeat until: Tests run: X, Failures: 0, Errors: 0

# === COMPLETE BYTECODE UPDATE WORKFLOW ===
mvn test -Dtest="**/bytecode/*Test" -pl compiler-main 2>&1 | tee /tmp/bc_output.txt
python3 update_bytecode_directives.py /tmp/bc_output.txt
# Repeat until: Tests run: X, Failures: 0, Errors: 0

# === VERIFY FULL BUILD ===
mvn clean test -pl compiler-main

# === RESTORE IF SOMETHING GOES WRONG ===
git checkout -- compiler-main/src/test/resources/examples/
```

---

## Architecture Notes

### Why Structured Output?

The structured output serves dual purposes:
1. **Debugging** - When tests fail unexpectedly, the actual output helps diagnose issues
2. **Automation** - When changes are intentional, enables bulk updates

### Why Synchronized Blocks?

Tests run in parallel (8 threads by default). Without synchronization, output from different tests interleaves, making parsing impossible. The `OUTPUT_LOCK` ensures complete blocks are written atomically.

### Path Conversion

Test files are copied to `target/test-classes/` during build. The scripts convert these paths back to `src/test/resources/` for updates.

---

## Historical Context

This process was developed in December 2025 when:
- `IRConstruct` was enhanced with `traceableFields` and `implementsTraits`
- `OperationInstr` was made `Comparable` for deterministic ordering
- Operations became sorted alphabetically by `symbol.getFriendlyName()`

These changes caused 42 IR tests and 89 BYTECODE tests to fail. The automated approach reduced update time from potentially hours of manual editing to approximately 10 minutes of scripted updates across 2-3 iterations.

---

## Listener Implementation Reference

The structured output is generated in:
- `compiler-main/src/main/java/org/ek9lang/compiler/directives/IRDirectiveListener.java`
- `compiler-main/src/main/java/org/ek9lang/compiler/directives/ByteCodeDirectiveListener.java`

Key implementation pattern:
```java
private static final Object OUTPUT_LOCK = new Object();

// In the comparison failure block:
synchronized (OUTPUT_LOCK) {
    System.err.println("===IR_UPDATE_START===");
    System.err.println("FILE: " + compilationEvent.source().getFileName());
    System.err.println("SYMBOL: " + resolutionDirective.getSymbolName());
    System.err.println("CATEGORY: " + resolutionDirective.getSymbolCategory());
    System.err.println("---IR_CONTENT_START---");
    System.err.print(output);  // Note: print, not println
    System.err.println("---IR_CONTENT_END---");
    System.err.println("===IR_UPDATE_END===");
    System.err.flush();
}
```
