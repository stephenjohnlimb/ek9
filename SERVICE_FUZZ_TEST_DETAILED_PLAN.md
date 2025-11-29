# EK9 Service Fuzz Testing: Comprehensive Detailed Plan

**Created:** 2025-11-12
**Scope:** Complete service validation coverage across compilation phases 1-6
**Status:** Planning Phase

---

## 📊 Executive Summary

### Current Coverage Analysis

| Metric | Count | Percentage |
|--------|-------|------------|
| **Total SERVICE Errors** | 23 | 100% |
| **Already Tested** | 16 | 69.6% |
| **ZERO Coverage** | 7 | 30.4% |
| **Existing Test Files** | 4 | - |
| **Proposed New Tests** | 35 | - |

### Gap Analysis Summary

**Phase 1 (SYMBOL_DEFINITION):** 3/6 tested (50% coverage)
**Phase 2 (EXPLICIT_TYPE_SYMBOL_DEFINITION):** 13/17 tested (76.5% coverage)
**Total Gap:** 7 untested errors + 28 edge case scenarios = **35 new test files needed**

---

## 🎯 Complete Error Inventory with Test Status

### Phase 1: SYMBOL_DEFINITION (6 errors)

#### ✅ TESTED (3 errors)

1. **SERVICE_URI_WITH_VARS_NOT_SUPPORTED**
   - Current Coverage: 1 test (serviceDefinitionWithErrors.ek9:13)
   - Additional Scenarios Needed: 2

2. **SERVICE_HTTP_ACCESS_NOT_SUPPORTED**
   - Current Coverage: 2 tests (serviceDefinitionWithErrors.ek9, variousVariableOnlyDeclarationErrors.ek9)
   - Additional Scenarios Needed: 2

3. **SERVICE_OPERATOR_NOT_SUPPORTED**
   - Current Coverage: 1 test (serviceDefinitionWithErrors.ek9:23)
   - Additional Scenarios Needed: 3

#### 🔴 UNTESTED (3 errors)

4. **SERVICE_HTTP_CACHING_NOT_SUPPORTED** ⚠️ **ZERO COVERAGE**
   - Tests Needed: 2

5. **SERVICE_HTTP_ACCESS_NAME_NOT_SUPPORTED** ⚠️ **ZERO COVERAGE**
   - Tests Needed: 2

6. **NO_VERB_REQUIRED_WITH_SERVICE_OPERATOR** ⚠️ **ZERO COVERAGE**
   - Tests Needed: 3

---

### Phase 2: EXPLICIT_TYPE_SYMBOL_DEFINITION (17 errors)

#### ✅ TESTED (13 errors)

7. **SERVICE_HTTP_PATH_PARAM_INVALID**
   - Current Coverage: 2 tests (badServiceMethodArgumentType.ek9:95, 102)
   - Additional Scenarios Needed: 2

8. **SERVICE_HTTP_PARAM_NEEDS_QUALIFIER**
   - Current Coverage: 1 test (badServiceMethodArgumentType.ek9:114)
   - Additional Scenarios Needed: 2

9. **SERVICE_HTTP_PARAM_QUALIFIER_NOT_ALLOWED**
   - Current Coverage: 1 test (badServiceMethodArgumentType.ek9:121)
   - Additional Scenarios Needed: 2

10. **SERVICE_HTTP_PATH_DUPLICATED**
    - Current Coverage: 4 tests (badServiceMethodArgumentType.ek9)
    - Additional Scenarios Needed: 3

11. **SERVICE_HTTP_PATH_PARAM_COUNT_INVALID**
    - Current Coverage: 1 test (badServiceMethodArgumentType.ek9:107)
    - Additional Scenarios Needed: 2

12. **SERVICE_WITH_NO_BODY_PROVIDED**
    - Current Coverage: 1 test (badServiceMethodReturnType.ek9:40)
    - Additional Scenarios Needed: 1

13. **SERVICE_INCOMPATIBLE_RETURN_TYPE**
    - Current Coverage: 1 test (badServiceMethodReturnType.ek9:28)
    - Additional Scenarios Needed: 3

14. **SERVICE_MISSING_RETURN**
    - Current Coverage: 1 test (badServiceMethodReturnType.ek9:34)
    - Additional Scenarios Needed: 2

15. **SERVICE_INCOMPATIBLE_PARAM_TYPE**
    - Current Coverage: 1 test (badServiceMethodArgumentType.ek9:65)
    - Additional Scenarios Needed: 3

16. **SERVICE_INCOMPATIBLE_PARAM_TYPE_REQUEST**
    - Current Coverage: 1 test (badServiceMethodArgumentType.ek9:72)
    - Additional Scenarios Needed: 1

17. **SERVICE_REQUEST_BY_ITSELF**
    - Current Coverage: 1 test (badServiceMethodArgumentType.ek9:86)
    - Additional Scenarios Needed: 1

18. **SERVICE_INCOMPATIBLE_PARAM_TYPE_NON_REQUEST**
    - Current Coverage: 1 test (badServiceMethodArgumentType.ek9:79)
    - Additional Scenarios Needed: 1

