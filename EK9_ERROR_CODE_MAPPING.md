# EK9 Error Code Mapping - Complete Reference

**Complete mapping of all 215 SemanticClassification errors to E[PP][NNN] error codes**

Generated: 2025-11-18

## Error Code Format

- **E[PP][NNN]** - Phase-based error codes
  - **PP** = Phase number (01-20) or 50 for multi-phase errors
  - **NNN** = Sequential error number within phase (incremented by 10 for future additions)

## Phase Mapping Reference

| Phase | Number | Description |
|-------|--------|-------------|
| PARSING | 00 | ANTLR4-based parsing (syntax errors) |
| SYMBOL_DEFINITION | 01 | Symbol table creation |
| DUPLICATION_CHECK | 02 | Duplicate detection |
| REFERENCE_CHECKS | 03 | Reference validation |
| EXPLICIT_TYPE_SYMBOL_DEFINITION | 04 | Type resolution |
| TYPE_HIERARCHY_CHECKS | 05 | Inheritance validation |
| FULL_RESOLUTION | 06 | Template and generic resolution |
| POST_RESOLUTION_CHECKS | 07 | Symbol validation |
| PRE_IR_CHECKS | 08 | Code flow analysis |
| PLUGIN_RESOLUTION | 09 | Plugin resolution |
| IR_GENERATION | 10 | Intermediate representation generation |
| IR_ANALYSIS | 11 | IR analysis and validation |
| IR_OPTIMISATION | 12 | IR-level optimizations |
| CODE_GENERATION_PREPARATION | 13 | Code generation preparation |
| CODE_GENERATION_AGGREGATES | 14 | Generate code for aggregates |
| CODE_GENERATION_CONSTANTS | 15 | Generate code for constants |
| CODE_OPTIMISATION | 16 | Target code optimizations |
| PLUGIN_LINKAGE | 17 | Link external plugins |
| APPLICATION_PACKAGING | 18 | Application packaging |
| PACKAGING_POST_PROCESSING | 19 | Completing post processing |
| MULTI_PHASE | 50 | Errors that can occur in multiple phases |

---

## PHASE 00: PARSING (Syntax Errors)

**Note:** Syntax errors are handled by ANTLR4 and do not use SemanticClassification enum.
Error codes E00xxx are reserved for future syntax error categorization.

---

## PHASE 01: SYMBOL_DEFINITION

### E01010-E01090: Symbol Definition Errors

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E01010 | INVALID_SYMBOL_BY_REFERENCE | invalid reference naming; module scope name missing |
| E01020 | INVALID_MODULE_NAME | invalid module name |
| E01030 | DUPLICATE_NAME | Variable/Function/Type duplicated, likely to lead to confusion |
| E01040 | DUPLICATE_TYPE | Type/Function name duplicated |
| E01050 | POSSIBLE_DUPLICATE_ENUMERATED_VALUE | are duplicated values (or are too similar, likely to be confusing) |

---

## PHASE 02: DUPLICATION_CHECK

### E02010-E02090: Duplication Detection Errors

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E02010 | DUPLICATE_PROPERTY_FIELD | Property/Field duplicated |
| E02020 | CANNOT_SUPPORT_TO_JSON_DUPLICATE_PROPERTY_FIELD | Property/Field duplicated, $$ (JSON) operator not supported |
| E02030 | METHOD_DUPLICATED | duplicate/ambiguous methods/operations |
| E02040 | DUPLICATE_VARIABLE_IN_CAPTURE | variable name duplicated, resulting in multiple fields of same name |
| E02050 | DUPLICATE_TRAIT_REFERENCE | same trait referenced multiple times |
| E02060 | DUPLICATE_ENUMERATED_VALUES_PRESENT_IN_SWITCH | duplicated enumerated value in switch 'case' |
| E02070 | SERVICE_HTTP_PATH_DUPLICATED | PATH is duplicated in terms of structure/naming |
| E02080 | DELEGATE_AND_METHOD_NAMES_CLASH | use of a delegate and methods of same name |

---

## PHASE 03: REFERENCE_CHECKS

### E03010-E03090: Reference Validation Errors

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E03010 | CONSTRUCT_REFERENCE_CONFLICT | conflicts with a reference |
| E03020 | REFERENCES_CONFLICT | conflicting references |
| E03030 | REFERENCE_DOES_NOT_RESOLVED | reference does not resolve |

---

## PHASE 04: EXPLICIT_TYPE_SYMBOL_DEFINITION

### E04010-E04090: Type Resolution Errors

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E04010 | TYPE_CANNOT_BE_CONSTRAINED | is not a candidate to be constrained |
| E04020 | TYPE_MUST_BE_CONVERTABLE_TO_STRING | result must be String, have the $ operator or can be promoted to String |
| E04030 | TYPE_MUST_EXTEND_EXCEPTION | type must be of Exception type |
| E04040 | TYPE_MUST_BE_FUNCTION | type must be a function or delegate |
| E04050 | TYPE_MUST_BE_SIMPLE | type must be a simple aggregate/list/dict |
| E04060 | IS_NOT_AN_AGGREGATE_TYPE | not an aggregate type |
| E04070 | NOT_A_TEMPLATE | as it is not 'template/generic' in nature |
| E04080 | TEMPLATE_TYPE_REQUIRES_PARAMETERIZATION | Template/Generic requires parameterization |

---

## PHASE 05: TYPE_HIERARCHY_CHECKS

