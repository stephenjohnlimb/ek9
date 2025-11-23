# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Documentation-First Workflow

**CRITICAL**: These MD documents are Claude's longer-term memory. Given Claude's limited context window compared to human memory, these documents must be actively used as the primary knowledge base.

### Documentation Reading Requirements

**Before any technical analysis or implementation:**

1. **Always read relevant documentation COMPLETELY** when asked to "refresh," "review," or work on specific areas
2. **Understand principles before applying them** - don't jump to conclusions without proper foundation
3. **Use documentation systematically** - these documents contain hard-won knowledge and patterns
4. **Reference line numbers and specific sections** when citing documented patterns

**When Steve asks to "refresh yourself" or "review" documentation:**
- Read the ENTIRE relevant document thoroughly
- Take mental notes on key principles and patterns
- Fully internalize concepts before proceeding with any analysis
- Apply documented understanding systematically to technical work

**Example**: For IR generation work, always reference `EK9_IR_AND_CODE_GENERATION.md` memory management rules (lines 350-450) and variable declaration patterns before evaluating any IR code.

### Mandatory Process for EK9 Directive and Test File Work

**CRITICAL WORKFLOW**: When creating/modifying EK9 test files with directives (@IR, @BYTECODE, @Resolved, @Error, @Complexity):

**🛑 STOP - DO NOT IMPLEMENT IMMEDIATELY 🛑**

1. Check documentation first (grep for directive examples)
2. Find 2-3 working examples (MANDATORY)
3. Copy structure from working example
4. Modify content only - NEVER change format
5. Validate against pattern

**Why mandatory:** Steve has created hundreds of test files following identical patterns. Random format variations break test infrastructure and require 30+ min debugging per file. Following the pattern works immediately (5 min per file).

**Complete process with examples and anti-patterns:** See **`EK9_DIRECTIVE_TEST_FILE_PROCESS.md`**

## Project Overview

EK9 is a new programming language implementation with a comprehensive compiler written in Java 25. The project consists of a multi-pass compiler that transforms EK9 source code (`.ek9` files) into various target formats, primarily Java bytecode.

## Development Strategy

### Dual-Backend Architecture

EK9 is being developed with **two parallel code generation backends** to maximize market reach and performance capabilities:

1. **JVM Backend** (Primary) - Java bytecode generation for enterprise compatibility
2. **LLVM Native Backend** - Native compilation for performance-critical and embedded systems

### Branch and Development Organization

**Branch Strategy**:
- **`main` branch**: JVM bytecode generation development (this repository)
- **`ek9llvm` branch**: LLVM native backend development (`../ek9llvm/llvm-native/`)

**Parallel Development Approach**:
- Two Claude Code instances work in parallel on separate branches
- Each Claude focuses on their respective backend while sharing the common IR foundation
- Convergence after both backends achieve feature completeness

### Current Development Priorities

**Priority 1: Complete IR Generation (Phase 10)**
- **Goal**: Comprehensive, correct IR generation for ALL EK9 language constructs
- **Rationale**: IR is the common foundation for both JVM and LLVM backends
- **Focus Areas**:
  - All control flow statements (if/else, while, for, switch, etc.)
  - All EK9 operators and expressions
  - Function/method calls and closures
  - Generic type instantiation
  - Exception handling
  - Pattern matching and guards

**Priority 2: Correct Code Generation**
- **JVM Backend**: Java bytecode generation for all IR constructs
- **LLVM Backend**: LLVM IR generation bridging to C runtime
- **Principle**: Correctness before optimization

**Deferred: IR Optimization (Phase 12)**
- Current status: Stub implementation
- Rationale: Optimization is valuable but secondary to complete, correct functionality
- Timeline: After both backends achieve feature completeness

### LLVM Native Backend Status

