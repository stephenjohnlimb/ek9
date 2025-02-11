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
    UNKNOWN_DIRECTIVE("invalid directive"),
    DIRECTIVE_MISSING("but the directive is missing"),
    DIRECTIVE_WRONG_CLASSIFICATION("directive error classification incorrect"),
    ERROR_MISSING("but the error is missing"),
    DIRECTIVE_SYMBOL_COMPLEXITY("complexity mismatch"),
    EXCESSIVE_COMPLEXITY("excessive complexity - refactor"),
    DIRECTIVE_SYMBOL_NOT_RESOLVED("symbol not resolved"),
    DIRECTIVE_HIERARCHY_NOT_RESOLVED("symbol hierarchy not in place"),
    DIRECTIVE_SYMBOL_CATEGORY_MISMATCH("symbol category mismatched"),
    DIRECTIVE_SYMBOL_GENUS_MISMATCH("symbol genus mismatched"),
    DIRECTIVE_SYMBOL_NO_SUCH_GENUS("genus does not exist"),
    DIRECTIVE_SYMBOL_FOUND_UNEXPECTED_SYMBOL("unexpected symbol resolved"),
    DIRECTIVE_ERROR_MISMATCH("count does not match"),
    NOT_RESOLVED_FUZZY_MATCH("is the closest match found"),
    NOT_RESOLVED("not resolved"),
    CANNOT_EXTEND_IMPLEMENT_ITSELF("extension/implementation of self is not logical"),
    CIRCULAR_HIERARCHY_DETECTED("a circular type/function hierarchy has been used"),
    OBJECT_NOT_RESOLVED("object not resolved"),
    FIELD_NOT_RESOLVED("field not resolved"),
    FIELD_OR_VARIABLE_NOT_RESOLVED("field/variable not resolved"),
    METHOD_NOT_RESOLVED("method/function not resolved"),
    TYPE_NOT_RESOLVED("type not resolved"),
    INAPPROPRIATE_USE("inappropriate use in this context"),
    RETURN_TYPE_VOID_MEANINGLESS("'void' return type cannot be used with an assignment"),
    UNABLE_TO_DETERMINE_COMMON_TYPE("unable to determine a common type in this expression"),
    TYPE_IN_FOR_LOOP_NOT_RESOLVED("type of variable in for loop therefore not resolved"),
    TYPE_AMBIGUOUS("type use is ambiguous"),
    TYPE_CANNOT_BE_CONSTRAINED("is not a candidate to be constrained"),
    TYPE_MUST_BE_STRING("result must be String or can be promoted to String"),
    TYPE_MUST_BE_CONVERTABLE_TO_STRING(
        "result must be String, have the $ operator or can be promoted to String"),
    SYMBOL_LOCATED_BUT_TYPE_NOT_RESOLVED("symbol located but its type is not resolved"),
    TYPE_OR_FUNCTION_NOT_RESOLVED("type/function of variable not resolved"),
    //In the generic case where we're not sure what you're looking for.
    CONSTRUCTOR_NOT_RESOLVED("constructor not resolved with specified parameters"),
    CONSTRUCTOR_BY_JSON_NOT_RESOLVED("constructor of this type with single JSON parameter is not resolved"),
    CONSTRUCTOR_NOT_RESOLVED_IN_GENERIC_CONTEXT(
        "constructor not resolved, check parameters where this generic type is being created"),
    PARENTHESIS_NOT_REQUIRED("use of parenthesis '( )' not allowed in this context"),
    PARENTHESIS_REQUIRED("parenthesis '( )' required in this context"),
    VALUES_AND_TYPE_INCOMPATIBLE("choose either empty parenthesis '( )' with values or a type definition"),
    GENERIC_TYPE_OR_FUNCTION_PARAMETERS_NEEDED("type/function is generic but no parameters were supplied"),
    GENERIC_TYPE_OR_FUNCTION_PARAMETERS_INVALID("this generic usage with this type is not allowed"),
    GENERIC_TYPE_OR_FUNCTION_PARAMETERS_INCORRECT(
        "type/function is generic, but incorrect number of parameters supplied"),
    GENERIC_TYPE_CONSTRUCTOR_INAPPROPRIATE(
        "type is generic, but for type inference to work; the number of generic "
            + "and constructor parameters must be the same"),
    GENERIC_TYPE_REQUIRES_TWO_CONSTRUCTORS("a generic type requires 2 constructors, default and inferred type"),
    GENERIC_TYPE_REQUIRES_CORRECT_CONSTRUCTOR_ARGUMENT_TYPES(
        "a generic type requires correct constructor argument to match parametric types (and order)"),
    GENERIC_CONSTRUCTORS_MUST_BE_PUBLIC("a generic type does not support private or protected constructors"),
    GENERIC_TYPE_OR_FUNCTION_PARAMETER_MISMATCH(
        "type/function is generic but generic parameters and constructor parameters conflict"),
    GENERIC_TYPE_OR_FUNCTION_NOT_RESOLVED(
        "type/function is generic but could not be resolved, use 'define type as' and type inference will work."),
    TYPE_INFERENCE_NOT_SUPPORTED("type inference is not supported within generic/template type/functions"),
    CONSTRAINED_FUNCTIONS_NOT_SUPPORTED("the constraining type with a generic/template cannot be a function"),
    GENERIC_TYPE_OR_FUNCTION_NOT_APPLICABLE("type/function is not generic but parameters were supplied"),
    TYPE_REQUIRED_FOR_PROPERTIES(
        "type must be declared for this property - '<-' operator is not supported with complex call"),
    USE_OF_THIS_OR_SUPER_INAPPROPRIATE("can be used with :=:, :~:, +=, -=, /= and *=. But not direct assignment"),
    INVALID_TEXT_INTERPOLATION("interpolated text is invalid - check balanced ${}"),
    DURATION_NOT_FULLY_SPECIFIED("duration is not fully specified expecting P[n]Y[n]M[n]W[n]DT[n]H[n]M[n]S"),
    SERVICE_URI_WITH_VARS_NOT_SUPPORTED("URI with place holder variable not supported in this context"),
    SERVICE_HTTP_CACHING_NOT_SUPPORTED("HTTP caching is not supported in this context"),
    SERVICE_HTTP_ACCESS_NOT_SUPPORTED("HTTP access verb not supported in this context"),
    SERVICE_HTTP_ACCESS_NAME_NOT_SUPPORTED("HTTP access name mapping not supported in this context"),
    SERVICE_HTTP_HEADER_MISSING("use of HEADER requires valid 'http header' value"),
    SERVICE_HTTP_HEADER_INVALID("HTTP HEADER name is invalid"),
    SERVICE_HTTP_PATH_PARAM_INVALID("HTTP PATH parameter is invalid"),
    SERVICE_HTTP_PARAM_NEEDS_QUALIFIER("additional name needed for QUERY, HEADER or PATH parameters"),
    SERVICE_HTTP_PARAM_QUALIFIER_NOT_ALLOWED(
        "additional name only required for QUERY, HEADER or PATH parameters"),
    SERVICE_HTTP_PATH_DUPLICATED("PATH is duplicated in terms of structure/naming"),
    SERVICE_HTTP_PATH_PARAM_COUNT_INVALID("HTTP PATH variable count and PATH parameter count mismatch"),
    SERVICE_HTTP_PATH_ASSUMED_BUT_INVALID("HTTP PATH parameter was assumed, did you mean HEADER, QUERY or BODY?"),
    SERVICE_HTTP_BODY_MAPPING_NOT_ALLOWED("HTTP BODY parameter cannot have a mapping name"),
    SERVICE_WITH_NO_BODY_PROVIDED("implementation not provided, but services cannot be abstract"),
    SERVICE_INCOMPATIBLE_RETURN_TYPE("Web Service return type must be compatible with HTTPResponse"),
    SERVICE_INCOMPATIBLE_PARAM_TYPE(
        "only Integer, String, Date, Time, DateTime, Milliseconds, Duration and HTTPRequest supported"),
    SERVICE_INCOMPATIBLE_PARAM_TYPE_REQUEST("Web Service parameter type must be HTTPRequest for REQUEST"),
    SERVICE_REQUEST_BY_ITSELF("Web Service type HTTPRequest can only be used by itself"),
    SERVICE_INCOMPATIBLE_PARAM_TYPE_NON_REQUEST("Web Service parameter type cannot be HTTPRequest"),
    SERVICE_MISSING_RETURN("Web Service must have return value and it must be compatible with HTTPResponse"),
    GENERIC_TYPE_DEFINITION_CANNOT_EXTEND("this generic class definition cannot extend other classes or generic types"),
    TYPE_REQUIRED_FOR_RETURN("type must be declared for returning values"),
    RETURNING_REQUIRED("returning block required for assignment to 'lhs' when used in expression"),
    RETURNING_MISSING("returning variable and type missing"),
    RETURNING_NOT_REQUIRED("returning block is not required as there is no 'lhs' variable to assign it to"),
    MUST_RETURN_SAME_AS_CONSTRUCT_TYPE("returning type must be same as the construct type"),
    MUST_RETURN_SAME_ARGUMENT_TYPE("returning type must be same as the argument(s) type"),
    MUST_NOT_RETURN_SAME_TYPE("returning type must not be same for promotion"),
    METHOD_ACCESS_MODIFIER_PRIVATE_OVERRIDE("remove 'override' with use of 'private' access modifier"),
    OVERRIDE_AND_ABSTRACT("'override' of a method/operator and 'abstract' (no implementation) is not logical"),
    DEFAULT_AND_ABSTRACT("'default' of an operator and 'abstract' is not logical"),
    DEFAULT_AND_TRAIT("'default' of operators on a trait is not supported"),
    DUPLICATE_TRAIT_REFERENCE("same trait referenced multiple times"),
    TRAIT_BY_IDENTIFIER_NOT_SUPPORTED("'by' variable is not supported on a trait only on a class"),
    MISSING_OPERATOR_IN_THIS("'default' of operators requires this type to have appropriate operator"),
    MISSING_OPERATOR_IN_SUPER("'default' of operators requires super to have appropriate operator"),
    MISSING_OPERATOR_IN_PROPERTY_TYPE("'default' of operators requires property/field to have appropriate operator"),
    FUNCTION_DELEGATE_WITH_DEFAULT_OPERATORS(
        "'default' of operators can only support '?' operator with function delegate fields"),
    OPERATOR_DEFAULT_NOT_SUPPORTED("'default' is not supported on this operator"),
    DEFAULT_WITH_OPERATOR_SIGNATURE("'default' with an operator must not have signature or body"),
    ABSTRACT_CONSTRUCTOR("'abstract' modifier on a constructor is not logical"),
    OVERRIDE_CONSTRUCTOR("'override' is not required on a constructor"),
    TRAITS_DO_NOT_HAVE_CONSTRUCTORS("traits do not support constructor methods"),
    INVALID_DEFAULT_CONSTRUCTOR("'default' constructor with parameters is not supported"),
    EXPLICIT_CONSTRUCTOR_REQUIRED(
        "a developer coded constructor(s) are require where uninitialized properties are used"),
    DUPLICATE_ENUMERATED_VALUES_PRESENT_IN_SWITCH("duplicated enumerated value in switch 'case'"),
    NOT_ALL_ENUMERATED_VALUES_PRESENT_IN_SWITCH("'cases' should cover all enumerated values in 'switch'"),
    DEFAULT_REQUIRED_IN_SWITCH_STATEMENT("'default' is required in this 'switch' statement"),
    DEFAULT_REQUIRED_IN_SWITCH_EXPRESSION("'default' is required in this 'switch' expression"),
    GUARD_USED_IN_EXPRESSION("a 'guard' cannot be used in an expression as it may leave 'lhs' uninitialised"),
    METHOD_MODIFIER_PROTECTED_IN_SERVICE(
        "non web service methods cannot be marked with the 'protected' access modifier"),
    METHOD_MODIFIER_PROTECTED_IN_COMPONENT(
        "component methods cannot be marked with the 'protected' access modifier"),
    METHOD_MODIFIER_PROTECTED_IN_CLOSED_CLASS(
        "class methods can only be marked with the 'protected' access modifier in classes that are 'open'"),
    METHOD_MODIFIER_NOT_REQUIRED_IN_TRAIT(
        "trait methods cannot be marked with an access modifier, they are always public"),
    RECORDS_ONLY_SUPPORT_CONSTRUCTOR_AND_OPERATOR_METHODS(
        "only constructors and operators methods are supported on records"),
    MUST_BE_DECLARED_AS_POSSIBLE_NULL("declaration must indicate that this can be 'uninitialised'"),
    CAN_BE_ASSIGNED_NULL_VALUE("assignment could result in 'uninitialised', so declaration must support this"),
    DECLARED_AS_NULL_NOT_NEEDED("declaration supporting 'uninitialised' is not needed"),
    METHOD_ACCESS_MODIFIER_DEFAULT(
        "access modifier is not needed here - methods are 'public' by default"),
    METHODS_CONFLICT("conflicting methods to be resolved"),
    ACCESS_MODIFIER_INAPPROPRIATE("access modifier inappropriate here"),
    APPLICATION_SELECTION_INVALID("application selection not allowed in this context"),
    STATEMENT_UNREACHABLE("all paths lead to an Exception"),
    RETURN_UNREACHABLE("return not possible, as instructions only result in an Exception"),
    POINTLESS_EXPRESSION("constant Boolean literal in expression is pointless"),
    TYPE_IS_ABSTRACT("type is abstract and cannot be instantiated"),
    TYPE_IS_INJECTABLE("type is or extends an injectable component"),
    TYPE_IS_NOT_INJECTABLE("type is not an injectable component"),
    TYPE_IS_NOT_ASPECT("type is not an aspect"),
    TYPE_MUST_EXTEND_EXCEPTION("type must be of Exception type"),
    SINGLE_EXCEPTION_ONLY("only a single Exception is supported"),
    TYPE_MUST_BE_FUNCTION("type must be a function or delegate"),
    TYPE_MUST_NOT_BE_FUNCTION("type must not be a function"),
    TYPE_MUST_BE_SIMPLE("type must be a simple aggregate/list/dict"),
    FUNCTION_OR_DELEGATE_REQUIRED("require a function or function delegate"),
    INTEGER_VAR_OR_FUNCTION_OR_DELEGATE_REQUIRED("require an Integer value or function/function delegate"),
    FUNCTION_OR_DELEGATE_NOT_REQUIRED("a function or function delegate is not required here"),
    PARAM_MUST_BE_VARIABLE("parameter must be a variable"),
    FUNCTION_MUST_HAVE_NO_PARAMETERS("function must have no parameters"),
    FUNCTION_MUST_HAVE_SINGLE_PARAMETER("function must have a single parameter"),
    FUNCTION_MUST_HAVE_TWO_PARAMETERS("function must have a two parameters"),
    FUNCTION_PARAMETER_MISMATCH("parameter mismatch"),
    NOT_A_FUNCTION_DELEGATE("is not a function delegate"),
    DELEGATE_AND_METHOD_NAMES_CLASH("use of a delegate and methods of same name"),
    PARAMETERS_MUST_BE_OF_SAME_TYPE("parameters must be of the same type"),
    STREAM_TYPE_CANNOT_CONSUME("cannot establish what type can be consumed"),
    STREAM_TYPE_NOT_DEFINED("stream pipeline parts must produce a type"),
    STREAM_TYPE_CANNOT_PRODUCE("cannot establish what type can be produced by this"),
    STREAM_GT_REQUIRES_CLEAR("a method named 'clear()' is required with that stream operator"),
    STREAM_PARAMETERS_ONLY_ONE_PRODUCER("only a single producer type is supported"),
    MUST_RETURN_BOOLEAN("must return a Boolean"),
    MUST_BE_A_BOOLEAN("is not compatible with a Boolean type"),
    MUST_RETURN_INTEGER("must return an Integer"),
    MUST_BE_INTEGER_GREATER_THAN_ZERO("must be an Integer with a value greater than zero"),
    MUST_RETURN_STRING("must return a String"),
    MUST_RETURN_JSON("must return a JSON"),
    PROGRAM_CAN_ONLY_RETURN_INTEGER("if a program returns a value it can only be an Integer"),
    ONLY_SIMPLE_RETURNING_TYPES_SUPPORTED("only simple inferred types are supported for returning values"),
    PROGRAM_ARGUMENT_TYPE_INVALID("program arguments are limited to a finite range of EK9 built in types"),
    PROGRAM_ARGUMENTS_INAPPROPRIATE("inappropriate combination of program arguments"),
    TOO_MANY_ARGUMENTS("too many arguments"),
    TOO_FEW_ARGUMENTS("too few arguments"),
    REQUIRE_ONE_ARGUMENT("require one argument only"),
    REQUIRE_NO_ARGUMENTS("function must have no arguments"),
    FUNCTION_MUST_RETURN_VALUE("function must return a value"),
    FUNCTION_MUST_RETURN_SAME_TYPE_AS_INPUT("function must return the same type as the input type"),
    CALL_DOES_NOT_RETURN_ANYTHING("Right hand side does not return a value"),
    CONVERT_CONSTANT_TO_VARIABLE("convert this constant to a variable"),
    OPERATOR_MUST_BE_PURE("operator must be declared pure"),
    OPERATOR_CANNOT_BE_PURE("operator must not be declared pure"),
    CONSTANT_PARAM_NEEDS_PURE(
        "use 'pure' modifier with a constant parameter or convert the constant to a variable"),
    DUPLICATE_PROPERTY_FIELD("Property/Field duplicated"),
    CANNOT_SUPPORT_TO_JSON_DUPLICATE_PROPERTY_FIELD("Property/Field duplicated, $$ (JSON) operator not supported"),
    METHOD_DUPLICATED("duplicate/ambiguous methods/operations"),
    DUPLICATE_VARIABLE("Variable/Constant duplicated"),
    DUPLICATE_VARIABLE_IN_CAPTURE("variable name duplicated, resulting in multiple fields of same name"),
    OVERLOADING_NOT_SUPPORTED("Method/Operator overloading not supported in Template Class"),
    DUPLICATE_NAME("Variable/Function/Type duplicated, likely to lead to confusion"),
    POSSIBLE_DUPLICATE_ENUMERATED_VALUE("are duplicated values (or are too similar, likely to be confusing)"),
    EXCEPTION_ONLY_SINGLE_PARAMETER("Exception handling is for a single parameter only"),
    DUPLICATE_TYPE("Type/Function name duplicated"),
    DUPLICATE_SYMBOL("Symbol duplicated"),
    INVALID_SYMBOL_BY_REFERENCE("invalid reference naming; module scope name missing"),
    INVALID_MODULE_NAME("invalid module name"),
    CONSTRUCT_REFERENCE_CONFLICT("conflicts with a reference"),
    REFERENCES_CONFLICT("conflicting references"),
    REFERENCE_DOES_NOT_RESOLVED("reference does not resolve"),
    DISPATCH_ONLY_SUPPORTED_IN_CLASSES("Dispatch only supported in classes"),
    TRAIT_DELEGATE_NOT_USED("has not been used for any methods as they have been defined"),
    ABSTRACT_BUT_BODY_PROVIDED("defined as default/abstract but an implementation has been provided"),
    CANNOT_BE_ABSTRACT("cannot be abstract"),
    CANNOT_CALL_ABSTRACT_TYPE("cannot make a call on an abstract function/type directly"),
    BAD_ABSTRACT_FUNCTION_USE("cannot use an abstract function in this manner"),
    OVERRIDE_INAPPROPRIATE("cannot override anything"),
    NOT_ABSTRACT_AND_NO_BODY_PROVIDED("implementation not provided so must be declared as abstract"),
    DISPATCHER_BUT_NO_BODY_PROVIDED("base level implementation must be provided for dispatcher method"),
    DYNAMIC_CLASS_CANNOT_BE_ABSTRACT("a dynamic class cannot have abstract methods"),
    GENERIC_WITH_NAMED_DYNAMIC_CLASS("a named dynamic class cannot be used within a generic type/function"),
    CAPTURED_VARIABLE_MUST_BE_NAMED("variables being captured must be named when not just using identifiers"),
    EITHER_ALL_PARAMETERS_NAMED_OR_NONE("either all variable must be named or none, when passing parameters"),
    NAMED_PARAMETERS_MUST_MATCH_ARGUMENTS("the order and naming of arguments must match parameters"),
    GENERIC_FUNCTION_IMPLEMENTATION_REQUIRED("implementation must be provided for generic function"),
    NOT_MARKED_ABSTRACT_BUT_IS_ABSTRACT("not declared abstract but still has abstract methods/operators"),
    DYNAMIC_CLASS_MUST_IMPLEMENT_ABSTRACTS("all abstract methods/operators must be implemented"),
    ABSTRACT_METHOD_NOT_IMPLEMENTED("method is abstract - should be overridden/implemented"),
    TEXT_METHOD_MISSING("text method missing for language variant"),
    METHOD_NOT_OVERRIDDEN("method must be overridden/implemented"),
    IMPLEMENTATION_MUST_BE_PROVIDED("implementation must be provided"),
    PARAMETER_MISMATCH("parameter mismatch"),
    UNABLE_TO_FIND_PIPE_FOR_TYPE("unable to find a '|' pipe operator for type"),
    INCOMPATIBLE_TYPES("types are not compatible with each other"),
    INCOMPATIBLE_TYPE_ARGUMENTS("argument types are not compatible with each other"),
    CONSTRUCTOR_USED_ON_ABSTRACT_TYPE("use of a constructor directly on an abstract type is not permitted."),
    CONSTRUCTOR_WITH_FUNCTION_IN_GENERIC(
        "use of a constructor in a generic/template that uses a function is not supported"),
    FUNCTION_USED_IN_GENERIC("functions can be used in generics/templates but only '?' is supported"),
    INCOMPATIBLE_TYPES_BUT_CONSTRUCTOR_EXISTS(
        "- constructor would be appropriate: direct type usage is not appropriate in this context"),
    NOT_IN_AN_AGGREGATE_TYPE("not contained within an aggregate type (class/component/etc)"),
    IS_NOT_AN_AGGREGATE_TYPE("not an aggregate type"),
    MISSING_ITERATE_METHOD("it does not have compatible iterator() - hasNext()/next() methods"),
    ITERATE_METHOD_MUST_RETURN_ITERATOR("iterate method must return an Iterator"),
    AGGREGATE_HAS_NO_SUPER("but has no 'super'"),
    THIS_AND_SUPER_MUST_BE_FIRST_IN_CONSTRUCTOR("'this()' and 'super()' must be the first statement in a constructor"),
    THIS_AND_SUPER_CALLS_ONLY_IN_CONSTRUCTOR(
        "'this()' and 'super()' can only be used in constructors, did you mean 'this.' or 'super.'"),
    INAPPROPRIATE_USE_OF_THIS("inappropriate use of 'this'"),
    INAPPROPRIATE_USE_OF_SUPER("inappropriate use of 'super'"),
    INVALID_LITERAL("invalid literal"),
    INVALID_LITERAL_MUST_BE_GREATER_THAN_ZERO("must be integer value greater than zero"),
    NOT_MUTABLE("not mutable"),
    ONLY_CONSTANTS_ALLOWED("only constant values allowed"),
    MUTABLE_NOT_ALLOWED("mutable items not allowed"),
    INCOMPATIBLE_PARAMETER_GENUS("incompatible genus in parameter(s)"),
    INCOMPATIBLE_GENUS("incompatible genus"),
    INCOMPATIBLE_CATEGORY("incompatible category"),
    TRAIT_BY_DELEGATE_FOR_CLASS_ONLY("delegation by a trait is only applicable for classes"),
    ONLY_CONSTRUCTORS_ALLOWED("only constructor methods can be defined in this scope"),
    ONLY_ONE_CONSTRUCTOR_ALLOWED("only a single constructor is allowed in this scope"),
    CONSTRAINED_TYPE_CONSTRUCTOR_MISSING("constraining type constructors must exist on parameterizing type"),
    CLASS_IS_NOT_ALLOWED_IN_THIS_CONTEXT("not allowed in this context"),
    NOT_OPEN_TO_EXTENSION("not open to be extended"),
    TEMPLATE_TYPES_NOT_EXTENSIBLE("Template/Generic types cannot be extended"),
    TEMPLATE_TYPE_REQUIRES_PARAMETERIZATION("Template/Generic requires parameterization"),
    NOT_A_TEMPLATE("as it is not 'template/generic' in nature"),
    RESULT_MUST_HAVE_DIFFERENT_TYPES("EK9 Result must be used with two different types"),
    DISPATCHERS_NOT_EXTENDABLE("extension is not possible"),
    DISPATCHERS_ONLY_HAVE_ONE_METHOD_ENTRY_POINT_MARKED("only one method can be marked as a dispatcher entry point"),
    INVALID_NUMBER_OF_PARAMETERS("invalid number of parameters"),
    INVALID_PARAMETER_TYPE("invalid parameter type"),
    SIGNATURE_MISMATCH("parameter type mismatch"),
    OPERATOR_EQUALS_AND_HASHCODE_INCONSISTENT("operators should be implemented for consistency"),
    OVERRIDE_OPERATOR_EQUALS("operator == should be overridden for consistency"),
    OVERRIDE_OPERATOR_HASHCODE("operator #? should be overridden for consistency"),
    OPERATOR_NOT_DEFINED("operator not defined"),
    OPERATOR_CANNOT_BE_USED_ON_ENUMERATION("operator cannot be used on an Enumeration in this way"),
    BAD_NOT_EQUAL_OPERATOR("use <> for the not equal operator"),
    BAD_NOT_OPERATOR("use ~ for the not operator"),
    OPERATOR_DOES_NOT_SUPPORT_PARAMETERS("operator does not support parameters"),
    OPERATOR_REQUIRES_PARAMETER("operator requires a single parameter"),
    OPERATOR_INCORRECT_RETURN_TYPE("operator has incorrect return type or no return type"),
    OPERATOR_NAME_USED_AS_METHOD("operator name cannot be used as a method"),
    SERVICE_OPERATOR_NOT_SUPPORTED("operator not supported, only +, +=, -, -=, :^:, :~: and ? are supported"),
    NO_VERB_REQUIRED_WITH_SERVICE_OPERATOR("use of operator means that verb is implied and cannot be specified"),
    OPERATOR_NOT_DEFINED_FROM_GENERIC(
        "operator must be defined when using a generic implementation with a specific type"),
    IMPLIED_OPERATOR_NOT_DEFINED_FROM_GENERIC(
        "implied operator must be defined when using a generic implementation with a specific type"),
    OPERATOR_AMBIGUOUS("operator ambiguous match"),
    METHOD_AMBIGUOUS("ambiguous match"),
    NOT_IMMEDIATE_TRAIT("not an immediate trait of this context"),
    TRAIT_ACCESS_NOT_SUPPORTED("trait method access not supported here"),
    NOT_IMMEDIATE_SUPER("not an immediate super of this context"),
    NOT_ACCESSIBLE("not accessible from this context"),
    NO_REASSIGNMENT_FROM_CONTEXT_AS_PURE("not re-assignable from this 'pure' context"),
    TYPE_ADDRESS_NOT_SUITABLE("addressing access in this way is not suitable from this context"),
    METHOD_OVERRIDES("as it overrides method of same name/signature in hierarchy"),
    DOES_NOT_OVERRIDE("does not 'override' any method/operator"),
    METHOD_ACCESS_MODIFIERS_DIFFER("methods with same signature have different access modifiers"),
    FUNCTION_SIGNATURE_DOES_NOT_MATCH_SUPER("function signature does not match 'super' function"),
    SUPER_IS_PURE("'pure' in super requires 'pure' for this definition"),
    DISPATCHER_PURE_MISMATCH("'pure' on dispatcher requires 'pure' for matching dispatcher method"),
    DISPATCHER_PRIVATE_IN_SUPER("same method name as dispatcher, but marked private in super - won't be called"),
    DISPATCHER_PRIVATE_ENTRY("private dispatcher method, other matching methods cannot be public"),
    SUPER_IS_NOT_PURE("'super is not 'pure', requires this definition not to be marked as 'pure'"),
    MIX_OF_PURE_AND_NOT_PURE_CONSTRUCTORS("if any constructor is marked pure, all constructors must be pure"),
    SWITCH_REQUIRES_EQUALS("switch statement requires type to have '=' operator"),
    USED_BEFORE_DEFINED("used before definition"),
    USED_BEFORE_INITIALISED("might be used before being initialised"),
    UNSAFE_METHOD_ACCESS("has not been checked before access"),
    RETURN_NOT_ALWAYS_INITIALISED("return value is not always initialised"),
    NOT_INITIALISED_BEFORE_USE("is/may not be initialised before use"),
    NEVER_INITIALISED("never initialised"),
    SELF_ASSIGNMENT("self assignment"),
    NOT_REFERENCED("is not referenced anywhere, or not referenced after assignment"),
    LIKELY_DEFECT("likely defect"),
    NO_PURE_REASSIGNMENT("reassignment not allowed when scope is marked as 'pure' (':=?' is supported)"),
    NO_INCOMING_ARGUMENT_REASSIGNMENT("reassignment not allowed of an incoming argument/parameter"),
    NO_MUTATION_IN_PURE_CONTEXT("mutating variables is not allowed when scope is marked as 'pure'"),
    COVARIANCE_MISMATCH("return types are incompatible (covariance required)"),
    RETURN_VALUE_NOT_SUPPORTED("return not required/supported in this context"),
    COMPONENT_INJECTION_IN_PURE("component injection not allowed when scope is marked as 'pure'"),
    COMPONENT_INJECTION_OF_NON_ABSTRACT(
        "dependency injection of a non-abstract component is not allowed, use an abstract base component"),
    COMPONENT_INJECTION_NOT_POSSIBLE("dependency injection is not allowed"),
    REASSIGNMENT_OF_INJECTED_COMPONENT(
        "direct reassignment of an injected component is not allowed, use ':=?' for conditional reassignment"),
    NOT_INITIALISED_IN_ANY_WAY("not marked for injection nor initialised"),
    COMPONENT_NOT_MARKED_FOR_INJECTION("component not marked for injection"),
    USE_OF_NULLABLE_NOT_POSSIBLE("use of nullable is meaningless here"),
    NONE_PURE_CALL_IN_PURE_SCOPE("is not marked 'pure', but call is made in a scope that is marked as 'pure'"),
    DEFAULT_VALUE_SHOULD_BE_PROVIDED("A Default value should be provided"),
    INVALID_VALUE("Invalid value"),
    DEFAULT_VALUE_WILL_NOT_BE_USED("A Default value will not be used in this context (abstract function/method)");

    private final String description;

    SemanticClassification(String description) {
      this.description = description;
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

      return buffer.toString();
    }
  }
}