### E05020-E05200: Type Hierarchy Validation Errors

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E05020 | CIRCULAR_HIERARCHY_DETECTED | a circular type/function hierarchy has been used |
| E05030 | NOT_OPEN_TO_EXTENSION | not open to be extended |
| E05040 | SUPER_FOR_ANY_NOT_REQUIRED | 'super' for implicit 'Any' base class is not required |
| E05050 | THIS_AND_SUPER_MUST_BE_FIRST_IN_CONSTRUCTOR | 'this()' and 'super()' must be the first statement in a constructor |
| E05060 | THIS_AND_SUPER_CALLS_ONLY_IN_CONSTRUCTOR | 'this()' and 'super()' can only be used in constructors, did you mean 'this.' or 'super.' |
| E05070 | INAPPROPRIATE_USE_OF_THIS | inappropriate use of 'this' |
| E05080 | INAPPROPRIATE_USE_OF_SUPER | inappropriate use of 'super' |
| E05090 | USE_OF_THIS_OR_SUPER_INAPPROPRIATE | can be used with :=:, :~:, +=, -=, /= and *=. But not direct assignment |
| E05100 | OVERRIDE_INAPPROPRIATE | cannot override anything |
| E05110 | DOES_NOT_OVERRIDE | does not 'override' any method/operator |
| E05120 | METHOD_OVERRIDES | as it overrides method of same name/signature in hierarchy |
| E05130 | METHOD_ACCESS_MODIFIERS_DIFFER | methods with same signature have different access modifiers |
| E05140 | FUNCTION_SIGNATURE_DOES_NOT_MATCH_SUPER | function signature does not match 'super' function |
| E05150 | SUPER_IS_PURE | 'pure' in super requires 'pure' for this definition |
| E05160 | SUPER_IS_NOT_PURE | 'super is not 'pure', requires this definition not to be marked as 'pure' |
| E05170 | DISPATCHER_PURE_MISMATCH | 'pure' on dispatcher requires 'pure' for matching dispatcher method |
| E05180 | DISPATCHER_PRIVATE_IN_SUPER | same method name as dispatcher, but marked private in super - won't be called |
| E05190 | MIX_OF_PURE_AND_NOT_PURE_CONSTRUCTORS | if any constructor is marked pure, all constructors must be pure |
| E05200 | INCOMPATIBLE_GENUS_CONSTRUCTOR | incompatible genus with local constructor use |

---

## PHASE 06: FULL_RESOLUTION

### E06010-E06330: Template and Generic Resolution Errors

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E06010 | GENERIC_TYPE_OR_FUNCTION_PARAMETERS_NEEDED | type/function is generic but no parameters were supplied |
| E06020 | GENERIC_TYPE_OR_FUNCTION_PARAMETERS_INCORRECT | type/function is generic, but incorrect number of parameters supplied |
| E06030 | GENERIC_TYPE_CONSTRUCTOR_INAPPROPRIATE | type is generic, but for type inference to work; the number of generic and constructor parameters must be the same |
| E06040 | GENERIC_TYPE_REQUIRES_TWO_CONSTRUCTORS | a generic type requires 2 constructors, default and inferred type |
| E06050 | GENERIC_TYPE_REQUIRES_CORRECT_CONSTRUCTOR_ARGUMENT_TYPES | a generic type requires correct constructor argument to match parametric types (and order) |
| E06060 | GENERIC_CONSTRUCTORS_MUST_BE_PUBLIC | a generic type does not support private or protected constructors |
| E06070 | TYPE_INFERENCE_NOT_SUPPORTED | type inference is not supported within generic/template type/functions |
| E06080 | CONSTRAINED_FUNCTIONS_NOT_SUPPORTED | the constraining type with a generic/template cannot be a function |
| E06090 | GENERIC_WITH_NAMED_DYNAMIC_CLASS | a named dynamic class cannot be used within a generic type/function |
| E06100 | GENERIC_FUNCTION_IMPLEMENTATION_REQUIRED | implementation must be provided for generic function |
| E06110 | CONSTRUCTOR_WITH_FUNCTION_IN_GENERIC | use of a constructor in a generic/template that uses a function is not supported |
| E06120 | FUNCTION_USED_IN_GENERIC | functions can be used in generics/templates but only '?' is supported |
| E06130 | CONSTRAINED_TYPE_CONSTRUCTOR_MISSING | constraining type constructors must exist on parameterizing type |
| E06140 | METHOD_AMBIGUOUS | ambiguous match |
| E06150 | METHODS_CONFLICT | conflicting methods to be resolved |
| E06160 | NOT_IMMEDIATE_TRAIT | not an immediate trait of this context |
| E06170 | TRAIT_ACCESS_NOT_SUPPORTED | trait method access not supported here |
| E06180 | NOT_ACCESSIBLE | not accessible from this context |
| E06190 | RESULT_MUST_HAVE_DIFFERENT_TYPES | EK9 Result must be used with two different types |
| E06200 | PARENTHESIS_NOT_REQUIRED | use of parenthesis '( )' not allowed in this context |
| E06210 | PARENTHESIS_REQUIRED | parenthesis '( )' required in this context |
| E06220 | VALUES_AND_TYPE_INCOMPATIBLE | choose either empty parenthesis '( )' with values or a type definition |
| E06230 | CAPTURED_VARIABLE_MUST_BE_NAMED | variables being captured must be named when not just using identifiers |
| E06240 | EITHER_ALL_PARAMETERS_NAMED_OR_NONE | either all variable must be named or none, when passing parameters |
| E06250 | NAMED_PARAMETERS_MUST_MATCH_ARGUMENTS | the order and naming of arguments must match parameters |
| E06260 | PARAMETER_MISMATCH | parameter mismatch |
| E06270 | FUNCTION_PARAMETER_MISMATCH | parameter mismatch |
| E06280 | TOO_MANY_ARGUMENTS | too many arguments |
| E06290 | TOO_FEW_ARGUMENTS | too few arguments |
| E06300 | REQUIRE_ONE_ARGUMENT | require one argument only |
| E06310 | REQUIRE_NO_ARGUMENTS | function must have no arguments |
| E06320 | INVALID_NUMBER_OF_PARAMETERS | invalid number of parameters |
| E06330 | INCOMPATIBLE_TYPE_ARGUMENTS | argument types are not compatible with each other |

---

## PHASE 07: POST_RESOLUTION_CHECKS

### E07010-E07500: Symbol Validation Errors

#### E07010-E07090: Modifier and Access Control Errors

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E07010 | METHOD_ACCESS_MODIFIER_PRIVATE_OVERRIDE | remove 'override' with use of 'private' access modifier |
| E07020 | OVERRIDE_AND_ABSTRACT | 'override' of a method/operator and 'abstract' (no implementation) is not logical |
| E07030 | DEFAULT_AND_TRAIT | 'default' of operators on a trait is not supported |
| E07040 | TRAIT_BY_IDENTIFIER_NOT_SUPPORTED | 'by' variable is not supported on a trait only on a class |
| E07050 | ABSTRACT_CONSTRUCTOR | 'abstract' modifier on a constructor is not logical |
| E07060 | OVERRIDE_CONSTRUCTOR | 'override' is not required on a constructor |
| E07070 | TRAITS_DO_NOT_HAVE_CONSTRUCTORS | traits do not support constructor methods |
| E07080 | INVALID_DEFAULT_CONSTRUCTOR | 'default' constructor with parameters is not supported |
| E07090 | DEFAULT_ONLY_FOR_CONSTRUCTORS | 'default' modifier is only valid for constructors, not regular methods |

