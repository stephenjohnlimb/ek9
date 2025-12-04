package org.ek9lang.lsp;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.ek9lang.compiler.tokenizer.TokenResult;

/**
 * Designed for us in the LSP only as a set of words that are part of the language.
 * But there is also some descriptive text that is provided with the work.
 * These can be used for code completion or for the hover functionality.
 * This might seem like a duplication of what ANTLR has generated, but we need to supply
 * much more information around each word. This is to really help developers get started
 * even very new developers.
 * In the future might make this functional optional as you get used to it, you might now
 * want the hover/help.
 * This only deals with simple single word completion and simple cases.
 */
final class Ek9LanguageWords {
  private final Map<String, KeyWordInformation> keywordMap = new HashMap<>();

  Ek9LanguageWords() {
    setupKeyWords();
  }

  List<String> getAllKeyWords() {

    return keywordMap.keySet().stream().sorted().toList();
  }

  private void setupKeyWords() {

    keywordMap.put("#!ek9",
        new KeyWordInformation(
            "SHEBANG: EK9 source file identifier (must be first line). Declares file as EK9 source code, enables direct execution on Unix/Linux systems. Required at start of every .ek9 file. Use when: creating any EK9 source file. Allows scripts to be executable (chmod +x file.ek9, then ./file.ek9). Similar to #!/usr/bin/env in shell scripts. https://ek9.io/structure.html",
            List.of(), TokenResult::previousTokensIndentsOrFirst));

    keywordMap.put("defines", new KeyWordInformation(
        "DEFINES BLOCK: Begin definition section for module constructs (functions, classes, records, traits, etc.). All type definitions must appear within a `defines` block inside a module. Use to organize and declare all constructs. Module can have multiple defines blocks for organization. Use when: declaring any functions, classes, records, components, services, applications. Required for all type definitions - cannot define constructs outside defines block. https://ek9.io/structure.html",
        List.of(),
        TokenResult::previousTokensIndentsOrFirst));

    //Just joining words
    keywordMap.put("with", new KeyWordInformation(
        "MULTI-PURPOSE LINKING KEYWORD (context-dependent): (1) STREAM pipeline mapping (map with function), (2) GUARD condition in control flow (if x <- y with x > 10), (3) TRAIT composition (with trait of BaseType), (4) APPLICATION composition (with application of AppName), (5) ASPECT application (with aspect of AspectName). Context determines meaning. https://ek9.io/flowControl.html#guard_operators",
        Arrays.asList("with", "with trait of", "with application of", "with aspect of"),
        search -> !search.previousTokenIsPipe()));

    keywordMap.put("as", new KeyWordInformation(
        "MULTI-PURPOSE KEYWORD (context-dependent): (1) Type declaration (variable as Integer), (2) Modifier (as pure, as abstract), (3) HTTP verb mapping (as GET for :/path), (4) Function/class modifier (as function, as class), (5) Trait/application composition syntax. Meaning determined by surrounding context. https://ek9.io/basics.html",
        Arrays.asList("as", "as\n", "as open\n", "as abstract\n", "as function\n", "as class\n",
            "as dispatcher\n", "as pure\n", "as pure abstract\n", "as pure function\n",
            "as pure abstract\n", "as pure dispatcher\n", "as GET for :/", "as DELETE for :/",
            "as HEAD for :/", "as POST for :/", "as PUT for :/", "as PATCH for :/",
            "as OPTIONS for :/"), search -> !search.previousTokenIsPipe()));

    keywordMap.put("of", new KeyWordInformation(
        "TYPE AND COMPOSITION LINKING KEYWORD (context-dependent): (1) GENERIC type parameters (List of String, Dict of String, Integer), (2) TRAIT composition (trait of BaseType), (3) APPLICATION composition (application of AppName), (4) ASPECT composition (aspect of AspectName), (5) STREAM operations (length of collection), (6) MATHEMATICAL operations (abs of value, sqrt of number). Context determines meaning.",
        Arrays.asList("of", "of type"),
        search -> !search.previousTokenIsPipe()));

    keywordMap.put("assert",
        new KeyWordInformation(
            "ASSERTION: Verify condition is true at runtime - throws exception if false. Use for preconditions, postconditions, invariants, and validation checks. Assertions help catch programming errors early during development and testing. Use when: validating assumptions, checking method contracts, verifying internal state. Exception thrown includes assertion details for debugging. Prefer assertions for programmer errors, not user input validation. https://ek9.io/basics.html#assertions",
            Arrays.asList("assert", "assert()"), TokenResult::previousTokensIndentsOrFirst));

    keywordMap.put("by", new KeyWordInformation(
        "MULTI-PURPOSE KEYWORD (context-dependent): (1) TYPE CONSTRAINT (type T constrain by SomeType), (2) LOOP INCREMENT (for i in 1..10 by 2), (3) TRAIT DELEGATION (delegate functionality by trait), (4) STREAM PIPELINE operations (filter by predicate, sort by comparator, group by key). Context determines meaning. https://ek9.io/streamsAndPipelines.html",
        List.of("by"),
        TokenResult::previousTokensIndentsOrFirst));

    keywordMap.put("is", new KeyWordInformation(
        "MULTI-PURPOSE KEYWORD (context-dependent): (1) TYPE CHECKING - Check if value is specific type (if item is String), (2) SET MEMBERSHIP - Check if value in collection (is in list, is not in set), (3) LOGICAL COMPARISON - Boolean equality/inequality (is, is not). Type checking enables runtime polymorphism. Set membership tests containment. Use when: need runtime type checking, testing collection membership, boolean comparisons. Common patterns: if value is SomeType, value is in collection, result is not empty. https://ek9.io/inheritance.html",
        Arrays.asList("is", "is not", "is in", "is not in"),
        getSearchNotIndentsAndNotPipe()));

    keywordMap.put("register", new KeyWordInformation(
        "REGISTER COMPONENT: Register component instance with dependency injection container for later injection. Components must be registered before they can be injected into other components/services. Use in program/application to bootstrap DI container with component instances. Use when: configuring dependency injection, setting up services, initializing application context. Register creates and wires component dependencies. Registered components become available for injection throughout application lifecycle. https://ek9.io/dependencyInjection.html",
        List.of("register"), TokenResult::previousTokensIndentsOrFirst));

    keywordMap.put("dispatcher", new KeyWordInformation(
        "DISPATCHER METHOD: Special method that routes calls to appropriate implementation based on actual runtime type of object. Enables dynamic method dispatch for polymorphic operations - the dispatcher examines the object's type at runtime and calls the correct method variant. Use when: need type-based method routing, handling multiple types with single entry point, visitor pattern implementation. Dispatcher automatically selects correct method based on actual object type, enabling clean polymorphic code without manual type checking. https://ek9.io/advancedClassMethods.html",
        List.of("dispatcher"), search -> !search.previousTokensIndentsOrFirst()));

    populateConstructs(keywordMap);
    populateModifiers(keywordMap);
    populateOperators(keywordMap);
    populateFlowControl(keywordMap);
    populateLoops(keywordMap);
    populateTryCatch(keywordMap);
    populateStreaming(keywordMap);
    populateWebServices(keywordMap);

  }

