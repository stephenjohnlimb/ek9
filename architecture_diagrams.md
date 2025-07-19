# EK9 Compiler Architecture Diagrams

## 1. Module Dependency Graph

```
┌─────────────────┐
│  compiler-main  │ (Final JAR: ek9c.jar)
│                 │ - CLI, LSP, Compiler phases
│                 │ - ANTLR4, LSP4J, ASM
└─────────┬───────┘
          │
          ▼
┌─────────────────┐
│    ek9-lang     │ (Final JAR: org.ek9.lang.jar)
│  (stdlib-lang)  │ - EK9 built-in types
│                 │ - Jakarta JSON
└─────┬───┬───────┘
      │   │
      ▼   ▼
┌────────────────────┐     ┌─────────────────────┐
│ java-introspection │     │ compiler-tooling    │
│                    │     │                     │
│ - Reflections      │     │ - EK9 Annotations   │
│ - SLF4J            │     │ - Base tooling      │
└────────────────────┘     └─────────────────────┘
                                     ▲
                                     │
                            ┌────────┴────────┐
                            │   Foundation    │
                            │   Layer for     │
                            │   All Modules   │
                            └─────────────────┘
```

## 2. EK9 Compiler 22-Phase Pipeline

```
FRONTEND (Phases 0-9)          MIDDLE-END (Phases 10-14)      BACKEND (Phases 15-21)
┌─────────────────────────┐   ┌─────────────────────────┐   ┌─────────────────────────┐
│  0: PARSING             │   │ 10: SIMPLE_IR_GENERATION│   │ 15: CODE_GEN_PREP       │
│     ANTLR4 Parser       │   │     IRDefinitionVisitor │   │     File Creation       │
│  1: SYMBOL_DEFINITION   │   │ 11: PROGRAM_IR_CONFIG   │   │ 16: CODE_GEN_AGGREGATES │
│     Basic Symbols       │   │     IR Integration      │   │     ASM Bytecode        │
│  2: DUPLICATION_CHECK   │   │ 12: TEMPLATE_IR_GEN     │   │ 17: CODE_GEN_CONSTANTS  │
│     Duplicate Detection │   │     Generic Instantiate │   │     Constant Values     │
│  3: REFERENCE_CHECKS    │   │ 13: IR_ANALYSIS         │   │ 18: CODE_OPTIMISATION   │
│     Reference Resolution│   │     Whole Program       │   │     Target Optimizations│
│  4: EXPLICIT_TYPE_DEF   │   │ 14: IR_OPTIMISATION     │   │ 19: PLUGIN_LINKAGE      │
│     Generic Types       │   │     IR Optimizations    │   │     External Libraries  │
│  5: TYPE_HIERARCHY      │   └─────────────────────────┘   │ 20: APPLICATION_PACK    │
│     Inheritance Check   │                                 │     JAR Creation        │
│  6: FULL_RESOLUTION     │   ┌─── LSP STOPS HERE ─────┐    │ 21: PACKAGING_POST      │
│     Inferred Types      │   │   (IR_ANALYSIS)        │    │     Final Processing    │
│  7: POST_RESOLUTION     │   └────────────────────────┘    └─────────────────────────┘
│     Symbol Validation   │
│  8: PRE_IR_CHECKS       │   ┌─ Multi-threaded ──┐ ┌─ Single-threaded ──┐ ┌─ Configurable ─┐
│     Flow Analysis       │   │ 0,1,4,6,8,10,15,  │ │ 2,3,5,7,9,11,12,   │ │ Threading based │
│  9: PLUGIN_RESOLUTION   │   │ 16,17             │ │ 13,14,18,19,20,21  │ │ on CompilerFlags│
│     Plugin Points       │   └───────────────────┘ └────────────────────┘ └─────────────────┘
└─────────────────────────┘
```

## 3. Symbol System Class Hierarchy