19. **METHOD_DUPLICATED** (service context)
    - Current Coverage: 5 tests (badDuplicateAndModifierServiceMethods.ek9)
    - Additional Scenarios Needed: 0 (well covered)

#### 🔴 UNTESTED (4 errors)

20. **SERVICE_HTTP_HEADER_MISSING** ⚠️ **ZERO COVERAGE**
    - Tests Needed: 2

21. **SERVICE_HTTP_HEADER_INVALID** ⚠️ **ZERO COVERAGE**
    - Tests Needed: 3

22. **SERVICE_HTTP_PATH_ASSUMED_BUT_INVALID** ⚠️ **ZERO COVERAGE**
    - Tests Needed: 2

23. **SERVICE_HTTP_BODY_MAPPING_NOT_ALLOWED** ⚠️ **ZERO COVERAGE**
    - Tests Needed: 2

---

## 📁 Proposed Test File Structure

### FuzzCorpus Directory Organization

```
compiler-main/src/test/resources/fuzzCorpus/serviceValidation/
├── Phase 1: Service Definition Errors (10 files)
│   ├── service_uri_multiple_variables.ek9
│   ├── service_uri_nested_variables.ek9
│   ├── service_http_access_in_constructor.ek9
│   ├── service_http_access_in_regular_method.ek9
│   ├── service_operator_invalid_bitwise.ek9
│   ├── service_operator_invalid_logical.ek9
│   ├── service_operator_invalid_comparison.ek9
│   ├── service_caching_in_definition.ek9
│   ├── service_caching_in_operation.ek9
│   ├── service_access_name_mapping.ek9
│   ├── service_access_name_custom.ek9
│   ├── service_operator_with_get_verb.ek9
│   ├── service_operator_with_post_verb.ek9
│   └── service_operator_with_delete_verb.ek9
│
├── Phase 2: HTTP Parameter Errors (12 files)
│   ├── service_header_missing_name.ek9
│   ├── service_header_empty_name.ek9
│   ├── service_header_invalid_characters.ek9
│   ├── service_header_reserved_name.ek9
│   ├── service_header_numeric_name.ek9
│   ├── service_body_with_qualifier.ek9
│   ├── service_content_with_qualifier.ek9
│   ├── service_path_assumed_no_variables.ek9
│   ├── service_path_assumed_wrong_name.ek9
│   ├── service_param_type_list.ek9
│   ├── service_param_type_dict.ek9
│   ├── service_param_type_custom_class.ek9
│   └── service_param_type_optional.ek9
│
├── Phase 2: URI Path Errors (5 files)
│   ├── service_path_duplicate_complex.ek9
│   ├── service_path_duplicate_nested.ek9
│   ├── service_path_duplicate_reordered.ek9
│   ├── service_path_count_too_many.ek9
│   └── service_path_count_too_few.ek9
│
└── Phase 2: Return Type Errors (8 files)
    ├── service_return_type_integer.ek9
    ├── service_return_type_string.ek9
    ├── service_return_type_void.ek9
    ├── service_return_type_custom_class.ek9
    ├── service_missing_return_operator.ek9
    ├── service_missing_return_method.ek9
    ├── service_no_body_get_method.ek9
    └── service_no_body_post_operator.ek9
```

**Total:** 35 new test files across 4 categories

---

## 🧪 Detailed Test Scenarios

### Category 1: Service Definition Errors (Phase 1) - 14 Files

#### Test File 1: `service_uri_multiple_variables.ek9`

**Error:** SERVICE_URI_WITH_VARS_NOT_SUPPORTED
**Phase:** SYMBOL_DEFINITION
**Purpose:** Service-level URI cannot have path variables

```ek9
#!ek9
defines module fuzztest.service.uri.multiple

  defines service
    <?- Service URI with multiple path variables -?>
    @Error: SYMBOL_DEFINITION: SERVICE_URI_WITH_VARS_NOT_SUPPORTED
    ApiService :/api/{version}/{tenant}

      index() :/
        <- response as HTTPResponse: () with trait of HTTPResponse
          override content()
            <- rtn as String: "OK"
```

**Rationale:** Service base URI should be fixed (no variables). Variables are only allowed in operation URIs.

---

#### Test File 2: `service_uri_nested_variables.ek9`

**Error:** SERVICE_URI_WITH_VARS_NOT_SUPPORTED
**Phase:** SYMBOL_DEFINITION
**Purpose:** Nested path structure with variables in service URI

```ek9
#!ek9
defines module fuzztest.service.uri.nested

  defines service
    <?- Service URI with nested variable structure -?>
    @Error: SYMBOL_DEFINITION: SERVICE_URI_WITH_VARS_NOT_SUPPORTED
    ResourceService :/resources/{resourceType}/items/{itemId}

      get() :/
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** More complex nested variable pattern to ensure comprehensive detection.

---

#### Test File 3: `service_http_access_in_constructor.ek9`

**Error:** SERVICE_HTTP_ACCESS_NOT_SUPPORTED
**Phase:** SYMBOL_DEFINITION
**Purpose:** HTTP access verbs not allowed in service constructors

```ek9
#!ek9
defines module fuzztest.service.access.constructor

  defines service
    UserService :/users

      <?- Constructor with HTTP access parameter -?>
      UserService()
        @Error: SYMBOL_DEFINITION: SERVICE_HTTP_ACCESS_NOT_SUPPORTED
        -> config as String :=: QUERY "config"