#### E07100-E07170: Abstract and Implementation Errors

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E07100 | ABSTRACT_BUT_BODY_PROVIDED | defined as default/abstract but an implementation has been provided |
| E07110 | NOT_ABSTRACT_AND_NO_BODY_PROVIDED | implementation not provided so must be declared as abstract |
| E07120 | DISPATCHER_BUT_NO_BODY_PROVIDED | base level implementation must be provided for dispatcher method |
| E07130 | NOT_MARKED_ABSTRACT_BUT_IS_ABSTRACT | not declared abstract but still has abstract methods/operators |
| E07140 | DYNAMIC_CLASS_MUST_IMPLEMENT_ABSTRACTS | all abstract methods/operators must be implemented |
| E07150 | TEXT_METHOD_MISSING | text method missing for language variant |
| E07160 | IMPLEMENTATION_MUST_BE_PROVIDED | implementation must be provided |
| E07170 | EXPLICIT_CONSTRUCTOR_REQUIRED | a developer coded constructor(s) are require where uninitialized properties are used |

#### E07180-E07250: Default Operator Errors

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E07180 | MISSING_OPERATOR_IN_THIS | 'default' of operators requires this type to have appropriate operator |
| E07190 | MISSING_OPERATOR_IN_SUPER | 'default' of operators requires super to have appropriate operator |
| E07200 | MISSING_OPERATOR_IN_PROPERTY_TYPE | 'default' of operators requires property/field to have appropriate operator |
| E07210 | FUNCTION_DELEGATE_WITH_DEFAULT_OPERATORS | 'default' of operators can only support '?' operator with function delegate fields |
| E07220 | OPERATOR_DEFAULT_NOT_SUPPORTED | 'default' is not supported on this operator |
| E07230 | DEFAULT_WITH_OPERATOR_SIGNATURE | 'default' with an operator must not have signature or body |

#### E07240-E07310: Method Access Modifier Errors

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E07240 | METHOD_MODIFIER_PROTECTED_IN_SERVICE | non web service methods cannot be marked with the 'protected' access modifier |
| E07250 | METHOD_MODIFIER_PROTECTED_IN_COMPONENT | component methods cannot be marked with the 'protected' access modifier |
| E07260 | METHOD_MODIFIER_PROTECTED_IN_CLOSED_CLASS | class methods can only be marked with the 'protected' access modifier in classes that are 'open' |
| E07270 | METHOD_MODIFIER_NOT_REQUIRED_IN_TRAIT | trait methods cannot be marked with an access modifier, they are always public |
| E07280 | METHOD_ACCESS_MODIFIER_DEFAULT | access modifier is not needed here - methods are 'public' by default |
| E07290 | RECORDS_ONLY_SUPPORT_CONSTRUCTOR_AND_OPERATOR_METHODS | only constructors and operators methods are supported on records |
| E07300 | DECLARED_AS_NULL_NOT_NEEDED | declaration supporting 'uninitialised' is not needed |

#### E07310-E07380: Switch Statement Errors

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E07310 | NOT_ALL_ENUMERATED_VALUES_PRESENT_IN_SWITCH | 'cases' should cover all enumerated values in 'switch' |
| E07320 | DEFAULT_REQUIRED_IN_SWITCH_STATEMENT | 'default' is required in this 'switch' statement |
| E07330 | DEFAULT_REQUIRED_IN_SWITCH_EXPRESSION | 'default' is required in this 'switch' expression |

#### E07340-E07410: Control Flow and Guards

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E07340 | PRE_FLOW_SYMBOL_NOT_RESOLVED | without a control, failed to find subject of flow |
| E07350 | GUARD_USED_IN_EXPRESSION | a 'guard' cannot be used in an expression as it may leave 'lhs' uninitialised |
| E07360 | APPLICATION_SELECTION_INVALID | application selection not allowed in this context |
| E07370 | STATEMENT_UNREACHABLE | all paths lead to an Exception |
| E07380 | RETURN_UNREACHABLE | return not possible, as instructions only result in an Exception |
| E07390 | POINTLESS_EXPRESSION | constant Boolean literal in expression is pointless |

#### E07400-E07470: Return and Returning Errors

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E07400 | RETURNING_MISSING | returning variable and type missing |
| E07410 | MUST_RETURN_SAME_AS_CONSTRUCT_TYPE | returning type must be same as the construct type |
| E07420 | MUST_NOT_RETURN_SAME_TYPE | returning type must not be same for promotion |
| E07430 | RETURN_VALUE_NOT_SUPPORTED | return not required/supported in this context |
| E07440 | COVARIANCE_MISMATCH | return types are incompatible (covariance required) |

#### E07450-E07520: Function and Operator Requirements

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E07450 | FUNCTION_MUST_HAVE_NO_PARAMETERS | function must have no parameters |
| E07460 | FUNCTION_MUST_HAVE_SINGLE_PARAMETER | function must have a single parameter |
| E07470 | FUNCTION_MUST_HAVE_TWO_PARAMETERS | function must have a two parameters |
| E07480 | NOT_A_FUNCTION_DELEGATE | is not a function delegate |
| E07490 | FUNCTION_MUST_RETURN_VALUE | function must return a value |
| E07500 | OPERATOR_MUST_BE_PURE | operator must be declared pure |
| E07510 | OPERATOR_CANNOT_BE_PURE | operator must not be declared pure |

#### E07520-E07590: Type Compatibility Errors

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E07520 | MUST_RETURN_BOOLEAN | must return a Boolean |
| E07530 | ONLY_COMPATIBLE_WITH_BOOLEAN | only compatible with Boolean type |
| E07540 | MUST_BE_A_BOOLEAN | is not compatible with a Boolean type |
| E07550 | MUST_RETURN_INTEGER | must return an Integer |
| E07560 | MUST_BE_INTEGER_GREATER_THAN_ZERO | must be an Integer with a value greater than zero |
| E07570 | MUST_RETURN_STRING | must return a String |
| E07580 | MUST_RETURN_JSON | must return a JSON |