```
                                    ISymbol (interface)
                                        │
                                        ▼
                                   Symbol (base)
                                 ┌─────┴─────┐
                                 ▼           ▼
                         ScopedSymbol    VariableSymbol
                              │               │
                              ▼               ▼
                    PossibleGenericSymbol   ExpressionSymbol
                       ┌─────┴─────┐
                       ▼           ▼
               AggregateSymbol   FunctionSymbol
                      │               │
                      ▼               ▼
              ┌─────────────┐   MethodSymbol
              │ Classes     │
              │ Records     │
              │ Components  │
              │ Traits      │
              └─────────────┘

Symbol Categories:                 Symbol Genus:
├─ TYPE                           ├─ CLASS / CLASS_TRAIT
├─ TEMPLATE_TYPE                  ├─ CLASS_CONSTRAINED / CLASS_ENUMERATION  
├─ METHOD                         ├─ RECORD / COMPONENT / VALUE
├─ TEMPLATE_FUNCTION              ├─ FUNCTION / FUNCTION_TRAIT
├─ FUNCTION                       ├─ SERVICE / PROGRAM / APPLICATION
├─ CONTROL                        └─ ANY
├─ VARIABLE
└─ ANY
```

## 4. Scope Resolution Hierarchy

```
┌─────────────────────────────────────────────────────────────┐
│                    Scope Resolution Chain                   │
└─────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   LocalScope    │───▶│   ModuleScope   │───▶│ ImplicitScopes  │
│                 │    │                 │    │                 │
│ - Block scopes  │    │ - Cross-module  │    │ - org.ek9.lang  │
│ - Method vars   │    │ - References    │    │ - org.ek9.math  │
│ - Parameters    │    │ - FQN resolution│    │ - Built-ins     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                               │
                               ▼
                  ┌─────────────────────────┐
                  │  SharedThreadContext    │
                  │    <CompilableProgram>  │
                  │                         │
                  │ - Thread-safe access    │
                  │ - Symbol definition     │
                  │ - Cross-module lookup   │
                  └─────────────────────────┘
```

## 5. Compilation Data Flow

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   .ek9      │───▶│ Workspace   │───▶│CompilableP- │───▶│    IR       │
│   Source    │    │             │    │   rogram    │    │   Module    │
│   Files     │    │ - Sources   │    │             │    │             │
└─────────────┘    │ - Ordering  │    │ - Modules   │    │ - Constructs│
                   │ - Thread    │    │ - Symbols   │    │ - Operations│
                   │   Safety    │    │ - Types     │    │ - Blocks    │
                   └─────────────┘    └─────────────┘    └─────────────┘
                                              │
                                              ▼
                                    ┌─────────────────┐
                                    │  ParsedModule   │
                                    │                 │
                                    │ - Symbol Tables │
                                    │ - Scopes        │
                                    │ - Error Lists   │
                                    └─────────────────┘
```

## 6. Bootstrap Process Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          Bootstrap Sequence                             │
└─────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│ Java Classes    │  ──────▶│   Introspection │  ──────▶│  Generated EK9  │
│ with @Ek9Class  │         │   Processing    │         │    Sources      │
└─────────────────┘         └─────────────────┘         └─────────────────┘
                                      │                           │
                                      ▼                           ▼
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│ Hard-coded EK9  │  ──────▶│  Ek9BuiltinLang │  ──────▶│  CompilableP-   │
│   Sources       │         │    Supplier     │         │    rogram       │
└─────────────────┘         └─────────────────┘         └─────────────────┘
                                                                  │
                                      ┌───────────────────────────┘
                                      ▼
                            ┌─────────────────┐
                            │ Frontend Phases │
                            │ (0-9) Only      │
                            │                 │
                            │ Stops at:       │
                            │ PLUGIN_         │
                            │ RESOLUTION      │
                            └─────────────────┘
```

## 7. Language Server Protocol Integration

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        LSP Architecture                                 │
└─────────────────────────────────────────────────────────────────────────┘
                                      │
                 ┌────────────────────┼────────────────────┐
                 ▼                    ▼                    ▼
    ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
    │      IDE        │◀──│ LSP4J Protocol  │──▶│  EK9 Language   │
    │   (VS Code)     │   │                 │   │     Server      │
    └─────────────────┘   │ - stdin/stdout  │   └─────────────────┘
                          │ - JSON-RPC      │            │
                          └─────────────────┘            ▼
                                                ┌─────────────────┐
                                                │ Text Document   │
                                                │    Service      │
                                                │                 │
                                                │ - Completion    │
                                                │ - Hover         │
                                                │ - Diagnostics   │
                                                └─────────────────┘
                                                         │
                                                         ▼
                                                ┌─────────────────┐
                                                │ EK9 Compiler    │
                                                │ (Limited to     │
                                                │ IR_ANALYSIS)    │
                                                └─────────────────┘
