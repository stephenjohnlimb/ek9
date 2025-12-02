package org.ek9lang.compiler.common;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.search.MatchResults;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.OsSupport;

/**
 * We need an error listener for the lexing parsing and also our own tree
 * visiting. So our own tree visiting will be able to use additional methods we
 * add on to this; so we can report semantic errors and warnings.
 * Why? Because we need to gather-up all the errors on a per-file processing
 * basis. This is needed because we are going to multi-thread the various stages
 * of compilation based on a thread per source file. So, we would not want the
 * error information being interleaved in the console output. Plus eventually
 * there will be a UI over the compiler, and we will need very focused sets of
 * errors on a per source code basis.
 * This is not designed to be thread safe, only one thread should be parsing/processing
 * a source or one a specific phase.
 */
public class ErrorListener extends BaseErrorListener implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;
  private final OsSupport osSupport = new OsSupport();
  private final String generalIdentifierOfSource;
  private boolean exceptionOnAmbiguity = false;
  private boolean exceptionOnContextSensitive = false;
  private boolean exceptionOnFullContext = false;
  //Now have @ type directives we can hold these to one side.
  private List<ErrorDetails> directiveErrors = new ArrayList<>();
  private List<ErrorDetails> errors = new ArrayList<>();
  //This is so we can limit the number of errors we output when we get multiple triggers for
  //the same type/variable but for different reasons.
  //Normally it's the first reason that is the cause, the rest are just follow ones from that.
  private HashMap<String, ErrorDetails> uniqueErrors = new HashMap<>();
  private List<ErrorDetails> warnings = new ArrayList<>();

  /**
   * Create new error listener.
   */
  public ErrorListener(final String generalIdentifierOfSource) {

    this.generalIdentifierOfSource = generalIdentifierOfSource;
    reset();

  }

  public String getGeneralIdentifierOfSource() {

    return generalIdentifierOfSource;
  }

  /**
   * Remove all errors and warnings.
   */
  public void reset() {

    directiveErrors = new ArrayList<>();
    errors = new ArrayList<>();
    uniqueErrors = new HashMap<>();
    warnings = new ArrayList<>();

  }

  /**
   * Get the filename (but not full path) of the source file.
   */
  public String getShortNameOfSourceFile(final IToken token) {

    AssertValue.checkNotNull("Token cannot be null", token);

    return osSupport.getFileNameWithoutPath(token.getSourceName());
  }

  public boolean isExceptionOnAmbiguity() {

    return exceptionOnAmbiguity;
  }

  public void setExceptionOnAmbiguity(final boolean exceptionOnAmbiguity) {

    this.exceptionOnAmbiguity = exceptionOnAmbiguity;
  }

  public boolean isExceptionOnContextSensitive() {

    return exceptionOnContextSensitive;
  }

  public void setExceptionOnContextSensitive(final boolean exceptionOnContextSensitive) {

    this.exceptionOnContextSensitive = exceptionOnContextSensitive;
  }

  public boolean isExceptionOnFullContext() {

    return exceptionOnFullContext;
  }

  public void setExceptionOnFullContext(final boolean exceptionOnFullContext) {

    this.exceptionOnFullContext = exceptionOnFullContext;
  }

  /**
   * Get all Errors. See other methods (yet to come to get specific types of errors/warnings).
   */
  public Iterator<ErrorDetails> getErrors() {

    return errors.iterator();
  }

  public Iterator<ErrorDetails> getDirectiveErrors() {

    return directiveErrors.iterator();
  }

  public boolean hasDirectiveErrors() {

    return !directiveErrors.isEmpty();
  }

  public boolean hasErrors() {

    return !isErrorFree();
  }

  public boolean isErrorFree() {

    return errors.isEmpty();
  }

  public Iterator<ErrorDetails> getWarnings() {

    return warnings.iterator();
  }

  public boolean hasWarnings() {

    return !isWarningFree();
  }

  /**
   * true if has no warnings.
   */
  public boolean isWarningFree() {

    return warnings.isEmpty();
  }

  /**
   * Create new semantic error.
   */
  public void raiseReturningNotRequired(final IToken token, final String msg) {

    semanticError(token, msg, SemanticClassification.RETURNING_NOT_REQUIRED);

  }

  /**
   * Create new semantic error.
   */
  public void raiseReturningRequired(final IToken token, final String msg) {

    semanticError(token, msg, SemanticClassification.RETURNING_REQUIRED);

  }

  /**
   * Create new semantic error.
   */
  public void semanticError(final IToken offendingToken,
                            final String msg,
                            final SemanticClassification classification,
                            final MatchResults results) {

    addErrorDetails(createSemanticError(offendingToken, msg, classification).setFuzzySearchResults(results));

  }

  /**
   * Create new semantic error.
   */
  public void semanticError(final IToken offendingToken,
                            final String msg,
                            final SemanticClassification classification) {

    addErrorDetails(createSemanticError(offendingToken, msg, classification));

  }

  /**
   * Create new semantic error.
   */
  public void semanticError(final Token offendingToken,
                            final String msg,
                            final SemanticClassification classification) {

    addErrorDetails(createSemanticError(new Ek9Token(offendingToken), msg, classification));

  }

  /**
   * Create a new error based on the directive. This means that if a directive was present or was missing
   * against an actual compiler error - we can raise this specific type of error to indicate something
   * that the EK9 developer was checking for (via a directive) is in error.
   */
  public void directiveError(final IToken offendingToken,
                             final String msg,
                             final SemanticClassification classification) {

    final var shortFileName = osSupport.getFileNameWithoutPath(offendingToken.getSourceName());
    final var tokenLength = offendingToken.getText().length();
    final var error = new ErrorDetails(ErrorClassification.DIRECTIVE_ERROR, offendingToken.getText(), shortFileName,
        offendingToken.getLine(), offendingToken.getCharPositionInLine(), tokenLength, msg);

    error.setSemanticClassification(classification);
    directiveErrors.add(error);

  }

  /**
   * Issue a semantic warning.
   */
  public void semanticWarning(final IToken offendingToken,
                              final String msg,
                              final SemanticClassification classification) {

    ErrorDetails warning;
    if (offendingToken == null) {
      warning = new ErrorDetails(ErrorClassification.SEMANTIC_WARNING, "Unknown Text", null, 0, 0, 1, msg);
    } else {
      final var tokenLength = offendingToken.getText().length();
      warning = new ErrorDetails(ErrorClassification.SEMANTIC_WARNING, offendingToken.getText(), null,
          offendingToken.getLine(), offendingToken.getCharPositionInLine(), tokenLength, msg);
    }
    warning.setSemanticClassification(classification);

    if (!warnings.contains(warning)) {
      warnings.add(warning);
    }

  }

  private ErrorDetails createSemanticError(final IToken offendingToken,
                                           final String msg,
                                           final SemanticClassification classification) {

    ErrorDetails error;

    if (offendingToken == null) {
      error = new ErrorDetails(ErrorClassification.SEMANTIC_ERROR, "Unknown Location", "unknown", 0, 0, 1, msg);
    } else {
      final var shortFileName = osSupport.getFileNameWithoutPath(offendingToken.getSourceName());
      final var tokenLength = offendingToken.getText().length();
      error = new ErrorDetails(ErrorClassification.SEMANTIC_ERROR, offendingToken.getText(), shortFileName,
          offendingToken.getLine(), offendingToken.getCharPositionInLine(), tokenLength, msg);
    }
    error.setSemanticClassification(classification);

    return error;
  }

  @Override
  public void syntaxError(final Recognizer<?, ?> recognizer,
                          final Object offendingSymbol,
                          final int line,
                          final int charPositionInLine,
                          final String msg,
                          final RecognitionException e) {

    ErrorDetails error;
    String reason = "Input not expected";
    if (msg != null) {
      //Handle our own messages
      if (msg.startsWith("_EK9")) {
        reason = msg.replace("_EK9", "Probable Cause");
      } else {
        reason = msg;
      }
    } else if (e != null) {
      reason = e.getClass().getSimpleName();
    }

    if (offendingSymbol instanceof IToken offender) {
      final var tokenLength = offender.getText().length();
      error = new ErrorDetails(ErrorClassification.SYNTAX_ERROR, offender.getText(), null, line, charPositionInLine,
          tokenLength, reason);
    } else {
      error = new ErrorDetails(ErrorClassification.SYNTAX_ERROR, "Unknown", null, line, charPositionInLine, 1, reason);
    }
    addErrorDetails(error);

  }

  private void addErrorDetails(final ErrorDetails details) {

    final var key = details.toLinePositionReference();

    if (!uniqueErrors.containsKey(key)) {
      uniqueErrors.put(key, details);
      errors.add(details);
    } else {
      //OK so we might have a match but this set of details might have fuzzy search results
      //But if we already have some fuzzy results then stick with those.
      if (details.isHoldingFuzzySearchResults()) {
        final var oldDetails = uniqueErrors.get(key);
        if (!oldDetails.isHoldingFuzzySearchResults()) {
          errors.remove(oldDetails);
          uniqueErrors.put(key, details);
          errors.add(details);
        }
      }
    }

  }

  /**
   * The type of the error.
   */
  public enum ErrorClassification {
    WARNING("Warning :"),
    DEPRECATION("Deprecat:"),
    SYNTAX_ERROR("Syntax  :"),
    SEMANTIC_WARNING("Warning :"),
    SEMANTIC_ERROR("Error   :"),
    DIRECTIVE_ERROR("Directiv:");

    private final String description;

    ErrorClassification(String description) {
      this.description = description;
    }
  }

  /**
   * What is the semantic nature of the error.
   */
  public enum SemanticClassification {
    // DIRECTIVE ERRORS (Multi-Phase E50xxx)
    UNKNOWN_DIRECTIVE("E50200", "invalid directive"),
    DIRECTIVE_MISSING("E50210", "but the directive is missing"),
    DIRECTIVE_WRONG_CLASSIFICATION("E50220", "directive error classification incorrect"),
    ERROR_MISSING("E50230", "but the error is missing"),
    DIRECTIVE_SYMBOL_COMPLEXITY("E50240", "complexity mismatch"),
    EXCESSIVE_COMPLEXITY("E11010", "excessive complexity - refactor"),
    EXCESSIVE_NESTING("E11011", "excessive nesting - extract function"),
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
    RETURN_TYPE_VOID_MEANINGLESS("E10010", "'void' return type cannot be used with an assignment"),
    TYPE_CANNOT_BE_CONSTRAINED("E04010", "is not a candidate to be constrained"),
    TYPE_MUST_BE_CONVERTABLE_TO_STRING("E04020",
        "result must be String, have the $ operator or can be promoted to String"),
    PARENTHESIS_NOT_REQUIRED("E06200", "use of parenthesis '( )' not allowed in this context"),
    PARENTHESIS_REQUIRED("E06210", "parenthesis '( )' required in this context"),
    VALUES_AND_TYPE_INCOMPATIBLE("E06220", "choose either empty parenthesis '( )' with values or a type definition"),
    GENERIC_TYPE_OR_FUNCTION_PARAMETERS_NEEDED("E06010", "type/function is generic but no parameters were supplied"),
    GENERIC_TYPE_OR_FUNCTION_PARAMETERS_INCORRECT("E06020",
        "type/function is generic, but incorrect number of parameters supplied"),
    GENERIC_TYPE_CONSTRUCTOR_INAPPROPRIATE("E06030",
        "type is generic, but for type inference to work; the number of generic "
            + "and constructor parameters must be the same"),
    GENERIC_TYPE_REQUIRES_TWO_CONSTRUCTORS("E06040",
        "a generic type requires 2 constructors, default and inferred type"),
    GENERIC_TYPE_REQUIRES_CORRECT_CONSTRUCTOR_ARGUMENT_TYPES("E06050",
        "a generic type requires correct constructor argument to match parametric types (and order)"),
    GENERIC_CONSTRUCTORS_MUST_BE_PUBLIC("E06060", "a generic type does not support private or protected constructors"),
    TYPE_INFERENCE_NOT_SUPPORTED("E06070", "type inference is not supported within generic/template type/functions"),
    CONSTRAINED_FUNCTIONS_NOT_SUPPORTED("E06080", "the constraining type with a generic/template cannot be a function"),
    USE_OF_THIS_OR_SUPER_INAPPROPRIATE("E05090",
        "can be used with :=:, :~:, +=, -=, /= and *=. But not direct assignment"),
    SERVICE_URI_WITH_VARS_NOT_SUPPORTED("E07680", "URI with place holder variable not supported in this context"),
    SERVICE_HTTP_ACCESS_NOT_SUPPORTED("E07690", "HTTP access verb not supported in this context"),
    SERVICE_HTTP_PATH_PARAM_INVALID("E07700", "HTTP PATH parameter is invalid"),
    SERVICE_HTTP_PARAM_NEEDS_QUALIFIER("E07710", "additional name needed for QUERY, HEADER or PATH parameters"),
    SERVICE_HTTP_PARAM_QUALIFIER_NOT_ALLOWED("E07720",
        "additional name only required for QUERY, HEADER or PATH parameters"),
    SERVICE_HTTP_PATH_DUPLICATED("E02070", "PATH is duplicated in terms of structure/naming"),
    SERVICE_HTTP_PATH_PARAM_COUNT_INVALID("E07730", "HTTP PATH variable count and PATH parameter count mismatch"),
    SERVICE_WITH_NO_BODY_PROVIDED("E07740", "implementation not provided, but services cannot be abstract"),
    SERVICE_INCOMPATIBLE_RETURN_TYPE("E07750", "Web Service return type must be compatible with HTTPResponse"),
    SERVICE_INCOMPATIBLE_PARAM_TYPE("E07760",
        "only Integer, String, Date, Time, DateTime, Milliseconds, Duration and HTTPRequest supported"),
    SERVICE_INCOMPATIBLE_PARAM_TYPE_REQUEST("E07770", "Web Service parameter type must be HTTPRequest for REQUEST"),
    SERVICE_REQUEST_BY_ITSELF("E07780", "Web Service type HTTPRequest can only be used by itself"),
    SERVICE_INCOMPATIBLE_PARAM_TYPE_NON_REQUEST("E07790", "Web Service parameter type cannot be HTTPRequest"),
    SERVICE_MISSING_RETURN("E07800", "Web Service must have return value and it must be compatible with HTTPResponse"),
    RETURNING_REQUIRED("E07405", "returning block required for assignment to 'lhs' when used in expression"),
    RETURNING_MISSING("E07400", "returning variable and type missing"),
    RETURNING_NOT_REQUIRED("E07406", "returning block is not required as there is no 'lhs' variable to assign it to"),
    MUST_RETURN_SAME_AS_CONSTRUCT_TYPE("E07410", "returning type must be same as the construct type"),
    MUST_NOT_RETURN_SAME_TYPE("E07420", "returning type must not be same for promotion"),
    METHOD_ACCESS_MODIFIER_PRIVATE_OVERRIDE("E07010", "remove 'override' with use of 'private' access modifier"),
    OVERRIDE_AND_ABSTRACT("E07020",
        "'override' of a method/operator and 'abstract' (no implementation) is not logical"),
    // Note: DEFAULT_AND_ABSTRACT removed - unreachable (default operator auto-generates, can't mark as abstract)
    DEFAULT_AND_TRAIT("E07030", "'default' of operators on a trait is not supported"),
    DUPLICATE_TRAIT_REFERENCE("E02050", "same trait referenced multiple times"),
    TRAIT_BY_IDENTIFIER_NOT_SUPPORTED("E07040", "'by' variable is not supported on a trait only on a class"),
    MISSING_OPERATOR_IN_THIS("E07180", "'default' of operators requires this type to have appropriate operator"),
    MISSING_OPERATOR_IN_SUPER("E07190", "'default' of operators requires super to have appropriate operator"),
    MISSING_OPERATOR_IN_PROPERTY_TYPE("E07200",
        "'default' of operators requires property/field to have appropriate operator"),
    FUNCTION_DELEGATE_WITH_DEFAULT_OPERATORS("E07210",
        "'default' of operators can only support '?' operator with function delegate fields"),
    OPERATOR_DEFAULT_NOT_SUPPORTED("E07220", "'default' is not supported on this operator"),
    DEFAULT_WITH_OPERATOR_SIGNATURE("E07230", "'default' with an operator must not have signature or body"),
    ABSTRACT_CONSTRUCTOR("E07050", "'abstract' modifier on a constructor is not logical"),
    OVERRIDE_CONSTRUCTOR("E07060", "'override' is not required on a constructor"),
    TRAITS_DO_NOT_HAVE_CONSTRUCTORS("E07070", "traits do not support constructor methods"),
    INVALID_DEFAULT_CONSTRUCTOR("E07080", "'default' constructor with parameters is not supported"),
    DEFAULT_ONLY_FOR_CONSTRUCTORS("E07090", "'default' modifier is only valid for constructors, not regular methods"),
    EXPLICIT_CONSTRUCTOR_REQUIRED("E07170",
        "a developer coded constructor(s) are require where uninitialized properties are used"),
    DUPLICATE_ENUMERATED_VALUES_PRESENT_IN_SWITCH("E02060", "duplicated enumerated value in switch 'case'"),
    NOT_ALL_ENUMERATED_VALUES_PRESENT_IN_SWITCH("E07310", "'cases' should cover all enumerated values in 'switch'"),
    DEFAULT_REQUIRED_IN_SWITCH_STATEMENT("E07320", "'default' is required in this 'switch' statement"),
    DEFAULT_REQUIRED_IN_SWITCH_EXPRESSION("E07330", "'default' is required in this 'switch' expression"),
    // Note: PRE_FLOW_OR_CONTROL_REQUIRED removed - grammar enforces at least one is present (unreachable)
    PRE_FLOW_SYMBOL_NOT_RESOLVED("E07340", "without a control, failed to find subject of flow"),
    GUARD_USED_IN_EXPRESSION("E07350", "a 'guard' cannot be used in an expression as it may leave 'lhs' uninitialised"),
    METHOD_MODIFIER_PROTECTED_IN_SERVICE("E07240",
        "non web service methods cannot be marked with the 'protected' access modifier"),
    METHOD_MODIFIER_PROTECTED_IN_COMPONENT("E07250",
        "component methods cannot be marked with the 'protected' access modifier"),
    METHOD_MODIFIER_PROTECTED_IN_CLOSED_CLASS("E07260",
        "class methods can only be marked with the 'protected' access modifier in classes that are 'open'"),
    METHOD_MODIFIER_NOT_REQUIRED_IN_TRAIT("E07270",
        "trait methods cannot be marked with an access modifier, they are always public"),
    RECORDS_ONLY_SUPPORT_CONSTRUCTOR_AND_OPERATOR_METHODS("E07290",
        "only constructors and operators methods are supported on records"),
    DECLARED_AS_NULL_NOT_NEEDED("E07300", "declaration supporting 'uninitialised' is not needed"),
    METHOD_ACCESS_MODIFIER_DEFAULT("E07280",
        "access modifier is not needed here - methods are 'public' by default"),
    METHODS_CONFLICT("E06150", "conflicting methods to be resolved"),
    APPLICATION_SELECTION_INVALID("E07360", "application selection not allowed in this context"),
    STATEMENT_UNREACHABLE("E07370", "all paths lead to an Exception"),
    RETURN_UNREACHABLE("E07380", "return not possible, as instructions only result in an Exception"),
    POINTLESS_EXPRESSION("E07390", "constant Boolean literal in expression is pointless"),
    TYPE_MUST_EXTEND_EXCEPTION("E04030", "type must be of Exception type"),
    SINGLE_EXCEPTION_ONLY("E07850", "only a single Exception is supported"),
    TYPE_MUST_BE_FUNCTION("E04040", "type must be a function or delegate"),
    TYPE_MUST_BE_SIMPLE("E04050", "type must be a simple aggregate/list/dict"),
    FUNCTION_OR_DELEGATE_REQUIRED("E07860", "require a function or function delegate"),
    INTEGER_VAR_OR_FUNCTION_OR_DELEGATE_REQUIRED("E07870", "require an Integer value or function/function delegate"),
    FUNCTION_OR_DELEGATE_NOT_REQUIRED("E07880", "a function or function delegate is not required here"),
    FUNCTION_MUST_HAVE_NO_PARAMETERS("E07450", "function must have no parameters"),
    FUNCTION_MUST_HAVE_SINGLE_PARAMETER("E07460", "function must have a single parameter"),
    FUNCTION_MUST_HAVE_TWO_PARAMETERS("E07470", "function must have a two parameters"),
    FUNCTION_PARAMETER_MISMATCH("E06270", "parameter mismatch"),
    NOT_A_FUNCTION_DELEGATE("E07480", "is not a function delegate"),
    DELEGATE_AND_METHOD_NAMES_CLASH("E02080", "use of a delegate and methods of same name"),
    // Note: PARAMETERS_MUST_BE_OF_SAME_TYPE removed - never implemented/used (unreachable code)
    STREAM_TYPE_NOT_DEFINED("E10020", "Void cannot be used in stream pipelines"),
    MUST_RETURN_BOOLEAN("E07520", "must return a Boolean"),
    ONLY_COMPATIBLE_WITH_BOOLEAN("E07530", "only compatible with Boolean type"),
    MUST_BE_A_BOOLEAN("E07540", "is not compatible with a Boolean type"),
    MUST_RETURN_INTEGER("E07550", "must return an Integer"),
    MUST_BE_INTEGER_GREATER_THAN_ZERO("E07560", "must be an Integer with a value greater than zero"),
    MUST_RETURN_STRING("E07570", "must return a String"),
    MUST_RETURN_JSON("E07580", "must return a JSON"),
    PROGRAM_CAN_ONLY_RETURN_INTEGER("E07590", "if a program returns a value it can only be an Integer"),
    PROGRAM_ARGUMENT_TYPE_INVALID("E07600", "program arguments are limited to a finite range of EK9 built in types"),
    PROGRAM_ARGUMENTS_INAPPROPRIATE("E07610", "inappropriate combination of program arguments"),
    TOO_MANY_ARGUMENTS("E06280", "too many arguments"),
    TOO_FEW_ARGUMENTS("E06290", "too few arguments"),
    REQUIRE_ONE_ARGUMENT("E06300", "require one argument only"),
    REQUIRE_NO_ARGUMENTS("E06310", "function must have no arguments"),
    FUNCTION_MUST_RETURN_VALUE("E07490", "function must return a value"),
    OPERATOR_MUST_BE_PURE("E07500", "operator must be declared pure"),
    OPERATOR_CANNOT_BE_PURE("E07510", "operator must not be declared pure"),
    DUPLICATE_PROPERTY_FIELD("E02010", "Property/Field duplicated"),
    CANNOT_SUPPORT_TO_JSON_DUPLICATE_PROPERTY_FIELD("E02020",
        "Property/Field duplicated, $$ (JSON) operator not supported"),
    METHOD_DUPLICATED("E02030", "duplicate/ambiguous methods/operations"),
    DUPLICATE_VARIABLE("E50050", "Variable/Constant duplicated"),
    DUPLICATE_VARIABLE_IN_CAPTURE("E02040", "variable name duplicated, resulting in multiple fields of same name"),
    DUPLICATE_NAME("E01030", "Variable/Function/Type duplicated, likely to lead to confusion"),
    POSSIBLE_DUPLICATE_ENUMERATED_VALUE("E01050", "are duplicated values (or are too similar, likely to be confusing)"),
    DUPLICATE_TYPE("E01040", "Type/Function name duplicated"),
    INVALID_SYMBOL_BY_REFERENCE("E01010", "invalid reference naming; module scope name missing"),
    INVALID_MODULE_NAME("E01020", "invalid module name"),
    CONSTRUCT_REFERENCE_CONFLICT("E03010", "conflicts with a reference"),
    REFERENCES_CONFLICT("E03020", "conflicting references"),
    REFERENCE_DOES_NOT_RESOLVED("E03030", "reference does not resolve"),
    DISPATCH_ONLY_SUPPORTED_IN_CLASSES("E07810", "Dispatch only supported in classes"),
    ABSTRACT_BUT_BODY_PROVIDED("E07100", "defined as default/abstract but an implementation has been provided"),
    CANNOT_BE_ABSTRACT("E50040", "cannot be abstract"),
    CANNOT_CALL_ABSTRACT_TYPE("E50080", "cannot make a call on an abstract function/type directly"),
    BAD_ABSTRACT_FUNCTION_USE("E50070", "cannot use an abstract function in this manner"),
    OVERRIDE_INAPPROPRIATE("E05100", "cannot override anything"),
    NOT_ABSTRACT_AND_NO_BODY_PROVIDED("E07110", "implementation not provided so must be declared as abstract"),
    DISPATCHER_BUT_NO_BODY_PROVIDED("E07120", "base level implementation must be provided for dispatcher method"),
    GENERIC_WITH_NAMED_DYNAMIC_CLASS("E06090", "a named dynamic class cannot be used within a generic type/function"),
    CAPTURED_VARIABLE_MUST_BE_NAMED("E06230", "variables being captured must be named when not just using identifiers"),
    EITHER_ALL_PARAMETERS_NAMED_OR_NONE("E06240", "either all variable must be named or none, when passing parameters"),
    NAMED_PARAMETERS_MUST_MATCH_ARGUMENTS("E06250", "the order and naming of arguments must match parameters"),
    GENERIC_FUNCTION_IMPLEMENTATION_REQUIRED("E06100", "implementation must be provided for generic function"),
    NOT_MARKED_ABSTRACT_BUT_IS_ABSTRACT("E07130", "not declared abstract but still has abstract methods/operators"),
    DYNAMIC_CLASS_MUST_IMPLEMENT_ABSTRACTS("E07140", "all abstract methods/operators must be implemented"),
    TEXT_METHOD_MISSING("E07150", "text method missing for language variant"),
    IMPLEMENTATION_MUST_BE_PROVIDED("E07160", "implementation must be provided"),
    PARAMETER_MISMATCH("E06260", "parameter mismatch"),
    UNABLE_TO_FIND_PIPE_FOR_TYPE("E07830", "unable to find a '|' pipe operator for type"),
    INCOMPATIBLE_TYPES("E50030", "types are not compatible with each other"),
    INCOMPATIBLE_TYPE_ARGUMENTS("E06330", "argument types are not compatible with each other"),
    CONSTRUCTOR_USED_ON_ABSTRACT_TYPE("E10030", "use of a constructor directly on an abstract type is not permitted."),
    CONSTRUCTOR_WITH_FUNCTION_IN_GENERIC("E06110",
        "use of a constructor in a generic/template that uses a function is not supported"),
    FUNCTION_USED_IN_GENERIC("E06120", "functions can be used in generics/templates but only '?' is supported"),
    IS_NOT_AN_AGGREGATE_TYPE("E04060", "not an aggregate type"),
    MISSING_ITERATE_METHOD("E07840", "it does not have compatible iterator() - hasNext()/next() methods"),
    // Note: AGGREGATE_HAS_NO_SUPER removed - now throws CompilerException (defensive code)
    SUPER_FOR_ANY_NOT_REQUIRED("E05040", "'super' for implicit 'Any' base class is not required"),
    THIS_AND_SUPER_MUST_BE_FIRST_IN_CONSTRUCTOR("E05050",
        "'this()' and 'super()' must be the first statement in a constructor"),
    THIS_AND_SUPER_CALLS_ONLY_IN_CONSTRUCTOR("E05060",
        "'this()' and 'super()' can only be used in constructors, did you mean 'this.' or 'super.'"),
    INAPPROPRIATE_USE_OF_THIS("E05070", "inappropriate use of 'this'"),
    INAPPROPRIATE_USE_OF_SUPER("E05080", "inappropriate use of 'super'"),
    NOT_MUTABLE("E07890", "not mutable"),
    INCOMPATIBLE_PARAMETER_GENUS("E50090", "incompatible genus in parameter(s)"),
    INCOMPATIBLE_GENUS("E50020", "incompatible genus"),
    INCOMPATIBLE_GENUS_CONSTRUCTOR("E05200", "incompatible genus with local constructor use"),
    INCOMPATIBLE_CATEGORY("E50100", "incompatible category"),
    CONSTRAINED_TYPE_CONSTRUCTOR_MISSING("E06130", "constraining type constructors must exist on parameterizing type"),
    NOT_OPEN_TO_EXTENSION("E05030", "not open to be extended"),
    TEMPLATE_TYPE_REQUIRES_PARAMETERIZATION("E04080", "Template/Generic requires parameterization"),
    NOT_A_TEMPLATE("E04070", "as it is not 'template/generic' in nature"),
    RESULT_MUST_HAVE_DIFFERENT_TYPES("E06190", "EK9 Result must be used with two different types"),
    DISPATCHERS_ONLY_HAVE_ONE_METHOD_ENTRY_POINT_MARKED("E07820",
        "only one method can be marked as a dispatcher entry point"),
    INVALID_NUMBER_OF_PARAMETERS("E06320", "invalid number of parameters"),
    OPERATOR_NOT_DEFINED("E07620", "operator not defined"),
    OPERATOR_CANNOT_BE_USED_ON_ENUMERATION("E07630", "operator cannot be used on an Enumeration in this way"),
    BAD_NOT_EQUAL_OPERATOR("E07640", "use <> for the not equal operator"),
    BAD_NOT_OPERATOR("E07650", "use ~ for the not operator"),
    OPERATOR_NAME_USED_AS_METHOD("E07660", "operator name cannot be used as a method"),
    SERVICE_OPERATOR_NOT_SUPPORTED("E07670", "operator not supported, only +, +=, -, -=, :^:, :~: and ? are supported"),
    METHOD_AMBIGUOUS("E06140", "ambiguous match"),
    NOT_IMMEDIATE_TRAIT("E06160", "not an immediate trait of this context"),
    TRAIT_ACCESS_NOT_SUPPORTED("E06170", "trait method access not supported here"),
    NOT_ACCESSIBLE("E06180", "not accessible from this context"),
    METHOD_OVERRIDES("E05120", "as it overrides method of same name/signature in hierarchy"),
    DOES_NOT_OVERRIDE("E05110", "does not 'override' any method/operator"),
    METHOD_ACCESS_MODIFIERS_DIFFER("E05130", "methods with same signature have different access modifiers"),
    FUNCTION_SIGNATURE_DOES_NOT_MATCH_SUPER("E05140", "function signature does not match 'super' function"),
    SUPER_IS_PURE("E05150", "'pure' in super requires 'pure' for this definition"),
    DISPATCHER_PURE_MISMATCH("E05170", "'pure' on dispatcher requires 'pure' for matching dispatcher method"),
    DISPATCHER_PRIVATE_IN_SUPER("E05180",
        "same method name as dispatcher, but marked private in super - won't be called"),
    SUPER_IS_NOT_PURE("E05160", "'super is not 'pure', requires this definition not to be marked as 'pure'"),
    MIX_OF_PURE_AND_NOT_PURE_CONSTRUCTORS("E05190", "if any constructor is marked pure, all constructors must be pure"),
    USED_BEFORE_DEFINED("E08010", "used before definition"),
    USED_BEFORE_INITIALISED("E08020", "might be used before being initialised"),
    UNSAFE_METHOD_ACCESS("E08030", "has not been checked before access"),
    NO_REASSIGNMENT_WITHIN_SAFE_ACCESS("E08040",
        "Reassignment/mutation within possible 'safe method access' scope is not allowed"),
    RETURN_NOT_ALWAYS_INITIALISED("E08050", "return value is not always initialised"),
    NOT_INITIALISED_BEFORE_USE("E08060", "is/may not be initialised before use"),
    NEVER_INITIALISED("E08070", "never initialised"),
    SELF_ASSIGNMENT("E08080", "self assignment"),
    NOT_REFERENCED("E08090", "is not referenced anywhere, or not referenced after assignment"),
    NO_PURE_REASSIGNMENT("E08100", "reassignment not allowed when scope is marked as 'pure' (':=?' is supported)"),
    NO_INCOMING_ARGUMENT_REASSIGNMENT("E08110", "reassignment not allowed of an incoming argument/parameter"),
    NO_MUTATION_IN_PURE_CONTEXT("E08120", "mutating variables is not allowed when scope is marked as 'pure'"),
    COVARIANCE_MISMATCH("E07440", "return types are incompatible (covariance required)"),
    RETURN_VALUE_NOT_SUPPORTED("E07430", "return not required/supported in this context"),
    COMPONENT_INJECTION_IN_PURE("E08140", "component injection not allowed when scope is marked as 'pure'"),
    COMPONENT_INJECTION_OF_NON_ABSTRACT("E08150",
        "dependency injection of a non-abstract component is not allowed, use an abstract base component"),
    COMPONENT_INJECTION_NOT_POSSIBLE("E08160", "dependency injection is not allowed"),
    REASSIGNMENT_OF_INJECTED_COMPONENT("E08170",
        "direct reassignment of an injected component is not allowed, use ':=?' for conditional reassignment"),
    NOT_INITIALISED_IN_ANY_WAY("E08180", "not marked for injection nor initialised"),
    NONE_PURE_CALL_IN_PURE_SCOPE("E08130",
        "is not marked 'pure', but call is made in a scope that is marked as 'pure'"),
    INVALID_VALUE("E07900", "Invalid value");

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

  /**
   * The details of an error, position, etc.
   */
  public static class ErrorDetails extends Details implements CompilationIssue, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ErrorClassification classification;
    private transient MatchResults fuzzySearchResults;

    private ErrorDetails(final ErrorClassification classification,
                         final String likelyOffendingSymbol,
                         final String shortFileName,
                         final int lineNumber,
                         final int characterNumber,
                         final int tokenLength,
                         final String typeOfError) {

      super(likelyOffendingSymbol, shortFileName, lineNumber, characterNumber, tokenLength, typeOfError);
      this.classification = classification;

    }

    public ErrorClassification getClassification() {

      return classification;
    }

    public String getClassificationDescription() {

      return classification.description;
    }

    /**
     * Adds any fuzzy match results, these could be the matches the developer was looking for.
     */
    public ErrorDetails setFuzzySearchResults(final MatchResults fuzzySearchResults) {

      this.fuzzySearchResults = fuzzySearchResults;

      return this;
    }

    public boolean isHoldingFuzzySearchResults() {

      return fuzzySearchResults != null;
    }

    @Override
    public boolean equals(final Object o) {

      if (this == o) {
        return true;
      }
      if (!(o instanceof ErrorDetails that)) {
        return false;
      }
      if (!super.equals(o)) {
        return false;
      }

      if (getClassification() != that.getClassification()) {
        return false;
      }

      return Objects.equals(fuzzySearchResults, that.fuzzySearchResults);
    }

    @Override
    public int hashCode() {

      int result = super.hashCode();
      result = 31 * result + getClassification().hashCode();
      result = 31 * result + (fuzzySearchResults != null ? fuzzySearchResults.hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      final var buffer = new StringBuilder(super.toString());

      if (fuzzySearchResults != null && fuzzySearchResults.size() > 0) {
        buffer.append(System.lineSeparator());
        buffer.append("Matches found: ");
        buffer.append(fuzzySearchResults.toString());
      }

      return buffer.toString();
    }
  }

  /**
   * Fine detail of the error.
   */
  public abstract static class Details implements CompilationIssue, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Not always set.
     */
    private final String possibleShortFileName;

    // Normally just before the error is actually reported.
    private final String likelyOffendingSymbol;
    private final String symbolErrorText;
    private final String typeOfError;
    private final int lineNumber;
    private final int position;
    private final int tokenLength;

    //Only used with semantic errors.
    private SemanticClassification semanticClassification;

    //Required for deserialization.

    private Details(final String likelyOffendingSymbol,
                    final String shortFileName,
                    final int lineNumber,
                    final int characterNumber,
                    final int tokenLength,
                    final String typeOfError) {

      this.likelyOffendingSymbol = likelyOffendingSymbol;
      this.typeOfError = typeOfError;
      this.possibleShortFileName = shortFileName;
      this.lineNumber = lineNumber;
      this.position = characterNumber;
      this.tokenLength = tokenLength;

      if (lineNumber != 0 || position != 0) {
        symbolErrorText = String.format("%s' on line %d position %d",
            likelyOffendingSymbol, lineNumber, characterNumber);
      } else {
        symbolErrorText = likelyOffendingSymbol + "'";
      }

    }

    public SemanticClassification getSemanticClassification() {

      return semanticClassification;
    }

    public void setSemanticClassification(SemanticClassification semanticClassification) {

      this.semanticClassification = semanticClassification;

    }

    @Override
    public String getTypeOfError() {

      return typeOfError;
    }

    @Override
    public int getLineNumber() {

      return lineNumber;
    }

    @Override
    public int getPosition() {

      return position;
    }

    @Override
    public int getTokenLength() {

      return tokenLength;
    }

    @Override
    public String getLikelyOffendingSymbol() {
      return likelyOffendingSymbol;
    }

    @Override
    public int hashCode() {

      return toString().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {

      if (obj instanceof Details) {
        return this.toString().equals(obj.toString());
      }

      return false;
    }

    /**
     * So we can reduce the number of errors on the line at a certain position.
     *
     * @return The unique vector for the position.
     */
    @Override
    public String toLinePositionReference() {

      final var buffer = new StringBuilder();
      if (possibleShortFileName != null) {
        buffer.append(possibleShortFileName);
      }
      buffer.append(":").append(lineNumber);

      return buffer.toString();
    }

    @Override
    public String toString() {

      final var buffer = new StringBuilder();

      buffer.append(getClassificationDescription());

      // Add error code if semantic classification is present
      if (semanticClassification != null) {
        buffer.append(" ").append(semanticClassification.getErrorCode()).append(":");
      }

      buffer.append(" '").append(symbolErrorText);

      if (typeOfError != null || semanticClassification != null) {
        buffer.append(": ");
      }

      if (typeOfError != null && !typeOfError.isEmpty()) {
        buffer.append(typeOfError).append(" ");
      }

      if (semanticClassification != null) {
        buffer.append(semanticClassification.description);
      }

      // Add documentation URL for semantic errors
      if (semanticClassification != null) {
        buffer.append(System.lineSeparator());
        buffer.append("             See: https://ek9.io/errors.html#");
        buffer.append(semanticClassification.getErrorCode());
      }

      return buffer.toString();
    }
  }
}