#### E07590-E07660: Program Constraints

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E07590 | PROGRAM_CAN_ONLY_RETURN_INTEGER | if a program returns a value it can only be an Integer |
| E07600 | PROGRAM_ARGUMENT_TYPE_INVALID | program arguments are limited to a finite range of EK9 built in types |
| E07610 | PROGRAM_ARGUMENTS_INAPPROPRIATE | inappropriate combination of program arguments |

#### E07620-E07690: Operator Errors

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E07620 | OPERATOR_NOT_DEFINED | operator not defined |
| E07630 | OPERATOR_CANNOT_BE_USED_ON_ENUMERATION | operator cannot be used on an Enumeration in this way |
| E07640 | BAD_NOT_EQUAL_OPERATOR | use <> for the not equal operator |
| E07650 | BAD_NOT_OPERATOR | use ~ for the not operator |
| E07660 | OPERATOR_NAME_USED_AS_METHOD | operator name cannot be used as a method |
| E07670 | SERVICE_OPERATOR_NOT_SUPPORTED | operator not supported, only +, +=, -, -=, :^:, :~: and ? are supported |

#### E07680-E07750: Service-Specific Errors

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E07680 | SERVICE_URI_WITH_VARS_NOT_SUPPORTED | URI with place holder variable not supported in this context |
| E07690 | SERVICE_HTTP_ACCESS_NOT_SUPPORTED | HTTP access verb not supported in this context |
| E07700 | SERVICE_HTTP_PATH_PARAM_INVALID | HTTP PATH parameter is invalid |
| E07710 | SERVICE_HTTP_PARAM_NEEDS_QUALIFIER | additional name needed for QUERY, HEADER or PATH parameters |
| E07720 | SERVICE_HTTP_PARAM_QUALIFIER_NOT_ALLOWED | additional name only required for QUERY, HEADER or PATH parameters |
| E07730 | SERVICE_HTTP_PATH_PARAM_COUNT_INVALID | HTTP PATH variable count and PATH parameter count mismatch |
| E07740 | SERVICE_WITH_NO_BODY_PROVIDED | implementation not provided, but services cannot be abstract |
| E07750 | SERVICE_INCOMPATIBLE_RETURN_TYPE | Web Service return type must be compatible with HTTPResponse |
| E07760 | SERVICE_INCOMPATIBLE_PARAM_TYPE | only Integer, String, Date, Time, DateTime, Milliseconds, Duration and HTTPRequest supported |
| E07770 | SERVICE_INCOMPATIBLE_PARAM_TYPE_REQUEST | Web Service parameter type must be HTTPRequest for REQUEST |
| E07780 | SERVICE_REQUEST_BY_ITSELF | Web Service type HTTPRequest can only be used by itself |
| E07790 | SERVICE_INCOMPATIBLE_PARAM_TYPE_NON_REQUEST | Web Service parameter type cannot be HTTPRequest |
| E07800 | SERVICE_MISSING_RETURN | Web Service must have return value and it must be compatible with HTTPResponse |

#### E07810-E07880: Dispatcher Errors

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E07810 | DISPATCH_ONLY_SUPPORTED_IN_CLASSES | Dispatch only supported in classes |
| E07820 | DISPATCHERS_ONLY_HAVE_ONE_METHOD_ENTRY_POINT_MARKED | only one method can be marked as a dispatcher entry point |

#### E07830-E07900: Stream and Iterator Errors

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E07830 | UNABLE_TO_FIND_PIPE_FOR_TYPE | unable to find a '|' pipe operator for type |
| E07840 | MISSING_ITERATE_METHOD | it does not have compatible iterator() - hasNext()/next() methods |

#### E07850-E07920: Miscellaneous Semantic Errors

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E07850 | SINGLE_EXCEPTION_ONLY | only a single Exception is supported |
| E07860 | FUNCTION_OR_DELEGATE_REQUIRED | require a function or function delegate |
| E07870 | INTEGER_VAR_OR_FUNCTION_OR_DELEGATE_REQUIRED | require an Integer value or function/function delegate |
| E07880 | FUNCTION_OR_DELEGATE_NOT_REQUIRED | a function or function delegate is not required here |
| E07890 | NOT_MUTABLE | not mutable |
| E07900 | INVALID_VALUE | Invalid value |

---

## PHASE 08: PRE_IR_CHECKS

### E08010-E08180: Code Flow Analysis Errors

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E08010 | USED_BEFORE_DEFINED | used before definition |
| E08020 | USED_BEFORE_INITIALISED | might be used before being initialised |
| E08030 | UNSAFE_METHOD_ACCESS | has not been checked before access |
| E08040 | NO_REASSIGNMENT_WITHIN_SAFE_ACCESS | Reassignment/mutation within possible 'safe method access' scope is not allowed |
| E08050 | RETURN_NOT_ALWAYS_INITIALISED | return value is not always initialised |
| E08060 | NOT_INITIALISED_BEFORE_USE | is/may not be initialised before use |
| E08070 | NEVER_INITIALISED | never initialised |
| E08080 | SELF_ASSIGNMENT | self assignment |
| E08090 | NOT_REFERENCED | is not referenced anywhere, or not referenced after assignment |
| E08100 | NO_PURE_REASSIGNMENT | reassignment not allowed when scope is marked as 'pure' (':=?' is supported) |
| E08110 | NO_INCOMING_ARGUMENT_REASSIGNMENT | reassignment not allowed of an incoming argument/parameter |
| E08120 | NO_MUTATION_IN_PURE_CONTEXT | mutating variables is not allowed when scope is marked as 'pure' |
| E08130 | NONE_PURE_CALL_IN_PURE_SCOPE | is not marked 'pure', but call is made in a scope that is marked as 'pure' |
| E08140 | COMPONENT_INJECTION_IN_PURE | component injection not allowed when scope is marked as 'pure' |
| E08150 | COMPONENT_INJECTION_OF_NON_ABSTRACT | dependency injection of a non-abstract component is not allowed, use an abstract base component |
| E08160 | COMPONENT_INJECTION_NOT_POSSIBLE | dependency injection is not allowed |
| E08170 | REASSIGNMENT_OF_INJECTED_COMPONENT | direct reassignment of an injected component is not allowed, use ':=?' for conditional reassignment |
| E08180 | NOT_INITIALISED_IN_ANY_WAY | not marked for injection nor initialised |