**Production-Ready Components** (as of 2025-10-08):
- ✅ **C Runtime**: 14,068 lines of production C code implementing 35 built-in types
- ✅ **Memory Management**: Swift-inspired ARC (Automatic Reference Counting)
- ✅ **Name Mangling**: 100% Java/C hash consistency (50/50 validated)
- ✅ **Cycle Detection**: Complete infrastructure (gc_color, gc_next fields)
- ✅ **VTable Dispatch**: Polymorphic method resolution

**Critical Path Component**:
- 🔨 **LLVM IR Generator**: Bridge from EK9 IR → LLVM IR → C runtime (in development)

**Strategic Importance**:
The LLVM native backend enables EK9 to compete with Rust/C++ on performance while maintaining the safety and simplicity advantages. This dual-backend approach is critical for the "so much better than anything else" positioning required for adoption.

### Why Correctness Precedes Optimization

1. **Foundation First**: Incomplete IR generation blocks both backends
2. **Testing Confidence**: Correct behavior enables comprehensive testing
3. **User Trust**: Working features are more valuable than optimized incomplete features
4. **Parallel Progress**: Both backends can advance simultaneously once IR is complete

### Convergence Strategy

Once both backends achieve feature completeness:
1. Merge optimization work from both branches
2. Unified performance benchmarking
3. Cross-backend validation (same EK9 code → same behavior)
4. Preparation for production release

## Architecture Documentation

### Comprehensive Technical References
For detailed understanding of the EK9 compiler architecture and implementation:

- **`EK9_Compiler_Architecture_and_Design.md`** - Complete 85-page technical specification covering:
  - Multi-phase compilation pipeline (detailed analysis of each phase)
  - Module structure and Maven dependencies (compiler development)
  - Symbol table and type system architecture
  - Bootstrap process and built-in type loading
  - Intermediate representation and code generation
  - Language Server Protocol integration
  - Performance considerations and threading
  - Implementation status and future roadmap

- **`architecture_diagrams.md`** - Visual architecture diagrams including:
  - Module dependency graphs
  - Multi-phase compilation pipeline flow
  - Symbol system class hierarchies
  - LSP integration architecture
  - Bootstrap process flow
  - Multi-target code generation
  - Generic type system architecture
  - Error handling and diagnostics flow

These documents provide comprehensive technical context for all EK9 compiler development work and should be referenced for understanding the complete architecture.

## Specialized Implementation Guides

For specific EK9 development tasks, refer to these comprehensive guides:

### EK9 Example Development and Learning
- **`EK9_CLAUDE_LEARNING_EXAMPLES.md`** - Claude Code learning experience and example development patterns
  - Test-driven learning approach with immediate compiler feedback
  - EK9 unique language features (native literals, tri-state semantics, assignment operators)
  - Successful example patterns and common pitfalls
  - Error-driven learning methodology for understanding EK9 compiler diagnostics
  - Template patterns for basic type testing and educational examples
  - **Use this guide when:** Creating EK9 examples, learning EK9 language features, or developing educational materials

### EK9 Generic Type Implementation
- **`EK9_GENERIC_TYPES_IMPLEMENTATION.md`** - Complete guide for implementing parameterized generic types
  - Decorated name generation process and patterns
  - Delegation pattern implementation with type parameterization
  - Single and two-parameter generic implementation examples
  - Integration patterns and comprehensive testing strategies
  - Critical lessons learned from complex implementations
  - **Use this guide when:** Implementing `Iterator<String>`, `Dict<K,V>`, `Optional<T>`, `List<String>`, etc.

- **`EK9_TYPE_SUBSTITUTION_ARCHITECTURE.md`** - TypeSubstitution system architecture and design
  - Complete explanation of how TypeSubstitution works for ALL parameterized types
  - "C header/library" pattern: separation of type system from code generation
  - Why Java-defined (extern) and user-defined types are treated identically during compilation
  - Phase-by-phase flow showing when TypeSubstitution populates methods
  - Parse tree traversal order guarantees for constructor resolution
  - SUBSTITUTED squirrelled data flag purpose and usage
  - Debugging TypeSubstitution issues and common failure modes
  - **Use this guide when:** Understanding how parameterized types work internally, debugging type resolution issues, or working on compiler phases 3-14

