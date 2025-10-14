# HTTP Service Fuzzing Tests

**Target**: HTTP service declarations with verbs and URI templates
**Total Tests Planned**: 200
**Target Phase**: FULL_RESOLUTION (Phase 6)

## Test Matrix

### Batch 1: Single HTTP Verbs (30 tests) - test001-030
- Root path `/` (6 tests, one per verb)
- Simple path `/users` (6 tests)
- With path param `/{id}` (6 tests)
- Invalid verb syntax (6 tests)
- Edge cases (6 tests)

### Batch 2: Multi-Verb Services (40 tests) - test031-070
- Two verbs (GET + POST) (10 tests)
- Three verbs (GET + POST + PUT) (10 tests)
- Four verbs (CRUD operations) (10 tests)
- Invalid combinations (10 tests)

### Batch 3: URI Template Variations (50 tests) - test071-120
- Single param `/{id}` (10 tests)
- Multiple params `/{userId}/{postId}` (10 tests)
- Nested paths `/{a}/sub/{b}` (10 tests)
- Complex patterns (10 tests)
- Invalid patterns (10 tests)

### Batch 4: Request/Response Bodies (40 tests) - test121-160
- POST with body (10 tests)
- PUT with body (10 tests)
- Response body variations (10 tests)
- Invalid body syntax (10 tests)

### Batch 5: Invalid/Malformed Syntax (40 tests) - test161-200
- Missing `as open` (8 tests)
- Missing `for` keyword (8 tests)
- Malformed method declaration (8 tests)
- Indentation errors (8 tests)
- Other syntax errors (8 tests)

## Current Status

**Total Tests**: 0/200
**Status**: EMPTY (awaiting Phase 1B generation)
