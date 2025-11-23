# Nested Guards - Future Design Work

## Status: DEFERRED
**Date:** 2025-01-18
**Decision:** Solutions exist (records/tuples), need real-world validation before standardizing pattern.

---

## Challenge

Real-world code often requires accessing nested optional values through multiple levels of object relationships. This is a common pattern in enterprise applications:

```ek9
// Common pattern: user → profile → settings → preferences → theme
user <- getUser(id)
if user?
  profile <- user.getProfile()
  if profile?
    settings <- profile.getSettings()
    if settings?
      preferences <- settings.getPreferences()
      if preferences?
        theme <- preferences.getTheme()
        if theme?
          process(theme)
```

**Problems with deep nesting:**
1. **Cognitive Load:** 5+ levels of nesting difficult to read and maintain
2. **Error Prone:** Easy to miss error handling at any level
3. **Verbose:** Lots of repetitive guard checks
4. **Unclear Intent:** Business logic buried in structural nesting

**Real-World Frequency:**
- Database access: Entity → Related Entity → Nested Property (3-5 levels)
- API responses: Response → Data → Nested Object → Field (3-4 levels)
- Configuration: Config → Section → Subsection → Setting (3-4 levels)
- UI state: State → Component → SubComponent → Value (2-4 levels)

**Open Questions:**
1. Is deep nesting (3-5 levels) idiomatic EK9?
2. Should there be a better composition pattern?
3. How do guards interact with nested optionals?
4. What's the recommended maximum nesting depth?
5. When to refactor vs when to accept nesting?

---

## Steve's Proposed Solutions

### Solution 1: Record with Constructor Composition

**Concept:** Encapsulate nested access logic inside a record's constructor, exposing a single guard point at the call site.

**Example:**
```ek9
defines record
  UserTheme
    userId as String
    theme as Theme?

    // Constructor handles all nested Optional access
    UserTheme()
      -> id as String

      // Nested guards happen inside constructor
      if user <- getUser(id)
        if profile <- user.getProfile()
          if settings <- profile.getSettings()
            if preferences <- settings.getPreferences()
              theme :=? preferences.getTheme()

// Call site - SINGLE guard!
if userTheme <- UserTheme(userId)
  if theme <- userTheme.theme
    process(theme)
```

**Benefits:**
- ✅ **Single guard at call site** - complexity hidden in constructor
- ✅ **Testable composition logic** - constructor can be unit tested
- ✅ **Reusable pattern** - same record used across application
- ✅ **Clear intent** - record name documents purpose
- ✅ **Type safety** - compiler enforces all access paths

**Drawbacks:**
- ⚠️ **Requires record definition** - more boilerplate for one-off usage
- ⚠️ **Constructor complexity** - nested logic still exists, just moved
- ⚠️ **May not fit all scenarios** - some patterns too dynamic for records

**When to use:**
- Repeated access pattern (used 3+ times in codebase)
- Clear business entity (e.g., "UserTheme", "OrderDetails")
- Stable structure (nested path unlikely to change frequently)

---

### Solution 2: Tuple (Dynamic Class) Composition

**Concept:** Use tuple for ad-hoc composition of multiple optionals without defining a formal record.

**Example:**
```ek9
// Tuple composes multiple optional fetches
getUserTheme()
  -> id as String
  <- themeResult as Tuple?

  if user <- getUser(id)
    if profile <- user.getProfile()
      if settings <- profile.getSettings()
        if preferences <- settings.getPreferences()
          if theme <- preferences.getTheme()
            // Return tuple with all intermediate values
            themeResult: Tuple(user, profile, settings, preferences, theme)

// Call site
if result <- getUserTheme(userId)
  user, profile, settings, preferences, theme <- result.unpack()
  process(theme)
  // Also have access to intermediate values if needed
```

**Benefits:**
- ✅ **No record definition needed** - dynamic composition
- ✅ **Flexible unpacking** - can destructure to get any level
- ✅ **Intermediate value access** - all levels available after unpack
- ✅ **Ad-hoc usage** - perfect for one-off patterns

**Drawbacks:**
- ⚠️ **Less type safety** - tuple elements not named
- ⚠️ **Positional unpacking** - easy to swap order accidentally
- ⚠️ **No documentation** - unclear what each position means
- ⚠️ **Harder to refactor** - changing structure breaks call sites

**When to use:**
- One-off access pattern (used 1-2 times)
- Exploratory code / prototyping
- Need access to intermediate values
- Temporary solution before extracting to record

---

### Solution 3: Streaming/Pipeline Pattern (Alternative)

**Concept:** Some nested access patterns might be better expressed as data transformations.