```

**Rationale:** Constructors cannot have HTTP parameter bindings (REQUEST, QUERY, HEADER, PATH, CONTENT).

---

#### Test File 4: `service_http_access_in_regular_method.ek9`

**Error:** SERVICE_HTTP_ACCESS_NOT_SUPPORTED
**Phase:** SYMBOL_DEFINITION
**Purpose:** HTTP access verbs only allowed in service operations, not regular methods

```ek9
#!ek9
defines module fuzztest.service.access.method

  defines service
    DataService :/data

      <?- Private helper method with HTTP access -?>
      private processRequest()
        @Error: SYMBOL_DEFINITION: SERVICE_HTTP_ACCESS_NOT_SUPPORTED
        -> payload as String :=: CONTENT
        <- result as String: payload

      <?- Public service operation (valid) -?>
      create() as POST :/
        -> content as String :=: CONTENT
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** Only service operations (methods with HTTP verbs/URIs) can bind HTTP parameters.

---

#### Test File 5: `service_operator_invalid_bitwise.ek9`

**Error:** SERVICE_OPERATOR_NOT_SUPPORTED
**Phase:** SYMBOL_DEFINITION
**Purpose:** Bitwise operators not supported in services

```ek9
#!ek9
defines module fuzztest.service.operator.bitwise

  defines service
    BitwiseService :/bitwise

      <?- Invalid bitwise AND operator -?>
      @Error: SYMBOL_DEFINITION: SERVICE_OPERATOR_NOT_SUPPORTED
      operator & :/
        -> request as HTTPRequest :=: REQUEST
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** Only REST-style operators (+, +=, -, -=, :^:, :~:, ?) are supported.

---

#### Test File 6: `service_operator_invalid_logical.ek9`

**Error:** SERVICE_OPERATOR_NOT_SUPPORTED
**Phase:** SYMBOL_DEFINITION
**Purpose:** Logical operators not supported in services

```ek9
#!ek9
defines module fuzztest.service.operator.logical

  defines service
    LogicalService :/logical

      <?- Invalid logical OR operator -?>
      @Error: SYMBOL_DEFINITION: SERVICE_OPERATOR_NOT_SUPPORTED
      operator or :/
        -> request as HTTPRequest :=: REQUEST
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** Logical operators don't map to HTTP verbs.

---

#### Test File 7: `service_operator_invalid_comparison.ek9`

**Error:** SERVICE_OPERATOR_NOT_SUPPORTED
**Phase:** SYMBOL_DEFINITION
**Purpose:** Comparison operators not supported in services

```ek9
#!ek9
defines module fuzztest.service.operator.comparison

  defines service
    CompareService :/compare

      <?- Invalid comparison operator -?>
      @Error: SYMBOL_DEFINITION: SERVICE_OPERATOR_NOT_SUPPORTED
      operator <=> :/
        -> request as HTTPRequest :=: REQUEST
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** Comparison operators don't have HTTP verb mappings.

---

#### Test File 8: `service_caching_in_definition.ek9`

**Error:** SERVICE_HTTP_CACHING_NOT_SUPPORTED ⚠️ **NEW COVERAGE**
**Phase:** SYMBOL_DEFINITION
**Purpose:** HTTP caching directives not supported in service definition

```ek9
#!ek9
defines module fuzztest.service.caching.definition

  defines service
    <?- Attempting to specify caching at service level -?>
    @Error: SYMBOL_DEFINITION: SERVICE_HTTP_CACHING_NOT_SUPPORTED
    CachedService :/api with caching

      index() :/
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** If grammar supports `with caching` or similar caching directive, it should be rejected (or feature not implemented).

**NOTE:** Requires investigation of EK9 grammar for `httpCaching` rules. If grammar doesn't support this, error may be unused.

---

#### Test File 9: `service_caching_in_operation.ek9`

**Error:** SERVICE_HTTP_CACHING_NOT_SUPPORTED ⚠️ **NEW COVERAGE**
**Phase:** SYMBOL_DEFINITION
**Purpose:** HTTP caching directives not supported in service operations

```ek9
#!ek9
defines module fuzztest.service.caching.operation

  defines service
    ApiService :/api

      <?- Attempting to specify caching on operation -?>
      @Error: SYMBOL_DEFINITION: SERVICE_HTTP_CACHING_NOT_SUPPORTED
      getData() as GET for :/ with cache
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** Caching should be controlled via HTTPResponse.cacheControl() method, not declarative syntax.

---

#### Test File 10: `service_access_name_mapping.ek9`

**Error:** SERVICE_HTTP_ACCESS_NAME_NOT_SUPPORTED ⚠️ **NEW COVERAGE**
**Phase:** SYMBOL_DEFINITION
**Purpose:** HTTP access name mapping not supported

```ek9
#!ek9
defines module fuzztest.service.access.name.mapping

  defines service
    MappedService :/mapped

      <?- Attempting to map HTTP method name -?>
      @Error: SYMBOL_DEFINITION: SERVICE_HTTP_ACCESS_NAME_NOT_SUPPORTED
      index() as HTTP "CUSTOM" for :/
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** If grammar supports custom HTTP method names (like HTTP "CUSTOM"), it should be rejected.

