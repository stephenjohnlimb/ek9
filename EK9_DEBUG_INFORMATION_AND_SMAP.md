# EK9 Debug Information and JSR-45 SMAP Implementation

## Overview

This document describes the complete implementation of debug information flow in the EK9 compiler, from EK9 source code through intermediate representation (IR) to JVM bytecode with JSR-45 SMAP (Source Map) support. This enables debugging of `.ek9` files with standard Java debuggers.

**Status**: ✅ **COMPLETE** - Full debug information chain implemented and tested

## The Debug Information Chain

The EK9 compiler maintains debug information through three stages:

```
EK9 Source (.ek9)
    ↓ (Parsing - Phase 0)
    ↓ Contains: Line numbers, column positions, source file paths
    ↓
IR (Intermediate Representation)
    ↓ (IR Generation - Phase 10)
    ↓ DebugInfo objects attached to IR instructions
    ↓
JVM Bytecode (.class)
    ↓ (Code Generation - Phase 15)
    ↓ LineNumberTable + SourceDebugExtension (SMAP)
    ↓
Java Debuggers (jdb, IDE debuggers)
    Can step through .ek9 source files!
```

## JSR-45 SMAP (Source Map)

### What is SMAP?

JSR-45 defines a standard mechanism for debugging non-Java languages on the JVM. Without SMAP, debuggers expect `.java` source files and fail when debugging compiled EK9 code. With SMAP, debuggers can correctly map bytecode back to `.ek9` source files.