### EK9 Built-in Type Development  
- **`EK9_DEVELOPMENT_CONTEXT.md`** - Built-in type development patterns and conventions
  - EK9 type system architecture and inheritance patterns
  - Standard implementation patterns for constructors, operators, and validation
  - Comprehensive testing patterns and assertion helpers
  - Type-specific insights (Boolean, Integer, Float, String, Collections, etc.)
  - Common compilation issues and solutions
  - **Use this guide when:** Adding new EK9 built-in types like GUID, HMAC, FileSystemPath

### EK9 Built-in Type Analysis
- **`EK9_CONSTRUCTS_INTERFACE_ANALYSIS.md`** - Comprehensive analysis of EK9 built-in language constructs
  - Complete catalog of 76 interface constructs vs 77 implementations
  - Interface definitions from Ek9BuiltinLangSupplier.java analysis
  - Implementation status mapping for all construct types (classes, traits, functions, records)
  - Special type documentation (Any as built-in base type)
  - Gap analysis framework for method/operator completeness checking
  - Foundation for identifying missing implementations toward 107-symbol target
  - **Use this guide when:** Understanding EK9 construct completeness, analyzing missing methods/operators, or planning built-in type development

### EK9 Method Resolution and Matching
- **`EK9_METHOD_RESOLUTION_AND_MATCHING.md`** - Complete analysis of EK9's method resolution mechanism
  - Cost-based method matching algorithm with SymbolMatcher implementation
  - 'Any' type's critical role as universal base type with HIGH_COST (20.0) penalty
  - Ambiguity detection logic with 0.001 tolerance threshold
  - Method resolution priority hierarchy (exact → superclass → trait → coercion → Any)
  - Real-world examples including workarea.ek9 ambiguity scenarios
  - Integration with FULL_RESOLUTION compilation phase
  - Polymorphic method patterns enabling gradual typing
  - **Use this guide when:** Understanding method call resolution, debugging ambiguous method errors, implementing method overloading, or working with 'Any' type parameters

### EK9 Language Semantics
- **`EK9_OPERATOR_SEMANTICS.md`** - EK9 operator behavior and semantics documentation
  - Increment/decrement operator semantics (different from Java/C++)
  - Mutating vs non-mutating operator distinctions
  - Testing implications for operators that mutate objects
  - **Use this when:** Working with EK9 operators or debugging operator behavior

### Code Quality and Standards
- **`EK9_CODING_STANDARDS.md`** - Comprehensive coding standards and style guide
  - Formatting rules (2-space indentation, 120-char lines, no tabs)
  - Import organization and package structure guidelines
  - EK9-specific annotation formatting and operator naming conventions
  - Control flow standards (mandatory braces, final variables)
  - Complete examples of properly formatted EK9 classes and tests
  - IDE configuration instructions for automatic compliance
  - **Use this when:** Writing any Java code for the EK9 project to ensure consistency

### Architectural Design Philosophy
- **`EK9_FUNCTIONAL_DESIGN_PHILOSOPHY.md`** - Core architectural patterns used throughout EK9
  - Functional decomposition pattern with helper classes
  - Java functional interface selection and usage guidelines
  - Single responsibility principle applied systematically
  - Node* helper class pattern extensively used in compiler design
  - Standard library refactoring techniques and best practices
  - **Use this when:** Refactoring complex classes or understanding EK9's architectural approach

- **`EK9_PRAGMATIC_PURITY_ARCHITECTURE.md`** - Revolutionary purity model and backend optimization architecture
  - Three-tier purity system (enforcement, pragmatic I/O, controlled mutation)
  - PurityInfo IR enhancement with side effects classification
  - Backend optimization framework for LLVM/JVM targets
  - Next-generation computing vision (SIMD, GPU, quantum optimization)
  - Strategic positioning as "Pure Performance Language" vs Haskell/Rust/Java
  - **Use this when:** Understanding purity-based optimization, backend IR enhancement, or performance strategy