  private void populateConstructs(final Map<String, KeyWordInformation> keywordMap) {

    keywordMap.put("references",
        new KeyWordInformation(
            "REFERENCES: Import constructs from other modules for use in current module. Use to access external types, functions, or components. Syntax: `references com.example::ModuleName` imports all public constructs. Use when you need functionality from other modules. Enables modular code organization and reuse. https://ek9.io/structure.html#references",
            List.of("references\n"), TokenResult::previousTokensIndentsOrFirst));
    keywordMap.put("module",
        new KeyWordInformation(
            "MODULE: Primary code organization unit containing related constructs (functions, classes, records, etc.). Use to group logically related functionality. One module per .ek9 file. Syntax: `defines module ModuleName`. Modules are EK9's package/namespace equivalent. Use for organizing code into cohesive units. https://ek9.io/structure.html#module",
            List.of("module"), TokenResult::previousTokenIsDefines));
    keywordMap.put("constant",
        new KeyWordInformation(
            "CONSTANT: Immutable named value defined at compile-time. Use for configuration values, magic numbers, fixed strings that never change. Constants are evaluated at compile time, inlined where used. Use when: API keys (not actual secrets!), configuration defaults, mathematical constants (PI, E), fixed enumerations. NOT for runtime values. More efficient than variables. https://ek9.io/constants.html",
            List.of("constant\n"), TokenResult::previousTokenIsDefines));
    keywordMap.put("program",
        new KeyWordInformation(
            "PROGRAM: Executable entry point with main function. Use for command-line applications, scripts, batch processing. Programs define `_main()` method as entry point. Use when: building executables, CLI tools, standalone applications. Can use `program application` pattern for composing multiple programs. Compiles to runnable artifact. https://ek9.io/programs.html",
            Arrays.asList("program\n", "program application\n"), TokenResult::previousTokenIsDefines));
    keywordMap.put("type",
        new KeyWordInformation(
            "TYPE: Enumeration, type alias, or generic type parameter. Use for: (1) ENUMERATIONS - fixed set of named values (type Color: RED, GREEN, BLUE), (2) TYPE ALIASES - rename existing types for clarity, (3) GENERIC PARAMETERS - define parameterized types (type T constrain by SomeType). Use when: defining constants set, simplifying complex type names, creating generic types. https://ek9.io/enumerations.html",
            Arrays.asList("type", "type T", "type T constrain by"), search -> !search.previousTokenIsPipe()));
    keywordMap.put("function",
        new KeyWordInformation(
            "FUNCTION: Pure or stateful behavior unit (first-class citizens in EK9). Use for reusable logic, algorithms, transformations. Functions can be passed as parameters, returned from functions. Use when: pure calculations, stream operations, higher-order functions. Mark `as pure` for no side effects. Can be generic. More flexible than methods. https://ek9.io/functions.html",
            List.of("function"), TokenResult::previousTokenIsDefines));
    keywordMap.put("record",
        new KeyWordInformation(
            "RECORD: Immutable data structure for value types (DTOs, events, messages). Use for pure data without behavior. Records are inherently immutable - cannot be modified after creation. Use when: data transfer objects, event data, configuration, or any value type. Lighter than classes. NOT for: stateful objects, entities with lifecycle. https://ek9.io/records.html",
            List.of("record"), TokenResult::previousTokenIsDefines));
    keywordMap.put("class",
        new KeyWordInformation(
            "CLASS: Mutable object with state and behavior (methods, operators). Use for entities with lifecycle, business logic, or mutable state. Classes can extend other classes, implement traits. Use when: domain entities, services, mutable objects. NOT for: simple data (use record), DI beans (use component). https://ek9.io/classes.html",
            List.of("class"), search -> !search.previousTokenIsPipe()));
    keywordMap.put("trait",
        new KeyWordInformation(
            "TRAIT: Reusable behavior contract (interface + default implementations). Use for shared behavior across unrelated types, composition over inheritance. Classes/records/components can use multiple traits. Use when: defining common capabilities, mixins, protocol definitions. Traits can have abstract methods (must be implemented) and concrete methods (inherited). Alternative to multiple inheritance. https://ek9.io/traits.html",
            List.of("trait"), TokenResult::previousTokenIsDefines));
    keywordMap.put("package",
        new KeyWordInformation(
            "PACKAGE: Build and publishing configuration for distributing modules. Use to define package metadata (version, dependencies, licensing). Syntax: `defines package PackageName`. Use when preparing modules for distribution or consumption by other projects. EK9's built-in dependency management - no Maven/Gradle needed. https://ek9.io/packaging.html",
            List.of("package\n"), TokenResult::previousTokenIsDefines));
    keywordMap.put("text",
        new KeyWordInformation(
            "TEXT: Template construct for generating formatted text output (HTML, JSON, XML, etc.). Use for string interpolation, templating, internationalization. Text blocks support language selection (`text for language en`), property files integration. Use when: generating reports, HTML responses, formatted messages, i18n strings. Alternative to string concatenation for complex output. https://ek9.io/textProperties.html",
            List.of("text\n"), TokenResult::previousTokenIsDefines));
    keywordMap.put("component",
        new KeyWordInformation(
            "COMPONENT: Dependency injection bean with lifecycle management. Use for services that need injection (database connections, repositories, business services). Components are singletons by default, managed by EK9 DI container. Use when: injectable services, managed lifecycle, cross-cutting concerns. NOT for: domain entities (use class), simple data (use record). Requires `register` for instantiation. https://ek9.io/components.html",
            List.of("component\n"), TokenResult::previousTokenIsDefines));
    keywordMap.put("application",
        new KeyWordInformation(
            "APPLICATION: Reusable application configuration/template. Use to define common application patterns (web apps, batch processors, microservices). Applications can be composed with programs (`program application`). Use when: creating reusable app structures, defining application archetypes, building frameworks. Applications are templates - programs provide concrete entry points. https://ek9.io/structure.html#application",
            Arrays.asList("application\n", "application of"), search -> !search.previousTokenIsPipe()));
    keywordMap.put("service",
        new KeyWordInformation(
            "SERVICE: HTTP web service with REST endpoints. Use to define HTTP APIs with `GET`, `POST`, `PUT`, `DELETE`, etc. Services map URIs to functions (e.g., `GET for :/users/:id`). Use when: building REST APIs, microservices, web backends. Services can use `service application` pattern. Built-in HTTP server - no external container needed. https://ek9.io/webServices.html",
            Arrays.asList("service", "service application"), TokenResult::previousTokenIsDefines));

  }