**Example:**
```ek9
// Instead of nested access, stream through transformations
theme <- cat [userId]
  | map with getUser
  | filter by isSet  // Only continue if user is set
  | map with getProfile
  | filter by isSet
  | map with getSettings
  | filter by isSet
  | map with getPreferences
  | filter by isSet
  | map with getTheme
  | head  // Take first (only) result
  | collect

if theme?
  process(theme)
```

**Benefits:**
- ✅ **Declarative** - clear transformation pipeline
- ✅ **Short-circuit** - stops at first unset value
- ✅ **Composable** - can add/remove stages easily
- ✅ **Functional style** - aligns with EK9's streaming philosophy

**Drawbacks:**
- ⚠️ **Overkill for simple cases** - 3 lines of nesting might not need pipeline
- ⚠️ **Less obvious** - requires understanding streaming semantics
- ⚠️ **Performance questions** - pipeline overhead vs direct access?

**When to use:**
- Long chains (5+ levels)
- Each level has additional processing/filtering
- Functional style preferred
- Chain might be reused with different stages

---

## Comparison of Solutions

| Aspect | Record Constructor | Tuple Composition | Pipeline Pattern |
|--------|-------------------|-------------------|------------------|
| **Boilerplate** | High (record definition) | Low (no definition) | Medium (pipeline syntax) |
| **Type Safety** | Excellent (named fields) | Good (positional) | Excellent (typed stages) |
| **Reusability** | Excellent | Poor | Good |
| **Clarity** | Excellent (intent clear) | Medium (positional) | Good (declarative) |
| **Flexibility** | Low (fixed structure) | High (ad-hoc) | High (composable) |
| **Learning Curve** | Low (familiar pattern) | Low (simple concept) | Medium (streaming knowledge) |
| **Best For** | Repeated patterns | One-off explorations | Long functional chains |

---

## Deferred Design Questions

### 1. Syntax Options for Native Support

**Question:** Should EK9 provide native syntax for nested guard composition?

**Option A: Multiple guards in one statement**
```ek9
// Hypothetical syntax - NOT YET DECIDED
if user <- getUser(id),
   profile <- user.getProfile(),
   settings <- profile.getSettings()
  process(settings)
```

**Option B: Chain operator (like Swift/Kotlin)**
```ek9
// Hypothetical syntax - NOT YET DECIDED
if settings <- getUser(id)?.getProfile()?.getSettings()
  process(settings)
```

**Option C: Dedicated composition construct**
```ek9
// Hypothetical syntax - NOT YET DECIDED
compose
  user <- getUser(id)
  profile <- user.getProfile()
  settings <- profile.getSettings()
then
  process(settings)
```

**Decision Criteria:**
- Must maintain "boring consistency" with existing guard syntax
- Must not introduce special cases
- Must work across all control flow constructs (if/switch/while/for/try)
- Must be clearly superior to record/tuple solutions

**Status:** Needs community feedback and real-world usage patterns before adding syntax.

---

### 2. Performance Considerations

**Question:** What are the performance implications of each approach?

**Unknowns:**
- Record constructor call overhead vs inline nesting?
- Tuple allocation/unpacking cost vs direct access?
- Pipeline streaming overhead vs manual iteration?
- JIT optimization opportunities for each pattern?

**Decision Criteria:**
- Should approach "zero overhead" for common cases
- Performance must be measurable and documented
- Benchmarks needed before standardizing pattern

**Status:** Needs profiling data from real-world usage.

---

### 3. Error Handling Integration

**Question:** How to know WHICH level of nesting failed?

**Challenge:**
```ek9
if userTheme <- UserTheme(userId)
  // userTheme is unset - but WHY?
  // Did getUser fail? getProfile? getSettings? getPreferences? getTheme?
  // No way to know!
```

**Possible Solutions:**
- Return `Result<Theme, String>` with error message indicating level
- Logging inside constructor (but side effects in constructors?)
- Separate validation method that returns detailed failure info
- Pattern matching on failure types

**Decision Criteria:**
- Must not break pure function semantics
- Must provide actionable debugging information
- Must integrate with existing error handling patterns

**Status:** Needs design work on error handling strategy.

---

### 4. Best Practices and Maximum Depth

**Question:** When is nesting acceptable vs when should it be refactored?

**Proposed Guidelines (DRAFT - NOT FINALIZED):**
- **1-2 levels:** Acceptable inline nesting - no refactoring needed
- **3-4 levels:** Consider record pattern if reused 3+ times
- **5+ levels:** Strong signal to refactor (record or pipeline)
- **8+ levels:** Code smell - likely architectural issue

**Open Questions:**
- Are these thresholds correct for EK9?
- Do they vary by domain (UI vs backend vs data processing)?
- Should compiler warn at certain depth?