**NOTE:** Requires grammar investigation. If not supported, error may be unused.

---

#### Test File 11: `service_access_name_custom.ek9`

**Error:** SERVICE_HTTP_ACCESS_NAME_NOT_SUPPORTED ⚠️ **NEW COVERAGE**
**Phase:** SYMBOL_DEFINITION
**Purpose:** Custom HTTP verbs not supported

```ek9
#!ek9
defines module fuzztest.service.access.name.custom

  defines service
    CustomService :/custom

      <?- Using non-standard HTTP verb -?>
      @Error: SYMBOL_DEFINITION: SERVICE_HTTP_ACCESS_NAME_NOT_SUPPORTED
      process() as PATCH for :/process
        -> content as String :=: CONTENT
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** Only standard HTTP verbs (GET, POST, PUT, DELETE, HEAD, OPTIONS) should be allowed.

**NOTE:** PATCH is actually standard. May need to use truly custom verb like LINK, UNLINK, or PROPFIND.

---

#### Test File 12: `service_operator_with_get_verb.ek9`

**Error:** NO_VERB_REQUIRED_WITH_SERVICE_OPERATOR ⚠️ **NEW COVERAGE**
**Phase:** SYMBOL_DEFINITION
**Purpose:** Operator implies HTTP verb - explicit verb redundant

```ek9
#!ek9
defines module fuzztest.service.operator.get.redundant

  defines service
    RedundantService :/redundant

      <?- Operator ? implies GET - explicit GET is redundant -?>
      @Error: SYMBOL_DEFINITION: NO_VERB_REQUIRED_WITH_SERVICE_OPERATOR
      operator ? as GET :/check
        -> id as String
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** Operator `?` already maps to GET. Adding `as GET` is redundant.

---

#### Test File 13: `service_operator_with_post_verb.ek9`

**Error:** NO_VERB_REQUIRED_WITH_SERVICE_OPERATOR ⚠️ **NEW COVERAGE**
**Phase:** SYMBOL_DEFINITION
**Purpose:** Operator += implies POST - explicit verb redundant

```ek9
#!ek9
defines module fuzztest.service.operator.post.redundant

  defines service
    PostService :/posts

      <?- Operator += implies POST - explicit POST is redundant -?>
      @Error: SYMBOL_DEFINITION: NO_VERB_REQUIRED_WITH_SERVICE_OPERATOR
      operator += as POST :/
        -> request as HTTPRequest :=: REQUEST
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** Operator `+=` maps to POST. Specifying `as POST` is redundant.

---

#### Test File 14: `service_operator_with_delete_verb.ek9`

**Error:** NO_VERB_REQUIRED_WITH_SERVICE_OPERATOR ⚠️ **NEW COVERAGE**
**Phase:** SYMBOL_DEFINITION
**Purpose:** Operator -= implies DELETE - explicit verb redundant

```ek9
#!ek9
defines module fuzztest.service.operator.delete.redundant

  defines service
    DeleteService :/items

      <?- Operator -= implies DELETE - explicit DELETE is redundant -?>
      @Error: SYMBOL_DEFINITION: NO_VERB_REQUIRED_WITH_SERVICE_OPERATOR
      operator -= as DELETE :/{id}
        -> id as String
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** Operator `-=` maps to DELETE. Specifying `as DELETE` is redundant.

---

### Category 2: HTTP Header Parameter Errors (Phase 2) - 5 Files

#### Test File 15: `service_header_missing_name.ek9`

**Error:** SERVICE_HTTP_HEADER_MISSING ⚠️ **NEW COVERAGE**
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** HEADER access requires valid header name

```ek9
#!ek9
defines module fuzztest.service.header.missing

  defines service
    HeaderService :/headers

      check() as GET :/check
        <?- HEADER without header name -?>
        @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_HTTP_HEADER_MISSING
        -> userAgent as String :=: HEADER
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** HEADER requires a qualifier like `HEADER "User-Agent"`.

---

#### Test File 16: `service_header_empty_name.ek9`

**Error:** SERVICE_HTTP_HEADER_MISSING ⚠️ **NEW COVERAGE**
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** HEADER with empty name string

```ek9
#!ek9
defines module fuzztest.service.header.empty

  defines service
    EmptyHeaderService :/empty

      check() as GET :/check
        <?- HEADER with empty string -?>
        @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_HTTP_HEADER_MISSING
        -> header as String :=: HEADER ""
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** Header name cannot be empty string.

---

#### Test File 17: `service_header_invalid_characters.ek9`

**Error:** SERVICE_HTTP_HEADER_INVALID ⚠️ **NEW COVERAGE**
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** HTTP header names must follow RFC 7230 token rules

