# EK9 Error Code Quick Reference

**Fast lookup table for implementing error codes in ErrorListener.java**

## Implementation Template

```java
public enum SemanticClassification {
  // Example with error code
  NOT_RESOLVED("E50001", "not resolved"),
  TYPE_NOT_RESOLVED("E50010", "type not resolved"),

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

## Error Code Assignments by Enum Order

This table follows the exact order of enum values in ErrorListener.java for easy copy-paste implementation:

```java
// DIRECTIVE ERRORS (Multi-Phase E50xxx)
UNKNOWN_DIRECTIVE("E50200", "invalid directive"),
DIRECTIVE_MISSING("E50210", "but the directive is missing"),
DIRECTIVE_WRONG_CLASSIFICATION("E50220", "directive error classification incorrect"),
ERROR_MISSING("E50230", "but the error is missing"),
DIRECTIVE_SYMBOL_COMPLEXITY("E50240", "complexity mismatch"),
EXCESSIVE_COMPLEXITY("E11010", "excessive complexity - refactor"),
DIRECTIVE_SYMBOL_NOT_RESOLVED("E50250", "symbol not resolved"),
DIRECTIVE_HIERARCHY_NOT_RESOLVED("E50260", "symbol hierarchy not in place"),
DIRECTIVE_SYMBOL_CATEGORY_MISMATCH("E50270", "symbol category mismatched"),
DIRECTIVE_SYMBOL_GENUS_MISMATCH("E50280", "symbol genus mismatched"),
DIRECTIVE_SYMBOL_NO_SUCH_GENUS("E50290", "genus does not exist"),
DIRECTIVE_SYMBOL_FOUND_UNEXPECTED_SYMBOL("E50300", "unexpected symbol resolved"),
DIRECTIVE_ERROR_MISMATCH("E50310", "count does not match"),

// MULTI-PHASE ERRORS (E50xxx)
NOT_RESOLVED("E50001", "not resolved"),
CIRCULAR_HIERARCHY_DETECTED("E05020", "a circular type/function hierarchy has been used"),
METHOD_NOT_RESOLVED("E50060", "method/function not resolved"),
TYPE_NOT_RESOLVED("E50010", "type not resolved"),
INAPPROPRIATE_USE("E09010", "inappropriate use in this context"),

// PHASE 10: IR_GENERATION
RETURN_TYPE_VOID_MEANINGLESS("E10010", "'void' return type cannot be used with an assignment"),

// PHASE 04: TYPE RESOLUTION
TYPE_CANNOT_BE_CONSTRAINED("E04010", "is not a candidate to be constrained"),
TYPE_MUST_BE_CONVERTABLE_TO_STRING("E04020", "result must be String, have the $ operator or can be promoted to String"),

// PHASE 06: FULL_RESOLUTION
PARENTHESIS_NOT_REQUIRED("E06200", "use of parenthesis '( )' not allowed in this context"),
PARENTHESIS_REQUIRED("E06210", "parenthesis '( )' required in this context"),
VALUES_AND_TYPE_INCOMPATIBLE("E06220", "choose either empty parenthesis '( )' with values or a type definition"),
GENERIC_TYPE_OR_FUNCTION_PARAMETERS_NEEDED("E06010", "type/function is generic but no parameters were supplied"),
GENERIC_TYPE_OR_FUNCTION_PARAMETERS_INCORRECT("E06020", "type/function is generic, but incorrect number of parameters supplied"),
GENERIC_TYPE_CONSTRUCTOR_INAPPROPRIATE("E06030", "type is generic, but for type inference to work; the number of generic and constructor parameters must be the same"),
GENERIC_TYPE_REQUIRES_TWO_CONSTRUCTORS("E06040", "a generic type requires 2 constructors, default and inferred type"),
GENERIC_TYPE_REQUIRES_CORRECT_CONSTRUCTOR_ARGUMENT_TYPES("E06050", "a generic type requires correct constructor argument to match parametric types (and order)"),
GENERIC_CONSTRUCTORS_MUST_BE_PUBLIC("E06060", "a generic type does not support private or protected constructors"),
TYPE_INFERENCE_NOT_SUPPORTED("E06070", "type inference is not supported within generic/template type/functions"),
CONSTRAINED_FUNCTIONS_NOT_SUPPORTED("E06080", "the constraining type with a generic/template cannot be a function"),