### Strategic Documentation (Reference Only)

**NOTE**: The following strategic and marketing documents are maintained separately and should be referenced only when needed for business planning, competitive analysis, or enterprise adoption strategies. For day-to-day EK9 compiler development, focus on the technical implementation guides above.

**Strategic Market Analysis**:
- **`EK9_COMPREHENSIVE_COMPETITIVE_ANALYSIS.md`** - Market positioning and competitive advantage framework
- **`EK9_ENTERPRISE_LANGUAGE_COMPARISON.md`** - Comparison with major programming languages
- **`EK9_CORPORATE_SPONSORSHIP_STRATEGY.md`** - Corporate partnership and commercialization strategy

**Enterprise Capabilities**:
- **`EK9_ENTERPRISE_DEVOPS_INTEGRATION.md`** - DevOps platform capabilities
- **`EK9_SUPPLY_CHAIN_SECURITY.md`** - Security architecture and compliance
- **`EK9_REVOLUTIONARY_ENTERPRISE_CAPABILITIES.md`** - Built-in enterprise features
- **`EK9_INTEGRATED_BUILD_SYSTEM.md`** - Dependency management system
- **`EK9_ENTERPRISE_ADOPTION_ROADMAP.md`** - Implementation strategies

**AI Collaboration** (Relevant for AI-assisted development):
- **`EK9_AI_DEVELOPMENT_PLATFORM.md`** - AI collaboration framework and guardrails
- **`EK9_AI_FRIENDLY_LANGUAGE_STRATEGY.md`** - AI-specific design patterns

**When to reference these documents**:
- Preparing business cases or sponsor presentations
- Enterprise adoption planning
- Competitive analysis and positioning
- Academic collaboration proposals
- Understanding EK9's strategic vision

**For compiler development work**, prioritize the technical implementation guides and architectural documentation sections above.

### Historical Context and Lessons
- **`EK9_SESSION_NOTES.md`** - Session-specific implementation notes and lessons learned
  - Detailed session notes from specific implementation challenges
  - Evolution of patterns and best practices
  - Common pitfalls and their solutions
  - Multi-module build process lessons
  - **Use this for:** Understanding past implementation challenges and proven solutions

### Compiler Implementation Guides
- **`EK9_COMPILER_PHASES.md`** - Detailed compiler phase implementation and pipeline
  - Multi-phase compilation pipeline specifics
  - Symbol table management across phases
  - Phase interdependencies and error handling
  - **Use this guide when:** Working on compiler phase implementation and pipeline optimization

- **`EK9_IR_AND_CODE_GENERATION.md`** - IR generation and multi-target code generation
  - Intermediate representation structure and patterns
  - Java bytecode generation and future LLVM support
  - Optimization passes and target-specific considerations
  - **Use this guide when:** Working on compiler backend, IR generation, or code generation

- **`EK9_CONTROL_FLOW_IR_DESIGN.md`** - Control flow IR architecture and guard integration
  - CONTROL_FLOW_CHAIN unified instruction for all control flow
  - Guard variable patterns (`<-`, `:=`, `:=?`, `?=`)
  - Guard entry check pattern (QUESTION_OPERATOR)
  - FOR-IN as WHILE_LOOP, FOR-RANGE as polymorphic dispatch
  - **Use this guide when:** Working on if/switch/while/for/try IR generation or bytecode

- **`EK9_DUAL_BACKEND_IR_ARCHITECTURE.md`** - How IR supports both JVM and LLVM backends
  - SSA compatibility analysis (SSA-ready without being pure SSA)
  - ARC memory management (RETAIN/RELEASE/SCOPE_* patterns)
  - Control flow bytecode generation patterns
  - Backend-specific implementation differences
  - **Use this guide when:** Understanding dual-backend design, SSA conversion, or ARC semantics

- **`EK9_IR_TO_LLVM_MAPPING.md`** - Specific EK9 IR to LLVM IR mapping patterns
  - LLVM runtime function declarations
  - PHI node generation at control flow merge points
  - Memory management runtime calls
  - **Use this guide when:** Implementing LLVM backend code generation

