# EK9 Documentation and Tooling TODO

This document outlines the required documentation, tooling, and infrastructure work needed to make EK9 ready for mainstream adoption. It covers gaps identified in the current HTML documentation and establishes a roadmap for completion.

## Table of Contents
1. [Critical Documentation Gaps](#critical-documentation-gaps)
2. [API Documentation Tooling](#api-documentation-tooling)
3. [User Onboarding Materials](#user-onboarding-materials)
4. [Advanced Topics](#advanced-topics)
5. [Tooling and IDE Support](#tooling-and-ide-support)
6. [Community and Ecosystem](#community-and-ecosystem)
7. [Implementation Priority](#implementation-priority)

---

## 1. Critical Documentation Gaps

### 1.1 Installation Guide
**Status:** Missing
**Priority:** CRITICAL

**Required Content:**
- **Platform-specific installation:**
  - macOS (Intel & Apple Silicon)
  - Linux (Ubuntu, Debian, Fedora, Arch)
  - Windows (native & WSL)
  - Docker images

- **Prerequisites:**
  - Java runtime requirements (version 21+)
  - PATH configuration
  - Environment variables (EK9_HOME, EK9_TARGET)
  - Optional dependencies

- **Installation methods:**
  - Binary downloads from repo.ek9lang.org
  - Package managers (homebrew, apt, yum, chocolatey)
  - Building from source
  - IDE plugin installation (VSCode, IntelliJ, Eclipse)

- **Verification:**
  - `ek9 -V` version check
  - Running first program
  - Troubleshooting common installation issues

**File to create:** `installation.html`

---

### 1.2 Quick Start Tutorial
**Status:** Missing
**Priority:** CRITICAL

**Required Content:**
- **Beyond Hello World:**
  - Step-by-step first program with explanation
  - Understanding the compilation process
  - Running programs with command-line arguments
  - Creating your first function
  - Working with basic types (String, Integer, List)

- **Common patterns:**
  - Reading user input (Stdin)
  - File I/O basics
  - Simple error handling
  - Basic flow control (if/for/switch)

- **Building incrementally:**
  - Adding a second source file
  - Using the package construct
  - Creating reusable functions
  - Introduction to records

**Files to create:**
- `quickstart.html` - Main tutorial
- `firstprogram.html` - Detailed walkthrough

---

### 1.3 Troubleshooting Guide
**Status:** Missing
**Priority:** HIGH

**Required Content:**
- **Common compilation errors:**
  - Indentation errors and fixes
  - Type mismatch errors
  - Unset variable usage
  - Module/namespace issues
  - Import/reference errors

- **Runtime errors:**
  - Exception handling patterns
  - Debugging unset values
  - Common NullPointerException equivalents (unset issues)

- **Build system issues:**
  - Dependency resolution failures
  - Version conflicts
  - Package not found errors
  - Incremental vs full compilation

- **IDE/LSP issues:**
  - Language server not starting
  - Syntax highlighting not working
  - Autocomplete issues

**File to create:** `troubleshooting.html`

---

### 1.4 Migration Guides
**Status:** Missing
**Priority:** HIGH

**Required Content:**
- **From Java to EK9:**
  - Syntax comparison table
  - Concept mapping (class → class, interface → trait)
  - Handling null → unset transition
  - Collections API differences
  - Exception handling differences
  - No static methods - alternatives
  - Package/import differences

- **From Python to EK9:**
  - Indentation similarities
  - Type system differences (dynamic → static)
  - Function vs method distinctions
  - Collections and iteration
  - Error handling patterns

- **From Kotlin to EK9:**
  - Null safety approaches
  - Extension functions → composition
  - Data classes → records
  - Coroutines → async pipelines
  - DSL capabilities comparison

- **From Scala to EK9:**
  - Functional programming features
  - Pattern matching → switch/dispatcher
  - Traits comparison
  - Generics/parametric polymorphism
  - Implicits → dependency injection

**Files to create:**
- `migration-java.html`
- `migration-python.html`
- `migration-kotlin.html`
- `migration-scala.html`

---

## 2. API Documentation Tooling

### 2.1 EK9Doc Tool (equivalent to Javadoc)
**Status:** Not started
**Priority:** CRITICAL

**Requirements:**

#### 2.1.1 Documentation Comment Format
Define standard format for EK9 documentation comments:

```ek9
/**
 * Brief description of the function/class/method.
 *
 * Detailed description can span multiple lines and include
 * examples and usage notes.
 *
 * @param paramName Description of the parameter
 * @param anotherParam Description of another parameter
 * @returns Description of what is returned
 * @throws ExceptionType When this exception might be thrown
 * @example
 *   //Example usage
 *   result <- someFunction(value1, value2)
 * @see RelatedFunction
 * @since 1.0.0
 * @deprecated Use alternativeFunction instead
 */
```

#### 2.1.2 Extractable Elements
- Module documentation
- Constant definitions
- Type/Enumeration definitions
- Record fields and operators
- Function signatures and documentation
- Class properties, methods, and operators
- Trait method signatures
- Component definitions
- Service endpoints
- Text/Properties definitions

#### 2.1.3 Output Formats
- **HTML output** (similar to Javadoc)
  - Index pages by module
  - Cross-references between constructs
  - Search functionality
  - Inheritance diagrams for classes
  - Trait implementation overview

- **Markdown output** (for GitHub/documentation sites)

- **JSON output** (for IDE autocomplete/LSP)

#### 2.1.4 EK9Doc Command Line Interface
```bash
# Generate HTML docs for a module
ek9doc -f html -o docs/ mymodule.ek9

# Generate docs for entire package
ek9doc -f html -o docs/ -r .

# Include private members
ek9doc -f html -o docs/ -private mymodule.ek9

# Generate JSON for IDE integration
ek9doc -f json -o api.json mymodule.ek9
```

#### 2.1.5 Integration Points
- **Build integration:** Generate docs during `-P` packaging
- **IDE integration:** Provide hover documentation in LSP
- **Repository integration:** Auto-publish to repo.ek9lang.org
- **CI/CD integration:** Automated doc generation in pipelines

#### 2.1.6 Implementation Tasks
1. **Parser for doc comments** - Extract /** */ style comments
2. **AST integration** - Link comments to language constructs
3. **Template engine** - Generate HTML from templates
4. **Cross-reference resolver** - Link @see references
5. **Search indexer** - Create searchable documentation
6. **Style customization** - Allow custom CSS/themes
7. **Incremental generation** - Only regenerate changed modules

**Estimated effort:** 4-6 weeks for MVP, 12+ weeks for full feature parity with Javadoc

---

### 2.2 Built-in Type API Reference
**Status:** Partial (exists in source but not extracted)
**Priority:** HIGH

**Required Content:**
- **Systematic extraction from ek9-lang module:**
  - All @Ek9Class annotated classes
  - All @Ek9Method annotated methods
  - All @Ek9Operator annotated operators
  - All @Ek9Constructor annotated constructors

- **Generated documentation should include:**
  - Type hierarchy (what it extends/implements)
  - All available operators with signatures
  - All methods with parameters and return types
  - Usage examples for common operations
  - Purity annotations (as pure)
  - Tri-state semantics (set/unset behavior)

- **Coverage needed for:**
  - Any, Boolean, Character, String, Integer, Float, Bits
  - Time, Date, DateTime, Duration, Millisecond
  - Money, Locale, Colour, Dimension, Path
  - JSON, RegEx
  - List, Dict, Iterator, Optional, Result, PriorityQueue
  - Mutex, Semaphore, Signal
  - Stdin, Stdout, Stderr
  - HTTPRequest, HTTPResponse, HTTPServer
  - NetworkProperties, UDPSocket, TCPSocket

**Implementation approach:**
Use EK9Doc tool to extract from Java source annotations → Generate comprehensive API reference

**File to create:** `api-reference.html` (generated from source)

---

### 2.3 Standard Library Documentation
**Status:** Minimal
**Priority:** HIGH

**Required Content:**
- **Core utilities documentation:**
  - File system operations (Path, File types)
  - Network utilities (beyond basic sockets)
  - Cryptographic functions (HMAC, hashing)
  - Encoding/decoding utilities
  - JSON/XML parsing and generation
  - Regular expression utilities

- **Collection algorithms:**
  - Sorting algorithms and comparators
  - Filtering and transformation patterns
  - Pipeline operations reference
  - Stream processing best practices

- **Concurrency utilities:**
  - Thread safety patterns
  - Async pipeline usage
  - Mutex and synchronization
  - Signal handling

**Files to create:**
- `stdlib-overview.html`
- `stdlib-collections.html`
- `stdlib-io.html`
- `stdlib-network.html`
- `stdlib-concurrency.html`

---

## 3. User Onboarding Materials

### 3.1 Interactive Tutorials
**Status:** Missing
**Priority:** MEDIUM

**Required Content:**
- **Tutorial series:**
  1. Variables and basic types
  2. Functions and pure functions
  3. Records and data structures
  4. Classes and methods
  5. Traits and composition
  6. Generics and templates
  7. Pipeline processing
  8. Error handling
  9. File I/O
  10. Web services

- **Format options:**
  - Web-based interactive examples (like Kotlin Koans)
  - Command-line tutorial mode (`ek9 --tutorial`)
  - Jupyter-style notebook integration

**Files to create:** `tutorials/` directory with individual HTML files

---

### 3.2 Cookbook / How-To Guides
**Status:** Missing
**Priority:** MEDIUM

**Required Content:**
- **Common tasks:**
  - "How to read a CSV file"
  - "How to parse JSON"
  - "How to make HTTP requests"
  - "How to build a REST API"
  - "How to create a CLI tool"
  - "How to work with databases" (even though no ORM)
  - "How to handle configuration"
  - "How to write testable code"
  - "How to package for distribution"
  - "How to use dependency injection"

- **Pattern guides:**
  - "Builder pattern in EK9"
  - "Factory pattern with functions"
  - "Observer pattern with dynamic functions"
  - "Strategy pattern with function delegates"

**File to create:** `cookbook.html`

---

### 3.3 FAQ Section
**Status:** Missing
**Priority:** MEDIUM

**Required Content:**
- **Language design questions:**
  - Why no null keyword?
  - Why no break/return in loops?
  - Why no in-line lambdas?
  - Why indentation-based syntax?
  - Why operator overloading?

- **Practical questions:**
  - How do I handle optional values?
  - How do I iterate over collections?
  - How do I pass functions as parameters?
  - When should I use records vs classes?
  - How does error handling work?
  - What's the difference between := and = and :?

- **Tooling questions:**
  - Which IDE should I use?
  - How do I debug EK9 programs?
  - How do I profile performance?
  - Can I use EK9 with Docker?

**File to create:** `faq.html`

---

### 3.4 Glossary
**Status:** Missing
**Priority:** LOW

**Required Content:**
- **EK9-specific terms:**
  - Construct (program, function, class, etc.)
  - Unset (vs null)
  - Pure (function/method modifier)
  - Trait (vs interface)
  - Dynamic class/function
  - Pipeline processing
  - Dispatcher method
  - Coalescing operators
  - Fuzzy match
  - Constraint (in generics)

- **Cross-reference to documentation sections**

**File to create:** `glossary.html`

---

## 4. Advanced Topics

### 4.1 Performance and Optimization
**Status:** Missing
**Priority:** MEDIUM

**Required Content:**
- **Performance characteristics:**
  - Object allocation costs (no primitives)
  - Collection performance (List vs Dict vs Set)
  - Pipeline optimization (when compiled)
  - Function call overhead
  - Virtual dispatch costs

- **Optimization strategies:**
  - When to use pure functions
  - Avoiding unnecessary object creation
  - Efficient pipeline construction
  - Memory-conscious programming
  - Lazy evaluation techniques

- **Profiling and debugging:**
  - Performance profiling tools
  - Memory profiling
  - Identifying bottlenecks
  - JVM tuning for EK9 applications

**File to create:** `performance.html`

---

### 4.2 Testing Strategies
**Status:** Minimal
**Priority:** HIGH

**Required Content:**
- **Unit testing framework documentation:**
  - Test program structure (`/dev` directory)
  - Assertion helpers
  - Test organization patterns
  - Mocking and stubbing (if available)
  - Parameterized tests

- **Testing patterns:**
  - Testing pure functions
  - Testing classes with state
  - Testing web services
  - Testing asynchronous code
  - Testing pipeline operations
  - Testing with unset values

- **Integration testing:**
  - Component testing
  - Service testing
  - End-to-end testing

- **Test automation:**
  - Running tests with `-t` flag
  - CI/CD integration
  - Code coverage tools
  - Test reporting

**File to create:** `testing.html`

---

### 4.3 Concurrency Model
**Status:** Mentioned but not detailed
**Priority:** MEDIUM

**Required Content:**
- **Threading model:**
  - Virtual threads (Java 21+) integration
  - Thread-per-request model
  - Thread safety guarantees

- **Asynchronous programming:**
  - Async pipeline operations
  - Concurrent function execution
  - Future/Promise equivalents
  - Async web service handlers

- **Synchronization:**
  - Mutex usage patterns
  - Semaphore patterns
  - Lock-free programming
  - Signal handling

- **Common pitfalls:**
  - Race conditions
  - Deadlock prevention
  - Thread-safe collection usage

**File to create:** `concurrency.html`

---

### 4.4 Purity and Side Effects
**Status:** Mentioned, needs expansion
**Priority:** MEDIUM

**Required Content:**
- **Pure function semantics:**
  - What makes a function pure
  - Compiler enforcement of purity
  - Benefits of pure functions
  - When to use pure vs impure

- **Immutability patterns:**
  - Working with immutable data
  - Copy-on-write semantics
  - Persistent data structures
  - Efficient immutable updates

- **Side effect management:**
  - IO in pure contexts
  - Managing state changes
  - Effect tracking
  - Functional core, imperative shell pattern

**File to create:** `purity.html`

---

### 4.5 Memory Model
**Status:** Missing
**Priority:** LOW

**Required Content:**
- **Object lifecycle:**
  - Object creation and initialization
  - Set vs unset state
  - Garbage collection
  - Reference semantics

- **Memory management:**
  - Pass by reference behavior
  - Object copying patterns
  - Memory-efficient data structures
  - Large data handling

**File to create:** `memory-model.html`

---

## 5. Tooling and IDE Support

### 5.1 VSCode Extension Documentation
**Status:** Minimal
**Priority:** HIGH

**Required Content:**
- **Installation and setup:**
  - Extension installation from marketplace
  - EK9 compiler configuration
  - Workspace settings
  - Keyboard shortcuts

- **Features documentation:**
  - Syntax highlighting
  - Code completion
  - Go to definition
  - Find references
  - Code formatting
  - Error diagnostics
  - Debugging support (if available)

- **Troubleshooting:**
  - LSP connection issues
  - Extension not working
  - Performance issues

**File to create:** `ide-vscode.html`

---

### 5.2 IntelliJ IDEA Plugin (if planned)
**Status:** Not started
**Priority:** MEDIUM

**Required Content:**
- Plugin architecture
- Feature set
- Installation guide
- Configuration options

**File to create:** `ide-intellij.html`

---

### 5.3 Build Tool Integration
**Status:** Minimal
**Priority:** MEDIUM

**Required Content:**
- **CI/CD pipeline integration:**
  - GitHub Actions examples
  - GitLab CI examples
  - Jenkins integration
  - Azure DevOps integration

- **Docker integration:**
  - Dockerfile for EK9 applications
  - Multi-stage builds
  - Containerization best practices
  - Docker Compose examples

- **Build system integration:**
  - Using EK9 with Maven/Gradle (if needed)
  - Custom build scripts
  - Incremental compilation strategies

**File to create:** `build-integration.html`

---

### 5.4 Debugging Tools
**Status:** Mentioned, not documented
**Priority:** MEDIUM

**Required Content:**
- **EK9 debugger (edb) documentation:**
  - Installation and setup
  - Setting breakpoints
  - Step execution
  - Variable inspection
  - Stack traces

- **Remote debugging:**
  - Debugging server applications
  - Attaching to running processes
  - Debug port configuration

- **Debugging techniques:**
  - Debugging unset values
  - Debugging pipeline operations
  - Debugging async code
  - Debugging web services

**File to create:** `debugging.html`

---

## 6. Community and Ecosystem

### 6.1 Contribution Guide
**Status:** Missing
**Priority:** MEDIUM

**Required Content:**
- **How to contribute:**
  - Code contributions
  - Documentation improvements
  - Bug reports
  - Feature requests
  - Community support

- **Development setup:**
  - Building EK9 from source
  - Running tests
  - Code style guidelines
  - Commit message conventions

- **Pull request process:**
  - PR templates
  - Review process
  - Acceptance criteria

**File to create:** `CONTRIBUTING.md` (already exists?) + `contributing.html`

---

### 6.2 Package Repository Guide
**Status:** Partial
**Priority:** MEDIUM

**Required Content:**
- **Publishing packages:**
  - Creating a package
  - Version numbering
  - Package metadata
  - Signing and security
  - Publishing to repo.ek9lang.org

- **Finding and using packages:**
  - Searching the repository
  - Evaluating packages
  - Adding dependencies
  - Managing versions
  - Dealing with conflicts

- **Package best practices:**
  - Package structure
  - Documentation requirements
  - Testing requirements
  - Semantic versioning
  - Deprecation policies

**File to create:** `packages.html` (expand existing packaging.html)

---

### 6.3 Examples Repository
**Status:** Some examples exist in test resources
**Priority:** LOW

**Required Content:**
- **Curated examples:**
  - Full application examples
  - Microservice templates
  - CLI tool templates
  - Web application examples
  - Library examples

- **Code snippets:**
  - Common patterns
  - Idiomatic EK9 code
  - Anti-patterns to avoid

**Location:** Separate GitHub repository or `/examples` in main repo

---

## 7. Implementation Priority

### Phase 1: Critical (Weeks 1-8)
**Goal:** Enable developers to get started and be productive

1. **Installation Guide** (Week 1)
   - All platforms covered
   - Verification steps
   - Common issues

2. **Quick Start Tutorial** (Week 2)
   - Beyond hello world
   - First real program
   - Basic patterns

3. **EK9Doc Tool - MVP** (Weeks 3-6)
   - Doc comment parsing
   - HTML generation
   - Basic API extraction

4. **API Reference - Built-in Types** (Week 7)
   - Generate from annotations
   - Complete coverage
   - Usage examples

5. **Troubleshooting Guide** (Week 8)
   - Common errors
   - Solutions
   - Debugging tips

### Phase 2: High Priority (Weeks 9-16)
**Goal:** Enable advanced usage and migration

1. **Migration Guides** (Weeks 9-10)
   - Java to EK9
   - Python to EK9
   - Syntax comparisons

2. **Testing Documentation** (Week 11)
   - Framework docs
   - Patterns and practices
   - CI/CD integration

3. **Standard Library Docs** (Weeks 12-13)
   - Extract from source
   - Usage examples
   - Best practices

4. **VSCode Extension Docs** (Week 14)
   - Setup and features
   - Troubleshooting
   - Advanced usage

5. **Performance Guide** (Weeks 15-16)
   - Optimization strategies
   - Profiling tools
   - Best practices

### Phase 3: Medium Priority (Weeks 17-24)
**Goal:** Complete the ecosystem

1. **Cookbook/How-To Guides** (Weeks 17-18)
   - Common tasks
   - Pattern implementations
   - Real-world examples

2. **Concurrency Documentation** (Week 19)
   - Threading model
   - Async patterns
   - Synchronization

3. **Build Integration Guide** (Week 20)
   - CI/CD examples
   - Docker integration
   - Build tools

4. **Interactive Tutorials** (Weeks 21-22)
   - Web-based or CLI
   - Progressive learning
   - Hands-on exercises

5. **Debugging Documentation** (Weeks 23-24)
   - Debugger usage
   - Techniques
   - Remote debugging

### Phase 4: Nice to Have (Ongoing)
**Goal:** Polish and community growth

1. **FAQ Section**
   - Collect common questions
   - Provide clear answers

2. **Glossary**
   - EK9 terminology
   - Cross-references

3. **Advanced Topics**
   - Purity deep dive
   - Memory model
   - Compiler internals

4. **Community Resources**
   - Contribution guide
   - Package repository guide
   - Examples repository

---

## 8. Tooling Implementation Details

### 8.1 EK9Doc Architecture

```
ek9doc/
├── src/main/java/org/ek9lang/doc/
│   ├── parser/
│   │   ├── DocCommentParser.java        # Parse /** */ comments
│   │   ├── DocTagParser.java            # Parse @param, @returns, etc.
│   │   └── AnnotationExtractor.java     # Extract @Ek9* annotations
│   ├── model/
│   │   ├── DocumentedElement.java       # Base for all documented items
│   │   ├── DocumentedModule.java
│   │   ├── DocumentedClass.java
│   │   ├── DocumentedFunction.java
│   │   └── DocumentedMethod.java
│   ├── generator/
│   │   ├── HtmlGenerator.java           # Generate HTML output
│   │   ├── MarkdownGenerator.java       # Generate Markdown
│   │   ├── JsonGenerator.java           # Generate JSON for LSP
│   │   └── TemplateEngine.java          # Template processing
│   ├── linker/
│   │   ├── CrossReferenceLinker.java    # Link @see references
│   │   └── InheritanceLinker.java       # Build hierarchy
│   └── EK9DocMain.java                   # CLI entry point
└── src/main/resources/
    └── templates/
        ├── module-index.html
        ├── class-detail.html
        ├── function-detail.html
        └── search.html
```

### 8.2 LSP Integration for Documentation

The Language Server Protocol should provide:
- **Hover documentation:** Extract from doc comments
- **Signature help:** Show parameter documentation
- **Completion documentation:** Show method/function docs
- **Quick documentation:** F1 key or similar

Integration points:
```java
// In LSP server
public CompletableFuture<Hover> hover(HoverParams params) {
    // Extract documentation from EK9Doc model
    DocumentedElement element = findElementAt(params);
    return CompletableFuture.completedFuture(
        new Hover(createMarkdownDocumentation(element))
    );
}
```

### 8.3 Documentation in Build Process

Integrate into EK9 compiler phases:
```bash
# During packaging (-P flag)
ek9 -P myapp.ek9
# Should automatically:
# 1. Generate EK9Doc HTML
# 2. Package docs in artifact
# 3. Create docs/ directory in package
```

---

## 9. Documentation Quality Standards

### 9.1 Every Built-in Type Must Have:
- [ ] Overview description
- [ ] Constructor documentation
- [ ] All method signatures with parameters
- [ ] All operator implementations
- [ ] Set/unset behavior explanation
- [ ] Usage examples (minimum 3)
- [ ] Common pitfalls section
- [ ] Related types section

### 9.2 Every Language Construct Must Have:
- [ ] Syntax definition
- [ ] Use cases and when to use
- [ ] Complete working example
- [ ] Anti-patterns to avoid
- [ ] Comparison with similar constructs
- [ ] Advanced usage scenarios

### 9.3 Every How-To Guide Must Have:
- [ ] Clear problem statement
- [ ] Step-by-step solution
- [ ] Complete working code
- [ ] Explanation of key points
- [ ] Variations and alternatives
- [ ] Related topics links

---

## 10. Success Metrics

### Documentation Completeness
- [ ] 100% of built-in types documented via EK9Doc
- [ ] 100% of language constructs have examples
- [ ] All compiler flags documented
- [ ] All LSP features documented
- [ ] At least 50 how-to guides
- [ ] Migration guide for top 3 languages (Java, Python, Kotlin)

### User Onboarding
- [ ] Developer can install in <10 minutes
- [ ] Developer can run first program in <15 minutes
- [ ] Developer can find API documentation in <2 minutes
- [ ] Developer can troubleshoot common errors via docs

### Tooling Maturity
- [ ] EK9Doc generates javadoc-quality output
- [ ] LSP provides hover documentation
- [ ] IDE autocomplete shows documentation
- [ ] Search functionality works across all docs

### Community Readiness
- [ ] Contribution guide is clear
- [ ] Package publishing is documented
- [ ] Code examples repository exists
- [ ] FAQ covers 90% of common questions

---

## 11. Documentation Maintenance

### Ongoing Tasks
1. **Keep docs in sync with code:**
   - Update API docs when built-ins change
   - Update examples when syntax changes
   - Version documentation alongside releases

2. **Community contributions:**
   - Accept doc improvements via PR
   - Review and merge how-to guides
   - Curate user-contributed examples

3. **Quality assurance:**
   - Test all code examples
   - Verify links work
   - Check cross-references
   - Ensure search index is current

4. **Feedback loop:**
   - Monitor GitHub issues for doc questions
   - Track common support questions
   - Update FAQ based on real usage
   - Add troubleshooting based on bugs

---

## Conclusion

This TODO represents approximately **6 months of focused work** for a small documentation team:
- **2 developers** building EK9Doc tool (8 weeks)
- **2 technical writers** creating documentation (16 weeks)
- **1 developer** on tooling integration (ongoing)

**Quick wins for immediate impact:**
1. Installation guide (1 week)
2. Quick start tutorial (1 week)
3. Troubleshooting guide (1 week)
4. Basic EK9Doc tool (4 weeks)

This will get EK9 from "interesting language with good docs" to "production-ready platform with excellent documentation."