// PHASE 05: TYPE_HIERARCHY_CHECKS
USE_OF_THIS_OR_SUPER_INAPPROPRIATE("E05090", "can be used with :=:, :~:, +=, -=, /= and *=. But not direct assignment"),

// PHASE 07: SERVICE ERRORS
SERVICE_URI_WITH_VARS_NOT_SUPPORTED("E07680", "URI with place holder variable not supported in this context"),
SERVICE_HTTP_ACCESS_NOT_SUPPORTED("E07690", "HTTP access verb not supported in this context"),
SERVICE_HTTP_PATH_PARAM_INVALID("E07700", "HTTP PATH parameter is invalid"),
SERVICE_HTTP_PARAM_NEEDS_QUALIFIER("E07710", "additional name needed for QUERY, HEADER or PATH parameters"),
SERVICE_HTTP_PARAM_QUALIFIER_NOT_ALLOWED("E07720", "additional name only required for QUERY, HEADER or PATH parameters"),
SERVICE_HTTP_PATH_DUPLICATED("E02070", "PATH is duplicated in terms of structure/naming"),
SERVICE_HTTP_PATH_PARAM_COUNT_INVALID("E07730", "HTTP PATH variable count and PATH parameter count mismatch"),
SERVICE_WITH_NO_BODY_PROVIDED("E07740", "implementation not provided, but services cannot be abstract"),
SERVICE_INCOMPATIBLE_RETURN_TYPE("E07750", "Web Service return type must be compatible with HTTPResponse"),
SERVICE_INCOMPATIBLE_PARAM_TYPE("E07760", "only Integer, String, Date, Time, DateTime, Milliseconds, Duration and HTTPRequest supported"),
SERVICE_INCOMPATIBLE_PARAM_TYPE_REQUEST("E07770", "Web Service parameter type must be HTTPRequest for REQUEST"),
SERVICE_REQUEST_BY_ITSELF("E07780", "Web Service type HTTPRequest can only be used by itself"),
SERVICE_INCOMPATIBLE_PARAM_TYPE_NON_REQUEST("E07790", "Web Service parameter type cannot be HTTPRequest"),
SERVICE_MISSING_RETURN("E07800", "Web Service must have return value and it must be compatible with HTTPResponse"),

// PHASE 07: RETURNING ERRORS
RETURNING_REQUIRED("E07405", "returning block required for assignment to 'lhs' when used in expression"),
RETURNING_MISSING("E07400", "returning variable and type missing"),
RETURNING_NOT_REQUIRED("E07406", "returning block is not required as there is no 'lhs' variable to assign it to"),
MUST_RETURN_SAME_AS_CONSTRUCT_TYPE("E07410", "returning type must be same as the construct type"),
MUST_NOT_RETURN_SAME_TYPE("E07420", "returning type must not be same for promotion"),

// PHASE 07: METHOD MODIFIERS
METHOD_ACCESS_MODIFIER_PRIVATE_OVERRIDE("E07010", "remove 'override' with use of 'private' access modifier"),
OVERRIDE_AND_ABSTRACT("E07020", "'override' of a method/operator and 'abstract' (no implementation) is not logical"),
DEFAULT_AND_TRAIT("E07030", "'default' of operators on a trait is not supported"),
DUPLICATE_TRAIT_REFERENCE("E02050", "same trait referenced multiple times"),
TRAIT_BY_IDENTIFIER_NOT_SUPPORTED("E07040", "'by' variable is not supported on a trait only on a class"),

// PHASE 07: DEFAULT OPERATORS
MISSING_OPERATOR_IN_THIS("E07180", "'default' of operators requires this type to have appropriate operator"),
MISSING_OPERATOR_IN_SUPER("E07190", "'default' of operators requires super to have appropriate operator"),
MISSING_OPERATOR_IN_PROPERTY_TYPE("E07200", "'default' of operators requires property/field to have appropriate operator"),
FUNCTION_DELEGATE_WITH_DEFAULT_OPERATORS("E07210", "'default' of operators can only support '?' operator with function delegate fields"),
OPERATOR_DEFAULT_NOT_SUPPORTED("E07220", "'default' is not supported on this operator"),
DEFAULT_WITH_OPERATOR_SIGNATURE("E07230", "'default' with an operator must not have signature or body"),