- **`EK9_BACKEND_IMPLEMENTATION_ARCHITECTURE.md`** - Program entry point and execution architecture
  - Universal entry point strategy (ek9.Main)
  - Program registry and command-line parsing
  - JVM and LLVM backend program execution patterns
  - **Use this guide when:** Working on program entry points or execution infrastructure

### Language and Examples
- **`EK9_LANGUAGE_EXAMPLES.md`** - Idiomatic EK9 source code examples and patterns
  - EK9 syntax and semantic demonstrations
  - Best practices and coding patterns
  - Real-world examples and migration guidance
  - **Use this guide when:** Writing EK9 code examples or demonstrating language features

## Build System and Common Commands

**Important Distinction**: 
- **EK9 Compiler Development**: Uses Maven (for developing the compiler itself)
- **EK9 Language Users**: Don't need Maven - EK9 provides integrated dependency management

### Compiler Development Build Commands
**Note: These are for developing the EK9 compiler itself**
- `mvn clean install` - Build the entire project and install to local repository
- `mvn clean compile` - Compile all modules
- `mvn test` - Run all unit tests (uses parallel execution with 8 threads)
- `mvn clean` - Clean all build artifacts

### EK9 User Commands
**Note: EK9 users work with `.ek9` files and don't need Maven/Gradle**
After building the compiler, EK9 provides these commands:
- `java -jar compiler-main/target/ek9c-jar-with-dependencies.jar -c <file.ek9>` - Compile EK9 source
- `java -jar compiler-main/target/ek9c-jar-with-dependencies.jar -ls` - Start Language Server mode
- `java -jar compiler-main/target/ek9c-jar-with-dependencies.jar -t <file.ek9>` - Run tests
- `java -jar compiler-main/target/ek9c-jar-with-dependencies.jar -d <file.ek9>` - Debug mode

### Native Binary (GraalVM)
- `native-image --no-fallback -jar ek9c-jar-with-dependencies.jar` - Create native binary
- `./ek9` - Run native binary directly

### Testing
- `mvn test -Dtest=ExamplesBasicsTest` - Run specific test class
- `mvn test -pl compiler-main` - Run tests for specific module

### JUnit 5 Parameterized Testing
- **`EK9_JUNIT_PARAMETERIZED_TESTING_PATTERNS.md`** - Best practices for JUnit 5 parameterized tests
  - Direct parameter provision vs wrapper objects
  - Fluent API patterns for test data creation
  - Common anti-patterns and elegant solutions
  - When to use/avoid parameterized tests
  - Static method data provider guidelines
  - **Use this guide when:** Refactoring repetitive tests or creating parameterized test suites

### MCP Integration and LSP Testing
- **`MCP_EK9_LSP_TESTING_GUIDE.md`** - Complete guide for testing EK9 files using Model Context Protocol with EK9 LSP
  - MCP-EK9 server setup and configuration (`mcp-server/` directory structure)
  - EK9 compiler integration in Language Server Protocol mode (`-ls` flag)
  - Step-by-step testing procedures with exact commands
  - Debug output analysis and LSP message flow verification
  - Known limitations with diagnostic generation in LSP mode
  - Complete restoration process for working MCP-LSP integration
  - **Use this guide when:** Setting up MCP integration, testing EK9 LSP functionality, or debugging MCP-EK9 communication

## Module Structure

### compiler-main
The core compiler implementation containing:
- **CLI commands** (`org.ek9lang.cli`) - All command-line tools and entry points
- **Compiler core** (`org.ek9lang.compiler`) - Multi-pass compiler engine with multi-phase pipeline
- **LSP server** (`org.ek9lang.lsp`) - Language Server Protocol implementation

### compiler-tooling
Tooling support for EK9 constructs and language features.

### java-introspection
Java reflection and introspection utilities for bootstrap and external library integration.