---

## PHASE 09: PLUGIN_RESOLUTION

### E09010-E09020: Plugin Resolution Errors

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E09010 | INAPPROPRIATE_USE | inappropriate use in this context |

---

## PHASE 10: IR_GENERATION

### E10010-E10050: IR Generation Errors

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E10010 | RETURN_TYPE_VOID_MEANINGLESS | 'void' return type cannot be used with an assignment |
| E10020 | STREAM_TYPE_NOT_DEFINED | Void cannot be used in stream pipelines |
| E10030 | CONSTRUCTOR_USED_ON_ABSTRACT_TYPE | use of a constructor directly on an abstract type is not permitted. |

---

## PHASE 11: IR_ANALYSIS

### E11010-E11020: IR Analysis Errors

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E11010 | EXCESSIVE_COMPLEXITY | excessive complexity - refactor |

---

## PHASE 50: MULTI_PHASE ERRORS

**Errors that can occur in multiple compilation phases**

### E50001-E50070: Cross-Phase Errors

| Code | SemanticClassification | Description | Primary Phases |
|------|----------------------|-------------|----------------|
| E50001 | NOT_RESOLVED | not resolved | 01, 03, 04, 06 |
| E50010 | TYPE_NOT_RESOLVED | type not resolved | 04, 06 |
| E50020 | INCOMPATIBLE_GENUS | incompatible genus | 05, 06, 07 |
| E50030 | INCOMPATIBLE_TYPES | types are not compatible with each other | 04, 06, 07, 10 |
| E50040 | CANNOT_BE_ABSTRACT | cannot be abstract | 05, 07 |
| E50050 | DUPLICATE_VARIABLE | Variable/Constant duplicated | 01, 02, 08 |
| E50060 | METHOD_NOT_RESOLVED | method/function not resolved | 06, 07 |
| E50070 | BAD_ABSTRACT_FUNCTION_USE | cannot use an abstract function in this manner | 07, 10 |
| E50080 | CALL_ABSTRACT_TYPE | cannot make a call on an abstract function/type directly | 07, 10 |
| E50090 | INCOMPATIBLE_PARAMETER_GENUS | incompatible genus in parameter(s) | 06, 07 |
| E50100 | INCOMPATIBLE_CATEGORY | incompatible category | 04, 06, 07 |

### E50200-E50250: Directive Errors (Multi-Phase)

**Note:** Directive errors can occur in any phase where directives are validated

| Code | SemanticClassification | Description |
|------|----------------------|-------------|
| E50200 | UNKNOWN_DIRECTIVE | invalid directive |
| E50210 | DIRECTIVE_MISSING | but the directive is missing |
| E50220 | DIRECTIVE_WRONG_CLASSIFICATION | directive error classification incorrect |
| E50230 | ERROR_MISSING | but the error is missing |
| E50240 | DIRECTIVE_SYMBOL_COMPLEXITY | complexity mismatch |
| E50250 | DIRECTIVE_SYMBOL_NOT_RESOLVED | symbol not resolved |
| E50260 | DIRECTIVE_HIERARCHY_NOT_RESOLVED | symbol hierarchy not in place |
| E50270 | DIRECTIVE_SYMBOL_CATEGORY_MISMATCH | symbol category mismatched |
| E50280 | DIRECTIVE_SYMBOL_GENUS_MISMATCH | symbol genus mismatched |
| E50290 | DIRECTIVE_SYMBOL_NO_SUCH_GENUS | genus does not exist |
| E50300 | DIRECTIVE_SYMBOL_FOUND_UNEXPECTED_SYMBOL | unexpected symbol resolved |
| E50310 | DIRECTIVE_ERROR_MISMATCH | count does not match |

---

## Complete Alphabetical Index

All 220 SemanticClassification enum values mapped to error codes:

| SemanticClassification | Error Code | Phase |
|------------------------|------------|-------|
| ABSTRACT_BUT_BODY_PROVIDED | E07100 | 07 |
| ABSTRACT_CONSTRUCTOR | E07050 | 07 |
| APPLICATION_SELECTION_INVALID | E07360 | 07 |
| BAD_ABSTRACT_FUNCTION_USE | E50070 | 50 |
| BAD_NOT_EQUAL_OPERATOR | E07640 | 07 |
| BAD_NOT_OPERATOR | E07650 | 07 |
| CALL_ABSTRACT_TYPE | E50080 | 50 |
| CANNOT_BE_ABSTRACT | E50040 | 50 |
| CANNOT_SUPPORT_TO_JSON_DUPLICATE_PROPERTY_FIELD | E02020 | 02 |
| CAPTURED_VARIABLE_MUST_BE_NAMED | E06230 | 06 |
| CIRCULAR_HIERARCHY_DETECTED | E05020 | 05 |
| COMPONENT_INJECTION_IN_PURE | E08140 | 08 |
| COMPONENT_INJECTION_NOT_POSSIBLE | E08160 | 08 |
| COMPONENT_INJECTION_OF_NON_ABSTRACT | E08150 | 08 |
| CONSTRAINED_FUNCTIONS_NOT_SUPPORTED | E06080 | 06 |
| CONSTRAINED_TYPE_CONSTRUCTOR_MISSING | E06130 | 06 |
| CONSTRUCTOR_USED_ON_ABSTRACT_TYPE | E10030 | 10 |
| CONSTRUCTOR_WITH_FUNCTION_IN_GENERIC | E06110 | 06 |
| CONSTRUCT_REFERENCE_CONFLICT | E03010 | 03 |
| COVARIANCE_MISMATCH | E07440 | 07 |
| DECLARED_AS_NULL_NOT_NEEDED | E07300 | 07 |
| DEFAULT_AND_TRAIT | E07030 | 07 |
| DEFAULT_ONLY_FOR_CONSTRUCTORS | E07090 | 07 |
| DEFAULT_REQUIRED_IN_SWITCH_EXPRESSION | E07330 | 07 |
| DEFAULT_REQUIRED_IN_SWITCH_STATEMENT | E07320 | 07 |
| DEFAULT_WITH_OPERATOR_SIGNATURE | E07230 | 07 |
| DELEGATE_AND_METHOD_NAMES_CLASH | E02080 | 02 |
| DIRECTIVE_ERROR_MISMATCH | E50310 | 50 |
| DIRECTIVE_HIERARCHY_NOT_RESOLVED | E50260 | 50 |
| DIRECTIVE_MISSING | E50210 | 50 |
| DIRECTIVE_SYMBOL_CATEGORY_MISMATCH | E50270 | 50 |
| DIRECTIVE_SYMBOL_COMPLEXITY | E50240 | 50 |
| DIRECTIVE_SYMBOL_FOUND_UNEXPECTED_SYMBOL | E50300 | 50 |
| DIRECTIVE_SYMBOL_GENUS_MISMATCH | E50280 | 50 |
| DIRECTIVE_SYMBOL_NOT_RESOLVED | E50250 | 50 |
| DIRECTIVE_SYMBOL_NO_SUCH_GENUS | E50290 | 50 |
| DIRECTIVE_WRONG_CLASSIFICATION | E50220 | 50 |
| DISPATCH_ONLY_SUPPORTED_IN_CLASSES | E07810 | 07 |
| DISPATCHER_BUT_NO_BODY_PROVIDED | E07120 | 07 |
| DISPATCHER_PRIVATE_IN_SUPER | E05180 | 05 |
| DISPATCHER_PURE_MISMATCH | E05170 | 05 |
| DISPATCHERS_ONLY_HAVE_ONE_METHOD_ENTRY_POINT_MARKED | E07820 | 07 |
| DOES_NOT_OVERRIDE | E05110 | 05 |
| DUPLICATE_ENUMERATED_VALUES_PRESENT_IN_SWITCH | E02060 | 02 |
| DUPLICATE_NAME | E01030 | 01 |
| DUPLICATE_PROPERTY_FIELD | E02010 | 02 |
| DUPLICATE_TRAIT_REFERENCE | E02050 | 02 |
| DUPLICATE_TYPE | E01040 | 01 |
| DUPLICATE_VARIABLE | E50050 | 50 |
| DUPLICATE_VARIABLE_IN_CAPTURE | E02040 | 02 |
| DYNAMIC_CLASS_MUST_IMPLEMENT_ABSTRACTS | E07140 | 07 |
| EITHER_ALL_PARAMETERS_NAMED_OR_NONE | E06240 | 06 |
| ERROR_MISSING | E50230 | 50 |
| EXCESSIVE_COMPLEXITY | E11010 | 11 |
| EXPLICIT_CONSTRUCTOR_REQUIRED | E07170 | 07 |
| FUNCTION_DELEGATE_WITH_DEFAULT_OPERATORS | E07210 | 07 |
| FUNCTION_MUST_HAVE_NO_PARAMETERS | E07450 | 07 |
| FUNCTION_MUST_HAVE_SINGLE_PARAMETER | E07460 | 07 |
| FUNCTION_MUST_HAVE_TWO_PARAMETERS | E07470 | 07 |
| FUNCTION_MUST_RETURN_VALUE | E07490 | 07 |
| FUNCTION_OR_DELEGATE_NOT_REQUIRED | E07880 | 07 |
| FUNCTION_OR_DELEGATE_REQUIRED | E07860 | 07 |
| FUNCTION_PARAMETER_MISMATCH | E06270 | 06 |
| FUNCTION_SIGNATURE_DOES_NOT_MATCH_SUPER | E05140 | 05 |
| FUNCTION_USED_IN_GENERIC | E06120 | 06 |
| GENERIC_CONSTRUCTORS_MUST_BE_PUBLIC | E06060 | 06 |
| GENERIC_FUNCTION_IMPLEMENTATION_REQUIRED | E06100 | 06 |
| GENERIC_TYPE_CONSTRUCTOR_INAPPROPRIATE | E06030 | 06 |
| GENERIC_TYPE_OR_FUNCTION_PARAMETERS_INCORRECT | E06020 | 06 |
| GENERIC_TYPE_OR_FUNCTION_PARAMETERS_NEEDED | E06010 | 06 |
| GENERIC_TYPE_REQUIRES_CORRECT_CONSTRUCTOR_ARGUMENT_TYPES | E06050 | 06 |
| GENERIC_TYPE_REQUIRES_TWO_CONSTRUCTORS | E06040 | 06 |
| GENERIC_WITH_NAMED_DYNAMIC_CLASS | E06090 | 06 |
| GUARD_USED_IN_EXPRESSION | E07350 | 07 |
| IMPLEMENTATION_MUST_BE_PROVIDED | E07160 | 07 |
| INAPPROPRIATE_USE | E09010 | 09 |
| INAPPROPRIATE_USE_OF_SUPER | E05080 | 05 |
| INAPPROPRIATE_USE_OF_THIS | E05070 | 05 |
| INCOMPATIBLE_CATEGORY | E50100 | 50 |
| INCOMPATIBLE_GENUS | E50020 | 50 |
| INCOMPATIBLE_GENUS_CONSTRUCTOR | E05200 | 05 |
| INCOMPATIBLE_PARAMETER_GENUS | E50090 | 50 |
| INCOMPATIBLE_TYPE_ARGUMENTS | E06330 | 06 |
| INCOMPATIBLE_TYPES | E50030 | 50 |
| INTEGER_VAR_OR_FUNCTION_OR_DELEGATE_REQUIRED | E07870 | 07 |
| INVALID_DEFAULT_CONSTRUCTOR | E07080 | 07 |
| INVALID_MODULE_NAME | E01020 | 01 |
| INVALID_NUMBER_OF_PARAMETERS | E06320 | 06 |
| INVALID_SYMBOL_BY_REFERENCE | E01010 | 01 |
| INVALID_VALUE | E07900 | 07 |
| IS_NOT_AN_AGGREGATE_TYPE | E04060 | 04 |
| METHOD_ACCESS_MODIFIERS_DIFFER | E05130 | 05 |
| METHOD_ACCESS_MODIFIER_DEFAULT | E07280 | 07 |
| METHOD_ACCESS_MODIFIER_PRIVATE_OVERRIDE | E07010 | 07 |
| METHOD_AMBIGUOUS | E06140 | 06 |
| METHOD_DUPLICATED | E02030 | 02 |
| METHOD_MODIFIER_NOT_REQUIRED_IN_TRAIT | E07270 | 07 |
| METHOD_MODIFIER_PROTECTED_IN_CLOSED_CLASS | E07260 | 07 |
| METHOD_MODIFIER_PROTECTED_IN_COMPONENT | E07250 | 07 |
| METHOD_MODIFIER_PROTECTED_IN_SERVICE | E07240 | 07 |
| METHOD_NOT_RESOLVED | E50060 | 50 |
| METHOD_OVERRIDES | E05120 | 05 |
| METHODS_CONFLICT | E06150 | 06 |
| MISSING_ITERATE_METHOD | E07840 | 07 |
| MISSING_OPERATOR_IN_PROPERTY_TYPE | E07200 | 07 |
| MISSING_OPERATOR_IN_SUPER | E07190 | 07 |
| MISSING_OPERATOR_IN_THIS | E07180 | 07 |
| MIX_OF_PURE_AND_NOT_PURE_CONSTRUCTORS | E05190 | 05 |
| MUST_BE_A_BOOLEAN | E07540 | 07 |
| MUST_BE_INTEGER_GREATER_THAN_ZERO | E07560 | 07 |
| MUST_NOT_RETURN_SAME_TYPE | E07420 | 07 |
| MUST_RETURN_BOOLEAN | E07520 | 07 |
| MUST_RETURN_INTEGER | E07550 | 07 |
| MUST_RETURN_JSON | E07580 | 07 |
| MUST_RETURN_SAME_AS_CONSTRUCT_TYPE | E07410 | 07 |
| MUST_RETURN_STRING | E07570 | 07 |
| NAMED_PARAMETERS_MUST_MATCH_ARGUMENTS | E06250 | 06 |
| NEVER_INITIALISED | E08070 | 08 |
| NONE_PURE_CALL_IN_PURE_SCOPE | E08130 | 08 |
| NOT_ABSTRACT_AND_NO_BODY_PROVIDED | E07110 | 07 |
| NOT_ACCESSIBLE | E06180 | 06 |
| NOT_ALL_ENUMERATED_VALUES_PRESENT_IN_SWITCH | E07310 | 07 |
| NOT_A_FUNCTION_DELEGATE | E07480 | 07 |
| NOT_A_TEMPLATE | E04070 | 04 |
| NOT_IMMEDIATE_TRAIT | E06160 | 06 |
| NOT_INITIALISED_BEFORE_USE | E08060 | 08 |
| NOT_INITIALISED_IN_ANY_WAY | E08180 | 08 |
| NOT_MARKED_ABSTRACT_BUT_IS_ABSTRACT | E07130 | 07 |
| NOT_MUTABLE | E07890 | 07 |
| NOT_OPEN_TO_EXTENSION | E05030 | 05 |
| NOT_REFERENCED | E08090 | 08 |
| NOT_RESOLVED | E50001 | 50 |
| NO_INCOMING_ARGUMENT_REASSIGNMENT | E08110 | 08 |
| NO_MUTATION_IN_PURE_CONTEXT | E08120 | 08 |
| NO_PURE_REASSIGNMENT | E08100 | 08 |
| NO_REASSIGNMENT_WITHIN_SAFE_ACCESS | E08040 | 08 |
| ONLY_COMPATIBLE_WITH_BOOLEAN | E07530 | 07 |
| OPERATOR_CANNOT_BE_PURE | E07510 | 07 |
| OPERATOR_CANNOT_BE_USED_ON_ENUMERATION | E07630 | 07 |
| OPERATOR_DEFAULT_NOT_SUPPORTED | E07220 | 07 |
| OPERATOR_MUST_BE_PURE | E07500 | 07 |
| OPERATOR_NAME_USED_AS_METHOD | E07660 | 07 |
| OPERATOR_NOT_DEFINED | E07620 | 07 |
| OVERRIDE_AND_ABSTRACT | E07020 | 07 |
| OVERRIDE_CONSTRUCTOR | E07060 | 07 |
| OVERRIDE_INAPPROPRIATE | E05100 | 05 |
| PARAMETER_MISMATCH | E06260 | 06 |
| PARENTHESIS_NOT_REQUIRED | E06200 | 06 |
| PARENTHESIS_REQUIRED | E06210 | 06 |
| POINTLESS_EXPRESSION | E07390 | 07 |
| POSSIBLE_DUPLICATE_ENUMERATED_VALUE | E01050 | 01 |
| PRE_FLOW_SYMBOL_NOT_RESOLVED | E07340 | 07 |
| PROGRAM_ARGUMENTS_INAPPROPRIATE | E07610 | 07 |
| PROGRAM_ARGUMENT_TYPE_INVALID | E07600 | 07 |
| PROGRAM_CAN_ONLY_RETURN_INTEGER | E07590 | 07 |
| REASSIGNMENT_OF_INJECTED_COMPONENT | E08170 | 08 |
| RECORDS_ONLY_SUPPORT_CONSTRUCTOR_AND_OPERATOR_METHODS | E07290 | 07 |
| REFERENCES_CONFLICT | E03020 | 03 |
| REFERENCE_DOES_NOT_RESOLVED | E03030 | 03 |
| REQUIRE_NO_ARGUMENTS | E06310 | 06 |
| REQUIRE_ONE_ARGUMENT | E06300 | 06 |
| RESULT_MUST_HAVE_DIFFERENT_TYPES | E06190 | 06 |
| RETURNING_MISSING | E07400 | 07 |
| RETURNING_NOT_REQUIRED | -- | Special |
| RETURNING_REQUIRED | -- | Special |
| RETURN_NOT_ALWAYS_INITIALISED | E08050 | 08 |
| RETURN_TYPE_VOID_MEANINGLESS | E10010 | 10 |
| RETURN_UNREACHABLE | E07380 | 07 |
| RETURN_VALUE_NOT_SUPPORTED | E07430 | 07 |
| SELF_ASSIGNMENT | E08080 | 08 |
| SERVICE_HTTP_ACCESS_NOT_SUPPORTED | E07690 | 07 |
| SERVICE_HTTP_PARAM_NEEDS_QUALIFIER | E07710 | 07 |
| SERVICE_HTTP_PARAM_QUALIFIER_NOT_ALLOWED | E07720 | 07 |
| SERVICE_HTTP_PATH_DUPLICATED | E02070 | 02 |
| SERVICE_HTTP_PATH_PARAM_COUNT_INVALID | E07730 | 07 |
| SERVICE_HTTP_PATH_PARAM_INVALID | E07700 | 07 |
| SERVICE_INCOMPATIBLE_PARAM_TYPE | E07760 | 07 |
| SERVICE_INCOMPATIBLE_PARAM_TYPE_NON_REQUEST | E07790 | 07 |
| SERVICE_INCOMPATIBLE_PARAM_TYPE_REQUEST | E07770 | 07 |
| SERVICE_INCOMPATIBLE_RETURN_TYPE | E07750 | 07 |
| SERVICE_MISSING_RETURN | E07800 | 07 |
| SERVICE_OPERATOR_NOT_SUPPORTED | E07670 | 07 |
| SERVICE_REQUEST_BY_ITSELF | E07780 | 07 |
| SERVICE_URI_WITH_VARS_NOT_SUPPORTED | E07680 | 07 |
| SERVICE_WITH_NO_BODY_PROVIDED | E07740 | 07 |
| SINGLE_EXCEPTION_ONLY | E07850 | 07 |
| STATEMENT_UNREACHABLE | E07370 | 07 |
| STREAM_TYPE_NOT_DEFINED | E10020 | 10 |
| SUPER_FOR_ANY_NOT_REQUIRED | E05040 | 05 |
| SUPER_IS_NOT_PURE | E05160 | 05 |
| SUPER_IS_PURE | E05150 | 05 |
| TEMPLATE_TYPE_REQUIRES_PARAMETERIZATION | E04080 | 04 |
| TEXT_METHOD_MISSING | E07150 | 07 |
| THIS_AND_SUPER_CALLS_ONLY_IN_CONSTRUCTOR | E05060 | 05 |
| THIS_AND_SUPER_MUST_BE_FIRST_IN_CONSTRUCTOR | E05050 | 05 |
| TOO_FEW_ARGUMENTS | E06290 | 06 |
| TOO_MANY_ARGUMENTS | E06280 | 06 |
| TRAITS_DO_NOT_HAVE_CONSTRUCTORS | E07070 | 07 |
| TRAIT_ACCESS_NOT_SUPPORTED | E06170 | 06 |
| TRAIT_BY_IDENTIFIER_NOT_SUPPORTED | E07040 | 07 |
| TYPE_CANNOT_BE_CONSTRAINED | E04010 | 04 |
| TYPE_INFERENCE_NOT_SUPPORTED | E06070 | 06 |
| TYPE_MUST_BE_CONVERTABLE_TO_STRING | E04020 | 04 |
| TYPE_MUST_BE_FUNCTION | E04030 | 04 |
| TYPE_MUST_BE_SIMPLE | E04050 | 04 |
| TYPE_MUST_EXTEND_EXCEPTION | E04030 | 04 |
| TYPE_NOT_RESOLVED | E50010 | 50 |
| UNABLE_TO_FIND_PIPE_FOR_TYPE | E07830 | 07 |
| UNKNOWN_DIRECTIVE | E50200 | 50 |
| UNSAFE_METHOD_ACCESS | E08030 | 08 |
| USED_BEFORE_DEFINED | E08010 | 08 |
| USED_BEFORE_INITIALISED | E08020 | 08 |
| USE_OF_THIS_OR_SUPER_INAPPROPRIATE | E05090 | 05 |
| VALUES_AND_TYPE_INCOMPATIBLE | E06220 | 06 |