// PHASE 07: CONSTRUCTOR ERRORS
ABSTRACT_CONSTRUCTOR("E07050", "'abstract' modifier on a constructor is not logical"),
OVERRIDE_CONSTRUCTOR("E07060", "'override' is not required on a constructor"),
TRAITS_DO_NOT_HAVE_CONSTRUCTORS("E07070", "traits do not support constructor methods"),
INVALID_DEFAULT_CONSTRUCTOR("E07080", "'default' constructor with parameters is not supported"),
DEFAULT_ONLY_FOR_CONSTRUCTORS("E07090", "'default' modifier is only valid for constructors, not regular methods"),
EXPLICIT_CONSTRUCTOR_REQUIRED("E07170", "a developer coded constructor(s) are require where uninitialized properties are used"),

// PHASE 02: DUPLICATION
DUPLICATE_ENUMERATED_VALUES_PRESENT_IN_SWITCH("E02060", "duplicated enumerated value in switch 'case'"),

// PHASE 07: SWITCH ERRORS
NOT_ALL_ENUMERATED_VALUES_PRESENT_IN_SWITCH("E07310", "'cases' should cover all enumerated values in 'switch'"),
DEFAULT_REQUIRED_IN_SWITCH_STATEMENT("E07320", "'default' is required in this 'switch' statement"),
DEFAULT_REQUIRED_IN_SWITCH_EXPRESSION("E07330", "'default' is required in this 'switch' expression"),
PRE_FLOW_SYMBOL_NOT_RESOLVED("E07340", "without a control, failed to find subject of flow"),
GUARD_USED_IN_EXPRESSION("E07350", "a 'guard' cannot be used in an expression as it may leave 'lhs' uninitialised"),

// PHASE 07: ACCESS MODIFIERS
METHOD_MODIFIER_PROTECTED_IN_SERVICE("E07240", "non web service methods cannot be marked with the 'protected' access modifier"),
METHOD_MODIFIER_PROTECTED_IN_COMPONENT("E07250", "component methods cannot be marked with the 'protected' access modifier"),
METHOD_MODIFIER_PROTECTED_IN_CLOSED_CLASS("E07260", "class methods can only be marked with the 'protected' access modifier in classes that are 'open'"),
METHOD_MODIFIER_NOT_REQUIRED_IN_TRAIT("E07270", "trait methods cannot be marked with an access modifier, they are always public"),
RECORDS_ONLY_SUPPORT_CONSTRUCTOR_AND_OPERATOR_METHODS("E07290", "only constructors and operators methods are supported on records"),
DECLARED_AS_NULL_NOT_NEEDED("E07300", "declaration supporting 'uninitialised' is not needed"),
METHOD_ACCESS_MODIFIER_DEFAULT("E07280", "access modifier is not needed here - methods are 'public' by default"),

// PHASE 06: METHOD RESOLUTION
METHODS_CONFLICT("E06150", "conflicting methods to be resolved"),

// PHASE 07: CONTROL FLOW
APPLICATION_SELECTION_INVALID("E07360", "application selection not allowed in this context"),
STATEMENT_UNREACHABLE("E07370", "all paths lead to an Exception"),
RETURN_UNREACHABLE("E07380", "return not possible, as instructions only result in an Exception"),
POINTLESS_EXPRESSION("E07390", "constant Boolean literal in expression is pointless"),

// PHASE 04: TYPE REQUIREMENTS
TYPE_MUST_EXTEND_EXCEPTION("E04030", "type must be of Exception type"),
SINGLE_EXCEPTION_ONLY("E07850", "only a single Exception is supported"),
TYPE_MUST_BE_FUNCTION("E04030", "type must be a function or delegate"),
TYPE_MUST_BE_SIMPLE("E04050", "type must be a simple aggregate/list/dict"),