### ek9-lang
Core EK9 language runtime and built-in types.

## Compiler Architecture

The EK9 compiler follows a **multi-phase compilation pipeline** (currently 20 phases, subject to evolution):

### Frontend (Phases 0-9)
0. **PARSING** - ANTLR4-based parsing
1. **SYMBOL_DEFINITION** - Symbol table creation
2. **DUPLICATION_CHECK** - Duplicate detection
3. **REFERENCE_CHECKS** - Reference validation
4. **EXPLICIT_TYPE_SYMBOL_DEFINITION** - Type resolution
5. **TYPE_HIERARCHY_CHECKS** - Inheritance validation
6. **FULL_RESOLUTION** - Template and generic resolution
7. **POST_RESOLUTION_CHECKS** - Symbol validation
8. **PRE_IR_CHECKS** - Code flow analysis
9. **PLUGIN_RESOLUTION** - Plugin resolution

### Middle-end (Phases 10-12)
10. **IR_GENERATION** - Intermediate representation generation
11. **IR_ANALYSIS** - IR analysis and validation
12. **IR_OPTIMISATION** - IR-level optimizations

### Backend (Phases 13-19)
13. **CODE_GENERATION_PREPARATION** - Code generation preparation
14. **CODE_GENERATION_AGGREGATES** - Generate code for aggregates
15. **CODE_GENERATION_CONSTANTS** - Generate code for constants
16. **CODE_OPTIMISATION** - Target code optimizations
17. **PLUGIN_LINKAGE** - Link external plugins
18. **APPLICATION_PACKAGING** - Application packaging
19. **PACKAGING_POST_PROCESSING** - Completing post processing

*Note: Phase structure may evolve during development*

### Key Classes
- `Ek9Compiler` - Main compiler orchestrator
- `CompilerPhase` - Base class for all compilation phases
- `Workspace` - Manages compilation units and source files
- `CompilableProgram` - Central data structure for compilation

## Development Guidelines

### EK9 Operator Method Names
**CRITICAL**: For correct EK9 operator implementation, always reference the definitive mapping at:
- **`compiler-main/src/main/java/org/ek9lang/compiler/common/OperatorMap.java`**

Key operator mappings include:
- `"<>"` → `_neq` (not `_ne`)
- `"#?"` → `_hashcode` (lowercase 'c')
- `"=="` → `_eq`
- `"<=>"` → `_cmp`
- `":=:"` → `_copy`
- `":^:"` → `_replace`
- `":~:"` → `_merge`
- `"?"` → `_isSet`
- `"$"` → `_string`
- `"#^"` → `_promote`

All method names are consistent between OperatorMap and actual implementations.

### EK9 Control Flow Philosophy: Designed Exclusions

**CRITICAL**: EK9 deliberately EXCLUDES break/continue/return/fallthrough based on 50+ years of production bug evidence. These are NOT missing features - they were **designed out of existence**.

**What EK9 Does NOT Have (By Design):**
- ❌ NO `break` statement
- ❌ NO `continue` statement
- ❌ NO `return` statement
- ❌ NO switch fallthrough

**Grammar Evidence:** Review `EK9.g4` - these keywords don't exist in EK9's formal grammar.

**Why Removed:**
- Microsoft Study (2011): 15% of production bugs involved break/continue
- Apple SSL Bug (2014): `goto fail` bypassed SSL validation, affected millions
- Linux Kernel: 200+ CVE fixes for "break in wrong loop" bugs
- CERT: Switch fallthrough ranked #7 most dangerous coding error

**EK9's Superior Alternatives:**
- **Stream pipelines** replace break/continue: `cat items | filter by matches | head`
- **Guard expressions** (`<-`, `:=?`) replace early returns
- **Multiple case values** replace switch fallthrough: `case MONDAY, TUESDAY, WEDNESDAY`
- **Return value declarations** replace return statements: compiler enforces ALL paths initialize