```ek9
#!ek9
defines module fuzztest.service.header.invalid.chars

  defines service
    InvalidHeaderService :/invalid

      check() as GET :/check
        <?- HEADER with invalid characters (@, spaces, etc.) -?>
        @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_HTTP_HEADER_INVALID
        -> header as String :=: HEADER "Invalid@Header-Name"
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** RFC 7230: header names are tokens (alphanumeric, -, _, etc.). Special chars like @, spaces not allowed.

---

#### Test File 18: `service_header_reserved_name.ek9`

**Error:** SERVICE_HTTP_HEADER_INVALID ⚠️ **NEW COVERAGE**
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** Certain HTTP headers may be reserved/managed by server

```ek9
#!ek9
defines module fuzztest.service.header.reserved

  defines service
    ReservedHeaderService :/reserved

      check() as GET :/check
        <?- Using reserved/managed header like Content-Length -?>
        @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_HTTP_HEADER_INVALID
        -> contentLength as Integer :=: HEADER "Content-Length"
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** Headers like Content-Length, Transfer-Encoding managed by server infrastructure.

**NOTE:** This may not be enforced - needs validation against compiler implementation.

---

#### Test File 19: `service_header_numeric_name.ek9`

**Error:** SERVICE_HTTP_HEADER_INVALID ⚠️ **NEW COVERAGE**
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** HTTP header names cannot be purely numeric

```ek9
#!ek9
defines module fuzztest.service.header.numeric

  defines service
    NumericHeaderService :/numeric

      check() as GET :/check
        <?- HEADER with numeric name -?>
        @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_HTTP_HEADER_INVALID
        -> header as String :=: HEADER "12345"
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** Header names should be descriptive tokens, not numeric strings.

---

### Category 3: HTTP Body Parameter Errors (Phase 2) - 2 Files

#### Test File 20: `service_body_with_qualifier.ek9`

**Error:** SERVICE_HTTP_BODY_MAPPING_NOT_ALLOWED ⚠️ **NEW COVERAGE**
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** BODY/CONTENT parameters cannot have name qualifiers

```ek9
#!ek9
defines module fuzztest.service.body.qualifier

  defines service
    BodyService :/body

      create() as POST :/
        <?- CONTENT with name qualifier (not allowed) -?>
        @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_HTTP_BODY_MAPPING_NOT_ALLOWED
        -> payload as String :=: CONTENT "bodyName"
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** Unlike QUERY/HEADER/PATH, CONTENT/BODY doesn't need a name (entire body is mapped).

---

#### Test File 21: `service_content_with_qualifier.ek9`

**Error:** SERVICE_HTTP_BODY_MAPPING_NOT_ALLOWED ⚠️ **NEW COVERAGE**
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** Alternative BODY keyword with qualifier

```ek9
#!ek9
defines module fuzztest.service.content.qualifier

  defines service
    ContentService :/content

      update() as PUT :/{id}
        -> id as String
        <?- BODY with name qualifier (not allowed) -?>
        @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_HTTP_BODY_MAPPING_NOT_ALLOWED
        -> data as String :=: BODY "payload"
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** BODY (if supported as keyword) also shouldn't have qualifiers.

**NOTE:** Check if BODY is valid keyword or only CONTENT is used.

---

### Category 4: HTTP Path Assumption Errors (Phase 2) - 2 Files

#### Test File 22: `service_path_assumed_no_variables.ek9`

**Error:** SERVICE_HTTP_PATH_ASSUMED_BUT_INVALID ⚠️ **NEW COVERAGE**
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** PATH assumed but URI has no path variables

```ek9
#!ek9
defines module fuzztest.service.path.assumed.none

  defines service
    NoPathService :/fixed

      get() as GET :/static/resource.html
        <?- Assuming PATH but URI has no variables -?>
        @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_HTTP_PATH_ASSUMED_BUT_INVALID
        -> id as String
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** Parameter `id` without explicit `:=:` qualifier assumes PATH, but URI has no `{id}`.

---

#### Test File 23: `service_path_assumed_wrong_name.ek9`

**Error:** SERVICE_HTTP_PATH_ASSUMED_BUT_INVALID ⚠️ **NEW COVERAGE**
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** PATH assumed but name doesn't match any URI variable

```ek9
#!ek9
defines module fuzztest.service.path.assumed.wrong

  defines service
    WrongPathService :/items

      getItem() as GET :/{itemId}
        <?- Assuming PATH but name doesn't match {itemId} -?>
        @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_HTTP_PATH_ASSUMED_BUT_INVALID
        -> wrongName as String
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** Parameter `wrongName` assumes PATH but URI only has `{itemId}`.

---

### Category 5: Service Parameter Type Errors (Phase 2) - 4 Files

#### Test File 24: `service_param_type_list.ek9`

**Error:** SERVICE_INCOMPATIBLE_PARAM_TYPE
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** Generic collection types not supported in service parameters

```ek9
#!ek9
defines module fuzztest.service.param.list

  defines service
    ListParamService :/lists

      create() as POST :/
        <?- List type not supported in HTTP parameters -?>
        @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_INCOMPATIBLE_PARAM_TYPE
        -> items as List of String :=: QUERY "items"
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** Only scalar types (Integer, String, Date, etc.) supported. Collections need manual parsing from String.

---

#### Test File 25: `service_param_type_dict.ek9`