// PHASE 07: FUNCTION/DELEGATE REQUIREMENTS
FUNCTION_OR_DELEGATE_REQUIRED("E07860", "require a function or function delegate"),
INTEGER_VAR_OR_FUNCTION_OR_DELEGATE_REQUIRED("E07870", "require an Integer value or function/function delegate"),
FUNCTION_OR_DELEGATE_NOT_REQUIRED("E07880", "a function or function delegate is not required here"),
FUNCTION_MUST_HAVE_NO_PARAMETERS("E07450", "function must have no parameters"),
FUNCTION_MUST_HAVE_SINGLE_PARAMETER("E07460", "function must have a single parameter"),
FUNCTION_MUST_HAVE_TWO_PARAMETERS("E07470", "function must have a two parameters"),
FUNCTION_PARAMETER_MISMATCH("E06270", "parameter mismatch"),
NOT_A_FUNCTION_DELEGATE("E07480", "is not a function delegate"),
DELEGATE_AND_METHOD_NAMES_CLASH("E02080", "use of a delegate and methods of same name"),

// PHASE 10: STREAM ERRORS
STREAM_TYPE_NOT_DEFINED("E10020", "Void cannot be used in stream pipelines"),

// PHASE 07: TYPE REQUIREMENTS
MUST_RETURN_BOOLEAN("E07520", "must return a Boolean"),
ONLY_COMPATIBLE_WITH_BOOLEAN("E07530", "only compatible with Boolean type"),
MUST_BE_A_BOOLEAN("E07540", "is not compatible with a Boolean type"),
MUST_RETURN_INTEGER("E07550", "must return an Integer"),
MUST_BE_INTEGER_GREATER_THAN_ZERO("E07560", "must be an Integer with a value greater than zero"),
MUST_RETURN_STRING("E07570", "must return a String"),
MUST_RETURN_JSON("E07580", "must return a JSON"),

// PHASE 07: PROGRAM CONSTRAINTS
PROGRAM_CAN_ONLY_RETURN_INTEGER("E07590", "if a program returns a value it can only be an Integer"),
PROGRAM_ARGUMENT_TYPE_INVALID("E07600", "program arguments are limited to a finite range of EK9 built in types"),
PROGRAM_ARGUMENTS_INAPPROPRIATE("E07610", "inappropriate combination of program arguments"),

// PHASE 06: ARGUMENT ERRORS
TOO_MANY_ARGUMENTS("E06280", "too many arguments"),
TOO_FEW_ARGUMENTS("E06290", "too few arguments"),
REQUIRE_ONE_ARGUMENT("E06300", "require one argument only"),
REQUIRE_NO_ARGUMENTS("E06310", "function must have no arguments"),

// PHASE 07: FUNCTION/OPERATOR REQUIREMENTS
FUNCTION_MUST_RETURN_VALUE("E07490", "function must return a value"),
OPERATOR_MUST_BE_PURE("E07500", "operator must be declared pure"),
OPERATOR_CANNOT_BE_PURE("E07510", "operator must not be declared pure"),

// PHASE 02: DUPLICATION
DUPLICATE_PROPERTY_FIELD("E02010", "Property/Field duplicated"),
CANNOT_SUPPORT_TO_JSON_DUPLICATE_PROPERTY_FIELD("E02020", "Property/Field duplicated, $$ (JSON) operator not supported"),
METHOD_DUPLICATED("E02030", "duplicate/ambiguous methods/operations"),
DUPLICATE_VARIABLE("E50050", "Variable/Constant duplicated"),
DUPLICATE_VARIABLE_IN_CAPTURE("E02040", "variable name duplicated, resulting in multiple fields of same name"),

// PHASE 01: SYMBOL DEFINITION
DUPLICATE_NAME("E01030", "Variable/Function/Type duplicated, likely to lead to confusion"),
POSSIBLE_DUPLICATE_ENUMERATED_VALUE("E01050", "are duplicated values (or are too similar, likely to be confusing)"),
DUPLICATE_TYPE("E01040", "Type/Function name duplicated"),
INVALID_SYMBOL_BY_REFERENCE("E01010", "invalid reference naming; module scope name missing"),
INVALID_MODULE_NAME("E01020", "invalid module name"),