**Impact:**
- Eliminate 15-25% of production bugs (Microsoft/Google data)
- No Apple SSL-style bypass vulnerabilities
- Modern languages moving this direction - EK9 is ahead

**Complete explanation:** See **`EK9_CONTROL_FLOW_PHILOSOPHY.md`** for bug evidence, detailed alternatives, and migration patterns.

**When in doubt:** These features do not exist in EK9. Do not try to use them. They were deliberately removed based on 50 years of production evidence.

### EK9 Tri-State Semantics

**CRITICAL**: EK9 implements tri-state object model (absent/unset/set).

**The Three States:**
1. **Object Absent** - Object doesn't exist (e.g., missing key in Dict)
2. **Object Present but Unset** - Object exists but has no meaningful value
3. **Object Present and Set** - Object exists with valid, usable value

**Type-Specific Semantics:**
- **Primitives** (String, Integer, Boolean): Can be unset, `_isSet()` returns false for unset
- **Collections** (List, Dict): **Always set when created**, even if empty (`new List()` → set with 0 items)
- **Containers** (DictEntry, Optional): Complex tri-state logic based on contained values
- **Never accept Java `null`** - always reject null to maintain type safety

**Key Principle:** The `_isSet()` method defines "meaningful, normal, usable value" differently for each type. Collections are always meaningful when created (empty is valid). Primitives require actual data to be "set".

**Complete explanation:** See **`EK9_TRI_STATE_SEMANTICS.md`** for implementation patterns, testing guidelines, and JSON integration.

### EK9 Assignment Operators and Guard System

**CRITICAL**: Three distinct assignment operators with precise semantics:

| Operator | Name | Semantics | Use Case |
|----------|------|-----------|----------|
| `<-` | Declaration | Create NEW variable + first assignment | First-time assignment |
| `:=` | Assignment | Assign to EXISTING variable | Reassignment |
| `:=?` | Guarded Assignment | Only assign if variable is UNSET | Conditional initialization |

**Guard Variables in Control Flow:**

Guards combine assignment with null/isSet checking. Same syntax works identically in IF, SWITCH, FOR, WHILE, and TRY:

```ek9
if name <- getName()          // Only execute if SET
  stdout.println(name)

switch record <- database.getRecord(id)  // Only execute if SET
  case .type == "USER"
    processUser(record)

for item <- iterator.next()   // Loop while SET
  process(item)

while conn <- getActiveConnection()  // Continue while SET
  transferData(conn)
```

**Revolutionary Impact:**
- **90-95% elimination** of null pointer exceptions through compile-time enforcement
- **Universal pattern** - one syntax across all control flow constructs
- **Cannot bypass** - compiler enforces safety, no escape hatches

**Complete explanation:** See **`EK9_ASSIGNMENT_OPERATORS_AND_GUARDS.md`** for detailed examples and patterns.

### Code Style
- Java 25 with virtual threads support
- Follow existing naming conventions (CamelCase for classes, camelCase for methods)
- All new code must include comprehensive unit tests
- Use parallel processing where possible for performance

### Testing
- Test files are located in `src/test/resources/examples/`
- Bad examples (should fail compilation) are in `src/test/resources/badExamples/`
- Use `ExamplesBasicsTest` pattern for testing language features
- For built-in type testing, follow patterns in `EK9_DEVELOPMENT_CONTEXT.md`

### EK9 Built-in Type Testing Best Practices

**Key Patterns:**
- **Always use try-with-resources** for streams and closeable resources
- **Test both SET and UNSET states** for all operations
- **Test all parameter combinations** including unset/null inputs
- **Test method overloads** separately (e.g., `createFile()` vs `createFile(Boolean)`)
- **Use `@Execution(SAME_THREAD)`** for file system operations
- **Create isolated temporary directories** per test
- **Use `File.separator`** for cross-platform path operations

**Example:**
```java
// Test with set values
assertTrue(setPath.exists().state);

// Test with unset values
assertUnset.accept(unsetPath.exists());

// Test with unset parameter
assertUnset.accept(path.startsWith(new FileSystemPath()));
```