**Key Specification**: [JSR-45: Debugging Support for Other Languages](https://jakarta.ee/specifications/debugging/2.0/)

### SMAP Format Structure

SMAP is stored in the `SourceDebugExtension` class file attribute:

```
SMAP
GeneratedFileName
LanguageName
*S LanguageName      ← Stratum section (language view)
*F                   ← File section
+ FileID FileName
  FilePath
*L                   ← Line section
InputStartLine#FileID:OutputStartLine
InputStartLine#FileID,InputLineCount:OutputStartLine,OutputLineIncrement
*E                   ← End marker
```

### EK9 SMAP Example

```
SourceDebugExtension:
  SMAP
  HelloWorld.class
  EK9
  *S EK9
  *F
  + 1 ./helloWorld.ek9
  ./helloWorld.ek9
  *L
  88#1:88
  89#1:89
  90#1:90
  *E
```

**Format Explanation**:
- `88#1:88` means: line 88 in file #1 (helloWorld.ek9) maps to bytecode line 88
- File IDs use `#N:` format (colon required) to distinguish from constant pool references
- Multiple mappings per source line handle complex expressions compiled to multiple bytecode instructions

## Implementation Components

### 1. SmapGenerator (NEW)

**Location**: `compiler-main/src/main/java/org/ek9lang/compiler/backend/jvm/SmapGenerator.java`

**Purpose**: Generates JSR-45 SMAP from IR debug information

**Key Features**:
- Collects debug info from all IR operations in an `IRConstruct`
- Maintains file ID mapping for multi-file constructs
- Generates 1:1 line mappings (source line → bytecode line)
- Returns `null` if no debug info available (allows graceful fallback)

**Core Methods**:

```java
public SmapGenerator(final String generatedFileName)
// Create generator for specific .class file

public void collectFromIRConstruct(final IRConstruct construct)
// Process all operations and basic blocks, extracting DebugInfo

public void addMapping(final DebugInfo debugInfo, final int outputLineNumber)
// Add single line mapping from IR debug info

public String generate()
// Generate complete SMAP string in JSR-45 format
```

**Usage Pattern**:

```java
final var smapGenerator = new SmapGenerator("HelloWorld.class");
smapGenerator.collectFromIRConstruct(construct);
final var smap = smapGenerator.generate();
if (smap != null) {
  classWriter.visitSource(sourceFileName, smap);
}
```

**Implementation Details**:

1. **File ID Management**: Uses `LinkedHashMap<String, Integer>` to preserve file order and assign sequential IDs
2. **Line Mapping**: Stores `LineMapping(inputLine, fileId, outputLine)` records
3. **Format Generation**: Builds SMAP string with correct sections and syntax
4. **Null Safety**: Returns `null` if no debug info (allows classes without debug data)

### 2. AsmStructureCreator Integration

**Location**: `compiler-main/src/main/java/org/ek9lang/compiler/backend/jvm/AsmStructureCreator.java`

**Changes** (lines 87-97):

```java
// Add source file information for debugging - uses actual .ek9 source filename from IR
classWriter.visitSource(construct.getSourceFileName(), null);

// Generate and add JSR-45 SMAP for .ek9 source debugging
final var smapGenerator = new SmapGenerator(getSimpleClassName(programClassName) + ".class");
smapGenerator.collectFromIRConstruct(construct);
final var smap = smapGenerator.generate();
if (smap != null) {
  // Add SourceDebugExtension attribute with SMAP
  classWriter.visitSource(construct.getSourceFileName(), smap);
}
```

**Key Points**:
- SMAP generation happens during class initialization (before method generation)
- Processes entire IR construct to collect all debug information
- Double `visitSource()` call: first without SMAP, then with SMAP (ASM requirement)
- Gracefully handles constructs without debug info

### 3. BytecodeNormalizer Enhancement

**Location**: `compiler-main/src/main/java/org/ek9lang/compiler/backend/jvm/BytecodeNormalizer.java`

**Purpose**: Normalizes javap output for stable test comparisons while preserving debug info

**New Configuration API**:

```java
// Default: Include all debug information
public static String normalize(final byte[] classBytes)

// Configurable: Selectively include/exclude debug sections
public static String normalize(final byte[] classBytes,
                                final boolean includeLineNumberTable,
                                final boolean includeSourceDebugExtension)
```

**Key Enhancements**:

1. **Verbose javap output** (line 78):
```java
final ProcessBuilder pb = new ProcessBuilder("javap", "-c", "-p", "-l", "-v", classFile.toString());
```
- `-c`: Disassemble bytecode
- `-p`: Show all classes and members (private included)
- `-l`: Print line number tables
- `-v`: Verbose output including SourceDebugExtension

2. **SMAP File ID Preservation** (line 158):
```java
// Normalize constant pool references: #7 → #CP (but NOT SMAP file IDs like #1:58)
// SMAP file IDs are always followed by colon, constant pool refs are not
.replaceAll("#(\\d+)(?!:)", "#CP")
```
**Critical Pattern**: Uses negative lookahead `(?!:)` to preserve SMAP file IDs like `88#1:88`

3. **Conditional Debug Info Filtering** (lines 166-173):
```java
// Optionally remove LineNumberTable sections
.replaceAll(includeLineNumberTable ? "(?!LineNumberTable:)"
    : "(?s)LineNumberTable:.*?(?=\\n\\n|\\n +}|\\n})", "")

// Optionally remove SourceDebugExtension
.replaceAll(includeSourceDebugExtension ? "(?!SourceDebugExtension:)"
    : "(?s)SourceDebugExtension:.*?(?=\\n\\n|\\n}|$)", "")
```

**Testing Strategy**:
- **Minimal Tests**: Most bytecode tests filter out `LineNumberTable` and `SourceDebugExtension` to focus on instruction sequences
- **Debug Info Tests**: Specific tests (like `HelloWorld`) validate complete debug information including SMAP
- **Similar to IR Tests**: Parallels the IR testing approach with minimal/verbose output modes

### 4. @BYTECODE Directive Structure

**Purpose**: EK9 testing infrastructure for validating generated JVM bytecode

**Critical Structure Requirement**: Directive **BEFORE** actual EK9 code

**Correct Pattern** (from `helloWorld.ek9`):

```ek9
defines module bytecode.test

  defines program

    @BYTECODE: CODE_GENERATION_AGGREGATES: TYPE: "bytecode.test::HelloWorld": `
    public class bytecode.test.HelloWorld {
      static {};
        Code:
             0: return

      public bytecode.test.HelloWorld();
        Code:
             0: aload_0
             1: invokespecial #CP  // Method java/lang/Object."<init>":()V
             4: aload_0
             5: invokevirtual #CP  // Method i_init:()V
             8: return
          LineNumberTable:
            line 88: 4
            line 88: 8

      public void _main();
        Code:
             0: aconst_null
             ...
          LineNumberTable:
            line 89: 0
            line 89: 2
            ...
    }
    SourceDebugExtension:
      SMAP
      HelloWorld.class
      EK9
      *S EK9
      *F
      + 1 ./helloWorld.ek9
      ./helloWorld.ek9
      *L
      88#1:88
      89#1:89
      90#1:90
      *E`

    HelloWorld()                    ← Line 88: Actual EK9 code AFTER directive
      stdout <- Stdout()            ← Line 89
      stdout.println("Hello, World") ← Line 90

//EOF
```

**Why This Structure?**

1. **Stable Line Numbers**: Actual EK9 code appears at **fixed line numbers** (88-90) regardless of directive content changes
2. **No Circular Dependency**: Directive specifies line numbers that refer to code appearing below it
3. **Parser Compatibility**: EK9 parser expects directives at construct definition level (inside `defines program`)
4. **Follows IR Pattern**: Matches pattern used in `irGeneration/` test files where `@IR` directive precedes actual code

**Key Lesson**: Initial implementation attempted directive after code, creating circular dependency:
- Adding SMAP lines → shifts source code → changes line numbers → requires directive update → repeat
- Solution: Directive first, code at fixed lines below

## Testing Strategy

### BytecodeNormalizer Tests

**Location**: `compiler-main/src/test/java/org/ek9lang/compiler/backend/jvm/BytecodeNormalizerTest.java`

**Coverage** (6 tests):
1. `testNormalizeClassWithBasicMethods()` - Basic structure normalization
2. `testNormalizeRemovesConstantPoolIndices()` - Constant pool reference normalization
3. `testNormalizePreservesMethodSignatureComments()` - Comment preservation
4. `testNormalizeWithLineNumberTables()` - LineNumberTable preservation
5. `testNormalizeWithSourceDebugExtension()` - SMAP preservation
6. `testNormalizeWithoutDebugInfo()` - Minimal output (both flags false)

**Test Pattern**:

```java
@Test
void testNormalizeWithSourceDebugExtension() throws Exception {
  final byte[] classBytes = compileSimpleClass();
  final String normalized = BytecodeNormalizer.normalize(classBytes, true, true);

  // Verify SMAP present
  assertTrue(normalized.contains("SourceDebugExtension:"));
  assertTrue(normalized.contains("SMAP"));
  assertTrue(normalized.contains("*S EK9"));

  // Verify file IDs preserved (not normalized to #CP)
  assertTrue(normalized.contains("#1:")); // SMAP file ID format
}
```

### HelloWorld Integration Test

**Location**: `compiler-main/src/test/resources/examples/bytecodeGeneration/helloWorld/helloWorld.ek9`

**Purpose**: End-to-end validation of complete debug information chain

**Validates**:
- ✅ EK9 source parses correctly
- ✅ IR generation includes DebugInfo
- ✅ Bytecode includes LineNumberTable with correct line numbers (88-90)
- ✅ SourceDebugExtension contains complete SMAP
- ✅ SMAP file IDs not corrupted by normalization (`88#1:88` preserved)
- ✅ javap can read and display SMAP

**Test Execution**:

```bash
mvn test -Dtest=HelloWorldTest -pl compiler-main
```

**Results**:
```
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
```

### Verifying SMAP in Generated Classes

**Command**:

```bash
javap -v path/to/HelloWorld.class | grep -A 25 "SourceDebugExtension:"
```

**Expected Output**:

```
SourceDebugExtension:
  SMAP
  HelloWorld.class
  EK9
  *S EK9
  *F
  + 1 ./helloWorld.ek9
  ./helloWorld.ek9
  *L
  88#1:88
  88#1:88
  89#1:89
  ...
  *E
```

## Configuration Usage Guide

### For Most Bytecode Tests (Minimal Output)

Focus on instruction sequences, filter out debug info:

```java
final String normalized = BytecodeNormalizer.normalize(classBytes, false, false);
```

**Removes**:
- LineNumberTable sections
- SourceDebugExtension (SMAP)

**Preserves**:
- Method signatures
- Bytecode instructions
- Constant pool comments (method/field names)

### For Debug Information Tests (Full Output)

Validate complete debug information:

```java
final String normalized = BytecodeNormalizer.normalize(classBytes, true, true);
// or simply:
final String normalized = BytecodeNormalizer.normalize(classBytes);
```

**Includes**:
- LineNumberTable sections
- SourceDebugExtension (SMAP)
- All method signatures and instructions

### For Line Number Testing Only

Test LineNumberTable without SMAP:

```java
final String normalized = BytecodeNormalizer.normalize(classBytes, true, false);
```

## Debugging with Standard Java Debuggers

### Command-Line Debugging (jdb)

**Basic Setup**:

```bash
# Compile EK9 source to .class files
java -jar compiler-main/target/ek9c-jar-with-dependencies.jar -c path/to/program.ek9

# Navigate to generated bytecode directory
cd path/to/.ek9/generated/main/jvm

# Run with jdb
jdb -sourcepath ../../../../../../.. \
    -classpath /path/to/ek9-lang/target/classes:. \
    ek9.Main
```

**jdb Commands**:

```
stop in bytecode.test.HelloWorld._main  # Set breakpoint
run                                      # Start program
list                                     # Show source (maps to .ek9 via SMAP)
step                                     # Step through code
where                                    # Show stack trace
locals                                   # Show local variables
quit                                     # Exit debugger
```

**Debugging Programs with stdin Input**:

For programs that read from stdin (like PassThrough or StdinDebugTest), you can use stdin to control execution timing:

```bash
# Terminal 1: Start jdb and set breakpoint
cd examples/parseAndCompile/stdinDebugTest/.ek9/generated/main/jvm
echo "test input" | jdb \
  -sourcepath ../../../../../../.. \
  -classpath /path/to/ek9-lang/target/classes:. \
  ek9.Main

# In jdb session:
> stop in bytecode.debug.stdin.StdinDebugTest._main
> run
# Program prints "Ready for debugging. Enter text and press return:"
# Then waits for stdin input, giving you time to step through
> step
> locals
```

**Example StdinDebugTest Program**:

```ek9
#!ek9
defines module bytecode.debug.stdin

  defines program

    StdinDebugTest()
      stdin <- Stdin()
      stdout <- Stdout()

      stdout.println("Ready for debugging. Enter text and press return:")

      inputLine <- stdin.next()  ← Waits here for input, perfect for debugging!

      stdout.print("You entered: ")
      stdout.println(inputLine)

      result <- inputLine + " - processed"
      stdout.println(result)

//EOF
```

**Key Advantage**: The `stdin.next()` call blocks execution, giving you time to:
1. Set breakpoints after program starts
2. Step through code while it's waiting for input
3. Send input when ready to continue execution

**Note**: jdb has basic SMAP support but works best with IDE debuggers.

### IDE Debugging (IntelliJ IDEA, VSCode, Eclipse)

**Requirements**:
1. ✅ `.class` files with SourceDebugExtension attribute (implemented)
2. ✅ SMAP format correctly generated (implemented)
3. ⚠️ IDE plugin support for `.ek9` file mapping (future work)

**Current Status**: SMAP is correctly generated in bytecode. Full IDE debugging will require:
- EK9 IDE plugin/extension registration
- Debugger configuration to recognize `.ek9` files
- Source path configuration pointing to EK9 source directories

**Future Enhancement**: Language Server Protocol (LSP) integration with Debug Adapter Protocol (DAP) for comprehensive IDE debugging support.

## Common Issues and Solutions

### Issue 1: SMAP File IDs Converted to #CP

**Problem**: Regex `.replaceAll("#\\d+", "#CP")` converts SMAP file IDs like `88#1:88` to `88#CP:88`, breaking SMAP format.

**Solution**: Use negative lookahead to preserve file IDs:

```java
.replaceAll("#(\\d+)(?!:)", "#CP")
```

**Explanation**: `(?!:)` means "not followed by colon", so `#1:88` is preserved while `#7` (constant pool ref) becomes `#CP`.

### Issue 2: Circular Dependency with Directive Placement

**Problem**: Placing `@BYTECODE` directive after actual EK9 code creates circular dependency:
- Adding SMAP lines → shifts code → changes line numbers → requires directive update → repeat

**Solution**: Place directive **BEFORE** actual EK9 code:

```ek9
defines program
  @BYTECODE: ... `line 88: ...`

  HelloWorld()  ← Line 88 (stable!)
    ...
```

**Key Insight**: From Steve: "Ultrathink, I cant see why there would be a circular dependency - this seems fairly directed." The directive content describes the code that appears at fixed lines below it.

### Issue 3: Bytecode Tests Failing with Debug Info

**Problem**: Most bytecode tests focus on instruction sequences, not debug info. Including debug sections makes tests fragile (line numbers change with code edits).

**Solution**: Use configuration flags for selective filtering:

```java
// For most tests: minimal output
BytecodeNormalizer.normalize(classBytes, false, false);

// For debug-specific tests: full output
BytecodeNormalizer.normalize(classBytes, true, true);
```

**Pattern**: Similar to IR tests with `EK9_IR_NORMALIZE_INCLUDES` environment variable.

### Issue 4: javap Missing Debug Information

**Problem**: Basic `javap -c` doesn't show SourceDebugExtension.

**Solution**: Use verbose flag:

```bash
javap -v HelloWorld.class  # Shows SourceDebugExtension
```

**Flags Explanation**:
- `-c`: Disassemble bytecode
- `-l`: Show line number tables
- `-v`: Verbose (includes SourceDebugExtension, constant pool, etc.)
- `-p`: Show all members (private included)

## Technical Deep Dive

### DebugInfo Flow Through Compilation Phases

**Phase 0: PARSING**
- ANTLR4 captures line numbers, column positions, source file paths
- Attached to AST nodes as `IToken` information

**Phase 10: IR_GENERATION**
- `DebugInfo` objects created from AST token information
- Attached to IR instructions via `Optional<DebugInfo> getDebugInfo()`
- `DebugInfo` includes: `sourceFile()`, `lineNumber()`, `columnNumber()`, `isValidLocation()`

**Phase 15: CODE_GENERATION_AGGREGATES**
- `SmapGenerator` collects `DebugInfo` from all IR instructions
- Generates SMAP mapping source lines to bytecode lines
- ASM `ClassWriter.visitSource()` adds SourceDebugExtension attribute
- ASM `MethodVisitor.visitLineNumber()` adds LineNumberTable entries

**Key Classes**:
- `org.ek9lang.compiler.ir.support.DebugInfo` - IR debug information container
- `org.objectweb.asm.ClassWriter` - ASM class generation with debug support
- `org.objectweb.asm.MethodVisitor` - ASM method generation with line numbers

### SMAP vs LineNumberTable

Both are JVM debug mechanisms serving different purposes:

| Feature | LineNumberTable | SourceDebugExtension (SMAP) |
|---------|----------------|----------------------------|
| **Purpose** | Map bytecode offsets → source lines | Map source files → bytecode lines |
| **Scope** | Per-method attribute | Class-level attribute |
| **Format** | Binary (bytecode offset, line number pairs) | Text (JSR-45 SMAP format) |
| **Required For** | Basic Java debugging | Non-Java language debugging |
| **Maps** | Bytecode instruction offset → line | Source line → bytecode line |

**Both are needed**:
- **LineNumberTable**: Tells debugger which bytecode instruction corresponds to which line
- **SMAP**: Tells debugger which source file and line to display for that line number

**Example**:
```
LineNumberTable: "Bytecode offset 14 is at line 89"
SMAP: "Line 89 is line 89 in file ./helloWorld.ek9"
Result: Debugger shows helloWorld.ek9 line 89 when stopped at bytecode offset 14
```

### Performance Considerations

**SMAP Generation Cost**: Minimal
- One-time traversal of IR construct during class initialization
- String building and file ID mapping
- ~O(n) where n = number of IR instructions with debug info

**Class File Size Impact**: Small
- SourceDebugExtension attribute adds ~500-2000 bytes per class
- Proportional to number of unique source lines
- LineNumberTable adds ~8 bytes per line mapping

**Runtime Impact**: None
- Debug attributes ignored by JVM during execution
- Only loaded by debuggers when debugging session active
- No performance penalty for production bytecode with debug info

**Recommendation**: Always generate debug info in development builds. Consider stripping for production if class file size critical (rare).

## Future Enhancements

### 1. Language Server Protocol (LSP) Integration

**Current Status**: LSP implementation exists but needs DAP integration

**Planned Enhancement**: Full Debug Adapter Protocol (DAP) support in EK9 LSP server

**Benefits**:
- IDE-native debugging experience
- Breakpoint setting in `.ek9` files
- Variable inspection with EK9 type system awareness
- Step through EK9 source (not bytecode)

**Reference**: See `EK9_Compiler_Architecture_and_Design.md` section on LSP integration

### 2. Multi-File SMAP Support

**Current Implementation**: Single file mapping per construct

**Future**: Support for constructs spanning multiple `.ek9` files
- Include/import directives
- Multi-file modules
- Generic type instantiations from different files

**SMAP Format Supports**: Multiple file IDs and complex line ranges

### 3. Optimized SMAP Generation

**Current**: 1:1 line mapping (each IR instruction → one SMAP entry)

**Future**: Line range compression using SMAP's line range syntax:
```
InputStartLine#FileID,InputLineCount:OutputStartLine,OutputLineIncrement
```

**Example**:
```
# Current (6 entries):
10#1:10
11#1:11
12#1:12
13#1:13
14#1:14
15#1:15

# Compressed (1 entry):
10#1,6:10,1
```

**Benefit**: Smaller SourceDebugExtension attribute (reduced class file size)

### 4. Source Path Embedding

**Current**: SMAP uses relative paths (`./helloWorld.ek9`)

**Future**: Option to embed absolute source paths for better debugger integration:
```
*F
+ 1 helloWorld.ek9
  /absolute/path/to/helloWorld.ek9
```

**Benefit**: Debuggers can locate source files without manual source path configuration

### 5. Inlining and Optimization Support

**Challenge**: Bytecode optimizations (inlining, dead code elimination) break simple line mappings

**Future**: Enhanced SMAP with "output line increment" to track inlined code:
```
50#1:100,3  ← Line 50 inlined at lines 100-102
60#2:103    ← Different file inlined after
```

**Requires**: IR optimization pass tracking and propagation of debug info through transformations

## Best Practices

### When Writing EK9 Tests with @BYTECODE Directives

1. **Always place directive BEFORE actual code** (inside construct definition)
2. **Use fixed line numbers** - calculate where actual code will appear
3. **Update SMAP and LineNumberTable together** when editing
4. **Verify with javap** - check generated class includes correct SMAP
5. **Use BytecodeNormalizer configuration** - filter debug info for instruction-focused tests

### When Debugging EK9 Code

1. **Compile with debug info** (default in EK9 compiler)
2. **Use verbose javap** to verify SMAP present: `javap -v Class.class`
3. **Set sourcepath for debugger** to locate `.ek9` files
4. **Prefer IDE debuggers over jdb** for better SMAP support
5. **Check SMAP file IDs** if debugging shows wrong source file

### When Modifying Code Generation

1. **Preserve DebugInfo through transformations** in IR passes
2. **Call MethodVisitor.visitLineNumber()** when generating bytecode
3. **Update SMAP if changing class structure** (new methods, inlining, etc.)
4. **Test with BytecodeNormalizerTest** to ensure no regression
5. **Verify HelloWorld test passes** for end-to-end validation

## References

### Specifications
- [JSR-45: Debugging Support for Other Languages](https://jakarta.ee/specifications/debugging/2.0/)
- [JVM Specification - SourceDebugExtension Attribute](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-4.html#jvms-4.7.11)
- [JVM Specification - LineNumberTable Attribute](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-4.html#jvms-4.7.12)

### EK9 Documentation
- `EK9_Compiler_Architecture_and_Design.md` - Overall compiler architecture
- `EK9_IR_AND_CODE_GENERATION.md` - IR structure and code generation process
- `EK9_COMPILER_PHASES.md` - Detailed phase-by-phase compilation pipeline

### Related Code
- `compiler-main/src/main/java/org/ek9lang/compiler/backend/jvm/SmapGenerator.java`
- `compiler-main/src/main/java/org/ek9lang/compiler/backend/jvm/AsmStructureCreator.java`
- `compiler-main/src/main/java/org/ek9lang/compiler/backend/jvm/BytecodeNormalizer.java`
- `compiler-main/src/main/java/org/ek9lang/compiler/ir/support/DebugInfo.java`
- `compiler-main/src/test/java/org/ek9lang/compiler/backend/jvm/BytecodeNormalizerTest.java`
- `compiler-main/src/test/resources/examples/bytecodeGeneration/helloWorld/helloWorld.ek9`

## Summary

The EK9 compiler now includes comprehensive debug information support:

✅ **Complete Implementation**:
- Debug info flows from EK9 source → IR → JVM bytecode
- JSR-45 SMAP generation for non-Java language debugging
- LineNumberTable generation for bytecode offset mapping
- BytecodeNormalizer with configurable debug info filtering
- Comprehensive test coverage (BytecodeNormalizer + HelloWorld integration)

✅ **Verified Functionality**:
- SMAP correctly generated and embedded in .class files
- javap displays SourceDebugExtension with correct format
- File IDs preserved during bytecode normalization
- Tests pass with both minimal and full debug output modes

🚀 **Ready For**:
- IDE debugger integration (requires plugin development)
- Language Server Protocol + Debug Adapter Protocol implementation
- Production use with debuggable EK9 bytecode

🔮 **Future Enhancements**:
- LSP/DAP integration for IDE-native debugging
- Multi-file SMAP support for complex constructs
- SMAP compression for smaller class files
- Optimization-aware debug info tracking

---

*Last Updated: 2025-10-06*
*Implemented By: Claude Code in collaboration with Steve*
*Status: Complete and Production-Ready*