// PHASE 03: REFERENCE CHECKS
CONSTRUCT_REFERENCE_CONFLICT("E03010", "conflicts with a reference"),
REFERENCES_CONFLICT("E03020", "conflicting references"),
REFERENCE_DOES_NOT_RESOLVED("E03030", "reference does not resolve"),

// PHASE 07: DISPATCH ERRORS
DISPATCH_ONLY_SUPPORTED_IN_CLASSES("E07810", "Dispatch only supported in classes"),

// PHASE 07: ABSTRACT ERRORS
ABSTRACT_BUT_BODY_PROVIDED("E07100", "defined as default/abstract but an implementation has been provided"),
CANNOT_BE_ABSTRACT("E50040", "cannot be abstract"),
CANNOT_CALL_ABSTRACT_TYPE("E50080", "cannot make a call on an abstract function/type directly"),
BAD_ABSTRACT_FUNCTION_USE("E50070", "cannot use an abstract function in this manner"),
OVERRIDE_INAPPROPRIATE("E05100", "cannot override anything"),
NOT_ABSTRACT_AND_NO_BODY_PROVIDED("E07110", "implementation not provided so must be declared as abstract"),
DISPATCHER_BUT_NO_BODY_PROVIDED("E07120", "base level implementation must be provided for dispatcher method"),

// PHASE 06: GENERIC/DYNAMIC CLASS
GENERIC_WITH_NAMED_DYNAMIC_CLASS("E06090", "a named dynamic class cannot be used within a generic type/function"),
CAPTURED_VARIABLE_MUST_BE_NAMED("E06230", "variables being captured must be named when not just using identifiers"),
EITHER_ALL_PARAMETERS_NAMED_OR_NONE("E06240", "either all variable must be named or none, when passing parameters"),
NAMED_PARAMETERS_MUST_MATCH_ARGUMENTS("E06250", "the order and naming of arguments must match parameters"),
GENERIC_FUNCTION_IMPLEMENTATION_REQUIRED("E06100", "implementation must be provided for generic function"),
NOT_MARKED_ABSTRACT_BUT_IS_ABSTRACT("E07130", "not declared abstract but still has abstract methods/operators"),
DYNAMIC_CLASS_MUST_IMPLEMENT_ABSTRACTS("E07140", "all abstract methods/operators must be implemented"),
TEXT_METHOD_MISSING("E07150", "text method missing for language variant"),
IMPLEMENTATION_MUST_BE_PROVIDED("E07160", "implementation must be provided"),

// PHASE 06: PARAMETER ERRORS
PARAMETER_MISMATCH("E06260", "parameter mismatch"),

// PHASE 07: STREAM/PIPE ERRORS
UNABLE_TO_FIND_PIPE_FOR_TYPE("E07830", "unable to find a '|' pipe operator for type"),

// MULTI-PHASE: TYPE COMPATIBILITY
INCOMPATIBLE_TYPES("E50030", "types are not compatible with each other"),
INCOMPATIBLE_TYPE_ARGUMENTS("E06330", "argument types are not compatible with each other"),

// PHASE 10: CONSTRUCTOR ERRORS
CONSTRUCTOR_USED_ON_ABSTRACT_TYPE("E10030", "use of a constructor directly on an abstract type is not permitted."),
CONSTRUCTOR_WITH_FUNCTION_IN_GENERIC("E06110", "use of a constructor in a generic/template that uses a function is not supported"),
FUNCTION_USED_IN_GENERIC("E06120", "functions can be used in generics/templates but only '?' is supported"),

// PHASE 04: AGGREGATE ERRORS
IS_NOT_AN_AGGREGATE_TYPE("E04060", "not an aggregate type"),
MISSING_ITERATE_METHOD("E07840", "it does not have compatible iterator() - hasNext()/next() methods"),