---

## Special Error Codes (Not Mapped)

The following two errors are raised through special methods and may not need standard error codes:

| SemanticClassification | Method | Notes |
|------------------------|--------|-------|
| RETURNING_NOT_REQUIRED | raiseReturningNotRequired() | Special handling for returning block validation |
| RETURNING_REQUIRED | raiseReturningRequired() | Special handling for returning block validation |

These could be assigned codes (e.g., E07440-E07450) if standard error code reporting is desired.

---

## Summary Statistics

- **Total SemanticClassification enum values:** 215
- **Multi-phase errors (E50xxx):** 23
- **Single-phase errors:** 190
- **Special error methods:** 2 (RETURNING_REQUIRED, RETURNING_NOT_REQUIRED)
- **Phase with most errors:** Phase 07 (POST_RESOLUTION_CHECKS) with 100+ errors
- **Phase with fewest errors:** Phase 09 (PLUGIN_RESOLUTION) with 1 error

---

## Implementation Notes

### Using Error Codes in ErrorListener.java

To implement these error codes, each SemanticClassification enum value should be annotated with its error code:

```java
public enum SemanticClassification {
  // Example implementation
  NOT_RESOLVED("E50001", "not resolved"),
  TYPE_NOT_RESOLVED("E50010", "type not resolved"),
  INCOMPATIBLE_GENUS("E50020", "incompatible genus"),
  // ... etc

  private final String errorCode;
  private final String description;

  SemanticClassification(String errorCode, String description) {
    this.errorCode = errorCode;
    this.description = description;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
```

