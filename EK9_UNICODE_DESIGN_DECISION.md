# EK9 Unicode Design Decision

## Design Philosophy

**EK9 identifiers are ASCII-only by design - not a limitation.**

### Rationale: English-Only Code Identifiers

All code identifiers (variables, functions, classes, etc.) must be written in **English using ASCII characters** (a-zA-Z, 0-9, underscore).

**Why:**
1. **Universal readability** - English is the lingua franca of programming
2. **Team collaboration** - Mixed-language codebases create maintenance nightmares
3. **Tool compatibility** - Many development tools expect ASCII identifiers
4. **Search and grep** - ASCII identifiers are easier to search across codebases
5. **Code review clarity** - Reviewers worldwide can read identifier names

**This is intentional, not a technical limitation.**

### Unicode IS Supported

Unicode is fully supported in:
- ✅ **String literals** - All Unicode characters, emoji, RTL text, etc.
- ✅ **Character literals** - Unicode characters in char types
- ✅ **Comments** - Documentation can use any language/script

### Testing Strategy

**Valid Unicode Mutations** (should compile successfully):
- Unicode in string literals (various scripts, emoji, combining characters)
- Unicode in comments
- RTL (right-to-left) text in strings
- Zero-width characters in strings
- Emoji in strings

**Invalid Unicode Mutations** (should fail by design):
- Unicode identifiers (function names, variable names, etc.)
- Location: `parseButFailCompile/phase0/badUnicodeIdentifiers/`
- Expected error: Lexer/parser rejects non-ASCII in identifiers

## Example

```ek9
defines module unicode.test

  defines function

    // ✅ VALID - English identifier, Unicode in strings
    greetUser()
      name <- "José García"           // ✅ Unicode in string
      greeting <- "Hello 👋"          // ✅ Emoji in string
      arabic <- "مرحبا"               // ✅ RTL text in string
      stdout.println(greeting)

    // ❌ INVALID - Unicode identifier (fails by design)
    función()                         // ❌ 'función' uses non-ASCII 'ó'
      valor <- 42                     // ❌ 'valor' - non-English word
```

## Implementation

**Grammar Definition** (`EK9LexerRules.g4:567-574`):
```antlr
Identifier
    : Letter LetterOrDigit*
    ;

fragment Letter
    : [a-zA-Z]                        // ASCII only by design
    ;

fragment LetterOrDigit
    : [a-zA-Z0-9_]                    // ASCII only by design
    ;
```

**String Literals**: Support full Unicode via standard string parsing.

## Related Standards

This approach aligns with:
- **Go language** - ASCII identifiers, Unicode strings
- **Rust language** - ASCII keywords, Unicode strings
- **Python 3** - Allows Unicode identifiers but discouraged in PEP 8
- **Java** - Allows Unicode identifiers but Oracle style guide recommends ASCII

EK9 takes the stricter approach: **enforce English identifiers at the language level**.

## Benefits

1. **No encoding issues** - ASCII identifiers eliminate charset problems
2. **Universal code review** - Anyone can review any EK9 code
3. **Clearer intent** - Forces consistent naming conventions
4. **Better tooling** - All tools work reliably with ASCII
5. **Internationalization where it matters** - Users see localized strings, not code

## Mutation Testing Plan

### Session 6a: Valid Unicode Mutations
**Location:** `fuzzCorpus/mutations/valid/unicode/`
- Test Unicode in strings (Latin extended, Cyrillic, CJK, Arabic, Hebrew, emoji)
- Test Unicode in comments
- Test edge cases (combining characters, zero-width, RTL)
- **Expected:** All tests compile successfully

### Session 6b: Invalid Unicode Identifiers
**Location:** `parseButFailCompile/phase0/badUnicodeIdentifiers/`
- Test non-ASCII identifier names
- Various scripts as identifiers (Cyrillic, CJK, Arabic, etc.)
- Emoji as identifiers
- **Expected:** All tests fail at parse time (by design)

This two-part approach validates:
1. Unicode IS supported where appropriate (strings/comments)
2. Unicode is correctly rejected in identifiers (by design)