// PHASE 05: SUPER/THIS ERRORS
SUPER_FOR_ANY_NOT_REQUIRED("E05040", "'super' for implicit 'Any' base class is not required"),
THIS_AND_SUPER_MUST_BE_FIRST_IN_CONSTRUCTOR("E05050", "'this()' and 'super()' must be the first statement in a constructor"),
THIS_AND_SUPER_CALLS_ONLY_IN_CONSTRUCTOR("E05060", "'this()' and 'super()' can only be used in constructors, did you mean 'this.' or 'super.'"),
INAPPROPRIATE_USE_OF_THIS("E05070", "inappropriate use of 'this'"),
INAPPROPRIATE_USE_OF_SUPER("E05080", "inappropriate use of 'super'"),

// PHASE 07: MUTABILITY
NOT_MUTABLE("E07890", "not mutable"),

// MULTI-PHASE: GENUS/CATEGORY
INCOMPATIBLE_PARAMETER_GENUS("E50090", "incompatible genus in parameter(s)"),
INCOMPATIBLE_GENUS("E50020", "incompatible genus"),
INCOMPATIBLE_GENUS_CONSTRUCTOR("E05200", "incompatible genus with local constructor use"),
INCOMPATIBLE_CATEGORY("E50100", "incompatible category"),

// PHASE 06: GENERIC CONSTRAINTS
CONSTRAINED_TYPE_CONSTRUCTOR_MISSING("E06130", "constraining type constructors must exist on parameterizing type"),
NOT_OPEN_TO_EXTENSION("E05030", "not open to be extended"),
TEMPLATE_TYPE_REQUIRES_PARAMETERIZATION("E04080", "Template/Generic requires parameterization"),
NOT_A_TEMPLATE("E04070", "as it is not 'template/generic' in nature"),
RESULT_MUST_HAVE_DIFFERENT_TYPES("E06190", "EK9 Result must be used with two different types"),

// PHASE 07: DISPATCHER
DISPATCHERS_ONLY_HAVE_ONE_METHOD_ENTRY_POINT_MARKED("E07820", "only one method can be marked as a dispatcher entry point"),
INVALID_NUMBER_OF_PARAMETERS("E06320", "invalid number of parameters"),

// PHASE 07: OPERATOR ERRORS
OPERATOR_NOT_DEFINED("E07620", "operator not defined"),
OPERATOR_CANNOT_BE_USED_ON_ENUMERATION("E07630", "operator cannot be used on an Enumeration in this way"),
BAD_NOT_EQUAL_OPERATOR("E07640", "use <> for the not equal operator"),
BAD_NOT_OPERATOR("E07650", "use ~ for the not operator"),
OPERATOR_NAME_USED_AS_METHOD("E07660", "operator name cannot be used as a method"),
SERVICE_OPERATOR_NOT_SUPPORTED("E07670", "operator not supported, only +, +=, -, -=, :^:, :~: and ? are supported"),

// PHASE 06: METHOD RESOLUTION
METHOD_AMBIGUOUS("E06140", "ambiguous match"),
NOT_IMMEDIATE_TRAIT("E06160", "not an immediate trait of this context"),
TRAIT_ACCESS_NOT_SUPPORTED("E06170", "trait method access not supported here"),
NOT_ACCESSIBLE("E06180", "not accessible from this context"),

// PHASE 05: METHOD OVERRIDES
METHOD_OVERRIDES("E05120", "as it overrides method of same name/signature in hierarchy"),
DOES_NOT_OVERRIDE("E05110", "does not 'override' any method/operator"),
METHOD_ACCESS_MODIFIERS_DIFFER("E05130", "methods with same signature have different access modifiers"),
FUNCTION_SIGNATURE_DOES_NOT_MATCH_SUPER("E05140", "function signature does not match 'super' function"),
SUPER_IS_PURE("E05150", "'pure' in super requires 'pure' for this definition"),
DISPATCHER_PURE_MISMATCH("E05170", "'pure' on dispatcher requires 'pure' for matching dispatcher method"),
DISPATCHER_PRIVATE_IN_SUPER("E05180", "same method name as dispatcher, but marked private in super - won't be called"),
SUPER_IS_NOT_PURE("E05160", "'super is not 'pure', requires this definition not to be marked as 'pure'"),
MIX_OF_PURE_AND_NOT_PURE_CONSTRUCTORS("E05190", "if any constructor is marked pure, all constructors must be pure"),