**Complete guide:** See **`EK9_BUILT_IN_TYPE_TESTING.md`** for comprehensive coverage patterns, thread safety, and portable testing guidelines.

### EK9 Annotation Validation Process

**⚠️ CRITICAL MANDATORY PROCESS ⚠️**

When modifying ANY `@Ek9Class`, `@Ek9Constructor`, `@Ek9Method`, `@Ek9Operator` annotation in `ek9-lang` module:

```bash
# Complete validation sequence - run these commands in order:
mvn clean install -pl ek9-lang
mvn clean compile -pl compiler-main
mvn test -Dtest=Ek9IntrospectedBootStrapTest -pl compiler-main
```

**Why Required:**
- `compiler-main` depends on `ek9-lang`
- `Ek9IntrospectedBootStrapTest` uses reflection to find `@Ek9Class` annotated classes
- Annotations are converted to EK9 source code and parsed
- Parser catches annotation formatting errors

**Common Annotation Patterns:**

✅ **CORRECT - multi-line with proper newlines:**
```java
@Ek9Operator("""
    operator ? as pure
      <- rtn as Boolean?""")
```

❌ **INCORRECT - single line (missing newlines):**
```java
@Ek9Operator("operator ? as pure <- rtn as Boolean?")  // WRONG
```

**Complete process:** See **`EK9_ANNOTATION_VALIDATION.md`** for detailed validation steps, common errors, and solutions.

**FAILURE TO FOLLOW THIS PROCESS WILL BREAK THE EK9 COMPILER BOOTSTRAP**

### Collection Types Set/Unset Semantics
**CRITICAL**: Collection types (Dict, List, etc.) are **always set/valid** even when empty:
- `new Dict()` → **set** (empty dict with 0 items)
- `new List()` → **set** (empty list with 0 items) 
- Only explicit `unSet()` calls or invalid constructor arguments make collections unset
- Empty collections ≠ unset collections

### EK9 Source Files
- EK9 uses indentation-based syntax (similar to Python)
- Files must end with `.ek9` extension
- Use examples in `src/test/resources/examples/` as reference

## Language Server Integration

The compiler includes full LSP support:
- Start with `java -jar <jar> -ls`
- Integrates with VSCode extension for syntax highlighting
- Provides real-time compilation and error reporting

## Bootstrap Process

The compiler has a sophisticated bootstrap mechanism that loads built-in EK9 types and language constructs before compiling user code. This is handled by `Ek9LanguageBootStrap` and related classes.

## Target Architectures

Currently supports:
- Java bytecode generation (primary target)
- Planned: LLVM IR output for native compilation

## Common Development Tasks

### Adding New Language Features
1. Update ANTLR4 grammar in `compiler-main/src/main/antlr4/org/ek9lang/antlr/`
2. Add symbol definitions in appropriate compiler phase
3. Update IR generation and code generation phases
4. Add comprehensive tests in `examples/` directory

### Running Single Tests
Use the existing test infrastructure and examples directory structure for validation.

## Analysis and Planning Requirements

### Critical Analysis Process
**Before proposing any changes, additions, or improvements, ALWAYS:**

1. **Systematically map existing functionality** - Catalog what each existing test, method, or component already covers
2. **Identify genuine gaps** - Focus on what's actually missing rather than theoretical improvements
3. **Avoid redundancy** - Check thoroughly against existing coverage before proposing new tests or features
4. **Be specific about what's missing** - Don't use generic improvement checklists

### Example of Proper Analysis
When asked to improve test coverage:
- ✅ "The existing tests cover X, Y, and Z scenarios. Missing coverage includes A and B."
- ❌ "We should add tests for parameter validation" (when parameter validation is already tested)

**Key Lesson**: When Steve asks to "review" or "analyze" existing code, do thorough analysis before jumping to solutions.

## Personal Preferences
- **Always refer to Steve by name** (not "user" or "the user")
- Steve prefers direct, concise communication
- Focus on practical implementation details and patterns