  private void populateModifiers(final Map<String, KeyWordInformation> keywordMap) {

    keywordMap.put("override",
        new KeyWordInformation(
            "OVERRIDE: Explicitly mark method/operator as overriding parent implementation. REQUIRED when overriding - compiler enforces this to prevent accidental overrides. Use on methods in classes that extend other classes or use traits. Ensures you're actually overriding (not creating new method). Prevents breakage if parent signature changes. https://ek9.io/methods.html",
            List.of("override"), TokenResult::previousTokensIndentsOrFirst));
    keywordMap.put("public",
        new KeyWordInformation(
            "PUBLIC: Visibility modifier - NOT NEEDED in EK9! All constructs and methods are public by default. EK9 philosophy: public-by-default reduces boilerplate. Use `private` or `protected` when you need restriction. Most code should NOT use `public` keyword. https://ek9.io/basics.html#visibility",
            List.of(""), TokenResult::previousTokensIndentsOrFirst));
    keywordMap.put("protected",
        new KeyWordInformation(
            "PROTECTED: Restrict method/property visibility to class and subclasses only. Use when implementation detail should be available to subclasses but not external callers. Only applicable to classes (not records, components, functions). Use when: template method pattern, protected helpers for subclasses. NOT for: public APIs. https://ek9.io/basics.html#class_visibility",
            List.of("protected"), TokenResult::previousTokensIndentsOrFirst));
    keywordMap.put("private",
        new KeyWordInformation(
            "PRIVATE: Restrict method/property visibility to declaring class only. Use for internal implementation details that should never be accessed outside the class. More restrictive than `protected`. Use when: internal helpers, implementation hiding, encapsulation. Best practice: minimize public surface, maximize private internals. https://ek9.io/basics.html#visibility",
            List.of("private"), TokenResult::previousTokensIndentsOrFirst));
    keywordMap.put("open",
        new KeyWordInformation(
            "OPEN: Allow class/function to be extended by subclasses. EK9 default: classes/functions are FINAL (cannot be extended). Must explicitly mark `as open` to enable inheritance. Use when: designing for extension, template method pattern, framework classes. NOT for: most classes (prefer composition). Prevents fragile base class problem. https://ek9.io/inheritance.html",
            List.of("open"), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("extends",
        new KeyWordInformation(
            "EXTENDS: Inherit from parent class/function/component/record. Creates is-a relationship. Child inherits all parent methods/properties. Can override methods with `override` keyword. Use when: specializing behavior, reusing implementation. NOT for: code reuse (prefer traits/composition). Single inheritance only - use traits for multiple behaviors. https://ek9.io/inheritance.html",
            List.of("extends"), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("constrain",
        new KeyWordInformation(
            "CONSTRAIN: Restrict type/generic parameter to specific capabilities. Use for: (1) TYPE CONSTRAINTS - limit what types can be (constrain by trait), (2) GENERIC CONSTRAINTS - restrict type parameters (type T constrain by Comparable). Ensures generic code can safely call required methods. Use when: generics need specific operations, type safety requirements. Makes generic code type-safe. https://ek9.io/generics.html",
            Arrays.asList("constrain by", "constrain as"), search -> !search.previousTokenIsPipe()));
    keywordMap.put("pure",
        new KeyWordInformation(
            "PURE: Mark function/method as having no side effects (no mutation, no I/O, deterministic). Pure functions always return same output for same input. Enables compiler optimizations, parallel execution, memoization. Use when: mathematical functions, transformations, immutable operations. Compiler enforces: no reassignment, no external state mutation. Benefits: testable, cacheable, predictable. https://ek9.io/basics.html#pure",
            List.of("pure"), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("abstract",
        new KeyWordInformation(
            "ABSTRACT: Mark construct/method as incomplete (no implementation). Must be implemented by subclasses/implementations. Use for: abstract base classes, template methods, contracts. Abstract functions/classes cannot be instantiated - only concrete subclasses can. Use when: defining interface contracts, forcing subclass implementation. Combine with `open` for extensible abstractions. https://ek9.io/inheritance.html",
            List.of("abstract"), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("allow",
        new KeyWordInformation(
            "ALLOW: Control which specific types can extend/use a trait. Restricts trait usage to approved types only. Use when: trait has strict requirements, preventing misuse, security/safety constraints. Syntax: `allow SomeClass, AnotherClass`. Opposite of open-for-all. Use when: trait implementation assumes specific type properties. Prevents inappropriate trait application. https://ek9.io/traits.html",
            List.of("allow"), getSearchNotIndentsAndNotPipe()));

  }


  private void populateOperators(final Map<String, KeyWordInformation> keywordMap) {

    populateListOfOperators(keywordMap);
    populateEqualityAndCoalescingTypeOperators(keywordMap);
    populateAssignmentTypeOperators(keywordMap);
    populateEqualityOperators(keywordMap);
    populateMathematicsOperators(keywordMap);
    populatesInOutOperators(keywordMap);
    populateConversionOperators(keywordMap);
    populateAccessOperators(keywordMap);
    populateBooleanOperators(keywordMap);

  }

  private void populateAccessOperators(final Map<String, KeyWordInformation> keywordMap) {

    keywordMap.put("contains", new KeyWordInformation(
        "CONTAINS: Check if collection/container has specific item (list contains value). Works with List, Dict (checks keys), Set, String (substring). Use for membership testing before access. Returns Boolean: true if item present, false otherwise. https://ek9.io/operators.html#functional",
        List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("matches", new KeyWordInformation(
        "MATCHES: Check if value matches pattern or predicate (value matches pattern). Works with String (regex), custom matching logic. Use for pattern validation, regex testing, or custom match rules. Returns Boolean: true if match succeeds, false otherwise. https://ek9.io/operators.html#functional",
        List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("empty", new KeyWordInformation(
        "IS EMPTY: Check if collection/container has no elements (list empty). Works with List, Dict, Set, String. Use to test before iteration or to validate non-empty requirement. Returns Boolean: true if no elements, false if has elements. Note: empty collections are still SET (not unset). https://ek9.io/operators.html#functional",
        List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("length", new KeyWordInformation(
        "LENGTH: Get number of elements in collection/container (list length → count). Works with List, Dict, Set, String (character count), Iterator. Use for size checks, iteration bounds, or capacity planning. Returns Integer count of elements. https://ek9.io/operators.html#functional",
        List.of(), getSearchNotIndentsAndNotPipe()));

  }

  private void populateConversionOperators(final Map<String, KeyWordInformation> keywordMap) {

    keywordMap.put("$", new KeyWordInformation(
        "STRING CONVERSION: Convert any value to String representation (value$ → \"string\"). All EK9 types implement this operator. Use for display, logging, debugging, or string concatenation. More explicit than implicit toString(). Returns String representation of value. https://ek9.io/operators.html#functional",
        List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("$$", new KeyWordInformation(
        "JSON CONVERSION: Convert value to JSON string representation (value$$ → \"{json}\"). Works with records, classes, collections. Use for API responses, data serialization, or persistence. Returns JSON-formatted String. Unset values map to JSON null. https://ek9.io/operators.html#functional",
        List.of(), getSearchNotIndentsAndNotPipe()));

    keywordMap.put("#^", new KeyWordInformation(
        "TYPE PROMOTION: Convert value to wider/more general type (Integer → Float, specific type → Any). Returns promoted value if possible, returns unset if promotion not supported. Use when you need automatic type widening for polymorphic operations or generic containers. https://ek9.io/operators.html#functional",
        List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("#?", new KeyWordInformation(
        "HASH CODE: Get hash code integer for value (value#? → hash). All EK9 types implement this operator. Use for hash-based collections (Dict, Set), equality comparisons, or custom hashing. Returns Integer hash code. Equal values must have equal hash codes. https://ek9.io/operators.html#functional",
        List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("#<", new KeyWordInformation(
        "GET FIRST: Extract first element from collection or sequence (list#< → firstItem). Works with List, String (first character), Iterator. Use when you need just the first element without iteration. Returns first element or unset if collection empty. https://ek9.io/operators.html#functional",
        List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("#>", new KeyWordInformation(
        "GET LAST: Extract last element from collection or sequence (list#> → lastItem). Works with List, String (last character). Use when you need just the last element without iteration. Returns last element or unset if collection empty. https://ek9.io/operators.html#functional",
        List.of(), getSearchNotIndentsAndNotPipe()));

  }

  private void populatesInOutOperators(final Map<String, KeyWordInformation> keywordMap) {

    keywordMap.put("->", new KeyWordInformation(
        "INCOMING PARAMETERS: Declare function/method input parameters with types (-> param as Type). Use when: defining function parameters, method arguments, operator inputs. Syntax: `-> param1 as Type1, param2 as Type2`. Parameters are immutable by default. Multiple parameters separated by commas. Use for all function/method/operator parameter declarations. https://ek9.io/functions.html",
        List.of(), search -> !search.previousTokenIsPipe()));

    keywordMap.put("<-", new KeyWordInformation(
        "DECLARATION GUARD: Create new variable + check if SET (most common pattern). In control flow: only execute if value is SET. https://ek9.io/flowControl.html#guard_operators",
        List.of(), search -> !search.previousTokenIsPipe()));

  }

  private void populateBooleanOperators(final Map<String, KeyWordInformation> keywordMap) {

    keywordMap.put("and", new KeyWordInformation(
        "DUAL USE (type-dependent): (1) BOOLEAN AND - logical conjunction with short-circuit evaluation (true and false → false), (2) BITWISE AND - binary AND operation on integers (0b1100 and 0b1010 → 0b1000). Operation determined by operand types. https://ek9.io/operators.html",
        List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("or", new KeyWordInformation(
        "DUAL USE (type-dependent): (1) BOOLEAN OR - logical disjunction with short-circuit evaluation (true or false → true), (2) BITWISE OR - binary OR operation on integers (0b1100 or 0b1010 → 0b1110). Operation determined by operand types. https://ek9.io/operators.html",
        List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("xor", new KeyWordInformation(
        "DUAL USE (type-dependent): (1) BOOLEAN XOR - logical exclusive-or (true xor true → false), (2) BITWISE XOR - binary XOR operation on integers (0b1100 xor 0b1010 → 0b0110). Operation determined by operand types. https://ek9.io/operators.html",
        List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("not", new KeyWordInformation(
        "DUAL USE (type-dependent): (1) BOOLEAN NOT - logical negation (not true → false), (2) BITWISE NOT - binary NOT operation (invert all bits). Operation determined by operand type. For bitwise NOT, `~` is preferred. https://ek9.io/operators.html",
        List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("~", new KeyWordInformation(
        "THREE DISTINCT USES (context-dependent): (1) BOOLEAN NOT - negate boolean value (~true → false), (2) BITWISE NOT - invert all bits in integer (~0b1010 → 0b0101), (3) REVERSE - reverse order of list/collection items (~[1,2,3] → [3,2,1]). Operation determined by operand type. https://ek9.io/operators.html",
        List.of(), getSearchNotIndentsAndNotPipe()));

  }

  private void populateMathematicsOperators(final Map<String, KeyWordInformation> keywordMap) {

    keywordMap.put("+",
        new KeyWordInformation(
            "ADDITION: Add two numbers or concatenate strings (a + b). For numbers: standard arithmetic addition. For strings: concatenation. Use when: numeric calculations, combining values, string building. Returns Integer for integers, Float for floating-point, String for string concatenation. Supports operator overloading for custom types. https://ek9.io/operators.html#mathematical",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("-",
        new KeyWordInformation(
            "SUBTRACTION: Subtract right value from left (a - b) or negate number (-a). Binary form: returns difference. Unary form: returns negative. Use when: numeric calculations, differences, negation. Returns Integer for integers, Float for floating-point. Supports operator overloading for custom types. https://ek9.io/operators.html#mathematical",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("*",
        new KeyWordInformation(
            "MULTIPLICATION: Multiply two numbers (a * b). Standard arithmetic multiplication. Use when: scaling values, calculating products, area/volume calculations. Returns Integer for integers, Float for floating-point. Supports operator overloading for custom types (e.g., matrix multiplication). https://ek9.io/operators.html#mathematical",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("/",
        new KeyWordInformation(
            "DIVISION: Divide left value by right (a / b). Integer division truncates toward zero. Floating-point division produces decimal results. Use when: splitting values, ratios, averages. Returns Integer for integer operands (truncated), Float for floating-point. Division by zero produces unset value (not exception). https://ek9.io/operators.html#mathematical",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("+=",
        new KeyWordInformation(
            "ADD ASSIGN: Add value and assign result to variable (a += b → a := a + b). Shorthand for addition assignment. Use when: accumulating totals, incrementing by amount, string building. Mutates variable with sum. For strings, appends to existing value. Returns updated value. https://ek9.io/operators.html#assignment",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("-=",
        new KeyWordInformation(
            "SUBTRACT ASSIGN: Subtract value and assign result to variable (a -= b → a := a - b). Shorthand for subtraction assignment. Use when: decreasing counters, removing amounts, calculating differences. Mutates variable with difference. Returns updated value. https://ek9.io/operators.html#assignment",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("*=",
        new KeyWordInformation(
            "MULTIPLY ASSIGN: Multiply value and assign result to variable (a *= b → a := a * b). Shorthand for multiplication assignment. Use when: scaling values, repeated multiplication, compound calculations. Mutates variable with product. Returns updated value. https://ek9.io/operators.html#assignment",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("/=",
        new KeyWordInformation(
            "DIVIDE ASSIGN: Divide value and assign result to variable (a /= b → a := a / b). Shorthand for division assignment. Use when: scaling down, ratio calculations, averaging. Mutates variable with quotient. Integer division truncates. Returns updated value. https://ek9.io/operators.html#assignment",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("++",
        new KeyWordInformation(
            "INCREMENT: Add 1 to numeric value returning NEW incremented object (value++ → value+1). EK9 semantics DIFFER from Java/C++: always returns new object (immutable pattern), never mutates in-place. Use for counting, iteration. Returns new Integer/Float with value+1. https://ek9.io/operators.html#mathematical",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("--",
        new KeyWordInformation(
            "DECREMENT: Subtract 1 from numeric value returning NEW decremented object (value-- → value-1). EK9 semantics DIFFER from Java/C++: always returns new object (immutable pattern), never mutates in-place. Use for counting down, iteration. Returns new Integer/Float with value-1. https://ek9.io/operators.html#mathematical",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("^",
        new KeyWordInformation(
            "EXPONENTIATION: Raise number to power (base ^ exponent). Calculates base raised to exponent power. Use when: mathematical power operations, exponential calculations, scientific computing. Example: 2 ^ 8 → 256, 10 ^ 3 → 1000. Returns Integer for integer operands, Float for floating-point. https://ek9.io/operators.html#mathematical",
            List.of(), getSearchNotIndentsAndNotPipe()));

    keywordMap.put("mod",
        new KeyWordInformation(
            "MODULUS: Mathematical modulo operation (a mod b). Always returns non-negative result matching divisor sign. Use for cyclic patterns, array indexing with wrap-around. Different from `rem`: mod always positive, rem matches dividend sign. Returns Integer. https://ek9.io/operators.html#mathematical",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("rem",
        new KeyWordInformation(
            "REMAINDER: Remainder after division (a rem b). Result takes sign of dividend (first operand). Use for splitting values, checking divisibility. Different from `mod`: rem matches dividend sign, mod always positive. Example: -7 rem 3 → -1, -7 mod 3 → 2. https://ek9.io/operators.html#mathematical",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("abs",
        new KeyWordInformation(
            "ABSOLUTE VALUE: Get non-negative magnitude of number (abs of value). Removes sign, returns positive equivalent. Use when: distance calculations, magnitude operations, ensuring non-negative results. Example: abs of -5 → 5, abs of 3 → 3. Returns Integer for Integer input, Float for Float input. https://ek9.io/operators.html#mathematical",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("sqrt",
        new KeyWordInformation(
            "SQUARE ROOT: Calculate square root of number (sqrt of value). Returns value that when multiplied by itself equals input. Use when: distance formulas, geometric calculations, mathematical operations. Example: sqrt of 16 → 4, sqrt of 2 → 1.414... Returns Float for precise decimal results. https://ek9.io/operators.html#mathematical",
            List.of(), getSearchNotIndentsAndNotPipe()));

  }

  private void populateEqualityOperators(final Map<String, KeyWordInformation> keywordMap) {

    keywordMap.put("<",
        new KeyWordInformation(
            "LESS THAN: Compare if left value is smaller than right (a < b). Returns Boolean: true if a is less than b, false otherwise. Use when: ordering comparisons, range checks, sorting logic. Works with Integer, Float, String (lexicographic), and types implementing comparison operators. Supports tri-state semantics for optional values. https://ek9.io/operators.html#comparison",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("<?",
        new KeyWordInformation(
            "NULL-SAFE LESS THAN: Compare values but returns unset (not error) if either operand is unset/null. Use when comparing Optional values where unset is valid state. Standard `<` would error on unset operands. Result: true/false if both set, unset if either unset. https://ek9.io/operators.html#assignment_coalescing",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("<=",
        new KeyWordInformation(
            "LESS THAN OR EQUAL: Compare if left value is smaller than or equal to right (a <= b). Returns Boolean: true if a is less than or equal to b, false otherwise. Use when: range validation, boundary checks, inclusive comparisons. Works with all comparable types. Supports tri-state semantics. https://ek9.io/operators.html#comparison",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("<=?",
        new KeyWordInformation(
            "NULL-SAFE LESS THAN OR EQUAL: Compare values but returns unset (not error) if either operand is unset/null. Use when comparing Optional values where unset is valid state. Standard `<=` would error on unset operands. Result: true/false if both set, unset if either unset. https://ek9.io/operators.html#assignment_coalescing",
            List.of(), getSearchNotIndentsAndNotPipe()));

    keywordMap.put(">",
        new KeyWordInformation(
            "DUAL USE (context-dependent): (1) COMPARISON - greater than operator (a > b), (2) STREAM TERMINATOR - add items to collection from stream pipeline (cat items | filter by x > 10 > resultList). Context determines meaning. https://ek9.io/operators.html#comparison ",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put(">?",
        new KeyWordInformation(
            "NULL-SAFE GREATER THAN: Compare values but returns unset (not error) if either operand is unset/null. Use when comparing Optional values where unset is valid state. Standard `>` would error on unset operands. Result: true/false if both set, unset if either unset. https://ek9.io/operators.html#assignment_coalescing",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put(">=",
        new KeyWordInformation(
            "GREATER THAN OR EQUAL: Compare if left value is larger than or equal to right (a >= b). Returns Boolean: true if a is greater than or equal to b, false otherwise. Use when: threshold checks, minimum value validation, inclusive range checks. Works with all comparable types. Supports tri-state semantics. https://ek9.io/operators.html#comparison",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put(">=?",
        new KeyWordInformation(
            "NULL-SAFE GREATER THAN OR EQUAL: Compare values but returns unset (not error) if either operand is unset/null. Use when comparing Optional values where unset is valid state. Standard `>=` would error on unset operands. Result: true/false if both set, unset if either unset. https://ek9.io/operators.html#assignment_coalescing",
            List.of(), getSearchNotIndentsAndNotPipe()));

    keywordMap.put("<=>",
        new KeyWordInformation(
            "THREE-WAY COMPARISON (spaceship operator): Compare two values returning -1 (less), 0 (equal), or 1 (greater). Use for sorting implementations or when you need all three comparison outcomes in one operation (a <=> b). More efficient than separate <, ==, > checks. Returns Integer: -1/0/1. https://ek9.io/operators.html#comparison",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("<~>",
        new KeyWordInformation(
            "FUZZY COMPARISON: Compare values with tolerance for approximate equality. Use for floating-point comparisons (0.1 + 0.2 <~> 0.3 → true), strings with minor differences, or \"close enough\" matching. Returns Boolean indicating if values are approximately equal within tolerance. https://ek9.io/operators.html#comparison",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("<<",
        new KeyWordInformation(
            "BITWISE SHIFT LEFT: Shift bits left in integer value (0b0011 << 2 → 0b1100). Each shift left multiplies by 2. Use for bit manipulation, flag operations, or fast multiplication by powers of 2. Returns Integer with shifted bits. https://ek9.io/operators.html",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put(">>",
        new KeyWordInformation(
            "DUAL USE (context-dependent): (1) BITWISE - shift bits right on integer (0b1100 >> 2 → 0b0011), (2) STREAM TERMINATOR - append items to collection from stream pipeline (cat items | filter by condition >> existingList). Context determines meaning. https://ek9.io/operators.html",
            List.of(), getSearchNotIndentsAndNotPipe()));

  }

  private void populateEqualityAndCoalescingTypeOperators(final Map<String, KeyWordInformation> keywordMap) {

    keywordMap.put("?",
        new KeyWordInformation(
            "IS SET: Check if value is set/initialized (not unset/null). Returns Boolean: true if value has valid data, false if unset. Fundamental to EK9's tri-state system (absent, unset, set). Use when: validating optional values, checking before access, conditional logic on presence. Works with all EK9 types. Central to null-safety. Example: if value? then use(value). https://ek9.io/operators.html#ternary",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("??",
        new KeyWordInformation(
            "NULL COALESCING: Return first set value from multiple options (a ?? b ?? c). Evaluates left-to-right, returns first value that is set (not unset/null). Use when: providing fallback values, default chains, cascading options. Short-circuits - stops at first set value. Returns first set value or unset if all unset. Example: name ?? defaultName ?? \"Unknown\". https://ek9.io/operators.html#ternary",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("?:",
        new KeyWordInformation(
            "ELVIS OPERATOR: Return value if set, otherwise return alternative (value ?: defaultValue). Named for resemblance to Elvis Presley's hair (?:). Use when: single fallback needed, providing defaults for unset values. More concise than ?? for single fallback. Returns left operand if set, right operand if left is unset. Example: displayName := userName ?: \"Guest\". https://ek9.io/operators.html#ternary",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("?=",
        new KeyWordInformation(
            "GUARDED ASSIGNMENT: Checks RIGHT side (new value) is SET. ALWAYS evaluates expression. Use in control flow to validate new value. NOT same as :=? (different check). https://ek9.io/flowControl.html#guard_operators",
            List.of(""), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("!=",
        new KeyWordInformation(
            "NOT EQUALS: Check if two values are different. `!=` and `<>` are synonyms (work identically). Choose based on preference or codebase consistency. C/Java developers typically prefer `!=`, Pascal/SQL developers prefer `<>`. https://ek9.io/operators.html#comparison",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("<>",
        new KeyWordInformation(
            "NOT EQUALS: Check if two values are different. `<>` and `!=` are synonyms (work identically). Choose based on preference or codebase consistency. Pascal/SQL developers typically prefer `<>`, C/Java developers prefer `!=`. https://ek9.io/operators.html#comparison",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("==",
        new KeyWordInformation(
            "EQUALS: Check if two values are equal by value (a == b). Compares object contents, not references (unlike Java). Returns Boolean: true if equal, false if different. Use when: comparing values, equality checks, conditional logic. Works with all EK9 types. Respects tri-state semantics - comparing unset values returns appropriate result. https://ek9.io/operators.html#comparison",
            List.of(), getSearchNotIndentsAndNotPipe()));

  }

  private void populateAssignmentTypeOperators(final Map<String, KeyWordInformation> keywordMap) {

    keywordMap.put("=",
        new KeyWordInformation(
            "INITIAL ASSIGNMENT: Used for first-time variable initialization with literal values or simple expressions. For declarations with type safety checking, use `<-` (declaration guard). For updates, use `:=`. Cannot be used in control flow guards. https://ek9.io/operators.html#assignment",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put(":",
        new KeyWordInformation(
            "SHORT ASSIGNMENT: Shorthand for `:=` (reassignment). Updates existing variable with new value. Commonly used for simple reassignment. Cannot be used in control flow guards. For safety-checked updates, use `?=` (guarded assignment). https://ek9.io/operators.html#assignment",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put(":=",
        new KeyWordInformation(
            "BLIND ASSIGNMENT: Update existing variable with NO safety check. Use only when you KNOW value is valid. Consider ?= for safety. https://ek9.io/operators.html#assignment",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("::",
        new KeyWordInformation(
            "MODULE REFERENCE: Access constructs from other modules (ModuleName::ConstructName). Scope resolution operator for module-qualified names. Use when: referencing external module constructs, disambiguating names, explicit module imports. Syntax: `com.example.utils::StringHelper`. Common in `references` statements. Enables modular code organization. https://ek9.io/basics.html#references_example",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put(":=:",
        new KeyWordInformation(
            "COPY: Copy operator (target :=: source). The 'default' implementation does SHALLOW copy (copies field references). For DEEP copy, implement your own :=: operator - only you know which fields need true duplication vs shared references (e.g., insurance: coverClass shared, customerData duplicated). See HTML docs for deep copy example pattern. https://ek9.io/operators.html#modification",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put(":~:",
        new KeyWordInformation(
            "MERGE: Combine two objects by merging their properties (target :~: source). Source properties override target where they differ. Use for configuration merging, partial updates, or combining data. Non-destructive on source. Returns merged result. https://ek9.io/operators.html#modification",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put(":^:",
        new KeyWordInformation(
            "REPLACE: Replace target object's content with source (target :^: source). Updates target in-place with source values. Use for complete object replacement while preserving target identity/references. Destructive operation on target. Returns modified target. https://ek9.io/operators.html#modification",
            List.of(), getSearchNotIndentsAndNotPipe()));
    keywordMap.put(":=?",
        new KeyWordInformation(
            "ASSIGNMENT IF UNSET: Checks LEFT side (current variable) FIRST. Only assigns if currently UNSET. LAZY evaluation (doesn't evaluate if already set). Perfect for caching. NOT same as ?= (opposite check). https://ek9.io/flowControl.html#guard_operators",
            List.of(), getSearchNotIndentsAndNotPipe()));

  }

  private void populateListOfOperators(final Map<String, KeyWordInformation> keywordMap) {

    keywordMap.put("operator", new KeyWordInformation(
        "OPERATOR DEFINITION: Define custom operator implementation for user-defined types (operator overloading). Enables your classes/records to use standard operators like +, -, *, ==, <, etc. Use when: want natural syntax for domain operations (matrix + matrix, vector * scalar, date + duration). Define operators to make types work with standard operators. Common operators: arithmetic (+, -, *, /), comparison (==, <, >), conversion ($, #^), testing (?, empty, length). Operator overloading enables intuitive, mathematical syntax for custom types. https://ek9.io/operators.html",
        Arrays.asList("operator", "operator $", "operator $$", "operator >", "operator <",
            "operator :=", "operator !=", "operator <=", "operator >=", "operator <=>",
            "operator <~>", "operator :~:", "operator :=:", "operator :^:", "operator !",
            "operator ?", "operator ~", "operator ++", "operator --", "operator +", "operator -",
            "operator *", "operator /", "operator +=", "operator -=", "operator *=", "operator /=",
            "operator ^", "operator |", "operator #^", "operator #?", "operator #<", "operator #>",
            "operator mod", "operator rem", "operator abs", "operator sqrt", "operator contains",
            "operator matches", "operator empty", "operator length"),
        search -> search.previousTokensIndentsOrFirst() || search.previousTokenIsOverride()));

  }

  private void populateFlowControl(final Map<String, KeyWordInformation> keywordMap) {

    keywordMap.put("if",
        new KeyWordInformation(
            "IF STATEMENT: Conditional execution based on boolean condition. Supports GUARDS for null-safety (if value <- getOptional()). Supports explicit conditions with `with` keyword (if x <- get() with x > 10). No early return - all branches must initialize return variables. Use when: making binary decisions, implementing conditional logic, branching based on conditions. Can combine with guards to eliminate null checks entirely. https://ek9.io/flowControl.html#if_elseif_else",
            List.of(), getSearchIndentsOrFirstOrAssignment()));
    keywordMap.put("switch",
        new KeyWordInformation(
            "SWITCH STATEMENT: Multi-way conditional branching with case statements. `switch` and `given` are synonyms (identical behavior). Supports guards for null-safety (switch value <- expression). Use `case` for conditions, `default` for fallback. No fallthrough between cases. https://ek9.io/flowControl.html#switch",
            List.of(), getSearchIndentsOrFirstOrAssignment()));
    keywordMap.put("given",
        new KeyWordInformation(
            "GIVEN STATEMENT: Alternative keyword for `switch` - multi-way conditional branching. `given` and `switch` are synonyms (identical behavior). Some developers find `given` reads more naturally. Supports guards and case statements. Choose based on readability preference. https://ek9.io/flowControl.html#switch",
            List.of("given"), getSearchIndentsOrFirstOrAssignment()));
    keywordMap.put("case",
        new KeyWordInformation(
            "CASE: Branch condition in switch/given statement. Can match single value (`case 1`), multiple values (`case 1, 2, 3`), ranges, or expressions. NO FALLTHROUGH - each case is independent. Can include guard conditions. Use when: pattern matching specific values, handling specific conditions in switch. Each case executes independently - no break needed (EK9 safety feature). https://ek9.io/flowControl.html#switch",
            List.of("case"), TokenResult::previousTokensIndentsOrFirst));
    keywordMap.put("default",
        new KeyWordInformation(
            "DEFAULT: Fallback case in switch/given when no other case matches. Always executes if no explicit case matched. OPTIONAL but recommended for completeness. Use when: handling unexpected values, ensuring all paths covered. Compiler may require if return value must be initialized in all branches. https://ek9.io/flowControl.html#switch",
            List.of("default\n"), TokenResult::previousTokensIndentsOrFirst));
    keywordMap.put("when",
        new KeyWordInformation(
            "WHEN: Alternative keyword for `if` in conditional expressions. `if` and `when` are synonyms. Some developers find `when` reads more naturally in certain contexts. Use based on readability preference. Identical behavior to `if`. Supports same guard syntax and conditions. https://ek9.io/flowControl.html#if_elseif_else",
            List.of("when")));
    keywordMap.put("else",
        new KeyWordInformation(
            "ELSE: Alternative branch when if/when condition is false. Can chain with `else if` for multiple conditions. Final `else` provides default path. Use when: binary decisions need both paths, providing fallback behavior. In functions with return values, compiler may require else to ensure all paths initialize return variable. https://ek9.io/flowControl.html#if_elseif_else",
            List.of("else"), TokenResult::previousTokensIndentsOrFirst));

  }

  private void populateLoops(final Map<String, KeyWordInformation> keywordMap) {

    keywordMap.put("do",
        new KeyWordInformation(
            "DO-WHILE LOOP: Execute body at least once, then repeat while condition true. Checks condition AFTER body execution. Supports guards in condition (do ... while value <- get()). NO break/continue - use stream pipelines for early termination. Use when: body must execute at least once, polling/retry logic. Condition evaluated after each iteration. https://ek9.io/flowControl.html#do_while_loop",
            List.of(), TokenResult::previousTokensIndentsOrFirst));
    keywordMap.put("while",
        new KeyWordInformation(
            "WHILE LOOP: Repeat body while condition is true. Checks condition BEFORE each iteration. Supports guards in condition (while value <- get()). NO break/continue - use stream pipelines for complex iteration. Use when: unknown iteration count, conditional loops. Body may execute zero times if condition initially false. https://ek9.io/flowControl.html#while_loop",
            List.of(), TokenResult::previousTokensIndentsOrFirst));
    keywordMap.put("for",
        new KeyWordInformation(
            "FOUR DISTINCT USES (context-dependent): (1) LOOP iteration (for item in list), (2) STREAM pipeline filter (cat items | filter for condition), (3) TEXT template language selection (text for language), (4) HTTP SERVICE URI mapping (GET for :/path). Meaning determined by context. https://ek9.io/flowControl.html",
            List.of(), getSearchIndentsOrFirstOrAssignment()));

  }

  private void populateTryCatch(final Map<String, Ek9LanguageWords.KeyWordInformation> keywordMap) {

    keywordMap.put("try",
        new KeyWordInformation(
            "TRY BLOCK: Begin exception handling block. Code in try block monitored for exceptions. Supports guards for resource management (try resource <- acquire()). Use with catch/handle to handle exceptions, finally for cleanup. Use when: operations may throw exceptions, resource management, error handling. Exceptions propagate to nearest catch block. Guards ensure resources properly initialized. https://ek9.io/exceptions.html",
            List.of(), getSearchIndentsOrFirstOrAssignment()));
    keywordMap.put("catch",
        new KeyWordInformation(
            "CATCH EXCEPTION: Handle exceptions in try block. Use `catch -> ex as ExceptionType` to capture exception and handle it. Can have multiple catch blocks for different exception types. `catch` and `handle` are synonyms in EK9. https://ek9.io/exceptions.html",
            List.of(), TokenResult::previousTokensIndentsOrFirst));
    keywordMap.put("handle",
        new KeyWordInformation(
            "HANDLE EXCEPTION: Alternative keyword for `catch` - handles exceptions in try block. Use `handle -> ex as ExceptionType` to capture exception. `handle` and `catch` are synonyms (identical behavior). Choose based on readability preference. https://ek9.io/exceptions.html",
            List.of(), TokenResult::previousTokensIndentsOrFirst));
    keywordMap.put("finally",
        new KeyWordInformation(
            "FINALLY BLOCK: Cleanup code that ALWAYS executes after try/catch blocks, regardless of whether exception occurred or was caught. Executes even if try/catch contains return statement. Use for guaranteed cleanup operations: closing files, releasing resources, logging, resetting state. Works with guards for resource safety. Use when: cleanup must happen no matter what, resource release is critical, state must be restored. Finally blocks cannot be skipped - guaranteed execution. https://ek9.io/exceptions.html",
            List.of(), TokenResult::previousTokensIndentsOrFirst));

  }

  private void populateStreaming(final Map<String, Ek9LanguageWords.KeyWordInformation> keywordMap) {

    keywordMap.put("cat", new KeyWordInformation(
        "CATENATE STREAM: Start streaming pipeline from collection/source (cat items | filter | map). Short for concatenate - initiates stream processing. Use to begin any stream pipeline. Can concatenate multiple sources (cat list1, list2). Returns Stream for pipeline operations. https://ek9.io/streamsAndPipelines.html",
        List.of(), getSearchIndentsOrFirstOrAssignment()));

    keywordMap.put("|", new KeyWordInformation(
        "PIPE OPERATOR: Link stream operations into processing pipeline (cat items | filter | map | collect). Chains operations left-to-right, passing results from one to next. Each operation receives stream from previous operation. Enables declarative data transformation. Use to compose stream operations. Fundamental to EK9 streaming. Similar to Unix pipe - clean, readable data flow. https://ek9.io/streamsAndPipelines.html",
        List.of(), getSearchNotIndentsAndNotPipe()));

    keywordMap.put("filter", new KeyWordInformation(
        "FILTER STREAM: Keep only items that match predicate in stream pipeline. `filter` and `select` are synonyms (identical behavior). Use `cat items | filter by condition` or `filter with predicate`. Choose based on readability preference - `filter` is more common. https://ek9.io/streamsAndPipelines.html",
        Arrays.asList("filter", "filter with", "filter by"), TokenResult::previousTokenIsPipe));
    keywordMap.put("select", new KeyWordInformation(
        "SELECT STREAM: Alternative keyword for `filter` - keeps only items that match predicate. `select` and `filter` are synonyms (identical behavior). Use `cat items | select by condition`. SQL developers may find `select` more familiar. https://ek9.io/streamsAndPipelines.html",
        Arrays.asList("select", "select with", "select by"), TokenResult::previousTokenIsPipe));
    keywordMap.put("map", new KeyWordInformation(
        "MAP STREAM: Transform each stream item to different value or type using function (cat items | map with transformer). Applies transformation function to every item. Use when: converting types, extracting properties, applying calculations. Use `map with function` or `map by property`. Returns stream of transformed items. Common pattern for data transformation pipelines. https://ek9.io/streamsAndPipelines.html",
        Arrays.asList("map", "map with", "map by"), TokenResult::previousTokenIsPipe));
    keywordMap.put("sort", new KeyWordInformation(
        "SORT STREAM: Order stream items using comparator function or natural ordering (cat items | sort by comparator). Sorts all items in stream. Use when: need ordered output, ranking items, organizing data. Use `sort with comparator` for custom ordering, `sort by property` for property-based sorting. Returns stream of sorted items. Maintains stable sort order. https://ek9.io/streamsAndPipelines.html",
        Arrays.asList("sort", "sort with", "sort by"), TokenResult::previousTokenIsPipe));
    keywordMap.put("group", new KeyWordInformation(
        "GROUP STREAM: Collect stream items into groups based on key property or function (cat items | group by category). Creates map of key to list of items with that key. Use when: categorizing data, aggregating by property, creating lookups. Use `group by property` or `group with function`. Returns Dict mapping keys to grouped items. Common for aggregation and reporting. https://ek9.io/streamsAndPipelines.html",
        Arrays.asList("group", "group with", "group by"), TokenResult::previousTokenIsPipe));
    keywordMap.put("join", new KeyWordInformation(
        "JOIN STREAMS: Combine items from stream into single string or merge streams. Use `join with separator` for string concatenation, or join multiple streams together. Use for creating delimited output, combining data sources. Returns joined result. https://ek9.io/streamsAndPipelines.html",
        Arrays.asList("join", "join with", "join by"), TokenResult::previousTokenIsPipe));
    keywordMap.put("split", new KeyWordInformation(
        "SPLIT STREAM: Divide stream items based on property or predicate. Use `split by criteria` to partition stream into sub-streams. Use for categorization, parallel processing paths, or conditional routing. Returns split streams. https://ek9.io/streamsAndPipelines.html",
        Arrays.asList("split", "split with", "split by"), TokenResult::previousTokenIsPipe));
    keywordMap.put("uniq", new KeyWordInformation(
        "UNIQUE STREAM: Remove duplicate items from stream, keeping only first occurrence (cat items | uniq). Filters stream to distinct values only. Use when: eliminating duplicates, finding unique values, deduplication. Use `uniq by property` to determine uniqueness by specific property. Returns stream of unique items. Preserves first occurrence order. Common for data cleaning. https://ek9.io/streamsAndPipelines.html",
        Arrays.asList("uniq", "uniq with", "uniq by"), TokenResult::previousTokenIsPipe));
    keywordMap.put("tee", new KeyWordInformation(
        "TEE STREAM: Split stream sending copies to multiple outputs while continuing main pipeline (like Unix tee command). Use `tee with consumer` to perform side effects (logging, metrics) without interrupting stream flow. Use for debugging, monitoring, or parallel processing. Returns original stream. https://ek9.io/streamsAndPipelines.html",
        Arrays.asList("tee", "tee with", "tee by", "tee in"), TokenResult::previousTokenIsPipe));
    keywordMap.put("flatten", new KeyWordInformation(
        "FLATTEN STREAM: Extract items from nested collections into single flat stream (cat listOfLists | flatten). Unnests one level of collection hierarchy. Use when: working with nested structures, combining multiple collections, converting 2D to 1D. Flattens List of Lists into single List. Returns flat stream of all nested items. Common for processing hierarchical data. https://ek9.io/streamsAndPipelines.html",
        List.of("flatten"), TokenResult::previousTokenIsPipe));
    keywordMap.put("call", new KeyWordInformation(
        "CALL FUNCTION: Invoke function/method on each stream item synchronously (cat items | call with processor). Blocks until each call completes. Use for sequential processing with functions. Each item processed in order. Returns stream of results. https://ek9.io/streamsAndPipelines.html",
        List.of("call"), TokenResult::previousTokenIsPipe));
    keywordMap.put("async", new KeyWordInformation(
        "ASYNC CALL: Invoke function/method on stream items asynchronously without blocking (cat items | async with processor). Processes items concurrently. Use for I/O-bound operations, parallel processing, or when order doesn't matter. Returns stream of Future results. https://ek9.io/streamsAndPipelines.html",
        List.of("async"), TokenResult::previousTokenIsPipe));
    keywordMap.put("skip", new KeyWordInformation(
        "SKIP STREAM: Discard first N items from stream, passing rest through (cat items | skip 5). Removes items from beginning of stream. Use when: ignoring headers, pagination (skip offset), removing leading items. Use `skip N` to skip fixed count. Returns stream without first N items. Combines with head for pagination. Common for data windowing. https://ek9.io/streamsAndPipelines.html",
        Arrays.asList("skip", "skip by", "skip of", "skip only"),
        TokenResult::previousTokenIsPipe));
    keywordMap.put("head", new KeyWordInformation(
        "HEAD STREAM: Take only first N items from stream, discarding rest (cat items | head 10). Limits stream to leading items. Use when: limiting results, getting samples, pagination (page size), early termination. Use `head N` to take fixed count, `head` alone takes first item. Returns stream of at most N items. Stops processing after N items - efficient for large streams. https://ek9.io/streamsAndPipelines.html",
        Arrays.asList("head", "head by", "head of", "head only"),
        TokenResult::previousTokenIsPipe));
    keywordMap.put("tail", new KeyWordInformation(
        "TAIL STREAM: Take only last N items from stream, discarding earlier ones (cat items | tail 10). Keeps trailing items only. Use when: getting recent items, last page of results, monitoring latest events. Use `tail N` to take last N items, `tail` alone takes last item. Returns stream of at most N items from end. Must process entire stream to find last N items. https://ek9.io/streamsAndPipelines.html",
        Arrays.asList("tail", "tail by", "tail of", "tail only"),
        TokenResult::previousTokenIsPipe));
    keywordMap.put("collect", new KeyWordInformation(
        "COLLECT STREAM: Terminate stream pipeline and gather all items into collection (cat items | filter | map | collect). Materializes stream results. Use when: need concrete collection, storing results, ending pipeline. Use `collect` for List, `collect as Type` for specific collection type. Returns final collection of all streamed items. Terminal operation - ends stream processing. Required to get actual results from pipeline. https://ek9.io/streamsAndPipelines.html",
        List.of("collect as"), TokenResult::previousTokenIsPipe));

  }

  private void populateWebServices(final Map<String, Ek9LanguageWords.KeyWordInformation> keywordMap) {

    keywordMap.put("GET", new KeyWordInformation(
        "HTTP GET: Retrieve resource from server without modifying it (GET :/path). Safe and idempotent - same request always produces same result, no side effects. Use when: fetching data, reading resources, querying information. Must not modify server state. Supports query parameters. Most common HTTP verb. Responses are cacheable. https://ek9.io/webServices.html",
        Arrays.asList("GET :/", "GET for :/"), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("DELETE", new KeyWordInformation(
        "HTTP DELETE: Remove resource from server (DELETE :/path/{id}). Idempotent - deleting same resource multiple times has same effect as once. Use when: removing resources, cleanup operations, deleting entities. Should require authentication and authorization. Often requires precondition checks (If-Match headers) for safety. Returns success even if resource already gone. Permanent removal operation. https://ek9.io/webServices.html",
        Arrays.asList("DELETE :/", "DELETE for :/"), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("HEAD", new KeyWordInformation(
        "HTTP HEAD: Get resource metadata without body (HEAD :/path). Identical to GET but returns only headers, no response body. Safe and idempotent. Use when: checking resource existence, getting content-length, checking last-modified date, testing links. Efficient for validation - no payload transfer. Returns same headers as GET would. Common for resource discovery. https://ek9.io/webServices.html",
        Arrays.asList("HEAD :/", "HEAD for :/"), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("POST", new KeyWordInformation(
        "HTTP POST: Create new resource or submit data to server (POST :/path). Not idempotent - repeated requests create multiple resources. Use when: creating resources, submitting forms, triggering actions, uploading files. Server determines new resource URL. Returns 201 Created with Location header for new resources. Body contains data for creation. Most common for state-changing operations. https://ek9.io/webServices.html",
        Arrays.asList("POST :/", "POST for :/"), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("PUT", new KeyWordInformation(
        "HTTP PUT: Replace entire resource at URL with new content (PUT :/path/{id}). Idempotent - same request multiple times has same effect. Use when: updating existing resources, replacing content, creating resource at known URL. Client specifies resource URL. Body contains complete replacement resource. Replaces entire resource - use PATCH for partial updates. Creates resource if doesn't exist. https://ek9.io/webServices.html",
        Arrays.asList("PUT :/", "PUT for :/"), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("PATCH", new KeyWordInformation(
        "HTTP PATCH: Apply partial modifications to resource (PATCH :/path/{id}). Updates only specified fields, leaves others unchanged. Use when: partially updating resources, changing specific properties, making incremental modifications. Body contains only changes (JSON Patch, merge patch). More efficient than PUT for small changes. Not guaranteed idempotent - depends on patch format. Preferred over PUT for partial updates. https://ek9.io/webServices.html",
        Arrays.asList("PATCH :/", "PATCH for :/"), getSearchNotIndentsAndNotPipe()));
    keywordMap.put("OPTIONS", new KeyWordInformation(
        "HTTP OPTIONS: Discover supported HTTP methods and capabilities for resource (OPTIONS :/path). Safe and idempotent. Use when: discovering APIs, checking allowed methods, handling CORS preflight requests. Returns Allow header listing supported verbs. Doesn't modify resource. Required for CORS cross-origin requests. Returns communication options. Common for API introspection. https://ek9.io/webServices.html",
        Arrays.asList("OPTIONS :/", "OPTIONS for :/"), getSearchNotIndentsAndNotPipe()));
  }

  private Function<TokenResult, Boolean> getSearchNotIndentsAndNotPipe() {
    return search -> !search.previousTokensIndentsOrFirst() && !search.previousTokenIsPipe();
  }

  private Function<TokenResult, Boolean> getSearchIndentsOrFirstOrAssignment() {
    return search -> search.previousTokensIndentsOrFirst() || search.previousTokenIsAssignment();
  }

  /**
   * Get only an exact match for this search.
   */
  public KeyWordInformation exactMatch(final TokenResult search) {

    KeyWordInformation rtn = null;
    if (search.isPresent()) {
      rtn = keywordMap.get(search.getToken().getText());
    }

    return rtn;
  }

  /**
   * Typically used for completion, where string is partial.
   * Receive a TokenResults, so we can also see position in the line.
   * This will affect the search results.
   */
  public List<String> fuzzyMatch(final TokenResult search) {
    final List<String> rtn = new ArrayList<>();
    if (search.isPresent()) {
      keywordMap.entrySet().stream()
          .filter(entry -> entry.getKey().startsWith(search.getToken().getText()))
          .filter(entry -> entry.getValue().isValidInThisContext(search))
          .forEach(entry -> entry.getValue().completionText.forEach(match -> {
            if (!rtn.contains(match)) {
              rtn.add(match);
            }
          }));
    }

    return rtn;
  }

  /**
   * Holds information relating to the EK9 language keywords.
   */
  public static class KeyWordInformation {
    //The hover text to be shown
    public final String hoverText;
    //One or more bits of text that could complete the partial keyword.
    private final List<String> completionText;
    //An optional function that can be used to indicate if this keyword is appropriate in the
    //context of use i.e. defines is only appropriate is the previous tokens were only indents.
    private final Function<TokenResult, Boolean> inContext;

    /**
     * Create key word info in terms of hover text, completions for a context of use.
     */
    public KeyWordInformation(final String hover,
                              final List<String> completions,
                              final Function<TokenResult, Boolean> inContext) {

      this.hoverText = hover;
      this.completionText = completions;
      this.inContext = inContext;

    }

    public KeyWordInformation(final String hover, final List<String> completions) {

      this(hover, completions, null);

    }

    /**
     * Check if a token is valid in a particular context of use.
     */
    public boolean isValidInThisContext(final TokenResult tokenResult) {

      if (inContext != null) {
        return inContext.apply(tokenResult);
      }

      return true;
    }
  }
}