**Error:** SERVICE_INCOMPATIBLE_PARAM_TYPE
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** Dictionary types not supported in service parameters

```ek9
#!ek9
defines module fuzztest.service.param.dict

  defines service
    DictParamService :/dicts

      search() as GET :/search
        <?- Dict type not supported in HTTP parameters -?>
        @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_INCOMPATIBLE_PARAM_TYPE
        -> filters as Dict of (String, String) :=: QUERY "filters"
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** Dict cannot be automatically mapped from HTTP parameters.

---

#### Test File 26: `service_param_type_custom_class.ek9`

**Error:** SERVICE_INCOMPATIBLE_PARAM_TYPE
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** Custom user-defined types not supported

```ek9
#!ek9
defines module fuzztest.service.param.custom

  defines record
    SearchFilter
      term <- String()
      limit <- Integer()

  defines service
    CustomParamService :/custom

      search() as GET :/search
        <?- Custom type not supported in HTTP parameters -?>
        @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_INCOMPATIBLE_PARAM_TYPE
        -> filter as SearchFilter :=: QUERY "filter"
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** Only built-in scalar types supported for automatic HTTP parameter mapping.

---

#### Test File 27: `service_param_type_optional.ek9`

**Error:** SERVICE_INCOMPATIBLE_PARAM_TYPE
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** Optional wrapper types not supported

```ek9
#!ek9
defines module fuzztest.service.param.optional

  defines service
    OptionalParamService :/optional

      get() as GET :/data
        <?- Optional type not supported in HTTP parameters -?>
        @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_INCOMPATIBLE_PARAM_TYPE
        -> value as Optional of String :=: QUERY "value"
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** Optional not in supported type list. Parameters are implicitly optional (can be unset).

---

### Category 6: URI Path Duplication Errors (Phase 2) - 5 Files

#### Test File 28: `service_path_duplicate_complex.ek9`

**Error:** SERVICE_HTTP_PATH_DUPLICATED
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** Complex duplicate path structures

```ek9
#!ek9
defines module fuzztest.service.path.duplicate.complex

  defines service
    DupComplexService :/api

      <?- First operation with complex path -?>
      @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_HTTP_PATH_DUPLICATED
      op1() as GET :/{version}/users/{userId}/posts/{postId}
        ->
          version as String
          userId as Integer
          postId as Integer
        <- response as HTTPResponse: () with trait of HTTPResponse

      <?- Duplicate structure with different variable names -?>
      @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_HTTP_PATH_DUPLICATED
      op2() as GET :/{apiVersion}/users/{userNum}/posts/{postNum}
        ->
          apiVersion as String
          userNum as Integer
          postNum as Integer
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** Path structure `/{}/users/{}/posts/{}` is same despite different variable names.

---

#### Test File 29: `service_path_duplicate_nested.ek9`

**Error:** SERVICE_HTTP_PATH_DUPLICATED
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** Nested resource path duplicates

```ek9
#!ek9
defines module fuzztest.service.path.duplicate.nested

  defines service
    DupNestedService :/resources

      @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_HTTP_PATH_DUPLICATED
      getResource1() as GET :/{categoryId}/{subcategoryId}/{itemId}
        ->
          categoryId as String
          subcategoryId as String
          itemId as String
        <- response as HTTPResponse: () with trait of HTTPResponse

      @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_HTTP_PATH_DUPLICATED
      getResource2() as GET :/{cat}/{subcat}/{item}
        ->
          cat as String
          subcat as String
          item as String
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** Three consecutive path variables create identical structure.

---

#### Test File 30: `service_path_duplicate_reordered.ek9`

**Error:** SERVICE_HTTP_PATH_DUPLICATED
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** Verify type order doesn't matter for duplication detection

```ek9
#!ek9
defines module fuzztest.service.path.duplicate.reordered

  defines service
    DupReorderedService :/data

      <?- Integer first, String second -?>
      @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_HTTP_PATH_DUPLICATED
      get1() as GET :/{id}/{name}/details.json
        ->
          id as Integer
          name as String
        <- response as HTTPResponse: () with trait of HTTPResponse

      <?- String first, Integer second - still duplicate structure -?>
      @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_HTTP_PATH_DUPLICATED
      get2() as GET :/{identifier}/{count}/details.json
        ->
          identifier as String
          count as Integer
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** Path structure `/{}/{}` /details.json` matches regardless of parameter types.

---

#### Test File 31: `service_path_count_too_many.ek9`

**Error:** SERVICE_HTTP_PATH_PARAM_COUNT_INVALID
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** More URI variables than method parameters

```ek9
#!ek9
defines module fuzztest.service.path.count.many

  defines service
    TooManyPathService :/items

      <?- URI has 3 variables but only 2 parameters -?>
      @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_HTTP_PATH_PARAM_COUNT_INVALID
      getItem() as GET :/{categoryId}/{subcategoryId}/{itemId}
        ->
          categoryId as String
          subcategoryId as String
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** URI has `{itemId}` but method doesn't accept it.

---

#### Test File 32: `service_path_count_too_few.ek9`

**Error:** SERVICE_HTTP_PATH_PARAM_COUNT_INVALID
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** More PATH parameters than URI variables

