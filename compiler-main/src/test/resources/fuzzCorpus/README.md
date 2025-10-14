# EK9 Compiler Fuzzing Corpus

This directory contains fuzzing test corpus for validating EK9 compiler robustness.

## Structure

- **httpServices/** - HTTP service declaration tests (200 tests planned)
- **textInterpolation/** - Text block interpolation tests (150 tests planned)
- **complexLiterals/** - Complex literal format tests (150 tests planned)

## Test File Format

Every `.ek9` test file follows this standard:

```ek9
#!ek9
defines module {area}.fuzz.test{number}

  // @ExpectPhase: FULL_RESOLUTION (or PARSING, SYMBOL_DEFINITION, etc.)
  // @TestDescription: Brief description of what this test validates

  [test content]

//EOF
```

## Naming Convention

Format: `test{number}_{category}_{description}.ek9`

Examples:
- `test001_single_get_root_path.ek9`
- `test045_multi_verb_get_post_put.ek9`
- `test111_uri_invalid_unclosed.ek9`

## Purpose

These tests verify:
- ✅ No compiler crashes (JVM exceptions)
- ✅ No infinite loops (tests complete within timeout)
- ✅ Clear error messages for invalid constructs
- ✅ Graceful handling of edge cases

## Test Execution

```bash
# Run only fuzzing tests
mvn test -P fuzz

# Run specific category
mvn test -P fuzz -Dtest=HttpServiceFuzzTest

# Run all tests including fuzzing
mvn test -P all
```

## Status

- **Phase 1A**: Infrastructure setup (IN PROGRESS)
- **Phase 1B**: Corpus generation (PENDING)
- **Phase 1C**: Execution and analysis (PENDING)