**Status:** Needs validation from real-world EK9 codebases (when they exist).

---

## Next Steps (Future Work)

### Phase 1: Evidence Gathering (3-6 months)

1. **Collect Real-World Patterns:**
   - Survey common nested optional patterns in target domains:
     - Enterprise CRUD applications
     - API integration code
     - Data transformation pipelines
     - UI state management
   - Document frequency and depth of nesting
   - Identify most common failure modes

2. **Prototype Solutions:**
   - Implement record pattern in example code (5-10 examples)
   - Implement tuple pattern in example code (5-10 examples)
   - Implement pipeline pattern in example code (3-5 examples)
   - Document developer experience for each

3. **Performance Analysis:**
   - Benchmark record vs tuple vs pipeline vs inline nesting
   - Measure JIT optimization effectiveness
   - Profile memory allocation patterns
   - Identify performance cliffs

---

### Phase 2: Community Feedback (2-3 months)

1. **Developer Surveys:**
   - Which pattern feels most natural?
   - Which pattern would you reach for first?
   - Pain points with each approach?
   - Comparison to patterns in other languages (Java, Kotlin, Swift)

2. **AI Code Generation Testing:**
   - Can Claude/GPT generate correct nested patterns?
   - Which pattern has highest AI success rate?
   - Error patterns when AI gets it wrong?
   - Context window efficiency of each approach?

3. **Documentation Refinement:**
   - Write clear recipes for each pattern
   - Document when to use which approach
   - Create comprehensive examples
   - Integration with existing guard documentation

---

### Phase 3: Standardization (if needed, 1-2 months)

1. **Decision:**
   - Is native syntax needed, or are records/tuples sufficient?
   - If syntax needed, which option (multiple guards, chain operator, compose)?
   - Document rationale and tradeoffs

2. **Implementation:**
   - Grammar updates (if syntax added)
   - Compiler support
   - IR generation
   - Bytecode generation
   - Comprehensive testing

3. **Documentation:**
   - Add pattern to guard documentation
   - Update decision tree
   - Add to flowControl.html
   - Recipe book examples
   - Migration guide from inline nesting

---

## Related Documents

- **`GUARD_SEMANTICS_SUMMARY.md`** - Technical guard semantics reference
- **`EK9_GUARDS_AND_TRI_STATE_UNIFIED_SYSTEM.md`** - Complete guard system guide
- **`flowControl.html`** - User-facing control flow documentation
- **`EK9_LANGUAGE_EXAMPLES.md`** - Idiomatic EK9 code patterns

---

## Decision Rationale (Why Defer?)

**Why not solve this immediately?**

1. **Solutions Already Exist:**
   - Record pattern works today (no syntax changes needed)
   - Tuple pattern works today (no syntax changes needed)
   - Pipeline pattern works today (no syntax changes needed)
   - Developers have tools to handle nesting right now

2. **Need Real-World Data:**
   - Don't know if 3-5 levels is actually common in practice
   - Don't know which pattern developers prefer
   - Don't know performance characteristics at scale
   - Don't have enough examples to validate best practices

3. **Avoid Premature Optimization:**
   - Adding syntax without validation risks complexity
   - EK9 principle: "Boring consistency" > clever features
   - Better to standardize proven patterns than guess

4. **Focus on Foundation First:**
   - Guard system is revolutionary as-is
   - IR generation needs to be complete
   - Bytecode generation needs validation
   - LLVM backend needs implementation
   - Optimization comes after correctness

5. **Maintain Language Simplicity:**
   - Every new syntax increases learning curve
   - Every special case breaks systematic design
   - Records and tuples are general-purpose solutions
   - Don't need syntax sugar for every pattern

**Bottom Line:** The hard problem (guard safety) is solved. The convenience problem (nested access ergonomics) has working solutions. We can afford to wait for data before committing to syntax.

---

## Conclusion

Nested guard composition is a known challenge with multiple working solutions in EK9 today:
- **Records** for repeated, structured patterns
- **Tuples** for ad-hoc, flexible composition
- **Pipelines** for functional transformation chains

The decision to defer native syntax support is strategic:
- ✅ Avoid premature complexity
- ✅ Validate patterns with real-world usage
- ✅ Maintain "boring consistency" principle
- ✅ Focus compiler development on foundation

**When to revisit:** After 6-12 months of real-world EK9 usage, with concrete evidence that:
1. Nested optional access is frequent (10%+ of code patterns)
2. Record/tuple solutions are insufficient (developer complaints)
3. A clear syntax winner emerges from prototypes
4. AI code generation would benefit significantly

Until then: **Recommend record pattern for production, tuple for exploration, pipeline for functional style.**

**Status:** OPEN for future review, CLOSED for immediate implementation.