```ek9
#!ek9
defines module fuzztest.service.path.count.few

  defines service
    TooFewPathService :/users

      <?- URI has 1 variable but method has 2 PATH parameters -?>
      @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_HTTP_PATH_PARAM_COUNT_INVALID
      getUser() as GET :/{userId}
        ->
          userId as Integer
          additionalParam as String
        <- response as HTTPResponse: () with trait of HTTPResponse
```

**Rationale:** Method assumes 2 PATH params but URI only has 1 variable.

---

### Category 7: Service Return Type Errors (Phase 2) - 8 Files

#### Test File 33: `service_return_type_integer.ek9`

**Error:** SERVICE_INCOMPATIBLE_RETURN_TYPE
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** Service operations must return HTTPResponse

```ek9
#!ek9
defines module fuzztest.service.return.integer

  defines service
    IntegerReturnService :/numbers

      getCount() as GET :/count
        <?- Returning Integer instead of HTTPResponse -?>
        @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_INCOMPATIBLE_RETURN_TYPE
        <- count as Integer: 42
```

**Rationale:** All service operations must return HTTPResponse or compatible trait.

---

#### Test File 34: `service_return_type_string.ek9`

**Error:** SERVICE_INCOMPATIBLE_RETURN_TYPE
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** String return not allowed

```ek9
#!ek9
defines module fuzztest.service.return.string

  defines service
    StringReturnService :/messages

      getMessage() as GET :/message
        <?- Returning String instead of HTTPResponse -?>
        @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_INCOMPATIBLE_RETURN_TYPE
        <- message as String: "Hello, World"
```

**Rationale:** Direct String return bypasses HTTP metadata (status, headers, etc.).

---

#### Test File 35: `service_return_type_void.ek9`

**Error:** SERVICE_MISSING_RETURN
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** Service operations cannot be void

```ek9
#!ek9
defines module fuzztest.service.return.void

  defines service
    VoidReturnService :/actions

      <?- No return declaration - effectively void -?>
      @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_MISSING_RETURN
      performAction() as POST :/action
        -> data as String :=: CONTENT

        stdout <- Stdout()
        stdout.println(data)
```

**Rationale:** Service operations must return HTTPResponse to communicate with HTTP client.

---

#### Test File 36: `service_return_type_custom_class.ek9`

**Error:** SERVICE_INCOMPATIBLE_RETURN_TYPE
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** Custom classes not compatible with HTTPResponse

```ek9
#!ek9
defines module fuzztest.service.return.custom

  defines class
    CustomResponse
      status <- Integer()
      body <- String()

      default operator ?

  defines service
    CustomReturnService :/custom

      getData() as GET :/data
        <?- Returning custom class instead of HTTPResponse -?>
        @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_INCOMPATIBLE_RETURN_TYPE
        <- response as CustomResponse: CustomResponse()
```

**Rationale:** Return type must be HTTPResponse trait or implement compatible interface.

---

#### Test File 37: `service_missing_return_operator.ek9`

**Error:** SERVICE_MISSING_RETURN
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** Service operators must have return declarations

```ek9
#!ek9
defines module fuzztest.service.return.missing.operator

  defines service
    MissingReturnOperatorService :/items

      <?- POST operator without return -?>
      @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_MISSING_RETURN
      operator += :/
        -> request as HTTPRequest :=: REQUEST

        stdout <- Stdout()
        stdout.println(request.content())
```

**Rationale:** Even operators must return HTTPResponse.

---

#### Test File 38: `service_missing_return_method.ek9`

**Error:** SERVICE_MISSING_RETURN
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** Named service methods must have return declarations

```ek9
#!ek9
defines module fuzztest.service.return.missing.method

  defines service
    MissingReturnMethodService :/resources

      <?- GET method without return -?>
      @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_MISSING_RETURN
      getResource() as GET :/{id}
        -> id as String

        stdout <- Stdout()
        stdout.println(id)
```

**Rationale:** Service methods must explicitly declare HTTPResponse return.

---

#### Test File 39: `service_no_body_get_method.ek9`

**Error:** SERVICE_WITH_NO_BODY_PROVIDED
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** Service GET methods cannot be abstract/unimplemented

```ek9
#!ek9
defines module fuzztest.service.nobody.get

  defines service
    AbstractGetService :/abstract

      <?- Abstract/unimplemented GET method -?>
      @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_WITH_NO_BODY_PROVIDED
      getData() as GET :/data
        <- response as HTTPResponse?
```

**Rationale:** Services cannot have abstract operations - all must be implemented.

---

#### Test File 40: `service_no_body_post_operator.ek9`

**Error:** SERVICE_WITH_NO_BODY_PROVIDED
**Phase:** EXPLICIT_TYPE_SYMBOL_DEFINITION
**Purpose:** Service POST operators cannot be abstract/unimplemented

```ek9
#!ek9
defines module fuzztest.service.nobody.post

  defines service
    AbstractPostService :/abstract

      <?- Abstract/unimplemented POST operator -?>
      @Error: EXPLICIT_TYPE_SYMBOL_DEFINITION: SERVICE_WITH_NO_BODY_PROVIDED
      operator += :/
        -> request as HTTPRequest :=: REQUEST
        <- response as HTTPResponse?
```