```

## 8. Target Architecture and Code Generation

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     Multi-Target Generation                             │
└─────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
                              ┌─────────────┐
                              │     IR      │
                              │ (Target     │
                              │  Neutral)   │
                              └─────────────┘
                                      │
                       ┌──────────────┼──────────────┐
                       ▼                             ▼
              ┌─────────────────┐           ┌─────────────────┐
              │   JVM Target    │           │  LLVM Target    │
              │                 │           │                 │
              │ ┌─────────────┐ │           │ ┌─────────────┐ │
              │ │ ASM Library │ │           │ │ LLVM IR     │ │
              │ │ Java 23     │ │           │ │ (Future)    │ │
              │ │ Bytecode    │ │           │ │ Native Code │ │
              │ └─────────────┘ │           │ └─────────────┘ │
              └─────────────────┘           └─────────────────┘
                       │                             │
                       ▼                             ▼
              ┌─────────────────┐           ┌─────────────────┐
              │   .class        │           │   Native        │
              │   Files         │           │   Binary        │
              └─────────────────┘           └─────────────────┘
```

## 9. Generic Type System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Generic/Template Type System                         │
└─────────────────────────────────────────────────────────────────────────┘

                    ┌─────────────────┐
                    │ Generic Type    │
                    │ Definition      │
                    │                 │
                    │ List of type T  │
                    └─────────────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼                             ▼
     ┌─────────────────┐            ┌─────────────────┐
     │ Type Parameters │            │ Instantiation   │
     │                 │            │                 │
     │ T, K, V         │     ───▶   │ List of String  │
     │ (Conceptual)    │            │ (Concrete)      │
     └─────────────────┘            └─────────────────┘
                                             │
                                             ▼
                                    ┌─────────────────┐
                                    │ Method          │
                                    │ Substitution    │
                                    │                 │
                                    │ add(T) →        │
                                    │ add(String)     │
                                    └─────────────────┘

Resolution Chain:
1. EXPLICIT_TYPE_SYMBOL_DEFINITION → 2. FULL_RESOLUTION → 3. TEMPLATE_IR_GENERATION
```

## 10. Error Handling and Diagnostics Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      Error Processing Pipeline                          │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ EK9 Source  │───▶│   Parser    │───▶│ Error       │───▶│ Compilation │
│ (Syntax     │    │ (ANTLR4)    │    │ Listener    │    │ Reporter    │
│  Errors)    │    └─────────────┘    └─────────────┘    └─────────────┘
└─────────────┘                               │                   │
                                              ▼                   ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ Symbol      │───▶│ Resolution  │───▶│ Semantic    │───▶│ Error       │
│ Resolution  │    │ Phases      │    │ Errors      │    │ Aggregation │
│ (Semantic   │    │ (1-8)       │    └─────────────┘    └─────────────┘
│  Errors)    │    └─────────────┘                               │
└─────────────┘                                                  ▼
                                                        ┌─────────────┐
      ┌─────────────────────────────────────────────────│ LSP Mode    │
      ▼                                                 │             │
┌─────────────┐                                         │ Errors to   │
│ Command     │                                         │ Diagnostics │
│ Line Mode   │                                         └─────────────┘
│             │                                                │
│ Console     │                                                ▼
│ Output      │                                        ┌─────────────┐
└─────────────┘                                        │ IDE Client  │
                                                       │ (VS Code)   │
                                                       └─────────────┘
```

## Summary

These diagrams illustrate the sophisticated architecture of the EK9 compiler, showing:

1. **Modular Design**: Clean separation between modules with clear dependencies
2. **Multi-Phase Pipeline**: 22 distinct phases enabling targeted compilation and LSP optimization
3. **Symbol System**: Comprehensive type hierarchy supporting EK9's rich type system
4. **Scope Resolution**: Multi-level resolution with thread-safe cross-module access
5. **Bootstrap Process**: Dual-path loading for built-in types and Java integration
6. **LSP Integration**: Full IDE support while reusing compiler infrastructure
7. **Multi-Target Generation**: Extensible backend supporting multiple output formats
8. **Generic Support**: Complete template/generic type system with instantiation
9. **Error Handling**: Comprehensive error processing for both CLI and IDE modes

The architecture demonstrates excellent separation of concerns,
enabling the compiler to serve both traditional batch compilation and modern interactive IDE requirements while maintaining type safety and performance.