### Error Message Format

Error messages should include the error code for easy reference:

```
Error   : 'variableName' on line 42 position 10: E08020 might be used before being initialised
```

---

## Appendix: Phase-Based Error Count

| Phase | Error Count | Percentage |
|-------|-------------|------------|
| Phase 01 (SYMBOL_DEFINITION) | 5 | 2.3% |
| Phase 02 (DUPLICATION_CHECK) | 8 | 3.7% |
| Phase 03 (REFERENCE_CHECKS) | 3 | 1.4% |
| Phase 04 (EXPLICIT_TYPE_SYMBOL_DEFINITION) | 8 | 3.7% |
| Phase 05 (TYPE_HIERARCHY_CHECKS) | 20 | 9.3% |
| Phase 06 (FULL_RESOLUTION) | 33 | 15.3% |
| Phase 07 (POST_RESOLUTION_CHECKS) | 98 | 45.6% |
| Phase 08 (PRE_IR_CHECKS) | 18 | 8.4% |
| Phase 09 (PLUGIN_RESOLUTION) | 1 | 0.5% |
| Phase 10 (IR_GENERATION) | 3 | 1.4% |
| Phase 11 (IR_ANALYSIS) | 1 | 0.5% |
| Phase 50 (MULTI_PHASE) | 23 | 10.7% |
| **Total** | **215** | **100%** |

---

**End of Error Code Mapping Document**