**Rationale:** Service operators must have implementation body.

---

## 📊 Test Organization Summary

### By Compilation Phase

| Phase | Error Categories | Test Files | ZERO Coverage Tests |
|-------|------------------|------------|---------------------|
| **Phase 1 (SYMBOL_DEFINITION)** | Service Definition, URI, Operators, HTTP Access | 14 | 8 |
| **Phase 2 (EXPLICIT_TYPE_SYMBOL_DEFINITION)** | Parameters, Headers, Paths, Returns | 26 | 9 |
| **TOTAL** | **7 categories** | **40** | **17** |

### By Error Type Priority

| Priority | Description | Test Files |
|----------|-------------|------------|
| **CRITICAL** | Zero coverage errors | 17 |
| **HIGH** | Edge cases for tested errors | 23 |
| **TOTAL** | | **40** |

---

## 🎯 Implementation Strategy

### Phase 1: ZERO Coverage Errors (Priority 1)

**Estimated Effort:** 4-5 hours
**Test Files:** 17
**Impact:** +7 new error types covered

**Order of Implementation:**
1. HTTP Header errors (5 files) - straightforward validation
2. HTTP Body mapping errors (2 files) - simple qualifier checks
3. Path assumption errors (2 files) - requires understanding path inference logic
4. Operator verb redundancy (3 files) - operator/verb mapping rules
5. Caching/access name errors (5 files) - may require grammar investigation

### Phase 2: Edge Cases for Tested Errors (Priority 2)

**Estimated Effort:** 5-6 hours
**Test Files:** 23
**Impact:** Strengthen coverage of 13 existing error types

**Order of Implementation:**
1. Parameter type errors (4 files) - test with collections, custom types
2. Path duplication errors (5 files) - complex nested structures
3. Return type errors (8 files) - various invalid return types
4. URI variable errors (2 files) - service-level URI variables
5. HTTP access errors (2 files) - constructor/method context
6. Operator errors (3 files) - invalid operator types

### Phase 3: Test Suite Implementation

**Test Suite Class:** `ServiceValidationFuzzTest.java`

```java
package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzz tests for EK9 service validation across compilation phases.
 * <p>
 * Tests comprehensive service error detection including:
 * - Phase 1 (SYMBOL_DEFINITION): Service definition, URI, operators
 * - Phase 2 (EXPLICIT_TYPE_SYMBOL_DEFINITION): Parameters, headers, returns
 * <p>
 * Coverage: 23 SERVICE error types across 40 test scenarios.
 */
class ServiceValidationFuzzTest extends FuzzTestBase {
  public ServiceValidationFuzzTest() {
    // Service errors span Phase 1 and Phase 2
    // Using EXPLICIT_TYPE_SYMBOL_DEFINITION as target phase
    // to catch both Phase 1 and Phase 2 errors
    super("serviceValidation", CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION, false);
  }

  @Test
  void testServiceValidationRobustness() {
    assertTrue(runTests() != 0);
  }
}
```

**NOTE:** Since service errors span Phase 1 and Phase 2, using Phase 2 as target ensures both are validated.

---

## 📈 Expected Outcomes

### Coverage Improvements

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| **SERVICE Errors Tested** | 16/23 (69.6%) | 23/23 (100%) | +30.4% |
| **Test Files** | 4 | 44 | +1000% |
| **Test Scenarios** | ~20 | ~60 | +200% |
| **Phase 1 Coverage** | 3/6 (50%) | 6/6 (100%) | +50% |
| **Phase 2 Coverage** | 13/17 (76.5%) | 17/17 (100%) | +23.5% |

### Quality Metrics

- ✅ **100% SERVICE error coverage** - all 23 errors systematically tested
- ✅ **Zero duplications** - comprehensive review ensures non-overlapping scenarios
- ✅ **Comprehensive edge cases** - complex nested paths, custom types, operator combinations
- ✅ **Production-ready validation** - HTTP service robustness significantly increased

---

## 🚀 Next Steps

### Immediate Actions

1. **Review plan with Steve** - validate test scenarios and priorities
2. **Investigate grammar for unused errors** - determine if caching/access name features exist
3. **Begin Phase 1 implementation** - start with ZERO coverage tests (highest priority)
4. **Validate against existing tests** - ensure no overlaps with current coverage

### Post-Implementation

1. **Run full test suite** - ensure all 40 tests pass with correct error directives
2. **Update FUZZING_MASTER_STATUS.md** - document Phase 1B completion
3. **Create comprehensive status report** - similar to STREAM_PROCESSING_FUZZING_STATUS.md
4. **Move to Phase 1C** - Literal validation fuzzing (next priority)

---

**Plan Completed:** 2025-11-12
**Total Planned Tests:** 40 files
**Estimated Total Effort:** 9-11 hours
**Expected Error Coverage Increase:** +7 error types (30.4% → 100% SERVICE coverage)

---

**Report Generated By:** Claude Code (Anthropic)
**Project:** EK9 Compiler Phase 1B Fuzzing Test Suite - Service Validation
**Repository:** github.com/stephenjohnlimb/ek9