// PHASE 08: PRE_IR_CHECKS (Code Flow Analysis)
USED_BEFORE_DEFINED("E08010", "used before definition"),
USED_BEFORE_INITIALISED("E08020", "might be used before being initialised"),
UNSAFE_METHOD_ACCESS("E08030", "has not been checked before access"),
NO_REASSIGNMENT_WITHIN_SAFE_ACCESS("E08040", "Reassignment/mutation within possible 'safe method access' scope is not allowed"),
RETURN_NOT_ALWAYS_INITIALISED("E08050", "return value is not always initialised"),
NOT_INITIALISED_BEFORE_USE("E08060", "is/may not be initialised before use"),
NEVER_INITIALISED("E08070", "never initialised"),
SELF_ASSIGNMENT("E08080", "self assignment"),
NOT_REFERENCED("E08090", "is not referenced anywhere, or not referenced after assignment"),
NO_PURE_REASSIGNMENT("E08100", "reassignment not allowed when scope is marked as 'pure' (':=?' is supported)"),
NO_INCOMING_ARGUMENT_REASSIGNMENT("E08110", "reassignment not allowed of an incoming argument/parameter"),
NO_MUTATION_IN_PURE_CONTEXT("E08120", "mutating variables is not allowed when scope is marked as 'pure'"),

// PHASE 07: COVARIANCE
COVARIANCE_MISMATCH("E07440", "return types are incompatible (covariance required)"),
RETURN_VALUE_NOT_SUPPORTED("E07430", "return not required/supported in this context"),

// PHASE 08: COMPONENT INJECTION
COMPONENT_INJECTION_IN_PURE("E08140", "component injection not allowed when scope is marked as 'pure'"),
COMPONENT_INJECTION_OF_NON_ABSTRACT("E08150", "dependency injection of a non-abstract component is not allowed, use an abstract base component"),
COMPONENT_INJECTION_NOT_POSSIBLE("E08160", "dependency injection is not allowed"),
REASSIGNMENT_OF_INJECTED_COMPONENT("E08170", "direct reassignment of an injected component is not allowed, use ':=?' for conditional reassignment"),
NOT_INITIALISED_IN_ANY_WAY("E08180", "not marked for injection nor initialised"),

// PHASE 08: PURE SCOPE
NONE_PURE_CALL_IN_PURE_SCOPE("E08130", "is not marked 'pure', but call is made in a scope that is marked as 'pure'"),

// PHASE 07: MISC
INVALID_VALUE("E07900", "Invalid value");
```

## Quick Phase Lookup

| Phase | Code Range | Primary Focus |
|-------|------------|---------------|
| 01 | E01xxx | Symbol definition and naming |
| 02 | E02xxx | Duplication detection |
| 03 | E03xxx | Reference validation |
| 04 | E04xxx | Type resolution and constraints |
| 05 | E05xxx | Type hierarchy and inheritance |
| 06 | E06xxx | Template/generic resolution |
| 07 | E07xxx | Post-resolution validation (largest phase) |
| 08 | E08xxx | Code flow and initialization analysis |
| 09 | E09xxx | Plugin resolution |
| 10 | E10xxx | IR generation |
| 11 | E11xxx | IR analysis |
| 50 | E50xxx | Multi-phase errors |

## Next Steps for Implementation

1. Add `errorCode` field to `SemanticClassification` enum
2. Update enum constructor to accept error code parameter
3. Add getter method `getErrorCode()`
4. Update all enum values with their assigned error codes (use table above)
5. Modify error message formatting to include error code:
   ```java
   String.format("Error   : '%s' on line %d position %d: %s %s",
       symbol, line, position, classification.getErrorCode(), classification.description)
   ```

## Verification

After implementation, verify:
- All 215 enum values have unique error codes
- Error codes follow phase-based numbering scheme
- No gaps in critical error sequences
- Error messages display codes consistently

---

**For complete documentation see:** `EK9_ERROR_CODE_MAPPING.md`
