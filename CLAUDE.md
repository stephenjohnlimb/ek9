# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EK9 is a new programming language implementation with a comprehensive compiler written in Java 23. The project consists of a multi-pass compiler that transforms EK9 source code (`.ek9` files) into various target formats, primarily Java bytecode.

## Build System and Common Commands

This is a Maven-based project with a multi-module structure. All commands should be run from the root directory.

### Core Build Commands
- `mvn clean install` - Build the entire project and install to local repository
- `mvn clean compile` - Compile all modules
- `mvn test` - Run all unit tests (uses parallel execution with 8 threads)
- `mvn clean` - Clean all build artifacts

### EK9 Compiler Commands
After building, the EK9 compiler provides these commands:
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

## Module Structure

### compiler-main
The core compiler implementation containing:
- **CLI commands** (`org.ek9lang.cli`) - All command-line tools and entry points
- **Compiler core** (`org.ek9lang.compiler`) - Multi-pass compiler engine with 12 phases
- **LSP server** (`org.ek9lang.lsp`) - Language Server Protocol implementation

### compiler-tooling
Tooling support for EK9 constructs and language features.

### java-introspection
Java reflection and introspection utilities for bootstrap and external library integration.

### ek9-lang
Core EK9 language runtime and built-in types.

## Compiler Architecture

The EK9 compiler follows a **12-phase compilation pipeline**:

### Frontend (Phases 0-3)
1. **PARSING** - ANTLR4-based parsing
2. **SYMBOL_DEFINITION** - Symbol table creation
3. **DUPLICATION_CHECK** - Duplicate detection
4. **REFERENCE_CHECKS** - Reference validation

### Middle-end (Phases 4-8)
5. **EXPLICIT_TYPE_SYMBOL_DEFINITION** - Type resolution
6. **TYPE_HIERARCHY_CHECKS** - Inheritance validation
7. **FULL_RESOLUTION** - Template and generic resolution
8. **POST_RESOLUTION_CHECKS** - Symbol validation
9. **PRE_IR_CHECKS** - Code flow analysis

### Backend (Phases 9-11)
10. **IR_GENERATION** - Intermediate representation generation
11. **CODE_GENERATION** - Target code generation
12. **PACKAGING** - Application packaging

### Key Classes
- `Ek9Compiler` - Main compiler orchestrator
- `CompilerPhase` - Base class for all compilation phases
- `Workspace` - Manages compilation units and source files
- `CompilableProgram` - Central data structure for compilation

## Development Guidelines

### Code Style
- Java 23 with virtual threads support
- Follow existing naming conventions (CamelCase for classes, camelCase for methods)
- All new code must include comprehensive unit tests
- Use parallel processing where possible for performance

### Testing
- Test files are located in `src/test/resources/examples/`
- Bad examples (should fail compilation) are in `src/test/resources/badExamples/`
- Use `ExamplesBasicsTest` pattern for testing language features

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