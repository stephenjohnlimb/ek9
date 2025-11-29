# Error Recovery and Testing Issues

## Document Purpose
This document captures two semantic errors that have **correct detection logic** but present challenges for testing or require improved error recovery.

---

## 1. INVALID_VALUE - Error Recovery Issue

### Status
- **Error Detection**: ✅ Working correctly
- **Error Recovery**: ⚠️ Incomplete - causes CompilerException

### Description
`INVALID_VALUE` is triggered when a text construct language code doesn't match the pattern `[a-z]+(_[A-Z]+)?`.

**Valid examples**: `"en"`, `"de"`, `"en_GB"`, `"fr_FR"`
**Invalid examples**: `"EN"` (uppercase base), `"en-US"` (hyphen), `"EN_US"` (uppercase base)

### Error Detection (Working Correctly)

**Location**: `TextLanguageExtraction.java:26-34`
```java
final var m = languagePattern.matcher(stringLitContext.getText());
if (m.find()) {
  return m.group(1);
} else {
  errorListener.semanticError(stringLitContext.start, "Language must be \"[a-z]+(+_[A-Z]+)?\"",
      ErrorListener.SemanticClassification.INVALID_VALUE);
}
return null;  // Returns null when pattern doesn't match
```

### Error Recovery Problem

**Flow when invalid language code is encountered**:
1. `TextLanguageExtraction.apply()` detects invalid format → triggers **INVALID_VALUE** → returns `null`
2. `DefinitionListener.enterTextBlock:402` stores null: `currentTextBlockLanguage = textLanguageExtraction.apply(...)`
3. `TextFactory.newText:40-41` receives null → **skips text declaration creation**:
   ```java
   //an error will have been created for the developer as language does not conform.
   if (forLanguage != null) {
     // Create text declaration...
   }
   // If null, nothing is created!
   ```
4. Parser **still processes text body** (e.g., `TestText` with methods)
5. `DefinitionListener.exitTextBodyDeclaration:439` calls `ensureTextBodyIsInSuper()`
6. **CompilerException thrown**: `"Only expecting text body to be using within a textDeclaration scope with a super"`

### Root Cause
Text body processing continues even though parent text declaration was never created due to invalid language code.

### Impact
- User sees **INVALID_VALUE** error (correct)
- But compiler also crashes with **CompilerException** (internal error)
- Test framework cannot validate with `@Error` directives due to crash

### Potential Solutions

**Option A**: Skip text body processing when `currentTextBlockLanguage == null`
**Option B**: Create text declaration even with null language (mark as invalid)
**Option C**: Convert CompilerException to semantic error when super is missing
**Option D**: Accept current behavior (INVALID_VALUE error is shown, crash is acceptable for invalid state)

### Test Case (Currently Fails with CompilerException)
```ek9
#!ek9
defines module bad.text.language

  //Invalid language code format - should be lowercase with optional _UPPERCASE
  @Error: SYMBOL_DEFINITION: INVALID_VALUE
  defines text for "EN"  // Invalid: uppercase base

    TestText
      welcome()
        "Welcome"

//EOF
```

**Current Result**:
- ✅ INVALID_VALUE error is triggered correctly
- ❌ CompilerException thrown during text body processing

---

## 2. INVALID_MODULE_NAME - Line 0 Token Issue

### Status
- **Error Detection**: ✅ Working correctly
- **Test Framework**: ⚠️ Cannot match @Error directives on line 0

### Description
`INVALID_MODULE_NAME` is triggered when user attempts to use reserved module names (`org.ek9.lang` or `org.ek9.math`).

### Error Detection (Working Correctly)

**Location**: `SymbolDefinition.java` (approximately line 65-70)
```java
if (notBootStrapping
    && (EK9TypeNames.EK9_LANG.equals(parsedModule.getModuleName())
    || EK9TypeNames.EK9_MATH.equals(parsedModule.getModuleName()))) {

  source.getErrorListener().semanticError(new Ek9Token(parsedModule.getModuleName()),
      "", ErrorListener.SemanticClassification.INVALID_MODULE_NAME);
}
```

### Testing Problem

**Issue**: Error token is created with `new Ek9Token(parsedModule.getModuleName())` which **doesn't have source location** (line 0).

**Test Case (Fails @Error Directive Matching)**:
```ek9
#!ek9
//Using reserved namespace org.ek9.lang - should fail
@Error: SYMBOL_DEFINITION: INVALID_MODULE_NAME
defines module org.ek9.lang

//EOF
```

**Current Result**:
- ✅ INVALID_MODULE_NAME error is triggered correctly
- ❌ Test fails: `'Expecting @Error directive': as line: 0 has error: 'INVALID_MODULE_NAME' but the directive is missing`

### Root Cause
`@Error` directive matching requires line number to match error location. Module-level errors use tokens without proper source location (line 0).

### Potential Solutions

**Option A**: Create token with proper source location from module declaration context
**Option B**: Special handling in test framework for module-level errors (line 0)
**Option C**: Alternative test approach for module-level validation errors
**Option D**: Document as "tested manually" (error logic is correct, @Error matching not applicable)

---

## Recommendations

### INVALID_VALUE
**Recommended**: Option A - Skip text body processing when language is null
**Rationale**: Most user-friendly - show INVALID_VALUE error without internal crash

### INVALID_MODULE_NAME
**Recommended**: Option A - Fix token creation to include source location
**Rationale**: Enables proper @Error directive testing for module-level errors

---

## Status
- **Date Identified**: 2025-11-18
- **Identified By**: Claude (Session: Error cleanup continuation)
- **Requires**: Steve's decision on solution approach
- **Priority**: Low (errors are detected correctly, issues are with recovery/testing)
