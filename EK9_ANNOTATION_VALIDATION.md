# EK9 Annotation Validation Process

**Date**: 2025-11-15
**Purpose**: Mandatory validation process for EK9 annotations in ek9-lang module

---

## ⚠️ CRITICAL MANDATORY PROCESS ⚠️

**ALWAYS REQUIRED**: Whenever you alter, add, or modify ANY EK9 annotation (`@Ek9Class`, `@Ek9Constructor`, `@Ek9Method`, `@Ek9Operator`) in ANY Java class in the `ek9-lang` module, you MUST follow this exact validation sequence.

**THIS IS NOT OPTIONAL - THE EK9 COMPILER DEPENDS ON SYNTACTICALLY CORRECT ANNOTATIONS**

## Why This Process is Required

- **Multi-module Dependency**: `compiler-main` depends on `ek9-lang`
- **Introspection Process**: `Ek9IntrospectedBootStrapTest` uses Java reflection to find `@Ek9Class` annotated classes
- **EK9 Code Generation**: Annotations are converted to EK9 source code and parsed
- **Syntax Validation**: The EK9 parser catches annotation formatting errors

**FAILURE TO FOLLOW THIS PROCESS WILL BREAK THE EK9 COMPILER BOOTSTRAP**

## The 3-Phase Validation Process

### Phase 1: Development

Make your changes and verify unit tests:

```bash
# Make changes to ek9-lang classes with @Ek9Class, @Ek9Constructor, @Ek9Method, @Ek9Operator annotations
# Add comprehensive unit tests
mvn test -pl ek9-lang  # Verify unit tests pass
```

### Phase 2: Annotation Validation

Run the complete validation sequence:

```bash
# STEP 1: Install ek9-lang to local Maven repository
mvn clean install -pl ek9-lang

# STEP 2: Rebuild compiler-main to pick up updated dependency
mvn clean compile -pl compiler-main

# STEP 3: Run bootstrap test to validate EK9 annotations
mvn test -Dtest=Ek9IntrospectedBootStrapTest -pl compiler-main
```

### Phase 3: Understand Results

**If Test Passes**: ✅ EK9 annotations are syntactically correct and properly formatted.

**If Test Fails**: ❌ The test will output the generated EK9 source code showing the exact syntax error:
- Look for the specific line and position in the error message
- Common issues:
  - Missing newlines in multi-line annotations (use `"""` triple quotes)
  - Incorrect indentation in EK9 syntax
  - Missing `as pure` qualifiers
  - Incorrect parameter/return type formatting

## Common Annotation Patterns

### Correct Operator Formatting

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

### Method with Parameters

✅ **CORRECT:**
```java
@Ek9Method("""
    methodName() as pure
      -> param as ParamType
      <- rtn as ReturnType?""")
```

### Constructor

✅ **CORRECT:**
```java
@Ek9Constructor("""
    ClassName()
      -> param1 as Type1
      -> param2 as Type2""")
```

### Complex Method with Multiple Parameters

✅ **CORRECT:**
```java
@Ek9Method("""
    process() as pure
      -> input as String
      -> config as Config
      <- result as Result?""")
```

## Key Formatting Rules

### 1. Always Use Triple Quotes for Multi-line

```java
// ✅ CORRECT
@Ek9Operator("""
    operator ? as pure
      <- rtn as Boolean?""")

// ❌ WRONG - single quotes don't allow proper formatting
@Ek9Operator("operator ? as pure\n  <- rtn as Boolean?")
```

### 2. Proper Indentation

```java
// ✅ CORRECT - proper EK9 indentation
@Ek9Method("""
    methodName() as pure
      -> param as Type    // 2-space indent
      <- rtn as Type?     // 2-space indent
    """)

// ❌ WRONG - no indentation
@Ek9Method("""
methodName() as pure
-> param as Type
<- rtn as Type?
""")
```

### 3. Include 'as pure' for Pure Methods

```java
// ✅ CORRECT - pure method marked
@Ek9Method("""
    getValue() as pure
      <- rtn as String?""")

// ❌ WRONG - missing 'as pure' qualifier
@Ek9Method("""
    getValue()
      <- rtn as String?""")
```

### 4. Return Type Must Use '?'

```java
// ✅ CORRECT - return type with '?'
@Ek9Method("""
    getValue() as pure
      <- rtn as String?""")

// ❌ WRONG - missing '?' on return type
@Ek9Method("""
    getValue() as pure
      <- rtn as String""")
```

## Quick Validation Command Sequence

For easy copy-paste when making EK9 annotation changes:

```bash
# Complete validation sequence - run these commands in order:
mvn clean install -pl ek9-lang
mvn clean compile -pl compiler-main
mvn test -Dtest=Ek9IntrospectedBootStrapTest -pl compiler-main
```

## Mandatory Validation Triggers

🚨 **YOU MUST RUN THE VALIDATION PROCESS WHENEVER YOU:**

- Add ANY new EK9 built-in types
- Modify ANY existing EK9 annotations in ANY Java class
- Add ANY new methods/operators to existing types
- Change ANY `@Ek9Operator`, `@Ek9Method`, `@Ek9Constructor`, or `@Ek9Class` annotations
- Before committing ANY changes to EK9 built-in types

## Common Errors and Solutions

### Error: "Unexpected token"

**Cause:** Missing newline or incorrect formatting

**Solution:** Use triple quotes and ensure proper line breaks:
```java
// ❌ WRONG
@Ek9Operator("operator ? as pure <- rtn as Boolean?")

// ✅ CORRECT
@Ek9Operator("""
    operator ? as pure
      <- rtn as Boolean?""")
```

### Error: "Expected '->' but found '<-'"

**Cause:** Missing parameter declaration before return value

**Solution:** Add parameter if needed, or verify syntax:
```java
// ❌ WRONG - missing method parentheses
@Ek9Method("""
    methodName as pure
      <- rtn as Type?""")

// ✅ CORRECT
@Ek9Method("""
    methodName() as pure
      <- rtn as Type?""")
```

### Error: "Type 'Type' not found"

**Cause:** Return type missing '?' qualifier

**Solution:** Add '?' to return type:
```java
// ❌ WRONG
@Ek9Method("""
    getValue() as pure
      <- rtn as String""")

// ✅ CORRECT
@Ek9Method("""
    getValue() as pure
      <- rtn as String?""")
```

## Integration with Development Workflow

### Before Committing

Always run the validation sequence before committing:

```bash
# 1. Verify unit tests
mvn test -pl ek9-lang

# 2. Run complete validation
mvn clean install -pl ek9-lang
mvn clean compile -pl compiler-main
mvn test -Dtest=Ek9IntrospectedBootStrapTest -pl compiler-main

# 3. If all pass, commit
git add .
git commit -m "Add/modify EK9 built-in type"
```

### After Modifying Multiple Types

Run validation after each type modification, not in batch:

```bash
# Modify Type1
mvn clean install -pl ek9-lang && mvn clean compile -pl compiler-main && mvn test -Dtest=Ek9IntrospectedBootStrapTest -pl compiler-main

# Modify Type2
mvn clean install -pl ek9-lang && mvn clean compile -pl compiler-main && mvn test -Dtest=Ek9IntrospectedBootStrapTest -pl compiler-main
```

**Why:** Catches errors early, making them easier to fix.

## See Also

- **`EK9_DEVELOPMENT_CONTEXT.md`** - Built-in type development patterns
- **`EK9_CODING_STANDARDS.md`** - Java coding standards for EK9 project
- **`Ek9IntrospectedBootStrapTest.java`** - The bootstrap test that validates annotations

---

**Last Updated**: 2025-11-15
